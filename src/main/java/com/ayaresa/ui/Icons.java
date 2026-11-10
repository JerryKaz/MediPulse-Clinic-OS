package com.ayaresa.ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Renders {@link TablerIcon} glyphs as Swing {@link Icon}s. Vector, so they stay sharp at any
 * size and look identical on Windows, macOS and Linux (unlike the old emoji).
 */
public final class Icons {

    private static final String FONT_PATH = "/com/ayaresa/fonts/tabler-icons.ttf";
    private static Font base;

    private Icons() {
    }

    static synchronized void load() {
        if (base != null) {
            return;
        }
        try (InputStream in = Objects.requireNonNull(Icons.class.getResourceAsStream(FONT_PATH), FONT_PATH)) {
            base = Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | java.awt.FontFormatException | NullPointerException e) {
            throw new IllegalStateException("Icon font missing from the build: " + FONT_PATH, e);
        }
    }

    public static Icon of(TablerIcon icon, int size, Color color) {
        load();
        return new GlyphIcon(icon, base.deriveFont((float) size), size, color);
    }

    private record GlyphIcon(TablerIcon icon, Font font, int size, Color color) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2.setFont(font);
                Color paint = color != null ? color : (c != null ? c.getForeground() : Theme.LABEL);
                if (c != null && !c.isEnabled()) {
                    paint = Theme.TERTIARY_LABEL;
                }
                g2.setColor(paint);
                FontMetrics fm = g2.getFontMetrics();
                String s = icon.glyph();
                int gx = x + (size - fm.stringWidth(s)) / 2;
                int gy = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(s, gx, gy);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
