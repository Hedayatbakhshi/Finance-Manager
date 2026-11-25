package com.financemanager.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.financemanager.app.di.AppContainer;
import com.financemanager.app.ui.lock.LockActivity;
import com.financemanager.app.utils.ThemeHelper;
import com.financemanager.app.workers.WorkScheduler;

/** Application entry point: builds the dependency graph and bootstraps app-wide services. */
public class FinanceManagerApp extends Application implements Application.ActivityLifecycleCallbacks {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        ThemeHelper.apply(container.settings().getThemeMode());
        container.notifications().createChannels();
        container.sessionManager().configure(container.settings().isAppLockEnabled());
        registerActivityLifecycleCallbacks(this);
        WorkScheduler.schedulePeriodicWork(this);
    }

    @NonNull
    public AppContainer container() {
        return container;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        container.sessionManager().onForeground();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        // Ignore rotation/theme recreations and the transient lock screen launch.
        if (activity.isChangingConfigurations()) {
            return;
        }
        if (activity instanceof com.financemanager.app.ui.MainActivity
                || activity instanceof LockActivity) {
            container.sessionManager().onBackground();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
