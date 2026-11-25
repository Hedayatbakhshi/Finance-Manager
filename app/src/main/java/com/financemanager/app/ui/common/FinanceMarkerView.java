package com.financemanager.app.ui.common;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.financemanager.app.R;
import com.financemanager.app.utils.Money;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.List;

/** Tooltip showing income and expense for the highlighted chart point. */
public class FinanceMarkerView extends MarkerView {

    private final TextView title;
    private final TextView income;
    private final TextView expense;
    private String currency = "USD";

    public FinanceMarkerView(@NonNull Context context, int layoutResource) {
        super(context, layoutResource);
        title = findViewById(R.id.tooltip_title);
        income = findViewById(R.id.tooltip_income);
        expense = findViewById(R.id.tooltip_expense);
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = Math.max(0, Math.round(e.getX()));
        LineChart chart = (LineChart) getChartView();
        if (chart != null && chart.getLineData() != null) {
            List<ILineDataSet> sets = chart.getLineData().getDataSets();
            long incomeMinor = readValue(sets, 0, index);
            long expenseMinor = readValue(sets, 1, index);
            income.setText(getContext().getString(R.string.tooltip_income) + "  "
                    + Money.format(incomeMinor, currency));
            expense.setText(getContext().getString(R.string.tooltip_expense) + "  "
                    + Money.format(expenseMinor, currency));
            if (chart.getXAxis().getValueFormatter() != null) {
                title.setText(chart.getXAxis().getValueFormatter().getFormattedValue(e.getX()));
            }
        }
        super.refreshContent(e, highlight);
    }

    private long readValue(List<ILineDataSet> sets, int setIndex, int index) {
        if (sets.size() <= setIndex) {
            return 0;
        }
        Entry entry = sets.get(setIndex).getEntryForIndex(index);
        return entry == null ? 0 : Math.round(entry.getY() * 100.0);
    }

}
