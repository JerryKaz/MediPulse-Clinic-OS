package com.medipulse;

import java.time.Duration;

/** Per-user settings that survive restarts (stored with java.util.prefs, never in the data file). */
public final class Preferences {

    private static final java.util.prefs.Preferences P =
            java.util.prefs.Preferences.userRoot().node("com/medipulse");

    private Preferences() {
    }

    public static boolean darkMode() {
        return P.getBoolean("darkMode", false);
    }

    public static void setDarkMode(boolean value) {
        P.putBoolean("darkMode", value);
    }

    public static float glassOpacity() {
        return P.getFloat("glassOpacity", 0.62f);
    }

    public static void setGlassOpacity(float value) {
        P.putFloat("glassOpacity", value);
    }

    /** Whether this user has seen (or skipped) the guided tour on this computer. */
    public static boolean tourSeen(String username) {
        return P.getBoolean("tourSeen." + username, false);
    }

    public static void setTourSeen(String username) {
        P.putBoolean("tourSeen." + username, true);
    }

    public static Duration idleTimeout() {
        return Duration.ofMinutes(P.getInt("idleMinutes", 15));
    }

    public static void setIdleTimeout(Duration value) {
        P.putInt("idleMinutes", (int) value.toMinutes());
    }
}
