package com.financemanager.app.utils;

import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

/**
 * All money in Finance Manager is stored as a {@code long} number of minor units
 * (cents). Floating point values are never used, so sums and comparisons stay exact.
 *
 * <p>Every supported currency here uses two decimals, which keeps a single scale
 * across the app. User input is parsed with {@link BigDecimal} and rounded to the
 * nearest cent exactly once, at the boundary.</p>
 */
public final class Money {

    public static final int SCALE = 2;
    private static final long MAX_ABS_MINOR = 1_000_000_000_000_000L; // 10 trillion units

    private Money() {
    }

    /** Formats minor units with the currency symbol, e.g. {@code $12,450.00}. */
    public static String format(long minorUnits, @Nullable String currencyCode) {
        DecimalFormat format = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.getDefault()));
        format.setGroupingUsed(true);
        String number = format.format(BigDecimal.valueOf(Math.abs(minorUnits), SCALE));
        String sign = minorUnits < 0 ? "-" : "";
        return sign + symbol(currencyCode) + number;
    }

    /** Formats with an explicit sign, e.g. {@code +$120.00} / {@code -$45.50}. */
    public static String formatSigned(long minorUnits, @Nullable String currencyCode) {
        if (minorUnits > 0) {
            return "+" + format(minorUnits, currencyCode);
        }
        return format(minorUnits, currencyCode);
    }

    /** Formats without a currency symbol, e.g. {@code 1,234.56}. */
    public static String formatPlain(long minorUnits) {
        DecimalFormat format = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.getDefault()));
        return format.format(BigDecimal.valueOf(minorUnits, SCALE));
    }

    /** Plain, group-free text for edit fields: {@code 1234.56}. */
    public static String toInput(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, SCALE).toPlainString();
    }

    /** Short form for chart axes: {@code $1.2k}, {@code $3.4M}. */
    public static String formatCompact(long minorUnits, @Nullable String currencyCode) {
        double value = minorUnits / 100.0;
        String sign = value < 0 ? "-" : "";
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000) {
            return sign + symbol(currencyCode) + trim(abs / 1_000_000_000.0) + "B";
        }
        if (abs >= 1_000_000) {
            return sign + symbol(currencyCode) + trim(abs / 1_000_000.0) + "M";
        }
        if (abs >= 1_000) {
            return sign + symbol(currencyCode) + trim(abs / 1_000.0) + "k";
        }
        return sign + symbol(currencyCode) + trim(abs);
    }

    private static String trim(double value) {
        DecimalFormat format = new DecimalFormat("0.#",
                DecimalFormatSymbols.getInstance(Locale.getDefault()));
        return format.format(value);
    }

    /** Currency symbol for display, falling back to the code for unknown currencies. */
    public static String symbol(@Nullable String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return "$";
        }
        String code = currencyCode.trim().toUpperCase(Locale.ROOT);
        if ("AFN".equals(code)) {
            // The afghani sign is not always present in the platform currency data.
            return "\u060B";
        }
        try {
            String symbol = Currency.getInstance(code).getSymbol(Locale.getDefault());
            if (symbol != null && !symbol.isEmpty()) {
                // Currency codes such as "AFN" need a space; glyphs such as "$" do not.
                return symbol.matches("[A-Za-z]{3}") ? symbol + " " : symbol;
            }
        } catch (IllegalArgumentException ignored) {
            // Unknown currency: fall through to the code itself.
        }
        return code + " ";
    }

    /**
     * Parses free-form user input ("1,234.5", "$ 12.00", ".75") into minor units.
     * Returns {@code null} when the text is not a usable amount.
     */
    @Nullable
    public static Long parse(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = normalizeNumberCharacters(raw.trim())
                .replace(" ", "")
                .replace("\u00A0", "");
        // Remove currency symbols and letters but keep digits, separators and sign.
        cleaned = cleaned.replaceAll("[^0-9.,\\-+]", "");
        cleaned = normalizeSeparators(cleaned);
        // Collapse accidental double separators from typing, e.g. "1.234.56".
        int firstDot = cleaned.indexOf('.');
        if (firstDot >= 0) {
            String head = cleaned.substring(0, firstDot + 1);
            String tail = cleaned.substring(firstDot + 1).replace(".", "");
            cleaned = head + tail;
        }
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals("+")
                || cleaned.equals(".") || cleaned.equals("-.")) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(cleaned);
            BigDecimal minor = value.movePointRight(SCALE).setScale(0, RoundingMode.HALF_UP);
            long result = minor.longValueExact();
            if (Math.abs(result) > MAX_ABS_MINOR) {
                return null;
            }
            return result;
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    /** Converts Persian/Arabic-Indic digits and separators to ASCII equivalents. */
    private static String normalizeNumberCharacters(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '\u06F0' && c <= '\u06F9') {
                normalized.append((char) ('0' + (c - '\u06F0')));
            } else if (c >= '\u0660' && c <= '\u0669') {
                normalized.append((char) ('0' + (c - '\u0660')));
            } else if (c == '\u066B') { // Arabic decimal separator
                normalized.append('.');
            } else if (c == '\u066C' || c == '\u060C') { // Arabic thousands / comma
                normalized.append(',');
            } else {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    /**
     * Turns comma variants into a single decimal point.
     * When both separators appear the comma is grouping ("1,234.50"); a lone comma
     * with more than three trailing digits or repeated commas is also grouping
     * ("1,234"), while a short trailing group is treated as a decimal comma ("1,50").
     */
    private static String normalizeSeparators(String value) {
        int commas = count(value, ',');
        int dots = count(value, '.');
        if (commas > 0 && dots > 0) {
            // The right-most separator is the decimal one; the other is grouping.
            char decimal = value.lastIndexOf(',') > value.lastIndexOf('.') ? ',' : '.';
            char grouping = decimal == ',' ? '.' : ',';
            return value.replace(String.valueOf(grouping), "")
                    .replace(decimal, '.');
        }
        if (commas > 0) {
            if (commas > 1) {
                return value.replace(",", "");
            }
            int firstComma = value.indexOf(',');
            String before = value.substring(0, firstComma);
            String after = value.substring(firstComma + 1);
            if (after.length() == 3 && !before.replaceAll("[^0-9]", "").isEmpty()) {
                return value.replace(",", "");
            }
            return value.replace(',', '.');
        }
        if (dots > 1) {
            return value.replace(".", "");
        }
        return value;
    }

    private static int count(String value, char target) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                total++;
            }
        }
        return total;
    }

    /** Parses and requires a strictly positive amount, otherwise returns {@code null}. */
    @Nullable
    public static Long parsePositive(@Nullable String raw) {
        Long value = parse(raw);
        return value == null || value <= 0 ? null : value;
    }

    /** Percentage of {@code part} relative to {@code total}, clamped to 0..100. */
    public static int percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        int percent = (int) Math.round(part * 100.0 / total);
        return Math.max(0, Math.min(100, percent));
    }

    /** Unclamped percentage, used to detect over-budget states. */
    public static int rawPercent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
    }

    /** Percentage change from {@code previous} to {@code current}; null when undefined. */
    @Nullable
    public static Double percentChange(long current, long previous) {
        if (previous == 0) {
            return null;
        }
        return (current - previous) * 100.0 / Math.abs(previous);
    }

    public static boolean isValidCurrencyCode(@Nullable String code) {
        if (code == null) {
            return false;
        }
        String trimmed = code.trim().toUpperCase(Locale.ROOT);
        if (!trimmed.matches("[A-Z]{3}")) {
            return false;
        }
        try {
            Currency.getInstance(trimmed);
            return true;
        } catch (IllegalArgumentException e) {
            // Accept well-formed codes the platform does not know yet.
            return true;
        }
    }
}
