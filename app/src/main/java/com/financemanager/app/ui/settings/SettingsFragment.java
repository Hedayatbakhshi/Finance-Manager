package com.financemanager.app.ui.settings;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.databinding.DialogPinBinding;
import com.financemanager.app.databinding.FragmentSettingsBinding;
import com.financemanager.app.databinding.ItemSettingBinding;
import com.financemanager.app.notifications.NotificationHelper;
import com.financemanager.app.security.BiometricHelper;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.ui.common.CurrencyPicker;
import com.financemanager.app.ui.common.LanguagePicker;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.LanguageHelper;
import com.financemanager.app.utils.ThemeHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Profile, appearance, security, notification and data preferences. */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsRepository settings;
    private FinanceRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FinanceManagerApp app = (FinanceManagerApp) requireActivity().getApplication();
        settings = app.container().settings();
        repository = app.container().repository();

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.nameRow.getRoot().setOnClickListener(v -> editName());
        binding.currencyRow.getRoot().setOnClickListener(v -> pickCurrency());
        binding.languageRow.getRoot().setOnClickListener(v -> pickLanguage());
        binding.themeRow.getRoot().setOnClickListener(v -> pickTheme());
        binding.accentRow.getRoot().setOnClickListener(v -> pickAccent());
        binding.autoLockRow.getRoot().setOnClickListener(v -> pickAutoLock());
        binding.changePinRow.getRoot().setOnClickListener(v -> changePin());
        binding.dataRow.getRoot().setOnClickListener(v -> navigate(R.id.data));
        binding.aboutRow.getRoot().setOnClickListener(v -> navigate(R.id.about));
        binding.clearRow.getRoot().setOnClickListener(v -> confirmClearData());

        binding.hideBalanceRow.getRoot().setOnClickListener(v ->
                binding.hideBalanceRow.switchWidget.performClick());
        binding.appLockRow.getRoot().setOnClickListener(v ->
                binding.appLockRow.switchWidget.performClick());
        binding.biometricRow.getRoot().setOnClickListener(v -> {
            if (!settings.hasPin()) {
                Toast.makeText(requireContext(), R.string.app_lock_summary,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            binding.biometricRow.switchWidget.performClick();
        });
        binding.blockScreenshotsRow.getRoot().setOnClickListener(v ->
                binding.blockScreenshotsRow.switchWidget.performClick());
        binding.notifBudgetRow.getRoot().setOnClickListener(v ->
                binding.notifBudgetRow.switchWidget.performClick());
        binding.notifRecurringRow.getRoot().setOnClickListener(v ->
                binding.notifRecurringRow.switchWidget.performClick());
        binding.notifSavingsRow.getRoot().setOnClickListener(v ->
                binding.notifSavingsRow.switchWidget.performClick());
        binding.notifMonthlyRow.getRoot().setOnClickListener(v ->
                binding.notifMonthlyRow.switchWidget.performClick());

        render();
    }

    private void render() {
        String name = settings.getUserName();
        valueRow(binding.nameRow, R.string.your_name, R.string.your_name_hint,
                name == null || name.trim().isEmpty()
                        ? getString(R.string.more_profile_name) : name);
        valueRow(binding.currencyRow, R.string.default_currency,
                R.string.default_currency_summary, settings.getCurrency());
        valueRow(binding.languageRow, R.string.language,
                R.string.language_summary, languageLabel());
        valueRow(binding.themeRow, R.string.theme, 0, themeLabel(settings.getThemeMode()));
        valueRow(binding.accentRow, R.string.accent_color, R.string.accent_summary,
                accentLabel(settings.getAccent()));
        valueRow(binding.autoLockRow, R.string.auto_lock, R.string.auto_lock_summary,
                autoLockLabel(settings.getAutoLockMinutes()));

        boolean hasPin = settings.hasPin();
        binding.changePinRow.getRoot().setVisibility(hasPin ? View.VISIBLE : View.GONE);
        binding.dividerChangePin.setVisibility(hasPin ? View.VISIBLE : View.GONE);
        valueRow(binding.changePinRow, R.string.change_pin, 0, "");
        switchRow(binding.blockScreenshotsRow, R.string.block_screenshots,
                R.string.block_screenshots_summary, settings.isBlockScreenshots(),
                (button, checked) -> {
                    settings.setBlockScreenshots(checked);
                    applyScreenshotPolicy(checked);
                });

        switchRow(binding.hideBalanceRow, R.string.hide_balance,
                R.string.hide_balance_summary, settings.isHideBalance(), (button, checked) -> {
                    settings.setHideBalance(checked);
                });

        switchRow(binding.appLockRow, R.string.app_lock, R.string.app_lock_summary,
                settings.hasPin(), (button, checked) -> {
                    if (checked) {
                        showPinDialog(false, () -> {
                            render();
                            Toast.makeText(requireContext(), R.string.pin_setup_title,
                                    Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        UiUtils.confirm(requireContext(), getString(R.string.disable_pin),
                                getString(R.string.app_lock_summary),
                                getString(R.string.disable_pin), () -> {
                                    settings.clearPin();
                                    settings.setBiometricEnabled(false);
                                    ((FinanceManagerApp) requireActivity().getApplication())
                                            .container().sessionManager().lock();
                                    render();
                                });
                    }
                });
        switchRow(binding.biometricRow, R.string.biometric_unlock, R.string.biometric_summary,
                settings.hasPin() && settings.isBiometricEnabled(), (button, checked) -> {
                    if (checked) {
                        if (!settings.hasPin()) {
                            render();
                            Toast.makeText(requireContext(), R.string.app_lock_summary,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (BiometricHelper.needsEnrollment(requireContext())) {
                            render();
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.biometric_unlock)
                                    .setMessage(R.string.biometric_not_enrolled)
                                    .setPositiveButton(R.string.done, null)
                                    .show();
                            return;
                        }
                        if (!BiometricHelper.isAvailable(requireContext())) {
                            render();
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.biometric_unlock)
                                    .setMessage(R.string.biometric_not_available)
                                    .setPositiveButton(R.string.done, null)
                                    .show();
                            return;
                        }
                        settings.setBiometricEnabled(true);
                    } else {
                        settings.setBiometricEnabled(false);
                    }
                });

        switchRow(binding.notifBudgetRow, R.string.notif_budget, R.string.notif_budget_summary,
                settings.isBudgetNotificationsEnabled(), (button, checked) -> {
                    settings.setBudgetNotificationsEnabled(checked);
                    ensureNotificationsAllowed(checked);
                });
        switchRow(binding.notifRecurringRow, R.string.notif_recurring,
                R.string.notif_recurring_summary, settings.isRecurringRemindersEnabled(),
                (button, checked) -> {
                    settings.setRecurringRemindersEnabled(checked);
                    ensureNotificationsAllowed(checked);
                });
        switchRow(binding.notifSavingsRow, R.string.notif_savings,
                R.string.notif_savings_summary, settings.isSavingsRemindersEnabled(),
                (button, checked) -> {
                    settings.setSavingsRemindersEnabled(checked);
                    ensureNotificationsAllowed(checked);
                });
        switchRow(binding.notifMonthlyRow, R.string.notif_monthly,
                R.string.notif_monthly_summary, settings.isMonthlySummaryEnabled(),
                (button, checked) -> {
                    settings.setMonthlySummaryEnabled(checked);
                    ensureNotificationsAllowed(checked);
                });

        valueRow(binding.dataRow, R.string.backup_restore, R.string.more_data_subtitle, "");
        binding.dataRow.chevron.setVisibility(View.VISIBLE);

        binding.clearRow.title.setText(R.string.clear_data);
        binding.clearRow.subtitle.setText(R.string.clear_data_summary);
        binding.clearRow.subtitle.setVisibility(View.VISIBLE);
        binding.clearRow.value.setVisibility(View.GONE);
        binding.clearRow.switchWidget.setVisibility(View.GONE);
        binding.clearRow.chevron.setVisibility(View.VISIBLE);
        binding.clearRow.title.setTextColor(UiUtils.color(requireContext(), R.color.expense));

        valueRow(binding.aboutRow, R.string.about, R.string.about_data_storage, "");
        binding.aboutRow.chevron.setVisibility(View.VISIBLE);
    }

    private void valueRow(ItemSettingBinding row, int title, int subtitle, String value) {
        row.title.setText(title);
        row.subtitle.setVisibility(subtitle == 0 ? View.GONE : View.VISIBLE);
        if (subtitle != 0) {
            row.subtitle.setText(subtitle);
        }
        row.value.setVisibility(value == null || value.isEmpty() ? View.GONE : View.VISIBLE);
        row.value.setText(value);
        row.switchWidget.setVisibility(View.GONE);
        row.chevron.setVisibility(View.VISIBLE);
    }

    private void switchRow(ItemSettingBinding row, int title, int subtitle, boolean checked,
                           android.widget.CompoundButton.OnCheckedChangeListener listener) {
        row.title.setText(title);
        row.subtitle.setText(subtitle);
        row.subtitle.setVisibility(View.VISIBLE);
        row.value.setVisibility(View.GONE);
        row.chevron.setVisibility(View.GONE);
        row.switchWidget.setVisibility(View.VISIBLE);
        row.switchWidget.setOnCheckedChangeListener(null);
        row.switchWidget.setChecked(checked);
        row.switchWidget.setEnabled(true);
        row.switchWidget.setOnCheckedChangeListener(listener);
    }

    private String themeLabel(int mode) {
        switch (mode) {
            case SettingsRepository.THEME_LIGHT:
                return getString(R.string.theme_light);
            case SettingsRepository.THEME_DARK:
                return getString(R.string.theme_dark);
            case SettingsRepository.THEME_SYSTEM:
            default:
                return getString(R.string.theme_system);
        }
    }

    private void pickAccent() {
        int[] accents = {SettingsRepository.ACCENT_BLUE, SettingsRepository.ACCENT_EMERALD,
                SettingsRepository.ACCENT_VIOLET, SettingsRepository.ACCENT_AMBER};
        String[] labels = {getString(R.string.accent_blue), getString(R.string.accent_emerald),
                getString(R.string.accent_violet), getString(R.string.accent_amber)};
        int checked = 0;
        for (int i = 0; i < accents.length; i++) {
            if (accents[i] == settings.getAccent()) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.accent_color)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    settings.setAccent(accents[which]);
                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String accentLabel(int accent) {
        switch (accent) {
            case SettingsRepository.ACCENT_EMERALD:
                return getString(R.string.accent_emerald);
            case SettingsRepository.ACCENT_VIOLET:
                return getString(R.string.accent_violet);
            case SettingsRepository.ACCENT_AMBER:
                return getString(R.string.accent_amber);
            case SettingsRepository.ACCENT_BLUE:
            default:
                return getString(R.string.accent_blue);
        }
    }

    private String languageLabel() {
        return LanguagePicker.label(requireContext(), LanguageHelper.currentLanguage());
    }

    private void pickLanguage() {
        LanguagePicker.show(requireContext(), LanguageHelper::apply);
    }

    private String autoLockLabel(int minutes) {
        switch (minutes) {
            case SettingsRepository.AUTO_LOCK_NEVER:
                return getString(R.string.auto_lock_never);
            case SettingsRepository.AUTO_LOCK_ONE_MINUTE:
                return getString(R.string.auto_lock_1_min);
            case SettingsRepository.AUTO_LOCK_FIVE_MINUTES:
                return getString(R.string.auto_lock_5_min);
            case SettingsRepository.AUTO_LOCK_IMMEDIATELY:
            default:
                return getString(R.string.auto_lock_immediately);
        }
    }

    private void editName() {
        EditText input = new EditText(requireContext());
        input.setText(settings.getUserName());
        input.setHint(R.string.your_name_hint);
        input.setSelectAllOnFocus(true);
        int padding = UiUtils.dp(requireContext(), 20);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.your_name)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    settings.setUserName(input.getText().toString().trim());
                    render();
                })
                .show();
    }

    private void pickCurrency() {
        CurrencyPicker.show(requireContext(), settings.getCurrency(), code -> {
            settings.setCurrency(code);
            render();
            requireActivity().recreate();
        });
    }

    private void pickTheme() {
        int[] modes = {SettingsRepository.THEME_SYSTEM, SettingsRepository.THEME_LIGHT,
                SettingsRepository.THEME_DARK};
        String[] labels = {getString(R.string.theme_system), getString(R.string.theme_light),
                getString(R.string.theme_dark)};
        int checked = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == settings.getThemeMode()) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    settings.setThemeMode(modes[which]);
                    ThemeHelper.apply(modes[which]);
                    dialog.dismiss();
                    render();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void changePin() {
        if (!settings.hasPin()) {
            return;
        }
        showPinDialog(true, () -> {
            render();
            Toast.makeText(requireContext(), R.string.pin_updated, Toast.LENGTH_SHORT).show();
        });
    }

    private void applyScreenshotPolicy(boolean block) {
        if (block) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void pickAutoLock() {
        if (!settings.hasPin()) {
            Toast.makeText(requireContext(), R.string.app_lock_summary, Toast.LENGTH_SHORT).show();
            return;
        }
        int[] values = {SettingsRepository.AUTO_LOCK_IMMEDIATELY,
                SettingsRepository.AUTO_LOCK_ONE_MINUTE,
                SettingsRepository.AUTO_LOCK_FIVE_MINUTES,
                SettingsRepository.AUTO_LOCK_NEVER};
        String[] labels = {getString(R.string.auto_lock_immediately),
                getString(R.string.auto_lock_1_min), getString(R.string.auto_lock_5_min),
                getString(R.string.auto_lock_never)};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == settings.getAutoLockMinutes()) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auto_lock)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    settings.setAutoLockMinutes(values[which]);
                    dialog.dismiss();
                    render();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showPinDialog(boolean requireCurrent, Runnable onSuccess) {
        DialogPinBinding pin = DialogPinBinding.inflate(getLayoutInflater());
        pin.currentLayout.setVisibility(requireCurrent ? View.VISIBLE : View.GONE);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(requireCurrent ? R.string.change_pin : R.string.pin_setup_title)
                .setView(pin.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    pin.currentLayout.setError(null);
                    pin.newLayout.setError(null);
                    pin.confirmLayout.setError(null);
                    if (requireCurrent) {
                        String current = text(pin.currentInput.getText());
                        if (!settings.verifyPin(current)) {
                            pin.currentLayout.setError(getString(R.string.pin_incorrect));
                            return;
                        }
                    }
                    String fresh = text(pin.newInput.getText());
                    if (!fresh.matches("\\d{4,8}")) {
                        pin.newLayout.setError(getString(R.string.pin_length_error));
                        return;
                    }
                    if (!fresh.equals(text(pin.confirmInput.getText()))) {
                        pin.confirmLayout.setError(getString(R.string.pin_mismatch));
                        return;
                    }
                    if (!settings.setPin(fresh)) {
                        UiUtils.snackbar(binding.getRoot(), getString(R.string.error_generic));
                        return;
                    }
                    dialog.dismiss();
                    onSuccess.run();
                }));
        dialog.show();
    }

    private String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private void ensureNotificationsAllowed(boolean checked) {
        if (!checked || NotificationHelper.canNotify(requireContext())) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.notifications)
                .setMessage(R.string.notifications_permission_needed)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.notifications_permission_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE,
                                    requireContext().getPackageName());
                    startActivity(intent);
                })
                .show();
    }

    private void confirmClearData() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_data_title)
                .setMessage(R.string.clear_data_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_data_confirm, (dialog, which) -> {
                    binding.clearRow.getRoot().setEnabled(false);
                    repository.clearAllData(new FinanceRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            repository.seedIfNeeded(requireContext(), settings.getCurrency(), () -> {
                                if (binding == null) {
                                    return;
                                }
                                binding.clearRow.getRoot().setEnabled(true);
                                Toast.makeText(requireContext(), R.string.data_cleared,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            if (binding == null) {
                                return;
                            }
                            binding.clearRow.getRoot().setEnabled(true);
                            UiUtils.snackbar(binding.getRoot(), message);
                        }
                    });
                })
                .show();
    }

    private void navigate(int destination) {
        NavHostFragment.findNavController(this).navigate(destination);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
