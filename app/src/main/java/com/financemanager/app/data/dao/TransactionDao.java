package com.financemanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.CategoryTotal;
import com.financemanager.app.models.DailyTotal;
import com.financemanager.app.models.MonthlyTotal;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.models.TransactionWithDetails;

import java.util.List;

@Dao
public interface TransactionDao {

    String DETAIL_SELECT = "SELECT t.*,"
            + " c.name AS category_name, c.icon AS category_icon, c.color AS category_color,"
            + " a.name AS account_name, ta.name AS to_account_name, a.currency AS account_currency"
            + " FROM transactions t"
            + " LEFT JOIN categories c ON c.id = t.category_id"
            + " LEFT JOIN accounts a ON a.id = t.account_id"
            + " LEFT JOIN accounts ta ON ta.id = t.to_account_id";

    @Insert
    long insert(TransactionEntity transaction);

    @Update
    int update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM transactions WHERE id = :id")
    TransactionEntity getById(long id);

    @Query(DETAIL_SELECT + " WHERE t.id = :id")
    LiveData<TransactionWithDetails> observeById(long id);

    @Query(DETAIL_SELECT + " ORDER BY t.date ASC, t.id ASC")
    List<TransactionWithDetails> getAllDetailsSync();

    @Query(DETAIL_SELECT + " ORDER BY t.date DESC, t.id DESC LIMIT :limit")
    LiveData<List<TransactionWithDetails>> observeRecent(int limit);

    @Query(DETAIL_SELECT
            + " WHERE (:type IS NULL OR t.type = :type)"
            + " AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND (:categoryId IS NULL OR t.category_id = :categoryId)"
            + " AND (:accountId IS NULL OR t.account_id = :accountId OR t.to_account_id = :accountId)"
            + " AND (t.title LIKE :query OR t.description LIKE :query)"
            + " AND (:minAmount IS NULL OR t.amount_minor >= :minAmount)"
            + " AND (:maxAmount IS NULL OR t.amount_minor <= :maxAmount)"
            + " ORDER BY CASE WHEN :sort = 0 THEN t.date END DESC,"
            + " CASE WHEN :sort = 1 THEN t.date END ASC,"
            + " CASE WHEN :sort = 2 THEN t.amount_minor END DESC,"
            + " CASE WHEN :sort = 3 THEN t.amount_minor END ASC,"
            + " t.id DESC")
    LiveData<List<TransactionWithDetails>> observeFiltered(TransactionType type, String query,
                                                           long fromDate, long toDate,
                                                           Long categoryId, Long accountId,
                                                           Long minAmount, Long maxAmount,
                                                           int sort);

    @Query(DETAIL_SELECT
            + " WHERE (t.account_id = :accountId OR t.to_account_id = :accountId)"
            + " ORDER BY t.date DESC, t.id DESC")
    LiveData<List<TransactionWithDetails>> observeForAccount(long accountId);

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId OR to_account_id = :accountId")
    int countForAccount(long accountId);

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    int countForCategory(long categoryId);

    @Query("UPDATE transactions SET category_id = :toCategoryId WHERE category_id = :fromCategoryId")
    int reassignCategory(long fromCategoryId, long toCategoryId);

    @Query("SELECT COUNT(*) FROM transactions")
    int count();

    @Query("SELECT * FROM transactions ORDER BY date ASC, id ASC")
    List<TransactionEntity> getAllSync();

