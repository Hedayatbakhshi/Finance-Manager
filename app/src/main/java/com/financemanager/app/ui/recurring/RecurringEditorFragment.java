package com.financemanager.app.ui.recurring;

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
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.databinding.FragmentRecurringEditorBinding;
import com.financemanager.app.models.RecurrenceFrequency;
import com.financemanager.app.models.TransactionType;
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

/** Creates or edits a recurring transaction schedule. */
public class RecurringEditorFragment extends Fragment {

    private FragmentRecurringEditorBinding binding;
    private RecurringEditorViewModel viewModel;
    private final List<Account> accounts = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private String pendingPicker;
    private boolean filled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecurringEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(RecurringEditorViewModel.class);
        Bundle args = getArguments();
        long id = args == null ? -1 : args.getLong("recurringId", -1);
        if (args != null) {
            viewModel.householdId = args.getLong("householdId", -1);
            viewModel.householdCategoryId = args.getLong("householdCategoryId", -1);
        }
        if (id > 0) {
            viewModel.load(id);
        }

        binding.toolbar.setTitle(viewModel.isEditing()
                ? R.string.edit_recurring : R.string.new_recurring);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.currencySymbol.setText(Money.symbol(
                ((FinanceManagerApp) requireActivity().getApplication())
                        .container().settings().getCurrency()));
        binding.categoryField.fieldLabel.setText(R.string.category);
        binding.accountField.fieldLabel.setText(R.string.account);
        binding.toAccountField.fieldLabel.setText(R.string.to_account);
        binding.nextDateField.fieldLabel.setText(R.string.date);
        binding.categoryField.fieldRoot.setOnClickListener(v -> openCategoryPicker());
        binding.accountField.fieldRoot.setOnClickListener(v -> openAccountPicker(false));
        binding.toAccountField.fieldRoot.setOnClickListener(v -> openAccountPicker(true));
        binding.nextDateField.fieldRoot.setOnClickListener(v -> pickDate());
        binding.delete.setVisibility(viewModel.isEditing() ? View.VISIBLE : View.GONE);
        binding.delete.setOnClickListener(v -> confirmDelete());
        binding.save.setOnClickListener(v -> save());

