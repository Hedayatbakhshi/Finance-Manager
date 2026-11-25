package com.financemanager.app.models;

/** Supported account categories. */
public enum AccountType {
    CASH,
    BANK,
    SAVINGS,
    CREDIT,
    WALLET;

    public static AccountType fromKey(String key) {
        if (key == null) {
            return CASH;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            return CASH;
        }
    }
}
