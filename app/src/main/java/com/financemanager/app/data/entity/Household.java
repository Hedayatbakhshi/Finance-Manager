package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A local household profile with its own monthly budget and currency. */
@Entity(tableName = "households",
        indices = {@Index(value = "name", unique = true)})
public class Household {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @NonNull
    @ColumnInfo(name = "description")
    public String description = "";

    /** Monthly household budget in minor units; 0 means "not set". */
    @ColumnInfo(name = "monthly_budget_minor")
    public long monthlyBudgetMinor;

    @NonNull
    @ColumnInfo(name = "currency")
    public String currency = "AFN";

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
