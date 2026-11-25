package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A savings target. Goals track progress independently from account balances so money
 * is never double-counted; users move money in and out of a goal explicitly.
 */
@Entity(tableName = "savings_goals")
public class SavingsGoal {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "target_minor")
    public long targetMinor;

    @ColumnInfo(name = "current_minor")
    public long currentMinor;

    /** Local midnight timestamp, 0 when no deadline is set. */
    @ColumnInfo(name = "target_date")
    public long targetDate;

    @NonNull
    @ColumnInfo(name = "description")
    public String description = "";

    @NonNull
    @ColumnInfo(name = "icon")
    public String icon = "goal_target";

    @NonNull
    @ColumnInfo(name = "color")
    public String color = "#16A36A";

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
