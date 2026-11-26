package com.financemanager.app.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.financemanager.app.BuildConfig;
import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Budget;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.HouseholdCategory;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.AccountType;
import com.financemanager.app.models.RecurrenceFrequency;
import com.financemanager.app.models.TransactionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backup format (version 2) is a single document containing every record. It can
 * be written as plain JSON or wrapped in a password-encrypted envelope (see
 * {@link BackupCrypto}); encrypted files are detected automatically on restore.
 *
 * <p>Restore supports two modes. Merge (the default) adds records with fresh ids and
 * remapped foreign keys, reusing categories with the same name. Replace deletes all
 * existing records in the same transaction first, so a backup can be restored onto a
 * clean slate without duplicating totals.</p>
 */
public class BackupManager {

    private static final String TAG = "BackupManager";
    public static final int FORMAT_VERSION = 2;

    /** Restore failed because the file is encrypted and no password was supplied. */
    public static final String ERROR_PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    /** Restore failed because the supplied password did not decrypt the file. */
    public static final String ERROR_WRONG_PASSWORD = "WRONG_PASSWORD";
    private static final String KEY_FORMAT = "format";
    private static final String KEY_VERSION = "version";
    private static final String KEY_APP_VERSION = "app_version";
    private static final String KEY_EXPORTED_AT = "exported_at";

    private final Context context;
    private final FinanceDatabase database;

    public BackupManager(Context context, FinanceDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
    }

    public static String suggestedFileName() {
        return suggestedFileName(false);
    }

    public static String suggestedFileName(boolean encrypted) {
        return "finance-manager-backup-" + LocalDate.now()
                + (encrypted ? ".fmb" : ".json");
    }

    /** Snapshot counts shown in the restore confirmation. */
    public static class BackupInfo {
        public int accounts;
        public int categories;
        public int transactions;
        public int budgets;
        public int goals;
        public int recurring;
        public int households;
        public int members;
        public int householdCategories;
    }

    public static class BackupException extends Exception {
        public BackupException(String message) {
            super(message);
        }

        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------

    public BackupInfo writeBackup(Uri uri) throws IOException, JSONException {
        return writeBackup(uri, null);
    }

