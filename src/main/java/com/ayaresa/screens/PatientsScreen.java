package com.ayaresa.screens;

import com.ayaresa.domain.Patient;
import com.ayaresa.domain.QueueEntry;
import com.ayaresa.security.Screen;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Chips;
import com.ayaresa.ui.DataTable;
import com.ayaresa.ui.Fields;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.StatusPill;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Toast;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

import static com.ayaresa.ui.DataTable.col;

/** Patient register: search by name, ID, phone, Ghana Card or NHIS number, filter, open the chart. */
public final class PatientsScreen extends ScreenView {

    private final com.ayaresa.ui.IconField searchBox = com.ayaresa.ui.IconField.search("Search name, ID, phone or NHIS");
    private final JTextField search = (JTextField) searchBox.input();
    private final Chips chips = new Chips(List.of("All", "NHIS active", "NHIS expired", "Allergies", "Admitted", "In clinic today"));
    private final DataTable<Patient> table;

    public PatientsScreen(AppContext app) {
        super(app, ClinicStore.Topic.PATIENTS, ClinicStore.Topic.QUEUE, ClinicStore.Topic.BEDS);
        LocalDate today = app.store().today();
        table = new DataTable<>(List.of(
                col("Patient", p -> new DataTable.Person(p.name(), p.id()), 240, DataTable.Kind.PERSON),
                col("Age / sex", p -> p.age(today) + " · " + (p.sex() == null ? "" : p.sex().name().substring(0, 1)), 90, DataTable.Kind.TEXT),
                col("Phone", p -> p.phone() == null ? "" : p.phone(), 130, DataTable.Kind.SECONDARY),
                col("NHIS", p -> p.nhisActive(today) ? new DataTable.Pill("Active", StatusPill.Kind.OK)
                        : p.hasNhis() ? new DataTable.Pill("Expired", StatusPill.Kind.WARNING)
                        : new DataTable.Pill("None", StatusPill.Kind.IDLE), 110, DataTable.Kind.PILL),
                col("Allergies", p -> p.allergies().isEmpty() ? new DataTable.Pill("None known", StatusPill.Kind.IDLE)
                        : new DataTable.Pill(String.join(", ", p.allergies()), StatusPill.Kind.CRITICAL), 150, DataTable.Kind.PILL),
                col("Conditions", p -> String.join(", ", p.conditions()), 200, DataTable.Kind.SECONDARY),
                col("Today", p -> app.store().activeVisit(p.id()).map(q -> new DataTable.Pill(q.tokenLabel() + " · " + q.stage().label(), StatusPill.Kind.INFO))
                        .orElse(null), 160, DataTable.Kind.PILL)));
        table.onOpen(p -> app.openPatient(p.id()));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        searchBox.setPreferredSize(new Dimension(340, 38));
        PillButton export = secondaryButton("Export CSV", TablerIcon.FILE_EXPORT, this::export);
        top.add(header("Patients", "Double-click a patient to open their chart", searchBox, export), BorderLayout.NORTH);
        top.add(chips, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        chips.onChange(i -> refresh());
    }

    @Override
    public PillButton primaryAction() {
        return app.canEdit(Screen.PATIENTS) ? primary("New patient", TablerIcon.USER_PLUS, () -> Sheets.newPatient(app)) : null;
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        LocalDate today = s.today();
        List<Predicate<Patient>> filters = List.of(
                p -> true,
                p -> p.nhisActive(today),
                p -> p.hasNhis() && !p.nhisActive(today),
                p -> !p.allergies().isEmpty(),
                p -> s.bedOf(p.id()).isPresent(),
                p -> s.visitsFor(p.id()).stream().map(QueueEntry::arrivedAt).anyMatch(t -> t.toLocalDate().equals(today)));
        for (int i = 1; i < filters.size(); i++) {
            chips.setCount(i, s.count(filters.get(i)));
        }
        String q = search.getText().trim();
        List<Patient> rows = s.patients().stream()
                .filter(filters.get(chips.selected()))
                .filter(p -> q.isEmpty() || p.matches(q))
                .toList();
        table.setRows(rows, q.isEmpty() ? "No patients match this filter." : "No patient matches \"" + q + "\".");
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("patients-" + app.store().today() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        StringBuilder csv = new StringBuilder("id,name,sex,date_of_birth,phone,ghana_card,nhis_number,nhis_expiry,blood_group,allergies,conditions\n");
        for (Patient p : table.rows()) {
            csv.append(String.join(",", cell(p.id()), cell(p.name()), cell(p.sex()), cell(p.dateOfBirth()), cell(p.phone()),
                    cell(p.ghanaCard()), cell(p.nhisNumber()), cell(p.nhisExpiry()), cell(p.bloodGroup()),
                    cell(String.join("; ", p.allergies())), cell(String.join("; ", p.conditions())))).append('\n');
        }
        try {
            Files.writeString(file, csv, StandardCharsets.UTF_8);
            app.toast("Exported " + table.rows().size() + " patients.", Toast.Kind.SUCCESS);
            com.ayaresa.security.AuditLog.record(com.ayaresa.security.AuditLog.Type.RECORD_CHANGE, app.user().username(),
                    "exported " + table.rows().size() + " patient records");
        } catch (IOException e) {
            app.toast("Couldn't save the file: " + e.getMessage(), Toast.Kind.ERROR);
        }
    }

    /** CSV-escapes a value, and neutralises leading =,+,-,@ so spreadsheets don't run it as a formula. */
    static String cell(Object v) {
        String s = v == null ? "" : v.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
