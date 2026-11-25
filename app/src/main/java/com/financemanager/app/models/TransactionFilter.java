package com.financemanager.app.models;

import androidx.annotation.Nullable;

import com.financemanager.app.utils.DateUtils;

import java.io.Serializable;
import java.time.LocalDate;

/** Everything the transaction list can filter and sort by. */
public class TransactionFilter implements Serializable {

    public static final int SORT_NEWEST = 0;
    public static final int SORT_OLDEST = 1;
    public static final int SORT_HIGHEST = 2;
    public static final int SORT_LOWEST = 3;

    @Nullable
    public TransactionType type;

    public String query = "";

    public long fromDate;

    public long toDate;

    @Nullable
    public Long categoryId;

    @Nullable
    public Long accountId;

    @Nullable
    public Long minAmount;

    @Nullable
    public Long maxAmount;

    public int sort = SORT_NEWEST;

    public ReportPeriod period = ReportPeriod.ALL_TIME;

    public TransactionFilter() {
        fromDate = DateUtils.startOfDay(LocalDate.of(1970, 1, 1));
        toDate = DateUtils.endOfDay(LocalDate.now());
    }

    public static TransactionFilter all() {
        return new TransactionFilter();
    }

    /** SQL LIKE pattern for the title/description search. */
    public String likeQuery() {
        return "%" + query.trim() + "%";
    }

    public boolean hasActiveFilters() {
        return type != null || categoryId != null || accountId != null
                || minAmount != null || maxAmount != null
                || fromDate > DateUtils.startOfDay(LocalDate.of(1970, 1, 2))
                || toDate < DateUtils.endOfDay(LocalDate.now())
                || !query.trim().isEmpty()
                || period != ReportPeriod.ALL_TIME;
    }

    public void reset() {
        type = null;
        categoryId = null;
        accountId = null;
        minAmount = null;
        maxAmount = null;
        fromDate = DateUtils.startOfDay(LocalDate.of(1970, 1, 1));
        toDate = DateUtils.endOfDay(LocalDate.now());
        query = "";
        sort = SORT_NEWEST;
        period = ReportPeriod.ALL_TIME;
    }

    public TransactionFilter copy() {
        TransactionFilter copy = new TransactionFilter();
        copy.type = type;
        copy.query = query;
        copy.fromDate = fromDate;
        copy.toDate = toDate;
        copy.categoryId = categoryId;
        copy.accountId = accountId;
        copy.minAmount = minAmount;
        copy.maxAmount = maxAmount;
        copy.sort = sort;
        copy.period = period;
        return copy;
    }
}
