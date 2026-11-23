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

/** An exercise prescribed on a plan day: sets, reps or duration, rest and target effort. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_exercises")
public class WorkoutExercise extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_day_id", nullable = false)
    private WorkoutPlanDay planDay;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false)
    private int sets;

    @Column(name = "reps_min")
    private Integer repsMin;

    @Column(name = "reps_max")
    private Integer repsMax;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "rest_sec", nullable = false)
    private int restSec;

    @Column(name = "target_rpe")
    private Integer targetRpe;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Why this exercise was chosen (shown to the user). */
    @Column(length = 255)
    private String notes;
}
