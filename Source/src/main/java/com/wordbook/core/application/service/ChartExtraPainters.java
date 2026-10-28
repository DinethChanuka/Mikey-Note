package com.wordbook.core.application.service;

import com.wordbook.core.application.service.ChartEngineService.Row;
import com.wordbook.core.application.service.ChartEngineService.ThemeSpec;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Extra Java 2D chart painters (Lollipop, Waterfall, Funnel, Gauge, Polar Area) and the
 * chart-type-aware "minimal detail" renderers used by the detail-level control.
 * All painters are stateless and draw into an already prepared card background.
 */
final class ChartExtraPainters {
    private static final Color UP = new Color(22, 163, 74);
    private static final Color DOWN = new Color(220, 38, 38);

    private ChartExtraPainters() {}

    // ------------------------------------------------------------------ helpers
    private static double val(Row r) {
        return r.value() != null ? r.value() : 1.0;
    }

    private static double maxAbs(List<Row> rows) {
        double m = 0;
        for (Row r : rows) m = Math.max(m, Math.abs(val(r)));
        return m <= 0 ? 1.0 : m;
    }

    static String fmt(double d) {
        if (Math.abs(d) >= 1_000_000) return String.format("%.1fM", d / 1_000_000);
        if (Math.abs(d) >= 10_000) return String.format("%.0fk", d / 1000);
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format("%.1f", d);
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static void centered(Graphics2D g2, String s, int cx, int y) {
        g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, y);
    }

    // ------------------------------------------------------------------ lollipop
    static void drawLollipop(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM,
                             boolean showValues, boolean compact) {
        int count = rows.size();
        int bottomM = 40, leftM = 48, rightM = 32;
        int chartH = h - topM - bottomM;
        int chartW = w - leftM - rightM;
        double max = maxAbs(rows);

        g2.setColor(theme.getBorder());
        g2.drawLine(leftM, h - bottomM, w - rightM, h - bottomM);
        if (!compact) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(theme.getTextSecondary());
            for (int i = 1; i <= 3; i++) {
                int gy = (h - bottomM) - (int) (i / 4.0 * chartH);
                g2.setColor(theme.getBorder());
                g2.drawLine(leftM, gy, w - rightM, gy);
                g2.setColor(theme.getTextSecondary());
                g2.drawString(fmt(max * i / 4.0), 10, gy + 4);
            }
        }

