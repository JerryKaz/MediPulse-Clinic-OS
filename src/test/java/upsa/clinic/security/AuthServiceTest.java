package upsa.clinic.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    /** A clock the test can move forward. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-10T12:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private TestClock clock;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        auth = new AuthService(clock);
        auth.addUser(new User("nurse1", "Ama Owusu", Role.NURSE), "2222".toCharArray());
    }

    @Test
    void correctPasswordSignsIn() {
        var r = auth.signIn("nurse1", "2222".toCharArray());
        User u = assertInstanceOf(AuthService.Success.class, r).user();
        assertEquals(Role.NURSE, u.role());
    }

    @Test
    void usernameIsCaseAndWhitespaceInsensitive() {
        assertInstanceOf(AuthService.Success.class, auth.signIn("  NURSE1 ", "2222".toCharArray()));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameKindOfFailure() {
        assertInstanceOf(AuthService.Failure.class, auth.signIn("nurse1", "nope".toCharArray()));
        assertInstanceOf(AuthService.Failure.class, auth.signIn("ghost", "2222".toCharArray()));
    }

    @Test
    void locksOutAfterRepeatedFailuresThenRecovers() {
        for (int i = 1; i < AuthService.MAX_FAILED_ATTEMPTS; i++) {
            assertInstanceOf(AuthService.Failure.class, auth.signIn("nurse1", "bad".toCharArray()));
        }
        assertInstanceOf(AuthService.LockedOut.class, auth.signIn("nurse1", "bad".toCharArray()));
        // Even the right password is refused while locked.
        assertInstanceOf(AuthService.LockedOut.class, auth.signIn("nurse1", "2222".toCharArray()));

        clock.now = clock.now.plus(AuthService.LOCKOUT).plus(Duration.ofSeconds(1));
        assertInstanceOf(AuthService.Success.class, auth.signIn("nurse1", "2222".toCharArray()));
    }

    @Test
    void passwordBufferIsWiped() {
        char[] pw = "2222".toCharArray();
        auth.signIn("nurse1", pw);
        assertArrayEquals(new char[4], pw);
    }

    @Test
    void hashesAreSaltedAndVerify() {
        String a = PasswordHasher.hash("secret".toCharArray());
        String b = PasswordHasher.hash("secret".toCharArray());
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.verify("secret".toCharArray(), a));
        assertFalse(PasswordHasher.verify("Secret".toCharArray(), a));
        assertFalse(PasswordHasher.verify("secret".toCharArray(), "garbage"));
    }
}
