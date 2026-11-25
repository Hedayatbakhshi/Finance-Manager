package com.financemanager.app.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.utils.Money;

import java.time.LocalDate;
import java.util.List;

/** Weekly nudge for the savings goal that is closest to completion but not finished. */
public class SavingsReminderWorker extends Worker {

    public SavingsReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FinanceManagerApp app = (FinanceManagerApp) getApplicationContext();
        try {
            if (!app.container().settings().isSavingsRemindersEnabled()
                    || !com.financemanager.app.notifications.NotificationHelper.canNotify(app)) {
                return Result.success();
            }
            String weekKey = LocalDate.now().toString();
            String lastSent = app.container().settings().getFlag("savings_reminder");
            if (weekKey.equals(lastSent)) {
                return Result.success();
            }
            List<SavingsGoal> goals = app.container().database().savingsGoalDao().getAllSync();
            SavingsGoal best = null;
            int bestPercent = -1;
            for (SavingsGoal goal : goals) {
                if (goal.targetMinor <= 0) {
                    continue;
                }
                int percent = Money.percent(goal.currentMinor, goal.targetMinor);
                if (percent < 100 && percent > bestPercent) {
                    bestPercent = percent;
                    best = goal;
                }
            }
            if (best == null) {
                return Result.success();
            }
            app.container().settings().setFlag("savings_reminder", weekKey);
            app.container().notifications().notifySavings(3000,
                    app.getString(R.string.notif_savings_title),
                    app.getString(R.string.notif_savings_body, best.name, bestPercent));
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
