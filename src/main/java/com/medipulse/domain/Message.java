package com.medipulse.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public record Message(String id, String thread, String from, String text, LocalDateTime at) implements Serializable {
}
