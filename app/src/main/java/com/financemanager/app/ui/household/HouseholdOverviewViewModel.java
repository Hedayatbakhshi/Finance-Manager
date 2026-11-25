package com.financemanager.app.ui.household;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.models.HouseholdSummary;

/** Aggregated household figures for the overview screen. */
public class HouseholdOverviewViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<HouseholdSummary> summary = new MutableLiveData<>();
    private final MutableLiveData<Household> household = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public HouseholdOverviewViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
    }

    public LiveData<HouseholdSummary> getSummary() {
        return summary;
    }

    public LiveData<Household> getHousehold() {
        return household;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load(long householdId, long fromDate, long toDate, String fallbackCurrency) {
        loading.setValue(true);
        repository.loadHousehold(householdId, new FinanceRepository.Callback<Household>() {
            @Override
            public void onSuccess(Household value) {
                household.setValue(value);
                String currency = value == null || value.currency == null
                        ? fallbackCurrency : value.currency;
                loadSummary(householdId, fromDate, toDate, currency);
            }

            @Override
            public void onError(String message) {
                loadSummary(householdId, fromDate, toDate, fallbackCurrency);
            }
        });
    }

    /** Loads only the figures, for screens that already know the household currency. */
    public void loadSummary(long householdId, long fromDate, long toDate, String currency) {
        repository.loadHouseholdSummary(householdId, fromDate, toDate, currency,
                new FinanceRepository.Callback<HouseholdSummary>() {
                    @Override
                    public void onSuccess(HouseholdSummary value) {
                        loading.setValue(false);
                        summary.setValue(value);
                    }

                    @Override
                    public void onError(String message) {
                        loading.setValue(false);
                    }
                });
    }
}
