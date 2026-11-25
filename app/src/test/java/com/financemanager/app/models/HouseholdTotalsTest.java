package com.financemanager.app.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.financemanager.app.data.entity.TransactionEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/** Household list totals must never add different currencies together. */
public class HouseholdTotalsTest {

    @Test
    public void totalsAreSeparatedByAccountCurrency() {
        HouseholdExpense afn = expense(12_500L, "AFN");
        HouseholdExpense usd = expense(3_000L, "USD");
        HouseholdExpense afnAgain = expense(500L, "AFN");

        Map<String, Long> totals = HouseholdTotals.byCurrency(
                Arrays.asList(afn, usd, afnAgain));

        assertEquals(Long.valueOf(13_000L), totals.get("AFN"));
        assertEquals(Long.valueOf(3_000L), totals.get("USD"));
        assertEquals(13_000L, HouseholdTotals.totalFor(
                Arrays.asList(afn, usd, afnAgain), "AFN"));
        assertTrue(HouseholdTotals.hasForeignCurrency(
                Arrays.asList(afn, usd, afnAgain), "AFN"));
        assertFalse(HouseholdTotals.hasForeignCurrency(
                Arrays.asList(afn, afnAgain), "AFN"));
    }

    @Test
    public void emptyListHasNoTotalsOrForeignRows() {
        assertEquals(0L, HouseholdTotals.totalFor(Collections.emptyList(), "AFN"));
        assertFalse(HouseholdTotals.hasForeignCurrency(Collections.emptyList(), "AFN"));
    }

    private HouseholdExpense expense(long amountMinor, String currency) {
        HouseholdExpense expense = new HouseholdExpense();
        expense.transaction = new TransactionWithDetails();
        expense.transaction.transaction = new TransactionEntity();
        expense.transaction.transaction.amountMinor = amountMinor;
        expense.transaction.accountCurrency = currency;
        return expense;
    }
}
