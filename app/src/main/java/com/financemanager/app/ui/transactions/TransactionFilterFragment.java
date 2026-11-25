package com.financemanager.app.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.R;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.databinding.FragmentTransactionFilterBinding;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.ReportPeriod;
import com.financemanager.app.models.TransactionFilter;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.ui.common.Labels;
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

/** Builds a {@link TransactionFilter} from type, date, category, account and amount rules. */
public class TransactionFilterFragment extends Fragment {

    public static final String ARG_FILTER = "filter";
    public static final String RESULT_KEY = "transaction_filter_result";
    public static final String RESULT_FILTER = "filter";

    private FragmentTransactionFilterBinding binding;
    private TransactionFilterViewModel viewModel;
    private TransactionFilter working;
    private List<Category> categories = new ArrayList<>();
    private List<AccountWithBalance> accounts = new ArrayList<>();
    private LocalDate customFrom = LocalDate.now().minusMonths(1);
    private LocalDate customTo = LocalDate.now();
    private String pendingPicker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Object raw = getArguments() == null ? null
                : getArguments().getSerializable(ARG_FILTER);
        working = raw instanceof TransactionFilter
                ? ((TransactionFilter) raw).copy() : new TransactionFilter();
        customFrom = DateUtils.toLocalDate(working.fromDate);
        customTo = DateUtils.toLocalDate(working.toDate);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(TransactionFilterViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.categoryField.fieldLabel.setText(R.string.category);
        binding.accountField.fieldLabel.setText(R.string.account);
        binding.categoryField.fieldRoot.setOnClickListener(v -> openCategoryPicker());
        binding.accountField.fieldRoot.setOnClickListener(v -> openAccountPicker());

        binding.typeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.type_income) {
                working.type = TransactionType.INCOME;
            } else if (checkedId == R.id.type_expense) {
                working.type = TransactionType.EXPENSE;
            } else if (checkedId == R.id.type_transfer) {
                working.type = TransactionType.TRANSFER;
            } else {
                working.type = null;
            }
        });

        binding.dateGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                working.period = ReportPeriod.ALL_TIME;
                binding.customDates.setVisibility(View.GONE);
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.date_week) {
                working.period = ReportPeriod.THIS_WEEK;
            } else if (id == R.id.date_month) {
                working.period = ReportPeriod.THIS_MONTH;
            } else if (id == R.id.date_last_month) {
                working.period = ReportPeriod.LAST_MONTH;
            } else if (id == R.id.date_three_months) {
                working.period = ReportPeriod.LAST_3_MONTHS;
            } else if (id == R.id.date_year) {
                working.period = ReportPeriod.THIS_YEAR;
            } else if (id == R.id.date_custom) {
                working.period = ReportPeriod.CUSTOM;
            } else {
                working.period = ReportPeriod.ALL_TIME;
            }
            binding.customDates.setVisibility(working.period == ReportPeriod.CUSTOM
                    ? View.VISIBLE : View.GONE);
        });

        binding.fromDate.setOnClickListener(v -> pickDate(true));
        binding.toDate.setOnClickListener(v -> pickDate(false));

        binding.sortGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                working.sort = TransactionFilter.SORT_NEWEST;
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.sort_oldest) {
                working.sort = TransactionFilter.SORT_OLDEST;
            } else if (id == R.id.sort_highest) {
                working.sort = TransactionFilter.SORT_HIGHEST;
            } else if (id == R.id.sort_lowest) {
                working.sort = TransactionFilter.SORT_LOWEST;
            } else {
                working.sort = TransactionFilter.SORT_NEWEST;
            }
        });

        binding.reset.setOnClickListener(v -> reset());
        binding.apply.setOnClickListener(v -> apply());

        getParentFragmentManager().setFragmentResultListener(PickerBottomSheet.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    long id = bundle.getLong(PickerBottomSheet.KEY_SELECTED_ID, -1);
                    boolean cleared = bundle.getBoolean(PickerBottomSheet.KEY_CLEARED, false);
                    if ("category".equals(pendingPicker)) {
                        working.categoryId = cleared || id <= 0 ? null : id;
                        renderCategoryField();
                    } else if ("account".equals(pendingPicker)) {
                        working.accountId = cleared || id <= 0 ? null : id;
                        renderAccountField();
                    }
                    pendingPicker = null;
                });

        viewModel.getCategories().observe(getViewLifecycleOwner(), value -> {
            categories = value;
            renderCategoryField();
        });
        viewModel.getAccounts().observe(getViewLifecycleOwner(), value -> {
            accounts = value;
            renderAccountField();
        });

        restoreSelection();
    }

    private void restoreSelection() {
        if (working.type == TransactionType.INCOME) {
            binding.typeGroup.check(R.id.type_income);
        } else if (working.type == TransactionType.EXPENSE) {
            binding.typeGroup.check(R.id.type_expense);
        } else if (working.type == TransactionType.TRANSFER) {
            binding.typeGroup.check(R.id.type_transfer);
        } else {
            binding.typeGroup.check(R.id.type_all);
        }

        switch (working.period) {
            case THIS_WEEK:
                binding.dateGroup.check(R.id.date_week);
                break;
            case THIS_MONTH:
                binding.dateGroup.check(R.id.date_month);
                break;
            case LAST_MONTH:
                binding.dateGroup.check(R.id.date_last_month);
                break;
            case LAST_3_MONTHS:
                binding.dateGroup.check(R.id.date_three_months);
                break;
            case THIS_YEAR:
                binding.dateGroup.check(R.id.date_year);
                break;
            case CUSTOM:
                binding.dateGroup.check(R.id.date_custom);
                break;
            case ALL_TIME:
            default:
                binding.dateGroup.check(R.id.date_any);
                break;
        }
        binding.customDates.setVisibility(working.period == ReportPeriod.CUSTOM
                ? View.VISIBLE : View.GONE);

        switch (working.sort) {
            case TransactionFilter.SORT_OLDEST:
                binding.sortGroup.check(R.id.sort_oldest);
                break;
            case TransactionFilter.SORT_HIGHEST:
                binding.sortGroup.check(R.id.sort_highest);
                break;
            case TransactionFilter.SORT_LOWEST:
                binding.sortGroup.check(R.id.sort_lowest);
                break;
            default:
                binding.sortGroup.check(R.id.sort_newest);
                break;
        }

        if (working.minAmount != null) {
            binding.minAmount.setText(Money.formatPlain(working.minAmount));
        }
        if (working.maxAmount != null) {
            binding.maxAmount.setText(Money.formatPlain(working.maxAmount));
        }
        updateDateButtons();
        renderCategoryField();
        renderAccountField();
    }

    private void updateDateButtons() {
        binding.fromDate.setText(getString(R.string.from_date) + ": "
                + DateUtils.formatShortDay(DateUtils.startOfDay(customFrom)));
        binding.toDate.setText(getString(R.string.to_date) + ": "
                + DateUtils.formatShortDay(DateUtils.startOfDay(customTo)));
    }

    private void renderCategoryField() {
        Category selected = null;
        for (Category category : categories) {
            if (working.categoryId != null && category.id == working.categoryId) {
                selected = category;
                break;
            }
        }
        if (selected == null) {
            binding.categoryField.fieldValue.setText(R.string.all_categories);
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
        AccountWithBalance selected = null;
        for (AccountWithBalance account : accounts) {
            if (working.accountId != null && account.account.id == working.accountId) {
                selected = account;
                break;
            }
        }
        if (selected == null) {
            binding.accountField.fieldValue.setText(R.string.all_accounts);
            binding.accountField.fieldIconContainer.setVisibility(View.GONE);
        } else {
            binding.accountField.fieldValue.setText(selected.account.name);
            binding.accountField.fieldIconContainer.setVisibility(View.VISIBLE);
            binding.accountField.fieldIcon.setImageResource(Icons.res(selected.account.icon));
            UiUtils.tintCircle(binding.accountField.fieldIconContainer,
                    binding.accountField.fieldIcon, selected.account.color);
        }
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
                items, working.categoryId == null ? -1 : working.categoryId, true,
                getString(R.string.all_categories));
    }

    private void openAccountPicker() {
        pendingPicker = "account";
        List<PickerItem> items = new ArrayList<>();
        for (AccountWithBalance account : accounts) {
            items.add(new PickerItem(account.account.id, account.account.name,
                    Money.format(account.balanceMinor, account.account.currency),
                    account.account.icon, account.account.color));
        }
        PickerBottomSheet.show(getParentFragmentManager(), getString(R.string.account),
                items, working.accountId == null ? -1 : working.accountId, true,
                getString(R.string.all_accounts));
    }

    private void pickDate(boolean isFrom) {
        LocalDate initial = isFrom ? customFrom : customTo;
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            LocalDate picked = LocalDate.of(year, month + 1, day);
            if (isFrom) {
                customFrom = picked;
                if (customFrom.isAfter(customTo)) {
                    customTo = customFrom;
                }
            } else {
                customTo = picked;
                if (customTo.isBefore(customFrom)) {
                    customFrom = customTo;
                }
            }
            updateDateButtons();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void apply() {
        Long min = Money.parse(binding.minAmount.getText() == null ? ""
                : binding.minAmount.getText().toString());
        Long max = Money.parse(binding.maxAmount.getText() == null ? ""
                : binding.maxAmount.getText().toString());
        if (min != null && max != null && min > max) {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.error_amount_invalid));
            return;
        }
        working.minAmount = min;
        working.maxAmount = max;

        if (working.period == ReportPeriod.CUSTOM) {
            working.fromDate = DateUtils.startOfDay(customFrom);
            working.toDate = DateUtils.endOfDay(customTo);
        } else {
            long[] range = DateUtils.range(working.period, LocalDate.now());
            working.fromDate = range[0];
            working.toDate = range[1];
        }

        Bundle result = new Bundle();
        result.putSerializable(RESULT_FILTER, working.copy());
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        NavHostFragment.findNavController(this).navigateUp();
    }

    private void reset() {
        working = new TransactionFilter();
        customFrom = LocalDate.now().minusMonths(1);
        customTo = LocalDate.now();
        binding.minAmount.setText("");
        binding.maxAmount.setText("");
        restoreSelection();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
