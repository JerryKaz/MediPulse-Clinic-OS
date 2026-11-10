package com.ayaresa.screens;

import com.ayaresa.clinical.News2;
import com.ayaresa.domain.Appointment;
import com.ayaresa.domain.AppointmentStatus;
import com.ayaresa.domain.Bed;
import com.ayaresa.domain.Drug;
import com.ayaresa.domain.InvoiceStatus;
import com.ayaresa.domain.Priority;
import com.ayaresa.domain.QueueEntry;
import com.ayaresa.domain.Stage;
import com.ayaresa.domain.VitalSigns;
import com.ayaresa.security.Screen;
import com.ayaresa.shell.AppContext;
import com.ayaresa.shell.ScreenStyle;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Avatar;
import com.ayaresa.ui.BarChart;
import com.ayaresa.ui.Card;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.StatusPill;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * "Today": a live, role-aware overview. Every tile, list and action is chosen from what the
 * signed-in role can see, and it updates as soon as anyone changes the data.
 */
public final class TodayScreen extends ScreenView {

    private final Page page = new Page();

    public TodayScreen(AppContext app) {
        super(app, ClinicStore.Topic.values());
        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        every(60_000, this::refresh);
    }

    @Override
    public PillButton primaryAction() {
        if (app.canEdit(Screen.QUEUE)) {
            return primary("Check in", TablerIcon.DOOR_ENTER, () -> Sheets.checkIn(app, null));
        }
        if (app.canEdit(Screen.PHARMACY)) {
            return primary("Dispense", TablerIcon.PILL, () -> Sheets.dispense(app, null, null));
        }
        return null;
    }

    @Override
    protected void refresh() {
        page.removeAll();
        page.add(header(greeting(LocalTime.now()) + ", " + app.user().firstName(), summary()));

        JPanel kpis = new JPanel(new GridLayout(1, 0, Theme.GAP, 0));
        kpis.setOpaque(false);
        tiles().stream().limit(4).forEach(kpis::add);
        page.add(kpis);

        List<JComponent> middle = new ArrayList<>();
        if (app.user().canView(Screen.QUEUE)) {
            middle.add(flowChart());
        }
        middle.add(attentionCard());
        page.add(row(middle));

        List<JComponent> bottom = new ArrayList<>();
        if (app.user().canView(Screen.APPOINTMENTS)) {
            bottom.add(upNextCard());
        }
        JComponent quick = quickActions();
        if (quick != null) {
            bottom.add(quick);
        }
        if (!bottom.isEmpty()) {
            page.add(row(bottom));
        }
        page.revalidate();
        page.repaint();
    }

    static String greeting(LocalTime now) {
        int h = now.getHour();
        return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
    }

    private String summary() {
        ClinicStore s = app.store();
        List<String> parts = new ArrayList<>();
        parts.add(app.user().role().label());
        if (app.user().canView(Screen.QUEUE)) {
            parts.add(s.queue().stream().filter(q -> q.stage() != Stage.DONE).count() + " patients in the clinic");
        }
        if (app.user().canView(Screen.APPOINTMENTS)) {
            long left = s.appointmentsOn(s.today()).stream().filter(a -> a.status() == AppointmentStatus.SCHEDULED).count();
            parts.add(left + " appointments to come");
        }
        return String.join("  ·  ", parts);
    }

    // ---- KPI tiles -------------------------------------------------------------------

