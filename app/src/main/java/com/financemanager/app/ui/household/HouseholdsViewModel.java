package com.financemanager.app.ui.household;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Household;

import java.util.List;

/** List of local household profiles. */
public class HouseholdsViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final LiveData<List<Household>> households;

    public HouseholdsViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        households = repository.observeHouseholds();
    }

    public LiveData<List<Household>> getHouseholds() {
        return households;
    }

    public void delete(long id, FinanceRepository.Callback<Void> callback) {
        repository.deleteHousehold(id, callback);
    }
}
