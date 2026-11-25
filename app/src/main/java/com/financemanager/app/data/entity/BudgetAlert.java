package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Records that a budget threshold was already announced in a given budget period. */
@Entity(tableName = "budget_alerts",
        foreignKeys = @ForeignKey(entity = Budget.class, parentColumns = "id",
                childColumns = "budget_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"budget_id", "period_key", "threshold"}, unique = true)})
public class BudgetAlert {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "budget_id")
    public long budgetId;

    @NonNull
    @ColumnInfo(name = "period_key")
    public String periodKey = "";

    /** 50, 80, 100 or 101 for "over budget". */
    @ColumnInfo(name = "threshold")
    public int threshold;

    @ColumnInfo(name = "notified_at")
    public long notifiedAt;
}
