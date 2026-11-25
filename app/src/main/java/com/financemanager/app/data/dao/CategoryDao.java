package com.financemanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.financemanager.app.data.entity.Category;
import com.financemanager.app.models.TransactionType;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Category category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("DELETE FROM categories WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getById(long id);

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY is_default DESC, name COLLATE NOCASE ASC")
    List<Category> getByTypeSync(TransactionType type);

    @Query("SELECT * FROM categories ORDER BY type ASC, is_default DESC, name COLLATE NOCASE ASC")
    LiveData<List<Category>> observeAll();

    @Query("SELECT * FROM categories ORDER BY type ASC, name COLLATE NOCASE ASC")
    List<Category> getAllSync();

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY is_default DESC, name COLLATE NOCASE ASC")
    LiveData<List<Category>> observeByType(TransactionType type);

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    Category findByNameAndType(String name, TransactionType type);

    @Query("SELECT * FROM categories WHERE type = :type AND icon = 'cat_other'"
            + " AND is_default = 1 LIMIT 1")
    Category findDefaultOther(TransactionType type);

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type AND id != :excludeId LIMIT 1")
    Category findDuplicate(String name, TransactionType type, long excludeId);

    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type")
    int countByType(TransactionType type);
}
