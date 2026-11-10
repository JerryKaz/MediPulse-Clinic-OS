package com.medipulse.domain;

/** Triage priority. Patients are seen by priority first, then by arrival time. */
public enum Priority {
    EMERGENCY("Emergency"), URGENT("Urgent"), ROUTINE("Routine");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
