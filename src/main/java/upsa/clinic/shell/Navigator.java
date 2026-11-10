package upsa.clinic.shell;

import upsa.clinic.security.Screen;
import upsa.clinic.ui.Toast;

/** What a screen may ask of the main window. Keeps screens decoupled from the frame. */
public interface Navigator {

    /** Opens a module. Refused (with a toast and an audit entry) if the role can't view it. */
    void open(Screen module);

    void toast(String message, Toast.Kind kind);
}
