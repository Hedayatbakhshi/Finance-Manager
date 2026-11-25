package com.financemanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.HouseholdCategory;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.models.CategoryTotal;
import com.financemanager.app.models.DailyTotal;
import com.financemanager.app.models.HouseholdCategoryBudget;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.models.MonthlyTotal;

import java.util.List;

/**
 * Household-scoped views over the existing transaction ledger. Household expenses
 * are ordinary {@code transactions} rows; these queries only add a membership filter.
 */
@Dao
public interface HouseholdDao {

    String EXPENSE_SELECT = "SELECT t.*,"
            + " c.name AS category_name, c.icon AS category_icon, c.color AS category_color,"
            + " a.name AS account_name, ta.name AS to_account_name, a.currency AS account_currency,"
            + " m.name AS member_name"
            + " FROM transactions t"
            + " LEFT JOIN categories c ON c.id = t.category_id"
            + " LEFT JOIN accounts a ON a.id = t.account_id"
            + " LEFT JOIN accounts ta ON ta.id = t.to_account_id"
            + " LEFT JOIN household_members m ON m.id = t.household_member_id";

    String BUDGET_SELECT = "SELECT hc.*, c.name AS category_name, c.icon AS category_icon,"
            + " c.color AS category_color,"
            + " COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.type = 'EXPENSE' AND t.household_category_id = hc.id"
            + " AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND a.currency = :currency), 0) AS spent_minor"
            + " FROM household_categories hc"
            + " LEFT JOIN categories c ON c.id = hc.category_id";

    // --- Households -------------------------------------------------------

    @Insert
    long insert(Household household);

    @Update
    void update(Household household);

    @Delete
    void delete(Household household);

    @Query("DELETE FROM households WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM households WHERE id = :id")
    Household getById(long id);

    @Query("SELECT * FROM households ORDER BY created_at DESC, id DESC")
    LiveData<List<Household>> observeAll();

    @Query("SELECT * FROM households ORDER BY name COLLATE NOCASE")
    List<Household> getAllSync();

    @Query("SELECT COUNT(*) FROM households")
    int count();

    @Query("SELECT COUNT(*) FROM households WHERE name = :name AND id != :excludeId")
    int countByName(String name, long excludeId);

    // --- Members ----------------------------------------------------------

    @Insert
    long insertMember(HouseholdMember member);

    @Update
    void updateMember(HouseholdMember member);

    @Delete
    void deleteMember(HouseholdMember member);

    @Query("DELETE FROM household_members WHERE id = :id")
    void deleteMemberById(long id);

    @Query("SELECT * FROM household_members WHERE id = :id")
    HouseholdMember getMember(long id);

    @Query("SELECT * FROM household_members WHERE household_id = :householdId ORDER BY name COLLATE NOCASE")
    LiveData<List<HouseholdMember>> observeMembers(long householdId);

    @Query("SELECT * FROM household_members WHERE household_id = :householdId ORDER BY name COLLATE NOCASE")
    List<HouseholdMember> getMembersSync(long householdId);

    @Query("SELECT COUNT(*) FROM household_members WHERE household_id = :householdId"
            + " AND name = :name AND id != :excludeId")
    int countMemberByName(long householdId, String name, long excludeId);

    @Query("SELECT COUNT(*) FROM household_members WHERE household_id = :householdId")
    int countMembers(long householdId);

    // --- Categories -------------------------------------------------------

    @Insert
    long insertCategory(HouseholdCategory category);

    @Update
    void updateCategory(HouseholdCategory category);

    @Delete
    void deleteCategory(HouseholdCategory category);

    @Query("SELECT * FROM household_categories WHERE household_id = :householdId"
            + " AND category_id = :categoryId")
    HouseholdCategory getCategory(long householdId, long categoryId);

    @Query("SELECT * FROM household_categories WHERE id = :id")
    HouseholdCategory getCategoryById(long id);

    @Query("SELECT * FROM household_categories WHERE household_id = :householdId")
    List<HouseholdCategory> getCategoriesSync(long householdId);

    @Query(BUDGET_SELECT + " WHERE hc.household_id = :householdId"
            + " ORDER BY spent_minor DESC, c.name COLLATE NOCASE")
    LiveData<List<HouseholdCategoryBudget>> observeCategoryBudgets(
            long householdId, long fromDate, long toDate, String currency);

    @Query(BUDGET_SELECT + " WHERE hc.household_id = :householdId"
            + " ORDER BY spent_minor DESC, c.name COLLATE NOCASE")
    List<HouseholdCategoryBudget> getCategoryBudgetsSync(
            long householdId, long fromDate, long toDate, String currency);

    @Query("SELECT COUNT(*) FROM household_categories WHERE household_id = :householdId")
    int countCategories(long householdId);

    @Query("SELECT COUNT(*) FROM transactions WHERE household_category_id = :categoryId")
    int countExpensesForCategory(long categoryId);

    // --- Expenses (normal transactions, household-scoped) -----------------

