package com.ayaresa.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A bed in a ward. Free when {@code patientId} is null. */
public record Bed(String id, String ward, String patientId, LocalDateTime admittedAt, String note)
        implements Serializable {

    public boolean isFree() {
        return patientId == null;
    }

    public Bed admit(String patient, LocalDateTime at, String reason) {
        return new Bed(id, ward, patient, at, reason);
    }

    public Bed discharge() {
        return new Bed(id, ward, null, null, null);
    }
}
