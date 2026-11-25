package com.financemanager.app.ui.transactions;

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
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.databinding.FragmentTransactionEditorBinding;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.ui.common.Labels;
import com.financemanager.app.ui.common.PickerBottomSheet;
import com.financemanager.app.ui.common.PickerItem;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Create or edit a transaction, including transfers between accounts. */
public class TransactionEditorFragment extends Fragment {

    private FragmentTransactionEditorBinding binding;
    private TransactionEditorViewModel viewModel;
    private SettingsRepository settings;
    private final List<Account> accounts = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private String pendingPicker;
    private boolean fieldsFilled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings();
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(TransactionEditorViewModel.class);

        Bundle args = getArguments();
        long transactionId = args == null ? -1 : args.getLong("transactionId", -1);
        String initialType = args == null ? "EXPENSE" : args.getString("initialType", "EXPENSE");
        long initialAccountId = args == null ? -1 : args.getLong("initialAccountId", -1);

        if (transactionId > 0) {
            viewModel.load(transactionId);
        } else if (viewModel.type == TransactionType.EXPENSE && initialType != null) {
            viewModel.type = TransactionType.fromKey(initialType);
            if (viewModel.type != TransactionType.TRANSFER) {
                viewModel.setType(viewModel.type);
            }
        }
        if (viewModel.selectedAccountId <= 0 && initialAccountId > 0) {
            viewModel.selectedAccountId = initialAccountId;
        }

        binding.toolbar.setTitle(viewModel.isEditing()
                ? R.string.edit_transaction : R.string.new_transaction);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.delete.setVisibility(viewModel.isEditing() ? View.VISIBLE : View.GONE);
        binding.duplicate.setVisibility(viewModel.isEditing() ? View.VISIBLE : View.GONE);
        binding.currencySymbol.setText(Money.symbol(settings.getCurrency()));

        binding.categoryField.fieldLabel.setText(R.string.category);
        binding.accountField.fieldLabel.setText(R.string.from_account);
        binding.toAccountField.fieldLabel.setText(R.string.to_account);
        binding.dateField.fieldLabel.setText(R.string.date);
        binding.categoryField.fieldRoot.setOnClickListener(v -> openCategoryPicker());
        binding.accountField.fieldRoot.setOnClickListener(v -> openAccountPicker(false));
        binding.toAccountField.fieldRoot.setOnClickListener(v -> openAccountPicker(true));
        binding.dateField.fieldRoot.setOnClickListener(v -> pickDate());

        int checkedId = viewModel.type == TransactionType.INCOME ? R.id.type_income
                : viewModel.type == TransactionType.TRANSFER ? R.id.type_transfer
                : R.id.type_expense;
        binding.typeGroup.check(checkedId);
        binding.typeGroup.addOnButtonCheckedListener((group, id, isChecked) -> {
            if (!isChecked) {
                return;
            }
            TransactionType type = id == R.id.type_income ? TransactionType.INCOME
                    : id == R.id.type_transfer ? TransactionType.TRANSFER
                    : TransactionType.EXPENSE;
            viewModel.setType(type);
            updateTypeUi();
        });

        binding.save.setOnClickListener(v -> save());
        binding.duplicate.setOnClickListener(v -> confirmDuplicate());
        binding.delete.setOnClickListener(v -> confirmDelete());
        updateTypeUi();
        renderDate();
        renderAccountField();
        renderToAccountField();
        renderCategoryField();

