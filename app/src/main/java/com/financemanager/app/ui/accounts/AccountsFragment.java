package com.financemanager.app.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.AccountAdapter;
import com.financemanager.app.databinding.FragmentAccountsBinding;
import com.financemanager.app.models.AccountWithBalance;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.Money;

import java.util.List;

/** Lists every account and its calculated balance. */
public class AccountsFragment extends Fragment {

    private FragmentAccountsBinding binding;
    private AccountAdapter adapter;
    private String currency;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AccountsViewModel viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(AccountsViewModel.class);
        currency = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings().getCurrency();

        adapter = new AccountAdapter(this::openAccount);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.add.setOnClickListener(v -> openEditor(-1));
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_action_wallet);
        binding.empty.emptyTitle.setText(R.string.empty_accounts_title);
        binding.empty.emptyMessage.setText(R.string.empty_accounts_message);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.no_accounts_cta);
        binding.empty.emptyAction.setOnClickListener(v -> openEditor(-1));

        viewModel.getAccounts().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<AccountWithBalance> accounts) {
        adapter.submitList(accounts);
        long total = 0;
        boolean hasForeign = false;
        for (AccountWithBalance account : accounts) {
            if (currency.equalsIgnoreCase(account.account.currency)) {
                total += account.balanceMinor;
            } else {
                hasForeign = true;
            }
        }
        binding.totalBalance.setText(Money.format(total, currency));
        binding.currencyNote.setVisibility(hasForeign ? View.VISIBLE : View.GONE);
        boolean empty = accounts.isEmpty();
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void openAccount(AccountWithBalance account) {
        Bundle args = new Bundle();
        args.putLong("accountId", account.account.id);
        NavHostFragment.findNavController(this).navigate(R.id.accountDetail, args);
    }

    private void openEditor(long accountId) {
        Bundle args = new Bundle();
        args.putLong("accountId", accountId);
        NavHostFragment.findNavController(this).navigate(R.id.accountEditor, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
