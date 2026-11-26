package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.databinding.ItemBudgetBinding;
import com.financemanager.app.models.BudgetWithProgress;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

/** Budget card with a progress bar and a status pill. */
public class BudgetAdapter extends ListAdapter<BudgetWithProgress, BudgetAdapter.Holder> {

    public interface OnBudgetListener {
        void onBudget(BudgetWithProgress budget);
    }

    private final OnBudgetListener listener;
    private final String currency;

    public BudgetAdapter(OnBudgetListener listener, String currency) {
        super(DIFF);
        this.listener = listener;
        this.currency = currency;
    }

    private static final DiffUtil.ItemCallback<BudgetWithProgress> DIFF =
            new DiffUtil.ItemCallback<BudgetWithProgress>() {
                @Override
                public boolean areItemsTheSame(@NonNull BudgetWithProgress oldItem,
                                               @NonNull BudgetWithProgress newItem) {
                    return oldItem.budget.id == newItem.budget.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull BudgetWithProgress oldItem,
                                                  @NonNull BudgetWithProgress newItem) {
                    return oldItem.budget.id == newItem.budget.id
                            && oldItem.budget.name.equals(newItem.budget.name)
                            && oldItem.budget.amountMinor == newItem.budget.amountMinor
                            && oldItem.budget.startDate == newItem.budget.startDate
                            && oldItem.budget.endDate == newItem.budget.endDate
                            && oldItem.budget.rollover == newItem.budget.rollover
                            && oldItem.spentMinor == newItem.spentMinor
                            && oldItem.carryoverMinor == newItem.carryoverMinor
                            && oldItem.budget.categoryId == newItem.budget.categoryId;
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemBudgetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemBudgetBinding binding;

        Holder(ItemBudgetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BudgetWithProgress item) {
            android.content.Context context = binding.getRoot().getContext();
            binding.name.setText(item.budget.name);
            binding.period.setText(DateUtils.formatShortDay(item.budget.startDate) + " – "
                    + DateUtils.formatShortDay(item.budget.endDate));
            long available = item.availableMinor();
            binding.spent.setText(Money.format(item.spentMinor, currency));
            binding.total.setText("/ " + Money.format(available, currency));
            binding.rolloverNote.setVisibility(item.carryoverMinor > 0 ? View.VISIBLE : View.GONE);
            if (item.carryoverMinor > 0) {
                binding.rolloverNote.setText(context.getString(R.string.budget_rollover_note,
                        Money.format(item.carryoverMinor, currency)));
            }

            int percent = Money.rawPercent(item.spentMinor, available);
            binding.progress.setProgress(Math.min(100, Math.max(0, percent)));

            int color;
            int background;
            int label;
            if (percent >= 100) {
                color = ContextCompat.getColor(context, R.color.expense);
                background = R.drawable.bg_pill_expense;
                label = R.string.budget_status_exceeded;
            } else if (percent >= 80) {
                color = ContextCompat.getColor(context, R.color.expense);
                background = R.drawable.bg_pill_expense;
                label = R.string.budget_status_critical;
            } else if (percent >= 50) {
                color = ContextCompat.getColor(context, R.color.warning);
                background = R.drawable.bg_pill_warning;
                label = R.string.budget_status_warning;
            } else {
                color = ContextCompat.getColor(context, R.color.success);
                background = R.drawable.bg_pill_income;
                label = R.string.budget_status_healthy;
            }
            binding.progress.setIndicatorColor(color);
            binding.status.setText(label);
            binding.status.setTextColor(color);
            binding.status.setBackgroundResource(background);
            binding.percent.setText(context.getString(R.string.budget_percent_used,
                    Math.max(0, percent)));

            long remaining = available - item.spentMinor;
            if (remaining >= 0) {
                binding.remaining.setText(context.getString(R.string.remaining_amount,
                        Money.format(remaining, currency)));
                binding.remaining.setTextColor(UiUtils.color(context, R.color.text_secondary));
            } else {
                binding.remaining.setText(context.getString(R.string.budget_over_amount,
                        Money.format(-remaining, currency)));
                binding.remaining.setTextColor(UiUtils.color(context, R.color.expense));
            }

            binding.icon.setImageResource(Icons.res(item.categoryIcon));
            UiUtils.tintIconTile(binding.iconContainer, binding.icon,
                    item.categoryColor == null ? "#778397" : item.categoryColor);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBudget(item);
                }
            });
        }
    }
}
