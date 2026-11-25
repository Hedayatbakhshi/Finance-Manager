package com.financemanager.app.ui.household;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.BreakdownAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.databinding.FragmentHouseholdReportsBinding;
import com.financemanager.app.models.HouseholdSummary;
import com.financemanager.app.ui.common.ChartStyler;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.CsvExporter;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Household reports with range selection, comparison and CSV export. */
public class HouseholdReportsFragment extends Fragment {

    private FragmentHouseholdReportsBinding binding;
    private HouseholdReportsViewModel viewModel;
    private BreakdownAdapter breakdownAdapter;
    private long householdId = -1;
    private long fromDate;
    private long toDate;
    private String currency;
    private HouseholdSummary lastReport;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    export(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        householdId = getArguments() == null ? -1 : getArguments().getLong("householdId", -1);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdReportsViewModel.class);
        currency = viewModel.currency;
        ChartStyler.styleBarChart(binding.chart, currency);
        breakdownAdapter = new BreakdownAdapter(0, currency);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(breakdownAdapter);
        binding.empty.getRoot().setVisibility(View.GONE);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.rangeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            binding.customRange.setVisibility(id == R.id.range_custom ? View.VISIBLE : View.GONE);
            LocalDate today = LocalDate.now();
            if (id == R.id.range_month) {
                load(DateUtils.startOfMonth(today), DateUtils.endOfMonth(today));
            } else if (id == R.id.range_last) {
                LocalDate last = today.minusMonths(1);
                load(DateUtils.startOfMonth(last), DateUtils.endOfMonth(last));
            }
        });
        binding.fromButton.setOnClickListener(v -> pickDate(true));
        binding.toButton.setOnClickListener(v -> pickDate(false));
        binding.export.setOnClickListener(v ->
                exportLauncher.launch(CsvExporter.suggestedHouseholdFileName()));

        load(DateUtils.startOfMonth(LocalDate.now()), DateUtils.endOfMonth(LocalDate.now()));
        viewModel.getReport().observe(getViewLifecycleOwner(), this::render);
    }

    private void pickDate(boolean from) {
        LocalDate initial = DateUtils.toLocalDate(from ? fromDate : toDate);
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            LocalDate date = LocalDate.of(year, month + 1, day);
            if (from) {
                fromDate = DateUtils.startOfDay(date);
                binding.fromButton.setText(DateUtils.formatShortDay(fromDate));
            } else {
                toDate = DateUtils.endOfDay(date);
                binding.toButton.setText(DateUtils.formatShortDay(toDate));
            }
            if (toDate >= fromDate) {
                load(fromDate, toDate);
            }
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void load(long from, long to) {
        fromDate = from;
        toDate = to;
        FinanceRepository repository = ((FinanceManagerApp) requireActivity().getApplication())
                .container().repository();
        repository.loadHousehold(householdId, new FinanceRepository.Callback<Household>() {
            @Override
            public void onSuccess(Household value) {
                if (value != null && value.currency != null) {
                    applyCurrency(value.currency);
                }
                viewModel.load(householdId, from, to, currency);
            }

            @Override
            public void onError(String message) {
                viewModel.load(householdId, from, to, currency);
            }
        });
    }

    private void applyCurrency(String value) {
        if (value == null || value.trim().isEmpty() || value.equals(currency)) {
            return;
        }
        currency = value;
        ChartStyler.styleBarChart(binding.chart, currency);
        breakdownAdapter.setCurrency(currency);
        if (lastReport != null) {
            render(lastReport);
        }
    }

    private void render(HouseholdSummary report) {
        if (report == null) {
            return;
        }
        lastReport = report;
        binding.total.setText(Money.format(report.currentTotal, currency));
        binding.count.setText(getString(R.string.transactions_count, report.expenseCount));
        long difference = report.currentTotal - report.previousTotal;
        String comparison = getString(R.string.household_compare_month) + " · "
                + Money.formatSigned(difference, currency);
        if (report.foreignCount > 0) {
            comparison += " · " + getString(R.string.household_currency_note);
        }
        binding.comparison.setText(comparison);
        breakdownAdapter.setTotal(report.currentTotal);
        breakdownAdapter.submitList(report.categories);
        boolean hasData = report.daily != null && !report.daily.isEmpty();
        binding.chart.setVisibility(hasData ? View.VISIBLE : View.GONE);
        binding.chartEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
        if (hasData) {
            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < report.daily.size(); i++) {
                entries.add(new BarEntry(i, report.daily.get(i).totalMinor / 100f));
            }
            BarDataSet set = new BarDataSet(entries, getString(R.string.chart_expense));
            set.setColor(ChartStyler.expenseColor(requireContext()));
            set.setDrawValues(false);
            binding.chart.setData(new BarData(set));
            binding.chart.getXAxis().setAxisMaximum(Math.max(1, report.daily.size() - 1));
            binding.chart.invalidate();
        }
    }

    private void export(Uri uri) {
        CsvExporter exporter = ((FinanceManagerApp) requireActivity().getApplication())
                .container().csvExporter();
        ((FinanceManagerApp) requireActivity().getApplication()).container().repository()
                .runAsync(() -> {
                    try {
                        int rows = exporter.exportHousehold(uri, householdId, fromDate, toDate,
                                currency);
                        requireActivity().runOnUiThread(() -> UiUtils.snackbar(binding.getRoot(),
                                getString(R.string.household_exported) + " · " + rows));
                    } catch (java.io.IOException e) {
                        requireActivity().runOnUiThread(() -> UiUtils.snackbar(binding.getRoot(),
                                e.getMessage() == null ? "Export failed" : e.getMessage()));
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
