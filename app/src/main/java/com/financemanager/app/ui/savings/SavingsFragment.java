package com.financemanager.app.ui.savings;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.SavingsGoalAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.databinding.FragmentSavingsBinding;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.Money;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Savings goals with quick contributions and withdrawals. */
public class SavingsFragment extends Fragment {

    private FragmentSavingsBinding binding;
    private SavingsViewModel viewModel;
    private SavingsGoalAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSavingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(SavingsViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.fab.setOnClickListener(v -> openEditor(-1));

        adapter = new SavingsGoalAdapter(new SavingsGoalAdapter.Listener() {
            @Override
            public void onAdd(SavingsGoal goal) {
                showAmountDialog(goal, true);
            }

            @Override
            public void onWithdraw(SavingsGoal goal) {
                showAmountDialog(goal, false);
            }

            @Override
            public void onMenu(SavingsGoal goal, View anchor) {
                showMenu(goal, anchor);
            }
        }, viewModel.getCurrency());
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_goal_target);
        binding.empty.emptyTitle.setText(R.string.empty_goals_title);
        binding.empty.emptyMessage.setText(R.string.empty_goals_message);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.no_goals_cta);
        binding.empty.emptyAction.setOnClickListener(v -> openEditor(-1));

        viewModel.getGoals().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<SavingsGoal> goals) {
        adapter.submitList(goals);
        long saved = 0;
        for (SavingsGoal goal : goals) {
            saved += goal.currentMinor;
        }
        binding.totalSaved.setText(Money.format(saved, viewModel.getCurrency()));
        binding.goalCount.setText(String.valueOf(goals.size()));
        boolean empty = goals.isEmpty();
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showAmountDialog(SavingsGoal goal, boolean add) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(R.string.amount_hint);
        int padding = UiUtils.dp(requireContext(), 20);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);

        String title = getString(add ? R.string.add_money : R.string.withdraw_money)
                + " · " + goal.name;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(getString(R.string.goal_withdraw_hint,
                        Money.format(goal.currentMinor, viewModel.getCurrency())))
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(add ? R.string.add : R.string.withdraw_money,
                        (dialog, which) -> {
                            Long amount = Money.parsePositive(input.getText().toString());
                            if (amount == null) {
                                Toast.makeText(requireContext(), R.string.invalid_amount,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            long delta = add ? amount : -amount;
                            viewModel.adjust(goal.id, delta,
                                    new FinanceRepository.Callback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean value) {
                                            Toast.makeText(requireContext(),
                                                    R.string.goal_added, Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(String message) {
                                            Toast.makeText(requireContext(), message,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        })
                .show();
    }

    private void showMenu(SavingsGoal goal, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(R.string.edit);
        menu.getMenu().add(R.string.delete);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals(getString(R.string.edit))) {
                openEditor(goal.id);
            } else {
                UiUtils.confirm(requireContext(), getString(R.string.delete_goal_title),
                        getString(R.string.delete_goal_message, goal.name),
                        getString(R.string.delete), () -> viewModel.delete(goal.id,
                                new FinanceRepository.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                        Toast.makeText(requireContext(),
                                                R.string.goal_deleted,
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        if (binding != null) {
                                            UiUtils.snackbar(binding.getRoot(), message);
                                        }
                                    }
                                }));
            }
            return true;
        });
        menu.show();
    }

    private void openEditor(long goalId) {
        Bundle args = new Bundle();
        args.putLong("goalId", goalId);
        NavHostFragment.findNavController(this).navigate(R.id.savingsEditor, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
