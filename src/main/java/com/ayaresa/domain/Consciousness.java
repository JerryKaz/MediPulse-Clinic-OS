package com.ayaresa.domain;

/** ACVPU scale used by NEWS2. Anything other than Alert scores 3. */
public enum Consciousness {
    ALERT("Alert"), NEW_CONFUSION("New confusion"), VOICE("Responds to voice"),
    PAIN("Responds to pain"), UNRESPONSIVE("Unresponsive");

    private final String label;

    Consciousness(String label) {
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