    /**
     * Writes a backup. A non-empty password encrypts the whole document with
     * AES-256-GCM; otherwise the file is readable JSON.
     */
    public BackupInfo writeBackup(Uri uri, String password) throws IOException, JSONException {
        JSONObject root = buildJson();
        byte[] plaintext = root.toString(2).getBytes(StandardCharsets.UTF_8);
        byte[] payload = plaintext;
        if (password != null && !password.isEmpty()) {
            try {
                payload = BackupCrypto.encrypt(plaintext, password);
            } catch (BackupCrypto.CryptoException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        try (OutputStream stream = context.getContentResolver().openOutputStream(uri, "w")) {
            if (stream == null) {
                throw new IOException("Could not open the selected file");
            }
            stream.write(payload);
        }
        return count(root);
    }

    private JSONObject buildJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_FORMAT, "finance-manager-backup");
        root.put(KEY_VERSION, FORMAT_VERSION);
        root.put(KEY_APP_VERSION, BuildConfig.VERSION_NAME);
        root.put(KEY_EXPORTED_AT, System.currentTimeMillis());

        JSONArray accounts = new JSONArray();
        for (Account account : database.accountDao().getAllSync()) {
            JSONObject item = new JSONObject();
            item.put("id", account.id);
            item.put("name", account.name);
            item.put("type", account.type.name());
            item.put("initial_balance_minor", account.initialBalanceMinor);
            item.put("currency", account.currency);
            item.put("color", account.color);
            item.put("icon", account.icon);
            item.put("created_at", account.createdAt);
            accounts.put(item);
        }
        root.put("accounts", accounts);

        JSONArray categories = new JSONArray();
        for (Category category : database.categoryDao().getAllSync()) {
            JSONObject item = new JSONObject();
            item.put("id", category.id);
            item.put("name", category.name);
            item.put("type", category.type.name());
            item.put("icon", category.icon);
            item.put("color", category.color);
            item.put("is_default", category.isDefault);
            categories.put(item);
        }
        root.put("categories", categories);

        JSONArray households = new JSONArray();
        for (Household household : database.householdDao().getAllSync()) {
            JSONObject item = new JSONObject();
            item.put("id", household.id);
            item.put("name", household.name);
            item.put("description", household.description);
            item.put("monthly_budget_minor", household.monthlyBudgetMinor);
            item.put("currency", household.currency);
            item.put("created_at", household.createdAt);
            households.put(item);
        }
        root.put("households", households);

        JSONArray members = new JSONArray();
        for (Household memberOwner : database.householdDao().getAllSync()) {
            for (HouseholdMember member : database.householdDao()
                    .getMembersSync(memberOwner.id)) {
                JSONObject item = new JSONObject();
                item.put("id", member.id);
                item.put("household_id", member.householdId);
                item.put("name", member.name);
                item.put("created_at", member.createdAt);
                members.put(item);
            }
        }
        root.put("household_members", members);

        JSONArray householdCategories = new JSONArray();
        for (Household owner : database.householdDao().getAllSync()) {
            for (HouseholdCategory link : database.householdDao().getCategoriesSync(owner.id)) {
                JSONObject item = new JSONObject();
                item.put("id", link.id);
                item.put("household_id", link.householdId);
                item.put("category_id", link.categoryId);
                item.put("monthly_budget_minor", link.monthlyBudgetMinor);
                item.put("created_at", link.createdAt);
                householdCategories.put(item);
            }
        }
        root.put("household_categories", householdCategories);

        JSONArray transactions = new JSONArray();
        for (TransactionEntity transaction : database.transactionDao().getAllSync()) {
            JSONObject item = new JSONObject();
            item.put("id", transaction.id);
            item.put("title", transaction.title);
            item.put("amount_minor", transaction.amountMinor);
            item.put("type", transaction.type.name());
            item.put("category_id", transaction.categoryId == null ? JSONObject.NULL : transaction.categoryId);
            item.put("account_id", transaction.accountId);
            item.put("to_account_id", transaction.toAccountId == null ? JSONObject.NULL : transaction.toAccountId);
            item.put("date", transaction.date);
            item.put("description", transaction.description);
            item.put("notes", transaction.notes);
            item.put("recurring_id", transaction.recurringId == null ? JSONObject.NULL : transaction.recurringId);
            item.put("household_id", transaction.householdId == null
                    ? JSONObject.NULL : transaction.householdId);
            item.put("household_category_id", transaction.householdCategoryId == null
                    ? JSONObject.NULL : transaction.householdCategoryId);
            item.put("household_member_id", transaction.householdMemberId == null
                    ? JSONObject.NULL : transaction.householdMemberId);
            item.put("created_at", transaction.createdAt);
            transactions.put(item);
        }
        root.put("transactions", transactions);

        JSONArray budgets = new JSONArray();
        for (Budget budget : database.budgetDao().getAllSync()) {
            JSONObject item = new JSONObject();
            item.put("id", budget.id);
            item.put("name", budget.name);
            item.put("category_id", budget.categoryId);
            item.put("amount_minor", budget.amountMinor);
            item.put("start_date", budget.startDate);
            item.put("end_date", budget.endDate);
            budgets.put(item);
        }
        root.put("budgets", budgets);

        JSONArray goals = new JSONArray();
        for (SavingsGoal goal : database.savingsGoalDao().getAllSync()) {
            JSONObject item = new JSONObject();
            item.put("id", goal.id);
            item.put("name", goal.name);
            item.put("target_minor", goal.targetMinor);
            item.put("current_minor", goal.currentMinor);
            item.put("target_date", goal.targetDate);
            item.put("description", goal.description);
            item.put("icon", goal.icon);
            item.put("color", goal.color);
            goals.put(item);
        }
        root.put("savings_goals", goals);

        JSONArray recurring = new JSONArray();
        for (RecurringTransaction item : database.recurringDao().getAllSync()) {
            JSONObject json = new JSONObject();
            json.put("id", item.id);
            json.put("title", item.title);
            json.put("amount_minor", item.amountMinor);
            json.put("type", item.type.name());
            json.put("category_id", item.categoryId == null ? JSONObject.NULL : item.categoryId);
            json.put("account_id", item.accountId);
            json.put("to_account_id", item.toAccountId == null ? JSONObject.NULL : item.toAccountId);
            json.put("frequency", item.frequency.name());
            json.put("next_run_date", item.nextRunDate);
            json.put("description", item.description);
            json.put("notes", item.notes);
            json.put("household_id", item.householdId == null
                    ? JSONObject.NULL : item.householdId);
            json.put("household_category_id", item.householdCategoryId == null
                    ? JSONObject.NULL : item.householdCategoryId);
            json.put("household_member_id", item.householdMemberId == null
                    ? JSONObject.NULL : item.householdMemberId);
            json.put("is_active", item.active);
            recurring.put(json);
        }
        root.put("recurring_transactions", recurring);
        return root;
    }

