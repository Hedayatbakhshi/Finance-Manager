package com.financemanager.app.ui.household;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.R;
import com.financemanager.app.adapters.HouseholdCategoryAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.databinding.FragmentHouseholdCategoriesBinding;
import com.financemanager.app.models.HouseholdCategoryBudget;
import com.financemanager.app.ui.common.PickerBottomSheet;
import com.financemanager.app.ui.common.PickerItem;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Links existing categories to a household and sets per-category limits. */
public class HouseholdCategoriesFragment extends Fragment {

    private FragmentHouseholdCategoriesBinding binding;
    private HouseholdCategoriesViewModel viewModel;
    private HouseholdCategoryAdapter adapter;
    private long householdId = -1;
    private List<Category> available = Collections.emptyList();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        householdId = getArguments() == null ? -1 : getArguments().getLong("householdId", -1);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdCategoriesViewModel.class);
        LocalDate today = LocalDate.now();
        viewModel.load(householdId, DateUtils.startOfMonth(today),
                DateUtils.endOfMonth(today));

        adapter = new HouseholdCategoryAdapter(viewModel.currency());
        adapter.setListener(this::editCategory);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_action_category);
        binding.empty.emptyTitle.setText(R.string.household_no_categories);
        binding.empty.emptyMessage.setText(R.string.household_link_category);
        binding.empty.emptyAction.setVisibility(View.GONE);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.fab.setOnClickListener(v -> showLinkPicker());

        getParentFragmentManager().setFragmentResultListener(PickerBottomSheet.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    long selected = bundle.getLong(PickerBottomSheet.KEY_SELECTED_ID, -1);
                    if (selected > 0) {
                        viewModel.link(selected, new FinanceRepository.Callback<Long>() {
                            @Override
                            public void onSuccess(Long value) {
                            }

                            @Override
                            public void onError(String message) {
                                UiUtils.snackbar(binding.getRoot(), message);
                            }
                        });
                    }
                });

        viewModel.getCurrency().observe(getViewLifecycleOwner(), adapter::setCurrency);
        viewModel.getBudgets().observe(getViewLifecycleOwner(), items -> {
            adapter.setCurrency(viewModel.currency());
            boolean empty = items == null || items.isEmpty();
            adapter.submitList(empty ? Collections.emptyList() : items);
            binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        });
        viewModel.getAvailable().observe(getViewLifecycleOwner(), value ->
                available = value == null ? Collections.emptyList() : value);
    }

    private void showLinkPicker() {
        List<PickerItem> items = new ArrayList<>();
        for (Category category : available) {
            items.add(new PickerItem(category.id, category.name, "",
                    category.icon, category.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(R.string.household_link_category), items, -1, false, null);
    }

    private void editCategory(HouseholdCategoryBudget item) {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(item.category.monthlyBudgetMinor == 0 ? ""
                : Money.toInput(item.category.monthlyBudgetMinor));
        input.setHint(R.string.household_category_budget);
        int padding = UiUtils.dp(requireContext(), 20);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.categoryName)
                .setView(container)
                .setNeutralButton(R.string.household_unlink, (dialog, which) ->
                        viewModel.unlink(item.category.id,
                                new FinanceRepository.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                    }

                                    @Override
                                    public void onError(String message) {
                                        UiUtils.snackbar(binding.getRoot(), message);
                                    }
                                }))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String text = input.getText() == null ? "" : input.getText().toString();
                    Long limit = Money.parsePositive(text);
                    viewModel.saveLimit(item, limit == null ? 0 : limit,
                            new FinanceRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void value) {
                                }

                                @Override
                                public void onError(String message) {
                                    UiUtils.snackbar(binding.getRoot(), message);
                                }
                            });
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
