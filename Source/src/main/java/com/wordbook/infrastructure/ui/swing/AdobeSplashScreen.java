package com.wordbook.infrastructure.ui.swing;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Adobe-style creative splash screen featuring Nano Banana branding,
 * sleek dark gradient, high-DPI typography, and real-time initialization steps.
 */
public class AdobeSplashScreen extends JWindow {
    private static final int WIDTH = 560;
    private static final int HEIGHT = 340;

    private BufferedImage logoImage;
    private int progress = 0;
    private String statusMessage = "Initializing Mikey Note Core Engine...";

    public AdobeSplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        loadLogo();

        // Rounded window corners if supported
        try {
            setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 18, 18));
        } catch (Exception ignored) {}
    }

    private void loadLogo() {
        try {
            File logoFile = new File("assets/mikey_note_logo.png");
            if (!logoFile.exists()) {
                logoFile = new File("assets/logo.png");
            }
            if (!logoFile.exists()) {
                logoFile = new File("assets/notedown_logo.png");
            }
            if (logoFile.exists()) {
                logoImage = ImageIO.read(logoFile);
            }
        } catch (Exception e) {
            System.err.println("[Splash] Logo load notice: " + e.getMessage());
        }
    }

    public void updateProgress(int percent, String message) {
        this.progress = Math.min(100, Math.max(0, percent));
        this.statusMessage = message;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Creative Dark Gradient Background (Adobe Creative Cloud style)
        GradientPaint bgPaint = new GradientPaint(
                0, 0, new Color(20, 24, 38),
                WIDTH, HEIGHT, new Color(10, 12, 20)
        );
        g2.setPaint(bgPaint);
        g2.fillRoundRect(0, 0, WIDTH, HEIGHT, 18, 18);

        // Subtle geometric cybernetic grid lines
        g2.setColor(new Color(255, 255, 255, 6));
        for (int x = 0; x < WIDTH; x += 32) {
            g2.drawLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y < HEIGHT; y += 32) {
            g2.drawLine(0, y, WIDTH, y);
        }

        // 2. NoteDown Modern Origami Logo Image
        int logoSize = 130;
        int logoX = 40;
        int logoY = 50;

        if (logoImage != null) {
            // Draw logo with rounded clip & glow shadow
            Shape oldClip = g2.getClip();
            RoundRectangle2D logoClip = new RoundRectangle2D.Double(logoX, logoY, logoSize, logoSize, 22, 22);

            // Azure & Gold glow underneath
            g2.setColor(new Color(56, 189, 248, 50));
            g2.fillRoundRect(logoX - 4, logoY - 4, logoSize + 8, logoSize + 8, 26, 26);

            g2.clip(logoClip);
            g2.drawImage(logoImage, logoX, logoY, logoSize, logoSize, null);
            g2.setClip(oldClip);

            // Border
            g2.setColor(new Color(56, 189, 248, 140));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(logoX, logoY, logoSize, logoSize, 22, 22);
        } else {
            g2.setColor(new Color(56, 189, 248));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 48));
            g2.drawString("N", logoX + 38, logoY + 80);
        }

        // 3. Typography & Adobe-style Branding
        int textX = logoX + logoSize + 32;

        // "MIKEY NOTE STUDIO" Badge
        g2.setColor(new Color(56, 189, 248));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("MIKEY NOTE CREATIVE STUDIO", textX, 74);

        // App Title "Mikey Note"
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
        g2.drawString("Mikey Note", textX, 116);

        // Version & Tagline
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.drawString("v2.0 • Advanced Modular Java 24 & SQLite", textX, 142);

        g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        g2.setColor(new Color(100, 116, 139));
        g2.drawString("Modern Document Studio & Notes System", textX, 164);

        // 4. Loading Status Message
        g2.setColor(new Color(226, 232, 240));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString(statusMessage, 40, HEIGHT - 55);

        // Percentage
        String pctStr = progress + "%";
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(234, 179, 8));
        g2.drawString(pctStr, WIDTH - 40 - fm.stringWidth(pctStr), HEIGHT - 55);

        // 5. Sleek Golden Loading Bar
        int barX = 40;
        int barY = HEIGHT - 40;
        int barW = WIDTH - 80;
        int barH = 5;

        // Background groove
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRoundRect(barX, barY, barW, barH, 5, 5);

        // Active glowing progress
        if (progress > 0) {
            int fillW = (int) (barW * (progress / 100.0));
            GradientPaint progressPaint = new GradientPaint(
                    barX, barY, new Color(250, 204, 21),
                    barX + fillW, barY, new Color(234, 88, 12)
            );
            g2.setPaint(progressPaint);
            g2.fillRoundRect(barX, barY, fillW, barH, 5, 5);
        }

        // 6. Subtle Outer Border
        g2.setColor(new Color(255, 255, 255, 20));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 18, 18);

        g2.dispose();
    }
}
