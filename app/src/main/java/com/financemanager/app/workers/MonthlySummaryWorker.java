package com.financemanager.app.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Posts a recap of the previous month on the first day of each month.
 * The daily periodic job checks the date and a persisted month key, so the summary
 * is sent exactly once.
 */
public class MonthlySummaryWorker extends Worker {

    public MonthlySummaryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FinanceManagerApp app = (FinanceManagerApp) getApplicationContext();
        try {
            if (!app.container().settings().isMonthlySummaryEnabled()
                    || !com.financemanager.app.notifications.NotificationHelper.canNotify(app)) {
                return Result.success();
            }
            LocalDate today = LocalDate.now();
            if (today.getDayOfMonth() != 1 || LocalTime.now().getHour() < 9) {
                return Result.success();
            }
            String monthKey = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (monthKey.equals(app.container().settings().getFlag("monthly_summary"))) {
                return Result.success();
            }
            LocalDate previousMonth = today.minusMonths(1);
            long from = DateUtils.startOfMonth(previousMonth);
            long to = DateUtils.endOfMonth(previousMonth);
            String currency = app.container().settings().getCurrency();
            long income = app.container().database().transactionDao()
                    .totalForType(TransactionType.INCOME, from, to, currency);
            long expense = app.container().database().transactionDao()
                    .totalForType(TransactionType.EXPENSE, from, to, currency);
            if (income == 0 && expense == 0) {
                app.container().settings().setFlag("monthly_summary", monthKey);
                return Result.success();
            }
            app.container().settings().setFlag("monthly_summary", monthKey);
            app.container().notifications().notifySummary(4000,
                    app.getString(R.string.notif_summary_title),
                    app.getString(R.string.notif_summary_body,
                            Money.format(income, currency), Money.format(expense, currency)));
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