    /** Total for a type, restricted to accounts in the reporting currency. */
    @Query("SELECT COALESCE(SUM(t.amount_minor), 0) FROM transactions t"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency")
    long totalForType(TransactionType type, long fromDate, long toDate, String currency);

    @Query("SELECT COALESCE(SUM(t.amount_minor), 0) FROM transactions t"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency")
    LiveData<Long> observeTotalForType(TransactionType type, long fromDate, long toDate,
                                       String currency);

    @Query("SELECT COUNT(*) FROM transactions t"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency")
    int countForType(TransactionType type, long fromDate, long toDate, String currency);

    /** Expenses per category inside a range, largest first. */
    @Query("SELECT t.category_id AS category_id, c.name AS category_name,"
            + " c.icon AS category_icon, c.color AS category_color,"
            + " SUM(t.amount_minor) AS total_minor"
            + " FROM transactions t"
            + " LEFT JOIN categories c ON c.id = t.category_id"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency"
            + " GROUP BY t.category_id ORDER BY total_minor DESC")
    List<CategoryTotal> categoryTotals(TransactionType type, long fromDate, long toDate,
                                       String currency);

    @Query("SELECT t.category_id AS category_id, c.name AS category_name,"
            + " c.icon AS category_icon, c.color AS category_color,"
            + " SUM(t.amount_minor) AS total_minor"
            + " FROM transactions t"
            + " LEFT JOIN categories c ON c.id = t.category_id"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency"
            + " GROUP BY t.category_id ORDER BY total_minor DESC")
    LiveData<List<CategoryTotal>> observeCategoryTotals(TransactionType type, long fromDate,
                                                        long toDate, String currency);

    /** Totals per day, used for weekly/monthly trend charts. */
    @Query("SELECT t.date AS day_millis, SUM(t.amount_minor) AS total_minor"
            + " FROM transactions t JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency"
            + " GROUP BY t.date ORDER BY t.date ASC")
    List<DailyTotal> dailyTotals(TransactionType type, long fromDate, long toDate, String currency);

    @Query("SELECT t.date AS day_millis, SUM(t.amount_minor) AS total_minor"
            + " FROM transactions t JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency"
            + " GROUP BY t.date ORDER BY t.date ASC")
    LiveData<List<DailyTotal>> observeDailyTotals(TransactionType type, long fromDate, long toDate,
                                                  String currency);

    /** Monthly income and expense totals for cash-flow comparisons. */
    @Query("SELECT strftime('%Y-%m', t.date / 1000, 'unixepoch', 'localtime') AS month_key,"
            + " COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount_minor ELSE 0 END), 0) AS income_minor,"
            + " COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount_minor ELSE 0 END), 0) AS expense_minor"
            + " FROM transactions t JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type IN ('INCOME', 'EXPENSE') AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency"
            + " GROUP BY month_key ORDER BY month_key ASC")
    List<MonthlyTotal> monthlyTotals(long fromDate, long toDate, String currency);

    @Query("SELECT strftime('%Y-%m', t.date / 1000, 'unixepoch', 'localtime') AS month_key,"
            + " COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount_minor ELSE 0 END), 0) AS income_minor,"
            + " COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount_minor ELSE 0 END), 0) AS expense_minor"
            + " FROM transactions t JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type IN ('INCOME', 'EXPENSE') AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency"
            + " GROUP BY month_key ORDER BY month_key ASC")
    LiveData<List<MonthlyTotal>> observeMonthlyTotals(long fromDate, long toDate, String currency);

    @Query(DETAIL_SELECT
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency"
            + " ORDER BY t.amount_minor DESC, t.id ASC LIMIT 1")
    TransactionWithDetails largestTransaction(TransactionType type, long fromDate, long toDate,
                                              String currency);

    @Query("SELECT COALESCE(SUM(t.amount_minor), 0) FROM transactions t"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = 'EXPENSE' AND t.category_id = :categoryId"
            + " AND t.date >= :fromDate AND t.date <= :toDate AND a.currency = :currency")
    long expenseForCategory(long categoryId, long fromDate, long toDate, String currency);

    @Query("SELECT COUNT(*) FROM transactions WHERE recurring_id = :recurringId AND date = :date")
    int countForRecurringOnDate(long recurringId, long date);

    @Query("SELECT COALESCE(SUM(t.amount_minor), 0) FROM transactions t"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = :type AND t.date >= :fromDate AND t.date <= :toDate")
    long totalForTypeAllCurrencies(TransactionType type, long fromDate, long toDate);

    @Query("SELECT * FROM transactions WHERE recurring_id = :recurringId")
    List<TransactionEntity> getForRecurring(long recurringId);
}
