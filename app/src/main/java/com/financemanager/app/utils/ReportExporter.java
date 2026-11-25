package com.financemanager.app.utils;

import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.CategoryTotal;
import com.financemanager.app.models.MonthlyTotal;
import com.financemanager.app.models.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Builds a plain-text financial report from real records. */
public class ReportExporter {

    private final FinanceDatabase database;

    public ReportExporter(FinanceDatabase database) {
        this.database = database;
    }

    public static String suggestedFileName() {
        return "finance-manager-report-" + LocalDate.now() + ".txt";
    }

    public String build(String currency) {
        StringBuilder builder = new StringBuilder();
        builder.append("FINANCE MANAGER — FINANCIAL REPORT\n");
        builder.append("Generated ").append(DateUtils.formatDay(System.currentTimeMillis()))
                .append(" | Currency: ").append(currency).append("\n\n");

        builder.append("ACCOUNTS\n");
        List<AccountWithBalance> accounts = database.accountDao().getWithBalancesSync();
        long total = 0;
        for (AccountWithBalance account : accounts) {
            builder.append("  ").append(account.account.name)
                    .append(" — ").append(Money.format(account.balanceMinor, account.account.currency))
                    .append('\n');
            if (account.account.currency.equalsIgnoreCase(currency)) {
                total += account.balanceMinor;
            }
        }
        builder.append("  Total (").append(currency).append("): ")
                .append(Money.format(total, currency)).append("\n\n");

        LocalDate today = LocalDate.now();
        long from = DateUtils.startOfMonth(today.minusMonths(5));
        long to = DateUtils.endOfMonth(today);
        builder.append("LAST 6 MONTHS\n");
        builder.append(String.format(Locale.getDefault(), "  %-10s %14s %14s %14s%n",
                "Month", "Income", "Expenses", "Net"));
        List<MonthlyTotal> months = database.transactionDao().monthlyTotals(from, to, currency);
        long incomeSum = 0;
        long expenseSum = 0;
        for (MonthlyTotal month : months) {
            incomeSum += month.incomeMinor;
            expenseSum += month.expenseMinor;
            builder.append(String.format(Locale.getDefault(), "  %-10s %14s %14s %14s%n",
                    DateUtils.formatMonthKey(month.monthKey),
                    Money.formatPlain(month.incomeMinor),
                    Money.formatPlain(month.expenseMinor),
                    Money.formatPlain(month.incomeMinor - month.expenseMinor)));
        }
        builder.append(String.format(Locale.getDefault(), "  %-10s %14s %14s %14s%n",
                "Total", Money.formatPlain(incomeSum), Money.formatPlain(expenseSum),
                Money.formatPlain(incomeSum - expenseSum)));
        builder.append('\n');

        long monthFrom = DateUtils.startOfMonth(today);
        long monthTo = DateUtils.endOfMonth(today);
        builder.append("EXPENSES BY CATEGORY — ")
                .append(DateUtils.formatMonth(DateUtils.startOfMonth(today)))
                .append('\n');
        List<CategoryTotal> categories = database.transactionDao()
                .categoryTotals(TransactionType.EXPENSE, monthFrom, monthTo, currency);
        if (categories.isEmpty()) {
            builder.append("  No expenses recorded this month.\n");
        } else {
            for (CategoryTotal category : categories) {
                builder.append("  ").append(category.categoryName == null ? "Uncategorized"
                                : category.categoryName)
                        .append(": ").append(Money.format(category.totalMinor, currency))
                        .append('\n');
            }
        }
        builder.append('\n');
        builder.append("Budgets\n");
        builder.append("  Active budgets: ")
                .append(database.budgetDao().count()).append('\n');
        builder.append("Savings goals: ")
                .append(database.savingsGoalDao().count()).append('\n');
        builder.append("Recurring schedules: ")
                .append(database.recurringDao().count()).append('\n');
        return builder.toString();
    }
}
