package com.financemanager.app.ui.recurring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.financemanager.app.R;
import com.financemanager.app.adapters.RecurringAdapter;
import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.RecurringTransaction;
import com.financemanager.app.databinding.FragmentRecurringBinding;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.ui.common.ViewModelFactory;

import java.util.List;

/** Manage recurring income, expenses and transfers. */
public class RecurringFragment extends Fragment {

    private FragmentRecurringBinding binding;
    private RecurringViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecurringBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(RecurringViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.fab.setOnClickListener(v -> openEditor(-1));

        RecurringAdapter adapter = new RecurringAdapter(new RecurringAdapter.Listener() {
            @Override
            public void onToggle(RecurringTransaction recurring, boolean active) {
                viewModel.setActive(recurring.id, active);
            }

            @Override
            public void onOpen(RecurringTransaction recurring) {
                openEditor(recurring.id);
            }
        }, viewModel.getCurrency());
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_action_repeat);
        binding.empty.emptyTitle.setText(R.string.empty_recurring_title);
        binding.empty.emptyMessage.setText(R.string.empty_recurring_message);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.new_recurring);
        binding.empty.emptyAction.setOnClickListener(v -> openEditor(-1));

        viewModel.getRecurring().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            boolean empty = items.isEmpty();
            binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void openEditor(long id) {
        Bundle args = new Bundle();
        args.putLong("recurringId", id);
        NavHostFragment.findNavController(this).navigate(R.id.recurringEditor, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
