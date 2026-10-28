package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.ChartEngineService;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central catalog of everything the Chart Studio dialog and the Chart ribbon tabs show as
 * visual cards: chart types (with sample data), card designs and detail levels.
 * Thumbnails are rendered by the real chart engine, so what you see on the card is what you get.
 */
public final class ChartCatalog {
    private ChartCatalog() {}

    public record TypeInfo(ChartType type, String label, String hint, String sample) {}

    public record StyleInfo(String code, String label, String hint, String shortLabel) {
        public StyleInfo(String code, String label, String hint) {
            this(code, label, hint, label);
        }
    }

    public record DetailInfo(String code, String label, String hint) {}

    private static final TypeInfo[] TYPES = {
            new TypeInfo(ChartType.BAR, "Bar", "Compare categories", "Q1 Actual: 45\nQ2 Actual: 68\nQ3 Actual: 84\nQ4 Actual: 105"),
            new TypeInfo(ChartType.COLUMN, "Column", "Vertical comparison", "Alpha: 60\nBeta: 85\nGamma: 45\nDelta: 95"),
            new TypeInfo(ChartType.LINE, "Line", "Trends over time", "Jan: 20\nFeb: 35\nMar: 50\nApr: 45\nMay: 70\nJun: 88"),
            new TypeInfo(ChartType.AREA, "Area", "Volume over time", "W1: 30\nW2: 55\nW3: 42\nW4: 78\nW5: 90"),
            new TypeInfo(ChartType.PIE, "Pie", "Share of a whole", "Strategy: 40\nExecution: 30\nDesign: 20\nReview: 10"),
            new TypeInfo(ChartType.DONUT, "Donut", "Share with a total", "Completed: 65\nIn Progress: 25\nPending: 10"),
            new TypeInfo(ChartType.RADAR, "Radar", "Multi-axis scoring", "Speed: 85\nSecurity: 90\nDesign: 75\nReliability: 95\nScale: 80"),
            new TypeInfo(ChartType.SCATTER, "Scatter", "Correlation points", "A: 25\nB: 55\nC: 40\nD: 80\nE: 92"),
            new TypeInfo(ChartType.LOLLIPOP, "Lollipop", "Clean ranked values", "Mon: 12\nTue: 19\nWed: 8\nThu: 24\nFri: 16"),
            new TypeInfo(ChartType.WATERFALL, "Waterfall", "Gains and losses (use -)", "Revenue: 120\nCosts: -45\nTax: -18\nBonus: 22"),
            new TypeInfo(ChartType.FUNNEL, "Funnel", "Conversion stages", "Visitors: 1000\nSign-ups: 620\nTrials: 340\nPaid: 120"),
            new TypeInfo(ChartType.GAUGE, "Gauge", "Value vs target (2nd line = max)", "Completion: 72\nTarget: 100"),
            new TypeInfo(ChartType.POLAR, "Polar", "Radial magnitude", "North: 40\nEast: 70\nSouth: 55\nWest: 90\nNE: 30"),
            new TypeInfo(ChartType.TREE, "Treemap", "Nested proportions (indent)", "Company\n  Tech\n    Backend: 6\n    Frontend: 4\n  Ops\n    Finance: 3\n    HR: 2"),
            new TypeInfo(ChartType.DIAGRAM, "Diagram", "Hierarchy (indent)", "Project\n  Discovery\n  Design\n    Wireframes\n    Prototypes\n  Launch"),
    };

    private static final StyleInfo[] STYLES = {
            new StyleInfo("MODERN_GLASS", "Modern Glass", "Soft rounded glass card with gradient reflection", "Glass"),
            new StyleInfo("SHARP_MINIMAL", "Sharp Minimal", "Clean sharp corners with zero padding clutter", "Minimal"),
            new StyleInfo("GRADIENT", "Gradient Card", "Vibrant smooth vertical gradient background", "Gradient"),
            new StyleInfo("DARK_EXECUTIVE", "Dark Slate", "High-contrast dark slate executive card", "Dark Slate"),
            new StyleInfo("CARD_OUTLINE", "Card Outline", "Bold 2px themed border outline", "Outline"),
            new StyleInfo("ELEVATED_SHADOW", "Elevated Shadow", "Floating card with layered soft drop shadow", "Elevated"),
            new StyleInfo("WARM_CREAM", "Warm Editorial", "Warm cream paper tone with earthy accents", "Editorial"),
            new StyleInfo("CYBERPUNK", "Cyber Neon", "Pitch black card with glowing electric cyan & violet neon", "Cyber Neon"),
    };

