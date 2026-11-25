package com.financemanager.app.ui.household;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.R;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.HouseholdCategory;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.databinding.FragmentHouseholdExpenseEditorBinding;
import com.financemanager.app.ui.common.PickerBottomSheet;
import com.financemanager.app.ui.common.PickerItem;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Add/edit form that writes a normal transaction tagged with household metadata. */
public class HouseholdExpenseEditorFragment extends Fragment {

    private static final int PICK_CATEGORY = 1;
    private static final int PICK_ACCOUNT = 2;
    private static final int PICK_MEMBER = 3;

    private FragmentHouseholdExpenseEditorBinding binding;
    private HouseholdExpenseEditorViewModel viewModel;
    private long householdId = -1;
    private long expenseId = -1;
    private int picker = 0;
    private List<HouseholdCategory> categoryLinks = Collections.emptyList();
    private List<Account> accounts = Collections.emptyList();
    private List<HouseholdMember> members = Collections.emptyList();
    private List<Category> globalCategories = Collections.emptyList();
    private String householdCurrency = "";
    private boolean userPickedAccount;

    private final ActivityResultLauncher<String[]> pickReceipt = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    attachReceipt(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdExpenseEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        householdId = args == null ? -1 : args.getLong("householdId", -1);
        expenseId = args == null ? -1 : args.getLong("expenseId", -1);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdExpenseEditorViewModel.class);
        viewModel.init(householdId);
        if (expenseId > 0) {
            viewModel.load(expenseId);
            binding.toolbar.setTitle(R.string.household_expense_edit);
            binding.delete.setVisibility(View.VISIBLE);
        }

        binding.toolbar.setNavigationOnClickListener(v -> back());
        setupPickerFields();
        binding.categoryField.fieldRoot.setOnClickListener(v -> pickCategory());
        binding.accountField.fieldRoot.setOnClickListener(v -> pickAccount());
        binding.dateField.fieldRoot.setOnClickListener(v -> pickDate());
        binding.memberField.fieldRoot.setOnClickListener(v -> pickMember());
        binding.attachReceipt.setOnClickListener(v -> pickReceipt.launch(new String[]{"image/*", "application/pdf"}));
        binding.viewReceipt.setOnClickListener(v -> viewReceipt());
        binding.save.setOnClickListener(v -> save());
        binding.delete.setOnClickListener(v -> confirmDelete());
        binding.makeRecurring.setVisibility(expenseId > 0 ? View.VISIBLE : View.GONE);
        binding.makeRecurring.setOnClickListener(v -> openRecurring());

