package com.financemanager.app.ui.data;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.di.AppContainer;
import com.financemanager.app.utils.BackupManager;
import com.financemanager.app.utils.CsvExporter;
import com.financemanager.app.utils.CsvImporter;
import com.financemanager.app.utils.PdfStatementExporter;
import com.financemanager.app.utils.ReportExporter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Runs exports, backups and restores on the repository's background thread. */
public class DataViewModel extends ViewModel {

    private final Context context;
    private final FinanceRepository repository;
    private final CsvExporter csvExporter;
    private final CsvImporter csvImporter;
    private final ReportExporter reportExporter;
    private final PdfStatementExporter pdfStatementExporter;
    private final BackupManager backupManager;
    private final String currency;

    public DataViewModel(@NonNull Context context, @NonNull AppContainer container) {
        this.context = context.getApplicationContext();
        this.repository = container.repository();
        this.csvExporter = container.csvExporter();
        this.csvImporter = container.csvImporter();
        this.reportExporter = container.reportExporter();
        this.pdfStatementExporter = container.pdfStatementExporter();
        this.backupManager = container.backupManager();
        this.currency = container.settings().getCurrency();
    }

    public String currency() {
        return currency;
    }

    public void exportCsv(Uri uri, FinanceRepository.Callback<Integer> callback) {
        repository.runAsync(() -> csvExporter.export(uri), callback);
    }

    public void exportReport(Uri uri, FinanceRepository.Callback<Integer> callback) {
        repository.runAsync(() -> {
            String text = reportExporter.build(currency);
            try (OutputStream stream = context.getContentResolver().openOutputStream(uri, "w")) {
                if (stream == null) {
                    throw new IOException("Could not open the selected file");
                }
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes);
                stream.flush();
                return bytes.length;
            }
        }, callback);
    }

    public void exportStatement(Uri uri, FinanceRepository.Callback<Integer> callback) {
        repository.runAsync(() -> pdfStatementExporter.export(
                uri, context.getContentResolver(), currency), callback);
    }

    public void backup(Uri uri, String password,
                       FinanceRepository.Callback<BackupManager.BackupInfo> callback) {
        repository.runAsync(() -> backupManager.writeBackup(uri, password), callback);
    }

    public void inspect(Uri uri, String password,
                        FinanceRepository.Callback<BackupManager.BackupInfo> callback) {
        repository.runAsync(() -> backupManager.readInfo(uri, password), callback);
    }

    public void restore(Uri uri, String password, boolean replace,
                        FinanceRepository.Callback<BackupManager.BackupInfo> callback) {
        repository.runAsync(() -> backupManager.restore(uri, password, replace), callback);
    }

    public void importCsv(Uri uri, FinanceRepository.Callback<CsvImporter.Result> callback) {
        repository.runAsync(() -> csvImporter.importCsv(uri, currency), callback);
    }

    public void clearAll(FinanceRepository.Callback<Void> callback) {
        repository.clearAllData(new FinanceRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                repository.seedIfNeeded(context, currency, () -> callback.onSuccess(null));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
