package com.ayaresa;

import java.time.Duration;

/** Per-user settings that survive restarts (stored with java.util.prefs, never in the data file). */
public final class Preferences {

    private static final java.util.prefs.Preferences P =
            java.util.prefs.Preferences.userRoot().node("com/ayaresa");

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

    public static Duration idleTimeout() {
        return Duration.ofMinutes(P.getInt("idleMinutes", 15));
    }

    public static void setIdleTimeout(Duration value) {
        P.putInt("idleMinutes", (int) value.toMinutes());
    }
}
