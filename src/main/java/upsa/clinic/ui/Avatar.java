package upsa.clinic.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Circle with initials, coloured consistently from the name. */
public class Avatar extends JComponent {

    private static final Color[] PALETTE = {
            Theme.BLUE, Theme.GREEN, Theme.ORANGE, Theme.PINK, Theme.PURPLE, Theme.INDIGO, Theme.TEAL
    };

    private final String initials;
    private final Color color;
    private final int size;

    public Avatar(String initials, String seed, int size) {
        this.initials = initials;
        this.color = PALETTE[Math.floorMod(seed.hashCode(), PALETTE.length)];
        this.size = size;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    /** "Kwame Asante" -> "KA". */
    public static String initialsOf(String name) {
        StringBuilder sb = new StringBuilder();
        for (String p : name.trim().split("\\s+")) {
            if (!p.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        return sb.toString();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.font(Font.BOLD, size * 0.4f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (size - fm.stringWidth(initials)) / 2,
                    (size - fm.getHeight()) / 2 + fm.getAscent());
        } finally {
            g2.dispose();
        }
    }
}
