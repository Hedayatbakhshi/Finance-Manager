package com.financemanager.app.data;

import androidx.room.TypeConverter;

import com.financemanager.app.models.AccountType;
import com.financemanager.app.models.RecurrenceFrequency;
import com.financemanager.app.models.TransactionType;

/** Stores enums by name so the schema stays readable in raw SQL. */
public final class Converters {

    @TypeConverter
    public static TransactionType toTransactionType(String value) {
        return value == null ? null : TransactionType.fromKey(value);
    }

    @TypeConverter
    public static String fromTransactionType(TransactionType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static AccountType toAccountType(String value) {
        return value == null ? null : AccountType.fromKey(value);
    }

    @TypeConverter
    public static String fromAccountType(AccountType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static RecurrenceFrequency toFrequency(String value) {
        return value == null ? null : RecurrenceFrequency.fromKey(value);
    }

    @TypeConverter
    public static String fromFrequency(RecurrenceFrequency value) {
        return value == null ? null : value.name();
    }
}
