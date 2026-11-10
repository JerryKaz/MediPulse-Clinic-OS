package com.ayaresa.screens;

import com.ayaresa.clinical.News2;
import com.ayaresa.domain.Bed;
import com.ayaresa.domain.Patient;
import com.ayaresa.security.Screen;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Card;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.Sheet;
import com.ayaresa.ui.StatusPill;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;
import com.ayaresa.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ward map. Occupied beds are coloured by the patient's latest NEWS2 risk, so the sickest
 * patients stand out at a glance. Click a free bed to admit, an occupied one to open the chart or discharge.
 */
public final class BedsScreen extends ScreenView {

    private final JPanel wards = vertical();
    private final JPanel head = new JPanel(new BorderLayout());

    public BedsScreen(AppContext app) {
        super(app, ClinicStore.Topic.BEDS, ClinicStore.Topic.VITALS, ClinicStore.Topic.PATIENTS);
        head.setOpaque(false);
        add(head, BorderLayout.NORTH);
        add(com.ayaresa.ui.VerticalScroll.of(wards), BorderLayout.CENTER);
    }

    @Override
    protected void refresh() {
        List<Bed> beds = app.store().beds();
        long used = beds.stream().filter(b -> !b.isFree()).count();
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        legend.setOpaque(false);
        legend.add(new StatusPill("Free", StatusPill.Kind.IDLE));
        legend.add(new StatusPill("No obs yet", StatusPill.Kind.INFO));
        legend.add(new StatusPill("Low risk", StatusPill.Kind.OK));
        legend.add(new StatusPill("Medium", StatusPill.Kind.WARNING));
        legend.add(new StatusPill("High", StatusPill.Kind.CRITICAL));
        head.removeAll();
        head.add(header("Beds", used + " of " + beds.size() + " occupied  ·  colour shows the patient's latest NEWS2", legend));
        head.revalidate();

        Map<String, List<Bed>> byWard = new LinkedHashMap<>();
        beds.forEach(b -> byWard.computeIfAbsent(b.ward(), k -> new java.util.ArrayList<>()).add(b));
        wards.removeAll();
        byWard.forEach((ward, list) -> {
            Card card = new Card(new BorderLayout(0, 12));
            long u = list.stream().filter(b -> !b.isFree()).count();
            JPanel t = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            t.setOpaque(false);
            t.add(sectionTitle(ward));
            t.add(secondary("    " + u + "/" + list.size() + " occupied"));
            card.add(t, BorderLayout.NORTH);
            JPanel grid = new JPanel(new GridLayout(0, 4, 10, 10));
            grid.setOpaque(false);
            list.forEach(b -> grid.add(tile(b)));
            card.add(grid, BorderLayout.CENTER);
            card.setAlignmentX(LEFT_ALIGNMENT);
            wards.add(card);
            wards.add(gap(Theme.GAP));
        });
        wards.revalidate();
        wards.repaint();
    }

    private JComponent tile(Bed b) {
        ClinicStore s = app.store();
        StatusPill.Kind kind = StatusPill.Kind.IDLE;
        String line1 = "Free";
        String line2 = app.canEdit(Screen.BEDS) ? "Click to admit" : "";
        if (!b.isFree()) {
            Patient p = s.patient(b.patientId()).orElse(null);
            line1 = p == null ? b.patientId() : p.name();
            long days = Duration.between(b.admittedAt(), s.now()).toDays();
            line2 = (days == 0 ? "Admitted today" : "Day " + (days + 1)) + " · " + b.note();
            kind = s.latestVitals(b.patientId()).map(v -> Format.kind(News2.score(v).risk())).orElse(StatusPill.Kind.INFO);
        }
        StatusPill.Kind k = kind;
        JPanel t = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(k.background());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        t.setOpaque(false);
        t.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel id = new JLabel(b.id(), Icons.of(b.isFree() ? TablerIcon.BED : TablerIcon.BED, 18, k.foreground()), JLabel.LEFT);
        id.setFont(Theme.font(Font.BOLD, 15));
        id.setForeground(k.foreground());
        t.add(id, BorderLayout.NORTH);
        JLabel l1 = new JLabel(line1);
        l1.setFont(Theme.bodyBold());
        l1.setForeground(Theme.LABEL);
        JLabel l2 = new JLabel(line2);
        l2.setFont(Theme.footnote());
        l2.setForeground(Theme.SECONDARY_LABEL);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(l1);
        text.add(l2);
        t.add(text, BorderLayout.CENTER);
        t.setToolTipText(b.isFree() ? "Bed " + b.id() + " is free" : line1 + ", " + line2);
        t.getAccessibleContext().setAccessibleName("Bed " + b.id() + ": " + line1);
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                click(b);
            }
        });
        return t;
    }

    private void click(Bed b) {
        if (b.isFree()) {
            if (app.canEdit(Screen.BEDS)) {
                Sheets.admit(app, b);
            }
            return;
        }
        if (!app.canEdit(Screen.BEDS)) {
            app.openPatient(b.patientId());
            return;
        }
        String name = app.store().patient(b.patientId()).map(p -> p.name()).orElse(b.patientId());
        JPanel body = new JPanel();
        Sheet.open(this, "Bed " + b.id(), name + " · " + b.note(), body, 420)
                .cancel()
                .secondary("Open chart", () -> {
                    app.openPatient(b.patientId());
                    return true;
                })
                .destructive("Discharge", () -> {
                    app.store().discharge(b.id());
                    app.toast(name + " discharged from " + b.id() + ".", Toast.Kind.SUCCESS);
                    return true;
                });
    }
}
