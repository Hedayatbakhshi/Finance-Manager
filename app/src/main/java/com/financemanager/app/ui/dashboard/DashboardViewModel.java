package com.financemanager.app.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.BudgetWithProgress;
import com.financemanager.app.models.CategoryTotal;
import com.financemanager.app.models.DailyTotal;
import com.financemanager.app.models.MonthlyTotal;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.utils.DateUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Combines every dashboard source into one snapshot. Room's LiveData keeps income,
 * expense, balance and budget figures in sync with the database automatically.
 */
public class DashboardViewModel extends ViewModel {

    private interface ValueConsumer<T> {
        void accept(T value);
    }

    private final FinanceRepository repository;
    private final String currency;
    private final MediatorLiveData<DashboardState> state = new MediatorLiveData<>();
    private final DashboardState draft = new DashboardState();
    private final List<Boolean> seen = new ArrayList<>(Collections.nCopies(9, false));
    private final int daysElapsedThisMonth;

    private final LocalDate weekStart;
    private final LocalDate weekEnd;
    private final LocalDate monthStartDate;
    private final LocalDate yearStartDate;

    private List<DailyTotal> weekIncome = Collections.emptyList();
    private List<DailyTotal> weekExpense = Collections.emptyList();
    private List<DailyTotal> monthIncome = Collections.emptyList();
    private List<DailyTotal> monthExpense = Collections.emptyList();
    private List<MonthlyTotal> yearMonths = Collections.emptyList();
    private List<CategoryTotal> monthCategories = Collections.emptyList();

    public DashboardViewModel(@NonNull FinanceRepository repository,
                              @NonNull SettingsRepository settings) {
        this.repository = repository;
        this.currency = settings.getCurrency();
        draft.currency = currency;
        state.setValue(draft);

        LocalDate today = LocalDate.now();
        daysElapsedThisMonth = today.getDayOfMonth();
        weekStart = today.with(DayOfWeek.MONDAY);
        weekEnd = weekStart.plusDays(6);
        monthStartDate = today.withDayOfMonth(1);
        yearStartDate = today.withDayOfYear(1);

        long weekFrom = DateUtils.startOfDay(weekStart);
        long weekTo = DateUtils.endOfDay(weekEnd);
        long monthFrom = DateUtils.startOfDay(monthStartDate);
        long monthTo = DateUtils.endOfMonth(today);
        long yearFrom = DateUtils.startOfDay(yearStartDate);
        long yearTo = DateUtils.endOfDay(today.withDayOfYear(today.lengthOfYear()));
        long lastMonthEnd = DateUtils.endOfMonth(today.minusMonths(1));

        observe(repository.observeAccounts(), 0, accounts -> {
            draft.accounts = accounts;
            draft.accountsCount = accounts.size();
            boolean foreign = false;
            for (AccountWithBalance item : accounts) {
                if (!currency.equalsIgnoreCase(item.account.currency)) {
                    foreign = true;
                    break;
                }
            }
            draft.hasForeignCurrencyAccounts = foreign;
        });
        observe(repository.observeBalanceUpTo(currency, lastMonthEnd), 1,
                value -> draft.previousMonthBalance = value == null ? 0 : value);
        observe(repository.observeTotalBalance(currency, DateUtils.endOfDay(today)), 2,
                value -> draft.totalBalance = value == null ? 0 : value);
        observe(repository.database().transactionDao().observeTotalForType(
                        TransactionType.INCOME, monthFrom, monthTo, currency), 3,
                value -> draft.monthIncome = value == null ? 0 : value);
        observe(repository.database().transactionDao().observeTotalForType(
                        TransactionType.EXPENSE, monthFrom, monthTo, currency), 4,
                value -> draft.monthExpense = value == null ? 0 : value);
        observe(repository.observeRecentTransactions(5), 5, value -> draft.recent = value);
        observe(repository.observeBudgets(currency), 6, this::applyBudgets);

        observe(repository.database().transactionDao().observeDailyTotals(
                TransactionType.INCOME, weekFrom, weekTo, currency), 7,
                value -> weekIncome = value);
        observe(repository.database().transactionDao().observeDailyTotals(
                TransactionType.EXPENSE, weekFrom, weekTo, currency), 7,
                value -> weekExpense = value);
        observe(repository.database().transactionDao().observeDailyTotals(
                TransactionType.INCOME, monthFrom, monthTo, currency), 7,
                value -> monthIncome = value);
        observe(repository.database().transactionDao().observeDailyTotals(
                TransactionType.EXPENSE, monthFrom, monthTo, currency), 7,
                value -> monthExpense = value);
        observe(repository.database().transactionDao().observeMonthlyTotals(
                yearFrom, yearTo, currency), 7, value -> yearMonths = value);
        observe(repository.database().transactionDao().observeCategoryTotals(
                TransactionType.EXPENSE, monthFrom, monthTo, currency), 8,
                value -> monthCategories = value == null
                        ? Collections.emptyList() : value);
    }

