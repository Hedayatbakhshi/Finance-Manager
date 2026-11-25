package com.financemanager.app.security;

import android.content.Context;

import androidx.biometric.BiometricManager;

/** Small wrapper around BiometricManager capability checks. */
public final class BiometricHelper {

    public static final int AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private BiometricHelper() {
    }

    /** True when the device can authenticate the user right now. */
    public static boolean isAvailable(Context context) {
        BiometricManager manager = BiometricManager.from(context);
        return manager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /** True when hardware exists but the user has not enrolled biometrics. */
    public static boolean needsEnrollment(Context context) {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)
                == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }
}
