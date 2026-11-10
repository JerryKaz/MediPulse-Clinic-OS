package com.medipulse.store;

import com.medipulse.domain.Appointment;
import com.medipulse.domain.AppointmentStatus;
import com.medipulse.domain.Bed;
import com.medipulse.domain.Consciousness;
import com.medipulse.domain.Drug;
import com.medipulse.domain.Invoice;
import com.medipulse.domain.InvoiceLine;
import com.medipulse.domain.InvoiceStatus;
import com.medipulse.domain.Message;
import com.medipulse.domain.Patient;
import com.medipulse.domain.Payer;
import com.medipulse.domain.Priority;
import com.medipulse.domain.QueueEntry;
import com.medipulse.domain.Sex;
import com.medipulse.domain.Stage;
import com.medipulse.domain.Staff;
import com.medipulse.domain.VitalSigns;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/** Realistic demo data for a small Ghanaian clinic, generated relative to "now". Deterministic. */
final class SeedData {

    private SeedData() {
    }

    static ClinicStore.Data create(Clock clock) {
        ClinicStore.Data d = new ClinicStore.Data();
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        Random rnd = new Random(42);

        // --- Staff (names match the demo sign-in accounts) ---
        d.staff.addAll(List.of(
                new Staff("S-1", "Dr. Kofi Mensah", "Medical officer", "General practice", "024 555 0101", true),
                new Staff("S-2", "Dr. Abena Owusu", "Paediatrician", "Paediatrics", "020 555 0102", true),
                new Staff("S-3", "Dr. Kwesi Appiah", "Physician", "Internal medicine", "027 555 0103", false),
                new Staff("S-4", "Ama Owusu", "Nursing officer", "Ward A", "024 555 0104", true),
                new Staff("S-5", "Esi Quaye", "Midwife", "Maternity", "055 555 0105", true),
                new Staff("S-6", "Yaw Boateng", "Pharmacist", "Dispensary", "026 555 0106", true),
                new Staff("S-7", "Efia Darko", "Front desk", "Reception", "050 555 0107", true)));

        // --- Patients ---
        Object[][] people = {
                {"Kwame Asante", Sex.MALE, 1988, "024 311 2201", true, true, List.of(), List.of("Hypertension")},
                {"Ama Boateng", Sex.FEMALE, 1996, "020 311 2202", true, true, List.of("Penicillin"), List.of()},
                {"Yaw Ofori", Sex.MALE, 1961, "027 311 2203", true, true, List.of(), List.of("Hypertension", "Type 2 diabetes")},
                {"Efua Addo", Sex.FEMALE, 2001, "055 311 2204", true, false, List.of("Sulfa"), List.of("Asthma")},
                {"Kojo Amponsah", Sex.MALE, 2015, "024 311 2205", true, true, List.of(), List.of()},
                {"Akua Serwaa", Sex.FEMALE, 1993, "026 311 2206", false, false, List.of(), List.of("Migraine")},
                {"Kofi Badu", Sex.MALE, 1979, "050 311 2207", true, true, List.of("NSAID"), List.of("Peptic ulcer")},
                {"Adwoa Pokuaa", Sex.FEMALE, 1999, "024 311 2208", true, true, List.of(), List.of("Pregnancy, 32 weeks")},
                {"Nana Yeboah", Sex.MALE, 1954, "020 311 2209", true, true, List.of("Penicillin"), List.of("COPD")},
                {"Abena Frimpong", Sex.FEMALE, 2019, "027 311 2210", true, true, List.of(), List.of("Sickle cell disease")},
                {"Kwabena Osei", Sex.MALE, 2003, "055 311 2211", false, false, List.of(), List.of()},
                {"Afia Nyarko", Sex.FEMALE, 1985, "024 311 2212", true, false, List.of(), List.of("Hypertension")},
                {"Selorm Agbeko", Sex.MALE, 1990, "026 311 2213", true, true, List.of(), List.of()},
                {"Fatima Issah", Sex.FEMALE, 1997, "050 311 2214", true, true, List.of("Sulfa"), List.of()},
                {"Elikem Dzansi", Sex.MALE, 2011, "024 311 2215", true, true, List.of(), List.of("Asthma")},
                {"Mansa Owusu-Ansah", Sex.FEMALE, 1968, "020 311 2216", true, true, List.of(), List.of("Type 2 diabetes")},
        };
        String[] bloods = {"O+", "A+", "B+", "O-", "AB+", "O+", "A-", "B+"};
        for (int i = 0; i < people.length; i++) {
            Object[] p = people[i];
            boolean hasNhis = (Boolean) p[4];
            boolean active = (Boolean) p[5];
            @SuppressWarnings("unchecked") List<String> allergies = (List<String>) p[6];
            @SuppressWarnings("unchecked") List<String> conditions = (List<String>) p[7];
            LocalDate dob = LocalDate.of((Integer) p[2], 1 + rnd.nextInt(12), 1 + rnd.nextInt(28));
            String card = String.format("GHA-%09d-%d", 700000000 + rnd.nextInt(99999999), rnd.nextInt(10));
            d.patients.add(new Patient("P-" + (1001 + i), (String) p[0], (Sex) p[1], dob, (String) p[3], card,
                    hasNhis ? String.format("%08d", 10000000 + rnd.nextInt(89999999)) : null,
                    hasNhis ? (active ? today.plusMonths(2 + rnd.nextInt(10)) : today.minusMonths(1 + rnd.nextInt(5))) : null,
                    bloods[i % bloods.length], allergies, conditions, now.minusDays(30 + rnd.nextInt(400))));
        }
        d.seq = 2000;

        // --- Today's queue ---
        Object[][] queue = {
                {"P-1003", Priority.URGENT, Stage.CONSULTATION, "Headache and dizziness", 95},
                {"P-1001", Priority.ROUTINE, Stage.DONE, "Blood pressure review", 180},
                {"P-1005", Priority.ROUTINE, Stage.TRIAGE, "Fever for 3 days", 40},
                {"P-1002", Priority.ROUTINE, Stage.PHARMACY, "Sore throat", 120},
                {"P-1009", Priority.EMERGENCY, Stage.TRIAGE, "Shortness of breath", 12},
                {"P-1008", Priority.ROUTINE, Stage.WAITING, "Antenatal check", 25},
                {"P-1013", Priority.ROUTINE, Stage.WAITING, "Malaria test", 18},
                {"P-1010", Priority.URGENT, Stage.WAITING, "Joint pain (sickle cell)", 8},
                {"P-1007", Priority.ROUTINE, Stage.BILLING, "Stomach pain", 150},
        };
        int token = 1;
        for (Object[] q : queue) {
            LocalDateTime arrived = clampToday(now.minusMinutes((Integer) q[4]), today);
            Stage stage = (Stage) q[2];
            LocalDateTime since = stage == Stage.WAITING ? arrived : clampToday(arrived.plusMinutes(5 + rnd.nextInt(15)), today);
            if (since.isAfter(now)) {
                since = now;
            }
            d.queue.add(new QueueEntry("V-" + (2000 + token), token++, (String) q[0], (Priority) q[1], stage, (String) q[3], arrived, since));
        }
        d.nextToken = token;
        d.tokenDay = today;

        // Past visits (for the 14-day trend)
        int vid = 3000;
        for (int day = 13; day >= 1; day--) {
            LocalDate date = today.minusDays(day);
            int count = 18 + rnd.nextInt(16) - (date.getDayOfWeek().getValue() >= 6 ? 10 : 0);
            for (int k = 0; k < count; k++) {
                String pid = "P-" + (1001 + rnd.nextInt(people.length));
                LocalDateTime t = date.atTime(8, 0).plusMinutes(rnd.nextInt(480));
                d.queue.add(new QueueEntry("V-" + (vid++), k + 1, pid, Priority.ROUTINE, Stage.DONE, "Consultation", t, t.plusHours(1)));
            }
        }

        // --- Vitals ---
        d.vitals.add(v("P-1003", now.minusMinutes(80), 22, 94, false, 186, 112, 104, Consciousness.ALERT, 37.2));
        d.vitals.add(v("P-1001", now.minusMinutes(170), 16, 98, false, 138, 88, 76, Consciousness.ALERT, 36.7));
        d.vitals.add(v("P-1002", now.minusMinutes(110), 18, 98, false, 118, 76, 88, Consciousness.ALERT, 38.4));
        d.vitals.add(v("P-1009", now.minusMinutes(8), 26, 89, true, 102, 64, 118, Consciousness.ALERT, 38.9));
        d.vitals.add(v("P-1016", now.minusHours(20), 18, 96, false, 132, 84, 82, Consciousness.ALERT, 36.9));
        d.vitals.add(v("P-1012", now.minusHours(26), 20, 95, false, 150, 96, 92, Consciousness.ALERT, 37.0));
        d.vitals.add(v("P-1008", now.minusDays(3), 16, 99, false, 112, 72, 84, Consciousness.ALERT, 36.8));

        // --- Beds ---
        for (int i = 1; i <= 8; i++) {
            d.beds.add(new Bed("A" + i, "Ward A · General", null, null, null));
        }
        for (int i = 1; i <= 6; i++) {
            d.beds.add(new Bed("B" + i, "Ward B · Maternity", null, null, null));
        }
        admit(d, "A1", "P-1009", now.minusHours(1), "Respiratory distress, on oxygen");
        admit(d, "A2", "P-1016", now.minusDays(1), "Diabetic review");
        admit(d, "A4", "P-1012", now.minusDays(2), "Hypertension, observation");
        admit(d, "A5", "P-1015", now.minusDays(1), "Asthma exacerbation");
        admit(d, "B2", "P-1008", now.minusDays(3), "Antenatal monitoring");

        // --- Drugs ---
        Object[][] drugs = {
                {"Amoxicillin 500 mg", "Capsule", "penicillin", 18, 50, "1.20", 200, true},
                {"Artemether-Lumefantrine 20/120", "Tablet", null, 9, 40, "2.50", 160, true},
                {"Paracetamol 500 mg", "Tablet", null, 640, 200, "0.20", 400, true},
                {"Ibuprofen 400 mg", "Tablet", "nsaid", 210, 100, "0.40", 60, true},
                {"ORS sachet", "Powder", null, 22, 60, "1.00", 300, true},
                {"Insulin (Actrapid) 100 IU/ml", "Vial", null, 3, 10, "65.00", 120, false},
                {"Metformin 500 mg", "Tablet", null, 420, 150, "0.30", 500, true},
                {"Amlodipine 5 mg", "Tablet", null, 380, 150, "0.35", 45, true},
                {"Ciprofloxacin 500 mg", "Tablet", null, 96, 60, "1.10", 220, true},
                {"Co-trimoxazole 960 mg", "Tablet", "sulfa", 150, 80, "0.50", -12, true},
                {"Salbutamol inhaler", "Inhaler", null, 14, 20, "28.00", 300, true},
                {"Folic acid 5 mg", "Tablet", null, 900, 200, "0.10", 700, true},
                {"Ferrous sulphate 200 mg", "Tablet", null, 520, 200, "0.15", 80, true},
                {"Omeprazole 20 mg", "Capsule", null, 140, 80, "0.60", 380, false},
        };
        for (int i = 0; i < drugs.length; i++) {
            Object[] x = drugs[i];
            d.drugs.add(new Drug("DR-" + (101 + i), (String) x[0], (String) x[1], (String) x[2], (Integer) x[3],
                    (Integer) x[4], new BigDecimal((String) x[5]), today.plusDays((Integer) x[6]), (Boolean) x[7]));
        }

        // --- Appointments today ---
        LocalDateTime base = now.truncatedTo(ChronoUnit.HOURS);
        Object[][] appts = {
                {"P-1001", "S-1", -150, "Blood pressure review", AppointmentStatus.COMPLETED},
                {"P-1003", "S-1", -90, "Hypertension follow-up", AppointmentStatus.CHECKED_IN},
                {"P-1006", "S-1", -60, "Migraine review", AppointmentStatus.NO_SHOW},
                {"P-1011", "S-1", 30, "General check-up", AppointmentStatus.SCHEDULED},
                {"P-1014", "S-1", 90, "Lab results", AppointmentStatus.SCHEDULED},
                {"P-1005", "S-2", -30, "Child fever", AppointmentStatus.CHECKED_IN},
                {"P-1010", "S-2", 60, "Sickle cell review", AppointmentStatus.SCHEDULED},
                {"P-1015", "S-2", 120, "Asthma review", AppointmentStatus.SCHEDULED},
                {"P-1016", "S-3", 150, "Diabetes review", AppointmentStatus.SCHEDULED},
        };
        for (int i = 0; i < appts.length; i++) {
            Object[] a = appts[i];
            LocalDateTime start = clampToday(base.plusMinutes((Integer) a[2]), today);
            d.appointments.add(new Appointment("A-" + (2100 + i), (String) a[0], (String) a[1], start, 20, (String) a[3], (AppointmentStatus) a[4]));
        }

        // --- Invoices: 14 days of history plus today's open bills ---
        Payer[] payers = {Payer.NHIS, Payer.CASH, Payer.MOBILE_MONEY, Payer.NHIS, Payer.MOBILE_MONEY, Payer.CARD};
        int inv = 5000;
        for (int day = 13; day >= 0; day--) {
            int n = day == 0 ? 3 : 6 + rnd.nextInt(6);
            for (int k = 0; k < n; k++) {
                Payer payer = payers[rnd.nextInt(payers.length)];
                InvoiceStatus status = payer == Payer.NHIS
                        ? (day > 7 ? InvoiceStatus.CLAIM_PAID : day > 2 ? InvoiceStatus.CLAIM_SUBMITTED : InvoiceStatus.UNPAID)
                        : InvoiceStatus.PAID;
                if (day == 0) {
                    status = InvoiceStatus.PAID;
                }
                String pid = "P-" + (1001 + rnd.nextInt(people.length));
                List<InvoiceLine> lines = List.of(
                        new InvoiceLine("Consultation", 1, new BigDecimal("80.00")),
                        new InvoiceLine("Laboratory test", 1, new BigDecimal(25 + rnd.nextInt(60) + ".00")),
                        new InvoiceLine("Medicines", 1, new BigDecimal(10 + rnd.nextInt(120) + ".50")));
                String ref = payer == Payer.MOBILE_MONEY ? "MP" + (240000000 + rnd.nextInt(9999999)) : null;
                d.invoices.add(new Invoice("INV-" + (inv++), pid, lines, payer, status,
                        today.minusDays(day).atTime(9 + rnd.nextInt(7), rnd.nextInt(60)), ref));
            }
        }
        d.invoices.add(new Invoice("INV-" + (inv++), "P-1007",
                List.of(new InvoiceLine("Consultation", 1, new BigDecimal("80.00")),
                        new InvoiceLine("Omeprazole 20 mg", 28, new BigDecimal("0.60"))),
                Payer.NHIS, InvoiceStatus.UNPAID, now.minusMinutes(20), null));
        d.invoices.add(new Invoice("INV-" + (inv++), "P-1006",
                List.of(new InvoiceLine("Consultation", 1, new BigDecimal("80.00"))),
                Payer.CASH, InvoiceStatus.UNPAID, now.minusMinutes(50), null));
        d.invoices.add(new Invoice("INV-" + inv, "P-1012",
                List.of(new InvoiceLine("Consultation", 1, new BigDecimal("80.00")),
                        new InvoiceLine("Amlodipine 5 mg", 30, new BigDecimal("0.35"))),
                Payer.NHIS, InvoiceStatus.CLAIM_REJECTED, now.minusDays(4), "NHIS expired"));

        // --- Messages ---
        d.messages.add(new Message("M-1", "Ward A", "Ama Owusu", "Bed A1 (Nana Yeboah) is on 2 L oxygen, SpO₂ 89%. Please review.", now.minusMinutes(9)));
        d.messages.add(new Message("M-2", "Ward A", "Dr. Kofi Mensah", "On my way after this consult.", now.minusMinutes(6)));
        d.messages.add(new Message("M-3", "Pharmacy", "Yaw Boateng", "Insulin is down to 3 vials. Order placed with the supplier.", now.minusHours(2)));
        d.messages.add(new Message("M-4", "Pharmacy", "Akosua Admin", "Thanks. Co-trimoxazole batch has expired, please quarantine it.", now.minusHours(1)));
        d.messages.add(new Message("M-5", "Front desk", "Efia Darko", "Walk-ins are building up. Can triage take two more?", now.minusMinutes(25)));
        return d;
    }

    private static VitalSigns v(String pid, LocalDateTime at, int rr, int spo2, boolean o2, int sys, int dia,
                                int pulse, Consciousness c, double temp) {
        return new VitalSigns("O-" + pid + "-" + at.toLocalTime().toSecondOfDay(), pid, at, rr, spo2, o2, sys, dia, pulse, c, temp, "nurse1");
    }

    private static void admit(ClinicStore.Data d, String bedId, String pid, LocalDateTime at, String note) {
        d.beds.replaceAll(b -> b.id().equals(bedId) ? b.admit(pid, at, note) : b);
    }

    /** Keeps generated times inside today, so demo data never spills into yesterday or tomorrow. */
    private static LocalDateTime clampToday(LocalDateTime t, LocalDate today) {
        if (t.toLocalDate().isBefore(today)) {
            return today.atTime(LocalTime.of(0, 5));
        }
        if (t.toLocalDate().isAfter(today)) {
            return today.atTime(LocalTime.of(23, 30));
        }
        return t;
    }
}
