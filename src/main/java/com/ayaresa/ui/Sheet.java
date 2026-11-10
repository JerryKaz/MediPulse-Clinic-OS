package com.ayaresa.ui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/**
 * A centred modal sheet with a title, a body and a button row: the replacement for
 * JOptionPane forms. The primary action returns true to close, false to stay open (for
 * example when validation fails and an inline error is shown).
 */
public final class Sheet extends Overlay {

    private final JLabel error = new JLabel(" ");
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    private final Card card;

    private Sheet(Component anchor, String title, String subtitle, JComponent body, int width) {
        super(anchor, new GridBagLayout(), false);
        card = new Card(new BorderLayout(0, 14), 26);
        card.fill(Theme.RAISED_BG);
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Theme.title());
        t.setForeground(Theme.LABEL);
        head.add(t);
        if (subtitle != null) {
            JLabel s = new JLabel("<html>" + subtitle + "</html>");
            s.setFont(Theme.body());
            s.setForeground(Theme.SECONDARY_LABEL);
            s.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            head.add(s);
        }
        card.add(head, BorderLayout.NORTH);
        body.setOpaque(false);
        card.add(body, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout(0, 10));
        foot.setOpaque(false);
        error.setFont(Theme.font(java.awt.Font.PLAIN, 13));
        error.setForeground(Theme.RED);
        foot.add(error, BorderLayout.NORTH);
        buttons.setOpaque(false);
        foot.add(buttons, BorderLayout.SOUTH);
        card.add(foot, BorderLayout.SOUTH);

        int w = width;
        card.setPreferredSize(null);
        Dimension pref = card.getPreferredSize();
        card.setPreferredSize(new Dimension(w, pref.height));
        add(card);
    }

    public static Sheet open(Component anchor, String title, String subtitle, JComponent body) {
        return open(anchor, title, subtitle, body, 480);
    }

    public static Sheet open(Component anchor, String title, String subtitle, JComponent body, int width) {
        Sheet s = new Sheet(anchor, title, subtitle, body, width);
        s.present();
        return s;
    }

    /** Adds the default (blue) action, bound to Enter. */
    public Sheet primary(String label, Supplier<Boolean> action) {
        return action(label, action, PillButton.Style.PRIMARY, true);
    }

    /** A red action for irreversible steps (delete, discharge, reset). */
    public Sheet destructive(String label, Supplier<Boolean> action) {
        PillButton b = addButton(label, action, PillButton.Style.PRIMARY);
        b.setDanger(true);
        bindEnter(b);
        return this;
    }

    public Sheet secondary(String label, Supplier<Boolean> action) {
        return action(label, action, PillButton.Style.SECONDARY, false);
    }

    public Sheet cancel() {
        PillButton b = new PillButton("Cancel", null, PillButton.Style.SECONDARY);
        b.addActionListener(e -> close());
        buttons.add(b, 0);
        relayout();
        return this;
    }

    private Sheet action(String label, Supplier<Boolean> action, PillButton.Style style, boolean enter) {
        PillButton b = addButton(label, action, style);
        if (enter) {
            bindEnter(b);
        }
        return this;
    }

    private PillButton addButton(String label, Supplier<Boolean> action, PillButton.Style style) {
        PillButton b = new PillButton(label, null, style);
        b.addActionListener(e -> {
            error.setText(" ");
            try {
                if (Boolean.TRUE.equals(action.get())) {
                    close();
                }
            } catch (RuntimeException ex) {
                showError(ex.getMessage() == null ? "Something went wrong." : ex.getMessage());
            }
        });
        buttons.add(b);
        relayout();
        return b;
    }

    private void bindEnter(PillButton b) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "primary");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "primary");
        getActionMap().put("primary", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                b.doClick();
            }
        });
    }

    public void showError(String message) {
        error.setText(message);
    }

    private void relayout() {
        Dimension size = card.getPreferredSize();
        card.setPreferredSize(null);
        Dimension pref = card.getPreferredSize();
        card.setPreferredSize(new Dimension(size.width, pref.height));
        revalidate();
        repaint();
    }

    // ---- Ready-made sheets -------------------------------------------------------------

    /** Asks for confirmation before an action. */
    public static void confirm(Component anchor, String title, String message, String actionLabel,
                               boolean destructive, Runnable onConfirm) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(Box.createVerticalStrut(2));
        Sheet s = open(anchor, title, message, body, 420).cancel();
        if (destructive) {
            s.destructive(actionLabel, () -> {
                onConfirm.run();
                return true;
            });
        } else {
            s.primary(actionLabel, () -> {
                onConfirm.run();
                return true;
            });
        }
    }

    /** Small helper used by forms: a label above a component. */
    public static JComponent labelled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(Theme.font(java.awt.Font.BOLD, 12));
        l.setForeground(Theme.SECONDARY_LABEL);
        l.setLabelFor(field);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }
}
