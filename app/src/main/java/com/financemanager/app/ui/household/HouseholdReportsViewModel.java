package com.financemanager.app.ui.household;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.models.HouseholdSummary;

/** Report state for a household range, including month comparison. */
public class HouseholdReportsViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<HouseholdSummary> report = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public final String currency;
    public long householdId = -1;
    public long fromDate;
    public long toDate;

    public HouseholdReportsViewModel(@NonNull FinanceRepository repository, String currency) {
        this.repository = repository;
        this.currency = currency;
    }

    public LiveData<HouseholdSummary> getReport() {
        return report;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load(long householdId, long fromDate, long toDate) {
        load(householdId, fromDate, toDate, currency);
    }

    public void load(long householdId, long fromDate, long toDate, String reportCurrency) {
        this.householdId = householdId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        loading.setValue(true);
        repository.loadHouseholdSummary(householdId, fromDate, toDate, reportCurrency,
                new FinanceRepository.Callback<HouseholdSummary>() {
                    @Override
                    public void onSuccess(HouseholdSummary value) {
                        loading.setValue(false);
                        report.setValue(value);
                    }

                    @Override
                    public void onError(String message) {
                        loading.setValue(false);
                        report.setValue(null);
                    }
                });
    }
}
