package com.fittrack.assessment.entity;

import com.fittrack.common.entity.BaseEntity;
import com.fittrack.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fitness_goals")
public class FitnessGoal extends BaseEntity {

    public enum GoalType {
        TARGET_WEIGHT("kg"), WEEKLY_WORKOUTS("sessions"), DAILY_STEPS("steps"),
        DAILY_WATER("ml"), SLEEP_HOURS("hours"), BODY_FAT("%");

        private final String unit;

        GoalType(String unit) {
            this.unit = unit;
        }

        public String unit() {
            return unit;
        }
    }

    public enum GoalStatus { ACTIVE, ACHIEVED, ABANDONED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private GoalType goalType;

    @Column(length = 120)
    private String title;

    @Column(name = "start_value", precision = 10, scale = 2)
    private BigDecimal startValue;

    @Column(name = "target_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetValue;

    @Column(nullable = false, length = 15)
    private String unit;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "achieved_at")
    private Instant achievedAt;
}
