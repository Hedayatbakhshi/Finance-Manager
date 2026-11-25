package com.financemanager.app.ui.transactions;

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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.TransactionAdapter;
import com.financemanager.app.databinding.FragmentTransactionsBinding;
import com.financemanager.app.models.TransactionFilter;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.models.TransactionWithDetails;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.Money;

import java.util.List;

/** Searchable, filterable transaction history with delete support. */
public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private SettingsRepository settings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings();
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(TransactionsViewModel.class);

        adapter = new TransactionAdapter();
        adapter.setOnTransactionClickListener(this::openTransaction);
        adapter.setOnTransactionLongClickListener(this::confirmDelete);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_nav_transactions);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.no_transactions_cta);
        binding.empty.emptyAction.setOnClickListener(v -> openNewTransaction());

        binding.fab.setOnClickListener(v -> openNewTransaction());
        binding.filterButton.setOnClickListener(v -> openFilters());
        binding.filterStatus.setOnClickListener(v -> openFilters());

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

        getParentFragmentManager().setFragmentResultListener(
                TransactionFilterFragment.RESULT_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                    TransactionFilter filter = (TransactionFilter) bundle.getSerializable(
                            TransactionFilterFragment.RESULT_FILTER);
                    if (filter != null) {
                        viewModel.applyFilter(filter);
                    }
                });

        viewModel.getTransactions().observe(getViewLifecycleOwner(), this::render);
        viewModel.getFilter().observe(getViewLifecycleOwner(), this::renderFilterStatus);
    }

    private void render(List<TransactionWithDetails> items) {
        adapter.submitList(items);
        binding.summary.setText(getString(R.string.transactions_count, items.size()));
        renderTotals(items);
        TransactionFilter filter = viewModel.getFilter().getValue();
        boolean filtered = filter != null && filter.hasActiveFilters();
        if (items.isEmpty()) {
            binding.list.setVisibility(View.GONE);
            binding.empty.getRoot().setVisibility(View.VISIBLE);
            binding.empty.emptyTitle.setText(filtered
                    ? R.string.empty_transactions_filtered_title
                    : R.string.empty_transactions_title);
            binding.empty.emptyMessage.setText(filtered
                    ? R.string.empty_transactions_filtered_message
                    : R.string.empty_transactions_message);
            binding.empty.emptyAction.setVisibility(filtered ? View.GONE : View.VISIBLE);
        } else {
            binding.list.setVisibility(View.VISIBLE);
            binding.empty.getRoot().setVisibility(View.GONE);
        }
    }

    private void renderTotals(List<TransactionWithDetails> items) {
        if (items.isEmpty()) {
            binding.totalsCard.setVisibility(View.GONE);
            binding.totalsNote.setVisibility(View.GONE);
            return;
        }
        String currency = settings.getCurrency();
        long income = 0;
        long expenses = 0;
        boolean mixedCurrencies = false;
        for (TransactionWithDetails item : items) {
            if (item.transaction.isTransfer()) {
                continue;
            }
            if (item.accountCurrency == null
                    || !item.accountCurrency.equalsIgnoreCase(currency)) {
                mixedCurrencies = true;
                continue;
            }
            if (item.transaction.type == TransactionType.INCOME) {
                income += item.transaction.amountMinor;
            } else if (item.transaction.type == TransactionType.EXPENSE) {
                expenses += item.transaction.amountMinor;
            }
        }
        long net = income - expenses;
        binding.totalsCard.setVisibility(View.VISIBLE);
        binding.totalIncome.setText(Money.format(income, currency));
        binding.totalExpenses.setText(Money.format(expenses, currency));
        binding.totalNet.setText(Money.format(net, currency));
        binding.totalNet.setTextColor(net < 0
                ? UiUtils.color(requireContext(), R.color.expense)
                : net > 0
                ? UiUtils.color(requireContext(), R.color.success)
                : com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),
                com.google.android.material.R.attr.colorOnSurface));
        binding.totalsNote.setVisibility(mixedCurrencies ? View.VISIBLE : View.GONE);
    }

    private void renderFilterStatus(TransactionFilter filter) {
        if (filter == null || !filter.hasActiveFilters()) {
            binding.filterStatus.setVisibility(View.GONE);
            return;
        }
        int count = 0;
        if (filter.type != null) count++;
        if (filter.categoryId != null) count++;
        if (filter.accountId != null) count++;
        if (filter.minAmount != null || filter.maxAmount != null) count++;
        if (filter.period != com.financemanager.app.models.ReportPeriod.ALL_TIME) count++;
        binding.filterStatus.setVisibility(View.VISIBLE);
        binding.filterStatus.setText(getString(R.string.filter) + (count > 0 ? " · " + count : ""));
    }

    private void openFilters() {
        TransactionFilter filter = viewModel.getFilter().getValue();
        Bundle args = new Bundle();
        args.putSerializable(TransactionFilterFragment.ARG_FILTER, filter);
        NavHostFragment.findNavController(this).navigate(R.id.transactionFilter, args);
    }

    private void openTransaction(TransactionWithDetails item) {
        Bundle args = new Bundle();
        args.putLong("transactionId", item.transaction.id);
        args.putLong("initialAccountId", -1);
        navigate(args);
    }

    private void openNewTransaction() {
        Bundle args = new Bundle();
        args.putLong("transactionId", -1);
        args.putString("initialType", "EXPENSE");
        args.putLong("initialAccountId", -1);
        navigate(args);
    }

    private void navigate(Bundle args) {
        NavHostFragment.findNavController(this).navigate(R.id.transactionEditor, args);
    }

    private void confirmDelete(TransactionWithDetails item) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_transaction_title)
                .setMessage(getString(R.string.delete_transaction_message,
                        item.transaction.title))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.deleteTransaction(item.transaction.id,
                                new com.financemanager.app.data.FinanceRepository.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                        UiUtils.snackbar(binding.getRoot(),
                                                getString(R.string.deleted));
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
