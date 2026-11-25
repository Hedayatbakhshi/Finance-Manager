package com.financemanager.app.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.notifications.BudgetAlertChecker;

/** Re-evaluates budgets daily so alerts still fire when spending happens off-screen. */
public class BudgetCheckWorker extends Worker {

    public BudgetCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FinanceManagerApp app = (FinanceManagerApp) getApplicationContext();
        BudgetAlertChecker.checkAll(app, app.container().database(),
                app.container().settings(), app.container().notifications());
        return Result.success();
    }
}
