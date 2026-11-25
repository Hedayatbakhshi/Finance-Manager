package com.financemanager.app.models;

import java.time.LocalDate;

/** Date presets used by reports and filters. */
public enum ReportPeriod {
    ALL_TIME,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    THIS_YEAR,
    CUSTOM;

    /** First day (inclusive) of this period. */
    public LocalDate startOf(LocalDate today) {
        switch (this) {
            case THIS_WEEK:
                return today.with(java.time.DayOfWeek.MONDAY);
            case THIS_MONTH:
                return today.withDayOfMonth(1);
            case LAST_MONTH:
                return today.minusMonths(1).withDayOfMonth(1);
            case LAST_3_MONTHS:
                return today.minusMonths(2).withDayOfMonth(1);
            case THIS_YEAR:
                return today.withDayOfYear(1);
            case ALL_TIME:
                return LocalDate.of(1970, 1, 1);
            case CUSTOM:
            default:
                return today;
        }
    }

    /** Last day (inclusive) of this period. */
    public LocalDate endOf(LocalDate today) {
        switch (this) {
            case THIS_WEEK:
                return today.with(java.time.DayOfWeek.MONDAY).plusDays(6);
            case THIS_MONTH:
                return today.withDayOfMonth(today.lengthOfMonth());
            case LAST_MONTH:
                LocalDate lastMonth = today.minusMonths(1);
                return lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
            case LAST_3_MONTHS:
                return today.withDayOfMonth(today.lengthOfMonth());
            case THIS_YEAR:
                return today.withDayOfYear(today.lengthOfYear());
            default:
                return today;
        }
    }
}
