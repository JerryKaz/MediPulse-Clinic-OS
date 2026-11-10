package com.ayaresa.domain;

/** Where a patient is in today's visit. */
public enum Stage {
    WAITING("Waiting"), TRIAGE("Triage"), CONSULTATION("With doctor"),
    PHARMACY("Pharmacy"), BILLING("Billing"), DONE("Done");

    private final String label;

    Stage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Button text for moving a patient out of this stage. */
    public String moveLabel() {
        return switch (this) {
            case WAITING -> "Start triage";
            case TRIAGE -> "Send to doctor";
            case CONSULTATION -> "Send to pharmacy";
            case PHARMACY -> "Send to billing";
            case BILLING -> "Finish visit";
            case DONE -> "Done";
        };
    }

    public Stage next() {
        return this == DONE ? DONE : values()[ordinal() + 1];
    }
}
