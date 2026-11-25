package com.financemanager.app.models;

import com.financemanager.app.data.entity.RecurringTransaction;

import java.util.Collections;
import java.util.List;

/** Aggregated household figures derived from ordinary transactions. */
public class HouseholdSummary {

    public long fromDate;
    public long toDate;
    public long currentTotal;
    public long previousTotal;
    public long budgetMinor;
    public long remainingMinor;
    public int percent;
    public int expenseCount;
    public int foreignCount;
    public List<CategoryTotal> categories = Collections.emptyList();
    public List<DailyTotal> daily = Collections.emptyList();
    public List<MonthlyTotal> monthly = Collections.emptyList();
    public List<HouseholdExpense> recent = Collections.emptyList();
    public List<RecurringTransaction> upcoming = Collections.emptyList();

    public boolean isEmpty() {
        return expenseCount == 0;
    }
}
