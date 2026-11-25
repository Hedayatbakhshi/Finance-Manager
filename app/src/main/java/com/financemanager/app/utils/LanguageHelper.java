package com.financemanager.app.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/** Applies and reads the user's per-app language choice. */
public final class LanguageHelper {

    public static final String SYSTEM = "system";
    public static final String ENGLISH = "en";
    public static final String PERSIAN = "fa";

    private LanguageHelper() {
    }

    /**
     * Mirrors the app locale into {@link Locale#getDefault()} so numbers, dates and
     * currency symbols are formatted with the same language as the UI. AppCompat only
     * changes the resources locale, not the process-wide default.
     */
    public static void syncDefaultLocale() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        Locale locale = locales.isEmpty() ? null : locales.get(0);
        Locale.setDefault(locale == null ? systemLocale() : locale);
    }

    /** The device language, used when the app follows the system setting. */
    private static Locale systemLocale() {
        LocaleListCompat systemLocales = LocaleListCompat.wrap(
                Resources.getSystem().getConfiguration().getLocales());
        Locale locale = systemLocales.isEmpty() ? null : systemLocales.get(0);
        return locale == null ? Locale.US : locale;
    }

    /** Null means "follow the device language". */
    @Nullable
    public static String currentLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return null;
        }
        Locale locale = locales.get(0);
        return locale == null ? null : locale.getLanguage();
    }

    /** Wraps a context with the current app locale, for text created outside an Activity. */
    public static Context localizedContext(Context context) {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return context;
        }
        Locale locale = locales.get(0);
        if (locale == null) {
            return context;
        }
        Configuration configuration = new Configuration(
                context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    public static void apply(@Nullable String language) {
        LocaleListCompat locales;
        if (language == null || SYSTEM.equals(language)) {
            locales = LocaleListCompat.getEmptyLocaleList();
            Locale.setDefault(systemLocale());
        } else {
            locales = LocaleListCompat.forLanguageTags(language);
            Locale locale = locales.get(0);
            if (locale != null) {
                Locale.setDefault(locale);
            }
        }
        if (!locales.equals(AppCompatDelegate.getApplicationLocales())) {
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }
}
