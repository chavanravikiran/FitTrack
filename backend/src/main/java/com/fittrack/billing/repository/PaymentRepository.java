package com.fittrack.billing.repository;

import com.fittrack.billing.entity.BillingEnums.PaymentProvider;
import com.fittrack.billing.entity.BillingEnums.PaymentStatus;
import com.fittrack.billing.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.provider = :provider and p.providerOrderId = :orderId")
    Optional<Payment> lockByOrder(@Param("provider") PaymentProvider provider, @Param("orderId") String orderId);

    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    @Query("select coalesce(sum(p.amountMinor), 0) from Payment p where p.status = :status and p.paidAt >= :since")
    long sumAmount(@Param("status") PaymentStatus status, @Param("since") Instant since);
}
