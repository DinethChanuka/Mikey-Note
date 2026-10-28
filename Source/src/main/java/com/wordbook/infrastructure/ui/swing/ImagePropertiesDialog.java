package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Intelligent Image Layout & Controlling Studio Dialog.
 * Provides live visual preview, dimension controls (with aspect ratio lock),
 * floating & text wrap positioning, stylish frames (drop shadows, polaroid, rounded),
 * visual image filters (grayscale, sepia, contrast), and accessible alt text for screen readers.
 */
public class ImagePropertiesDialog extends JDialog {

    private final String imageSrc;
    private BufferedImage originalImage = null;

    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private JCheckBox lockAspectCheck;
    private JComboBox<String> alignCombo;
    private JComboBox<String> styleCombo;
    private JComboBox<String> filterCombo;
    private JTextField altTextField;
    private JTextField captionField;
    private JPanel previewCanvas;

    private double aspectRatio = 1.333; // width / height
    private boolean updatingSpinners = false;
    private boolean confirmed = false;

    public ImagePropertiesDialog(Frame owner, int initialWidth, String initialAlign, String initialCaption) {
        this(owner, initialWidth, initialAlign, initialCaption, null);
    }

    public ImagePropertiesDialog(Frame owner, int initialWidth, String initialAlign, String initialCaption, String imageSrc) {
        super(owner, "Image Layout & Controlling Studio", true);
        this.imageSrc = imageSrc;
        setSize(580, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        loadImageIfAvailable();

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        root.setBackground(ThemeManager.getSurface());

        // Center Split: Left Controls, Right Live Preview
        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        contentGrid.setOpaque(false);

        // --- Left Form ---
        JPanel leftForm = new JPanel();
        leftForm.setLayout(new BoxLayout(leftForm, BoxLayout.Y_AXIS));
        leftForm.setOpaque(false);

        // 1. Dimensions
        leftForm.add(createSectionLabel("Dimensions (Width × Height):"));
        JPanel dimRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        dimRow.setOpaque(false);

        int initW = initialWidth > 0 ? initialWidth : 520;
        int initH = (int) Math.round(initW / aspectRatio);

        widthSpinner = new JSpinner(new SpinnerNumberModel(initW, 60, 1600, 20));
        widthSpinner.setPreferredSize(new Dimension(74, 28));
        widthSpinner.addChangeListener(e -> onWidthChanged());
        dimRow.add(widthSpinner);

        dimRow.add(new JLabel("×"));

        heightSpinner = new JSpinner(new SpinnerNumberModel(initH, 40, 1600, 20));
        heightSpinner.setPreferredSize(new Dimension(74, 28));
        heightSpinner.addChangeListener(e -> onHeightChanged());
        dimRow.add(heightSpinner);

        lockAspectCheck = new JCheckBox("Lock Aspect", true);
        lockAspectCheck.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lockAspectCheck.setOpaque(false);
        dimRow.add(lockAspectCheck);
        leftForm.add(dimRow);

        // Preset sizing pills
        JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        presetRow.setOpaque(false);
        addPresetBtn(presetRow, "25%", 180);
        addPresetBtn(presetRow, "50%", 360);
        addPresetBtn(presetRow, "75%", 540);
        addPresetBtn(presetRow, "100%", 720);
        leftForm.add(presetRow);
        leftForm.add(Box.createVerticalStrut(10));

        // 2. Alignment & Text Wrap
        leftForm.add(createSectionLabel("Alignment & Text Wrap:"));
        alignCombo = new ModernComboBox<>(new String[]{
                "Center (Block Element)",
                "Align Left",
                "Align Right",
                "Float Left (Text Wrap Around)",
                "Float Right (Text Wrap Around)"
        });
        if (initialAlign != null) {
            for (int i = 0; i < alignCombo.getItemCount(); i++) {
                if (alignCombo.getItemAt(i).toLowerCase().contains(initialAlign.toLowerCase())) {
                    alignCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        alignCombo.addActionListener(e -> updatePreview());
        leftForm.add(alignCombo);
        leftForm.add(Box.createVerticalStrut(10));

        // 3. Border & Frame Styling
        leftForm.add(createSectionLabel("Border & Frame Style:"));
        styleCombo = new ModernComboBox<>(new String[]{
                "Modern Rounded (8px)",
                "Large Rounded (16px)",
                "Floating Drop Shadow",
                "Polaroid Photo Frame",
                "Circle Avatar (50% Radius)",
                "Clean Thin Border",
                "Plain (No Frame)"
        });
        styleCombo.addActionListener(e -> updatePreview());
        leftForm.add(styleCombo);
        leftForm.add(Box.createVerticalStrut(10));

        // 4. Visual Image Filters
        leftForm.add(createSectionLabel("Image Tone Filter:"));
        filterCombo = new ModernComboBox<>(new String[]{
                "Original Colors",
                "Grayscale (Monochrome)",
                "Warm Sepia Vintage",
                "High Contrast Punch",
                "Cool Blue Tint"
        });
        filterCombo.addActionListener(e -> updatePreview());
        leftForm.add(filterCombo);
        leftForm.add(Box.createVerticalStrut(10));

        // 5. Accessibility Alt Text
        leftForm.add(createSectionLabel("Screen Reader Alt Text (A11y):"));
        altTextField = new JTextField("Descriptive illustration for document");
        altTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        altTextField.setMaximumSize(new Dimension(300, 28));
        leftForm.add(altTextField);
        leftForm.add(Box.createVerticalStrut(10));

        // 6. Caption
        leftForm.add(createSectionLabel("Figure Caption:"));
        captionField = new JTextField(initialCaption != null ? initialCaption : "Figure 1: Illustration");
        captionField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        captionField.setMaximumSize(new Dimension(300, 28));
        captionField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        leftForm.add(captionField);

        contentGrid.add(leftForm);

        // --- Right Preview Panel ---
        JPanel rightBox = new JPanel(new BorderLayout(0, 8));
        rightBox.setOpaque(false);
        JLabel prevHdr = new JLabel("Live Studio Preview:");
        prevHdr.setFont(new Font("Segoe UI", Font.BOLD, 12));
        prevHdr.setForeground(new Color(100, 116, 139));
        rightBox.add(prevHdr, BorderLayout.NORTH);

        previewCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawLivePreview((Graphics2D) g);
            }
        };
        previewCanvas.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));
        previewCanvas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        rightBox.add(previewCanvas, BorderLayout.CENTER);

        contentGrid.add(rightBox);
        root.add(contentGrid, BorderLayout.CENTER);

        // --- Bottom Actions ---
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottomBar.setBackground(ThemeManager.getSurface());
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorder()));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton applyBtn = new JButton("Apply Image Layout", VectorIcons.createIcon(VectorIcons.CHECKMARK, 13, Color.WHITE));
        applyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        applyBtn.setBackground(new Color(37, 99, 235));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setOpaque(true);
        applyBtn.setBorderPainted(false);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        bottomBar.add(cancelBtn);
        bottomBar.add(applyBtn);
        root.add(bottomBar, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    private void loadImageIfAvailable() {
        if (imageSrc == null || imageSrc.isBlank()) return;
        try {
            File f = null;
            if (imageSrc.startsWith("file:")) {
                try {
                    String clean = imageSrc;
                    while (clean.startsWith("file:////")) clean = "file:/" + clean.substring(8);
                    while (clean.startsWith("file:///")) clean = "file:/" + clean.substring(7);
                    f = new File(new java.net.URI(clean));
                } catch (Exception e) {
                    f = new File(imageSrc.replaceAll("^file:/+", "/"));
                }
            } else {
                f = new File(imageSrc);
            }
            if (f != null && f.exists()) {
                originalImage = ImageIO.read(f);
                if (originalImage != null && originalImage.getHeight() > 0) {
                    aspectRatio = (double) originalImage.getWidth() / (double) originalImage.getHeight();
                }
            }
        } catch (Exception ignored) {}
    }

    private JLabel createSectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(ThemeManager.isDark() ? new Color(203, 213, 225) : new Color(51, 65, 85));
        l.setBorder(new EmptyBorder(4, 0, 2, 0));
        return l;
    }

    private void addPresetBtn(JPanel panel, String label, int w) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            widthSpinner.setValue(w);
            onWidthChanged();
        });
        panel.add(btn);
    }

    private void onWidthChanged() {
        if (updatingSpinners) return;
        if (lockAspectCheck.isSelected()) {
            updatingSpinners = true;
            int w = (Integer) widthSpinner.getValue();
            int h = Math.max(30, (int) Math.round(w / aspectRatio));
            heightSpinner.setValue(h);
            updatingSpinners = false;
        }
        updatePreview();
    }

    private void onHeightChanged() {
        if (updatingSpinners) return;
        if (lockAspectCheck.isSelected()) {
            updatingSpinners = true;
            int h = (Integer) heightSpinner.getValue();
            int w = Math.max(40, (int) Math.round(h * aspectRatio));
            widthSpinner.setValue(w);
            updatingSpinners = false;
        }
        updatePreview();
    }

    private void updatePreview() {
        if (previewCanvas != null) previewCanvas.repaint();
    }

    private void drawLivePreview(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int cw = previewCanvas.getWidth();
        int ch = previewCanvas.getHeight();

        // Target box in center of preview
        int maxW = cw - 40;
        int maxH = ch - 60;
        int targetW = Math.min(maxW, Math.max(60, getSelectedWidth() / 3));
        int targetH = (int) Math.round(targetW / aspectRatio);
        if (targetH > maxH) {
            targetH = maxH;
            targetW = (int) Math.round(targetH * aspectRatio);
        }

        int x = (cw - targetW) / 2;
        int y = (ch - targetH - 24) / 2;

        String style = getSelectedStyle();
        String filter = (String) filterCombo.getSelectedItem();

        // Drop shadow style
        if (style.contains("Shadow") || style.contains("Polaroid")) {
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillRoundRect(x + 4, y + 6, targetW, targetH, 12, 12);
        }

        // Draw image or placeholder
        if (originalImage != null) {
            g2.drawImage(originalImage, x, y, targetW, targetH, null);
        } else {
            // Stylized placeholder gradient
            GradientPaint gp = new GradientPaint(x, y, new Color(59, 130, 246), x + targetW, y + targetH, new Color(147, 51, 234));
            g2.setPaint(gp);
            if (style.contains("Circle")) {
                int diam = Math.min(targetW, targetH);
                g2.fillOval(x + (targetW - diam) / 2, y + (targetH - diam) / 2, diam, diam);
            } else if (style.contains("Large Rounded")) {
                g2.fillRoundRect(x, y, targetW, targetH, 20, 20);
            } else if (style.contains("Modern Rounded")) {
                g2.fillRoundRect(x, y, targetW, targetH, 10, 10);
            } else {
                g2.fillRect(x, y, targetW, targetH);
            }
        }

        // Draw Filter overlay
        if (filter != null) {
            if (filter.contains("Grayscale")) {
                g2.setColor(new Color(128, 128, 128, 90));
                g2.fillRect(x, y, targetW, targetH);
            } else if (filter.contains("Sepia")) {
                g2.setColor(new Color(245, 158, 11, 50));
                g2.fillRect(x, y, targetW, targetH);
            } else if (filter.contains("Cool Blue")) {
                g2.setColor(new Color(14, 165, 233, 45));
                g2.fillRect(x, y, targetW, targetH);
            }
        }

        // Border
        g2.setColor(new Color(148, 163, 184));
        if (style.contains("Large Rounded")) {
            g2.drawRoundRect(x, y, targetW, targetH, 20, 20);
        } else if (style.contains("Modern Rounded")) {
            g2.drawRoundRect(x, y, targetW, targetH, 10, 10);
        } else if (style.contains("Circle")) {
            int diam = Math.min(targetW, targetH);
            g2.drawOval(x + (targetW - diam)/2, y + (targetH - diam)/2, diam, diam);
        } else if (!style.contains("Plain")) {
            g2.drawRect(x, y, targetW, targetH);
        }

        // Caption label below
        String cap = getCaption();
        if (cap != null && !cap.isBlank()) {
            g2.setColor(ThemeManager.isDark() ? new Color(148, 163, 184) : new Color(71, 85, 105));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            FontMetrics fm = g2.getFontMetrics();
            int strW = fm.stringWidth(cap);
            g2.drawString(cap, (cw - strW) / 2, y + targetH + 18);
        }
    }

    public boolean isConfirmed() { return confirmed; }
    public int getSelectedWidth() { return (Integer) widthSpinner.getValue(); }
    public int getSelectedHeight() { return (Integer) heightSpinner.getValue(); }
    public String getSelectedAlign() { return (String) alignCombo.getSelectedItem(); }
    public String getSelectedStyle() { return (String) styleCombo.getSelectedItem(); }
    public String getSelectedFilter() { return (String) filterCombo.getSelectedItem(); }
    public String getAltText() { return altTextField.getText().trim(); }
    public String getCaption() { return captionField.getText().trim(); }

    /**
     * Generates a modern CSS style attribute string for embedding the image.
     */
    public String generateImageCssStyle() {
        StringBuilder css = new StringBuilder();
        String style = getSelectedStyle();
        if (style.contains("Large Rounded")) css.append("border-radius:16px;");
        else if (style.contains("Modern Rounded")) css.append("border-radius:8px;");
        else if (style.contains("Circle")) css.append("border-radius:50%;");

        if (style.contains("Shadow")) css.append("box-shadow:0 4px 14px rgba(0,0,0,0.15);");
        else if (style.contains("Clean Thin Border")) css.append("border:1px solid #cbd5e1;");
        else if (style.contains("Polaroid")) css.append("padding:8px;background:#ffffff;box-shadow:0 4px 12px rgba(0,0,0,0.12);");

        String filter = getSelectedFilter();
        if (filter.contains("Grayscale")) css.append("filter:grayscale(100%);");
        else if (filter.contains("Sepia")) css.append("filter:sepia(80%);");
        else if (filter.contains("High Contrast")) css.append("filter:contrast(140%);");

        String align = getSelectedAlign();
        if (align.contains("Float Left")) css.append("float:left;margin:8px 16px 8px 0;");
        else if (align.contains("Float Right")) css.append("float:right;margin:8px 0 8px 16px;");
        else if (align.contains("Center")) css.append("display:block;margin:12px auto;");

        return css.toString();
    }
}
