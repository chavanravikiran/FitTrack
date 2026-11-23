package com.fittrack.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittrack.billing.dto.BillingDtos.CheckoutRequest;
import com.fittrack.billing.dto.BillingDtos.CheckoutResponse;
import com.fittrack.billing.dto.BillingDtos.SubscriptionDto;
import com.fittrack.billing.dto.BillingDtos.VerifyPaymentRequest;
import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import com.fittrack.billing.entity.BillingEnums.PaymentProvider;
import com.fittrack.billing.entity.BillingEnums.PaymentStatus;
import com.fittrack.billing.entity.Payment;
import com.fittrack.billing.entity.Subscription;
import com.fittrack.billing.entity.SubscriptionPlan;
import com.fittrack.billing.gateway.PaymentGateway;
import com.fittrack.billing.mapper.BillingMapper;
import com.fittrack.billing.repository.PaymentRepository;
import com.fittrack.billing.repository.SubscriptionPlanRepository;
import com.fittrack.common.exception.BadRequestException;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.common.exception.ServiceUnavailableException;
import com.fittrack.config.AppProperties;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/**
 * Checkout flow:
 * <ol>
 *   <li>{@link #createCheckout} - server prices the plan and creates a provider order + CREATED payment.</li>
 *   <li>Browser completes Razorpay Checkout and posts the signed result to {@link #verify}.</li>
 *   <li>The {@code payment.captured}/{@code order.paid} webhook is a backup if the browser never returns.</li>
 * </ol>
 * Activation is idempotent: only the CREATED -> PAID transition (under a row lock) grants access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final PaymentGateway gateway;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final BillingMapper mapper;
    private final CurrentUserService currentUser;
    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public CheckoutResponse createCheckout(CheckoutRequest request) {
        if (!gateway.isConfigured()) {
            throw new ServiceUnavailableException("Online payments are not available yet. Please contact support to upgrade.");
        }
        SubscriptionPlan plan = planRepository.findByCode(request.planCode())
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not available: " + request.planCode()));
        if (plan.isFree()) {
            throw new BadRequestException("The free plan doesn't need a payment");
        }
        long amount = plan.priceFor(request.billingCycle());
        if (amount <= 0) {
            throw new BusinessRuleException("This plan is not available for the selected billing period");
        }

        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", currentUser.userId()));
        if (!user.isEmailVerified()) {
            throw new BusinessRuleException("Please verify your email address before upgrading, so we can send your receipts.");
        }
        String receipt = "ft-" + user.getId() + "-" + clock.instant().toEpochMilli();
        String orderId = gateway.createOrder(amount, plan.getCurrency(), receipt, Map.of(
                "userId", String.valueOf(user.getId()),
                "plan", plan.getCode(),
                "cycle", request.billingCycle().name()));

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPlan(plan);
        payment.setBillingCycle(request.billingCycle());
        payment.setAmountMinor(amount);
        payment.setCurrency(plan.getCurrency());
        payment.setProvider(PaymentProvider.RAZORPAY);
        payment.setProviderOrderId(orderId);
        paymentRepository.save(payment);

        String cycleLabel = request.billingCycle() == BillingCycle.YEARLY ? "1 year" : "1 month";
        return new CheckoutResponse(PaymentProvider.RAZORPAY.name(), gateway.publicKey(), orderId, amount,
                plan.getCurrency(), props.getBilling().getProductName(), plan.getName() + " - " + cycleLabel,
                user.getProfile() == null ? null : user.getProfile().getFullName(), user.getEmail());
    }

    /** Called by the browser after Checkout succeeds. */
    @Transactional
    public SubscriptionDto verify(VerifyPaymentRequest request) {
        Payment payment = paymentRepository.lockByOrder(PaymentProvider.RAZORPAY, request.orderId())
                .filter(p -> p.getUser().getId().equals(currentUser.userId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!gateway.verifyPaymentSignature(request.orderId(), request.paymentId(), request.signature())) {
            log.warn("Invalid payment signature for order {} (user {})", request.orderId(), currentUser.userId());
            throw new BadRequestException("Payment could not be verified. If money was deducted, it will be confirmed shortly or refunded by your bank.");
        }
        Subscription sub = markPaid(payment, request.paymentId());
        return mapper.toDto(sub, clock.instant());
    }

    /**
     * Razorpay webhook. Returns normally for events we don't handle so Razorpay doesn't retry them.
     *
     * @return true if the signature was valid
     */
    @Transactional
    public boolean handleWebhook(String rawBody, String signature) {
        if (!gateway.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected Razorpay webhook with invalid signature");
            return false;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (IOException e) {
            log.warn("Unparseable Razorpay webhook body");
            return true;
        }
        String event = root.path("event").asText();
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String orderId = paymentEntity.path("order_id").asText(null);
        String paymentId = paymentEntity.path("id").asText(null);

        switch (event) {
            case "payment.captured", "order.paid" -> {
                if (orderId == null) {
                    orderId = root.path("payload").path("order").path("entity").path("id").asText(null);
                }
                if (orderId != null) {
                    final String pid = paymentId;
                    paymentRepository.lockByOrder(PaymentProvider.RAZORPAY, orderId)
                            .ifPresent(p -> markPaid(p, pid));
                }
            }
            case "payment.failed" -> {
                if (orderId != null) {
                    String reason = paymentEntity.path("error_description").asText("Payment failed");
                    paymentRepository.lockByOrder(PaymentProvider.RAZORPAY, orderId)
                            .filter(p -> p.getStatus() == PaymentStatus.CREATED)
                            .ifPresent(p -> {
                                p.setStatus(PaymentStatus.FAILED);
                                p.setFailureReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
                            });
                }
            }
            default -> log.debug("Ignoring Razorpay webhook event {}", event);
        }
        return true;
    }

    /** Idempotent: a payment already PAID returns the current subscription unchanged. */
    private Subscription markPaid(Payment payment, String providerPaymentId) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return Optional.ofNullable(payment.getSubscription())
                    .orElseGet(() -> subscriptionService.current(payment.getUser().getId()));
        }
        Subscription sub = subscriptionService.activatePaid(payment.getUser(), payment.getPlan(), payment.getBillingCycle());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(clock.instant());
        payment.setFailureReason(null);
        if (providerPaymentId != null) {
            payment.setProviderPaymentId(providerPaymentId);
        }
        payment.setSubscription(sub);
        log.info("Payment {} (order {}) captured; user {} now on {} until {}", payment.getId(),
                payment.getProviderOrderId(), payment.getUser().getId(), sub.getPlan().getCode(), sub.getCurrentPeriodEnd());
        return sub;
    }
}
