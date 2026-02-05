package com.bank.reconciliation.entity;

public enum MatchType {
    EXACT_MATCH,
    FUZZY_MATCH,
    PARTIAL_PAYMENT,
    OVERPAYMENT,
    MISSING_PAYMENT,
    UNEXPECTED_PAYMENT,
    MANUAL_OVERRIDE
}
