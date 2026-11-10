package com.medipulse.screens;

import com.medipulse.domain.Dispense;
import com.medipulse.domain.Drug;
import com.medipulse.security.Screen;
import com.medipulse.shell.AppContext;
import com.medipulse.store.ClinicStore;
import com.medipulse.ui.Chips;
import com.medipulse.ui.DataTable;
import com.medipulse.ui.PillButton;
import com.medipulse.ui.StatusPill;
import com.medipulse.ui.TablerIcon;
import com.medipulse.ui.Theme;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Predicate;

import static com.medipulse.ui.DataTable.col;

/**
 * Dispensary stock with reorder and expiry tracking. Dispensing runs allergy, stock and
 * expiry checks first and adds the medicine to the patient's bill.
 */
public final class PharmacyScreen extends ScreenView {

    private final JPanel kpis = new JPanel(new GridLayout(1, 4, Theme.GAP, 0));
    private final Chips chips = new Chips(List.of("All", "Low stock", "Expiring in 90 days", "Expired", "NHIS covered"));
    private final DataTable<Drug> table;
    private final PillButton restock;

    public PharmacyScreen(AppContext app) {
        super(app, ClinicStore.Topic.PHARMACY);
        LocalDate today = app.store().today();
        table = new DataTable<>(List.of(
                col("Medicine", d -> new DataTable.Person(d.name(), d.form() + (d.allergyClass() == null ? "" : " · " + d.allergyClass() + " class")), 260, DataTable.Kind.ITEM),
                col("In stock", d -> d.stock(), 80, DataTable.Kind.RIGHT),
                col("Reorder at", d -> d.reorderLevel(), 90, DataTable.Kind.RIGHT),
                col("Stock", d -> d.stock() == 0 ? new DataTable.Pill("Out", StatusPill.Kind.CRITICAL)
                        : d.lowStock() ? new DataTable.Pill("Low", StatusPill.Kind.WARNING)
                        : new DataTable.Pill("OK", StatusPill.Kind.OK), 90, DataTable.Kind.PILL),
                col("Expiry", d -> d.expired(today) ? new DataTable.Pill("Expired " + d.expiry().format(Format.SHORT_DATE), StatusPill.Kind.CRITICAL)
                        : d.expiringSoon(today) ? new DataTable.Pill(ChronoUnit.DAYS.between(today, d.expiry()) + " days left", StatusPill.Kind.WARNING)
                        : new DataTable.Pill(d.expiry().format(Format.DATE), StatusPill.Kind.IDLE), 150, DataTable.Kind.PILL),
                col("Unit price", d -> Format.cedis(d.unitPrice()), 110, DataTable.Kind.RIGHT),
                col("NHIS", d -> d.nhisCovered() ? new DataTable.Pill("Covered", StatusPill.Kind.INFO) : null, 100, DataTable.Kind.PILL)));
        table.onOpen(d -> {
            if (app.canEdit(Screen.PHARMACY)) {
                Sheets.dispense(app, d.id(), null);
            }
        });
        restock = secondaryButton("Receive stock", TablerIcon.PACKAGE, () -> table.selected().ifPresentOrElse(
                d -> Sheets.restock(app, d), () -> app.toast("Select a medicine first.", com.medipulse.ui.Toast.Kind.INFO)));
        restock.setVisible(app.canEdit(Screen.PHARMACY));

        kpis.setOpaque(false);
        JPanel top = vertical();
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(restock);
        top.add(header("Pharmacy", "Allergy, stock and expiry are checked before every dispense", actions));
        top.add(gap(14));
        top.add(kpis);
        top.add(gap(14));
        top.add(chips);
        kpis.setAlignmentX(LEFT_ALIGNMENT);
        chips.setAlignmentX(LEFT_ALIGNMENT);
        top.getComponent(0).setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 80));
        ((javax.swing.JComponent) top.getComponent(0)).setAlignmentX(LEFT_ALIGNMENT);
        add(top, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        chips.onChange(i -> refresh());
    }

    @Override
    public PillButton primaryAction() {
        return app.canEdit(Screen.PHARMACY) ? primary("Dispense", TablerIcon.PILL, () -> Sheets.dispense(app, null, null)) : null;
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        LocalDate today = s.today();
        List<Drug> drugs = s.drugs();
        List<Predicate<Drug>> filters = List.of(d -> true, Drug::lowStock, d -> d.expiringSoon(today), d -> d.expired(today), Drug::nhisCovered);
        for (int i = 1; i < filters.size(); i++) {
            chips.setCount(i, (int) drugs.stream().filter(filters.get(i)).count());
        }
        table.setRows(drugs.stream().filter(filters.get(chips.selected())).toList(), "Nothing in this list.");

        List<Dispense> todays = s.dispensesToday();
        long overrides = todays.stream().filter(Dispense::allergyOverride).count();
        long low = drugs.stream().filter(Drug::lowStock).count();
        long soon = drugs.stream().filter(d -> d.expiringSoon(today)).count();
        long expired = drugs.stream().filter(d -> d.expired(today)).count();
        kpis.removeAll();
        kpis.add(kpi(TablerIcon.PACKAGE, Theme.ORANGE, "Low stock", String.valueOf(low), "At or below reorder level",
                low > 0 ? StatusPill.Kind.WARNING : StatusPill.Kind.OK));
        kpis.add(kpi(TablerIcon.CALENDAR_TIME, Theme.ORANGE, "Expiring soon", String.valueOf(soon), "Within 90 days: use first", null));
        kpis.add(kpi(TablerIcon.ALERT_OCTAGON, Theme.RED, "Expired", String.valueOf(expired),
                expired > 0 ? "Quarantine; blocked from dispensing" : "None", expired > 0 ? StatusPill.Kind.CRITICAL : StatusPill.Kind.OK));
        kpis.add(kpi(TablerIcon.PILL, Theme.GREEN, "Dispensed today", String.valueOf(todays.size()),
                overrides > 0 ? overrides + " allergy override" + (overrides == 1 ? "" : "s") : "No allergy overrides",
                overrides > 0 ? StatusPill.Kind.WARNING : null));
        kpis.revalidate();
        kpis.repaint();
    }
}
