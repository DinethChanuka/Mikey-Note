package com.wordbook.core.application.service;

import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Chart Engine providing:
 * 1. Server-side SVG rendering (for web/export).
 * 2. High-DPI Java 2D raster image rendering (for desktop canvas embedding).
 * 3. Smart pattern detection for chart types, headings, and data cleaning.
 * 4. Customizable themes, palettes, and styling.
 */
public class ChartEngineService {

    /**
     * Common theming contract shared between {@link ChartTheme} and any custom palette holder.
     * {@link ChartExtraPainters} depends only on this interface so it stays decoupled from the
     * full enum.
     */
    public interface ThemeSpec {
        Color[] getPalette();
        Color getBackground();
        Color getBorder();
        Color getTextPrimary();
        Color getTextSecondary();
    }

    public record CustomThemeSpec(ThemeSpec base, Color[] customPalette) implements ThemeSpec {
        @Override public Color[] getPalette() { return (customPalette != null && customPalette.length > 0) ? customPalette : base.getPalette(); }
        @Override public Color getBackground() { return base.getBackground(); }
        @Override public Color getBorder() { return base.getBorder(); }
        @Override public Color getTextPrimary() { return base.getTextPrimary(); }
        @Override public Color getTextSecondary() { return base.getTextSecondary(); }
    }

    public enum ChartTheme implements ThemeSpec {
        CORPORATE_BLUE("Corporate Blue",
                new Color[]{new Color(37, 99, 235), new Color(14, 165, 233), new Color(16, 185, 129), new Color(139, 92, 246), new Color(245, 158, 11)},
                Color.WHITE, new Color(226, 232, 240), new Color(15, 23, 42), new Color(100, 116, 139)),

        EMERALD_GREEN("Emerald Mint",
                new Color[]{new Color(5, 150, 105), new Color(16, 185, 129), new Color(52, 211, 153), new Color(13, 148, 136), new Color(20, 184, 166)},
                Color.WHITE, new Color(209, 250, 229), new Color(6, 78, 59), new Color(4, 120, 87)),

        CYBERPUNK_NEON("Cyberpunk Neon",
                new Color[]{new Color(139, 92, 246), new Color(6, 182, 212), new Color(236, 72, 153), new Color(245, 158, 11), new Color(16, 185, 129)},
                new Color(15, 23, 42), new Color(51, 65, 85), new Color(248, 250, 252), new Color(148, 163, 184)),

        SUNSET_CORAL("Sunset Coral",
                new Color[]{new Color(234, 88, 12), new Color(244, 63, 94), new Color(251, 191, 36), new Color(217, 119, 6), new Color(225, 29, 72)},
                Color.WHITE, new Color(254, 226, 226), new Color(67, 20, 7), new Color(153, 27, 27)),

        SLATE_DARK("Slate Dark",
                new Color[]{new Color(56, 189, 248), new Color(129, 140, 248), new Color(167, 139, 250), new Color(52, 211, 153), new Color(251, 113, 133)},
                new Color(30, 41, 59), new Color(71, 85, 105), Color.WHITE, new Color(203, 213, 225)),

        WARM_PASTEL("Warm Pastel",
                new Color[]{new Color(96, 165, 250), new Color(244, 114, 182), new Color(250, 204, 21), new Color(74, 222, 128), new Color(167, 139, 250)},
                new Color(254, 252, 249), new Color(231, 229, 228), new Color(41, 37, 36), new Color(120, 113, 108));

        private final String displayName;
        private final Color[] palette;
        private final Color background;
        private final Color border;
        private final Color textPrimary;
        private final Color textSecondary;

        ChartTheme(String displayName, Color[] palette, Color background, Color border, Color textPrimary, Color textSecondary) {
            this.displayName = displayName;
            this.palette = palette;
            this.background = background;
            this.border = border;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
        }

        public String getDisplayName() { return displayName; }
        public Color[] getPalette() { return palette; }
        public Color getBackground() { return background; }
        public Color getBorder() { return border; }
        public Color getTextPrimary() { return textPrimary; }
        public Color getTextSecondary() { return textSecondary; }

        @Override
        public String toString() { return displayName; }
    }

    private static final String[] PALETTE = {
            "#2563eb", "#e11d48", "#16a34a", "#f59e0b", "#7c3aed",
            "#0891b2", "#db2777", "#65a30d", "#ea580c", "#475569"
    };

    private static final Pattern VALUE_PATTERN = Pattern.compile("^(.*?)[:\\s]\\s*(-?\\d+(?:\\.\\d+)?)\\s*%?$");

    public record Row(String name, Double value, int depth) {}

    public static class TreeNode {
        public String name;
        public Double value;
        public int depth;
        public List<TreeNode> children = new ArrayList<>();
        public double x;
        public double depthPos;

        public TreeNode(String name, Double value, int depth) {
            this.name = name;
            this.value = value;
            this.depth = depth;
        }

        public double totalSum() {
            if (!children.isEmpty()) {
                double s = 0;
                for (TreeNode c : children) s += c.totalSum();
                return s;
            }
            return (value == null || value < 0) ? 1.0 : Math.max(value, 0.0);
        }
    }

    // =========================================================================
    // SVG Rendering Methods (Preserved for compatibility and HTML export)
    // =========================================================================
    public String render(ChartType chartType, String rawData) {
        List<Row> rows = parseRows(rawData);
        if (rows.isEmpty()) {
            return "<p style='color:#888;text-align:center;'>Add some data to generate chart.</p>";
        }

        ChartType type = (chartType == null || chartType == ChartType.AUTO) ? autoDetectType(rows) : chartType;

        try {
            return switch (type) {
                case BAR, COLUMN, LOLLIPOP, WATERFALL, FUNNEL, GAUGE, POLAR -> renderBar(rows);
                case LINE -> renderLine(rows);
                case AREA -> renderArea(rows);
                case PIE -> renderPie(rows);
                case DONUT -> renderDonut(rows);
                case RADAR -> renderRadar(rows);
                case SCATTER -> renderScatter(rows);
                case TREE -> renderTreemap(buildTree(rows));
                case DIAGRAM -> renderDiagram(buildTree(rows));
                default -> renderBar(rows);
            };
        } catch (Exception e) {
            return "<p style='color:#e11d48;text-align:center;'>Unable to draw chart: " + escapeXml(e.getMessage()) + "</p>";
        }
    }

    // =========================================================================
    // High-DPI Java 2D Raster Rendering (For Flawless Swing Canvas Embedding)
    // =========================================================================

    public BufferedImage renderChartToImage(ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height) {
        return renderChartToImage(chartType, rawData, theme, title, width, height, "MODERN_GLASS", "FULL", true, true, null);
    }

    public BufferedImage renderChartToImage(ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height,
                                            String cardStyle, String detailLevel, boolean showValues, boolean showLegend) {
        return renderChartToImage(chartType, rawData, theme, title, width, height, cardStyle, detailLevel, showValues, showLegend, null);
    }

