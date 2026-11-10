package com.medipulse.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Small coloured capsule for patient, bed and stock status. Same colours on every screen. */
public class StatusPill extends JLabel {

    public enum Kind {
        OK, WARNING, CRITICAL, INFO, IDLE;

        public Color background() {
            return switch (this) {
                case OK -> Theme.OK_BG;
                case WARNING -> Theme.WARN_BG;
                case CRITICAL -> Theme.CRIT_BG;
                case INFO -> Theme.INFO_BG;
                case IDLE -> Theme.IDLE_BG;
            };
        }

        public Color foreground() {
            return switch (this) {
                case OK -> Theme.OK_FG;
                case WARNING -> Theme.WARN_FG;
                case CRITICAL -> Theme.CRIT_FG;
                case INFO -> Theme.INFO_FG;
                case IDLE -> Theme.IDLE_FG;
            };
        }
    }

    private final Kind kind;

    public StatusPill(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(Theme.font(java.awt.Font.BOLD, 12));
        setForeground(kind.foreground());
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(kind.background());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
