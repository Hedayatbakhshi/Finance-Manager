package com.financemanager.app.utils;

import android.content.Context;
import android.net.Uri;

import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.TransactionType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Imports transactions from the CSV format written by {@link CsvExporter}.
 *
 * <p>Accounts are matched by name and created when missing; categories are matched by
 * name and type and likewise created when missing. A transfer without a valid
 * destination account is skipped. The whole file is inserted in one transaction, so a
 * malformed file cannot leave half of the rows behind.</p>
 *
 * <p>Import is additive: importing the same file twice duplicates transactions. That
 * matches how restores behave and keeps the importer predictable.</p>
 */
public class CsvImporter {

    public static class Result {
        public int imported;
        public int skipped;
        public int accountsCreated;
        public int categoriesCreated;
    }

    private final Context context;
    private final FinanceDatabase database;

    public CsvImporter(Context context, FinanceDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
    }

    public Result importCsv(Uri uri, String defaultCurrency) throws IOException {
        String text;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IOException("Could not read the selected file");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
                if (buffer.size() > 32 * 1024 * 1024) {
                    throw new IOException("CSV file is too large");
                }
            }
            text = buffer.toString(StandardCharsets.UTF_8.name());
        }

        List<List<String>> rows = CsvImportParser.parse(text);
        if (rows.size() < 2) {
            throw new IOException("This CSV file contains no transactions");
        }
        List<String> header = rows.get(0);
        int dateColumn = CsvImportParser.indexOf(header, "date");
        int titleColumn = CsvImportParser.indexOf(header, "title");
        int typeColumn = CsvImportParser.indexOf(header, "type");
        int categoryColumn = CsvImportParser.indexOf(header, "category");
        int accountColumn = CsvImportParser.indexOf(header, "account");
        int amountColumn = CsvImportParser.indexOf(header, "amount");
        int currencyColumn = CsvImportParser.indexOf(header, "currency");
        int descriptionColumn = CsvImportParser.indexOf(header, "description");
        int toAccountColumn = CsvImportParser.indexOf(header, "to account");
        int notesColumn = CsvImportParser.indexOf(header, "notes");

        if (dateColumn < 0 || titleColumn < 0 || typeColumn < 0
                || accountColumn < 0 || amountColumn < 0) {
            throw new IOException("This CSV file is missing required columns");
        }

        String fallbackCurrency = Money.isValidCurrencyCode(defaultCurrency)
                ? defaultCurrency.toUpperCase() : "USD";
        Result result = new Result();
        try {
            database.runInTransaction(() -> {
                for (int i = 1; i < rows.size(); i++) {
                    importRow(rows.get(i), result, fallbackCurrency,
                            dateColumn, titleColumn, typeColumn, categoryColumn,
                            accountColumn, amountColumn, currencyColumn,
                            descriptionColumn, toAccountColumn, notesColumn);
                }
            });
        } catch (RuntimeException e) {
            throw new IOException("Could not import this CSV file", e);
        }
        return result;
    }

    private void importRow(List<String> row, Result result, String fallbackCurrency,
                           int dateColumn, int titleColumn, int typeColumn,
                           int categoryColumn, int accountColumn, int amountColumn,
                           int currencyColumn, int descriptionColumn,
                           int toAccountColumn, int notesColumn) {
        TransactionType type = CsvImportParser.parseType(value(row, typeColumn));
        Long amount = CsvImportParser.parseAmount(value(row, amountColumn));
        long date = CsvImportParser.parseDate(value(row, dateColumn));
        String accountName = value(row, accountColumn).trim();
        if (type == null || amount == null || date <= 0 || accountName.isEmpty()) {
            result.skipped++;
            return;
        }

        String rowCurrency = value(row, currencyColumn).trim().toUpperCase();
        String currency = Money.isValidCurrencyCode(rowCurrency)
                ? rowCurrency : fallbackCurrency;
        Account account = findOrCreateAccount(accountName, currency, result);
        if (account == null) {
            result.skipped++;
            return;
        }

        TransactionEntity entity = new TransactionEntity();
        entity.type = type;
        entity.amountMinor = amount;
        entity.accountId = account.id;
        entity.date = date;
        entity.description = value(row, descriptionColumn);
        entity.notes = value(row, notesColumn);
        entity.createdAt = System.currentTimeMillis();
        entity.updatedAt = entity.createdAt;

        String title = value(row, titleColumn).trim();
        if (title.isEmpty()) {
            title = type == TransactionType.TRANSFER ? "Transfer" : "Imported transaction";
        }
        entity.title = title;

        if (type == TransactionType.TRANSFER) {
            String toAccountName = value(row, toAccountColumn).trim();
            if (toAccountName.isEmpty()) {
                result.skipped++;
                return;
            }
            Account toAccount = findOrCreateAccount(toAccountName, currency, result);
            if (toAccount == null || toAccount.id == account.id) {
                result.skipped++;
                return;
            }
            entity.toAccountId = toAccount.id;
            entity.categoryId = null;
        } else {
            entity.categoryId = findOrCreateCategory(
                    value(row, categoryColumn).trim(), type, result);
        }

        database.transactionDao().insert(entity);
        result.imported++;
    }

    private Account findOrCreateAccount(String name, String currency, Result result) {
        Account existing = database.accountDao().findByName(name);
        if (existing != null) {
            return existing;
        }
        Account account = new Account();
        account.name = name;
        account.currency = currency;
        account.createdAt = System.currentTimeMillis();
        account.updatedAt = account.createdAt;
        long id = database.accountDao().insert(account);
        result.accountsCreated++;
        account.id = id;
        return account;
    }

    private Long findOrCreateCategory(String name, TransactionType type, Result result) {
        if (name.isEmpty()) {
            Category other = database.categoryDao().findDefaultOther(type);
            if (other != null) {
                return other.id;
            }
            name = "Other";
        }
        Category existing = database.categoryDao().findByNameAndType(name, type);
        if (existing != null) {
            return existing.id;
        }
        Category category = new Category();
        category.name = name;
        category.type = type;
        category.icon = "cat_other";
        category.color = "#778397";
        category.isDefault = false;
        category.createdAt = System.currentTimeMillis();
        long id = database.categoryDao().insert(category);
        result.categoriesCreated++;
        return id;
    }

    private String value(List<String> row, int column) {
        if (column < 0 || column >= row.size()) {
            return "";
        }
        return row.get(column);
    }
}
