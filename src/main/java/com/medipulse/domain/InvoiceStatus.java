package com.medipulse.domain;

public enum InvoiceStatus {
    UNPAID("Unpaid"), PAID("Paid"), CLAIM_SUBMITTED("Claim submitted"), CLAIM_PAID("Claim paid"),
    CLAIM_REJECTED("Claim rejected"), WAIVED("Waived");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean settled() {
        return this == PAID || this == CLAIM_PAID || this == WAIVED;
    }
}
