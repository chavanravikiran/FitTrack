package com.fittrack.billing.service;

import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import com.fittrack.billing.entity.BillingEnums.SubscriptionSource;
import com.fittrack.billing.entity.BillingEnums.SubscriptionStatus;
import com.fittrack.billing.entity.Subscription;
import com.fittrack.billing.entity.SubscriptionPlan;
import com.fittrack.billing.repository.SubscriptionPlanRepository;
import com.fittrack.billing.repository.SubscriptionRepository;
import com.fittrack.config.AppProperties;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Owns the subscription lifecycle. Invariant: every user has exactly one live (ACTIVE/TRIALING)
 * subscription - the free plan when nothing else applies. Changes lock the live row so concurrent
 * payments, admin grants and expiry for the same user serialise.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    public static final Set<SubscriptionStatus> LIVE = EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final AppProperties props;
    private final Clock clock;

    /** Called at registration: Premium trial if enabled, otherwise the free plan. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Subscription startForNewUser(User user) {
        int trialDays = props.getBilling().getTrialDays();
        Optional<SubscriptionPlan> trialPlan = trialDays > 0
                ? planRepository.findByCode(props.getBilling().getTrialPlanCode()).filter(SubscriptionPlan::isActive)
                : Optional.empty();
        Instant now = clock.instant();
        if (trialPlan.isPresent()) {
            return create(user, trialPlan.get(), SubscriptionStatus.TRIALING, SubscriptionSource.TRIAL, null,
                    now, now.plus(Duration.ofDays(trialDays)), trialDays + "-day free trial");
        }
        return create(user, freePlan(), SubscriptionStatus.ACTIVE, SubscriptionSource.FREE, null, now, null, null);
    }

    /** Current live subscription; self-heals by assigning the free plan if none exists. */
    @Transactional
    public Subscription current(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusIn(userId, LIVE)
                .orElseGet(() -> create(userRepository.getReferenceById(userId), freePlan(),
                        SubscriptionStatus.ACTIVE, SubscriptionSource.FREE, null, clock.instant(), null, null));
    }

    /**
     * Applies a successful payment. Paying again for the same plan while it is still running extends it
     * from the current end date, so renewing early never loses days.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Subscription activatePaid(User user, SubscriptionPlan plan, BillingCycle cycle) {
        Instant now = clock.instant();
        Optional<Subscription> live = subscriptionRepository.lockLive(user.getId(), LIVE);

        if (live.isPresent() && isExtendable(live.get(), plan, now)) {
            Subscription s = live.get();
            s.setCurrentPeriodEnd(addCycle(s.getCurrentPeriodEnd(), cycle));
            s.setBillingCycle(cycle);
            s.setSource(SubscriptionSource.PAYMENT);
            s.setStatus(SubscriptionStatus.ACTIVE);
            return s;
        }
        live.ifPresent(s -> end(s, now, SubscriptionStatus.CANCELED));
        return create(user, plan, SubscriptionStatus.ACTIVE, SubscriptionSource.PAYMENT, cycle, now, addCycle(now, cycle), null);
    }

    /** Admin-granted access for a number of days (extends an existing grant/payment of the same plan). */
    @Transactional
    public Subscription grant(Long userId, SubscriptionPlan plan, int days, String note) {
        User user = userRepository.getReferenceById(userId);
        Instant now = clock.instant();
        Optional<Subscription> live = subscriptionRepository.lockLive(userId, LIVE);
        if (live.isPresent() && isExtendable(live.get(), plan, now)) {
            Subscription s = live.get();
            s.setCurrentPeriodEnd(s.getCurrentPeriodEnd().plus(Duration.ofDays(days)));
            s.setStatus(SubscriptionStatus.ACTIVE);
            s.setNotes(note);
            return s;
        }
        live.ifPresent(s -> end(s, now, SubscriptionStatus.CANCELED));
        return create(user, plan, SubscriptionStatus.ACTIVE, SubscriptionSource.ADMIN_GRANT, null,
                now, now.plus(Duration.ofDays(days)), note);
    }

    /** Ends any paid/trial access immediately and moves the user to the free plan. */
    @Transactional
    public Subscription revertToFree(Long userId, String reason) {
        Instant now = clock.instant();
        Optional<Subscription> live = subscriptionRepository.lockLive(userId, LIVE);
        if (live.isPresent() && live.get().getPlan().isFree()) {
            return live.get();
        }
        live.ifPresent(s -> end(s, now, SubscriptionStatus.CANCELED));
        return create(userRepository.getReferenceById(userId), freePlan(), SubscriptionStatus.ACTIVE,
                SubscriptionSource.FREE, null, now, null, reason);
    }

    /** Expires one subscription whose period has ended. Runs in its own transaction per row. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireIfDue(Long subscriptionId) {
        Instant now = clock.instant();
        subscriptionRepository.findById(subscriptionId)
                .filter(s -> s.getStatus().isLive())
                .filter(s -> s.getCurrentPeriodEnd() != null && !s.getCurrentPeriodEnd().isAfter(now))
                .ifPresent(s -> {
                    Long userId = s.getUser().getId();
                    subscriptionRepository.lockLive(userId, LIVE); // serialise with concurrent payments
                    end(s, now, SubscriptionStatus.EXPIRED);
                    create(s.getUser(), freePlan(), SubscriptionStatus.ACTIVE, SubscriptionSource.FREE, null, now, null,
                            "Moved to Free after " + s.getPlan().getName() + " ended");
                    log.info("Subscription {} for user {} expired; moved to free plan", s.getId(), userId);
                });
    }

    public SubscriptionPlan freePlan() {
        return planRepository.findByFreeTrue()
                .orElseThrow(() -> new IllegalStateException("No free plan configured - check migrations"));
    }

    private boolean isExtendable(Subscription s, SubscriptionPlan plan, Instant now) {
        return s.getPlan().getId().equals(plan.getId())
                && !plan.isFree()
                && s.getSource() != SubscriptionSource.TRIAL
                && s.getCurrentPeriodEnd() != null
                && s.getCurrentPeriodEnd().isAfter(now);
    }

    private void end(Subscription s, Instant now, SubscriptionStatus status) {
        s.setStatus(status);
        s.setEndedAt(now);
        subscriptionRepository.saveAndFlush(s); // release the partial unique index before inserting the next row
    }

    private Subscription create(User user, SubscriptionPlan plan, SubscriptionStatus status, SubscriptionSource source,
                                BillingCycle cycle, Instant start, Instant end, String notes) {
        Subscription s = new Subscription();
        s.setUser(user);
        s.setPlan(plan);
        s.setStatus(status);
        s.setSource(source);
        s.setBillingCycle(cycle);
        s.setCurrentPeriodStart(start);
        s.setCurrentPeriodEnd(end);
        s.setNotes(notes);
        return subscriptionRepository.save(s);
    }

    /** Calendar-aware: +1 month / +1 year in UTC. */
    static Instant addCycle(Instant from, BillingCycle cycle) {
        var dt = from.atOffset(ZoneOffset.UTC);
        return (cycle == BillingCycle.YEARLY ? dt.plusYears(1) : dt.plusMonths(1)).toInstant();
    }
}
