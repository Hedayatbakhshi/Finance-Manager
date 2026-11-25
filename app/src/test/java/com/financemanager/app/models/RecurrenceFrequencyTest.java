package com.financemanager.app.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;

/** Monthly and yearly schedules must clamp instead of skipping a period. */
public class RecurrenceFrequencyTest {

    @Test
    public void monthlyClampsToShortMonths() {
        assertEquals(LocalDate.of(2026, 2, 28),
                RecurrenceFrequency.MONTHLY.advance(LocalDate.of(2026, 1, 31)));
        assertEquals(LocalDate.of(2024, 2, 29),
                RecurrenceFrequency.MONTHLY.advance(LocalDate.of(2024, 1, 31)));
        assertEquals(LocalDate.of(2026, 4, 30),
                RecurrenceFrequency.MONTHLY.advance(LocalDate.of(2026, 3, 31)));
    }

    @Test
    public void yearlyClampsLeapDay() {
        assertEquals(LocalDate.of(2025, 2, 28),
                RecurrenceFrequency.YEARLY.advance(LocalDate.of(2024, 2, 29)));
        assertEquals(LocalDate.of(2027, 2, 28),
                RecurrenceFrequency.YEARLY.advance(LocalDate.of(2026, 2, 28)));
    }

    @Test
    public void dailyAndWeeklyAddTheRightSpan() {
        assertEquals(LocalDate.of(2026, 11, 25),
                RecurrenceFrequency.DAILY.advance(LocalDate.of(2026, 11, 24)));
        assertEquals(LocalDate.of(2026, 12, 1),
                RecurrenceFrequency.WEEKLY.advance(LocalDate.of(2026, 11, 24)));
    }

    @Test
    public void fromKeyDefaultsToMonthly() {
        assertEquals(RecurrenceFrequency.MONTHLY, RecurrenceFrequency.fromKey(null));
        assertEquals(RecurrenceFrequency.MONTHLY, RecurrenceFrequency.fromKey("nope"));
        assertEquals(RecurrenceFrequency.DAILY, RecurrenceFrequency.fromKey("DAILY"));
    }
}
