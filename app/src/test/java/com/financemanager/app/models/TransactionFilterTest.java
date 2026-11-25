package com.financemanager.app.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Filters start neutral and copy without sharing mutable state. */
public class TransactionFilterTest {

    @Test
    public void newFilterIsInactive() {
        TransactionFilter filter = TransactionFilter.all();
        assertFalse(filter.hasActiveFilters());
        assertEquals("%%", filter.likeQuery());
        assertEquals(TransactionFilter.SORT_NEWEST, filter.sort);
    }

    @Test
    public void detectsActiveFilters() {
        TransactionFilter filter = TransactionFilter.all();
        filter.type = TransactionType.EXPENSE;
        assertTrue(filter.hasActiveFilters());

        filter.reset();
        filter.query = "coffee";
        assertTrue(filter.hasActiveFilters());
        assertEquals("%coffee%", filter.likeQuery());

        filter.reset();
        filter.minAmount = 100L;
        assertTrue(filter.hasActiveFilters());

        filter.reset();
        filter.categoryId = 4L;
        assertTrue(filter.hasActiveFilters());
    }

    @Test
    public void copyIsIndependent() {
        TransactionFilter filter = TransactionFilter.all();
        filter.type = TransactionType.INCOME;
        filter.query = "salary";
        TransactionFilter copy = filter.copy();
        assertEquals(TransactionType.INCOME, copy.type);
        assertEquals("salary", copy.query);

        copy.reset();
        assertEquals(TransactionType.INCOME, filter.type);
        assertEquals("salary", filter.query);
        assertNull(copy.type);
    }

    @Test
    public void resetRestoresDefaults() {
        TransactionFilter filter = TransactionFilter.all();
        filter.type = TransactionType.TRANSFER;
        filter.fromDate = 0;
        filter.maxAmount = 50L;
        filter.reset();
        assertNull(filter.type);
        assertNull(filter.maxAmount);
        assertFalse(filter.hasActiveFilters());
    }
}