    private List<JComponent> tiles() {
        ClinicStore s = app.store();
        LocalDate today = s.today();
        List<JComponent> out = new ArrayList<>();
        List<QueueEntry> queue = s.queue();
        if (app.user().canView(Screen.QUEUE)) {
            List<QueueEntry> active = queue.stream().filter(q -> q.stage() != Stage.DONE).toList();
            long avg = active.isEmpty() ? 0 : Math.round(active.stream()
                    .mapToLong(q -> Duration.between(q.arrivedAt(), s.now()).toMinutes()).average().orElse(0));
            out.add(tile(Screen.QUEUE, kpi(TablerIcon.USERS_GROUP, Theme.INDIGO, "In the clinic now", String.valueOf(active.size()),
                    "Average time here " + avg + " min", avg > 60 ? StatusPill.Kind.WARNING : null)));
            int seenToday = (int) queue.stream().filter(q -> q.stage() == Stage.DONE).count();
            List<Integer> perDay = s.visitsPerDay(2);
            int yesterday = perDay.get(0);
            out.add(tile(Screen.QUEUE, kpi(TablerIcon.USER_CHECK, Theme.GREEN, "Visits today", String.valueOf(queue.size()),
                    seenToday + " completed · " + yesterday + " yesterday", null)));
        }
        if (app.user().canView(Screen.VITALS)) {
            long high = highNews(s).size();
            out.add(tile(Screen.VITALS, kpi(TablerIcon.ACTIVITY_HEARTBEAT, Theme.PINK, "NEWS2 alerts (12 h)", String.valueOf(high),
                    high == 0 ? "No medium or high scores" : "Need clinical review", high > 0 ? StatusPill.Kind.CRITICAL : StatusPill.Kind.OK)));
        }
        if (app.user().canView(Screen.BEDS)) {
            List<Bed> beds = s.beds();
            long used = beds.stream().filter(b -> !b.isFree()).count();
            out.add(tile(Screen.BEDS, kpi(TablerIcon.BED, Theme.TEAL, "Bed occupancy",
                    Math.round(100.0 * used / Math.max(1, beds.size())) + "%", (beds.size() - used) + " beds free", null)));
        }
        if (app.user().canView(Screen.PHARMACY)) {
            List<Drug> drugs = s.drugs();
            long low = drugs.stream().filter(Drug::lowStock).count();
            long expired = drugs.stream().filter(d -> d.expired(today)).count();
            out.add(tile(Screen.PHARMACY, kpi(TablerIcon.PILL, Theme.GREEN, "Stock alerts", String.valueOf(low + expired),
                    low + " low · " + expired + " expired", expired > 0 ? StatusPill.Kind.CRITICAL : low > 0 ? StatusPill.Kind.WARNING : StatusPill.Kind.OK)));
        }
        if (app.user().canView(Screen.BILLING)) {
            out.add(tile(Screen.BILLING, kpi(TablerIcon.CASH, Theme.ORANGE, "Collected today", Format.cedis(s.revenue(today, today)),
                    Format.cedis(s.outstanding()) + " outstanding", null)));
        }
        if (app.user().canView(Screen.APPOINTMENTS)) {
            List<Appointment> appts = s.appointmentsOn(today);
            long left = appts.stream().filter(a -> a.status() == AppointmentStatus.SCHEDULED).count();
            out.add(tile(Screen.APPOINTMENTS, kpi(TablerIcon.CALENDAR_EVENT, Theme.PINK, "Appointments", String.valueOf(appts.size()),
                    left + " still to come", null)));
        }
        return out;
    }

