package com.financemanager.app.utils;

import com.financemanager.app.R;

import java.util.HashMap;
import java.util.Map;

/** Maps persisted icon keys to drawable resources. */
public final class Icons {

    private static final Map<String, Integer> REGISTRY = new HashMap<>();

    static {
        // Categories
        REGISTRY.put("cat_food", R.drawable.ic_cat_food);
        REGISTRY.put("cat_transport", R.drawable.ic_cat_transport);
        REGISTRY.put("cat_shopping", R.drawable.ic_cat_shopping);
        REGISTRY.put("cat_entertainment", R.drawable.ic_cat_entertainment);
        REGISTRY.put("cat_health", R.drawable.ic_cat_health);
        REGISTRY.put("cat_education", R.drawable.ic_cat_education);
        REGISTRY.put("cat_housing", R.drawable.ic_cat_housing);
        REGISTRY.put("cat_utilities", R.drawable.ic_cat_utilities);
        REGISTRY.put("cat_travel", R.drawable.ic_cat_travel);
        REGISTRY.put("cat_subscriptions", R.drawable.ic_cat_subscriptions);
        REGISTRY.put("cat_personal_care", R.drawable.ic_cat_personal_care);
        REGISTRY.put("cat_other", R.drawable.ic_cat_other);
        REGISTRY.put("cat_salary", R.drawable.ic_cat_salary);
        REGISTRY.put("cat_freelance", R.drawable.ic_cat_freelance);
        REGISTRY.put("cat_business", R.drawable.ic_cat_business);
        REGISTRY.put("cat_investment", R.drawable.ic_cat_investment);
        REGISTRY.put("cat_gifts", R.drawable.ic_cat_gifts);

        // Accounts
        REGISTRY.put("account_cash", R.drawable.ic_account_cash);
        REGISTRY.put("account_bank", R.drawable.ic_account_bank);
        REGISTRY.put("account_savings", R.drawable.ic_account_savings);
        REGISTRY.put("account_credit", R.drawable.ic_account_credit);
        REGISTRY.put("account_wallet", R.drawable.ic_account_wallet);

        // Goals and actions
        REGISTRY.put("goal_target", R.drawable.ic_goal_target);
        REGISTRY.put("action_wallet", R.drawable.ic_action_wallet);
        REGISTRY.put("action_person", R.drawable.ic_action_person);
        REGISTRY.put("action_target", R.drawable.ic_action_target);
        REGISTRY.put("action_warning", R.drawable.ic_action_warning);
    }

    private Icons() {
    }

    /** Returns the drawable for a key, falling back to a neutral icon. */
    public static int res(String key) {
        Integer drawable = key == null ? null : REGISTRY.get(key);
        return drawable == null ? R.drawable.ic_cat_other : drawable;
    }

    public static String[] expenseIcons() {
        return new String[]{"cat_food", "cat_transport", "cat_shopping", "cat_entertainment",
                "cat_health", "cat_education", "cat_housing", "cat_utilities", "cat_travel",
                "cat_subscriptions", "cat_personal_care", "cat_other"};
    }

    public static String[] incomeIcons() {
        return new String[]{"cat_salary", "cat_freelance", "cat_business", "cat_investment",
                "cat_gifts", "cat_other"};
    }

    public static String[] accountIcons() {
        return new String[]{"account_cash", "account_bank", "account_savings", "account_credit",
                "account_wallet"};
    }

    public static String[] goalIcons() {
        return new String[]{"goal_target", "action_wallet", "action_target", "cat_travel",
                "cat_shopping", "cat_housing", "cat_education", "cat_health"};
    }

    /** Palette shared by categories, accounts and goals. */
    public static String[] colors() {
        return new String[]{"#3E6DB5", "#16A36A", "#E5A43B", "#E05252", "#7E63C9",
                "#D96A9B", "#2AA3C0", "#778397", "#A9714B", "#7B9E21"};
    }
}
