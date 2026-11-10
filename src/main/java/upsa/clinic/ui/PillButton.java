package upsa.clinic.ui;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Capsule button. {@link Style#PRIMARY} is the single tinted action per screen;
 * everything else is {@link Style#SECONDARY} or {@link Style#PLAIN}.
 */
public class PillButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, PLAIN }

    private final Style style;
    private boolean hover;
    private boolean pressed;

    public PillButton(String text, Icon icon, Style style) {
        super(text, icon);
        this.style = style;
        setFont(Theme.bodyBold());
        setForeground(style == Style.PRIMARY ? Color.WHITE : (style == Style.PLAIN ? Theme.BLUE : Theme.LABEL));
        setIconTextGap(6);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 16, 0, 16));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(36, d.height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = switch (style) {
                case PRIMARY -> isEnabled() ? Theme.BLUE : Theme.GRAY;
                case SECONDARY -> new Color(118, 118, 128, 26);
                case PLAIN -> new Color(0, 0, 0, 0);
            };
            if (pressed) {
                bg = style == Style.PRIMARY ? Theme.BLUE.darker() : new Color(0, 0, 0, 25);
            } else if (hover) {
                bg = style == Style.PRIMARY ? new Color(0x1A86FF) : new Color(118, 118, 128, 46);
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
