package com.financemanager.app.ui.household;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.R;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.data.entity.HouseholdMember;
import com.financemanager.app.databinding.FragmentHouseholdEditorBinding;
import com.financemanager.app.ui.common.CurrencyPicker;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Money;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Creates/edits a household and manages its local member list. */
public class HouseholdEditorFragment extends Fragment {

    private FragmentHouseholdEditorBinding binding;
    private HouseholdEditorViewModel viewModel;
    private String currency;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdEditorViewModel.class);
        currency = viewModel.currency;
        long householdId = getArguments() == null ? -1
                : getArguments().getLong("householdId", -1);
        viewModel.load(householdId);

        binding.toolbar.setNavigationOnClickListener(v -> back());
        binding.currencyField.fieldIconContainer.setVisibility(View.VISIBLE);
        binding.currencyField.fieldIcon.setImageResource(R.drawable.ic_action_swap);
        binding.currencyField.fieldLabel.setText(R.string.household_currency);
        binding.currencyField.fieldValue.setText(currency);
        binding.currencyField.fieldRoot.setOnClickListener(v -> CurrencyPicker.show(
                requireContext(), currency, code -> {
                    currency = code;
                    binding.currencyField.fieldValue.setText(code);
                }));

        binding.addMember.setOnClickListener(v -> showMemberDialog());
        binding.save.setOnClickListener(v -> save());
        binding.delete.setOnClickListener(v -> confirmDelete());
        binding.delete.setVisibility(householdId > 0 ? View.VISIBLE : View.GONE);

        viewModel.getHousehold().observe(getViewLifecycleOwner(), this::renderHousehold);
        viewModel.getMembers().observe(getViewLifecycleOwner(), this::renderMembers);
    }

    private void renderHousehold(Household household) {
        if (household == null) {
            return;
        }
        binding.nameInput.setText(household.name);
        binding.descriptionInput.setText(household.description);
        binding.budgetInput.setText(household.monthlyBudgetMinor == 0 ? ""
                : Money.toInput(household.monthlyBudgetMinor));
        currency = household.currency;
        binding.currencyField.fieldLabel.setText(R.string.household_currency);
        binding.currencyField.fieldValue.setText(currency);
        binding.currencyField.fieldIcon.setImageResource(R.drawable.ic_action_swap);
        binding.toolbar.setTitle(getString(R.string.household_created,
                DateUtils.formatDay(household.createdAt)));
    }

    private void renderMembers(List<HouseholdMember> members) {
        LinearLayout container = binding.membersContainer;
        container.removeAllViews();
        boolean empty = members == null || members.isEmpty();
        binding.noMembers.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        for (HouseholdMember member : members) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView name = new TextView(requireContext());
            name.setText(member.name);
            name.setTextSize(14);
            name.setTextColor(UiUtils.color(requireContext(), R.color.text_primary));
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView remove = new TextView(requireContext());
            remove.setText(R.string.delete);
            remove.setTextSize(12);
            remove.setTextColor(UiUtils.color(requireContext(), R.color.expense));
            remove.setPadding(UiUtils.dp(requireContext(), 8), 0, 0, 0);
            remove.setOnClickListener(v -> viewModel.deleteMember(member.id,
                    new FinanceRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                        }

                        @Override
                        public void onError(String message) {
                            UiUtils.snackbar(binding.getRoot(), message);
                        }
                    }));
            row.addView(name);
            row.addView(remove);
            container.addView(row);
        }
    }

    private void showMemberDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.household_member);
        int padding = UiUtils.dp(requireContext(), 20);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_add_member)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }
                    viewModel.addMember(name, new FinanceRepository.Callback<Long>() {
                        @Override
                        public void onSuccess(Long value) {
                        }

                        @Override
                        public void onError(String message) {
                            UiUtils.snackbar(binding.getRoot(), message);
                        }
                    });
                })
                .show();
    }

    private void save() {
        String name = binding.nameInput.getText() == null
                ? "" : binding.nameInput.getText().toString();
        String description = binding.descriptionInput.getText() == null
                ? "" : binding.descriptionInput.getText().toString();
        String budget = binding.budgetInput.getText() == null
                ? "" : binding.budgetInput.getText().toString();
        viewModel.save(name, description, budget, currency, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                UiUtils.snackbar(binding.getRoot(), getString(R.string.household_saved));
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
                .setTitle(R.string.household_delete_title)
                .setMessage(R.string.household_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.delete(new FinanceRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                UiUtils.snackbar(binding.getRoot(),
                                        getString(R.string.household_deleted));
                                back();
                            }

                            @Override
                            public void onError(String message) {
                                UiUtils.snackbar(binding.getRoot(), message);
                            }
                        }))
                .show();
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
