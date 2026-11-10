package com.ayaresa.screens;

import com.ayaresa.clinical.News2;
import com.ayaresa.clinical.SafetyCheck;
import com.ayaresa.domain.Bed;
import com.ayaresa.domain.Drug;
import com.ayaresa.domain.Invoice;
import com.ayaresa.domain.Patient;
import com.ayaresa.domain.Payer;
import com.ayaresa.domain.Priority;
import com.ayaresa.domain.QueueEntry;
import com.ayaresa.domain.Sex;
import com.ayaresa.domain.Stage;
import com.ayaresa.domain.Staff;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Fields;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.Sheet;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;
import com.ayaresa.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** The app's forms, each shown as a sheet. Shared by screens, the patient chart and the command palette. */
public final class Sheets {

    private Sheets() {
    }

    // ---- Patients --------------------------------------------------------------------

    public static void newPatient(AppContext app) {
        patientForm(app, null);
    }

    public static void editPatient(AppContext app, Patient p) {
        patientForm(app, p);
    }

    private static void patientForm(AppContext app, Patient existing) {
        boolean editing = existing != null;
        JTextField name = Fields.text(editing ? existing.name() : "", "First and last name");
        JComboBox<Sex> sex = Fields.combo(Arrays.asList(Sex.values()), editing ? existing.sex() : Sex.FEMALE);
        sex.setRenderer(labelRenderer(x -> x == Sex.FEMALE ? "Female" : "Male"));
        JTextField dob = Fields.text(editing && existing.dateOfBirth() != null ? existing.dateOfBirth().toString() : "", "YYYY-MM-DD");
        JTextField phone = Fields.text(editing ? existing.phone() : "", "024 000 0000");
        JTextField card = Fields.text(editing ? existing.ghanaCard() : "", "GHA-123456789-0");
        JTextField nhis = Fields.text(editing ? existing.nhisNumber() : "", "8-digit membership number");
        JTextField nhisExp = Fields.text(editing && existing.nhisExpiry() != null ? existing.nhisExpiry().toString() : "", "YYYY-MM-DD");
        JComboBox<String> blood = Fields.combo(List.of("Unknown", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"),
                editing && existing.bloodGroup() != null ? existing.bloodGroup() : "Unknown");
        JTextField allergies = Fields.text(editing ? String.join(", ", existing.allergies()) : "", "Comma separated, e.g. Penicillin, Sulfa");
        JTextField conditions = Fields.text(editing ? String.join(", ", existing.conditions()) : "", "Comma separated");

        JComponent body = Fields.stack(
                Sheet.labelled("Full name", name),
                Fields.row(Sheet.labelled("Sex", sex), Sheet.labelled("Date of birth", dob), Sheet.labelled("Blood group", blood)),
                Fields.row(Sheet.labelled("Phone", phone), Sheet.labelled("Ghana Card", card)),
                Fields.row(Sheet.labelled("NHIS number", nhis), Sheet.labelled("NHIS valid until", nhisExp)),
                Sheet.labelled("Allergies", allergies),
                Sheet.labelled("Long-term conditions", conditions));

        Sheet.open(app.window(), editing ? "Edit " + existing.name() : "Register patient",
                        editing ? existing.id() : "Ghana Card and NHIS details let you check membership and claim on the day.", body, 620)
                .cancel()
                .primary(editing ? "Save changes" : "Register", () -> {
                    Patient draft = new Patient(editing ? existing.id() : "", name.getText(), (Sex) sex.getSelectedItem(),
                            date(dob, "date of birth"), blank(phone), blank(card), blank(nhis), date(nhisExp, "NHIS expiry"),
                            "Unknown".equals(blood.getSelectedItem()) ? null : (String) blood.getSelectedItem(),
                            list(allergies), list(conditions), editing ? existing.registeredAt() : null);
                    if (draft.nhisNumber() != null && draft.nhisExpiry() == null) {
                        throw new IllegalArgumentException("Enter the NHIS expiry date as well.");
                    }
                    Patient saved = app.store().savePatient(draft);
                    app.toast(editing ? "Changes saved." : saved.name() + " registered as " + saved.id() + ".", Toast.Kind.SUCCESS);
                    if (!editing) {
                        app.openPatient(saved.id());
                    }
                    return true;
                });
    }

    // ---- Queue -----------------------------------------------------------------------

    public static void checkIn(AppContext app, String patientId) {
        List<Patient> candidates = app.store().patients().stream()
                .filter(p -> app.store().activeVisit(p.id()).isEmpty()).toList();
        JComboBox<Patient> patient = patientCombo(candidates, patientId);
        JComboBox<Priority> priority = Fields.combo(Arrays.asList(Priority.values()), Priority.ROUTINE);
        priority.setRenderer(labelRenderer(p -> ((Priority) p).label()));
        JTextField complaint = Fields.text("", "Main complaint, in the patient's words");
        Sheet.open(app.window(), "Check in", "Gives the patient a token and adds them to today's queue. "
                        + "Emergency and urgent patients are seen first.",
                Fields.stack(Sheet.labelled("Patient", patient), Sheet.labelled("Triage priority", priority),
                        Sheet.labelled("Reason for visit", complaint)))
                .cancel()
                .primary("Check in", () -> {
                    Patient p = (Patient) Objects.requireNonNull(patient.getSelectedItem(), "Choose a patient.");
                    QueueEntry q = app.store().checkIn(p.id(), (Priority) priority.getSelectedItem(), complaint.getText());
                    app.toast(p.name() + " checked in. Token " + q.tokenLabel() + ".", Toast.Kind.SUCCESS);
                    return true;
                });
    }

    // ---- Vitals ----------------------------------------------------------------------

    public static void vitals(AppContext app, String patientId) {
        Patient p = app.store().requirePatient(patientId);
        VitalsForm form = new VitalsForm();
        Sheet.open(app.window(), "Record vitals", p.name() + " · " + p.id(), form, 560)
                .cancel()
                .primary("Save", () -> {
                    announce(app, form.save(app.store(), patientId));
                    return true;
                });
    }

    static void announce(AppContext app, ClinicStore.VitalsOutcome out) {
        News2.Result r = out.news2();
        String msg = "Saved. NEWS2 " + r.total() + ", " + r.risk().label().toLowerCase() + " risk"
                + (out.escalated() ? ". Queue priority raised." : ".");
        app.toast(msg, r.risk() == News2.Risk.HIGH ? Toast.Kind.ERROR
                : r.risk() == News2.Risk.LOW ? Toast.Kind.SUCCESS : Toast.Kind.WARNING);
    }

    // ---- Appointments ----------------------------------------------------------------

    public static void book(AppContext app, String patientId) {
        JComboBox<Patient> patient = patientCombo(app.store().patients(), patientId);
        List<Staff> clinicians = app.store().staff().stream().filter(s -> s.name().startsWith("Dr.")).toList();
        JComboBox<Staff> who = Fields.combo(clinicians, clinicians.isEmpty() ? null : clinicians.get(0));
        who.setRenderer(labelRenderer(s -> ((Staff) s).name() + " · " + ((Staff) s).specialty()));
        LocalDateTime next = app.store().now().plusMinutes(60 - app.store().now().getMinute());
        JTextField date = Fields.text(next.toLocalDate().toString(), "YYYY-MM-DD");
        JTextField time = Fields.text(next.toLocalTime().format(Format.TIME), "HH:MM");
        JComboBox<Integer> length = Fields.combo(List.of(15, 20, 30, 45, 60), 20);
        length.setRenderer(labelRenderer(m -> m + " minutes"));
        JTextField reason = Fields.text("", "e.g. Follow-up");
        Sheet.open(app.window(), "Book appointment", "Double bookings for the same clinician are blocked.",
                Fields.stack(Sheet.labelled("Patient", patient), Sheet.labelled("Clinician", who),
                        Fields.row(Sheet.labelled("Date", date), Sheet.labelled("Time", time), Sheet.labelled("Length", length)),
                        Sheet.labelled("Reason", reason)), 560)
                .cancel()
                .primary("Book", () -> {
                    Patient p = (Patient) Objects.requireNonNull(patient.getSelectedItem(), "Choose a patient.");
                    Staff s = (Staff) Objects.requireNonNull(who.getSelectedItem(), "Choose a clinician.");
                    LocalDate d = Objects.requireNonNull(date(date, "date"), "Enter a date.");
                    LocalTime t;
                    try {
                        t = LocalTime.parse(time.getText().trim());
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException("Enter the time as HH:MM, e.g. 14:30.");
                    }
                    app.store().book(p.id(), s.id(), d.atTime(t), (Integer) length.getSelectedItem(), reason.getText());
                    app.toast("Booked " + p.name() + " with " + s.name() + " at " + t.format(Format.TIME) + ".", Toast.Kind.SUCCESS);
                    return true;
                });
    }

    // ---- Pharmacy --------------------------------------------------------------------

    public static void dispense(AppContext app, String drugId, String patientId) {
        ClinicStore store = app.store();
        // Patients waiting at the pharmacy come first in the list.
        List<Patient> ordered = new ArrayList<>();
        store.queueAt(Stage.PHARMACY).forEach(q -> store.patient(q.patientId()).ifPresent(ordered::add));
        store.patients().stream().filter(p -> !ordered.contains(p)).forEach(ordered::add);
        JComboBox<Patient> patient = patientCombo(ordered, patientId != null ? patientId
                : ordered.isEmpty() ? null : ordered.get(0).id());
        List<Drug> drugs = store.drugs().stream().filter(d -> d.stock() > 0).toList();
        JComboBox<Drug> drug = Fields.combo(drugs, drugs.stream().filter(d -> d.id().equals(drugId)).findFirst().orElse(null));
        drug.setRenderer(labelRenderer(d -> ((Drug) d).name() + "  ·  " + ((Drug) d).stock() + " in stock"));
        JTextField qty = Fields.text("1", "Units");
        JCheckBox override = Fields.check("I've checked with the prescriber. Dispense anyway.", false);
        JLabel issues = new JLabel(" ");
        issues.setFont(Theme.body());
        JPanel issueBox = new JPanel(new BorderLayout(0, 8));
        issueBox.setOpaque(false);
        issueBox.add(issues, BorderLayout.CENTER);

        Runnable check = () -> {
            Patient p = (Patient) patient.getSelectedItem();
            Drug d = (Drug) drug.getSelectedItem();
            issueBox.remove(override);
            if (p == null || d == null) {
                issues.setText(" ");
                return;
            }
            int q;
            try {
                q = Integer.parseInt(qty.getText().trim());
            } catch (NumberFormatException e) {
                q = 0;
            }
            List<SafetyCheck.Issue> found = SafetyCheck.beforeDispense(p, d, q, store.today());
            if (found.isEmpty()) {
                issues.setIcon(Icons.of(TablerIcon.SHIELD_CHECK, 18, Theme.GREEN));
                issues.setText("No allergy, stock or expiry problems found.");
                issues.setForeground(Theme.OK_FG);
            } else {
                SafetyCheck.Issue first = found.get(0);
                boolean block = first.level() == SafetyCheck.Level.BLOCK;
                issues.setIcon(Icons.of(block ? TablerIcon.CIRCLE_X : TablerIcon.ALERT_TRIANGLE, 18, block ? Theme.RED : Theme.ORANGE));
                issues.setText("<html><div style='width:360px'>" + first.message() + "</div></html>");
                issues.setForeground(block ? Theme.CRIT_FG : Theme.WARN_FG);
                if (!SafetyCheck.blocked(found)) {
                    issueBox.add(override, BorderLayout.SOUTH);
                }
            }
            issueBox.revalidate();
            issueBox.repaint();
        };
        patient.addActionListener(e -> check.run());
        drug.addActionListener(e -> check.run());
        qty.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { check.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { check.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });
        check.run();

        Sheet.open(app.window(), "Dispense", "Checked against the patient's allergies, stock and expiry before anything is given.",
                Fields.stack(Sheet.labelled("Patient", patient),
                        Fields.row(Sheet.labelled("Medicine", drug)), Sheet.labelled("Quantity", qty), issueBox), 560)
                .cancel()
                .primary("Dispense", () -> {
                    Patient p = (Patient) Objects.requireNonNull(patient.getSelectedItem(), "Choose a patient.");
                    Drug d = (Drug) Objects.requireNonNull(drug.getSelectedItem(), "Choose a medicine.");
                    int q = Fields.intValue(qty, "quantity");
                    store.dispense(d.id(), p.id(), q, override.isSelected());
                    app.toast("Dispensed " + q + " × " + d.name() + ". Added to today's bill.", Toast.Kind.SUCCESS);
                    return true;
                });
    }

    public static void restock(AppContext app, Drug d) {
        JTextField qty = Fields.text("", "Units received");
        Sheet.open(app.window(), "Receive stock", d.name() + " · " + d.stock() + " in stock now",
                        Fields.stack(Sheet.labelled("Units received", qty)), 420)
                .cancel()
                .primary("Add to stock", () -> {
                    int q = Fields.intValue(qty, "units received");
                    app.store().restock(d.id(), q);
                    app.toast(d.name() + ": " + (d.stock() + q) + " in stock.", Toast.Kind.SUCCESS);
                    return true;
                });
    }

    // ---- Billing ---------------------------------------------------------------------

    public static void recordPayment(AppContext app, Invoice inv) {
        Patient p = app.store().requirePatient(inv.patientId());
        JComboBox<Payer> payer = Fields.combo(List.of(Payer.CASH, Payer.MOBILE_MONEY, Payer.CARD, Payer.WAIVER),
                inv.payer() == Payer.NHIS ? Payer.CASH : inv.payer());
        JTextField ref = Fields.text("", "Mobile Money transaction ID (MTN, Telecel, AirtelTigo)");
        JComponent refRow = Sheet.labelled("Transaction ID", ref);
        refRow.setVisible(payer.getSelectedItem() == Payer.MOBILE_MONEY);
        payer.addActionListener(e -> {
            refRow.setVisible(payer.getSelectedItem() == Payer.MOBILE_MONEY);
            refRow.getParent().revalidate();
        });
        Sheet.open(app.window(), "Take payment", inv.id() + " · " + p.name() + " · " + Format.cedis(inv.total()),
                        Fields.stack(Sheet.labelled("Paid by", payer), refRow), 460)
                .cancel()
                .primary("Mark as paid", () -> {
                    Payer by = (Payer) payer.getSelectedItem();
                    app.store().recordPayment(inv.id(), by, ref.getText());
                    app.toast(inv.id() + (by == Payer.WAIVER ? " waived." : " paid by " + by.label() + "."), Toast.Kind.SUCCESS);
                    return true;
                });
    }

    // ---- Beds ------------------------------------------------------------------------

    public static void admit(AppContext app, Bed bed) {
        List<Patient> candidates = app.store().patients().stream()
                .filter(p -> app.store().bedOf(p.id()).isEmpty()).toList();
        JComboBox<Patient> patient = patientCombo(candidates, null);
        JTextField reason = Fields.text("", "Reason for admission");
        Sheet.open(app.window(), "Admit to " + bed.id(), bed.ward(),
                        Fields.stack(Sheet.labelled("Patient", patient), Sheet.labelled("Reason", reason)), 460)
                .cancel()
                .primary("Admit", () -> {
                    Patient p = (Patient) Objects.requireNonNull(patient.getSelectedItem(), "Choose a patient.");
                    app.store().admit(bed.id(), p.id(), reason.getText().isBlank() ? "Admitted" : reason.getText().trim());
                    app.toast(p.name() + " admitted to bed " + bed.id() + ".", Toast.Kind.SUCCESS);
                    return true;
                });
    }

    // ---- Helpers ---------------------------------------------------------------------

    static JComboBox<Patient> patientCombo(List<Patient> patients, String selectedId) {
        JComboBox<Patient> c = Fields.combo(patients, patients.stream()
                .filter(p -> p.id().equals(selectedId)).findFirst().orElse(null));
        c.setRenderer(labelRenderer(p -> ((Patient) p).name() + "  ·  " + ((Patient) p).id()));
        c.setMaximumRowCount(12);
        // Type a letter to jump to the first matching name.
        c.setKeySelectionManager((key, model) -> {
            for (int i = 0; i < model.getSize(); i++) {
                Patient p = (Patient) model.getElementAt(i);
                if (Character.toLowerCase(p.name().charAt(0)) == Character.toLowerCase(key)) {
                    return i;
                }
            }
            return -1;
        });
        return c;
    }

    static DefaultListCellRenderer labelRenderer(java.util.function.Function<Object, String> text) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value == null ? "" : text.apply(value), index, sel, focus);
                l.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return l;
            }
        };
    }

    private static LocalDate date(JTextField f, String what) {
        String s = f.getText().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Enter the " + what + " as YYYY-MM-DD.");
        }
    }

    private static String blank(JTextField f) {
        String s = f.getText().trim();
        return s.isEmpty() ? null : s;
    }

    private static List<String> list(JTextField f) {
        return Arrays.stream(f.getText().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
