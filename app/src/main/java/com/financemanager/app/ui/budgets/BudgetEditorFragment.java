package com.financemanager.app.ui.budgets;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Budget;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.databinding.FragmentBudgetEditorBinding;
import com.financemanager.app.ui.common.PickerBottomSheet;
import com.financemanager.app.ui.common.PickerItem;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Form for a category budget over a date range. */
public class BudgetEditorFragment extends Fragment {

    private FragmentBudgetEditorBinding binding;
    private BudgetEditorViewModel viewModel;
    private final List<Category> categories = new ArrayList<>();
    private boolean filled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(BudgetEditorViewModel.class);
        long budgetId = getArguments() == null ? -1 : getArguments().getLong("budgetId", -1);
        if (budgetId > 0) {
            viewModel.load(budgetId);
        }

        binding.toolbar.setTitle(viewModel.isEditing()
                ? R.string.edit_budget : R.string.new_budget);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.currencySymbol.setText(Money.symbol(
                ((FinanceManagerApp) requireActivity().getApplication())
                        .container().settings().getCurrency()));
        binding.categoryField.fieldLabel.setText(R.string.category);
        binding.categoryField.fieldRoot.setOnClickListener(v -> openCategoryPicker());
        binding.startDate.setOnClickListener(v -> pickDate(true));
        binding.endDate.setOnClickListener(v -> pickDate(false));
        binding.save.setOnClickListener(v -> save());
        binding.delete.setVisibility(viewModel.isEditing() ? View.VISIBLE : View.GONE);
        binding.delete.setOnClickListener(v -> confirmDelete());
        renderDates();
        renderCategory();

        getParentFragmentManager().setFragmentResultListener(PickerBottomSheet.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    long id = bundle.getLong(PickerBottomSheet.KEY_SELECTED_ID, -1);
                    if (id > 0) {
                        viewModel.selectedCategoryId = id;
                        renderCategory();
                        if (binding.nameInput.getText() == null
                                || binding.nameInput.getText().toString().trim().isEmpty()) {
                            for (Category category : categories) {
                                if (category.id == id) {
                                    binding.nameInput.setText(category.name + " budget");
                                    break;
                                }
                            }
                        }
                    }
                });

        viewModel.getCategories().observe(getViewLifecycleOwner(), value -> {
            categories.clear();
            categories.addAll(value);
            if (viewModel.selectedCategoryId <= 0 && !categories.isEmpty()
                    && !viewModel.isEditing()) {
                viewModel.selectedCategoryId = categories.get(0).id;
            }
            renderCategory();
        });

        viewModel.getLoaded().observe(getViewLifecycleOwner(), budget -> {
            if (budget == null || filled) {
                return;
            }
            filled = true;
            binding.nameInput.setText(budget.name);
            binding.amountInput.setText(Money.toInput(budget.amountMinor));
            renderDates();
            renderCategory();
        });

        viewModel.getDeleted().observe(getViewLifecycleOwner(), success -> {
            if (success == null) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), R.string.budget_deleted,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDates() {
        binding.startDate.setText(getString(R.string.budget_start_date) + ": "
                + DateUtils.formatShortDay(viewModel.startDate));
        binding.endDate.setText(getString(R.string.budget_end_date) + ": "
                + DateUtils.formatShortDay(viewModel.endDate));
    }

    private void renderCategory() {
        Category selected = null;
        for (Category category : categories) {
            if (category.id == viewModel.selectedCategoryId) {
                selected = category;
                break;
            }
        }
        if (selected == null) {
            binding.categoryField.fieldValue.setText(R.string.category);
            binding.categoryField.fieldIconContainer.setVisibility(View.GONE);
        } else {
            binding.categoryField.fieldValue.setText(selected.name);
            binding.categoryField.fieldIconContainer.setVisibility(View.VISIBLE);
            binding.categoryField.fieldIcon.setImageResource(Icons.res(selected.icon));
            UiUtils.tintCircle(binding.categoryField.fieldIconContainer,
                    binding.categoryField.fieldIcon, selected.color);
        }
    }

    private void openCategoryPicker() {
        List<PickerItem> items = new ArrayList<>();
        for (Category category : categories) {
            items.add(new PickerItem(category.id, category.name,
                    getString(R.string.type_expense), category.icon, category.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(), getString(R.string.category),
                items, viewModel.selectedCategoryId, false, null);
    }

    private void pickDate(boolean isStart) {
        LocalDate initial = DateUtils.toLocalDate(isStart ? viewModel.startDate : viewModel.endDate);
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            long millis = DateUtils.startOfDay(LocalDate.of(year, month + 1, day));
            if (isStart) {
                viewModel.startDate = millis;
                if (viewModel.endDate < viewModel.startDate) {
                    viewModel.endDate = DateUtils.endOfDay(LocalDate.of(year, month + 1, day));
                }
            } else {
                viewModel.endDate = DateUtils.endOfDay(LocalDate.of(year, month + 1, day));
                if (viewModel.endDate < viewModel.startDate) {
                    viewModel.startDate = DateUtils.startOfDay(LocalDate.of(year, month + 1, day));
                }
            }
            renderDates();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void save() {
        binding.nameLayout.setError(null);
        binding.amountError.setVisibility(View.GONE);
        String name = binding.nameInput.getText() == null ? ""
                : binding.nameInput.getText().toString().trim();
        String amount = binding.amountInput.getText() == null ? ""
                : binding.amountInput.getText().toString();
        boolean valid = true;
        if (name.isEmpty()) {
            binding.nameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        }
        if (Money.parsePositive(amount) == null) {
            binding.amountError.setText(R.string.error_amount_invalid);
            binding.amountError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (viewModel.selectedCategoryId <= 0) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_category_required));
            valid = false;
        }
        if (viewModel.startDate > viewModel.endDate) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_date_range));
            valid = false;
        }
        if (!valid) {
            return;
        }
        binding.save.setEnabled(false);
        viewModel.save(name, amount, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                if (binding == null) {
                    return;
                }
                binding.save.setEnabled(true);
                Toast.makeText(requireContext(), R.string.budget_saved,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(BudgetEditorFragment.this).navigateUp();
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
        String name = binding.nameInput.getText() == null ? ""
                : binding.nameInput.getText().toString();
        UiUtils.confirm(requireContext(), getString(R.string.delete_budget_title),
                getString(R.string.delete_budget_message, name),
                getString(R.string.delete), () -> viewModel.delete());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
