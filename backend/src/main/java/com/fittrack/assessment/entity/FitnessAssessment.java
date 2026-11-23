package com.fittrack.assessment.entity;

import com.fittrack.assessment.entity.AssessmentEnums.ActivityLevel;
import com.fittrack.assessment.entity.AssessmentEnums.DietPreference;
import com.fittrack.assessment.entity.AssessmentEnums.FitnessLevel;
import com.fittrack.assessment.entity.AssessmentEnums.PrimaryGoal;
import com.fittrack.assessment.entity.AssessmentEnums.WorkoutLocation;
import com.fittrack.common.entity.BaseEntity;
import com.fittrack.user.entity.User;
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

/**
 * Versioned snapshot of a user's fitness situation. Updating the assessment inserts a new row and
 * flips {@code isCurrent} on the previous one, so past recommendations remain explainable.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fitness_assessments")
public class FitnessAssessment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level", nullable = false, length = 20)
    private FitnessLevel fitnessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_goal", nullable = false, length = 20)
    private PrimaryGoal primaryGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "workout_location", nullable = false, length = 10)
    private WorkoutLocation workoutLocation;

    /** Bitmask of available weekdays, see {@link com.fittrack.common.util.DayMask}. */
    @Column(name = "available_days", nullable = false)
    private int availableDays;

    @Column(name = "session_minutes", nullable = false)
    private int sessionMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_preference", nullable = false, length = 20)
    private DietPreference dietPreference;

    @Column(length = 500)
    private String allergies;

    @Column(name = "dietary_restrictions", length = 500)
    private String dietaryRestrictions;

    @Column(name = "injury_notes", length = 1000)
    private String injuryNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 20)
    private ActivityLevel activityLevel;

    @Column(name = "sleep_hours", nullable = false, precision = 3, scale = 1)
    private BigDecimal sleepHours;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "assessment_equipment",
            joinColumns = @JoinColumn(name = "assessment_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "assessment_limitations", joinColumns = @JoinColumn(name = "assessment_id"))
    private Set<Limitation> limitations = new HashSet<>();
}
