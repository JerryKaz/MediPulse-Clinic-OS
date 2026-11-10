package com.ayaresa.clinical;

import com.ayaresa.domain.Drug;
import com.ayaresa.domain.Patient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Checks run before a drug is dispensed. Warnings can be overridden; blocks cannot. */
public final class SafetyCheck {

    public enum Level { WARNING, BLOCK }

    public record Issue(Level level, String message) {
    }

    private SafetyCheck() {
    }

    public static List<Issue> beforeDispense(Patient patient, Drug drug, int quantity, LocalDate today) {
        List<Issue> issues = new ArrayList<>();
        if (quantity <= 0) {
            issues.add(new Issue(Level.BLOCK, "Quantity must be at least 1."));
        }
        if (quantity > drug.stock()) {
            issues.add(new Issue(Level.BLOCK, "Only " + drug.stock() + " in stock."));
        }
        if (drug.expired(today)) {
            issues.add(new Issue(Level.BLOCK, drug.name() + " expired on " + drug.expiry() + ". Remove it from stock."));
        }
        if (patient.isAllergicTo(drug.allergyClass())) {
            issues.add(new Issue(Level.WARNING, patient.name() + " has a recorded " + drug.allergyClass()
                    + " allergy. " + drug.name() + " may cause a reaction."));
        }
        return issues;
    }

    public static boolean blocked(List<Issue> issues) {
        return issues.stream().anyMatch(i -> i.level() == Level.BLOCK);
    }
}
