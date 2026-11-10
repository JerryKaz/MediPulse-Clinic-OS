package com.ayaresa.clinical;

import com.ayaresa.domain.Consciousness;
import com.ayaresa.domain.VitalSigns;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * National Early Warning Score 2 (Royal College of Physicians, 2017), SpO2 scale 1.
 *
 * <p>Scale 2 (for patients with hypercapnic respiratory failure) must only be used under
 * clinician direction, so it is deliberately not offered here. NEWS2 is a screening aid,
 * not a diagnosis: a low score never overrides clinical judgement.
 */
public final class News2 {

    public enum Risk {
        LOW("Low", "Continue routine monitoring (at least every 12 hours; 4–6 hourly if score 1–4)."),
        LOW_MEDIUM("Low–medium", "A single parameter scored 3: inform the doctor, monitor at least hourly."),
        MEDIUM("Medium", "Urgent review by a clinician; monitor at least hourly."),
        HIGH("High", "Emergency response: immediate senior review and continuous monitoring.");

        private final String label;
        private final String response;

        Risk(String label, String response) {
            this.label = label;
            this.response = response;
        }

        public String label() {
            return label;
        }

        public String response() {
            return response;
        }
    }

    /** The total, each parameter's sub-score (in chart order) and the resulting risk band. */
    public record Result(int total, Map<String, Integer> parts, Risk risk) {
        public Result {
            parts = Collections.unmodifiableMap(new LinkedHashMap<>(parts));
        }

        public boolean hasRedScore() {
            return parts.values().stream().anyMatch(v -> v == 3);
        }
    }

    private News2() {
    }

    public static Result score(VitalSigns v) {
        Map<String, Integer> p = new LinkedHashMap<>();
        p.put("Respiration rate", respiration(v.respirationRate()));
        p.put("SpO₂", spo2Scale1(v.spo2()));
        p.put("Air or oxygen", v.onOxygen() ? 2 : 0);
        p.put("Systolic BP", systolic(v.systolic()));
        p.put("Pulse", pulse(v.pulse()));
        p.put("Consciousness", v.consciousness() == Consciousness.ALERT ? 0 : 3);
        p.put("Temperature", temperature(v.temperature()));

        int total = p.values().stream().mapToInt(Integer::intValue).sum();
        boolean red = p.values().stream().anyMatch(x -> x == 3);
        Risk risk = total >= 7 ? Risk.HIGH
                : total >= 5 ? Risk.MEDIUM
                : red ? Risk.LOW_MEDIUM
                : Risk.LOW;
        return new Result(total, p, risk);
    }

    static int respiration(int rr) {
        if (rr <= 8) return 3;
        if (rr <= 11) return 1;
        if (rr <= 20) return 0;
        if (rr <= 24) return 2;
        return 3;
    }

    static int spo2Scale1(int spo2) {
        if (spo2 <= 91) return 3;
        if (spo2 <= 93) return 2;
        if (spo2 <= 95) return 1;
        return 0;
    }

    static int systolic(int sbp) {
        if (sbp <= 90) return 3;
        if (sbp <= 100) return 2;
        if (sbp <= 110) return 1;
        if (sbp <= 219) return 0;
        return 3;
    }

    static int pulse(int hr) {
        if (hr <= 40) return 3;
        if (hr <= 50) return 1;
        if (hr <= 90) return 0;
        if (hr <= 110) return 1;
        if (hr <= 130) return 2;
        return 3;
    }

    /** Charted to one decimal place; round first so 38.04 isn't treated as above 38.0. */
    static int temperature(double celsius) {
        double t = Math.round(celsius * 10) / 10.0;
        if (t <= 35.0) return 3;
        if (t <= 36.0) return 1;
        if (t <= 38.0) return 0;
        if (t <= 39.0) return 1;
        return 2;
    }
}
