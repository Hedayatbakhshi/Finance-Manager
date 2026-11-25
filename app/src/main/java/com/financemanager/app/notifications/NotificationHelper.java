package com.financemanager.app.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.financemanager.app.R;
import com.financemanager.app.ui.MainActivity;

/** Creates notification channels and posts local notifications. */
public class NotificationHelper {

    public static final String CHANNEL_BUDGET = "budget_alerts";
    public static final String CHANNEL_RECURRING = "recurring_reminders";
    public static final String CHANNEL_SAVINGS = "savings_reminders";
    public static final String CHANNEL_SUMMARY = "monthly_summary";

    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_BUDGET,
                context.getString(R.string.channel_budget), NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_RECURRING,
                context.getString(R.string.channel_recurring), NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_SAVINGS,
                context.getString(R.string.channel_savings), NotificationManager.IMPORTANCE_LOW));
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_SUMMARY,
                context.getString(R.string.channel_summary), NotificationManager.IMPORTANCE_LOW));
    }

    public static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public void notifyBudget(int id, String title, String message) {
        post(CHANNEL_BUDGET, id, title, message);
    }

    public void notifyRecurring(int id, String title, String message) {
        post(CHANNEL_RECURRING, id, title, message);
    }

    public void notifySavings(int id, String title, String message) {
        post(CHANNEL_SAVINGS, id, title, message);
    }

    public void notifySummary(int id, String title, String message) {
        post(CHANNEL_SUMMARY, id, title, message);
    }

    private void post(String channelId, int id, String title, String message) {
        if (!canNotify(context)) {
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_action_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {
            // Permission can be revoked between the check and the post; nothing to do.
        }
    }
}
