package com.financemanager.app.ui.dashboard;

import androidx.annotation.NonNull;

import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.BudgetWithProgress;
import com.financemanager.app.models.TransactionWithDetails;
import com.financemanager.app.utils.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Everything the dashboard renders, derived from live database results. */
public class DashboardState {

    public static final int RANGE_WEEK = 0;
    public static final int RANGE_MONTH = 1;
    public static final int RANGE_YEAR = 2;

    public String currency = "USD";
    public long totalBalance;
    public long previousMonthBalance;
    public long monthIncome;
    public long monthExpense;
    public int accountsCount;
    public boolean hasForeignCurrencyAccounts;
    public boolean loaded;

    public List<AccountWithBalance> accounts = Collections.emptyList();
    public List<TransactionWithDetails> recent = Collections.emptyList();
    public List<BudgetWithProgress> activeBudgets = Collections.emptyList();

    public ChartData week = new ChartData();
    public ChartData month = new ChartData();
    public ChartData year = new ChartData();

    public long budgetTotal;
    public long budgetSpent;

    public String topCategoryName;
    public String topCategoryIcon;
    public String topCategoryColor;
    public long topCategoryTotal;
    public long averageDailyExpense;

    /** Percentage change of net worth versus the end of last month, if calculable. */
    public Double balanceChangePercent() {
        return Money.percentChange(totalBalance, previousMonthBalance);
    }

    public int budgetPercent() {
        return Money.percent(budgetSpent, budgetTotal);
    }

    @NonNull
    public ChartData chart(int range) {
        if (range == RANGE_WEEK) {
            return week;
        }
        if (range == RANGE_YEAR) {
            return year;
        }
        return month;
    }

    /** Labels and values (in major units) for one chart range. */
    public static class ChartData {
        public final List<String> labels = new ArrayList<>();
        public final List<Float> income = new ArrayList<>();
        public final List<Float> expense = new ArrayList<>();

        public boolean hasData() {
            return !labels.isEmpty() && (sum(income) > 0f || sum(expense) > 0f);
        }

        private float sum(List<Float> values) {
            float total = 0f;
            for (Float value : values) {
                if (value != null) {
                    total += value;
                }
            }
            return total;
        }
    }
}
