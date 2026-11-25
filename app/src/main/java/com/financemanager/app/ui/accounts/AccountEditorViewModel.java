package com.financemanager.app.ui.accounts;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.models.AccountType;
import com.financemanager.app.utils.Money;

/** State for creating or editing an account. */
public class AccountEditorViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<Account> loaded = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public long accountId = -1;
    public AccountType type = AccountType.CASH;
    public String currency;
    public String icon = "account_cash";
    public String color = "#3E6DB5";
    public Account editing;

    public AccountEditorViewModel(@NonNull FinanceRepository repository, String defaultCurrency) {
        this.repository = repository;
        this.currency = defaultCurrency;
    }

    public LiveData<Account> getLoaded() {
        return loaded;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public boolean isEditing() {
        return accountId > 0;
    }

    public void load(long id) {
        accountId = id;
        repository.loadAccountEntity(id, new FinanceRepository.Callback<Account>() {
            @Override
            public void onSuccess(Account value) {
                if (value == null) {
                    return;
                }
                editing = value;
                type = value.type;
                currency = value.currency;
                icon = value.icon;
                color = value.color;
                loaded.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void save(String name, String balanceText, FinanceRepository.Callback<Long> callback) {
        Long balance = Money.parse(balanceText);
        if (balance == null) {
            callback.onError("Enter a valid opening balance");
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            callback.onError("Enter an account name");
            return;
        }
        Account account = editing != null ? editing : new Account();
        account.name = name.trim();
        account.type = type;
        account.initialBalanceMinor = balance;
        account.currency = currency;
        account.icon = icon;
        account.color = color;
        repository.saveAccount(account, !isEditing(), callback);
    }

    public void countTransactions(FinanceRepository.Callback<Integer> callback) {
        repository.countAccountTransactions(accountId, callback);
    }

    public void delete() {
        repository.deleteAccount(accountId, new FinanceRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                deleted.setValue(true);
            }

            @Override
            public void onError(String message) {
                deleted.setValue(false);
            }
        });
    }
}
