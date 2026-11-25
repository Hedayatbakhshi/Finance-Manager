package com.financemanager.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A person who can be tagged on household expenses. Purely local metadata. */
@Entity(tableName = "household_members",
        foreignKeys = @ForeignKey(entity = Household.class, parentColumns = "id",
                childColumns = "household_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"household_id", "name"}, unique = true),
                @Index("household_id")})
public class HouseholdMember {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "household_id")
    public long householdId;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
