package com.ayaresa.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Non-blocking capsule notification at the bottom of the window. Replaces the old
 * information/success JOptionPane popups. Keep dialogs for confirmations only.
 */
public final class Toast {

    public enum Kind {
        SUCCESS(TablerIcon.CIRCLE_CHECK, Theme.GREEN),
        INFO(TablerIcon.INFO_CIRCLE, Theme.BLUE),
        WARNING(TablerIcon.ALERT_TRIANGLE, Theme.ORANGE),
        ERROR(TablerIcon.CIRCLE_X, Theme.RED);

        final TablerIcon icon;
        final java.awt.Color color;

        Kind(TablerIcon icon, java.awt.Color color) {
            this.icon = icon;
            this.color = color;
        }
    }

    private static final int SHOW_MS = 3200;
    private static final int FADE_MS = 220;
    private static final int BOTTOM_MARGIN = 26;
    private static final String KEY = "com.ayaresa.toast";

    private Toast() {
    }

    public static void show(Component anchor, String message, Kind kind) {
        RootPaneContainer window = (RootPaneContainer) SwingUtilities.getAncestorOfClass(RootPaneContainer.class, anchor);
        if (window == null && anchor instanceof RootPaneContainer rpc) {
            window = rpc;
        }
        if (window == null) {
            return;
        }
        JRootPane root = window.getRootPane();
        JLayeredPane layers = window.getLayeredPane();

        // Only one toast at a time: replace the previous one.
        Object previous = root.getClientProperty(KEY);
        if (previous instanceof View v) {
            v.dismiss();
        }

        View view = new View(message, kind);
        Dimension pref = view.getPreferredSize();
        int x = (layers.getWidth() - pref.width) / 2;
        int y = layers.getHeight() - pref.height - BOTTOM_MARGIN;
        view.setBounds(x, y, pref.width, pref.height);
        layers.add(view, JLayeredPane.POPUP_LAYER);
        root.putClientProperty(KEY, view);
        view.getAccessibleContext().setAccessibleName(message);
        view.start();
    }

    private static final class View extends GlassPanel {
        private float alpha = 0f;
        private Timer timer;

        View(String message, Kind kind) {
            super(new BorderLayout(), 46);
            int s = GlassPanel.shadowInset();
            setBorder(BorderFactory.createEmptyBorder(s, s + 16, s, s + 20));
            JLabel label = new JLabel(message, Icons.of(kind.icon, 20, kind.color), JLabel.LEFT);
            label.setIconTextGap(10);
            label.setFont(Theme.body());
            label.setForeground(Theme.LABEL);
            add(label, BorderLayout.CENTER);
            setSize(getPreferredSize());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, 46 + 2 * GlassPanel.shadowInset());
        }

        void start() {
            long started = System.currentTimeMillis();
            timer = new Timer(16, e -> {
                long t = System.currentTimeMillis() - started;
                if (t < FADE_MS) {
                    alpha = t / (float) FADE_MS;
                } else if (t < SHOW_MS) {
                    alpha = 1f;
                } else if (t < SHOW_MS + FADE_MS) {
                    alpha = 1f - (t - SHOW_MS) / (float) FADE_MS;
                } else {
                    dismiss();
                    return;
                }
                repaint();
            });
            timer.start();
        }

        void dismiss() {
            if (timer != null) {
                timer.stop();
            }
            java.awt.Container parent = getParent();
            if (parent != null) {
                parent.remove(this);
                parent.repaint(getX(), getY(), getWidth(), getHeight());
            }
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            super.paint(g2);
            g2.dispose();
        }
    }

    /** For tests and screenshots: the toast currently shown in this window, if any. */
    static JComponent current(JRootPane root) {
        return (JComponent) root.getClientProperty(KEY);
    }
}
