package com.fittrack.billing.gateway;

import com.fittrack.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpayGatewayTest {

    private RazorpayGateway gateway;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getBilling().getRazorpay().setKeyId("rzp_test_key");
        props.getBilling().getRazorpay().setKeySecret("test_secret");
        props.getBilling().getRazorpay().setWebhookSecret("hook_secret");
        gateway = new RazorpayGateway(props, RestClient.builder());
    }

    @Test
    void hmacMatchesKnownVector() {
        // RFC 4231-style sanity check computed independently: HMAC-SHA256("key", "The quick brown fox jumps over the lazy dog")
        assertThat(RazorpayGateway.hmacHex("key", "The quick brown fox jumps over the lazy dog"))
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void acceptsCorrectPaymentSignatureAndRejectsTampering() {
        String sig = RazorpayGateway.hmacHex("test_secret", "order_123|pay_456");

        assertThat(gateway.verifyPaymentSignature("order_123", "pay_456", sig)).isTrue();
        assertThat(gateway.verifyPaymentSignature("order_123", "pay_999", sig)).isFalse();
        assertThat(gateway.verifyPaymentSignature("order_123", "pay_456", "deadbeef")).isFalse();
        assertThat(gateway.verifyPaymentSignature("order_123", "pay_456", null)).isFalse();
    }

    @Test
    void verifiesWebhookAgainstRawBody() {
        String body = "{\"event\":\"payment.captured\"}";
        String sig = RazorpayGateway.hmacHex("hook_secret", body);

        assertThat(gateway.verifyWebhookSignature(body, sig)).isTrue();
        assertThat(gateway.verifyWebhookSignature(body + " ", sig)).isFalse();
    }

    @Test
    void unconfiguredGatewayRejectsEverything() {
        RazorpayGateway empty = new RazorpayGateway(new AppProperties(), RestClient.builder());
        assertThat(empty.isConfigured()).isFalse();
        assertThat(empty.verifyPaymentSignature("o", "p", RazorpayGateway.hmacHex("any", "o|p"))).isFalse();
        assertThat(empty.verifyWebhookSignature("{}", "x")).isFalse();
    }
}
