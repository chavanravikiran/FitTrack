package com.fittrack.workout.entity;

import com.fittrack.common.entity.BaseEntity;
import com.fittrack.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A planned workout on a date that the user acted on. Days the user hasn't touched are derived from the plan
 * and not stored, so the calendar never fills up with empty rows.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_sessions")
public class WorkoutSession extends BaseEntity {

    public enum Status { SCHEDULED, IN_PROGRESS, COMPLETED, SKIPPED, RESCHEDULED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id")
    private WorkoutPlanDay planDay;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.SCHEDULED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "rescheduled_to")
    private LocalDate rescheduledTo;

    @Column(name = "perceived_effort")
    private Integer perceivedEffort;

    @Column(name = "calories_est")
    private Integer caloriesEst;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber ASC")
    private List<WorkoutSessionExercise> sets = new ArrayList<>();
}
