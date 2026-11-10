package com.medipulse.screens;

import com.medipulse.Brand;
import com.medipulse.Preferences;
import com.medipulse.security.AuditLog;
import com.medipulse.security.Screen;
import com.medipulse.shell.AppContext;
import com.medipulse.store.ClinicStore;
import com.medipulse.ui.Card;
import com.medipulse.ui.DataTable;
import com.medipulse.ui.Fields;
import com.medipulse.ui.SegmentedControl;
import com.medipulse.ui.Sheet;
import com.medipulse.ui.StatusPill;
import com.medipulse.ui.TablerIcon;
import com.medipulse.ui.Theme;
import com.medipulse.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Duration;
import java.util.List;

import static com.medipulse.ui.DataTable.col;

/**
 * Appearance for everyone (light/dark, Liquid Glass intensity like the iOS 27 slider).
 * Security and data tools (idle sign-out, audit log, reset) for administrators only.
 */
public final class SettingsScreen extends ScreenView {

    private final DataTable<AuditLog.Entry> audit = new DataTable<>(List.of(
            col("When", e -> e.at().format(Format.DATE_TIME), 130, DataTable.Kind.SECONDARY),
            col("Who", AuditLog.Entry::username, 100, DataTable.Kind.BOLD),
            col("Event", e -> new DataTable.Pill(e.type().name().replace('_', ' ').toLowerCase(),
                    e.type() == AuditLog.Type.ACCESS_DENIED || e.type() == AuditLog.Type.LOCKED_OUT || e.type() == AuditLog.Type.SIGN_IN_FAILED
                            ? StatusPill.Kind.WARNING : StatusPill.Kind.IDLE), 150, DataTable.Kind.PILL),
            col("Detail", AuditLog.Entry::detail, 380, DataTable.Kind.TEXT)));

    public SettingsScreen(AppContext app) {
        super(app, ClinicStore.Topic.values());
        boolean admin = app.canEdit(Screen.SETTINGS);
        add(header("Settings", admin ? "Appearance, security and data" : "Appearance"), BorderLayout.NORTH);

        JPanel cols = new JPanel(new BorderLayout(Theme.GAP, 0));
        cols.setOpaque(false);
        JPanel left = vertical();
        left.add(card("Appearance", appearance()));
        if (admin) {
            left.add(gap(Theme.GAP));
            left.add(card("Security", security()));
            left.add(gap(Theme.GAP));
            left.add(card("Data", data()));
        }
        JScrollPane leftScroll = com.medipulse.ui.VerticalScroll.of(left);
        leftScroll.setPreferredSize(new Dimension(420, 0));
        cols.add(leftScroll, BorderLayout.WEST);
        if (admin) {
            JPanel right = new JPanel(new BorderLayout(0, 10));
            right.setOpaque(false);
            right.add(sectionTitle("Audit log"), BorderLayout.NORTH);
            right.add(audit, BorderLayout.CENTER);
            cols.add(right, BorderLayout.CENTER);
        }
        add(cols, BorderLayout.CENTER);
    }

    private JComponent appearance() {
        SegmentedControl mode = new SegmentedControl(List.of("Light", "Dark"));
        mode.select(Theme.isDark() ? 1 : 0);
        mode.getAccessibleContext().setAccessibleName("Appearance");
        mode.onChange(i -> {
            Preferences.setDarkMode(i == 1);
            Theme.setDark(i == 1);
            app.rebuild();
        });
        JSlider glass = new JSlider(35, 100, Math.round(Theme.glassOpacity() * 100));
        glass.setOpaque(false);
        glass.getAccessibleContext().setAccessibleName("Glass intensity");
        glass.addChangeListener(e -> {
            Theme.setGlassOpacity(glass.getValue() / 100f);
            Preferences.setGlassOpacity(Theme.glassOpacity());
            javax.swing.SwingUtilities.getWindowAncestor(this).repaint();
        });
        JPanel ends = new JPanel(new BorderLayout());
        ends.setOpaque(false);
        ends.add(secondary("Clear"), BorderLayout.WEST);
        ends.add(secondary("Tinted"), BorderLayout.EAST);
        JPanel slider = new JPanel(new BorderLayout());
        slider.setOpaque(false);
        slider.add(glass, BorderLayout.CENTER);
        slider.add(ends, BorderLayout.SOUTH);
        return Fields.stack(Sheet.labelled("Mode", mode), Sheet.labelled("Liquid Glass", slider),
                secondary("<html>Like the iOS 27 slider: more tinted glass is easier to read.</html>"));
    }

    private JComponent security() {
        JComboBox<Integer> idle = Fields.combo(List.of(5, 10, 15, 30), (int) Preferences.idleTimeout().toMinutes());
        idle.setRenderer(Sheets.labelRenderer(m -> m + " minutes"));
        idle.addActionListener(e -> {
            Preferences.setIdleTimeout(Duration.ofMinutes((Integer) idle.getSelectedItem()));
            app.toast("Applies from the next sign-in.", Toast.Kind.INFO);
        });
        return Fields.stack(Sheet.labelled("Sign out after inactivity", idle),
                secondary("<html>Accounts lock for 60 seconds after 5 wrong passwords. Passwords are stored as salted PBKDF2 hashes.</html>"));
    }

    private JComponent data() {
        JLabel where = secondary("<html>Saved on this computer at<br>" + ClinicStore.defaultFile() + "</html>");
        return Fields.stack(where,
                secondaryButton("Reset demo data", TablerIcon.REFRESH, () -> Sheet.confirm(this, "Reset demo data?",
                        "All patients, visits, bills and messages are replaced with fresh demo data. This can't be undone.",
                        "Reset", true, () -> {
                            app.store().resetDemoData();
                            app.toast("Demo data restored.", Toast.Kind.SUCCESS);
                        })),
                secondary(Brand.NAME + " " + Brand.VERSION));
    }

    @Override
    protected void refresh() {
        if (app.canEdit(Screen.SETTINGS)) {
            audit.setRows(AuditLog.entries(), "No events yet.");
        }
    }

    private static JComponent card(String title, JComponent body) {
        Card c = new Card(new BorderLayout(0, 14));
        c.add(sectionTitle(title), BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent wrapTop(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }

}
