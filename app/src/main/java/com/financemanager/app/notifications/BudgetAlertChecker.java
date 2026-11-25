package com.financemanager.app.notifications;

import android.content.Context;
import android.util.Log;

import com.financemanager.app.R;
import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.data.entity.BudgetAlert;
import com.financemanager.app.models.BudgetWithProgress;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.util.List;

/**
 * Evaluates every active budget and posts a notification when a threshold is crossed.
 * Each (budget, period, threshold) is recorded once, so the same alert is never sent
 * twice; jumping from 40% to 110% sends a single, accurate "over budget" alert.
 */
public final class BudgetAlertChecker {

    private static final String TAG = "BudgetAlertChecker";
    private static final int[] THRESHOLDS = {50, 80, 100, 101};

    private BudgetAlertChecker() {
    }

    public static void checkAll(Context context, FinanceDatabase database,
                                SettingsRepository settings, NotificationHelper notifications) {
        if (!settings.isBudgetNotificationsEnabled() || !NotificationHelper.canNotify(context)) {
            return;
        }
        try {
            String currency = settings.getCurrency();
            long today = DateUtils.todayStart();
            List<BudgetWithProgress> budgets = database.budgetDao().getActiveOn(today, currency);
            for (BudgetWithProgress item : budgets) {
                checkBudget(context, database, notifications, currency, item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Budget alert check failed", e);
        }
    }

    private static void checkBudget(Context context, FinanceDatabase database,
                                    NotificationHelper notifications, String currency,
                                    BudgetWithProgress item) {
        if (item.budget.amountMinor <= 0) {
            return;
        }
        long spent = item.budget.categoryId > 0
                ? database.transactionDao().expenseForCategory(item.budget.categoryId,
                item.budget.startDate, item.budget.endDate, currency)
                : item.spentMinor;
        int percent = Money.rawPercent(spent, item.budget.amountMinor);
        int highest = 0;
        for (int threshold : THRESHOLDS) {
            if (percent >= threshold) {
                highest = threshold;
            }
        }
        if (highest == 0) {
            return;
        }
        String periodKey = DateUtils.formatIsoDate(item.budget.startDate) + "_"
                + DateUtils.formatIsoDate(item.budget.endDate);

        if (database.budgetAlertDao().count(item.budget.id, periodKey, highest) > 0) {
            return;
        }

        // Mark every threshold at or below the highest reached as delivered.
        for (int threshold : THRESHOLDS) {
            if (threshold <= highest) {
                BudgetAlert alert = new BudgetAlert();
                alert.budgetId = item.budget.id;
                alert.periodKey = periodKey;
                alert.threshold = threshold;
                alert.notifiedAt = System.currentTimeMillis();
                database.budgetAlertDao().insert(alert);
            }
        }

        String name = item.budget.name;
        String title = context.getString(R.string.notif_budget_title, name);
        String message;
        if (highest == 101) {
            message = context.getString(R.string.notif_budget_over, name,
                    Money.format(spent - item.budget.amountMinor, currency));
        } else if (highest == 100) {
            message = context.getString(R.string.notif_budget_100, name);
        } else if (highest == 80) {
            message = context.getString(R.string.notif_budget_80, name);
        } else {
            message = context.getString(R.string.notif_budget_50, name);
        }
        notifications.notifyBudget(1000 + (int) (item.budget.id % 1_000_000), title, message);
    }

    /** Utility used by the monthly summary worker to reuse the expense query. */
    public static long expenseTotal(FinanceDatabase database, long from, long to, String currency) {
        return database.transactionDao().totalForType(TransactionType.EXPENSE, from, to, currency);
    }
}
