package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Quality vector icon renderer replacing all emojis with crisp, anti-aliased SVG-like vector icons.
 */
public class VectorIcons {

    public enum Type {
        UNDO, REDO,
        BOLD, ITALIC, UNDERLINE, STRIKETHROUGH,
        ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT, ALIGN_JUSTIFY,
        LIST_BULLET, LIST_NUMBERED, CHECKBOX, CHECKBOX_CHECKED,
        TABLE, CHART, IMAGE, LINK, QUOTE, CODE, DATE, PAGE_BREAK, LINE,
        PLUS, DELETE, EDIT, SEARCH, SETTINGS, PALETTE, FOLDER, BACK,
        BOOK, DOCUMENT, CHECKMARK, REFRESH, EXPORT, IMPORT,
        ZOOM_IN, ZOOM_OUT, THEME_LIGHT, THEME_DARK, CLOSE, CHEVRON_DOWN,
        ROW_ABOVE, ROW_BELOW, COL_LEFT, COL_RIGHT, DELETE_ROW, DELETE_COL, DELETE_TABLE,
        SHAPE, VIDEO,
        PRINT, CLEAR_FORMAT,
        SUBSCRIPT, SUPERSCRIPT, FONT_SIZE_UP, FONT_SIZE_DOWN,
        INDENT_INC, INDENT_DEC, ALERT_INFO, ALERT_TIP, ALERT_WARN,
        // AI & Research Vector Icons
        CLAUDE, CHATGPT, GEMINI, DEEPSEEK, PERPLEXITY, SCRAPER, WEB,
        // Word Office Editing & Layout Icons
        PASTE, CUT, COPY, FORMAT_PAINTER,
        FONT_COLOR, HIGHLIGHT,
        FIND, REPLACE, WORD_COUNT,
        EQUATION, ORIENTATION, MARGINS, COLUMNS, WATERMARK,
        FULLSCREEN, SPEECH, A11Y,
        // Chart Studio & Visual Card Vector Icons
        CHART_BAR, CHART_COLUMN, CHART_LINE, CHART_AREA, CHART_PIE, CHART_DONUT, CHART_RADAR, CHART_SCATTER, CHART_TREE,
        CHART_LOLLIPOP, CHART_WATERFALL, CHART_FUNNEL, CHART_GAUGE, CHART_POLAR,
        KPI_CARD, MULTI_KPI, SPOTLIGHT, COMPARISON, PROGRESS, TAKEAWAYS,
        GRID_2X2, GRID_3X3, DIVIDER, STAR_RATING, STEP_MINUS, STEP_PLUS
    }

    // Convenience constants
    public static final Type BACK = Type.BACK;
    public static final Type BOOK = Type.BOOK;
    public static final Type DOCUMENT = Type.DOCUMENT;
    public static final Type PLUS = Type.PLUS;
    public static final Type EDIT = Type.EDIT;
    public static final Type DELETE = Type.DELETE;
    public static final Type CHECKMARK = Type.CHECKMARK;
    public static final Type REFRESH = Type.REFRESH;
    public static final Type FOLDER = Type.FOLDER;
    public static final Type EXPORT = Type.EXPORT;
    public static final Type IMPORT = Type.IMPORT;
    public static final Type TABLE = Type.TABLE;
    public static final Type CHART = Type.CHART;
    public static final Type SETTINGS = Type.SETTINGS;
    public static final Type PALETTE = Type.PALETTE;
    public static final Type ZOOM_IN = Type.ZOOM_IN;
    public static final Type ZOOM_OUT = Type.ZOOM_OUT;
    public static final Type THEME_LIGHT = Type.THEME_LIGHT;
    public static final Type THEME_DARK = Type.THEME_DARK;
    public static final Type CLOSE = Type.CLOSE;
    public static final Type PRINT = Type.PRINT;
    public static final Type IMAGE = Type.IMAGE;
    public static final Type SHAPE = Type.SHAPE;
    public static final Type VIDEO = Type.VIDEO;
    public static final Type ROW_ABOVE = Type.ROW_ABOVE;
    public static final Type ROW_BELOW = Type.ROW_BELOW;
    public static final Type COL_LEFT = Type.COL_LEFT;
    public static final Type COL_RIGHT = Type.COL_RIGHT;
    public static final Type DELETE_ROW = Type.DELETE_ROW;
    public static final Type DELETE_COL = Type.DELETE_COL;
    public static final Type DELETE_TABLE = Type.DELETE_TABLE;
    public static final Type ALIGN_LEFT = Type.ALIGN_LEFT;
    public static final Type ALIGN_CENTER = Type.ALIGN_CENTER;
    public static final Type ALIGN_RIGHT = Type.ALIGN_RIGHT;
    public static final Type ALIGN_JUSTIFY = Type.ALIGN_JUSTIFY;
    public static final Type PAGE_BREAK = Type.PAGE_BREAK;
    public static final Type CLAUDE = Type.CLAUDE;
    public static final Type CHATGPT = Type.CHATGPT;
    public static final Type GEMINI = Type.GEMINI;
    public static final Type DEEPSEEK = Type.DEEPSEEK;
    public static final Type PERPLEXITY = Type.PERPLEXITY;
    public static final Type SCRAPER = Type.SCRAPER;
    public static final Type WEB = Type.WEB;
    public static final Type PASTE = Type.PASTE;
    public static final Type CUT = Type.CUT;
    public static final Type COPY = Type.COPY;
    public static final Type FORMAT_PAINTER = Type.FORMAT_PAINTER;
    public static final Type FONT_COLOR = Type.FONT_COLOR;
    public static final Type HIGHLIGHT = Type.HIGHLIGHT;
    public static final Type FIND = Type.FIND;
    public static final Type REPLACE = Type.REPLACE;
    public static final Type WORD_COUNT = Type.WORD_COUNT;
    public static final Type EQUATION = Type.EQUATION;
    public static final Type ORIENTATION = Type.ORIENTATION;
    public static final Type MARGINS = Type.MARGINS;
    public static final Type COLUMNS = Type.COLUMNS;
    public static final Type WATERMARK = Type.WATERMARK;
    public static final Type FULLSCREEN = Type.FULLSCREEN;
    public static final Type SPEECH = Type.SPEECH;
    public static final Type A11Y = Type.A11Y;
    public static final Type CHART_BAR = Type.CHART_BAR;
    public static final Type CHART_COLUMN = Type.CHART_COLUMN;
    public static final Type CHART_LINE = Type.CHART_LINE;
    public static final Type CHART_AREA = Type.CHART_AREA;
    public static final Type CHART_PIE = Type.CHART_PIE;
    public static final Type CHART_DONUT = Type.CHART_DONUT;
    public static final Type CHART_RADAR = Type.CHART_RADAR;
    public static final Type CHART_SCATTER = Type.CHART_SCATTER;
    public static final Type CHART_TREE = Type.CHART_TREE;
    public static final Type KPI_CARD = Type.KPI_CARD;
    public static final Type MULTI_KPI = Type.MULTI_KPI;
    public static final Type SPOTLIGHT = Type.SPOTLIGHT;
    public static final Type COMPARISON = Type.COMPARISON;
    public static final Type PROGRESS = Type.PROGRESS;
    public static final Type TAKEAWAYS = Type.TAKEAWAYS;
    public static final Type GRID_2X2 = Type.GRID_2X2;
    public static final Type GRID_3X3 = Type.GRID_3X3;
    public static final Type DIVIDER = Type.DIVIDER;
    public static final Type STAR_RATING = Type.STAR_RATING;
    public static final Type STEP_MINUS = Type.STEP_MINUS;
    public static final Type STEP_PLUS = Type.STEP_PLUS;

