package com.fittrack.reminder.service;

import com.fittrack.common.util.DayMask;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderScheduleTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Test
    void nextOccurrenceIsLaterTodayInUsersZone() {
        // 2026-01-05 is a Monday. 08:00 IST == 02:30 UTC.
        Instant now = Instant.parse("2026-01-05T01:00:00Z"); // 06:30 IST
        Instant next = ReminderSchedule.next(LocalTime.of(8, 0), DayMask.ALL_DAYS, null, null, IST, now);
        assertThat(next).isEqualTo(Instant.parse("2026-01-05T02:30:00Z"));
    }

    @Test
    void skipsDaysNotInRepeatMask() {
        Instant mondayEvening = Instant.parse("2026-01-05T15:00:00Z"); // 20:30 IST Monday
        int wednesdayOnly = DayMask.toMask(EnumSet.of(DayOfWeek.WEDNESDAY));
        Instant next = ReminderSchedule.next(LocalTime.of(7, 0), wednesdayOnly, null, null, IST, mondayEvening);
        assertThat(next).isEqualTo(Instant.parse("2026-01-07T01:30:00Z")); // Wed 07:00 IST
    }

    @Test
    void intervalRemindersRepeatUntilEndTime() {
        List<LocalTime> times = ReminderSchedule.timesOfDay(LocalTime.of(9, 0), 120, LocalTime.of(15, 0));
        assertThat(times).containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(13, 0), LocalTime.of(15, 0));

        Instant at1130Ist = Instant.parse("2026-01-05T06:00:00Z");
        assertThat(ReminderSchedule.next(LocalTime.of(9, 0), DayMask.ALL_DAYS, 120, LocalTime.of(15, 0), IST, at1130Ist))
                .isEqualTo(Instant.parse("2026-01-05T07:30:00Z")); // 13:00 IST
    }

    @Test
    void handlesDaylightSavingTransitions() {
        ZoneId ny = ZoneId.of("America/New_York");
        // DST starts 2026-03-08 at 02:00 -> 02:30 does not exist and is shifted to 03:30 EDT (07:30Z)
        Instant before = Instant.parse("2026-03-08T05:00:00Z");
        Instant next = ReminderSchedule.next(LocalTime.of(2, 30), DayMask.ALL_DAYS, null, null, ny, before);
        assertThat(next).isEqualTo(Instant.parse("2026-03-08T07:30:00Z"));
    }

    @Test
    void emptyMaskNeverFires() {
        assertThat(ReminderSchedule.next(LocalTime.NOON, 0, null, null, IST, Instant.now())).isNull();
    }
}
