package com.wordbook.infrastructure.ui.swing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages desktop application themes with White / Light as the default,
 * supporting dynamic theme switching in Settings across all UI components.
 */
public class ThemeManager {

    public enum Theme {
        LIGHT("White / Light (Default)"),
        DARK("Dark Titanium"),
        CREAM("Warm Paper / Sepia"),
        NORDIC_BLUE("Nordic Cool Blue");

        private final String displayName;

        Theme(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public boolean isDark() {
            return this == DARK;
        }

        public Color surface() {
            return switch (this) {
                case LIGHT -> Color.WHITE;
                case DARK -> new Color(30, 41, 59);
                case CREAM -> new Color(253, 251, 247);
                case NORDIC_BLUE -> Color.WHITE;
            };
        }

        public Color border() {
            return switch (this) {
                case LIGHT -> new Color(214, 219, 225);
                case DARK -> new Color(51, 65, 85);
                case CREAM -> new Color(214, 206, 190);
                case NORDIC_BLUE -> new Color(184, 205, 224);
            };
        }

        public Color textPrimary() {
            return switch (this) {
                case LIGHT -> new Color(15, 23, 42);
                case DARK -> new Color(248, 250, 252);
                case CREAM -> new Color(41, 37, 36);
                case NORDIC_BLUE -> new Color(12, 35, 64);
            };
        }

        public Color textSecondary() {
            return switch (this) {
                case LIGHT -> new Color(71, 85, 105);
                case DARK -> new Color(148, 163, 184);
                case CREAM -> new Color(87, 83, 78);
                case NORDIC_BLUE -> new Color(51, 92, 128);
            };
        }

        public Color accent() {
            return switch (this) {
                case LIGHT -> new Color(37, 99, 235);
                case DARK -> new Color(56, 189, 248);
                case CREAM -> new Color(180, 83, 9);
                case NORDIC_BLUE -> new Color(2, 132, 199);
            };
        }

        public Color background() {
            return switch (this) {
                case LIGHT -> new Color(248, 250, 252);
                case DARK -> new Color(15, 23, 42);
                case CREAM -> new Color(247, 244, 237);
                case NORDIC_BLUE -> new Color(240, 244, 248);
            };
        }
    }

    public interface ThemeChangeListener {
        void onThemeChanged(Theme newTheme);
    }

    private static final ThemeManager INSTANCE = new ThemeManager();

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY_THEME = "notedown_theme";

    private static Theme currentTheme = Theme.LIGHT;
    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    static {
        try {
            String saved = PREFS.get(PREF_KEY_THEME, Theme.LIGHT.name());
            currentTheme = Theme.valueOf(saved);
        } catch (Exception e) {
            currentTheme = Theme.LIGHT;
        }
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void setTheme(Theme theme) {
        if (theme == null || theme == currentTheme) return;
        currentTheme = theme;
        try {
            PREFS.put(PREF_KEY_THEME, theme.name());
        } catch (Exception ignored) {}

        for (ThemeChangeListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onThemeChanged(theme);
            } catch (Exception ignored) {}
        }
    }

    public static void addListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    // Color tokens
    public static Color getAppBackground() {
        return switch (currentTheme) {
            case LIGHT -> new Color(248, 250, 252);
            case DARK -> new Color(15, 23, 42);
            case CREAM -> new Color(247, 244, 237);
            case NORDIC_BLUE -> new Color(240, 244, 248);
        };
    }

    public static Color getSurface() {
        return switch (currentTheme) {
            case LIGHT -> Color.WHITE;
            case DARK -> new Color(30, 41, 59);
            case CREAM -> new Color(253, 251, 247);
            case NORDIC_BLUE -> Color.WHITE;
        };
    }

    public static Color getSurfaceElevated() {
        return switch (currentTheme) {
            case LIGHT -> new Color(241, 245, 249);
            case DARK -> new Color(40, 53, 76);
            case CREAM -> new Color(242, 238, 227);
            case NORDIC_BLUE -> new Color(232, 239, 245);
        };
    }

    public static Color getShelfBackground() {
        return switch (currentTheme) {
            case LIGHT -> new Color(238, 242, 246);
            case DARK -> new Color(11, 16, 26);
            case CREAM -> new Color(240, 236, 226);
            case NORDIC_BLUE -> new Color(225, 235, 244);
        };
    }

    public static Color getRibbonBackground() {
        return switch (currentTheme) {
            case LIGHT -> new Color(248, 250, 252);
            case DARK -> new Color(24, 32, 47);
            case CREAM -> new Color(250, 247, 240);
            case NORDIC_BLUE -> new Color(235, 242, 248);
        };
    }

    public static Color getCanvasBackdrop() {
        return switch (currentTheme) {
            case LIGHT -> new Color(226, 232, 240);
            case DARK -> new Color(11, 16, 26);
            case CREAM -> new Color(232, 227, 213);
            case NORDIC_BLUE -> new Color(215, 226, 238);
        };
    }

    public static Color getTextPrimary() {
        return switch (currentTheme) {
            case LIGHT -> new Color(15, 23, 42);
            case DARK -> new Color(248, 250, 252);
            case CREAM -> new Color(41, 37, 36);
            case NORDIC_BLUE -> new Color(12, 35, 64);
        };
    }

    public static Color getTextSecondary() {
        return switch (currentTheme) {
            case LIGHT -> new Color(71, 85, 105);
            case DARK -> new Color(148, 163, 184);
            case CREAM -> new Color(87, 83, 78);
            case NORDIC_BLUE -> new Color(51, 92, 128);
        };
    }

    public static Color getBorder() {
        return switch (currentTheme) {
            case LIGHT -> new Color(214, 219, 225);
            case DARK -> new Color(51, 65, 85);
            case CREAM -> new Color(214, 206, 190);
            case NORDIC_BLUE -> new Color(184, 205, 224);
        };
    }

    public static Color getAccent() {
        return switch (currentTheme) {
            case LIGHT -> new Color(37, 99, 235);
            case DARK -> new Color(56, 189, 248);
            case CREAM -> new Color(180, 83, 9);
            case NORDIC_BLUE -> new Color(2, 132, 199);
        };
    }

    public static boolean isDark() {
        return currentTheme != null && currentTheme.isDark();
    }
}
