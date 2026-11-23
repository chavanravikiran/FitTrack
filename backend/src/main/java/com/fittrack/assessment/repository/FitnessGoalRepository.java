package com.fittrack.assessment.repository;

import com.fittrack.assessment.entity.FitnessGoal;
import com.fittrack.assessment.entity.FitnessGoal.GoalStatus;
import com.fittrack.assessment.entity.FitnessGoal.GoalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FitnessGoalRepository extends JpaRepository<FitnessGoal, Long> {

    List<FitnessGoal> findByUserIdOrderByStatusAscCreatedAtDesc(Long userId);

    List<FitnessGoal> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, GoalStatus status);

    Optional<FitnessGoal> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, GoalStatus status);

    Optional<FitnessGoal> findByUserIdAndGoalTypeAndStatus(Long userId, GoalType goalType, GoalStatus status);
}
