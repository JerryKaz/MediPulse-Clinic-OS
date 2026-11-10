package com.medipulse.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/** An immutable dispensing record. {@code allergyOverride} is set when a warning was overridden. */
public record Dispense(String id, String drugId, String patientId, int quantity, LocalDateTime at,
                       String by, boolean allergyOverride) implements Serializable {
}
