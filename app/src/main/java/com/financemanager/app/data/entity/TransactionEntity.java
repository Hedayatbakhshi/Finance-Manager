package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.financemanager.app.models.TransactionType;

/**
 * A single money movement.
 *
 * <p>Transfers are stored as one row: {@link #accountId} is the source account and
 * {@link #toAccountId} the destination. Keeping both sides in one row makes a transfer
 * atomic by construction, so the two account balances can never disagree.</p>
 */
@Entity(tableName = "transactions",
        foreignKeys = {
                @ForeignKey(entity = Account.class, parentColumns = "id", childColumns = "account_id",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Account.class, parentColumns = "id", childColumns = "to_account_id",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "category_id",
                        onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = Household.class, parentColumns = "id", childColumns = "household_id",
                        onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = HouseholdCategory.class, parentColumns = "id",
                        childColumns = "household_category_id", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = HouseholdMember.class, parentColumns = "id",
                        childColumns = "household_member_id", onDelete = ForeignKey.SET_NULL)
        },
        indices = {
                @Index("account_id"),
                @Index("to_account_id"),
                @Index("category_id"),
                @Index("date"),
                @Index("type"),
                @Index("recurring_id"),
                @Index("household_id"),
                @Index("household_category_id"),
                @Index("household_member_id")
        })
public class TransactionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    /** Amount in minor units (cents). Always positive. */
    @ColumnInfo(name = "amount_minor")
    public long amountMinor;

    @NonNull
    @ColumnInfo(name = "type")
    public TransactionType type = TransactionType.EXPENSE;

    /** Null for transfers. */
    @ColumnInfo(name = "category_id")
    public Long categoryId;

    /** Source account for expenses/transfers, destination for income. */
    @ColumnInfo(name = "account_id")
    public long accountId;

    /** Destination account for transfers only. */
    @ColumnInfo(name = "to_account_id")
    public Long toAccountId;

    /** Local midnight timestamp of the transaction date. */
    @ColumnInfo(name = "date")
    public long date;

    @NonNull
    @ColumnInfo(name = "description")
    public String description = "";

    @NonNull
    @ColumnInfo(name = "notes")
    public String notes = "";

    @ColumnInfo(name = "recurring_id")
    public Long recurringId;

    /** Set when this row is a household expense; the row is still a normal transaction. */
    @ColumnInfo(name = "household_id")
    public Long householdId;

    /** Household-specific category link (the global category_id stays authoritative). */
    @ColumnInfo(name = "household_category_id")
    public Long householdCategoryId;

    @ColumnInfo(name = "household_member_id")
    public Long householdMemberId;

    /** Internal relative path to a locally stored receipt, when attached. */
    @ColumnInfo(name = "receipt_path")
    public String receiptPath;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public boolean isTransfer() {
        return type == TransactionType.TRANSFER;
    }
}
