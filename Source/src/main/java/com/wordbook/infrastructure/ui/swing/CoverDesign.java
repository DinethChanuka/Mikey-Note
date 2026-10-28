package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Registry of 55 distinct vector cover designs, watermarks, and patterns
 * for notebook covers in NoteDown.
 */
public class CoverDesign {

    public interface DesignRenderer {
        void render(Graphics2D g2, int w, int h, Color baseColor);
    }

    public static final double REF_WIDTH = 140.0;
    public static final double REF_HEIGHT = 160.0;

    private static final Map<String, String> KEYWORDS_MAP = new HashMap<>();

    private static void tag(String id, String tags) {
        KEYWORDS_MAP.put(id.toUpperCase(), tags);
    }

    private final String id;
    private final String displayName;
    private final String category;
    private final String keywords;
    private final DesignRenderer renderer;
    private Icon cachedThumbnail;

    public CoverDesign(String id, String displayName, String category, String keywords, DesignRenderer renderer) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.keywords = keywords != null ? keywords : "";
        this.renderer = renderer;
    }

    public CoverDesign(String id, String displayName, String category, DesignRenderer renderer) {
        this(id, displayName, category, KEYWORDS_MAP.getOrDefault(id != null ? id.toUpperCase() : "", ""), renderer);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getCategory() { return category; }
    public String getKeywords() { return keywords; }

    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        String[] terms = q.split("\\s+");
        String haystack = (id + " " + displayName + " " + category + " " + keywords).toLowerCase();
        for (String term : terms) {
            if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }

    public void render(Graphics2D g2, int w, int h, Color baseColor) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        renderer.render(g, w, h, baseColor);
        g.dispose();
    }

    public Icon getThumbnail(Color baseColor) {
        if (cachedThumbnail == null) {
            int tw = 28, th = 28;
            BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(baseColor != null ? baseColor : new Color(37, 99, 235));
            g2.fillRoundRect(2, 2, tw - 4, th - 4, 6, 6);
            render(g2, tw, th, baseColor != null ? baseColor : new Color(37, 99, 235));
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawRoundRect(2, 2, tw - 5, th - 5, 6, 6);
            g2.dispose();
            cachedThumbnail = new ImageIcon(img);
        }
        return cachedThumbnail;
    }

    @Override
    public String toString() {
        return displayName;
    }

    // -------------------------------------------------------------
    // 112 DISTINCT VECTOR WATERMARK / COVER DESIGNS
    // -------------------------------------------------------------
    public static final List<CoverDesign> ALL_DESIGNS = new ArrayList<>();
    private static final Map<String, CoverDesign> BY_ID = new HashMap<>();

    private static void register(String id, String name, String category, DesignRenderer r) {
        CoverDesign cd = new CoverDesign(id, name, category, r);
        ALL_DESIGNS.add(cd);
        BY_ID.put(id.toUpperCase(), cd);
    }

    public static CoverDesign fromId(String id) {
        if (id == null) return ALL_DESIGNS.get(0);
        CoverDesign found = BY_ID.get(id.toUpperCase().trim());
        return found != null ? found : ALL_DESIGNS.get(0);
    }

    static {
        initKeywords();

        // 1. None
        register("NONE", "Minimalist Solid", "Minimal", (g, w, h, c) -> {});

        // --- NATURE & BOTANICAL (2 - 10) ---
        register("FLOWERS", "Blossom Bouquet", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            // 5 sakura petals with layered outlines
            for (int i = 0; i < 5; i++) {
                double a = i * 2 * Math.PI / 5 - Math.PI / 2;
                int px = (int)(cx + 25 * Math.cos(a));
                int py = (int)(cy + 25 * Math.sin(a));
                g.setColor(new Color(255, 255, 255, 65));
                g.fillOval(px - 15, py - 15, 30, 30);
                g.setColor(new Color(255, 255, 255, 110));
                g.setStroke(new BasicStroke(1.2f));
                g.drawOval(px - 15, py - 15, 30, 30);
            }
            // Central stamen core
            g.setColor(new Color(255, 255, 255, 160));
            g.fillOval(cx - 10, cy - 10, 20, 20);
            g.setColor(new Color(255, 255, 255, 220));
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5;
                int x2 = (int)(cx + 14 * Math.cos(a));
                int y2 = (int)(cy + 14 * Math.sin(a));
                g.drawLine(cx, cy, x2, y2);
                g.fillOval(x2 - 2, y2 - 2, 4, 4);
            }
            // Drifting petals
            g.setColor(new Color(255, 255, 255, 75));
            g.fillOval(cx - 36, cy + 30, 10, 6);
            g.fillOval(cx + 34, cy - 36, 8, 5);
            g.fillOval(cx + 38, cy + 28, 12, 7);
        });

        register("ROSE", "English Rose", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setStroke(new BasicStroke(2.2f));
            g.setColor(new Color(255, 255, 255, 75));
            for (int r = 8; r <= 42; r += 7) {
                g.draw(new Arc2D.Float(cx - r, cy - r, r * 2, r * 2, r * 23, 270, Arc2D.OPEN));
            }
            g.setColor(new Color(255, 255, 255, 140));
            g.fillOval(cx - 6, cy - 6, 12, 12);
            // Leaf flourish
            Path2D leaf = new Path2D.Float();
            leaf.moveTo(cx + 34, cy + 20);
            leaf.curveTo(cx + 52, cy + 12, cx + 55, cy + 32, cx + 34, cy + 20);
            g.setColor(new Color(255, 255, 255, 70));
            g.fill(leaf);
        });

        register("LOTUS", "Sacred Lotus", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 5;
            g.setColor(new Color(255, 255, 255, 70));
            // Center petal
            Path2D p0 = new Path2D.Float();
            p0.moveTo(cx, cy - 36);
            p0.curveTo(cx - 16, cy - 10, cx - 18, cy + 12, cx, cy + 16);
            p0.curveTo(cx + 18, cy + 12, cx + 16, cy - 10, cx, cy - 36);
            g.fill(p0);
            // Left petal
            Path2D p1 = new Path2D.Float();
            p1.moveTo(cx, cy + 16);
            p1.curveTo(cx - 36, cy + 10, cx - 44, cy - 18, cx - 20, cy - 25);
            p1.curveTo(cx - 24, cy - 5, cx - 14, cy + 8, cx, cy + 16);
            g.fill(p1);
            // Right petal
            Path2D p2 = new Path2D.Float();
            p2.moveTo(cx, cy + 16);
            p2.curveTo(cx + 36, cy + 10, cx + 44, cy - 18, cx + 20, cy - 25);
            p2.curveTo(cx + 24, cy - 5, cx + 14, cy + 8, cx, cy + 16);
            g.fill(p2);
            // Water ripples underneath
            g.setColor(new Color(255, 255, 255, 90));
            g.setStroke(new BasicStroke(1.5f));
            g.drawArc(cx - 30, cy + 18, 60, 10, 0, -180);
            g.drawArc(cx - 42, cy + 24, 84, 12, 0, -180);
        });

        register("LEAF_FOLIAGE", "Monstera Leaf", "Nature", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            int cx = w / 2, cy = h / 2 - 10;
            g.fillOval(cx - 25, cy - 35, 50, 70);
            g.setColor(c);
            g.drawLine(cx, cy - 35, cx, cy + 35);
            g.drawLine(cx - 18, cy - 10, cx + 18, cy - 10);
            g.drawLine(cx - 20, cy + 10, cx + 20, cy + 10);
        });

        register("BAMBOO", "Zen Bamboo", "Nature", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 50));
            g.setStroke(new BasicStroke(4.0f));
            for (int x = w / 2 - 30; x <= w / 2 + 30; x += 30) {
                g.drawLine(x, 20, x, h - 20);
                for (int y = 40; y < h - 20; y += 45) {
                    g.drawLine(x - 8, y, x + 8, y);
                }
            }
        });

        register("FOREST_PINE", "Pine Woodland", "Nature", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 50));
            int cx = w / 2, cy = h / 2 - 10;
            for (int dx = -25; dx <= 25; dx += 25) {
                int[] px = {cx + dx, cx + dx - 18, cx + dx + 18};
                int[] py = {cy - 20, cy + 25, cy + 25};
                g.fillPolygon(px, py, 3);
            }
        });

        register("MOUNTAIN_PEAKS", "Alpine Crests", "Nature", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 60));
            int cx = w / 2, cy = h / 2 + 10;
            int[] px1 = {cx - 45, cx - 10, cx + 25};
            int[] py1 = {cy + 25, cy - 30, cy + 25};
            g.fillPolygon(px1, py1, 3);
            int[] px2 = {cx - 5, cx + 30, cx + 65};
            int[] py2 = {cy + 25, cy - 15, cy + 25};
            g.fillPolygon(px2, py2, 3);
        });

        register("OCEAN_TIDE", "Ocean Waves", "Nature", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(2.5f));
            for (int y = h / 2 - 35; y <= h / 2 + 35; y += 22) {
                Path2D p = new Path2D.Float();
                p.moveTo(15, y);
                p.curveTo(w * 0.3f, y - 18, w * 0.7f, y + 18, w - 15, y);
                g.draw(p);
            }
        });

        register("TREE_OF_LIFE", "Tree of Life", "Nature", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 60));
            int cx = w / 2, cy = h / 2 - 5;
            g.fillOval(cx - 30, cy - 35, 60, 50);
            g.fillRect(cx - 5, cy, 10, 35);
            g.drawArc(cx - 35, cy + 25, 70, 20, 0, 180);
        });

        register("RAINDROPS", "Rain Shower", "Nature", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(2.0f));
            for (int x = 20; x < w - 20; x += 22) {
                for (int y = 25; y < h - 25; y += 35) {
                    g.drawLine(x, y, x - 8, y + 16);
                }
            }
        });

        // --- CELESTIAL & SKY (11 - 16) ---
        register("SUN", "Radiant Sun", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            // Halo ring
            g.setColor(new Color(255, 255, 255, 45));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(cx - 36, cy - 36, 72, 72);

            // 16 Dual-length solar rays
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                boolean isMajor = (i % 2 == 0);
                int r1 = 20;
                int r2 = isMajor ? 44 : 32;

                int x1 = (int)(cx + r1 * Math.cos(a));
                int y1 = (int)(cy + r1 * Math.sin(a));
                int x2 = (int)(cx + r2 * Math.cos(a));
                int y2 = (int)(cy + r2 * Math.sin(a));

                g.setColor(new Color(255, 255, 255, isMajor ? 95 : 60));
                g.setStroke(new BasicStroke(isMajor ? 2.4f : 1.5f));
                g.drawLine(x1, y1, x2, y2);

                if (isMajor) {
                    g.setColor(new Color(255, 255, 255, 140));
                    g.fillOval(x2 - 3, y2 - 3, 6, 6);
                }
            }

            // Central radiant core
            g.setColor(new Color(255, 255, 255, 90));
            g.fillOval(cx - 16, cy - 16, 32, 32);
            g.setColor(new Color(255, 255, 255, 180));
            g.drawOval(cx - 16, cy - 16, 32, 32);
            g.fillOval(cx - 8, cy - 8, 16, 16);
        });

        register("SUNRISE", "Dawn Horizon", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 + 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.fillArc(cx - 32, cy - 32, 64, 64, 0, 180);
            g.setStroke(new BasicStroke(2.2f));
            g.drawLine(15, cy, w - 15, cy);

            // Radiant dawn fan rays
            for (int i = 1; i < 8; i++) {
                double a = i * Math.PI / 8;
                int x = (int)(cx + 48 * Math.cos(a));
                int y = (int)(cy - 48 * Math.sin(a));
                g.setColor(new Color(255, 255, 255, 75));
                g.drawLine(cx, cy, x, y);
            }

            // Water reflection ripples
            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(cx - 24, cy + 8, cx + 24, cy + 8);
            g.drawLine(cx - 16, cy + 16, cx + 16, cy + 16);
            g.drawLine(cx - 8, cy + 24, cx + 8, cy + 24);
        });

        register("SUNBURST", "Solar Starburst", "Celestial", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 65));
            int cx = w / 2, cy = h / 2 - 10;
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                int len = (i % 2 == 0) ? 45 : 28;
                int x = (int)(cx + len * Math.cos(a));
                int y = (int)(cy + len * Math.sin(a));
                g.drawLine(cx, cy, x, y);
            }
        });

        register("CRESCENT_MOON", "Lunar Crescent", "Celestial", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 80));
            int cx = w / 2, cy = h / 2 - 10;
            Area a1 = new Area(new Ellipse2D.Float(cx - 25, cy - 25, 50, 50));
            Area a2 = new Area(new Ellipse2D.Float(cx - 15, cy - 30, 48, 48));
            a1.subtract(a2);
            g.fill(a1);
            // Twinkling stars
            g.fillOval(cx + 15, cy - 12, 4, 4);
            g.fillOval(cx + 25, cy + 4, 6, 6);
            g.fillOval(cx + 8, cy + 20, 3, 3);
        });

        register("CONSTELLATION", "Star Map", "Celestial", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.5f));
            int[][] pts = {{w/2-30, h/2-25}, {w/2-10, h/2-30}, {w/2+15, h/2-15}, {w/2+30, h/2+10}, {w/2+5, h/2+25}, {w/2-25, h/2+15}};
            for (int i = 0; i < pts.length - 1; i++) {
                g.drawLine(pts[i][0], pts[i][1], pts[i+1][0], pts[i+1][1]);
                g.fillOval(pts[i][0] - 3, pts[i][1] - 3, 6, 6);
            }
            g.fillOval(pts[pts.length - 1][0] - 3, pts[pts.length - 1][1] - 3, 6, 6);
        });

        register("GALAXY_SPIRAL", "Cosmic Vortex", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(2.0f));
            // Dual logarithmic spiral arms
            for (int arm = 0; arm < 2; arm++) {
                double offset = arm * Math.PI;
                for (double t = 0; t < 12; t += 0.3) {
                    double r = t * 3.4;
                    int x = (int)(cx + r * Math.cos(t + offset));
                    int y = (int)(cy + r * Math.sin(t + offset));
                    g.fillOval(x, y, 3, 3);
                }
            }
            // Dense core
            g.setColor(new Color(255, 255, 255, 140));
            g.fillOval(cx - 7, cy - 7, 14, 14);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval(cx - 3, cy - 3, 6, 6);
        });

        // --- CYBER, TECH & ROBOTS (17 - 23) ---
        register("ROBOT", "Cyber Android", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setStroke(new BasicStroke(2.0f));

            // Outer Helm Armor Plate
            g.setColor(new Color(255, 255, 255, 80));
            g.drawRoundRect(cx - 28, cy - 28, 56, 56, 14, 14);

            // Forehead crest gem
            Polygon crest = new Polygon(
                    new int[]{cx, cx + 8, cx, cx - 8},
                    new int[]{cy - 24, cy - 16, cy - 8, cy - 16}, 4
            );
            g.setColor(new Color(255, 255, 255, 120));
            g.fill(crest);

            // Panoramic Optical Visor Bar
            g.setColor(new Color(255, 255, 255, 140));
            g.fillRoundRect(cx - 20, cy - 4, 40, 10, 4, 4);
            g.setColor(new Color(255, 255, 255, 220));
            g.drawLine(cx - 16, cy + 1, cx + 16, cy + 1);

            // Chin ventilation grille
            g.setColor(new Color(255, 255, 255, 90));
            for (int y = cy + 14; y <= cy + 22; y += 4) {
                g.drawLine(cx - 12, y, cx + 12, y);
            }

            // Side Audio / Node Dials
            g.setColor(new Color(255, 255, 255, 80));
            g.drawOval(cx - 36, cy - 8, 8, 16);
            g.drawOval(cx + 28, cy - 8, 8, 16);

            // Top Antenna Array
            g.drawLine(cx, cy - 28, cx, cy - 42);
            g.fillOval(cx - 4, cy - 46, 8, 8);
            g.drawLine(cx - 18, cy - 28, cx - 24, cy - 38);
            g.drawLine(cx + 18, cy - 28, cx + 24, cy - 38);
        });

        register("CYBER_CIRCUIT", "PCB Circuit", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setStroke(new BasicStroke(1.8f));

            // Central Quad Micro-Die
            g.setColor(new Color(255, 255, 255, 95));
            g.drawRoundRect(cx - 16, cy - 16, 32, 32, 6, 6);
            g.setColor(new Color(255, 255, 255, 55));
            g.fillRect(cx - 10, cy - 10, 20, 20);

            // Bus Routes & Vias
            g.setColor(new Color(255, 255, 255, 80));
            // North-West bus
            g.drawLine(cx - 16, cy - 10, cx - 35, cy - 10);
            g.drawLine(cx - 35, cy - 10, cx - 48, cy - 24);
            g.drawLine(cx - 48, cy - 24, cx - 48, cy - 40);
            g.fillOval(cx - 51, cy - 44, 7, 7);

            // North-East bus
            g.drawLine(cx + 16, cy - 10, cx + 32, cy - 10);
            g.drawLine(cx + 32, cy - 10, cx + 46, cy - 24);
            g.drawLine(cx + 46, cy - 24, cx + 54, cy - 24);
            g.fillOval(cx + 53, cy - 27, 7, 7);

            // South-West bus
            g.drawLine(cx - 10, cy + 16, cx - 10, cy + 32);
            g.drawLine(cx - 10, cy + 32, cx - 26, cy + 48);
            g.fillOval(cx - 30, cy + 47, 7, 7);

            // South-East bus
            g.drawLine(cx + 10, cy + 16, cx + 10, cy + 28);
            g.drawLine(cx + 10, cy + 28, cx + 32, cy + 28);
            g.drawLine(cx + 32, cy + 28, cx + 46, cy + 42);
            g.fillOval(cx + 44, cy + 41, 7, 7);
        });

        register("MICROCHIP", "Silicon Die", "Tech", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            int cx = w / 2, cy = h / 2 - 10;
            g.drawRect(cx - 24, cy - 24, 48, 48);
            g.fillRect(cx - 14, cy - 14, 28, 28);
            for (int i = -18; i <= 18; i += 9) {
                g.drawLine(cx + i, cy - 24, cx + i, cy - 32);
                g.drawLine(cx + i, cy + 24, cx + i, cy + 32);
                g.drawLine(cx - 24, cy + i, cx - 32, cy + i);
                g.drawLine(cx + 24, cy + i, cx + 32, cy + i);
            }
        });

        register("GEAR_MATRIX", "Clockwork Gears", "Tech", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 65));
            int cx = w / 2, cy = h / 2 - 10;
            g.drawOval(cx - 22, cy - 22, 44, 44);
            g.drawOval(cx - 10, cy - 10, 20, 20);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int x1 = (int)(cx + 20 * Math.cos(a));
                int y1 = (int)(cy + 20 * Math.sin(a));
                int x2 = (int)(cx + 28 * Math.cos(a));
                int y2 = (int)(cy + 28 * Math.sin(a));
                g.drawLine(x1, y1, x2, y2);
            }
        });

        register("ATOM_ORBITS", "Quantum Atom", "Tech", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(1.8f));
            int cx = w / 2, cy = h / 2 - 10;
            g.fillOval(cx - 6, cy - 6, 12, 12);
            g.draw(new Ellipse2D.Float(cx - 30, cy - 12, 60, 24));
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(60), cx, cy);
            g.draw(new Ellipse2D.Float(cx - 30, cy - 12, 60, 24));
            g.rotate(Math.toRadians(60), cx, cy);
            g.draw(new Ellipse2D.Float(cx - 30, cy - 12, 60, 24));
            g.setTransform(old);
        });

        register("DNA_HELIX", "DNA Strand", "Tech", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(2.0f));
            int cx = w / 2;
            for (int y = h / 2 - 35; y <= h / 2 + 35; y += 10) {
                int dx = (int)(22 * Math.sin(y * 0.15));
                g.drawLine(cx - dx, y, cx + dx, y);
                g.fillOval(cx - dx - 2, y - 2, 5, 5);
                g.fillOval(cx + dx - 2, y - 2, 5, 5);
            }
        });

        register("TELESCOPE", "Observatory", "Tech", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            int cx = w / 2, cy = h / 2 - 10;
            g.drawLine(cx - 20, cy + 20, cx + 25, cy - 25);
            g.drawRect(cx - 15, cy + 10, 10, 8);
            g.drawLine(cx, cy, cx - 18, cy + 35);
            g.drawLine(cx, cy, cx + 18, cy + 35);
        });

        // --- GEOMETRIC PATTERNS (24 - 33) ---
        register("HEXAGONS", "Honeycomb Hive", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 45));
            g.setStroke(new BasicStroke(1.5f));
            int s = 16;
            for (int y = 20; y < h - 20; y += 26) {
                for (int x = 20; x < w - 20; x += 30) {
                    Polygon hex = new Polygon();
                    for (int i = 0; i < 6; i++) {
                        hex.addPoint((int)(x + s * Math.cos(i * Math.PI / 3)),
                                     (int)(y + s * Math.sin(i * Math.PI / 3)));
                    }
                    g.drawPolygon(hex);
                }
            }
        });

        register("TRIANGLES", "Delta Mosaic", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 45));
            for (int y = 20; y < h - 20; y += 30) {
                for (int x = 20; x < w - 20; x += 30) {
                    int[] px = {x, x + 15, x - 15};
                    int[] py = {y - 15, y + 15, y + 15};
                    g.drawPolygon(px, py, 3);
                }
            }
        });

        register("DIAMONDS", "Argyle Harlequin", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 45));
            for (int y = 25; y < h - 20; y += 32) {
                for (int x = 25; x < w - 20; x += 32) {
                    int[] px = {x, x + 14, x, x - 14};
                    int[] py = {y - 14, y, y + 14, y};
                    g.drawPolygon(px, py, 4);
                }
            }
        });

        register("CHEVRON", "Nordic Weave", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 50));
            g.setStroke(new BasicStroke(2.5f));
            for (int y = 25; y < h - 20; y += 22) {
                Path2D p = new Path2D.Float();
                p.moveTo(15, y);
                p.lineTo(w / 2, y + 14);
                p.lineTo(w - 15, y);
                g.draw(p);
            }
        });

        register("CONCENTRIC_RINGS", "Ripple Radar", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(1.8f));
            int cx = w / 2, cy = h / 2 - 10;
            for (int r = 10; r <= 50; r += 10) {
                g.drawOval(cx - r, cy - r, r * 2, r * 2);
            }
        });

        register("ISOMETRIC_CUBES", "Escher Cubes", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 50));
            int cx = w / 2, cy = h / 2 - 10, sz = 24;
            g.drawRect(cx - sz, cy - sz, sz * 2, sz * 2);
            g.drawLine(cx - sz, cy - sz, cx + sz, cy + sz);
            g.drawLine(cx + sz, cy - sz, cx - sz, cy + sz);
        });

        register("DOT_MATRIX", "Halftone Array", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            for (int x = 20; x < w - 20; x += 14) {
                for (int y = 20; y < h - 20; y += 14) {
                    g.fillOval(x - 2, y - 2, 4, 4);
                }
            }
        });

        register("CROSSHATCH", "Fine Crosshatch", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 45));
            int pad = 10;
            Shape oldClip = g.getClip();
            g.clipRect(pad, pad, w - pad * 2, h - pad * 2);
            for (int i = -h; i < w + h; i += 16) {
                g.drawLine(i, pad, i + h, h - pad);
                g.drawLine(i, h - pad, i + h, pad);
            }
            g.setClip(oldClip);
        });

        register("BLUEPRINT_GRID", "Drafting Grid", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 35));
            for (int x = 15; x < w - 15; x += 18) g.drawLine(x, 0, x, h);
            for (int y = 15; y < h - 15; y += 18) g.drawLine(0, y, w, y);
        });

        register("BAUHAUS_SHAPES", "Bauhaus Geometry", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 60));
            int cx = w / 2, cy = h / 2 - 10;
            g.fillOval(cx - 30, cy - 30, 28, 28);
            g.fillRect(cx + 4, cy - 30, 26, 26);
            int[] px = {cx, cx - 24, cx + 24};
            int[] py = {cy + 6, cy + 34, cy + 34};
            g.fillPolygon(px, py, 3);
        });

        // --- ARTS, CULTURE & SYMBOLS (34 - 45) ---
        register("QUILL_AND_INK", "Author Quill", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 75));
            int cx = w / 2, cy = h / 2 - 10;
            Path2D p = new Path2D.Float();
            p.moveTo(cx - 20, cy + 25);
            p.curveTo(cx - 15, cy - 10, cx + 15, cy - 30, cx + 30, cy - 35);
            p.curveTo(cx + 10, cy - 20, cx - 5, cy + 5, cx - 20, cy + 25);
            g.fill(p);
            g.fillRect(cx - 22, cy + 24, 6, 8);
        });

        register("OPEN_BOOK", "Tome of Wisdom", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(2.0f));
            int cx = w / 2, cy = h / 2 - 10;
            Path2D p = new Path2D.Float();
            p.moveTo(cx, cy + 18);
            p.curveTo(cx - 15, cy + 14, cx - 30, cy + 18, cx - 35, cy + 14);
            p.lineTo(cx - 35, cy - 16);
            p.curveTo(cx - 30, cy - 12, cx - 15, cy - 16, cx, cy - 12);
            p.curveTo(cx + 15, cy - 16, cx + 30, cy - 12, cx + 35, cy - 16);
            p.lineTo(cx + 35, cy + 14);
            p.curveTo(cx + 30, cy + 18, cx + 15, cy + 14, cx, cy + 18);
            g.draw(p);
            g.drawLine(cx, cy - 12, cx, cy + 18);
        });

        register("COMPASS_ROSE", "Navigator Compass", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            int cx = w / 2, cy = h / 2 - 10;
            g.drawOval(cx - 28, cy - 28, 56, 56);
            int[] px = {cx, cx + 7, cx, cx - 7};
            int[] py = {cy - 35, cy, cy + 35, cy};
            g.fillPolygon(px, py, 4);
            int[] px2 = {cx - 35, cx, cx + 35, cx};
            int[] py2 = {cy, cy + 7, cy, cy - 7};
            g.fillPolygon(px2, py2, 4);
        });

        register("PALETTE_BRUSH", "Painter Palette", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            int cx = w / 2, cy = h / 2 - 10;
            g.drawOval(cx - 28, cy - 25, 56, 50);
            g.fillOval(cx - 15, cy - 12, 6, 6);
            g.fillOval(cx, cy - 16, 6, 6);
            g.fillOval(cx + 14, cy - 10, 6, 6);
            g.fillOval(cx + 12, cy + 6, 8, 8);
        });

        register("MUSIC_NOTES", "Melodic Staff", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 75));
            int cx = w / 2, cy = h / 2 - 10;
            g.fillOval(cx - 20, cy + 10, 12, 8);
            g.fillOval(cx + 8, cy + 4, 12, 8);
            g.fillRect(cx - 10, cy - 18, 3, 30);
            g.fillRect(cx + 18, cy - 24, 3, 30);
            g.fillRect(cx - 10, cy - 24, 31, 6);
        });

        register("ORIGAMI_CRANE", "Origami Bird", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            int cx = w / 2, cy = h / 2 - 10;
            Path2D p = new Path2D.Float();
            p.moveTo(cx, cy + 20);
            p.lineTo(cx - 35, cy - 10);
            p.lineTo(cx, cy - 25);
            p.lineTo(cx + 35, cy - 10);
            p.closePath();
            g.draw(p);
            g.drawLine(cx, cy - 25, cx, cy + 20);
        });

        register("CHESS_KNIGHT", "Grandmaster Knight", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            int cx = w / 2, cy = h / 2 - 10;
            Path2D p = new Path2D.Float();
            p.moveTo(cx - 15, cy + 25);
            p.lineTo(cx + 15, cy + 25);
            p.lineTo(cx + 12, cy + 15);
            p.curveTo(cx + 18, cy - 5, cx + 8, cy - 25, cx - 10, cy - 25);
            p.curveTo(cx - 20, cy - 15, cx - 18, cy + 15, cx - 15, cy + 25);
            g.draw(p);
            g.fillOval(cx - 4, cy - 18, 4, 4);
        });

        register("CELTIC_KNOT", "Celtic Loop", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(3.0f));
            int cx = w / 2, cy = h / 2 - 10;
            g.drawRoundRect(cx - 22, cy - 22, 44, 44, 18, 18);
            g.drawOval(cx - 16, cy - 16, 32, 32);
        });

        register("ZEN_MANDALA", "Mandala Bloom", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(1.5f));
            int cx = w / 2, cy = h / 2 - 10;
            for (int r = 8; r <= 36; r += 12) {
                g.drawOval(cx - r, cy - r, r * 2, r * 2);
            }
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                g.drawLine(cx, cy, (int)(cx + 40 * Math.cos(a)), (int)(cy + 40 * Math.sin(a)));
            }
        });

        register("GOLDEN_SPIRAL", "Fibonacci Curve", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(2.0f));
            int cx = w / 2 + 10, cy = h / 2 + 10;
            g.draw(new Arc2D.Float(cx - 50, cy - 50, 100, 100, 0, 90, Arc2D.OPEN));
            g.draw(new Arc2D.Float(cx - 30, cy - 30, 60, 60, 90, 90, Arc2D.OPEN));
            g.draw(new Arc2D.Float(cx - 18, cy - 18, 36, 36, 180, 90, Arc2D.OPEN));
            g.draw(new Arc2D.Float(cx - 10, cy - 10, 20, 20, 270, 90, Arc2D.OPEN));
        });

        register("SHIELD_HERALDIC", "Knight Crest", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 70));
            int cx = w / 2, cy = h / 2 - 10;
            Path2D p = new Path2D.Float();
            p.moveTo(cx - 24, cy - 25);
            p.lineTo(cx + 24, cy - 25);
            p.lineTo(cx + 24, cy + 5);
            p.curveTo(cx + 20, cy + 25, cx, cy + 35, cx, cy + 35);
            p.curveTo(cx, cy + 35, cx - 20, cy + 25, cx - 24, cy + 5);
            p.closePath();
            g.draw(p);
            g.drawLine(cx, cy - 25, cx, cy + 35);
            g.drawLine(cx - 24, cy + 5, cx + 24, cy + 5);
        });

        register("PRISM_SPECTRUM", "Prism Facets", "Arts", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 65));
            int cx = w / 2, cy = h / 2 - 10;
            int[] px = {cx, cx - 35, cx + 35};
            int[] py = {cy - 30, cy + 25, cy + 25};
            g.drawPolygon(px, py, 3);
            g.drawLine(cx, cy - 30, cx, cy + 25);
        });

        // --- TEXTURES & SURFACES (46 - 55) ---
        register("LEATHER_GRAIN", "Moroccan Grain", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 25));
            Random rnd = new Random(42);
            for (int i = 0; i < 280; i++) {
                int rx = rnd.nextInt(w);
                int ry = rnd.nextInt(h);
                g.fillOval(rx, ry, 2, 2);
            }
        });

        register("MARBLE_SWIRLS", "Florentine Marble", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 40));
            g.setStroke(new BasicStroke(1.8f));
            for (int y = 24; y < h - 16; y += 34) {
                Path2D p = new Path2D.Float();
                p.moveTo(8, y);
                p.curveTo(w * 0.3f, y + 22, w * 0.7f, y - 22, w - 8, y + 8);
                g.draw(p);
            }
        });

        register("CARBON_FIBER", "Carbon Twill", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(0, 0, 0, 45));
            int pad = 8;
            for (int x = pad; x < w - pad - 6; x += 12) {
                for (int y = pad; y < h - pad - 6; y += 12) {
                    if ((x + y) % 24 == 0) g.fillRect(x, y, 6, 6);
                }
            }
        });

        register("LINEN_CANVAS", "Woven Linen", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 28));
            int pad = 8;
            for (int x = pad; x <= w - pad; x += 6) g.drawLine(x, pad, x, h - pad);
            for (int y = pad; y <= h - pad; y += 6) g.drawLine(pad, y, w - pad, y);
        });

        register("WOOD_GRAIN", "Timber Rings", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 45));
            g.setStroke(new BasicStroke(1.5f));
            int cx = w / 2, cy = h / 2 - 10;
            int maxR = Math.min(w, h) / 2 - 12;
            for (int r = 12; r <= maxR; r += 11) {
                g.draw(new Arc2D.Float(cx - r, cy - r, r * 2, r * 2, 0, 360, Arc2D.OPEN));
            }
        });

        register("RETRO_GRID", "Synthwave Horizon", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            int hy = h / 2 + 5;
            // Sun on horizon
            g.drawArc(w / 2 - 22, hy - 22, 44, 44, 0, 180);
            g.drawLine(10, hy, w - 10, hy);
            for (int y = hy + 6; y < h - 10; y += 8) {
                g.drawLine(10, y, w - 10, y);
            }
            int bottomMargin = 12;
            int step = (w - 2 * bottomMargin) / 6;
            for (int x = bottomMargin; x <= w - bottomMargin; x += step) {
                g.drawLine(w / 2, hy, x, h - 10);
            }
        });

        register("TOPOGRAPHIC_MAP", "Elevation Contours", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 40));
            g.setStroke(new BasicStroke(1.5f));
            int cx = w / 2, cy = h / 2 - 10;
            g.drawOval(cx - 15, cy - 10, 30, 20);
            g.drawOval(cx - 30, cy - 22, 60, 44);
            g.drawOval(cx - 45, cy - 35, 90, 70);
            g.drawOval(cx - 60, cy - 48, 120, 96);
        });

        register("CLOUDS", "Cumulus Drift", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            int cx = w / 2, cy = h / 2 - 10;
            g.fillOval(cx - 30, cy - 5, 26, 26);
            g.fillOval(cx - 12, cy - 20, 32, 32);
            g.fillOval(cx + 8, cy - 8, 24, 24);
            g.fillRect(cx - 25, cy + 5, 50, 16);
        });

        register("STRIPES_MINIMAL", "Minimalist Pinstripe", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 45));
            g.setStroke(new BasicStroke(2.0f));
            int cx = w / 2;
            g.drawLine(cx - 12, 10, cx - 12, h - 10);
            g.drawLine(cx - 4, 10, cx - 4, h - 10);
            g.drawLine(cx + 4, 10, cx + 4, h - 10);
            g.drawLine(cx + 12, 10, cx + 12, h - 10);
        });

        // ============================================================
        // 50+ NEW CREATIVE VECTOR DESIGNS (TOTAL: 108 DESIGNS)
        // ============================================================

        // --- EXPANDED NATURE & BOTANICAL ---
        register("SUNFLOWER", "Golden Sunflower", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 65));
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                int x1 = (int)(cx + 20 * Math.cos(a));
                int y1 = (int)(cy + 20 * Math.sin(a));
                int x2 = (int)(cx + 36 * Math.cos(a));
                int y2 = (int)(cy + 36 * Math.sin(a));
                g.fillOval(x2 - 5, y2 - 5, 10, 10);
                g.drawLine(x1, y1, x2, y2);
            }
            g.setColor(new Color(255, 255, 255, 120));
            g.fillOval(cx - 14, cy - 14, 28, 28);
            g.setColor(new Color(255, 255, 255, 180));
            g.drawOval(cx - 14, cy - 14, 28, 28);
        });

        register("MAPLE_LEAF", "Autumn Maple", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 70));
            Path2D m = new Path2D.Float();
            m.moveTo(cx, cy - 35);
            m.lineTo(cx + 8, cy - 20);
            m.lineTo(cx + 25, cy - 24);
            m.lineTo(cx + 18, cy - 8);
            m.lineTo(cx + 35, cy + 5);
            m.lineTo(cx + 15, cy + 12);
            m.lineTo(cx + 22, cy + 28);
            m.lineTo(cx + 6, cy + 22);
            m.lineTo(cx, cy + 34);
            m.lineTo(cx - 6, cy + 22);
            m.lineTo(cx - 22, cy + 28);
            m.lineTo(cx - 15, cy + 12);
            m.lineTo(cx - 35, cy + 5);
            m.lineTo(cx - 18, cy - 8);
            m.lineTo(cx - 25, cy - 24);
            m.lineTo(cx - 8, cy - 20);
            m.closePath();
            g.fill(m);
            g.drawLine(cx, cy + 34, cx, cy + 44);
        });

        register("GINKGO", "Ginkgo Biloba", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            Path2D p = new Path2D.Float();
            p.moveTo(cx, cy + 25);
            p.curveTo(cx - 40, cy + 10, cx - 45, cy - 25, cx, cy - 32);
            p.curveTo(cx + 45, cy - 25, cx + 40, cy + 10, cx, cy + 25);
            g.fill(p);
            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(1.5f));
            for (int dx = -24; dx <= 24; dx += 8) {
                g.drawLine(cx, cy + 25, cx + dx, cy - 26);
            }
            g.drawLine(cx, cy + 25, cx, cy + 40);
        });

        register("FERN_FROND", "Forest Fern", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(2.0f));
            g.drawArc(cx - 30, cy - 35, 60, 80, 90, -140);
            for (int y = cy - 28; y <= cy + 30; y += 8) {
                int dx = (int)(22 * Math.cos((y - cy) * 0.05));
                g.drawLine(cx - 5, y, cx - 5 + dx, y - 6);
                g.drawLine(cx - 5, y, cx - 5 - dx, y - 6);
            }
        });

        register("DANDELION", "Dandelion Whisps", "Nature", (g, w, h, c) -> {
            int cx = w / 2 - 8, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 20; i++) {
                double a = i * Math.PI / 10;
                int x = (int)(cx + 30 * Math.cos(a));
                int y = (int)(cy + 30 * Math.sin(a));
                g.drawLine(cx, cy, x, y);
                g.fillOval(x - 2, y - 2, 4, 4);
            }
            g.drawLine(cx, cy, cx - 12, cy + 42);
            // Floating parachute seeds
            g.fillOval(cx + 34, cy - 32, 5, 5);
            g.drawLine(cx + 34, cy - 32, cx + 42, cy - 40);
            g.fillOval(cx + 42, cy - 12, 4, 4);
            g.drawLine(cx + 42, cy - 12, cx + 50, cy - 18);
        });

        register("CACTUS_DESERT", "Saguaro Desert", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(2.2f));
            // Main trunk
            g.drawRoundRect(cx - 8, cy - 35, 16, 75, 10, 10);
            // Left arm
            g.drawLine(cx - 8, cy + 5, cx - 22, cy + 5);
            g.drawLine(cx - 22, cy + 5, cx - 22, cy - 16);
            // Right arm
            g.drawLine(cx + 8, cy - 5, cx + 24, cy - 5);
            g.drawLine(cx + 24, cy - 5, cx + 24, cy - 24);
        });

        register("WILLOW_TREE", "Weeping Willow", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(1.8f));
            g.drawLine(cx, cy - 15, cx, cy + 35);
            for (int i = -3; i <= 3; i++) {
                int ox = i * 9;
                Path2D b = new Path2D.Float();
                b.moveTo(cx, cy - 15);
                b.curveTo(cx + ox * 1.5f, cy - 25, cx + ox * 2.2f, cy + 10, cx + ox * 2.2f, cy + 30);
                g.draw(b);
            }
        });

        register("ORCHID", "Wild Orchid", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.fillOval(cx - 18, cy - 30, 36, 24);
            g.fillOval(cx - 32, cy - 10, 26, 32);
            g.fillOval(cx + 6, cy - 10, 26, 32);
            g.setColor(new Color(255, 255, 255, 130));
            g.fillOval(cx - 10, cy, 20, 22);
            g.setColor(new Color(255, 255, 255, 200));
            g.fillOval(cx - 4, cy - 6, 8, 8);
        });

        register("OAK_ACORN", "Royal Oak", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.fillArc(cx - 18, cy - 24, 36, 24, 0, 180);
            g.drawRoundRect(cx - 14, cy - 12, 28, 34, 18, 18);
            g.drawLine(cx, cy - 24, cx, cy - 34);
            g.setStroke(new BasicStroke(1.2f));
            for (int x = cx - 14; x <= cx + 14; x += 6) {
                g.drawLine(x, cy - 24, x, cy - 14);
            }
        });

        // --- EXPANDED CELESTIAL & SKY ---
        register("SATURN_RINGS", "Saturnian Orbit", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 85));
            g.fillOval(cx - 20, cy - 20, 40, 40);
            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(2.2f));
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(-25), cx, cy);
            g.draw(new Ellipse2D.Float(cx - 48, cy - 12, 96, 24));
            g.draw(new Ellipse2D.Float(cx - 56, cy - 15, 112, 30));
            g.setTransform(old);
        });

        register("ECLIPSE_RING", "Solar Eclipse", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 90));
            g.setStroke(new BasicStroke(2.8f));
            g.drawOval(cx - 28, cy - 28, 56, 56);
            // Diamond corona glint
            g.setColor(new Color(255, 255, 255, 220));
            int gx = cx + 20, gy = cy - 20;
            g.fillOval(gx - 6, gy - 6, 12, 12);
            g.drawLine(gx - 14, gy, gx + 14, gy);
            g.drawLine(gx, gy - 14, gx, gy + 14);
        });

        register("NORTHERN_AURORA", "Aurora Borealis", "Celestial", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(2.0f));
            for (int y = h / 2 - 40; y <= h / 2 + 20; y += 14) {
                Path2D p = new Path2D.Float();
                p.moveTo(15, y);
                p.curveTo(w * 0.3f, y - 24, w * 0.7f, y + 24, w - 15, y - 8);
                g.draw(p);
            }
            g.setColor(new Color(255, 255, 255, 90));
            g.fillOval(w / 2 - 25, h / 2 - 35, 4, 4);
            g.fillOval(w / 2 + 35, h / 2 - 20, 5, 5);
        });

        register("METEOR_SHOWER", "Shooting Stars", "Celestial", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.8f));
            int[][] meteors = {{w/2-30, h/2-40}, {w/2+10, h/2-25}, {w/2-10, h/2+5}, {w/2+35, h/2-5}};
            for (int[] m : meteors) {
                g.fillOval(m[0], m[1], 6, 6);
                g.drawLine(m[0], m[1], m[0] - 28, m[1] - 28);
            }
        });

        register("ASTROLABE", "Astrolabe Dial", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.8f));
            g.drawOval(cx - 36, cy - 36, 72, 72);
            g.drawOval(cx - 24, cy - 24, 48, 48);
            g.drawLine(cx, cy - 36, cx, cy + 36);
            g.drawLine(cx - 36, cy, cx + 36, cy);
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(45), cx, cy);
            g.draw(new Ellipse2D.Float(cx - 24, cy - 12, 48, 24));
            g.setTransform(old);
        });

        register("SUPERNOVA", "Cosmic Supernova", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < 32; i++) {
                double a = i * Math.PI / 16;
                int len = (i % 4 == 0) ? 46 : ((i % 2 == 0) ? 32 : 18);
                int x = (int)(cx + len * Math.cos(a));
                int y = (int)(cy + len * Math.sin(a));
                g.drawLine(cx, cy, x, y);
            }
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(cx - 8, cy - 8, 16, 16);
        });

        // --- EXPANDED TECH, CYBER & FUTURE ---
        register("NEURAL_NETWORK", "Neural Nodes", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(1.4f));
            int[][] layer1 = {{cx - 35, cy - 25}, {cx - 35, cy}, {cx - 35, cy + 25}};
            int[][] layer2 = {{cx, cy - 35}, {cx, cy - 12}, {cx, cy + 12}, {cx, cy + 35}};
            int[][] layer3 = {{cx + 35, cy - 20}, {cx + 35, cy + 20}};

            for (int[] p1 : layer1) {
                for (int[] p2 : layer2) g.drawLine(p1[0], p1[1], p2[0], p2[1]);
            }
            for (int[] p2 : layer2) {
                for (int[] p3 : layer3) g.drawLine(p2[0], p2[1], p3[0], p3[1]);
            }
            g.setColor(new Color(255, 255, 255, 160));
            for (int[] p : layer1) g.fillOval(p[0] - 4, p[1] - 4, 8, 8);
            for (int[] p : layer2) g.fillOval(p[0] - 4, p[1] - 4, 8, 8);
            for (int[] p : layer3) g.fillOval(p[0] - 4, p[1] - 4, 8, 8);
        });

        register("FIBER_OPTIC", "Fiber Matrix", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(1.8f));
            for (int i = -3; i <= 3; i++) {
                Path2D f = new Path2D.Float();
                f.moveTo(cx - 40, cy + 35);
                f.curveTo(cx - 10, cy + 10, cx + i * 14, cy - 10, cx + i * 16, cy - 35);
                g.draw(f);
                g.fillOval(cx + i * 16 - 3, cy - 38, 6, 6);
            }
        });

        register("NANOTECH_HEX", "Nanotech Lattice", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.8f));
            int r = 18;
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                int hx = (int)(cx + 28 * Math.cos(a));
                int hy = (int)(cy + 28 * Math.sin(a));
                Polygon poly = new Polygon();
                for (int j = 0; j < 6; j++) {
                    poly.addPoint((int)(hx + r * Math.cos(j * Math.PI / 3)),
                                  (int)(hy + r * Math.sin(j * Math.PI / 3)));
                }
                g.drawPolygon(poly);
            }
            g.fillOval(cx - 5, cy - 5, 10, 10);
        });

        register("DRONE_QUAD", "Autonomous Drone", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(2.2f));
            g.drawRect(cx - 12, cy - 12, 24, 24);
            g.drawLine(cx - 12, cy - 12, cx - 28, cy - 28);
            g.drawLine(cx + 12, cy - 12, cx + 28, cy - 28);
            g.drawLine(cx - 12, cy + 12, cx - 28, cy + 28);
            g.drawLine(cx + 12, cy + 12, cx + 28, cy + 28);
            // 4 rotors
            g.drawOval(cx - 36, cy - 36, 16, 16);
            g.drawOval(cx + 20, cy - 36, 16, 16);
            g.drawOval(cx - 36, cy + 20, 16, 16);
            g.drawOval(cx + 20, cy + 20, 16, 16);
            g.fillOval(cx - 4, cy - 4, 8, 8);
        });

        register("CYBER_SKULL", "Cyber Skull", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(2.0f));
            g.drawOval(cx - 24, cy - 28, 48, 40);
            g.drawRect(cx - 14, cy + 12, 28, 16);
            // Eye sockets
            g.fillOval(cx - 16, cy - 10, 10, 10);
            g.fillOval(cx + 6, cy - 10, 10, 10);
            // Cyber circuits
            g.drawLine(cx - 24, cy - 8, cx - 36, cy - 8);
            g.drawLine(cx + 24, cy - 8, cx + 36, cy - 8);
            for (int x = cx - 10; x <= cx + 10; x += 5) g.drawLine(x, cy + 12, x, cy + 28);
        });

        register("QUANTUM_WAVE", "Wave Function", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(2.0f));
            for (int f = 1; f <= 3; f++) {
                Path2D p = new Path2D.Float();
                p.moveTo(cx - 50, cy);
                for (int x = -50; x <= 50; x += 2) {
                    double y = 22 * Math.sin(x * 0.1 * f) * Math.exp(-Math.abs(x) * 0.02);
                    p.lineTo(cx + x, cy + y);
                }
                g.draw(p);
            }
        });

        register("AI_PROCESSOR", "AI Core Die", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(cx - 25, cy - 25, 50, 50, 10, 10);
            g.drawRect(cx - 15, cy - 15, 30, 30);
            g.drawString("AI", cx - 8, cy + 5);
            for (int i = -18; i <= 18; i += 9) {
                g.drawLine(cx + i, cy - 25, cx + i, cy - 35);
                g.drawLine(cx + i, cy + 25, cx + i, cy + 35);
                g.drawLine(cx - 25, cy + i, cx - 35, cy + i);
                g.drawLine(cx + 25, cy + i, cx + 35, cy + i);
            }
        });

        // --- EXPANDED GEOMETRIC & SACRED GEOMETRY ---
        register("METATRON_CUBE", "Metatron Cube", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(1.4f));
            int r = 32;
            int[][] pts = new int[6][2];
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                pts[i][0] = (int)(cx + r * Math.cos(a));
                pts[i][1] = (int)(cy + r * Math.sin(a));
            }
            for (int i = 0; i < 6; i++) {
                for (int j = i + 1; j < 6; j++) {
                    g.drawLine(pts[i][0], pts[i][1], pts[j][0], pts[j][1]);
                }
                g.drawOval(pts[i][0] - 6, pts[i][1] - 6, 12, 12);
            }
            g.drawOval(cx - 8, cy - 8, 16, 16);
        });

        register("FLOWER_OF_LIFE", "Flower of Life", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(1.5f));
            int r = 22;
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                int ox = (int)(cx + r * Math.cos(a));
                int oy = (int)(cy + r * Math.sin(a));
                g.drawOval(ox - r, oy - r, r * 2, r * 2);
            }
        });

        register("SIERPINSKI", "Sierpinski Delta", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.8f));
            int s = 45;
            int[] px = {cx, cx - s, cx + s};
            int[] py = {cy - s, cy + s, cy + s};
            g.drawPolygon(px, py, 3);
            int[] ix = {cx, cx - s/2, cx + s/2};
            int[] iy = {cy + s, cy, cy};
            g.drawPolygon(ix, iy, 3);
        });

        register("PENROSE_TILING", "Penrose Rhombus", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5;
                int x1 = (int)(cx + 16 * Math.cos(a));
                int y1 = (int)(cy + 16 * Math.sin(a));
                int x2 = (int)(cx + 38 * Math.cos(a));
                int y2 = (int)(cy + 38 * Math.sin(a));
                g.drawLine(cx, cy, x2, y2);
                g.drawLine(x1, y1, x2, y2);
            }
        });

        register("GREEK_KEY", "Meander Key", "Geometric", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(2.0f));
            for (int y = 30; y < h - 30; y += 45) {
                Path2D k = new Path2D.Float();
                for (int x = 20; x < w - 30; x += 36) {
                    k.moveTo(x, y);
                    k.lineTo(x + 24, y);
                    k.lineTo(x + 24, y + 20);
                    k.lineTo(x + 8, y + 20);
                    k.lineTo(x + 8, y + 8);
                    k.lineTo(x + 16, y + 8);
                }
                g.draw(k);
            }
        });

        register("VORONOI_CELLS", "Voronoi Network", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(1.6f));
            int[][] pts = {{cx-20, cy-25}, {cx+20, cy-25}, {cx+35, cy}, {cx+15, cy+30}, {cx-25, cy+25}, {cx-35, cy}};
            for (int i = 0; i < pts.length; i++) {
                g.drawLine(cx, cy, pts[i][0], pts[i][1]);
                int next = (i + 1) % pts.length;
                g.drawLine(pts[i][0], pts[i][1], pts[next][0], pts[next][1]);
            }
        });

        register("GOTHIC_TREFOIL", "Gothic Trefoil", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(2.0f));
            g.drawOval(cx - 15, cy - 35, 30, 30);
            g.drawOval(cx - 32, cy - 5, 30, 30);
            g.drawOval(cx + 2, cy - 5, 30, 30);
            g.drawOval(cx - 36, cy - 36, 72, 72);
        });

        // --- EXPANDED ARTS, CULTURE & SYMBOLS ---
        register("TORII_GATE", "Japanese Torii", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(3.0f));
            // Curved top beam
            Path2D top = new Path2D.Float();
            top.moveTo(cx - 42, cy - 30);
            top.curveTo(cx - 20, cy - 35, cx + 20, cy - 35, cx + 42, cy - 30);
            g.draw(top);
            // Straight tie beam
            g.setStroke(new BasicStroke(2.0f));
            g.drawLine(cx - 34, cy - 20, cx + 34, cy - 20);
            // Pillars
            g.setStroke(new BasicStroke(3.2f));
            g.drawLine(cx - 24, cy - 20, cx - 22, cy + 35);
            g.drawLine(cx + 24, cy - 20, cx + 22, cy + 35);
            // Center tablet
            g.fillRect(cx - 6, cy - 20, 12, 10);
        });

        register("SCARAB_BEETLE", "Egyptian Scarab", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval(cx - 16, cy - 14, 32, 38);
            g.fillOval(cx - 12, cy - 26, 24, 16);
            g.setStroke(new BasicStroke(2.0f));
            g.drawLine(cx, cy - 14, cx, cy + 24);
            // Legs
            g.drawLine(cx - 16, cy - 6, cx - 32, cy - 16);
            g.drawLine(cx + 16, cy - 6, cx + 32, cy - 16);
            g.drawLine(cx - 16, cy + 12, cx - 32, cy + 22);
            g.drawLine(cx + 16, cy + 12, cx + 32, cy + 22);
            // Sun disc above head
            g.setColor(new Color(255, 255, 255, 140));
            g.fillOval(cx - 8, cy - 42, 16, 16);
        });

        register("PEN_NIB", "Fountain Pen Nib", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2.0f));
            Path2D nib = new Path2D.Float();
            nib.moveTo(cx, cy - 35);
            nib.lineTo(cx + 18, cy + 5);
            nib.lineTo(cx + 18, cy + 30);
            nib.lineTo(cx - 18, cy + 30);
            nib.lineTo(cx - 18, cy + 5);
            nib.closePath();
            g.draw(nib);
            g.drawLine(cx, cy - 35, cx, cy + 5);
            g.fillOval(cx - 4, cy + 5, 8, 8);
        });

        register("HOURGLASS", "Hourglass Chronos", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(2.2f));
            g.drawLine(cx - 24, cy - 32, cx + 24, cy - 32);
            g.drawLine(cx - 24, cy + 32, cx + 24, cy + 32);
            // Glass curves
            Path2D g1 = new Path2D.Float();
            g1.moveTo(cx - 20, cy - 32);
            g1.curveTo(cx - 16, cy - 10, cx - 4, cy - 2, cx, cy);
            g1.curveTo(cx - 4, cy + 2, cx - 16, cy + 10, cx - 20, cy + 32);
            g.draw(g1);
            Path2D g2 = new Path2D.Float();
            g2.moveTo(cx + 20, cy - 32);
            g2.curveTo(cx + 16, cy - 10, cx + 4, cy - 2, cx, cy);
            g2.curveTo(cx + 4, cy + 2, cx + 16, cy + 10, cx + 20, cy + 32);
            g.draw(g2);
            // Sand
            g.fillPolygon(new int[]{cx - 12, cx + 12, cx}, new int[]{cy - 20, cy - 20, cy}, 3);
            g.fillPolygon(new int[]{cx - 14, cx + 14, cx}, new int[]{cy + 28, cy + 28, cy + 6}, 3);
        });

        register("MARITIME_ANCHOR", "Naval Anchor", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(cx, cy - 25, cx, cy + 25);
            g.drawLine(cx - 18, cy - 12, cx + 18, cy - 12);
            g.drawOval(cx - 8, cy - 38, 16, 16);
            g.drawArc(cx - 32, cy - 5, 64, 40, 0, -180);
            g.drawLine(cx - 32, cy + 15, cx - 36, cy + 7);
            g.drawLine(cx + 32, cy + 15, cx + 36, cy + 7);
        });

        register("SKELETON_KEY", "Antique Key", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2.2f));
            g.drawOval(cx - 16, cy - 35, 32, 26);
            g.drawOval(cx - 8, cy - 31, 16, 18);
            g.drawLine(cx, cy - 9, cx, cy + 35);
            // Key teeth
            g.drawLine(cx, cy + 22, cx + 14, cy + 22);
            g.drawLine(cx, cy + 32, cx + 18, cy + 32);
            g.drawLine(cx + 18, cy + 28, cx + 18, cy + 32);
        });

        register("VENETIAN_MASK", "Carnival Mask", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            Path2D m = new Path2D.Float();
            m.moveTo(cx - 38, cy - 12);
            m.curveTo(cx - 20, cy - 25, cx - 10, cy - 10, cx, cy - 8);
            m.curveTo(cx + 10, cy - 10, cx + 20, cy - 25, cx + 38, cy - 12);
            m.curveTo(cx + 36, cy + 18, cx + 10, cy + 20, cx, cy + 15);
            m.curveTo(cx - 10, cy + 20, cx - 36, cy + 18, cx - 38, cy - 12);
            g.draw(m);
            // Eyes
            g.drawOval(cx - 24, cy - 4, 14, 8);
            g.drawOval(cx + 10, cy - 4, 14, 8);
        });

        register("PEGASUS_WINGS", "Pegasus Plume", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(1.8f));
            for (int i = 0; i < 4; i++) {
                int dy = i * 8;
                Path2D w1 = new Path2D.Float();
                w1.moveTo(cx, cy + 15);
                w1.curveTo(cx - 20, cy - 5 + dy, cx - 35, cy - 25 + dy, cx - 45 + i * 4, cy - 20 + dy);
                g.draw(w1);
                Path2D w2 = new Path2D.Float();
                w2.moveTo(cx, cy + 15);
                w2.curveTo(cx + 20, cy - 5 + dy, cx + 35, cy - 25 + dy, cx + 45 - i * 4, cy - 20 + dy);
                g.draw(w2);
            }
        });

        // --- EXPANDED TEXTURES & MINERALS ---
        register("SAND_DUNES", "Sahara Dunes", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 45));
            g.setStroke(new BasicStroke(1.8f));
            for (int y = h / 2 - 28; y <= h / 2 + 30; y += 16) {
                Path2D d = new Path2D.Float();
                d.moveTo(8, y);
                d.curveTo(w * 0.35f, y - 16, w * 0.65f, y + 14, w - 8, y);
                g.draw(d);
            }
        });

        register("CRYSTAL_GEODE", "Crystal Geode", "Texture", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(1.6f));
            for (int r = 14; r <= 46; r += 10) {
                Polygon p = new Polygon();
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    int j = (i % 2 == 0) ? r : r - 4;
                    p.addPoint((int)(cx + j * Math.cos(a)), (int)(cy + j * Math.sin(a)));
                }
                g.drawPolygon(p);
            }
        });

        register("WATER_CAUSTICS", "Caustic Shimmer", "Texture", (g, w, h, c) -> {
            g.setColor(new Color(255, 255, 255, 40));
            g.setStroke(new BasicStroke(1.4f));
            for (int y = 20; y < h - 20; y += 22) {
                for (int x = 15; x < w - 20; x += 32) {
                    g.drawOval(x, y, 22, 14);
                    g.drawLine(x + 11, y, x + 22, y + 14);
                }
            }
        });

        // --- EXPANDED BOTANICAL & NATURE ---
        register("BONSAI_TREE", "Japanese Bonsai", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 5;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2.2f));
            g.drawRoundRect(cx - 28, cy + 30, 56, 12, 4, 4);
            g.drawLine(cx - 32, cy + 30, cx + 32, cy + 30);
            Path2D trunk = new Path2D.Float();
            trunk.moveTo(cx - 4, cy + 30);
            trunk.curveTo(cx - 16, cy + 15, cx + 18, cy + 5, cx - 10, cy - 10);
            trunk.curveTo(cx - 20, cy - 18, cx - 12, cy - 25, cx, cy - 30);
            g.draw(trunk);
            g.setColor(new Color(255, 255, 255, 55));
            g.fillRoundRect(cx - 36, cy - 35, 34, 16, 10, 10);
            g.fillRoundRect(cx + 2, cy - 22, 38, 18, 12, 12);
            g.fillRoundRect(cx - 16, cy - 12, 32, 14, 10, 10);
            g.setColor(new Color(255, 255, 255, 95));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(cx - 36, cy - 35, 34, 16, 10, 10);
            g.drawRoundRect(cx + 2, cy - 22, 38, 18, 12, 12);
        });

        register("PEACOCK_FEATHER", "Peacock Feather", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(cx, cy + 45, cx, cy - 35);
            g.drawOval(cx - 24, cy - 35, 48, 42);
            g.setColor(new Color(255, 255, 255, 45));
            g.fillOval(cx - 16, cy - 28, 32, 28);
            g.setColor(new Color(255, 255, 255, 120));
            g.fillOval(cx - 8, cy - 22, 16, 16);
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(1.0f));
            for (int y = cy - 10; y <= cy + 40; y += 7) {
                g.drawLine(cx, y, cx - 22, y - 10);
                g.drawLine(cx, y, cx + 22, y - 10);
            }
        });

        register("CORAL_REEF", "Coral Reef", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 + 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D coral = new Path2D.Float();
            coral.moveTo(cx, cy + 25);
            coral.lineTo(cx, cy);
            coral.lineTo(cx - 18, cy - 18);
            coral.lineTo(cx - 24, cy - 36);
            coral.moveTo(cx - 18, cy - 18);
            coral.lineTo(cx - 8, cy - 32);
            coral.moveTo(cx, cy);
            coral.lineTo(cx + 18, cy - 16);
            coral.lineTo(cx + 26, cy - 34);
            coral.moveTo(cx + 18, cy - 16);
            coral.lineTo(cx + 10, cy - 30);
            g.draw(coral);
            g.drawOval(cx - 12, cy - 44, 6, 6);
            g.drawOval(cx + 16, cy - 48, 8, 8);
            g.drawOval(cx + 2, cy - 54, 4, 4);
        });

        register("CHERRY_BRANCH", "Sakura Branch", "Nature", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2.2f));
            Path2D b = new Path2D.Float();
            b.moveTo(cx - 40, cy + 25);
            b.curveTo(cx - 15, cy + 10, cx + 10, cy - 5, cx + 38, cy - 25);
            g.draw(b);
            int[][] pts = {{cx - 20, cy + 12}, {cx, cy + 2}, {cx + 22, cy - 14}};
            for (int[] p : pts) {
                g.setColor(new Color(255, 255, 255, 60));
                for (int i = 0; i < 5; i++) {
                    double a = i * 2 * Math.PI / 5;
                    g.fillOval((int)(p[0] + 9 * Math.cos(a) - 4), (int)(p[1] + 9 * Math.sin(a) - 4), 8, 8);
                }
                g.setColor(new Color(255, 255, 255, 140));
                g.fillOval(p[0] - 2, p[1] - 2, 4, 4);
            }
        });

        // --- EXPANDED CELESTIAL ---
        register("HALLEY_COMET", "Halley Comet", "Celestial", (g, w, h, c) -> {
            int cx = w / 2 + 15, cy = h / 2 - 20;
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(cx - 7, cy - 7, 14, 14);
            g.setStroke(new BasicStroke(1.8f));
            for (int i = -3; i <= 3; i++) {
                int a = 120 - Math.abs(i) * 25;
                g.setColor(new Color(255, 255, 255, Math.max(15, a)));
                Path2D tail = new Path2D.Float();
                tail.moveTo(cx - 5, cy + i * 2);
                tail.curveTo(cx - 30, cy + 25 + i * 6, cx - 55, cy + 50 + i * 8, cx - 80, cy + 65 + i * 10);
                g.draw(tail);
            }
        });

        register("ANDROMEDA_CORE", "Andromeda Core", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setStroke(new BasicStroke(1.4f));
            for (int r = 12; r <= 52; r += 10) {
                g.setColor(new Color(255, 255, 255, Math.max(10, 110 - r)));
                AffineTransform old = g.getTransform();
                g.rotate(Math.toRadians(-25), cx, cy);
                g.drawOval(cx - r, cy - r / 3, r * 2, (r * 2) / 3);
                g.setTransform(old);
            }
            g.setColor(new Color(255, 255, 255, 200));
            g.fillOval(cx - 5, cy - 5, 10, 10);
        });

        register("NORTH_STAR", "Polaris Compass", "Celestial", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval(cx - 32, cy - 32, 64, 64);
            Polygon star = new Polygon();
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8 - Math.PI / 2;
                int r = (i % 2 == 0) ? ((i % 4 == 0) ? 42 : 24) : 8;
                star.addPoint((int)(cx + r * Math.cos(a)), (int)(cy + r * Math.sin(a)));
            }
            g.setColor(new Color(255, 255, 255, 110));
            g.fillPolygon(star);
            g.setColor(new Color(255, 255, 255, 220));
            g.drawPolygon(star);
        });

        // --- EXPANDED TECH & CYBER ---
        register("QUANTUM_QUBIT", "Quantum Qubit", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(cx - 34, cy - 34, 68, 68);
            g.drawOval(cx - 34, cy - 12, 68, 24);
            g.drawLine(cx, cy - 42, cx, cy + 42);
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(2.0f));
            g.drawLine(cx, cy, cx + 22, cy - 24);
            g.fillOval(cx + 20, cy - 26, 6, 6);
        });

        register("SATELLITE_ORBIT", "Orbital Satellite", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4, 4}, 0));
            g.drawArc(cx - 60, cy - 50, 120, 100, 150, 240);
            g.setColor(new Color(255, 255, 255, 180));
            g.setStroke(new BasicStroke(1.5f));
            g.fillRect(cx - 6, cy - 6, 12, 12);
            g.setColor(new Color(255, 255, 255, 120));
            g.fillRect(cx - 24, cy - 5, 14, 10);
            g.fillRect(cx + 10, cy - 5, 14, 10);
            g.setColor(new Color(255, 255, 255, 220));
            g.drawRect(cx - 24, cy - 5, 14, 10);
            g.drawRect(cx + 10, cy - 5, 14, 10);
            g.drawArc(cx - 4, cy - 14, 8, 8, 0, 180);
        });

        register("FIBONACCI_TREE", "Fractal Tree", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 + 25;
            g.setColor(new Color(255, 255, 255, 90));
            g.setStroke(new BasicStroke(1.6f));
            drawFractalBranch(g, cx, cy, -Math.PI / 2, 28, 5);
        });

        register("CYBER_HEX_SHIELD", "Hex Shield", "Tech", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(1.5f));
            int[] dx = {0, -18, 18, -18, 18, 0};
            int[] dy = {-22, -11, -11, 11, 11, 22};
            for (int k = 0; k < 6; k++) {
                drawHex(g, cx + dx[k], cy + dy[k], 12);
            }
            drawHex(g, cx, cy, 12);
            g.setColor(new Color(255, 255, 255, 160));
            g.drawOval(cx - 38, cy - 38, 76, 76);
        });

        // --- EXPANDED GEOMETRIC ---
        register("SIERPINSKI_CARPET", "Sierpinski Carpet", "Geometric", (g, w, h, c) -> {
            int cx = w / 2 - 36, cy = h / 2 - 46;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRect(cx, cy, 72, 72);
            g.setColor(new Color(255, 255, 255, 45));
            g.fillRect(cx + 24, cy + 24, 24, 24);
            g.setColor(new Color(255, 255, 255, 120));
            g.drawRect(cx + 24, cy + 24, 24, 24);
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 3; col++) {
                    if (r == 1 && col == 1) continue;
                    g.drawRect(cx + col * 24 + 8, cy + r * 24 + 8, 8, 8);
                }
            }
        });

        register("ICOSAHEDRON_WIRE", "Icosahedron Facets", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 85));
            g.setStroke(new BasicStroke(1.4f));
            int rOuter = 38;
            int rInner = 20;
            Point2D.Double[] out = new Point2D.Double[6];
            Point2D.Double[] in = new Point2D.Double[6];
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 - Math.PI / 6;
                out[i] = new Point2D.Double(cx + rOuter * Math.cos(a), cy + rOuter * Math.sin(a));
                in[i] = new Point2D.Double(cx + rInner * Math.cos(a + Math.PI / 6), cy + rInner * Math.sin(a + Math.PI / 6));
            }
            for (int i = 0; i < 6; i++) {
                g.drawLine((int)out[i].x, (int)out[i].y, (int)out[(i + 1) % 6].x, (int)out[(i + 1) % 6].y);
                g.drawLine((int)in[i].x, (int)in[i].y, (int)in[(i + 1) % 6].x, (int)in[(i + 1) % 6].y);
                g.drawLine((int)out[i].x, (int)out[i].y, (int)in[i].x, (int)in[i].y);
                g.drawLine((int)out[(i + 1) % 6].x, (int)out[(i + 1) % 6].y, (int)in[i].x, (int)in[i].y);
            }
        });

        register("CRETAN_LABYRINTH", "Cretan Labyrinth", "Geometric", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.8f));
            for (int r = 12; r <= 44; r += 8) {
                g.drawArc(cx - r, cy - r, r * 2, r * 2, 30, 300);
            }
            g.drawLine(cx, cy - 44, cx, cy - 12);
        });

        // --- EXPANDED ARTS & DRAMA ---
        register("VIOLIN_CLEF", "Violin F-Hole", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(2.2f));
            for (int side : new int[]{-1, 1}) {
                int ox = cx + side * 18;
                Path2D f = new Path2D.Float();
                f.moveTo(ox, cy - 30);
                f.curveTo(ox + side * 10, cy - 20, ox - side * 10, cy, ox, cy + 20);
                f.curveTo(ox + side * 8, cy + 28, ox, cy + 32, ox - side * 4, cy + 30);
                g.draw(f);
                g.fillOval(ox - side * 2 - 3, cy - 32, 6, 6);
                g.fillOval(ox - side * 5 - 3, cy + 28, 6, 6);
            }
        });

        register("THEATER_MASKS", "Comedy & Tragedy", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 10;
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(1.6f));
            g.drawRoundRect(cx - 32, cy - 22, 26, 36, 14, 14);
            g.drawOval(cx - 27, cy - 12, 6, 6);
            g.drawOval(cx - 17, cy - 12, 6, 6);
            g.drawArc(cx - 26, cy - 2, 14, 10, 0, -180);
            g.drawRoundRect(cx + 6, cy - 14, 26, 36, 14, 14);
            g.drawOval(cx + 11, cy - 4, 6, 6);
            g.drawOval(cx + 21, cy - 4, 6, 6);
            g.drawArc(cx + 12, cy + 12, 14, 10, 0, 180);
        });

        register("PHOENIX_BIRD", "Phoenix Rising", "Arts", (g, w, h, c) -> {
            int cx = w / 2, cy = h / 2 - 8;
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(1.8f));
            // Body and head
            g.fillOval(cx - 4, cy - 24, 8, 12);
            // Crest
            Path2D crest = new Path2D.Float();
            crest.moveTo(cx, cy - 24);
            crest.curveTo(cx - 8, cy - 34, cx + 2, cy - 36, cx + 4, cy - 30);
            g.draw(crest);
            // Wings
            Path2D wLeft = new Path2D.Float();
            wLeft.moveTo(cx - 4, cy - 18);
            wLeft.curveTo(cx - 24, cy - 32, cx - 40, cy - 14, cx - 34, cy + 6);
            wLeft.curveTo(cx - 22, cy - 4, cx - 12, cy - 6, cx - 2, cy - 8);
            g.draw(wLeft);
            Path2D wRight = new Path2D.Float();
            wRight.moveTo(cx + 4, cy - 18);
            wRight.curveTo(cx + 24, cy - 32, cx + 40, cy - 14, cx + 34, cy + 6);
            wRight.curveTo(cx + 22, cy - 4, cx + 12, cy - 6, cx + 2, cy - 8);
            g.draw(wRight);
            // Fiery tail feathers
            Path2D tail = new Path2D.Float();
            tail.moveTo(cx, cy - 6);
            tail.curveTo(cx - 14, cy + 16, cx - 18, cy + 32, cx - 8, cy + 30);
            tail.curveTo(cx - 3, cy + 20, cx + 3, cy + 20, cx + 8, cy + 30);
            tail.curveTo(cx + 18, cy + 32, cx + 14, cy + 16, cx, cy - 6);
            g.draw(tail);
        });
    }

    private static void drawFractalBranch(Graphics2D g, double x, double y, double angle, double length, int depth) {
        if (depth <= 0) return;
        double x2 = x + length * Math.cos(angle);
        double y2 = y + length * Math.sin(angle);
        g.drawLine((int)x, (int)y, (int)x2, (int)y2);
        drawFractalBranch(g, x2, y2, angle - 0.45, length * 0.72, depth - 1);
        drawFractalBranch(g, x2, y2, angle + 0.45, length * 0.72, depth - 1);
    }

    private static void drawHex(Graphics2D g, int cx, int cy, int r) {
        Polygon p = new Polygon();
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            p.addPoint((int)(cx + r * Math.cos(a)), (int)(cy + r * Math.sin(a)));
        }
        g.drawPolygon(p);
    }

    private static void initKeywords() {
        tag("NONE", "minimal solid plain blank empty clean modern simple subtle");
        tag("FLOWERS", "flower blossom sakura bouquet petal spring floral bloom garden japan flora");
        tag("ROSE", "rose english flower petal thorn love valentine floral romantic garden flora");
        tag("LOTUS", "lotus sacred water lily zen buddhism meditation spa spiritual pond flower flora");
        tag("LEAF_FOLIAGE", "leaf foliage monstera jungle tropical plant botanical nature palm green tree leaves");
        tag("BAMBOO", "bamboo zen stalk plant forest asian stem china japan nature cane");
        tag("FOREST_PINE", "pine forest woodland tree evergreen conifer mountain woods nature landscape fir spruce");
        tag("MOUNTAIN_PEAKS", "mountain peaks alpine alps hills snow summit outdoor climb nature range landscape");
        tag("OCEAN_TIDE", "ocean tide waves sea water surf beach tsunami current river ripple marine coast");
        tag("TREE_OF_LIFE", "tree life roots branches trunk oak spiritual nature growth ancient yggdrasil");
        tag("RAINDROPS", "rain drops shower water weather storm cloud drizzle tear autumn winter wet climate");
        tag("BONSAI_TREE", "bonsai tree japanese miniature pot plant zen garden nature prune trunk leaves");
        tag("PEACOCK_FEATHER", "peacock feather bird plume eye radiant colorful animal plumage tail wings");
        tag("CORAL_REEF", "coral reef sea ocean marine underwater aquatic polyps tropical marine fish");
        tag("CHERRY_BRANCH", "cherry sakura branch blossom floral spring bough flower petal bloom japan tree");
        tag("SUNFLOWER", "sunflower golden flower bloom seed petal yellow nature sun plant helianthus rays");
        tag("MAPLE_LEAF", "maple autumn leaf canada fall foliage orange red nature tree foliage season");
        tag("GINKGO", "ginkgo biloba leaf fan ancient healing medicinal plant nature gold tree herb");
        tag("FERN_FROND", "fern frond forest shade woodland botanical foliage plant leaves fronds flora");
        tag("DANDELION", "dandelion whisps seeds wish wind blow fluff flower garden meadow drift");
        tag("CACTUS_DESERT", "cactus desert saguaro arizona arid succulent spines landscape arid hot");
        tag("WILLOW_TREE", "willow weeping lake water river drooping branches graceful tree pond shade");
        tag("ORCHID", "orchid wild exotic tropical blossom bloom petals elegant flora botanical");
        tag("OAK_ACORN", "oak acorn royal majestic ancient branches solid forest timber strength leaf");

        tag("SUN", "sun radiant solar sunshine light warmth morning summer day celestial star yellow rays shine");
        tag("SUNRISE", "sunrise dawn horizon morning sky sun daybreak morning glow rays golden sunset dusk");
        tag("SUNBURST", "sunburst solar rays starburst flare beams light shine burst corona flash beam");
        tag("CRESCENT_MOON", "moon crescent lunar night sky evening sleep dream orbit crescent stars bedtime");
        tag("CONSTELLATION", "constellation star map galaxy astronomy zodiac sky night north ursa orion stars");
        tag("GALAXY_SPIRAL", "galaxy spiral cosmic vortex milky way universe space deep outer nebula stars");
        tag("SATURN_RINGS", "saturn rings planet orbit ring gas giant astronomy space cosmos celestial system");
        tag("ECLIPSE_RING", "eclipse solar totality shadow corona black sun moon alignment celestial event");
        tag("NORTHERN_AURORA", "aurora borealis northern lights ribbons curtains sky green night polar magnetic");
        tag("METEOR_SHOWER", "shooting stars meteor shower night sky wishes streaks falling stars cosmos flare");
        tag("ASTROLABE", "astrolabe dial navigation brass ancient astronomy celestial chart instrument time");
        tag("SUPERNOVA", "supernova blast exploding star cosmic expansion shockwave nebula stellar outer space");
        tag("HALLEY_COMET", "comet halley meteor shooting star streak celestial ice cosmos tail core space");
        tag("ANDROMEDA_CORE", "andromeda core galaxy cosmic nebula ellipse spiral space astronomy halo satellite");
        tag("NORTH_STAR", "north star polaris compass navigation guide celestial light shine true navigator pole");

        tag("ROBOT", "robot android ai artificial intelligence cyborg mech tech future automation machine bot agent");
        tag("CYBER_CIRCUIT", "circuit pcb cyber computer electronic hardware tech chip motherboard traces wires");
        tag("MICROCHIP", "microchip chip silicon die cpu processor semiconductor hardware tech wafer integrated");
        tag("GEAR_MATRIX", "gears clockwork mechanism cogs machinery steam industrial tech watch engineering cogs");
        tag("ATOM_ORBITS", "atom orbits quantum physics chemistry nuclear electron science core nucleus particles");
        tag("DNA_HELIX", "dna helix genetics biology gene code science medicine molecule chromosome biotech");
        tag("TELESCOPE", "telescope observatory astronomy stars space optics lens galaxy watch satellite look");
        tag("NEURAL_NETWORK", "neural network deep learning synapses nodes brain ai machine learning graph vectors");
        tag("FIBER_OPTIC", "fiber optic matrix data transmission light internet cable lines high speed network");
        tag("NANOTECH_HEX", "nanotech lattice molecular carbon nanotubes buckyball nano particles engineering science");
        tag("DRONE_QUAD", "drone quadcopter autonomous aircraft propeller surveillance flight robotics tech");
        tag("CYBER_SKULL", "cyber skull tech matrix futuristic bionic hacker security dark terminal machine");
        tag("QUANTUM_WAVE", "wave function quantum mechanics physics sine cosine wave probability frequency oscillation");
        tag("AI_PROCESSOR", "ai core die tensor processing unit accelerator hardware compute logic tensor engine");
        tag("QUANTUM_QUBIT", "quantum qubit bloch sphere physics computer superposition state vector tech gates computing");
        tag("SATELLITE_ORBIT", "satellite orbit space solar station telemetry broadcast trajectory tech grounddish");
        tag("FIBONACCI_TREE", "tree fractal binary recursive branches math code growth nature algorithm logic");
        tag("CYBER_HEX_SHIELD", "hex shield forcefield cyber barrier defense energy tech grid armor firewall security");

        tag("HEXAGONS", "hexagons honeycomb hive bee geometry honey cells polygon tech geometric tiles");
        tag("TRIANGLES", "triangles delta mosaic polygon pyramid modern geometry prism facets geometric shapes");
        tag("DIAMONDS", "diamonds argyle harlequin rhomb pattern crystal luxury gem geometric diamond rhombi");
        tag("CHEVRON", "chevron weave zigzag pattern nordic fabric textile geometric arrow herringbone");
        tag("CONCENTRIC_RINGS", "concentric rings ripple radar waves circles target sonar signal radio echo");
        tag("ISOMETRIC_CUBES", "cubes isometric 3d escher blocks spatial dimension geometric box hexagonal 3-dimensional");
        tag("DOT_MATRIX", "dots matrix halftone array grid stipple point digital screen print points circles");
        tag("CROSSHATCH", "crosshatch hatch texture sketch shading lines grid drawing ink mesh hatching");
        tag("BLUEPRINT_GRID", "blueprint grid drafting engineering graph architecture cad floorplan technical millimeter");
        tag("BAUHAUS_SHAPES", "bauhaus shapes geometry modern art abstract circle square triangle design modernist");
        tag("METATRON_CUBE", "metatron cube sacred geometry platonic solids archangel vector balance mystic math");
        tag("FLOWER_OF_LIFE", "flower of life sacred geometry overlapping circles creation pattern harmony sacred");
        tag("SIERPINSKI", "sierpinski triangle delta fractal recursion triforce geometric math iterative");
        tag("PENROSE_TILING", "penrose tiling aperiodic kites darts symmetry crystal modern math tiles geometric");
        tag("GREEK_KEY", "greek meander key fret labyrinth classical border ancient geometric continuous line");
        tag("VORONOI_CELLS", "voronoi cells organic biological tessellation partition polygons natural math nature");
        tag("GOTHIC_TREFOIL", "gothic trefoil cathedral window architecture medieval stone church triple arch");
        tag("SIERPINSKI_CARPET", "sierpinski carpet fractal recursion squares geometry math pattern matrix nested");
        tag("ICOSAHEDRON_WIRE", "icosahedron wireframe 3d facets geodesic polyhedron polygon geometry dice d20 faces");
        tag("CRETAN_LABYRINTH", "labyrinth maze cretan path puzzle classical concentric track winding thread");

        tag("QUILL_AND_INK", "quill pen ink feather writing author literature calligraphy journal poet words scroll");
        tag("OPEN_BOOK", "book open tome wisdom library study reading novel education literature pages text stories");
        tag("COMPASS_ROSE", "compass rose navigation north map direction travel sailor wind explorer expedition");
        tag("PALETTE_BRUSH", "palette paint brush artist colors artwork creative canvas painter oil color acrylic");
        tag("MUSIC_NOTES", "music notes melody staff treble clef rhythm song chord tune harmony sound musical");
        tag("ORIGAMI_CRANE", "origami crane paper bird folded japanese craft peace flight art peace folding");
        tag("CHESS_KNIGHT", "chess knight horse strategy piece game grandmaster king tactics play tournament board");
        tag("CELTIC_KNOT", "celtic knot braid loop endless irish scotland medieval shield pattern eternity ribbon");
        tag("ZEN_MANDALA", "mandala zen bloom meditation sacred geometry kaleidoscope spiritual balance harmony");
        tag("GOLDEN_SPIRAL", "golden spiral fibonacci ratio nautilus shell geometry math proportion snail golden");
        tag("SHIELD_HERALDIC", "shield heraldic knight crest armor coat of arms honor royalty guard emblem defense");
        tag("PRISM_SPECTRUM", "prism spectrum rainbow optics light refraction crystal dispersion glass triangular physics");
        tag("TORII_GATE", "torii gate shinto shrine japan sacred entrance red arch traditional temple culture");
        tag("SCARAB_BEETLE", "scarab egyptian pharaoh beetle hieroglyph ancient pyramid gold sun amulet re birth");
        tag("PEN_NIB", "fountain pen nib gold nib calligraphy signature writer author luxury ink writing");
        tag("HOURGLASS", "hourglass time chronos sand passing countdown patience temp eternity timer clock");
        tag("MARITIME_ANCHOR", "anchor naval sailor nautical maritime sea harbor navy ship rope marine safe");
        tag("SKELETON_KEY", "antique key skeleton key vintage brass unlock secret mystery treasure portal lock");
        tag("VENETIAN_MASK", "carnival mask venice masquerade ball mysterious disguise drama phantom costume party");
        tag("PEGASUS_WINGS", "pegasus wings plume feathers myth greek flying horse flight angel plume divine");
        tag("VIOLIN_CLEF", "violin f-hole cello instrument string classical music acoustic audio melody orchestra");
        tag("THEATER_MASKS", "theater masks comedy tragedy drama acting stage performance greek broadway thespian");
        tag("PHOENIX_BIRD", "phoenix bird fire flame rebirth rising wings feathers mythical creature legend fly avian arts");

        tag("LEATHER_GRAIN", "leather grain moroccan texture hide skin book cover luxury antique vintage bound");
        tag("MARBLE_SWIRLS", "marble swirls stone vein florentine luxury texture mineral granite slab counter");
        tag("CARBON_FIBER", "carbon fiber twill weave composite high tech race material texture dark composite");
        tag("SAND_DUNES", "sand dunes sahara desert waves wind ripples hills landscape dry texture");
        tag("CRYSTAL_GEODE", "crystal geode mineral quartz gemstone facets rock agate cave amethyst crystal");
        tag("WATER_CAUSTICS", "caustic shimmer water swimming pool light reflections ripples liquid underwater");
        tag("LINEN_CANVAS", "linen canvas fabric cloth woven weave natural textile thread background texture");
        tag("WOOD_GRAIN", "timber rings tree wood grain rings annual growth stump natural forest texture");
        tag("RETRO_GRID", "synthwave horizon 80s retrowave grid sunset neon outrun retro perspective matrix");
        tag("TOPOGRAPHIC_MAP", "elevation contours topo map topography terrain altitude geography lines cartography");
        tag("CLOUDS", "cumulus drift clouds sky soft fluffy overcast weather atmospheric air float");
        tag("STRIPES_MINIMAL", "minimalist pinstripe clean vertical lines tailored elegant modern simple pinstripes");
    }
}
