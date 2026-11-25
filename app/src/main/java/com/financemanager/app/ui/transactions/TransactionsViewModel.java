package com.financemanager.app.ui.transactions;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.models.TransactionFilter;
import com.financemanager.app.models.TransactionWithDetails;

import java.util.List;

/** Holds the active search/filter state for the transaction history. */
public class TransactionsViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<TransactionFilter> filter =
            new MutableLiveData<>(TransactionFilter.all());
    private final LiveData<List<TransactionWithDetails>> transactions;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private String searchText = "";

    public TransactionsViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        transactions = Transformations.switchMap(filter, repository::observeTransactions);
    }

    public LiveData<TransactionFilter> getFilter() {
        return filter;
    }

    public LiveData<List<TransactionWithDetails>> getTransactions() {
        return transactions;
    }

    /** Debounced so typing does not issue a query per keystroke. */
    public void setSearch(String text) {
        searchText = text == null ? "" : text;
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> applyQueryToFilter();
        handler.postDelayed(pendingSearch, 250);
    }

    public void applyFilter(TransactionFilter newFilter) {
        TransactionFilter copy = newFilter.copy();
        copy.query = searchText;
        filter.setValue(copy);
    }

    public void resetFilters() {
        TransactionFilter reset = new TransactionFilter();
        reset.query = searchText;
        filter.setValue(reset);
    }

    private void applyQueryToFilter() {
        TransactionFilter current = filter.getValue();
        TransactionFilter copy = current == null ? new TransactionFilter() : current.copy();
        copy.query = searchText;
        filter.setValue(copy);
    }

    public void deleteTransaction(long id, FinanceRepository.Callback<Void> callback) {
        repository.deleteTransaction(id, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }
    }
}