    // ------------------------------------------------------------------
    // Import
    // ------------------------------------------------------------------

    public BackupInfo readInfo(Uri uri) throws BackupException {
        return readInfo(uri, null);
    }

    public BackupInfo readInfo(Uri uri, String password) throws BackupException {
        JSONObject root = readJson(uri, password);
        validate(root);
        return count(root);
    }

    public BackupInfo restore(Uri uri) throws BackupException {
        return restore(uri, null, false);
    }

    /**
     * Restores a backup on the background thread. When {@code replace} is true every
     * existing record is deleted inside the same transaction before the backup is
     * inserted, so a failure leaves the previous data untouched.
     */
    public BackupInfo restore(Uri uri, String password, boolean replace)
            throws BackupException {
        JSONObject root = readJson(uri, password);
        validate(root);
        BackupInfo info = count(root);
        try {
            database.runInTransaction(() -> {
                try {
                    if (replace) {
                        DataEraser.clearAll(database);
                    }
                    restoreInternal(root);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Restore failed", e);
            throw new BackupException("Restore failed: " + e.getMessage(), e);
        }
        return info;
    }

    private void restoreInternal(JSONObject root) throws JSONException {
        Map<Long, Long> accountMap = new HashMap<>();
        Map<Long, Long> categoryMap = new HashMap<>();
        Map<Long, Long> recurringMap = new HashMap<>();
        Map<Long, Long> householdMap = new HashMap<>();
        Map<Long, Long> memberMap = new HashMap<>();
        Map<Long, Long> householdCategoryMap = new HashMap<>();
        long now = System.currentTimeMillis();

        JSONArray accounts = root.optJSONArray("accounts");
        if (accounts != null) {
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject item = accounts.getJSONObject(i);
                String name = item.optString("name", "Restored account").trim();
                if (name.isEmpty()) {
                    name = "Restored account";
                }
                Account account = new Account();
                account.name = uniqueAccountName(name);
                account.type = AccountType.fromKey(item.optString("type", "CASH"));
                account.initialBalanceMinor = item.optLong("initial_balance_minor", 0);
                String currency = item.optString("currency", "USD").toUpperCase();
                account.currency = Money.isValidCurrencyCode(currency) ? currency : "USD";
                account.color = item.optString("color", "#3E6DB5");
                account.icon = item.optString("icon", "account_cash");
                account.createdAt = item.optLong("created_at", now);
                account.updatedAt = now;
                long newId = database.accountDao().insert(account);
                accountMap.put(item.optLong("id"), newId);
            }
        }

        JSONArray categories = root.optJSONArray("categories");
        if (categories != null) {
            for (int i = 0; i < categories.length(); i++) {
                JSONObject item = categories.getJSONObject(i);
                String name = item.optString("name", "Restored category").trim();
                TransactionType type = TransactionType.fromKey(item.optString("type", "EXPENSE"));
                Category existing = database.categoryDao().findByNameAndType(name, type);
                if (existing != null) {
                    categoryMap.put(item.optLong("id"), existing.id);
                    continue;
                }
                Category category = new Category();
                category.name = name;
                category.type = type;
                category.icon = item.optString("icon", "cat_other");
                category.color = item.optString("color", "#778397");
                category.isDefault = false;
                category.createdAt = now;
                long newId = database.categoryDao().insert(category);
                categoryMap.put(item.optLong("id"), newId);
            }
        }

        JSONArray households = root.optJSONArray("households");
        if (households != null) {
            for (int i = 0; i < households.length(); i++) {
                JSONObject item = households.getJSONObject(i);
                Household household = new Household();
                String name = item.optString("name", "Restored household").trim();
                household.name = uniqueHouseholdName(name.isEmpty() ? "Restored household" : name);
                household.description = item.optString("description", "");
                household.monthlyBudgetMinor = item.optLong("monthly_budget_minor", 0);
                String currency = item.optString("currency", "USD").toUpperCase();
                household.currency = Money.isValidCurrencyCode(currency) ? currency : "USD";
                household.createdAt = item.optLong("created_at", now);
                household.updatedAt = now;
                long newId = database.householdDao().insert(household);
                householdMap.put(item.optLong("id"), newId);
            }
        }

        JSONArray members = root.optJSONArray("household_members");
        if (members != null) {
            for (int i = 0; i < members.length(); i++) {
                JSONObject item = members.getJSONObject(i);
                Long householdId = mapId(householdMap, item.optLong("household_id", -1));
                String name = item.optString("name", "").trim();
                if (householdId == null || name.isEmpty()) {
                    continue;
                }
                HouseholdMember member = new HouseholdMember();
                member.householdId = householdId;
                member.name = name;
                member.createdAt = item.optLong("created_at", now);
                long newId = database.householdDao().insertMember(member);
                memberMap.put(item.optLong("id"), newId);
            }
        }

        JSONArray householdCategories = root.optJSONArray("household_categories");
        if (householdCategories != null) {
            for (int i = 0; i < householdCategories.length(); i++) {
                JSONObject item = householdCategories.getJSONObject(i);
                Long householdId = mapId(householdMap, item.optLong("household_id", -1));
                Long categoryId = mapId(categoryMap, item.optLong("category_id", -1));
                if (householdId == null || categoryId == null
                        || database.householdDao().getCategory(householdId, categoryId) != null) {
                    continue;
                }
                HouseholdCategory link = new HouseholdCategory();
                link.householdId = householdId;
                link.categoryId = categoryId;
                link.monthlyBudgetMinor = item.optLong("monthly_budget_minor", 0);
                link.createdAt = item.optLong("created_at", now);
                link.updatedAt = now;
                long newId = database.householdDao().insertCategory(link);
                householdCategoryMap.put(item.optLong("id"), newId);
            }
        }

        JSONArray recurring = root.optJSONArray("recurring_transactions");
        if (recurring != null) {
            for (int i = 0; i < recurring.length(); i++) {
                JSONObject item = recurring.getJSONObject(i);
                Long accountId = mapId(accountMap, item.optLong("account_id", -1));
                if (accountId == null) {
                    continue;
                }
                RecurringTransaction entity = new RecurringTransaction();
                entity.title = item.optString("title", "Recurring");
                entity.amountMinor = item.optLong("amount_minor", 0);
                entity.type = TransactionType.fromKey(item.optString("type", "EXPENSE"));
                entity.categoryId = mapId(categoryMap, item.isNull("category_id")
                        ? -1 : item.optLong("category_id"));
                entity.accountId = accountId;
                entity.toAccountId = mapId(accountMap, item.isNull("to_account_id")
                        ? -1 : item.optLong("to_account_id"));
                entity.frequency = RecurrenceFrequency.fromKey(item.optString("frequency", "MONTHLY"));
                entity.nextRunDate = item.optLong("next_run_date",
                        DateUtils.todayStart());
                entity.description = item.optString("description", "");
                entity.notes = item.optString("notes", "");
                entity.householdId = optionalMappedId(item, "household_id", householdMap);
                entity.householdCategoryId = optionalMappedId(item,
                        "household_category_id", householdCategoryMap);
                entity.householdMemberId = optionalMappedId(item,
                        "household_member_id", memberMap);
                entity.active = item.optBoolean("is_active", true);
                entity.createdAt = now;
                entity.updatedAt = now;
                if (entity.amountMinor <= 0) {
                    continue;
                }
                long newId = database.recurringDao().insert(entity);
                recurringMap.put(item.optLong("id"), newId);
            }
        }

        JSONArray transactions = root.optJSONArray("transactions");
        if (transactions != null) {
            for (int i = 0; i < transactions.length(); i++) {
                JSONObject item = transactions.getJSONObject(i);
                TransactionEntity entity = new TransactionEntity();
                entity.title = item.optString("title", "Restored transaction");
                entity.amountMinor = item.optLong("amount_minor", 0);
                entity.type = TransactionType.fromKey(item.optString("type", "EXPENSE"));
                Long accountId = mapId(accountMap, item.optLong("account_id", -1));
                if (accountId == null || entity.amountMinor <= 0) {
                    continue;
                }
                entity.accountId = accountId;
                entity.categoryId = mapId(categoryMap, item.isNull("category_id")
                        ? -1 : item.optLong("category_id"));
                entity.toAccountId = mapId(accountMap, item.isNull("to_account_id")
                        ? -1 : item.optLong("to_account_id"));
                if (entity.type == TransactionType.TRANSFER) {
                    if (entity.toAccountId == null || entity.toAccountId == entity.accountId) {
                        continue;
                    }
                    entity.categoryId = null;
                } else if (entity.categoryId == null) {
                    continue;
                }
                entity.date = item.optLong("date", now);
                entity.description = item.optString("description", "");
                entity.notes = item.optString("notes", "");
                entity.recurringId = mapId(recurringMap, item.isNull("recurring_id")
                        ? -1 : item.optLong("recurring_id"));
                entity.householdId = optionalMappedId(item, "household_id", householdMap);
                entity.householdCategoryId = optionalMappedId(item,
                        "household_category_id", householdCategoryMap);
                entity.householdMemberId = optionalMappedId(item,
                        "household_member_id", memberMap);
                entity.createdAt = item.optLong("created_at", now);
                entity.updatedAt = now;
                database.transactionDao().insert(entity);
            }
        }

        JSONArray budgets = root.optJSONArray("budgets");
        if (budgets != null) {
            for (int i = 0; i < budgets.length(); i++) {
                JSONObject item = budgets.getJSONObject(i);
                Long categoryId = mapId(categoryMap, item.optLong("category_id", -1));
                if (categoryId == null) {
                    continue;
                }
                Budget budget = new Budget();
                budget.name = item.optString("name", "Restored budget");
                budget.categoryId = categoryId;
                budget.amountMinor = item.optLong("amount_minor", 0);
                budget.startDate = item.optLong("start_date", 0);
                budget.endDate = item.optLong("end_date", 0);
                if (budget.amountMinor <= 0 || budget.startDate <= 0
                        || budget.endDate < budget.startDate) {
                    continue;
                }
                budget.createdAt = now;
                budget.updatedAt = now;
                database.budgetDao().insert(budget);
            }
        }

        JSONArray goals = root.optJSONArray("savings_goals");
        if (goals != null) {
            for (int i = 0; i < goals.length(); i++) {
                JSONObject item = goals.getJSONObject(i);
                SavingsGoal goal = new SavingsGoal();
                goal.name = item.optString("name", "Restored goal");
                goal.targetMinor = item.optLong("target_minor", 0);
                goal.currentMinor = item.optLong("current_minor", 0);
                goal.targetDate = item.optLong("target_date", 0);
                goal.description = item.optString("description", "");
                goal.icon = item.optString("icon", "goal_target");
                goal.color = item.optString("color", "#16A36A");
                if (goal.targetMinor <= 0) {
                    continue;
                }
                goal.createdAt = now;
                goal.updatedAt = now;
                database.savingsGoalDao().insert(goal);
            }
        }
    }

    private String uniqueAccountName(String base) {
        String name = base;
        int suffix = 2;
        while (database.accountDao().countByName(name, -1) > 0 && suffix < 1000) {
            name = base + " (" + suffix + ")";
            suffix++;
        }
        return name;
    }

    private Long mapId(Map<Long, Long> map, long oldId) {
        if (oldId <= 0) {
            return null;
        }
        return map.get(oldId);
    }

    /** Household links are optional and absent from backups made before version 2. */
    private Long optionalMappedId(JSONObject item, String key, Map<Long, Long> map) {
        if (item.isNull(key)) {
            return null;
        }
        return mapId(map, item.optLong(key, -1));
    }

    private String uniqueHouseholdName(String base) {
        String name = base;
        int suffix = 2;
        while (database.householdDao().countByName(name, -1) > 0 && suffix < 1000) {
            name = base + " (" + suffix + ")";
            suffix++;
        }
        return name;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private JSONObject readJson(Uri uri, String password) throws BackupException {
        byte[] bytes;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new BackupException("Could not read the selected file");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
                if (buffer.size() > 64 * 1024 * 1024) {
                    throw new BackupException("Backup file is too large");
                }
            }
            bytes = buffer.toByteArray();
        } catch (IOException e) {
            throw new BackupException("Could not read the selected file", e);
        }

        if (BackupCrypto.isEncrypted(bytes)) {
            if (password == null || password.isEmpty()) {
                throw new BackupException(ERROR_PASSWORD_REQUIRED);
            }
            try {
                bytes = BackupCrypto.decrypt(bytes, password);
            } catch (BackupCrypto.CryptoException e) {
                if (e.isWrongPassword()) {
                    throw new BackupException(ERROR_WRONG_PASSWORD, e);
                }
                throw new BackupException("Could not decrypt this backup", e);
            }
        }

        try {
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (JSONException e) {
            throw new BackupException("This file is not a valid backup", e);
        }
    }

    private void validate(JSONObject root) throws BackupException {
        if (!"finance-manager-backup".equals(root.optString(KEY_FORMAT))) {
            throw new BackupException("This file is not a Finance Manager backup");
        }
        int version = root.optInt(KEY_VERSION, 0);
        if (version > FORMAT_VERSION) {
            throw new BackupException("This backup was created by a newer app version");
        }
        if (version < 1) {
            throw new BackupException("Unsupported backup version");
        }
    }

    private BackupInfo count(JSONObject root) {
        BackupInfo info = new BackupInfo();
        info.accounts = length(root.optJSONArray("accounts"));
        info.categories = length(root.optJSONArray("categories"));
        info.transactions = length(root.optJSONArray("transactions"));
        info.budgets = length(root.optJSONArray("budgets"));
        info.goals = length(root.optJSONArray("savings_goals"));
        info.recurring = length(root.optJSONArray("recurring_transactions"));
        info.households = length(root.optJSONArray("households"));
        info.members = length(root.optJSONArray("household_members"));
        info.householdCategories = length(root.optJSONArray("household_categories"));
        return info;
    }

    private int length(JSONArray array) {
        return array == null ? 0 : array.length();
    }

    /** True when the database holds no records (used to reassure the user). */
    public boolean isDatabaseEmpty() {
        return database.accountDao().count() == 0 && database.transactionDao().count() == 0;
    }

    public List<Account> accountsSync() {
        return database.accountDao().getAllSync();
    }
}
