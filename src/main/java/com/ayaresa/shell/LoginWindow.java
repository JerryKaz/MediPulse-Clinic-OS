package com.ayaresa.shell;

import com.ayaresa.security.AuthService;
import com.ayaresa.security.User;
import com.ayaresa.ui.Backdrop;
import com.ayaresa.ui.GlassPanel;
import com.ayaresa.ui.IconField;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.util.function.Consumer;

/** Sign-in screen: a glass card over the soft backdrop. Enter submits; errors show inline. */
public final class LoginWindow extends JFrame {

    private final AuthService auth;
    private final Consumer<User> onSignedIn;
    private final IconField username = IconField.text(TablerIcon.USER, "Username");
    private final IconField password = IconField.password(TablerIcon.LOCK, "Password");
    private final JLabel message = new JLabel(" ");
    private final PillButton signIn = new PillButton("Sign in", null, PillButton.Style.PRIMARY);

    public LoginWindow(AuthService auth, String notice, Consumer<User> onSignedIn) {
        super(com.ayaresa.Brand.NAME + " — Sign in");
        this.auth = auth;
        this.onSignedIn = onSignedIn;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(520, 640);
        setResizable(false);
        setLocationRelativeTo(null);
        setIconImage(MainWindow.iconImage());

        Backdrop bg = new Backdrop(new GridBagLayout(), 2.6f);
        bg.add(card());
        setContentPane(bg);
        getRootPane().setDefaultButton(signIn);

        if (notice != null) {
            showMessage(notice, Theme.SECONDARY_LABEL);
        }
    }

    private JComponent card() {
        GlassPanel card = new GlassPanel(null, 30);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        int s = GlassPanel.shadowInset();
        card.setBorder(BorderFactory.createEmptyBorder(s + 30, s + 28, s + 26, s + 28));
        card.setPreferredSize(new Dimension(400 + 2 * s, 480 + 2 * s));

        JLabel logo = new JLabel(Icons.of(TablerIcon.HEARTBEAT, 30, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        fixed(logo, 56, 56);

        JLabel title = new JLabel(com.ayaresa.Brand.NAME);
        title.setFont(Theme.font(Font.BOLD, 24));
        title.setForeground(Theme.LABEL);
        JLabel sub = new JLabel("Sign in to continue");
        sub.setFont(Theme.body());
        sub.setForeground(Theme.SECONDARY_LABEL);

        JPasswordField pw = (JPasswordField) password.input();
        JButton reveal = new JButton(Icons.of(TablerIcon.EYE, 18, Theme.TERTIARY_LABEL));
        reveal.setContentAreaFilled(false);
        reveal.setBorderPainted(false);
        reveal.setFocusPainted(false);
        reveal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reveal.setToolTipText("Show password");
        reveal.getAccessibleContext().setAccessibleName("Show password");
        char echo = pw.getEchoChar();
        reveal.addActionListener(e -> {
            boolean hidden = pw.getEchoChar() != 0;
            pw.setEchoChar(hidden ? (char) 0 : echo);
            reveal.setIcon(Icons.of(hidden ? TablerIcon.EYE_OFF : TablerIcon.EYE, 18, Theme.TERTIARY_LABEL));
            reveal.setToolTipText(hidden ? "Hide password" : "Show password");
        });
        password.setTrailing(reveal);

        // Clear the error state as soon as the user edits either field.
        javax.swing.event.DocumentListener clear = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { clearError(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { clearError(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        };
        username.input().getDocument().addDocumentListener(clear);
        pw.getDocument().addDocumentListener(clear);

        message.setFont(Theme.font(Font.PLAIN, 13));
        message.setHorizontalAlignment(SwingConstants.CENTER);

        signIn.setFont(Theme.font(Font.BOLD, 16));
        signIn.addActionListener(e -> submit());

        JLabel hint = new JLabel("<html><div style='text-align:center'>Demo: admin / 1234 · doctor1 / 3333 · nurse1 / 2222<br>pharm1 / 4444 · front1 / 5555</div></html>");
        hint.setFont(Theme.footnote());
        hint.setForeground(Theme.TERTIARY_LABEL);
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        for (JComponent c : new JComponent[]{logo, title, sub, message, hint}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        stretch(username, 46);
        stretch(password, 46);
        stretch(signIn, 48);
        stretch(message, 20);

        card.add(logo);
        card.add(Box.createVerticalStrut(14));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(22));
        card.add(username);
        card.add(Box.createVerticalStrut(10));
        card.add(password);
        card.add(Box.createVerticalStrut(10));
        card.add(message);
        card.add(Box.createVerticalStrut(8));
        card.add(signIn);
        card.add(Box.createVerticalGlue());
        card.add(hint);

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.add(card);
        return wrap;
    }

    private void submit() {
        String user = ((JTextField) username.input()).getText().trim();
        char[] pass = ((JPasswordField) password.input()).getPassword();
        if (user.isEmpty() || pass.length == 0) {
            username.setError(user.isEmpty());
            password.setError(pass.length == 0);
            showMessage("Enter your username and password.", Theme.RED);
            (user.isEmpty() ? username : password).input().requestFocusInWindow();
            return;
        }
        signIn.setEnabled(false);
        signIn.setText("Signing in…");
        // Hashing is deliberately slow; keep it off the UI thread so the window stays responsive.
        new SwingWorker<AuthService.Result, Void>() {
            @Override
            protected AuthService.Result doInBackground() {
                return auth.signIn(user, pass);
            }

            @Override
            protected void done() {
                signIn.setEnabled(true);
                signIn.setText("Sign in");
                AuthService.Result result;
                try {
                    result = get();
                } catch (Exception ex) {
                    showMessage("Something went wrong. Try again.", Theme.RED);
                    return;
                }
                handle(result);
            }
        }.execute();
    }

    private void handle(AuthService.Result result) {
        if (result instanceof AuthService.Success s) {
            dispose();
            onSignedIn.accept(s.user());
        } else if (result instanceof AuthService.Failure f) {
            password.input().setText(""); // clear first: editing fires clearError()
            username.setError(true);
            password.setError(true);
            password.input().requestFocusInWindow();
            showMessage(f.attemptsLeft() <= 2
                    ? "Incorrect username or password. " + f.attemptsLeft() + " attempts left."
                    : "Incorrect username or password.", Theme.RED);
        } else if (result instanceof AuthService.LockedOut l) {
            password.input().setText("");
            password.setError(true);
            showMessage("Too many attempts. Try again in " + Math.max(1, l.remaining().toSeconds()) + " seconds.", Theme.RED);
        }
    }

    private void clearError() {
        username.setError(false);
        password.setError(false);
        if (Theme.RED.equals(message.getForeground())) {
            showMessage(" ", Theme.SECONDARY_LABEL);
        }
    }

    private void showMessage(String text, Color color) {
        message.setText(text);
        message.setForeground(color);
    }

    private static void fixed(JComponent c, int w, int h) {
        Dimension d = new Dimension(w, h);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
    }

    private static void stretch(JComponent c, int h) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.setPreferredSize(new Dimension(340, h));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
    }
}
