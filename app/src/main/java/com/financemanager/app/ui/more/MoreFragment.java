package com.financemanager.app.ui.more;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.financemanager.app.BuildConfig;
import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.databinding.FragmentMoreBinding;
import com.financemanager.app.databinding.ItemMoreBinding;
import com.financemanager.app.security.SettingsRepository;
import com.financemanager.app.ui.common.LanguagePicker;
import com.financemanager.app.ui.common.UiUtils;
import com.financemanager.app.utils.LanguageHelper;

/** Hub for secondary screens and app information. */
public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SettingsRepository settings = ((FinanceManagerApp) requireActivity().getApplication())
                .container().settings();

        String name = settings.getUserName();
        binding.avatar.setText(UiUtils.initials(name));
        binding.name.setText(name == null || name.trim().isEmpty()
                ? getString(R.string.more_profile_name) : name);
        binding.subtitle.setText(getString(R.string.more_currency_summary,
                settings.getCurrency()));
        binding.profileCard.setOnClickListener(v -> navigate(R.id.settings));

        addRow(binding.manageContainer, R.drawable.ic_action_wallet,
                R.string.accounts, R.string.more_accounts_subtitle, R.id.accounts);
        addRow(binding.manageContainer, R.drawable.ic_action_category,
                R.string.categories, R.string.more_categories_subtitle, R.id.categories);
        addRow(binding.manageContainer, R.drawable.ic_goal_target,
                R.string.savings_goals, R.string.more_savings_subtitle, R.id.savings);
        addRow(binding.manageContainer, R.drawable.ic_action_repeat,
                R.string.recurring_transactions, R.string.more_recurring_subtitle,
                R.id.recurring);
        addRow(binding.manageContainer, R.drawable.ic_nav_budgets,
                R.string.nav_budgets, R.string.empty_budgets_message, R.id.budgets);
        addRow(binding.manageContainer, R.drawable.ic_action_person,
                R.string.household_expenses, R.string.household, R.id.households);

        addRow(binding.systemContainer, R.drawable.ic_action_language,
                R.string.language, LanguagePicker.label(requireContext(),
                        LanguageHelper.currentLanguage()),
                () -> LanguagePicker.show(requireContext(), LanguageHelper::apply));
        addRow(binding.systemContainer, R.drawable.ic_action_backup,
                R.string.backup_restore, R.string.more_data_subtitle, R.id.data);
        addRow(binding.systemContainer, R.drawable.ic_action_settings,
                R.string.settings, R.string.more_settings_subtitle, R.id.settings);
        addRow(binding.systemContainer, R.drawable.ic_action_info,
                R.string.about, R.string.about_data_storage, R.id.about);

        binding.version.setText(getString(R.string.app_version_label, BuildConfig.VERSION_NAME));
    }

    private void addRow(LinearLayout container, @DrawableRes int icon,
                        @StringRes int title, @StringRes int subtitle,
                        int destination) {
        addRow(container, icon, title, getString(subtitle), () -> navigate(destination));
    }

    private void addRow(LinearLayout container, @DrawableRes int icon,
                        @StringRes int title, CharSequence subtitle, Runnable action) {
        ItemMoreBinding row = ItemMoreBinding.inflate(getLayoutInflater(), container, false);
        row.icon.setImageResource(icon);
        row.title.setText(title);
        row.subtitle.setText(subtitle);
        UiUtils.tintIconTile(row.iconContainer, row.icon, "#5B7FA6");
        row.getRoot().setOnClickListener(v -> action.run());
        container.addView(row.getRoot());
    }

    private void navigate(int destination) {
        NavHostFragment.findNavController(this).navigate(destination);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
