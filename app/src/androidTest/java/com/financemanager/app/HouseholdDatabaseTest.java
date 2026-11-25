package com.financemanager.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.HouseholdCategory;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.AccountType;
import com.financemanager.app.models.RecurrenceFrequency;
import com.financemanager.app.models.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** End-to-end checks for household rows living inside the existing ledger. */
@RunWith(AndroidJUnit4.class)
public class HouseholdDatabaseTest {

    private FinanceDatabase database;
    private long householdId;
    private long accountId;
    private long categoryId;
    private long linkId;
    private long memberId;
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_800_000_000_000L;
    private static final long DATE = 1_750_000_000_000L;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), FinanceDatabase.class)
                .allowMainThreadQueries()
                .build();
        Account account = new Account();
        account.name = "Household cash";
        account.type = AccountType.CASH;
        account.initialBalanceMinor = 0;
        account.currency = "USD";
        account.color = "#3E6DB5";
        account.icon = "account_cash";
        account.createdAt = System.currentTimeMillis();
        account.updatedAt = account.createdAt;
        accountId = database.accountDao().insert(account);

        Category category = new Category();
        category.name = "Groceries";
        category.type = TransactionType.EXPENSE;
        category.icon = "cat_food";
        category.color = "#E5A43B";
        category.createdAt = System.currentTimeMillis();
        categoryId = database.categoryDao().insert(category);

        Household household = new Household();
        household.name = "Test family";
        household.currency = "USD";
        household.monthlyBudgetMinor = 100_000L;
        household.createdAt = System.currentTimeMillis();
        household.updatedAt = household.createdAt;
        householdId = database.householdDao().insert(household);

        HouseholdMember member = new HouseholdMember();
        member.householdId = householdId;
        member.name = "Sara";
        member.createdAt = System.currentTimeMillis();
        memberId = database.householdDao().insertMember(member);

        HouseholdCategory link = new HouseholdCategory();
        link.householdId = householdId;
        link.categoryId = categoryId;
        link.monthlyBudgetMinor = 50_000L;
        link.createdAt = System.currentTimeMillis();
        link.updatedAt = link.createdAt;
        linkId = database.householdDao().insertCategory(link);
    }

    @After
    public void tearDown() {
        database.close();
    }

    private TransactionEntity expense(long amountMinor) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.title = "Naan and rice";
        transaction.amountMinor = amountMinor;
        transaction.type = TransactionType.EXPENSE;
        transaction.accountId = accountId;
        transaction.categoryId = categoryId;
        transaction.date = DATE;
        transaction.householdId = householdId;
        transaction.householdCategoryId = linkId;
        transaction.householdMemberId = memberId;
        transaction.createdAt = System.currentTimeMillis();
        transaction.updatedAt = transaction.createdAt;
        return transaction;
    }

    @Test
    public void expenseIsOneLedgerRowExposedThroughHouseholdQueries() {
        long id = database.transactionDao().insert(expense(12_345L));

        assertEquals(1, database.transactionDao().count());
        assertEquals(12_345L,
                database.householdDao().totalExpense(householdId, FROM, TO, "USD"));
        assertEquals(0L,
                database.householdDao().totalExpense(householdId, FROM, TO, "EUR"));
        assertEquals(1, database.householdDao().countExpenses(householdId));
        assertEquals("Sara", database.householdDao().getExpense(id).memberName);
        assertEquals(1, database.householdDao().categoryTotals(
                householdId, FROM, TO, "USD").size());
        assertEquals(12_345L, database.householdDao().getCategoryBudgetsSync(
                householdId, FROM, TO, "USD").get(0).spentMinor);

        // Editing and deleting must not duplicate the row.
        TransactionEntity changed = database.transactionDao().getById(id);
        changed.amountMinor = 20_000L;
        database.transactionDao().update(changed);
        assertEquals(1, database.transactionDao().count());
        assertEquals(20_000L,
                database.householdDao().totalExpense(householdId, FROM, TO, "USD"));

        database.transactionDao().deleteById(id);
        assertEquals(0, database.transactionDao().count());
        assertEquals(0L,
                database.householdDao().totalExpense(householdId, FROM, TO, "USD"));
    }

    @Test
    public void recurringHouseholdExpenseKeepsItsHouseholdLink() {
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.title = "Monthly rent";
        recurring.amountMinor = 30_000L;
        recurring.type = TransactionType.EXPENSE;
        recurring.frequency = RecurrenceFrequency.MONTHLY;
        recurring.accountId = accountId;
        recurring.categoryId = categoryId;
        recurring.nextRunDate = DATE;
        recurring.householdId = householdId;
        recurring.householdCategoryId = linkId;
        recurring.householdMemberId = memberId;
        recurring.createdAt = System.currentTimeMillis();
        recurring.updatedAt = recurring.createdAt;
        long id = database.recurringDao().insert(recurring);

        FinanceRepository repository = new FinanceRepository(database,
                ApplicationProvider.getApplicationContext());
        int created = repository.generateDueTransactions(DATE);
        assertEquals(1, created);
        // Running again must not duplicate the generated entry.
        assertEquals(0, repository.generateDueTransactions(DATE));

        List<TransactionEntity> generated = database.transactionDao().getForRecurring(id);
        assertEquals(1, generated.size());
        assertNotNull(generated.get(0).householdId);
        assertEquals(householdId, generated.get(0).householdId.longValue());
        assertEquals(linkId, generated.get(0).householdCategoryId.longValue());
        assertEquals(memberId, generated.get(0).householdMemberId.longValue());
        assertTrue(database.householdDao().getUpcomingRecurring(householdId, DATE, TO).isEmpty()
                || database.householdDao().getUpcomingRecurring(householdId, DATE, TO)
                .get(0).id == id);
    }
}
