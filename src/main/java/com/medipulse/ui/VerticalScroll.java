package com.medipulse.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Vertical-only, transparent scrolling. Content is laid out at the viewport's width (never
 * wider), so cards shrink to fit their column instead of being clipped or scrolling sideways.
 */
public final class VerticalScroll {

    private VerticalScroll() {
    }

    /** Wraps {@code content} pinned to the top. */
    public static JScrollPane of(JComponent content) {
        Track track = new Track();
        track.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(track, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static final class Track extends JPanel implements Scrollable {
        Track() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(16, r.height - 32); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
