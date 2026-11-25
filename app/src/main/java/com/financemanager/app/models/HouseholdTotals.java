package com.financemanager.app.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Currency-aware totals for household expense lists.
 *
 * <p>Amounts are never converted or added across currencies because the app is fully
 * offline and has no exchange-rate feed. Callers show the household-currency total and
 * disclose that rows in other currencies exist.</p>
 */
public final class HouseholdTotals {

    private HouseholdTotals() {
    }

    /** Sums expenses per account currency. */
    public static Map<String, Long> byCurrency(List<HouseholdExpense> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> totals = new LinkedHashMap<>();
        for (HouseholdExpense item : items) {
            if (item == null || item.transaction == null
                    || item.transaction.transaction == null) {
                continue;
            }
            String currency = normalize(item.transaction.accountCurrency);
            Long current = totals.get(currency);
            totals.put(currency, (current == null ? 0L : current)
                    + item.transaction.transaction.amountMinor);
        }
        return totals;
    }

    /** Total for one currency, or zero when there are no matching rows. */
    public static long totalFor(List<HouseholdExpense> items, String currency) {
        Long total = byCurrency(items).get(normalize(currency));
        return total == null ? 0L : total;
    }

    /** True when some rows were recorded in a different currency. */
    public static boolean hasForeignCurrency(List<HouseholdExpense> items, String currency) {
        String wanted = normalize(currency);
        for (String code : byCurrency(items).keySet()) {
            if (!wanted.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
