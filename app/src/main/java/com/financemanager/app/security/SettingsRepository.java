package com.financemanager.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Persisted user preferences.
 *
 * <p>The PIN is never stored in plain text. A random salt plus a PBKDF2-HMAC-SHA256
 * hash live in an EncryptedSharedPreferences file backed by the Android Keystore.
 * If Keystore-backed storage is unavailable on a device, the app degrades to private
 * preferences instead of crashing; the PIN is still only stored as a salted hash.</p>
 */
public class SettingsRepository {

    private static final String TAG = "SettingsRepository";
    private static final String PREFS = "finance_settings";
    private static final String SECURE_PREFS = "finance_settings_secure";

    private static final String KEY_NAME = "user_name";
    private static final String KEY_CURRENCY = "default_currency";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_HIDE_BALANCE = "hide_balance";
    private static final String KEY_BLOCK_SCREENSHOTS = "block_screenshots";
    private static final String KEY_LAST_BACKUP = "last_backup_at";
    private static final String KEY_PIN_ENABLED = "pin_enabled";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_ITERATIONS = "pin_iterations";
    private static final String KEY_PIN_LENGTH = "pin_length";
    private static final String KEY_BIOMETRIC = "biometric_enabled";
    private static final String KEY_AUTO_LOCK = "auto_lock_minutes";
    private static final String KEY_NOTIF_BUDGET = "notif_budget";
    private static final String KEY_NOTIF_RECURRING = "notif_recurring";
    private static final String KEY_NOTIF_SAVINGS = "notif_savings";
    private static final String KEY_NOTIF_MONTHLY = "notif_monthly";
    private static final String KEY_SEEDED = "seeded_v1";
    private static final String KEY_PREFIX_FLAG = "flag_";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    /** Auto-lock values: 0 = immediately, -1 = never, otherwise minutes. */
    public static final int AUTO_LOCK_IMMEDIATELY = 0;
    public static final int AUTO_LOCK_NEVER = -1;
    public static final int AUTO_LOCK_ONE_MINUTE = 1;
    public static final int AUTO_LOCK_FIVE_MINUTES = 5;

    private static final int PIN_ITERATIONS = 120_000;
    private static final int PIN_KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;

    private final SharedPreferences prefs;
    private final SharedPreferences securePrefs;

    public SettingsRepository(Context context) {
        Context appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        securePrefs = openSecurePreferences(appContext);
    }

