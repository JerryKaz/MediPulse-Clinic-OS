package com.ayaresa.shell;

import com.ayaresa.ui.GlassPanel;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Floating glass toolbar: breadcrumb, command palette launcher, alerts and one primary action. */
final class Toolbar extends GlassPanel {

    static final int HEIGHT = 56 + 2 * GlassPanel.shadowInset();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM");

    /** One alert in the bell menu; {@code onOpen} navigates to its source. */
    record Alert(String text, TablerIcon icon, Color tint, Runnable onOpen) {
    }

    private final JLabel crumb = new JLabel();
    private final BellButton bell = new BellButton();
    private final JPanel actionSlot = new JPanel(new BorderLayout());
    private List<Alert> alerts = List.of();

    Toolbar(Runnable openPalette) {
        super(new BorderLayout(12, 0), 28);
        int s = shadowInset();
        setBorder(BorderFactory.createEmptyBorder(s + 10, s + 20, s + 10, s + 10));

        crumb.setFont(Theme.font(Font.PLAIN, 13));
        crumb.setForeground(Theme.SECONDARY_LABEL);
        add(crumb, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        SearchLauncher launcher = new SearchLauncher();
        launcher.addActionListener(e -> openPalette.run());
        right.add(launcher);
        bell.addActionListener(e -> showAlerts());
        right.add(bell);
        actionSlot.setOpaque(false);
        right.add(actionSlot);
        add(right, BorderLayout.EAST);
    }

    void setCrumb(String title) {
        crumb.setText(title + "  ·  " + LocalDate.now().format(DATE));
    }

    void setPrimaryAction(PillButton button) {
        actionSlot.removeAll();
        if (button != null) {
            actionSlot.add(button);
        }
        actionSlot.revalidate();
        actionSlot.repaint();
    }

    void setAlerts(List<Alert> items) {
        boolean grew = items.size() > alerts.size();
        this.alerts = List.copyOf(items);
        if (grew || items.isEmpty()) {
            bell.unread = !items.isEmpty();
        }
        bell.count = items.size();
        bell.setToolTipText(items.isEmpty() ? "No alerts" : items.size() + " alerts");
        bell.getAccessibleContext().setAccessibleDescription(bell.getToolTipText());
        bell.repaint();
    }

    private void showAlerts() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.SEPARATOR));
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        list.setBackground(Theme.RAISED_BG);
        JLabel head = new JLabel("Alerts");
        head.setFont(Theme.headline());
        head.setForeground(Theme.LABEL);
        list.add(head);
        list.add(Box.createVerticalStrut(8));
        if (alerts.isEmpty()) {
            JLabel none = new JLabel("You're all caught up.");
            none.setFont(Theme.body());
            none.setForeground(Theme.SECONDARY_LABEL);
            list.add(none);
        }
        for (Alert a : alerts) {
            JButton row = new JButton("<html><div style='width:250px'>" + a.text() + "</div></html>", Icons.of(a.icon(), 18, a.tint()));
            row.setHorizontalAlignment(JButton.LEFT);
            row.setFont(Theme.body());
            row.setForeground(Theme.LABEL);
            row.setIconTextGap(10);
            row.setContentAreaFilled(false);
            row.setBorderPainted(false);
            row.setFocusPainted(false);
            row.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 0));
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.addActionListener(e -> {
                popup.setVisible(false);
                a.onOpen().run();
            });
            list.add(row);
        }
        list.setPreferredSize(new Dimension(330, list.getPreferredSize().height));
        popup.add(list);
        popup.show(bell, bell.getWidth() - 330, bell.getHeight() + 8);
        bell.unread = false;
        bell.repaint();
    }

    /** Looks like a search field; opens the command palette. */
    private static final class SearchLauncher extends JButton {
        SearchLauncher() {
            setPreferredSize(new Dimension(320, 36));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            getAccessibleContext().setAccessibleName("Search patients, screens and actions");
            setToolTipText("Search patients, screens and actions");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? Theme.FILL.darker() : Theme.FILL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            Icons.of(TablerIcon.SEARCH, 17, Theme.TERTIARY_LABEL).paintIcon(this, g2, 12, (getHeight() - 17) / 2);
            g2.setFont(Theme.body());
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(Theme.TERTIARY_LABEL);
            g2.drawString("Search patients, screens, actions", 38, baseline);
            boolean mac = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() == java.awt.event.InputEvent.META_DOWN_MASK;
            String key = mac ? "⌘K" : "Ctrl K";
            g2.setFont(Theme.font(Font.BOLD, 11));
            fm = g2.getFontMetrics();
            int kw = fm.stringWidth(key) + 12;
            int kx = getWidth() - kw - 10;
            g2.setColor(Theme.hover());
            g2.fillRoundRect(kx, (getHeight() - 20) / 2, kw, 20, 8, 8);
            g2.setColor(Theme.SECONDARY_LABEL);
            g2.drawString(key, kx + 6, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Round bell with a red dot for unseen alerts. */
    private static final class BellButton extends JButton {
        boolean unread;
        int count;

        BellButton() {
            super(Icons.of(TablerIcon.BELL, 19, Theme.LABEL));
            setPreferredSize(new Dimension(36, 36));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Alerts");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = Theme.isDark() ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 150);
            g2.setColor(getModel().isRollover() ? Theme.FILL : base);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
            if (unread && count > 0) {
                Graphics2D d = (Graphics2D) g.create();
                d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(Theme.CARD_BG);
                d.fillOval(getWidth() - 14, 3, 12, 12);
                d.setColor(Theme.RED);
                d.fillOval(getWidth() - 12, 5, 8, 8);
                d.dispose();
            }
        }
    }
}