        getParentFragmentManager().setFragmentResultListener(PickerBottomSheet.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    long id = bundle.getLong(PickerBottomSheet.KEY_SELECTED_ID, -1);
                    boolean cleared = bundle.getBoolean(PickerBottomSheet.KEY_CLEARED, false);
                    if ("category".equals(pendingPicker)) {
                        if (!cleared) {
                            viewModel.selectedCategoryId = id;
                        }
                        renderCategoryField();
                    } else if ("account".equals(pendingPicker)) {
                        if (!cleared) {
                            viewModel.selectedAccountId = id;
                            renderAccountField();
                            renderCurrency();
                        }
                    } else if ("toAccount".equals(pendingPicker)) {
                        if (!cleared) {
                            viewModel.selectedToAccountId = id;
                        }
                        renderToAccountField();
                    }
                    pendingPicker = null;
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
            boolean accountMissing = true;
            for (Account account : accounts) {
                if (account.id == viewModel.selectedAccountId) {
                    accountMissing = false;
                    break;
                }
            }
            if (accountMissing) {
                viewModel.selectedAccountId = accounts.get(0).id;
            }
            if (viewModel.type == TransactionType.TRANSFER
                    && (viewModel.selectedToAccountId <= 0
                    || viewModel.selectedToAccountId == viewModel.selectedAccountId)) {
                viewModel.selectedToAccountId = pickOtherAccount(viewModel.selectedAccountId);
            }
            renderAccountField();
            renderToAccountField();
            renderCurrency();
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
            renderCategoryField();
        });

        viewModel.getLoaded().observe(getViewLifecycleOwner(), entity -> {
            if (entity == null || fieldsFilled) {
                return;
            }
            fieldsFilled = true;
            fillFromEntity(entity);
        });

        viewModel.getDeleted().observe(getViewLifecycleOwner(), success -> {
            if (success == null) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), R.string.deleted, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            }
        });

        if (!viewModel.isEditing()) {
            binding.amountInput.requestFocus();
        }
    }

    private void fillFromEntity(TransactionEntity entity) {
        if (entity.title != null) {
            binding.titleInput.setText(entity.title);
        }
        binding.amountInput.setText(Money.toInput(entity.amountMinor));
        binding.amountInput.setSelection(binding.amountInput.getText().length());
        binding.descriptionInput.setText(entity.description);
        binding.notesInput.setText(entity.notes);
        int checkedId = entity.type == TransactionType.INCOME ? R.id.type_income
                : entity.type == TransactionType.TRANSFER ? R.id.type_transfer
                : R.id.type_expense;
        binding.typeGroup.check(checkedId);
        updateTypeUi();
        renderDate();
        renderCategoryField();
        renderAccountField();
        renderToAccountField();
        renderCurrency();
    }

    private void updateTypeUi() {
        boolean transfer = viewModel.type == TransactionType.TRANSFER;
        binding.categoryField.getRoot().setVisibility(transfer ? View.GONE : View.VISIBLE);
        binding.categoryDivider.setVisibility(transfer ? View.GONE : View.VISIBLE);
        binding.toAccountField.getRoot().setVisibility(transfer ? View.VISIBLE : View.GONE);
        binding.toAccountDivider.setVisibility(transfer ? View.VISIBLE : View.GONE);
        binding.accountField.fieldLabel.setText(transfer
                ? R.string.from_account : R.string.account);
        if (transfer) {
            if (viewModel.selectedToAccountId <= 0
                    || viewModel.selectedToAccountId == viewModel.selectedAccountId) {
                viewModel.selectedToAccountId = pickOtherAccount(viewModel.selectedAccountId);
            }
            renderToAccountField();
        } else if (viewModel.selectedCategoryId <= 0 && !categories.isEmpty()) {
            viewModel.selectedCategoryId = categories.get(0).id;
        }
        renderCategoryField();
    }

    private long pickOtherAccount(long excluded) {
        for (Account account : accounts) {
            if (account.id != excluded) {
                return account.id;
            }
        }
        return -1;
    }

    private void renderCategoryField() {
        Category selected = null;
        for (Category category : categories) {
            if (category.id == viewModel.selectedCategoryId) {
                selected = category;
                break;
            }
        }
        if (selected == null) {
            binding.categoryField.fieldValue.setText(viewModel.type == TransactionType.TRANSFER
                    ? getString(R.string.none) : getString(R.string.category));
            binding.categoryField.fieldIconContainer.setVisibility(View.GONE);
        } else {
            binding.categoryField.fieldValue.setText(selected.name);
            binding.categoryField.fieldIconContainer.setVisibility(View.VISIBLE);
            binding.categoryField.fieldIcon.setImageResource(Icons.res(selected.icon));
            UiUtils.tintCircle(binding.categoryField.fieldIconContainer,
                    binding.categoryField.fieldIcon, selected.color);
        }
    }

    private void renderAccountField() {
        Account account = findAccount(viewModel.selectedAccountId);
        if (account == null) {
            binding.accountField.fieldValue.setText(R.string.account);
            binding.accountField.fieldIconContainer.setVisibility(View.GONE);
        } else {
            binding.accountField.fieldValue.setText(account.name + " · " + account.currency);
            binding.accountField.fieldIconContainer.setVisibility(View.VISIBLE);
            binding.accountField.fieldIcon.setImageResource(Icons.res(account.icon));
            UiUtils.tintCircle(binding.accountField.fieldIconContainer,
                    binding.accountField.fieldIcon, account.color);
        }
    }

    private void renderToAccountField() {
        Account account = findAccount(viewModel.selectedToAccountId);
        if (account == null) {
            binding.toAccountField.fieldValue.setText(R.string.none);
            binding.toAccountField.fieldIconContainer.setVisibility(View.GONE);
        } else {
            binding.toAccountField.fieldValue.setText(account.name + " · " + account.currency);
            binding.toAccountField.fieldIconContainer.setVisibility(View.VISIBLE);
            binding.toAccountField.fieldIcon.setImageResource(Icons.res(account.icon));
            UiUtils.tintCircle(binding.toAccountField.fieldIconContainer,
                    binding.toAccountField.fieldIcon, account.color);
        }
    }

    private void renderCurrency() {
        Account account = findAccount(viewModel.selectedAccountId);
        binding.currencySymbol.setText(Money.symbol(account == null
                ? settings.getCurrency() : account.currency));
    }

    private Account findAccount(long id) {
        for (Account account : accounts) {
            if (account.id == id) {
                return account;
            }
        }
        return null;
    }

    private void renderDate() {
        binding.dateField.fieldValue.setText(DateUtils.formatDay(viewModel.selectedDate));
        binding.dateField.fieldIconContainer.setVisibility(View.GONE);
    }

    private void openCategoryPicker() {
        pendingPicker = "category";
        List<PickerItem> items = new ArrayList<>();
        for (Category category : categories) {
            items.add(new PickerItem(category.id, category.name,
                    Labels.transactionType(requireContext(), category.type),
                    category.icon, category.color));
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
                    Labels.accountType(requireContext(), account.type) + " · "
                            + account.currency,
                    account.icon, account.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(destination ? R.string.to_account : R.string.account),
                items, destination ? viewModel.selectedToAccountId : viewModel.selectedAccountId,
                false, null);
    }

    private void pickDate() {
        LocalDate date = DateUtils.toLocalDate(viewModel.selectedDate);
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            viewModel.selectedDate = DateUtils.startOfDay(LocalDate.of(year, month + 1, day));
            renderDate();
        }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()).show();
    }

    private void save() {
        if (validateInputs()) {
            persist(false);
        }
    }

    private void confirmDuplicate() {
        if (!viewModel.isEditing() || !validateInputs()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.duplicate_transaction_title)
                .setMessage(R.string.duplicate_transaction_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.duplicate_transaction,
                        (dialog, which) -> persist(true))
                .show();
    }

    private boolean validateInputs() {
        binding.titleLayout.setError(null);
        binding.amountError.setVisibility(View.GONE);

        String title = binding.titleInput.getText() == null ? ""
                : binding.titleInput.getText().toString().trim();
        String amountText = binding.amountInput.getText() == null ? ""
                : binding.amountInput.getText().toString();
        boolean valid = true;
        if (title.isEmpty()) {
            binding.titleLayout.setError(getString(R.string.error_title_required));
            valid = false;
        }
        if (Money.parsePositive(amountText) == null) {
            binding.amountError.setText(R.string.error_amount_invalid);
            binding.amountError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (accounts.isEmpty()) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.no_accounts_for_transaction));
            return false;
        }
        if (viewModel.type != TransactionType.TRANSFER && viewModel.selectedCategoryId <= 0) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_category_required));
            valid = false;
        }
        if (viewModel.selectedAccountId <= 0) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_account_required));
            valid = false;
        }
        if (viewModel.type == TransactionType.TRANSFER) {
            if (viewModel.selectedToAccountId <= 0
                    || viewModel.selectedToAccountId == viewModel.selectedAccountId) {
                UiUtils.snackbar(binding.getRoot(), getString(R.string.error_same_account));
                valid = false;
            }
        }
        return valid;
    }

    private void persist(boolean copy) {
        binding.save.setEnabled(false);
        binding.duplicate.setEnabled(false);
        String title = binding.titleInput.getText() == null ? ""
                : binding.titleInput.getText().toString().trim();
        String amountText = binding.amountInput.getText() == null ? ""
                : binding.amountInput.getText().toString();
        String description = binding.descriptionInput.getText() == null ? ""
                : binding.descriptionInput.getText().toString();
        String notes = binding.notesInput.getText() == null ? ""
                : binding.notesInput.getText().toString();
        FinanceRepository.Callback<Long> callback = new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                if (binding == null) {
                    return;
                }
                binding.save.setEnabled(true);
                binding.duplicate.setEnabled(true);
                Toast.makeText(requireContext(), R.string.transaction_saved,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(TransactionEditorFragment.this).navigateUp();
            }

            @Override
            public void onError(String message) {
                if (binding == null) {
                    return;
                }
                binding.save.setEnabled(true);
                binding.duplicate.setEnabled(true);
                UiUtils.snackbar(binding.getRoot(), message);
            }
        };
        if (copy) {
            viewModel.saveCopy(title, amountText, description, notes, callback);
        } else {
            viewModel.save(title, amountText, description, notes, callback);
        }
    }

    private void confirmDelete() {
        if (!viewModel.isEditing()) {
            return;
        }
        String title = binding.titleInput.getText() == null ? ""
                : binding.titleInput.getText().toString();
        UiUtils.confirm(requireContext(), getString(R.string.delete_transaction_title),
                getString(R.string.delete_transaction_message, title),
                getString(R.string.delete),
                () -> viewModel.delete(viewModel.transactionId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
