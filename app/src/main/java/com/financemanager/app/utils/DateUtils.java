package com.financemanager.app.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.financemanager.app.R;
import com.financemanager.app.models.ReportPeriod;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Date helpers. Dates are stored as local-midnight epoch milliseconds. */
public final class DateUtils {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private DateUtils() {
    }

    public static long startOfDay(@NonNull LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    public static long endOfDay(@NonNull LocalDate date) {
        return startOfDay(date.plusDays(1)) - 1;
    }

    public static long todayStart() {
        return startOfDay(LocalDate.now());
    }

    @NonNull
    public static LocalDate toLocalDate(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate();
    }

    public static long startOfMonth(@NonNull LocalDate date) {
        return startOfDay(date.withDayOfMonth(1));
    }

    public static long endOfMonth(@NonNull LocalDate date) {
        return endOfDay(date.withDayOfMonth(date.lengthOfMonth()));
    }

    /** Inclusive millisecond bounds for a report preset. */
    public static long[] range(@NonNull ReportPeriod period, @NonNull LocalDate today) {
        LocalDate start = period.startOf(today);
        LocalDate end = period.endOf(today);
        return new long[]{startOfDay(start), endOfDay(end)};
    }

    public static long[] range(@NonNull LocalDate start, @NonNull LocalDate end) {
        return new long[]{startOfDay(start), endOfDay(end)};
    }

    public static String formatDay(long millis) {
        return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                .format(toLocalDate(millis));
    }

    public static String formatShortDay(long millis) {
        return DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                .format(toLocalDate(millis));
    }

    public static String formatIsoDate(long millis) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(toLocalDate(millis));
    }

    public static String formatWeekday(long millis) {
        return DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                .format(toLocalDate(millis));
    }

    public static String formatMonth(long millis) {
        return DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
                .format(toLocalDate(millis));
    }

    /** Formats a yyyy-MM month key into "Nov 2026". */
    public static String formatMonthKey(@Nullable String monthKey) {
        if (monthKey == null) {
            return "";
        }
        try {
            YearMonth yearMonth = YearMonth.parse(monthKey);
            return DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
                    .format(yearMonth.atDay(1));
        } catch (Exception e) {
            return monthKey;
        }
    }

    /** Short label for a yyyy-MM key: "Nov". */
    public static String shortMonthKey(@Nullable String monthKey) {
        if (monthKey == null) {
            return "";
        }
        try {
            YearMonth yearMonth = YearMonth.parse(monthKey);
            return DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
                    .format(yearMonth.atDay(1));
        } catch (Exception e) {
            return monthKey;
        }
    }

    /** Localized "Today"/"Yesterday", otherwise a short date. */
    public static String friendlyDate(Context context, long millis) {
        LocalDate date = toLocalDate(millis);
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return context.getString(R.string.today_label);
        }
        if (date.equals(today.minusDays(1))) {
            return context.getString(R.string.yesterday_label);
        }
        if (date.getYear() == today.getYear()) {
            return formatShortDay(millis);
        }
        return formatDay(millis);
    }

    public static String formatTimestamp(long millis) {
        return DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
                .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE));
    }

    public static boolean isSameDay(long firstMillis, long secondMillis) {
        return toLocalDate(firstMillis).equals(toLocalDate(secondMillis));
    }

    /** Moves a date forward one month, clamping to the end of the target month. */
    public static long plusMonthsClamped(long millis, int months) {
        LocalDate date = toLocalDate(millis);
        return startOfDay(date.plusMonths(months));
    }
}
