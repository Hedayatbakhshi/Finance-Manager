package com.financemanager.app.utils;

import android.app.Activity;

import androidx.appcompat.app.AppCompatDelegate;

import com.financemanager.app.R;
import com.financemanager.app.security.SettingsRepository;

/** Applies the user's light/dark/system preference and accent palette app-wide. */
public final class ThemeHelper {

    private ThemeHelper() {
    }

    public static void apply(int themeMode) {
        switch (themeMode) {
            case SettingsRepository.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SettingsRepository.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SettingsRepository.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Overlays the chosen accent on the activity theme. Must be called before
     * {@code super.onCreate} so every inflated view picks up the palette.
     */
    public static void applyAccent(Activity activity, int accent) {
        activity.getTheme().applyStyle(overlayFor(accent), true);
    }

    private static int overlayFor(int accent) {
        switch (accent) {
            case SettingsRepository.ACCENT_EMERALD:
                return R.style.ThemeOverlay_Finance_Accent_Emerald;
            case SettingsRepository.ACCENT_VIOLET:
                return R.style.ThemeOverlay_Finance_Accent_Violet;
            case SettingsRepository.ACCENT_AMBER:
                return R.style.ThemeOverlay_Finance_Accent_Amber;
            case SettingsRepository.ACCENT_BLUE:
            default:
                return R.style.ThemeOverlay_Finance_Accent_Blue;
        }
    }
}
