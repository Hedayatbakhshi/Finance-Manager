package com.financemanager.app.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;

import com.financemanager.app.R;
import com.financemanager.app.models.AccountType;
import com.financemanager.app.models.RecurrenceFrequency;
import com.financemanager.app.models.ReportPeriod;
import com.financemanager.app.models.TransactionType;

/** Maps domain enums to localized strings. */
public final class Labels {

    private Labels() {
    }

    public static String accountType(Context context, @NonNull AccountType type) {
        switch (type) {
            case BANK:
                return context.getString(R.string.account_type_bank);
            case SAVINGS:
                return context.getString(R.string.account_type_savings);
            case CREDIT:
                return context.getString(R.string.account_type_credit);
            case WALLET:
                return context.getString(R.string.account_type_wallet);
            case CASH:
            default:
                return context.getString(R.string.account_type_cash);
        }
    }

    public static String transactionType(Context context, @NonNull TransactionType type) {
        switch (type) {
            case INCOME:
                return context.getString(R.string.type_income);
            case TRANSFER:
                return context.getString(R.string.type_transfer);
            case EXPENSE:
            default:
                return context.getString(R.string.type_expense);
        }
    }

    public static String frequency(Context context, @NonNull RecurrenceFrequency frequency) {
        switch (frequency) {
            case DAILY:
                return context.getString(R.string.frequency_daily);
            case WEEKLY:
                return context.getString(R.string.frequency_weekly);
            case YEARLY:
                return context.getString(R.string.frequency_yearly);
            case MONTHLY:
            default:
                return context.getString(R.string.frequency_monthly);
        }
    }

    public static String period(Context context, @NonNull ReportPeriod period) {
        switch (period) {
            case THIS_WEEK:
                return context.getString(R.string.date_range_week);
            case THIS_MONTH:
                return context.getString(R.string.date_range_month);
            case LAST_MONTH:
                return context.getString(R.string.date_range_last_month);
            case LAST_3_MONTHS:
                return context.getString(R.string.date_range_3_months);
            case THIS_YEAR:
                return context.getString(R.string.date_range_year);
            case CUSTOM:
                return context.getString(R.string.date_range_custom);
            case ALL_TIME:
            default:
                return context.getString(R.string.all);
        }
    }

    public static String accountIconKey(@NonNull AccountType type) {
        switch (type) {
            case BANK:
                return "account_bank";
            case SAVINGS:
                return "account_savings";
            case CREDIT:
                return "account_credit";
            case WALLET:
                return "account_wallet";
            case CASH:
            default:
                return "account_cash";
        }
    }
}
