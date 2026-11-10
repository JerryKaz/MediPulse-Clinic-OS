package com.medipulse.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Signs staff in against an in-memory user store with hashed passwords, and locks an
 * account for a short time after repeated failures to slow down password guessing.
 *
 * <p>Error messages never say whether the username or the password was wrong.
 */
public final class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCKOUT = Duration.ofSeconds(60);

    public sealed interface Result permits Success, Failure, LockedOut {
    }

    public record Success(User user) implements Result {
    }

    public record Failure(int attemptsLeft) implements Result {
    }

    public record LockedOut(Duration remaining) implements Result {
    }

    private record Account(User user, String passwordHash) {
    }

    private static final class Attempts {
        int failures;
        Instant lockedUntil = Instant.EPOCH;
    }

    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Attempts> attempts = new HashMap<>();
    private final Clock clock;

    public AuthService(Clock clock) {
        this.clock = clock;
    }

    /** The demo accounts. Replace with a database-backed store in production. */
    public static AuthService withDemoUsers() {
        AuthService auth = new AuthService(Clock.systemUTC());
        auth.addUser(new User("admin", "Akosua Admin", Role.ADMIN), "1234".toCharArray());
        auth.addUser(new User("doctor1", "Kofi Mensah", Role.DOCTOR), "3333".toCharArray());
        auth.addUser(new User("nurse1", "Ama Owusu", Role.NURSE), "2222".toCharArray());
        auth.addUser(new User("pharm1", "Yaw Boateng", Role.PHARMACIST), "4444".toCharArray());
        auth.addUser(new User("front1", "Efia Darko", Role.RECEPTIONIST), "5555".toCharArray());
        return auth;
    }

    public synchronized void addUser(User user, char[] password) {
        try {
            accounts.put(key(user.username()), new Account(user, PasswordHasher.hash(password)));
        } finally {
            PasswordHasher.wipe(password);
        }
    }

    /** Checks the credentials. The password array is wiped before returning. */
    public synchronized Result signIn(String username, char[] password) {
        try {
            String key = key(username);
            Attempts a = attempts.computeIfAbsent(key, k -> new Attempts());
            Instant now = clock.instant();

            if (now.isBefore(a.lockedUntil)) {
                return new LockedOut(Duration.between(now, a.lockedUntil));
            }

            Account account = accounts.get(key);
            // Verify against a dummy hash when the user doesn't exist, so response time
            // doesn't reveal which usernames are valid.
            String hash = account != null ? account.passwordHash() : DUMMY_HASH;
            boolean ok = PasswordHasher.verify(password, hash) && account != null;

            if (ok) {
                attempts.remove(key);
                AuditLog.record(AuditLog.Type.SIGN_IN, account.user().username(), account.user().role().label());
                return new Success(account.user());
            }

            a.failures++;
            if (a.failures >= MAX_FAILED_ATTEMPTS) {
                a.failures = 0;
                a.lockedUntil = now.plus(LOCKOUT);
                AuditLog.record(AuditLog.Type.LOCKED_OUT, key, "after " + MAX_FAILED_ATTEMPTS + " failures");
                return new LockedOut(LOCKOUT);
            }
            AuditLog.record(AuditLog.Type.SIGN_IN_FAILED, key, "attempt " + a.failures);
            return new Failure(MAX_FAILED_ATTEMPTS - a.failures);
        } finally {
            PasswordHasher.wipe(password);
        }
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password".toCharArray());
}
