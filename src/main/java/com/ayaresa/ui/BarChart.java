package com.ayaresa.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** Minimal bar chart: one series, rounded bars, value shown on hover, last bar highlighted. */
public final class BarChart extends JComponent {

    public record Bar(String label, double value, String tooltip) {
    }

    private List<Bar> bars = List.of();
    private final Color color;
    private int hover = -1;

    public BarChart(Color color) {
        this.color = color;
        setPreferredSize(new Dimension(400, 180));
        setOpaque(false);
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i != hover) {
                    hover = i;
                    setToolTipText(i >= 0 ? bars.get(i).tooltip() : null);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.CANVAS;
                }
            };
            accessibleContext.setAccessibleName("Bar chart");
        }
        return accessibleContext;
    }

    public void setBars(List<Bar> bars) {
        this.bars = List.copyOf(bars);
        StringBuilder desc = new StringBuilder();
        for (Bar b : bars) {
            desc.append(b.tooltip()).append(". ");
        }
        getAccessibleContext().setAccessibleDescription(desc.toString());
        repaint();
    }

    private int indexAt(int x) {
        if (bars.isEmpty()) {
            return -1;
        }
        int slot = getWidth() / bars.size();
        int i = x / Math.max(1, slot);
        return i >= 0 && i < bars.size() ? i : -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (bars.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.footnote());
        FontMetrics fm = g2.getFontMetrics();
        int labelH = fm.getHeight() + 6;
        int top = fm.getHeight() + 4;
        int h = getHeight() - labelH - top;
        double max = bars.stream().mapToDouble(Bar::value).max().orElse(1);
        int slot = getWidth() / bars.size();
        int bw = Math.max(6, (int) (slot * 0.56));

        // baseline
        g2.setColor(Theme.SEPARATOR);
        g2.drawLine(0, top + h, getWidth(), top + h);

        for (int i = 0; i < bars.size(); i++) {
            Bar b = bars.get(i);
            int bh = max <= 0 ? 0 : (int) Math.round(h * b.value() / max);
            int x = i * slot + (slot - bw) / 2;
            boolean emphasis = i == hover || (hover < 0 && i == bars.size() - 1);
            g2.setColor(emphasis ? color : Theme.withAlpha(color, Theme.isDark() ? 110 : 90));
            g2.fillRoundRect(x, top + h - bh, bw, Math.max(bh, 3), 8, 8);
            if (emphasis) {
                String v = b.value() == Math.rint(b.value()) ? String.valueOf((long) b.value()) : String.format("%.0f", b.value());
                g2.setColor(Theme.LABEL);
                g2.drawString(v, x + (bw - fm.stringWidth(v)) / 2, top + h - bh - 4);
            }
            g2.setColor(emphasis ? Theme.LABEL : Theme.TERTIARY_LABEL);
            g2.drawString(b.label(), i * slot + (slot - fm.stringWidth(b.label())) / 2, top + h + fm.getAscent() + 4);
        }
        g2.dispose();
    }
}
