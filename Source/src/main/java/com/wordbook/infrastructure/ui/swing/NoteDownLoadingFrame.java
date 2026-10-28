package com.wordbook.infrastructure.ui.swing;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Dedicated loading JFrame presenting the NoteDown Studio startup pipeline,
 * animated progress bar, and real-time initialization steps before launching the main studio window.
 */
public class NoteDownLoadingFrame extends JFrame {
    private static final int WIDTH = 560;
    private static final int HEIGHT = 340;

    private BufferedImage logoImage;
    private int progress = 0;
    private String statusMessage = "Initializing Mikey Note Core Engine...";

    public NoteDownLoadingFrame() {
        setUndecorated(true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));

        loadLogo();

        try {
            setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 20, 20));
        } catch (Exception ignored) {}
    }

    private void loadLogo() {
        try {
            File logoFile = new File("assets/mikey_note_logo.png");
            if (!logoFile.exists()) logoFile = new File("assets/logo.png");
            if (!logoFile.exists()) logoFile = new File("assets/notedown_logo.png");
            if (logoFile.exists()) {
                logoImage = ImageIO.read(logoFile);
                setIconImage(logoImage);
            }
        } catch (Exception ignored) {}
    }

    public void updateProgress(int percent, String message) {
        this.progress = Math.min(100, Math.max(0, percent));
        this.statusMessage = message;
        repaint();
    }

    public void finishAndLaunch(Runnable onLaunch) {
        updateProgress(100, "Ready! Opening Mikey Note Studio...");
        try {
            Thread.sleep(350);
        } catch (InterruptedException ignored) {}

        SwingUtilities.invokeLater(() -> {
            dispose();
            if (onLaunch != null) {
                onLaunch.run();
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Creative Studio Gradient Background (White / Pure Light Titanium)
        GradientPaint bgPaint = new GradientPaint(
                0, 0, new Color(255, 255, 255),
                WIDTH, HEIGHT, new Color(241, 245, 249)
        );
        g2.setPaint(bgPaint);
        g2.fillRoundRect(0, 0, WIDTH, HEIGHT, 20, 20);

        // Subtle geometric grid
        g2.setColor(new Color(0, 0, 0, 8));
        for (int x = 0; x < WIDTH; x += 32) g2.drawLine(x, 0, x, HEIGHT);
        for (int y = 0; y < HEIGHT; y += 32) g2.drawLine(0, y, WIDTH, y);

        // 2. NoteDown Origami Logo
        int logoSize = 120;
        int logoX = 40;
        int logoY = 55;

        if (logoImage != null) {
            Shape oldClip = g2.getClip();
            RoundRectangle2D logoClip = new RoundRectangle2D.Double(logoX, logoY, logoSize, logoSize, 22, 22);

            g2.setColor(new Color(37, 99, 235, 40));
            g2.fillRoundRect(logoX - 4, logoY - 4, logoSize + 8, logoSize + 8, 26, 26);

            g2.clip(logoClip);
            g2.drawImage(logoImage, logoX, logoY, logoSize, logoSize, null);
            g2.setClip(oldClip);

            g2.setColor(new Color(37, 99, 235, 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(logoX, logoY, logoSize, logoSize, 22, 22);
        }

        // 3. Typography & Branding (No Emojis, No SQLite mentioned)
        int textX = logoX + logoSize + 32;

        g2.setColor(new Color(37, 99, 235));
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString("MIKEY NOTE CREATIVE STUDIO", textX, 78);

        g2.setColor(new Color(15, 23, 42));
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        g2.drawString("Mikey Note", textX, 120);

        g2.setColor(new Color(71, 85, 105));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.drawString("v2.0 • Advanced Modular Java 24 Architecture", textX, 146);

        g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g2.setColor(new Color(148, 163, 184));
        g2.drawString("Modern Document Studio & Notes System", textX, 168);

        // 4. Loading Status Message
        g2.setColor(new Color(51, 65, 85));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(statusMessage, 40, HEIGHT - 55);

        // Percentage
        String pctStr = progress + "%";
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(37, 99, 235));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(pctStr, WIDTH - 40 - fm.stringWidth(pctStr), HEIGHT - 55);

        // 5. Sleek Azure / Gold Loading Bar
        int barX = 40;
        int barY = HEIGHT - 40;
        int barW = WIDTH - 80;
        int barH = 6;

        g2.setColor(new Color(0, 0, 0, 18));
        g2.fillRoundRect(barX, barY, barW, barH, 6, 6);

        if (progress > 0) {
            int fillW = (int) (barW * (progress / 100.0));
            GradientPaint progressPaint = new GradientPaint(
                    barX, barY, new Color(37, 99, 235),
                    barX + fillW, barY, new Color(14, 165, 233)
            );
            g2.setPaint(progressPaint);
            g2.fillRoundRect(barX, barY, fillW, barH, 6, 6);
        }

        // 6. Border
        g2.setColor(new Color(0, 0, 0, 30));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 20, 20);

        g2.dispose();
    }
}
