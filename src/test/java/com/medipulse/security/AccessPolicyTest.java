package com.medipulse.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessPolicyTest {

    @Test
    void adminHasFullAccessToEverything() {
        for (Screen m : Screen.values()) {
            assertEquals(AccessLevel.FULL, AccessPolicy.levelFor(Role.ADMIN, m), m.name());
        }
    }

    @Test
    void everyRoleCanOpenTheDashboard() {
        for (Role r : Role.values()) {
            assertTrue(AccessPolicy.canView(r, Screen.DASHBOARD), r.name());
        }
    }

    @Test
    void billingIsAdminAndFrontDeskOnly() {
        for (Role r : Role.values()) {
            boolean allowed = r == Role.ADMIN || r == Role.RECEPTIONIST;
            assertEquals(allowed, AccessPolicy.canView(r, Screen.BILLING), r + " billing");
        }
    }

    @Test
    void everyoneHasPersonalSettingsButOnlyAdminManagesThem() {
        for (Role r : Role.values()) {
            assertTrue(AccessPolicy.canView(r, Screen.SETTINGS), r.name());
            assertEquals(r == Role.ADMIN, AccessPolicy.canEdit(r, Screen.SETTINGS), r.name());
        }
    }

    @Test
    void receptionistCannotSeeClinicalData() {
        assertFalse(AccessPolicy.canView(Role.RECEPTIONIST, Screen.VITALS));
        assertFalse(AccessPolicy.canView(Role.RECEPTIONIST, Screen.BEDS));
        assertFalse(AccessPolicy.canView(Role.RECEPTIONIST, Screen.PHARMACY));
    }

    @Test
    void viewDoesNotImplyEdit() {
        assertTrue(AccessPolicy.canView(Role.DOCTOR, Screen.PHARMACY));
        assertFalse(AccessPolicy.canEdit(Role.DOCTOR, Screen.PHARMACY));
        assertTrue(AccessPolicy.canView(Role.PHARMACIST, Screen.PATIENTS));
        assertFalse(AccessPolicy.canEdit(Role.PHARMACIST, Screen.PATIENTS));
    }

    @Test
    void pharmacistCannotSeeClinicalScreens() {
        assertFalse(AccessPolicy.canView(Role.PHARMACIST, Screen.VITALS));
        assertFalse(AccessPolicy.canView(Role.PHARMACIST, Screen.BEDS));
        assertFalse(AccessPolicy.canView(Role.PHARMACIST, Screen.APPOINTMENTS));
    }

    @Test
    void unknownInputIsDenied() {
        assertEquals(AccessLevel.NONE, AccessPolicy.levelFor(null, Screen.PATIENTS));
        assertEquals(AccessLevel.NONE, AccessPolicy.levelFor(Role.NURSE, null));
    }

    @Test
    void visibleScreensKeepSidebarOrderAndMatchCanView() {
        for (Role r : Role.values()) {
            var visible = AccessPolicy.visibleScreens(r);
            for (Screen m : Screen.values()) {
                assertEquals(AccessPolicy.canView(r, m), visible.contains(m), r + " " + m);
            }
            for (int i = 1; i < visible.size(); i++) {
                assertTrue(visible.get(i - 1).ordinal() < visible.get(i).ordinal());
            }
        }
    }
}
