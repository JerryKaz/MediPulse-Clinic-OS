package upsa.clinic;

import upsa.clinic.security.AuthService;
import upsa.clinic.security.User;
import upsa.clinic.shell.LoginWindow;
import upsa.clinic.shell.MainWindow;
import upsa.clinic.ui.Theme;

import javax.swing.SwingUtilities;

public final class Main {

    private static final AuthService AUTH = AuthService.withDemoUsers();

    private Main() {
    }

    public static void main(String[] args) {
        // Theme must be installed before any component is created.
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            showSignIn(null);
        });
    }

    private static void showSignIn(String notice) {
        new LoginWindow(AUTH, notice, Main::showApp).setVisible(true);
    }

    private static void showApp(User user) {
        new MainWindow(user, Main::showSignIn).setVisible(true);
    }
}