    private static final DetailInfo[] DETAILS = {
            new DetailInfo("FULL", "Full", "Title, subtitle, grid, values and legend"),
            new DetailInfo("COMPACT", "Compact", "No subtitle, grid or axis ticks"),
            new DetailInfo("MINIMAL", "Minimal", "Headline figure + the simplest form of this chart type"),
    };

    public static TypeInfo[] types() { return TYPES.clone(); }
    public static StyleInfo[] styles() { return STYLES.clone(); }
    public static DetailInfo[] details() { return DETAILS.clone(); }

    public static TypeInfo info(ChartType type) {
        for (TypeInfo t : TYPES) if (t.type() == type) return t;
        return TYPES[0];
    }

    public static String label(ChartType type) {
        return type == ChartType.AUTO ? "Auto" : info(type).label();
    }

    /** Maps a ribbon/command name ("Bar", "Polar", "Tree"...) to a chart type. */
    public static ChartType parse(String name) {
        if (name == null) return ChartType.BAR;
        String n = name.trim().toUpperCase().replace(" ", "");
        return switch (n) {
            case "COLUMN" -> ChartType.COLUMN;
            case "LINE" -> ChartType.LINE;
            case "AREA" -> ChartType.AREA;
            case "PIE" -> ChartType.PIE;
            case "DONUT" -> ChartType.DONUT;
            case "RADAR" -> ChartType.RADAR;
            case "SCATTER" -> ChartType.SCATTER;
            case "TREE", "TREEMAP" -> ChartType.TREE;
            case "DIAGRAM", "TREEDIAGRAM" -> ChartType.DIAGRAM;
            case "LOLLIPOP" -> ChartType.LOLLIPOP;
            case "WATERFALL" -> ChartType.WATERFALL;
            case "FUNNEL" -> ChartType.FUNNEL;
            case "GAUGE" -> ChartType.GAUGE;
            case "POLAR", "POLARAREA" -> ChartType.POLAR;
            case "AUTO" -> ChartType.AUTO;
            default -> ChartType.BAR;
        };
    }

    /** Resolves a theme from either its enum name or its display name. */
    public static ChartTheme parseTheme(String name) {
        if (name == null) return ChartTheme.CORPORATE_BLUE;
        String n = name.trim();
        for (ChartTheme t : ChartTheme.values()) {
            if (t.name().equalsIgnoreCase(n) || t.getDisplayName().equalsIgnoreCase(n)) return t;
        }
        String l = n.toLowerCase();
        if (l.contains("emerald")) return ChartTheme.EMERALD_GREEN;
        if (l.contains("cyber") || l.contains("neon")) return ChartTheme.CYBERPUNK_NEON;
        if (l.contains("sunset") || l.contains("coral")) return ChartTheme.SUNSET_CORAL;
        if (l.contains("slate") && l.contains("dark")) return ChartTheme.SLATE_DARK;
        if (l.contains("pastel")) return ChartTheme.WARM_PASTEL;
        return ChartTheme.CORPORATE_BLUE;
    }

    // ------------------------------------------------------------------ thumbnails
    private static final ChartEngineService ENGINE = new ChartEngineService();
    private static final Map<String, BufferedImage> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > 160;
        }
    };

    /** Cached, engine-rendered preview. {@code data} may be null to use the chart type's own sample. */
    public static synchronized BufferedImage thumb(ChartType type, String data, ChartTheme theme, String paletteHex,
                                                   String cardStyle, String detail, int w, int h) {
        ChartType t = (type == null || type == ChartType.AUTO) ? ChartType.COLUMN : type;
        String raw = (data == null || data.isBlank()) ? info(t).sample() : data;
        String key = t + "|" + raw.hashCode() + "|" + theme + "|" + paletteHex + "|" + cardStyle + "|" + detail + "|" + w + "x" + h;
        BufferedImage img = CACHE.get(key);
        if (img == null) {
            img = ENGINE.renderThumbnail(t, raw, theme == null ? ChartTheme.CORPORATE_BLUE : theme, paletteHex,
                    cardStyle == null ? "MODERN_GLASS" : cardStyle, detail == null ? "FULL" : detail, w, h);
            CACHE.put(key, img);
        }
        return img;
    }
}
