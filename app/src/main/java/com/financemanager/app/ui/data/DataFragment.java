package com.financemanager.app.ui.data;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.databinding.DialogBackupPasswordBinding;
import com.financemanager.app.databinding.FragmentDataBinding;
import com.financemanager.app.databinding.ItemMoreBinding;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.BackupManager;
import com.financemanager.app.utils.CsvExporter;
import com.financemanager.app.utils.CsvImporter;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.PdfStatementExporter;
import com.financemanager.app.utils.ReportExporter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Export, import, back up, restore and clear local records. */
public class DataFragment extends Fragment {

    private FragmentDataBinding binding;
    private DataViewModel viewModel;
    private SettingsRepository settings;
    private ItemMoreBinding backupRow;
    private boolean busy;
    private String pendingPassword;

    private final ActivityResultLauncher<String> createCsv = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    runExportCsv(uri);
                }
            });

    private final ActivityResultLauncher<String> createReport = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/plain"),
            uri -> {
                if (uri != null) {
                    runExportReport(uri);
                }
            });

    private final ActivityResultLauncher<String> createStatement = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> {
                if (uri != null) {
                    runExportStatement(uri);
                }
            });

    private final ActivityResultLauncher<String> createBackup = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    runBackup(uri);
                } else {
                    pendingPassword = null;
                }
            });

    private final ActivityResultLauncher<String> createEncryptedBackup = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> {
                if (uri != null) {
                    runBackup(uri);
                } else {
                    pendingPassword = null;
                }
            });

    private final ActivityResultLauncher<String[]> openBackup = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    beginInspect(uri, null);
                }
            });

    private final ActivityResultLauncher<String[]> openCsv = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    runImport(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(DataViewModel.class);
        settings = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings();
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        addRow(binding.exportContainer, R.drawable.ic_action_export, R.string.export_csv,
                R.string.export_csv_summary, () -> createCsv.launch(CsvExporter.suggestedFileName()));
        addRow(binding.exportContainer, R.drawable.ic_nav_reports, R.string.export_report,
                R.string.export_report_summary,
                () -> createReport.launch(ReportExporter.suggestedFileName()));
        addRow(binding.exportContainer, R.drawable.ic_action_download, R.string.export_statement,
                R.string.export_statement_summary,
                () -> createStatement.launch(PdfStatementExporter.suggestedFileName()));
        addRow(binding.exportContainer, R.drawable.ic_action_upload, R.string.import_csv,
                R.string.import_csv_summary, () -> openCsv.launch(new String[]{
                        "text/csv", "text/comma-separated-values", "text/plain",
                        "application/csv", "application/octet-stream"}));
        backupRow = addRow(binding.backupContainer, R.drawable.ic_action_backup,
                R.string.backup_data, R.string.backup_summary,
                this::showBackupPasswordDialog);
        renderLastBackup();
        addRow(binding.backupContainer, R.drawable.ic_action_data, R.string.restore_data,
                R.string.restore_summary, () -> openBackup.launch(new String[]{
                        "application/json", "text/plain", "application/octet-stream"}));

        binding.clearData.setOnClickListener(v -> confirmClear());
    }

    private ItemMoreBinding addRow(LinearLayout container, @DrawableRes int icon,
                                   @StringRes int title, @StringRes int subtitle,
                                   Runnable action) {
        ItemMoreBinding row = ItemMoreBinding.inflate(getLayoutInflater(), container, false);
        row.icon.setImageResource(icon);
        row.title.setText(title);
        row.subtitle.setText(subtitle);
        UiUtils.tintIconTile(row.iconContainer, row.icon, "#5B7FA6");
        row.getRoot().setOnClickListener(v -> {
            if (!busy) {
                action.run();
            }
        });
        container.addView(row.getRoot());
        return row;
    }

    private void renderLastBackup() {
        if (backupRow == null) {
            return;
        }
        long lastBackup = settings.getLastBackupAt();
        backupRow.subtitle.setText(lastBackup > 0
                ? getString(R.string.last_backup,
                        DateUtils.formatTimestamp(lastBackup))
                : getString(R.string.backup_summary));
    }

    private void runExportCsv(Uri uri) {
        setBusy(true);
        viewModel.exportCsv(uri, new FinanceRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer rows) {
                setBusy(false);
                Toast.makeText(requireContext(), R.string.export_success,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(R.string.export_failed);
            }
        });
    }

    private void runExportReport(Uri uri) {
        setBusy(true);
        viewModel.exportReport(uri, new FinanceRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer bytes) {
                setBusy(false);
                Toast.makeText(requireContext(), R.string.export_success,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(R.string.export_failed);
            }
        });
    }

    private void runExportStatement(Uri uri) {
        setBusy(true);
        viewModel.exportStatement(uri, new FinanceRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer value) {
                setBusy(false);
                Toast.makeText(requireContext(), R.string.statement_success,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(R.string.statement_failed);
            }
        });
    }

    private void runImport(Uri uri) {
        setBusy(true);
        viewModel.importCsv(uri, new FinanceRepository.Callback<CsvImporter.Result>() {
            @Override
            public void onSuccess(CsvImporter.Result result) {
                setBusy(false);
                if (binding == null) {
                    return;
                }
                Toast.makeText(requireContext(), getString(R.string.import_success,
                        result.imported, result.skipped), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(R.string.import_failed);
            }
        });
    }

    private void showBackupPasswordDialog() {
        if (busy || !isAdded()) {
            return;
        }
        DialogBackupPasswordBinding dialogBinding =
                DialogBackupPasswordBinding.inflate(getLayoutInflater());
        dialogBinding.message.setText(R.string.backup_password_message);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_password_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String password = password(dialogBinding);
                    pendingPassword = password.isEmpty() ? null : password;
                    if (pendingPassword == null) {
                        createBackup.launch(BackupManager.suggestedFileName());
                    } else {
                        createEncryptedBackup.launch(BackupManager.suggestedFileName(true));
                    }
                })
                .show();
    }

    private void runBackup(Uri uri) {
        setBusy(true);
        viewModel.backup(uri, pendingPassword,
                new FinanceRepository.Callback<BackupManager.BackupInfo>() {
                    @Override
                    public void onSuccess(BackupManager.BackupInfo info) {
                        setBusy(false);
                        pendingPassword = null;
                        settings.setLastBackupAt(System.currentTimeMillis());
                        renderLastBackup();
                        Toast.makeText(requireContext(), R.string.backup_success,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        setBusy(false);
                        pendingPassword = null;
                        showError(R.string.backup_failed);
                    }
                });
    }

    private void beginInspect(Uri uri, String password) {
        setBusy(true);
        viewModel.inspect(uri, password,
                new FinanceRepository.Callback<BackupManager.BackupInfo>() {
                    @Override
                    public void onSuccess(BackupManager.BackupInfo info) {
                        setBusy(false);
                        pendingPassword = password;
                        showRestoreModeDialog(uri, password, info);
                    }

                    @Override
                    public void onError(String message) {
                        setBusy(false);
                        if (BackupManager.ERROR_PASSWORD_REQUIRED.equals(message)) {
                            promptRestorePassword(uri, false);
                        } else if (BackupManager.ERROR_WRONG_PASSWORD.equals(message)) {
                            promptRestorePassword(uri, true);
                        } else {
                            showError(R.string.restore_failed);
                        }
                    }
                });
    }

    private void promptRestorePassword(Uri uri, boolean wrongPassword) {
        if (!isAdded()) {
            return;
        }
        DialogBackupPasswordBinding dialogBinding =
                DialogBackupPasswordBinding.inflate(getLayoutInflater());
        String message = (wrongPassword ? getString(R.string.wrong_password) + "\n\n" : "")
                + getString(R.string.restore_password_message);
        dialogBinding.message.setText(message);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.restore_password_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.unlock, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String password = password(dialogBinding);
                    if (password.isEmpty()) {
                        dialogBinding.passwordLayout.setError(
                                getString(R.string.password_required));
                        return;
                    }
                    dialog.dismiss();
                    beginInspect(uri, password);
                }));
        dialog.show();
    }

    private void showRestoreModeDialog(Uri uri, String password,
                                       BackupManager.BackupInfo info) {
        if (!isAdded()) {
            return;
        }
        String message = getString(R.string.restore_message, info.accounts,
                info.categories, info.transactions, info.budgets, info.goals)
                + "\n\n" + getString(R.string.restore_mode_hint);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.restore_title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.restore_merge,
                        (dialog, which) -> restore(uri, password, false))
                .setPositiveButton(R.string.restore_replace,
                        (dialog, which) -> confirmReplace(uri, password, info))
                .show();
    }

    private void confirmReplace(Uri uri, String password, BackupManager.BackupInfo info) {
        if (!isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.replace_confirm_title)
                .setMessage(getString(R.string.replace_confirm_message, info.transactions))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.replace_confirm, (dialog, which) ->
                        restore(uri, password, true))
                .show();
    }

    private void restore(Uri uri, String password, boolean replace) {
        setBusy(true);
        viewModel.restore(uri, password, replace,
                new FinanceRepository.Callback<BackupManager.BackupInfo>() {
                    @Override
                    public void onSuccess(BackupManager.BackupInfo info) {
                        setBusy(false);
                        pendingPassword = null;
                        Toast.makeText(requireContext(), R.string.restore_success,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        setBusy(false);
                        if (BackupManager.ERROR_PASSWORD_REQUIRED.equals(message)) {
                            promptRestorePassword(uri, false);
                        } else if (BackupManager.ERROR_WRONG_PASSWORD.equals(message)) {
                            promptRestorePassword(uri, true);
                        } else {
                            showError(R.string.restore_failed);
                        }
                    }
                });
    }

    private String password(DialogBackupPasswordBinding dialogBinding) {
        if (dialogBinding.passwordInput.getText() == null) {
            return "";
        }
        return dialogBinding.passwordInput.getText().toString().trim();
    }

    private void confirmClear() {
        if (busy) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_data_title)
                .setMessage(R.string.clear_data_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_data_confirm, (dialog, which) -> {
                    setBusy(true);
                    viewModel.clearAll(new FinanceRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            setBusy(false);
                            Toast.makeText(requireContext(), R.string.data_cleared,
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            setBusy(false);
                            showError(R.string.error_generic);
                        }
                    });
                })
                .show();
    }

    private void setBusy(boolean value) {
        busy = value;
        if (binding == null) {
            return;
        }
        binding.progress.setVisibility(value ? View.VISIBLE : View.GONE);
        binding.exportContainer.setAlpha(value ? 0.5f : 1f);
        binding.backupContainer.setAlpha(value ? 0.5f : 1f);
        binding.clearData.setEnabled(!value);
    }

    private void showError(int message) {
        if (binding != null) {
            UiUtils.snackbar(binding.getRoot(), getString(message));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
