package upsa.clinic.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records security-relevant events (sign-ins, failures, lockouts, denied access).
 * In-memory for the demo; swap {@link #record} for a database insert later.
 */
public final class AuditLog {

    public enum Type { SIGN_IN, SIGN_IN_FAILED, LOCKED_OUT, SIGN_OUT, AUTO_LOCK, ACCESS_DENIED }

    public record Entry(LocalDateTime at, Type type, String username, String detail) {
    }

    private static final int MAX_ENTRIES = 1_000;
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final System.Logger LOG = System.getLogger("upsa.clinic.audit");

    private AuditLog() {
    }

    public static synchronized void record(Type type, String username, String detail) {
        Entry e = new Entry(LocalDateTime.now(), type, username, detail);
        ENTRIES.add(e);
        if (ENTRIES.size() > MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
        LOG.log(System.Logger.Level.INFO, "{0} user={1} {2}", type, username, detail);
    }

    public static synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES));
    }
}
