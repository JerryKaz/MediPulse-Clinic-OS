package com.ayaresa.ui;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/** Factory for consistently styled form controls, independent of the look and feel. */
public final class Fields {

    private Fields() {
    }

    public static JTextField text(String value, String hint) {
        JTextField f = new HintField(value == null ? "" : value, hint);
        style(f, hint);
        return f;
    }

    /** Text field that paints a grey hint while empty (never stored as text). */
    private static final class HintField extends JTextField {
        private final String hint;

        HintField(String value, String hint) {
            super(value);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            if (hint != null && getDocument().getLength() == 0) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                g2.setColor(Theme.TERTIARY_LABEL);
                Insets in = getInsets();
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    public static JTextArea area(String value, int rows) {
        JTextArea a = new JTextArea(value == null ? "" : value, rows, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        style(a, null);
        return a;
    }

    public static <E> JComboBox<E> combo(List<E> items, E selected) {
        JComboBox<E> c = new JComboBox<>(new javax.swing.DefaultComboBoxModel<>(new java.util.Vector<>(items)));
        if (selected != null) {
            c.setSelectedItem(selected);
        }
        c.setFont(Theme.body());
        c.setPreferredSize(new Dimension(160, 40));
        return c;
    }

    public static JCheckBox check(String label, boolean value) {
        JCheckBox c = new JCheckBox(label, value);
        c.setOpaque(false);
        c.setFont(Theme.body());
        c.setForeground(Theme.LABEL);
        return c;
    }

    /** Evenly spaced columns of labelled fields. */
    public static JComponent row(JComponent... cells) {
        JPanel p = new JPanel(new GridLayout(1, cells.length, 12, 0));
        p.setOpaque(false);
        for (JComponent c : cells) {
            p.add(c);
        }
        return p;
    }

    /** Vertical stack with even spacing. */
    public static JComponent stack(JComponent... rows) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.Y_AXIS));
        for (int i = 0; i < rows.length; i++) {
            rows[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(rows[i]);
            if (i < rows.length - 1) {
                p.add(javax.swing.Box.createVerticalStrut(12));
            }
        }
        return p;
    }

    public static int intValue(JTextField f, String what) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a whole number for " + what + ".");
        }
    }

    public static double decimalValue(JTextField f, String what) {
        try {
            return Double.parseDouble(f.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a number for " + what + ".");
        }
    }

    public static void style(JTextComponent f, String hint) {
        f.setFont(Theme.body());
        f.setForeground(Theme.LABEL);
        f.setCaretColor(Theme.BLUE);
        f.setBackground(Theme.isDark() ? new java.awt.Color(0x2C2C2E) : new java.awt.Color(0xF5F5F7));
        f.setOpaque(true);
        RoundBorder border = new RoundBorder();
        f.setBorder(border);
        if (hint != null) {
            f.setToolTipText(hint);
            f.getAccessibleContext().setAccessibleDescription(hint);
        }
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { border.focused = true; f.repaint(); }
            @Override public void focusLost(FocusEvent e) { border.focused = false; f.repaint(); }
        });
        if (f instanceof JTextField) {
            f.setPreferredSize(new Dimension(160, 40));
        }
    }

    /** Rounded outline that turns blue on focus. */
    private static final class RoundBorder extends AbstractBorder {
        boolean focused;

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Paint the parent's background into the corners so the field looks rounded.
            Card card = (Card) javax.swing.SwingUtilities.getAncestorOfClass(Card.class, c);
            g2.setColor(card != null ? card.surface() : Theme.RAISED_BG);
            java.awt.geom.Area corners = new java.awt.geom.Area(new java.awt.Rectangle(x, y, w, h));
            corners.subtract(new java.awt.geom.Area(new java.awt.geom.RoundRectangle2D.Float(x, y, w - 1, h - 1, 12, 12)));
            g2.fill(corners);
            g2.setColor(focused ? Theme.BLUE : Theme.SEPARATOR);
            g2.setStroke(new java.awt.BasicStroke(focused ? 1.6f : 1f));
            g2.drawRoundRect(x, y, w - 1, h - 1, 12, 12);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 12, 8, 12);
        }
    }
}
