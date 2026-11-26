package com.financemanager.app.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.financemanager.app.data.entity.Budget;

/** A budget joined with its category and the amount spent inside the budget period. */
public class BudgetWithProgress {

    @Embedded
    public Budget budget;

    @ColumnInfo(name = "category_name")
    public String categoryName;

    @ColumnInfo(name = "category_color")
    public String categoryColor;

    @ColumnInfo(name = "category_icon")
    public String categoryIcon;

    @ColumnInfo(name = "spent_minor")
    public long spentMinor;

    /** Unspent amount carried over from the preceding budget for the same category. */
    @ColumnInfo(name = "carryover_minor")
    public long carryoverMinor;

    /** The limit that spending is measured against, including any rollover. */
    public long availableMinor() {
        return budget.amountMinor + carryoverMinor;
    }
}