    private SharedPreferences openSecurePreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.w(TAG, "Encrypted preferences unavailable, using private preferences", e);
            return context.getSharedPreferences(SECURE_PREFS + "_fallback", Context.MODE_PRIVATE);
        }
    }

    // ------------------------------------------------------------------
    // Profile and appearance
    // ------------------------------------------------------------------

    public String getUserName() {
        return prefs.getString(KEY_NAME, "");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_NAME, name == null ? "" : name.trim()).apply();
    }

    public String getCurrency() {
        return prefs.getString(KEY_CURRENCY, "USD");
    }

    public void setCurrency(String code) {
        if (code != null && code.trim().matches("[A-Za-z]{3}")) {
            prefs.edit().putString(KEY_CURRENCY, code.trim().toUpperCase(Locale.ROOT)).apply();
        }
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
    }

    public boolean isHideBalance() {
        return prefs.getBoolean(KEY_HIDE_BALANCE, false);
    }

    public void setHideBalance(boolean hide) {
        prefs.edit().putBoolean(KEY_HIDE_BALANCE, hide).apply();
    }

    public boolean isBlockScreenshots() {
        return prefs.getBoolean(KEY_BLOCK_SCREENSHOTS, false);
    }

    public void setBlockScreenshots(boolean block) {
        prefs.edit().putBoolean(KEY_BLOCK_SCREENSHOTS, block).apply();
    }

    /** Wall-clock time of the last successful backup, 0 when none has been made. */
    public long getLastBackupAt() {
        return prefs.getLong(KEY_LAST_BACKUP, 0);
    }

    public void setLastBackupAt(long millis) {
        prefs.edit().putLong(KEY_LAST_BACKUP, millis).apply();
    }

    public boolean isSeeded() {
        return prefs.getBoolean(KEY_SEEDED, false);
    }

    public void markSeeded() {
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
    }

    // ------------------------------------------------------------------
    // Security
    // ------------------------------------------------------------------

    public boolean hasPin() {
        return securePrefs.getBoolean(KEY_PIN_ENABLED, false)
                && securePrefs.getString(KEY_PIN_HASH, null) != null;
    }

    /** Stores a salted PBKDF2 hash of the PIN. */
    public boolean setPin(String pin) {
        if (pin == null || !pin.matches("\\d{4,8}")) {
            return false;
        }
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);
            byte[] hash = hashPin(pin, salt, PIN_ITERATIONS);
            securePrefs.edit()
                    .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                    .putInt(KEY_PIN_ITERATIONS, PIN_ITERATIONS)
                    .putInt(KEY_PIN_LENGTH, pin.length())
                    .putBoolean(KEY_PIN_ENABLED, true)
                    .apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Could not store PIN", e);
            return false;
        }
    }

    public boolean verifyPin(String pin) {
        if (pin == null) {
            return false;
        }
        String storedHash = securePrefs.getString(KEY_PIN_HASH, null);
        String storedSalt = securePrefs.getString(KEY_PIN_SALT, null);
        if (storedHash == null || storedSalt == null) {
            return false;
        }
        try {
            byte[] salt = Base64.decode(storedSalt, Base64.NO_WRAP);
            int iterations = securePrefs.getInt(KEY_PIN_ITERATIONS, PIN_ITERATIONS);
            byte[] expected = Base64.decode(storedHash, Base64.NO_WRAP);
            byte[] actual = hashPin(pin, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            Log.e(TAG, "PIN verification failed", e);
            return false;
        }
    }

    public void clearPin() {
        securePrefs.edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .remove(KEY_PIN_ITERATIONS)
                .remove(KEY_PIN_LENGTH)
                .putBoolean(KEY_PIN_ENABLED, false)
                .putBoolean(KEY_BIOMETRIC, false)
                .apply();
    }

    private byte[] hashPin(String pin, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, iterations, PIN_KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        try {
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /** Used only to auto-submit the lock screen once the expected digits are entered. */
    public int getPinLength() {
        return securePrefs.getInt(KEY_PIN_LENGTH, 4);
    }

    public boolean isBiometricEnabled() {
        return securePrefs.getBoolean(KEY_BIOMETRIC, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        securePrefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply();
    }

    public int getAutoLockMinutes() {
        return prefs.getInt(KEY_AUTO_LOCK, AUTO_LOCK_IMMEDIATELY);
    }

    public void setAutoLockMinutes(int minutes) {
        prefs.edit().putInt(KEY_AUTO_LOCK, minutes).apply();
    }

    /** True when any app lock (PIN) is configured. */
    public boolean isAppLockEnabled() {
        return hasPin();
    }

    // ------------------------------------------------------------------
    // Notifications
    // ------------------------------------------------------------------

    public boolean isBudgetNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIF_BUDGET, true);
    }

    public void setBudgetNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_BUDGET, enabled).apply();
    }

    public boolean isRecurringRemindersEnabled() {
        return prefs.getBoolean(KEY_NOTIF_RECURRING, true);
    }

    public void setRecurringRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_RECURRING, enabled).apply();
    }

    public boolean isSavingsRemindersEnabled() {
        return prefs.getBoolean(KEY_NOTIF_SAVINGS, true);
    }

    public void setSavingsRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_SAVINGS, enabled).apply();
    }

    public boolean isMonthlySummaryEnabled() {
        return prefs.getBoolean(KEY_NOTIF_MONTHLY, true);
    }

    public void setMonthlySummaryEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_MONTHLY, enabled).apply();
    }

    // ------------------------------------------------------------------
    // Lightweight flags used by workers to avoid duplicate reminders
    // ------------------------------------------------------------------

    @Nullable
    public String getFlag(String key) {
        return prefs.getString(KEY_PREFIX_FLAG + key, null);
    }

    public void setFlag(String key, String value) {
        prefs.edit().putString(KEY_PREFIX_FLAG + key, value).apply();
    }

    public void clearAllPreferences() {
        prefs.edit().clear().apply();
        securePrefs.edit().clear().apply();
    }
}
