package com.financemanager.app.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.util.List;

/**
 * Generates due recurring transactions and reminds the user about payments arriving
 * in the next two days. Generation is idempotent: a transaction is never created
 * twice for the same schedule and date.
 */
public class RecurringWorker extends Worker {

    private static final String TAG = "RecurringWorker";

    public RecurringWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FinanceManagerApp app = (FinanceManagerApp) getApplicationContext();
        try {
            long today = DateUtils.todayStart();
            int created = app.container().repository().generateDueTransactions(today);
            if (created > 0) {
                app.container().repository().runAsync(() ->
                        com.financemanager.app.notifications.BudgetAlertChecker.checkAll(
                                app, app.container().database(), app.container().settings(),
                                app.container().notifications()));
            }
            sendUpcomingReminders(app, today);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Recurring work failed", e);
            return Result.retry();
        }
    }

    private void sendUpcomingReminders(FinanceManagerApp app, long today) {
        if (!app.container().settings().isRecurringRemindersEnabled()
                || !com.financemanager.app.notifications.NotificationHelper.canNotify(app)) {
            return;
        }
        long upTo = DateUtils.endOfDay(DateUtils.toLocalDate(today).plusDays(2));
        List<RecurringTransaction> upcoming = app.container().database().recurringDao()
                .getUpcoming(today - 1, upTo);
        for (RecurringTransaction recurring : upcoming) {
            String flagKey = "recurring_" + recurring.id + "_"
                    + DateUtils.formatIsoDate(recurring.nextRunDate);
            if (app.container().settings().getFlag(flagKey) != null) {
                continue;
            }
            app.container().settings().setFlag(flagKey, "sent");
            String amount = Money.format(recurring.amountMinor,
                    app.container().settings().getCurrency());
            String prefix = recurring.type == TransactionType.INCOME ? "+" : "-";
            app.container().notifications().notifyRecurring(
                    2000 + (int) (recurring.id % 1_000_000),
                    app.getApplicationContext().getString(R.string.notif_recurring_title),
                    app.getApplicationContext().getString(R.string.notif_recurring_body,
                            recurring.title + " (" + prefix + amount + ")",
                            DateUtils.friendlyDate(app, recurring.nextRunDate)));
        }
    }
}