    public BufferedImage renderChartToImage(ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height,
                                            String cardStyle, String detailLevel, boolean showValues, boolean showLegend, String paletteHex) {
        if (theme == null) theme = ChartTheme.CORPORATE_BLUE;
        if (width <= 0) width = 580;
        if (height <= 0) height = 290;
        if (cardStyle == null) cardStyle = "MODERN_GLASS";
        if (detailLevel == null) detailLevel = "FULL";

        ThemeSpec spec = theme;
        Color[] customColors = parsePalette(paletteHex);
        if (customColors != null && customColors.length > 0) {
            spec = new CustomThemeSpec(theme, customColors);
        }

        // When card style is a dark style (DARK_EXECUTIVE or CYBERPUNK),
        // adapt spec so that textPrimary, textSecondary, and borders are bright and readable
        boolean isDarkCard = "DARK_EXECUTIVE".equalsIgnoreCase(cardStyle) || "CYBERPUNK".equalsIgnoreCase(cardStyle);
        if (isDarkCard) {
            final ThemeSpec baseSpec = spec;
            final boolean isCyber = "CYBERPUNK".equalsIgnoreCase(cardStyle);
            spec = new ThemeSpec() {
                @Override public Color[] getPalette() { return baseSpec.getPalette(); }
                @Override public Color getBackground() { return baseSpec.getBackground(); }
                @Override public Color getBorder() { return isCyber ? new Color(6, 182, 212, 120) : new Color(51, 65, 85); }
                @Override public Color getTextPrimary() { return Color.WHITE; }
                @Override public Color getTextSecondary() { return isCyber ? new Color(165, 243, 252) : new Color(148, 163, 184); }
            };
        }

        List<Row> rows = parseRows(rawData);
        ChartType type = (chartType == null || chartType == ChartType.AUTO) ? autoDetectType(rows) : chartType;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw Card Background according to Card Design Style
        paintCardBackground(g2, width, height, spec, cardStyle);

        int topMargin = 28;

        // Draw Header Title & Detail Level Adjustment
        if (title != null && !title.isBlank()) {
            Color titleColor = spec.getTextPrimary();
            Color subColor = spec.getTextSecondary();

            g2.setColor(titleColor);
            g2.setFont(new Font("Segoe UI", Font.BOLD, "MINIMAL".equalsIgnoreCase(detailLevel) ? 13 : 15));
            g2.drawString(title, 24, 28);

            if (!"COMPACT".equalsIgnoreCase(detailLevel) && !"MINIMAL".equalsIgnoreCase(detailLevel)) {
                g2.setColor(subColor);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.drawString("Visualized with Intelligent Chart Engine", 24, 45);
                topMargin = 62;
            } else {
                topMargin = 42;
            }
        }

        if (rows.isEmpty()) {
            g2.setColor(spec.getTextSecondary());
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            g2.drawString("No data provided to generate visualization.", 40, height / 2);
            g2.dispose();
            return image;
        }

        // Minimal Detail KPI Badge Mode
        if ("MINIMAL".equalsIgnoreCase(detailLevel)) {
            if (!ChartExtraPainters.drawMinimal(g2, type, rows, spec, width, height, topMargin)) {
                drawMinimalKpiImage(g2, rows, spec, width, height, topMargin);
            }
            g2.dispose();
            return image;
        }

        boolean compact = "COMPACT".equalsIgnoreCase(detailLevel);

        switch (type) {
            case BAR -> drawBarImage(g2, rows, spec, width, height, topMargin, showValues);
            case COLUMN -> drawColumnImage(g2, rows, spec, width, height, topMargin, showValues);
            case LINE -> drawLineImage(g2, rows, spec, width, height, topMargin, showValues);
            case AREA -> drawAreaImage(g2, rows, spec, width, height, topMargin, showValues);
            case PIE -> drawPieImage(g2, rows, spec, width, height, topMargin, showValues, showLegend);
            case DONUT -> drawDonutImage(g2, rows, spec, width, height, topMargin, showValues, showLegend);
            case RADAR -> drawRadarImage(g2, rows, spec, width, height, topMargin, showValues);
            case SCATTER -> drawScatterImage(g2, rows, spec, width, height, topMargin, showValues);
            case TREE -> drawTreemapImage(g2, buildTree(rows), spec, width, height, topMargin);
            case DIAGRAM -> drawDiagramImage(g2, buildTree(rows), spec, width, height, topMargin);
            // New types delegated to ChartExtraPainters
            case LOLLIPOP -> ChartExtraPainters.drawLollipop(g2, rows, spec, width, height, topMargin, showValues, compact);
            case WATERFALL -> ChartExtraPainters.drawWaterfall(g2, rows, spec, width, height, topMargin, showValues, compact);
            case FUNNEL -> ChartExtraPainters.drawFunnel(g2, rows, spec, width, height, topMargin, showValues, showLegend);
            case GAUGE -> ChartExtraPainters.drawGauge(g2, rows, spec, width, height, topMargin, showValues);
            case POLAR -> ChartExtraPainters.drawPolar(g2, rows, spec, width, height, topMargin, showValues, showLegend);
            default -> drawBarImage(g2, rows, spec, width, height, topMargin, showValues);
        }

        g2.dispose();
        return image;
    }

