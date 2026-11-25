package com.financemanager.app.ui.recurring;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.RecurringTransaction;

import java.util.List;

/** Scheduled transactions with pause/resume. */
public class RecurringViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final String currency;
    private final LiveData<List<RecurringTransaction>> recurring;

    public RecurringViewModel(@NonNull FinanceRepository repository, String currency) {
        this.repository = repository;
        this.currency = currency;
        this.recurring = repository.observeRecurring();
    }

    public LiveData<List<RecurringTransaction>> getRecurring() {
        return recurring;
    }

    public String getCurrency() {
        return currency;
    }

    public void setActive(long id, boolean active) {
        repository.setRecurringActive(id, active, new FinanceRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void delete(long id, FinanceRepository.Callback<Void> callback) {
        repository.deleteRecurring(id, callback);
    }
}
