package com.financemanager.app.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.di.AppContainer;
import com.financemanager.app.ui.accounts.AccountDetailViewModel;
import com.financemanager.app.ui.accounts.AccountEditorViewModel;
import com.financemanager.app.ui.accounts.AccountsViewModel;
import com.financemanager.app.ui.budgets.BudgetEditorViewModel;
import com.financemanager.app.ui.budgets.BudgetsViewModel;
import com.financemanager.app.ui.categories.CategoriesViewModel;
import com.financemanager.app.ui.dashboard.DashboardViewModel;
import com.financemanager.app.ui.data.DataViewModel;
import com.financemanager.app.ui.household.HouseholdCategoriesViewModel;
import com.financemanager.app.ui.household.HouseholdEditorViewModel;
import com.financemanager.app.ui.household.HouseholdExpenseEditorViewModel;
import com.financemanager.app.ui.household.HouseholdExpensesViewModel;
import com.financemanager.app.ui.household.HouseholdOverviewViewModel;
import com.financemanager.app.ui.household.HouseholdReportsViewModel;
import com.financemanager.app.ui.household.HouseholdsViewModel;
import com.financemanager.app.ui.recurring.RecurringEditorViewModel;
import com.financemanager.app.ui.recurring.RecurringViewModel;
import com.financemanager.app.ui.reports.ReportsViewModel;
import com.financemanager.app.ui.savings.SavingsEditorViewModel;
import com.financemanager.app.ui.savings.SavingsViewModel;
import com.financemanager.app.ui.transactions.TransactionEditorViewModel;
import com.financemanager.app.ui.transactions.TransactionFilterViewModel;
import com.financemanager.app.ui.transactions.TransactionsViewModel;

/** Constructs ViewModels with their repository dependencies (manual DI). */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;
    private final Context appContext;
    private final String currency;

    public ViewModelFactory(AppContainer container, Context appContext) {
        this.container = container;
        this.appContext = appContext.getApplicationContext();
        this.currency = container.settings().getCurrency();
    }

    public static ViewModelFactory of(@NonNull Fragment fragment) {
        return new ViewModelFactory(
                ((FinanceManagerApp) fragment.requireActivity().getApplication()).container(),
                fragment.requireContext());
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(container.repository(), container.settings());
        }
        if (modelClass.isAssignableFrom(TransactionsViewModel.class)) {
            return (T) new TransactionsViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(TransactionFilterViewModel.class)) {
            return (T) new TransactionFilterViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(TransactionEditorViewModel.class)) {
            return (T) new TransactionEditorViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(AccountsViewModel.class)) {
            return (T) new AccountsViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(AccountEditorViewModel.class)) {
            return (T) new AccountEditorViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(AccountDetailViewModel.class)) {
            return (T) new AccountDetailViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(BudgetsViewModel.class)) {
            return (T) new BudgetsViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(BudgetEditorViewModel.class)) {
            return (T) new BudgetEditorViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(CategoriesViewModel.class)) {
            return (T) new CategoriesViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(ReportsViewModel.class)) {
            return (T) new ReportsViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(SavingsViewModel.class)) {
            return (T) new SavingsViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(SavingsEditorViewModel.class)) {
            return (T) new SavingsEditorViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(RecurringViewModel.class)) {
            return (T) new RecurringViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(RecurringEditorViewModel.class)) {
            return (T) new RecurringEditorViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(DataViewModel.class)) {
            return (T) new DataViewModel(appContext, container);
        }
        if (modelClass.isAssignableFrom(HouseholdsViewModel.class)) {
            return (T) new HouseholdsViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(HouseholdEditorViewModel.class)) {
            return (T) new HouseholdEditorViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(HouseholdOverviewViewModel.class)) {
            return (T) new HouseholdOverviewViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(HouseholdExpensesViewModel.class)) {
            return (T) new HouseholdExpensesViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(HouseholdExpenseEditorViewModel.class)) {
            return (T) new HouseholdExpenseEditorViewModel(container.repository());
        }
        if (modelClass.isAssignableFrom(HouseholdCategoriesViewModel.class)) {
            return (T) new HouseholdCategoriesViewModel(container.repository(), currency);
        }
        if (modelClass.isAssignableFrom(HouseholdReportsViewModel.class)) {
            return (T) new HouseholdReportsViewModel(container.repository(), currency);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
