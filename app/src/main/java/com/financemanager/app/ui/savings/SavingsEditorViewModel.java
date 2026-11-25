package com.financemanager.app.ui.savings;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.utils.Money;

/** State for creating or editing a savings goal. */
public class SavingsEditorViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<SavingsGoal> loaded = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public long goalId = -1;
    public long targetDate;
    public String icon = "goal_target";
    public String color = "#16A36A";
    public SavingsGoal editing;

    public SavingsEditorViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
    }

    public LiveData<SavingsGoal> getLoaded() {
        return loaded;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public boolean isEditing() {
        return goalId > 0;
    }

    public void load(long id) {
        goalId = id;
        repository.loadGoal(id, new FinanceRepository.Callback<SavingsGoal>() {
            @Override
            public void onSuccess(SavingsGoal value) {
                if (value == null) {
                    return;
                }
                editing = value;
                targetDate = value.targetDate;
                icon = value.icon;
                color = value.color;
                loaded.setValue(value);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void save(String name, String targetText, String currentText, String description,
                     FinanceRepository.Callback<Long> callback) {
        Long target = Money.parsePositive(targetText);
        if (target == null) {
            callback.onError("Enter a target amount greater than zero");
            return;
        }
        Long current = Money.parse(currentText == null ? "" : currentText);
        SavingsGoal goal = editing != null ? editing : new SavingsGoal();
        goal.name = name == null ? "" : name.trim();
        goal.targetMinor = target;
        goal.currentMinor = current == null ? 0 : Math.max(0, current);
        goal.targetDate = targetDate;
        goal.description = description == null ? "" : description.trim();
        goal.icon = icon;
        goal.color = color;
        repository.saveSavingsGoal(goal, !isEditing(), callback);
    }

    public void delete() {
        repository.deleteGoal(goalId, new FinanceRepository.Callback<Void>() {
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
