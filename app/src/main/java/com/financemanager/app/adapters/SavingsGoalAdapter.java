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
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.databinding.ItemGoalBinding;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

/** Savings goal card with quick add/withdraw actions. */
public class SavingsGoalAdapter extends ListAdapter<SavingsGoal, SavingsGoalAdapter.Holder> {

    public interface Listener {
        void onAdd(SavingsGoal goal);

        void onWithdraw(SavingsGoal goal);

        void onMenu(SavingsGoal goal, View anchor);
    }

    private final Listener listener;
    private final String currency;

    public SavingsGoalAdapter(Listener listener, String currency) {
        super(DIFF);
        this.listener = listener;
        this.currency = currency;
    }

    private static final DiffUtil.ItemCallback<SavingsGoal> DIFF =
            new DiffUtil.ItemCallback<SavingsGoal>() {
                @Override
                public boolean areItemsTheSame(@NonNull SavingsGoal oldItem,
                                               @NonNull SavingsGoal newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull SavingsGoal oldItem,
                                                  @NonNull SavingsGoal newItem) {
                    return oldItem.id == newItem.id && oldItem.name.equals(newItem.name)
                            && oldItem.targetMinor == newItem.targetMinor
                            && oldItem.currentMinor == newItem.currentMinor
                            && oldItem.targetDate == newItem.targetDate
                            && oldItem.color.equals(newItem.color)
                            && oldItem.icon.equals(newItem.icon);
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemGoalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemGoalBinding binding;

        Holder(ItemGoalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SavingsGoal goal) {
            android.content.Context context = binding.getRoot().getContext();
            binding.name.setText(goal.name);
            int percent = Money.percent(goal.currentMinor, goal.targetMinor);
            binding.percent.setText(context.getString(R.string.goal_percent, percent));
            binding.saved.setText(Money.format(goal.currentMinor, currency));
            binding.target.setText("/ " + Money.format(goal.targetMinor, currency));
            binding.progress.setProgress(percent);
            binding.progress.setIndicatorColor(UiUtils.parseColor(goal.color,
                    ContextCompat.getColor(context, R.color.success)));

            if (goal.targetDate > 0) {
                binding.targetDate.setText(DateUtils.formatDay(goal.targetDate));
                binding.targetDate.setVisibility(View.VISIBLE);
            } else {
                binding.targetDate.setText(R.string.goal_progress);
                binding.targetDate.setVisibility(View.VISIBLE);
            }

            binding.icon.setImageResource(Icons.res(goal.icon));
            UiUtils.tintIconTile(binding.iconContainer, binding.icon, goal.color);

            binding.addMoney.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAdd(goal);
                }
            });
            binding.withdraw.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWithdraw(goal);
                }
            });
            binding.more.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMenu(goal, v);
                }
            });
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMenu(goal, v);
                }
            });
        }
    }
}
