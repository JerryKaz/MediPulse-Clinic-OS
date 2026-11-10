package com.medipulse.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Liquid Glass surface for the functional layer only: the floating sidebar, toolbar,
 * toasts and the login card. Never use it for content (cards, tables, lists).
 *
 * <p>Swing has no backdrop blur, so the glass is a translucent fill over the soft
 * {@link Backdrop}, with a bright rim, an inner top highlight and a soft drop shadow.
 * Over that backdrop it reads the same as real blur.
 */
public class GlassPanel extends JPanel {

    private static final int SHADOW = 10;
    private final int arc;

    public GlassPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
    }

    /** Space kept free around the shape for the shadow; add it to the panel's border. */
    public static int shadowInset() {
        return SHADOW;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float w = getWidth() - 2f * SHADOW;
            float h = getHeight() - 2f * SHADOW;

            // Soft shadow: stacked low-alpha rounded rects, offset down
            for (int i = SHADOW; i > 0; i--) {
                int alpha = (int) (14 * (1 - i / (float) SHADOW) + 1);
                g2.setColor(Theme.withAlpha(Theme.SHADOW, Theme.isDark() ? alpha * 3 : alpha));
                g2.fill(new RoundRectangle2D.Float(SHADOW - i * 0.6f, SHADOW - i * 0.3f + 3,
                        w + i * 1.2f, h + i * 1.2f, arc + i, arc + i));
            }

            RoundRectangle2D shape = new RoundRectangle2D.Float(SHADOW, SHADOW, w, h, arc, arc);
            g2.setColor(Theme.glassFill());
            g2.fill(shape);

            // Inner top highlight
            g2.setClip(shape);
            g2.setColor(new Color(255, 255, 255, Theme.isDark() ? 28 : 140));
            g2.fill(new RoundRectangle2D.Float(SHADOW, SHADOW, w, 1.5f, 0, 0));
            g2.setClip(null);

            // Bright rim
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 255, Theme.isDark() ? 36 : 190));
            g2.draw(new RoundRectangle2D.Float(SHADOW + 0.5f, SHADOW + 0.5f, w - 1, h - 1, arc, arc));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