    private <T> void observe(LiveData<T> source, int index, ValueConsumer<T> consumer) {
        state.addSource(source, value -> {
            consumer.accept(value);
            if (index < seen.size()) {
                seen.set(index, true);
            }
            publish();
        });
    }

    private void applyBudgets(List<BudgetWithProgress> budgets) {
        List<BudgetWithProgress> active = new ArrayList<>();
        long todayStart = DateUtils.todayStart();
        long todayEnd = DateUtils.endOfDay(LocalDate.now());
        long total = 0;
        long spent = 0;
        for (BudgetWithProgress item : budgets) {
            if (item.budget.startDate <= todayEnd && item.budget.endDate >= todayStart) {
                active.add(item);
                total += item.budget.amountMinor;
                spent += item.spentMinor;
            }
        }
        draft.activeBudgets = active;
        draft.budgetTotal = total;
        draft.budgetSpent = spent;
    }

    private void publish() {
        draft.week = buildDailyChart(weekStart, 7, weekIncome, weekExpense, true);
        draft.month = buildDailyChart(monthStartDate, monthStartDate.lengthOfMonth(),
                monthIncome, monthExpense, false);
        draft.year = buildYearChart(yearMonths);
        draft.averageDailyExpense = draft.monthExpense
                / Math.max(1, daysElapsedThisMonth);
        applyTopCategory();
        draft.loaded = !seen.contains(false);
        state.setValue(draft);
    }

    private void applyTopCategory() {
        draft.topCategoryName = null;
        draft.topCategoryIcon = null;
        draft.topCategoryColor = null;
        draft.topCategoryTotal = 0;
        if (monthCategories == null || monthCategories.isEmpty()) {
            return;
        }
        CategoryTotal top = monthCategories.get(0);
        draft.topCategoryName = top.categoryName;
        draft.topCategoryIcon = top.categoryIcon;
        draft.topCategoryColor = top.categoryColor;
        draft.topCategoryTotal = top.totalMinor;
    }

    public LiveData<DashboardState> getState() {
        return state;
    }

    public String getCurrency() {
        return currency;
    }

    public FinanceRepository repository() {
        return repository;
    }

    private DashboardState.ChartData buildDailyChart(LocalDate start, int days,
                                                      List<DailyTotal> incomeValues,
                                                      List<DailyTotal> expenseValues,
                                                      boolean weekdayLabels) {
        DashboardState.ChartData data = new DashboardState.ChartData();
        Map<Long, Long> incomeByDay = toDayMap(incomeValues);
        Map<Long, Long> expenseByDay = toDayMap(expenseValues);
        for (int i = 0; i < days; i++) {
            LocalDate date = start.plusDays(i);
            long millis = DateUtils.startOfDay(date);
            String label = weekdayLabels
                    ? date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    : String.format(Locale.getDefault(), "%d", date.getDayOfMonth());
            data.labels.add(label);
            data.income.add(incomeByDay.getOrDefault(millis, 0L) / 100f);
            data.expense.add(expenseByDay.getOrDefault(millis, 0L) / 100f);
        }
        return data;
    }

    private DashboardState.ChartData buildYearChart(List<MonthlyTotal> months) {
        DashboardState.ChartData data = new DashboardState.ChartData();
        Map<String, MonthlyTotal> byKey = new HashMap<>();
        for (MonthlyTotal month : months) {
            byKey.put(month.monthKey, month);
        }
        for (int i = 0; i < 12; i++) {
            LocalDate date = yearStartDate.plusMonths(i);
            String key = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            MonthlyTotal total = byKey.get(key);
            data.labels.add(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            data.income.add((total == null ? 0 : total.incomeMinor) / 100f);
            data.expense.add((total == null ? 0 : total.expenseMinor) / 100f);
        }
        return data;
    }

    private Map<Long, Long> toDayMap(List<DailyTotal> totals) {
        Map<Long, Long> map = new HashMap<>();
        if (totals != null) {
            for (DailyTotal total : totals) {
                map.put(total.dayMillis, total.totalMinor);
            }
        }
        return map;
    }
}
