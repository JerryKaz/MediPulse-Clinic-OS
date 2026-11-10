package upsa.clinic.ui;

/**
 * Glyphs from the bundled Tabler icon font (outline set, MIT licence).
 * Codepoints come from tabler-icons-outline.css; browse names at https://tabler.io/icons.
 */
public enum TablerIcon {
    LAYOUT_DASHBOARD(0xf02c),
    USERS(0xebf2),
    CALENDAR_EVENT(0xea52),
    ACTIVITY_HEARTBEAT(0xf0db),
    BED(0xeb5c),
    STETHOSCOPE(0xedbe),
    PILL(0xec44),
    RECEIPT(0xedfd),
    MESSAGE_CIRCLE(0xeaed),
    SETTINGS(0xeb20),
    SEARCH(0xeb1c),
    BELL(0xea35),
    PLUS(0xeb0b),
    HEARTBEAT(0xef92),
    USER(0xeb4d),
    LOCK(0xeae2),
    EYE(0xea9a),
    EYE_OFF(0xecf0),
    LOGOUT(0xeba8),
    CIRCLE_CHECK(0xea67),
    CIRCLE_X(0xea6a),
    ALERT_TRIANGLE(0xea06),
    INFO_CIRCLE(0xeac5),
    X(0xeb55),
    CHEVRON_RIGHT(0xea61),
    CLOCK(0xea70),
    ARROW_UP_RIGHT(0xea24),
    ARROW_DOWN_RIGHT(0xea15),
    USER_PLUS(0xeb4b),
    CALENDAR_PLUS(0xebba),
    FILE_PLUS(0xeaa0),
    SHIELD_LOCK(0xed58),
    USER_CIRCLE(0xef68);

    private final int codepoint;

    TablerIcon(int codepoint) {
        this.codepoint = codepoint;
    }

    public String glyph() {
        return new String(Character.toChars(codepoint));
    }
}
