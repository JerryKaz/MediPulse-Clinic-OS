package com.ayaresa.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** A row of filter chips; exactly one is selected. Each chip can show a count. */
public final class Chips extends JPanel {

    private final List<Chip> chips = new ArrayList<>();
    private int selected;
    private IntConsumer onChange = i -> { };

    public Chips(List<String> labels) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setOpaque(false);
        for (int i = 0; i < labels.size(); i++) {
            int index = i;
            Chip c = new Chip(labels.get(i));
            c.addActionListener(e -> select(index));
            chips.add(c);
            add(c);
        }
        select(0);
    }

    public void onChange(IntConsumer listener) {
        this.onChange = listener;
    }

    public int selected() {
        return selected;
    }

    public void select(int index) {
        selected = index;
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).active = i == index;
            chips.get(i).setForeground(i == index ? java.awt.Color.WHITE : Theme.LABEL);
            chips.get(i).getAccessibleContext().setAccessibleDescription(i == index ? "Selected" : null);
            chips.get(i).repaint();
        }
        onChange.accept(index);
    }

    /** Updates a chip's label, e.g. "Low stock · 4". */
    public void setCount(int index, int count) {
        Chip c = chips.get(index);
        c.setText(c.base + (count >= 0 ? "  " + count : ""));
    }

    private static final class Chip extends JButton {
        final String base;
        boolean active;

        Chip(String label) {
            super(label);
            base = label;
            setFont(Theme.font(java.awt.Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(active ? Theme.LABEL : getModel().isRollover() ? Theme.FILL.darker() : Theme.FILL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            setForeground(active ? Theme.CARD_BG : Theme.LABEL);
            super.paintComponent(g);
        }
    }
}
