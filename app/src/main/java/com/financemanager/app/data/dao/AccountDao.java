package com.financemanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.financemanager.app.data.entity.Account;
import com.financemanager.app.models.AccountWithBalance;

import java.util.List;

@Dao
public interface AccountDao {

    /**
     * Account balance = opening balance + income received in the account
     * - expenses paid from the account - transfers sent + transfers received.
     * Transfers therefore move money without changing overall net worth.
     */
    String BALANCE_SELECT = "SELECT a.*, a.initial_balance_minor"
            + " + COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     WHERE t.account_id = a.id AND t.type = 'INCOME'), 0)"
            + " - COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     WHERE t.account_id = a.id AND t.type = 'EXPENSE'), 0)"
            + " - COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     WHERE t.account_id = a.id AND t.type = 'TRANSFER'), 0)"
            + " + COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     WHERE t.to_account_id = a.id AND t.type = 'TRANSFER'), 0)"
            + " AS balance_minor FROM accounts a";

    /**
     * Net worth in one currency as of the end of {@code upTo}. Transfers between
     * accounts in the same currency cancel out, but transfers to or from a foreign
     * currency account legitimately move value, so both sides are counted.
     */
    String BALANCE_UP_TO_SELECT = "SELECT COALESCE(SUM(a.initial_balance_minor), 0)"
            + " + COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     JOIN accounts a2 ON a2.id = t.account_id"
            + "     WHERE t.type = 'INCOME' AND t.date <= :upTo AND a2.currency = :currency), 0)"
            + " - COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     JOIN accounts a2 ON a2.id = t.account_id"
            + "     WHERE t.type = 'EXPENSE' AND t.date <= :upTo AND a2.currency = :currency), 0)"
            + " - COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     JOIN accounts a2 ON a2.id = t.account_id"
            + "     WHERE t.type = 'TRANSFER' AND t.date <= :upTo AND a2.currency = :currency), 0)"
            + " + COALESCE((SELECT SUM(t.amount_minor) FROM transactions t"
            + "     JOIN accounts a2 ON a2.id = t.to_account_id"
            + "     WHERE t.type = 'TRANSFER' AND t.date <= :upTo AND a2.currency = :currency), 0)"
            + " FROM accounts a WHERE a.currency = :currency";

    @Insert
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("DELETE FROM accounts WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM accounts WHERE id = :id")
    Account getById(long id);

    @Query("SELECT * FROM accounts ORDER BY created_at ASC")
    List<Account> getAllSync();

    @Query("SELECT * FROM accounts ORDER BY created_at ASC")
    LiveData<List<Account>> observeAll();

    @Query(BALANCE_SELECT + " ORDER BY a.created_at ASC")
    LiveData<List<AccountWithBalance>> observeWithBalances();

    @Query(BALANCE_SELECT + " ORDER BY a.created_at ASC")
    List<AccountWithBalance> getWithBalancesSync();

    @Query(BALANCE_SELECT + " WHERE a.id = :id")
    AccountWithBalance getWithBalance(long id);

    @Query(BALANCE_UP_TO_SELECT)
    LiveData<Long> observeBalanceUpTo(String currency, long upTo);

    @Query(BALANCE_UP_TO_SELECT)
    long getBalanceUpTo(String currency, long upTo);

    @Query("SELECT COUNT(*) FROM accounts")
    int count();

    @Query("SELECT COUNT(*) FROM accounts WHERE name = :name AND id != :excludeId")
    int countByName(String name, long excludeId);

    @Query("SELECT COUNT(*) FROM accounts WHERE currency = :currency")
    int countByCurrency(String currency);
}
