package com.financemanager.app.ui.household;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.utils.Money;

import java.util.List;

/** Household profile plus its local member list. */
public class HouseholdEditorViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<Household> household = new MutableLiveData<>();
    private LiveData<List<HouseholdMember>> members;

    public boolean editing = false;
    public long householdId = -1;
    public String currency;

    public HouseholdEditorViewModel(@NonNull FinanceRepository repository,
                                    String defaultCurrency) {
        this.repository = repository;
        this.currency = defaultCurrency == null || defaultCurrency.trim().isEmpty()
                ? "AFN" : defaultCurrency;
    }

    public LiveData<Household> getHousehold() {
        return household;
    }

    public LiveData<List<HouseholdMember>> getMembers() {
        return members;
    }

    public void load(long id) {
        editing = id > 0;
        householdId = id;
        if (members == null) {
            members = repository.observeHouseholdMembers(id);
        }
        repository.loadHousehold(id, new FinanceRepository.Callback<Household>() {
            @Override
            public void onSuccess(Household value) {
                if (value != null) {
                    currency = value.currency;
                    household.setValue(value);
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void save(String name, String description, String budgetText, String currencyCode,
                     FinanceRepository.Callback<Long> callback) {
        Long budget = Money.parsePositive(budgetText);
        Household entity = household.getValue();
        if (entity == null) {
            entity = new Household();
        }
        entity.id = editing ? householdId : 0;
        entity.name = name;
        entity.description = description == null ? "" : description.trim();
        entity.monthlyBudgetMinor = budget == null ? 0 : budget;
        entity.currency = currencyCode;
        repository.saveHousehold(entity, !editing, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                if (value != null) {
                    householdId = value;
                    editing = true;
                }
                callback.onSuccess(value);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void addMember(String name, FinanceRepository.Callback<Long> callback) {
        HouseholdMember member = new HouseholdMember();
        member.householdId = householdId;
        member.name = name;
        repository.saveHouseholdMember(member, true, callback);
    }

    public void deleteMember(long id, FinanceRepository.Callback<Void> callback) {
        repository.deleteHouseholdMember(id, callback);
    }

    public void delete(FinanceRepository.Callback<Void> callback) {
        repository.deleteHousehold(householdId, callback);
    }
}
