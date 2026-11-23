package com.fittrack.exercise.entity;

import com.fittrack.assessment.entity.AssessmentEnums.BodyArea;
import com.fittrack.assessment.entity.Equipment;
import com.fittrack.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exercises")
public class Exercise extends BaseEntity {

    public enum Difficulty {
        BEGINNER, INTERMEDIATE, ADVANCED;

        public int rank() {
            return ordinal();
        }
    }

    public enum Location { HOME, GYM, BOTH }

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "primary_muscle_group_id", nullable = false)
    private MuscleGroup primaryMuscleGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Location location = Location.BOTH;

    @Column(name = "default_sets")
    private Integer defaultSets;

    @Column(name = "default_reps")
    private Integer defaultReps;

    @Column(name = "default_duration_sec")
    private Integer defaultDurationSec;

    @Column(name = "default_rest_sec")
    private Integer defaultRestSec;

    @Column(columnDefinition = "text")
    private String instructions;

    @Column(name = "common_mistakes", columnDefinition = "text")
    private String commonMistakes;

    @Column(name = "safety_precautions", columnDefinition = "text")
    private String safetyPrecautions;

    @Column(name = "met_value", precision = 4, scale = 1)
    private BigDecimal metValue;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "exercise_secondary_muscles",
            joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "muscle_group_id"))
    private Set<MuscleGroup> secondaryMuscles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "exercise_equipment",
            joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exercise_contraindications", joinColumns = @JoinColumn(name = "exercise_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "body_area", length = 20)
    private Set<BodyArea> contraindications = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "exercise_alternatives",
            joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "alternative_exercise_id"))
    private Set<Exercise> alternatives = new HashSet<>();

    public boolean isDurationBased() {
        return defaultReps == null && defaultDurationSec != null;
    }

    public double met() {
        return metValue == null ? 3.5 : metValue.doubleValue();
    }
}
