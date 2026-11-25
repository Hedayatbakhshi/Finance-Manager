package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.financemanager.app.models.RecurrenceFrequency;
import com.financemanager.app.models.TransactionType;

/** A template that generates transactions on a schedule. */
@Entity(tableName = "recurring_transactions",
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
        indices = {@Index("account_id"), @Index("to_account_id"), @Index("category_id"),
                @Index("next_run_date"), @Index("household_id"),
                @Index("household_category_id"), @Index("household_member_id")})
public class RecurringTransaction {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    @ColumnInfo(name = "amount_minor")
    public long amountMinor;

    @NonNull
    @ColumnInfo(name = "type")
    public TransactionType type = TransactionType.EXPENSE;

    @ColumnInfo(name = "category_id")
    public Long categoryId;

    @ColumnInfo(name = "account_id")
    public long accountId;

    @ColumnInfo(name = "to_account_id")
    public Long toAccountId;

    @NonNull
    @ColumnInfo(name = "frequency")
    public RecurrenceFrequency frequency = RecurrenceFrequency.MONTHLY;

    /** Next date a transaction should be generated for (local midnight). */
    @ColumnInfo(name = "next_run_date")
    public long nextRunDate;

    @NonNull
    @ColumnInfo(name = "description")
    public String description = "";

    @NonNull
    @ColumnInfo(name = "notes")
    public String notes = "";

    @ColumnInfo(name = "household_id")
    public Long householdId;

    @ColumnInfo(name = "household_category_id")
    public Long householdCategoryId;

    @ColumnInfo(name = "household_member_id")
    public Long householdMemberId;

    @ColumnInfo(name = "is_active")
    public boolean active = true;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
