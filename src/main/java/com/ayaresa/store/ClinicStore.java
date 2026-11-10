package com.ayaresa.store;

import com.ayaresa.clinical.News2;
import com.ayaresa.clinical.SafetyCheck;
import com.ayaresa.domain.Appointment;
import com.ayaresa.domain.AppointmentStatus;
import com.ayaresa.domain.Bed;
import com.ayaresa.domain.Consciousness;
import com.ayaresa.domain.Dispense;
import com.ayaresa.domain.Drug;
import com.ayaresa.domain.Invoice;
import com.ayaresa.domain.InvoiceLine;
import com.ayaresa.domain.InvoiceStatus;
import com.ayaresa.domain.Message;
import com.ayaresa.domain.Patient;
import com.ayaresa.domain.Payer;
import com.ayaresa.domain.Priority;
import com.ayaresa.domain.QueueEntry;
import com.ayaresa.domain.Stage;
import com.ayaresa.domain.Staff;
import com.ayaresa.domain.VitalSigns;
import com.ayaresa.security.AuditLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * The single place clinic data lives. Screens read from it and change it only through its
 * methods, which validate input, write an audit entry, notify subscribers and save to disk.
 *
 * <p>Threading: call it from the Swing event thread. Saving happens on a background thread
 * from an immutable byte snapshot, so the UI never waits for the disk.
 *
 * <p>Persistence is Java serialization to one file. That is fine for a single-PC clinic
 * demo; a multi-user deployment should replace {@link #save()} and {@link #open} with a
 * database (PostgreSQL) behind the same methods.
 */
public final class ClinicStore {

    public enum Topic { PATIENTS, QUEUE, VITALS, APPOINTMENTS, BEDS, STAFF, PHARMACY, BILLING, MESSAGES }

    /** Thrown for invalid actions; the message is safe to show to the user. */
    public static final class RuleException extends RuntimeException {
        public RuleException(String message) {
            super(message);
        }
    }

    /** Result of recording vitals: the score, plus whether the queue priority was raised. */
    public record VitalsOutcome(VitalSigns vitals, News2.Result news2, boolean escalated) {
    }

    /** Everything that is saved. Bump {@code serialVersionUID} when the shape changes. */
    static final class Data implements Serializable {
        private static final long serialVersionUID = 4L;
        int seq = 1000;
        int nextToken = 1;
        LocalDate tokenDay;
        final List<Patient> patients = new ArrayList<>();
        final List<QueueEntry> queue = new ArrayList<>();
        final List<VitalSigns> vitals = new ArrayList<>();
        final List<Staff> staff = new ArrayList<>();
        final List<Appointment> appointments = new ArrayList<>();
        final List<Bed> beds = new ArrayList<>();
        final List<Drug> drugs = new ArrayList<>();
        final List<Dispense> dispenses = new ArrayList<>();
        final List<Invoice> invoices = new ArrayList<>();
        final List<Message> messages = new ArrayList<>();
        final List<AuditLog.Entry> audit = new ArrayList<>();
    }

    private static final System.Logger LOG = System.getLogger("com.ayaresa.store");
    private static final BigDecimal CONSULTATION_FEE = new BigDecimal("80.00");

    private Data d;
    private final Clock clock;
    private final Path file;
    private final Map<Topic, List<Runnable>> listeners = new EnumMap<>(Topic.class);
    private final ExecutorService saver = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ayaresa-save");
        t.setDaemon(true);
        return t;
    });
    private String actor = "system";

    private ClinicStore(Data data, Clock clock, Path file) {
        this.d = data;
        this.clock = clock;
        this.file = file;
        for (Topic t : Topic.values()) {
            listeners.put(t, new CopyOnWriteArrayList<>());
        }
        AuditLog.restore(d.audit);
        AuditLog.setSink(e -> {
            d.audit.add(e);
            if (d.audit.size() > 5_000) {
                d.audit.remove(0);
            }
            save();
        });
    }

    /** Opens the store saved at {@code file}, or seeds demo data if there is none (or it's unreadable). */
    public static ClinicStore open(Path file, Clock clock) {
        Data data = null;
        if (file != null && Files.exists(file)) {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(Files.readAllBytes(file)))) {
                data = (Data) in.readObject();
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not read " + file + "; starting with demo data", e);
                backupCorrupt(file);
            }
        }
        if (data == null) {
            data = SeedData.create(clock);
        }
        ClinicStore store = new ClinicStore(data, clock, file);
        store.save();
        return store;
    }

    /** In-memory store with demo data and no file; for tests. */
    public static ClinicStore inMemory(Clock clock) {
        return new ClinicStore(SeedData.create(clock), clock, null);
    }

    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".ayaresa", "clinic.dat");
    }

    /** Who is making changes; recorded in the audit log. */
    public void setActor(String username) {
        this.actor = username == null ? "system" : username;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    // ---- Subscriptions ---------------------------------------------------------------

    /** Runs {@code listener} after any change to the given topics. Close the result to stop. */
    public AutoCloseable subscribe(Runnable listener, Topic... topics) {
        for (Topic t : topics) {
            listeners.get(t).add(listener);
        }
        return () -> {
            for (Topic t : topics) {
                listeners.get(t).remove(listener);
            }
        };
    }

    private void changed(Topic... topics) {
        save();
        java.util.Set<Runnable> once = new java.util.LinkedHashSet<>();
        for (Topic t : topics) {
            once.addAll(listeners.get(t));
        }
        once.forEach(Runnable::run);
    }

    private String nextId(String prefix) {
        return prefix + "-" + (++d.seq);
    }

    private void audit(String detail) {
        AuditLog.record(AuditLog.Type.RECORD_CHANGE, actor, detail);
    }

    // ---- Patients --------------------------------------------------------------------

    public List<Patient> patients() {
        return d.patients.stream().sorted(Comparator.comparing(Patient::name)).toList();
    }

    public Optional<Patient> patient(String id) {
        return d.patients.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    public Patient requirePatient(String id) {
        return patient(id).orElseThrow(() -> new RuleException("Patient " + id + " not found."));
    }

    public List<Patient> searchPatients(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return d.patients.stream().filter(p -> p.matches(query)).limit(limit).toList();
    }

    /** Registers a new patient (id ignored) or saves edits to an existing one. */
    public Patient savePatient(Patient p) {
        if (p.name() == null || p.name().isBlank()) {
            throw new RuleException("Enter the patient's full name.");
        }
        if (p.dateOfBirth() != null && p.dateOfBirth().isAfter(today())) {
            throw new RuleException("Date of birth can't be in the future.");
        }
        if (p.ghanaCard() != null && !p.ghanaCard().isBlank() && !p.ghanaCard().matches("GHA-\\d{9}-\\d")) {
            throw new RuleException("Ghana Card numbers look like GHA-123456789-0.");
        }
        boolean isNew = p.id() == null || p.id().isBlank() || patient(p.id()).isEmpty();
        Patient saved = isNew
                ? new Patient(nextId("P"), p.name().trim(), p.sex(), p.dateOfBirth(), p.phone(), p.ghanaCard(),
                p.nhisNumber(), p.nhisExpiry(), p.bloodGroup(), p.allergies(), p.conditions(), now())
                : p;
        if (isNew) {
            d.patients.add(saved);
            audit("registered patient " + saved.id());
        } else {
            d.patients.replaceAll(x -> x.id().equals(saved.id()) ? saved : x);
            audit("updated patient " + saved.id());
        }
        changed(Topic.PATIENTS);
        return saved;
    }

    // ---- Queue -----------------------------------------------------------------------

    /** Today's visits, in triage order. */
    public List<QueueEntry> queue() {
        LocalDate today = today();
        return d.queue.stream()
                .filter(q -> q.arrivedAt().toLocalDate().equals(today))
                .sorted(QueueEntry.TRIAGE_ORDER)
                .toList();
    }

    public List<QueueEntry> queueAt(Stage stage) {
        return queue().stream().filter(q -> q.stage() == stage).toList();
    }

    /** Every visit by this patient, newest first. */
    public List<QueueEntry> visitsFor(String patientId) {
        return d.queue.stream().filter(q -> q.patientId().equals(patientId))
                .sorted(Comparator.comparing(QueueEntry::arrivedAt).reversed()).toList();
    }

    /** Completed visits per day for the last {@code days} days, oldest first. */
    public List<Integer> visitsPerDay(int days) {
        LocalDate today = today();
        List<Integer> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            out.add((int) d.queue.stream().filter(q -> q.arrivedAt().toLocalDate().equals(day)).count());
        }
        return out;
    }

    public Optional<QueueEntry> activeVisit(String patientId) {
        return queue().stream().filter(q -> q.patientId().equals(patientId) && q.stage() != Stage.DONE).findFirst();
    }

    public QueueEntry checkIn(String patientId, Priority priority, String complaint) {
        requirePatient(patientId);
        if (activeVisit(patientId).isPresent()) {
            throw new RuleException("This patient is already in today's queue.");
        }
        if (!today().equals(d.tokenDay)) {
            d.tokenDay = today();
            d.nextToken = 1;
        }
        QueueEntry q = new QueueEntry(nextId("V"), d.nextToken++, patientId, priority, Stage.WAITING,
                complaint == null || complaint.isBlank() ? "Not stated" : complaint.trim(), now(), now());
        d.queue.add(q);
        audit("checked in " + patientId + " as " + q.tokenLabel() + " (" + priority.label() + ")");
        changed(Topic.QUEUE);
        return q;
    }

    public QueueEntry advance(String entryId) {
        QueueEntry q = entry(entryId);
        QueueEntry next = q.withStage(q.stage().next(), now());
        replace(next);
        audit("moved " + q.tokenLabel() + " to " + next.stage().label());
        changed(Topic.QUEUE);
        return next;
    }

    public void setPriority(String entryId, Priority p) {
        QueueEntry q = entry(entryId);
        replace(q.withPriority(p));
        audit("set " + q.tokenLabel() + " priority to " + p.label());
        changed(Topic.QUEUE);
    }

    private QueueEntry entry(String id) {
        return d.queue.stream().filter(q -> q.id().equals(id)).findFirst()
                .orElseThrow(() -> new RuleException("Visit not found."));
    }

    private void replace(QueueEntry e) {
        d.queue.replaceAll(x -> x.id().equals(e.id()) ? e : x);
    }

    // ---- Vitals ----------------------------------------------------------------------

    public List<VitalSigns> vitals(String patientId) {
        return d.vitals.stream().filter(v -> v.patientId().equals(patientId))
                .sorted(Comparator.comparing(VitalSigns::takenAt).reversed()).toList();
    }

    public Optional<VitalSigns> latestVitals(String patientId) {
        return vitals(patientId).stream().findFirst();
    }

    public List<VitalSigns> recentVitals(int limit) {
        return d.vitals.stream().sorted(Comparator.comparing(VitalSigns::takenAt).reversed()).limit(limit).toList();
    }

    /**
     * Records observations and scores them. A medium or high NEWS2 automatically raises the
     * patient's queue priority so they are seen sooner.
     */
    public VitalsOutcome recordVitals(String patientId, int rr, int spo2, boolean oxygen, int sys, int dia,
                                      int pulse, Consciousness acvpu, double temp) {
        requirePatient(patientId);
        check(rr, 3, 70, "Respiration rate");
        check(spo2, 50, 100, "SpO₂");
        check(sys, 50, 280, "Systolic BP");
        check(dia, 20, 180, "Diastolic BP");
        check(pulse, 20, 250, "Pulse");
        if (temp < 30 || temp > 44) {
            throw new RuleException("Temperature must be between 30 and 44 °C.");
        }
        if (dia >= sys) {
            throw new RuleException("Diastolic must be lower than systolic.");
        }
        VitalSigns v = new VitalSigns(nextId("O"), patientId, now(), rr, spo2, oxygen, sys, dia, pulse, acvpu, temp, actor);
        d.vitals.add(v);
        News2.Result r = News2.score(v);
        audit("recorded vitals for " + patientId + " (NEWS2 " + r.total() + ")");

        boolean escalated = false;
        Optional<QueueEntry> visit = activeVisit(patientId);
        if (visit.isPresent()) {
            Priority wanted = r.risk() == News2.Risk.HIGH ? Priority.EMERGENCY
                    : r.risk() == News2.Risk.MEDIUM ? Priority.URGENT : null;
            if (wanted != null && wanted.compareTo(visit.get().priority()) < 0) {
                replace(visit.get().withPriority(wanted));
                audit("auto-escalated " + visit.get().tokenLabel() + " to " + wanted.label() + " (NEWS2 " + r.total() + ")");
                escalated = true;
            }
        }
        changed(Topic.VITALS, Topic.QUEUE, Topic.BEDS);
        return new VitalsOutcome(v, r, escalated);
    }

    private static void check(int value, int min, int max, String what) {
        if (value < min || value > max) {
            throw new RuleException(what + " must be between " + min + " and " + max + ".");
        }
    }

    // ---- Staff -----------------------------------------------------------------------

    public List<Staff> staff() {
        return List.copyOf(d.staff);
    }

    public Optional<Staff> staffMember(String id) {
        return d.staff.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public void setOnDuty(String staffId, boolean onDuty) {
        d.staff.replaceAll(s -> s.id().equals(staffId) ? s.withOnDuty(onDuty) : s);
        audit("set " + staffId + (onDuty ? " on duty" : " off duty"));
        changed(Topic.STAFF);
    }

    // ---- Appointments ----------------------------------------------------------------

    public List<Appointment> appointmentsOn(LocalDate day) {
        return d.appointments.stream().filter(a -> a.start().toLocalDate().equals(day))
                .sorted(Comparator.comparing(Appointment::start)).toList();
    }

    public List<Appointment> appointmentsFor(String patientId) {
        return d.appointments.stream().filter(a -> a.patientId().equals(patientId))
                .sorted(Comparator.comparing(Appointment::start).reversed()).toList();
    }

    public Appointment book(String patientId, String staffId, LocalDateTime start, int minutes, String reason) {
        requirePatient(patientId);
        if (staffMember(staffId).isEmpty()) {
            throw new RuleException("Choose a clinician.");
        }
        if (start.isBefore(now().minusMinutes(5))) {
            throw new RuleException("That time has already passed.");
        }
        Appointment a = new Appointment(nextId("A"), patientId, staffId, start, minutes,
                reason == null || reason.isBlank() ? "Consultation" : reason.trim(), AppointmentStatus.SCHEDULED);
        boolean clash = d.appointments.stream()
                .filter(x -> x.status() == AppointmentStatus.SCHEDULED || x.status() == AppointmentStatus.CHECKED_IN)
                .anyMatch(a::overlaps);
        if (clash) {
            throw new RuleException("The clinician already has an appointment at that time.");
        }
        d.appointments.add(a);
        audit("booked " + a.id() + " for " + patientId);
        changed(Topic.APPOINTMENTS);
        return a;
    }

    /** Checking in an appointment also puts the patient in today's queue. */
    public void setAppointmentStatus(String id, AppointmentStatus status) {
        Appointment a = d.appointments.stream().filter(x -> x.id().equals(id)).findFirst()
                .orElseThrow(() -> new RuleException("Appointment not found."));
        d.appointments.replaceAll(x -> x.id().equals(id) ? x.withStatus(status) : x);
        audit("set " + id + " to " + status.label());
        if (status == AppointmentStatus.CHECKED_IN && activeVisit(a.patientId()).isEmpty()) {
            checkIn(a.patientId(), Priority.ROUTINE, a.reason());
        }
        changed(Topic.APPOINTMENTS);
    }

    // ---- Beds ------------------------------------------------------------------------

    public List<Bed> beds() {
        return List.copyOf(d.beds);
    }

    public Optional<Bed> bedOf(String patientId) {
        return d.beds.stream().filter(b -> patientId.equals(b.patientId())).findFirst();
    }

    public void admit(String bedId, String patientId, String reason) {
        requirePatient(patientId);
        if (bedOf(patientId).isPresent()) {
            throw new RuleException("This patient already has a bed.");
        }
        Bed bed = d.beds.stream().filter(b -> b.id().equals(bedId)).findFirst()
                .orElseThrow(() -> new RuleException("Bed not found."));
        if (!bed.isFree()) {
            throw new RuleException("Bed " + bedId + " is occupied.");
        }
        d.beds.replaceAll(b -> b.id().equals(bedId) ? b.admit(patientId, now(), reason) : b);
        audit("admitted " + patientId + " to bed " + bedId);
        changed(Topic.BEDS);
    }

    public void discharge(String bedId) {
        d.beds.replaceAll(b -> b.id().equals(bedId) ? b.discharge() : b);
        audit("discharged bed " + bedId);
        changed(Topic.BEDS);
    }

    // ---- Pharmacy --------------------------------------------------------------------

    public List<Drug> drugs() {
        return d.drugs.stream().sorted(Comparator.comparing(Drug::name)).toList();
    }

    public Optional<Drug> drug(String id) {
        return d.drugs.stream().filter(x -> x.id().equals(id)).findFirst();
    }

    public List<Dispense> dispensesToday() {
        LocalDate today = today();
        return d.dispenses.stream().filter(x -> x.at().toLocalDate().equals(today))
                .sorted(Comparator.comparing(Dispense::at).reversed()).toList();
    }

    public List<Dispense> dispensesFor(String patientId) {
        return d.dispenses.stream().filter(x -> x.patientId().equals(patientId))
                .sorted(Comparator.comparing(Dispense::at).reversed()).toList();
    }

    /**
     * Dispenses after the safety checks. Blocking issues always stop it; warnings (allergy)
     * stop it unless {@code overrideWarnings} is set, which is recorded. The drug is added
     * to the patient's open invoice for today.
     */
    public Dispense dispense(String drugId, String patientId, int qty, boolean overrideWarnings) {
        Drug drug = drug(drugId).orElseThrow(() -> new RuleException("Drug not found."));
        Patient patient = requirePatient(patientId);
        List<SafetyCheck.Issue> issues = SafetyCheck.beforeDispense(patient, drug, qty, today());
        if (SafetyCheck.blocked(issues)) {
            throw new RuleException(issues.stream().filter(i -> i.level() == SafetyCheck.Level.BLOCK)
                    .findFirst().orElseThrow().message());
        }
        boolean warned = !issues.isEmpty();
        if (warned && !overrideWarnings) {
            throw new RuleException(issues.get(0).message());
        }
        d.drugs.replaceAll(x -> x.id().equals(drugId) ? x.withStock(x.stock() - qty) : x);
        Dispense disp = new Dispense(nextId("D"), drugId, patientId, qty, now(), actor, warned);
        d.dispenses.add(disp);
        audit("dispensed " + qty + " x " + drug.name() + " to " + patientId + (warned ? " (ALLERGY WARNING OVERRIDDEN)" : ""));
        addToOpenInvoice(patient, new InvoiceLine(drug.name(), qty, drug.unitPrice()), drug.nhisCovered());
        changed(Topic.PHARMACY, Topic.BILLING);
        return disp;
    }

    public void restock(String drugId, int qty) {
        if (qty <= 0) {
            throw new RuleException("Enter how many units arrived.");
        }
        d.drugs.replaceAll(x -> x.id().equals(drugId) ? x.withStock(x.stock() + qty) : x);
        audit("restocked " + drugId + " +" + qty);
        changed(Topic.PHARMACY);
    }

    // ---- Billing ---------------------------------------------------------------------

    public List<Invoice> invoices() {
        return d.invoices.stream().sorted(Comparator.comparing(Invoice::createdAt).reversed()).toList();
    }

    public List<Invoice> invoicesFor(String patientId) {
        return invoices().stream().filter(i -> i.patientId().equals(patientId)).toList();
    }

    public BigDecimal revenue(LocalDate from, LocalDate to) {
        return d.invoices.stream()
                .filter(i -> i.status() == InvoiceStatus.PAID || i.status() == InvoiceStatus.CLAIM_PAID)
                .filter(i -> !i.createdAt().toLocalDate().isBefore(from) && !i.createdAt().toLocalDate().isAfter(to))
                .map(Invoice::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal outstanding() {
        return d.invoices.stream().filter(i -> !i.status().settled() && i.status() != InvoiceStatus.CLAIM_REJECTED)
                .map(Invoice::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** A consultation invoice; NHIS is chosen automatically when membership is active. */
    public Invoice billConsultation(String patientId) {
        Patient p = requirePatient(patientId);
        Invoice inv = addToOpenInvoice(p, new InvoiceLine("Consultation", 1, CONSULTATION_FEE), true);
        audit("billed consultation to " + patientId);
        changed(Topic.BILLING);
        return inv;
    }

    private Invoice addToOpenInvoice(Patient p, InvoiceLine line, boolean nhisCovered) {
        LocalDate today = today();
        Optional<Invoice> open = d.invoices.stream()
                .filter(i -> i.patientId().equals(p.id()) && i.status() == InvoiceStatus.UNPAID
                        && i.createdAt().toLocalDate().equals(today))
                .findFirst();
        Invoice inv;
        if (open.isPresent()) {
            List<InvoiceLine> lines = new ArrayList<>(open.get().lines());
            lines.add(line);
            Invoice o = open.get();
            inv = new Invoice(o.id(), o.patientId(), lines, o.payer(), o.status(), o.createdAt(), o.reference());
            replaceInvoice(inv);
        } else {
            Payer payer = p.nhisActive(today) && nhisCovered ? Payer.NHIS : Payer.CASH;
            inv = new Invoice(nextId("INV"), p.id(), List.of(line), payer, InvoiceStatus.UNPAID, now(), null);
            d.invoices.add(inv);
        }
        return inv;
    }

    /** Settles an invoice by cash, Mobile Money (reference required), card or waiver. */
    public void recordPayment(String invoiceId, Payer payer, String reference) {
        Invoice inv = invoice(invoiceId);
        if (inv.status().settled()) {
            throw new RuleException("This invoice is already settled.");
        }
        if (payer == Payer.NHIS) {
            throw new RuleException("NHIS invoices are settled by submitting a claim.");
        }
        if (payer == Payer.MOBILE_MONEY && (reference == null || !reference.trim().matches("[A-Za-z0-9]{6,20}"))) {
            throw new RuleException("Enter the Mobile Money transaction ID (6–20 letters or digits).");
        }
        InvoiceStatus status = payer == Payer.WAIVER ? InvoiceStatus.WAIVED : InvoiceStatus.PAID;
        replaceInvoice(inv.with(payer, status, reference == null ? null : reference.trim()));
        audit((payer == Payer.WAIVER ? "waived " : "received payment for ") + invoiceId + " via " + payer.label());
        changed(Topic.BILLING);
    }

    /** NHIS claims need an active membership on the day of service. */
    public void submitClaim(String invoiceId) {
        Invoice inv = invoice(invoiceId);
        Patient p = requirePatient(inv.patientId());
        if (!p.hasNhis()) {
            throw new RuleException(p.name() + " has no NHIS number on file.");
        }
        if (!p.nhisActive(inv.createdAt().toLocalDate())) {
            throw new RuleException(p.name() + "'s NHIS membership had expired on the day of service.");
        }
        if (inv.status() != InvoiceStatus.UNPAID && inv.status() != InvoiceStatus.CLAIM_REJECTED) {
            throw new RuleException("This invoice can't be claimed in its current state.");
        }
        replaceInvoice(inv.with(Payer.NHIS, InvoiceStatus.CLAIM_SUBMITTED, p.nhisNumber()));
        audit("submitted NHIS claim for " + invoiceId);
        changed(Topic.BILLING);
    }

    public void recordClaimResult(String invoiceId, boolean paid) {
        Invoice inv = invoice(invoiceId);
        if (inv.status() != InvoiceStatus.CLAIM_SUBMITTED) {
            throw new RuleException("No claim is pending for this invoice.");
        }
        replaceInvoice(inv.with(Payer.NHIS, paid ? InvoiceStatus.CLAIM_PAID : InvoiceStatus.CLAIM_REJECTED, inv.reference()));
        audit("NHIS claim " + invoiceId + (paid ? " paid" : " rejected"));
        changed(Topic.BILLING);
    }

    private Invoice invoice(String id) {
        return d.invoices.stream().filter(i -> i.id().equals(id)).findFirst()
                .orElseThrow(() -> new RuleException("Invoice not found."));
    }

    private void replaceInvoice(Invoice inv) {
        d.invoices.replaceAll(x -> x.id().equals(inv.id()) ? inv : x);
    }

    // ---- Messages --------------------------------------------------------------------

    public List<String> threads() {
        return d.messages.stream().map(Message::thread).distinct().toList();
    }

    public List<Message> messages(String thread) {
        return d.messages.stream().filter(m -> m.thread().equals(thread))
                .sorted(Comparator.comparing(Message::at)).toList();
    }

    public Optional<Message> lastMessage(String thread) {
        List<Message> m = messages(thread);
        return m.isEmpty() ? Optional.empty() : Optional.of(m.get(m.size() - 1));
    }

    public void post(String thread, String from, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        d.messages.add(new Message(nextId("M"), thread, from, text.trim(), now()));
        changed(Topic.MESSAGES);
    }

    // ---- Maintenance -----------------------------------------------------------------

    /** Replaces all data with fresh demo data. */
    public void resetDemoData() {
        d = SeedData.create(clock);
        AuditLog.restore(d.audit);
        audit("reset demo data");
        changed(Topic.values());
    }

    public int count(Predicate<Patient> filter) {
        return (int) d.patients.stream().filter(filter).count();
    }

    /** Serializes on the caller's thread (a consistent snapshot), writes on a background thread. */
    private void save() {
        if (file == null) {
            return;
        }
        byte[] bytes;
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(buf)) {
            out.writeObject(d);
            out.flush();
            bytes = buf.toByteArray();
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "Could not serialize clinic data", e);
            return;
        }
        saver.execute(() -> {
            try {
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.write(tmp, bytes);
                // Atomic replace: a crash mid-write never leaves a half-written data file.
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                LOG.log(System.Logger.Level.ERROR, "Could not save clinic data to " + file, e);
            }
        });
    }

    private static void backupCorrupt(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".unreadable"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // nothing more we can do; we'll overwrite it with demo data
        }
    }
}
