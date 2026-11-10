package com.ayaresa.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** A registered patient. Immutable: edits produce a new record via the store. */
public record Patient(
        String id,
        String name,
        Sex sex,
        LocalDate dateOfBirth,
        String phone,
        String ghanaCard,
        String nhisNumber,
        LocalDate nhisExpiry,
        String bloodGroup,
        List<String> allergies,
        List<String> conditions,
        LocalDateTime registeredAt) implements Serializable {

    public Patient {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        allergies = List.copyOf(allergies == null ? List.of() : allergies);
        conditions = List.copyOf(conditions == null ? List.of() : conditions);
    }

    public int age(LocalDate today) {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, today).getYears();
    }

    public boolean hasNhis() {
        return nhisNumber != null && !nhisNumber.isBlank();
    }

    /** NHIS membership must be valid on the day of service for a claim to be accepted. */
    public boolean nhisActive(LocalDate today) {
        return hasNhis() && nhisExpiry != null && !nhisExpiry.isBefore(today);
    }

    public boolean isAllergicTo(String allergyClass) {
        if (allergyClass == null) {
            return false;
        }
        String c = allergyClass.toLowerCase(Locale.ROOT);
        return allergies.stream().anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(c));
    }

    /** Matches name, ID, phone, Ghana Card or NHIS number (for search). */
    public boolean matches(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        return name.toLowerCase(Locale.ROOT).contains(q)
                || id.toLowerCase(Locale.ROOT).contains(q)
                || (phone != null && phone.replace(" ", "").contains(q.replace(" ", "")))
                || (ghanaCard != null && ghanaCard.toLowerCase(Locale.ROOT).contains(q))
                || (nhisNumber != null && nhisNumber.toLowerCase(Locale.ROOT).contains(q));
    }
}
