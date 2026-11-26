package com.financemanager.app.utils;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.financemanager.app.data.FinanceDatabase;

/**
 * Deletes every user record while keeping the schema and defaults intact.
 * Callers are responsible for running this inside a database transaction and for
 * re-seeding default categories/accounts afterwards when needed.
 */
public final class DataEraser {

    private DataEraser() {
    }

    public static void clearAll(FinanceDatabase database) {
        SupportSQLiteDatabase sql = database.getOpenHelper().getWritableDatabase();
        // Children first so foreign keys never block the deletes.
        sql.execSQL("DELETE FROM household_members");
        sql.execSQL("DELETE FROM household_categories");
        sql.execSQL("DELETE FROM households");
        sql.execSQL("DELETE FROM budget_alerts");
        sql.execSQL("DELETE FROM budgets");
        sql.execSQL("DELETE FROM savings_goals");
        sql.execSQL("DELETE FROM recurring_transactions");
        sql.execSQL("DELETE FROM transactions");
        sql.execSQL("DELETE FROM categories");
        sql.execSQL("DELETE FROM accounts");
    }
}
