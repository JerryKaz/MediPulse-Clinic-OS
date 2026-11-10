package upsa.clinic.panels;

import upsa.clinic.data.ClinicData;
import upsa.clinic.data.ClinicData.Appointment;
import upsa.clinic.data.ClinicData.Bed;
import upsa.clinic.data.ClinicData.Range;
import upsa.clinic.data.ClinicData.Stat;
import upsa.clinic.data.ClinicData.StockItem;
import upsa.clinic.security.Screen;
import upsa.clinic.security.User;
import upsa.clinic.shell.ScreenStyle;
import upsa.clinic.shell.Navigator;
import upsa.clinic.ui.Avatar;
import upsa.clinic.ui.Card;
import upsa.clinic.ui.Icons;
import upsa.clinic.ui.PillButton;
import upsa.clinic.ui.SegmentedControl;
import upsa.clinic.ui.StatusPill;
import upsa.clinic.ui.TablerIcon;
import upsa.clinic.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * "Today": the new role-aware dashboard. Every card, number and quick action is chosen
 * from what the signed-in role can see, so a pharmacist and a doctor get different pages.
 */
public class DashboardPanel extends JPanel implements Scrollable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_STATS = 4;

    private final User user;
    private final Navigator nav;
    private final JPanel statsRow = new JPanel(new GridLayout(1, 0, Theme.GAP, 0));

    public DashboardPanel(User user, Navigator nav) {
        this.user = user;
        this.nav = nav;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 6, 24, 6));

        add(header());
        add(Box.createVerticalStrut(16));

        SegmentedControl range = new SegmentedControl(List.of("Today", "This week", "This month"));
        range.getAccessibleContext().setAccessibleName("Time range");
        range.onChange(i -> showStats(Range.values()[i]));
        add(left(range));
        add(Box.createVerticalStrut(16));

        statsRow.setOpaque(false);
        add(left(statsRow));
        showStats(Range.TODAY);
        add(Box.createVerticalStrut(Theme.GAP));

        List<JComponent> cards = detailCards();
        if (!cards.isEmpty()) {
            JPanel row = new JPanel(new GridLayout(1, cards.size(), Theme.GAP, 0));
            row.setOpaque(false);
            cards.forEach(row::add);
            add(left(row));
            add(Box.createVerticalStrut(Theme.GAP));
        }

        JComponent actions = quickActions();
        if (actions != null) {
            add(left(actions));
        }
        add(Box.createVerticalGlue());
    }

    // ---- Header ----------------------------------------------------------------------

    private JComponent header() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(greeting(LocalTime.now()) + ", " + user.firstName());
        title.setFont(Theme.largeTitle());
        title.setForeground(Theme.LABEL);
        JLabel sub = new JLabel(summary());
        sub.setFont(Theme.body());
        sub.setForeground(Theme.SECONDARY_LABEL);
        sub.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        p.add(title);
        p.add(sub);
        return left(p);
    }

    static String greeting(LocalTime now) {
        int h = now.getHour();
        return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
    }

    private String summary() {
        List<String> parts = new ArrayList<>();
        parts.add(user.role().label() + " view");
        if (user.canView(Screen.APPOINTMENTS)) {
            long left = ClinicData.upcomingAppointments().size();
            parts.add(left + " appointments still to come");
        }
        if (user.canView(Screen.VITALS)) {
            long critical = ClinicData.upcomingAppointments().stream()
                    .filter(a -> a.status() == ClinicData.CareStatus.CRITICAL).count();
            if (critical > 0) {
                parts.add(critical + " patient needs attention");
            }
        }
        if (user.canView(Screen.PHARMACY)) {
            parts.add(ClinicData.lowStock().size() + " drugs low on stock");
        }
        return String.join("  ·  ", parts);
    }

    // ---- Stats -----------------------------------------------------------------------

    private void showStats(Range range) {
        statsRow.removeAll();
        ClinicData.stats(range).stream()
                .filter(s -> user.canView(s.module()))
                .limit(MAX_STATS)
                .forEach(s -> statsRow.add(statCard(s)));
        statsRow.revalidate();
        statsRow.repaint();
    }

    private JComponent statCard(Stat s) {
        Card card = new Card(new BorderLayout(0, 6));
        JLabel label = new JLabel(s.label(), Icons.of(ScreenStyle.icon(s.module()), 18, ScreenStyle.tint(s.module())), JLabel.LEFT);
        label.setIconTextGap(8);
        label.setFont(Theme.font(Font.PLAIN, 13));
        label.setForeground(Theme.SECONDARY_LABEL);

        JLabel value = new JLabel(s.value());
        value.setFont(Theme.font(Font.BOLD, 28));
        value.setForeground(Theme.LABEL);

        JLabel trend = new JLabel(s.trend());
        trend.setFont(Theme.footnote());
        trend.setForeground(s.good() == null ? Theme.SECONDARY_LABEL
                : s.good() ? new Color(0x248A3D) : new Color(0xD70015));
        if (s.good() != null) {
            trend.setIcon(Icons.of(s.good() ? TablerIcon.ARROW_UP_RIGHT : TablerIcon.ARROW_DOWN_RIGHT, 14, trend.getForeground()));
            trend.setIconTextGap(4);
        }

        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(trend, BorderLayout.SOUTH);
        clickable(card, s.module(), "Open " + s.module().title());
        return card;
    }

    // ---- Detail cards ----------------------------------------------------------------

    private List<JComponent> detailCards() {
        List<Supplier<JComponent>> candidates = new ArrayList<>();
        if (user.canView(Screen.APPOINTMENTS)) {
            candidates.add(this::upNextCard);
        }
        if (user.canView(Screen.BEDS)) {
            candidates.add(this::bedsCard);
        }
        if (user.canView(Screen.PHARMACY)) {
            candidates.add(this::lowStockCard);
        }
        List<JComponent> out = new ArrayList<>();
        for (int i = 0; i < Math.min(2, candidates.size()); i++) {
            out.add(candidates.get(i).get());
        }
        return out;
    }

    private JComponent upNextCard() {
        Card card = new Card(new BorderLayout(0, 6));
        card.add(cardHeader("Up next", Screen.APPOINTMENTS), BorderLayout.NORTH);
        JPanel list = vertical();
        List<Appointment> appts = ClinicData.upcomingAppointments();
        for (int i = 0; i < appts.size(); i++) {
            list.add(appointmentRow(appts.get(i), i < appts.size() - 1));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JComponent appointmentRow(Appointment a, boolean divider) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                divider ? BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR) : BorderFactory.createEmptyBorder(),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));

        JLabel time = new JLabel(a.time().format(TIME));
        time.setFont(Theme.font(Font.PLAIN, 13));
        time.setForeground(Theme.SECONDARY_LABEL);
        time.setPreferredSize(new Dimension(44, 20));

        JPanel who = new JPanel(new BorderLayout(10, 0));
        who.setOpaque(false);
        who.add(new Avatar(Avatar.initialsOf(a.patient()), a.patient(), 32), BorderLayout.WEST);
        JPanel text = vertical();
        JLabel name = new JLabel(a.patient());
        name.setFont(Theme.body());
        name.setForeground(Theme.LABEL);
        JLabel detail = new JLabel(a.reason() + " · " + a.doctor());
        detail.setFont(Theme.footnote());
        detail.setForeground(Theme.TERTIARY_LABEL);
        text.add(name);
        text.add(detail);
        who.add(text, BorderLayout.CENTER);

        JPanel west = new JPanel(new BorderLayout(10, 0));
        west.setOpaque(false);
        west.add(time, BorderLayout.WEST);
        west.add(who, BorderLayout.CENTER);

        row.add(west, BorderLayout.CENTER);
        row.add(center(careStatus(a.status())), BorderLayout.EAST);
        return row;
    }

    private static StatusPill careStatus(ClinicData.CareStatus s) {
        return switch (s) {
            case CHECKED_IN -> new StatusPill("Checked in", StatusPill.Kind.INFO);
            case WAITING -> new StatusPill("Waiting", StatusPill.Kind.WARNING);
            case STABLE -> new StatusPill("Stable", StatusPill.Kind.OK);
            case CRITICAL -> new StatusPill("Critical", StatusPill.Kind.CRITICAL);
        };
    }

    private JComponent bedsCard() {
        Card card = new Card(new BorderLayout(0, 12));
        card.add(cardHeader("Ward A beds", Screen.BEDS), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 6, 8, 8));
        grid.setOpaque(false);
        for (Bed b : ClinicData.wardA()) {
            StatusPill.Kind kind = switch (b.status()) {
                case FREE -> StatusPill.Kind.IDLE;
                case STABLE -> StatusPill.Kind.OK;
                case OBSERVATION -> StatusPill.Kind.WARNING;
                case CRITICAL -> StatusPill.Kind.CRITICAL;
            };
            grid.add(new BedTile(b.id(), kind));
        }
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createEmptyBorder(14, -8, 0, 0));
        legend.add(new StatusPill("Free", StatusPill.Kind.IDLE));
        legend.add(new StatusPill("Stable", StatusPill.Kind.OK));
        legend.add(new StatusPill("Observation", StatusPill.Kind.WARNING));
        legend.add(new StatusPill("Critical", StatusPill.Kind.CRITICAL));

        // Keep grid and legend together at the top when the card is taller than its content.
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(grid, BorderLayout.NORTH);
        top.add(legend, BorderLayout.CENTER);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(top, BorderLayout.NORTH);
        card.add(holder, BorderLayout.CENTER);
        return card;
    }

    private JComponent lowStockCard() {
        Card card = new Card(new BorderLayout(0, 6));
        card.add(cardHeader("Low stock", Screen.PHARMACY), BorderLayout.NORTH);
        JPanel list = vertical();
        List<StockItem> items = ClinicData.lowStock();
        for (int i = 0; i < items.size(); i++) {
            StockItem s = items.get(i);
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    i < items.size() - 1 ? BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR) : BorderFactory.createEmptyBorder(),
                    BorderFactory.createEmptyBorder(10, 0, 10, 0)));
            JLabel name = new JLabel(s.drug(), Icons.of(TablerIcon.PILL, 18, Theme.GREEN), JLabel.LEFT);
            name.setIconTextGap(10);
            name.setFont(Theme.body());
            row.add(name, BorderLayout.CENTER);
            boolean critical = s.unitsLeft() * 4 < s.reorderLevel();
            row.add(center(new StatusPill(s.unitsLeft() + " left",
                    critical ? StatusPill.Kind.CRITICAL : StatusPill.Kind.WARNING)), BorderLayout.EAST);
            list.add(row);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    // ---- Quick actions ---------------------------------------------------------------

    private record QuickAction(Screen module, String label, TablerIcon icon) {
    }

    private JComponent quickActions() {
        List<QuickAction> all = List.of(
                new QuickAction(Screen.PATIENTS, "Register patient", TablerIcon.USER_PLUS),
                new QuickAction(Screen.APPOINTMENTS, "Book appointment", TablerIcon.CALENDAR_PLUS),
                new QuickAction(Screen.VITALS, "Record vitals", TablerIcon.ACTIVITY_HEARTBEAT),
                new QuickAction(Screen.PHARMACY, "Dispense drugs", TablerIcon.PILL),
                new QuickAction(Screen.BILLING, "Create invoice", TablerIcon.FILE_PLUS));
        // Only actions the role can actually perform (FULL access), not just view.
        List<QuickAction> allowed = all.stream().filter(q -> user.canEdit(q.module())).toList();
        if (allowed.isEmpty()) {
            return null;
        }
        Card card = new Card(new BorderLayout(0, 12));
        JLabel title = new JLabel("Quick actions");
        title.setFont(Theme.headline());
        card.add(title, BorderLayout.NORTH);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        for (QuickAction q : allowed) {
            PillButton b = new PillButton(q.label(), Icons.of(q.icon(), 17, ScreenStyle.tint(q.module())), PillButton.Style.SECONDARY);
            b.setFont(Theme.font(Font.PLAIN, 14));
            b.addActionListener(e -> nav.open(q.module()));
            row.add(b);
        }
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    // ---- Helpers ---------------------------------------------------------------------

    private JComponent cardHeader(String title, Screen target) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.headline());
        t.setForeground(Theme.LABEL);
        p.add(t, BorderLayout.WEST);
        JButton link = new JButton("See all");
        link.setFont(Theme.body());
        link.setForeground(Theme.BLUE);
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setFocusPainted(false);
        link.setBorder(BorderFactory.createEmptyBorder());
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.getAccessibleContext().setAccessibleName("See all " + target.title());
        link.addActionListener(e -> nav.open(target));
        p.add(link, BorderLayout.EAST);
        return p;
    }

    private void clickable(JComponent c, Screen target, String tooltip) {
        c.setToolTipText(tooltip);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                nav.open(target);
            }
        });
    }

    private static JPanel vertical() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JComponent center(JComponent c) {
        JPanel p = new JPanel(new java.awt.GridBagLayout());
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    /**
     * BoxLayout children must share an alignment or they drift. Heights stay at their
     * preferred size because the panel is never stretched (it scrolls instead).
     */
    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Rounded bed tile coloured by status. */
    private static final class BedTile extends JLabel {
        private final StatusPill.Kind kind;

        BedTile(String id, StatusPill.Kind kind) {
            super(id, CENTER);
            this.kind = kind;
            setFont(Theme.font(Font.BOLD, 12));
            setForeground(kind.foreground());
            setPreferredSize(new Dimension(48, 36));
            setToolTipText("Bed " + id);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(kind.background());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---- Scrollable: fill the viewport width, scroll vertically ------------------------

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 32; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
