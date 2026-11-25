package com.financemanager.app.ui.common;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.financemanager.app.R;
import com.financemanager.app.utils.Money;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;

/** Applies the app's visual language to MPAndroidChart views. */
public final class ChartStyler {

    private ChartStyler() {
    }

    public static int incomeColor(Context context) {
        return ContextCompat.getColor(context, R.color.success);
    }

    public static int expenseColor(Context context) {
        return ContextCompat.getColor(context, R.color.expense);
    }

    public static int primaryColor(Context context) {
        return ContextCompat.getColor(context, R.color.secondary);
    }

    public static int gridColor(Context context) {
        return ContextCompat.getColor(context, R.color.chart_grid);
    }

    public static int labelColor(Context context) {
        return ContextCompat.getColor(context, R.color.text_secondary);
    }

    public static void styleLineChart(LineChart chart, String currency) {
        Context context = chart.getContext();
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setExtraOffsets(0f, 6f, 4f, 4f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(labelColor(context));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setYOffset(6f);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawAxisLine(false);
        yAxis.setDrawGridLines(true);
        yAxis.setGridColor(gridColor(context));
        yAxis.setGridLineWidth(1f);
        yAxis.setTextColor(labelColor(context));
        yAxis.setTextSize(10f);
        yAxis.setSpaceTop(24f);
        yAxis.setAxisMinimum(0f);
        yAxis.setValueFormatter(new MoneyAxisFormatter(currency));
        chart.getAxisRight().setEnabled(false);
        chart.setNoDataText("");
    }

    public static void styleBarChart(BarChart chart, String currency) {
        Context context = chart.getContext();
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setExtraOffsets(0f, 6f, 4f, 4f);
        chart.setFitBars(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(labelColor(context));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawAxisLine(false);
        yAxis.setDrawGridLines(true);
        yAxis.setGridColor(gridColor(context));
        yAxis.setTextColor(labelColor(context));
        yAxis.setTextSize(10f);
        yAxis.setAxisMinimum(0f);
        yAxis.setValueFormatter(new MoneyAxisFormatter(currency));
        chart.getAxisRight().setEnabled(false);
        chart.setNoDataText("");
    }

    public static void stylePieChart(PieChart chart) {
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(62f);
        chart.setTransparentCircleRadius(0f);
        chart.setDrawEntryLabels(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setUsePercentValues(false);
        chart.setExtraOffsets(6f, 6f, 6f, 6f);
        chart.setNoDataText("");
    }

    /** Formats chart axis values with the active currency, compactly. */
    public static class MoneyAxisFormatter extends ValueFormatter {
        private final String currency;

        public MoneyAxisFormatter(String currency) {
            this.currency = currency;
        }

        @Override
        public String getFormattedValue(float value) {
            return Money.formatCompact(Math.round(value * 100.0), currency);
        }
    }

    /** Formats raw amounts (minor units) for tooltips. */
    public static class AmountFormatter extends ValueFormatter {
        private final String currency;

        public AmountFormatter(String currency) {
            this.currency = currency;
        }

        @Override
        public String getFormattedValue(float value) {
            return Money.format(Math.round(value * 100.0), currency);
        }
    }
}
