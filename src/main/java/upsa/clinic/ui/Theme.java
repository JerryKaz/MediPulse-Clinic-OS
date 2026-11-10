package upsa.clinic.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Design tokens for the iOS 27 / macOS 27 style UI, plus one-time look-and-feel setup.
 * New code should take colours, fonts and radii from here, never hard-code them.
 */
public final class Theme {

    // Apple system colours (light appearance)
    public static final Color BLUE = new Color(0x007AFF);
    public static final Color GREEN = new Color(0x34C759);
    public static final Color ORANGE = new Color(0xFF9500);
    public static final Color RED = new Color(0xFF3B30);
    public static final Color PINK = new Color(0xFF2D55);
    public static final Color PURPLE = new Color(0xAF52DE);
    public static final Color INDIGO = new Color(0x5856D6);
    public static final Color TEAL = new Color(0x5AC8FA);
    public static final Color GRAY = new Color(0x8E8E93);

    // Text
    public static final Color LABEL = new Color(0x1C1C1E);
    public static final Color SECONDARY_LABEL = new Color(0x6E6E73);
    public static final Color TERTIARY_LABEL = new Color(0x8E8E93);

    // Surfaces
    public static final Color WINDOW_BG = new Color(0xF2F4F8);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color SEPARATOR = new Color(0xEDEDF0);
    public static final Color FILL = new Color(118, 118, 128, 31); // search fields, segmented controls
    public static final Color SELECTION = new Color(0, 122, 255, 36);

    // Status (background, text) pairs for pills, matching across every screen
    public static final Color OK_BG = new Color(0xE3F6E8), OK_FG = new Color(0x1F7A37);
    public static final Color WARN_BG = new Color(0xFFF2DC), WARN_FG = new Color(0x9A5B00);
    public static final Color CRIT_BG = new Color(0xFFE5E3), CRIT_FG = new Color(0xB3261E);
    public static final Color INFO_BG = new Color(0xE5F0FF), INFO_FG = new Color(0x0A58C2);
    public static final Color IDLE_BG = new Color(0xF2F2F7), IDLE_FG = new Color(0x8E8E93);

    // Shape
    public static final int RADIUS_WINDOW_INSET = 22; // floating sidebar / toolbar
    public static final int RADIUS_CARD = 20;
    public static final int RADIUS_CONTROL = 12;
    public static final int GAP = 14;

    /**
     * Glass opacity, 0 (clear) to 1 (solid). Mirrors the iOS 27 "Liquid Glass" slider;
     * Settings will expose it in phase 3.
     */
    private static float glassOpacity = 0.62f;

    private static String fontFamily = Font.SANS_SERIF;

    private Theme() {
    }

    /** Call once from main(), before any component is created. */
    public static void install() {
        try {
            FlatInterFont.install();
            FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        } catch (RuntimeException | LinkageError e) {
            // Font is cosmetic: fall back to the platform font rather than fail to start.
        }
        UIManager.put("Component.arc", RADIUS_CONTROL);
        UIManager.put("Button.arc", 999);
        UIManager.put("TextComponent.arc", RADIUS_CONTROL);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Component.accentColor", BLUE);
        UIManager.put("Component.focusColor", BLUE);
        UIManager.put("Table.rowHeight", 36);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("TitlePane.unifiedBackground", true);
        FlatLightLaf.setup();
        fontFamily = resolveFamily();
        Icons.load();
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

    /** Large page title, like iOS "Large Title". */
    public static Font largeTitle() {
        return font(Font.BOLD, 30);
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

    public static float glassOpacity() {
        return glassOpacity;
    }

    public static void setGlassOpacity(float value) {
        glassOpacity = Math.max(0.35f, Math.min(1f, value)); // below 0.35 text loses contrast
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
