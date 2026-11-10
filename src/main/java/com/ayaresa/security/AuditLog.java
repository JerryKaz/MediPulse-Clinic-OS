package com.ayaresa.security;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Records security and clinical events: sign-ins, lockouts, denied access, and every change
 * to patient data. Entries are kept in memory and handed to a sink (the data store), which
 * persists them, so the log survives restarts.
 */
public final class AuditLog {

    public enum Type { SIGN_IN, SIGN_IN_FAILED, LOCKED_OUT, SIGN_OUT, AUTO_LOCK, ACCESS_DENIED, RECORD_CHANGE }

    public record Entry(LocalDateTime at, Type type, String username, String detail) implements Serializable {
    }

    private static final int MAX_ENTRIES = 5_000;
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final System.Logger LOG = System.getLogger("com.ayaresa.audit");
    private static Consumer<Entry> sink = e -> { };

    private AuditLog() {
    }

    public static synchronized void record(Type type, String username, String detail) {
        Entry e = new Entry(LocalDateTime.now().withNano(0), type, username, detail);
        ENTRIES.add(e);
        if (ENTRIES.size() > MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
        LOG.log(System.Logger.Level.DEBUG, "{0} user={1} {2}", type, username, detail);
        sink.accept(e);
    }

    /** Newest first. */
    public static synchronized List<Entry> entries() {
        List<Entry> copy = new ArrayList<>(ENTRIES);
        Collections.reverse(copy);
        return Collections.unmodifiableList(copy);
    }

    /** Loads previously saved entries (called once when the store opens). */
    public static synchronized void restore(List<Entry> saved) {
        ENTRIES.clear();
        ENTRIES.addAll(saved);
    }

    public static synchronized void setSink(Consumer<Entry> consumer) {
        sink = consumer == null ? e -> { } : consumer;
    }
}
