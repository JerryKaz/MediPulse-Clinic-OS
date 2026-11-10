package com.ayaresa.screens;

import com.ayaresa.clinical.News2;
import com.ayaresa.domain.Appointment;
import com.ayaresa.domain.Dispense;
import com.ayaresa.domain.Invoice;
import com.ayaresa.domain.Patient;
import com.ayaresa.domain.QueueEntry;
import com.ayaresa.domain.Stage;
import com.ayaresa.domain.VitalSigns;
import com.ayaresa.security.Screen;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Avatar;
import com.ayaresa.ui.Drawer;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.StatusPill;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The patient chart: a drawer with demographics, safety banners (allergies, NHIS), latest
 * observations and a single timeline of everything that happened to this patient. It
 * refreshes itself while open if the record changes.
 */
public final class PatientChart {

    private record Event(LocalDateTime at, TablerIcon icon, Color tint, String title, String detail) {
    }

    private PatientChart() {
    }

    public static void open(AppContext app, String patientId) {
        ClinicStore store = app.store();
        if (store.patient(patientId).isEmpty()) {
            app.toast("Patient not found.", com.ayaresa.ui.Toast.Kind.ERROR);
            return;
        }
        JPanel body = new JPanel(new BorderLayout());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        Runnable fill = () -> {
            body.removeAll();
            footer.removeAll();
            store.patient(patientId).ifPresent(p -> {
                body.add(content(app, p), BorderLayout.NORTH);
                actions(app, p, footer);
            });
            body.revalidate();
            body.repaint();
            footer.revalidate();
            footer.repaint();
        };
        fill.run();
        Drawer drawer = Drawer.open(app.window(), "PATIENT CHART", body, footer);
        AutoCloseable sub = store.subscribe(fill, ClinicStore.Topic.values());
        drawer.onClose(() -> {
            try {
                sub.close();
            } catch (Exception ignored) {
                // unsubscribing can't fail
            }
        });
    }

