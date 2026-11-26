package com.financemanager.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.TransactionAdapter;
import com.financemanager.app.databinding.FragmentDashboardBinding;
import com.financemanager.app.models.TransactionWithDetails;
import com.financemanager.app.notifications.NotificationHelper;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.ui.MainActivity;
import com.financemanager.app.ui.common.ChartStyler;
import com.financemanager.app.ui.common.FinanceMarkerView;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Financial overview: balance, cash flow, chart, recent activity, budgets, shortcuts. */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private SettingsRepository settings;
    private TransactionAdapter recentAdapter;
    private DashboardState lastState;
    private int chartRange = DashboardState.RANGE_MONTH;
    private boolean balanceVisible = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings();
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(DashboardViewModel.class);
        balanceVisible = !settings.isHideBalance();

        recentAdapter = new TransactionAdapter();
        recentAdapter.setOnTransactionClickListener(this::openTransaction);
        binding.recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentList.setAdapter(recentAdapter);
        binding.recentList.setNestedScrollingEnabled(false);

        binding.recentEmpty.emptyIcon.setImageResource(R.drawable.ic_nav_transactions);
        binding.recentEmpty.emptyTitle.setText(R.string.empty_transactions_title);
        binding.recentEmpty.emptyMessage.setText(R.string.empty_transactions_message);
        binding.recentEmpty.emptyAction.setVisibility(View.VISIBLE);
        binding.recentEmpty.emptyAction.setText(R.string.no_transactions_cta);
        binding.recentEmpty.emptyAction.setOnClickListener(v -> openNewTransaction("EXPENSE"));

        setUpChart();
        setUpActions();
        setGreeting();

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            lastState = state;
            render(state);
        });
    }

    private void setUpActions() {
        binding.toggleBalance.setOnClickListener(v -> {
            balanceVisible = !balanceVisible;
            settings.setHideBalance(!balanceVisible);
            if (lastState != null) {
                render(lastState);
            }
        });
        binding.seeAll.setOnClickListener(v -> navigateTopLevel(R.id.transactions));
        binding.actionReports.setOnClickListener(v -> navigateTopLevel(R.id.reports));
        binding.manageBudgets.setOnClickListener(v -> navigateTopLevel(R.id.budgets));
        binding.createBudget.setOnClickListener(v -> navigate(R.id.budgetEditor, new Bundle()));
        binding.actionIncome.setOnClickListener(v -> openNewTransaction("INCOME"));
        binding.actionExpense.setOnClickListener(v -> openNewTransaction("EXPENSE"));
        binding.actionTransfer.setOnClickListener(v -> openNewTransaction("TRANSFER"));
        binding.avatar.setOnClickListener(v -> navigate(R.id.settings, new Bundle()));
        binding.notifications.setOnClickListener(v -> {
            if (!NotificationHelper.canNotify(requireContext())) {
                UiUtils.confirm(requireContext(), getString(R.string.notifications),
                        getString(R.string.notifications_permission_needed),
                        getString(R.string.notifications_permission_settings),
                        () -> {
                            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE,
                                            requireContext().getPackageName());
                            startActivity(intent);
                        });
            } else {
                navigate(R.id.settings, new Bundle());
            }
        });

        binding.chartToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.chart_week) {
                chartRange = DashboardState.RANGE_WEEK;
            } else if (checkedId == R.id.chart_year) {
                chartRange = DashboardState.RANGE_YEAR;
            } else {
                chartRange = DashboardState.RANGE_MONTH;
            }
            if (lastState != null) {
                renderChart(lastState);
            }
        });
    }

    private void setUpChart() {
        ChartStyler.styleLineChart(binding.chart, viewModel.getCurrency());
        FinanceMarkerView marker = new FinanceMarkerView(requireContext(),
                R.layout.view_chart_tooltip);
        marker.setCurrency(viewModel.getCurrency());
        binding.chart.setMarker(marker);
    }

    private void setGreeting() {
        int hour = LocalTime.now().getHour();
        int greetingRes;
        if (hour < 12) {
            greetingRes = R.string.greeting_morning;
        } else if (hour < 17) {
            greetingRes = R.string.greeting_afternoon;
        } else if (hour < 22) {
            greetingRes = R.string.greeting_evening;
        } else {
            greetingRes = R.string.greeting_night;
        }
        binding.greetingLabel.setText(getString(greetingRes).toUpperCase(Locale.getDefault()));
        String name = settings.getUserName();
        if (name.isEmpty()) {
            binding.greeting.setText(R.string.welcome_back);
            binding.avatarText.setText("FM");
        } else {
            binding.greeting.setText(name);
            binding.avatarText.setText(UiUtils.initials(name));
        }
    }

    private void render(DashboardState state) {
        setGreeting();
        binding.avatarText.setText(UiUtils.initials(settings.getUserName()));
        String currency = state.currency;

        binding.balance.setText(shouldMask() ? getString(R.string.masked_amount)
                : Money.format(state.totalBalance, currency));
        binding.toggleBalance.setImageResource(balanceVisible
                ? R.drawable.ic_action_visibility : R.drawable.ic_action_visibility_off);

        binding.incomeAmount.setText(shouldMask() ? getString(R.string.masked_amount)
                : Money.format(state.monthIncome, currency));
        binding.expenseAmount.setText(shouldMask() ? getString(R.string.masked_amount)
                : Money.format(state.monthExpense, currency));
        renderTrend(binding.incomeTrend, state.incomeChangePercent(), true);
        renderTrend(binding.expenseTrend, state.expenseChangePercent(), false);
        renderWageUsage(state);

        binding.accountsSummary.setText(getString(R.string.accounts_summary,
                state.accountsCount, currency));
        binding.currencyNote.setVisibility(state.hasForeignCurrencyAccounts
                ? View.VISIBLE : View.GONE);

        Double change = state.balanceChangePercent();
        if (change == null) {
            binding.trendPill.setVisibility(View.INVISIBLE);
            binding.trendNote.setText(R.string.balance_no_history);
        } else {
            binding.trendPill.setVisibility(View.VISIBLE);
            binding.trendValue.setText(String.format(Locale.getDefault(), "%+.1f%%", change));
            binding.trendIcon.setImageResource(change >= 0
                    ? R.drawable.ic_action_trending_up : R.drawable.ic_action_trending_down);
            binding.trendIcon.setColorFilter(ContextCompat.getColor(requireContext(),
                    change >= 0 ? R.color.success : R.color.expense));
            binding.trendNote.setText(R.string.balance_compared_short);
        }

        recentAdapter.submitList(state.recent);
        boolean hasRecent = !state.recent.isEmpty();
        binding.recentList.setVisibility(hasRecent ? View.VISIBLE : View.GONE);
        binding.recentEmpty.getRoot().setVisibility(hasRecent ? View.GONE : View.VISIBLE);

        renderInsights(state);
        renderBudget(state);
        renderChart(state);
    }

    /** Shows a change versus last month, colored by whether the change is favorable. */
    private void renderTrend(TextView view, Double change, boolean increaseIsGood) {
        if (change == null) {
            view.setText(R.string.no_last_month_data);
            return;
        }
        view.setText(getString(R.string.change_vs_last_month,
                String.format(Locale.getDefault(), "%+.1f%%", change)));
        boolean favorable = (change >= 0) == increaseIsGood;
        view.setTextColor(ContextCompat.getColor(requireContext(),
                favorable ? R.color.success : R.color.expense));
    }

    private void renderWageUsage(DashboardState state) {
        long income = state.monthIncome;
        long expense = state.monthExpense;
        boolean hasData = income > 0 || expense > 0;
        binding.wageCard.setVisibility(hasData ? View.VISIBLE : View.GONE);
        if (!hasData) {
            return;
        }

        String incomeText = shouldMask() ? getString(R.string.masked_amount)
                : Money.format(income, state.currency);
        String expenseText = shouldMask() ? getString(R.string.masked_amount)
                : Money.format(expense, state.currency);
        binding.wageIncomeAmount.setText(incomeText);
        binding.wageExpenseAmount.setText(expenseText);

        if (income <= 0) {
            binding.wageBars.setVisibility(View.GONE);
            binding.wagePercent.setVisibility(View.GONE);
            binding.wageNote.setText(R.string.wage_usage_none);
            return;
        }

        binding.wageBars.setVisibility(View.VISIBLE);
        binding.wagePercent.setVisibility(View.VISIBLE);
        int used = Money.rawPercent(expense, income);
        binding.wagePercent.setText(getString(R.string.percent_used_short, used));
        binding.wagePercent.setTextColor(ContextCompat.getColor(requireContext(),
                used >= 100 ? R.color.expense
                        : used >= 80 ? R.color.warning : R.color.success));

        long baseline = Math.max(income, expense);
        binding.wageIncomeBar.setProgress(Math.min(100,
                Money.rawPercent(income, baseline)));
        binding.wageExpenseBar.setProgress(Math.min(100,
                Money.rawPercent(expense, baseline)));

        long left = income - expense;
        binding.wageNote.setText(getString(
                left >= 0 ? R.string.wage_usage_left : R.string.wage_usage_over,
                shouldMask() ? getString(R.string.masked_amount)
                        : Money.format(Math.abs(left), state.currency)));
    }

    private void renderInsights(DashboardState state) {
        boolean visible = state.monthExpense > 0 && state.topCategoryTotal > 0;
        binding.insightsCard.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        String name = state.topCategoryName == null
                ? getString(R.string.category_uncategorized) : state.topCategoryName;
        binding.topCategoryName.setText(name);
        binding.topCategoryAmount.setText(shouldMask()
                ? getString(R.string.masked_amount)
                : Money.format(state.topCategoryTotal, state.currency));
        binding.dailyAverage.setText(shouldMask()
                ? getString(R.string.masked_amount)
                : Money.format(state.averageDailyExpense, state.currency));
        binding.topCategoryIcon.setImageResource(Icons.res(state.topCategoryIcon));
        UiUtils.tintCircle(binding.topCategoryIconContainer, binding.topCategoryIcon,
                state.topCategoryColor == null ? "#778397" : state.topCategoryColor);
    }

    private void renderBudget(DashboardState state) {
        boolean hasBudget = !state.activeBudgets.isEmpty() && state.budgetTotal > 0;
        binding.budgetContent.setVisibility(hasBudget ? View.VISIBLE : View.GONE);
        binding.budgetEmpty.setVisibility(hasBudget ? View.GONE : View.VISIBLE);
        if (!hasBudget) {
            return;
        }
        int percent = state.budgetPercent();
        binding.budgetTotal.setText(getString(R.string.budgets_total) + " · "
                + (shouldMask() ? getString(R.string.masked_amount)
                : Money.format(state.budgetTotal, state.currency)));
        binding.budgetPercent.setText(getString(R.string.percent_used_short, percent));
        binding.budgetProgress.setProgress(percent);
        int color;
        if (percent >= 100) {
            color = ContextCompat.getColor(requireContext(), R.color.expense);
        } else if (percent >= 80) {
            color = ContextCompat.getColor(requireContext(), R.color.warning);
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.success);
        }
        binding.budgetProgress.setIndicatorColor(color);
        long remaining = state.budgetTotal - state.budgetSpent;
        String detail = getString(R.string.spent_amount,
                shouldMask() ? getString(R.string.masked_amount)
                        : Money.format(state.budgetSpent, state.currency))
                + " · "
                + (remaining >= 0
                ? getString(R.string.remaining_amount, shouldMask()
                ? getString(R.string.masked_amount) : Money.format(remaining, state.currency))
                : getString(R.string.budget_over_amount, shouldMask()
                ? getString(R.string.masked_amount) : Money.format(-remaining, state.currency)));
        binding.budgetDetail.setText(detail);
    }

    private void renderChart(DashboardState state) {
        DashboardState.ChartData data = state.chart(chartRange);
        binding.chartCaption.setText(chartRange == DashboardState.RANGE_WEEK
                ? R.string.date_range_week
                : chartRange == DashboardState.RANGE_YEAR
                ? R.string.date_range_year : R.string.date_range_month);

        if (!data.hasData()) {
            binding.chart.setVisibility(View.GONE);
            binding.chartEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.chart.setVisibility(View.VISIBLE);
        binding.chartEmpty.setVisibility(View.GONE);

        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < data.labels.size(); i++) {
            incomeEntries.add(new Entry(i, data.income.get(i)));
            expenseEntries.add(new Entry(i, data.expense.get(i)));
        }

        int incomeColor = ContextCompat.getColor(requireContext(), R.color.success);
        int expenseColor = ContextCompat.getColor(requireContext(), R.color.expense);

        LineDataSet incomeSet = new LineDataSet(incomeEntries, getString(R.string.chart_income));
        incomeSet.setColor(incomeColor);
        incomeSet.setLineWidth(2f);
        incomeSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        incomeSet.setDrawCircles(false);
        incomeSet.setDrawValues(false);
        incomeSet.setDrawFilled(true);
        incomeSet.setFillColor(incomeColor);
        incomeSet.setFillAlpha(28);
        incomeSet.setHighlightEnabled(true);
        incomeSet.setHighLightColor(incomeColor);
        incomeSet.setHighlightLineWidth(1f);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, getString(R.string.chart_expense));
        expenseSet.setColor(expenseColor);
        expenseSet.setLineWidth(2f);
        expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        expenseSet.setDrawCircles(false);
        expenseSet.setDrawValues(false);
        expenseSet.setDrawFilled(true);
        expenseSet.setFillColor(expenseColor);
        expenseSet.setFillAlpha(24);
        expenseSet.setHighlightEnabled(true);
        expenseSet.setHighLightColor(expenseColor);
        expenseSet.setHighlightLineWidth(1f);

        binding.chart.setData(new LineData(incomeSet, expenseSet));
        binding.chart.getXAxis().setAxisMinimum(0f);
        binding.chart.getXAxis().setAxisMaximum(Math.max(1, data.labels.size() - 1));
        binding.chart.getXAxis().setLabelCount(Math.min(
                chartRange == DashboardState.RANGE_WEEK ? 7 : 6, data.labels.size()), false);
        binding.chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index < 0 || index >= data.labels.size()) {
                    return "";
                }
                return data.labels.get(index);
            }
        });
        binding.chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.chart.notifyDataSetChanged();
        binding.chart.invalidate();
    }

    private boolean shouldMask() {
        return !balanceVisible;
    }

    private void openTransaction(TransactionWithDetails item) {
        Bundle args = new Bundle();
        args.putLong("transactionId", item.transaction.id);
        args.putLong("initialAccountId", -1);
        navigate(R.id.transactionEditor, args);
    }

    private void openNewTransaction(String type) {
        Bundle args = new Bundle();
        args.putLong("transactionId", -1);
        args.putString("initialType", type);
        args.putLong("initialAccountId", -1);
        navigate(R.id.transactionEditor, args);
    }

    private void navigate(int destination, Bundle args) {
        NavController controller = NavHostFragment.findNavController(this);
        controller.navigate(destination, args);
    }

    private void navigateTopLevel(int destinationId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateTopLevel(destinationId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (settings != null) {
            boolean visible = !settings.isHideBalance();
            if (visible != balanceVisible) {
                balanceVisible = visible;
                if (lastState != null) {
                    render(lastState);
                }
            }
            setGreeting();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
