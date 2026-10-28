package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Customized global modern color picker dialog featuring an interactive 2D HSB gradient canvas,
 * hue slider, live swatch preview, RGB controls, HEX input, and curated color palettes.
 */
public class ModernColorPickerDialog extends JDialog {

    private Color selectedColor;
    private Color initialColor;
    private boolean confirmed = false;

    private float hue = 0.6f;        // 0.0 to 1.0
    private float saturation = 0.8f; // 0.0 to 1.0
    private float brightness = 0.9f; // 0.0 to 1.0

    private HsbCanvas hsbCanvas;
    private HueSlider hueSlider;
    private JPanel currentSwatch;
    private JPanel newSwatch;
    private JTextField hexField;
    private JSlider rSlider, gSlider, bSlider;
    private JLabel rVal, gVal, bVal;

    private static final String[] PRESET_PALETTES = {
            "#2563eb", "#0284c7", "#059669", "#16a34a", "#ca8a04", "#ea580c", "#dc2626", "#db2777", "#9333ea", "#4f46e5",
            "#bfdbfe", "#bae6fd", "#a7f3d0", "#bbf7d0", "#fef08a", "#fed7aa", "#fecaca", "#fbcfe8", "#e9d5ff", "#c7d2fe",
            "#0f172a", "#1e293b", "#334155", "#475569", "#64748b", "#94a3b8", "#cbd5e1", "#e2e8f0", "#78350f", "#713f12"
    };

