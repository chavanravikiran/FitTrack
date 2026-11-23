package com.fittrack.common.util;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Converts between a set of weekdays and the compact bitmask stored in the database
 * (bit 0 = Monday ... bit 6 = Sunday).
 */
public final class DayMask {

    public static final int ALL_DAYS = 0b1111111;

    private DayMask() {
    }

    public static int toMask(Collection<DayOfWeek> days) {
        int mask = 0;
        if (days != null) {
            for (DayOfWeek d : days) {
                mask |= 1 << (d.getValue() - 1);
            }
        }
        return mask;
    }

    public static Set<DayOfWeek> fromMask(int mask) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            if ((mask & (1 << (d.getValue() - 1))) != 0) {
                days.add(d);
            }
        }
        return days;
    }

    public static boolean contains(int mask, DayOfWeek day) {
        return (mask & (1 << (day.getValue() - 1))) != 0;
    }
}
