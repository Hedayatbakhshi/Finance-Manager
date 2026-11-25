package com.financemanager.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Budget;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.HouseholdCategory;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.BudgetWithProgress;
import com.financemanager.app.models.HouseholdCategoryBudget;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.models.HouseholdFilter;
import com.financemanager.app.models.HouseholdSummary;
import com.financemanager.app.models.ReportResult;
import com.financemanager.app.models.TransactionFilter;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.models.TransactionWithDetails;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.DefaultData;
import com.financemanager.app.utils.LanguageHelper;
import com.financemanager.app.utils.Money;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth for financial data.
 *
 * <p>All database work happens on a dedicated background executor; UI callbacks are
 * posted to the main thread. Financial invariants (positive amounts, transfer
 * accounts, sufficient funds, category fallbacks) are enforced here rather than in
 * the UI so every caller gets the same guarantees.</p>
 */
public class FinanceRepository {

    private static final String TAG = "FinanceRepository";

    public interface Callback<T> {
        void onSuccess(@Nullable T value);

        void onError(String message);
    }

    private final FinanceDatabase database;
    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile Runnable dataChangeListener;

    public FinanceRepository(FinanceDatabase database, Context context) {
        this.database = database;
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "finance-db");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
    }

    public FinanceDatabase database() {
        return database;
    }

    /**
     * Registers a listener invoked on the database thread after money-related changes.
     * The app uses it to keep budget alerts current without the repository knowing
     * anything about notifications.
     */
    public void setDataChangeListener(@Nullable Runnable listener) {
        this.dataChangeListener = listener;
    }

    private void notifyDataChanged() {
        Runnable listener = dataChangeListener;
        if (listener == null) {
            return;
        }
        try {
            listener.run();
        } catch (Exception e) {
            Log.e(TAG, "Data change listener failed", e);
        }
    }

    public ExecutorService executor() {
        return executor;
    }

    /** Runs a background task and reports the result on the main thread. */
    public <T> void runAsync(@NonNull Callable<T> work, @Nullable Callback<T> callback) {
        executor.execute(() -> {
            try {
                T result = work.call();
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                Log.e(TAG, "Background task failed", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(
                            e.getMessage() == null ? "Operation failed" : e.getMessage()));
                }
            }
        });
    }

    /** Fire-and-forget background action. */
    public void runAsync(@NonNull Runnable work) {
        executor.execute(work);
    }

    // ---------------------------------------------------------------------
    // Seeding
    // ---------------------------------------------------------------------

    /** Inserts default categories and a starter account once, on first launch. */
    public void seedIfNeeded(@Nullable Runnable onDone) {
        seedIfNeeded(context, null, onDone);
    }

    /** Seeds using the current app language, even when called outside an Activity. */
    public void seedIfNeeded(Context seedContext, @Nullable Runnable onDone) {
        seedIfNeeded(seedContext, null, onDone);
    }

    /**
     * Seeds first-launch data, creating the starter cash account in the app's
     * configured currency instead of always defaulting to USD.
     */
    public void seedIfNeeded(Context seedContext, @Nullable String preferredCurrency,
                             @Nullable Runnable onDone) {
        Context localized = LanguageHelper.localizedContext(seedContext);
        String currency = preferredCurrency == null || preferredCurrency.trim().isEmpty()
                ? "USD" : preferredCurrency.trim().toUpperCase(java.util.Locale.ROOT);
        executor.execute(() -> {
            try {
                if (database.categoryDao().count() == 0) {
                    database.categoryDao().insertAll(
                            DefaultData.categories(localized, System.currentTimeMillis()));
                }
                if (database.accountDao().count() == 0) {
                    database.accountDao().insert(DefaultData.starterAccount(localized, currency,
                            System.currentTimeMillis()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Seeding failed", e);
            } finally {
                if (onDone != null) {
                    mainHandler.post(onDone);
                }
            }
        });
    }

    // ---------------------------------------------------------------------
    // Accounts
    // ---------------------------------------------------------------------

    public LiveData<List<AccountWithBalance>> observeAccounts() {
        return database.accountDao().observeWithBalances();
    }

    public LiveData<Long> observeTotalBalance(String currency, long upTo) {
        return database.accountDao().observeBalanceUpTo(currency, upTo);
    }

    public LiveData<Long> observeBalanceUpTo(String currency, long upTo) {
        return database.accountDao().observeBalanceUpTo(currency, upTo);
    }

    public void loadAccount(long id, Callback<AccountWithBalance> callback) {
        runAsync(() -> database.accountDao().getWithBalance(id), callback);
    }

    public void loadAccounts(Callback<List<AccountWithBalance>> callback) {
        runAsync(() -> database.accountDao().getWithBalancesSync(), callback);
    }

    public void loadAccountEntity(long id, Callback<Account> callback) {
        runAsync(() -> database.accountDao().getById(id), callback);
    }

    public void loadAccountsSync(Callback<List<Account>> callback) {
        runAsync(() -> database.accountDao().getAllSync(), callback);
    }

    /**
     * Creates or updates an account. Names are unique; a duplicate is rejected with a
     * readable error rather than a raw SQLite exception.
     */
    public void saveAccount(Account account, boolean isNew, Callback<Long> callback) {
        runAsync(() -> {
            String name = account.name == null ? "" : account.name.trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Account name is required");
            }
            account.name = name;
            if (account.currency == null || !account.currency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("Choose a valid currency");
            }
            if (database.accountDao().countByName(account.name, isNew ? -1 : account.id) > 0) {
                throw new IllegalArgumentException("An account with this name already exists");
            }
            long now = System.currentTimeMillis();
            account.updatedAt = now;
            if (isNew) {
                account.createdAt = now;
                return database.accountDao().insert(account);
            }
            database.accountDao().update(account);
            return account.id;
        }, callback);
    }

    /** Deletes an account and (by foreign key) all of its transactions. */
    public void countAccountTransactions(long id, Callback<Integer> callback) {
        runAsync(() -> database.transactionDao().countForAccount(id), callback);
    }

    public void deleteAccount(long id, Callback<Void> callback) {
        runAsync(() -> {
            if (database.accountDao().count() <= 1) {
                throw new IllegalStateException("Keep at least one account");
            }
            database.accountDao().deleteById(id);
            return null;
        }, callback);
    }

    // ---------------------------------------------------------------------
    // Categories
    // ---------------------------------------------------------------------

    public LiveData<List<Category>> observeCategories() {
        return database.categoryDao().observeAll();
    }

    public LiveData<List<Category>> observeCategories(TransactionType type) {
        return database.categoryDao().observeByType(type);
    }

    public void loadCategories(TransactionType type, Callback<List<Category>> callback) {
        runAsync(() -> database.categoryDao().getByTypeSync(type), callback);
    }

    public void saveCategory(Category category, boolean isNew, Callback<Long> callback) {
        runAsync(() -> {
            String name = category.name == null ? "" : category.name.trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Category name is required");
            }
            category.name = name;
            if (database.categoryDao().findDuplicate(category.name, category.type,
                    isNew ? -1 : category.id) != null) {
                throw new IllegalArgumentException("A category with this name already exists");
            }
            if (isNew) {
                category.createdAt = System.currentTimeMillis();
                return database.categoryDao().insert(category);
            }
            database.categoryDao().update(category);
            return category.id;
        }, callback);
    }

    /**
     * Deletes a custom category. Transactions that used it are moved to the "Other"
     * category of the same type so history and totals survive; budgets bound to the
     * category are removed with it.
     */
    public void deleteCategory(long categoryId, Callback<Boolean> callback) {
        runAsync(() -> {
            Category category = database.categoryDao().getById(categoryId);
            if (category == null) {
                return false;
            }
            if (category.isDefault) {
                throw new IllegalStateException("Default categories cannot be deleted");
            }
            return database.runInTransaction((Callable<Boolean>) () -> {
                Category fallback = database.categoryDao()
                        .findDefaultOther(category.type);
                if (fallback != null && fallback.id != categoryId) {
                    database.transactionDao().reassignCategory(categoryId, fallback.id);
                }
                database.categoryDao().deleteById(categoryId);
                return true;
            });
        }, callback);
    }

    // ---------------------------------------------------------------------
    // Transactions
    // ---------------------------------------------------------------------

    public LiveData<List<TransactionWithDetails>> observeRecentTransactions(int limit) {
        return database.transactionDao().observeRecent(limit);
    }

    public LiveData<List<TransactionWithDetails>> observeTransactions(TransactionFilter filter) {
        return database.transactionDao().observeFiltered(
                filter.type,
                filter.likeQuery(),
                filter.fromDate,
                filter.toDate,
                filter.categoryId,
                filter.accountId,
                filter.minAmount,
                filter.maxAmount,
                filter.sort);
    }

    public LiveData<TransactionWithDetails> observeTransaction(long id) {
        return database.transactionDao().observeById(id);
    }

    public LiveData<List<TransactionWithDetails>> observeAccountTransactions(long accountId) {
        return database.transactionDao().observeForAccount(accountId);
    }

    public void loadTransaction(long id, Callback<TransactionEntity> callback) {
        runAsync(() -> database.transactionDao().getById(id), callback);
    }

    /**
     * Validates and persists a transaction.
     *
     * <p>Transfers are validated and written inside one database transaction: the
     * source account must exist, differ from the destination, use the same currency,
     * and hold enough funds. Because the whole transfer lives in a single row, both
     * balances change together or not at all.</p>
     */
    public void saveTransaction(TransactionEntity transaction, boolean isNew,
                                Callback<Long> callback) {
        runAsync(() -> saveTransactionSync(transaction, isNew), callback);
    }

    /** Synchronous version used by household expenses and by the async façade. */
    private long saveTransactionSync(TransactionEntity transaction, boolean isNew) {
        validateTransaction(transaction);
        long now = System.currentTimeMillis();
        transaction.updatedAt = now;
        if (isNew) {
            transaction.createdAt = now;
        }
        Long result = database.runInTransaction((Callable<Long>) () -> {
                if (transaction.isTransfer()) {
                    AccountWithBalance source = database.accountDao()
                            .getWithBalance(transaction.accountId);
                    if (source == null) {
                        throw new IllegalArgumentException("Select a valid source account");
                    }
                    if (source.balanceMinor < transaction.amountMinor) {
                        throw new IllegalArgumentException("Insufficient funds in the source account");
                    }
                }
                if (isNew) {
                    if (transaction.type != TransactionType.TRANSFER) {
                        transaction.toAccountId = null;
                    }
                    return database.transactionDao().insert(transaction);
                }
                if (transaction.type != TransactionType.TRANSFER) {
                    transaction.toAccountId = null;
                }
                database.transactionDao().update(transaction);
                return transaction.id;
            });
        notifyDataChanged();
        return result;
    }

    private void validateTransaction(TransactionEntity transaction) {
        if (transaction.amountMinor <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (transaction.title == null || transaction.title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        transaction.title = transaction.title.trim();
        if (transaction.date <= 0) {
            throw new IllegalArgumentException("Choose a valid date");
        }
        if (database.accountDao().getById(transaction.accountId) == null) {
            throw new IllegalArgumentException("Select a valid account");
        }
        if (transaction.type == TransactionType.TRANSFER) {
            if (transaction.toAccountId == null
                    || transaction.toAccountId == transaction.accountId) {
                throw new IllegalArgumentException(
                        "Source and destination accounts must be different");
            }
            Account destination = database.accountDao().getById(transaction.toAccountId);
            if (destination == null) {
                throw new IllegalArgumentException("Select a valid destination account");
            }
            Account source = database.accountDao().getById(transaction.accountId);
            if (source != null && !source.currency.equalsIgnoreCase(destination.currency)) {
                throw new IllegalArgumentException(
                        "Transfers require accounts in the same currency");
            }
            transaction.categoryId = null;
        } else {
            if (transaction.categoryId == null
                    || database.categoryDao().getById(transaction.categoryId) == null) {
                throw new IllegalArgumentException("Select a category");
            }
        }
    }

    public void deleteTransaction(long id, Callback<Void> callback) {
        runAsync(() -> {
            deleteTransactionSync(id);
            return null;
        }, callback);
    }

    private void deleteTransactionSync(long id) {
        database.transactionDao().deleteById(id);
        notifyDataChanged();
    }

    // ---------------------------------------------------------------------
    // Budgets
    // ---------------------------------------------------------------------

    public LiveData<List<BudgetWithProgress>> observeBudgets(String currency) {
        return database.budgetDao().observeAll(currency);
    }

    public void loadBudget(long id, Callback<Budget> callback) {
        runAsync(() -> database.budgetDao().getById(id), callback);
    }

    public void saveBudget(Budget budget, boolean isNew, Callback<Long> callback) {
        runAsync(() -> {
            if (budget.name == null || budget.name.trim().isEmpty()) {
                throw new IllegalArgumentException("Budget name is required");
            }
            if (budget.amountMinor <= 0) {
                throw new IllegalArgumentException("Budget amount must be greater than zero");
            }
            if (budget.startDate > budget.endDate) {
                throw new IllegalArgumentException("End date must be after the start date");
            }
            if (database.categoryDao().getById(budget.categoryId) == null) {
                throw new IllegalArgumentException("Select a category");
            }
            long now = System.currentTimeMillis();
            budget.updatedAt = now;
            if (isNew) {
                budget.createdAt = now;
                long id = database.budgetDao().insert(budget);
                notifyDataChanged();
                return id;
            }
            database.budgetDao().update(budget);
            notifyDataChanged();
            return budget.id;
        }, callback);
    }

    public void deleteBudget(long id, Callback<Void> callback) {
        runAsync(() -> {
            database.budgetDao().deleteById(id);
            notifyDataChanged();
            return null;
        }, callback);
    }

    // ---------------------------------------------------------------------
    // Savings goals
    // ---------------------------------------------------------------------

    public LiveData<List<SavingsGoal>> observeSavingsGoals() {
        return database.savingsGoalDao().observeAll();
    }

    public void loadGoal(long id, Callback<SavingsGoal> callback) {
        runAsync(() -> database.savingsGoalDao().getById(id), callback);
    }

    public void saveSavingsGoal(SavingsGoal goal, boolean isNew, Callback<Long> callback) {
        runAsync(() -> {
            if (goal.name == null || goal.name.trim().isEmpty()) {
                throw new IllegalArgumentException("Goal name is required");
            }
            if (goal.targetMinor <= 0) {
                throw new IllegalArgumentException("Target must be greater than zero");
            }
            if (goal.currentMinor < 0) {
                goal.currentMinor = 0;
            }
            long now = System.currentTimeMillis();
            goal.updatedAt = now;
            if (isNew) {
                goal.createdAt = now;
                return database.savingsGoalDao().insert(goal);
            }
            database.savingsGoalDao().update(goal);
            return goal.id;
        }, callback);
    }

    public void deleteGoal(long id, Callback<Void> callback) {
        runAsync(() -> {
            database.savingsGoalDao().deleteById(id);
            return null;
        }, callback);
    }

    /**
     * Adds ({@code delta > 0}) or withdraws ({@code delta < 0}) savings.
     * The update is a single conditional SQL statement, so it cannot race and can
     * never drive a goal below zero.
     */
    public void adjustGoalAmount(long goalId, long delta, Callback<Boolean> callback) {
        runAsync(() -> {
            int updated = database.savingsGoalDao().adjustCurrent(goalId, delta,
                    System.currentTimeMillis());
            if (updated == 0) {
                throw new IllegalStateException("You cannot withdraw more than you have saved");
            }
            return true;
        }, callback);
    }

    // ---------------------------------------------------------------------
    // Recurring transactions
    // ---------------------------------------------------------------------

    public LiveData<List<RecurringTransaction>> observeRecurring() {
        return database.recurringDao().observeAll();
    }

    public void loadRecurring(long id, Callback<RecurringTransaction> callback) {
        runAsync(() -> database.recurringDao().getById(id), callback);
    }

    public void saveRecurring(RecurringTransaction recurring, boolean isNew,
                              Callback<Long> callback) {
        runAsync(() -> saveRecurringSync(recurring, isNew), callback);
    }

    private long saveRecurringSync(RecurringTransaction recurring, boolean isNew) {
        if (recurring.amountMinor <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }
            if (recurring.title == null || recurring.title.trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (recurring.nextRunDate <= 0) {
                throw new IllegalArgumentException("Choose the next date");
            }
            if (database.accountDao().getById(recurring.accountId) == null) {
                throw new IllegalArgumentException("Select a valid account");
            }
            if (recurring.type == TransactionType.TRANSFER) {
                if (recurring.toAccountId == null
                        || recurring.toAccountId == recurring.accountId) {
                    throw new IllegalArgumentException(
                            "Source and destination accounts must be different");
                }
                Account source = database.accountDao().getById(recurring.accountId);
                Account destination = database.accountDao().getById(recurring.toAccountId);
                if (source != null && destination != null
                        && !source.currency.equalsIgnoreCase(destination.currency)) {
                    throw new IllegalArgumentException(
                            "Transfers require accounts in the same currency");
                }
            } else if (recurring.categoryId == null) {
                throw new IllegalArgumentException("Select a category");
            }
            long now = System.currentTimeMillis();
            recurring.updatedAt = now;
            if (isNew) {
                recurring.createdAt = now;
                return database.recurringDao().insert(recurring);
            }
            database.recurringDao().update(recurring);
            return recurring.id;
    }

    public void setRecurringActive(long id, boolean active, Callback<Void> callback) {
        runAsync(() -> {
            database.recurringDao().setActive(id, active, System.currentTimeMillis());
            return null;
        }, callback);
    }

    public void deleteRecurring(long id, Callback<Void> callback) {
        runAsync(() -> {
            database.recurringDao().deleteById(id);
            return null;
        }, callback);
    }

    /**
     * Generates every transaction that is due on or before {@code upTo}.
     * Runs on the worker thread. Each generated row stores its recurring id and date;
     * the existing-row check makes duplicate generation impossible even if the worker
     * runs twice. Returns the number of transactions created.
     */
    public int generateDueTransactions(long upTo) {
        List<RecurringTransaction> due = database.recurringDao().getDue(upTo);
        if (due.isEmpty()) {
            return 0;
        }
        int created = 0;
        for (RecurringTransaction recurring : due) {
            long nextRun = recurring.nextRunDate;
            int guard = 0;
            while (nextRun <= upTo && guard < 400) {
                guard++;
                long date = nextRun;
                if (database.transactionDao().countForRecurringOnDate(recurring.id, date) == 0) {
                    TransactionEntity transaction = new TransactionEntity();
                    transaction.title = recurring.title;
                    transaction.amountMinor = recurring.amountMinor;
                    transaction.type = recurring.type;
                    transaction.categoryId = recurring.categoryId;
                    transaction.accountId = recurring.accountId;
                    transaction.toAccountId = recurring.toAccountId;
                    transaction.date = date;
                    transaction.description = recurring.description;
                    transaction.notes = recurring.notes;
                    transaction.recurringId = recurring.id;
                    transaction.householdId = recurring.householdId;
                    transaction.householdCategoryId = recurring.householdCategoryId;
                    transaction.householdMemberId = recurring.householdMemberId;
                    transaction.createdAt = System.currentTimeMillis();
                    transaction.updatedAt = System.currentTimeMillis();
                    database.transactionDao().insert(transaction);
                    created++;
                }
                nextRun = DateUtils.startOfDay(
                        recurring.frequency.advance(DateUtils.toLocalDate(date)));
                database.recurringDao().setNextRun(recurring.id, nextRun,
                        System.currentTimeMillis());
            }
        }
        return created;
    }

    // ---------------------------------------------------------------------
    // Reports
    // ---------------------------------------------------------------------

    /** Computes a full report from real records for the given range and currency. */
    public void loadReport(long fromDate, long toDate, String currency,
                           Callback<ReportResult> callback) {
        runAsync(() -> {
            ReportResult result = new ReportResult();
            result.fromDate = fromDate;
            result.toDate = toDate;
            result.incomeTotal = database.transactionDao()
                    .totalForType(TransactionType.INCOME, fromDate, toDate, currency);
            result.expenseTotal = database.transactionDao()
                    .totalForType(TransactionType.EXPENSE, fromDate, toDate, currency);
            result.incomeCount = database.transactionDao()
                    .countForType(TransactionType.INCOME, fromDate, toDate, currency);
            result.expenseCount = database.transactionDao()
                    .countForType(TransactionType.EXPENSE, fromDate, toDate, currency);
            result.incomeByCategory = database.transactionDao()
                    .categoryTotals(TransactionType.INCOME, fromDate, toDate, currency);
            result.expenseByCategory = database.transactionDao()
                    .categoryTotals(TransactionType.EXPENSE, fromDate, toDate, currency);
            result.dailyIncome = database.transactionDao()
                    .dailyTotals(TransactionType.INCOME, fromDate, toDate, currency);
            result.dailyExpense = database.transactionDao()
                    .dailyTotals(TransactionType.EXPENSE, fromDate, toDate, currency);
            result.monthly = database.transactionDao()
                    .monthlyTotals(fromDate, toDate, currency);
            result.largestIncome = database.transactionDao()
                    .largestTransaction(TransactionType.INCOME, fromDate, toDate, currency);
            result.largestExpense = database.transactionDao()
                    .largestTransaction(TransactionType.EXPENSE, fromDate, toDate, currency);
            return result;
        }, callback);
    }

    // ---------------------------------------------------------------------
    // Households — local profiles layered on the existing ledger
    // ---------------------------------------------------------------------

    public LiveData<List<Household>> observeHouseholds() {
        return database.householdDao().observeAll();
    }

    public void loadHouseholds(Callback<List<Household>> callback) {
        runAsync(() -> database.householdDao().getAllSync(), callback);
    }

    public void loadHousehold(long id, Callback<Household> callback) {
        runAsync(() -> database.householdDao().getById(id), callback);
    }

    public void saveHousehold(Household household, boolean isNew, Callback<Long> callback) {
        runAsync(() -> {
            household.name = household.name == null ? "" : household.name.trim();
            if (household.name.isEmpty()) {
                throw new IllegalArgumentException("Household name is required");
            }
            if (household.currency == null || household.currency.trim().isEmpty()) {
                throw new IllegalArgumentException("Choose a currency");
            }
            household.currency = household.currency.trim().toUpperCase(java.util.Locale.ROOT);
            if (household.monthlyBudgetMinor < 0) {
                throw new IllegalArgumentException("Budget cannot be negative");
            }
            if (database.householdDao().countByName(household.name,
                    isNew ? -1 : household.id) > 0) {
                throw new IllegalArgumentException("A household with this name already exists");
            }
            long now = System.currentTimeMillis();
            household.updatedAt = now;
            if (!isNew) {
                database.householdDao().update(household);
                return household.id;
            }
            household.createdAt = now;
            return database.runInTransaction((Callable<Long>) () -> {
                long id = database.householdDao().insert(household);
                // Link existing expense categories instead of duplicating them.
                for (Category category : database.categoryDao()
                        .getByTypeSync(TransactionType.EXPENSE)) {
                    HouseholdCategory link = new HouseholdCategory();
                    link.householdId = id;
                    link.categoryId = category.id;
                    link.createdAt = now;
                    link.updatedAt = now;
                    database.householdDao().insertCategory(link);
                }
                return id;
            });
        }, callback);
    }

    public void deleteHousehold(long id, Callback<Void> callback) {
        runAsync(() -> {
            database.householdDao().deleteById(id);
            return null;
        }, callback);
    }

    public LiveData<List<HouseholdMember>> observeHouseholdMembers(long householdId) {
        return database.householdDao().observeMembers(householdId);
    }

    public void loadHouseholdMembers(long householdId, Callback<List<HouseholdMember>> callback) {
        runAsync(() -> database.householdDao().getMembersSync(householdId), callback);
    }

    public void saveHouseholdMember(HouseholdMember member, boolean isNew,
                                    Callback<Long> callback) {
        runAsync(() -> {
            member.name = member.name == null ? "" : member.name.trim();
            if (member.name.isEmpty()) {
                throw new IllegalArgumentException("Member name is required");
            }
            if (database.householdDao().countMemberByName(member.householdId, member.name,
                    isNew ? -1 : member.id) > 0) {
                throw new IllegalArgumentException("This member already exists");
            }
            if (isNew) {
                member.createdAt = System.currentTimeMillis();
                return database.householdDao().insertMember(member);
            }
            database.householdDao().updateMember(member);
            return member.id;
        }, callback);
    }

    public void deleteHouseholdMember(long id, Callback<Void> callback) {
        runAsync(() -> {
            database.householdDao().deleteMemberById(id);
            return null;
        }, callback);
    }

    public LiveData<List<HouseholdCategoryBudget>> observeHouseholdCategories(
            long householdId, long fromDate, long toDate, String currency) {
        return database.householdDao().observeCategoryBudgets(
                householdId, fromDate, toDate, currency);
    }

    public void loadHouseholdCategoryLinks(long householdId,
                                           Callback<List<HouseholdCategory>> callback) {
        runAsync(() -> database.householdDao().getCategoriesSync(householdId), callback);
    }

    public void loadHouseholdCategories(long householdId, long fromDate, long toDate,
                                        String currency,
                                        Callback<List<HouseholdCategoryBudget>> callback) {
        runAsync(() -> database.householdDao().getCategoryBudgetsSync(
                householdId, fromDate, toDate, currency), callback);
    }

    public void linkHouseholdCategory(long householdId, long categoryId, Callback<Long> callback) {
        runAsync(() -> {
            if (database.categoryDao().getById(categoryId) == null) {
                throw new IllegalArgumentException("Select a category");
            }
            if (database.householdDao().getCategory(householdId, categoryId) != null) {
                throw new IllegalArgumentException("This category is already linked");
            }
            long now = System.currentTimeMillis();
            HouseholdCategory link = new HouseholdCategory();
            link.householdId = householdId;
            link.categoryId = categoryId;
            link.createdAt = now;
            link.updatedAt = now;
            return database.householdDao().insertCategory(link);
        }, callback);
    }

    public void saveHouseholdCategory(HouseholdCategory category, Callback<Void> callback) {
        runAsync(() -> {
            if (category.monthlyBudgetMinor < 0) {
                throw new IllegalArgumentException("Budget cannot be negative");
            }
            category.updatedAt = System.currentTimeMillis();
            database.householdDao().updateCategory(category);
            return null;
        }, callback);
    }

    public void unlinkHouseholdCategory(long id, Callback<Void> callback) {
        runAsync(() -> {
            HouseholdCategory link = database.householdDao().getCategoryById(id);
            if (link != null) {
                database.householdDao().deleteCategory(link);
            }
            return null;
        }, callback);
    }

    public LiveData<List<HouseholdExpense>> observeHouseholdExpenses(HouseholdFilter filter) {
        return database.householdDao().observeExpenses(filter.householdId, filter.likeQuery(),
                filter.fromDate, filter.toDate, filter.categoryId, filter.accountId,
                filter.memberId, filter.sort);
    }

    public void loadHouseholdExpense(long id, Callback<HouseholdExpense> callback) {
        runAsync(() -> database.householdDao().getExpense(id), callback);
    }

    /** Persists a household expense through the normal transaction writer. */
    public void saveHouseholdExpense(TransactionEntity transaction, boolean isNew,
                                     Callback<Long> callback) {
        runAsync(() -> {
            transaction.type = TransactionType.EXPENSE;
            transaction.toAccountId = null;
            if (transaction.householdId == null
                    || database.householdDao().getById(transaction.householdId) == null) {
                throw new IllegalArgumentException("Select a household");
            }
            if (transaction.householdCategoryId != null) {
                HouseholdCategory link = database.householdDao()
                        .getCategoryById(transaction.householdCategoryId);
                if (link == null || link.householdId != transaction.householdId) {
                    throw new IllegalArgumentException("Select a category of this household");
                }
                transaction.categoryId = link.categoryId;
            }
            if (transaction.householdMemberId != null) {
                HouseholdMember member = database.householdDao()
                        .getMember(transaction.householdMemberId);
                if (member == null || member.householdId != transaction.householdId) {
                    throw new IllegalArgumentException("Select a member of this household");
                }
            }
            return saveTransactionSync(transaction, isNew);
        }, callback);
    }

    public void deleteHouseholdExpense(long id, Callback<Void> callback) {
        deleteTransaction(id, callback);
    }

    /** Household dashboard figures, all derived from real ledger rows. */
    public void loadHouseholdSummary(long householdId, long fromDate, long toDate,
                                     String currency, Callback<HouseholdSummary> callback) {
        runAsync(() -> {
            Household household = database.householdDao().getById(householdId);
            if (household == null) {
                throw new IllegalArgumentException("Household not found");
            }
            HouseholdSummary summary = new HouseholdSummary();
            summary.fromDate = fromDate;
            summary.toDate = toDate;
            summary.currentTotal = database.householdDao()
                    .totalExpense(householdId, fromDate, toDate, currency);
            summary.previousTotal = database.householdDao().totalExpense(householdId,
                    DateUtils.startOfMonth(DateUtils.toLocalDate(fromDate).minusMonths(1)),
                    DateUtils.endOfMonth(DateUtils.toLocalDate(fromDate).minusMonths(1)),
                    currency);
            summary.budgetMinor = household.monthlyBudgetMinor;
            summary.remainingMinor = summary.budgetMinor - summary.currentTotal;
            summary.percent = summary.budgetMinor > 0
                    ? Money.rawPercent(summary.currentTotal, summary.budgetMinor) : 0;
            summary.expenseCount = database.householdDao().countExpenses(householdId);
            summary.foreignCount = database.householdDao()
                    .countForeignExpenses(householdId, fromDate, toDate, currency);
            summary.categories = database.householdDao()
                    .categoryTotals(householdId, fromDate, toDate, currency);
            summary.daily = database.householdDao()
                    .dailyTotals(householdId, fromDate, toDate, currency);
            summary.monthly = database.householdDao()
                    .monthlyTotals(householdId, fromDate, toDate, currency);
            summary.recent = database.householdDao().getRecentExpensesSync(householdId, 5);
            summary.upcoming = database.householdDao().getUpcomingRecurring(householdId,
                    DateUtils.todayStart(), DateUtils.endOfDay(
                            DateUtils.toLocalDate(DateUtils.todayStart()).plusDays(30)));
            return summary;
        }, callback);
    }

    public LiveData<List<RecurringTransaction>> observeHouseholdRecurring(long householdId) {
        return database.householdDao().observeRecurring(householdId);
    }

    public void loadHouseholdUpcoming(long householdId, long fromDate, long toDate,
                                      Callback<List<RecurringTransaction>> callback) {
        runAsync(() -> database.householdDao().getUpcomingRecurring(
                householdId, fromDate, toDate), callback);
    }

    public void saveHouseholdRecurring(RecurringTransaction recurring, boolean isNew,
                                       Callback<Long> callback) {
        runAsync(() -> {
            recurring.type = TransactionType.EXPENSE;
            if (recurring.householdId == null
                    || database.householdDao().getById(recurring.householdId) == null) {
                throw new IllegalArgumentException("Select a household");
            }
            if (recurring.householdCategoryId != null) {
                HouseholdCategory link = database.householdDao()
                        .getCategoryById(recurring.householdCategoryId);
                if (link == null || link.householdId != recurring.householdId) {
                    throw new IllegalArgumentException("Select a category of this household");
                }
                recurring.categoryId = link.categoryId;
            }
            return saveRecurringSync(recurring, isNew);
        }, callback);
    }

    // ---------------------------------------------------------------------
    // Maintenance
    // ---------------------------------------------------------------------

    /** Wipes every record, keeping app preferences. Used by "Clear all data". */
    public void clearAllData(Callback<Void> callback) {
        runAsync(() -> {
            database.runInTransaction((Runnable) () -> {
                SupportSQLiteDatabase sql = database.getOpenHelper().getWritableDatabase();
                sql.execSQL("DELETE FROM household_members");
                sql.execSQL("DELETE FROM household_categories");
                sql.execSQL("DELETE FROM households");
                sql.execSQL("DELETE FROM budget_alerts");
                sql.execSQL("DELETE FROM budgets");
                sql.execSQL("DELETE FROM savings_goals");
                sql.execSQL("DELETE FROM recurring_transactions");
                sql.execSQL("DELETE FROM transactions");
                sql.execSQL("DELETE FROM categories");
                sql.execSQL("DELETE FROM accounts");
            });
            return null;
        }, callback);
    }
}
