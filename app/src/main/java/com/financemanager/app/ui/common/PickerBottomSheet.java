package com.financemanager.app.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.R;
import com.financemanager.app.databinding.BottomSheetPickerBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/** A bottom sheet with an icon list used for accounts, categories and goals. */
public class PickerBottomSheet extends BottomSheetDialogFragment {

    public static final String RESULT_KEY = "picker_result";
    public static final String KEY_SELECTED_ID = "selected_id";
    public static final String KEY_CLEARED = "cleared";

    private static final String ARG_TITLE = "title";
    private static final String ARG_ITEMS = "items";
    private static final String ARG_SELECTED = "selected";
    private static final String ARG_ALLOW_CLEAR = "allow_clear";
    private static final String ARG_CLEAR_LABEL = "clear_label";

    private BottomSheetPickerBinding binding;

    public static void show(@NonNull FragmentManager manager, @Nullable String title,
                            @NonNull List<PickerItem> items, long selectedId,
                            boolean allowClear, @Nullable String clearLabel) {
        PickerBottomSheet sheet = new PickerBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putSerializable(ARG_ITEMS, new ArrayList<>(items));
        args.putLong(ARG_SELECTED, selectedId);
        args.putBoolean(ARG_ALLOW_CLEAR, allowClear);
        args.putString(ARG_CLEAR_LABEL, clearLabel);
        sheet.setArguments(args);
        sheet.show(manager, "picker_sheet");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        String title = args.getString(ARG_TITLE);
        binding.title.setText(title == null ? "" : title);

        long selectedId = args.getLong(ARG_SELECTED, -1);
        List<PickerItem> items = new ArrayList<>();
        java.io.Serializable serializable = args.getSerializable(ARG_ITEMS);
        if (serializable instanceof List) {
            for (Object item : (List<?>) serializable) {
                if (item instanceof PickerItem) {
                    items.add((PickerItem) item);
                }
            }
        }

        PickerAdapter adapter = new PickerAdapter(item -> {
            Bundle result = new Bundle();
            result.putLong(KEY_SELECTED_ID, item.id);
            result.putBoolean(KEY_CLEARED, false);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        }, selectedId);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        adapter.submitList(items);

        boolean allowClear = args.getBoolean(ARG_ALLOW_CLEAR, false);
        binding.clear.setVisibility(allowClear ? View.VISIBLE : View.GONE);
        String clearLabel = args.getString(ARG_CLEAR_LABEL);
        if (clearLabel != null) {
            binding.clear.setText(clearLabel);
        }
        binding.clear.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putLong(KEY_SELECTED_ID, -1);
            result.putBoolean(KEY_CLEARED, true);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });

        binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.empty.setText(R.string.none);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
