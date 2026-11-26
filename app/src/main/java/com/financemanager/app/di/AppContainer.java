package com.financemanager.app.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.notifications.BudgetAlertChecker;
import com.financemanager.app.notifications.NotificationHelper;
import com.financemanager.app.security.SessionManager;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.utils.BackupManager;
import com.financemanager.app.utils.CsvExporter;
import com.financemanager.app.utils.CsvImporter;
import com.financemanager.app.utils.PdfStatementExporter;
import com.financemanager.app.utils.ReportExporter;

/** Manual dependency graph, created once by the Application class. */
public class AppContainer {

    private final FinanceDatabase database;
    private final FinanceRepository repository;
    private final SettingsRepository settings;
    private final SessionManager sessionManager;
    private final NotificationHelper notificationHelper;
    private final BackupManager backupManager;
    private final CsvExporter csvExporter;
    private final CsvImporter csvImporter;
    private final ReportExporter reportExporter;
    private final PdfStatementExporter pdfStatementExporter;

    public AppContainer(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        database = FinanceDatabase.getInstance(appContext);
        settings = new SettingsRepository(appContext);
        sessionManager = new SessionManager();
        notificationHelper = new NotificationHelper(appContext);
        repository = new FinanceRepository(database, appContext);
        backupManager = new BackupManager(appContext, database);
        csvExporter = new CsvExporter(appContext, database);
        csvImporter = new CsvImporter(appContext, database);
        reportExporter = new ReportExporter(database);
        pdfStatementExporter = new PdfStatementExporter(appContext, database);
        // Keep budget alerts in sync whenever money moves.
        repository.setDataChangeListener(() -> BudgetAlertChecker.checkAll(
                appContext, database, settings, notificationHelper));
    }

    public FinanceDatabase database() {
        return database;
    }

    public FinanceRepository repository() {
        return repository;
    }

    public SettingsRepository settings() {
        return settings;
    }

    public SessionManager sessionManager() {
        return sessionManager;
    }

    public NotificationHelper notifications() {
        return notificationHelper;
    }

    public BackupManager backupManager() {
        return backupManager;
    }

    public CsvExporter csvExporter() {
        return csvExporter;
    }

    public CsvImporter csvImporter() {
        return csvImporter;
    }

    public ReportExporter reportExporter() {
        return reportExporter;
    }

    public PdfStatementExporter pdfStatementExporter() {
        return pdfStatementExporter;
    }
}
