package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.databinding.ItemHouseholdCategoryBinding;
import com.financemanager.app.models.HouseholdCategoryBudget;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

/** Linked household categories with their spending progress. */
public class HouseholdCategoryAdapter
        extends ListAdapter<HouseholdCategoryBudget, HouseholdCategoryAdapter.Holder> {

    public interface Listener {
        void onCategory(HouseholdCategoryBudget item);
    }

    private String currency;
    private Listener listener;

    public HouseholdCategoryAdapter(String currency) {
        super(DIFF);
        this.currency = currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<HouseholdCategoryBudget> DIFF =
            new DiffUtil.ItemCallback<HouseholdCategoryBudget>() {
                @Override
                public boolean areItemsTheSame(@NonNull HouseholdCategoryBudget oldItem,
                                               @NonNull HouseholdCategoryBudget newItem) {
                    return oldItem.category.id == newItem.category.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull HouseholdCategoryBudget oldItem,
                                                  @NonNull HouseholdCategoryBudget newItem) {
                    return oldItem.spentMinor == newItem.spentMinor
                            && oldItem.category.monthlyBudgetMinor
                            == newItem.category.monthlyBudgetMinor;
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHouseholdCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemHouseholdCategoryBinding binding;

        Holder(ItemHouseholdCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HouseholdCategoryBudget item) {
            binding.name.setText(item.categoryName == null ? "" : item.categoryName);
            binding.icon.setImageResource(Icons.res(item.categoryIcon));
            UiUtils.tintCircle(binding.iconContainer, binding.icon,
                    item.categoryColor == null ? "#778397" : item.categoryColor);
            long budget = item.category.monthlyBudgetMinor;
            int percent = budget > 0 ? Money.rawPercent(item.spentMinor, budget) : 0;
            binding.progress.setProgress(Math.min(100, percent));
            binding.progress.setIndicatorColor(ContextCompat.getColor(binding.getRoot().getContext(),
                    budget > 0 && item.spentMinor > budget ? R.color.expense
                            : budget > 0 && percent >= 80 ? R.color.warning : R.color.success));
            binding.status.setText(budget > 0
                    ? binding.getRoot().getContext().getString(R.string.percent_used_short, percent)
                    : "");
            String spent = Money.format(item.spentMinor, currency);
            binding.detail.setText(budget > 0
                    ? spent + " / " + Money.format(budget, currency) : spent);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategory(item);
                }
            });
        }
    }
}
