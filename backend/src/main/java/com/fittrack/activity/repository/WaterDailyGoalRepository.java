package com.fittrack.activity.repository;

import com.fittrack.activity.entity.WaterDailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaterDailyGoalRepository extends JpaRepository<WaterDailyGoal, Long> {

    Optional<WaterDailyGoal> findByUserIdAndRecordDate(Long userId, LocalDate date);

    List<WaterDailyGoal> findByUserIdAndRecordDateBetween(Long userId, LocalDate from, LocalDate to);
}
