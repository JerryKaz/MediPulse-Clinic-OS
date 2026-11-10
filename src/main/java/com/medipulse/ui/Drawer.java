package com.medipulse.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.LayoutManager;

/** A panel that slides in from the right edge, for record details (e.g. a patient chart). */
public final class Drawer extends Overlay {

    public static final int WIDTH = 460;

    private Drawer(Component anchor, String title, JComponent body, JComponent footer) {
        super(anchor, null, true);
        setLayout(new RightLayout());

        Card panel = new Card(new BorderLayout(), 26);
        panel.fill(Theme.RAISED_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 8));
        JLabel t = new JLabel(title);
        t.setFont(Theme.caption());
        t.setForeground(Theme.TERTIARY_LABEL);
        head.add(t, BorderLayout.WEST);
        JButton close = new JButton(Icons.of(TablerIcon.X, 18, Theme.SECONDARY_LABEL));
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText("Close (Esc)");
        close.getAccessibleContext().setAccessibleName("Close");
        close.addActionListener(e -> close());
        head.add(close, BorderLayout.EAST);
        panel.add(head, BorderLayout.NORTH);

        body.setOpaque(false);
        JScrollPane scroll = VerticalScroll.of(body);
        panel.add(scroll, BorderLayout.CENTER);
        if (footer != null) {
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 8));
            panel.add(footer, BorderLayout.SOUTH);
        }
        add(panel);
    }

    public static Drawer open(Component anchor, String title, JComponent body, JComponent footer) {
        Drawer d = new Drawer(anchor, title, body, footer);
        d.present();
        return d;
    }

    /** Pins the single child to the right edge with a small inset. */
    private static final class RightLayout implements LayoutManager {
        @Override public void addLayoutComponent(String n, Component c) { }
        @Override public void removeLayoutComponent(Component c) { }
        @Override public Dimension preferredLayoutSize(Container p) { return p.getSize(); }
        @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(0, 0); }

        @Override
        public void layoutContainer(Container p) {
            if (p.getComponentCount() > 0) {
                int inset = 12;
                int w = Math.min(WIDTH, p.getWidth() - 2 * inset);
                p.getComponent(0).setBounds(p.getWidth() - w - inset, inset, w, p.getHeight() - 2 * inset);
            }
        }
    }
}
