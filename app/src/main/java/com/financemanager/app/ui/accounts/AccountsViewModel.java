package com.financemanager.app.ui.accounts;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.models.AccountWithBalance;

import java.util.List;

/** Live account balances. */
public class AccountsViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final LiveData<List<AccountWithBalance>> accounts;

    public AccountsViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        this.accounts = repository.observeAccounts();
    }

    public LiveData<List<AccountWithBalance>> getAccounts() {
        return accounts;
    }

    public void deleteAccount(long id, FinanceRepository.Callback<Void> callback) {
        repository.deleteAccount(id, callback);
    }
}
