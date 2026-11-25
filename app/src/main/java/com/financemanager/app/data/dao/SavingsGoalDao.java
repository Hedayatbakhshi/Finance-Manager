package com.financemanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.financemanager.app.data.entity.SavingsGoal;

import java.util.List;

@Dao
public interface SavingsGoalDao {

    @Insert
    long insert(SavingsGoal goal);

    @Update
    void update(SavingsGoal goal);

    @Delete
    void delete(SavingsGoal goal);

    @Query("DELETE FROM savings_goals WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    SavingsGoal getById(long id);

    @Query("SELECT * FROM savings_goals ORDER BY created_at DESC")
    LiveData<List<SavingsGoal>> observeAll();

    @Query("SELECT * FROM savings_goals ORDER BY created_at DESC")
    List<SavingsGoal> getAllSync();

    /** Atomic adjustment: fails (0 rows) rather than allowing negative savings. */
    @Query("UPDATE savings_goals SET current_minor = current_minor + :delta, updated_at = :now"
            + " WHERE id = :id AND current_minor + :delta >= 0")
    int adjustCurrent(long id, long delta, long now);

    @Query("SELECT COUNT(*) FROM savings_goals")
    int count();
}
