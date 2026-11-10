package upsa.clinic.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** Solid white rounded container for the content layer (no glass). */
public class Card extends JPanel {

    private final int arc;
    private Color fill = Theme.CARD_BG;

    public Card(LayoutManager layout) {
        this(layout, Theme.RADIUS_CARD);
    }

    public Card(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    public Card fill(Color color) {
        this.fill = color;
        repaint();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setColor(new Color(0, 0, 0, 10));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
