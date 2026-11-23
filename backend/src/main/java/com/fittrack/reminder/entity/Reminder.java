package com.fittrack.reminder.entity;

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

import java.time.Instant;
import java.time.LocalTime;

/**
 * A recurring reminder defined in the user's local time and time zone; {@code nextFireAt} is the next
 * occurrence converted to UTC (DST-aware) so the scheduler only compares instants.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reminders")
public class Reminder extends BaseEntity {

    public enum Type { WAKE_UP, WATER, WORKOUT, WALK, MEAL, WEIGHT, SLEEP }

    public enum Channel { PUSH, IN_APP, EMAIL }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Type type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String message;

    @Column(name = "local_time", nullable = false)
    private LocalTime localTime;

    /** Bitmask of weekdays (bit 0 = Monday). */
    @Column(name = "repeat_days", nullable = false)
    private int repeatDays;

    /** Optional repeat within the day (e.g. water every 90 minutes) until {@code endTime}. */
    @Column(name = "interval_minutes")
    private Integer intervalMinutes;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel = Channel.IN_APP;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @Column(name = "last_fired_at")
    private Instant lastFiredAt;
}
