package com.financemanager.app.ui.reports;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.models.ReportResult;
import com.financemanager.app.utils.DateUtils;

import java.time.LocalDate;

/** Loads report aggregates for the selected period. */
public class ReportsViewModel extends ViewModel {

    public static final int RANGE_THIS_MONTH = 0;
    public static final int RANGE_LAST_MONTH = 1;
    public static final int RANGE_THREE_MONTHS = 2;
    public static final int RANGE_YEAR = 3;

    private final FinanceRepository repository;
    private final String currency;
    private final MutableLiveData<ReportResult> report = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private int range = RANGE_THIS_MONTH;

    public ReportsViewModel(@NonNull FinanceRepository repository, String currency) {
        this.repository = repository;
        this.currency = currency;
        load(RANGE_THIS_MONTH);
    }

    public LiveData<ReportResult> getReport() {
        return report;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public String getCurrency() {
        return currency;
    }

    public int getRange() {
        return range;
    }

    public void load(int newRange) {
        this.range = newRange;
        LocalDate today = LocalDate.now();
        long from;
        long to;
        switch (newRange) {
            case RANGE_LAST_MONTH:
                LocalDate lastMonth = today.minusMonths(1);
                from = DateUtils.startOfMonth(lastMonth);
                to = DateUtils.endOfMonth(lastMonth);
                break;
            case RANGE_THREE_MONTHS:
                from = DateUtils.startOfMonth(today.minusMonths(2));
                to = DateUtils.endOfMonth(today);
                break;
            case RANGE_YEAR:
                from = DateUtils.startOfDay(today.withDayOfYear(1));
                to = DateUtils.endOfMonth(today);
                break;
            case RANGE_THIS_MONTH:
            default:
                from = DateUtils.startOfMonth(today);
                to = DateUtils.endOfMonth(today);
                break;
        }
        loading.setValue(true);
        repository.loadReport(from, to, currency, new FinanceRepository.Callback<ReportResult>() {
            @Override
            public void onSuccess(ReportResult value) {
                report.setValue(value);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
            }
        });
    }
}
