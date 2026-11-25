package com.financemanager.app.security;

/** Tracks whether the current session is unlocked and when the app went to background. */
public class SessionManager {

    private boolean unlocked = true;
    private long backgroundedAt;

    /** Called once the app's security configuration is known. */
    public void configure(boolean appLockEnabled) {
        unlocked = !appLockEnabled;
        backgroundedAt = 0L;
    }

    public void unlock() {
        unlocked = true;
        backgroundedAt = 0L;
    }

    public void lock() {
        unlocked = false;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    /** Records that the app left the foreground. */
    public void onBackground() {
        if (unlocked) {
            backgroundedAt = System.currentTimeMillis();
        }
    }

    /** True when the user must authenticate again before seeing data. */
    public boolean requiresUnlock(SettingsRepository settings) {
        if (!settings.isAppLockEnabled()) {
            return false;
        }
        if (!unlocked) {
            return true;
        }
        int timeout = settings.getAutoLockMinutes();
        if (timeout == SettingsRepository.AUTO_LOCK_NEVER || backgroundedAt == 0L) {
            return false;
        }
        if (timeout == SettingsRepository.AUTO_LOCK_IMMEDIATELY) {
            return true;
        }
        return System.currentTimeMillis() - backgroundedAt >= timeout * 60_000L;
    }

    /** Called when the app returns to the foreground. */
    public void onForeground() {
        // Nothing to do: requiresUnlock compares the elapsed background time.
    }
}
