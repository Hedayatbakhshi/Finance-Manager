package com.financemanager.app.ui.household;

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

import com.financemanager.app.R;
import com.financemanager.app.adapters.HouseholdAdapter;
import com.financemanager.app.data.entity.Household;
import com.financemanager.app.databinding.FragmentHouseholdsBinding;
import com.financemanager.app.ui.common.ViewModelFactory;

import java.util.List;

/** Entry point for local household profiles. */
public class HouseholdsFragment extends Fragment {

    private FragmentHouseholdsBinding binding;
    private HouseholdsViewModel viewModel;
    private HouseholdAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHouseholdsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(HouseholdsViewModel.class);
        adapter = new HouseholdAdapter();
        adapter.setListener(this::openHousehold);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_action_person);
        binding.empty.emptyTitle.setText(R.string.household_no_households_title);
        binding.empty.emptyMessage.setText(R.string.household_no_households_message);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.household_new);
        binding.empty.emptyAction.setOnClickListener(v -> openEditor(-1));

        binding.fab.setOnClickListener(v -> openEditor(-1));
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        viewModel.getHouseholds().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<Household> households) {
        boolean empty = households == null || households.isEmpty();
        adapter.submitList(empty ? java.util.Collections.emptyList() : households);
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void openHousehold(Household household) {
        Bundle args = new Bundle();
        args.putLong("householdId", household.id);
        NavHostFragment.findNavController(this).navigate(R.id.householdOverview, args);
    }

    private void openEditor(long householdId) {
        Bundle args = new Bundle();
        args.putLong("householdId", householdId);
        NavHostFragment.findNavController(this).navigate(R.id.householdEditor, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
