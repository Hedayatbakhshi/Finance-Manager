package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.databinding.ItemTransactionBinding;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.TransactionWithDetails;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.google.android.material.color.MaterialColors;

/** Shared transaction row used on the dashboard, history lists and account details. */
public class TransactionAdapter
        extends ListAdapter<TransactionWithDetails, TransactionAdapter.Holder> {

    public interface OnTransactionListener {
        void onTransaction(TransactionWithDetails item);
    }

    private OnTransactionListener clickListener;
    private OnTransactionListener longClickListener;

    public TransactionAdapter() {
        super(DIFF);
    }

    public void setOnTransactionClickListener(OnTransactionListener listener) {
        this.clickListener = listener;
    }

    public void setOnTransactionLongClickListener(OnTransactionListener listener) {
        this.longClickListener = listener;
    }

    private static final DiffUtil.ItemCallback<TransactionWithDetails> DIFF =
            new DiffUtil.ItemCallback<TransactionWithDetails>() {
                @Override
                public boolean areItemsTheSame(@NonNull TransactionWithDetails oldItem,
                                               @NonNull TransactionWithDetails newItem) {
                    return oldItem.transaction.id == newItem.transaction.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull TransactionWithDetails oldItem,
                                                  @NonNull TransactionWithDetails newItem) {
                    TransactionEntity a = oldItem.transaction;
                    TransactionEntity b = newItem.transaction;
                    return a.id == b.id && a.title.equals(b.title)
                            && a.amountMinor == b.amountMinor && a.date == b.date
                            && a.type == b.type
                            && equal(a.description, b.description)
                            && equal(oldItem.categoryName, newItem.categoryName)
                            && equal(oldItem.categoryColor, newItem.categoryColor)
                            && equal(oldItem.accountName, newItem.accountName)
                            && equal(oldItem.toAccountName, newItem.toAccountName);
                }
            };

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        Holder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TransactionWithDetails item) {
            TransactionEntity transaction = item.transaction;
            String currency = item.accountCurrency == null ? "USD" : item.accountCurrency;
            binding.title.setText(transaction.title);

            android.content.Context context = binding.getRoot().getContext();
            if (transaction.isTransfer()) {
                String from = item.accountName == null
                        ? context.getString(R.string.account) : item.accountName;
                String to = item.toAccountName == null
                        ? context.getString(R.string.account) : item.toAccountName;
                binding.subtitle.setText(binding.getRoot().getContext().getString(
                        R.string.transfer_between, from, to) + " · "
                        + DateUtils.friendlyDate(binding.getRoot().getContext(), transaction.date));
                binding.icon.setImageResource(R.drawable.ic_action_transfer);
                UiUtils.tintIconTile(binding.iconContainer, binding.icon, "#5A87C0");
                binding.amount.setText(Money.format(transaction.amountMinor, currency));
                binding.amount.setTextColor(MaterialColors.getColor(binding.getRoot(),
                        com.google.android.material.R.attr.colorOnSurface));
                binding.typeBadge.setVisibility(View.VISIBLE);
                binding.typeBadge.setText(R.string.type_transfer);
            } else {
                String category = item.categoryName == null
                        ? context.getString(R.string.category_uncategorized) : item.categoryName;
                binding.subtitle.setText(category + " · "
                        + DateUtils.friendlyDate(binding.getRoot().getContext(), transaction.date));
                binding.icon.setImageResource(
                        com.financemanager.app.utils.Icons.res(item.categoryIcon));
                UiUtils.tintIconTile(binding.iconContainer, binding.icon,
                        item.categoryColor == null ? "#778397" : item.categoryColor);
                boolean income = transaction.type == com.financemanager.app.models.TransactionType.INCOME;
                int color = UiUtils.color(binding.getRoot().getContext(),
                        income ? R.color.success : R.color.expense);
                binding.amount.setTextColor(color);
                binding.amount.setText((income ? "+" : "-")
                        + Money.format(transaction.amountMinor, currency));
                binding.typeBadge.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTransaction(item);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onTransaction(item);
                    return true;
                }
                return false;
            });
        }
    }
}
