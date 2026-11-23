package com.fittrack.common.util;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DayMaskTest {

    @Test
    void roundTripsWeekdays() {
        var days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY);
        int mask = DayMask.toMask(days);

        assertThat(mask).isEqualTo(0b1000101);
        assertThat(DayMask.fromMask(mask)).containsExactlyInAnyOrderElementsOf(days);
    }

    @Test
    void allDaysMaskIs127() {
        assertThat(DayMask.toMask(List.of(DayOfWeek.values()))).isEqualTo(DayMask.ALL_DAYS);
        assertThat(DayMask.contains(DayMask.ALL_DAYS, DayOfWeek.SATURDAY)).isTrue();
        assertThat(DayMask.contains(0b0000001, DayOfWeek.TUESDAY)).isFalse();
    }
}
