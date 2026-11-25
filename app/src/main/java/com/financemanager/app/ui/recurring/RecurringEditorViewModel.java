package com.financemanager.app.ui.recurring;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.models.RecurrenceFrequency;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.util.List;

/** State for creating or editing a recurring schedule. */
public class RecurringEditorViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final LiveData<List<Account>> accounts;
    private final MutableLiveData<TransactionType> categoryType =
            new MutableLiveData<>(TransactionType.EXPENSE);
    private final LiveData<List<Category>> categories;
    private final MutableLiveData<RecurringTransaction> loaded = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public TransactionType type = TransactionType.EXPENSE;
    public RecurrenceFrequency frequency = RecurrenceFrequency.MONTHLY;
    public long recurringId = -1;
    public long selectedCategoryId = -1;
    public long selectedAccountId = -1;
    public long selectedToAccountId = -1;
    public long nextDate = DateUtils.todayStart();
    public long householdId = -1;
    public long householdCategoryId = -1;
    public RecurringTransaction editing;

    public RecurringEditorViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        accounts = repository.database().accountDao().observeAll();
        categories = Transformations.switchMap(categoryType, repository::observeCategories);
    }

    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<RecurringTransaction> getLoaded() {
        return loaded;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public boolean isEditing() {
        return recurringId > 0;
    }

    public void setType(TransactionType newType) {
        if (type == newType) {
            return;
        }
        type = newType;
        selectedCategoryId = -1;
        categoryType.setValue(newType == TransactionType.INCOME
                ? TransactionType.INCOME : TransactionType.EXPENSE);
    }

    public void load(long id) {
        recurringId = id;
        repository.loadRecurring(id, new FinanceRepository.Callback<RecurringTransaction>() {
            @Override
            public void onSuccess(RecurringTransaction value) {
                if (value == null) {
                    return;
                }
                editing = value;
                if (value.householdId != null) {
                    householdId = value.householdId;
                }
                if (value.householdCategoryId != null) {
                    householdCategoryId = value.householdCategoryId;
                }
                type = value.type;
                frequency = value.frequency;
                selectedCategoryId = value.categoryId == null ? -1 : value.categoryId;
                selectedAccountId = value.accountId;
                selectedToAccountId = value.toAccountId == null ? -1 : value.toAccountId;
                nextDate = value.nextRunDate;
                categoryType.setValue(value.type == TransactionType.INCOME
                        ? TransactionType.INCOME : TransactionType.EXPENSE);
                loaded.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void save(String title, String amountText, String description,
                     FinanceRepository.Callback<Long> callback) {
        Long amount = Money.parsePositive(amountText);
        if (amount == null) {
            callback.onError("Enter an amount greater than zero");
            return;
        }
        RecurringTransaction entity = editing != null ? editing : new RecurringTransaction();
        entity.title = title == null ? "" : title.trim();
        entity.amountMinor = amount;
        entity.type = type;
        entity.frequency = frequency;
        entity.categoryId = type == TransactionType.TRANSFER || selectedCategoryId <= 0
                ? null : selectedCategoryId;
        entity.accountId = selectedAccountId;
        entity.toAccountId = type == TransactionType.TRANSFER && selectedToAccountId > 0
                ? selectedToAccountId : null;
        entity.nextRunDate = nextDate;
        entity.description = description == null ? "" : description.trim();
        entity.active = editing == null || editing.active;
        if (householdId > 0) {
            entity.householdId = householdId;
            entity.householdCategoryId = householdCategoryId > 0 ? householdCategoryId : null;
            entity.type = TransactionType.EXPENSE;
            entity.toAccountId = null;
            repository.saveHouseholdRecurring(entity, !isEditing(), callback);
        } else {
            repository.saveRecurring(entity, !isEditing(), callback);
        }
    }

    public void delete() {
        repository.deleteRecurring(recurringId, new FinanceRepository.Callback<Void>() {
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
