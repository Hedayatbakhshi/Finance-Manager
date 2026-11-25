package com.financemanager.app.ui.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.ColorOptionAdapter;
import com.financemanager.app.adapters.IconOptionAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.databinding.BottomSheetCategoryEditorBinding;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.ui.common.Labels;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.Icons;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Creates and edits categories with icon and color pickers. */
public class CategoryEditorSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_TYPE = "type";
    private static final String ARG_ICON = "icon";
    private static final String ARG_COLOR = "color";
    private static final String ARG_DEFAULT = "is_default";

    private BottomSheetCategoryEditorBinding binding;
    private CategoriesViewModel viewModel;
    private IconOptionAdapter iconAdapter;
    private ColorOptionAdapter colorAdapter;
    private TransactionType type = TransactionType.EXPENSE;
    private String icon = "cat_other";
    private String color = "#778397";
    private long categoryId = -1;
    private boolean isDefault;

    public static void show(@NonNull FragmentManager manager, @Nullable Category category,
                            @NonNull TransactionType defaultType) {
        CategoryEditorSheet sheet = new CategoryEditorSheet();
        Bundle args = new Bundle();
        if (category != null) {
            args.putLong(ARG_ID, category.id);
            args.putString(ARG_NAME, category.name);
            args.putString(ARG_TYPE, category.type.name());
            args.putString(ARG_ICON, category.icon);
            args.putString(ARG_COLOR, category.color);
            args.putBoolean(ARG_DEFAULT, category.isDefault);
        } else {
            args.putLong(ARG_ID, -1);
            args.putString(ARG_TYPE, defaultType.name());
            args.putString(ARG_COLOR, "#3E6DB5");
            args.putString(ARG_ICON, defaultType == TransactionType.INCOME
                    ? "cat_salary" : "cat_other");
        }
        sheet.setArguments(args);
        sheet.show(manager, "category_editor");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCategoryEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        categoryId = args.getLong(ARG_ID, -1);
        type = TransactionType.fromKey(args.getString(ARG_TYPE, "EXPENSE"));
        icon = args.getString(ARG_ICON, "cat_other");
        color = args.getString(ARG_COLOR, "#778397");
        isDefault = args.getBoolean(ARG_DEFAULT, false);

        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(CategoriesViewModel.class);

        binding.title.setText(categoryId > 0 ? R.string.edit_category : R.string.new_category);
        binding.typeLabel.setText(Labels.transactionType(requireContext(), type));
        binding.nameInput.setText(args.getString(ARG_NAME, ""));
        binding.save.setOnClickListener(v -> save());
        binding.delete.setVisibility(categoryId > 0 && !isDefault ? View.VISIBLE : View.GONE);
        binding.delete.setOnClickListener(v -> confirmDelete());
        binding.defaultNote.setVisibility(categoryId > 0 && isDefault ? View.VISIBLE : View.GONE);

        binding.iconList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        String[] icons = type == TransactionType.INCOME
                ? Icons.incomeIcons() : Icons.expenseIcons();
        iconAdapter = new IconOptionAdapter(icons, icon, color, key -> icon = key);
        binding.iconList.setAdapter(iconAdapter);

        binding.colorList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        colorAdapter = new ColorOptionAdapter(Icons.colors(), color, hex -> {
            color = hex;
            iconAdapter.setSelected(icon, hex);
        });
        binding.colorList.setAdapter(colorAdapter);
    }

    private void save() {
        binding.nameLayout.setError(null);
        String name = binding.nameInput.getText() == null ? ""
                : binding.nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            binding.nameLayout.setError(getString(R.string.error_name_required));
            return;
        }
        Category category = new Category();
        category.id = categoryId > 0 ? categoryId : 0;
        category.name = name;
        category.type = type;
        category.icon = icon;
        category.color = color;
        category.isDefault = isDefault;
        binding.save.setEnabled(false);
        viewModel.saveCategory(category, categoryId <= 0, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                Toast.makeText(requireContext(), R.string.category_saved,
                        Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onError(String message) {
                if (binding == null) {
                    return;
                }
                binding.save.setEnabled(true);
                UiUtils.snackbar(binding.getRoot(), message);
            }
        });
    }

    private void confirmDelete() {
        UiUtils.confirm(requireContext(), getString(R.string.delete_category_title),
                getString(R.string.delete_category_message_empty,
                        binding.nameInput.getText().toString()),
                getString(R.string.delete), () -> viewModel.deleteCategory(categoryId,
                        new FinanceRepository.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean value) {
                                Toast.makeText(requireContext(), R.string.category_deleted,
                                        Toast.LENGTH_SHORT).show();
                                dismiss();
                            }

                            @Override
                            public void onError(String message) {
                                if (binding != null) {
                                    UiUtils.snackbar(binding.getRoot(), message);
                                }
                            }
                        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
