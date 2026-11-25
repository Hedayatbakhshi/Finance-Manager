package com.financemanager.app.ui.household;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.R;
import com.financemanager.app.adapters.HouseholdCategoryAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.databinding.FragmentHouseholdBudgetBinding;
import com.financemanager.app.models.HouseholdCategoryBudget;
import com.financemanager.app.models.HouseholdSummary;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Monthly household budget with per-category progress and month comparison. */
public class HouseholdBudgetFragment extends Fragment {

    private FragmentHouseholdBudgetBinding binding;
    private HouseholdCategoriesViewModel viewModel;
    private HouseholdCategoryAdapter adapter;
    private HouseholdOverviewViewModel summaryViewModel;
    private long householdId = -1;
    private long fromDate;
    private long toDate;
    private long currentSpent;
    private long totalBudget;
    private String summaryCurrency = "";
    private HouseholdSummary lastSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdBudgetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        householdId = getArguments() == null ? -1 : getArguments().getLong("householdId", -1);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdCategoriesViewModel.class);
        LocalDate today = LocalDate.now();
        fromDate = DateUtils.startOfMonth(today);
        toDate = DateUtils.endOfMonth(today);

        adapter = new HouseholdCategoryAdapter(viewModel.currency());
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.empty.getRoot().setVisibility(View.GONE);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.editBudget.setOnClickListener(v -> showBudgetDialog());

        summaryViewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdOverviewViewModel.class);
        summaryViewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getBudgets().observe(getViewLifecycleOwner(), this::renderCategories);
        viewModel.getCurrency().observe(getViewLifecycleOwner(), this::applyCurrency);
        viewModel.load(householdId, fromDate, toDate);
    }

    private void applyCurrency(String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        adapter.setCurrency(value);
        if (!value.equals(summaryCurrency)) {
            summaryCurrency = value;
            if (lastSummary != null) {
                renderSummary(lastSummary);
            }
            summaryViewModel.loadSummary(householdId, fromDate, toDate, value);
        }
    }

    private void renderCategories(List<HouseholdCategoryBudget> items) {
        adapter.setCurrency(viewModel.currency());
        adapter.submitList(items == null ? Collections.emptyList() : items);
    }

    private void renderSummary(HouseholdSummary summary) {
        if (summary == null) {
            return;
        }
        lastSummary = summary;
        currentSpent = summary.currentTotal;
        totalBudget = summary.budgetMinor;
        String currency = summaryCurrency.isEmpty() ? viewModel.currency() : summaryCurrency;
        binding.totalBudget.setText(Money.format(totalBudget, currency));
        binding.spent.setText(Money.format(currentSpent, currency));
        int percent = summary.percent;
        binding.percent.setText(getString(R.string.percent_used_short, percent));
        binding.progress.setProgress(Math.min(100, percent));
        binding.progress.setIndicatorColor(ContextCompat.getColor(requireContext(),
                percent >= 100 ? R.color.expense
                        : percent >= 80 ? R.color.warning : R.color.success));
        String comparison = getString(R.string.household_compare_month) + " · "
                + Money.format(summary.previousTotal, currency);
        if (summary.foreignCount > 0) {
            comparison += " · " + getString(R.string.household_currency_note);
        }
        binding.comparison.setText(comparison);
    }

    private void showBudgetDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(totalBudget == 0 ? "" : Money.toInput(totalBudget));
        input.setHint(R.string.household_monthly_budget);
        int padding = UiUtils.dp(requireContext(), 20);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_monthly_budget)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String text = input.getText() == null ? "" : input.getText().toString();
                    Long budget = Money.parsePositive(text);
                    viewModel.saveHouseholdBudget(budget == null ? 0 : budget,
                            new FinanceRepository.Callback<Long>() {
                                @Override
                                public void onSuccess(Long value) {
                                    UiUtils.snackbar(binding.getRoot(),
                                            getString(R.string.household_saved));
                                }

                                @Override
                                public void onError(String message) {
                                    UiUtils.snackbar(binding.getRoot(), message);
                                }
                            });
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
