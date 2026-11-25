package com.financemanager.app.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.databinding.ItemIconOptionBinding;
import com.financemanager.app.utils.Icons;

/** Horizontal row of selectable icons. */
public class IconOptionAdapter extends RecyclerView.Adapter<IconOptionAdapter.Holder> {

    public interface OnIconSelected {
        void onIcon(String key);
    }

    private final String[] icons;
    private final OnIconSelected listener;
    private String selected;
    private final String accentColor;

    public IconOptionAdapter(String[] icons, String selected, String accentColor,
                             OnIconSelected listener) {
        this.icons = icons;
        this.selected = selected;
        this.accentColor = accentColor;
        this.listener = listener;
    }

    public void setSelected(String key, String color) {
        this.selected = key;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemIconOptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(icons[position]);
    }

    @Override
    public int getItemCount() {
        return icons.length;
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemIconOptionBinding binding;

        Holder(ItemIconOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String key) {
            boolean isSelected = key.equals(selected);
            int base = com.financemanager.app.ui.common.UiUtils.parseColor(accentColor,
                    0xFF3E6DB5);
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setCornerRadius(com.financemanager.app.ui.common.UiUtils.dp(
                    binding.getRoot().getContext(), 14));
            background.setColor(com.financemanager.app.ui.common.UiUtils.withAlpha(base,
                    isSelected ? 0.22f : 0.10f));
            background.setStroke(com.financemanager.app.ui.common.UiUtils.dp(
                    binding.getRoot().getContext(), isSelected ? 2 : 1),
                    isSelected ? base : com.financemanager.app.ui.common.UiUtils.withAlpha(base, 0.16f));
            binding.box.setBackground(background);
            binding.icon.setImageResource(Icons.res(key));
            binding.icon.setColorFilter(base);
            binding.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            binding.getRoot().setOnClickListener(v -> {
                selected = key;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onIcon(key);
                }
            });
        }
    }
}
