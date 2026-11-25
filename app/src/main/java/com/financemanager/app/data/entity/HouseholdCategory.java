package com.financemanager.app.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Links an existing expense category to a household and optionally gives it a
 * monthly limit. Categories are never duplicated: the global categories table
 * remains the single source of names, icons and colours.
 */
@Entity(tableName = "household_categories",
        foreignKeys = {
                @ForeignKey(entity = Household.class, parentColumns = "id",
                        childColumns = "household_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class, parentColumns = "id",
                        childColumns = "category_id", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index(value = {"household_id", "category_id"}, unique = true),
                @Index("category_id")})
public class HouseholdCategory {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "household_id")
    public long householdId;

    @ColumnInfo(name = "category_id")
    public long categoryId;

    /** Optional monthly limit in minor units; 0 means no per-category limit. */
    @ColumnInfo(name = "monthly_budget_minor")
    public long monthlyBudgetMinor;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
