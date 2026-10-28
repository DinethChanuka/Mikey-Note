package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

/**
 * A small visual "card" button: a preview picture with a caption underneath.
 * Shows a clear hover state and a strong selected state (accent ring + check badge)
 * so the active design is obvious at a glance.
 */
public class ThumbCard extends JComponent {

    /** Draws the preview inside the given rectangle. */
    public interface ThumbPainter {
        void paint(Graphics2D g2, Rectangle area);
    }

    private static final Color ACCENT = new Color(14, 165, 233);

    private final String label;
    private ThumbPainter painter;
    private final int thumbH;
    private boolean selected;
    private boolean hover;
    private Runnable onClick;

    public ThumbCard(String label, String tooltip, ThumbPainter painter, int width, int thumbHeight) {
        this.label = label;
        this.painter = painter;
        this.thumbH = thumbHeight;
        setToolTipText(tooltip);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setFocusable(true);
        int labelH = (label == null || label.isEmpty()) ? 0 : 15;
        setPreferredSize(new Dimension(width, thumbHeight + labelH + 8));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint()) && onClick != null) onClick.run();
            }
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if ((e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER || e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) && onClick != null) onClick.run();
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
        });
    }

    public String getLabel() {
        return label;
    }

    public static ThumbCard ofImage(String label, String tooltip, BufferedImage img, int width, int thumbHeight) {
        return new ThumbCard(label, tooltip, (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(img, r.x, r.y, r.width, r.height, null);
        }, width, thumbHeight);
    }

    private String styleCode;
    private ChartType chartType = ChartType.COLUMN;
    private String themeName;
    private String headerStyle;
    private String footerStyle;
    private String textStyle;
    private String templateCode;

    public String getStyleCode() { return styleCode; }
    public void setStyleCode(String code) { this.styleCode = code; }

    public ChartType getChartType() { return chartType; }
    public void setChartType(ChartType chartType) { this.chartType = chartType; }

    public String getThemeName() { return themeName; }
    public void setThemeName(String themeName) { this.themeName = themeName; }

    public String getHeaderStyle() { return headerStyle; }
    public void setHeaderStyle(String headerStyle) { this.headerStyle = headerStyle; }

    public String getFooterStyle() { return footerStyle; }
    public void setFooterStyle(String footerStyle) { this.footerStyle = footerStyle; }

    public String getTextStyle() { return textStyle; }
    public void setTextStyle(String textStyle) { this.textStyle = textStyle; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public void setPainter(ThumbPainter painter) {
        this.painter = painter;
        repaint();
    }

    public void setDesignType(ChartType type) {
        this.chartType = type != null ? type : ChartType.COLUMN;
        if (this.styleCode != null) {
            this.painter = createDesignPainter(this.chartType, this.styleCode);
            repaint();
        }
    }

    public static ThumbCard ofDesign(ChartType type, String styleCode, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createDesignPainter(type, styleCode), width, thumbHeight);
        card.setStyleCode(styleCode);
        card.setChartType(type);
        return card;
    }

    public static ThumbCard ofDesign(String styleCode, String label, String tooltip, int width, int thumbHeight) {
        return ofDesign(ChartType.COLUMN, styleCode, label, tooltip, width, thumbHeight);
    }

    public static ThumbCard ofType(ChartType type, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createTypePainter(type), width, thumbHeight);
        card.setChartType(type);
        return card;
    }

    public static ThumbCard ofTheme(ChartTheme theme, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createThemePainter(theme), width, thumbHeight);
        card.setThemeName(theme != null ? theme.getDisplayName() : null);
        return card;
    }

    public static ThumbCard ofPaletteStudio(String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createPaletteStudioPainter(), width, thumbHeight);
        card.setThemeName("CUSTOM");
        return card;
    }

    public static ThumbCard ofHeaderDesign(String headerStyle, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createHeaderPainter(headerStyle), width, thumbHeight);
        card.setHeaderStyle(headerStyle);
        return card;
    }

    public static ThumbCard ofFooterDesign(String footerStyle, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createFooterPainter(footerStyle), width, thumbHeight);
        card.setFooterStyle(footerStyle);
        return card;
    }

    public static ThumbCard ofTextStyle(String textStyle, String label, String sampleText, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createTextStylePainter(textStyle, sampleText), width, thumbHeight);
        card.setTextStyle(textStyle);
        return card;
    }

    public static ThumbCard ofCardTemplate(String templateCode, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createCardTemplatePainter(templateCode), width, thumbHeight);
        card.setTemplateCode(templateCode);
        return card;
    }

    public static ThumbCard ofShape(String shapeCode, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createShapePainter(shapeCode), width, thumbHeight);
        card.setTemplateCode(shapeCode);
        return card;
    }

    public static ThumbCard ofMedia(String mediaCode, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createMediaPainter(mediaCode), width, thumbHeight);
        card.setTemplateCode(mediaCode);
        return card;
    }

    public static ThumbCard ofReference(String refCode, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createReferencePainter(refCode), width, thumbHeight);
        card.setTemplateCode(refCode);
        return card;
    }

    public static ThumbCard ofBusinessTemplate(String templateCode, String label, String tooltip, int width, int thumbHeight) {
        ThumbCard card = new ThumbCard(label, tooltip, createBusinessTemplatePainter(templateCode), width, thumbHeight);
        card.setTemplateCode(templateCode);
        return card;
    }

    public static String getThemeShortLabel(ChartTheme theme) {
        if (theme == null) return "";
        return switch (theme) {
            case CORPORATE_BLUE -> "Corporate";
            case EMERALD_GREEN -> "Emerald";
            case CYBERPUNK_NEON -> "Cyberpunk";
            case SUNSET_CORAL -> "Sunset";
            case SLATE_DARK -> "Slate";
            case WARM_PASTEL -> "Pastel";
        };
    }

    // =========================================================================
    // PAINTERS
    // =========================================================================

    private static ThumbPainter createDesignPainter(ChartType chartType, String styleCode) {
        String code = styleCode != null ? styleCode.toUpperCase() : "MODERN_GLASS";
        ChartType t = chartType != null ? chartType : ChartType.COLUMN;
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg;
            Color border;
            Color baselineColor;
            Color[] palette;
            boolean hasSheen = false;

            switch (code) {
                case "SHARP_MINIMAL" -> {
                    bg = Color.WHITE;
                    border = new Color(100, 116, 139);
                    baselineColor = new Color(51, 65, 85);
                    palette = new Color[]{new Color(30, 41, 59), new Color(71, 85, 105), new Color(100, 116, 139), new Color(148, 163, 184)};
                }
                case "GRADIENT" -> {
                    bg = null;
                    border = new Color(129, 140, 248, 140);
                    baselineColor = new Color(255, 255, 255, 60);
                    palette = new Color[]{new Color(56, 189, 248), new Color(168, 85, 247), new Color(244, 63, 94), new Color(250, 204, 21)};
                }
                case "DARK_EXECUTIVE" -> {
                    bg = new Color(15, 23, 42);
                    border = new Color(51, 65, 85);
                    baselineColor = new Color(71, 85, 105);
                    palette = new Color[]{new Color(16, 185, 129), new Color(239, 68, 68), new Color(245, 158, 11), new Color(14, 165, 233)};
                }
                case "CARD_OUTLINE" -> {
                    bg = Color.WHITE;
                    border = new Color(37, 99, 235);
                    baselineColor = new Color(226, 232, 240);
                    palette = new Color[]{new Color(37, 99, 235), new Color(59, 130, 246), new Color(96, 165, 250), new Color(147, 197, 253)};
                }
                case "ELEVATED_SHADOW" -> {
                    bg = Color.WHITE;
                    border = new Color(203, 213, 225);
                    baselineColor = new Color(226, 232, 240);
                    palette = new Color[]{new Color(124, 58, 237), new Color(236, 72, 153), new Color(16, 185, 129), new Color(14, 165, 233)};
                }
                case "WARM_CREAM" -> {
                    bg = new Color(254, 250, 235);
                    border = new Color(217, 119, 6);
                    baselineColor = new Color(180, 83, 9);
                    palette = new Color[]{new Color(194, 65, 12), new Color(217, 119, 6), new Color(101, 163, 13), new Color(71, 85, 105)};
                }
                case "CYBERPUNK" -> {
                    bg = new Color(5, 5, 13);
                    border = new Color(0, 245, 255);
                    baselineColor = new Color(0, 245, 255, 90);
                    palette = new Color[]{new Color(168, 85, 247), new Color(0, 245, 255), new Color(244, 63, 94), new Color(250, 204, 21)};
                }
                default -> { // MODERN_GLASS
                    bg = new Color(248, 250, 252);
                    border = new Color(203, 213, 225);
                    baselineColor = new Color(226, 232, 240);
                    palette = new Color[]{new Color(37, 99, 235), new Color(2, 132, 199), new Color(16, 185, 129), new Color(245, 158, 11)};
                    hasSheen = true;
                }
            }

            // Background
            if ("GRADIENT".equals(code)) {
                GradientPaint bgGp = new GradientPaint(r.x, r.y, new Color(30, 27, 75), r.x, r.y + r.height, new Color(37, 99, 235));
                g2.setPaint(bgGp);
                g2.fillRect(r.x, r.y, r.width, r.height);
            } else {
                g2.setColor(bg);
                g2.fillRect(r.x, r.y, r.width, r.height);
            }

            if (hasSheen) {
                g2.setPaint(new GradientPaint(r.x, r.y, new Color(255, 255, 255, 200), r.x, r.y + r.height / 2, new Color(255, 255, 255, 0)));
                g2.fillRect(r.x, r.y, r.width, r.height / 2);
            }

            // Border
            g2.setColor(border);
            if ("CARD_OUTLINE".equals(code)) {
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2);
                g2.setStroke(new BasicStroke(1f));
            } else {
                g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            }

            paintTypeGeometry(g2, r, t, palette, baselineColor);
        };
    }

    private static ThumbPainter createTypePainter(ChartType type) {
        ChartType t = type != null ? type : ChartType.COLUMN;
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean dark = ThemeManager.isDark();
            g2.setColor(dark ? new Color(24, 33, 47) : new Color(248, 250, 252));
            g2.fillRect(r.x, r.y, r.width, r.height);
            paintTypeGeometry(g2, r, t, null, null);
        };
    }

    private static void paintTypeGeometry(Graphics2D g2, Rectangle r, ChartType t, Color[] customPalette, Color customBaseline) {
        boolean dark = ThemeManager.isDark();
        Color baseLine = customBaseline != null ? customBaseline : (dark ? new Color(71, 85, 105) : new Color(203, 213, 225));
        Color[] defPal = {new Color(37, 99, 235), new Color(14, 165, 233), new Color(16, 185, 129), new Color(245, 158, 11)};
        Color[] c = customPalette != null ? customPalette : defPal;

        switch (t) {
            case BAR -> {
                int axisX = r.x + 6;
                g2.setColor(baseLine);
                g2.drawLine(axisX, r.y + 4, axisX, r.y + r.height - 5);
                int[] barLens = {r.width - 16, (int)((r.width - 16) * 0.65f), (int)((r.width - 16) * 0.85f)};
                int barH = 5;
                int gap = (r.height - 10 - (3 * barH)) / 2;
                for (int i = 0; i < 3; i++) {
                    int by = r.y + 6 + i * (barH + gap);
                    g2.setColor(c[i % c.length]);
                    g2.fillRoundRect(axisX + 1, by, Math.max(4, barLens[i]), barH, 3, 3);
                }
            }
            case LINE -> {
                int baseY = r.y + r.height - 6;
                g2.setColor(baseLine);
                g2.drawLine(r.x + 4, baseY, r.x + r.width - 5, baseY);
                g2.drawLine(r.x + 4, baseY - 12, r.x + r.width - 5, baseY - 12);
                int[] px = {r.x + 6, r.x + 18, r.x + 30, r.x + 42, r.x + r.width - 6};
                int[] py = {baseY - 6, baseY - 18, baseY - 10, baseY - 26, baseY - 20};
                g2.setColor(c[0]);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < px.length - 1; i++) g2.drawLine(px[i], py[i], px[i+1], py[i+1]);
                g2.setStroke(new BasicStroke(1.0f));
                for (int i = 0; i < px.length; i++) {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(px[i] - 3, py[i] - 3, 6, 6);
                    g2.setColor(c[0]);
                    g2.drawOval(px[i] - 3, py[i] - 3, 6, 6);
                }
            }
            case AREA -> {
                int baseY = r.y + r.height - 5;
                g2.setColor(baseLine);
                g2.drawLine(r.x + 4, baseY, r.x + r.width - 5, baseY);
                int[] ax = {r.x + 5, r.x + 18, r.x + 32, r.x + 44, r.x + r.width - 6, r.x + r.width - 6, r.x + 5};
                int[] ay = {baseY - 5, baseY - 20, baseY - 12, baseY - 26, baseY - 18, baseY, baseY};
                Color mainC = c[0];
                GradientPaint areaGp = new GradientPaint(r.x, r.y + 4, new Color(mainC.getRed(), mainC.getGreen(), mainC.getBlue(), 180),
                        r.x, baseY, new Color(mainC.getRed(), mainC.getGreen(), mainC.getBlue(), 30));
                g2.setPaint(areaGp);
                g2.fillPolygon(ax, ay, ax.length);
                g2.setColor(mainC);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 4; i++) g2.drawLine(ax[i], ay[i], ax[i+1], ay[i+1]);
                g2.setStroke(new BasicStroke(1.0f));
            }
            case PIE -> {
                int d = Math.min(r.width, r.height) - 10;
                int cx = r.x + (r.width - d) / 2;
                int cy = r.y + (r.height - d) / 2;
                g2.setColor(c[0]);
                g2.fillArc(cx, cy, d, d, 0, 110);
                g2.setColor(c[1 % c.length]);
                g2.fillArc(cx, cy, d, d, 110, 80);
                g2.setColor(c[2 % c.length]);
                g2.fillArc(cx, cy, d, d, 190, 100);
                g2.setColor(c[3 % c.length]);
                g2.fillArc(cx, cy, d, d, 290, 70);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx, cy, d, d);
            }
            case DONUT -> {
                int d = Math.min(r.width, r.height) - 10;
                int cx = r.x + (r.width - d) / 2;
                int cy = r.y + (r.height - d) / 2;
                g2.setColor(c[0]);
                g2.fillArc(cx, cy, d, d, 0, 140);
                g2.setColor(c[1 % c.length]);
                g2.fillArc(cx, cy, d, d, 140, 100);
                g2.setColor(c[2 % c.length]);
                g2.fillArc(cx, cy, d, d, 240, 120);
                int holeD = (int)(d * 0.52f);
                int hx = r.x + (r.width - holeD) / 2;
                int hy = r.y + (r.height - holeD) / 2;
                g2.setColor(dark ? new Color(24, 33, 47) : Color.WHITE);
                g2.fillOval(hx, hy, holeD, holeD);
                g2.setColor(c[0]);
                g2.fillOval(hx + holeD/2 - 2, hy + holeD/2 - 2, 4, 4);
            }
            case RADAR -> {
                int cx = r.x + r.width / 2;
                int cy = r.y + r.height / 2;
                int rad = Math.min(r.width, r.height) / 2 - 5;
                g2.setColor(baseLine);
                g2.drawOval(cx - rad, cy - rad, rad * 2, rad * 2);
                g2.drawOval(cx - rad/2, cy - rad/2, rad, rad);
                int[] rx = {cx, cx + (int)(rad * 0.8), cx, cx - (int)(rad * 0.7)};
                int[] ry = {cy - (int)(rad * 0.85), cy, cy + (int)(rad * 0.75), cy};
                Color mainC = c[0];
                g2.setColor(new Color(mainC.getRed(), mainC.getGreen(), mainC.getBlue(), 60));
                g2.fillPolygon(rx, ry, 4);
                g2.setColor(mainC);
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawPolygon(rx, ry, 4);
                for (int i = 0; i < 4; i++) g2.fillOval(rx[i] - 2, ry[i] - 2, 4, 4);
                g2.setStroke(new BasicStroke(1f));
            }
            case SCATTER -> {
                int baseY = r.y + r.height - 6;
                int axisX = r.x + 6;
                g2.setColor(baseLine);
                g2.drawLine(axisX, r.y + 4, axisX, baseY);
                g2.drawLine(axisX, baseY, r.x + r.width - 5, baseY);
                int[] sx = {axisX + 6, axisX + 12, axisX + 18, axisX + 24, axisX + 30, axisX + 36, axisX + 44};
                int[] sy = {baseY - 7, baseY - 14, baseY - 11, baseY - 22, baseY - 18, baseY - 26, baseY - 23};
                for (int i = 0; i < sx.length; i++) {
                    g2.setColor(c[i % c.length]);
                    g2.fillOval(sx[i] - 3, sy[i] - 3, 6, 6);
                }
            }
            case LOLLIPOP -> {
                int baseY = r.y + r.height - 6;
                g2.setColor(baseLine);
                g2.drawLine(r.x + 4, baseY, r.x + r.width - 5, baseY);
                int[] lx = {r.x + 10, r.x + 22, r.x + 36, r.x + 48};
                int[] lh = {14, 25, 11, 21};
                for (int i = 0; i < 4; i++) {
                    g2.setColor(baseLine);
                    g2.drawLine(lx[i], baseY, lx[i], baseY - lh[i]);
                    g2.setColor(c[i % c.length]);
                    g2.fillOval(lx[i] - 4, baseY - lh[i] - 4, 8, 8);
                }
            }
            case WATERFALL -> {
                int baseY = r.y + r.height - 6;
                g2.setColor(baseLine);
                g2.drawLine(r.x + 4, baseY, r.x + r.width - 5, baseY);
                int w1 = 7;
                int x1 = r.x + 7;
                g2.setColor(new Color(16, 185, 129)); // Positive
                g2.fillRect(x1, baseY - 18, w1, 18);
                int x2 = x1 + w1 + 3;
                g2.setColor(new Color(239, 68, 68)); // Negative dip
                g2.fillRect(x2, baseY - 18, w1, 8);
                int x3 = x2 + w1 + 3;
                g2.setColor(new Color(16, 185, 129)); // Positive rise
                g2.fillRect(x3, baseY - 24, w1, 14);
                int x4 = x3 + w1 + 3;
                g2.setColor(c[0]); // Net Total
                g2.fillRect(x4, baseY - 24, w1, 24);
                g2.setColor(baseLine);
                g2.drawLine(x1 + w1, baseY - 18, x2, baseY - 18);
                g2.drawLine(x2 + w1, baseY - 10, x3, baseY - 10);
            }
            case FUNNEL -> {
                int cx = r.x + r.width / 2;
                int topY = r.y + 6;
                int[] fHeights = {6, 6, 6, 6};
                int[] fWidths = {r.width - 16, (int)((r.width - 16) * 0.72), (int)((r.width - 16) * 0.48), (int)((r.width - 16) * 0.28)};
                int curY = topY;
                for (int i = 0; i < 4; i++) {
                    g2.setColor(c[i % c.length]);
                    int hw = fWidths[i] / 2;
                    g2.fillRoundRect(cx - hw, curY, fWidths[i], fHeights[i], 2, 2);
                    curY += fHeights[i] + 1;
                }
            }
            case GAUGE -> {
                int d = Math.min(r.width, r.height) + 4;
                int cx = r.x + (r.width - d) / 2;
                int cy = r.y + 7;
                g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(c[0]);
                g2.drawArc(cx, cy, d, d, 40, 100);
                g2.setColor(c[1 % c.length]);
                g2.drawArc(cx, cy, d, d, 0, 40);
                g2.setStroke(new BasicStroke(1f));
                int needleX = cx + d / 2;
                int needleY = cy + d / 2;
                g2.setColor(dark ? Color.WHITE : new Color(15, 23, 42));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(needleX, needleY, needleX + 11, needleY - 11);
                g2.fillOval(needleX - 3, needleY - 3, 6, 6);
                g2.setStroke(new BasicStroke(1f));
            }
            case POLAR -> {
                int cx = r.x + r.width / 2;
                int cy = r.y + r.height / 2;
                int[] rads = {14, 11, 16, 9, 13};
                for (int i = 0; i < 5; i++) {
                    g2.setColor(c[i % c.length]);
                    int rval = rads[i];
                    g2.fillArc(cx - rval, cy - rval, rval * 2, rval * 2, i * 72, 65);
                }
            }
            case TREE -> {
                int pad = 5;
                int tw = r.width - pad * 2;
                int th = r.height - pad * 2;
                int leftW = (int)(tw * 0.45f);
                int rightW = tw - leftW - 1;
                int topH = (int)(th * 0.55f);
                int botH = th - topH - 1;
                int subW = rightW / 2;
                g2.setColor(c[0]);
                g2.fillRoundRect(r.x + pad, r.y + pad, leftW, th, 2, 2);
                g2.setColor(c[1 % c.length]);
                g2.fillRoundRect(r.x + pad + leftW + 1, r.y + pad, rightW, topH, 2, 2);
                g2.setColor(c[2 % c.length]);
                g2.fillRoundRect(r.x + pad + leftW + 1, r.y + pad + topH + 1, subW, botH, 2, 2);
                g2.setColor(c[3 % c.length]);
                g2.fillRoundRect(r.x + pad + leftW + 1 + subW + 1, r.y + pad + topH + 1, rightW - subW - 1, botH, 2, 2);
            }
            case DIAGRAM -> {
                int rootW = 20, rootH = 8;
                int rootX = r.x + (r.width - rootW) / 2;
                int rootY = r.y + 5;
                g2.setColor(new Color(71, 85, 105));
                g2.fillRoundRect(rootX, rootY, rootW, rootH, 3, 3);
                int midY = rootY + rootH + 4;
                g2.setColor(baseLine);
                g2.drawLine(r.x + r.width / 2, rootY + rootH, r.x + r.width / 2, midY);
                int c1X = r.x + 9, c2X = r.x + r.width / 2, c3X = r.x + r.width - 9;
                g2.drawLine(c1X, midY, c3X, midY);
                g2.drawLine(c1X, midY, c1X, midY + 4);
                g2.drawLine(c2X, midY, c2X, midY + 4);
                g2.drawLine(c3X, midY, c3X, midY + 4);
                int childW = 12, childH = 7;
                g2.setColor(c[0]);
                g2.fillRoundRect(c1X - childW / 2, midY + 4, childW, childH, 2, 2);
                g2.setColor(c[1 % c.length]);
                g2.fillRoundRect(c2X - childW / 2, midY + 4, childW, childH, 2, 2);
                g2.setColor(c[2 % c.length]);
                g2.fillRoundRect(c3X - childW / 2, midY + 4, childW, childH, 2, 2);
            }
            default -> { // COLUMN
                int baseY = r.y + r.height - 6;
                g2.setColor(baseLine);
                g2.drawLine(r.x + 4, baseY, r.x + r.width - 5, baseY);
                int[] colH = {14, 25, 11, 20};
                int colW = 7;
                int colGap = (r.width - 10 - (4 * colW)) / 3;
                for (int i = 0; i < 4; i++) {
                    int bx = r.x + 5 + i * (colW + colGap);
                    g2.setColor(c[i % c.length]);
                    g2.fillRoundRect(bx, baseY - colH[i], colW, colH[i], 3, 3);
                }
            }
        }
    }

    private static ThumbPainter createThemePainter(ChartTheme theme) {
        ChartTheme th = theme != null ? theme : ChartTheme.CORPORATE_BLUE;
        Color[] pal = th.getPalette();
        Color bg = th.getBackground();
        Color border = th.getBorder();

        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 4, 4);
            g2.setColor(border);
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 4, 4);

            int swatchH = 4;
            int swatchY = r.y + r.height - swatchH - 1;
            int n = Math.min(5, pal.length);
            int swW = (r.width - 2) / n;
            for (int i = 0; i < n; i++) {
                g2.setColor(pal[i]);
                int sx = r.x + 1 + i * swW;
                int w = (i == n - 1) ? (r.x + r.width - 1 - sx) : swW;
                g2.fillRect(sx, swatchY, w, swatchH);
            }

            int baseY = swatchY - 3;
            int barW = 7;
            int gap = (r.width - 10 - (4 * barW)) / 3;
            int[] barHeights = {12, 22, 10, 18};
            for (int i = 0; i < 4; i++) {
                int bx = r.x + 5 + i * (barW + gap);
                g2.setColor(pal[i % pal.length]);
                g2.fillRoundRect(bx, baseY - barHeights[i], barW, barHeights[i], 3, 3);
            }
        };
    }

    private static ThumbPainter createPaletteStudioPainter() {
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean dark = ThemeManager.isDark();
            g2.setColor(dark ? new Color(24, 33, 47) : Color.WHITE);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 4, 4);
            g2.setColor(new Color(139, 92, 246, 120));
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 4, 4);

            int cx = r.x + r.width / 2;
            int cy = r.y + 14;
            int rad = 14;
            Color[] spectrum = {
                    new Color(239, 68, 68),
                    new Color(245, 158, 11),
                    new Color(16, 185, 129),
                    new Color(14, 165, 233),
                    new Color(139, 92, 246)
            };
            for (int i = 0; i < spectrum.length; i++) {
                double angle = Math.PI - (i * Math.PI / (spectrum.length - 1));
                int px = cx + (int)(rad * Math.cos(angle));
                int py = cy - (int)(rad * Math.sin(angle));
                g2.setColor(spectrum[i]);
                g2.fillOval(px - 3, py - 3, 6, 6);
            }

            int barY = r.y + r.height - 7;
            int barH = 5;
            for (int x = r.x + 4; x < r.x + r.width - 4; x++) {
                float hue = (float)(x - (r.x + 4)) / (r.width - 8);
                g2.setColor(Color.getHSBColor(hue, 0.9f, 0.95f));
                g2.drawLine(x, barY, x, barY + barH);
            }
        };
    }

    private static ThumbPainter createHeaderPainter(String headerStyle) {
        String code = headerStyle != null ? headerStyle.toUpperCase() : "MODERN_LINE";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 4, 4);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 4, 4);

            int hy = r.y + 4;

            switch (code) {
                case "CLEAN_MINIMAL" -> {
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillRoundRect(r.x + 6, hy + 3, 24, 4, 2, 2);
                    g2.setColor(new Color(148, 163, 184));
                    g2.fillRoundRect(r.x + r.width - 24, hy + 3, 18, 4, 2, 2);
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(r.x + 6, hy + 18, r.width - 12, 3, 2, 2);
                    g2.fillRoundRect(r.x + 6, hy + 24, r.width - 20, 3, 2, 2);
                }
                case "MODERN_LINE" -> {
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(r.x + 6, hy + 2, 26, 4, 2, 2);
                    g2.setColor(new Color(100, 116, 139));
                    g2.fillRoundRect(r.x + r.width - 22, hy + 2, 16, 4, 2, 2);
                    g2.setColor(new Color(14, 165, 233));
                    g2.fillRect(r.x + 5, hy + 8, r.width - 10, 2);
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(r.x + 6, hy + 16, r.width - 12, 3, 2, 2);
                    g2.fillRoundRect(r.x + 6, hy + 22, r.width - 24, 3, 2, 2);
                }
                case "ACCENT_BANNER" -> {
                    GradientPaint gp = new GradientPaint(r.x, hy, new Color(14, 165, 233), r.x + r.width, hy, new Color(37, 99, 235));
                    g2.setPaint(gp);
                    g2.fillRoundRect(r.x + 3, hy, r.width - 6, 12, 3, 3);
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(r.x + 7, hy + 4, 24, 4, 2, 2);
                    g2.fillRoundRect(r.x + r.width - 20, hy + 4, 12, 4, 2, 2);
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(r.x + 6, hy + 18, r.width - 12, 3, 2, 2);
                }
                case "EDITORIAL_SPLIT" -> {
                    g2.setColor(new Color(245, 158, 11));
                    g2.fillRoundRect(r.x + 5, hy + 2, 14, 5, 2, 2);
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(r.x + 24, hy + 2, 22, 4, 2, 2);
                    g2.setColor(new Color(148, 163, 184));
                    g2.fillRoundRect(r.x + r.width - 18, hy + 2, 12, 4, 2, 2);
                    g2.setColor(new Color(226, 232, 240));
                    g2.drawLine(r.x + 5, hy + 9, r.x + r.width - 5, hy + 9);
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(r.x + 6, hy + 16, r.width - 12, 3, 2, 2);
                }
                case "CORPORATE" -> {
                    g2.setColor(new Color(30, 41, 59));
                    g2.fillRoundRect(r.x + 6, hy + 2, 28, 4, 2, 2);
                    g2.setColor(new Color(225, 29, 72));
                    g2.fillRoundRect(r.x + r.width - 22, hy + 2, 16, 3, 1, 1);
                    g2.setColor(new Color(71, 85, 105));
                    g2.drawLine(r.x + 5, hy + 7, r.x + r.width - 5, hy + 7);
                    g2.drawLine(r.x + 5, hy + 9, r.x + r.width - 5, hy + 9);
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(r.x + 6, hy + 17, r.width - 12, 3, 2, 2);
                }
                default -> { // NONE
                    g2.setColor(new Color(203, 213, 225));
                    float[] dash = {3f, 3f};
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2.drawRoundRect(r.x + 4, hy + 1, r.width - 8, 12, 3, 3);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(148, 163, 184));
                    g2.drawString("No Header", r.x + 12, hy + 10);
                    g2.setStroke(new BasicStroke(1f));
                }
            }
        };
    }

    private static ThumbPainter createFooterPainter(String footerStyle) {
        String code = footerStyle != null ? footerStyle.toUpperCase() : "SPLIT_TITLE_PAGE";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 4, 4);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 4, 4);

            int fy = r.y + r.height - 15;
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(r.x + 6, r.y + 6, r.width - 12, 3, 2, 2);
            g2.fillRoundRect(r.x + 6, r.y + 12, r.width - 18, 3, 2, 2);

            switch (code) {
                case "PAGE_NUMBER_CENTER" -> {
                    g2.setColor(new Color(226, 232, 240));
                    g2.drawLine(r.x + 10, fy - 2, r.x + r.width - 10, fy - 2);
                    g2.setColor(new Color(14, 165, 233));
                    g2.fillRoundRect(r.x + r.width / 2 - 10, fy + 2, 20, 6, 3, 3);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 6));
                    g2.drawString("1 / 4", r.x + r.width / 2 - 7, fy + 7);
                }
                case "SPLIT_TITLE_PAGE" -> {
                    g2.setColor(new Color(203, 213, 225));
                    g2.drawLine(r.x + 5, fy - 2, r.x + r.width - 5, fy - 2);
                    g2.setColor(new Color(100, 116, 139));
                    g2.fillRoundRect(r.x + 6, fy + 3, 24, 4, 2, 2);
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillRoundRect(r.x + r.width - 18, fy + 2, 12, 6, 3, 3);
                }
                case "ACCENT_BAR" -> {
                    GradientPaint gp = new GradientPaint(r.x, fy + 4, new Color(37, 99, 235), r.x + r.width, fy + 4, new Color(14, 165, 233));
                    g2.setPaint(gp);
                    g2.fillRect(r.x + 2, fy + 6, r.width - 4, 4);
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(r.x + 6, fy - 1, 22, 4, 2, 2);
                    g2.setColor(new Color(14, 165, 233));
                    g2.fillRoundRect(r.x + r.width - 18, fy - 2, 12, 6, 3, 3);
                }
                case "DOCUMENT_INFO" -> {
                    g2.setColor(new Color(226, 232, 240));
                    g2.drawLine(r.x + 5, fy - 2, r.x + r.width - 5, fy - 2);
                    g2.setColor(new Color(148, 163, 184));
                    g2.fillRoundRect(r.x + 6, fy + 1, 18, 3, 1, 1);
                    g2.fillRoundRect(r.x + 6, fy + 5, 14, 3, 1, 1);
                    g2.setColor(new Color(71, 85, 105));
                    g2.fillRoundRect(r.x + r.width - 16, fy + 2, 10, 5, 2, 2);
                }
                case "MINIMAL_PAGE" -> {
                    g2.setColor(new Color(100, 116, 139));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.drawString("1", r.x + r.width - 14, fy + 8);
                }
                default -> { // NONE
                    g2.setColor(new Color(203, 213, 225));
                    float[] dash = {3f, 3f};
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2.drawRoundRect(r.x + 4, fy + 1, r.width - 8, 12, 3, 3);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(148, 163, 184));
                    g2.drawString("No Footer", r.x + 12, fy + 10);
                    g2.setStroke(new BasicStroke(1f));
                }
            }
        };
    }

    private static ThumbPainter createTextStylePainter(String textStyle, String sampleText) {
        String code = textStyle != null ? textStyle.toUpperCase().replace(" ", "_") : "NORMAL";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 6, 6);

            switch (code) {
                case "HEADING_1" -> {
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillRect(r.x + 5, r.y + 6, 3, r.height - 12);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    g2.setColor(new Color(15, 23, 42));
                    g2.drawString("H1", r.x + 12, r.y + 22);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(37, 99, 235));
                    g2.drawString("24pt Bold", r.x + 12, r.y + 33);
                }
                case "HEADING_2" -> {
                    g2.setColor(new Color(14, 165, 233));
                    g2.fillOval(r.x + 6, r.y + 12, 5, 5);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    g2.setColor(new Color(30, 41, 59));
                    g2.drawString("H2", r.x + 15, r.y + 21);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(100, 116, 139));
                    g2.drawString("19pt Bold", r.x + 15, r.y + 32);
                }
                case "HEADING_3" -> {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2.setColor(new Color(51, 65, 85));
                    g2.drawString("H3", r.x + 8, r.y + 20);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(100, 116, 139));
                    g2.drawString("16pt Semi", r.x + 8, r.y + 31);
                }
                case "TITLE" -> {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g2.setColor(new Color(15, 23, 42));
                    g2.drawString("Title", r.x + 8, r.y + 20);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(14, 165, 233));
                    g2.drawString("30pt Major", r.x + 8, r.y + 32);
                }
                case "SUBTITLE" -> {
                    g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                    g2.setColor(new Color(71, 85, 105));
                    g2.drawString("Subtitle", r.x + 8, r.y + 20);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(148, 163, 184));
                    g2.drawString("15pt Italic", r.x + 8, r.y + 31);
                }
                case "QUOTE" -> {
                    g2.setColor(new Color(124, 58, 237));
                    g2.fillRect(r.x + 6, r.y + 6, 3, r.height - 12);
                    g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                    g2.setColor(new Color(88, 28, 135));
                    g2.drawString("“Quote”", r.x + 13, r.y + 21);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(new Color(148, 163, 184));
                    g2.drawString("Callout Bar", r.x + 13, r.y + 32);
                }
                default -> { // NORMAL
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    g2.setColor(new Color(15, 23, 42));
                    g2.drawString("AaBb", r.x + 8, r.y + 19);
                    g2.setColor(new Color(203, 213, 225));
                    g2.fillRoundRect(r.x + 8, r.y + 25, r.width - 16, 3, 1, 1);
                    g2.fillRoundRect(r.x + 8, r.y + 31, r.width - 24, 3, 1, 1);
                }
            }
        };
    }

    private static ThumbPainter createCardTemplatePainter(String templateCode) {
        String code = templateCode != null ? templateCode.toUpperCase() : "KPI";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 6, 6);

            switch (code) {
                case "KPI" -> {
                    g2.setColor(new Color(16, 185, 129));
                    g2.fillRoundRect(r.x + 5, r.y + 4, 22, 6, 3, 3);
                    g2.setColor(new Color(15, 23, 42));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2.drawString("$128K", r.x + 5, r.y + 22);
                    g2.setColor(new Color(14, 165, 233));
                    g2.setStroke(new BasicStroke(1.5f));
                    int[] xs = {r.x + 6, r.x + 18, r.x + 30, r.x + 42, r.x + 56};
                    int[] ys = {r.y + 34, r.y + 28, r.y + 32, r.y + 26, r.y + 24};
                    g2.drawPolyline(xs, ys, 5);
                }
                case "MULTI_KPI", "3KPI" -> {
                    int colW = (r.width - 12) / 3;
                    g2.setColor(new Color(239, 246, 255));
                    g2.fillRoundRect(r.x + 3, r.y + 4, colW, r.height - 8, 3, 3);
                    g2.setColor(new Color(240, 253, 244));
                    g2.fillRoundRect(r.x + 5 + colW, r.y + 4, colW, r.height - 8, 3, 3);
                    g2.setColor(new Color(254, 252, 232));
                    g2.fillRoundRect(r.x + 7 + colW * 2, r.y + 4, colW, r.height - 8, 3, 3);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
                    g2.setColor(new Color(37, 99, 235));
                    g2.drawString("1.4M", r.x + 4, r.y + 22);
                    g2.setColor(new Color(22, 163, 74));
                    g2.drawString("84K", r.x + 7 + colW, r.y + 22);
                    g2.setColor(new Color(180, 83, 9));
                    g2.drawString("118%", r.x + 9 + colW * 2, r.y + 22);
                }
                case "SPOTLIGHT", "TOPIC" -> {
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillRect(r.x + 2, r.y + 4, 3, r.height - 8);
                    g2.fillRoundRect(r.x + 8, r.y + 5, 26, 5, 2, 2);
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(r.x + 8, r.y + 13, r.width - 14, 4, 1, 1);
                    g2.setColor(new Color(203, 213, 225));
                    g2.fillRoundRect(r.x + 8, r.y + 21, r.width - 18, 3, 1, 1);
                    g2.fillRoundRect(r.x + 8, r.y + 27, r.width - 24, 3, 1, 1);
                }
                case "COMPARISON" -> {
                    int halfW = (r.width - 14) / 2;
                    g2.setColor(new Color(240, 253, 244));
                    g2.fillRoundRect(r.x + 5, r.y + 5, halfW, r.height - 10, 3, 3);
                    g2.setColor(new Color(187, 247, 208));
                    g2.drawRoundRect(r.x + 5, r.y + 5, halfW, r.height - 10, 3, 3);
                    g2.setColor(new Color(22, 163, 74));
                    g2.fillOval(r.x + 8, r.y + 9, 6, 6);

                    int bx2 = r.x + 9 + halfW;
                    g2.setColor(new Color(239, 246, 255));
                    g2.fillRoundRect(bx2, r.y + 5, halfW, r.height - 10, 3, 3);
                    g2.setColor(new Color(191, 219, 254));
                    g2.drawRoundRect(bx2, r.y + 5, halfW, r.height - 10, 3, 3);
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillOval(bx2 + 3, r.y + 9, 6, 6);
                }
                case "PROGRESS" -> {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g2.setColor(new Color(15, 23, 42));
                    g2.drawString("Progress", r.x + 6, r.y + 14);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.setColor(new Color(14, 165, 233));
                    g2.drawString("74%", r.x + r.width - 24, r.y + 14);
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(r.x + 6, r.y + 22, r.width - 12, 8, 4, 4);
                    GradientPaint gp = new GradientPaint(r.x + 6, r.y + 22, new Color(14, 165, 233), r.x + 6 + (int)((r.width - 12) * 0.74), r.y + 22, new Color(37, 99, 235));
                    g2.setPaint(gp);
                    g2.fillRoundRect(r.x + 6, r.y + 22, (int)((r.width - 12) * 0.74), 8, 4, 4);
                }
                default -> { // TAKEAWAYS
                    g2.setColor(new Color(245, 158, 11));
                    g2.fillOval(r.x + 6, r.y + 6, 8, 8);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.setColor(new Color(15, 23, 42));
                    g2.drawString("Takeaways", r.x + 18, r.y + 13);
                    g2.setColor(new Color(225, 29, 72));
                    g2.fillRect(r.x + 6, r.y + 19, 4, 4);
                    g2.setColor(new Color(203, 213, 225));
                    g2.fillRoundRect(r.x + 14, r.y + 19, r.width - 22, 4, 2, 2);
                    g2.setColor(new Color(225, 29, 72));
                    g2.fillRect(r.x + 6, r.y + 27, 4, 4);
                    g2.setColor(new Color(203, 213, 225));
                    g2.fillRoundRect(r.x + 14, r.y + 27, r.width - 28, 4, 2, 2);
                }
            }
        };
    }

    private static ThumbPainter createShapePainter(String shapeCode) {
        String code = shapeCode != null ? shapeCode.toUpperCase() : "CARD_BOX";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (code) {
                case "CARD_BOX", "ROUNDED_CARD" -> {
                    g2.setColor(new Color(239, 246, 255));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 8, 8);
                    g2.setColor(new Color(59, 130, 246));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 8, 8);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.drawString("■ Card Box", r.x + 8, r.y + 16);
                    g2.setColor(new Color(147, 197, 253));
                    g2.fillRoundRect(r.x + 8, r.y + 22, r.width - 16, 3, 1, 1);
                    g2.fillRoundRect(r.x + 8, r.y + 28, r.width - 24, 3, 1, 1);
                }
                case "PROCESS_FLOW" -> {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillRoundRect(r.x + 4, r.y + 11, 16, 16, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.drawString("1", r.x + 9, r.y + 23);

                    g2.setColor(new Color(100, 116, 139));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.drawString("➔", r.x + 22, r.y + 23);

                    g2.setColor(new Color(16, 185, 129));
                    g2.fillRoundRect(r.x + 31, r.y + 11, 16, 16, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.drawString("2", r.x + 36, r.y + 23);

                    g2.setColor(new Color(100, 116, 139));
                    g2.drawString("➔", r.x + 49, r.y + 23);

                    g2.setColor(new Color(124, 58, 237));
                    g2.fillRoundRect(r.x + 58, r.y + 11, 16, 16, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.drawString("3", r.x + 63, r.y + 23);
                }
                case "DECISION_NODE", "DECISION" -> {
                    g2.setColor(new Color(254, 252, 232));
                    g2.fillRoundRect(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 16, 16);
                    g2.setColor(new Color(245, 158, 11));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 16, 16);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.drawString("◆ Decision", r.x + 10, r.y + 22);
                }
                case "TERMINAL_PILL", "TERMINAL" -> {
                    g2.setColor(new Color(240, 253, 244));
                    g2.fillRoundRect(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 16, 16);
                    g2.setColor(new Color(16, 185, 129));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 16, 16);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.drawString("● Start / End", r.x + 8, r.y + 22);
                }
                case "MILESTONE_BADGE", "BADGE" -> {
                    g2.setColor(new Color(240, 253, 244));
                    g2.fillRoundRect(r.x + 4, r.y + 6, r.width - 8, r.height - 12, 4, 4);
                    g2.setColor(new Color(22, 163, 74));
                    g2.fillRect(r.x + 4, r.y + 6, 3, r.height - 12);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.drawString("✓ Milestone", r.x + 9, r.y + 22);
                }
                default -> { // STAR_RATING
                    g2.setColor(new Color(254, 252, 232));
                    g2.fillRoundRect(r.x + 4, r.y + 6, r.width - 8, r.height - 12, 6, 6);
                    g2.setColor(new Color(251, 191, 36));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(r.x + 4, r.y + 6, r.width - 8, r.height - 12, 6, 6);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    g2.drawString("★★★★★", r.x + 8, r.y + 22);
                }
            }
        };
    }

    private static ThumbPainter createMediaPainter(String mediaCode) {
        String code = mediaCode != null ? mediaCode.toUpperCase() : "LOCAL_PHOTO";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (code) {
                case "LOCAL_PHOTO" -> {
                    g2.setColor(new Color(224, 242, 254));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(14, 165, 233));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(245, 158, 11));
                    g2.fillOval(r.x + 10, r.y + 8, 7, 7);
                    int[] xs = {r.x + 6, r.x + 22, r.x + 34, r.x + 48, r.x + 58};
                    int[] ys = {r.y + r.height - 8, r.y + 17, r.y + 23, r.y + 13, r.y + r.height - 8};
                    g2.setColor(new Color(13, 148, 136));
                    g2.fillPolygon(xs, ys, 5);
                }
                case "WEB_IMAGE" -> {
                    g2.setColor(new Color(248, 250, 252));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(203, 213, 225));
                    g2.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(226, 232, 240));
                    g2.fillRect(r.x + 4, r.y + 4, r.width - 8, 7);
                    g2.setColor(new Color(37, 99, 235));
                    g2.drawOval(r.x + (r.width - 16) / 2, r.y + 14, 16, 16);
                    g2.drawLine(r.x + (r.width - 16) / 2, r.y + 22, r.x + (r.width - 16) / 2 + 16, r.y + 22);
                }
                case "CHART_STUDIO" -> {
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(14, 165, 233));
                    g2.fillRoundRect(r.x + 8, r.y + 19, 8, 13, 2, 2);
                    g2.setColor(new Color(59, 130, 246));
                    g2.fillRoundRect(r.x + 19, r.y + 13, 8, 19, 2, 2);
                    g2.setColor(new Color(16, 185, 129));
                    g2.fillRoundRect(r.x + 30, r.y + 8, 8, 24, 2, 2);
                    g2.setColor(new Color(245, 158, 11));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(r.x + 10, r.y + 16, r.x + 48, r.y + 7);
                }
                case "VIDEO_MEDIA" -> {
                    g2.setColor(new Color(30, 41, 59));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(225, 29, 72));
                    g2.fillRoundRect(r.x + (r.width - 24) / 2, r.y + (r.height - 16) / 2, 24, 16, 4, 4);
                    g2.setColor(Color.WHITE);
                    int[] tx = {r.x + (r.width - 24) / 2 + 8, r.x + (r.width - 24) / 2 + 8, r.x + (r.width - 24) / 2 + 17};
                    int[] ty = {r.y + (r.height - 16) / 2 + 4, r.y + (r.height - 16) / 2 + 12, r.y + (r.height - 16) / 2 + 8};
                    g2.fillPolygon(tx, ty, 3);
                }
                case "WEB_SCRAPER" -> {
                    g2.setColor(new Color(240, 253, 250));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(13, 148, 136));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g2.drawString("</> Live", r.x + 8, r.y + 22);
                }
                default -> { // LAYOUT_STUDIO
                    g2.setColor(new Color(245, 243, 255));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(124, 58, 237));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(r.x + 10, r.y + 8, r.width - 20, r.height - 16, 4, 4);
                    g2.fillOval(r.x + 8, r.y + 6, 4, 4);
                    g2.fillOval(r.x + r.width - 12, r.y + 6, 4, 4);
                    g2.fillOval(r.x + 8, r.y + r.height - 10, 4, 4);
                    g2.fillOval(r.x + r.width - 12, r.y + r.height - 10, 4, 4);
                }
            }
        };
    }

    private static ThumbPainter createReferencePainter(String refCode) {
        String code = refCode != null ? refCode.toUpperCase() : "CITATIONS";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (code) {
                case "CITATIONS" -> {
                    g2.setColor(new Color(239, 246, 255));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(37, 99, 235));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setFont(new Font("Georgia", Font.BOLD, 15));
                    g2.drawString("“ ”", r.x + 10, r.y + 24);
                }
                case "DATE_STAMP" -> {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(r.x + 8, r.y + 4, r.width - 16, r.height - 8, 6, 6);
                    g2.setColor(new Color(225, 29, 72));
                    g2.fillRoundRect(r.x + 8, r.y + 4, r.width - 16, 8, 4, 4);
                    g2.setColor(new Color(203, 213, 225));
                    g2.drawRoundRect(r.x + 8, r.y + 4, r.width - 16, r.height - 8, 6, 6);
                    g2.setColor(new Color(15, 23, 42));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.drawString("28", r.x + (r.width - 14) / 2, r.y + 24);
                }
                case "MATH_SYMBOLS" -> {
                    g2.setColor(new Color(248, 250, 252));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(100, 116, 139));
                    g2.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g2.setColor(new Color(15, 23, 42));
                    g2.drawString("∑ √ π ∞", r.x + 6, r.y + 22);
                }
                default -> { // ICON_SYMBOLS
                    g2.setColor(new Color(254, 252, 232));
                    g2.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setColor(new Color(251, 191, 36));
                    g2.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 6, 6);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.setColor(new Color(22, 163, 74));
                    g2.drawString("✓", r.x + 6, r.y + 22);
                    g2.setColor(new Color(245, 158, 11));
                    g2.drawString("★", r.x + 18, r.y + 22);
                    g2.setColor(new Color(37, 99, 235));
                    g2.drawString("➔", r.x + 30, r.y + 22);
                    g2.setColor(new Color(225, 29, 72));
                    g2.drawString("±", r.x + 42, r.y + 22);
                }
            }
        };
    }

    private static ThumbPainter createBusinessTemplatePainter(String templateCode) {
        String code = templateCode != null ? templateCode.toUpperCase() : "SALES";
        return (g2, r) -> {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 6, 6);

            switch (code) {
                case "SALES" -> {
                    Color[] c = {new Color(37, 99, 235), new Color(14, 165, 233), new Color(16, 185, 129), new Color(139, 92, 246)};
                    int[] h = {12, 19, 24, 30};
                    int baseY = r.y + r.height - 5;
                    g2.setColor(new Color(226, 232, 240));
                    g2.drawLine(r.x + 4, baseY, r.x + r.width - 5, baseY);
                    for (int i = 0; i < 4; i++) {
                        g2.setColor(c[i]);
                        g2.fillRoundRect(r.x + 6 + i * 14, baseY - h[i], 10, h[i], 3, 3);
                    }
                }
                case "VELOCITY" -> {
                    int baseY = r.y + r.height - 5;
                    g2.setColor(new Color(226, 232, 240));
                    g2.drawLine(r.x + 4, baseY, r.x + r.width - 5, baseY);
                    int[] xs = {r.x + 8, r.x + 20, r.x + 32, r.x + 44, r.x + 56};
                    int[] ys = {baseY - 8, baseY - 14, baseY - 12, baseY - 22, baseY - 28};
                    g2.setColor(new Color(16, 185, 129));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawPolyline(xs, ys, 5);
                    for (int i = 0; i < 5; i++) {
                        g2.setColor(Color.WHITE);
                        g2.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
                        g2.setColor(new Color(5, 150, 105));
                        g2.drawOval(xs[i] - 3, ys[i] - 3, 6, 6);
                    }
                }
                case "MARKET_SHARE" -> {
                    int d = 28;
                    int cx = r.x + (r.width - d) / 2;
                    int cy = r.y + (r.height - d) / 2;
                    g2.setColor(new Color(234, 88, 12));
                    g2.fillArc(cx, cy, d, d, 0, 160);
                    g2.setColor(new Color(244, 63, 94));
                    g2.fillArc(cx, cy, d, d, 160, 110);
                    g2.setColor(new Color(251, 191, 36));
                    g2.fillArc(cx, cy, d, d, 270, 90);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(cx, cy, d, d);
                }
                default -> { // Org Diagram
                    int topX = r.x + (r.width - 20) / 2;
                    int topY = r.y + 5;
                    g2.setColor(new Color(30, 41, 59));
                    g2.fillRoundRect(topX, topY, 20, 9, 2, 2);

                    int midY = topY + 14;
                    g2.setColor(new Color(148, 163, 184));
                    g2.drawLine(topX + 10, topY + 9, topX + 10, midY);
                    g2.drawLine(r.x + 10, midY, r.x + r.width - 10, midY);

                    g2.drawLine(r.x + 10, midY, r.x + 10, midY + 4);
                    g2.drawLine(topX + 10, midY, topX + 10, midY + 4);
                    g2.drawLine(r.x + r.width - 10, midY, r.x + r.width - 10, midY + 4);

                    Color[] cc = {new Color(37, 99, 235), new Color(16, 185, 129), new Color(245, 158, 11)};
                    int[] cxs = {r.x + 3, topX, r.x + r.width - 23};
                    for (int i = 0; i < 3; i++) {
                        g2.setColor(cc[i]);
                        g2.fillRoundRect(cxs[i], midY + 4, 16, 8, 2, 2);
                    }
                }
            }
        };
    }

    public void setOnClick(Runnable r) { this.onClick = r; }
    public void setSelected(boolean s) { this.selected = s; repaint(); }
    public boolean isSelectedCard() { return selected; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean dark = ThemeManager.isDark();
        int w = getWidth(), h = getHeight();

        // card plate
        Color plate = selected ? (dark ? new Color(12, 74, 110) : new Color(224, 242, 254))
                : hover ? (dark ? new Color(51, 65, 85) : new Color(241, 245, 249))
                : (dark ? new Color(30, 41, 59) : Color.WHITE);
        g2.setColor(plate);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

        Rectangle area = new Rectangle(4, 4, w - 8, thumbH);
        Shape oldClip = g2.getClip();
        g2.clip(new java.awt.geom.RoundRectangle2D.Double(area.x, area.y, area.width, area.height, 7, 7));
        painter.paint(g2, area);
        g2.setClip(oldClip);

        if (label != null && !label.isEmpty()) {
            g2.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 10));
            g2.setColor(selected ? (dark ? Color.WHITE : new Color(3, 105, 161)) : ThemeManager.getTextPrimary());
            FontMetrics fm = g2.getFontMetrics();
            String s = label;
            while (s.length() > 3 && fm.stringWidth(s) > w - 6) s = s.substring(0, s.length() - 2) + "…";
            g2.drawString(s, (w - fm.stringWidth(s)) / 2, 4 + thumbH + 12);
        }

        // border: selected > focus > hover > idle
        if (selected) {
            g2.setColor(ACCENT);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
            g2.setColor(ACCENT);
            g2.fillOval(w - 17, 3, 14, 14);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(w - 14, 10, w - 11, 13);
            g2.drawLine(w - 11, 13, w - 7, 7);
        } else if (isFocusOwner()) {
            g2.setColor(new Color(14, 165, 233, 160));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 2f}, 0f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
        } else {
            g2.setColor(hover ? new Color(56, 189, 248) : (dark ? new Color(71, 85, 105) : new Color(203, 213, 225)));
            g2.setStroke(new BasicStroke(hover ? 1.6f : 1f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
        }
        g2.dispose();
    }
}
