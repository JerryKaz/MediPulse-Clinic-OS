package com.ayaresa.shell;

import com.ayaresa.Brand;
import com.ayaresa.Preferences;
import com.ayaresa.clinical.News2;
import com.ayaresa.domain.Drug;
import com.ayaresa.domain.InvoiceStatus;
import com.ayaresa.domain.Priority;
import com.ayaresa.domain.QueueEntry;
import com.ayaresa.domain.Stage;
import com.ayaresa.domain.VitalSigns;
import com.ayaresa.screens.AppointmentsScreen;
import com.ayaresa.screens.BedsScreen;
import com.ayaresa.screens.BillingScreen;
import com.ayaresa.screens.MessagesScreen;
import com.ayaresa.screens.PatientChart;
import com.ayaresa.screens.PatientsScreen;
import com.ayaresa.screens.PharmacyScreen;
import com.ayaresa.screens.QueueScreen;
import com.ayaresa.screens.ScreenView;
import com.ayaresa.screens.SettingsScreen;
import com.ayaresa.screens.Sheets;
import com.ayaresa.screens.StaffScreen;
import com.ayaresa.screens.TodayScreen;
import com.ayaresa.screens.VitalsScreen;
import com.ayaresa.security.AccessPolicy;
import com.ayaresa.security.AuditLog;
import com.ayaresa.security.Screen;
import com.ayaresa.security.User;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Backdrop;
import com.ayaresa.ui.GlassPanel;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.Sheet;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;
import com.ayaresa.ui.Toast;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The signed-in window: soft backdrop, floating glass sidebar and toolbar, and the screen
 * area. Every navigation goes through {@link #open(Screen)}, which enforces the access
 * policy; screens a role can't view are never constructed.
 */
public final class MainWindow extends JFrame implements AppContext {

    private final ClinicStore store;
    private final User user;
    private final Consumer<String> onSignedOut;
    private final Sidebar sidebar;
    private final Toolbar toolbar;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<Screen, ScreenView> built = new EnumMap<>(Screen.class);
    private final IdleLock idleLock;
    private final AutoCloseable subscription;
    private Screen current;

    /**
     * @param onSignedOut called after sign-out with a message for the sign-in screen (or null)
     */
    public MainWindow(ClinicStore store, User user, Consumer<String> onSignedOut) {
        super(Brand.NAME);
        this.store = store;
        this.user = user;
        this.onSignedOut = onSignedOut;
        store.setActor(user.username());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1400, 860);
        setLocationRelativeTo(null);
        setIconImage(iconImage());

        sidebar = new Sidebar(user, this::open, this::confirmSignOut);
        toolbar = new Toolbar(this::openPalette);
        content.setOpaque(false);

        Backdrop root = new Backdrop(new ShellLayout(), 1f);
        root.add(sidebar, "sidebar");
        root.add(toolbar, "toolbar");
        root.add(content, "content");
        setContentPane(root);

        idleLock = new IdleLock(Preferences.idleTimeout(), () -> {
            AuditLog.record(AuditLog.Type.AUTO_LOCK, user.username(), Preferences.idleTimeout().toMinutes() + " min idle");
            close("You were signed out after " + Preferences.idleTimeout().toMinutes() + " minutes of inactivity.");
        });
        subscription = store.subscribe(this::refreshStatus, ClinicStore.Topic.values());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                teardown();
            }
        });
        installShortcuts();
        refreshStatus();
        open(Screen.DASHBOARD);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            idleLock.start();
        }
    }

    // ---- AppContext ------------------------------------------------------------------

    @Override public ClinicStore store() { return store; }
    @Override public User user() { return user; }
    @Override public Component window() { return this; }

    @Override
    public void open(Screen screen) {
        if (!AccessPolicy.canView(user.role(), screen)) {
            AuditLog.record(AuditLog.Type.ACCESS_DENIED, user.username(), screen.name());
            toast("You don't have access to " + screen.title() + ".", Toast.Kind.WARNING);
            return;
        }
        ScreenView view = built.computeIfAbsent(screen, this::build);
        if (view.getParent() == null) {
            content.add(view, screen.name());
        }
        cards.show(content, screen.name());
        current = screen;
        sidebar.setSelected(screen);
        toolbar.setCrumb(screen.title());
        toolbar.setPrimaryAction(view.primaryAction());
        view.shown();
    }

    @Override
    public void openPatient(String patientId) {
        if (!user.canView(Screen.PATIENTS)) {
            toast("You don't have access to patient records.", Toast.Kind.WARNING);
            return;
        }
        PatientChart.open(this, patientId);
    }

    @Override
    public void toast(String message, Toast.Kind kind) {
        Toast.show(this, message, kind);
    }

    /** Re-creates the window (keeping the user signed in) so a new appearance takes effect. */
    @Override
    public void rebuild() {
        Screen keep = current;
        MainWindow next = new MainWindow(store, user, onSignedOut);
        next.setBounds(getBounds());
        next.setExtendedState(getExtendedState());
        if (keep != null) {
            next.open(keep);
        }
        teardown();
        dispose();
        next.setVisible(true);
    }

    public Screen currentScreen() {
        return current;
    }

    // ---- Screens ---------------------------------------------------------------------

    private ScreenView build(Screen screen) {
        return switch (screen) {
            case DASHBOARD -> new TodayScreen(this);
            case QUEUE -> new QueueScreen(this);
            case PATIENTS -> new PatientsScreen(this);
            case APPOINTMENTS -> new AppointmentsScreen(this);
            case VITALS -> new VitalsScreen(this);
            case BEDS -> new BedsScreen(this);
            case STAFF -> new StaffScreen(this);
            case PHARMACY -> new PharmacyScreen(this);
            case BILLING -> new BillingScreen(this);
            case MESSAGES -> new MessagesScreen(this);
            case SETTINGS -> new SettingsScreen(this);
        };
    }

    /** Sidebar badges and toolbar alerts, recomputed whenever data changes. */
    private void refreshStatus() {
        List<QueueEntry> queue = store.queue();
        long waiting = queue.stream().filter(q -> q.stage() != Stage.DONE).count();
        boolean emergency = queue.stream().anyMatch(q -> q.stage() != Stage.DONE && q.priority() == Priority.EMERGENCY);
        sidebar.setBadge(Screen.QUEUE, (int) waiting, emergency);

        LocalDate today = store.today();
        List<Drug> drugs = store.drugs();
        long expired = drugs.stream().filter(d -> d.expired(today)).count();
        long low = drugs.stream().filter(Drug::lowStock).count();
        sidebar.setBadge(Screen.PHARMACY, (int) (expired + low), expired > 0);
        long rejected = store.invoices().stream().filter(i -> i.status() == InvoiceStatus.CLAIM_REJECTED).count();
        sidebar.setBadge(Screen.BILLING, (int) rejected, rejected > 0);

        List<Toolbar.Alert> alerts = new ArrayList<>();
        if (user.canView(Screen.VITALS)) {
            for (VitalSigns v : store.recentVitals(40)) {
                if (v.takenAt().isAfter(store.now().minusHours(12))) {
                    News2.Result r = News2.score(v);
                    if (r.risk() == News2.Risk.HIGH || r.risk() == News2.Risk.MEDIUM) {
                        String name = store.patient(v.patientId()).map(p -> p.name()).orElse(v.patientId());
                        alerts.add(new Toolbar.Alert(name + ": NEWS2 " + r.total() + " (" + r.risk().label() + " risk)",
                                TablerIcon.ALERT_OCTAGON, Theme.RED, () -> openPatient(v.patientId())));
                    }
                }
            }
        }
        if (user.canView(Screen.QUEUE)) {
            queue.stream().filter(q -> q.stage() != Stage.DONE && q.priority() == Priority.EMERGENCY)
                    .forEach(q -> alerts.add(new Toolbar.Alert("Emergency in queue: " + q.tokenLabel() + " "
                            + store.patient(q.patientId()).map(p -> p.name()).orElse(""),
                            TablerIcon.AMBULANCE, Theme.RED, () -> open(Screen.QUEUE))));
        }
        if (user.canView(Screen.PHARMACY)) {
            drugs.stream().filter(d -> d.expired(today)).forEach(d -> alerts.add(new Toolbar.Alert(
                    d.name() + " has expired. Quarantine the batch.", TablerIcon.ALERT_TRIANGLE, Theme.ORANGE, () -> open(Screen.PHARMACY))));
            if (low > 0) {
                alerts.add(new Toolbar.Alert(low + " medicines are at or below reorder level.",
                        TablerIcon.PACKAGE, Theme.ORANGE, () -> open(Screen.PHARMACY)));
            }
        }
        if (user.canView(Screen.BILLING) && rejected > 0) {
            alerts.add(new Toolbar.Alert(rejected + " NHIS claim" + (rejected == 1 ? " was" : "s were") + " rejected.",
                    TablerIcon.FILE_INVOICE, Theme.ORANGE, () -> open(Screen.BILLING)));
        }
        toolbar.setAlerts(alerts);
    }

    private void openPalette() {
        List<CommandPalette.Action> actions = List.of(
                new CommandPalette.Action("Register a new patient", "add create patient", TablerIcon.USER_PLUS,
                        Screen.PATIENTS, () -> Sheets.newPatient(this)),
                new CommandPalette.Action("Check in a patient", "queue arrive walk-in token", TablerIcon.DOOR_ENTER,
                        Screen.QUEUE, () -> Sheets.checkIn(this, null)),
                new CommandPalette.Action("Record vitals", "observations news2 blood pressure temperature",
                        TablerIcon.ACTIVITY_HEARTBEAT, Screen.VITALS, () -> open(Screen.VITALS)),
                new CommandPalette.Action("Book an appointment", "schedule", TablerIcon.CALENDAR_PLUS,
                        Screen.APPOINTMENTS, () -> Sheets.book(this, null)),
                new CommandPalette.Action("Dispense medicine", "pharmacy drug", TablerIcon.PILL,
                        Screen.PHARMACY, () -> Sheets.dispense(this, null, null)),
                new CommandPalette.Action(Theme.isDark() ? "Switch to light mode" : "Switch to dark mode",
                        "theme appearance dark light", Theme.isDark() ? TablerIcon.SUN : TablerIcon.MOON,
                        Screen.DASHBOARD, this::toggleDarkMode));
        CommandPalette.show(this, actions);
    }

    void toggleDarkMode() {
        boolean dark = !Theme.isDark();
        Preferences.setDarkMode(dark);
        Theme.setDark(dark);
        rebuild();
    }

    private void installShortcuts() {
        JComponent root = getRootPane();
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Ctrl, or Cmd on macOS
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_K, menu), "palette", this::openPalette);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, menu), "new-patient", () -> {
            if (user.canEdit(Screen.PATIENTS)) {
                Sheets.newPatient(this);
            }
        });
        List<Screen> visible = AccessPolicy.visibleScreens(user.role());
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
        Sheet.confirm(this, "Sign out?", "You'll need your password to sign back in.", "Sign out", false, () -> {
            AuditLog.record(AuditLog.Type.SIGN_OUT, user.username(), "");
            close(null);
        });
    }

    private void close(String message) {
        teardown();
        dispose();
        onSignedOut.accept(message);
    }

    private boolean tornDown;

    private void teardown() {
        if (tornDown) {
            return;
        }
        tornDown = true;
        idleLock.stop();
        built.values().forEach(ScreenView::dispose);
        try {
            subscription.close();
        } catch (Exception ignored) {
            // unsubscribing can't fail
        }
    }

    public static java.awt.Image iconImage() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.BLUE);
        g.fillRoundRect(0, 0, 64, 64, 18, 18);
        Icons.of(TablerIcon.HEARTBEAT, 40, Color.WHITE).paintIcon(null, g, 12, 12);
        g.dispose();
        return img;
    }

    /** Positions the floating sidebar and toolbar with an even inset from the window edge. */
    private static final class ShellLayout implements LayoutManager {
        private static final int INSET = 12 - GlassPanel.shadowInset() + 2;
        private final Map<String, Component> parts = new HashMap<>();

        @Override public void addLayoutComponent(String name, Component comp) { parts.put(name, comp); }
        @Override public void removeLayoutComponent(Component comp) { parts.values().remove(comp); }
        @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(1400, 860); }
        @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(1180, 720); }

        @Override
        public void layoutContainer(Container parent) {
            int w = parent.getWidth(), h = parent.getHeight();
            int s = GlassPanel.shadowInset();
            int sideW = Sidebar.WIDTH + 2 * s;
            Component side = parts.get("sidebar");
            Component bar = parts.get("toolbar");
            Component body = parts.get("content");
            if (side != null) {
                side.setBounds(INSET, INSET, sideW, h - 2 * INSET);
            }
            int left = INSET + sideW - s + 6;
            if (bar != null) {
                bar.setBounds(left - s, INSET, w - left - INSET + s, Toolbar.HEIGHT);
            }
            int top = INSET + Toolbar.HEIGHT - s + 10;
            if (body != null) {
                body.setBounds(left + 4, top, w - left - 4 - 22, h - top - 16);
            }
        }
    }
}
