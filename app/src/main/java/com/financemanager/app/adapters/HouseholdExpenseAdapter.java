package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.databinding.ItemHouseholdExpenseBinding;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

/** Household expense rows; each row is a real ledger transaction. */
public class HouseholdExpenseAdapter
        extends ListAdapter<HouseholdExpense, HouseholdExpenseAdapter.Holder> {

    public interface Listener {
        void onExpense(HouseholdExpense expense);

        void onLongExpense(HouseholdExpense expense);
    }

    private Listener listener;
    private String householdCurrency;

    public HouseholdExpenseAdapter() {
        super(DIFF);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Household currency, used to explain rows recorded in another currency. */
    public void setCurrency(String currency) {
        if (currency == null || currency.equals(householdCurrency)) {
            return;
        }
        this.householdCurrency = currency;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<HouseholdExpense> DIFF =
            new DiffUtil.ItemCallback<HouseholdExpense>() {
                @Override
                public boolean areItemsTheSame(@NonNull HouseholdExpense oldItem,
                                               @NonNull HouseholdExpense newItem) {
                    return oldItem.transaction.transaction.id == newItem.transaction.transaction.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull HouseholdExpense oldItem,
                                                  @NonNull HouseholdExpense newItem) {
                    return oldItem.transaction.transaction.id == newItem.transaction.transaction.id
                            && oldItem.transaction.transaction.amountMinor
                            == newItem.transaction.transaction.amountMinor
                            && oldItem.transaction.transaction.title.equals(
                            newItem.transaction.transaction.title);
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHouseholdExpenseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemHouseholdExpenseBinding binding;

        Holder(ItemHouseholdExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HouseholdExpense item) {
            String category = item.transaction.categoryName == null
                    ? binding.getRoot().getContext().getString(R.string.category_uncategorized)
                    : item.transaction.categoryName;
            String subtitle = category;
            if (item.memberName != null && !item.memberName.isEmpty()) {
                subtitle += " · " + item.memberName;
            }
            String accountCurrency = item.transaction.accountCurrency;
            boolean foreign = householdCurrency != null && accountCurrency != null
                    && !householdCurrency.equalsIgnoreCase(accountCurrency);
            if (foreign) {
                // Never restate an amount without an exchange rate: label the real currency.
                subtitle += " · " + accountCurrency;
            }
            binding.title.setText(item.transaction.transaction.title);
            binding.subtitle.setText(subtitle);
            binding.date.setText(DateUtils.friendlyDate(binding.getRoot().getContext(),
                    item.transaction.transaction.date));
            String displayCurrency = accountCurrency == null || accountCurrency.trim().isEmpty()
                    ? householdCurrency : accountCurrency;
            binding.amount.setText(Money.format(item.transaction.transaction.amountMinor,
                    displayCurrency));
            binding.icon.setImageResource(Icons.res(item.transaction.categoryIcon));
            UiUtils.tintCircle(binding.iconContainer, binding.icon,
                    item.transaction.categoryColor == null ? "#778397"
                            : item.transaction.categoryColor);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExpense(item);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongExpense(item);
                }
                return true;
            });
        }
    }
}
