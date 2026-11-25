package com.financemanager.app.ui.common;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.financemanager.app.R;
import com.financemanager.app.utils.Money;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Currency;
import java.util.Locale;
import java.util.function.Consumer;

/** Chooses one of the supported currencies or a custom ISO code. */
public final class CurrencyPicker {

    private static final String[] COMMON = {
            "USD", "EUR", "GBP", "AFN", "PKR", "INR", "CAD", "AUD", "JPY", "AED"
    };

    private CurrencyPicker() {
    }

    public static void show(Context context, String current, Consumer<String> onSelected) {
        String[] labels = new String[COMMON.length + 1];
        for (int i = 0; i < COMMON.length; i++) {
            labels[i] = label(COMMON[i]);
        }
        labels[COMMON.length] = context.getString(R.string.custom_currency) + "…";
        int checked = -1;
        for (int i = 0; i < COMMON.length; i++) {
            if (COMMON[i].equalsIgnoreCase(current)) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.default_currency)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if (which == COMMON.length) {
                        dialog.dismiss();
                        showCustom(context, current, onSelected);
                    } else {
                        onSelected.accept(COMMON[which]);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static String label(String code) {
        String name = Currency.getInstance(code).getDisplayName(Locale.getDefault());
        return name + " (" + code + ") · " + Money.symbol(code);
    }

    private static void showCustom(Context context, String current, Consumer<String> onSelected) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint(R.string.custom_currency_hint);
        input.setText(current == null ? "" : current);
        input.setSelectAllOnFocus(true);
        int padding = Math.round(20 * context.getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(context);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.custom_currency)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String code = input.getText().toString().trim().toUpperCase(Locale.ROOT);
                    if (code.matches("[A-Z]{3}")) {
                        onSelected.accept(code);
                    } else {
                        UiUtils.snackbar(input, context.getString(R.string.currency_invalid));
                    }
                })
                .show();
    }
}
