package com.financemanager.app.ui.common;

import android.content.Context;

import com.financemanager.app.R;
import com.financemanager.app.utils.LanguageHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;

/** Chooses the app language and reports the selected code. */
public final class LanguagePicker {

    private LanguagePicker() {
    }

    /** Human-readable label for a stored language code. */
    public static String label(Context context, String language) {
        if (LanguageHelper.PERSIAN.equals(language)) {
            return context.getString(R.string.language_persian);
        }
        if (LanguageHelper.ENGLISH.equals(language)) {
            return context.getString(R.string.language_english);
        }
        return context.getString(R.string.language_system);
    }

    public static void show(Context context, Consumer<String> onSelected) {
        String[] codes = {LanguageHelper.SYSTEM, LanguageHelper.ENGLISH,
                LanguageHelper.PERSIAN};
        String[] labels = {context.getString(R.string.language_system),
                context.getString(R.string.language_english),
                context.getString(R.string.language_persian)};
        String current = LanguageHelper.currentLanguage();
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.language)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    onSelected.accept(codes[which]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
