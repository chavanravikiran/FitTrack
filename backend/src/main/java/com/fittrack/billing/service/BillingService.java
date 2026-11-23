package com.fittrack.billing.service;

import com.fittrack.billing.dto.BillingDtos.BillingConfigDto;
import com.fittrack.billing.dto.BillingDtos.PaymentDto;
import com.fittrack.billing.dto.BillingDtos.PlanDto;
import com.fittrack.billing.dto.BillingDtos.SubscriptionDto;
import com.fittrack.billing.gateway.PaymentGateway;
import com.fittrack.billing.mapper.BillingMapper;
import com.fittrack.billing.repository.PaymentRepository;
import com.fittrack.billing.repository.SubscriptionPlanRepository;
import com.fittrack.common.api.PageResponse;
import com.fittrack.config.AppProperties;
import com.fittrack.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Read-side billing queries for the signed-in user and the public pricing page. */
@Service
@RequiredArgsConstructor
public class BillingService {

    private final SubscriptionPlanRepository planRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentGateway gateway;
    private final BillingMapper mapper;
    private final CurrentUserService currentUser;
    private final AppProperties props;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PlanDto> publicPlans() {
        return planRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(mapper::toDto).toList();
    }

    public BillingConfigDto config() {
        return new BillingConfigDto(gateway.isConfigured(), props.getBilling().getTrialDays(), "RAZORPAY");
    }

    @Transactional
    public SubscriptionDto mySubscription() {
        return mapper.toDto(subscriptionService.current(currentUser.userId()), clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentDto> myPayments(int page, int size) {
        return PageResponse.of(paymentRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.userId(), PageRequest.of(page, size)), mapper::toDto);
    }
}
