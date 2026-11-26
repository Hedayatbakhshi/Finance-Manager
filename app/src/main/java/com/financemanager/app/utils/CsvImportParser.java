package com.financemanager.app.utils;

import com.financemanager.app.models.TransactionType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses CSV text and the individual values used by {@link CsvImporter}.
 *
 * <p>This class has no Android dependencies so the quoting rules, type labels,
 * amounts and dates can be unit-tested on the JVM. It accepts the header written by
 * {@link CsvExporter}, including quoted fields with commas, escaped quotes and
 * embedded newlines.</p>
 */
public final class CsvImportParser {

    private static final DateTimeFormatter SLASH_DATE =
            DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT);
    private static final DateTimeFormatter DAY_FIRST_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    private CsvImportParser() {
    }

    /** Splits CSV text into rows. Blank trailing lines are ignored. */
    public static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return rows;
        }
        if (text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else if (c == '\r') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    field.append('\n');
                } else {
                    field.append(c);
                }
                continue;
            }
            if (c == '"' && field.length() == 0) {
                quoted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    /** Returns the header index for a normalized name, or -1. */
    public static int indexOf(List<String> header, String name) {
        if (header == null) {
            return -1;
        }
        for (int i = 0; i < header.size(); i++) {
            if (normalize(header.get(i)).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Maps exported labels and enum keys to a type; null when unrecognized. */
    public static TransactionType parseType(String raw) {
        String value = normalize(raw);
        switch (value) {
            case "income":
            case "in":
                return TransactionType.INCOME;
            case "expense":
            case "out":
                return TransactionType.EXPENSE;
            case "transfer":
                return TransactionType.TRANSFER;
            default:
                return null;
        }
    }

    /** Parses an amount in major units; returns null for zero, negative or bad input. */
    public static Long parseAmount(String raw) {
        Long amount = Money.parse(raw);
        return amount == null || amount <= 0 ? null : amount;
    }

    /**
     * Parses ISO (yyyy-MM-dd), yyyy/MM/dd or dd/MM/yyyy dates into local midnight.
     * Returns 0 when the value cannot be understood.
     */
    public static long parseDate(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return 0;
        }
        LocalDate date = null;
        try {
            date = LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // Try the alternate formats below.
        }
        if (date == null) {
            try {
                date = LocalDate.parse(value, SLASH_DATE);
            } catch (DateTimeParseException ignored) {
                // Try day-first next.
            }
        }
        if (date == null) {
            try {
                date = LocalDate.parse(value, DAY_FIRST_DATE);
            } catch (DateTimeParseException ignored) {
                return 0;
            }
        }
        return DateUtils.startOfDay(date);
    }
}
