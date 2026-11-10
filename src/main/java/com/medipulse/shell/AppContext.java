package com.medipulse.shell;

import com.medipulse.security.Screen;
import com.medipulse.security.User;
import com.medipulse.store.ClinicStore;
import com.medipulse.ui.Toast;

import java.awt.Component;

/** What a screen may use: the data, the signed-in user, and navigation. */
public interface AppContext {

    ClinicStore store();

    User user();

    /** The window, for anchoring sheets and toasts. */
    Component window();

    /** Opens a screen. Refused (toast + audit entry) if the role can't view it. */
    void open(Screen screen);

    /** Opens the patient chart drawer. */
    void openPatient(String patientId);

    void toast(String message, Toast.Kind kind);

    /**
     * Runs a change against the store and turns rule violations into an error toast,
     * so screens don't need try/catch around every action. Returns true on success.
     */
    default boolean attempt(Runnable change) {
        try {
            change.run();
            return true;
        } catch (ClinicStore.RuleException | IllegalArgumentException e) {
            toast(e.getMessage(), Toast.Kind.ERROR);
            return false;
        }
    }

    default boolean canEdit(Screen s) {
        return user().canEdit(s);
    }

    /** Rebuilds the window, e.g. after switching between light and dark mode. */
    void rebuild();
}
