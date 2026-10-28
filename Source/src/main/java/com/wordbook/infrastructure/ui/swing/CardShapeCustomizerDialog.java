package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modern Customizer Dialog for Cards, Shapes, Flows, and Callouts.
 * Allows editing text values, titles, subtitles, metrics, and color palettes
 * with an interactive real-time live preview.
 */
public class CardShapeCustomizerDialog extends JDialog {

    public record ThemePalette(
            String name,
            Color primary,
            Color bg,
            Color border,
            Color text,
            Color badgeBg,
            Color badgeText,
            String hexPrimary,
            String hexBg,
            String hexBorder,
            String hexText,
            String hexBadgeBg,
            String hexBadgeText
    ) {}

    public static final Map<String, ThemePalette> PALETTES = new LinkedHashMap<>();

    static {
        PALETTES.put("BLUE", new ThemePalette(
                "Corporate Blue",
                new Color(37, 99, 235), new Color(239, 246, 255), new Color(59, 130, 246),
                new Color(30, 58, 138), new Color(219, 234, 254), new Color(29, 78, 216),
                "#2563eb", "#eff6ff", "#3b82f6", "#1e3a8a", "#dbeafe", "#1d4ed8"
        ));
        PALETTES.put("EMERALD", new ThemePalette(
                "Emerald Mint",
                new Color(16, 185, 129), new Color(240, 253, 244), new Color(34, 197, 94),
                new Color(20, 83, 45), new Color(220, 252, 231), new Color(21, 128, 61),
                "#10b981", "#f0fdf4", "#22c55e", "#14532d", "#dcfce7", "#15803d"
        ));
        PALETTES.put("PURPLE", new ThemePalette(
                "Violet Purple",
                new Color(124, 58, 237), new Color(245, 243, 255), new Color(139, 92, 246),
                new Color(76, 29, 149), new Color(237, 233, 254), new Color(109, 40, 217),
                "#7c3aed", "#f5f3ff", "#8b5cf6", "#4c1d95", "#ede9fe", "#6d28d9"
        ));
        PALETTES.put("AMBER", new ThemePalette(
                "Amber Gold",
                new Color(245, 158, 11), new Color(254, 252, 232), new Color(251, 191, 36),
                new Color(120, 53, 15), new Color(254, 243, 199), new Color(180, 83, 9),
                "#f59e0b", "#fefce8", "#fbbf24", "#78350f", "#fef3c7", "#b45309"
        ));
        PALETTES.put("CRIMSON", new ThemePalette(
                "Crimson Rose",
                new Color(225, 29, 72), new Color(255, 241, 242), new Color(244, 63, 94),
                new Color(136, 19, 55), new Color(255, 228, 230), new Color(190, 18, 60),
                "#e11d48", "#fff1f2", "#f43f5e", "#881337", "#ffe4e6", "#be123c"
        ));
        PALETTES.put("SLATE", new ThemePalette(
                "Dark Slate",
                new Color(51, 65, 85), new Color(15, 23, 42), new Color(71, 85, 105),
                new Color(248, 250, 252), new Color(30, 41, 59), new Color(226, 232, 240),
                "#334155", "#0f172a", "#475569", "#f8fafc", "#1e293b", "#e2e8f0"
        ));
        PALETTES.put("MINIMAL", new ThemePalette(
                "Clean Minimal",
                new Color(100, 116, 139), new Color(255, 255, 255), new Color(203, 213, 225),
                new Color(15, 23, 42), new Color(241, 245, 249), new Color(71, 85, 105),
                "#64748b", "#ffffff", "#cbd5e1", "#0f172a", "#f1f5f9", "#475569"
        ));
    }

    public static ThemePalette getPalette(String key) {
        if (key == null) return PALETTES.get("BLUE");
        String norm = key.trim().toUpperCase();
        if (norm.equals("CORPORATE")) norm = "BLUE";
        else if (norm.equals("VIOLET")) norm = "PURPLE";
        else if (norm.equals("ROSE")) norm = "CRIMSON";
        else if (norm.equals("CLEAN")) norm = "MINIMAL";
        ThemePalette p = PALETTES.get(norm);
        return p != null ? p : PALETTES.get("BLUE");
    }

