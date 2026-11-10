package upsa.clinic.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static upsa.clinic.security.AccessLevel.FULL;
import static upsa.clinic.security.AccessLevel.NONE;
import static upsa.clinic.security.AccessLevel.VIEW;

/**
 * The single source of truth for "who can do what".
 *
 * <p>Deny by default: any module a role is not explicitly granted is {@link AccessLevel#NONE}.
 * The sidebar, the navigation guard and the dashboard all ask this class, so the rules
 * can never drift apart the way the old hard-coded arrays did.
 */
public final class AccessPolicy {

    private static final Map<Role, Map<Screen, AccessLevel>> RULES = new EnumMap<>(Role.class);

    static {
        grant(Role.ADMIN, FULL, Screen.values());

        grant(Role.DOCTOR, FULL, Screen.DASHBOARD, Screen.PATIENTS, Screen.APPOINTMENTS,
                Screen.VITALS, Screen.MESSAGES);
        grant(Role.DOCTOR, VIEW, Screen.BEDS, Screen.DOCTORS, Screen.PHARMACY);

        grant(Role.NURSE, FULL, Screen.DASHBOARD, Screen.PATIENTS, Screen.APPOINTMENTS,
                Screen.VITALS, Screen.BEDS, Screen.MESSAGES);
        grant(Role.NURSE, VIEW, Screen.DOCTORS, Screen.PHARMACY);

        grant(Role.PHARMACIST, FULL, Screen.DASHBOARD, Screen.PHARMACY, Screen.MESSAGES);
        grant(Role.PHARMACIST, VIEW, Screen.PATIENTS);
    }

    private AccessPolicy() {
    }

    private static void grant(Role role, AccessLevel level, Screen... modules) {
        Map<Screen, AccessLevel> map = RULES.computeIfAbsent(role, r -> new EnumMap<>(Screen.class));
        for (Screen m : modules) {
            map.put(m, level);
        }
    }

    public static AccessLevel levelFor(Role role, Screen module) {
        if (role == null || module == null) {
            return NONE;
        }
        return RULES.getOrDefault(role, Collections.emptyMap()).getOrDefault(module, NONE);
    }

    public static boolean canView(Role role, Screen module) {
        return levelFor(role, module).atLeast(VIEW);
    }

    public static boolean canEdit(Role role, Screen module) {
        return levelFor(role, module).atLeast(FULL);
    }

    /** Modules the role may open, in sidebar order. */
    public static List<Screen> visibleModules(Role role) {
        return java.util.Arrays.stream(Screen.values())
                .filter(m -> canView(role, m))
                .toList();
    }
}
