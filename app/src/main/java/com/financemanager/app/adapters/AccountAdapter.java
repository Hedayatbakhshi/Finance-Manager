package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.databinding.ItemAccountBinding;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.ui.common.Labels;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

/** Account row showing its calculated balance. */
public class AccountAdapter extends ListAdapter<AccountWithBalance, AccountAdapter.Holder> {

    public interface OnAccountListener {
        void onAccount(AccountWithBalance account);
    }

    private final OnAccountListener listener;

    public AccountAdapter(OnAccountListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AccountWithBalance> DIFF =
            new DiffUtil.ItemCallback<AccountWithBalance>() {
                @Override
                public boolean areItemsTheSame(@NonNull AccountWithBalance oldItem,
                                               @NonNull AccountWithBalance newItem) {
                    return oldItem.account.id == newItem.account.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull AccountWithBalance oldItem,
                                                  @NonNull AccountWithBalance newItem) {
                    return oldItem.balanceMinor == newItem.balanceMinor
                            && oldItem.account.name.equals(newItem.account.name)
                            && oldItem.account.type == newItem.account.type
                            && oldItem.account.color.equals(newItem.account.color)
                            && oldItem.account.currency.equals(newItem.account.currency);
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAccountBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemAccountBinding binding;

        Holder(ItemAccountBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AccountWithBalance item) {
            binding.name.setText(item.account.name);
            binding.type.setText(Labels.accountType(binding.getRoot().getContext(),
                    item.account.type));
            binding.balance.setText(Money.format(item.balanceMinor, item.account.currency));
            binding.balance.setTextColor(item.balanceMinor < 0
                    ? UiUtils.color(binding.getRoot().getContext(),
                    com.financemanager.app.R.color.expense)
                    : UiUtils.themeColor(binding.getRoot(),
                    com.google.android.material.R.attr.colorOnSurface));
            binding.currency.setText(item.account.currency);
            binding.icon.setImageResource(Icons.res(item.account.icon));
            UiUtils.tintIconTile(binding.iconContainer, binding.icon, item.account.color);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccount(item);
                }
            });
        }
    }
}
