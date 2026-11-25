package com.financemanager.app.ui.household;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.models.HouseholdCategoryBudget;
import com.financemanager.app.models.TransactionType;

import java.util.ArrayList;
import java.util.List;

/** Category links, per-category limits and the household's monthly budget. */
public class HouseholdCategoriesViewModel extends ViewModel {

    private static class Query {
        final long householdId;
        final long fromDate;
        final long toDate;

        Query(long householdId, long fromDate, long toDate) {
            this.householdId = householdId;
            this.fromDate = fromDate;
            this.toDate = toDate;
        }
    }

    private final FinanceRepository repository;
    private final MutableLiveData<Query> query = new MutableLiveData<>();
    private final LiveData<List<HouseholdCategoryBudget>> budgets;
    private final MutableLiveData<List<Category>> available = new MutableLiveData<>();

    private final MutableLiveData<String> currency;
    public long householdId = -1;

    public HouseholdCategoriesViewModel(@NonNull FinanceRepository repository,
                                        String fallbackCurrency) {
        this.repository = repository;
        this.currency = new MutableLiveData<>(fallbackCurrency);
        budgets = Transformations.switchMap(query, q -> repository.observeHouseholdCategories(
                q.householdId, q.fromDate, q.toDate, currency.getValue()));
    }

    /** The household's currency; falls back to the app default until it loads. */
    public LiveData<String> getCurrency() {
        return currency;
    }

    public String currency() {
        String value = currency.getValue();
        return value == null ? "" : value;
    }

    public LiveData<List<HouseholdCategoryBudget>> getBudgets() {
        return budgets;
    }

    public LiveData<List<Category>> getAvailable() {
        return available;
    }

    public void load(long householdId, long fromDate, long toDate) {
        this.householdId = householdId;
        repository.loadHousehold(householdId, new FinanceRepository.Callback<Household>() {
            @Override
            public void onSuccess(Household value) {
                if (value != null && value.currency != null
                        && !value.currency.equals(currency.getValue())) {
                    currency.setValue(value.currency);
                }
                query.setValue(new Query(householdId, fromDate, toDate));
            }

            @Override
            public void onError(String message) {
                query.setValue(new Query(householdId, fromDate, toDate));
            }
        });
        repository.loadCategories(TransactionType.EXPENSE,
                new FinanceRepository.Callback<List<Category>>() {
                    @Override
                    public void onSuccess(List<Category> value) {
                        available.setValue(value == null ? new ArrayList<>() : value);
                    }

                    @Override
                    public void onError(String message) {
                        available.setValue(new ArrayList<>());
                    }
                });
    }

    public void link(long categoryId, FinanceRepository.Callback<Long> callback) {
        repository.linkHouseholdCategory(householdId, categoryId, callback);
    }

    public void saveLimit(HouseholdCategoryBudget item, long limitMinor,
                          FinanceRepository.Callback<Void> callback) {
        item.category.monthlyBudgetMinor = limitMinor;
        repository.saveHouseholdCategory(item.category, callback);
    }

    public void unlink(long linkId, FinanceRepository.Callback<Void> callback) {
        repository.unlinkHouseholdCategory(linkId, callback);
    }

    public void saveHouseholdBudget(long budgetMinor, FinanceRepository.Callback<Long> callback) {
        repository.loadHousehold(householdId, new FinanceRepository.Callback<Household>() {
            @Override
            public void onSuccess(Household value) {
                if (value == null) {
                    callback.onError("Household not found");
                    return;
                }
                value.monthlyBudgetMinor = budgetMinor;
                repository.saveHousehold(value, false, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
