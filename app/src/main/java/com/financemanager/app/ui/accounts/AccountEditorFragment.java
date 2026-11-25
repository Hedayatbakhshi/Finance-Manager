package com.financemanager.app.ui.accounts;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.ColorOptionAdapter;
import com.financemanager.app.adapters.IconOptionAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.databinding.FragmentAccountEditorBinding;
import com.financemanager.app.models.AccountType;
import com.financemanager.app.ui.common.CurrencyPicker;
import com.financemanager.app.ui.common.Labels;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

import java.util.ArrayList;
import java.util.List;

/** Create or edit an account, including icon, color, currency and opening balance. */
public class AccountEditorFragment extends Fragment {

    private FragmentAccountEditorBinding binding;
    private AccountEditorViewModel viewModel;
    private IconOptionAdapter iconAdapter;
    private ColorOptionAdapter colorAdapter;
    private boolean filled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(AccountEditorViewModel.class);
        long accountId = getArguments() == null ? -1 : getArguments().getLong("accountId", -1);
        if (accountId > 0) {
            viewModel.load(accountId);
        }

        binding.toolbar.setTitle(viewModel.isEditing()
                ? R.string.edit_account : R.string.new_account);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.delete.setVisibility(viewModel.isEditing() ? View.VISIBLE : View.GONE);
        binding.save.setOnClickListener(v -> save());
        binding.delete.setOnClickListener(v -> confirmDelete());

        binding.iconList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        iconAdapter = new IconOptionAdapter(Icons.accountIcons(), viewModel.icon,
                viewModel.color, icon -> {
            viewModel.icon = icon;
            renderPreview();
        });
        binding.iconList.setAdapter(iconAdapter);

        binding.colorList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        colorAdapter = new ColorOptionAdapter(Icons.colors(), viewModel.color, color -> {
            viewModel.color = color;
            iconAdapter.setSelected(viewModel.icon, color);
            renderPreview();
        });
        binding.colorList.setAdapter(colorAdapter);

        setUpTypeChips();
        setupNameWatcher();
        binding.currencyField.fieldLabel.setText(R.string.currency);
        binding.currencyField.fieldRoot.setOnClickListener(v -> CurrencyPicker.show(
                requireContext(), viewModel.currency, code -> {
                    viewModel.currency = code;
                    renderCurrency();
                }));

        if (!viewModel.isEditing()) {
            setCheckedType(viewModel.type);
        }
        renderAll();

        viewModel.getLoaded().observe(getViewLifecycleOwner(), account -> {
            if (account == null || filled) {
                return;
            }
            filled = true;
            binding.nameInput.setText(account.name);
            binding.balanceInput.setText(Money.toInput(account.initialBalanceMinor));
            setCheckedType(account.type);
            iconAdapter.setSelected(account.icon, account.color);
            colorAdapter.setSelected(account.color);
            renderAll();
        });

        viewModel.getDeleted().observe(getViewLifecycleOwner(), success -> {
            if (success == null) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), R.string.account_deleted,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUpTypeChips() {
        binding.typeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.type_bank) {
                viewModel.type = AccountType.BANK;
            } else if (id == R.id.type_savings) {
                viewModel.type = AccountType.SAVINGS;
            } else if (id == R.id.type_credit) {
                viewModel.type = AccountType.CREDIT;
            } else if (id == R.id.type_wallet) {
                viewModel.type = AccountType.WALLET;
            } else {
                viewModel.type = AccountType.CASH;
            }
            if (!filled) {
                viewModel.icon = Labels.accountIconKey(viewModel.type);
                iconAdapter.setSelected(viewModel.icon, viewModel.color);
            }
            renderPreview();
        });
    }

    private void setCheckedType(AccountType type) {
        int id;
        switch (type) {
            case BANK:
                id = R.id.type_bank;
                break;
            case SAVINGS:
                id = R.id.type_savings;
                break;
            case CREDIT:
                id = R.id.type_credit;
                break;
            case WALLET:
                id = R.id.type_wallet;
                break;
            case CASH:
            default:
                id = R.id.type_cash;
                break;
        }
        binding.typeGroup.check(id);
    }

    private void setupNameWatcher() {
        binding.nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                renderPreview();
            }
        });
    }

    private void renderAll() {
        renderCurrency();
        renderPreview();
    }

    private void renderCurrency() {
        binding.currencySymbol.setText(Money.symbol(viewModel.currency));
        binding.currencyField.fieldValue.setText(viewModel.currency);
        binding.currencyField.fieldIconContainer.setVisibility(View.GONE);
    }

    private void renderPreview() {
        if (binding == null) {
            return;
        }
        UiUtils.tintIconTile(binding.previewContainer, binding.previewIcon, viewModel.color);
        binding.previewIcon.setImageResource(Icons.res(viewModel.icon));
        String name = binding.nameInput.getText() == null ? ""
                : binding.nameInput.getText().toString().trim();
        binding.previewName.setText(name.isEmpty()
                ? getString(R.string.account_name) : name);
        if (binding.previewName.getLayoutParams() != null) {
            binding.previewName.setVisibility(View.VISIBLE);
        }
    }

    private void save() {
        binding.nameLayout.setError(null);
        String name = binding.nameInput.getText() == null ? ""
                : binding.nameInput.getText().toString().trim();
        String balance = binding.balanceInput.getText() == null ? ""
                : binding.balanceInput.getText().toString();
        if (name.isEmpty()) {
            binding.nameLayout.setError(getString(R.string.error_name_required));
            return;
        }
        if (Money.parse(balance) == null) {
            binding.nameLayout.setError(null);
            UiUtils.snackbar(binding.getRoot(), getString(R.string.invalid_amount));
            return;
        }
        binding.save.setEnabled(false);
        viewModel.save(name, balance, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                if (binding == null) {
                    return;
                }
                binding.save.setEnabled(true);
                Toast.makeText(requireContext(), R.string.account_saved,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(AccountEditorFragment.this).navigateUp();
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
        viewModel.countTransactions(new FinanceRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (binding == null) {
                    return;
                }
                int transactions = count == null ? 0 : count;
                String message = transactions > 0
                        ? getString(R.string.delete_account_message, name, transactions)
                        : getString(R.string.delete_account_message_empty, name);
                UiUtils.confirm(requireContext(), getString(R.string.delete_account_title),
                        message, getString(R.string.delete), () -> viewModel.delete());
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    UiUtils.snackbar(binding.getRoot(), message);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
