package com.financemanager.app.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.databinding.ItemColorOptionBinding;
import com.financemanager.app.ui.common.UiUtils;

/** Horizontal row of selectable accent colors. */
public class ColorOptionAdapter extends RecyclerView.Adapter<ColorOptionAdapter.Holder> {

    public interface OnColorSelected {
        void onColor(String hex);
    }

    private final String[] colors;
    private final OnColorSelected listener;
    private String selected;

    public ColorOptionAdapter(String[] colors, String selected, OnColorSelected listener) {
        this.colors = colors;
        this.selected = selected;
        this.listener = listener;
    }

    public void setSelected(String hex) {
        this.selected = hex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemColorOptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(colors[position]);
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemColorOptionBinding binding;

        Holder(ItemColorOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String hex) {
            int color = UiUtils.parseColor(hex, 0xFF3E6DB5);
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(color);
            binding.swatch.setBackground(background);
            boolean isSelected = hex.equalsIgnoreCase(selected);
            binding.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            binding.getRoot().setOnClickListener(v -> {
                selected = hex;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onColor(hex);
                }
            });
        }
    }
}
