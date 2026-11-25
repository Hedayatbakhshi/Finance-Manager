package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.financemanager.app.models.TransactionType;

/** An income or expense category. Transfers are not categorized. */
@Entity(tableName = "categories",
        indices = {@Index(value = {"name", "type"}, unique = true)})
public class Category {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @NonNull
    @ColumnInfo(name = "type")
    public TransactionType type = TransactionType.EXPENSE;

    @NonNull
    @ColumnInfo(name = "icon")
    public String icon = "cat_other";

    @NonNull
    @ColumnInfo(name = "color")
    public String color = "#778397";

    @ColumnInfo(name = "is_default")
    public boolean isDefault;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
