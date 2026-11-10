package com.ayaresa.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Rounded input with a leading icon, a real placeholder (painted, never stored as text)
 * and an error state. Optional trailing component, e.g. a show-password toggle.
 */
public class IconField extends JPanel {

    private final JTextComponent input;
    private final int arc;
    private Color fill;
    private boolean error;

    private IconField(JTextComponent input, TablerIcon icon, String placeholder, Color fill, int height, int arc) {
        super(new BorderLayout(10, 0));
        this.input = input;
        this.fill = fill;
        this.arc = arc;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 12));
        setPreferredSize(new Dimension(200, height));

        JLabel lead = new JLabel(Icons.of(icon, 18, Theme.TERTIARY_LABEL));
        add(lead, BorderLayout.WEST);

        input.setOpaque(false);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setFont(Theme.font(java.awt.Font.PLAIN, 15));
        input.setForeground(Theme.LABEL);
        input.setCaretColor(Theme.BLUE);
        input.getAccessibleContext().setAccessibleName(placeholder);
        add(new Placeholder(input, placeholder), BorderLayout.CENTER);

        input.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    public static IconField text(TablerIcon icon, String placeholder) {
        return new IconField(new JTextField(), icon, placeholder, Theme.FIELD_BG, 46, 14);
    }

    public static IconField password(TablerIcon icon, String placeholder) {
        return new IconField(new JPasswordField(), icon, placeholder, Theme.FIELD_BG, 46, 14);
    }

    /** Compact pill-shaped search field for toolbars. */
    public static IconField search(String placeholder) {
        return new IconField(new JTextField(), TablerIcon.SEARCH, placeholder, Theme.FILL, 36, 36);
    }

    public JTextComponent input() {
        return input;
    }

    public void setTrailing(JComponent c) {
        add(c, BorderLayout.EAST);
    }

    public void setError(boolean error) {
        this.error = error;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            Color ring = error ? Theme.RED : (input.isFocusOwner() ? Theme.BLUE : null);
            if (ring != null) {
                g2.setColor(Theme.withAlpha(ring, 70));
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
                g2.setColor(ring);
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    /**
     * Paints the placeholder itself so it works under any look and feel and is never
     * returned by getText() (the old code stored placeholder text in the field).
     */
    private static final class Placeholder extends JPanel {
        private final JTextComponent input;
        private final String text;

        Placeholder(JTextComponent input, String text) {
            super(new BorderLayout());
            this.input = input;
            this.text = text;
            setOpaque(false);
            add(input, BorderLayout.CENTER);
            input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (input.getDocument().getLength() == 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(input.getFont());
                g2.setColor(Theme.TERTIARY_LABEL);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, input.getX() + 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }
}