    private final JTextField titleField;
    private final JTextField valueField;
    private final JTextField subtitleField;
    private String selectedThemeKey = "BLUE";
    private final String cardShapeType;
    private final JPanel previewPanel;
    private boolean confirmed = false;

    public CardShapeCustomizerDialog(
            Frame owner,
            String initialTitle,
            String initialValue,
            String initialSubtitle,
            String initialThemeKey
    ) {
        this(owner, "Card & Shape", initialTitle, initialValue, initialSubtitle, initialThemeKey, null);
    }

    public CardShapeCustomizerDialog(
            Frame owner,
            String cardShapeType,
            String initialTitle,
            String initialValue,
            String initialSubtitle,
            String initialThemeKey,
            Consumer<CardShapeResult> onApply
    ) {
        super(owner, "Customize Card & Shape Values", true);
        this.cardShapeType = cardShapeType != null ? cardShapeType : "Card";
        if (initialThemeKey != null && PALETTES.containsKey(initialThemeKey.toUpperCase())) {
            this.selectedThemeKey = initialThemeKey.toUpperCase();
        }

        setSize(520, 560);
        setMinimumSize(new Dimension(480, 500));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(248, 250, 252));
        headerPanel.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel titleLabel = new JLabel("Customize: " + this.cardShapeType);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(ThemeManager.getTextPrimary());
        JLabel subLabel = new JLabel("Edit live text, headline metric values, subtitles, and color theme.");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(ThemeManager.getTextSecondary());
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Center: Form + Live Preview
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerPanel.setBackground(ThemeManager.getSurface());

