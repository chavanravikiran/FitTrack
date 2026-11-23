package com.fittrack.workout.entity;

import com.fittrack.assessment.entity.FitnessAssessment;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_plans")
public class WorkoutPlan extends BaseEntity {

    public enum SplitType { FULL_BODY, UPPER_LOWER, PUSH_PULL_LEGS, CUSTOM }

    public enum Status { ACTIVE, ARCHIVED }

    public enum GeneratedBy { ENGINE, USER }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private FitnessAssessment assessment;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 10)
    private GeneratedBy generatedBy = GeneratedBy.ENGINE;

    /** Plain-language explanations of how the plan was built, one per line. */
    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC")
    private List<WorkoutPlanDay> days = new ArrayList<>();

    public void addDay(WorkoutPlanDay day) {
        day.setPlan(this);
        days.add(day);
    }

    public Optional<WorkoutPlanDay> dayFor(int isoDayOfWeek) {
        return days.stream().filter(d -> d.getDayOfWeek() == isoDayOfWeek).findFirst();
    }
}
