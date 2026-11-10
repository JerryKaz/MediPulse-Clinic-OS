package com.ayaresa.screens;

import com.ayaresa.domain.Patient;
import com.ayaresa.domain.Priority;
import com.ayaresa.domain.QueueEntry;
import com.ayaresa.domain.Stage;
import com.ayaresa.security.Screen;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Card;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.StatusPill;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;
import com.ayaresa.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Duration;
import java.util.List;

/**
 * Live patient flow board. Each column is a stage of the visit; within a column patients are
 * ordered by triage priority, then by how long they have waited. Waiting times update every
 * 30 seconds and turn amber after 30 minutes and red after an hour.
 */
public final class QueueScreen extends ScreenView {

    private static final List<Stage> COLUMNS = List.of(Stage.WAITING, Stage.TRIAGE, Stage.CONSULTATION, Stage.PHARMACY, Stage.BILLING);
    private final JPanel board = new JPanel(new GridLayout(1, COLUMNS.size(), 12, 0));
    private final JPanel head = new JPanel(new BorderLayout());

    public QueueScreen(AppContext app) {
        super(app, ClinicStore.Topic.QUEUE, ClinicStore.Topic.PATIENTS, ClinicStore.Topic.VITALS);
        head.setOpaque(false);
        board.setOpaque(false);
        add(head, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        every(30_000, this::refresh);
    }

    @Override
    public PillButton primaryAction() {
        return app.canEdit(Screen.QUEUE) ? primary("Check in", TablerIcon.DOOR_ENTER, () -> Sheets.checkIn(app, null)) : null;
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        List<QueueEntry> all = s.queue();
        long done = all.stream().filter(q -> q.stage() == Stage.DONE).count();
        long active = all.size() - done;
        head.removeAll();
        head.add(header("Queue", active + " in the clinic  ·  " + done + " completed today  ·  ordered by triage priority, then waiting time"));
        head.revalidate();

        board.removeAll();
        for (Stage stage : COLUMNS) {
            board.add(column(stage, all.stream().filter(q -> q.stage() == stage).toList()));
        }
        board.revalidate();
        board.repaint();
    }

    private JComponent column(Stage stage, List<QueueEntry> entries) {
        JPanel col = new JPanel(new BorderLayout(0, 10));
        col.setOpaque(false);
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        title.setOpaque(false);
        JLabel t = new JLabel(stage.label());
        t.setFont(Theme.headline());
        t.setForeground(Theme.LABEL);
        title.add(t);
        title.add(new StatusPill(String.valueOf(entries.size()), entries.isEmpty() ? StatusPill.Kind.IDLE : StatusPill.Kind.INFO));
        col.add(title, BorderLayout.NORTH);

        JPanel list = vertical();
        if (entries.isEmpty()) {
            JLabel none = secondary("No one here");
            none.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));
            list.add(none);
        }
        for (QueueEntry q : entries) {
            JComponent card = card(q);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
            list.add(card);
            list.add(Box.createVerticalStrut(10));
        }
        col.add(com.ayaresa.ui.VerticalScroll.of(list), BorderLayout.CENTER);
        return col;
    }

    private JComponent card(QueueEntry q) {
        ClinicStore s = app.store();
        Patient p = s.patient(q.patientId()).orElse(null);
        String name = p == null ? q.patientId() : p.name();
        Duration waited = q.waitingInStage(s.now());

        Card card = new Card(new BorderLayout(0, 8), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 12));
        if (q.priority() == Priority.EMERGENCY) {
            card.fill(Theme.isDark() ? new Color(0x3A1517) : new Color(0xFFF1F0));
        }

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JLabel token = new JLabel(q.tokenLabel());
        token.setFont(Theme.font(Font.BOLD, 18));
        token.setForeground(Theme.LABEL);
        top.add(token, BorderLayout.WEST);
        top.add(new StatusPill(q.priority().label(), Format.kind(q.priority())), BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel mid = vertical();
        JLabel n = new JLabel(name);
        n.setFont(Theme.bodyBold());
        n.setForeground(Theme.LABEL);
        JLabel c = new JLabel(q.complaint());
        c.setFont(Theme.footnote());
        c.setForeground(Theme.SECONDARY_LABEL);
        mid.add(n);
        mid.add(Box.createVerticalStrut(2));
        mid.add(c);
        if (p != null && !p.allergies().isEmpty()) {
            JLabel allergy = new JLabel("Allergy: " + String.join(", ", p.allergies()), Icons.of(TablerIcon.ALERT_OCTAGON, 13, Theme.RED), JLabel.LEFT);
            allergy.setFont(Theme.font(Font.BOLD, 11));
            allergy.setForeground(Theme.CRIT_FG);
            allergy.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            mid.add(allergy);
        }
        card.add(mid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6, 8));
        bottom.setOpaque(false);
        long mins = waited.toMinutes();
        JLabel wait = new JLabel(Format.waited(waited), Icons.of(TablerIcon.CLOCK, 14,
                mins >= 60 ? Theme.RED : mins >= 30 ? Theme.ORANGE : Theme.TERTIARY_LABEL), JLabel.LEFT);
        wait.setFont(Theme.font(Font.BOLD, 12));
        wait.setForeground(mins >= 60 ? Theme.CRIT_FG : mins >= 30 ? Theme.WARN_FG : Theme.SECONDARY_LABEL);
        wait.setToolTipText("Time in this stage");
        JPanel status = new JPanel(new BorderLayout());
        status.setOpaque(false);
        status.add(wait, BorderLayout.WEST);
        status.add(iconButton(TablerIcon.DOTS, "More", e -> menu(q).show((Component) e.getSource(), 0, 30)), BorderLayout.EAST);
        bottom.add(status, BorderLayout.NORTH);
        if (app.canEdit(Screen.QUEUE) || (q.stage() == Stage.PHARMACY && app.canEdit(Screen.PHARMACY))) {
            PillButton next = new PillButton(q.stage().next().label(), Icons.of(TablerIcon.ARROW_RIGHT, 14, Color.WHITE), PillButton.Style.PRIMARY);
            next.setHorizontalTextPosition(JButton.LEFT);
            next.setFont(Theme.font(Font.BOLD, 12));
            next.setPreferredSize(new Dimension(80, 32));
            next.setText(q.stage().moveLabel());
            next.setToolTipText("Move to " + q.stage().next().label());
            next.addActionListener(e -> app.attempt(() -> {
                QueueEntry moved = s.advance(q.id());
                app.toast(q.tokenLabel() + " moved to " + moved.stage().label().toLowerCase() + ".", Toast.Kind.SUCCESS);
            }));
            bottom.add(next, BorderLayout.SOUTH);
        }
        card.add(bottom, BorderLayout.SOUTH);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    app.openPatient(q.patientId());
                }
            }
        });
        card.setToolTipText("Double-click to open the chart");
        return card;
    }

    private JPopupMenu menu(QueueEntry q) {
        JPopupMenu m = new JPopupMenu();
        m.add(item("Open chart", TablerIcon.USER, () -> app.openPatient(q.patientId())));
        if (app.canEdit(Screen.VITALS)) {
            m.add(item("Record vitals", TablerIcon.ACTIVITY_HEARTBEAT, () -> Sheets.vitals(app, q.patientId())));
        }
        if (app.canEdit(Screen.PHARMACY) && q.stage() == Stage.PHARMACY) {
            m.add(item("Dispense", TablerIcon.PILL, () -> Sheets.dispense(app, null, q.patientId())));
        }
        if (app.canEdit(Screen.BILLING) && q.stage() == Stage.BILLING) {
            m.add(item("Bill consultation", TablerIcon.RECEIPT, () -> app.attempt(() -> {
                app.store().billConsultation(q.patientId());
                app.toast("Consultation added to today's bill.", Toast.Kind.SUCCESS);
            })));
        }
        if (app.canEdit(Screen.QUEUE)) {
            m.addSeparator();
            for (Priority p : Priority.values()) {
                if (p != q.priority()) {
                    m.add(item("Set priority: " + p.label(), p == Priority.EMERGENCY ? TablerIcon.AMBULANCE : TablerIcon.CLOCK,
                            () -> app.attempt(() -> app.store().setPriority(q.id(), p))));
                }
            }
            m.addSeparator();
            m.add(item("Mark visit complete", TablerIcon.CIRCLE_CHECK, () -> app.attempt(() -> {
                QueueEntry cur = q;
                while (cur.stage() != Stage.DONE) {
                    cur = app.store().advance(cur.id());
                }
            })));
        }
        return m;
    }

    private static JMenuItem item(String text, TablerIcon icon, Runnable r) {
        JMenuItem i = new JMenuItem(text, Icons.of(icon, 16, Theme.SECONDARY_LABEL));
        i.setFont(Theme.body());
        i.addActionListener(e -> r.run());
        return i;
    }

    private static JButton iconButton(TablerIcon icon, String name, java.awt.event.ActionListener l) {
        JButton b = new JButton(Icons.of(icon, 18, Theme.SECONDARY_LABEL));
        b.setPreferredSize(new Dimension(30, 30));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(name);
        b.getAccessibleContext().setAccessibleName(name);
        b.addActionListener(l);
        return b;
    }
}
