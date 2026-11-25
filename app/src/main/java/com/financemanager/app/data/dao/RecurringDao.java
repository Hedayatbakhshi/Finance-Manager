package com.financemanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.financemanager.app.data.entity.RecurringTransaction;

import java.util.List;

@Dao
public interface RecurringDao {

    @Insert
    long insert(RecurringTransaction recurring);

    @Update
    void update(RecurringTransaction recurring);

    @Delete
    void delete(RecurringTransaction recurring);

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    RecurringTransaction getById(long id);

    @Query("SELECT * FROM recurring_transactions ORDER BY is_active DESC, next_run_date ASC")
    LiveData<List<RecurringTransaction>> observeAll();

    @Query("SELECT * FROM recurring_transactions ORDER BY is_active DESC, next_run_date ASC")
    List<RecurringTransaction> getAllSync();

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1 AND next_run_date <= :upTo"
            + " ORDER BY next_run_date ASC")
    List<RecurringTransaction> getDue(long upTo);

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1"
            + " AND next_run_date > :after AND next_run_date <= :upTo ORDER BY next_run_date ASC")
    List<RecurringTransaction> getUpcoming(long after, long upTo);

    @Query("UPDATE recurring_transactions SET is_active = :active, updated_at = :now WHERE id = :id")
    void setActive(long id, boolean active, long now);

    @Query("UPDATE recurring_transactions SET next_run_date = :nextDate, updated_at = :now WHERE id = :id")
    void setNextRun(long id, long nextDate, long now);

    @Query("SELECT COUNT(*) FROM recurring_transactions")
    int count();
}
