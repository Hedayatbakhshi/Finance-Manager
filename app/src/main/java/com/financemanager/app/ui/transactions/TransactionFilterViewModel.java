package com.financemanager.app.ui.transactions;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.models.AccountWithBalance;

import java.util.List;

/** Supplies categories and accounts to the filter screen. */
public class TransactionFilterViewModel extends ViewModel {

    private final LiveData<List<Category>> categories;
    private final LiveData<List<AccountWithBalance>> accounts;

    public TransactionFilterViewModel(@NonNull FinanceRepository repository) {
        categories = repository.observeCategories();
        accounts = repository.observeAccounts();
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<AccountWithBalance>> getAccounts() {
        return accounts;
    }
}
