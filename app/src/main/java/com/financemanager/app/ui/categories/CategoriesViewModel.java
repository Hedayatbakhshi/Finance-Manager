package com.financemanager.app.ui.categories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.financemanager.app.data.FinanceRepository;
import com.financemanager.app.data.entity.Category;

import java.util.List;

/** All categories, filtered by type in the UI. */
public class CategoriesViewModel extends ViewModel {

    private final FinanceRepository repository;
    private final LiveData<List<Category>> categories;

    public CategoriesViewModel(@NonNull FinanceRepository repository) {
        this.repository = repository;
        categories = repository.observeCategories();
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public void saveCategory(Category category, boolean isNew,
                             FinanceRepository.Callback<Long> callback) {
        repository.saveCategory(category, isNew, callback);
    }

    public void deleteCategory(long id, FinanceRepository.Callback<Boolean> callback) {
        repository.deleteCategory(id, callback);
    }
}
