package com.financemanager.app.models;

import java.time.LocalDate;

/** How often a recurring transaction repeats. */
public enum RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public static RecurrenceFrequency fromKey(String key) {
        if (key == null) {
            return MONTHLY;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            return MONTHLY;
        }
    }

    /**
     * Advances a date by one recurrence step.
     * Monthly/yearly steps clamp to the last valid day of the target month so a
     * payment on the 31st never skips a month.
     */
    public LocalDate advance(LocalDate date) {
        switch (this) {
            case DAILY:
                return date.plusDays(1);
            case WEEKLY:
                return date.plusWeeks(1);
            case YEARLY:
                return date.plusYears(1);
            case MONTHLY:
            default:
                return date.plusMonths(1);
        }
    }
}
