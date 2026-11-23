package com.fittrack.workout.repository;

import com.fittrack.workout.entity.WorkoutSession;
import com.fittrack.workout.entity.WorkoutSession.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    Optional<WorkoutSession> findByIdAndUserId(Long id, Long userId);

    List<WorkoutSession> findByUserIdAndScheduledDateBetween(Long userId, LocalDate from, LocalDate to);

    Optional<WorkoutSession> findByUserIdAndPlanDayIdAndScheduledDate(Long userId, Long planDayId, LocalDate date);

    Page<WorkoutSession> findByUserIdAndStatusInOrderByScheduledDateDesc(Long userId, Collection<Status> statuses, Pageable pageable);

    long countByUserIdAndStatus(Long userId, Status status);

    /** Most recent completed session that included the given exercise (for progression tips). */
    @Query("""
            select distinct s from WorkoutSession s join s.sets x
            where s.user.id = :userId and s.status = com.fittrack.workout.entity.WorkoutSession.Status.COMPLETED
              and x.exercise.id = :exerciseId
            order by s.scheduledDate desc
            """)
    List<WorkoutSession> findCompletedWithExercise(@Param("userId") Long userId, @Param("exerciseId") Long exerciseId,
                                                   Pageable pageable);

    @Query("""
            select avg(s.perceivedEffort) from WorkoutSession s
            where s.user.id = :userId and s.status = com.fittrack.workout.entity.WorkoutSession.Status.COMPLETED
              and s.scheduledDate between :from and :to and s.perceivedEffort is not null
            """)
    Double averageEffort(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
