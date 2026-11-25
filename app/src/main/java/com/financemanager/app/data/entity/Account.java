package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.financemanager.app.models.AccountType;

/**
 * A financial account (cash, bank, savings, credit card or wallet).
 * Balances are never stored directly: they are derived from the initial balance
 * plus all linked transactions so they can never drift out of sync.
 */
@Entity(tableName = "accounts", indices = {@Index(value = "name", unique = true)})
public class Account {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @NonNull
    @ColumnInfo(name = "type")
    public AccountType type = AccountType.CASH;

    /** Opening balance in minor units (cents). */
    @ColumnInfo(name = "initial_balance_minor")
    public long initialBalanceMinor;

    @NonNull
    @ColumnInfo(name = "currency")
    public String currency = "USD";

    /** Hex color used for the account avatar. */
    @NonNull
    @ColumnInfo(name = "color")
    public String color = "#3E6DB5";

    /** Drawable resource name, e.g. "account_cash". */
    @NonNull
    @ColumnInfo(name = "icon")
    public String icon = "account_cash";

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
