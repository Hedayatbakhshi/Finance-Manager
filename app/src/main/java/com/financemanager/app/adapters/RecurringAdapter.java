package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.databinding.ItemRecurringBinding;
import com.financemanager.app.ui.common.Labels;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;

/** Recurring schedule row with an active switch. */
public class RecurringAdapter extends ListAdapter<RecurringTransaction, RecurringAdapter.Holder> {

    public interface Listener {
        void onToggle(RecurringTransaction recurring, boolean active);

        void onOpen(RecurringTransaction recurring);
    }

    private final Listener listener;
    private String currency;

    public RecurringAdapter(Listener listener, String currency) {
        super(DIFF);
        this.listener = listener;
        this.currency = currency;
    }

    /** Updates the display currency after the household loads. */
    public void setCurrency(String currency) {
        if (currency == null || currency.equals(this.currency)) {
            return;
        }
        this.currency = currency;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<RecurringTransaction> DIFF =
            new DiffUtil.ItemCallback<RecurringTransaction>() {
                @Override
                public boolean areItemsTheSame(@NonNull RecurringTransaction oldItem,
                                               @NonNull RecurringTransaction newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull RecurringTransaction oldItem,
                                                  @NonNull RecurringTransaction newItem) {
                    return oldItem.id == newItem.id && oldItem.title.equals(newItem.title)
                            && oldItem.amountMinor == newItem.amountMinor
                            && oldItem.frequency == newItem.frequency
                            && oldItem.nextRunDate == newItem.nextRunDate
                            && oldItem.active == newItem.active;
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRecurringBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemRecurringBinding binding;

        Holder(ItemRecurringBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RecurringTransaction recurring) {
            android.content.Context context = binding.getRoot().getContext();
            binding.title.setText(recurring.title);
            String schedule = Labels.frequency(context, recurring.frequency);
            String subtitle = recurring.active
                    ? schedule + " · " + context.getString(R.string.next_run,
                    DateUtils.formatShortDay(recurring.nextRunDate))
                    : schedule + " · " + context.getString(R.string.recurring_paused);
            binding.subtitle.setText(subtitle);

            String amount = Money.format(recurring.amountMinor, currency);
            if (recurring.type == com.financemanager.app.models.TransactionType.INCOME) {
                binding.amount.setText("+" + amount);
                binding.amount.setTextColor(UiUtils.color(context, R.color.success));
            } else {
                binding.amount.setText("-" + amount);
                binding.amount.setTextColor(UiUtils.color(context, R.color.expense));
            }
            binding.icon.setImageResource(R.drawable.ic_action_repeat);
            UiUtils.tintIconTile(binding.iconContainer, binding.icon, "#5A87C0");
            binding.active.setOnCheckedChangeListener(null);
            binding.active.setChecked(recurring.active);
            binding.active.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggle(recurring, isChecked);
                }
            });
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOpen(recurring);
                }
            });
        }
    }
}
