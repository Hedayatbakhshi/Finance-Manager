package com.financemanager.app.ui.common;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.financemanager.app.databinding.ItemPickerBinding;
import com.financemanager.app.utils.Icons;

/** Renders picker rows with icons and a selection check mark. */
public class PickerAdapter extends ListAdapter<PickerItem, PickerAdapter.Holder> {

    public interface OnPickListener {
        void onPick(PickerItem item);
    }

    private final OnPickListener listener;
    private long selectedId;

    public PickerAdapter(OnPickListener listener, long selectedId) {
        super(DIFF);
        this.listener = listener;
        this.selectedId = selectedId;
    }

    private static final DiffUtil.ItemCallback<PickerItem> DIFF =
            new DiffUtil.ItemCallback<PickerItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull PickerItem oldItem, @NonNull PickerItem newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull PickerItem oldItem, @NonNull PickerItem newItem) {
                    return oldItem.id == newItem.id
                            && oldItem.title.equals(newItem.title)
                            && oldItem.subtitle.equals(newItem.subtitle)
                            && oldItem.colorHex.equals(newItem.colorHex)
                            && oldItem.iconKey.equals(newItem.iconKey);
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPickerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), getItem(position).id == selectedId);
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemPickerBinding binding;

        Holder(ItemPickerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PickerItem item, boolean selected) {
            binding.title.setText(item.title);
            binding.subtitle.setText(item.subtitle);
            binding.subtitle.setVisibility(item.subtitle.isEmpty()
                    ? android.view.View.GONE : android.view.View.VISIBLE);
            binding.icon.setImageResource(Icons.res(item.iconKey));
            UiUtils.tintCircle(binding.iconContainer, binding.icon, item.colorHex);
            binding.check.setVisibility(selected ? android.view.View.VISIBLE
                    : android.view.View.INVISIBLE);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPick(item);
                }
            });
        }
    }
}
