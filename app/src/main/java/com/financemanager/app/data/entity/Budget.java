package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A spending limit for one expense category over a date range. */
@Entity(tableName = "budgets",
        foreignKeys = @ForeignKey(entity = Category.class, parentColumns = "id",
                childColumns = "category_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index("category_id"), @Index("start_date")})
public class Budget {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "category_id")
    public long categoryId;

    @ColumnInfo(name = "amount_minor")
    public long amountMinor;

    /** Local midnight timestamp of the first day (inclusive). */
    @ColumnInfo(name = "start_date")
    public long startDate;

    /** Local midnight timestamp of the last day (inclusive). */
    @ColumnInfo(name = "end_date")
    public long endDate;

    /**
     * When true, the unspent amount of the immediately preceding budget for the
     * same category is added to this budget's limit.
     */
    @ColumnInfo(name = "rollover", defaultValue = "0")
    public boolean rollover;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
