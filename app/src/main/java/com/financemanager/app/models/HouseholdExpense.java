package com.financemanager.app.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** A household expense row: the normal transaction plus its optional member. */
public class HouseholdExpense {

    @Embedded
    public TransactionWithDetails transaction;

    @ColumnInfo(name = "member_name")
    public String memberName;
}
