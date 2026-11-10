package com.ayaresa.screens;

import com.ayaresa.clinical.News2;
import com.ayaresa.domain.Patient;
import com.ayaresa.domain.Stage;
import com.ayaresa.domain.VitalSigns;
import com.ayaresa.security.Screen;
import com.ayaresa.shell.AppContext;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Card;
import com.ayaresa.ui.DataTable;
import com.ayaresa.ui.PillButton;
import com.ayaresa.ui.Sheet;
import com.ayaresa.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import static com.ayaresa.ui.DataTable.col;

/**
 * Observations with automatic NEWS2 scoring. The score previews live while typing; saving a
 * medium or high score raises the patient's queue priority automatically.
 */
public final class VitalsScreen extends ScreenView {

    private final VitalsForm form = new VitalsForm();
    private final JPanel pickerSlot = new JPanel(new BorderLayout());
    private JComboBox<Patient> patient;
    private final DataTable<VitalSigns> recent;

    public VitalsScreen(AppContext app) {
        super(app, ClinicStore.Topic.VITALS, ClinicStore.Topic.QUEUE, ClinicStore.Topic.PATIENTS);
        ClinicStore s = app.store();
        recent = new DataTable<>(List.of(
                col("Patient", v -> new DataTable.Person(s.patient(v.patientId()).map(p -> p.name()).orElse(v.patientId()),
                        Format.relativeDay(v.takenAt(), s.today())), 190, DataTable.Kind.PERSON),
                col("NEWS2", v -> Format.pill(News2.score(v)), 150, DataTable.Kind.PILL),
                col("BP", VitalSigns::bloodPressure, 90, DataTable.Kind.TEXT),
                col("Pulse", v -> v.pulse(), 60, DataTable.Kind.TEXT),
                col("SpO₂", v -> v.spo2() + "%" + (v.onOxygen() ? " O₂" : ""), 90, DataTable.Kind.TEXT),
                col("Temp", v -> v.temperature() + "°", 70, DataTable.Kind.TEXT),
                col("By", VitalSigns::takenBy, 80, DataTable.Kind.SECONDARY)));
        recent.onOpen(v -> app.openPatient(v.patientId()));

        add(header("Vitals", "NEWS2 (Royal College of Physicians), SpO₂ scale 1. A screening aid; it never replaces clinical judgement."),
                BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        if (app.canEdit(Screen.VITALS)) {
            Card entry = new Card(new BorderLayout(0, 14));
            entry.setPreferredSize(new Dimension(430, 0));
            entry.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
            JPanel top = new JPanel(new BorderLayout(0, 12));
            top.setOpaque(false);
            top.add(sectionTitle("Record observations"), BorderLayout.NORTH);
            pickerSlot.setOpaque(false);
            top.add(pickerSlot, BorderLayout.CENTER);
            JPanel stack = new JPanel(new BorderLayout(0, 14));
            stack.setOpaque(false);
            stack.add(top, BorderLayout.NORTH);
            stack.add(form, BorderLayout.CENTER);
            entry.add(com.ayaresa.ui.VerticalScroll.of(stack), BorderLayout.CENTER);
            PillButton save = primary("Save observations", com.ayaresa.ui.TablerIcon.CHECK, this::save);
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.add(save, BorderLayout.EAST);
            entry.add(foot, BorderLayout.SOUTH);
            body.add(entry, BorderLayout.WEST);
        }
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);
        right.add(sectionTitle("Recent observations"), BorderLayout.NORTH);
        right.add(recent, BorderLayout.CENTER);
        body.add(right, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    /** Pre-selects a patient and example values; used by the screenshot harness and demos. */
    public void demo(String patientId, int rr, int spo2, boolean o2, int sys, int dia, int pulse, double temp) {
        refresh();
        for (int i = 0; i < patient.getItemCount(); i++) {
            if (patient.getItemAt(i).id().equals(patientId)) {
                patient.setSelectedIndex(i);
            }
        }
        form.fill(rr, spo2, o2, sys, dia, pulse, temp);
    }

    private void save() {
        Patient p = patient == null ? null : (Patient) patient.getSelectedItem();
        app.attempt(() -> {
            Sheets.announce(app, form.save(app.store(), p == null ? null : p.id()));
            form.clear();
        });
    }

    @Override
    protected void refresh() {
        ClinicStore s = app.store();
        recent.setRows(s.recentVitals(40), "No observations recorded yet.");
        if (app.canEdit(Screen.VITALS)) {
            // Patients in triage first: that's who usually needs observations next.
            List<Patient> ordered = new ArrayList<>();
            s.queueAt(Stage.TRIAGE).forEach(q -> s.patient(q.patientId()).ifPresent(ordered::add));
            s.queue().stream().filter(q -> q.stage() != Stage.DONE)
                    .forEach(q -> s.patient(q.patientId()).filter(x -> !ordered.contains(x)).ifPresent(ordered::add));
            s.patients().stream().filter(x -> !ordered.contains(x)).forEach(ordered::add);
            Patient keep = patient == null ? null : (Patient) patient.getSelectedItem();
            patient = Sheets.patientCombo(ordered, keep != null ? keep.id() : ordered.isEmpty() ? null : ordered.get(0).id());
            pickerSlot.removeAll();
            pickerSlot.add(Sheet.labelled("Patient (triage first)", patient));
            pickerSlot.revalidate();
        }
    }
}
