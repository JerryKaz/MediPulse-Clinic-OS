package com.medipulse.shell;

import com.medipulse.security.AccessPolicy;
import com.medipulse.security.Screen;
import com.medipulse.security.User;
import com.medipulse.ui.Avatar;
import com.medipulse.ui.GlassPanel;
import com.medipulse.ui.Icons;
import com.medipulse.ui.TablerIcon;
import com.medipulse.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Floating glass sidebar. Shows only the modules the signed-in role may open
 * (from {@link AccessPolicy}); there is nothing to click that would be refused.
 */
final class Sidebar extends GlassPanel {

    static final int WIDTH = 252;

    private final Map<Screen, Item> items = new EnumMap<>(Screen.class);

    Sidebar(User user, Consumer<Screen> onSelect, Runnable onSignOut) {
        super(new BorderLayout(), Theme.RADIUS_WINDOW_INSET);
        int s = shadowInset();
        setBorder(BorderFactory.createEmptyBorder(s + 16, s + 10, s + 10, s + 10));

        add(brand(), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        Screen.Section current = null;
        for (Screen m : AccessPolicy.visibleScreens(user.role())) {
            if (m.section() != current) {
                current = m.section();
                if (m.section() != Screen.Section.MAIN) {
                    list.add(sectionLabel(m.section().label()));
                }
            }
            Item item = new Item(m, () -> onSelect.accept(m));
            items.put(m, item);
            list.add(item);
            list.add(Box.createVerticalStrut(2));
        }
        list.add(Box.createVerticalGlue());
        add(list, BorderLayout.CENTER);
        add(userChip(user, onSignOut), BorderLayout.SOUTH);
    }

    /** Shows a count bubble next to a screen (e.g. patients waiting); 0 hides it. */
    void setBadge(Screen screen, int count, boolean alert) {
        Item item = items.get(screen);
        if (item != null) {
            item.badge = count;
            item.alert = alert;
            item.getAccessibleContext().setAccessibleDescription(count > 0 ? count + " items" : null);
            item.repaint();
        }
    }

    void setSelected(Screen module) {
        items.forEach((m, item) -> item.setActive(m == module));
    }

    private static JComponent brand() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 8, 18, 0));
        JLabel logo = new JLabel(Icons.of(TablerIcon.HEARTBEAT, 20, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 11, 11);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setPreferredSize(new Dimension(34, 34));
        logo.setHorizontalAlignment(JLabel.CENTER);
        p.add(logo, BorderLayout.WEST);
        p.add(twoLine(com.medipulse.Brand.NAME, com.medipulse.Brand.TAGLINE), BorderLayout.CENTER);
        return p;
    }

    private static JComponent sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(Theme.font(Font.BOLD, 11));
        l.setForeground(Theme.TERTIARY_LABEL);
        l.setBorder(BorderFactory.createEmptyBorder(14, 10, 6, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return l;
    }

    private static JComponent userChip(User user, Runnable onSignOut) {
        JPanel chip = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(255, 255, 255, 16) : new Color(255, 255, 255, 130));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 6));
        chip.add(new Avatar(user.initials(), user.username(), 32), BorderLayout.WEST);
        chip.add(twoLine(user.displayName(), user.role().label()), BorderLayout.CENTER);

        JButton signOut = new JButton(Icons.of(TablerIcon.LOGOUT, 18, Theme.SECONDARY_LABEL));
        signOut.setToolTipText("Sign out");
        signOut.getAccessibleContext().setAccessibleName("Sign out");
        signOut.setContentAreaFilled(false);
        signOut.setBorderPainted(false);
        signOut.setFocusPainted(false);
        signOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signOut.addActionListener(e -> onSignOut.run());
        chip.add(signOut, BorderLayout.EAST);
        return chip;
    }

    private static JComponent twoLine(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 14));
        t.setForeground(Theme.LABEL);
        JLabel s = new JLabel(subtitle);
        s.setFont(Theme.footnote());
        s.setForeground(Theme.SECONDARY_LABEL);
        p.add(Box.createVerticalGlue());
        p.add(t);
        p.add(s);
        p.add(Box.createVerticalGlue());
        return p;
    }

    /** One navigation row: coloured icon + label, blue tint when selected. */
    private static final class Item extends JButton {
        private boolean selected;
        private boolean hover;
        int badge;
        boolean alert;

        Item(Screen module, Runnable action) {
            super(module.title(), Icons.of(ScreenStyle.icon(module), 19, ScreenStyle.tint(module)));
            setHorizontalAlignment(LEFT);
            setIconTextGap(12);
            setFont(Theme.body());
            setForeground(Theme.LABEL);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            Dimension d = new Dimension(Integer.MAX_VALUE, 38);
            setMaximumSize(d);
            setPreferredSize(new Dimension(200, 38));
            getAccessibleContext().setAccessibleName(module.title());
            addActionListener(e -> action.run());
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean active) {
            this.selected = active;
            setFont(active ? Theme.bodyBold() : Theme.body());
            setForeground(active ? Theme.SELECTION_LABEL : Theme.LABEL);
            getAccessibleContext().setAccessibleDescription(active ? "Current screen" : null);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected || hover || isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selected ? Theme.SELECTION : Theme.hover());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
            super.paintComponent(g);
            if (badge > 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Theme.font(Font.BOLD, 11));
                String s = badge > 99 ? "99+" : String.valueOf(badge);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int w = Math.max(20, fm.stringWidth(s) + 12);
                int h = 20;
                int x = getWidth() - w - 8;
                int y = (getHeight() - h) / 2;
                g2.setColor(alert ? Theme.RED : Theme.FILL);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(alert ? Color.WHITE : Theme.SECONDARY_LABEL);
                g2.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }
}
