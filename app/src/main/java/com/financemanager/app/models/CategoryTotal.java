package com.financemanager.app.models;

import androidx.room.ColumnInfo;

/** Total amount for one category inside a date range. */
public class CategoryTotal {

    @ColumnInfo(name = "category_id")
    public Long categoryId;

    @ColumnInfo(name = "category_name")
    public String categoryName;

    @ColumnInfo(name = "category_icon")
    public String categoryIcon;

    @ColumnInfo(name = "category_color")
    public String categoryColor;

    @ColumnInfo(name = "total_minor")
    public long totalMinor;
}
