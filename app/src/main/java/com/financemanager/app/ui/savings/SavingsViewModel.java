package com.financemanager.app.ui.savings;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.SavingsGoal;

import java.util.List;

/** Savings goals with atomic add/withdraw operations. */
public class SavingsViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final String currency;
    private final LiveData<List<SavingsGoal>> goals;

    public SavingsViewModel(@NonNull FinanceRepository repository, String currency) {
        this.repository = repository;
        this.currency = currency;
        this.goals = repository.observeSavingsGoals();
    }

    public LiveData<List<SavingsGoal>> getGoals() {
        return goals;
    }

    public String getCurrency() {
        return currency;
    }

    public void adjust(long goalId, long delta, FinanceRepository.Callback<Boolean> callback) {
        repository.adjustGoalAmount(goalId, delta, callback);
    }

    public void delete(long goalId, FinanceRepository.Callback<Void> callback) {
        repository.deleteGoal(goalId, callback);
    }
}