        // 1. Live Preview Panel
        JLabel prevTitle = new JLabel("LIVE PREVIEW");
        prevTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        prevTitle.setForeground(new Color(100, 116, 139));
        centerPanel.add(prevTitle);
        centerPanel.add(Box.createVerticalStrut(4));

        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                renderLivePreview(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        previewPanel.setPreferredSize(new Dimension(460, 110));
        previewPanel.setMaximumSize(new Dimension(800, 110));
        previewPanel.setOpaque(true);
        previewPanel.setBackground(ThemeManager.isDark() ? new Color(24, 33, 47) : new Color(241, 245, 249));
        previewPanel.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true));
        centerPanel.add(previewPanel);
        centerPanel.add(Box.createVerticalStrut(14));

        // 2. Text Fields
        JPanel formGrid = new JPanel(new GridLayout(3, 2, 8, 10));
        formGrid.setOpaque(false);
        formGrid.setMaximumSize(new Dimension(800, 105));

        formGrid.add(new JLabel("Card Title / Header:"));
        titleField = new JTextField(initialTitle != null ? initialTitle : "Executive Milestone");
        formGrid.add(titleField);

        formGrid.add(new JLabel("Primary Metric / Value:"));
        valueField = new JTextField(initialValue != null ? initialValue : "$128,450");
        formGrid.add(valueField);

        formGrid.add(new JLabel("Subtitle / Description:"));
        subtitleField = new JTextField(initialSubtitle != null ? initialSubtitle : "Target completion rate vs last quarter");
        formGrid.add(subtitleField);

        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { previewPanel.repaint(); }
            @Override public void removeUpdate(DocumentEvent e) { previewPanel.repaint(); }
            @Override public void changedUpdate(DocumentEvent e) { previewPanel.repaint(); }
        };
        titleField.getDocument().addDocumentListener(dl);
        valueField.getDocument().addDocumentListener(dl);
        subtitleField.getDocument().addDocumentListener(dl);

        centerPanel.add(formGrid);
        centerPanel.add(Box.createVerticalStrut(14));

        // 3. Theme Palette Picker
        JLabel paletteTitle = new JLabel("CHOOSE COLOR & THEME PALETTE");
        paletteTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        paletteTitle.setForeground(new Color(100, 116, 139));
        centerPanel.add(paletteTitle);
        centerPanel.add(Box.createVerticalStrut(6));

        JPanel paletteRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        paletteRow.setOpaque(false);
        ButtonGroup group = new ButtonGroup();

        for (Map.Entry<String, ThemePalette> entry : PALETTES.entrySet()) {
            String key = entry.getKey();
            ThemePalette pal = entry.getValue();
            JToggleButton btn = new JToggleButton(pal.name);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setIcon(createColorDotIcon(pal.primary));
            if (key.equalsIgnoreCase(selectedThemeKey)) {
                btn.setSelected(true);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            }
            btn.addActionListener(e -> {
                selectedThemeKey = key;
                previewPanel.repaint();
            });
            group.add(btn);
            paletteRow.add(btn);
        }
        centerPanel.add(paletteRow);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(248, 250, 252));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorder()));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.addActionListener(e -> dispose());
        buttonPanel.add(cancelBtn);

        JButton applyBtn = new JButton("Apply & Update", VectorIcons.createIcon(VectorIcons.CHECKMARK, 14, Color.WHITE));
        applyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        applyBtn.setBackground(new Color(37, 99, 235));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setOpaque(true);
        applyBtn.setBorderPainted(false);
        applyBtn.setFocusPainted(false);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.addActionListener(e -> {
            confirmed = true;
            if (onApply != null) {
                onApply.accept(new CardShapeResult(
                        titleField.getText().trim(),
                        valueField.getText().trim(),
                        subtitleField.getText().trim(),
                        selectedThemeKey,
                        PALETTES.get(selectedThemeKey)
                ));
            }
            dispose();
        });
        buttonPanel.add(applyBtn);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private Icon createColorDotIcon(Color c) {
        return new Icon() {
            @Override public void paintIcon(Component cmp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x, y + 2, 10, 10);
                g2.setColor(new Color(0, 0, 0, 30));
                g2.drawOval(x, y + 2, 10, 10);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 12; }
            @Override public int getIconHeight() { return 14; }
        };
    }

    private void renderLivePreview(Graphics2D g2, int w, int h) {
        ThemePalette pal = PALETTES.getOrDefault(selectedThemeKey, PALETTES.get("BLUE"));
        String title = titleField.getText().trim();
        String val = valueField.getText().trim();
        String sub = subtitleField.getText().trim();

        int pad = 12;
        int cardW = Math.min(w - 24, 380);
        int cardH = h - 20;
        int cx = (w - cardW) / 2;
        int cy = (h - cardH) / 2;

        g2.setColor(pal.bg);
        g2.fillRoundRect(cx, cy, cardW, cardH, 10, 10);
        g2.setColor(pal.border);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(cx, cy, cardW, cardH, 10, 10);

        // Accent strip on left
        g2.setColor(pal.primary);
        g2.fillRoundRect(cx + 2, cy + 4, 4, cardH - 8, 2, 2);

        // Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.setColor(pal.primary);
        g2.drawString(title.isEmpty() ? "TITLE" : title.toUpperCase(), cx + 16, cy + 18);

        // Value
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(pal.text);
        g2.drawString(val.isEmpty() ? "Value / Metric" : val, cx + 16, cy + 46);

        // Subtitle & Badge
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(pal.text);
        g2.drawString(sub.isEmpty() ? "Subtitle description line" : sub, cx + 16, cy + 68);

        // Badge pill in bottom-right or right edge
        int bw = 65, bh = 18;
        int bx = cx + cardW - bw - 12;
        int by = cy + 10;
        g2.setColor(pal.badgeBg);
        g2.fillRoundRect(bx, by, bw, bh, 9, 9);
        g2.setColor(pal.badgeText);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.drawString("ACTIVE", bx + 16, by + 13);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getTitleText() {
        return titleField != null ? titleField.getText().trim() : "";
    }

    public String getValueText() {
        return valueField != null ? valueField.getText().trim() : "";
    }

    public String getSubtitleText() {
        return subtitleField != null ? subtitleField.getText().trim() : "";
    }

    public String getSelectedThemeKey() {
        return selectedThemeKey;
    }

    public record CardShapeResult(
            String title,
            String value,
            String subtitle,
            String themeKey,
            ThemePalette palette
    ) {}
}
