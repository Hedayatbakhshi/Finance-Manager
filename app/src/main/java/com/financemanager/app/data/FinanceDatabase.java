package com.financemanager.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.financemanager.app.data.dao.AccountDao;
import com.financemanager.app.data.dao.BudgetAlertDao;
import com.financemanager.app.data.dao.BudgetDao;
import com.financemanager.app.data.dao.CategoryDao;
import com.financemanager.app.data.dao.HouseholdDao;
import com.financemanager.app.data.dao.RecurringDao;
import com.financemanager.app.data.dao.SavingsGoalDao;
import com.financemanager.app.data.dao.TransactionDao;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Budget;
import com.financemanager.app.data.entity.BudgetAlert;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.HouseholdCategory;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.data.entity.TransactionEntity;

/**
 * Local Room database. Schema version 2 adds local household profiles.
 * concrete {@code Migration}; destructive migrations are intentionally not enabled
 * because losing financial history is never acceptable.
 */
@Database(
        entities = {
                Account.class,
                Category.class,
                TransactionEntity.class,
                Budget.class,
                SavingsGoal.class,
                RecurringTransaction.class,
                BudgetAlert.class,
                Household.class,
                HouseholdMember.class,
                HouseholdCategory.class
        },
        version = 2,
        exportSchema = true)
@TypeConverters(Converters.class)
public abstract class FinanceDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "finance_manager.db";
    private static volatile FinanceDatabase instance;

    public abstract AccountDao accountDao();

    public abstract CategoryDao categoryDao();

    public abstract TransactionDao transactionDao();

    public abstract BudgetDao budgetDao();

    public abstract SavingsGoalDao savingsGoalDao();

    public abstract RecurringDao recurringDao();

    public abstract BudgetAlertDao budgetAlertDao();

    public abstract HouseholdDao householdDao();

    public static FinanceDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (FinanceDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    FinanceDatabase.class, DATABASE_NAME)
                            .addMigrations(new Migration1To2())
                            .build();
                }
            }
        }
        return instance;
    }
}
