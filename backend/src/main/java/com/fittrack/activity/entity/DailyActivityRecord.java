package com.fittrack.activity.entity;

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

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "daily_activity_records")
public class DailyActivityRecord extends BaseEntity {

    /** Where the numbers came from. Steps are never claimed as automatic without a device integration. */
    public enum Source { MANUAL, HEALTH_CONNECT, APPLE_HEALTH, IMPORT }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private int steps;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "active_minutes")
    private Integer activeMinutes;

    @Column(name = "calories_est")
    private Integer caloriesEst;

    @Column(name = "step_goal")
    private Integer stepGoal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source = Source.MANUAL;
}
