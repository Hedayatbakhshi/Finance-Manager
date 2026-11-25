package com.financemanager.app.data;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Adds local household profiles without touching any existing financial row.
 * Transaction and recurring tables are rebuilt so the new columns have real
 * foreign keys; every old value is copied verbatim.
 */
public final class Migration1To2 extends Migration {

    public Migration1To2() {
        super(1, 2);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=OFF");
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS `households` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`name` TEXT NOT NULL,"
                    + "`description` TEXT NOT NULL,"
                    + "`monthly_budget_minor` INTEGER NOT NULL,"
                    + "`currency` TEXT NOT NULL,"
                    + "`created_at` INTEGER NOT NULL,"
                    + "`updated_at` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_households_name`"
                    + " ON `households` (`name`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `household_members` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`household_id` INTEGER NOT NULL,"
                    + "`name` TEXT NOT NULL,"
                    + "`created_at` INTEGER NOT NULL,"
                    + "FOREIGN KEY(`household_id`) REFERENCES `households`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS"
                    + " `index_household_members_household_id_name`"
                    + " ON `household_members` (`household_id`, `name`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_household_members_household_id`"
                    + " ON `household_members` (`household_id`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `household_categories` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`household_id` INTEGER NOT NULL,"
                    + "`category_id` INTEGER NOT NULL,"
                    + "`monthly_budget_minor` INTEGER NOT NULL,"
                    + "`created_at` INTEGER NOT NULL,"
                    + "`updated_at` INTEGER NOT NULL,"
                    + "FOREIGN KEY(`household_id`) REFERENCES `households`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE ,"
                    + "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS"
                    + " `index_household_categories_household_id_category_id`"
                    + " ON `household_categories` (`household_id`, `category_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_household_categories_category_id`"
                    + " ON `household_categories` (`category_id`)");

            db.execSQL("CREATE TABLE `transactions_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`title` TEXT NOT NULL,`amount_minor` INTEGER NOT NULL,"
                    + "`type` TEXT NOT NULL,`category_id` INTEGER,"
                    + "`account_id` INTEGER NOT NULL,`to_account_id` INTEGER,"
                    + "`date` INTEGER NOT NULL,`description` TEXT NOT NULL,"
                    + "`notes` TEXT NOT NULL,`recurring_id` INTEGER,"
                    + "`household_id` INTEGER,`household_category_id` INTEGER,"
                    + "`household_member_id` INTEGER,`receipt_path` TEXT,"
                    + "`created_at` INTEGER NOT NULL,`updated_at` INTEGER NOT NULL,"
                    + "FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE ,"
                    + "FOREIGN KEY(`to_account_id`) REFERENCES `accounts`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE ,"
                    + "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL ,"
                    + "FOREIGN KEY(`household_id`) REFERENCES `households`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL ,"
                    + "FOREIGN KEY(`household_category_id`) REFERENCES `household_categories`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL ,"
                    + "FOREIGN KEY(`household_member_id`) REFERENCES `household_members`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL )");
            db.execSQL("INSERT INTO `transactions_new` (`id`,`title`,`amount_minor`,`type`,"
                    + "`category_id`,`account_id`,`to_account_id`,`date`,`description`,`notes`,"
                    + "`recurring_id`,`created_at`,`updated_at`)"
                    + " SELECT `id`,`title`,`amount_minor`,`type`,`category_id`,`account_id`,"
                    + "`to_account_id`,`date`,`description`,`notes`,`recurring_id`,"
                    + "`created_at`,`updated_at` FROM `transactions`");
            db.execSQL("DROP TABLE `transactions`");
            db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`");
            createTransactionIndices(db);

            db.execSQL("CREATE TABLE `recurring_transactions_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`title` TEXT NOT NULL,`amount_minor` INTEGER NOT NULL,"
                    + "`type` TEXT NOT NULL,`category_id` INTEGER,"
                    + "`account_id` INTEGER NOT NULL,`to_account_id` INTEGER,"
                    + "`frequency` TEXT NOT NULL,`next_run_date` INTEGER NOT NULL,"
                    + "`description` TEXT NOT NULL,`notes` TEXT NOT NULL,"
                    + "`household_id` INTEGER,`household_category_id` INTEGER,"
                    + "`household_member_id` INTEGER,"
                    + "`is_active` INTEGER NOT NULL,"
                    + "`created_at` INTEGER NOT NULL,`updated_at` INTEGER NOT NULL,"
                    + "FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE ,"
                    + "FOREIGN KEY(`to_account_id`) REFERENCES `accounts`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE ,"
                    + "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL ,"
                    + "FOREIGN KEY(`household_id`) REFERENCES `households`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL ,"
                    + "FOREIGN KEY(`household_category_id`) REFERENCES `household_categories`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL ,"
                    + "FOREIGN KEY(`household_member_id`) REFERENCES `household_members`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE SET NULL )");
            db.execSQL("INSERT INTO `recurring_transactions_new` (`id`,`title`,`amount_minor`,"
                    + "`type`,`category_id`,`account_id`,`to_account_id`,`frequency`,"
                    + "`next_run_date`,`description`,`notes`,`is_active`,`created_at`,`updated_at`)"
                    + " SELECT `id`,`title`,`amount_minor`,`type`,`category_id`,`account_id`,"
                    + "`to_account_id`,`frequency`,`next_run_date`,`description`,`notes`,"
                    + "`is_active`,`created_at`,`updated_at` FROM `recurring_transactions`");
            db.execSQL("DROP TABLE `recurring_transactions`");
            db.execSQL("ALTER TABLE `recurring_transactions_new` RENAME TO `recurring_transactions`");
            createRecurringIndices(db);
        } finally {
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    }

    private static void createTransactionIndices(SupportSQLiteDatabase db) {
        String[] columns = {"account_id", "to_account_id", "category_id", "date", "type",
                "recurring_id", "household_id", "household_category_id", "household_member_id"};
        for (String column : columns) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_" + column + "`"
                    + " ON `transactions` (`" + column + "`)");
        }
    }

    private static void createRecurringIndices(SupportSQLiteDatabase db) {
        String[] columns = {"account_id", "to_account_id", "category_id", "next_run_date",
                "household_id", "household_category_id", "household_member_id"};
        for (String column : columns) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_" + column + "`"
                    + " ON `recurring_transactions` (`" + column + "`)");
        }
    }
}
