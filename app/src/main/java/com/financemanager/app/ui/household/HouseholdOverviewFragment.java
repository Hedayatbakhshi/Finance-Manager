package com.financemanager.app.ui.household;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.BreakdownAdapter;
import com.financemanager.app.adapters.HouseholdExpenseAdapter;
import com.financemanager.app.adapters.RecurringAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.databinding.FragmentHouseholdOverviewBinding;
import com.financemanager.app.models.DailyTotal;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.models.HouseholdSummary;
import com.financemanager.app.ui.common.ChartStyler;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Household dashboard built from the existing transaction ledger. */
public class HouseholdOverviewFragment extends Fragment {

    private FragmentHouseholdOverviewBinding binding;
    private HouseholdOverviewViewModel viewModel;
    private BreakdownAdapter categoryAdapter;
    private HouseholdExpenseAdapter expenseAdapter;
    private RecurringAdapter recurringAdapter;
    private FinanceRepository repository;
    private long householdId = -1;
    private String currency;
    private HouseholdSummary lastSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        householdId = getArguments() == null ? -1 : getArguments().getLong("householdId", -1);
        repository = ((FinanceManagerApp) requireActivity().getApplication())
                .container().repository();
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdOverviewViewModel.class);
        currency = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings().getCurrency();

        ChartStyler.styleLineChart(binding.chart, currency);
        categoryAdapter = new BreakdownAdapter(0, currency);
        binding.topCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.topCategories.setAdapter(categoryAdapter);

        expenseAdapter = new HouseholdExpenseAdapter();
        expenseAdapter.setCurrency(currency);
        expenseAdapter.setListener(new HouseholdExpenseAdapter.Listener() {
            @Override
            public void onExpense(HouseholdExpense expense) {
                openExpense(expense);
            }

            @Override
            public void onLongExpense(HouseholdExpense expense) {
                openExpense(expense);
            }
        });
        binding.recentExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentExpenses.setAdapter(expenseAdapter);
        binding.empty.emptyTitle.setText(R.string.household_empty_expenses_title);
        binding.empty.emptyMessage.setText(R.string.household_empty_expenses_message);
        binding.empty.emptyAction.setVisibility(View.GONE);

        recurringAdapter = new RecurringAdapter(new RecurringAdapter.Listener() {
            @Override
            public void onToggle(RecurringTransaction recurring, boolean active) {
                repository.setRecurringActive(recurring.id, active,
                        new FinanceRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                reload();
                            }

                            @Override
                            public void onError(String message) {
                            }
                        });
            }

            @Override
            public void onOpen(RecurringTransaction recurring) {
                Bundle args = new Bundle();
                args.putLong("recurringId", recurring.id);
                args.putLong("householdId", householdId);
                NavHostFragment.findNavController(HouseholdOverviewFragment.this)
                        .navigate(R.id.recurringEditor, args);
            }
        }, currency);
        binding.upcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.upcoming.setAdapter(recurringAdapter);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.openExpenses.setOnClickListener(v -> navigate(R.id.householdExpenses));
        binding.openCategories.setOnClickListener(v -> navigate(R.id.householdCategories));
        binding.openBudget.setOnClickListener(v -> navigate(R.id.householdBudget));
        binding.openReports.setOnClickListener(v -> navigate(R.id.householdReports));
        binding.openSettings.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("householdId", householdId);
            NavHostFragment.findNavController(this).navigate(R.id.householdEditor, args);
        });

        reload();
        viewModel.getHousehold().observe(getViewLifecycleOwner(), this::renderHousehold);
        viewModel.getSummary().observe(getViewLifecycleOwner(), this::render);
    }

    private void reload() {
        LocalDate today = LocalDate.now();
        viewModel.load(householdId, DateUtils.startOfMonth(today),
                DateUtils.endOfMonth(today), currency);
    }

    private void renderHousehold(Household household) {
        if (household == null) {
            return;
        }
        applyCurrency(household.currency);
        binding.householdName.setText(household.name);
        binding.householdMeta.setText(household.description == null
                || household.description.trim().isEmpty()
                ? getString(R.string.household_created, DateUtils.formatDay(household.createdAt))
                : household.description);
    }

    private void applyCurrency(String value) {
        if (value == null || value.trim().isEmpty() || value.equals(currency)) {
            return;
        }
        currency = value;
        categoryAdapter.setCurrency(currency);
        recurringAdapter.setCurrency(currency);
        expenseAdapter.setCurrency(currency);
        ChartStyler.styleLineChart(binding.chart, currency);
        binding.chart.invalidate();
        if (lastSummary != null) {
            render(lastSummary);
        }
    }

    private void render(HouseholdSummary summary) {
        if (summary == null) {
            return;
        }
        lastSummary = summary;
        binding.spent.setText(Money.format(summary.currentTotal, currency));
        binding.remaining.setText(Money.format(summary.remainingMinor, currency));
        binding.remaining.setTextColor(ContextCompat.getColor(requireContext(),
                summary.remainingMinor < 0 ? R.color.expense : R.color.success));
        binding.percent.setText(getString(R.string.percent_used_short, summary.percent));
        binding.progress.setProgress(Math.min(100, summary.percent));
        binding.progress.setIndicatorColor(ContextCompat.getColor(requireContext(),
                summary.percent >= 100 ? R.color.expense
                        : summary.percent >= 80 ? R.color.warning : R.color.success));
        StringBuilder note = new StringBuilder(summary.budgetMinor <= 0
                ? getString(R.string.household_no_budget)
                : getString(R.string.household_compare_month) + " · "
                + Money.format(summary.previousTotal, currency));
        if (summary.foreignCount > 0) {
            note.append(" · ").append(getString(R.string.household_currency_note));
        }
        binding.budgetNote.setText(note.toString());

        List<com.financemanager.app.models.CategoryTotal> top =
                summary.categories.size() > 5 ? summary.categories.subList(0, 5)
                        : summary.categories;
        categoryAdapter.setTotal(summary.currentTotal);
        categoryAdapter.submitList(top);
        expenseAdapter.submitList(summary.recent);
        recurringAdapter.submitList(summary.upcoming == null
                ? Collections.emptyList() : summary.upcoming);
        binding.empty.getRoot().setVisibility(summary.recent.isEmpty()
                ? View.VISIBLE : View.GONE);
        renderChart(summary.daily);
    }

    private void renderChart(List<DailyTotal> daily) {
        boolean hasData = daily != null && !daily.isEmpty();
        binding.chart.setVisibility(hasData ? View.VISIBLE : View.GONE);
        binding.chartEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
        if (!hasData) {
            return;
        }
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < daily.size(); i++) {
            entries.add(new Entry(i, daily.get(i).totalMinor / 100f));
        }
        LineDataSet set = new LineDataSet(entries, getString(R.string.chart_expense));
        set.setColor(ChartStyler.expenseColor(requireContext()));
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setFillColor(ChartStyler.expenseColor(requireContext()));
        set.setFillAlpha(28);
        set.setDrawFilled(true);
        binding.chart.setData(new LineData(set));
        binding.chart.getXAxis().setAxisMinimum(0);
        binding.chart.getXAxis().setAxisMaximum(Math.max(1, daily.size() - 1));
        binding.chart.getXAxis().setLabelCount(Math.min(5, daily.size()), false);
        binding.chart.getXAxis().setValueFormatter(
                new com.github.mikephil.charting.formatter.ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int index = Math.round(value);
                        return index >= 0 && index < daily.size()
                                ? DateUtils.formatShortDay(daily.get(index).dayMillis) : "";
                    }
                });
        binding.chart.invalidate();
    }

    private void openExpense(HouseholdExpense expense) {
        Bundle args = new Bundle();
        args.putLong("householdId", householdId);
        args.putLong("expenseId", expense.transaction.transaction.id);
        NavHostFragment.findNavController(this).navigate(R.id.householdExpenseEditor, args);
    }

    private void navigate(int destination) {
        Bundle args = new Bundle();
        args.putLong("householdId", householdId);
        NavHostFragment.findNavController(this).navigate(destination, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
