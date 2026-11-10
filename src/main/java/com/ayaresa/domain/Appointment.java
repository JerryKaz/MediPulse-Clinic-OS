package com.ayaresa.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public record Appointment(
        String id,
        String patientId,
        String staffId,
        LocalDateTime start,
        int minutes,
        String reason,
        AppointmentStatus status) implements Serializable {

    public LocalDateTime end() {
        return start.plusMinutes(minutes);
    }

    public boolean overlaps(Appointment o) {
        return staffId.equals(o.staffId) && start.isBefore(o.end()) && o.start.isBefore(end());
    }

    public Appointment withStatus(AppointmentStatus s) {
        return new Appointment(id, patientId, staffId, start, minutes, reason, s);
    }
}
