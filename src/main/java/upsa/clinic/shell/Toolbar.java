package upsa.clinic.shell;

import upsa.clinic.ui.GlassPanel;
import upsa.clinic.ui.IconField;
import upsa.clinic.ui.Icons;
import upsa.clinic.ui.PillButton;
import upsa.clinic.ui.TablerIcon;
import upsa.clinic.ui.Theme;

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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/** Floating glass toolbar: breadcrumb, go-to search, notifications and one primary action. */
final class Toolbar extends GlassPanel {

    static final int HEIGHT = 56 + 2 * GlassPanel.shadowInset();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM");

    private final JLabel crumb = new JLabel();
    private final IconField search = IconField.search("Go to a screen  (Ctrl+K)");
    private final BellButton bell = new BellButton();
    private final JPanel actionSlot = new JPanel(new BorderLayout());
    private List<String> notifications = List.of();

    Toolbar(Consumer<String> onSearch) {
        super(new BorderLayout(12, 0), 28);
        int s = shadowInset();
        setBorder(BorderFactory.createEmptyBorder(s + 10, s + 20, s + 10, s + 10));

        crumb.setFont(Theme.font(Font.PLAIN, 13));
        crumb.setForeground(Theme.SECONDARY_LABEL);
        add(crumb, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        search.setPreferredSize(new Dimension(320, 36));
        javax.swing.JTextField field = (javax.swing.JTextField) search.input();
        field.addActionListener(e -> {
            String q = field.getText().trim();
            if (!q.isEmpty()) {
                onSearch.accept(q);
                search.input().setText("");
            }
        });
        right.add(search);
        bell.addActionListener(e -> showNotifications());
        right.add(bell);
        actionSlot.setOpaque(false);
        right.add(actionSlot);
        add(right, BorderLayout.EAST);
    }

    void setCrumb(String title) {
        crumb.setText(title + "  ·  " + LocalDate.now().format(DATE));
    }

    void focusSearch() {
        search.input().requestFocusInWindow();
    }

    /** Shows the screen's primary action, or hides the slot when {@code button} is null. */
    void setPrimaryAction(PillButton button) {
        actionSlot.removeAll();
        if (button != null) {
            actionSlot.add(button);
        }
        actionSlot.revalidate();
        actionSlot.repaint();
    }

    void setNotifications(List<String> items) {
        this.notifications = List.copyOf(items);
        bell.unread = !items.isEmpty();
        bell.setToolTipText(items.isEmpty() ? "No new notifications" : items.size() + " new notifications");
        bell.repaint();
    }

    private void showNotifications() {
        JPopupMenu popup = new JPopupMenu();
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        list.setBackground(Color.WHITE);
        JLabel head = new JLabel("Notifications");
        head.setFont(Theme.headline());
        list.add(head);
        list.add(Box.createVerticalStrut(8));
        if (notifications.isEmpty()) {
            JLabel none = new JLabel("You're all caught up.");
            none.setFont(Theme.body());
            none.setForeground(Theme.SECONDARY_LABEL);
            list.add(none);
        }
        for (String n : notifications) {
            JLabel row = new JLabel(n, Icons.of(TablerIcon.INFO_CIRCLE, 16, Theme.BLUE), JLabel.LEFT);
            row.setFont(Theme.body());
            row.setIconTextGap(8);
            row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            list.add(row);
        }
        list.setPreferredSize(new Dimension(320, list.getPreferredSize().height));
        popup.add(list);
        popup.show(bell, bell.getWidth() - 320, bell.getHeight() + 8);
        bell.unread = false;
        bell.repaint();
    }

    /** Round bell button with a red unread dot. */
    private static final class BellButton extends JButton {
        boolean unread;

        BellButton() {
            super(Icons.of(TablerIcon.BELL, 19, Theme.LABEL));
            setPreferredSize(new Dimension(36, 36));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Notifications");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? new Color(255, 255, 255, 230) : new Color(255, 255, 255, 150));
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
            if (unread) {
                Graphics2D d = (Graphics2D) g.create();
                d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(Color.WHITE);
                d.fillOval(getWidth() - 14, 4, 11, 11);
                d.setColor(Theme.RED);
                d.fillOval(getWidth() - 12, 6, 7, 7);
                d.dispose();
            }
        }
    }
}