    @Query(EXPENSE_SELECT + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " AND t.date >= :fromDate AND t.date <= :toDate"
            + " AND (:categoryId IS NULL OR t.household_category_id = :categoryId)"
            + " AND (:accountId IS NULL OR t.account_id = :accountId)"
            + " AND (:memberId IS NULL OR t.household_member_id = :memberId)"
            + " AND (t.title LIKE :query OR t.description LIKE :query)"
            + " ORDER BY CASE WHEN :sort = 0 THEN t.date END DESC,"
            + " CASE WHEN :sort = 1 THEN t.date END ASC,"
            + " CASE WHEN :sort = 2 THEN t.amount_minor END DESC,"
            + " CASE WHEN :sort = 3 THEN t.amount_minor END ASC, t.id DESC")
    LiveData<List<HouseholdExpense>> observeExpenses(long householdId, String query,
                                                     long fromDate, long toDate,
                                                     Long categoryId, Long accountId,
                                                     Long memberId, int sort);

    @Query(EXPENSE_SELECT + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " ORDER BY t.date DESC, t.id DESC LIMIT :limit")
    LiveData<List<HouseholdExpense>> observeRecentExpenses(long householdId, int limit);

    @Query(EXPENSE_SELECT + " WHERE t.id = :id")
    HouseholdExpense getExpense(long id);

    @Query(EXPENSE_SELECT + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " ORDER BY t.date DESC, t.id DESC LIMIT :limit")
    List<HouseholdExpense> getRecentExpensesSync(long householdId, int limit);

    @Query(EXPENSE_SELECT + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " AND t.date >= :fromDate AND t.date <= :toDate AND a.currency = :currency"
            + " ORDER BY t.date ASC, t.id ASC")
    List<HouseholdExpense> getExpensesForExport(long householdId, long fromDate, long toDate,
                                                String currency);

    @Query("SELECT COUNT(*) FROM transactions WHERE household_id = :householdId"
            + " AND type = 'EXPENSE'")
    int countExpenses(long householdId);

    @Query("SELECT COUNT(*) FROM transactions t JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " AND t.date >= :fromDate AND t.date <= :toDate AND a.currency != :currency")
    int countForeignExpenses(long householdId, long fromDate, long toDate, String currency);

    @Query("SELECT COUNT(*) FROM transactions WHERE household_member_id = :memberId")
    int countExpensesForMember(long memberId);

    @Query("SELECT COALESCE(SUM(t.amount_minor), 0) FROM transactions t"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " AND t.date >= :fromDate AND t.date <= :toDate AND a.currency = :currency")
    long totalExpense(long householdId, long fromDate, long toDate, String currency);

    @Query("SELECT t.category_id AS category_id, c.name AS category_name,"
            + " c.icon AS category_icon, c.color AS category_color,"
            + " SUM(t.amount_minor) AS total_minor"
            + " FROM transactions t"
            + " LEFT JOIN categories c ON c.id = t.category_id"
            + " JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " AND t.date >= :fromDate AND t.date <= :toDate AND a.currency = :currency"
            + " GROUP BY t.category_id ORDER BY total_minor DESC")
    List<CategoryTotal> categoryTotals(long householdId, long fromDate, long toDate,
                                       String currency);

    @Query("SELECT t.date AS day_millis, SUM(t.amount_minor) AS total_minor"
            + " FROM transactions t JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " AND t.date >= :fromDate AND t.date <= :toDate AND a.currency = :currency"
            + " GROUP BY t.date ORDER BY t.date ASC")
    List<DailyTotal> dailyTotals(long householdId, long fromDate, long toDate, String currency);

    @Query("SELECT strftime('%Y-%m', t.date / 1000, 'unixepoch', 'localtime') AS month_key,"
            + " COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount_minor ELSE 0 END), 0)"
            + " AS expense_minor, 0 AS income_minor"
            + " FROM transactions t JOIN accounts a ON a.id = t.account_id"
            + " WHERE t.household_id = :householdId AND t.type = 'EXPENSE'"
            + " AND t.date >= :fromDate AND t.date <= :toDate AND a.currency = :currency"
            + " GROUP BY month_key ORDER BY month_key ASC")
    List<MonthlyTotal> monthlyTotals(long householdId, long fromDate, long toDate,
                                     String currency);

    // --- Recurring (reuses the existing scheduler) ------------------------

    @Query("SELECT * FROM recurring_transactions WHERE household_id = :householdId"
            + " ORDER BY is_active DESC, next_run_date ASC")
    LiveData<List<RecurringTransaction>> observeRecurring(long householdId);

    @Query("SELECT * FROM recurring_transactions WHERE household_id = :householdId"
            + " AND type = 'EXPENSE' AND is_active = 1"
            + " AND next_run_date >= :fromDate AND next_run_date <= :toDate"
            + " ORDER BY next_run_date ASC")
    List<RecurringTransaction> getUpcomingRecurring(long householdId, long fromDate, long toDate);
}