        getParentFragmentManager().setFragmentResultListener(PickerBottomSheet.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    long selected = bundle.getLong(PickerBottomSheet.KEY_SELECTED_ID, -1);
                    boolean cleared = bundle.getBoolean(PickerBottomSheet.KEY_CLEARED, false);
                    applyPicker(selected, cleared);
                });

        viewModel.getCategories().observe(getViewLifecycleOwner(), value -> {
            categoryLinks = value == null ? Collections.emptyList() : value;
            if (viewModel.selectedCategoryId <= 0 && !categoryLinks.isEmpty()) {
                viewModel.selectedCategoryId = categoryLinks.get(0).id;
            }
            renderCategory();
        });
        viewModel.getAccounts().observe(getViewLifecycleOwner(), value -> {
            accounts = value == null ? Collections.emptyList() : value;
            ensureDefaultAccount();
            renderAccount();
        });
        viewModel.getHouseholdCurrency().observe(getViewLifecycleOwner(), value -> {
            householdCurrency = value == null ? "" : value;
            ensureDefaultAccount();
            renderAccount();
        });
        viewModel.getMembers().observe(getViewLifecycleOwner(), value -> {
            members = value == null ? Collections.emptyList() : value;
        });
        viewModel.getGlobalCategories().observe(getViewLifecycleOwner(), value -> {
            globalCategories = value == null ? Collections.emptyList() : value;
            renderCategory();
        });
        viewModel.getLoaded().observe(getViewLifecycleOwner(), item -> {
            userPickedAccount = true;
            binding.titleInput.setText(item.transaction.transaction.title);
            binding.amountInput.setText(Money.toInput(item.transaction.transaction.amountMinor));
            binding.descriptionInput.setText(item.transaction.transaction.description);
            binding.notesInput.setText(item.transaction.transaction.notes);
            renderCategory();
            renderAccount();
            renderDate();
            renderMember();
            renderReceipt();
        });
        renderDate();
    }

    private void setupPickerFields() {
        int[][] fields = {
                {R.id.category_field, R.drawable.ic_action_category},
                {R.id.account_field, R.drawable.ic_action_wallet},
                {R.id.date_field, R.drawable.ic_action_calendar},
                {R.id.member_field, R.drawable.ic_action_person}
        };
        for (int[] field : fields) {
            View root = binding.getRoot().findViewById(field[0]);
            ImageView icon = root.findViewById(R.id.field_icon);
            root.findViewById(R.id.field_icon_container).setVisibility(View.VISIBLE);
            icon.setImageResource(field[1]);
        }
    }

    private void pickCategory() {
        picker = PICK_CATEGORY;
        List<PickerItem> items = new ArrayList<>();
        for (HouseholdCategory link : categoryLinks) {
            PickerItem item = new PickerItem(link.id, categoryName(link.categoryId), "",
                    "cat_other", "#5B7FA6");
            items.add(item);
            item.title = categoryName(link.categoryId);
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(R.string.household_expense_category), items,
                viewModel.selectedCategoryId, false, null);
    }

    private String categoryName(long categoryId) {
        for (Category category : globalCategories) {
            if (category.id == categoryId) {
                return category.name;
            }
        }
        return getString(R.string.category);
    }

    private void pickAccount() {
        picker = PICK_ACCOUNT;
        List<PickerItem> items = new ArrayList<>();
        for (Account account : accounts) {
            items.add(new PickerItem(account.id, account.name, account.currency,
                    account.icon, account.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(R.string.household_expense_account), items,
                viewModel.selectedAccountId, false, null);
    }

    private void pickMember() {
        picker = PICK_MEMBER;
        List<PickerItem> items = new ArrayList<>();
        for (HouseholdMember member : members) {
            items.add(new PickerItem(member.id, member.name, "", "action_person", "#5B7FA6"));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(R.string.household_all_members), items,
                viewModel.selectedMemberId, true, getString(R.string.household_all_members));
    }

    private void pickDate() {
        LocalDate date = DateUtils.toLocalDate(viewModel.selectedDate);
        new DatePickerDialog(requireContext(), (pickerView, year, month, day) -> {
            LocalDate chosen = LocalDate.of(year, month + 1, day);
            viewModel.selectedDate = DateUtils.startOfDay(chosen);
            renderDate();
        }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()).show();
    }

    private void ensureDefaultAccount() {
        if (userPickedAccount || accounts.isEmpty()) {
            return;
        }
        Account preferred = null;
        Account selected = null;
        for (Account account : accounts) {
            if (householdCurrency != null && !householdCurrency.isEmpty()
                    && householdCurrency.equalsIgnoreCase(account.currency)) {
                preferred = account;
            }
            if (account.id == viewModel.selectedAccountId) {
                selected = account;
            }
        }
        if (preferred != null && (selected == null
                || !householdCurrency.equalsIgnoreCase(selected.currency))) {
            viewModel.selectedAccountId = preferred.id;
            return;
        }
        if (viewModel.selectedAccountId <= 0) {
            viewModel.selectedAccountId = accounts.get(0).id;
        }
    }

    private void applyPicker(long selected, boolean cleared) {
        if (picker == PICK_CATEGORY && selected > 0) {
            viewModel.selectedCategoryId = selected;
            renderCategory();
        } else if (picker == PICK_ACCOUNT && selected > 0) {
            viewModel.selectedAccountId = selected;
            userPickedAccount = true;
            renderAccount();
        } else if (picker == PICK_MEMBER) {
            viewModel.selectedMemberId = cleared || selected <= 0 ? -1 : selected;
            renderMember();
        }
        picker = 0;
    }

    private void renderCategory() {
        binding.categoryField.fieldValue.setText(
                viewModel.selectedCategoryId > 0 ? categoryLabel(viewModel.selectedCategoryId)
                        : getString(R.string.household_expense_category));
        binding.categoryField.fieldLabel.setText(R.string.household_expense_category);
        binding.categoryField.fieldIcon.setImageResource(R.drawable.ic_action_category);
    }

    private String categoryLabel(long linkId) {
        for (HouseholdCategory link : categoryLinks) {
            if (link.id == linkId) {
                return categoryName(link.categoryId);
            }
        }
        return getString(R.string.category);
    }

    private void renderAccount() {
        String label = getString(R.string.household_expense_account);
        Account selected = null;
        for (Account account : accounts) {
            if (account.id == viewModel.selectedAccountId) {
                selected = account;
                label = account.name + " · " + account.currency;
                break;
            }
        }
        binding.accountField.fieldValue.setText(label);
        binding.accountField.fieldLabel.setText(R.string.household_expense_account);
        binding.accountField.fieldIcon.setImageResource(R.drawable.ic_action_wallet);

        boolean mismatch = selected != null && householdCurrency != null
                && !householdCurrency.isEmpty()
                && !householdCurrency.equalsIgnoreCase(selected.currency);
        binding.accountCurrencyWarning.setVisibility(mismatch ? View.VISIBLE : View.GONE);
        if (mismatch) {
            binding.accountCurrencyWarning.setText(getString(
                    R.string.household_account_currency_warning,
                    selected.currency, householdCurrency));
        }
    }

    private void renderDate() {
        binding.dateField.fieldValue.setText(DateUtils.formatDay(viewModel.selectedDate));
        binding.dateField.fieldLabel.setText(R.string.household_expense_date);
        binding.dateField.fieldIcon.setImageResource(R.drawable.ic_action_calendar);
    }

    private void renderMember() {
        String label = getString(R.string.household_all_members);
        for (HouseholdMember member : members) {
            if (member.id == viewModel.selectedMemberId) {
                label = member.name;
                break;
            }
        }
        binding.memberField.fieldValue.setText(label);
        binding.memberField.fieldLabel.setText(R.string.household_member);
        binding.memberField.fieldIcon.setImageResource(R.drawable.ic_action_person);
    }

    private void renderReceipt() {
        boolean attached = viewModel.receiptPath != null && !viewModel.receiptPath.isEmpty();
        binding.viewReceipt.setVisibility(attached ? View.VISIBLE : View.GONE);
        binding.receiptName.setVisibility(attached ? View.VISIBLE : View.GONE);
        if (attached) {
            binding.receiptName.setText(new File(viewModel.receiptPath).getName());
        }
    }

    private void attachReceipt(Uri uri) {
        try {
            File dir = new File(requireContext().getFilesDir(), "receipts");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Could not create receipt folder");
            }
            String name = System.currentTimeMillis() + ".dat";
            File file = new File(dir, name);
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(file)) {
                if (in == null) {
                    throw new IllegalStateException("Could not read receipt");
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            viewModel.receiptPath = file.getAbsolutePath();
            renderReceipt();
        } catch (Exception e) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.household_receipt_invalid));
        }
    }

    private void viewReceipt() {
        if (viewModel.receiptPath == null) {
            return;
        }
        File file = new File(viewModel.receiptPath);
        Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.household_view_receipt)));
    }

    private void save() {
        if (viewModel.selectedAccountId <= 0) {
            UiUtils.snackbar(binding.getRoot(),
                    getString(R.string.household_expense_account));
            return;
        }
        String title = binding.titleInput.getText() == null
                ? "" : binding.titleInput.getText().toString();
        String amount = binding.amountInput.getText() == null
                ? "" : binding.amountInput.getText().toString();
        String description = binding.descriptionInput.getText() == null
                ? "" : binding.descriptionInput.getText().toString();
        String notes = binding.notesInput.getText() == null
                ? "" : binding.notesInput.getText().toString();
        viewModel.save(title, amount, description, notes, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                UiUtils.snackbar(binding.getRoot(),
                        getString(R.string.household_expense_saved));
                back();
            }

            @Override
            public void onError(String message) {
                UiUtils.snackbar(binding.getRoot(), message);
            }
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_delete_expense_title)
                .setMessage(R.string.household_delete_expense_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.delete(new FinanceRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                UiUtils.snackbar(binding.getRoot(),
                                        getString(R.string.household_expense_deleted));
                                back();
                            }

                            @Override
                            public void onError(String message) {
                                UiUtils.snackbar(binding.getRoot(), message);
                            }
                        }))
                .show();
    }

    private void openRecurring() {
        Bundle args = new Bundle();
        args.putLong("recurringId", -1);
        args.putLong("householdId", householdId);
        args.putLong("householdCategoryId", viewModel.selectedCategoryId);
        NavHostFragment.findNavController(this).navigate(R.id.recurringEditor, args);
    }

    private void back() {
        NavHostFragment.findNavController(this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
