package com.financemanager.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Household budgets reuse the app's exact minor-unit money helpers. */
public class HouseholdMoneyTest {

    @Test
    public void overBudgetPercentIsNotClamped() {
        long budgetMinor = 50_000L;
        long spentMinor = 125_000L;
        assertEquals(250, Money.rawPercent(spentMinor, budgetMinor));
        assertEquals(100, Money.percent(spentMinor, budgetMinor));
        assertEquals(-75_000L, budgetMinor - spentMinor);
    }

    @Test
    public void afghaniSymbolIsUsedForHouseholdCurrency() {
        assertTrue(Money.symbol("AFN").contains("؋"));
        assertTrue(Money.symbol("USD").contains("$"));
    }

    @Test
    public void persianDigitsAreParsedForManualEntry() {
        Long parsed = Money.parse("۱۲٫۵۰");
        assertEquals(Long.valueOf(1_250L), parsed);
    }
}
