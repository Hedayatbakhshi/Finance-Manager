package com.financemanager.app.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.financemanager.app.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/** Small shared helpers for consistent spacing, tinting and dialogs. */
public final class UiUtils {

    private UiUtils() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static int color(Context context, int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    public static int themeColor(View view, int attr) {
        return MaterialColors.getColor(view, attr);
    }

    public static int parseColor(@Nullable String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        try {
            return Color.parseColor(hex.startsWith("#") ? hex : "#" + hex);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Paints a rounded icon tile with a soft tint of the given color. */
    public static void tintIconTile(FrameLayout container, ImageView icon, String colorHex) {
        int base = parseColor(colorHex, Color.GRAY);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(container.getContext(), 13));
        background.setColor(withAlpha(base, 0.16f));
        container.setBackground(background);
        icon.setColorFilter(base);
    }

    /** Paints a circular avatar with a soft tint of the given color. */
    public static void tintCircle(FrameLayout container, ImageView icon, String colorHex) {
        int base = parseColor(colorHex, Color.GRAY);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(withAlpha(base, 0.16f));
        container.setBackground(background);
        icon.setColorFilter(base);
    }

    public static int withAlpha(int color, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static void snackbar(View anchor, CharSequence message) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    public static void snackbarWithAction(View anchor, CharSequence message, CharSequence action,
                                         View.OnClickListener listener) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG)
                .setAction(action, listener)
                .show();
    }

    public static void confirm(Context context, String title, String message, String positive,
                               Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(positive, (dialog, which) -> onConfirm.run())
                .show();
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "FM";
        }
        String trimmed = name.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return trimmed.substring(0, Math.min(2, trimmed.length())).toUpperCase();
    }
}
