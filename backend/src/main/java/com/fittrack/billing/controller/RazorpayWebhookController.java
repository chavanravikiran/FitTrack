package com.fittrack.billing.controller;

import com.fittrack.billing.service.CheckoutService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configure in the Razorpay dashboard: URL {@code https://<your-domain>/api/v1/webhooks/razorpay},
 * events {@code payment.captured}, {@code order.paid}, {@code payment.failed}, and the same secret as
 * RAZORPAY_WEBHOOK_SECRET. The raw body is needed for signature verification.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final CheckoutService checkoutService;

    @PostMapping("/api/v1/webhooks/razorpay")
    public ResponseEntity<Void> handle(@RequestBody String rawBody,
                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        return checkoutService.handleWebhook(rawBody, signature)
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }
}
