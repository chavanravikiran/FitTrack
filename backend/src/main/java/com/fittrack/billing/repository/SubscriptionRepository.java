package com.fittrack.billing.repository;

import com.fittrack.billing.entity.BillingEnums.SubscriptionStatus;
import com.fittrack.billing.entity.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByUserIdAndStatusIn(Long userId, Collection<SubscriptionStatus> statuses);

    /** Locks the live row so concurrent upgrades/expiry for the same user serialise. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.user.id = :userId and s.status in :statuses")
    Optional<Subscription> lockLive(@Param("userId") Long userId, @Param("statuses") Collection<SubscriptionStatus> statuses);

    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select s.id from Subscription s
            where s.status in :statuses and s.currentPeriodEnd is not null and s.currentPeriodEnd <= :now
            """)
    List<Long> findDueForExpiry(@Param("statuses") Collection<SubscriptionStatus> statuses, @Param("now") Instant now);

    @Query("select count(s) from Subscription s where s.status in :statuses and s.plan.free = false")
    long countLivePaidPlans(@Param("statuses") Collection<SubscriptionStatus> statuses);

    @Query("select count(s) from Subscription s where s.status = :status")
    long countByStatus(@Param("status") SubscriptionStatus status);

    @Query("""
            select s from Subscription s
            where s.status = com.fittrack.billing.entity.BillingEnums.SubscriptionStatus.ACTIVE
              and s.plan.free = false and s.billingCycle is not null
            """)
    List<Subscription> findActivePaidWithCycle();
}
