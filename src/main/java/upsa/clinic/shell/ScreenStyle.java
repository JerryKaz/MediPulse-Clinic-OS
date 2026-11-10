package upsa.clinic.shell;

import upsa.clinic.security.Screen;
import upsa.clinic.ui.TablerIcon;
import upsa.clinic.ui.Theme;

import java.awt.Color;

/** Icon and tint for each module. Kept out of the security package so it stays UI-free. */
public final class ScreenStyle {

    private ScreenStyle() {
    }

    public static TablerIcon icon(Screen m) {
        return switch (m) {
            case DASHBOARD -> TablerIcon.LAYOUT_DASHBOARD;
            case PATIENTS -> TablerIcon.USERS;
            case APPOINTMENTS -> TablerIcon.CALENDAR_EVENT;
            case VITALS -> TablerIcon.ACTIVITY_HEARTBEAT;
            case BEDS -> TablerIcon.BED;
            case DOCTORS -> TablerIcon.STETHOSCOPE;
            case PHARMACY -> TablerIcon.PILL;
            case BILLING -> TablerIcon.RECEIPT;
            case MESSAGES -> TablerIcon.MESSAGE_CIRCLE;
            case SETTINGS -> TablerIcon.SETTINGS;
        };
    }

    /** iOS 27 / macOS 27 brought colour back to sidebar icons. */
    public static Color tint(Screen m) {
        return switch (m) {
            case DASHBOARD, PATIENTS -> Theme.BLUE;
            case APPOINTMENTS, VITALS -> Theme.PINK;
            case BEDS -> Theme.TEAL;
            case DOCTORS -> Theme.PURPLE;
            case PHARMACY, MESSAGES -> Theme.GREEN;
            case BILLING -> Theme.ORANGE;
            case SETTINGS -> Theme.GRAY;
        };
    }
}
