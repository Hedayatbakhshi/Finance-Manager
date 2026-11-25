package com.financemanager.app.ui.household;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.models.HouseholdFilter;
import com.financemanager.app.models.TransactionType;

import java.util.ArrayList;
import java.util.List;

/** Household expense list with live search, filters and sorting. */
public class HouseholdExpensesViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<HouseholdFilter> filter = new MutableLiveData<>();
    private final LiveData<List<HouseholdExpense>> expenses;
    private final LiveData<List<Account>> accounts;
    private MutableLiveData<List<com.financemanager.app.data.entity.HouseholdMember>> members;
    private final MutableLiveData<String> currency;

    public HouseholdExpensesViewModel(@NonNull FinanceRepository repository,
                                      String fallbackCurrency) {
        this.repository = repository;
        this.currency = new MutableLiveData<>(fallbackCurrency);
        HouseholdFilter initial = new HouseholdFilter();
        filter.setValue(initial);
        expenses = Transformations.switchMap(filter, repository::observeHouseholdExpenses);
        accounts = Transformations.map(repository.observeAccounts(),
                balances -> {
                    List<Account> result = new ArrayList<>();
                    if (balances != null) {
                        for (AccountWithBalance balance : balances) {
                            result.add(balance.account);
                        }
                    }
                    return result;
                });
    }

    public LiveData<List<HouseholdExpense>> getExpenses() {
        return expenses;
    }

    /** The household's own currency, loaded from its profile. */
    public LiveData<String> getCurrency() {
        return currency;
    }

    public String currency() {
        String value = currency.getValue();
        return value == null ? "" : value;
    }

    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }

    public LiveData<List<com.financemanager.app.data.entity.HouseholdMember>> getMembers() {
        return members;
    }

    public LiveData<List<com.financemanager.app.data.entity.Category>> getCategories() {
        return repository.observeCategories(TransactionType.EXPENSE);
    }

    public void setHouseholdId(long householdId) {
        HouseholdFilter current = filter.getValue();
        current.householdId = householdId;
        filter.setValue(current);
        if (members == null) {
            members = new MutableLiveData<>();
            repository.loadHouseholdMembers(householdId,
                    new FinanceRepository.Callback<List<com.financemanager.app.data.entity.HouseholdMember>>() {
                        @Override
                        public void onSuccess(List<com.financemanager.app.data.entity.HouseholdMember> value) {
                            members.setValue(value);
                        }

                        @Override
                        public void onError(String message) {
                            members.setValue(null);
                        }
                    });
        }
        repository.loadHousehold(householdId, new FinanceRepository.Callback<Household>() {
            @Override
            public void onSuccess(Household value) {
                if (value != null && value.currency != null
                        && !value.currency.equals(currency.getValue())) {
                    currency.setValue(value.currency);
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public HouseholdFilter getFilter() {
        return filter.getValue();
    }

    private void update(HouseholdFilter value) {
        filter.setValue(value);
    }

    public void setSearch(String query) {
        HouseholdFilter value = copy();
        value.query = query == null ? "" : query;
        update(value);
    }

    public void setSort(int sort) {
        HouseholdFilter value = copy();
        value.sort = sort;
        update(value);
    }

    public void setCategory(Long categoryId) {
        HouseholdFilter value = copy();
        value.categoryId = categoryId;
        update(value);
    }

    public void setAccount(Long accountId) {
        HouseholdFilter value = copy();
        value.accountId = accountId;
        update(value);
    }

    public void setMember(Long memberId) {
        HouseholdFilter value = copy();
        value.memberId = memberId;
        update(value);
    }

    public void setRange(long fromDate, long toDate) {
        HouseholdFilter value = copy();
        value.fromDate = fromDate;
        value.toDate = toDate;
        update(value);
    }

    public void clearFilters(long householdId) {
        HouseholdFilter value = new HouseholdFilter();
        value.householdId = householdId;
        update(value);
    }

    private HouseholdFilter copy() {
        HouseholdFilter old = filter.getValue();
        HouseholdFilter value = new HouseholdFilter();
        if (old != null) {
            value.householdId = old.householdId;
            value.query = old.query;
            value.fromDate = old.fromDate;
            value.toDate = old.toDate;
            value.categoryId = old.categoryId;
            value.accountId = old.accountId;
            value.memberId = old.memberId;
            value.sort = old.sort;
        }
        return value;
    }

    public void delete(long id, FinanceRepository.Callback<Void> callback) {
        repository.deleteHouseholdExpense(id, callback);
    }
}
