package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.databinding.ItemBreakdownBinding;
import com.financemanager.app.models.CategoryTotal;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

/** Category rows with share-of-total bars for report screens. */
public class BreakdownAdapter extends ListAdapter<CategoryTotal, BreakdownAdapter.Holder> {

    private long total;
    private String currency;

    public BreakdownAdapter(long total, String currency) {
        super(DIFF);
        this.total = total;
        this.currency = currency;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    /** Updates the display currency after the household loads. */
    public void setCurrency(String currency) {
        if (currency == null || currency.equals(this.currency)) {
            return;
        }
        this.currency = currency;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<CategoryTotal> DIFF =
            new DiffUtil.ItemCallback<CategoryTotal>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryTotal oldItem,
                                               @NonNull CategoryTotal newItem) {
                    if (oldItem.categoryId == null || newItem.categoryId == null) {
                        return oldItem.categoryId == newItem.categoryId;
                    }
                    return oldItem.categoryId.longValue() == newItem.categoryId.longValue();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CategoryTotal oldItem,
                                                  @NonNull CategoryTotal newItem) {
                    return oldItem.totalMinor == newItem.totalMinor
                            && oldItem.categoryName != null
                            && oldItem.categoryName.equals(newItem.categoryName);
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemBreakdownBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemBreakdownBinding binding;

        Holder(ItemBreakdownBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CategoryTotal item) {
            binding.name.setText(item.categoryName == null
                    ? binding.getRoot().getContext().getString(R.string.category_uncategorized)
                    : item.categoryName);
            binding.amount.setText(Money.format(item.totalMinor, currency));
            binding.icon.setImageResource(Icons.res(item.categoryIcon));
            UiUtils.tintIconTile(binding.iconContainer, binding.icon,
                    item.categoryColor == null ? "#778397" : item.categoryColor);
            int percent = Money.percent(item.totalMinor, total);
            binding.progress.setProgress(percent);
            binding.progress.setIndicatorColor(UiUtils.parseColor(item.categoryColor,
                    0xFF5B7FA6));
            binding.percent.setText(percent + "%");
        }
    }
}
