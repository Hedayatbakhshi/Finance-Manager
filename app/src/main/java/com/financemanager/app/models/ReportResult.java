package com.financemanager.app.models;

import java.util.ArrayList;
import java.util.List;

/** Everything a report screen needs, computed from real records for one date range. */
public class ReportResult {

    public long fromDate;
    public long toDate;

    public long incomeTotal;
    public long expenseTotal;
    public int incomeCount;
    public int expenseCount;

    public List<CategoryTotal> incomeByCategory = new ArrayList<>();
    public List<CategoryTotal> expenseByCategory = new ArrayList<>();
    public List<DailyTotal> dailyIncome = new ArrayList<>();
    public List<DailyTotal> dailyExpense = new ArrayList<>();
    public List<MonthlyTotal> monthly = new ArrayList<>();

    public TransactionWithDetails largestIncome;
    public TransactionWithDetails largestExpense;

    /** Net cash flow: income minus expenses. Transfers never participate. */
    public long netCashFlow() {
        return incomeTotal - expenseTotal;
    }

    public boolean isEmpty() {
        return incomeTotal == 0 && expenseTotal == 0;
    }

    /** Inclusive number of days in the range, used for daily averages. */
    public int dayCount() {
        long days = (toDate - fromDate) / 86_400_000L + 1;
        return (int) Math.max(1, days);
    }
}