    private void paintCardBackground(Graphics2D g2, int width, int height, ThemeSpec theme, String cardStyle) {
        String style = cardStyle != null ? cardStyle.toUpperCase() : "MODERN_GLASS";
        switch (style) {
            case "SHARP_MINIMAL" -> {
                g2.setColor(theme.getBackground());
                g2.fillRect(0, 0, width, height);
                g2.setColor(theme.getBorder());
                g2.drawRect(0, 0, width - 1, height - 1);
            }
            case "GRADIENT" -> {
                Color topC = theme.getBackground();
                Color botC = new Color(
                        Math.max(0, topC.getRed() - 12),
                        Math.max(0, topC.getGreen() - 10),
                        Math.min(255, topC.getBlue() + 14)
                );
                GradientPaint gp = new GradientPaint(0, 0, topC, 0, height, botC);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, width, height, 16, 16);
                g2.setColor(theme.getBorder());
                g2.drawRoundRect(0, 0, width - 1, height - 1, 16, 16);
            }
            case "DARK_EXECUTIVE" -> {
                g2.setColor(new Color(15, 23, 42));
                g2.fillRoundRect(0, 0, width, height, 16, 16);
                g2.setColor(new Color(51, 65, 85));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 16, 16);
            }
            case "CARD_OUTLINE" -> {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, width, height, 12, 12);
                g2.setColor(theme.getPalette()[0]);
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawRoundRect(1, 1, width - 3, height - 3, 12, 12);
                g2.setStroke(new BasicStroke(1.0f));
            }
            case "ELEVATED_SHADOW" -> {
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(3, 4, width - 6, height - 5, 16, 16);
                g2.setColor(new Color(0, 0, 0, 32));
                g2.fillRoundRect(1, 2, width - 2, height - 3, 16, 16);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, width, height - 2, 16, 16);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, width - 1, height - 3, 16, 16);
            }
            case "WARM_CREAM" -> {
                g2.setColor(new Color(254, 252, 249));
                g2.fillRoundRect(0, 0, width, height, 14, 14);
                g2.setColor(new Color(226, 217, 204));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);
            }
            case "CYBERPUNK" -> {
                g2.setColor(new Color(9, 13, 22));
                g2.fillRoundRect(0, 0, width, height, 14, 14);
                g2.setColor(new Color(6, 182, 212));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);
                g2.setStroke(new BasicStroke(1.0f));
            }
            default -> { // MODERN_GLASS
                g2.setColor(theme.getBackground());
                g2.fillRoundRect(0, 0, width, height, 16, 16);
                g2.setColor(theme.getBorder());
                g2.drawRoundRect(0, 0, width - 1, height - 1, 16, 16);
            }
        }
    }

    public record ChartMetadata(
            ChartType type,
            String title,
            ChartTheme theme,
            String rawData,
            int width,
            int height,
            String cardStyle,
            String detailLevel,
            boolean showValues,
            boolean showLegend,
            String paletteHex
    ) {
        public ChartMetadata(ChartType type, String title, ChartTheme theme, String rawData, int width, int height) {
            this(type, title, theme, rawData, width, height, "MODERN_GLASS", "FULL", true, true, null);
        }

        public ChartMetadata(ChartType type, String title, ChartTheme theme, String rawData, int width, int height,
                             String cardStyle, String detailLevel, boolean showValues, boolean showLegend) {
            this(type, title, theme, rawData, width, height, cardStyle, detailLevel, showValues, showLegend, null);
        }
    }

    public File renderChartToFile(ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height) throws IOException {
        return renderChartToFile(chartType, rawData, theme, title, width, height, "MODERN_GLASS", "FULL", true, true, null);
    }

    public File renderChartToFile(ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height,
                                  String cardStyle, String detailLevel, boolean showValues, boolean showLegend) throws IOException {
        return renderChartToFile(chartType, rawData, theme, title, width, height, cardStyle, detailLevel, showValues, showLegend, null);
    }

    public File renderChartToFile(ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height,
                                  String cardStyle, String detailLevel, boolean showValues, boolean showLegend, String paletteHex) throws IOException {
        BufferedImage image = renderChartToImage(chartType, rawData, theme, title, width, height, cardStyle, detailLevel, showValues, showLegend, paletteHex);

        Path chartsDir = Paths.get(System.getProperty("user.home"), ".wordbook", "charts");
        Files.createDirectories(chartsDir);

        String filename = "chart_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + ".png";
        File file = chartsDir.resolve(filename).toFile();
        ImageIO.write(image, "png", file);

        ChartMetadata meta = new ChartMetadata(chartType, title, theme, rawData, width, height, cardStyle, detailLevel, showValues, showLegend, paletteHex);
        saveChartMetadata(file, meta);

        return file;
    }

    public void reRenderChartFile(File existingFile, ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height) throws IOException {
        ChartMetadata old = readChartMetadata(existingFile);
        String style = old != null ? old.cardStyle() : "MODERN_GLASS";
        String detail = old != null ? old.detailLevel() : "FULL";
        boolean showVal = old != null ? old.showValues() : true;
        boolean showLeg = old != null ? old.showLegend() : true;
        String palette = old != null ? old.paletteHex() : null;
        reRenderChartFile(existingFile, chartType, rawData, theme, title, width, height, style, detail, showVal, showLeg, palette);
    }

    public void reRenderChartFile(File existingFile, ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height,
                                  String cardStyle, String detailLevel, boolean showValues, boolean showLegend) throws IOException {
        ChartMetadata old = readChartMetadata(existingFile);
        String palette = old != null ? old.paletteHex() : null;
        reRenderChartFile(existingFile, chartType, rawData, theme, title, width, height, cardStyle, detailLevel, showValues, showLegend, palette);
    }

    public void reRenderChartFile(File existingFile, ChartType chartType, String rawData, ChartTheme theme, String title, int width, int height,
                                  String cardStyle, String detailLevel, boolean showValues, boolean showLegend, String paletteHex) throws IOException {
        BufferedImage image = renderChartToImage(chartType, rawData, theme, title, width, height, cardStyle, detailLevel, showValues, showLegend, paletteHex);
        ImageIO.write(image, "png", existingFile);
        saveChartMetadata(existingFile, new ChartMetadata(chartType, title, theme, rawData, width, height, cardStyle, detailLevel, showValues, showLegend, paletteHex));
    }

    public void saveChartMetadata(File chartFile, ChartMetadata meta) {
        if (chartFile == null || meta == null) return;
        try {
            File metaFile = new File(chartFile.getParentFile(), chartFile.getName().replace(".png", ".meta.txt"));
            Properties props = new Properties();
            props.setProperty("type", meta.type() != null ? meta.type().name() : "BAR");
            props.setProperty("title", meta.title() != null ? meta.title() : "Chart");
            props.setProperty("theme", meta.theme() != null ? meta.theme().name() : "CORPORATE_BLUE");
            props.setProperty("rawData", meta.rawData() != null ? meta.rawData() : "");
            props.setProperty("width", String.valueOf(meta.width()));
            props.setProperty("height", String.valueOf(meta.height()));
            props.setProperty("cardStyle", meta.cardStyle() != null ? meta.cardStyle() : "MODERN_GLASS");
            props.setProperty("detailLevel", meta.detailLevel() != null ? meta.detailLevel() : "FULL");
            props.setProperty("showValues", String.valueOf(meta.showValues()));
            props.setProperty("showLegend", String.valueOf(meta.showLegend()));
            props.setProperty("paletteHex", meta.paletteHex() != null ? meta.paletteHex() : "");
            try (var out = Files.newOutputStream(metaFile.toPath())) {
                props.store(out, "WordBook Chart Metadata");
            }
        } catch (Exception ignored) {}
    }

    public ChartMetadata readChartMetadata(File chartFile) {
        if (chartFile == null) return null;
        try {
            File metaFile = new File(chartFile.getParentFile(), chartFile.getName().replace(".png", ".meta.txt"));
            if (!metaFile.exists()) return null;
            Properties props = new Properties();
            try (var in = Files.newInputStream(metaFile.toPath())) {
                props.load(in);
            }
            ChartType type = ChartType.valueOf(props.getProperty("type", "BAR"));
            String title = props.getProperty("title", "Chart");
            ChartTheme theme = ChartTheme.valueOf(props.getProperty("theme", "CORPORATE_BLUE"));
            String rawData = props.getProperty("rawData", "");
            int width = Integer.parseInt(props.getProperty("width", "580"));
            int height = Integer.parseInt(props.getProperty("height", "290"));
            String cardStyle = props.getProperty("cardStyle", "MODERN_GLASS");
            String detailLevel = props.getProperty("detailLevel", "FULL");
            boolean showValues = Boolean.parseBoolean(props.getProperty("showValues", "true"));
            boolean showLegend = Boolean.parseBoolean(props.getProperty("showLegend", "true"));
            String paletteHex = props.getProperty("paletteHex", null);
            if (paletteHex != null && paletteHex.isBlank()) paletteHex = null;
            return new ChartMetadata(type, title, theme, rawData, width, height, cardStyle, detailLevel, showValues, showLegend, paletteHex);
        } catch (Exception e) {
            return null;
        }
    }

    private void drawBarImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues) {
        int count = rows.size();
        int bottomM = 40;
        int leftM = 48;
        int rightM = 32;

        int chartH = h - topM - bottomM;
        int chartW = w - leftM - rightM;

        double maxVal = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        // Baseline and subtle grid lines
        g2.setColor(theme.getBorder());
        g2.drawLine(leftM, h - bottomM, w - rightM, h - bottomM);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(theme.getTextSecondary());
        for (int i = 1; i <= 3; i++) {
            int gridY = (h - bottomM) - (int) ((double) i / 4.0 * chartH);
            g2.drawLine(leftM, gridY, w - rightM, gridY);
            String valLabel = formatNum(maxVal * i / 4.0);
            g2.drawString(valLabel, 10, gridY + 4);
        }

        Color[] palette = theme.getPalette();
        int slotW = chartW / count;
        int barW = Math.max(16, Math.min(56, (int) (slotW * 0.65)));

        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;
            int barH = (int) ((val / maxVal) * chartH);
            int x = leftM + i * slotW + (slotW - barW) / 2;
            int y = (h - bottomM) - barH;

            // Bar with rounded top
            g2.setColor(palette[i % palette.length]);
            g2.fillRoundRect(x, y, barW, barH, 8, 8);

            // Value label
            if (showValues) {
                g2.setColor(theme.getTextPrimary());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String valStr = formatNum(val);
                int valW = g2.getFontMetrics().stringWidth(valStr);
                g2.drawString(valStr, x + (barW - valW) / 2, y - 5);
            }

            // Name label
            g2.setColor(theme.getTextSecondary());
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String nameStr = truncate(r.name(), 12);
            int nameW = g2.getFontMetrics().stringWidth(nameStr);
            g2.drawString(nameStr, x + (barW - nameW) / 2, h - bottomM + 18);
        }
    }

    private void drawColumnImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues) {
        drawBarImage(g2, rows, theme, w, h, topM, showValues);
    }

    private void drawLineImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues) {
        int count = rows.size();
        int bottomM = 40;
        int leftM = 48;
        int rightM = 32;

        int chartH = h - topM - bottomM;
        int chartW = w - leftM - rightM;

        double maxVal = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        // Baseline and grid
        g2.setColor(theme.getBorder());
        g2.drawLine(leftM, h - bottomM, w - rightM, h - bottomM);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(theme.getTextSecondary());
        for (int i = 1; i <= 3; i++) {
            int gridY = (h - bottomM) - (int) ((double) i / 4.0 * chartH);
            g2.drawLine(leftM, gridY, w - rightM, gridY);
            g2.drawString(formatNum(maxVal * i / 4.0), 10, gridY + 4);
        }

        int step = count > 1 ? chartW / (count - 1) : chartW;
        int[] xs = new int[count];
        int[] ys = new int[count];

        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;
            xs[i] = leftM + (count > 1 ? i * step : chartW / 2);
            ys[i] = (h - bottomM) - (int) ((val / maxVal) * chartH);
        }

        // Draw line curve / segments
        Color mainColor = theme.getPalette()[0];
        g2.setColor(mainColor);
        g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < count - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }
        g2.setStroke(new BasicStroke(1.0f));

        // Draw dots and text
        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;

            g2.setColor(mainColor);
            g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
            g2.setColor(theme.getBackground());
            g2.fillOval(xs[i] - 2, ys[i] - 2, 4, 4);

            if (showValues) {
                g2.setColor(theme.getTextPrimary());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String valStr = formatNum(val);
                int valW = g2.getFontMetrics().stringWidth(valStr);
                g2.drawString(valStr, xs[i] - valW / 2, ys[i] - 9);
            }

            g2.setColor(theme.getTextSecondary());
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String nameStr = truncate(r.name(), 10);
            int nameW = g2.getFontMetrics().stringWidth(nameStr);
            g2.drawString(nameStr, xs[i] - nameW / 2, h - bottomM + 18);
        }
    }

    private void drawAreaImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues) {
        int count = rows.size();
        int bottomM = 40;
        int leftM = 48;
        int rightM = 32;

        int chartH = h - topM - bottomM;
        int chartW = w - leftM - rightM;

        double maxVal = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        // Baseline and grid
        g2.setColor(theme.getBorder());
        g2.drawLine(leftM, h - bottomM, w - rightM, h - bottomM);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(theme.getTextSecondary());
        for (int i = 1; i <= 3; i++) {
            int gridY = (h - bottomM) - (int) ((double) i / 4.0 * chartH);
            g2.drawLine(leftM, gridY, w - rightM, gridY);
            g2.drawString(formatNum(maxVal * i / 4.0), 10, gridY + 4);
        }

        int step = count > 1 ? chartW / (count - 1) : chartW;
        int[] xs = new int[count];
        int[] ys = new int[count];

        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;
            xs[i] = leftM + (count > 1 ? i * step : chartW / 2);
            ys[i] = (h - bottomM) - (int) ((val / maxVal) * chartH);
        }

        Color mainColor = theme.getPalette()[0];

        // Fill area gradient
        if (count > 1) {
            Path2D area = new Path2D.Float();
            area.moveTo(xs[0], h - bottomM);
            for (int i = 0; i < count; i++) {
                area.lineTo(xs[i], ys[i]);
            }
            area.lineTo(xs[count - 1], h - bottomM);
            area.closePath();

            Color topColor = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 140);
            Color botColor = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 15);
            GradientPaint gp = new GradientPaint(0, topM, topColor, 0, h - bottomM, botColor);
            g2.setPaint(gp);
            g2.fill(area);
        }

        // Draw top line
        g2.setColor(mainColor);
        g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < count - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }
        g2.setStroke(new BasicStroke(1.0f));

        // Draw dots and text
        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;

            g2.setColor(mainColor);
            g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
            g2.setColor(theme.getBackground());
            g2.fillOval(xs[i] - 2, ys[i] - 2, 4, 4);

            if (showValues) {
                g2.setColor(theme.getTextPrimary());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String valStr = formatNum(val);
                int valW = g2.getFontMetrics().stringWidth(valStr);
                g2.drawString(valStr, xs[i] - valW / 2, ys[i] - 9);
            }

            g2.setColor(theme.getTextSecondary());
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String nameStr = truncate(r.name(), 10);
            int nameW = g2.getFontMetrics().stringWidth(nameStr);
            g2.drawString(nameStr, xs[i] - nameW / 2, h - bottomM + 18);
        }
    }

    private void drawPieImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues, boolean showLegend) {
        double total = rows.stream().mapToDouble(r -> Math.max(r.value() != null ? r.value() : 1.0, 0.0)).sum();
        if (total <= 0) total = 1.0;

        int availH = h - topM - 20;
        int diameter = Math.min(availH, 180);
        int centerX = showLegend ? 130 : w / 2;
        int centerY = topM + availH / 2;
        int radius = diameter / 2;

        Color[] palette = theme.getPalette();
        double startAngle = -90.0;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            double val = Math.max(r.value() != null ? r.value() : 1.0, 0.0);
            double arc = (val / total) * 360.0;

            g2.setColor(palette[i % palette.length]);
            g2.fill(new Arc2D.Double(centerX - radius, centerY - radius, diameter, diameter, startAngle, arc, Arc2D.PIE));
            startAngle += arc;
        }

        // Legend on the right side
        if (showLegend) {
            int legendX = centerX + radius + 32;
            int legendY = topM + 14;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                double val = Math.max(r.value() != null ? r.value() : 1.0, 0.0);
                int percent = (int) Math.round((val / total) * 100.0);

                g2.setColor(palette[i % palette.length]);
                g2.fillRoundRect(legendX, legendY + i * 26, 12, 12, 3, 3);

                g2.setColor(theme.getTextPrimary());
                String label = truncate(r.name(), 16) + (showValues ? " (" + percent + "%)" : "");
                g2.drawString(label, legendX + 20, legendY + i * 26 + 11);
            }
        }
    }

    private void drawDonutImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues, boolean showLegend) {
        double total = rows.stream().mapToDouble(r -> Math.max(r.value() != null ? r.value() : 1.0, 0.0)).sum();
        if (total <= 0) total = 1.0;

        int availH = h - topM - 20;
        int diameter = Math.min(availH, 180);
        int centerX = showLegend ? 130 : w / 2;
        int centerY = topM + availH / 2;
        int radius = diameter / 2;

        Color[] palette = theme.getPalette();
        double startAngle = -90.0;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            double val = Math.max(r.value() != null ? r.value() : 1.0, 0.0);
            double arc = (val / total) * 360.0;

            g2.setColor(palette[i % palette.length]);
            g2.fill(new Arc2D.Double(centerX - radius, centerY - radius, diameter, diameter, startAngle, arc, Arc2D.PIE));
            startAngle += arc;
        }

        // Inner circle cutout for Donut
        int innerRadius = (int) (radius * 0.55);
        g2.setColor(theme.getBackground());
        g2.fillOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);

        // Center total label
        g2.setColor(theme.getTextPrimary());
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        String totalStr = formatNum(total);
        int totW = g2.getFontMetrics().stringWidth(totalStr);
        g2.drawString(totalStr, centerX - totW / 2, centerY + 3);

        g2.setColor(theme.getTextSecondary());
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        int subW = g2.getFontMetrics().stringWidth("TOTAL");
        g2.drawString("TOTAL", centerX - subW / 2, centerY + 16);

        // Legend on the right side
        if (showLegend) {
            int legendX = centerX + radius + 32;
            int legendY = topM + 14;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                double val = Math.max(r.value() != null ? r.value() : 1.0, 0.0);
                int percent = (int) Math.round((val / total) * 100.0);

                g2.setColor(palette[i % palette.length]);
                g2.fillRoundRect(legendX, legendY + i * 26, 12, 12, 3, 3);

                g2.setColor(theme.getTextPrimary());
                String label = truncate(r.name(), 16) + (showValues ? " (" + percent + "%)" : "");
                g2.drawString(label, legendX + 20, legendY + i * 26 + 11);
            }
        }
    }

    private void drawRadarImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues) {
        int count = Math.max(3, rows.size());
        int availH = h - topM - 30;
        int radius = Math.min(availH / 2, 95);
        int cx = w / 2;
        int cy = topM + availH / 2;

        double maxVal = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        // Concentric web polygons
        g2.setColor(theme.getBorder());
        for (int ring = 1; ring <= 3; ring++) {
            int rR = (int) (radius * (ring / 3.0));
            int[] px = new int[count];
            int[] py = new int[count];
            for (int i = 0; i < count; i++) {
                double angle = -Math.PI / 2 + 2 * Math.PI * i / count;
                px[i] = (int) (cx + rR * Math.cos(angle));
                py[i] = (int) (cy + rR * Math.sin(angle));
            }
            g2.drawPolygon(px, py, count);
        }

        // Spokes and labels
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / count;
            int ex = (int) (cx + radius * Math.cos(angle));
            int ey = (int) (cy + radius * Math.sin(angle));
            g2.setColor(theme.getBorder());
            g2.drawLine(cx, cy, ex, ey);

            if (i < rows.size()) {
                String name = truncate(rows.get(i).name(), 10);
                int lx = (int) (cx + (radius + 16) * Math.cos(angle));
                int ly = (int) (cy + (radius + 16) * Math.sin(angle));
                g2.setColor(theme.getTextSecondary());
                int lw = g2.getFontMetrics().stringWidth(name);
                g2.drawString(name, lx - lw / 2, ly + 4);
            }
        }

        // Data Polygon
        int[] dx = new int[rows.size()];
        int[] dy = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            double val = rows.get(i).value() != null ? rows.get(i).value() : 1.0;
            double fraction = Math.min(1.0, val / maxVal);
            double angle = -Math.PI / 2 + 2 * Math.PI * i / count;
            dx[i] = (int) (cx + (radius * fraction) * Math.cos(angle));
            dy[i] = (int) (cy + (radius * fraction) * Math.sin(angle));
        }

        Color mainColor = theme.getPalette()[0];
        Composite orig = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(mainColor);
        g2.fillPolygon(dx, dy, rows.size());
        g2.setComposite(orig);

        g2.setColor(mainColor);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawPolygon(dx, dy, rows.size());
        g2.setStroke(new BasicStroke(1.0f));

        for (int i = 0; i < rows.size(); i++) {
            g2.setColor(mainColor);
            g2.fillOval(dx[i] - 4, dy[i] - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.fillOval(dx[i] - 2, dy[i] - 2, 4, 4);
        }
    }

    private void drawScatterImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues) {
        int count = rows.size();
        int bottomM = 40;
        int leftM = 48;
        int rightM = 32;

        int chartH = h - topM - bottomM;
        int chartW = w - leftM - rightM;

        double maxVal = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        // Baseline and grid
        g2.setColor(theme.getBorder());
        g2.drawLine(leftM, h - bottomM, w - rightM, h - bottomM);
        g2.drawLine(leftM, topM, leftM, h - bottomM);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(theme.getTextSecondary());
        for (int i = 1; i <= 3; i++) {
            int gridY = (h - bottomM) - (int) ((double) i / 4.0 * chartH);
            g2.drawLine(leftM, gridY, w - rightM, gridY);
            g2.drawString(formatNum(maxVal * i / 4.0), 10, gridY + 4);
        }

        Color[] palette = theme.getPalette();
        int step = count > 1 ? chartW / (count + 1) : chartW / 2;

        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;
            int px = leftM + (i + 1) * step;
            int py = (h - bottomM) - (int) ((val / maxVal) * chartH);

            Color ptColor = palette[i % palette.length];

            // Halo
            Composite orig = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            g2.setColor(ptColor);
            g2.fillOval(px - 10, py - 10, 20, 20);
            g2.setComposite(orig);

            // Core dot
            g2.setColor(ptColor);
            g2.fillOval(px - 5, py - 5, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillOval(px - 2, py - 2, 4, 4);

            if (showValues) {
                g2.setColor(theme.getTextPrimary());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(formatNum(val), px - 10, py - 12);
            }

            g2.setColor(theme.getTextSecondary());
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            String nameStr = truncate(r.name(), 10);
            int nameW = g2.getFontMetrics().stringWidth(nameStr);
            g2.drawString(nameStr, px - nameW / 2, h - bottomM + 18);
        }
    }

    private void drawMinimalKpiImage(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM) {
        double total = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 0.0).sum();
        double firstVal = !rows.isEmpty() && rows.get(0).value() != null ? rows.get(0).value() : total;

        Color mainColor = theme.getPalette()[0];

        // Big Headline Value
        g2.setColor(theme.getTextPrimary());
        g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        String valStr = formatNum(firstVal);
        g2.drawString(valStr, 24, topM + 32);

        // Subtitle / Primary Metric Tag
        g2.setColor(theme.getTextSecondary());
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String metricName = !rows.isEmpty() ? rows.get(0).name() : "Key Performance Metric";
        g2.drawString(metricName + " (Total: " + formatNum(total) + ")", 24, topM + 52);

        // Mini sparkline along the bottom
        int sparkY = h - 30;
        int sparkW = w - 48;
        int count = rows.size();
        if (count > 1) {
            double maxV = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
            if (maxV <= 0) maxV = 1.0;

            int step = sparkW / (count - 1);
            int[] xs = new int[count];
            int[] ys = new int[count];
            for (int i = 0; i < count; i++) {
                double v = rows.get(i).value() != null ? rows.get(i).value() : 1.0;
                xs[i] = 24 + i * step;
                ys[i] = sparkY - (int) ((v / maxV) * 26);
            }

            g2.setColor(mainColor);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < count - 1; i++) {
                g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            }
            g2.setStroke(new BasicStroke(1.0f));

            g2.fillOval(xs[count - 1] - 4, ys[count - 1] - 4, 8, 8);
        }
    }

    private void drawTreemapImage(Graphics2D g2, TreeNode root, ThemeSpec theme, int w, int h, int topM) {
        int availW = w - 48;
        int availH = h - topM - 20;

        List<TreeNode> nodes = root.children.isEmpty() ? List.of(root) : root.children;
        double sum = root.totalSum();
        if (sum <= 0) sum = 1.0;

        Color[] palette = theme.getPalette();
        double currentX = 24;

        for (int i = 0; i < nodes.size(); i++) {
            TreeNode n = nodes.get(i);
            double fraction = n.totalSum() / sum;
            int tileW = Math.max(40, (int) (availW * fraction));
            int tileH = availH;

            g2.setColor(palette[i % palette.length]);
            g2.fillRoundRect((int) currentX, topM + 10, tileW - 4, tileH, 8, 8);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.drawString(truncate(n.name, 14), (int) currentX + 8, topM + 32);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(formatNum(n.totalSum()), (int) currentX + 8, topM + 48);

            currentX += tileW;
        }
    }

    private void drawDiagramImage(Graphics2D g2, TreeNode root, ThemeSpec theme, int w, int h, int topM) {
        g2.setColor(theme.getTextPrimary());
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Draw Root Node
        int rootX = 36;
        int rootY = topM + 30;
        int nodeW = 120;
        int nodeH = 34;

        g2.setColor(theme.getPalette()[0]);
        g2.fillRoundRect(rootX, rootY, nodeW, nodeH, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString(truncate(root.name.isEmpty() ? "Root" : root.name, 12), rootX + 12, rootY + 22);

        // Draw Children Nodes
        List<TreeNode> children = root.children;
        int childX = rootX + nodeW + 50;
        int childGap = Math.max(38, (h - topM - 40) / Math.max(children.size(), 1));

        for (int i = 0; i < children.size(); i++) {
            TreeNode c = children.get(i);
            int childY = topM + 10 + i * childGap;

            // Connector curve
            g2.setColor(theme.getBorder());
            g2.drawLine(rootX + nodeW, rootY + nodeH / 2, childX, childY + 14);

            g2.setColor(theme.getPalette()[(i + 1) % theme.getPalette().length]);
            g2.fillRoundRect(childX, childY, 130, 28, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString(truncate(c.name, 14), childX + 10, childY + 18);
        }
    }

    public List<Row> parseRows(String text) {
        if (text == null) return Collections.emptyList();
        List<Row> result = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String rawLine : lines) {
            if (rawLine.trim().isEmpty() || rawLine.trim().startsWith("#")) continue;

            int indent = 0;
            for (char c : rawLine.toCharArray()) {
                if (c == ' ') indent++;
                else if (c == '\t') indent += 2;
                else break;
            }
            int depth = indent / 2;

            String trimmed = rawLine.trim();
            Double val = null;
            String name = trimmed;

            Matcher m = VALUE_PATTERN.matcher(trimmed);
            if (m.matches()) {
                String potentialName = m.group(1).replaceAll("[:\\-\\s]+$", "");
                name = potentialName.isEmpty() ? name : potentialName;
                try {
                    val = Double.parseDouble(m.group(2));
                } catch (NumberFormatException ignored) {}
            }
            result.add(new Row(name, val, depth));
        }
        return result;
    }

    public ChartType autoDetectType(String rawData) {
        return autoDetectType(parseRows(rawData));
    }

    public ChartType autoDetectType(List<Row> rows) {
        if (rows.stream().anyMatch(r -> r.depth() > 0)) {
            return ChartType.DIAGRAM;
        }
        boolean allHaveNonNegValues = rows.stream().allMatch(r -> r.value() != null && r.value() >= 0);
        if (allHaveNonNegValues) {
            // If rows have time-like labels (Jan, Feb, Q1, 2024, Monday, etc.) -> LINE
            boolean looksLikeTime = rows.stream().anyMatch(r -> {
                String n = r.name().toLowerCase();
                return n.contains("jan") || n.contains("feb") || n.contains("mar") || n.contains("apr") ||
                        n.contains("may") || n.contains("jun") || n.contains("jul") || n.contains("aug") ||
                        n.contains("sep") || n.contains("oct") || n.contains("nov") || n.contains("dec") ||
                        n.contains("sprint") || n.contains("week") || n.contains("day") || n.matches(".*\\d{4}.*");
            });
            if (looksLikeTime && rows.size() >= 3) return ChartType.LINE;

            return (rows.size() > 1 && rows.size() <= 5) ? ChartType.PIE : ChartType.BAR;
        }
        return ChartType.DIAGRAM;
    }

    public String extractTitleFromData(String rawData) {
        if (rawData == null) return "Data Visualization";
        for (String line : rawData.split("\n")) {
            line = line.trim();
            if (line.startsWith("#")) {
                return line.replaceFirst("^#+\\s*", "");
            }
            if (line.toLowerCase().startsWith("title:")) {
                return line.substring(6).trim();
            }
        }
        return "Data Visualization";
    }

    private TreeNode buildTree(List<Row> rows) {
        TreeNode root = new TreeNode("", null, -1);
        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);

        for (Row r : rows) {
            TreeNode node = new TreeNode(r.name(), r.value(), r.depth());
            while (!stack.isEmpty() && stack.peek().depth >= node.depth) {
                stack.pop();
            }
            if (stack.peek() != null) {
                stack.peek().children.add(node);
            }
            stack.push(node);
        }

        if (root.children.size() == 1) {
            return root.children.get(0);
        }
        TreeNode synthetic = new TreeNode("Root", null, 0);
        synthetic.children.addAll(root.children);
        return synthetic;
    }

    private String renderBar(List<Row> rows) {
        int count = rows.size();
        int width = Math.max(380, count * 70 + 60);
        int height = 260;
        int margin = 36;

        double maxVal = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        StringBuilder svg = new StringBuilder();
        appendAxes(svg, width, height, margin, maxVal);

        double barSlotWidth = (double) (width - margin - 10) / count;
        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;
            double barHeight = (height - margin - 20) * (val / maxVal);
            double barX = margin + i * barSlotWidth + barSlotWidth * 0.15;
            double barY = height - margin - barHeight;
            double barW = barSlotWidth * 0.7;
            String color = PALETTE[i % PALETTE.length];

            svg.append(String.format(Locale.US,
                    "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"4\" fill=\"%s\"/>",
                    barX, barY, barW, barHeight, color));
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-size=\"11\" text-anchor=\"middle\" fill=\"#333\">%s</text>",
                    barX + barW * 0.5, barY - 4, formatNum(val)));
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%d\" font-size=\"11\" text-anchor=\"middle\" fill=\"#444\">%s</text>",
                    barX + barW * 0.5, height - margin + 16, escapeXml(truncate(r.name(), 12))));
        }

        return wrapSvg(width, height, svg.toString());
    }

    private String renderLine(List<Row> rows) {
        int count = rows.size();
        int width = Math.max(380, count * 70 + 60);
        int height = 260;
        int margin = 36;

        double maxVal = rows.stream().mapToDouble(r -> r.value() != null ? r.value() : 1.0).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        StringBuilder svg = new StringBuilder();
        appendAxes(svg, width, height, margin, maxVal);

        double step = (double) (width - margin - 30) / Math.max(count - 1, 1);
        StringBuilder path = new StringBuilder();
        StringBuilder dots = new StringBuilder();

        for (int i = 0; i < count; i++) {
            Row r = rows.get(i);
            double val = r.value() != null ? r.value() : 1.0;
            double x = margin + 15 + i * step;
            double y = height - margin - (height - margin - 20) * (val / maxVal);

            path.append(i == 0 ? "M" : "L").append(String.format(Locale.US, "%.1f %.1f ", x, y));
            dots.append(String.format(Locale.US,
                    "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"4\" fill=\"#2563eb\"/>", x, y));
            dots.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-size=\"11\" text-anchor=\"middle\" fill=\"#2563eb\">%s</text>",
                    x, y - 9, formatNum(val)));
            dots.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%d\" font-size=\"11\" text-anchor=\"middle\" fill=\"#444\">%s</text>",
                    x, height - margin + 16, escapeXml(truncate(r.name(), 10))));
        }

        svg.append(String.format("<path d=\"%s\" fill=\"none\" stroke=\"#2563eb\" stroke-width=\"2.5\"/>", path.toString().trim()));
        svg.append(dots);

        return wrapSvg(width, height, svg.toString());
    }

    private String renderPie(List<Row> rows) {
        double total = rows.stream().mapToDouble(r -> Math.max(r.value() != null ? r.value() : 1.0, 0.0)).sum();
        if (total <= 0) total = 1.0;

        int radius = 95;
        int center = 110;
        int height = Math.max(220, 40 + rows.size() * 24);
        int width = 430;

        StringBuilder segments = new StringBuilder();
        StringBuilder legend = new StringBuilder();

        double angle = -Math.PI / 2.0;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            double val = Math.max(r.value() != null ? r.value() : 1.0, 0.0);
            double fraction = val / total;
            double nextAngle = angle + fraction * 2 * Math.PI;
            String color = PALETTE[i % PALETTE.length];

            if (fraction >= 0.999) {
                segments.append(String.format(Locale.US,
                        "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"%s\"/>",
                        center, center, radius, color));
            } else if (fraction > 0) {
                double x1 = center + radius * Math.cos(angle);
                double y1 = center + radius * Math.sin(angle);
                double x2 = center + radius * Math.cos(nextAngle);
                double y2 = center + radius * Math.sin(nextAngle);
                int largeArc = fraction > 0.5 ? 1 : 0;

                segments.append(String.format(Locale.US,
                        "<path d=\"M%d %dL%.2f %.2fA%d %d 0 %d 1 %.2f %.2fZ\" fill=\"%s\" stroke=\"#fff\" stroke-width=\"2\"/>",
                        center, center, x1, y1, radius, radius, largeArc, x2, y2, color));
            }

            int legendY = 30 + i * 24;
            legend.append(String.format(Locale.US,
                    "<rect x=\"240\" y=\"%d\" width=\"12\" height=\"12\" fill=\"%s\" rx=\"2\"/>",
                    legendY, color));
            legend.append(String.format(Locale.US,
                    "<text x=\"260\" y=\"%d\" font-size=\"12\" fill=\"#333\">%s (%.0f%%)</text>",
                    legendY + 10, escapeXml(truncate(r.name(), 16)), fraction * 100));

            angle = nextAngle;
        }

        return wrapSvg(width, height, segments.toString() + legend.toString());
    }

    private String renderTreemap(TreeNode root) {
        int width = 500;
        int height = 260;
        List<TreeNode> leaves = collectLeaves(root);
        double total = root.totalSum();
        if (total <= 0) total = 1.0;

        StringBuilder svg = new StringBuilder();
        double curX = 10;
        for (int i = 0; i < leaves.size(); i++) {
            TreeNode leaf = leaves.get(i);
            double fraction = leaf.totalSum() / total;
            double tileWidth = Math.max(30, (width - 20) * fraction);
            String color = PALETTE[i % PALETTE.length];

            svg.append(String.format(Locale.US,
                    "<rect x=\"%.1f\" y=\"10\" width=\"%.1f\" height=\"%d\" fill=\"%s\" rx=\"4\" stroke=\"#fff\" stroke-width=\"2\"/>",
                    curX, tileWidth - 2, height - 20, color));
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"32\" font-size=\"11\" fill=\"#fff\" font-weight=\"bold\">%s</text>",
                    curX + 6, escapeXml(truncate(leaf.name, 12))));
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"48\" font-size=\"10\" fill=\"#fff\">%s</text>",
                    curX + 6, formatNum(leaf.totalSum())));

            curX += tileWidth;
        }

        return wrapSvg(width, height, svg.toString());
    }

    private String renderDiagram(TreeNode root) {
        int width = 560;
        int height = 280;
        StringBuilder svg = new StringBuilder();

        layoutDiagramTree(root, 40, height / 2.0, 120, height / 2.5);
        drawDiagramNode(root, svg);

        return wrapSvg(width, height, svg.toString());
    }

    private void layoutDiagramTree(TreeNode node, double x, double y, double xStep, double ySpread) {
        node.x = x;
        node.depthPos = y;
        if (node.children.isEmpty()) return;

        double startY = y - (ySpread * (node.children.size() - 1)) / 2.0;
        for (int i = 0; i < node.children.size(); i++) {
            TreeNode child = node.children.get(i);
            layoutDiagramTree(child, x + xStep, startY + i * ySpread, xStep, ySpread * 0.55);
        }
    }

    private void drawDiagramNode(TreeNode node, StringBuilder svg) {
        for (TreeNode child : node.children) {
            svg.append(String.format(Locale.US,
                    "<path d=\"M%.1f %.1f C %.1f %.1f, %.1f %.1f, %.1f %.1f\" stroke=\"#cbd5e1\" stroke-width=\"1.5\" fill=\"none\"/>",
                    node.x + 40, node.depthPos, node.x + 80, node.depthPos, child.x - 40, child.depthPos, child.x, child.depthPos));
            drawDiagramNode(child, svg);
        }

        svg.append(String.format(Locale.US,
                "<rect x=\"%.1f\" y=\"%.1f\" width=\"80\" height=\"28\" rx=\"6\" fill=\"#f8fafc\" stroke=\"#2563eb\" stroke-width=\"1.5\"/>",
                node.x, node.depthPos - 14));
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-size=\"11\" text-anchor=\"middle\" fill=\"#1e293b\">%s</text>",
                node.x + 40, node.depthPos + 4, escapeXml(truncate(node.name, 11))));
    }

    private String renderArea(List<Row> rows) {
        return renderLine(rows);
    }

    private String renderDonut(List<Row> rows) {
        return renderPie(rows);
    }

    private String renderRadar(List<Row> rows) {
        return renderLine(rows);
    }

    private String renderScatter(List<Row> rows) {
        return renderLine(rows);
    }

    private List<TreeNode> collectLeaves(TreeNode node) {
        List<TreeNode> list = new ArrayList<>();
        if (node.children.isEmpty()) {
            list.add(node);
        } else {
            for (TreeNode c : node.children) list.addAll(collectLeaves(c));
        }
        return list;
    }

    private void appendAxes(StringBuilder svg, int w, int h, int m, double maxVal) {
        svg.append(String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e2e8f0\" stroke-width=\"1\"/>", m, h - m, w - 10, h - m));
        for (int i = 1; i <= 3; i++) {
            int y = (h - m) - (int) ((double) i / 4.0 * (h - m - 20));
            svg.append(String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#f1f5f9\" stroke-width=\"1\"/>", m, y, w - 10, y));
            svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#94a3b8\" text-anchor=\"end\">%s</text>",
                    m - 4, y + 4, formatNum(maxVal * i / 4.0)));
        }
    }

    private String wrapSvg(int w, int h, String content) {
        return String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" width=\"100%%\" style=\"max-width:%dpx;height:auto;background:#fff;border-radius:8px;\">%s</svg>",
                w, h, w, content);
    }

    private String formatNum(double d) {
        if (d == (long) d) return String.format(Locale.US, "%d", (long) d);
        return String.format(Locale.US, "%.1f", d);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // =========================================================================
    // Thumbnail helper (used by ChartCatalog)
    // =========================================================================

    /**
     * Renders a small preview thumbnail. The {@code paletteHex} arg overrides the theme's own
     * series colours if non-null (comma-separated hex values like "#ff0000,#00ff00").
     */
    public BufferedImage renderThumbnail(ChartType type, String rawData, ChartTheme theme,
                                         String paletteHex, String cardStyle, String detail,
                                         int w, int h) {
        ChartTheme effectiveTheme = theme == null ? ChartTheme.CORPORATE_BLUE : theme;
        // If a custom palette is provided wrap it so ChartEngineService picks it up via renderChartToImage.
        // The simplest way: renderChartToImage already honours the theme; for custom palette we apply
        // post-parse colour substitution in the per-series loops – here we just pass through.
        return renderChartToImage(type, rawData, effectiveTheme, null, w, h, cardStyle, detail, false, false, paletteHex);
    }

    /**
     * Converts a {@code Color[]} to a comma-separated hex string for storage/transport.
     * Example: "#2563eb,#e11d48,#16a34a"
     */
    public static String paletteToHex(Color[] palette) {
        if (palette == null || palette.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (Color c : palette) {
            if (sb.length() > 0) sb.append(',');
            sb.append(String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
        }
        return sb.toString();
    }

    /**
     * Parses a comma-separated hex palette string back to a {@code Color[]}.
     * Returns {@code null} if the input is blank or unparseable.
     */
    public static Color[] parsePalette(String paletteHex) {
        if (paletteHex == null || paletteHex.isBlank()) return null;
        String[] parts = paletteHex.split(",");
        List<Color> list = new ArrayList<>();
        for (String p : parts) {
            String hex = p.trim();
            if (hex.startsWith("#") && hex.length() == 7) {
                try {
                    list.add(new Color(
                            Integer.parseInt(hex.substring(1, 3), 16),
                            Integer.parseInt(hex.substring(3, 5), 16),
                            Integer.parseInt(hex.substring(5, 7), 16)
                    ));
                } catch (NumberFormatException ignored) {}
            }
        }
        return list.isEmpty() ? null : list.toArray(new Color[0]);
    }
}
