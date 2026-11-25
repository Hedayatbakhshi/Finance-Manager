package com.financemanager.app.ui.categories;

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
import com.financemanager.app.adapters.CategoryAdapter;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.databinding.FragmentCategoriesBinding;
import com.financemanager.app.models.TransactionType;
import com.financemanager.app.ui.common.ViewModelFactory;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/** Manages expense and income categories. */
public class CategoriesFragment extends Fragment {

    private FragmentCategoriesBinding binding;
    private CategoriesViewModel viewModel;
    private CategoryAdapter adapter;
    private TransactionType selectedType = TransactionType.EXPENSE;
    private List<Category> allCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelFactory.of(this))
                .get(CategoriesViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.fab.setOnClickListener(v -> openEditor(null));

        adapter = new CategoryAdapter((category, anchor) -> showActions(category));
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.empty.emptyIcon.setImageResource(R.drawable.ic_action_category);
        binding.empty.emptyTitle.setText(R.string.empty_categories_title);
        binding.empty.emptyMessage.setText(R.string.empty_categories_message);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.no_categories_cta);
        binding.empty.emptyAction.setOnClickListener(v -> openEditor(null));

        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedType = tab.getPosition() == 0
                        ? TransactionType.EXPENSE : TransactionType.INCOME;
                render();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories;
            render();
        });
    }

    private void render() {
        List<Category> filtered = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.type == selectedType) {
                filtered.add(category);
            }
        }
        adapter.submitList(filtered);
        boolean empty = filtered.isEmpty();
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showActions(Category category) {
        CategoryEditorSheet.show(getParentFragmentManager(), category, selectedType);
    }

    private void openEditor(@Nullable Category category) {
        CategoryEditorSheet.show(getParentFragmentManager(), category, selectedType);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
