package com.financemanager.app.models;

import androidx.room.ColumnInfo;

/** Income and expense totals for a calendar month keyed as yyyy-MM. */
public class MonthlyTotal {

    @ColumnInfo(name = "month_key")
    public String monthKey;

    @ColumnInfo(name = "income_minor")
    public long incomeMinor;

    @ColumnInfo(name = "expense_minor")
    public long expenseMinor;
}