        int checkedType = viewModel.type == TransactionType.INCOME ? R.id.type_income
                : viewModel.type == TransactionType.TRANSFER ? R.id.type_transfer
                : R.id.type_expense;
        binding.typeGroup.check(checkedType);
        binding.typeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            TransactionType type = checkedId == R.id.type_income ? TransactionType.INCOME
                    : checkedId == R.id.type_transfer ? TransactionType.TRANSFER
                    : TransactionType.EXPENSE;
            viewModel.setType(type);
            updateTypeUi();
        });

        binding.frequencyGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            if (checked == R.id.freq_daily) {
                viewModel.frequency = RecurrenceFrequency.DAILY;
            } else if (checked == R.id.freq_weekly) {
                viewModel.frequency = RecurrenceFrequency.WEEKLY;
            } else if (checked == R.id.freq_yearly) {
                viewModel.frequency = RecurrenceFrequency.YEARLY;
            } else {
                viewModel.frequency = RecurrenceFrequency.MONTHLY;
            }
        });
        checkFrequency(viewModel.frequency);

        getParentFragmentManager().setFragmentResultListener(PickerBottomSheet.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    long picked = bundle.getLong(PickerBottomSheet.KEY_SELECTED_ID, -1);
                    if (picked <= 0) {
                        pendingPicker = null;
                        return;
                    }
                    if ("category".equals(pendingPicker)) {
                        viewModel.selectedCategoryId = picked;
                    } else if ("account".equals(pendingPicker)) {
                        viewModel.selectedAccountId = picked;
                    } else if ("toAccount".equals(pendingPicker)) {
                        viewModel.selectedToAccountId = picked;
                    }
                    pendingPicker = null;
                    renderFields();
                });

        viewModel.getAccounts().observe(getViewLifecycleOwner(), value -> {
            accounts.clear();
            accounts.addAll(value);
            if (accounts.isEmpty()) {
                binding.save.setEnabled(false);
                UiUtils.confirm(requireContext(), getString(R.string.empty_accounts_title),
                        getString(R.string.no_accounts_for_transaction),
                        getString(R.string.no_accounts_cta),
                        () -> NavHostFragment.findNavController(this)
                                .navigate(R.id.accountEditor, new Bundle()));
                return;
            }
            binding.save.setEnabled(true);
            boolean exists = false;
            for (Account account : accounts) {
                if (account.id == viewModel.selectedAccountId) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                viewModel.selectedAccountId = accounts.get(0).id;
            }
            renderFields();
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), value -> {
            categories.clear();
            categories.addAll(value);
            boolean exists = false;
            for (Category category : categories) {
                if (category.id == viewModel.selectedCategoryId) {
                    exists = true;
                    break;
                }
            }
            if (!exists && !categories.isEmpty()
                    && viewModel.type != TransactionType.TRANSFER) {
                viewModel.selectedCategoryId = categories.get(0).id;
            }
            renderFields();
        });

        viewModel.getLoaded().observe(getViewLifecycleOwner(), recurring -> {
            if (recurring == null || filled) {
                return;
            }
            filled = true;
            binding.titleInput.setText(recurring.title);
            binding.amountInput.setText(Money.toInput(recurring.amountMinor));
            binding.descriptionInput.setText(recurring.description);
            checkFrequency(recurring.frequency);
            updateTypeUi();
            renderFields();
        });

        viewModel.getDeleted().observe(getViewLifecycleOwner(), success -> {
            if (success == null) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), R.string.deleted, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
            }
        });

        updateTypeUi();
        renderFields();
    }

    private void checkFrequency(RecurrenceFrequency frequency) {
        int id;
        switch (frequency) {
            case DAILY:
                id = R.id.freq_daily;
                break;
            case WEEKLY:
                id = R.id.freq_weekly;
                break;
            case YEARLY:
                id = R.id.freq_yearly;
                break;
            case MONTHLY:
            default:
                id = R.id.freq_monthly;
                break;
        }
        binding.frequencyGroup.check(id);
    }

    private void updateTypeUi() {
        boolean transfer = viewModel.type == TransactionType.TRANSFER;
        binding.categoryField.getRoot().setVisibility(transfer ? View.GONE : View.VISIBLE);
        binding.categoryDivider.setVisibility(transfer ? View.GONE : View.VISIBLE);
        binding.toAccountField.getRoot().setVisibility(transfer ? View.VISIBLE : View.GONE);
        binding.toAccountDivider.setVisibility(transfer ? View.VISIBLE : View.GONE);
        binding.accountField.fieldLabel.setText(transfer
                ? R.string.from_account : R.string.account);
        renderFields();
    }

    private void renderFields() {
        renderCategory();
        renderAccount(binding.accountField, viewModel.selectedAccountId);
        renderAccount(binding.toAccountField, viewModel.selectedToAccountId);
        binding.nextDateField.fieldValue.setText(DateUtils.formatDay(viewModel.nextDate));
        binding.nextDateField.fieldIconContainer.setVisibility(View.GONE);
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

    private void renderAccount(com.financemanager.app.databinding.ViewPickerFieldBinding field,
                               long accountId) {
        Account account = null;
        for (Account item : accounts) {
            if (item.id == accountId) {
                account = item;
                break;
            }
        }
        if (account == null) {
            field.fieldValue.setText(R.string.none);
            field.fieldIconContainer.setVisibility(View.GONE);
        } else {
            field.fieldValue.setText(account.name);
            field.fieldIconContainer.setVisibility(View.VISIBLE);
            field.fieldIcon.setImageResource(Icons.res(account.icon));
            UiUtils.tintCircle(field.fieldIconContainer, field.fieldIcon, account.color);
        }
    }

    private void openCategoryPicker() {
        pendingPicker = "category";
        List<PickerItem> items = new ArrayList<>();
        for (Category category : categories) {
            items.add(new PickerItem(category.id, category.name,
                    com.financemanager.app.ui.common.Labels.transactionType(requireContext(),
                            category.type), category.icon, category.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(), getString(R.string.category),
                items, viewModel.selectedCategoryId, false, null);
    }

    private void openAccountPicker(boolean destination) {
        pendingPicker = destination ? "toAccount" : "account";
        List<PickerItem> items = new ArrayList<>();
        for (Account account : accounts) {
            if (destination && account.id == viewModel.selectedAccountId) {
                continue;
            }
            items.add(new PickerItem(account.id, account.name,
                    com.financemanager.app.ui.common.Labels.accountType(requireContext(),
                            account.type), account.icon, account.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(destination ? R.string.to_account : R.string.account), items,
                destination ? viewModel.selectedToAccountId : viewModel.selectedAccountId,
                false, null);
    }

    private void pickDate() {
        LocalDate initial = DateUtils.toLocalDate(viewModel.nextDate);
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            viewModel.nextDate = DateUtils.startOfDay(LocalDate.of(year, month + 1, day));
            renderFields();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void save() {
        binding.titleLayout.setError(null);
        String title = binding.titleInput.getText() == null ? ""
                : binding.titleInput.getText().toString().trim();
        String amount = binding.amountInput.getText() == null ? ""
                : binding.amountInput.getText().toString();
        boolean valid = true;
        if (title.isEmpty()) {
            binding.titleLayout.setError(getString(R.string.error_title_required));
            valid = false;
        }
        if (Money.parsePositive(amount) == null) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_amount_invalid));
            valid = false;
        }
        if (viewModel.selectedAccountId <= 0) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_account_required));
            valid = false;
        }
        if (viewModel.type != TransactionType.TRANSFER && viewModel.selectedCategoryId <= 0) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_category_required));
            valid = false;
        }
        if (viewModel.type == TransactionType.TRANSFER
                && (viewModel.selectedToAccountId <= 0
                || viewModel.selectedToAccountId == viewModel.selectedAccountId)) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_same_account));
            valid = false;
        }
        if (!valid) {
            return;
        }
        binding.save.setEnabled(false);
        String description = binding.descriptionInput.getText() == null ? ""
                : binding.descriptionInput.getText().toString();
        viewModel.save(title, amount, description, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                if (binding == null) {
                    return;
                }
                binding.save.setEnabled(true);
                Toast.makeText(requireContext(), R.string.recurring_saved,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(RecurringEditorFragment.this).navigateUp();
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
        UiUtils.confirm(requireContext(), getString(R.string.delete_recurring_title),
                getString(R.string.delete_recurring_message,
                        binding.titleInput.getText() == null ? ""
                                : binding.titleInput.getText().toString()),
                getString(R.string.delete), () -> viewModel.delete());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
