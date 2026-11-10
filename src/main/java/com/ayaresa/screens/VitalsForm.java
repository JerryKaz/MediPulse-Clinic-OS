package com.ayaresa.screens;

import com.ayaresa.clinical.News2;
import com.ayaresa.domain.Consciousness;
import com.ayaresa.domain.VitalSigns;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Fields;
import com.ayaresa.ui.Sheet;
import com.ayaresa.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Map;

/**
 * Observation entry with a live NEWS2 preview: the score and each parameter's contribution
 * update as the nurse types, before anything is saved.
 */
final class VitalsForm extends JPanel {

    private final JTextField rr = Fields.text("", "Breaths per minute");
    private final JTextField spo2 = Fields.text("", "Oxygen saturation, %");
    private final JCheckBox oxygen = Fields.check("On supplemental oxygen", false);
    private final JTextField sys = Fields.text("", "Systolic, mmHg");
    private final JTextField dia = Fields.text("", "Diastolic, mmHg");
    private final JTextField pulse = Fields.text("", "Beats per minute");
    private final JComboBox<Consciousness> acvpu = Fields.combo(Arrays.asList(Consciousness.values()), Consciousness.ALERT);
    private final JTextField temp = Fields.text("", "Degrees Celsius");

    private final JLabel score = new JLabel("–");
    private final JLabel risk = new JLabel("Enter observations to see the NEWS2 score");
    private final JLabel response = new JLabel(" ");
    private final JPanel breakdown = new JPanel(new GridLayout(0, 4, 6, 4));

    VitalsForm() {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        JPanel fields = (JPanel) Fields.stack(
                Fields.row(Sheet.labelled("Respiration rate", rr), Sheet.labelled("SpO₂ %", spo2)),
                oxygen,
                Fields.row(Sheet.labelled("Systolic BP", sys), Sheet.labelled("Diastolic BP", dia)),
                Fields.row(Sheet.labelled("Pulse", pulse), Sheet.labelled("Temperature °C", temp)),
                Sheet.labelled("Consciousness (ACVPU)", acvpu));
        add(fields, BorderLayout.CENTER);

        JPanel preview = new JPanel(new BorderLayout(0, 6));
        preview.setOpaque(false);
        preview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)));
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        score.setFont(Theme.font(Font.BOLD, 34));
        score.setForeground(Theme.LABEL);
        risk.setFont(Theme.bodyBold());
        response.setFont(Theme.footnote());
        response.setForeground(Theme.SECONDARY_LABEL);
        JPanel words = new JPanel(new GridLayout(2, 1));
        words.setOpaque(false);
        words.add(risk);
        words.add(response);
        top.add(score, BorderLayout.WEST);
        top.add(words, BorderLayout.CENTER);
        preview.add(top, BorderLayout.NORTH);
        breakdown.setOpaque(false);
        preview.add(breakdown, BorderLayout.CENTER);
        add(preview, BorderLayout.SOUTH);

        DocumentListener update = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { preview(); }
            public void removeUpdate(DocumentEvent e) { preview(); }
            public void changedUpdate(DocumentEvent e) { preview(); }
        };
        for (JTextField f : new JTextField[]{rr, spo2, sys, dia, pulse, temp}) {
            f.getDocument().addDocumentListener(update);
        }
        oxygen.addActionListener(e -> preview());
        acvpu.addActionListener(e -> preview());
        preview();
    }

    /** Fills the form with example normal values (used for screenshots and quick entry). */
    void fill(int rrV, int spo2V, boolean o2, int sysV, int diaV, int pulseV, double tempV) {
        rr.setText(String.valueOf(rrV));
        spo2.setText(String.valueOf(spo2V));
        oxygen.setSelected(o2);
        sys.setText(String.valueOf(sysV));
        dia.setText(String.valueOf(diaV));
        pulse.setText(String.valueOf(pulseV));
        temp.setText(String.valueOf(tempV));
        preview();
    }

    void clear() {
        for (JTextField f : new JTextField[]{rr, spo2, sys, dia, pulse, temp}) {
            f.setText("");
        }
        oxygen.setSelected(false);
        acvpu.setSelectedItem(Consciousness.ALERT);
    }

    private void preview() {
        breakdown.removeAll();
        try {
            VitalSigns v = new VitalSigns("preview", "preview", null,
                    Integer.parseInt(rr.getText().trim()), Integer.parseInt(spo2.getText().trim()), oxygen.isSelected(),
                    Integer.parseInt(sys.getText().trim()), Integer.parseInt(dia.getText().trim()),
                    Integer.parseInt(pulse.getText().trim()), (Consciousness) acvpu.getSelectedItem(),
                    Double.parseDouble(temp.getText().trim().replace(',', '.')), null);
            News2.Result r = News2.score(v);
            score.setText(String.valueOf(r.total()));
            score.setForeground(Format.kind(r.risk()).foreground());
            risk.setText(r.risk().label() + " clinical risk");
            risk.setForeground(Format.kind(r.risk()).foreground());
            response.setText("<html>" + r.risk().response() + "</html>");
            for (Map.Entry<String, Integer> e : r.parts().entrySet()) {
                JLabel name = new JLabel(e.getKey());
                name.setFont(Theme.footnote());
                name.setForeground(Theme.SECONDARY_LABEL);
                JLabel val = new JLabel(e.getValue() == 0 ? "0" : "+" + e.getValue());
                val.setFont(Theme.font(Font.BOLD, 12));
                val.setForeground(e.getValue() >= 3 ? Theme.RED : e.getValue() > 0 ? Theme.ORANGE : Theme.TERTIARY_LABEL);
                breakdown.add(name);
                breakdown.add(val);
            }
        } catch (NumberFormatException incomplete) {
            score.setText("–");
            score.setForeground(Theme.TERTIARY_LABEL);
            risk.setText("Enter all observations to see the NEWS2 score");
            risk.setForeground(Theme.SECONDARY_LABEL);
            response.setText(" ");
        }
        breakdown.revalidate();
        breakdown.repaint();
    }

    /** Validates and saves. Throws with a user-facing message if a value is missing or out of range. */
    ClinicStore.VitalsOutcome save(ClinicStore store, String patientId) {
        if (patientId == null) {
            throw new IllegalArgumentException("Choose a patient.");
        }
        return store.recordVitals(patientId,
                Fields.intValue(rr, "respiration rate"), Fields.intValue(spo2, "SpO₂"), oxygen.isSelected(),
                Fields.intValue(sys, "systolic BP"), Fields.intValue(dia, "diastolic BP"),
                Fields.intValue(pulse, "pulse"), (Consciousness) acvpu.getSelectedItem(),
                Fields.decimalValue(temp, "temperature"));
    }
}
