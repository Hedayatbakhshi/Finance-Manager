package com.financemanager.app.data;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Adds the optional budget rollover flag. Existing budgets keep their exact
 * limits because the new column defaults to 0 (no rollover).
 */
public final class Migration2To3 extends Migration {

    public Migration2To3() {
        super(2, 3);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("ALTER TABLE `budgets` ADD COLUMN `rollover` INTEGER NOT NULL DEFAULT 0");
    }
}
