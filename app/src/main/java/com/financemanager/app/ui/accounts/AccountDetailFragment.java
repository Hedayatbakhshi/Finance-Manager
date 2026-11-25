package com.financemanager.app.ui.accounts;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.R;
import com.financemanager.app.adapters.TransactionAdapter;
import com.financemanager.app.databinding.FragmentAccountDetailBinding;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.models.TransactionWithDetails;
import com.financemanager.app.ui.common.Labels;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

import java.util.List;

/** Shows an account's balance and complete movement history. */
public class AccountDetailFragment extends Fragment {

    private FragmentAccountDetailBinding binding;
    private AccountDetailViewModel viewModel;
    private TransactionAdapter adapter;
    private long accountId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(AccountDetailViewModel.class);
        accountId = getArguments() == null ? -1 : getArguments().getLong("accountId", -1);
        viewModel.load(accountId);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.edit.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            NavHostFragment.findNavController(this).navigate(R.id.accountEditor, args);
        });
        binding.addTransaction.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("transactionId", -1);
            args.putString("initialType", "EXPENSE");
            args.putLong("initialAccountId", accountId);
            NavHostFragment.findNavController(this).navigate(R.id.transactionEditor, args);
        });
        binding.delete.setOnClickListener(v -> confirmDelete());

        adapter = new TransactionAdapter();
        adapter.setOnTransactionClickListener(item -> {
            Bundle args = new Bundle();
            args.putLong("transactionId", item.transaction.id);
            args.putLong("initialAccountId", -1);
            NavHostFragment.findNavController(this).navigate(R.id.transactionEditor, args);
        });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_nav_transactions);
        binding.empty.emptyTitle.setText(R.string.empty_transactions_title);
        binding.empty.emptyMessage.setText(R.string.empty_transactions_message);

        viewModel.getAccount().observe(getViewLifecycleOwner(), this::renderAccount);
        viewModel.getTransactions().observe(getViewLifecycleOwner(), this::renderTransactions);
        viewModel.getDeleted().observe(getViewLifecycleOwner(), success -> {
            if (success == null) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), R.string.account_deleted,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), R.string.cannot_delete_last_account,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderAccount(AccountWithBalance item) {
        if (item == null) {
            return;
        }
        binding.name.setText(item.account.name);
        binding.type.setText(Labels.accountType(requireContext(), item.account.type)
                + " · " + item.account.currency);
        binding.balance.setText(Money.format(item.balanceMinor, item.account.currency));
        binding.balance.setTextColor(item.balanceMinor < 0
                ? UiUtils.color(requireContext(), R.color.expense)
                : UiUtils.themeColor(binding.getRoot(),
                com.google.android.material.R.attr.colorOnSurface));
        binding.icon.setImageResource(Icons.res(item.account.icon));
        UiUtils.tintIconTile(binding.iconContainer, binding.icon, item.account.color);
    }

    private void renderTransactions(List<TransactionWithDetails> items) {
        adapter.submitList(items);
        boolean empty = items.isEmpty();
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete() {
        binding.name.getText();
        UiUtils.confirm(requireContext(), getString(R.string.delete_account_title),
                getString(R.string.delete_account_message_empty,
                        binding.name.getText().toString()),
                getString(R.string.delete), () -> viewModel.delete(accountId));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (accountId > 0) {
            viewModel.reload(accountId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
