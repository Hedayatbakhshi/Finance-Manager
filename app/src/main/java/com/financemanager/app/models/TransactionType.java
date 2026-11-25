package com.financemanager.app.models;

/** The kind of money movement a transaction represents. */
public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER;

    public static TransactionType fromKey(String key) {
        if (key == null) {
            return EXPENSE;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            return EXPENSE;
        }
    }

    public boolean isTransfer() {
        return this == TRANSFER;
    }

    /** Sign applied to this type when computing cash flow. Transfers are neutral. */
    public int cashFlowSign() {
        switch (this) {
            case INCOME:
                return 1;
            case EXPENSE:
                return -1;
            default:
                return 0;
        }
    }
}
