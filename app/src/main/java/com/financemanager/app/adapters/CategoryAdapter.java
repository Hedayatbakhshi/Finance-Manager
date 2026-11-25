package com.financemanager.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.R;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.databinding.ItemCategoryBinding;
import com.financemanager.app.ui.common.Labels;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.Icons;

/** Category row with a management menu. */
public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.Holder> {

    public interface OnCategoryMenu {
        void onMenu(Category category, View anchor);
    }

    private final OnCategoryMenu menuListener;

    public CategoryAdapter(OnCategoryMenu menuListener) {
        super(DIFF);
        this.menuListener = menuListener;
    }

    private static final DiffUtil.ItemCallback<Category> DIFF =
            new DiffUtil.ItemCallback<Category>() {
                @Override
                public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                    return oldItem.id == newItem.id && oldItem.name.equals(newItem.name)
                            && oldItem.type == newItem.type
                            && oldItem.color.equals(newItem.color)
                            && oldItem.icon.equals(newItem.icon)
                            && oldItem.isDefault == newItem.isDefault;
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        Holder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Category category) {
            binding.name.setText(category.name);
            String type = Labels.transactionType(binding.getRoot().getContext(), category.type);
            binding.usage.setText(category.isDefault ? type + " · Default" : type);
            binding.icon.setImageResource(Icons.res(category.icon));
            UiUtils.tintIconTile(binding.iconContainer, binding.icon, category.color);
            binding.more.setOnClickListener(v -> {
                if (menuListener != null) {
                    menuListener.onMenu(category, v);
                }
            });
            binding.getRoot().setOnClickListener(v -> {
                if (menuListener != null) {
                    menuListener.onMenu(category, v);
                }
            });
            binding.getRoot().setContentDescription(category.name);
        }
    }
}
