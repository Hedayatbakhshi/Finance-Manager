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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.databinding.FragmentDataBinding;
import com.financemanager.app.databinding.ItemMoreBinding;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.BackupManager;
import com.financemanager.app.utils.CsvExporter;
import com.financemanager.app.utils.ReportExporter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Export, back up, restore and clear local records. */
public class DataFragment extends Fragment {

    private FragmentDataBinding binding;
    private DataViewModel viewModel;
    private boolean busy;

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

    private final ActivityResultLauncher<String> createBackup = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    runBackup(uri);
                }
            });

    private final ActivityResultLauncher<String[]> openBackup = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    inspectBackup(uri);
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
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        addRow(binding.exportContainer, R.drawable.ic_action_export, R.string.export_csv,
                R.string.export_csv_summary, () -> createCsv.launch(CsvExporter.suggestedFileName()));
        addRow(binding.exportContainer, R.drawable.ic_nav_reports, R.string.export_report,
                R.string.export_report_summary,
                () -> createReport.launch(ReportExporter.suggestedFileName()));
        addRow(binding.backupContainer, R.drawable.ic_action_backup, R.string.backup_data,
                R.string.backup_summary,
                () -> createBackup.launch(BackupManager.suggestedFileName()));
        addRow(binding.backupContainer, R.drawable.ic_action_data, R.string.restore_data,
                R.string.restore_summary, () -> openBackup.launch(new String[]{
                        "application/json", "text/plain", "application/octet-stream"}));

        binding.clearData.setOnClickListener(v -> confirmClear());
    }

    private void addRow(LinearLayout container, @DrawableRes int icon,
                        @StringRes int title, @StringRes int subtitle, Runnable action) {
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

    private void runBackup(Uri uri) {
        setBusy(true);
        viewModel.backup(uri, new FinanceRepository.Callback<BackupManager.BackupInfo>() {
            @Override
            public void onSuccess(BackupManager.BackupInfo info) {
                setBusy(false);
                Toast.makeText(requireContext(), R.string.backup_success,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(R.string.backup_failed);
            }
        });
    }

    private void inspectBackup(Uri uri) {
        setBusy(true);
        viewModel.inspect(uri, new FinanceRepository.Callback<BackupManager.BackupInfo>() {
            @Override
            public void onSuccess(BackupManager.BackupInfo info) {
                setBusy(false);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.restore_title)
                        .setMessage(getString(R.string.restore_message, info.accounts,
                                info.categories, info.transactions, info.budgets, info.goals))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.restore_confirm,
                                (dialog, which) -> restore(uri))
                        .show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(R.string.restore_failed);
            }
        });
    }

    private void restore(Uri uri) {
        setBusy(true);
        viewModel.restore(uri, new FinanceRepository.Callback<BackupManager.BackupInfo>() {
            @Override
            public void onSuccess(BackupManager.BackupInfo info) {
                setBusy(false);
                Toast.makeText(requireContext(), R.string.restore_success,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(R.string.restore_failed);
            }
        });
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
