package com.medipulse.domain;

import java.io.Serializable;
import java.math.BigDecimal;

public record InvoiceLine(String description, int quantity, BigDecimal unitPrice) implements Serializable {

    public BigDecimal total() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
