package com.fittrack.workout.repository;

import com.fittrack.workout.entity.WorkoutPlan;
import com.fittrack.workout.entity.WorkoutPlan.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    @EntityGraph(attributePaths = {"days"})
    Optional<WorkoutPlan> findByUserIdAndStatus(Long userId, Status status);
}
