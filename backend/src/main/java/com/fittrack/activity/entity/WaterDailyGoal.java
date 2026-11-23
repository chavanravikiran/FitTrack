package com.fittrack.activity.entity;

import com.fittrack.common.entity.BaseEntity;
import com.fittrack.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Per-day override of the water goal (e.g. hot weather, long workout). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "water_daily_goals")
public class WaterDailyGoal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "goal_ml", nullable = false)
    private int goalMl;

    @Column(name = "adjustment_reason", length = 255)
    private String adjustmentReason;
}