    private JComponent tile(Screen target, Card card) {
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Open " + target.title());
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                app.open(target);
            }
        });
        return card;
    }

    // ---- Cards -----------------------------------------------------------------------

    private JComponent flowChart() {
        Card card = new Card(new BorderLayout(0, 10));
        card.add(cardHead("Patient visits", "Last 14 days", Screen.QUEUE), BorderLayout.NORTH);
        BarChart chart = new BarChart(Theme.INDIGO);
        List<Integer> perDay = app.store().visitsPerDay(14);
        LocalDate today = app.store().today();
        List<BarChart.Bar> bars = new ArrayList<>();
        for (int i = 0; i < perDay.size(); i++) {
            LocalDate d = today.minusDays(perDay.size() - 1L - i);
            String label = i == perDay.size() - 1 ? "Today" : d.getDayOfWeek().name().substring(0, 1);
            bars.add(new BarChart.Bar(label, perDay.get(i), d.format(Format.SHORT_DATE) + ": " + perDay.get(i) + " visits"));
        }
        chart.setBars(bars);
        chart.setPreferredSize(new Dimension(400, 200));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private record Attention(TablerIcon icon, Color tint, String title, String detail, StatusPill pill, Runnable open) {
    }

    private JComponent attentionCard() {
        Card card = new Card(new BorderLayout(0, 6));
        card.add(cardHead("Needs attention", null, null), BorderLayout.NORTH);
        ClinicStore s = app.store();
        LocalDate today = s.today();
        List<Attention> items = new ArrayList<>();
        if (app.user().canView(Screen.VITALS)) {
            for (VitalSigns v : highNews(s)) {
                News2.Result r = News2.score(v);
                String name = s.patient(v.patientId()).map(p -> p.name()).orElse(v.patientId());
                items.add(new Attention(TablerIcon.ALERT_OCTAGON, Theme.RED, name,
                        r.risk().response().split(";")[0].split(":")[0], new StatusPill("NEWS2 " + r.total(), Format.kind(r.risk())),
                        () -> app.openPatient(v.patientId())));
            }
        }
        if (app.user().canView(Screen.QUEUE)) {
            s.queue().stream().filter(q -> q.stage() != Stage.DONE && q.priority() != Priority.ROUTINE)
                    .forEach(q -> items.add(new Attention(q.priority() == Priority.EMERGENCY ? TablerIcon.AMBULANCE : TablerIcon.CLOCK,
                            q.priority() == Priority.EMERGENCY ? Theme.RED : Theme.ORANGE,
                            s.patient(q.patientId()).map(p -> p.name()).orElse("") + " · " + q.tokenLabel(),
                            q.complaint() + " · " + q.stage().label().toLowerCase(),
                            new StatusPill(q.priority().label(), Format.kind(q.priority())), () -> app.open(Screen.QUEUE))));
        }
        if (app.user().canView(Screen.PHARMACY)) {
            s.drugs().stream().filter(d -> d.expired(today) || d.stock() * 3 < d.reorderLevel()).forEach(d -> items.add(new Attention(
                    TablerIcon.PACKAGE, Theme.ORANGE, d.name(), d.expired(today) ? "Expired " + d.expiry().format(Format.DATE) : "Only " + d.stock() + " left",
                    new StatusPill(d.expired(today) ? "Expired" : "Reorder", d.expired(today) ? StatusPill.Kind.CRITICAL : StatusPill.Kind.WARNING),
                    () -> app.open(Screen.PHARMACY))));
        }
        if (app.user().canView(Screen.BILLING)) {
            s.invoices().stream().filter(i -> i.status() == InvoiceStatus.CLAIM_REJECTED).forEach(i -> items.add(new Attention(
                    TablerIcon.FILE_INVOICE, Theme.ORANGE, "NHIS claim rejected · " + i.id(),
                    s.patient(i.patientId()).map(p -> p.name()).orElse("") + " · " + Format.cedis(i.total()),
                    new StatusPill("Rejected", StatusPill.Kind.CRITICAL), () -> app.open(Screen.BILLING))));
        }
        JPanel list = vertical();
        if (items.isEmpty()) {
            JLabel ok = new JLabel("Nothing needs attention right now.", Icons.of(TablerIcon.CIRCLE_CHECK, 18, Theme.GREEN), JLabel.LEFT);
            ok.setFont(Theme.body());
            ok.setForeground(Theme.SECONDARY_LABEL);
            ok.setIconTextGap(8);
            ok.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
            list.add(ok);
        }
        for (int i = 0; i < Math.min(5, items.size()); i++) {
            Attention a = items.get(i);
            list.add(listRow(new JLabel(Icons.of(a.icon(), 20, a.tint())), a.title(), a.detail(), a.pill(), a.open(), i < Math.min(5, items.size()) - 1));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JComponent upNextCard() {
        Card card = new Card(new BorderLayout(0, 6));
        card.add(cardHead("Up next", "Appointments", Screen.APPOINTMENTS), BorderLayout.NORTH);
        ClinicStore s = app.store();
        List<Appointment> next = s.appointmentsOn(s.today()).stream()
                .filter(a -> a.status() == AppointmentStatus.SCHEDULED || a.status() == AppointmentStatus.CHECKED_IN)
                .limit(5).toList();
        JPanel list = vertical();
        if (next.isEmpty()) {
            list.add(secondary("No more appointments today."));
        }
        for (int i = 0; i < next.size(); i++) {
            Appointment a = next.get(i);
            String name = s.patient(a.patientId()).map(p -> p.name()).orElse(a.patientId());
            String who = s.staffMember(a.staffId()).map(x -> x.name()).orElse("");
            JPanel lead = new JPanel(new BorderLayout(10, 0));
            lead.setOpaque(false);
            JLabel time = new JLabel(a.start().format(Format.TIME));
            time.setFont(Theme.font(Font.PLAIN, 13));
            time.setForeground(Theme.SECONDARY_LABEL);
            time.setPreferredSize(new Dimension(42, 20));
            lead.add(time, BorderLayout.WEST);
            lead.add(new Avatar(Avatar.initialsOf(name), name, 32), BorderLayout.CENTER);
            list.add(listRow(lead, name, a.reason() + " · " + who,
                    new StatusPill(a.status().label(), Format.kind(a.status())), () -> app.openPatient(a.patientId()), i < next.size() - 1));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private record Quick(Screen screen, String label, TablerIcon icon, Runnable run) {
    }

    private JComponent quickActions() {
        List<Quick> all = List.of(
                new Quick(Screen.QUEUE, "Check in patient", TablerIcon.DOOR_ENTER, () -> Sheets.checkIn(app, null)),
                new Quick(Screen.PATIENTS, "Register patient", TablerIcon.USER_PLUS, () -> Sheets.newPatient(app)),
                new Quick(Screen.APPOINTMENTS, "Book appointment", TablerIcon.CALENDAR_PLUS, () -> Sheets.book(app, null)),
                new Quick(Screen.VITALS, "Record vitals", TablerIcon.ACTIVITY_HEARTBEAT, () -> app.open(Screen.VITALS)),
                new Quick(Screen.PHARMACY, "Dispense", TablerIcon.PILL, () -> Sheets.dispense(app, null, null)),
                new Quick(Screen.BILLING, "Take payment", TablerIcon.CASH, () -> app.open(Screen.BILLING)));
        List<Quick> allowed = all.stream().filter(q -> app.canEdit(q.screen())).toList();
        if (allowed.isEmpty()) {
            return null;
        }
        Card card = new Card(new BorderLayout(0, 12));
        card.add(cardHead("Quick actions", "Ctrl K for everything else", null), BorderLayout.NORTH);
        JPanel g = new JPanel(new GridLayout(0, 2, 10, 10));
        g.setOpaque(false);
        for (Quick q : allowed) {
            PillButton b = new PillButton(q.label(), Icons.of(q.icon(), 18, ScreenStyle.tint(q.screen())), PillButton.Style.SECONDARY);
            b.setFont(Theme.font(Font.PLAIN, 14));
            b.setHorizontalAlignment(JButton.LEFT);
            b.addActionListener(e -> q.run().run());
            g.add(b);
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(g, BorderLayout.NORTH);
        card.add(holder, BorderLayout.CENTER);
        return card;
    }

    // ---- Helpers ---------------------------------------------------------------------

    private List<VitalSigns> highNews(ClinicStore s) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<VitalSigns> out = new ArrayList<>();
        for (VitalSigns v : s.recentVitals(60)) {
            if (v.takenAt().isAfter(s.now().minusHours(12)) && seen.add(v.patientId())) {
                News2.Risk r = News2.score(v).risk();
                if (r == News2.Risk.MEDIUM || r == News2.Risk.HIGH) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    private JComponent cardHead(String title, String subtitle, Screen link) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel t = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        t.setOpaque(false);
        t.add(sectionTitle(title));
        if (subtitle != null) {
            JLabel s = secondary("   " + subtitle);
            t.add(s);
        }
        p.add(t, BorderLayout.WEST);
        if (link != null) {
            JButton b = new JButton("See all");
            b.setFont(Theme.body());
            b.setForeground(Theme.BLUE);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.getAccessibleContext().setAccessibleName("See all " + link.title());
            b.addActionListener(e -> app.open(link));
            p.add(b, BorderLayout.EAST);
        }
        return p;
    }

    private static JComponent listRow(JComponent lead, String title, String detail, JComponent trail, Runnable open, boolean divider) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                divider ? BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR) : BorderFactory.createEmptyBorder(),
                BorderFactory.createEmptyBorder(9, 0, 9, 0)));
        row.add(lead, BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.body());
        t.setForeground(Theme.LABEL);
        JLabel d = new JLabel(detail);
        d.setFont(Theme.footnote());
        d.setForeground(Theme.TERTIARY_LABEL);
        text.add(t);
        text.add(d);
        row.add(text, BorderLayout.CENTER);
        if (trail != null) {
            JPanel c = new JPanel(new java.awt.GridBagLayout());
            c.setOpaque(false);
            c.add(trail);
            row.add(c, BorderLayout.EAST);
        }
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                open.run();
            }
        });
        return row;
    }

    private static JComponent row(List<JComponent> cells) {
        JPanel p = new JPanel(new GridLayout(1, cells.size(), Theme.GAP, 0));
        p.setOpaque(false);
        cells.forEach(p::add);
        return p;
    }

    /** Vertical page that tracks the viewport width and stacks sections with even gaps. */
    private static final class Page extends JPanel implements Scrollable {
        Page() {
            setOpaque(false);
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        }

        @Override
        public Component add(Component c) {
            if (getComponentCount() > 0) {
                super.add(javax.swing.Box.createVerticalStrut(Theme.GAP));
            }
            ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            return super.add(c);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 32; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

}
