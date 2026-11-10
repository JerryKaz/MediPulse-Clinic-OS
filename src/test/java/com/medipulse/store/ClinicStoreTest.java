package com.medipulse.store;

import com.medipulse.domain.Consciousness;
import com.medipulse.domain.InvoiceStatus;
import com.medipulse.domain.Patient;
import com.medipulse.domain.Payer;
import com.medipulse.domain.Priority;
import com.medipulse.domain.QueueEntry;
import com.medipulse.domain.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClinicStoreTest {

    private ClinicStore store;

    @BeforeEach
    void setUp() {
        store = ClinicStore.inMemory(Clock.fixed(Instant.parse("2026-11-10T11:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void checkInIssuesTokenAndRejectsDuplicates() {
        QueueEntry q = store.checkIn("P-1004", Priority.ROUTINE, "Cough");
        assertEquals(Stage.WAITING, q.stage());
        assertThrows(ClinicStore.RuleException.class, () -> store.checkIn("P-1004", Priority.ROUTINE, "Again"));
    }

    @Test
    void queueIsOrderedByPriorityThenArrival() {
        var queue = store.queue();
        for (int i = 1; i < queue.size(); i++) {
            assertTrue(QueueEntry.TRIAGE_ORDER.compare(queue.get(i - 1), queue.get(i)) <= 0);
        }
    }

    @Test
    void highNews2EscalatesQueuePriority() {
        QueueEntry q = store.checkIn("P-1004", Priority.ROUTINE, "Breathless");
        var out = store.recordVitals("P-1004", 26, 89, true, 102, 64, 118, Consciousness.ALERT, 38.9);
        assertTrue(out.escalated());
        assertEquals(Priority.EMERGENCY, store.activeVisit("P-1004").orElseThrow().priority());
        assertEquals(q.id(), store.activeVisit("P-1004").orElseThrow().id());
    }

    @Test
    void implausibleVitalsAreRejected() {
        assertThrows(ClinicStore.RuleException.class,
                () -> store.recordVitals("P-1004", 16, 98, false, 80, 120, 72, Consciousness.ALERT, 36.8));
        assertThrows(ClinicStore.RuleException.class,
                () -> store.recordVitals("P-1004", 16, 140, false, 120, 80, 72, Consciousness.ALERT, 36.8));
    }

    @Test
    void allergyBlocksDispenseUnlessOverriddenAndOverrideIsRecorded() {
        // P-1002 (Ama Boateng) is allergic to penicillin; DR-101 is amoxicillin.
        assertThrows(ClinicStore.RuleException.class, () -> store.dispense("DR-101", "P-1002", 1, false));
        var d = store.dispense("DR-101", "P-1002", 1, true);
        assertTrue(d.allergyOverride());
    }

    @Test
    void expiredDrugCanNeverBeDispensed() {
        // DR-110 (co-trimoxazole) expired in the seed data.
        assertThrows(ClinicStore.RuleException.class, () -> store.dispense("DR-110", "P-1001", 1, true));
    }

    @Test
    void dispensingReducesStockAndBillsThePatient() {
        int before = store.drug("DR-103").orElseThrow().stock();
        int invoicesBefore = store.invoicesFor("P-1011").size();
        store.dispense("DR-103", "P-1011", 10, false);
        assertEquals(before - 10, store.drug("DR-103").orElseThrow().stock());
        assertEquals(invoicesBefore + 1, store.invoicesFor("P-1011").size());
    }

    @Test
    void nhisClaimNeedsActiveMembership() {
        // P-1004 (Efua Addo) has an expired NHIS card.
        store.dispense("DR-103", "P-1004", 2, false);
        var inv = store.invoicesFor("P-1004").get(0);
        assertThrows(ClinicStore.RuleException.class, () -> store.submitClaim(inv.id()));
        store.recordPayment(inv.id(), Payer.CASH, null);
        assertEquals(InvoiceStatus.PAID, store.invoicesFor("P-1004").get(0).status());
    }

    @Test
    void mobileMoneyNeedsATransactionId() {
        var inv = store.billConsultation("P-1011");
        assertThrows(ClinicStore.RuleException.class, () -> store.recordPayment(inv.id(), Payer.MOBILE_MONEY, ""));
        store.recordPayment(inv.id(), Payer.MOBILE_MONEY, "MP240012345");
        assertThrows(ClinicStore.RuleException.class, () -> store.recordPayment(inv.id(), Payer.CASH, null));
    }

    @Test
    void doubleBookingIsBlocked() {
        var start = store.now().plusHours(5);
        store.book("P-1004", "S-3", start, 30, "Review");
        assertThrows(ClinicStore.RuleException.class, () -> store.book("P-1006", "S-3", start.plusMinutes(10), 20, "Clash"));
    }

    @Test
    void ghanaCardFormatIsValidated() {
        Patient bad = new Patient("", "Test Person", null, null, null, "12345", null, null, null, null, null, null);
        assertThrows(ClinicStore.RuleException.class, () -> store.savePatient(bad));
        Patient ok = new Patient("", "Test Person", null, null, null, "GHA-123456789-0", null, null, null, null, null, null);
        assertTrue(store.savePatient(ok).id().startsWith("P-"));
    }

    @Test
    void subscribersHearOnlyTheirTopics() {
        AtomicInteger queueEvents = new AtomicInteger();
        store.subscribe(queueEvents::incrementAndGet, ClinicStore.Topic.QUEUE);
        store.post("Ward A", "Test", "hello");
        assertEquals(0, queueEvents.get());
        store.checkIn("P-1004", Priority.URGENT, "x");
        assertEquals(1, queueEvents.get());
        assertFalse(store.queue().isEmpty());
    }
}
