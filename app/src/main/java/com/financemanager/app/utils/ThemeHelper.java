package com.financemanager.app.utils;

import androidx.appcompat.app.AppCompatDelegate;

import com.financemanager.app.security.SettingsRepository;

/** Applies the user's light/dark/system preference app-wide. */
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
}