        Color[] pal = theme.getPalette();
        int slot = chartW / count;
        for (int i = 0; i < count; i++) {
            double v = Math.max(0, val(rows.get(i)));
            int cx = leftM + i * slot + slot / 2;
            int top = (h - bottomM) - (int) (v / max * chartH);
            Color c = pal[i % pal.length];
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, h - bottomM, cx, top);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(c);
            g2.fillOval(cx - 8, top - 8, 16, 16);
            g2.setColor(theme.getBackground());
            g2.fillOval(cx - 3, top - 3, 6, 6);
            if (showValues) {
                g2.setColor(theme.getTextPrimary());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                centered(g2, fmt(v), cx, top - 13);
            }
            g2.setColor(theme.getTextSecondary());
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            centered(g2, cut(rows.get(i).name(), Math.max(5, slot / 6)), cx, h - bottomM + 16);
        }
    }

    // ------------------------------------------------------------------ waterfall
    static void drawWaterfall(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM,
                              boolean showValues, boolean compact) {
        int bars = rows.size() + 1; // + final total
        int bottomM = 40, leftM = 48, rightM = 32;
        int chartH = h - topM - bottomM;
        int chartW = w - leftM - rightM;

        double[] starts = new double[bars];
        double[] ends = new double[bars];
        double run = 0, lo = 0, hi = 0;
        for (int i = 0; i < rows.size(); i++) {
            starts[i] = run;
            run += val(rows.get(i));
            ends[i] = run;
            lo = Math.min(lo, Math.min(starts[i], ends[i]));
            hi = Math.max(hi, Math.max(starts[i], ends[i]));
        }
        starts[bars - 1] = 0;
        ends[bars - 1] = run;
        lo = Math.min(lo, Math.min(0, run));
        hi = Math.max(hi, Math.max(0, run));
        double span = hi - lo;
        if (span <= 0) span = 1;

        final double fLo = lo, fSpan = span;
        java.util.function.DoubleToIntFunction yOf = v -> (h - bottomM) - (int) ((v - fLo) / fSpan * chartH);

        int zeroY = yOf.applyAsInt(0);
        g2.setColor(theme.getBorder());
        g2.drawLine(leftM, zeroY, w - rightM, zeroY);
        if (!compact) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            for (int i = 0; i <= 3; i++) {
                double gv = lo + span * i / 3.0;
                int gy = yOf.applyAsInt(gv);
                g2.setColor(theme.getBorder());
                g2.drawLine(leftM, gy, w - rightM, gy);
                g2.setColor(theme.getTextSecondary());
                g2.drawString(fmt(gv), 6, gy + 4);
            }
        }

        int slot = chartW / bars;
        int barW = Math.max(14, Math.min(54, (int) (slot * 0.62)));
        for (int i = 0; i < bars; i++) {
            boolean total = i == bars - 1;
            int x = leftM + i * slot + (slot - barW) / 2;
            int y1 = yOf.applyAsInt(Math.max(starts[i], ends[i]));
            int y2 = yOf.applyAsInt(Math.min(starts[i], ends[i]));
            int bh = Math.max(3, y2 - y1);
            Color c = total ? theme.getPalette()[0] : (ends[i] >= starts[i] ? UP : DOWN);
            g2.setColor(c);
            g2.fillRoundRect(x, y1, barW, bh, 6, 6);

            if (i < bars - 1) { // connector
                int cy = yOf.applyAsInt(ends[i]);
                g2.setColor(theme.getTextSecondary());
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                g2.drawLine(x + barW, cy, x + slot, cy);
                g2.setStroke(new BasicStroke(1f));
            }
            if (showValues) {
                double shown = total ? run : val(rows.get(i));
                g2.setColor(theme.getTextPrimary());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String s = (!total && shown > 0 ? "+" : "") + fmt(shown);
                centered(g2, s, x + barW / 2, y1 - 5);
            }
            g2.setColor(theme.getTextSecondary());
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            String name = total ? "Total" : rows.get(i).name();
            centered(g2, cut(name, Math.max(5, slot / 6)), x + barW / 2, h - bottomM + 16);
        }
    }

    // ------------------------------------------------------------------ funnel
    static void drawFunnel(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM,
                           boolean showValues, boolean showLegend) {
        int n = rows.size();
        double max = maxAbs(rows);
        int availH = h - topM - 18;
        int gap = 4;
        int stepH = Math.max(14, (availH - gap * (n - 1)) / n);
        int labelCol = showLegend ? Math.min(150, w / 4) : 0;
        int maxW = w - 48 - labelCol;
        int cx = 24 + labelCol + maxW / 2;
        Color[] pal = theme.getPalette();

        for (int i = 0; i < n; i++) {
            double v = Math.max(0, val(rows.get(i)));
            double next = i + 1 < n ? Math.max(0, val(rows.get(i + 1))) : v * 0.82;
            double topW = Math.max(0.12, v / max) * maxW;
            double botW = Math.max(0.10, Math.min(next, v) / max) * maxW;
            int y = topM + i * (stepH + gap);

            Path2D.Double p = new Path2D.Double();
            p.moveTo(cx - topW / 2, y);
            p.lineTo(cx + topW / 2, y);
            p.lineTo(cx + botW / 2, y + stepH);
            p.lineTo(cx - botW / 2, y + stepH);
            p.closePath();
            g2.setColor(pal[i % pal.length]);
            g2.fill(p);

            if (showValues && stepH >= 14) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, Math.min(12, stepH - 3)));
                String label = fmt(v) + (i > 0 ? "  (" + Math.round(v / max * 100.0) + "%)" : "");
                int tw = g2.getFontMetrics().stringWidth(label);
                double fitW = Math.min(topW, botW) - 10;
                if (tw <= fitW) {
                    g2.setColor(Color.WHITE);
                    centered(g2, label, cx, y + stepH / 2 + 4);
                } else {
                    g2.setColor(theme.getTextPrimary());
                    g2.drawString(label, (int) (cx + Math.max(topW, botW) / 2 + 8), y + stepH / 2 + 4);
                }
            }
            if (showLegend) {
                g2.setColor(theme.getTextPrimary());
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.drawString(cut(rows.get(i).name(), 20), 24, y + stepH / 2 + 4);
            }
        }
    }

    // ------------------------------------------------------------------ gauge
    /** First row = value, optional second row = maximum (defaults to 100 or the next "nice" ceiling). */
    static void drawGauge(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM, boolean showValues) {
        double value = Math.max(0, val(rows.get(0)));
        double max = 100;
        if (rows.size() > 1 && val(rows.get(1)) > 0) max = val(rows.get(1));
        else if (value > 100) max = Math.pow(10, Math.ceil(Math.log10(value)));
        if (value > max) max = value;
        double ratio = value / max;

        int availH = h - topM - 16;
        int radius = Math.min((w - 80) / 2, (int) (availH * 0.92));
        int thick = Math.max(14, radius / 4);
        int cx = w / 2;
        int cy = topM + radius + 4;
        if (cy > h - 26) cy = h - 26;

        g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(theme.getBorder());
        g2.draw(new Arc2D.Double(cx - radius + thick / 2.0, cy - radius + thick / 2.0, 2.0 * radius - thick, 2.0 * radius - thick,
                180, -180, Arc2D.OPEN));

        Color[] pal = theme.getPalette();
        Color c = ratio < 0.34 ? pal[Math.min(pal.length - 1, 3)] : ratio < 0.67 ? pal[Math.min(pal.length - 1, 1)] : pal[0];
        g2.setPaint(new GradientPaint(cx - radius, cy, pal[Math.min(1, pal.length - 1)], cx + radius, cy, c));
        g2.draw(new Arc2D.Double(cx - radius + thick / 2.0, cy - radius + thick / 2.0, 2.0 * radius - thick, 2.0 * radius - thick,
                180, -180 * ratio, Arc2D.OPEN));
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(theme.getTextPrimary());
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(18, radius / 3)));
        if (showValues) centered(g2, fmt(value), cx, cy - radius / 6);
        g2.setColor(theme.getTextSecondary());
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        centered(g2, cut(rows.get(0).name(), 24) + "  •  " + Math.round(ratio * 100) + "% of " + fmt(max), cx, cy + 14);
        g2.drawString("0", cx - radius, cy + 14);
        String mx = fmt(max);
        g2.drawString(mx, cx + radius - g2.getFontMetrics().stringWidth(mx), cy + 14);
    }

    // ------------------------------------------------------------------ polar area
    static void drawPolar(Graphics2D g2, List<Row> rows, ThemeSpec theme, int w, int h, int topM,
                          boolean showValues, boolean showLegend) {
        int n = rows.size();
        double max = maxAbs(rows);
        int availH = h - topM - 16;
        int radius = Math.min(availH / 2, 110);
        int cx = showLegend ? 24 + radius + 16 : w / 2;
        int cy = topM + availH / 2;
        Color[] pal = theme.getPalette();

        g2.setColor(theme.getBorder());
        for (int ring = 1; ring <= 3; ring++) {
            int rr = radius * ring / 3;
            g2.drawOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }
        double sweep = 360.0 / n;
        for (int i = 0; i < n; i++) {
            double v = Math.max(0, val(rows.get(i)));
            double r = Math.max(0.08, Math.sqrt(v / max)) * radius;
            g2.setColor(pal[i % pal.length]);
            g2.fill(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 90 - i * sweep, -sweep + 1.5, Arc2D.PIE));
        }
        if (showLegend) {
            int lx = cx + radius + 32;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int i = 0; i < n; i++) {
                int ly = topM + 10 + i * 24;
                if (ly > h - 16) break;
                g2.setColor(pal[i % pal.length]);
                g2.fillRoundRect(lx, ly, 12, 12, 3, 3);
                g2.setColor(theme.getTextPrimary());
                g2.drawString(cut(rows.get(i).name(), 16) + (showValues ? " (" + fmt(val(rows.get(i))) + ")" : ""), lx + 20, ly + 11);
            }
        }
    }

    // ------------------------------------------------------------------ minimal (per chart type)
    /**
     * Type-aware "Minimal" card: a headline figure plus the most reduced form of the chosen chart type.
     * @return false when the type has no sensible minimal form (caller falls back to the KPI badge)
     */
    static boolean drawMinimal(Graphics2D g2, ChartType type, List<Row> rows, ThemeSpec theme, int w, int h, int topM) {
        if (rows.isEmpty()) return false;
        if (type == ChartType.TREE || type == ChartType.DIAGRAM) return false;
        if (type == ChartType.WATERFALL) { // show the running total instead of raw deltas
            List<Row> running = new java.util.ArrayList<>();
            double acc = 0;
            for (Row r : rows) { acc += val(r); running.add(new Row(r.name(), acc, r.depth())); }
            rows = running;
        }

        Color[] pal = theme.getPalette();
        double total = 0;
        for (Row r : rows) total += val(r);
        double max = maxAbs(rows);
        double head = type == ChartType.WATERFALL ? val(rows.get(rows.size() - 1)) : val(rows.get(0));
        boolean radial = type == ChartType.PIE || type == ChartType.DONUT || type == ChartType.POLAR
                || type == ChartType.RADAR || type == ChartType.GAUGE;
        if (radial && type != ChartType.GAUGE) head = total;

        g2.setColor(theme.getTextPrimary());
        g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g2.drawString(fmt(head), 24, topM + 30);
        g2.setColor(theme.getTextSecondary());
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString(type == ChartType.GAUGE ? cut(rows.get(0).name(), 24)
                : radial ? rows.size() + " items • total"
                : type == ChartType.WATERFALL ? "net result" : cut(rows.get(0).name(), 26), 24, topM + 50);

        int vx = 24, vw = w - 48;
        int vy = topM + 62, vh = h - vy - 18;
        if (vh < 20) vh = 20;
        int n = rows.size();

        switch (type) {
            case PIE, DONUT, POLAR, RADAR, GAUGE -> {
                int d = Math.min(h - topM - 20, 120);
                int cx = w - 24 - d / 2 - 8;
                int cy = topM + (h - topM) / 2 - 4;
                if (type == ChartType.RADAR) {
                    Path2D.Double poly = new Path2D.Double();
                    int cnt = Math.max(3, n);
                    for (int i = 0; i < cnt; i++) {
                        double a = -Math.PI / 2 + i * 2 * Math.PI / cnt;
                        double rr = (i < n ? Math.max(0.1, Math.max(0, val(rows.get(i))) / max) : 0.1) * d / 2;
                        double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
                        if (i == 0) poly.moveTo(x, y); else poly.lineTo(x, y);
                    }
                    poly.closePath();
                    g2.setColor(new Color(pal[0].getRed(), pal[0].getGreen(), pal[0].getBlue(), 90));
                    g2.fill(poly);
                    g2.setColor(pal[0]);
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(poly);
                    g2.setStroke(new BasicStroke(1f));
                } else if (type == ChartType.GAUGE) {
                    double mx = n > 1 && val(rows.get(1)) > 0 ? val(rows.get(1)) : 100;
                    double ratio = Math.min(1, Math.max(0, val(rows.get(0)) / mx));
                    g2.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(theme.getBorder());
                    g2.draw(new Arc2D.Double(cx - d / 2.0 + 6, cy - d / 2.0 + 6, d - 12, d - 12, 180, -180, Arc2D.OPEN));
                    g2.setColor(pal[0]);
                    g2.draw(new Arc2D.Double(cx - d / 2.0 + 6, cy - d / 2.0 + 6, d - 12, d - 12, 180, -180 * ratio, Arc2D.OPEN));
                    g2.setStroke(new BasicStroke(1f));
                } else {
                    double start = 90;
                    for (int i = 0; i < n; i++) {
                        double v = Math.max(0, val(rows.get(i)));
                        double arc = type == ChartType.POLAR ? 360.0 / n : v / Math.max(1e-9, total) * 360.0;
                        double rr = type == ChartType.POLAR ? Math.max(0.15, Math.sqrt(v / max)) * d / 2 : d / 2.0;
                        g2.setColor(pal[i % pal.length]);
                        g2.fill(new Arc2D.Double(cx - rr, cy - rr, rr * 2, rr * 2, start, -arc, Arc2D.PIE));
                        start -= arc;
                    }
                    if (type == ChartType.DONUT) {
                        int in = (int) (d * 0.34);
                        g2.setColor(theme.getBackground());
                        g2.fillOval(cx - in, cy - in, in * 2, in * 2);
                    }
                }
            }
            case LINE, AREA, SCATTER -> {
                int[] xs = new int[n], ys = new int[n];
                for (int i = 0; i < n; i++) {
                    xs[i] = vx + (n > 1 ? i * vw / (n - 1) : vw / 2);
                    ys[i] = vy + vh - (int) (Math.max(0, val(rows.get(i))) / max * vh);
                }
                if (type == ChartType.AREA) {
                    Path2D.Double p = new Path2D.Double();
                    p.moveTo(xs[0], vy + vh);
                    for (int i = 0; i < n; i++) p.lineTo(xs[i], ys[i]);
                    p.lineTo(xs[n - 1], vy + vh);
                    p.closePath();
                    g2.setColor(new Color(pal[0].getRed(), pal[0].getGreen(), pal[0].getBlue(), 70));
                    g2.fill(p);
                }
                g2.setColor(pal[0]);
                if (type != ChartType.SCATTER) {
                    g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < n - 1; i++) g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
                    g2.setStroke(new BasicStroke(1f));
                }
                for (int i = 0; i < n; i++) {
                    int r = (type == ChartType.SCATTER || i == n - 1) ? 4 : 0;
                    if (r > 0) g2.fillOval(xs[i] - r, ys[i] - r, r * 2, r * 2);
                }
            }
            default -> { // BAR, COLUMN, LOLLIPOP, WATERFALL, FUNNEL and anything bar-like
                int slot = vw / n;
                int bw = Math.max(6, Math.min(34, (int) (slot * 0.6)));
                for (int i = 0; i < n; i++) {
                    int bh = Math.max(3, (int) (Math.max(0, val(rows.get(i))) / max * vh));
                    int x = vx + i * slot + (slot - bw) / 2;
                    g2.setColor(i == n - 1 ? pal[0] : new Color(pal[0].getRed(), pal[0].getGreen(), pal[0].getBlue(), 110));
                    if (type == ChartType.LOLLIPOP) {
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawLine(x + bw / 2, vy + vh, x + bw / 2, vy + vh - bh);
                        g2.setStroke(new BasicStroke(1f));
                        g2.fillOval(x + bw / 2 - 5, vy + vh - bh - 5, 10, 10);
                    } else {
                        g2.fillRoundRect(x, vy + vh - bh, bw, bh, 5, 5);
                    }
                }
            }
        }
        return true;
    }
}
