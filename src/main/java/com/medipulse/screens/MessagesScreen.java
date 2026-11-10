package com.medipulse.screens;

import com.medipulse.domain.Message;
import com.medipulse.shell.AppContext;
import com.medipulse.store.ClinicStore;
import com.medipulse.ui.Avatar;
import com.medipulse.ui.Card;
import com.medipulse.ui.Fields;
import com.medipulse.ui.Icons;
import com.medipulse.ui.PillButton;
import com.medipulse.ui.TablerIcon;
import com.medipulse.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/** Team channels (Ward A, Pharmacy, Front desk) for quick handovers. Your messages appear on the right. */
public final class MessagesScreen extends ScreenView {

    private final JPanel threads = vertical();
    private final JPanel conversation = vertical();
    private final JScrollPane convoScroll;
    private final JTextField compose = Fields.text("", "Write a message");
    private final JLabel threadTitle = sectionTitle("");
    private String current;

    public MessagesScreen(AppContext app) {
        super(app, ClinicStore.Topic.MESSAGES);
        add(header("Messages", "Team channels for handovers and requests"), BorderLayout.NORTH);

        Card left = new Card(new BorderLayout());
        left.setPreferredSize(new Dimension(280, 0));
        left.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(threads, BorderLayout.NORTH);
        left.add(holder, BorderLayout.CENTER);

        Card right = new Card(new BorderLayout(0, 10));
        right.add(threadTitle, BorderLayout.NORTH);
        convoScroll = com.medipulse.ui.VerticalScroll.of(conversation);
        right.add(convoScroll, BorderLayout.CENTER);
        JPanel composer = new JPanel(new BorderLayout(8, 0));
        composer.setOpaque(false);
        compose.addActionListener(e -> send());
        composer.add(compose, BorderLayout.CENTER);
        PillButton send = new PillButton("Send", Icons.of(TablerIcon.SEND, 16, Color.WHITE), PillButton.Style.PRIMARY);
        send.addActionListener(e -> send());
        composer.add(send, BorderLayout.EAST);
        right.add(composer, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(Theme.GAP, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private void send() {
        if (current != null && !compose.getText().isBlank()) {
            app.store().post(current, app.user().displayName(), compose.getText());
            compose.setText("");
        }
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        List<String> all = s.threads();
        if (current == null && !all.isEmpty()) {
            current = all.get(0);
        }
        threads.removeAll();
        for (String t : all) {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    if (t.equals(current)) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Theme.SELECTION);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            b.setLayout(new BorderLayout(10, 0));
            b.add(new JLabel(Icons.of(TablerIcon.MESSAGE_CIRCLE, 20, Theme.GREEN)), BorderLayout.WEST);
            JPanel text = vertical();
            JLabel name = new JLabel(t);
            name.setFont(Theme.bodyBold());
            name.setForeground(Theme.LABEL);
            text.add(name);
            String last = s.lastMessage(t).map(m -> m.from().split(" ")[0] + ": " + m.text()).orElse("");
            text.add(secondary(last.length() > 32 ? last.substring(0, 31) + "…" : last));
            b.add(text, BorderLayout.CENTER);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.getAccessibleContext().setAccessibleName(t);
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
            b.addActionListener(e -> {
                current = t;
                refresh();
            });
            threads.add(b);
        }
        threads.revalidate();
        threads.repaint();

        threadTitle.setText(current == null ? "" : current);
        conversation.removeAll();
        if (current != null) {
            for (Message m : s.messages(current)) {
                conversation.add(bubble(m, m.from().equals(app.user().displayName())));
                conversation.add(Box.createVerticalStrut(10));
            }
        }
        conversation.revalidate();
        conversation.repaint();
        SwingUtilities.invokeLater(() -> convoScroll.getVerticalScrollBar().setValue(convoScroll.getVerticalScrollBar().getMaximum()));
    }

    private JPanel bubble(Message m, boolean mine) {
        JPanel row = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        if (!mine) {
            row.add(new Avatar(Avatar.initialsOf(m.from().replace("Dr. ", "")), m.from(), 30));
        }
        JPanel b = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(mine ? Theme.BLUE : Theme.FILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel who = new JLabel((mine ? "You" : m.from()) + "  ·  " + m.at().format(Format.TIME));
        who.setFont(Theme.font(Font.BOLD, 11));
        who.setForeground(mine ? new Color(255, 255, 255, 200) : Theme.SECONDARY_LABEL);
        JLabel text = new JLabel("<html><div style='width:" + Math.min(360, Math.max(80, m.text().length() * 7)) + "px'>"
                + m.text().replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
        text.setFont(Theme.body());
        text.setForeground(mine ? Color.WHITE : Theme.LABEL);
        b.add(who, BorderLayout.NORTH);
        b.add(text, BorderLayout.CENTER);
        row.add(b);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
