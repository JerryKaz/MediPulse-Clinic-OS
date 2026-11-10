package com.ayaresa.screens;

import com.ayaresa.domain.AppointmentStatus;
import com.ayaresa.domain.Staff;
import com.ayaresa.security.Role;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Avatar;
import com.ayaresa.ui.Card;
import com.ayaresa.ui.Fields;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.StatusPill;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/** Staff directory with on-duty status. Only administrators can change who is on duty. */
public final class StaffScreen extends ScreenView {

    private final JPanel grid = grid(3, Theme.GAP);
    private final JPanel head = new JPanel(new BorderLayout());

    public StaffScreen(AppContext app) {
        super(app, ClinicStore.Topic.STAFF, ClinicStore.Topic.APPOINTMENTS);
        head.setOpaque(false);
        add(head, BorderLayout.NORTH);
        add(com.ayaresa.ui.VerticalScroll.of(grid), BorderLayout.CENTER);
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        List<Staff> staff = s.staff();
        long on = staff.stream().filter(Staff::onDuty).count();
        head.removeAll();
        head.add(header("Staff", on + " of " + staff.size() + " on duty"));
        head.revalidate();
        grid.removeAll();
        boolean canManage = app.user().role() == Role.ADMIN;
        for (Staff m : staff) {
            Card card = new Card(new BorderLayout(14, 0));
            card.add(new Avatar(Avatar.initialsOf(m.name().replace("Dr. ", "")), m.name(), 48), BorderLayout.WEST);
            JPanel text = vertical();
            JLabel n = sectionTitle(m.name());
            text.add(n);
            text.add(secondary(m.title() + " · " + m.specialty()));
            JLabel phone = new JLabel(m.phone(), Icons.of(TablerIcon.PHONE, 14, Theme.TERTIARY_LABEL), JLabel.LEFT);
            phone.setFont(Theme.footnote());
            phone.setForeground(Theme.SECONDARY_LABEL);
            phone.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            text.add(phone);
            long today = s.appointmentsOn(s.today()).stream()
                    .filter(a -> a.staffId().equals(m.id()) && a.status() != AppointmentStatus.CANCELLED).count();
            if (today > 0) {
                text.add(secondary(today + " appointments today"));
            }
            card.add(text, BorderLayout.CENTER);
            JPanel right = new JPanel(new GridLayout(2, 1, 0, 6));
            right.setOpaque(false);
            right.add(new StatusPill(m.onDuty() ? "On duty" : "Off duty", m.onDuty() ? StatusPill.Kind.OK : StatusPill.Kind.IDLE));
            if (canManage) {
                JCheckBox toggle = Fields.check("On duty", m.onDuty());
                toggle.setFont(Theme.footnote());
                toggle.addActionListener(e -> app.attempt(() -> s.setOnDuty(m.id(), toggle.isSelected())));
                right.add(toggle);
            }
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(right, BorderLayout.NORTH);
            card.add(wrap, BorderLayout.EAST);
            grid.add(card);
        }
        grid.revalidate();
        grid.repaint();
    }
}
