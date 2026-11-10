package com.medipulse.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;

/** iOS-style segmented control. Keyboard: Left/Right arrows when focused. */
public class SegmentedControl extends JComponent {

    private static final int PAD = 2;
    private static final int SEG_W = 110;
    private final List<String> labels;
    private int selected;
    private IntConsumer onChange = i -> { };

    public SegmentedControl(List<String> labels) {
        this.labels = List.copyOf(labels);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = new Dimension(labels.size() * SEG_W + PAD * 2, 32);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                select(Math.min(labels.size() - 1, Math.max(0, (e.getX() - PAD) / SEG_W)));
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    select(Math.max(0, selected - 1));
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    select(Math.min(labels.size() - 1, selected + 1));
                }
            }
        });
    }

    public void onChange(IntConsumer listener) {
        this.onChange = listener;
    }

    public int selected() {
        return selected;
    }

    public void select(int index) {
        if (index != selected) {
            selected = index;
            repaint();
            onChange.accept(index);
        }
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PAGE_TAB_LIST;
                }

                @Override
                public String getAccessibleDescription() {
                    return "Selected: " + labels.get(selected);
                }
            };
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int h = getHeight();
            g2.setColor(Theme.FILL);
            g2.fillRoundRect(0, 0, getWidth(), h, 10, 10);

            int x = PAD + selected * SEG_W;
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(x, PAD + 1, SEG_W, h - PAD * 2, 8, 8);
            g2.setColor(Theme.isDark() ? new Color(0x636366) : Color.WHITE);
            g2.fillRoundRect(x, PAD, SEG_W, h - PAD * 2, 8, 8);
            if (isFocusOwner()) {
                g2.setColor(Theme.withAlpha(Theme.BLUE, 120));
                g2.drawRoundRect(x, PAD, SEG_W - 1, h - PAD * 2 - 1, 8, 8);
            }

            for (int i = 0; i < labels.size(); i++) {
                g2.setFont(Theme.font(i == selected ? Font.BOLD : Font.PLAIN, 13));
                g2.setColor(i == selected ? Theme.LABEL : Theme.SECONDARY_LABEL);
                FontMetrics fm = g2.getFontMetrics();
                String s = labels.get(i);
                g2.drawString(s, PAD + i * SEG_W + (SEG_W - fm.stringWidth(s)) / 2,
                        (h - fm.getHeight()) / 2 + fm.getAscent());
            }
        } finally {
            g2.dispose();
        }
    }
}
