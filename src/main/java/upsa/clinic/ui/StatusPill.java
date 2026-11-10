package upsa.clinic.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Small coloured capsule for patient, bed and stock status. Same colours on every screen. */
public class StatusPill extends JLabel {

    public enum Kind {
        OK(Theme.OK_BG, Theme.OK_FG),
        WARNING(Theme.WARN_BG, Theme.WARN_FG),
        CRITICAL(Theme.CRIT_BG, Theme.CRIT_FG),
        INFO(Theme.INFO_BG, Theme.INFO_FG),
        IDLE(Theme.IDLE_BG, Theme.IDLE_FG);

        final Color bg;
        final Color fg;

        Kind(Color bg, Color fg) {
            this.bg = bg;
            this.fg = fg;
        }

        public Color background() {
            return bg;
        }

        public Color foreground() {
            return fg;
        }
    }

    private final Kind kind;

    public StatusPill(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(Theme.font(java.awt.Font.BOLD, 12));
        setForeground(kind.fg);
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(kind.bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
