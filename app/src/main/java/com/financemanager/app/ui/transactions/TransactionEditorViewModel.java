package com.financemanager.app.ui.transactions;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.util.List;

/** Holds editor selections so rotation does not lose a half-filled transaction. */
public class TransactionEditorViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final LiveData<List<Account>> accounts;
    private final MutableLiveData<TransactionType> categoryType =
            new MutableLiveData<>(TransactionType.EXPENSE);
    private final LiveData<List<Category>> categories;
    private final MutableLiveData<TransactionEntity> loaded = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public TransactionType type = TransactionType.EXPENSE;
    public long transactionId = -1;
    public long selectedAccountId = -1;
    public long selectedToAccountId = -1;
    public long selectedCategoryId = -1;
    public long selectedDate = DateUtils.todayStart();
    public TransactionEntity editing;

    public TransactionEditorViewModel(@NonNull FinanceRepository repository) {
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

    public LiveData<TransactionEntity> getLoaded() {
        return loaded;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public boolean isEditing() {
        return transactionId > 0;
    }

    public void setType(TransactionType newType) {
        if (type == newType) {
            return;
        }
        type = newType;
        // Categories are type-specific; start with no selection until the list arrives.
        selectedCategoryId = -1;
        categoryType.setValue(newType == TransactionType.INCOME
                ? TransactionType.INCOME : TransactionType.EXPENSE);
    }

    public void load(long id) {
        transactionId = id;
        repository.loadTransaction(id, new FinanceRepository.Callback<TransactionEntity>() {
            @Override
            public void onSuccess(TransactionEntity value) {
                if (value == null) {
                    return;
                }
                editing = value;
                type = value.type;
                selectedAccountId = value.accountId;
                selectedToAccountId = value.toAccountId == null ? -1 : value.toAccountId;
                selectedCategoryId = value.categoryId == null ? -1 : value.categoryId;
                selectedDate = value.date;
                categoryType.setValue(value.type == TransactionType.INCOME
                        ? TransactionType.INCOME : TransactionType.EXPENSE);
                loaded.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void save(String title, String amountText, String description, String notes,
                     FinanceRepository.Callback<Long> callback) {
        TransactionEntity entity = buildEntity(title, amountText, description, notes);
        if (entity == null) {
            callback.onError("Enter an amount greater than zero");
            return;
        }
        repository.saveTransaction(entity, !isEditing(), callback);
    }

    /** Saves the current editor values as a separate transaction. */
    public void saveCopy(String title, String amountText, String description, String notes,
                         FinanceRepository.Callback<Long> callback) {
        TransactionEntity entity = buildEntity(title, amountText, description, notes);
        if (entity == null) {
            callback.onError("Enter an amount greater than zero");
            return;
        }
        entity.id = 0;
        entity.recurringId = null;
        entity.createdAt = 0;
        repository.saveTransaction(entity, true, callback);
    }

    private TransactionEntity buildEntity(String title, String amountText, String description,
                                          String notes) {
        Long amount = Money.parsePositive(amountText);
        if (amount == null) {
            return null;
        }
        TransactionEntity entity = new TransactionEntity();
        if (editing != null) {
            entity.id = editing.id;
            entity.recurringId = editing.recurringId;
            entity.createdAt = editing.createdAt;
        }
        entity.title = title == null ? "" : title.trim();
        entity.amountMinor = amount;
        entity.type = type;
        entity.categoryId = type == TransactionType.TRANSFER || selectedCategoryId <= 0
                ? null : selectedCategoryId;
        entity.accountId = selectedAccountId;
        entity.toAccountId = type == TransactionType.TRANSFER && selectedToAccountId > 0
                ? selectedToAccountId : null;
        entity.date = selectedDate;
        entity.description = description == null ? "" : description.trim();
        entity.notes = notes == null ? "" : notes.trim();
        return entity;
    }

    public void delete(long id) {
        repository.deleteTransaction(id, new FinanceRepository.Callback<Void>() {
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

    public FinanceRepository repository() {
        return repository;
    }
}
