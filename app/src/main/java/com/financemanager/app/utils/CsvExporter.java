package com.financemanager.app.utils;

import android.content.Context;
import android.net.Uri;

import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.models.TransactionWithDetails;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Writes all transactions to a CSV file chosen through the Storage Access Framework.
 * Written on a background thread; the caller owns threading.
 */
public class CsvExporter {

    private final Context context;
    private final FinanceDatabase database;

    public CsvExporter(Context context, FinanceDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
    }

    public static String suggestedFileName() {
        return "finance-manager-transactions-" + LocalDate.now() + ".csv";
    }

    public static String suggestedHouseholdFileName() {
        return "finance-manager-household-" + LocalDate.now() + ".csv";
    }

    /** Writes the CSV and returns the number of rows written. */
    public int export(Uri uri) throws IOException {
        List<TransactionWithDetails> rows = database.transactionDao().getAllDetailsSync();
        try (OutputStream stream = context.getContentResolver().openOutputStream(uri, "w")) {
            if (stream == null) {
                throw new IOException("Could not open the selected file");
            }
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(stream, StandardCharsets.UTF_8));
            writer.write("Date,Title,Type,Category,Account,Amount,Currency,Description,To Account,Notes");
            writer.newLine();
            for (TransactionWithDetails row : rows) {
                writer.write(csvLine(row));
                writer.newLine();
            }
            writer.flush();
        }
        return rows.size();
    }

    /** Writes household expenses for one currency only — never mixes currencies. */
    public int exportHousehold(Uri uri, long householdId, long fromDate, long toDate,
                               String currency) throws IOException {
        List<HouseholdExpense> rows = database.householdDao().getExpensesForExport(
                householdId, fromDate, toDate, currency);
        try (OutputStream stream = context.getContentResolver().openOutputStream(uri, "w")) {
            if (stream == null) {
                throw new IOException("Could not open the selected file");
            }
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(stream, StandardCharsets.UTF_8));
            writer.write("Date,Title,Category,Account,Amount,Currency,Member,Description,Notes");
            writer.newLine();
            for (HouseholdExpense row : rows) {
                TransactionWithDetails details = row.transaction;
                StringBuilder line = new StringBuilder();
                line.append(escape(DateUtils.formatIsoDate(details.transaction.date))).append(',');
                line.append(escape(details.transaction.title)).append(',');
                line.append(escape(details.categoryName == null ? "" : details.categoryName)).append(',');
                line.append(escape(details.accountName == null ? "" : details.accountName)).append(',');
                line.append(BigDecimal.valueOf(details.transaction.amountMinor, 2)
                        .toPlainString()).append(',');
                line.append(escape(details.accountCurrency == null
                        ? currency : details.accountCurrency)).append(',');
                line.append(escape(row.memberName == null ? "" : row.memberName)).append(',');
                line.append(escape(details.transaction.description)).append(',');
                line.append(escape(details.transaction.notes));
                writer.write(line.toString());
                writer.newLine();
            }
            writer.flush();
        }
        return rows.size();
    }

    private String csvLine(TransactionWithDetails row) {
        StringBuilder builder = new StringBuilder();
        builder.append(escape(DateUtils.formatIsoDate(row.transaction.date))).append(',');
        builder.append(escape(row.transaction.title)).append(',');
        builder.append(escape(typeLabel(row.transaction.type))).append(',');
        builder.append(escape(row.categoryName == null ? "" : row.categoryName)).append(',');
        builder.append(escape(row.accountName == null ? "" : row.accountName)).append(',');
        builder.append(BigDecimal.valueOf(row.transaction.amountMinor, 2).toPlainString()).append(',');
        builder.append(escape(row.accountCurrency == null ? "" : row.accountCurrency)).append(',');
        builder.append(escape(row.transaction.description)).append(',');
        builder.append(escape(row.toAccountName == null ? "" : row.toAccountName)).append(',');
        builder.append(escape(row.transaction.notes));
        return builder.toString();
    }

    private String typeLabel(TransactionType type) {
        switch (type) {
            case INCOME:
                return "Income";
            case EXPENSE:
                return "Expense";
            case TRANSFER:
            default:
                return "Transfer";
        }
    }

    /** RFC 4180 escaping: quote fields containing separators, quotes or newlines. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
