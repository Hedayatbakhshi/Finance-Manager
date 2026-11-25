package com.financemanager.app.ui.accounts;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.TransactionWithDetails;

import java.util.List;

/** Account summary plus its live transaction history. */
public class AccountDetailViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<Long> accountId = new MutableLiveData<>(-1L);
    private final MutableLiveData<AccountWithBalance> account = new MutableLiveData<>();
    private final LiveData<List<TransactionWithDetails>> transactions;
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public AccountDetailViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        transactions = Transformations.switchMap(accountId, repository::observeAccountTransactions);
    }

    public LiveData<AccountWithBalance> getAccount() {
        return account;
    }

    public LiveData<List<TransactionWithDetails>> getTransactions() {
        return transactions;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public void load(long id) {
        accountId.setValue(id);
        repository.loadAccount(id, new FinanceRepository.Callback<AccountWithBalance>() {
            @Override
            public void onSuccess(AccountWithBalance value) {
                account.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void reload(long id) {
        repository.loadAccount(id, new FinanceRepository.Callback<AccountWithBalance>() {
            @Override
            public void onSuccess(AccountWithBalance value) {
                account.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void delete(long id) {
        repository.deleteAccount(id, new FinanceRepository.Callback<Void>() {
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
