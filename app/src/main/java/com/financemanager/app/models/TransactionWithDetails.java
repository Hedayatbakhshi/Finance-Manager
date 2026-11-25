package com.financemanager.app.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.financemanager.app.data.entity.TransactionEntity;

/** A transaction joined with its category and account display data. */
public class TransactionWithDetails {

    @Embedded
    public TransactionEntity transaction;

    @ColumnInfo(name = "category_name")
    public String categoryName;

    @ColumnInfo(name = "category_icon")
    public String categoryIcon;

    @ColumnInfo(name = "category_color")
    public String categoryColor;

    @ColumnInfo(name = "account_name")
    public String accountName;

    @ColumnInfo(name = "to_account_name")
    public String toAccountName;

    @ColumnInfo(name = "account_currency")
    public String accountCurrency;
}
