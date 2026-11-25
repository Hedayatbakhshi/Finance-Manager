package com.financemanager.app.ui.savings;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.adapters.ColorOptionAdapter;
import com.financemanager.app.adapters.IconOptionAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.SavingsGoal;
import com.financemanager.app.databinding.FragmentSavingsEditorBinding;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.financemanager.app.utils.DateUtils;
import com.financemanager.app.utils.Icons;
import com.financemanager.app.utils.Money;

import java.time.LocalDate;

/** Creates or edits a savings goal. */
public class SavingsEditorFragment extends Fragment {

    private FragmentSavingsEditorBinding binding;
    private SavingsEditorViewModel viewModel;
    private IconOptionAdapter iconAdapter;
    private ColorOptionAdapter colorAdapter;
    private boolean filled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSavingsEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(SavingsEditorViewModel.class);
        long goalId = getArguments() == null ? -1 : getArguments().getLong("goalId", -1);
        if (goalId > 0) {
            viewModel.load(goalId);
        }

        binding.toolbar.setTitle(viewModel.isEditing()
                ? R.string.edit_goal : R.string.new_goal);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.currencySymbol.setText(Money.symbol(
                ((FinanceManagerApp) requireActivity().getApplication())
                        .container().settings().getCurrency()));

        binding.iconList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        iconAdapter = new IconOptionAdapter(Icons.goalIcons(), viewModel.icon, viewModel.color,
                key -> viewModel.icon = key);
        binding.iconList.setAdapter(iconAdapter);

        binding.colorList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        colorAdapter = new ColorOptionAdapter(Icons.colors(), viewModel.color, hex -> {
            viewModel.color = hex;
            iconAdapter.setSelected(viewModel.icon, hex);
        });
        binding.colorList.setAdapter(colorAdapter);

        binding.targetDateField.fieldLabel.setText(R.string.target_date);
        binding.targetDateField.fieldRoot.setOnClickListener(v -> pickDate());
        renderDate();

        binding.delete.setVisibility(viewModel.isEditing() ? View.VISIBLE : View.GONE);
        binding.delete.setOnClickListener(v -> confirmDelete());
        binding.save.setOnClickListener(v -> save());

        viewModel.getLoaded().observe(getViewLifecycleOwner(), goal -> {
            if (goal == null || filled) {
                return;
            }
            filled = true;
            binding.nameInput.setText(goal.name);
            binding.targetInput.setText(Money.toInput(goal.targetMinor));
            binding.savedInput.setText(Money.toInput(goal.currentMinor));
            binding.descriptionInput.setText(goal.description);
            iconAdapter.setSelected(goal.icon, goal.color);
            colorAdapter.setSelected(goal.color);
            renderDate();
        });

        viewModel.getDeleted().observe(getViewLifecycleOwner(), success -> {
            if (success == null) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), R.string.goal_deleted,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDate() {
        if (viewModel.targetDate > 0) {
            binding.targetDateField.fieldValue.setText(DateUtils.formatDay(viewModel.targetDate));
        } else {
            binding.targetDateField.fieldValue.setText(R.string.pick_date);
        }
        binding.targetDateField.fieldIconContainer.setVisibility(View.GONE);
    }

    private void pickDate() {
        LocalDate initial = viewModel.targetDate > 0
                ? DateUtils.toLocalDate(viewModel.targetDate)
                : LocalDate.now().plusMonths(3);
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            viewModel.targetDate = DateUtils.startOfDay(LocalDate.of(year, month + 1, day));
            renderDate();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void save() {
        binding.nameLayout.setError(null);
        binding.targetError.setVisibility(View.GONE);
        String name = binding.nameInput.getText() == null ? ""
                : binding.nameInput.getText().toString().trim();
        String target = binding.targetInput.getText() == null ? ""
                : binding.targetInput.getText().toString();
        String saved = binding.savedInput.getText() == null ? ""
                : binding.savedInput.getText().toString();
        boolean valid = true;
        if (name.isEmpty()) {
            binding.nameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        }
        if (Money.parsePositive(target) == null) {
            binding.targetError.setText(R.string.error_target_invalid);
            binding.targetError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!valid) {
            return;
        }
        binding.save.setEnabled(false);
        String description = binding.descriptionInput.getText() == null ? ""
                : binding.descriptionInput.getText().toString();
        viewModel.save(name, target, saved, description, new FinanceRepository.Callback<Long>() {
            @Override
            public void onSuccess(Long value) {
                if (binding == null) {
                    return;
                }
                binding.save.setEnabled(true);
                Toast.makeText(requireContext(), R.string.goal_saved,
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(SavingsEditorFragment.this).navigateUp();
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
        UiUtils.confirm(requireContext(), getString(R.string.delete_goal_title),
                getString(R.string.delete_goal_message, name),
                getString(R.string.delete), () -> viewModel.delete());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
