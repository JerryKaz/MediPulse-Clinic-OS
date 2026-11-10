package com.medipulse.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

/** The soft, colourful window background that glass surfaces sit on. */
public class Backdrop extends JPanel {

    private final float strength;

    /** @param strength 1 for the app window, higher for the login screen */
    public Backdrop(LayoutManager layout, float strength) {
        super(layout);
        this.strength = strength;
        setBackground(Theme.WINDOW_BG);
    }

    @Override
    protected void paintComponent(Graphics g) {
        setBackground(Theme.WINDOW_BG);
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();
            blob(g2, w * 0.85f, h * 0.08f, Math.max(w, h) * 0.45f, Theme.BLUE, 0.13f);
            blob(g2, w * 0.25f, h * 1.0f, Math.max(w, h) * 0.40f, Theme.GREEN, 0.10f);
            blob(g2, w * 0.05f, h * 0.15f, Math.max(w, h) * 0.30f, Theme.INDIGO, 0.08f);
        } finally {
            g2.dispose();
        }
    }

    private void blob(Graphics2D g2, float cx, float cy, float r, Color c, float alpha) {
        int a = Math.min(255, Math.round(255 * alpha * strength * (Theme.isDark() ? 1.6f : 1f)));
        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Float(cx, cy), r,
                new float[]{0f, 1f},
                new Color[]{Theme.withAlpha(c, a), Theme.withAlpha(c, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g2.setPaint(p);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
