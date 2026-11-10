package com.ayaresa.domain;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

/** One patient's visit today, from arrival to discharge. */
public record QueueEntry(
        String id,
        int token,
        String patientId,
        Priority priority,
        Stage stage,
        String complaint,
        LocalDateTime arrivedAt,
        LocalDateTime stageSince) implements Serializable {

    /** Priority first, then whoever has waited longest. */
    public static final Comparator<QueueEntry> TRIAGE_ORDER =
            Comparator.comparing(QueueEntry::priority).thenComparing(QueueEntry::arrivedAt);

    public QueueEntry withStage(Stage s, LocalDateTime now) {
        return new QueueEntry(id, token, patientId, priority, s, complaint, arrivedAt, now);
    }

    public QueueEntry withPriority(Priority p) {
        return new QueueEntry(id, token, patientId, p, stage, complaint, arrivedAt, stageSince);
    }

    public Duration waitingInStage(LocalDateTime now) {
        return Duration.between(stageSince, now);
    }

    public String tokenLabel() {
        return String.format("A%03d", token);
    }
}
