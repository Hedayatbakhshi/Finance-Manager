package com.financemanager.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Money must round exactly once and never go through floating point. */
public class MoneyTest {

    @Test
    public void parsesPlainAndDecimalAmounts() {
        assertEquals(Long.valueOf(1250), Money.parse("12.50"));
        assertEquals(Long.valueOf(1250), Money.parse("12.5"));
        assertEquals(Long.valueOf(75), Money.parse(".75"));
        assertEquals(Long.valueOf(-4500), Money.parse("-45"));
        assertEquals(Long.valueOf(1200), Money.parse("$ 12.00"));
    }

    @Test
    public void parsesGroupedAmounts() {
        assertEquals(Long.valueOf(123450), Money.parse("1,234.50"));
        assertEquals(Long.valueOf(123450), Money.parse("1.234,50"));
        assertEquals(Long.valueOf(123400), Money.parse("1,234"));
        assertEquals(Long.valueOf(150), Money.parse("1,50"));
        assertEquals(Long.valueOf(1234500), Money.parse("12,345.00"));
    }

    @Test
    public void parsesPersianAndArabicDigits() {
        assertEquals(Long.valueOf(1250), Money.parse("۱۲٫۵۰"));
        assertEquals(Long.valueOf(123450), Money.parse("۱٬۲۳۴٫۵۰"));
        assertEquals(Long.valueOf(1250), Money.parse("١٢.٥٠"));
        assertEquals(Long.valueOf(1250), Money.parse("۱۲,۵۰"));
    }

    @Test
    public void rejectsUnusableInput() {
        assertNull(Money.parse(null));
        assertNull(Money.parse(""));
        assertNull(Money.parse("-"));
        assertNull(Money.parse("."));
        assertNull(Money.parse("abc"));
        assertNull(Money.parsePositive("0"));
        assertNull(Money.parsePositive("-5"));
        assertNull(Money.parsePositive(""));
        assertEquals(Long.valueOf(100), Money.parsePositive("1.00"));
    }

    @Test
    public void roundsToTheNearestCentOnce() {
        assertEquals(Long.valueOf(1235), Money.parse("12.345"));
        assertEquals(Long.valueOf(1234), Money.parse("12.344"));
    }

    @Test
    public void formatsSignedAmounts() {
        assertEquals("$12.50", Money.format(1250, "USD"));
        assertEquals("-$12.50", Money.format(-1250, "USD"));
        assertEquals("+$12.50", Money.formatSigned(1250, "USD"));
        assertEquals("12.50", Money.formatPlain(1250));
        assertEquals("12.50", Money.toInput(1250));
    }

    @Test
    public void formatsAfghanCurrency() {
        assertEquals("\u060B", Money.symbol("AFN"));
        assertEquals("\u060B12.50", Money.format(1250, "AFN"));
        assertEquals("-\u060B12.50", Money.format(-1250, "AFN"));
    }

    @Test
    public void formatsCompactlyForCharts() {
        assertEquals("$1.2k", Money.formatCompact(120_000, "USD"));
        assertEquals("$3.4M", Money.formatCompact(340_000_000, "USD"));
        assertEquals("$12.5", Money.formatCompact(1250, "USD"));
    }

    @Test
    public void percentageIsClampedForProgressBars() {
        assertEquals(25, Money.percent(50, 200));
        assertEquals(100, Money.percent(250, 200));
        assertEquals(0, Money.percent(50, 0));
        assertEquals(125, Money.rawPercent(250, 200));
    }

    @Test
    public void percentChangeHandlesZeroBaseline() {
        assertNull(Money.percentChange(100, 0));
        assertEquals(50.0, Money.percentChange(150, 100), 0.0001);
        assertEquals(-50.0, Money.percentChange(50, 100), 0.0001);
    }

    @Test
    public void validatesCurrencyCodes() {
        assertTrue(Money.isValidCurrencyCode("USD"));
        assertTrue(Money.isValidCurrencyCode("eur"));
        assertFalse(Money.isValidCurrencyCode("US"));
        assertFalse(Money.isValidCurrencyCode("US1"));
        assertFalse(Money.isValidCurrencyCode(null));
    }
}
