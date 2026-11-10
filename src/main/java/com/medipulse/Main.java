package com.medipulse;

import com.medipulse.security.AuthService;
import com.medipulse.security.User;
import com.medipulse.shell.LoginWindow;
import com.medipulse.shell.MainWindow;
import com.medipulse.store.ClinicStore;
import com.medipulse.ui.Theme;

import javax.swing.SwingUtilities;
import java.time.Clock;

public final class Main {

    private static AuthService auth;
    private static ClinicStore store;

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Theme first: no component may be created before the look and feel is installed.
            Theme.install(Preferences.darkMode(), Preferences.glassOpacity());
            store = ClinicStore.open(ClinicStore.defaultFile(), Clock.systemDefaultZone());
            auth = AuthService.withDemoUsers();
            showSignIn(null);
        });
    }

    private static void showSignIn(String notice) {
        new LoginWindow(auth, notice, Main::showApp).setVisible(true);
    }

    private static void showApp(User user) {
        new MainWindow(store, user, Main::showSignIn).setVisible(true);
    }
}
