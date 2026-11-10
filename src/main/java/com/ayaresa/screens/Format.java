package com.ayaresa.screens;

import com.ayaresa.clinical.News2;
import com.ayaresa.domain.AppointmentStatus;
import com.ayaresa.domain.InvoiceStatus;
import com.ayaresa.domain.Priority;
import com.ayaresa.ui.DataTable;
import com.ayaresa.ui.StatusPill;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Display formatting shared by all screens (money, times, status pills). */
public final class Format {

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm");
    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.UK);

    static {
        MONEY.setMinimumFractionDigits(2);
        MONEY.setMaximumFractionDigits(2);
    }

    private Format() {
    }

    /** Ghana cedis: "GH₵ 1,234.50". */
    public static String cedis(BigDecimal amount) {
        return "GH₵ " + MONEY.format(amount);
    }

    public static String waited(Duration d) {
        long m = Math.max(0, d.toMinutes());
        return m < 60 ? m + " min" : (m / 60) + " h " + (m % 60) + " min";
    }

    public static String relativeDay(LocalDateTime t, LocalDate today) {
        LocalDate d = t.toLocalDate();
        if (d.equals(today)) {
            return "Today, " + t.format(TIME);
        }
        if (d.equals(today.minusDays(1))) {
            return "Yesterday, " + t.format(TIME);
        }
        return t.format(DATE_TIME);
    }

    public static StatusPill.Kind kind(News2.Risk r) {
        return switch (r) {
            case LOW -> StatusPill.Kind.OK;
            case LOW_MEDIUM, MEDIUM -> StatusPill.Kind.WARNING;
            case HIGH -> StatusPill.Kind.CRITICAL;
        };
    }

    public static DataTable.Pill pill(News2.Result r) {
        return new DataTable.Pill("NEWS2 " + r.total() + " · " + r.risk().label(), kind(r.risk()));
    }

    public static StatusPill.Kind kind(Priority p) {
        return switch (p) {
            case EMERGENCY -> StatusPill.Kind.CRITICAL;
            case URGENT -> StatusPill.Kind.WARNING;
            case ROUTINE -> StatusPill.Kind.IDLE;
        };
    }

    public static StatusPill.Kind kind(InvoiceStatus s) {
        return switch (s) {
            case PAID, CLAIM_PAID -> StatusPill.Kind.OK;
            case UNPAID -> StatusPill.Kind.WARNING;
            case CLAIM_SUBMITTED -> StatusPill.Kind.INFO;
            case CLAIM_REJECTED -> StatusPill.Kind.CRITICAL;
            case WAIVED -> StatusPill.Kind.IDLE;
        };
    }

    public static StatusPill.Kind kind(AppointmentStatus s) {
        return switch (s) {
            case SCHEDULED -> StatusPill.Kind.IDLE;
            case CHECKED_IN -> StatusPill.Kind.INFO;
            case COMPLETED -> StatusPill.Kind.OK;
            case NO_SHOW -> StatusPill.Kind.WARNING;
            case CANCELLED -> StatusPill.Kind.CRITICAL;
        };
    }
}
