package com.medipulse.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Design tokens for the iOS 27 / macOS 27 style UI, with light and dark appearances.
 *
 * <p>Colour fields are reassigned by {@link #setDark(boolean)}; the main window is then
 * rebuilt so every component picks up the new values. Only touch them on the Swing thread.
 */
public final class Theme {

    // Apple system colours (these differ slightly in dark mode, as on iOS)
    public static Color BLUE, GREEN, ORANGE, RED, PINK, PURPLE, INDIGO, TEAL, GRAY;

    // Text
    public static Color LABEL, SECONDARY_LABEL, TERTIARY_LABEL;

    // Surfaces
    public static Color WINDOW_BG, CARD_BG, RAISED_BG, FIELD_BG, SEPARATOR, FILL, SELECTION, SELECTION_LABEL;
    /** Base colour of glass (white in light mode, near-black in dark); opacity comes from {@link #glassOpacity()}. */
    public static Color GLASS;
    public static Color SHADOW;

    // Status (background, text) pairs; identical meaning on every screen
    public static Color OK_BG, OK_FG, WARN_BG, WARN_FG, CRIT_BG, CRIT_FG, INFO_BG, INFO_FG, IDLE_BG, IDLE_FG;

    // Shape
    public static final int RADIUS_WINDOW_INSET = 22;
    public static final int RADIUS_CARD = 20;
    public static final int RADIUS_CONTROL = 12;
    public static final int GAP = 14;

    private static boolean dark;
    private static float glassOpacity = 0.62f;
    private static String fontFamily = Font.SANS_SERIF;

    static {
        applyPalette(false);
    }

    private Theme() {
    }

    /** Call once from main(), before any component is created. */
    public static void install(boolean darkMode, float glass) {
        try {
            FlatInterFont.install();
            FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        } catch (RuntimeException | LinkageError e) {
            // Font is cosmetic: fall back to the platform font rather than fail to start.
        }
        fontFamily = resolveFamily();
        Icons.load();
        setGlassOpacity(glass);
        setDark(darkMode);
    }

    /** Switches appearance. Callers must rebuild open windows afterwards. */
    public static void setDark(boolean value) {
        dark = value;
        applyPalette(value);
        UIManager.put("Component.arc", RADIUS_CONTROL);
        UIManager.put("Button.arc", 999);
        UIManager.put("TextComponent.arc", RADIUS_CONTROL);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Component.accentColor", BLUE);
        UIManager.put("Component.focusColor", BLUE);
        UIManager.put("Table.rowHeight", 44);
        if (value) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    public static boolean isDark() {
        return dark;
    }

    private static void applyPalette(boolean d) {
        BLUE = d ? new Color(0x0A84FF) : new Color(0x007AFF);
        GREEN = d ? new Color(0x30D158) : new Color(0x34C759);
        ORANGE = d ? new Color(0xFF9F0A) : new Color(0xFF9500);
        RED = d ? new Color(0xFF453A) : new Color(0xFF3B30);
        PINK = d ? new Color(0xFF375F) : new Color(0xFF2D55);
        PURPLE = d ? new Color(0xBF5AF2) : new Color(0xAF52DE);
        INDIGO = d ? new Color(0x5E5CE6) : new Color(0x5856D6);
        TEAL = d ? new Color(0x64D2FF) : new Color(0x5AC8FA);
        GRAY = new Color(0x8E8E93);

        LABEL = d ? new Color(0xF2F2F7) : new Color(0x1C1C1E);
        SECONDARY_LABEL = d ? new Color(0xAEAEB2) : new Color(0x6E6E73);
        TERTIARY_LABEL = d ? new Color(0x7C7C80) : new Color(0x8E8E93);

        WINDOW_BG = d ? new Color(0x0B0D12) : new Color(0xF2F4F8);
        CARD_BG = d ? new Color(0x1C1C1E) : Color.WHITE;
        RAISED_BG = d ? new Color(0x2C2C2E) : Color.WHITE;
        FIELD_BG = d ? new Color(255, 255, 255, 22) : new Color(255, 255, 255, 200);
        SEPARATOR = d ? new Color(0x2C2C2E) : new Color(0xEDEDF0);
        FILL = d ? new Color(118, 118, 128, 60) : new Color(118, 118, 128, 31);
        SELECTION = d ? new Color(10, 132, 255, 70) : new Color(0, 122, 255, 36);
        SELECTION_LABEL = d ? new Color(0x5AB0FF) : new Color(0x0060D0);
        GLASS = d ? new Color(30, 32, 38) : Color.WHITE;
        SHADOW = d ? new Color(0, 0, 0) : new Color(20, 30, 60);

        OK_BG = d ? new Color(0x163A22) : new Color(0xE3F6E8);
        OK_FG = d ? new Color(0x5BE07F) : new Color(0x1F7A37);
        WARN_BG = d ? new Color(0x3D2C0B) : new Color(0xFFF2DC);
        WARN_FG = d ? new Color(0xFFC155) : new Color(0x9A5B00);
        CRIT_BG = d ? new Color(0x43191A) : new Color(0xFFE5E3);
        CRIT_FG = d ? new Color(0xFF8A80) : new Color(0xB3261E);
        INFO_BG = d ? new Color(0x0F2E4F) : new Color(0xE5F0FF);
        INFO_FG = d ? new Color(0x7DBBFF) : new Color(0x0A58C2);
        IDLE_BG = d ? new Color(0x2C2C2E) : new Color(0xF2F2F7);
        IDLE_FG = d ? new Color(0xAEAEB2) : new Color(0x8E8E93);
    }

    private static String resolveFamily() {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equals("Inter")) {
                return name;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(fontFamily, style, Math.round(size));
    }

    public static Font largeTitle() {
        return font(Font.BOLD, 30);
    }

    public static Font title() {
        return font(Font.BOLD, 22);
    }

    public static Font headline() {
        return font(Font.BOLD, 17);
    }

    public static Font body() {
        return font(Font.PLAIN, 14);
    }

    public static Font bodyBold() {
        return font(Font.BOLD, 14);
    }

    public static Font footnote() {
        return font(Font.PLAIN, 12);
    }

    public static Font caption() {
        return font(Font.BOLD, 11);
    }

    public static float glassOpacity() {
        return glassOpacity;
    }

    /** Mirrors the iOS 27 Liquid Glass slider. Clamped so text on glass keeps its contrast. */
    public static void setGlassOpacity(float value) {
        glassOpacity = Math.max(0.35f, Math.min(1f, value));
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Glass fill for the current appearance and opacity. */
    public static Color glassFill() {
        float o = dark ? Math.min(1f, glassOpacity + 0.1f) : glassOpacity;
        return withAlpha(GLASS, Math.round(255 * o));
    }

    /** Subtle tint for hover states that works in both appearances. */
    public static Color hover() {
        return dark ? new Color(255, 255, 255, 20) : new Color(0, 0, 0, 12);
    }
}
