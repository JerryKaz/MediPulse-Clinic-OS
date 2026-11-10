package com.ayaresa.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A stocked medicine. {@code allergyClass} (e.g. "penicillin", "sulfa", "nsaid") is matched
 * against patient allergies before dispensing.
 */
public record Drug(
        String id,
        String name,
        String form,
        String allergyClass,
        int stock,
        int reorderLevel,
        BigDecimal unitPrice,
        LocalDate expiry,
        boolean nhisCovered) implements Serializable {

    public boolean lowStock() {
        return stock <= reorderLevel;
    }

    public boolean expired(LocalDate today) {
        return expiry != null && expiry.isBefore(today);
    }

    /** Within 90 days of expiry: use first or return to supplier. */
    public boolean expiringSoon(LocalDate today) {
        return expiry != null && !expired(today) && expiry.isBefore(today.plusDays(90));
    }

    public Drug withStock(int value) {
        return new Drug(id, name, form, allergyClass, value, reorderLevel, unitPrice, expiry, nhisCovered);
    }
}
