package upsa.clinic.shell;

import upsa.clinic.data.ClinicData;
import upsa.clinic.panels.AppointmentPanel;
import upsa.clinic.panels.BedManagementPanel;
import upsa.clinic.panels.BillingPanel;
import upsa.clinic.panels.DashboardPanel;
import upsa.clinic.panels.DoctorsPanel;
import upsa.clinic.panels.MessagesPanel;
import upsa.clinic.panels.PatientVitalsPanel;
import upsa.clinic.panels.PatientsPanel;
import upsa.clinic.panels.PharmacyPanel;
import upsa.clinic.panels.SettingsPanel;
import upsa.clinic.security.AccessPolicy;
import upsa.clinic.security.AuditLog;
import upsa.clinic.security.Screen;
import upsa.clinic.security.User;
import upsa.clinic.ui.Backdrop;
import upsa.clinic.ui.GlassPanel;
import upsa.clinic.ui.Icons;
import upsa.clinic.ui.PillButton;
import upsa.clinic.ui.TablerIcon;
import upsa.clinic.ui.Theme;
import upsa.clinic.ui.Toast;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The signed-in app window: soft backdrop, floating glass sidebar and toolbar, and the
 * content area. Every navigation goes through {@link #open(Screen)}, which enforces
 * {@link AccessPolicy}; screens a role can't view are never even constructed.
 */
public final class MainWindow extends JFrame implements Navigator {

    public static final Duration IDLE_TIMEOUT = Duration.ofMinutes(15);

    private final User user;
    private final Consumer<String> onSignedOut;
    private final Sidebar sidebar;
    private final Toolbar toolbar;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<Screen, JComponent> built = new EnumMap<>(Screen.class);
    private final IdleLock idleLock;
    private Screen current;

    /**
     * @param onSignedOut called after the window closes, with a message for the sign-in
     *                    screen (null for a normal sign-out)
     */
    public MainWindow(User user, Consumer<String> onSignedOut) {
        super("UPSA Clinic");
        this.user = user;
        this.onSignedOut = onSignedOut;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1360, 840);
        setLocationRelativeTo(null);
        setIconImage(iconImage());

        sidebar = new Sidebar(user, this::open, this::confirmSignOut);
        toolbar = new Toolbar(this::goTo);
        toolbar.setNotifications(ClinicData.notificationsFor(user));
        content.setOpaque(false);

        Backdrop root = new Backdrop(new ShellLayout(), 1f);
        root.add(sidebar, "sidebar");
        root.add(toolbar, "toolbar");
        root.add(content, "content");
        setContentPane(root);

        idleLock = new IdleLock(IDLE_TIMEOUT, () -> {
            AuditLog.record(AuditLog.Type.AUTO_LOCK, user.username(), IDLE_TIMEOUT.toMinutes() + " min idle");
            close("You were signed out after " + IDLE_TIMEOUT.toMinutes() + " minutes of inactivity.");
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                idleLock.stop();
            }
        });
        installShortcuts();
        open(Screen.DASHBOARD);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            idleLock.start();
        }
    }

    public User user() {
        return user;
    }

    // ---- Navigation ------------------------------------------------------------------

    @Override
    public void open(Screen module) {
        if (!AccessPolicy.canView(user.role(), module)) {
            AuditLog.record(AuditLog.Type.ACCESS_DENIED, user.username(), module.name());
            toast("You don't have access to " + module.title() + ".", Toast.Kind.WARNING);
            return;
        }
        JComponent screen = built.computeIfAbsent(module, this::build);
        if (screen.getParent() == null) {
            content.add(screen, module.name());
        }
        cards.show(content, module.name());
        current = module;
        sidebar.setSelected(module);
        toolbar.setCrumb(module.title());
        toolbar.setPrimaryAction(primaryActionFor(module));
    }

    @Override
    public void toast(String message, Toast.Kind kind) {
        Toast.show(this, message, kind);
    }

    public Screen currentModule() {
        return current;
    }

    /** Toolbar search: jump to a screen by name or a common synonym. */
    private void goTo(String query) {
        Optional<Screen> match = ScreenSearch.find(query);
        if (match.isEmpty()) {
            toast("No screen matches \"" + query + "\".", Toast.Kind.INFO);
            return;
        }
        open(match.get()); // the guard in open() handles screens this role can't see
    }

    private JComponent build(Screen module) {
        Supplier<JComponent> legacy = switch (module) {
            case DASHBOARD -> null;
            case PATIENTS -> PatientsPanel::new;
            case APPOINTMENTS -> AppointmentPanel::new;
            case VITALS -> PatientVitalsPanel::new;
            case BEDS -> BedManagementPanel::new;
            case DOCTORS -> DoctorsPanel::new;
            case PHARMACY -> PharmacyPanel::new;
            case BILLING -> BillingPanel::new;
            case MESSAGES -> MessagesPanel::new;
            case SETTINGS -> SettingsPanel::new;
        };
        if (legacy == null) {
            JScrollPane scroll = new JScrollPane(new DashboardPanel(user, this));
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            return scroll;
        }
        // Screens not yet redesigned (phase 2) sit in a rounded card so they match the shell.
        return new RoundedClip(legacy.get());
    }

    private PillButton primaryActionFor(Screen module) {
        if (module == Screen.DASHBOARD && user.canEdit(Screen.PATIENTS)) {
            PillButton b = new PillButton("New patient", Icons.of(TablerIcon.PLUS, 16, Color.WHITE),
                    PillButton.Style.PRIMARY);
            b.setToolTipText("Open Patients to register a new patient");
            b.addActionListener(e -> open(Screen.PATIENTS));
            return b;
        }
        if (module == Screen.DASHBOARD && user.canEdit(Screen.PHARMACY)) {
            PillButton b = new PillButton("Dispense", Icons.of(TablerIcon.PILL, 16, Color.WHITE),
                    PillButton.Style.PRIMARY);
            b.addActionListener(e -> open(Screen.PHARMACY));
            return b;
        }
        return null;
    }

    private void installShortcuts() {
        JComponent root = getRootPane();
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Ctrl on Windows/Linux, Cmd on macOS
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_K, menu), "search", toolbar::focusSearch);
        List<Screen> visible = AccessPolicy.visibleModules(user.role());
        for (int i = 0; i < Math.min(9, visible.size()); i++) {
            Screen m = visible.get(i);
            bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, menu), "open-" + m.name(), () -> open(m));
        }
    }

    private static void bind(JComponent c, KeyStroke key, String name, Runnable action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    // ---- Sign out --------------------------------------------------------------------

    private void confirmSignOut() {
        int choice = JOptionPane.showConfirmDialog(this, "Sign out of UPSA Clinic?", "Sign out",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            AuditLog.record(AuditLog.Type.SIGN_OUT, user.username(), "");
            close(null);
        }
    }

    private void close(String message) {
        idleLock.stop();
        dispose();
        onSignedOut.accept(message);
    }

    static java.awt.Image iconImage() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.BLUE);
        g.fillRoundRect(0, 0, 64, 64, 18, 18);
        Icons.of(TablerIcon.HEARTBEAT, 40, Color.WHITE).paintIcon(null, g, 12, 12);
        g.dispose();
        return img;
    }

    // ---- Layout ----------------------------------------------------------------------

    /** Positions the floating sidebar and toolbar with an even inset from the window edge. */
    private static final class ShellLayout implements LayoutManager {
        private static final int INSET = 12 - GlassPanel.shadowInset() + 2;
        private final Map<String, Component> parts = new java.util.HashMap<>();

        @Override public void addLayoutComponent(String name, Component comp) { parts.put(name, comp); }
        @Override public void removeLayoutComponent(Component comp) { parts.values().remove(comp); }
        @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(1360, 840); }
        @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(1100, 700); }

        @Override
        public void layoutContainer(Container parent) {
            int w = parent.getWidth(), h = parent.getHeight();
            int sideW = Sidebar.WIDTH + 2 * GlassPanel.shadowInset();
            Component side = parts.get("sidebar");
            Component bar = parts.get("toolbar");
            Component body = parts.get("content");
            if (side != null) {
                side.setBounds(INSET, INSET, sideW, h - 2 * INSET);
            }
            int left = INSET + sideW - GlassPanel.shadowInset() + 6;
            if (bar != null) {
                bar.setBounds(left - GlassPanel.shadowInset(), INSET, w - left - INSET + GlassPanel.shadowInset(), Toolbar.HEIGHT);
            }
            int top = INSET + Toolbar.HEIGHT - GlassPanel.shadowInset() + 10;
            if (body != null) {
                body.setBounds(left + 4, top, w - left - 4 - 22, h - top - 18);
            }
        }
    }

    /** Clips a legacy screen to rounded corners so it reads as a card in the new shell. */
    private static final class RoundedClip extends JPanel {
        RoundedClip(JComponent child) {
            super(new BorderLayout());
            setOpaque(false);
            add(child, BorderLayout.CENTER);
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Shape clip = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), Theme.RADIUS_CARD, Theme.RADIUS_CARD);
            g2.clip(clip);
            super.paintChildren(g2);
            g2.dispose();
        }
    }

    /** Maps what people type in the toolbar search to a module. */
    static final class ScreenSearch {
        private static final Map<String, Screen> SYNONYMS = Map.ofEntries(
                Map.entry("home", Screen.DASHBOARD), Map.entry("dashboard", Screen.DASHBOARD),
                Map.entry("patient", Screen.PATIENTS), Map.entry("records", Screen.PATIENTS),
                Map.entry("booking", Screen.APPOINTMENTS), Map.entry("schedule", Screen.APPOINTMENTS),
                Map.entry("bp", Screen.VITALS), Map.entry("blood pressure", Screen.VITALS),
                Map.entry("temperature", Screen.VITALS), Map.entry("ward", Screen.BEDS),
                Map.entry("staff", Screen.DOCTORS), Map.entry("drug", Screen.PHARMACY),
                Map.entry("drugs", Screen.PHARMACY), Map.entry("stock", Screen.PHARMACY),
                Map.entry("medicine", Screen.PHARMACY), Map.entry("invoice", Screen.BILLING),
                Map.entry("payment", Screen.BILLING), Map.entry("inbox", Screen.MESSAGES),
                Map.entry("chat", Screen.MESSAGES), Map.entry("preferences", Screen.SETTINGS));

        static Optional<Screen> find(String query) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
                return Optional.empty();
            }
            for (Screen m : Screen.values()) {
                if (m.title().toLowerCase(Locale.ROOT).startsWith(q)) {
                    return Optional.of(m);
                }
            }
            if (SYNONYMS.containsKey(q)) {
                return Optional.of(SYNONYMS.get(q));
            }
            for (Screen m : Screen.values()) {
                if (m.title().toLowerCase(Locale.ROOT).contains(q)) {
                    return Optional.of(m);
                }
            }
            return Optional.empty();
        }
    }
}