    public static Color showDialog(Component parent, String title, Color initialColor) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        ModernColorPickerDialog dialog = new ModernColorPickerDialog(window, title, initialColor);
        dialog.setVisible(true);
        return dialog.confirmed ? dialog.selectedColor : null;
    }

    public ModernColorPickerDialog(Window owner, String title, Color initial) {
        super(owner, title != null ? title : "Select Color", ModalityType.APPLICATION_MODAL);
        this.initialColor = initial != null ? initial : new Color(37, 99, 235);
        this.selectedColor = this.initialColor;

        float[] hsb = Color.RGBtoHSB(this.initialColor.getRed(), this.initialColor.getGreen(), this.initialColor.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];

        initUi();
    }

    private void initUi() {
        setSize(540, 480);
        setResizable(false);
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(ThemeManager.getSurface());
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Center Area: HSB Canvas + Hue Slider + Swatch / Sliders
        JPanel centerPanel = new JPanel(new BorderLayout(14, 14));
        centerPanel.setOpaque(false);

        // HSB 2D Canvas + Hue Bar
        JPanel visualPickerPanel = new JPanel(new BorderLayout(10, 0));
        visualPickerPanel.setOpaque(false);

        hsbCanvas = new HsbCanvas();
        hsbCanvas.setPreferredSize(new Dimension(240, 200));
        visualPickerPanel.add(hsbCanvas, BorderLayout.CENTER);

        hueSlider = new HueSlider();
        hueSlider.setPreferredSize(new Dimension(26, 200));
        visualPickerPanel.add(hueSlider, BorderLayout.EAST);

        centerPanel.add(visualPickerPanel, BorderLayout.WEST);

        // Right side: Previews, HEX, RGB Sliders
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setOpaque(false);
        controlsPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        // Swatch Before / After
        JPanel previewRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        previewRow.setOpaque(false);

        currentSwatch = new JPanel();
        currentSwatch.setPreferredSize(new Dimension(54, 40));
        currentSwatch.setBackground(initialColor);
        currentSwatch.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        currentSwatch.setToolTipText("Original Color");

        newSwatch = new JPanel();
        newSwatch.setPreferredSize(new Dimension(64, 40));
        newSwatch.setBackground(selectedColor);
        newSwatch.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        newSwatch.setToolTipText("New Selected Color");

        previewRow.add(new JLabel("Before:"));
        previewRow.add(currentSwatch);
        previewRow.add(new JLabel("New:"));
        previewRow.add(newSwatch);
        controlsPanel.add(previewRow);
        controlsPanel.add(Box.createVerticalStrut(12));

        // Hex input
        JPanel hexRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hexRow.setOpaque(false);
        hexRow.add(new JLabel("HEX:"));
        hexField = new JTextField(toHex(selectedColor), 8);
        hexField.setFont(new Font("Monospaced", Font.BOLD, 13));
        hexField.addActionListener(e -> applyHexInput());
        hexField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                applyHexInput();
            }
        });
        hexRow.add(hexField);
        controlsPanel.add(hexRow);
        controlsPanel.add(Box.createVerticalStrut(10));

        // RGB Sliders
        rVal = new JLabel(String.valueOf(selectedColor.getRed()));
        rSlider = createRgbSlider(selectedColor.getRed(), rVal, e -> onRgbChanged());
        controlsPanel.add(createRgbRow("R:", rSlider, rVal));

        gVal = new JLabel(String.valueOf(selectedColor.getGreen()));
        gSlider = createRgbSlider(selectedColor.getGreen(), gVal, e -> onRgbChanged());
        controlsPanel.add(createRgbRow("G:", gSlider, gVal));

        bVal = new JLabel(String.valueOf(selectedColor.getBlue()));
        bSlider = createRgbSlider(selectedColor.getBlue(), bVal, e -> onRgbChanged());
        controlsPanel.add(createRgbRow("B:", bSlider, bVal));

        centerPanel.add(controlsPanel, BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.NORTH);

        // 2. Preset Palettes Grid
        JPanel palettePanel = new JPanel(new BorderLayout(4, 6));
        palettePanel.setOpaque(false);
        palettePanel.setBorder(BorderFactory.createTitledBorder("Curated Modern Palettes"));

        JPanel swatchesGrid = new JPanel(new GridLayout(3, 10, 6, 6));
        swatchesGrid.setOpaque(false);

        for (String hex : PRESET_PALETTES) {
            Color c = Color.decode(hex);
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(22, 22));
            b.setBackground(c);
            b.setContentAreaFilled(false);
            b.setOpaque(true);
            b.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 40), 1));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> selectColor(c));
            swatchesGrid.add(b);
        }
        palettePanel.add(swatchesGrid, BorderLayout.CENTER);
        root.add(palettePanel, BorderLayout.CENTER);

        // 3. Bottom Button Row
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton applyBtn = new JButton("Apply Color");
        applyBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        applyBtn.setBackground(ThemeManager.getAccent());
        applyBtn.setForeground(Color.WHITE);
        applyBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(applyBtn);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createRgbRow(String label, JSlider slider, JLabel valLbl) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(18, 20));
        p.add(l);
        slider.setPreferredSize(new Dimension(110, 22));
        p.add(slider);
        valLbl.setPreferredSize(new Dimension(28, 20));
        p.add(valLbl);
        return p;
    }

    private JSlider createRgbSlider(int initial, JLabel valLbl, ChangeListener cl) {
        JSlider s = new JSlider(0, 255, initial);
        s.setOpaque(false);
        s.addChangeListener(cl);
        return s;
    }

    private void onRgbChanged() {
        int r = rSlider.getValue();
        int g = gSlider.getValue();
        int b = bSlider.getValue();
        rVal.setText(String.valueOf(r));
        gVal.setText(String.valueOf(g));
        bVal.setText(String.valueOf(b));

        Color c = new Color(r, g, b);
        updateFromColor(c, false);
    }

    private void applyHexInput() {
        String s = hexField.getText().trim();
        if (s.startsWith("#")) s = s.substring(1);
        try {
            if (s.length() == 6) {
                int r = Integer.parseInt(s.substring(0, 2), 16);
                int g = Integer.parseInt(s.substring(2, 4), 16);
                int b = Integer.parseInt(s.substring(4, 6), 16);
                selectColor(new Color(r, g, b));
            }
        } catch (Exception ignored) {}
    }

    private void selectColor(Color c) {
        updateFromColor(c, true);
    }

    private void updateFromColor(Color c, boolean updateSliders) {
        this.selectedColor = c;
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];

        newSwatch.setBackground(selectedColor);
        hexField.setText(toHex(selectedColor));

        if (updateSliders) {
            rSlider.setValue(selectedColor.getRed());
            gSlider.setValue(selectedColor.getGreen());
            bSlider.setValue(selectedColor.getBlue());
            rVal.setText(String.valueOf(selectedColor.getRed()));
            gVal.setText(String.valueOf(selectedColor.getGreen()));
            bVal.setText(String.valueOf(selectedColor.getBlue()));
        }

        hsbCanvas.repaint();
        hueSlider.repaint();
    }

    private void updateFromHsb() {
        this.selectedColor = Color.getHSBColor(hue, saturation, brightness);
        newSwatch.setBackground(selectedColor);
        hexField.setText(toHex(selectedColor));

        rSlider.setValue(selectedColor.getRed());
        gSlider.setValue(selectedColor.getGreen());
        bSlider.setValue(selectedColor.getBlue());
        rVal.setText(String.valueOf(selectedColor.getRed()));
        gVal.setText(String.valueOf(selectedColor.getGreen()));
        bVal.setText(String.valueOf(selectedColor.getBlue()));

        hsbCanvas.repaint();
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // 2D Saturation / Brightness Canvas
    private class HsbCanvas extends JPanel {
        private BufferedImage cache;

        public HsbCanvas() {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { pick(e.getX(), e.getY()); }
                @Override
                public void mouseDragged(MouseEvent e) { pick(e.getX(), e.getY()); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void pick(int x, int y) {
            saturation = Math.max(0f, Math.min(1f, (float) x / getWidth()));
            brightness = Math.max(0f, Math.min(1f, 1.0f - ((float) y / getHeight())));
            updateFromHsb();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            }

            for (int y = 0; y < h; y++) {
                float b = 1.0f - ((float) y / h);
                for (int x = 0; x < w; x++) {
                    float s = (float) x / w;
                    cache.setRGB(x, y, Color.HSBtoRGB(hue, s, b));
                }
            }
            g.drawImage(cache, 0, 0, null);

            // Crosshair indicator
            int kx = (int) (saturation * w);
            int ky = (int) ((1.0f - brightness) * h);
            g.setColor(brightness < 0.4f ? Color.WHITE : Color.BLACK);
            g.drawOval(kx - 5, ky - 5, 10, 10);
            g.setColor(brightness < 0.4f ? Color.BLACK : Color.WHITE);
            g.drawOval(kx - 6, ky - 6, 12, 12);
        }
    }

    // Vertical Hue Rainbow Slider
    private class HueSlider extends JPanel {
        public HueSlider() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { pick(e.getY()); }
                @Override
                public void mouseDragged(MouseEvent e) { pick(e.getY()); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void pick(int y) {
            hue = Math.max(0f, Math.min(1f, (float) y / getHeight()));
            updateFromHsb();
            hueSlider.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            for (int y = 0; y < h; y++) {
                float hVal = (float) y / h;
                g.setColor(Color.getHSBColor(hVal, 1.0f, 1.0f));
                g.drawLine(0, y, w, y);
            }

            // Arrow pointer
            int py = (int) (hue * h);
            g.setColor(Color.WHITE);
            g.fillRect(0, py - 2, w, 4);
            g.setColor(Color.BLACK);
            g.drawRect(0, py - 2, w - 1, 4);
        }
    }
}
