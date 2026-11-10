package com.medipulse.screens;

import com.medipulse.shell.AppContext;
import com.medipulse.store.ClinicStore;
import com.medipulse.ui.Card;
import com.medipulse.ui.Icons;
import com.medipulse.ui.PillButton;
import com.medipulse.ui.StatusPill;
import com.medipulse.ui.TablerIcon;
import com.medipulse.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Base for every screen: a large title header, store subscriptions that refresh the screen
 * when its data changes (lazily, only while visible), and shared building blocks.
 */
public abstract class ScreenView extends JPanel {

    protected final AppContext app;
    private final List<AutoCloseable> subscriptions = new ArrayList<>();
    private final List<Timer> timers = new ArrayList<>();
    private boolean dirty = true;

    protected ScreenView(AppContext app, ClinicStore.Topic... topics) {
        super(new BorderLayout(0, 16));
        this.app = app;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 6));
        if (topics.length > 0) {
            subscriptions.add(app.store().subscribe(this::changed, topics));
        }
    }

    /** Rebuilds the screen's data views from the store. */
    protected abstract void refresh();

    /** The screen's main action for the toolbar (at most one), or null. */
    public PillButton primaryAction() {
        return null;
    }

    /** Called by the window each time the screen is shown. */
    public final void shown() {
        if (dirty) {
            dirty = false;
            refresh();
        }
    }

    private void changed() {
        if (isShowing()) {
            refresh();
        } else {
            dirty = true;
        }
    }

    /** Repeats {@code r} while the window is open (e.g. to update waiting times). */
    protected void every(int millis, Runnable r) {
        Timer t = new Timer(millis, e -> {
            if (isShowing()) {
                r.run();
            }
        });
        t.start();
        timers.add(t);
    }

    public void dispose() {
        timers.forEach(Timer::stop);
        for (AutoCloseable s : subscriptions) {
            try {
                s.close();
            } catch (Exception ignored) {
                // unsubscribing can't fail
            }
        }
    }

    // ---- Building blocks -------------------------------------------------------------

    protected static JComponent header(String title, String subtitle, JComponent... trailing) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Theme.largeTitle());
        t.setForeground(Theme.LABEL);
        text.add(t);
        if (subtitle != null) {
            JLabel s = new JLabel(subtitle);
            s.setFont(Theme.body());
            s.setForeground(Theme.SECONDARY_LABEL);
            s.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            text.add(s);
        }
        row.add(text, BorderLayout.CENTER);
        if (trailing.length > 0) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            right.setOpaque(false);
            for (JComponent c : trailing) {
                right.add(c);
            }
            row.add(right, BorderLayout.EAST);
        }
        return row;
    }

    /** Headline number tile. {@code noteKind} colours the note (null = neutral). */
    protected static Card kpi(TablerIcon icon, Color tint, String label, String value, String note, StatusPill.Kind noteKind) {
        return kpi(icon, tint, label, value, note, noteKind, 26);
    }

    protected static Card kpi(TablerIcon icon, Color tint, String label, String value, String note, StatusPill.Kind noteKind, int valueSize) {
        Card card = new Card(new BorderLayout(0, 4));
        JLabel l = new JLabel(label, Icons.of(icon, 18, tint), JLabel.LEFT);
        l.setIconTextGap(8);
        l.setFont(Theme.font(Font.PLAIN, 13));
        l.setForeground(Theme.SECONDARY_LABEL);
        JLabel v = new JLabel(value);
        v.setFont(Theme.font(Font.BOLD, valueSize));
        v.setForeground(Theme.LABEL);
        JLabel n = new JLabel(note == null ? " " : note);
        n.setFont(Theme.footnote());
        n.setForeground(noteKind == null ? Theme.SECONDARY_LABEL : noteKind.foreground());
        card.add(l, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        card.add(n, BorderLayout.SOUTH);
        card.getAccessibleContext().setAccessibleName(label + ": " + value + (note == null ? "" : ", " + note));
        return card;
    }

    protected static JPanel grid(int cols, int gap) {
        JPanel p = new JPanel(new java.awt.GridLayout(0, cols, gap, gap));
        p.setOpaque(false);
        return p;
    }

    protected static JPanel vertical() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    protected static JLabel sectionTitle(String text) {
        JLabel t = new JLabel(text);
        t.setFont(Theme.headline());
        t.setForeground(Theme.LABEL);
        return t;
    }

    protected static JLabel secondary(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.footnote());
        l.setForeground(Theme.SECONDARY_LABEL);
        return l;
    }

    protected static PillButton primary(String text, TablerIcon icon, Runnable action) {
        PillButton b = new PillButton(text, Icons.of(icon, 16, Color.WHITE), PillButton.Style.PRIMARY);
        b.addActionListener(e -> action.run());
        return b;
    }

    protected static PillButton secondaryButton(String text, TablerIcon icon, Runnable action) {
        PillButton b = new PillButton(text, icon == null ? null : Icons.of(icon, 16, Theme.LABEL), PillButton.Style.SECONDARY);
        b.setFont(Theme.font(Font.PLAIN, 14));
        b.addActionListener(e -> action.run());
        return b;
    }

    protected static Box.Filler gap(int h) {
        return (Box.Filler) Box.createVerticalStrut(h);
    }
}
