package com.ayaresa.ui;

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
    USER_CIRCLE(0xef68),
    USERS_GROUP(0xfa21),
    AMBULANCE(0xebf5),
    FIRST_AID_KIT(0xef5f),
    MOON(0xeaf8),
    SUN(0xeb30),
    DOWNLOAD(0xea96),
    TRASH(0xeb41),
    REFRESH(0xeb13),
    CHECK(0xea5e),
    PENCIL(0xeb04),
    ARROW_RIGHT(0xea1f),
    ALERT_OCTAGON(0xecc6),
    CASH(0xea55),
    DEVICE_MOBILE(0xea8a),
    CREDIT_CARD(0xea84),
    SHIELD_CHECK(0xeb22),
    ID(0xeac3),
    PHONE(0xeb09),
    DROPLET(0xea97),
    CALENDAR(0xea53),
    SEND(0xeb1e),
    HISTORY(0xebea),
    FILTER(0xeaa5),
    DOOR_ENTER(0xef4c),
    BUILDING_HOSPITAL(0xea4d),
    TEMPERATURE(0xeb38),
    LUNGS(0xef62),
    HEART_RATE_MONITOR(0xef61),
    CLIPBOARD_HEART(0xf34e),
    FILE_INVOICE(0xeb67),
    PACKAGE(0xeaff),
    CALENDAR_TIME(0xee21),
    HOURGLASS(0xef93),
    GAUGE(0xeab1),
    CHART_BAR(0xea59),
    COMMAND(0xea78),
    USER_CHECK(0xeb49),
    ADJUSTMENTS(0xea03),
    PALETTE(0xeb01),
    DATABASE(0xea88),
    FILE_EXPORT(0xede9),
    USER_SHIELD(0xf7d0),
    LIST_DETAILS(0xef40),
    ARROW_LEFT(0xea19),
    DOOR_EXIT(0xef4d),
    DOTS(0xea95);

    private final int codepoint;

    TablerIcon(int codepoint) {
        this.codepoint = codepoint;
    }

    public String glyph() {
        return new String(Character.toChars(codepoint));
    }
}
