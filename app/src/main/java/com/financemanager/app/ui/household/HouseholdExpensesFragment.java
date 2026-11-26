package com.financemanager.app.ui.household;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.R;
import com.financemanager.app.adapters.HouseholdExpenseAdapter;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.databinding.FragmentHouseholdExpensesBinding;
import com.financemanager.app.models.HouseholdExpense;
import com.financemanager.app.models.HouseholdFilter;
import com.financemanager.app.models.HouseholdTotals;
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

/** Searchable, filterable household expense history. */
public class HouseholdExpensesFragment extends Fragment {

    private static final int PICK_CATEGORY = 1;
    private static final int PICK_ACCOUNT = 2;
    private static final int PICK_MEMBER = 3;
    private static final int PICK_RANGE = 4;
    private static final int PICK_SORT = 5;

    private FragmentHouseholdExpensesBinding binding;
    private HouseholdExpensesViewModel viewModel;
    private HouseholdExpenseAdapter adapter;
    private long householdId = -1;
    private int picker = 0;
    private List<Category> categories = Collections.emptyList();
    private List<Account> accounts = Collections.emptyList();
    private List<HouseholdMember> members = Collections.emptyList();
    private List<HouseholdExpense> currentExpenses = Collections.emptyList();
    private String currency = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdExpensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        householdId = getArguments() == null ? -1 : getArguments().getLong("householdId", -1);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdExpensesViewModel.class);
        viewModel.setHouseholdId(householdId);

        currency = viewModel.currency();
        adapter = new HouseholdExpenseAdapter();
        adapter.setCurrency(currency);
        adapter.setListener(new HouseholdExpenseAdapter.Listener() {
            @Override
            public void onExpense(HouseholdExpense expense) {
                openExpense(expense);
            }

            @Override
            public void onLongExpense(HouseholdExpense expense) {
                confirmDelete(expense);
            }
        });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_nav_transactions);
        binding.empty.emptyTitle.setText(R.string.household_empty_expenses_title);
        binding.empty.emptyMessage.setText(R.string.household_empty_expenses_message);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.household_expense_new);
        binding.empty.emptyAction.setOnClickListener(v -> openExpense(null));
        binding.fab.setOnClickListener(v -> openExpense(null));

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearch(s == null ? "" : s.toString());
            }
        });
        binding.filterButton.setOnClickListener(v -> showFilterMenu());
        binding.filterStatus.setOnClickListener(v -> showFilterMenu());

        getParentFragmentManager().setFragmentResultListener(PickerBottomSheet.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    long selected = bundle.getLong(PickerBottomSheet.KEY_SELECTED_ID, -1);
                    boolean cleared = bundle.getBoolean(PickerBottomSheet.KEY_CLEARED, false);
                    applyPickerResult(selected, cleared);
                });

        viewModel.getCategories().observe(getViewLifecycleOwner(), value -> {
            categories = value == null ? Collections.emptyList() : value;
        });
        viewModel.getAccounts().observe(getViewLifecycleOwner(), value -> {
            accounts = value == null ? Collections.emptyList() : value;
        });
        viewModel.getMembers().observe(getViewLifecycleOwner(), value -> {
            members = value == null ? Collections.emptyList() : value;
        });
        viewModel.getCurrency().observe(getViewLifecycleOwner(), value -> {
            if (value == null || value.equals(currency)) {
                return;
            }
            currency = value;
            adapter.setCurrency(currency);
            renderSummary();
        });
        viewModel.getExpenses().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<HouseholdExpense> items) {
        currentExpenses = items == null ? Collections.emptyList() : items;
        boolean empty = currentExpenses.isEmpty();
        adapter.submitList(currentExpenses);
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);

        renderSummary();
        HouseholdFilter filter = viewModel.getFilter();
        binding.filterStatus.setVisibility(
                filter == null || !filter.hasActiveFilters() ? View.GONE : View.VISIBLE);
    }

    private void renderSummary() {
        long total = HouseholdTotals.totalFor(currentExpenses, currency);
        String summary = getString(R.string.transactions_count, currentExpenses.size())
                + " · " + Money.format(total, currency);
        if (HouseholdTotals.hasForeignCurrency(currentExpenses, currency)) {
            summary += " · " + getString(R.string.household_currency_note);
        }
        binding.summary.setText(summary);
    }

    private void showFilterMenu() {
        String[] options = {
                getString(R.string.household_all_categories),
                getString(R.string.household_all_accounts),
                getString(R.string.household_all_members),
                getString(R.string.filter_date),
                getString(R.string.household_sort_newest),
                getString(R.string.clear_filters)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_filter)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickCategories();
                    } else if (which == 1) {
                        pickAccounts();
                    } else if (which == 2) {
                        pickMembers();
                    } else if (which == 3) {
                        pickRange();
                    } else if (which == 4) {
                        pickSort();
                    } else {
                        viewModel.clearFilters(householdId);
                        binding.searchInput.setText("");
                    }
                })
                .show();
    }

    private void pickCategories() {
        picker = PICK_CATEGORY;
        List<PickerItem> items = new ArrayList<>();
        for (Category category : categories) {
            items.add(new PickerItem(category.id, category.name, "",
                    category.icon, category.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(R.string.household_all_categories), items,
                viewModel.getFilter().categoryId == null ? -1 : viewModel.getFilter().categoryId,
                true, getString(R.string.household_all_categories));
    }

    private void pickAccounts() {
        picker = PICK_ACCOUNT;
        List<PickerItem> items = new ArrayList<>();
        for (Account account : accounts) {
            items.add(new PickerItem(account.id, account.name, account.currency,
                    account.icon, account.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(R.string.household_all_accounts), items,
                viewModel.getFilter().accountId == null ? -1 : viewModel.getFilter().accountId,
                true, getString(R.string.household_all_accounts));
    }

    private void pickMembers() {
        picker = PICK_MEMBER;
        List<PickerItem> items = new ArrayList<>();
        for (HouseholdMember member : members) {
            items.add(new PickerItem(member.id, member.name, "", "action_person", "#5A87C0"));
        }
        PickerBottomSheet.show(getParentFragmentManager(),
                getString(R.string.household_all_members), items,
                viewModel.getFilter().memberId == null ? -1 : viewModel.getFilter().memberId,
                true, getString(R.string.household_all_members));
    }

    private void pickRange() {
        picker = PICK_RANGE;
        List<PickerItem> items = new ArrayList<>();
        items.add(new PickerItem(0, getString(R.string.household_all_time), "", "action_calendar", "#5A87C0"));
        items.add(new PickerItem(1, getString(R.string.household_this_month), "",
                "action_calendar", "#5A87C0"));
        items.add(new PickerItem(2, getString(R.string.household_last_month), "",
                "action_calendar", "#5A87C0"));
        PickerBottomSheet.show(getParentFragmentManager(), getString(R.string.filter_date),
                items, -1, false, null);
    }

    private void pickSort() {
        picker = PICK_SORT;
        String[] labels = {
                getString(R.string.household_sort_newest),
                getString(R.string.household_sort_oldest),
                getString(R.string.household_sort_highest),
                getString(R.string.household_sort_lowest)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.filter)
                .setSingleChoiceItems(labels, viewModel.getFilter().sort, (dialog, which) -> {
                    viewModel.setSort(which);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyPickerResult(long selected, boolean cleared) {
        if (picker == PICK_CATEGORY) {
            viewModel.setCategory(cleared || selected <= 0 ? null : selected);
        } else if (picker == PICK_ACCOUNT) {
            viewModel.setAccount(cleared || selected <= 0 ? null : selected);
        } else if (picker == PICK_MEMBER) {
            viewModel.setMember(cleared || selected <= 0 ? null : selected);
        } else if (picker == PICK_RANGE) {
            LocalDate today = LocalDate.now();
            if (selected == 1) {
                viewModel.setRange(DateUtils.startOfMonth(today), DateUtils.endOfMonth(today));
            } else if (selected == 2) {
                LocalDate last = today.minusMonths(1);
                viewModel.setRange(DateUtils.startOfMonth(last), DateUtils.endOfMonth(last));
            } else {
                viewModel.setRange(1, Long.MAX_VALUE);
            }
        }
        picker = 0;
    }

    private void openExpense(HouseholdExpense expense) {
        Bundle args = new Bundle();
        args.putLong("householdId", householdId);
        args.putLong("expenseId", expense == null ? -1 : expense.transaction.transaction.id);
        NavHostFragment.findNavController(this).navigate(R.id.householdExpenseEditor, args);
    }

    private void confirmDelete(HouseholdExpense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_delete_expense_title)
                .setMessage(R.string.household_delete_expense_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.delete(expense.transaction.transaction.id,
                                new com.financemanager.app.data.FinanceRepository.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                        UiUtils.snackbar(binding.getRoot(),
                                                getString(R.string.household_expense_deleted));
                                    }

                                    @Override
                                    public void onError(String message) {
                                        UiUtils.snackbar(binding.getRoot(), message);
                                    }
                                }))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
