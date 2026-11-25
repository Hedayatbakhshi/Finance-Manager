package com.financemanager.app.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.financemanager.app.data.entity.HouseholdCategory;

/** A linked category with its monthly limit and spending in the selected period. */
public class HouseholdCategoryBudget {

    @Embedded
    public HouseholdCategory category;

    @ColumnInfo(name = "category_name")
    public String categoryName;

    @ColumnInfo(name = "category_icon")
    public String categoryIcon;

    @ColumnInfo(name = "category_color")
    public String categoryColor;

    @ColumnInfo(name = "spent_minor")
    public long spentMinor;
}
