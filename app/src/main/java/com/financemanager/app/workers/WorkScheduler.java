package com.financemanager.app.workers;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/** Registers the app's background jobs. */
public final class WorkScheduler {

    private static final String RECURRING_PERIODIC = "recurring_transactions_daily";
    private static final String BUDGET_PERIODIC = "budget_alerts_daily";
    private static final String SAVINGS_PERIODIC = "savings_reminder_weekly";
    private static final String SUMMARY_PERIODIC = "monthly_summary_daily";
    private static final String RECURRING_CATCH_UP = "recurring_transactions_catch_up";
    private static final String BUDGET_CATCH_UP = "budget_alerts_catch_up";

    private WorkScheduler() {
    }

    /** One-off work at startup so missed schedules are processed immediately. */
    public static void scheduleInitialWork(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();
        workManager.enqueueUniqueWork(RECURRING_CATCH_UP, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(RecurringWorker.class)
                        .setConstraints(constraints)
                        .build());
        workManager.enqueueUniqueWork(BUDGET_CATCH_UP, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(BudgetCheckWorker.class)
                        .setConstraints(constraints)
                        .build());
    }

    public static void schedulePeriodicWork(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        workManager.enqueueUniquePeriodicWork(RECURRING_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(RecurringWorker.class, 1, TimeUnit.DAYS)
                        .setInitialDelay(delayUntil(0, 10), TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .build());

        workManager.enqueueUniquePeriodicWork(BUDGET_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(BudgetCheckWorker.class, 1, TimeUnit.DAYS)
                        .setInitialDelay(delayUntil(7, 0), TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .build());

        workManager.enqueueUniquePeriodicWork(SAVINGS_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(SavingsReminderWorker.class, 7, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .build());

        workManager.enqueueUniquePeriodicWork(SUMMARY_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(MonthlySummaryWorker.class, 1, TimeUnit.DAYS)
                        .setInitialDelay(delayUntil(9, 0), TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .build());
    }

    private static long delayUntil(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return target.getTimeInMillis() - now.getTimeInMillis();
    }
}
