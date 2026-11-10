package com.ayaresa.domain;

public enum Payer {
    NHIS("NHIS"), CASH("Cash"), MOBILE_MONEY("Mobile Money"), CARD("Card"), WAIVER("Waiver");

    private final String label;

    Payer(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
