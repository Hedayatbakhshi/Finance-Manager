package com.financemanager.app.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.financemanager.app.data.entity.Account;

/** An account together with its calculated balance (initial balance plus all movements). */
public class AccountWithBalance {

    @Embedded
    public Account account;

    /** Computed in SQL from the account's initial balance and its transactions. */
    @ColumnInfo(name = "balance_minor")
    public long balanceMinor;

    @NonNull
    @Override
    public String toString() {
        return account.name + " (" + balanceMinor + ")";
    }
}
