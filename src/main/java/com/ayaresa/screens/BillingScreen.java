package com.ayaresa.screens;

import com.ayaresa.domain.Invoice;
import com.ayaresa.domain.InvoiceLine;
import com.ayaresa.domain.InvoiceStatus;
import com.ayaresa.domain.Payer;
import com.ayaresa.security.Role;
import com.ayaresa.security.Screen;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.BarChart;
import com.ayaresa.ui.Card;
import com.ayaresa.ui.Chips;
import com.ayaresa.ui.DataTable;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.Sheet;
import com.ayaresa.ui.StatusPill;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;
import com.ayaresa.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.ayaresa.ui.DataTable.col;

/**
 * Billing in Ghana cedis: cash, Mobile Money and card payments, waivers, and NHIS claims
 * (submitted only when membership was active on the day of service).
 */
public final class BillingScreen extends ScreenView {

    private final JPanel kpis = new JPanel(new GridLayout(1, 2, Theme.GAP, Theme.GAP));
    private final BarChart chart = new BarChart(Theme.ORANGE);
    private final Chips chips = new Chips(List.of("All", "Unpaid", "NHIS pending", "Rejected", "Settled"));
    private final DataTable<Invoice> table;
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    public BillingScreen(AppContext app) {
        super(app, ClinicStore.Topic.BILLING, ClinicStore.Topic.PATIENTS);
        ClinicStore s = app.store();
        table = new DataTable<>(List.of(
                col("Invoice", i -> new DataTable.Person(s.patient(i.patientId()).map(p -> p.name()).orElse(i.patientId()),
                        i.id() + " · " + Format.relativeDay(i.createdAt(), s.today())), 250, DataTable.Kind.PERSON),
                col("Items", i -> summary(i), 260, DataTable.Kind.SECONDARY),
                col("Payer", i -> new DataTable.Pill(i.payer().label(), i.payer() == Payer.NHIS ? StatusPill.Kind.INFO : StatusPill.Kind.IDLE), 130, DataTable.Kind.PILL),
                col("Amount", i -> Format.cedis(i.total()), 120, DataTable.Kind.RIGHT),
                col("Status", i -> new DataTable.Pill(i.status().label(), Format.kind(i.status())), 150, DataTable.Kind.PILL),
                col("Reference", i -> i.reference() == null ? "" : i.reference(), 130, DataTable.Kind.SECONDARY)));
        table.onSelect(sel -> updateActions(sel));
        table.onOpen(this::details);

        kpis.setOpaque(false);
        actions.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(header("Billing", "Amounts in Ghana cedis. Select an invoice to take payment or claim from NHIS.", actions), BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(Theme.GAP, 0));
        mid.setOpaque(false);
        mid.add(kpis, BorderLayout.CENTER);
        kpis.setLayout(new GridLayout(1, 4, Theme.GAP, 0));
        Card chartCard = new Card(new BorderLayout(0, 4));
        chartCard.add(secondary("Collected per day, last 7 days"), BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(250, 92));
        chartCard.add(chart, BorderLayout.CENTER);
        mid.add(chartCard, BorderLayout.EAST);
        top.add(mid, BorderLayout.CENTER);
        top.add(chips, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        chips.onChange(i -> refresh());
        updateActions(Optional.empty());
    }

    private static String summary(Invoice i) {
        List<String> names = i.lines().stream().map(InvoiceLine::description).toList();
        return names.size() <= 2 ? String.join(", ", names) : names.get(0) + ", " + names.get(1) + " +" + (names.size() - 2);
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        LocalDate today = s.today();
        List<Invoice> all = s.invoices();
        List<Predicate<Invoice>> filters = List.of(i -> true,
                i -> i.status() == InvoiceStatus.UNPAID,
                i -> i.status() == InvoiceStatus.CLAIM_SUBMITTED,
                i -> i.status() == InvoiceStatus.CLAIM_REJECTED,
                i -> i.status().settled());
        for (int i = 1; i < filters.size(); i++) {
            chips.setCount(i, (int) all.stream().filter(filters.get(i)).count());
        }
        table.setRows(all.stream().filter(filters.get(chips.selected())).toList(), "No invoices here.");

        BigDecimal pendingClaims = all.stream().filter(i -> i.status() == InvoiceStatus.CLAIM_SUBMITTED)
                .map(Invoice::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long momo = all.stream().filter(i -> i.createdAt().toLocalDate().equals(today) && i.payer() == Payer.MOBILE_MONEY && i.status().settled()).count();
        kpis.removeAll();
        kpis.add(kpi(TablerIcon.CASH, Theme.GREEN, "Collected today", Format.cedis(s.revenue(today, today)),
                momo + " by Mobile Money", null, 21));
        kpis.add(kpi(TablerIcon.CHART_BAR, Theme.BLUE, "Last 7 days", Format.cedis(s.revenue(today.minusDays(6), today)), "Paid and claims paid", null, 21));
        kpis.add(kpi(TablerIcon.RECEIPT, Theme.ORANGE, "Outstanding", Format.cedis(s.outstanding()), "Unpaid or claim pending", null, 21));
        kpis.add(kpi(TablerIcon.ID, Theme.INDIGO, "NHIS pending", Format.cedis(pendingClaims),
                all.stream().filter(i -> i.status() == InvoiceStatus.CLAIM_REJECTED).count() + " rejected", StatusPill.Kind.WARNING, 21));
        kpis.revalidate();

        List<BarChart.Bar> bars = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            BigDecimal v = s.revenue(day, day);
            bars.add(new BarChart.Bar(d == 0 ? "Today" : day.getDayOfWeek().name().substring(0, 1), v.doubleValue(),
                    day.format(Format.SHORT_DATE) + ": " + Format.cedis(v)));
        }
        chart.setBars(bars);
        updateActions(table.selected());
    }

    private void updateActions(Optional<Invoice> sel) {
        actions.removeAll();
        if (sel.isPresent() && app.canEdit(Screen.BILLING)) {
            Invoice i = sel.get();
            if (i.status() == InvoiceStatus.UNPAID || i.status() == InvoiceStatus.CLAIM_REJECTED) {
                actions.add(secondaryButton("Take payment", TablerIcon.CASH, () -> Sheets.recordPayment(app, i)));
                actions.add(secondaryButton("Claim from NHIS", TablerIcon.SEND, () -> app.attempt(() -> {
                    app.store().submitClaim(i.id());
                    app.toast("NHIS claim submitted for " + i.id() + ".", Toast.Kind.SUCCESS);
                })));
            }
            if (i.status() == InvoiceStatus.CLAIM_SUBMITTED && app.user().role() == Role.ADMIN) {
                actions.add(secondaryButton("Claim paid", TablerIcon.CHECK, () -> app.attempt(() -> app.store().recordClaimResult(i.id(), true))));
                actions.add(secondaryButton("Claim rejected", TablerIcon.X, () -> app.attempt(() -> app.store().recordClaimResult(i.id(), false))));
            }
        }
        actions.revalidate();
        actions.repaint();
    }

    private void details(Invoice i) {
        JPanel body = vertical();
        for (InvoiceLine l : i.lines()) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)));
            JLabel d = new JLabel(l.description() + (l.quantity() > 1 ? "  × " + l.quantity() : ""));
            d.setFont(Theme.body());
            d.setForeground(Theme.LABEL);
            JLabel a = new JLabel(Format.cedis(l.total()));
            a.setFont(Theme.body());
            a.setForeground(Theme.LABEL);
            row.add(d, BorderLayout.WEST);
            row.add(a, BorderLayout.EAST);
            row.setAlignmentX(LEFT_ALIGNMENT);
            body.add(row);
        }
        JPanel total = new JPanel(new BorderLayout());
        total.setOpaque(false);
        total.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel t = sectionTitle("Total");
        JLabel v = sectionTitle(Format.cedis(i.total()));
        total.add(t, BorderLayout.WEST);
        total.add(v, BorderLayout.EAST);
        total.setAlignmentX(LEFT_ALIGNMENT);
        body.add(total);
        String who = app.store().patient(i.patientId()).map(p -> p.name()).orElse(i.patientId());
        Sheet.open(this, i.id(), who + " · " + i.payer().label() + " · " + i.status().label(), body, 460)
                .primary("Close", () -> true);
    }

}
