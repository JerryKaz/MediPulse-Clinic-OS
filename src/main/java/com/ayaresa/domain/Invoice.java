package com.ayaresa.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Amounts are Ghana cedis (GHS), held as BigDecimal: never use double for money. */
public record Invoice(
        String id,
        String patientId,
        List<InvoiceLine> lines,
        Payer payer,
        InvoiceStatus status,
        LocalDateTime createdAt,
        String reference) implements Serializable {

    public Invoice {
        lines = List.copyOf(lines);
    }

    public BigDecimal total() {
        return lines.stream().map(InvoiceLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Invoice with(Payer p, InvoiceStatus s, String ref) {
        return new Invoice(id, patientId, lines, p, s, createdAt, ref);
    }
}
