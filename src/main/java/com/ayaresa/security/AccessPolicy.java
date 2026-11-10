package com.ayaresa.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.ayaresa.security.AccessLevel.FULL;
import static com.ayaresa.security.AccessLevel.NONE;
import static com.ayaresa.security.AccessLevel.VIEW;

/**
 * The single source of truth for "who can do what".
 *
 * <p>Deny by default: any screen a role is not explicitly granted is {@link AccessLevel#NONE}.
 * The sidebar, navigation guard, command palette and every action button ask this class.
 * Settings is VIEW for everyone (personal appearance options); FULL adds security and data tools.
 */
public final class AccessPolicy {

    private static final Map<Role, Map<Screen, AccessLevel>> RULES = new EnumMap<>(Role.class);

    static {
        grant(Role.ADMIN, FULL, Screen.values());

        grant(Role.DOCTOR, FULL, Screen.DASHBOARD, Screen.QUEUE, Screen.PATIENTS, Screen.APPOINTMENTS,
                Screen.VITALS, Screen.MESSAGES);
        grant(Role.DOCTOR, VIEW, Screen.BEDS, Screen.STAFF, Screen.PHARMACY, Screen.SETTINGS);

        grant(Role.NURSE, FULL, Screen.DASHBOARD, Screen.QUEUE, Screen.PATIENTS, Screen.APPOINTMENTS,
                Screen.VITALS, Screen.BEDS, Screen.MESSAGES);
        grant(Role.NURSE, VIEW, Screen.STAFF, Screen.PHARMACY, Screen.SETTINGS);

        grant(Role.PHARMACIST, FULL, Screen.DASHBOARD, Screen.PHARMACY, Screen.MESSAGES);
        grant(Role.PHARMACIST, VIEW, Screen.QUEUE, Screen.PATIENTS, Screen.SETTINGS);

        grant(Role.RECEPTIONIST, FULL, Screen.DASHBOARD, Screen.QUEUE, Screen.PATIENTS, Screen.APPOINTMENTS,
                Screen.BILLING, Screen.MESSAGES);
        grant(Role.RECEPTIONIST, VIEW, Screen.STAFF, Screen.SETTINGS);
    }

    private AccessPolicy() {
    }

    private static void grant(Role role, AccessLevel level, Screen... screens) {
        Map<Screen, AccessLevel> map = RULES.computeIfAbsent(role, r -> new EnumMap<>(Screen.class));
        for (Screen m : screens) {
            map.put(m, level);
        }
    }

    public static AccessLevel levelFor(Role role, Screen screen) {
        if (role == null || screen == null) {
            return NONE;
        }
        return RULES.getOrDefault(role, Collections.emptyMap()).getOrDefault(screen, NONE);
    }

    public static boolean canView(Role role, Screen screen) {
        return levelFor(role, screen).atLeast(VIEW);
    }

    public static boolean canEdit(Role role, Screen screen) {
        return levelFor(role, screen).atLeast(FULL);
    }

    /** Screens the role may open, in sidebar order. */
    public static List<Screen> visibleScreens(Role role) {
        return Arrays.stream(Screen.values()).filter(m -> canView(role, m)).toList();
    }
}
