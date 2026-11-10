package com.medipulse.screens;

import com.medipulse.domain.Appointment;
import com.medipulse.domain.AppointmentStatus;
import com.medipulse.domain.Staff;
import com.medipulse.security.Screen;
import com.medipulse.shell.AppContext;
import com.medipulse.store.ClinicStore;
import com.medipulse.ui.Card;
import com.medipulse.ui.Icons;
import com.medipulse.ui.PillButton;
import com.medipulse.ui.StatusPill;
import com.medipulse.ui.TablerIcon;
import com.medipulse.ui.Theme;
import com.medipulse.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

/** Day agenda with one column per clinician. Click an appointment to check in, complete or cancel it. */
public final class AppointmentsScreen extends ScreenView {

    private LocalDate day;
    private final JPanel head = new JPanel(new BorderLayout());
    private final JPanel columns = new JPanel(new GridLayout(1, 0, 12, 0));

    public AppointmentsScreen(AppContext app) {
        super(app, ClinicStore.Topic.APPOINTMENTS, ClinicStore.Topic.STAFF, ClinicStore.Topic.PATIENTS);
        day = app.store().today();
        head.setOpaque(false);
        columns.setOpaque(false);
        add(head, BorderLayout.NORTH);
        add(columns, BorderLayout.CENTER);
    }

    @Override
    public PillButton primaryAction() {
        return app.canEdit(Screen.APPOINTMENTS) ? primary("Book", TablerIcon.CALENDAR_PLUS, () -> Sheets.book(app, null)) : null;
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        List<Appointment> appts = s.appointmentsOn(day);
        String title = day.equals(s.today()) ? "Today" : day.equals(s.today().plusDays(1)) ? "Tomorrow" : day.format(Format.DATE);
        long noShows = appts.stream().filter(a -> a.status() == AppointmentStatus.NO_SHOW).count();
        head.removeAll();
        head.add(header("Appointments", title + "  ·  " + appts.size() + " booked  ·  " + noShows + " no-shows",
                secondaryButton("", TablerIcon.ARROW_LEFT, () -> move(-1)),
                secondaryButton("Today", null, () -> {
                    day = s.today();
                    refresh();
                }),
                secondaryButton("", TablerIcon.ARROW_RIGHT, () -> move(1))));
        head.revalidate();

        columns.removeAll();
        for (Staff doc : s.staff().stream().filter(x -> x.name().startsWith("Dr.")).toList()) {
            columns.add(column(doc, appts.stream().filter(a -> a.staffId().equals(doc.id())).toList()));
        }
        columns.revalidate();
        columns.repaint();
    }

    private void move(int days) {
        day = day.plusDays(days);
        refresh();
    }

    private JComponent column(Staff doc, List<Appointment> list) {
        JPanel col = new JPanel(new BorderLayout(0, 10));
        col.setOpaque(false);
        JPanel title = vertical();
        JLabel n = sectionTitle(doc.name());
        title.add(n);
        JLabel sp = secondary(doc.specialty() + (doc.onDuty() ? "  ·  on duty" : "  ·  off duty"));
        title.add(sp);
        col.add(title, BorderLayout.NORTH);

        JPanel items = vertical();
        if (list.isEmpty()) {
            items.add(secondary("No appointments"));
        }
        for (Appointment a : list) {
            JComponent c = card(a);
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            items.add(c);
            items.add(Box.createVerticalStrut(10));
        }
        col.add(com.medipulse.ui.VerticalScroll.of(items), BorderLayout.CENTER);
        return col;
    }

    private JComponent card(Appointment a) {
        ClinicStore s = app.store();
        String name = s.patient(a.patientId()).map(p -> p.name()).orElse(a.patientId());
        Card card = new Card(new BorderLayout(12, 0), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel time = vertical();
        JLabel start = new JLabel(a.start().format(Format.TIME));
        start.setFont(Theme.font(Font.BOLD, 16));
        start.setForeground(Theme.LABEL);
        time.add(start);
        time.add(secondary(a.minutes() + " min"));
        card.add(time, BorderLayout.WEST);
        JPanel text = vertical();
        JLabel nm = new JLabel(name);
        nm.setFont(Theme.bodyBold());
        nm.setForeground(Theme.LABEL);
        text.add(nm);
        text.add(secondary(a.reason()));
        card.add(text, BorderLayout.CENTER);
        JPanel pill = new JPanel(new java.awt.GridBagLayout());
        pill.setOpaque(false);
        pill.add(new StatusPill(a.status().label(), Format.kind(a.status())));
        card.add(pill, BorderLayout.EAST);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                menu(a, name).show(card, e.getX(), e.getY());
            }
        });
        return card;
    }

    private JPopupMenu menu(Appointment a, String name) {
        JPopupMenu m = new JPopupMenu();
        m.add(item("Open chart", TablerIcon.USER, () -> app.openPatient(a.patientId())));
        if (app.canEdit(Screen.APPOINTMENTS)) {
            m.addSeparator();
            status(m, a, AppointmentStatus.CHECKED_IN, "Check in (adds to queue)", TablerIcon.DOOR_ENTER, name);
            status(m, a, AppointmentStatus.COMPLETED, "Mark completed", TablerIcon.CIRCLE_CHECK, name);
            status(m, a, AppointmentStatus.NO_SHOW, "Mark no-show", TablerIcon.CLOCK, name);
            status(m, a, AppointmentStatus.CANCELLED, "Cancel appointment", TablerIcon.X, name);
        }
        return m;
    }

    private void status(JPopupMenu m, Appointment a, AppointmentStatus s, String label, TablerIcon icon, String name) {
        if (a.status() != s) {
            m.add(item(label, icon, () -> app.attempt(() -> {
                app.store().setAppointmentStatus(a.id(), s);
                app.toast(name + ": " + s.label().toLowerCase() + ".", Toast.Kind.SUCCESS);
            })));
        }
    }

    private static JMenuItem item(String text, TablerIcon icon, Runnable r) {
        JMenuItem i = new JMenuItem(text, Icons.of(icon, 16, Theme.SECONDARY_LABEL));
        i.setFont(Theme.body());
        i.addActionListener(e -> r.run());
        return i;
    }
}
