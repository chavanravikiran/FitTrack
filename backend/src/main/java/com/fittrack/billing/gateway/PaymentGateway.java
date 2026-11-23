package com.fittrack.billing.gateway;

import java.util.Map;

/** Abstraction over the payment provider so it can be swapped or mocked in tests. */
public interface PaymentGateway {

    boolean isConfigured();

    /** Public key the browser needs to open the provider's checkout. */
    String publicKey();

    /** Creates a provider order for the exact amount and returns the provider's order id. */
    String createOrder(long amountMinor, String currency, String receipt, Map<String, String> notes);

    /** Verifies the signature the checkout returns after a successful payment. */
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    /** Verifies the signature header of a webhook call against its raw body. */
    boolean verifyWebhookSignature(String rawBody, String signature);
}
