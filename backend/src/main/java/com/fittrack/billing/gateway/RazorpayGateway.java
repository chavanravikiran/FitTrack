package com.fittrack.billing.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fittrack.common.exception.ServiceUnavailableException;
import com.fittrack.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Razorpay integration using the Orders API + Standard Checkout.
 * <ul>
 *   <li>Order: {@code POST /v1/orders} (HTTP Basic auth with key id/secret).</li>
 *   <li>Checkout signature: {@code HMAC_SHA256(order_id + "|" + payment_id, key_secret)}.</li>
 *   <li>Webhook signature: {@code HMAC_SHA256(raw_body, webhook_secret)} in {@code X-Razorpay-Signature}.</li>
 * </ul>
 */
@Slf4j
@Component
public class RazorpayGateway implements PaymentGateway {

    private final AppProperties.Razorpay config;
    private final RestClient client;

    public RazorpayGateway(AppProperties props, RestClient.Builder builder) {
        this.config = props.getBilling().getRazorpay();
        this.client = builder.baseUrl(config.getApiBaseUrl()).build();
    }

    @Override
    public boolean isConfigured() {
        return config.isConfigured();
    }

    @Override
    public String publicKey() {
        return config.getKeyId();
    }

    @Override
    public String createOrder(long amountMinor, String currency, String receipt, Map<String, String> notes) {
        if (!isConfigured()) {
            throw new ServiceUnavailableException("Online payments are not available yet. Please try again later.");
        }
        try {
            JsonNode body = client.post()
                    .uri("/orders")
                    .headers(h -> h.setBasicAuth(config.getKeyId(), config.getKeySecret()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amountMinor, "currency", currency, "receipt", receipt, "notes", notes))
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.hasNonNull("id")) {
                throw new ServiceUnavailableException("Payment provider returned an invalid response");
            }
            return body.get("id").asText();
        } catch (RestClientException e) {
            log.error("Razorpay order creation failed", e);
            throw new ServiceUnavailableException("Could not start the payment. Please try again in a moment.");
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        return isConfigured() && constantTimeEquals(hmacHex(config.getKeySecret(), orderId + "|" + paymentId), signature);
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        String secret = config.getWebhookSecret();
        return secret != null && !secret.isBlank() && signature != null
                && constantTimeEquals(hmacHex(secret, rawBody), signature);
    }

    static String hmacHex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
