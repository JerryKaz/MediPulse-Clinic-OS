package com.medipulse.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One set of observations. Units: breaths/min, %, mmHg, beats/min, degrees C. */
public record VitalSigns(
        String id,
        String patientId,
        LocalDateTime takenAt,
        int respirationRate,
        int spo2,
        boolean onOxygen,
        int systolic,
        int diastolic,
        int pulse,
        Consciousness consciousness,
        double temperature,
        String takenBy) implements Serializable {

    public String bloodPressure() {
        return systolic + "/" + diastolic;
    }
}
