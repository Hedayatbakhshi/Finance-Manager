package com.financemanager.app.models;

/** Search, filter and sort options for the household expense list. */
public class HouseholdFilter {

    public static final int SORT_NEWEST = 0;
    public static final int SORT_OLDEST = 1;
    public static final int SORT_HIGHEST = 2;
    public static final int SORT_LOWEST = 3;

    public long householdId;
    public String query = "";
    public long fromDate = 1;
    public long toDate = Long.MAX_VALUE;
    public Long categoryId;
    public Long accountId;
    public Long memberId;
    public int sort = SORT_NEWEST;

    public String likeQuery() {
        String value = query == null ? "" : query.trim();
        return "%" + value + "%";
    }

    public boolean hasActiveFilters() {
        return (query != null && !query.trim().isEmpty())
                || categoryId != null || accountId != null || memberId != null
                || fromDate > 1 || toDate < Long.MAX_VALUE;
    }
}
