package com.financemanager.app.ui.budgets;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Budget;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.time.LocalDate;
import java.util.List;

/** State for creating or editing a budget period. */
public class BudgetEditorViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final LiveData<List<Category>> categories;
    private final MutableLiveData<Budget> loaded = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public long budgetId = -1;
    public long selectedCategoryId = -1;
    public long startDate;
    public long endDate;
    public Budget editing;

    public BudgetEditorViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        categories = repository.observeCategories(TransactionType.EXPENSE);
        LocalDate today = LocalDate.now();
        startDate = DateUtils.startOfMonth(today);
        endDate = DateUtils.endOfMonth(today);
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Budget> getLoaded() {
        return loaded;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public boolean isEditing() {
        return budgetId > 0;
    }

    public void load(long id) {
        budgetId = id;
        repository.loadBudget(id, new FinanceRepository.Callback<Budget>() {
            @Override
            public void onSuccess(Budget value) {
                if (value == null) {
                    return;
                }
                editing = value;
                selectedCategoryId = value.categoryId;
                startDate = value.startDate;
                endDate = value.endDate;
                loaded.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void save(String name, String amountText, FinanceRepository.Callback<Long> callback) {
        Long amount = Money.parsePositive(amountText);
        if (amount == null) {
            callback.onError("Enter a budget amount greater than zero");
            return;
        }
        Budget budget = editing != null ? editing : new Budget();
        budget.name = name == null ? "" : name.trim();
        budget.amountMinor = amount;
        budget.categoryId = selectedCategoryId;
        budget.startDate = startDate;
        budget.endDate = endDate;
        repository.saveBudget(budget, !isEditing(), callback);
    }

    public void delete() {
        repository.deleteBudget(budgetId, new FinanceRepository.Callback<Void>() {
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
