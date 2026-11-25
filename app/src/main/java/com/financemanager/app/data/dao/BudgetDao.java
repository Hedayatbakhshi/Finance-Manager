package com.financemanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.financemanager.app.data.entity.Budget;
import com.financemanager.app.models.BudgetWithProgress;

import java.util.List;

@Dao
public interface BudgetDao {

    /**
     * Adds the amount spent by the budget's category inside its own date range.
     * Spending is restricted to accounts in the reporting currency so multi-currency
     * accounts are never silently combined.
     */
    String BUDGET_SELECT = "SELECT b.*, c.name AS category_name, c.color AS category_color,"
            + " c.icon AS category_icon,"
            + " COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     JOIN accounts a ON a.id = t.account_id"
            + "     WHERE t.type = 'EXPENSE' AND t.category_id = b.category_id"
            + "     AND t.date >= b.start_date AND t.date <= b.end_date"
            + "     AND a.currency = :currency), 0) AS spent_minor"
            + " FROM budgets b LEFT JOIN categories c ON c.id = b.category_id";

    @Insert
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("DELETE FROM budgets WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM budgets WHERE id = :id")
    Budget getById(long id);

    @Query(BUDGET_SELECT + " WHERE b.id = :id")
    BudgetWithProgress getWithProgress(long id, String currency);

    @Query(BUDGET_SELECT + " ORDER BY b.start_date DESC, b.id DESC")
    LiveData<List<BudgetWithProgress>> observeAll(String currency);

    @Query(BUDGET_SELECT + " ORDER BY b.start_date DESC, b.id DESC")
    List<BudgetWithProgress> getAllWithProgress(String currency);

    @Query("SELECT * FROM budgets ORDER BY start_date ASC")
    List<Budget> getAllSync();

    @Query(BUDGET_SELECT + " WHERE b.start_date <= :date AND b.end_date >= :date"
            + " ORDER BY b.end_date ASC")
    List<BudgetWithProgress> getActiveOn(long date, String currency);

    @Query("SELECT COUNT(*) FROM budgets")
    int count();

    @Query("SELECT COUNT(*) FROM budgets WHERE category_id = :categoryId")
    int countForCategory(long categoryId);
}
