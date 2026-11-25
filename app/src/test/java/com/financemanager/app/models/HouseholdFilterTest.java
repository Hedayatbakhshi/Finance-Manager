package com.financemanager.app.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HouseholdFilterTest {

    @Test
    public void likeQueryWrapsSearchText() {
        HouseholdFilter filter = new HouseholdFilter();
        filter.query = " naan ";
        assertEquals("%naan%", filter.likeQuery());
        filter.query = null;
        assertEquals("%%", filter.likeQuery());
    }

    @Test
    public void defaultFilterHasNoActiveFilters() {
        HouseholdFilter filter = new HouseholdFilter();
        filter.householdId = 7;
        assertFalse(filter.hasActiveFilters());
        assertEquals(HouseholdFilter.SORT_NEWEST, filter.sort);
    }

    @Test
    public void anySelectionActivatesFilter() {
        HouseholdFilter filter = new HouseholdFilter();
        filter.categoryId = 3L;
        assertTrue(filter.hasActiveFilters());

        filter = new HouseholdFilter();
        filter.memberId = 2L;
        assertTrue(filter.hasActiveFilters());

        filter = new HouseholdFilter();
        filter.query = "rent";
        assertTrue(filter.hasActiveFilters());

        filter = new HouseholdFilter();
        filter.fromDate = 1000;
        assertTrue(filter.hasActiveFilters());
    }

    @Test
    public void sortOptionsAreDistinct() {
        assertTrue(HouseholdFilter.SORT_NEWEST != HouseholdFilter.SORT_OLDEST);
        assertTrue(HouseholdFilter.SORT_HIGHEST != HouseholdFilter.SORT_LOWEST);
    }
}
