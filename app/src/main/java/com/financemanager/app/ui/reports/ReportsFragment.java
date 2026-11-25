package com.financemanager.app.ui.reports;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.R;
import com.financemanager.app.adapters.BreakdownAdapter;
import com.financemanager.app.databinding.FragmentReportsBinding;
import com.financemanager.app.models.CategoryTotal;
import com.financemanager.app.models.DailyTotal;
import com.financemanager.app.models.MonthlyTotal;
import com.financemanager.app.models.ReportResult;
import com.financemanager.app.ui.common.ChartStyler;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Period reports computed from real transactions. */
public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportsViewModel viewModel;
    private BreakdownAdapter breakdownAdapter;
    private final List<String> chartLabels = new ArrayList<>();
    private boolean resumedOnce;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(ReportsViewModel.class);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_nav_reports);
        binding.empty.emptyTitle.setText(R.string.empty_reports_title);
        binding.empty.emptyMessage.setText(R.string.empty_reports_message);
        binding.empty.emptyAction.setVisibility(View.GONE);

        breakdownAdapter = new BreakdownAdapter(0, viewModel.getCurrency());
        binding.breakdownList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.breakdownList.setAdapter(breakdownAdapter);
        ChartStyler.styleLineChart(binding.chart, viewModel.getCurrency());
        ChartStyler.stylePieChart(binding.pie);

        binding.periodGroup.check(R.id.period_this_month);
        binding.periodGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.period_last_month) {
                viewModel.load(ReportsViewModel.RANGE_LAST_MONTH);
            } else if (id == R.id.period_three_months) {
                viewModel.load(ReportsViewModel.RANGE_THREE_MONTHS);
            } else if (id == R.id.period_year) {
                viewModel.load(ReportsViewModel.RANGE_YEAR);
            } else {
                viewModel.load(ReportsViewModel.RANGE_THIS_MONTH);
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.scroll.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
        });

        viewModel.getReport().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(ReportResult result) {
        if (result == null) {
            return;
        }
        String currency = viewModel.getCurrency();
        binding.incomeTotal.setText(Money.format(result.incomeTotal, currency));
        binding.expenseTotal.setText(Money.format(result.expenseTotal, currency));
        long net = result.netCashFlow();
        binding.netTotal.setText((net >= 0 ? "+" : "") + Money.format(net, currency));
        binding.netTotal.setTextColor(UiUtils.color(requireContext(),
                net >= 0 ? R.color.success : R.color.expense));

        boolean empty = result.isEmpty();
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.chartCard.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.breakdownCard.setVisibility(empty ? View.GONE : View.VISIBLE);

        binding.averageDaily.setText(getString(R.string.average_per_day,
                Money.format(result.expenseTotal / result.dayCount(), currency)));
        binding.counts.setText(getString(R.string.transactions_count,
                result.incomeCount + result.expenseCount));

        CategoryTotal highest = result.expenseByCategory.isEmpty()
                ? null : result.expenseByCategory.get(0);
        binding.highestCategory.setText(highest == null || highest.categoryName == null
                ? getString(R.string.none) : highest.categoryName);
        if (result.largestExpense == null) {
            binding.largestTransaction.setText(R.string.none);
        } else {
            binding.largestTransaction.setText(result.largestExpense.transaction.title
                    + " · " + Money.format(result.largestExpense.transaction.amountMinor,
                    currency));
        }

        if (!empty) {
            renderChart(result);
            renderBreakdown(result);
        }
    }

    private void renderChart(ReportResult result) {
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        chartLabels.clear();

        boolean daily = result.dayCount() <= 62;
        if (daily) {
            Map<Long, Long> income = new HashMap<>();
            Map<Long, Long> expense = new HashMap<>();
            for (DailyTotal total : result.dailyIncome) {
                income.put(total.dayMillis, total.totalMinor);
            }
            for (DailyTotal total : result.dailyExpense) {
                expense.put(total.dayMillis, total.totalMinor);
            }
            LocalDate cursor = DateUtils.toLocalDate(result.fromDate);
            LocalDate last = DateUtils.toLocalDate(result.toDate);
            int index = 0;
            while (!cursor.isAfter(last)) {
                long start = DateUtils.startOfDay(cursor);
                incomeEntries.add(new Entry(index, units(income.getOrDefault(start, 0L))));
                expenseEntries.add(new Entry(index, units(expense.getOrDefault(start, 0L))));
                chartLabels.add(DateUtils.formatShortDay(start));
                cursor = cursor.plusDays(1);
                index++;
            }
        } else {
            for (int i = 0; i < result.monthly.size(); i++) {
                MonthlyTotal total = result.monthly.get(i);
                incomeEntries.add(new Entry(i, units(total.incomeMinor)));
                expenseEntries.add(new Entry(i, units(total.expenseMinor)));
                chartLabels.add(DateUtils.shortMonthKey(total.monthKey));
            }
        }

        int incomeColor = ChartStyler.incomeColor(requireContext());
        int expenseColor = ChartStyler.expenseColor(requireContext());
        LineDataSet incomeSet = new LineDataSet(incomeEntries, getString(R.string.chart_income));
        styleSet(incomeSet, incomeColor);
        LineDataSet expenseSet = new LineDataSet(expenseEntries, getString(R.string.chart_expense));
        styleSet(expenseSet, expenseColor);
        binding.chart.setData(new LineData(incomeSet, expenseSet));

        XAxis xAxis = binding.chart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(Math.max(1, chartLabels.size() - 1));
        xAxis.setLabelCount(Math.min(6, chartLabels.size()), false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index < 0 || index >= chartLabels.size()) {
                    return "";
                }
                return chartLabels.get(index);
            }
        });
        binding.chart.invalidate();
    }

    private void styleSet(LineDataSet set, int color) {
        set.setColor(color);
        set.setLineWidth(2f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        set.setFillColor(color);
        set.setFillAlpha(24);
        set.setHighlightEnabled(true);
        set.setHighLightColor(color);
    }

    private void renderBreakdown(ReportResult result) {
        breakdownAdapter.setTotal(result.expenseTotal);
        List<CategoryTotal> items = result.expenseByCategory;
        breakdownAdapter.submitList(new ArrayList<>(items));
        binding.pieTotal.setText(Money.formatCompact(result.expenseTotal,
                viewModel.getCurrency()));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int limit = Math.min(items.size(), 7);
        for (int i = 0; i < limit; i++) {
            CategoryTotal item = items.get(i);
            entries.add(new PieEntry(item.totalMinor, item.categoryName == null
                    ? "Other" : item.categoryName));
            colors.add(UiUtils.parseColor(item.categoryColor, 0xFF5B7FA6));
        }
        if (items.size() > limit) {
            long other = 0;
            for (int i = limit; i < items.size(); i++) {
                other += items.get(i).totalMinor;
            }
            if (other > 0) {
                entries.add(new PieEntry(other, "Other"));
                colors.add(Color.parseColor("#9AA7B8"));
            }
        }
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        set.setSliceSpace(2f);
        set.setDrawValues(false);
        binding.pie.setData(new PieData(set));
        binding.pie.invalidate();
    }

    private float units(long minor) {
        return minor / 100f;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!resumedOnce) {
            resumedOnce = true;
            return;
        }
        if (binding != null) {
            viewModel.load(viewModel.getRange());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
