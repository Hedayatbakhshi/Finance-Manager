package com.financemanager.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.financemanager.app.data.entity.BudgetAlert;

@Dao
public interface BudgetAlertDao {

    /** Returns -1 when this threshold was already recorded for the period. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(BudgetAlert alert);

    @Query("SELECT COUNT(*) FROM budget_alerts WHERE budget_id = :budgetId"
            + " AND period_key = :periodKey AND threshold = :threshold")
    int count(long budgetId, String periodKey, int threshold);

    @Query("DELETE FROM budget_alerts WHERE budget_id = :budgetId")
    void deleteForBudget(long budgetId);

    @Query("DELETE FROM budget_alerts")
    void clear();
}
