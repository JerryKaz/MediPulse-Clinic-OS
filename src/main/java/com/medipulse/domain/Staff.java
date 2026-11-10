package com.medipulse.domain;

import java.io.Serializable;

public record Staff(String id, String name, String title, String specialty, String phone, boolean onDuty)
        implements Serializable {

    public Staff withOnDuty(boolean value) {
        return new Staff(id, name, title, specialty, phone, value);
    }
}