    public static Icon create(Type type, int size, Color color) {
        return new VectorIcon(type, size, color);
    }

    public static Icon create(Type type, int size) {
        return new VectorIcon(type, size, null);
    }

    public static Icon createIcon(Type type, int size, Color color) {
        return create(type, size, color);
    }

    public static Icon createIcon(Type type, int size) {
        return create(type, size);
    }

    private static class VectorIcon implements Icon {
        private final Type type;
        private final int size;
        private final Color fixedColor;

        public VectorIcon(Type type, int size, Color fixedColor) {
            this.type = type;
            this.size = size;
            this.fixedColor = fixedColor;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color paintColor = fixedColor != null ? fixedColor : (c != null ? c.getForeground() : Color.DARK_GRAY);
            g2.setColor(paintColor);

            g2.translate(x, y);
            float scale = size / 24.0f;
            g2.scale(scale, scale);

            render(g2, paintColor);

            g2.dispose();
        }

        private void render(Graphics2D g2, Color color) {
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case UNDO -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(9, 14);
                    p.lineTo(4, 9);
                    p.lineTo(9, 4);
                    g2.draw(p);
                    g2.draw(new Arc2D.Float(4, 9, 16, 12, 0, 180, Arc2D.OPEN));
                }
                case REDO -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(15, 14);
                    p.lineTo(20, 9);
                    p.lineTo(15, 4);
                    g2.draw(p);
                    g2.draw(new Arc2D.Float(4, 9, 16, 12, 0, 180, Arc2D.OPEN));
                }
                case BOLD -> {
                    Font font = new Font("SansSerif", Font.BOLD, 18);
                    g2.setFont(font);
                    g2.drawString("B", 6, 18);
                }
                case ITALIC -> {
                    Font font = new Font("SansSerif", Font.ITALIC | Font.BOLD, 18);
                    g2.setFont(font);
                    g2.drawString("I", 8, 18);
                }
                case UNDERLINE -> {
                    Font font = new Font("SansSerif", Font.PLAIN, 16);
                    g2.setFont(font);
                    g2.drawString("U", 6, 17);
                    g2.drawLine(5, 20, 19, 20);
                }
                case STRIKETHROUGH -> {
                    Font font = new Font("SansSerif", Font.BOLD, 16);
                    g2.setFont(font);
                    g2.drawString("S", 6, 17);
                    g2.drawLine(4, 12, 20, 12);
                }
                case ALIGN_LEFT -> {
                    g2.drawLine(4, 6, 20, 6);
                    g2.drawLine(4, 11, 14, 11);
                    g2.drawLine(4, 16, 18, 16);
                    g2.drawLine(4, 21, 12, 21);
                }
                case ALIGN_CENTER -> {
                    g2.drawLine(4, 6, 20, 6);
                    g2.drawLine(7, 11, 17, 11);
                    g2.drawLine(5, 16, 19, 16);
                    g2.drawLine(8, 21, 16, 21);
                }
                case ALIGN_RIGHT -> {
                    g2.drawLine(4, 6, 20, 6);
                    g2.drawLine(10, 11, 20, 11);
                    g2.drawLine(6, 16, 20, 16);
                    g2.drawLine(12, 21, 20, 21);
                }
                case ALIGN_JUSTIFY -> {
                    g2.drawLine(4, 6, 20, 6);
                    g2.drawLine(4, 11, 20, 11);
                    g2.drawLine(4, 16, 20, 16);
                    g2.drawLine(4, 21, 20, 21);
                }
                case LIST_BULLET -> {
                    g2.fillOval(4, 5, 4, 4);
                    g2.drawLine(11, 7, 20, 7);
                    g2.fillOval(4, 11, 4, 4);
                    g2.drawLine(11, 13, 20, 13);
                    g2.fillOval(4, 17, 4, 4);
                    g2.drawLine(11, 19, 20, 19);
                }
                case LIST_NUMBERED -> {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                    g2.drawString("1.", 3, 9);
                    g2.drawLine(11, 7, 20, 7);
                    g2.drawString("2.", 3, 15);
                    g2.drawLine(11, 13, 20, 13);
                    g2.drawString("3.", 3, 21);
                    g2.drawLine(11, 19, 20, 19);
                }
                case CHECKBOX -> {
                    g2.drawRoundRect(4, 4, 16, 16, 4, 4);
                }
                case CHECKBOX_CHECKED -> {
                    g2.drawRoundRect(4, 4, 16, 16, 4, 4);
                    Path2D p = new Path2D.Float();
                    p.moveTo(8, 12);
                    p.lineTo(11, 15);
                    p.lineTo(16, 8);
                    g2.draw(p);
                }
                case TABLE -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    g2.drawLine(3, 10, 21, 10);
                    g2.drawLine(3, 15, 21, 15);
                    g2.drawLine(9, 4, 9, 20);
                    g2.drawLine(15, 4, 15, 20);
                }
                case CHART -> {
                    g2.drawLine(3, 20, 21, 20);
                    g2.fillRect(5, 12, 3, 8);
                    g2.fillRect(10, 7, 3, 13);
                    g2.fillRect(15, 4, 3, 16);
                }
                case IMAGE -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    g2.fillOval(7, 7, 3, 3);
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 18);
                    p.lineTo(9, 13);
                    p.lineTo(13, 16);
                    p.lineTo(17, 11);
                    p.lineTo(20, 15);
                    g2.draw(p);
                }
                case LINK -> {
                    g2.drawOval(6, 6, 8, 8);
                    g2.drawOval(10, 10, 8, 8);
                }
                case QUOTE -> {
                    g2.setFont(new Font("Serif", Font.BOLD, 22));
                    g2.drawString("“", 5, 18);
                    g2.drawString("”", 13, 18);
                }
                case CODE -> {
                    Path2D p1 = new Path2D.Float();
                    p1.moveTo(8, 7);
                    p1.lineTo(4, 12);
                    p1.lineTo(8, 17);
                    g2.draw(p1);

                    Path2D p2 = new Path2D.Float();
                    p2.moveTo(16, 7);
                    p2.lineTo(20, 12);
                    p2.lineTo(16, 17);
                    g2.draw(p2);

                    g2.drawLine(13, 5, 11, 19);
                }
                case DATE -> {
                    g2.drawRoundRect(3, 5, 18, 15, 3, 3);
                    g2.drawLine(3, 10, 21, 10);
                    g2.drawLine(7, 3, 7, 6);
                    g2.drawLine(17, 3, 17, 6);
                    g2.fillRect(7, 13, 2, 2);
                    g2.fillRect(11, 13, 2, 2);
                    g2.fillRect(15, 13, 2, 2);
                }
                case PAGE_BREAK -> {
                    g2.drawLine(3, 7, 21, 7);
                    g2.drawLine(3, 17, 21, 17);
                    Path2D p = new Path2D.Float();
                    p.moveTo(9, 10);
                    p.lineTo(12, 13);
                    p.lineTo(15, 10);
                    g2.draw(p);
                }
                case LINE -> {
                    g2.drawLine(3, 12, 21, 12);
                }
                case PLUS -> {
                    g2.drawLine(12, 4, 12, 20);
                    g2.drawLine(4, 12, 20, 12);
                }
                case DELETE -> {
                    g2.drawLine(5, 7, 19, 7);
                    g2.drawLine(9, 7, 9, 4);
                    g2.drawLine(9, 4, 15, 4);
                    g2.drawLine(15, 4, 15, 7);
                    g2.drawRoundRect(6, 7, 12, 14, 2, 2);
                    g2.drawLine(9, 10, 9, 18);
                    g2.drawLine(12, 10, 12, 18);
                    g2.drawLine(15, 10, 15, 18);
                }
                case EDIT -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(18, 4);
                    p.lineTo(20, 6);
                    p.lineTo(8, 18);
                    p.lineTo(4, 20);
                    p.lineTo(6, 16);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(15, 7, 17, 9);
                }
                case SEARCH -> {
                    g2.drawOval(4, 4, 11, 11);
                    g2.drawLine(13, 13, 20, 20);
                }
                case SETTINGS -> {
                    g2.drawOval(8, 8, 8, 8);
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        int x1 = (int) (12 + 7 * Math.cos(angle));
                        int y1 = (int) (12 + 7 * Math.sin(angle));
                        int x2 = (int) (12 + 10 * Math.cos(angle));
                        int y2 = (int) (12 + 10 * Math.sin(angle));
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
                case PALETTE -> {
                    g2.drawOval(4, 4, 16, 16);
                    g2.fillOval(7, 8, 2, 2);
                    g2.fillOval(12, 6, 2, 2);
                    g2.fillOval(16, 9, 2, 2);
                    g2.fillOval(15, 14, 2, 2);
                }
                case FOLDER -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(3, 6);
                    p.lineTo(9, 6);
                    p.lineTo(11, 8);
                    p.lineTo(21, 8);
                    p.lineTo(21, 19);
                    p.lineTo(3, 19);
                    p.closePath();
                    g2.draw(p);
                }
                case BACK -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(15, 5);
                    p.lineTo(8, 12);
                    p.lineTo(15, 19);
                    g2.draw(p);
                    g2.drawLine(8, 12, 20, 12);
                }
                case BOOK -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 6);
                    p.curveTo(8, 4, 5, 5, 3, 6);
                    p.lineTo(3, 19);
                    p.curveTo(5, 18, 8, 17, 12, 19);
                    p.curveTo(16, 17, 19, 18, 21, 19);
                    p.lineTo(21, 6);
                    p.curveTo(19, 5, 16, 4, 12, 6);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(12, 6, 12, 19);
                }
                case DOCUMENT -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(5, 3);
                    p.lineTo(14, 3);
                    p.lineTo(19, 8);
                    p.lineTo(19, 21);
                    p.lineTo(5, 21);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(14, 3, 14, 8);
                    g2.drawLine(14, 8, 19, 8);
                }
                case CHECKMARK -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 12);
                    p.lineTo(9, 17);
                    p.lineTo(20, 6);
                    g2.draw(p);
                }
                case REFRESH -> {
                    g2.draw(new Arc2D.Float(4, 4, 16, 16, 45, 270, Arc2D.OPEN));
                    Path2D arrow = new Path2D.Float();
                    arrow.moveTo(16, 3);
                    arrow.lineTo(20, 7);
                    arrow.lineTo(16, 11);
                    g2.draw(arrow);
                }
                case EXPORT -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 3);
                    p.lineTo(12, 15);
                    p.moveTo(7, 8);
                    p.lineTo(12, 3);
                    p.lineTo(17, 8);
                    g2.draw(p);
                    g2.drawPolyline(new int[]{4, 4, 20, 20}, new int[]{14, 20, 20, 14}, 4);
                }
                case IMPORT -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 15);
                    p.lineTo(12, 3);
                    p.moveTo(7, 10);
                    p.lineTo(12, 15);
                    p.lineTo(17, 10);
                    g2.draw(p);
                    g2.drawPolyline(new int[]{4, 4, 20, 20}, new int[]{14, 20, 20, 14}, 4);
                }
                case ZOOM_IN -> {
                    g2.drawOval(4, 4, 11, 11);
                    g2.drawLine(13, 13, 20, 20);
                    g2.drawLine(9, 7, 9, 12);
                    g2.drawLine(7, 9, 11, 9);
                }
                case ZOOM_OUT -> {
                    g2.drawOval(4, 4, 11, 11);
                    g2.drawLine(13, 13, 20, 20);
                    g2.drawLine(7, 9, 11, 9);
                }
                case THEME_LIGHT -> {
                    g2.drawOval(8, 8, 8, 8);
                    g2.drawLine(12, 3, 12, 5);
                    g2.drawLine(12, 19, 12, 21);
                    g2.drawLine(3, 12, 5, 12);
                    g2.drawLine(19, 12, 21, 12);
                }
                case THEME_DARK -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 4);
                    p.curveTo(16, 4, 20, 8, 20, 12);
                    p.curveTo(20, 16, 16, 20, 12, 20);
                    p.curveTo(16, 17, 17, 13, 14, 9);
                    p.curveTo(13, 7, 12, 6, 12, 4);
                    p.closePath();
                    g2.draw(p);
                }
                case CLOSE -> {
                    g2.drawLine(6, 6, 18, 18);
                    g2.drawLine(18, 6, 6, 18);
                }
                case CHEVRON_DOWN -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(6, 9);
                    p.lineTo(12, 15);
                    p.lineTo(18, 9);
                    g2.draw(p);
                }
                case ROW_ABOVE -> {
                    g2.drawRect(4, 10, 16, 10);
                    g2.drawLine(4, 15, 20, 15);
                    Path2D p = new Path2D.Float();
                    p.moveTo(9, 7);
                    p.lineTo(12, 4);
                    p.lineTo(15, 7);
                    g2.draw(p);
                }
                case ROW_BELOW -> {
                    g2.drawRect(4, 4, 16, 10);
                    g2.drawLine(4, 9, 20, 9);
                    Path2D p = new Path2D.Float();
                    p.moveTo(9, 17);
                    p.lineTo(12, 20);
                    p.lineTo(15, 17);
                    g2.draw(p);
                }
                case COL_LEFT -> {
                    g2.drawRect(10, 4, 10, 16);
                    g2.drawLine(15, 4, 15, 20);
                    Path2D p = new Path2D.Float();
                    p.moveTo(7, 9);
                    p.lineTo(4, 12);
                    p.lineTo(7, 15);
                    g2.draw(p);
                }
                case COL_RIGHT -> {
                    g2.drawRect(4, 4, 10, 16);
                    g2.drawLine(9, 4, 9, 20);
                    Path2D p = new Path2D.Float();
                    p.moveTo(17, 9);
                    p.lineTo(20, 12);
                    p.lineTo(17, 15);
                    g2.draw(p);
                }
                case DELETE_ROW -> {
                    g2.drawRect(4, 6, 16, 12);
                    g2.drawLine(4, 12, 20, 12);
                    g2.drawLine(9, 9, 15, 15);
                    g2.drawLine(15, 9, 9, 15);
                }
                case DELETE_COL -> {
                    g2.drawRect(6, 4, 12, 16);
                    g2.drawLine(12, 4, 12, 20);
                    g2.drawLine(9, 9, 15, 15);
                    g2.drawLine(15, 9, 9, 15);
                }
                case DELETE_TABLE -> {
                    g2.drawRect(4, 4, 16, 16);
                    g2.drawLine(4, 10, 20, 10);
                    g2.drawLine(12, 4, 12, 20);
                    g2.drawLine(7, 7, 17, 17);
                    g2.drawLine(17, 7, 7, 17);
                }
                case SHAPE -> {
                    g2.drawOval(4, 4, 8, 8);
                    g2.drawRect(12, 12, 8, 8);
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(16, 4);
                    tri.lineTo(21, 11);
                    tri.lineTo(11, 11);
                    tri.closePath();
                    g2.draw(tri);
                }
                case VIDEO -> {
                    g2.drawRoundRect(3, 6, 13, 12, 3, 3);
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(16, 9);
                    tri.lineTo(21, 6);
                    tri.lineTo(21, 18);
                    tri.lineTo(16, 15);
                    tri.closePath();
                    g2.draw(tri);
                    Path2D play = new Path2D.Float();
                    play.moveTo(8, 9);
                    play.lineTo(12, 12);
                    play.lineTo(8, 15);
                    play.closePath();
                    g2.fill(play);
                }
                case PRINT -> {
                    g2.drawRect(6, 9, 12, 9);
                    g2.drawLine(8, 9, 8, 4);
                    g2.drawLine(8, 4, 16, 4);
                    g2.drawLine(16, 4, 16, 9);
                    g2.drawRect(8, 14, 8, 6);
                }
                case CLEAR_FORMAT -> {
                    Font font = new Font("SansSerif", Font.BOLD, 15);
                    g2.setFont(font);
                    g2.drawString("A", 4, 17);
                    g2.drawLine(14, 6, 20, 18);
                    g2.drawLine(20, 6, 14, 18);
                }
                case SUBSCRIPT -> {
                    Font font = new Font("SansSerif", Font.BOLD, 14);
                    g2.setFont(font);
                    g2.drawString("X", 4, 16);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2.drawString("2", 14, 21);
                }
                case SUPERSCRIPT -> {
                    Font font = new Font("SansSerif", Font.BOLD, 14);
                    g2.setFont(font);
                    g2.drawString("X", 4, 18);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2.drawString("2", 14, 10);
                }
                case FONT_SIZE_UP -> {
                    Font font = new Font("SansSerif", Font.BOLD, 14);
                    g2.setFont(font);
                    g2.drawString("A", 4, 17);
                    Path2D p = new Path2D.Float();
                    p.moveTo(17, 8);
                    p.lineTo(13, 14);
                    p.lineTo(21, 14);
                    p.closePath();
                    g2.fill(p);
                }
                case FONT_SIZE_DOWN -> {
                    Font font = new Font("SansSerif", Font.BOLD, 14);
                    g2.setFont(font);
                    g2.drawString("A", 4, 17);
                    Path2D p = new Path2D.Float();
                    p.moveTo(17, 16);
                    p.lineTo(13, 10);
                    p.lineTo(21, 10);
                    p.closePath();
                    g2.fill(p);
                }
                case INDENT_INC -> {
                    g2.drawLine(4, 6, 20, 6);
                    g2.drawLine(10, 11, 20, 11);
                    g2.drawLine(10, 16, 20, 16);
                    g2.drawLine(4, 21, 20, 21);
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 11);
                    p.lineTo(7, 13.5f);
                    p.lineTo(4, 16);
                    g2.draw(p);
                }
                case INDENT_DEC -> {
                    g2.drawLine(4, 6, 20, 6);
                    g2.drawLine(10, 11, 20, 11);
                    g2.drawLine(10, 16, 20, 16);
                    g2.drawLine(4, 21, 20, 21);
                    Path2D p = new Path2D.Float();
                    p.moveTo(7, 11);
                    p.lineTo(4, 13.5f);
                    p.lineTo(7, 16);
                    g2.draw(p);
                }
                case ALERT_INFO -> {
                    g2.drawOval(3, 3, 18, 18);
                    g2.fillOval(11, 7, 2, 2);
                    g2.drawLine(12, 11, 12, 17);
                }
                case ALERT_TIP -> {
                    g2.drawOval(3, 3, 18, 18);
                    Path2D p = new Path2D.Float();
                    p.moveTo(7, 12);
                    p.lineTo(10, 15);
                    p.lineTo(17, 8);
                    g2.draw(p);
                }
                case ALERT_WARN -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 4);
                    p.lineTo(21, 20);
                    p.lineTo(3, 20);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(12, 10, 12, 14);
                    g2.fillOval(11, 16, 2, 2);
                }
                case CLAUDE -> {
                    // Anthropic signature 8-beam radiant sunburst
                    Color prev = g2.getColor();
                    if (fixedColor == null) g2.setColor(new Color(204, 86, 54));
                    g2.fillOval(10, 10, 4, 4);
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4.0;
                        int x1 = (int) Math.round(12 + Math.cos(angle) * 4);
                        int y1 = (int) Math.round(12 + Math.sin(angle) * 4);
                        int x2 = (int) Math.round(12 + Math.cos(angle) * 9);
                        int y2 = (int) Math.round(12 + Math.sin(angle) * 9);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                    g2.setColor(prev);
                }
                case CHATGPT -> {
                    // OpenAI looping geometric hexagon
                    Color prev = g2.getColor();
                    if (fixedColor == null) g2.setColor(new Color(16, 163, 127));
                    Path2D knot = new Path2D.Float();
                    for (int i = 0; i < 6; i++) {
                        double a1 = i * Math.PI / 3.0;
                        double a2 = (i + 1) * Math.PI / 3.0;
                        float cx = (float) (12 + Math.cos(a1) * 7.5);
                        float cy = (float) (12 + Math.sin(a1) * 7.5);
                        float nx = (float) (12 + Math.cos(a2) * 4.5);
                        float ny = (float) (12 + Math.sin(a2) * 4.5);
                        if (i == 0) knot.moveTo(cx, cy);
                        knot.quadTo(12, 12, nx, ny);
                    }
                    knot.closePath();
                    g2.draw(knot);
                    g2.drawOval(8, 8, 8, 8);
                    g2.setColor(prev);
                }
                case GEMINI -> {
                    // Google 4-point radiant sparkle diamond star
                    Color prev = g2.getColor();
                    if (fixedColor == null) g2.setColor(new Color(124, 58, 237));
                    Path2D star = new Path2D.Float();
                    star.moveTo(12, 3);
                    star.quadTo(12, 12, 21, 12);
                    star.quadTo(12, 12, 12, 21);
                    star.quadTo(12, 12, 3, 12);
                    star.quadTo(12, 12, 12, 3);
                    star.closePath();
                    g2.fill(star);
                    // Secondary accent sparkle
                    Path2D mini = new Path2D.Float();
                    mini.moveTo(18, 4);
                    mini.quadTo(18, 6, 20, 6);
                    mini.quadTo(18, 6, 18, 8);
                    mini.quadTo(18, 6, 16, 6);
                    mini.quadTo(18, 6, 18, 4);
                    mini.closePath();
                    g2.fill(mini);
                    g2.setColor(prev);
                }
                case DEEPSEEK -> {
                    // DeepSeek dynamic wave / marine crest
                    Color prev = g2.getColor();
                    if (fixedColor == null) g2.setColor(new Color(2, 132, 199));
                    Path2D whale = new Path2D.Float();
                    whale.moveTo(4, 15);
                    whale.curveTo(7, 7, 15, 6, 20, 9);
                    whale.curveTo(18, 14, 14, 18, 7, 18);
                    whale.quadTo(4, 18, 4, 15);
                    whale.closePath();
                    g2.draw(whale);
                    // Dorsal fin & eye
                    g2.drawLine(12, 8, 14, 4);
                    g2.drawLine(14, 4, 16, 8);
                    g2.fillOval(7, 13, 2, 2);
                    g2.setColor(prev);
                }
                case PERPLEXITY -> {
                    // Perplexity 6-point intersecting asterism
                    Color prev = g2.getColor();
                    if (fixedColor == null) g2.setColor(new Color(13, 148, 136));
                    g2.drawLine(12, 3, 12, 21);
                    g2.drawLine(4, 8, 20, 16);
                    g2.drawLine(4, 16, 20, 8);
                    g2.drawOval(8, 8, 8, 8);
                    g2.setColor(prev);
                }
                case SCRAPER, WEB -> {
                    Color prev = g2.getColor();
                    if (fixedColor == null) g2.setColor(new Color(14, 165, 233));
                    g2.drawOval(3, 3, 18, 18);
                    g2.drawLine(3, 12, 21, 12);
                    g2.drawOval(7, 3, 10, 18);
                    g2.setColor(prev);
                }
                case PASTE -> {
                    // Clipboard with document paper
                    g2.drawRoundRect(6, 4, 12, 17, 3, 3);
                    g2.drawRect(9, 2, 6, 3);
                    g2.drawLine(9, 10, 15, 10);
                    g2.drawLine(9, 14, 15, 14);
                }
                case CUT -> {
                    // Scissor loops and blades
                    g2.drawOval(4, 14, 5, 5);
                    g2.drawOval(15, 14, 5, 5);
                    g2.drawLine(7, 14, 17, 4);
                    g2.drawLine(17, 14, 7, 4);
                }
                case COPY -> {
                    // Overlapping documents
                    g2.drawRoundRect(7, 7, 12, 14, 2, 2);
                    g2.drawPolyline(new int[]{5, 5, 15, 15}, new int[]{17, 3, 3, 7}, 4);
                }
                case FORMAT_PAINTER -> {
                    // Paint roller / brush
                    g2.drawRoundRect(5, 4, 14, 6, 2, 2);
                    g2.drawLine(12, 10, 12, 15);
                    g2.drawRoundRect(10, 15, 4, 7, 2, 2);
                }
                case FONT_COLOR -> {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                    g2.drawString("A", 6, 16);
                    g2.fillRect(4, 18, 16, 4);
                }
                case HIGHLIGHT -> {
                    Path2D h = new Path2D.Float();
                    h.moveTo(7, 17);
                    h.lineTo(16, 8);
                    h.lineTo(19, 11);
                    h.lineTo(10, 20);
                    h.closePath();
                    g2.draw(h);
                    g2.drawLine(5, 21, 9, 21);
                }
                case FIND -> {
                    g2.drawOval(5, 5, 10, 10);
                    g2.drawLine(13, 13, 20, 20);
                }
                case REPLACE -> {
                    g2.drawOval(4, 4, 9, 9);
                    g2.drawLine(11, 11, 17, 17);
                    // Cycle arrows
                    Path2D ar = new Path2D.Float();
                    ar.moveTo(13, 19);
                    ar.lineTo(20, 19);
                    ar.lineTo(17, 16);
                    g2.draw(ar);
                }
                case WORD_COUNT -> {
                    g2.drawRoundRect(4, 3, 16, 18, 3, 3);
                    g2.drawLine(8, 7, 16, 7);
                    g2.drawLine(8, 11, 14, 11);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 8));
                    g2.drawString("123", 7, 18);
                }
                case EQUATION -> {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                    g2.drawString("∑", 4, 16);
                    g2.drawString("√", 13, 16);
                }
                case ORIENTATION -> {
                    // Portrait & Landscape
                    g2.drawRect(4, 5, 8, 14);
                    g2.drawRect(10, 9, 10, 7);
                }
                case MARGINS -> {
                    g2.drawRect(4, 3, 16, 18);
                    float[] dash = {2.0f, 2.0f};
                    Stroke orig = g2.getStroke();
                    g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                    g2.drawRect(7, 6, 10, 12);
                    g2.setStroke(orig);
                }
                case COLUMNS -> {
                    g2.drawRect(4, 3, 16, 18);
                    g2.fillRect(7, 6, 4, 12);
                    g2.fillRect(13, 6, 4, 12);
                }
                case WATERMARK -> {
                    g2.drawRect(4, 3, 16, 18);
                    g2.setFont(new Font("SansSerif", Font.ITALIC, 9));
                    g2.drawString("DRAFT", 5, 14);
                }
                case FULLSCREEN -> {
                    // 4 corner brackets
                    g2.drawPolyline(new int[]{4, 4, 8}, new int[]{8, 4, 4}, 3);
                    g2.drawPolyline(new int[]{16, 20, 20}, new int[]{4, 4, 8}, 3);
                    g2.drawPolyline(new int[]{4, 4, 8}, new int[]{16, 20, 20}, 3);
                    g2.drawPolyline(new int[]{16, 20, 20}, new int[]{20, 20, 16}, 3);
                }
                case SPEECH -> {
                    g2.drawPolygon(new int[]{4, 9, 13, 13, 9, 4}, new int[]{9, 9, 5, 19, 15, 15}, 6);
                    g2.drawArc(12, 8, 6, 8, -60, 120);
                    g2.drawArc(12, 5, 9, 14, -60, 120);
                }
                case A11Y -> {
                    g2.drawOval(3, 3, 18, 18);
                    g2.fillOval(11, 5, 2, 2);
                    g2.drawLine(6, 10, 18, 10);
                    g2.drawLine(12, 10, 12, 15);
                    g2.drawLine(12, 15, 9, 19);
                    g2.drawLine(12, 15, 15, 19);
                }
                case CHART_BAR -> {
                    g2.drawLine(3, 20, 21, 20);
                    g2.fillRect(5, 12, 3, 8);
                    g2.fillRect(10, 7, 3, 13);
                    g2.fillRect(15, 3, 3, 17);
                }
                case CHART_COLUMN -> {
                    g2.drawLine(3, 20, 21, 20);
                    g2.fillRect(4, 13, 4, 7);
                    g2.fillRect(10, 6, 4, 14);
                    g2.fillRect(16, 9, 4, 11);
                }
                case CHART_LINE -> {
                    g2.drawLine(3, 20, 21, 20);
                    g2.drawLine(3, 4, 3, 20);
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 16);
                    p.lineTo(9, 11);
                    p.lineTo(14, 15);
                    p.lineTo(19, 6);
                    g2.draw(p);
                    g2.fillOval(3, 15, 3, 3);
                    g2.fillOval(8, 10, 3, 3);
                    g2.fillOval(13, 14, 3, 3);
                    g2.fillOval(18, 5, 3, 3);
                }
                case CHART_AREA -> {
                    g2.drawLine(3, 20, 21, 20);
                    g2.drawLine(3, 4, 3, 20);
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 16);
                    p.lineTo(9, 11);
                    p.lineTo(14, 14);
                    p.lineTo(19, 7);
                    p.lineTo(19, 20);
                    p.lineTo(4, 20);
                    p.closePath();
                    Composite orig = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                    g2.fill(p);
                    g2.setComposite(orig);
                    Path2D line = new Path2D.Float();
                    line.moveTo(4, 16);
                    line.lineTo(9, 11);
                    line.lineTo(14, 14);
                    line.lineTo(19, 7);
                    g2.draw(line);
                    g2.fillOval(8, 10, 3, 3);
                    g2.fillOval(13, 13, 3, 3);
                    g2.fillOval(18, 6, 3, 3);
                }
                case CHART_PIE -> {
                    g2.drawOval(3, 3, 18, 18);
                    g2.drawLine(12, 12, 12, 3);
                    g2.drawLine(12, 12, 21, 12);
                    g2.drawLine(12, 12, 6, 18);
                }
                case CHART_DONUT -> {
                    g2.drawOval(3, 3, 18, 18);
                    g2.drawOval(8, 8, 8, 8);
                    g2.drawLine(12, 3, 12, 8);
                    g2.drawLine(16, 16, 20, 18);
                    g2.drawLine(4, 15, 8, 14);
                }
                case CHART_RADAR -> {
                    int cx = 12, cy = 12;
                    int[] px = {12, 20, 20, 12, 4, 4};
                    int[] py = {3, 7, 17, 21, 17, 7};
                    g2.drawPolygon(px, py, 6);
                    int[] px2 = {12, 16, 16, 12, 8, 8};
                    int[] py2 = {7, 9, 15, 17, 15, 9};
                    g2.drawPolygon(px2, py2, 6);
                    for (int i = 0; i < 6; i++) {
                        g2.drawLine(cx, cy, px[i], py[i]);
                    }
                    int[] dataX = {12, 18, 15, 12, 6, 8};
                    int[] dataY = {5, 8, 16, 19, 14, 8};
                    Composite origR = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                    g2.fillPolygon(dataX, dataY, 6);
                    g2.setComposite(origR);
                    g2.drawPolygon(dataX, dataY, 6);
                }
                case CHART_SCATTER -> {
                    g2.drawLine(3, 20, 21, 20);
                    g2.drawLine(3, 4, 3, 20);
                    g2.drawOval(6, 14, 3, 3);
                    g2.fillOval(10, 8, 3, 3);
                    g2.drawOval(14, 12, 3, 3);
                    g2.fillOval(17, 5, 3, 3);
                    g2.drawOval(18, 15, 3, 3);
                }
                case CHART_TREE -> {
                    g2.drawRoundRect(8, 3, 8, 5, 2, 2);
                    g2.drawLine(12, 8, 12, 13);
                    g2.drawLine(6, 13, 18, 13);
                    g2.drawLine(6, 13, 6, 16);
                    g2.drawLine(18, 13, 18, 16);
                    g2.drawRoundRect(2, 16, 8, 5, 2, 2);
                    g2.drawRoundRect(14, 16, 8, 5, 2, 2);
                }
                case CHART_LOLLIPOP -> {
                    g2.drawLine(3, 20, 21, 20);
                    // 3 lollipops: line + round candy head
                    g2.drawLine(6, 12, 6, 20);
                    g2.fillOval(4, 9, 5, 5);
                    g2.drawLine(12, 6, 12, 20);
                    g2.fillOval(10, 3, 5, 5);
                    g2.drawLine(18, 14, 18, 20);
                    g2.fillOval(16, 11, 5, 5);
                }
                case CHART_WATERFALL -> {
                    g2.drawLine(3, 20, 21, 20);
                    // Floating step bars with connectors
                    g2.fillRect(4, 12, 3, 8); // Base gain
                    g2.drawLine(7, 12, 9, 12);
                    g2.fillRect(9, 7, 3, 5);  // Step up
                    g2.drawLine(12, 7, 14, 7);
                    g2.fillRect(14, 7, 3, 4); // Step down
                    g2.drawLine(17, 11, 18, 11);
                    g2.fillRect(18, 11, 3, 9); // Total bar
                }
                case CHART_FUNNEL -> {
                    // Segmented funnel polygon / horizontal trapezoids
                    int[] x1 = {4, 20, 17, 7};
                    int[] y1 = {4, 4, 8, 8};
                    g2.drawPolygon(x1, y1, 4);
                    int[] x2 = {7, 17, 15, 9};
                    int[] y2 = {10, 10, 14, 14};
                    g2.drawPolygon(x2, y2, 4);
                    int[] x3 = {9, 15, 13, 11};
                    int[] y3 = {16, 16, 20, 20};
                    g2.fillPolygon(x3, y3, 4);
                }
                case CHART_GAUGE -> {
                    // Speedometer / Gauge dial arc
                    g2.drawArc(3, 4, 18, 18, 0, 180);
                    g2.drawLine(3, 13, 5, 13);
                    g2.drawLine(21, 13, 19, 13);
                    g2.drawLine(12, 4, 12, 6);
                    // Center pivot & indicator needle
                    g2.fillOval(10, 11, 4, 4);
                    g2.drawLine(12, 12, 17, 7);
                }
                case CHART_POLAR -> {
                    // Concentric radar/polar grid rings
                    g2.drawOval(4, 4, 16, 16);
                    g2.drawOval(8, 8, 8, 8);
                    g2.drawLine(12, 3, 12, 21);
                    g2.drawLine(3, 12, 21, 12);
                    // Filled radial petal sector
                    Composite origPol = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                    g2.fillArc(4, 4, 16, 16, 0, 90);
                    g2.setComposite(origPol);
                }
                case KPI_CARD -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    g2.drawLine(6, 8, 13, 8);
                    g2.fillRect(6, 12, 6, 4);
                    Path2D ar = new Path2D.Float();
                    ar.moveTo(17, 9);
                    ar.lineTo(14, 13);
                    ar.lineTo(20, 13);
                    ar.closePath();
                    g2.fill(ar);
                    g2.drawLine(17, 13, 17, 17);
                }
                case MULTI_KPI -> {
                    g2.drawRoundRect(2, 4, 5, 16, 2, 2);
                    g2.drawRoundRect(9, 4, 6, 16, 2, 2);
                    g2.drawRoundRect(17, 4, 5, 16, 2, 2);
                    g2.drawLine(3, 8, 6, 8);
                    g2.drawLine(10, 8, 14, 8);
                    g2.drawLine(18, 8, 21, 8);
                }
                case SPOTLIGHT -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    Path2D star = new Path2D.Float();
                    star.moveTo(12, 7);
                    star.lineTo(14, 10);
                    star.lineTo(18, 12);
                    star.lineTo(14, 14);
                    star.lineTo(12, 17);
                    star.lineTo(10, 14);
                    star.lineTo(6, 12);
                    star.lineTo(10, 10);
                    star.closePath();
                    g2.fill(star);
                }
                case COMPARISON -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    g2.drawLine(12, 4, 12, 20);
                    g2.drawLine(5, 12, 7, 15);
                    g2.drawLine(7, 15, 10, 9);
                    g2.drawLine(14, 12, 16, 15);
                    g2.drawLine(16, 15, 19, 9);
                }
                case PROGRESS -> {
                    g2.drawRoundRect(3, 9, 18, 6, 3, 3);
                    g2.fillRoundRect(5, 11, 10, 2, 1, 1);
                }
                case TAKEAWAYS -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    g2.fillOval(5, 7, 2, 2);
                    g2.drawLine(9, 8, 18, 8);
                    g2.fillOval(5, 11, 2, 2);
                    g2.drawLine(9, 12, 18, 12);
                    g2.fillOval(5, 15, 2, 2);
                    g2.drawLine(9, 16, 18, 16);
                }
                case GRID_2X2 -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    g2.drawLine(3, 12, 21, 12);
                    g2.drawLine(12, 4, 12, 20);
                }
                case GRID_3X3 -> {
                    g2.drawRoundRect(3, 4, 18, 16, 3, 3);
                    g2.drawLine(3, 9, 21, 9);
                    g2.drawLine(3, 15, 21, 15);
                    g2.drawLine(9, 4, 9, 20);
                    g2.drawLine(15, 4, 15, 20);
                }
                case DIVIDER -> {
                    g2.drawLine(3, 12, 9, 12);
                    g2.fillRect(10, 10, 4, 4);
                    g2.drawLine(15, 12, 21, 12);
                }
                case STAR_RATING -> {
                    Font orig = g2.getFont();
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    g2.drawString("★", 4, 15);
                    g2.drawString("★", 13, 15);
                    g2.setFont(orig);
                }
                case STEP_MINUS -> {
                    g2.drawOval(4, 4, 16, 16);
                    g2.drawLine(8, 12, 16, 12);
                }
                case STEP_PLUS -> {
                    g2.drawOval(4, 4, 16, 16);
                    g2.drawLine(8, 12, 16, 12);
                    g2.drawLine(12, 8, 12, 16);
                }
                default -> g2.drawRect(4, 4, 16, 16);
            }
        }
    }
}
