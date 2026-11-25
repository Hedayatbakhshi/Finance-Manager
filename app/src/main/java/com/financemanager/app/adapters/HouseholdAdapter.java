package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.databinding.ItemHouseholdBinding;
import com.financemanager.app.utils.DateUtils;

/** Household cards on the households screen. */
public class HouseholdAdapter extends ListAdapter<Household, HouseholdAdapter.Holder> {

    public interface Listener {
        void onHousehold(Household household);
    }

    private Listener listener;

    public HouseholdAdapter() {
        super(DIFF);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Household> DIFF =
            new DiffUtil.ItemCallback<Household>() {
                @Override
                public boolean areItemsTheSame(@NonNull Household oldItem,
                                               @NonNull Household newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Household oldItem,
                                                  @NonNull Household newItem) {
                    return oldItem.id == newItem.id && oldItem.name.equals(newItem.name)
                            && oldItem.description.equals(newItem.description)
                            && oldItem.currency.equals(newItem.currency)
                            && oldItem.monthlyBudgetMinor == newItem.monthlyBudgetMinor;
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHouseholdBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemHouseholdBinding binding;

        Holder(ItemHouseholdBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Household household) {
            binding.name.setText(household.name);
            String subtitle = household.description == null || household.description.trim().isEmpty()
                    ? binding.getRoot().getContext().getString(R.string.household_created,
                    DateUtils.formatDay(household.createdAt))
                    : household.description;
            binding.subtitle.setText(subtitle);
            binding.currency.setText(household.currency);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHousehold(household);
                }
            });
        }
    }
}
