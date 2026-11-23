package com.fittrack.workout.entity;

import com.fittrack.common.entity.BaseEntity;
import com.fittrack.exercise.entity.Exercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One logged set of an exercise within a session. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_session_exercises")
public class WorkoutSessionExercise extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "reps_done")
    private Integer repsDone;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(nullable = false)
    private boolean completed;
}