    private static JComponent content(AppContext app, Patient p) {
        ClinicStore store = app.store();
        LocalDate today = store.today();
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));

        // Identity
        JPanel id = new JPanel(new BorderLayout(14, 0));
        id.setOpaque(false);
        id.add(new Avatar(Avatar.initialsOf(p.name()), p.name(), 56), BorderLayout.WEST);
        JPanel lines = new JPanel(new GridLayout(2, 1));
        lines.setOpaque(false);
        JLabel name = new JLabel(p.name());
        name.setFont(Theme.title());
        name.setForeground(Theme.LABEL);
        String sexAge = (p.sex() == null ? "" : p.sex().name().charAt(0) + p.sex().name().substring(1).toLowerCase())
                + " · " + p.age(today) + " years" + (p.bloodGroup() == null ? "" : " · " + p.bloodGroup());
        JLabel sub = new JLabel(p.id() + " · " + sexAge);
        sub.setFont(Theme.body());
        sub.setForeground(Theme.SECONDARY_LABEL);
        lines.add(name);
        lines.add(sub);
        id.add(lines, BorderLayout.CENTER);
        add(col, id, 14);

        // Safety banners
        if (!p.allergies().isEmpty()) {
            add(col, banner(TablerIcon.ALERT_OCTAGON, Theme.CRIT_BG, Theme.CRIT_FG,
                    "Allergies: " + String.join(", ", p.allergies())), 8);
        } else {
            add(col, banner(TablerIcon.SHIELD_CHECK, Theme.OK_BG, Theme.OK_FG, "No known allergies"), 8);
        }
        if (p.nhisActive(today)) {
            add(col, banner(TablerIcon.ID, Theme.INFO_BG, Theme.INFO_FG,
                    "NHIS " + p.nhisNumber() + " · valid until " + p.nhisExpiry().format(Format.DATE)), 14);
        } else if (p.hasNhis()) {
            add(col, banner(TablerIcon.ID, Theme.WARN_BG, Theme.WARN_FG,
                    "NHIS " + p.nhisNumber() + " expired " + p.nhisExpiry().format(Format.DATE) + ". Bill as cash or renew (*929#)."), 14);
        } else {
            add(col, banner(TablerIcon.ID, Theme.IDLE_BG, Theme.IDLE_FG, "No NHIS membership on file"), 14);
        }

        // Today
        Optional<QueueEntry> visit = store.activeVisit(p.id());
        store.bedOf(p.id()).ifPresent(b -> add(col, fact(TablerIcon.BED, "Admitted", "Bed " + b.id() + " · " + b.ward()), 4));
        visit.ifPresent(v -> add(col, fact(TablerIcon.USERS_GROUP, "In today's queue",
                v.tokenLabel() + " · " + v.stage().label() + " · " + v.priority().label()), 4));
        add(col, fact(TablerIcon.PHONE, "Phone", p.phone() == null ? "Not recorded" : p.phone()), 4);
        add(col, fact(TablerIcon.ID, "Ghana Card", p.ghanaCard() == null ? "Not recorded" : p.ghanaCard()), 4);
        add(col, fact(TablerIcon.CLIPBOARD_HEART, "Conditions",
                p.conditions().isEmpty() ? "None recorded" : String.join(", ", p.conditions())), 16);

        // Latest vitals
        if (app.user().canView(Screen.VITALS)) {
            Optional<VitalSigns> latest = store.latestVitals(p.id());
            col.add(left(ScreenView.sectionTitle("Latest observations")));
            col.add(Box.createVerticalStrut(8));
            if (latest.isPresent()) {
                VitalSigns v = latest.get();
                News2.Result r = News2.score(v);
                JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                head.setOpaque(false);
                head.add(new StatusPill("NEWS2 " + r.total() + " · " + r.risk().label() + " risk", Format.kind(r.risk())));
                JLabel when = new JLabel("   " + Format.relativeDay(v.takenAt(), today));
                when.setFont(Theme.footnote());
                when.setForeground(Theme.TERTIARY_LABEL);
                head.add(when);
                add(col, head, 8);
                JPanel g = new JPanel(new GridLayout(2, 3, 8, 8));
                g.setOpaque(false);
                g.add(vital("BP", v.bloodPressure(), "mmHg"));
                g.add(vital("Pulse", String.valueOf(v.pulse()), "bpm"));
                g.add(vital("Temp", String.valueOf(v.temperature()), "°C"));
                g.add(vital("SpO₂", v.spo2() + "%", v.onOxygen() ? "on O₂" : "air"));
                g.add(vital("Resp.", String.valueOf(v.respirationRate()), "/min"));
                g.add(vital("ACVPU", v.consciousness().label().split(" ")[0], ""));
                add(col, g, 18);
            } else {
                add(col, ScreenView.secondary("No observations recorded yet."), 18);
            }
        }

        // Timeline
        col.add(left(ScreenView.sectionTitle("Timeline")));
        col.add(Box.createVerticalStrut(6));
        List<Event> events = timeline(app, p);
        if (events.isEmpty()) {
            col.add(left(ScreenView.secondary("Nothing yet.")));
        }
        for (Event e : events.stream().limit(25).toList()) {
            col.add(left(eventRow(e, today)));
        }
        return col;
    }

    private static List<Event> timeline(AppContext app, Patient p) {
        ClinicStore store = app.store();
        List<Event> out = new ArrayList<>();
        out.add(new Event(p.registeredAt(), TablerIcon.USER_PLUS, Theme.BLUE, "Registered", p.id()));
        for (QueueEntry q : store.visitsFor(p.id())) {
            out.add(new Event(q.arrivedAt(), TablerIcon.DOOR_ENTER, Theme.INDIGO, "Visit " + q.tokenLabel(),
                    q.complaint() + (q.stage() == Stage.DONE ? "" : " · now " + q.stage().label().toLowerCase())));
        }
        if (app.user().canView(Screen.VITALS)) {
            for (VitalSigns v : store.vitals(p.id())) {
                News2.Result r = News2.score(v);
                out.add(new Event(v.takenAt(), TablerIcon.ACTIVITY_HEARTBEAT, Format.kind(r.risk()).foreground(),
                        "Vitals · NEWS2 " + r.total(), "BP " + v.bloodPressure() + ", pulse " + v.pulse() + ", " + v.temperature() + "°C"));
            }
        }
        for (Dispense d : store.dispensesFor(p.id())) {
            String drug = store.drug(d.drugId()).map(x -> x.name()).orElse(d.drugId());
            out.add(new Event(d.at(), TablerIcon.PILL, Theme.GREEN, "Dispensed " + d.quantity() + " × " + drug,
                    d.allergyOverride() ? "Allergy warning overridden by " + d.by() : "By " + d.by()));
        }
        for (Appointment a : store.appointmentsFor(p.id())) {
            String who = store.staffMember(a.staffId()).map(s -> s.name()).orElse("");
            out.add(new Event(a.start(), TablerIcon.CALENDAR_EVENT, Theme.PINK, "Appointment · " + a.status().label(),
                    a.reason() + " with " + who));
        }
        if (app.user().canView(Screen.BILLING)) {
            for (Invoice i : store.invoicesFor(p.id())) {
                out.add(new Event(i.createdAt(), TablerIcon.RECEIPT, Theme.ORANGE, i.id() + " · " + i.status().label(),
                        Format.cedis(i.total()) + " · " + i.payer().label()));
            }
        }
        out.removeIf(e -> e.at() == null);
        out.sort(Comparator.comparing(Event::at).reversed());
        return out;
    }

    private static void actions(AppContext app, Patient p, JPanel footer) {
        boolean inQueue = app.store().activeVisit(p.id()).isPresent();
        if (app.canEdit(Screen.QUEUE) && !inQueue) {
            footer.add(button("Check in", TablerIcon.DOOR_ENTER, true, () -> Sheets.checkIn(app, p.id())));
        }
        if (app.canEdit(Screen.VITALS)) {
            footer.add(button("Record vitals", TablerIcon.ACTIVITY_HEARTBEAT, inQueue || !app.canEdit(Screen.QUEUE),
                    () -> Sheets.vitals(app, p.id())));
        }
        if (app.canEdit(Screen.APPOINTMENTS)) {
            footer.add(button("Book", TablerIcon.CALENDAR_PLUS, false, () -> Sheets.book(app, p.id())));
        }
        if (app.canEdit(Screen.PHARMACY)) {
            footer.add(button("Dispense", TablerIcon.PILL, footer.getComponentCount() == 0, () -> Sheets.dispense(app, null, p.id())));
        }
        if (app.canEdit(Screen.PATIENTS)) {
            footer.add(button("Edit", TablerIcon.PENCIL, false, () -> Sheets.editPatient(app, p)));
        }
    }

    private static PillButton button(String text, TablerIcon icon, boolean primary, Runnable r) {
        PillButton b = new PillButton(text, Icons.of(icon, 16, primary ? Color.WHITE : Theme.LABEL),
                primary ? PillButton.Style.PRIMARY : PillButton.Style.SECONDARY);
        b.setFont(Theme.font(primary ? Font.BOLD : Font.PLAIN, 14));
        b.addActionListener(e -> r.run());
        return b;
    }

    private static JComponent banner(TablerIcon icon, Color bg, Color fg, String text) {
        JLabel l = new JLabel("<html><div style='width:330px'>" + text + "</div></html>", Icons.of(icon, 18, fg), JLabel.LEFT) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setIconTextGap(10);
        l.setFont(Theme.bodyBold());
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return l;
    }

    private static JComponent fact(TablerIcon icon, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 0));
        JLabel l = new JLabel(label, Icons.of(icon, 16, Theme.TERTIARY_LABEL), JLabel.LEFT);
        l.setIconTextGap(8);
        l.setFont(Theme.body());
        l.setForeground(Theme.SECONDARY_LABEL);
        JLabel v = new JLabel(value.length() > 34 ? "<html><div style='text-align:right;width:230px'>" + value + "</div></html>" : value);
        v.setFont(Theme.body());
        v.setForeground(Theme.LABEL);
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private static JComponent vital(String label, String value, String unit) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.FILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JLabel l = new JLabel(label);
        l.setFont(Theme.caption());
        l.setForeground(Theme.TERTIARY_LABEL);
        JLabel v = new JLabel(value + (unit.isEmpty() ? "" : "  "));
        v.setFont(Theme.font(Font.BOLD, 17));
        v.setForeground(Theme.LABEL);
        JLabel u = new JLabel(unit);
        u.setFont(Theme.footnote());
        u.setForeground(Theme.SECONDARY_LABEL);
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        line.setOpaque(false);
        line.add(v);
        line.add(u);
        p.add(l, BorderLayout.NORTH);
        p.add(line, BorderLayout.CENTER);
        return p;
    }

    private static JComponent eventRow(Event e, LocalDate today) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR),
                BorderFactory.createEmptyBorder(9, 0, 9, 0)));
        row.add(new JLabel(Icons.of(e.icon(), 18, e.tint())), BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        JLabel t = new JLabel(e.title());
        t.setFont(Theme.bodyBold());
        t.setForeground(Theme.LABEL);
        JLabel d = new JLabel(e.detail());
        d.setFont(Theme.footnote());
        d.setForeground(Theme.SECONDARY_LABEL);
        text.add(t);
        text.add(d);
        row.add(text, BorderLayout.CENTER);
        JLabel when = new JLabel(Format.relativeDay(e.at(), today));
        when.setFont(Theme.footnote());
        when.setForeground(Theme.TERTIARY_LABEL);
        row.add(when, BorderLayout.EAST);
        return row;
    }

    private static void add(JPanel col, JComponent c, int gapAfter) {
        col.add(left(c));
        col.add(Box.createVerticalStrut(gapAfter));
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }
}
