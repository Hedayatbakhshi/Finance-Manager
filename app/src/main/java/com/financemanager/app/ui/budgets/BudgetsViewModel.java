package com.financemanager.app.ui.budgets;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.models.BudgetWithProgress;

import java.util.List;

/** Live budget list with per-period spending already calculated in SQL. */
public class BudgetsViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final String currency;
    private final LiveData<List<BudgetWithProgress>> budgets;

    public BudgetsViewModel(@NonNull FinanceRepository repository, String currency) {
        this.repository = repository;
        this.currency = currency;
        this.budgets = repository.observeBudgets(currency);
    }

    public LiveData<List<BudgetWithProgress>> getBudgets() {
        return budgets;
    }

    public String getCurrency() {
        return currency;
    }

    public void deleteBudget(long id, FinanceRepository.Callback<Void> callback) {
        repository.deleteBudget(id, callback);
    }

    public LiveData<Long> observeActiveExpenses(long from, long to) {
        return repository.database().transactionDao().observeTotalForType(
                com.financemanager.app.models.TransactionType.EXPENSE, from, to, currency);
    }
}
