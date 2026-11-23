package com.fittrack.reminder.service;

import com.fittrack.common.util.DayMask;
import com.fittrack.reminder.entity.Reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Pure scheduling maths: next occurrence of a reminder after a given instant, in the reminder's time zone. */
public final class ReminderSchedule {

    private static final LocalTime DEFAULT_INTERVAL_END = LocalTime.of(21, 0);

    private ReminderSchedule() {
    }

    public static Instant next(Reminder r, Instant after) {
        return next(r.getLocalTime(), r.getRepeatDays(), r.getIntervalMinutes(), r.getEndTime(), ZoneId.of(r.getTimeZone()), after);
    }

    public static Instant next(LocalTime time, int repeatDays, Integer intervalMinutes, LocalTime endTime, ZoneId zone,
                               Instant after) {
        if (repeatDays <= 0) return null;
        LocalDate start = after.atZone(zone).toLocalDate();
        for (int offset = 0; offset <= 7; offset++) {
            LocalDate date = start.plusDays(offset);
            if (!DayMask.contains(repeatDays, date.getDayOfWeek())) continue;
            for (LocalTime t : timesOfDay(time, intervalMinutes, endTime)) {
                // ZonedDateTime.of shifts non-existent DST-gap times forward, so reminders still fire.
                Instant candidate = ZonedDateTime.of(date, t, zone).toInstant();
                if (candidate.isAfter(after)) return candidate;
            }
        }
        return null;
    }

    static List<LocalTime> timesOfDay(LocalTime first, Integer intervalMinutes, LocalTime endTime) {
        List<LocalTime> times = new ArrayList<>();
        times.add(first);
        if (intervalMinutes != null && intervalMinutes > 0) {
            LocalTime end = endTime == null ? DEFAULT_INTERVAL_END : endTime;
            LocalTime t = first.plusMinutes(intervalMinutes);
            while (!t.isAfter(end) && t.isAfter(first)) { // isAfter(first) stops at midnight wrap-around
                times.add(t);
                t = t.plusMinutes(intervalMinutes);
            }
        }
        return times;
    }
}
