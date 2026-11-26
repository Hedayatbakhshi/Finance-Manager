package com.financemanager.app.ui.budgets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.BudgetAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.databinding.FragmentBudgetsBinding;
import com.financemanager.app.models.BudgetWithProgress;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

import java.time.LocalDate;
import java.util.List;

/** Monthly budgets with live progress against real spending. */
public class BudgetsFragment extends Fragment {

    private FragmentBudgetsBinding binding;
    private BudgetsViewModel viewModel;
    private BudgetAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String currency = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings().getCurrency();
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(BudgetsViewModel.class);

        adapter = new BudgetAdapter(this::openBudget, currency);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_nav_budgets);
        binding.empty.emptyTitle.setText(R.string.empty_budgets_title);
        binding.empty.emptyMessage.setText(R.string.empty_budgets_message);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.no_budgets_cta);
        binding.empty.emptyAction.setOnClickListener(v -> openEditor(null));

        binding.add.setOnClickListener(v -> openEditor(null));
        binding.fab.setOnClickListener(v -> openEditor(null));

        viewModel.getBudgets().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<BudgetWithProgress> budgets) {
        adapter.submitList(budgets);
        boolean empty = budgets.isEmpty();
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.summaryCard.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.sectionLabel.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            return;
        }

        long total = 0;
        long spent = 0;
        long todayStart = DateUtils.todayStart();
        long todayEnd = DateUtils.endOfDay(LocalDate.now());
        int activeCount = 0;
        for (BudgetWithProgress item : budgets) {
            if (item.budget.startDate <= todayEnd && item.budget.endDate >= todayStart) {
                total += item.availableMinor();
                spent += item.spentMinor;
                activeCount++;
            }
        }
        String currency = viewModel.getCurrency();
        binding.summaryTotal.setText(Money.format(total, currency));
        int percent = Money.percent(spent, total);
        binding.summaryPercent.setText(getString(R.string.percent_used_short, percent));
        binding.summaryProgress.setProgress(percent);
        binding.summaryProgress.setIndicatorColor(ContextCompat.getColor(requireContext(),
                percent >= 100 ? R.color.expense : percent >= 80 ? R.color.warning
                        : R.color.success));
        binding.summaryDetail.setText(getString(R.string.spent_amount,
                Money.format(spent, currency)) + " · " + getString(R.string.remaining_amount,
                Money.format(Math.max(0, total - spent), currency)));
        binding.sectionLabel.setText(getString(R.string.budget_this_period) + " ("
                + activeCount + ")");
    }

    private void openBudget(BudgetWithProgress budget) {
        openEditor(budget);
    }

    private void openEditor(@Nullable BudgetWithProgress budget) {
        Bundle args = new Bundle();
        args.putLong("budgetId", budget == null ? -1 : budget.budget.id);
        NavHostFragment.findNavController(this).navigate(R.id.budgetEditor, args);
    }

    private void confirmDelete(BudgetWithProgress budget) {
        UiUtils.confirm(requireContext(), getString(R.string.delete_budget_title),
                getString(R.string.delete_budget_message, budget.budget.name),
                getString(R.string.delete), () -> viewModel.deleteBudget(budget.budget.id,
                        new FinanceRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                Toast.makeText(requireContext(), R.string.budget_deleted,
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                if (binding != null) {
                                    UiUtils.snackbar(binding.getRoot(), message);
                                }
                            }
                        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
