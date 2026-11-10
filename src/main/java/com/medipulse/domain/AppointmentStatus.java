package com.medipulse.domain;

public enum AppointmentStatus {
    SCHEDULED("Scheduled"), CHECKED_IN("Checked in"), COMPLETED("Completed"),
    NO_SHOW("No-show"), CANCELLED("Cancelled");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
