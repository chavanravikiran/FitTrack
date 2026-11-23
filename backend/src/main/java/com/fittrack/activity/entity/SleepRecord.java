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

import java.time.Instant;
import java.time.LocalDate;

/** One sleep per wake-up date. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sleep_records")
public class SleepRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sleep_date", nullable = false)
    private LocalDate sleepDate;

    @Column(name = "sleep_start", nullable = false)
    private Instant sleepStart;

    @Column(name = "wake_time", nullable = false)
    private Instant wakeTime;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    private Integer quality;

    @Column(name = "is_rest_day", nullable = false)
    private boolean restDay;

    @Column(length = 255)
    private String notes;
}
