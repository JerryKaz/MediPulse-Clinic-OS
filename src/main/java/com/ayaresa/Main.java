package com.ayaresa;

import com.ayaresa.security.AuthService;
import com.ayaresa.security.User;
import com.ayaresa.shell.LoginWindow;
import com.ayaresa.shell.MainWindow;
import com.ayaresa.store.ClinicStore;
import com.ayaresa.ui.Theme;

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
