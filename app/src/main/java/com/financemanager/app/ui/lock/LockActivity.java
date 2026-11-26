package com.financemanager.app.ui.lock;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.databinding.ActivityLockBinding;
import com.financemanager.app.di.AppContainer;
import com.financemanager.app.security.BiometricHelper;
import com.financemanager.app.utils.LanguageHelper;
import com.financemanager.app.utils.ThemeHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Unlock screen: numeric keypad PIN and optional biometric prompt. */
public class LockActivity extends AppCompatActivity {

    private ActivityLockBinding binding;
    private AppContainer container;
    private final StringBuilder entered = new StringBuilder();
    private boolean biometricPromptShown;
    private boolean verifying;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyAccent(this,
                ((FinanceManagerApp) getApplication()).container().settings().getAccent());
        super.onCreate(savedInstanceState);
        LanguageHelper.syncDefaultLocale();
        container = ((FinanceManagerApp) getApplication()).container();
        if (!container.settings().hasPin()) {
            setResult(RESULT_OK);
            finish();
            return;
        }
        if (container.settings().isBlockScreenshots()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        buildDots();
        wireKeypad();
        configureBiometric();

        binding.forgot.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pin_forgot)
                .setMessage(R.string.pin_forgot)
                .setPositiveButton(R.string.close, null)
                .show());
    }

    private void buildDots() {
        binding.dots.removeAllViews();
        int length = Math.max(4, container.settings().getPinLength());
        int size = Math.round(12 * getResources().getDisplayMetrics().density);
        int margin = Math.round(8 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(entered.length() > i
                    ? R.drawable.bg_circle : R.drawable.bg_dot_empty);
            if (entered.length() > i) {
                ViewCompat.setBackgroundTintList(dot,
                        ContextCompat.getColorStateList(this, R.color.primary));
            }
            binding.dots.addView(dot);
        }
    }

    private void wireKeypad() {
        binding.key0.setOnClickListener(v -> append("0"));
        binding.key1.setOnClickListener(v -> append("1"));
        binding.key2.setOnClickListener(v -> append("2"));
        binding.key3.setOnClickListener(v -> append("3"));
        binding.key4.setOnClickListener(v -> append("4"));
        binding.key5.setOnClickListener(v -> append("5"));
        binding.key6.setOnClickListener(v -> append("6"));
        binding.key7.setOnClickListener(v -> append("7"));
        binding.key8.setOnClickListener(v -> append("8"));
        binding.key9.setOnClickListener(v -> append("9"));
        binding.keyBackspace.setOnClickListener(v -> {
            if (entered.length() > 0) {
                entered.deleteCharAt(entered.length() - 1);
                binding.error.setText("");
                buildDots();
            }
        });
    }

    private void append(String digit) {
        int maxLength = Math.max(8, container.settings().getPinLength());
        if (entered.length() >= maxLength || verifying) {
            return;
        }
        entered.append(digit);
        buildDots();
        if (entered.length() >= container.settings().getPinLength()) {
            verifyPin();
        }
    }

    private void verifyPin() {
        verifying = true;
        boolean correct = container.settings().verifyPin(entered.toString());
        verifying = false;
        if (correct) {
            unlock();
        } else {
            binding.error.setText(R.string.pin_incorrect);
            shakeDots();
            entered.setLength(0);
            binding.dots.postDelayed(this::buildDots, 180);
        }
    }

    private void shakeDots() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(binding.dots, "translationX",
                0f, 18f, -18f, 12f, -12f, 0f);
        animator.setDuration(320);
        animator.start();
    }

    private void unlock() {
        container.sessionManager().unlock();
        setResult(RESULT_OK);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void configureBiometric() {
        boolean enabled = container.settings().isBiometricEnabled();
        boolean available = BiometricHelper.isAvailable(this);
        binding.keyBiometric.setVisibility(enabled && available ? View.VISIBLE : View.INVISIBLE);
        binding.keyBiometric.setOnClickListener(v -> showBiometricPrompt());
        if (enabled && available) {
            binding.keyBiometric.post(this::showBiometricPrompt);
        }
    }

    private void showBiometricPrompt() {
        if (biometricPromptShown || !container.settings().isBiometricEnabled()) {
            return;
        }
        biometricPromptShown = true;
        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                unlock();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                binding.error.setText(errString);
            }

            @Override
            public void onAuthenticationFailed() {
                binding.error.setText(R.string.pin_incorrect);
            }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setAllowedAuthenticators(BiometricHelper.AUTHENTICATORS)
                .build();
        prompt.authenticate(info);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        entered.setLength(0);
        if (binding != null) {
            buildDots();
        }
    }
}
