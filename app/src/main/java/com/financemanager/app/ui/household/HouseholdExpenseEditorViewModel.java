package com.financemanager.app.ui.household;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.data.entity.HouseholdCategory;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.util.ArrayList;
import java.util.List;

/** Editor state for a household expense; persists through the ledger writer. */
public class HouseholdExpenseEditorViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final LiveData<List<Account>> accounts;
    private MutableLiveData<List<HouseholdMember>> members;
    private final MutableLiveData<List<HouseholdCategory>> categories = new MutableLiveData<>();
    private final MutableLiveData<HouseholdExpense> loaded = new MutableLiveData<>();
    private final MutableLiveData<String> householdCurrency = new MutableLiveData<>();

    public long householdId = -1;
    public long expenseId = -1;
    public long selectedCategoryId = -1;
    public long selectedAccountId = -1;
    public long selectedMemberId = -1;
    public long selectedDate = DateUtils.todayStart();
    public String receiptPath;
    private TransactionEntity editing;

    public HouseholdExpenseEditorViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        accounts = androidx.lifecycle.Transformations.map(repository.observeAccounts(),
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

    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }

    public LiveData<List<HouseholdMember>> getMembers() {
        return members;
    }

    public LiveData<List<HouseholdCategory>> getCategories() {
        return categories;
    }

    public LiveData<List<com.financemanager.app.data.entity.Category>> getGlobalCategories() {
        return repository.observeCategories(TransactionType.EXPENSE);
    }

    public LiveData<HouseholdExpense> getLoaded() {
        return loaded;
    }

    /** Currency of the owning household; accounts in other currencies are flagged. */
    public LiveData<String> getHouseholdCurrency() {
        return householdCurrency;
    }

    public String householdCurrency() {
        String value = householdCurrency.getValue();
        return value == null ? "" : value;
    }

    public void init(long householdId) {
        this.householdId = householdId;
        repository.loadHousehold(householdId, new FinanceRepository.Callback<com.financemanager.app.data.entity.Household>() {
            @Override
            public void onSuccess(com.financemanager.app.data.entity.Household value) {
                if (value != null && value.currency != null
                        && !value.currency.equals(householdCurrency.getValue())) {
                    householdCurrency.setValue(value.currency);
                }
            }

            @Override
            public void onError(String message) {
            }
        });
        if (members == null) {
            members = new MutableLiveData<>();
            repository.loadHouseholdMembers(householdId,
                    new FinanceRepository.Callback<List<HouseholdMember>>() {
                        @Override
                        public void onSuccess(List<HouseholdMember> value) {
                            members.setValue(value);
                        }

                        @Override
                        public void onError(String message) {
                            members.setValue(null);
                        }
                    });
        }
        repository.loadHouseholdCategoryLinks(householdId,
                new FinanceRepository.Callback<List<HouseholdCategory>>() {
                    @Override
                    public void onSuccess(List<HouseholdCategory> value) {
                        categories.setValue(value);
                    }

                    @Override
                    public void onError(String message) {
                        categories.setValue(null);
                    }
                });
    }

    public void load(long id) {
        expenseId = id;
        repository.loadHouseholdExpense(id, new FinanceRepository.Callback<HouseholdExpense>() {
            @Override
            public void onSuccess(HouseholdExpense value) {
                if (value == null) {
                    return;
                }
                editing = value.transaction.transaction;
                householdId = editing.householdId == null ? householdId : editing.householdId;
                selectedAccountId = editing.accountId;
                selectedCategoryId = editing.householdCategoryId == null
                        ? -1 : editing.householdCategoryId;
                selectedMemberId = editing.householdMemberId == null
                        ? -1 : editing.householdMemberId;
                selectedDate = editing.date;
                receiptPath = editing.receiptPath;
                init(householdId);
                loaded.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void save(String title, String amountText, String description, String notes,
                     FinanceRepository.Callback<Long> callback) {
        Long amount = Money.parsePositive(amountText);
        if (amount == null) {
            callback.onError("Enter an amount greater than zero");
            return;
        }
        TransactionEntity entity = new TransactionEntity();
        if (editing != null) {
            entity.id = editing.id;
            entity.createdAt = editing.createdAt;
            entity.recurringId = editing.recurringId;
        }
        entity.title = title == null ? "" : title.trim();
        entity.amountMinor = amount;
        entity.type = TransactionType.EXPENSE;
        entity.accountId = selectedAccountId;
        entity.householdId = householdId;
        entity.householdCategoryId = selectedCategoryId > 0 ? selectedCategoryId : null;
        entity.householdMemberId = selectedMemberId > 0 ? selectedMemberId : null;
        entity.date = selectedDate;
        entity.description = description == null ? "" : description.trim();
        entity.notes = notes == null ? "" : notes.trim();
        entity.receiptPath = receiptPath;
        repository.saveHouseholdExpense(entity, expenseId <= 0, callback);
    }

    public void delete(FinanceRepository.Callback<Void> callback) {
        repository.deleteHouseholdExpense(expenseId, callback);
    }
}
