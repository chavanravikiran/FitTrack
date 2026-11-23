package com.fittrack.billing.repository;

import com.fittrack.billing.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByCode(String code);

    Optional<SubscriptionPlan> findByFreeTrue();

    List<SubscriptionPlan> findAllByOrderBySortOrderAsc();

    List<SubscriptionPlan> findByActiveTrueOrderBySortOrderAsc();
}
