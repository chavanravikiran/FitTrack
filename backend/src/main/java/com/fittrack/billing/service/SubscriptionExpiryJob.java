package com.fittrack.billing.service;

import com.fittrack.billing.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/** Every 10 minutes: moves users whose trial or paid period has ended back to the free plan. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpiryJob {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void expireDueSubscriptions() {
        List<Long> due = subscriptionRepository.findDueForExpiry(SubscriptionService.LIVE, clock.instant());
        for (Long id : due) {
            try {
                subscriptionService.expireIfDue(id);
            } catch (RuntimeException e) {
                log.error("Failed to expire subscription {}", id, e);
            }
        }
    }
}
