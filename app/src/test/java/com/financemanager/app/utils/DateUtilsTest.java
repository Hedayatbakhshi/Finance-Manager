package com.financemanager.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

/** Report ranges are inclusive whole local days. */
public class DateUtilsTest {

    @Test
    public void dayBoundsCoverTheWholeDay() {
        LocalDate date = LocalDate.of(2026, 11, 24);
        long start = DateUtils.startOfDay(date);
        long end = DateUtils.endOfDay(date);
        assertEquals(date, DateUtils.toLocalDate(start));
        assertEquals(date, DateUtils.toLocalDate(end));
        assertEquals(86_400_000L - 1, end - start);
    }

    @Test
    public void monthBoundsUseTheRealLengthOfTheMonth() {
        long febStart = DateUtils.startOfMonth(LocalDate.of(2026, 2, 10));
        long febEnd = DateUtils.endOfMonth(LocalDate.of(2026, 2, 10));
        assertTrue(febEnd > febStart);
        assertEquals(LocalDate.of(2026, 2, 28), DateUtils.toLocalDate(febEnd));

        long leapEnd = DateUtils.endOfMonth(LocalDate.of(2024, 2, 1));
        assertEquals(LocalDate.of(2024, 2, 29), DateUtils.toLocalDate(leapEnd));
    }

    @Test
    public void rangeReturnsInclusiveMonthBounds() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        long[] range = DateUtils.range(date.withDayOfMonth(1), date.withDayOfMonth(30));
        assertEquals(30, DateUtils.toLocalDate(range[1]).getDayOfMonth());
    }

    @Test
    public void formatsMonthKeys() {
        assertEquals("Nov 2026", DateUtils.formatMonthKey("2026-11"));
        assertEquals("Nov", DateUtils.shortMonthKey("2026-11"));
        assertEquals("", DateUtils.formatMonthKey(null));
        assertEquals("bad", DateUtils.shortMonthKey("bad"));
    }

    @Test
    public void sameDayIgnoresTimeOfDay() {
        long morning = DateUtils.startOfDay(LocalDate.of(2026, 5, 1)) + 60_000;
        long evening = DateUtils.startOfDay(LocalDate.of(2026, 5, 1)) + 80_000_000;
        assertTrue(DateUtils.isSameDay(morning, evening));
    }

    @Test
    public void monthShiftClampsTheDay() {
        long jan31 = DateUtils.startOfDay(LocalDate.of(2026, 1, 31));
        LocalDate shifted = DateUtils.toLocalDate(DateUtils.plusMonthsClamped(jan31, 1));
        assertEquals(LocalDate.of(2026, 2, 28), shifted);
    }
}
