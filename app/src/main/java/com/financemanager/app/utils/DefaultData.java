package com.financemanager.app.utils;

import android.content.Context;

import com.financemanager.app.R;
import com.financemanager.app.data.entity.Account;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.models.AccountType;
import com.financemanager.app.models.TransactionType;

import java.util.ArrayList;
import java.util.List;

/** First-launch content: a sensible category set and a starter cash account. */
public final class DefaultData {

    private DefaultData() {
    }

    public static List<Category> categories(Context context, long now) {
        List<Category> list = new ArrayList<>();
        list.add(category(context.getString(R.string.default_category_food),
                TransactionType.EXPENSE, "cat_food", "#E5A43B", now));
        list.add(category(context.getString(R.string.default_category_transport),
                TransactionType.EXPENSE, "cat_transport", "#3E6DB5", now));
        list.add(category(context.getString(R.string.default_category_shopping),
                TransactionType.EXPENSE, "cat_shopping", "#D96A9B", now));
        list.add(category(context.getString(R.string.default_category_entertainment),
                TransactionType.EXPENSE, "cat_entertainment", "#7E63C9", now));
        list.add(category(context.getString(R.string.default_category_health),
                TransactionType.EXPENSE, "cat_health", "#E05252", now));
        list.add(category(context.getString(R.string.default_category_education),
                TransactionType.EXPENSE, "cat_education", "#2AA3C0", now));
        list.add(category(context.getString(R.string.default_category_housing),
                TransactionType.EXPENSE, "cat_housing", "#A9714B", now));
        list.add(category(context.getString(R.string.default_category_utilities),
                TransactionType.EXPENSE, "cat_utilities", "#7B9E21", now));
        list.add(category(context.getString(R.string.default_category_travel),
                TransactionType.EXPENSE, "cat_travel", "#2AA3C0", now));
        list.add(category(context.getString(R.string.default_category_subscriptions),
                TransactionType.EXPENSE, "cat_subscriptions", "#7E63C9", now));
        list.add(category(context.getString(R.string.default_category_personal_care),
                TransactionType.EXPENSE, "cat_personal_care", "#D96A9B", now));
        list.add(category(context.getString(R.string.default_category_other),
                TransactionType.EXPENSE, "cat_other", "#778397", now));

        list.add(category(context.getString(R.string.default_category_salary),
                TransactionType.INCOME, "cat_salary", "#16A36A", now));
        list.add(category(context.getString(R.string.default_category_freelance),
                TransactionType.INCOME, "cat_freelance", "#3E6DB5", now));
        list.add(category(context.getString(R.string.default_category_business),
                TransactionType.INCOME, "cat_business", "#7E63C9", now));
        list.add(category(context.getString(R.string.default_category_investment),
                TransactionType.INCOME, "cat_investment", "#16A36A", now));
        list.add(category(context.getString(R.string.default_category_gifts),
                TransactionType.INCOME, "cat_gifts", "#E5A43B", now));
        list.add(category(context.getString(R.string.default_category_other),
                TransactionType.INCOME, "cat_other", "#778397", now));
        return list;
    }

    public static Account starterAccount(Context context, String currency, long now) {
        Account account = new Account();
        account.name = context.getString(R.string.default_account_cash);
        account.type = AccountType.CASH;
        account.initialBalanceMinor = 0L;
        account.currency = currency;
        account.color = "#3E6DB5";
        account.icon = "account_cash";
        account.createdAt = now;
        account.updatedAt = now;
        return account;
    }

    private static Category category(String name, TransactionType type, String icon,
                                     String color, long now) {
        Category category = new Category();
        category.name = name;
        category.type = type;
        category.icon = icon;
        category.color = color;
        category.isDefault = true;
        category.createdAt = now;
        return category;
    }
}
