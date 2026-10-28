package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.ChartEngineService;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/**
 * Intelligent Chart Designer & Studio dialog with real-time visual preview,
 * customizable themes & color palettes, auto-detection engine, and direct high-DPI image generation.
 */
public class ChartDialog extends JDialog {
    private final ChartEngineService chartEngineService;
    private JTextField titleField;
    private JComboBox<String> typeCombo;
    private JComboBox<ChartTheme> themeCombo;
    private JTextArea dataArea;
    private JLabel previewImageLabel;
    private JButton insertBtn;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private boolean confirmed = false;
    private File generatedChartFile = null;
    private File editingFile = null;
    private String existingCardStyle = "MODERN_GLASS";
    private String existingDetailLevel = "FULL";
    private boolean existingShowValues = true;
    private boolean existingShowLegend = true;
    private String customPaletteHex = null;

    public ChartDialog(Frame owner, ChartEngineService chartEngineService) {
        super(owner, "Intelligent Chart Studio & Customizer", true);
        this.chartEngineService = chartEngineService;
        setSize(780, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBorder(new EmptyBorder(14, 16, 14, 16));
        main.setBackground(ThemeManager.getSurface());

        // --- Controls Top Bar ---
        JPanel topForm = new JPanel(new GridLayout(2, 1, 6, 8));
        topForm.setOpaque(false);

        // Row 1: Title & Auto-Detect
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.add(new JLabel("Chart Title:"), BorderLayout.WEST);

        titleField = new JTextField("Quarterly Performance Metrics");
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        titleRow.add(titleField, BorderLayout.CENTER);

        JButton autoDetectBtn = new JButton("Smart Auto-Detect", VectorIcons.createIcon(VectorIcons.REFRESH, 13, new Color(37, 99, 235)));
        autoDetectBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        autoDetectBtn.setFocusPainted(false);
        autoDetectBtn.addActionListener(e -> runAutoDetect());
        titleRow.add(autoDetectBtn, BorderLayout.EAST);
        topForm.add(titleRow);

        // Row 2: Type, Theme, Sample
        JPanel configRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        configRow.setOpaque(false);

        configRow.add(new JLabel("Type:"));
        java.util.List<String> typeList = new java.util.ArrayList<>();
        typeList.add("Auto (smart detect)");
        for (ChartCatalog.TypeInfo info : ChartCatalog.types()) {
            typeList.add(info.label() + " chart");
        }
        typeCombo = new ModernComboBox<>(typeList.toArray(new String[0]));
        typeCombo.setPreferredSize(new Dimension(170, 28));
        typeCombo.addActionListener(e -> updatePreview());
        configRow.add(typeCombo);

        configRow.add(new JLabel("Theme / Palette:"));
        themeCombo = new ModernComboBox<>(ChartTheme.values());
        themeCombo.setPreferredSize(new Dimension(160, 28));
        themeCombo.addActionListener(e -> {
            customPaletteHex = null;
            updatePreview();
        });
        configRow.add(themeCombo);

        JButton paletteStudioBtn = new JButton("Palette...", VectorIcons.createIcon(VectorIcons.PALETTE, 13, new Color(124, 58, 237)));
        paletteStudioBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        paletteStudioBtn.setFocusPainted(false);
        paletteStudioBtn.setToolTipText("Open interactive color palette studio to pick theme & custom series colors");
        paletteStudioBtn.addActionListener(e -> {
            ChartTheme currentTheme = (ChartTheme) themeCombo.getSelectedItem();
            ChartPalettePanel.ChartPaletteChoice choice = new ChartPalettePanel.ChartPaletteChoice(currentTheme, customPaletteHex);
            if (ChartPalettePanel.showDialog(this, choice)) {
                if (choice.theme != null) {
                    themeCombo.setSelectedItem(choice.theme);
                }
                this.customPaletteHex = choice.paletteHex;
                updatePreview();
            }
        });
        configRow.add(paletteStudioBtn);

        JButton sampleBtn = new JButton("Load Sample", VectorIcons.createIcon(VectorIcons.DOCUMENT, 13, new Color(71, 85, 105)));
        sampleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sampleBtn.setFocusPainted(false);
        sampleBtn.addActionListener(e -> loadSample());
        configRow.add(sampleBtn);

        configRow.add(new JLabel("Size:"));
        widthSpinner = new JSpinner(new SpinnerNumberModel(580, 240, 1400, 20));
        widthSpinner.setPreferredSize(new Dimension(68, 28));
        widthSpinner.addChangeListener(e -> updatePreview());
        configRow.add(widthSpinner);

        configRow.add(new JLabel("×"));
        heightSpinner = new JSpinner(new SpinnerNumberModel(290, 160, 900, 10));
        heightSpinner.setPreferredSize(new Dimension(68, 28));
        heightSpinner.addChangeListener(e -> updatePreview());
        configRow.add(heightSpinner);

        topForm.add(configRow);
        main.add(topForm, BorderLayout.NORTH);

        // --- Center Split: Data Editor vs Live Image Preview ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.38);
        splitPane.setDividerSize(6);

        // Left: Data Editor
        JPanel dataPanel = new JPanel(new BorderLayout(0, 4));
        dataPanel.setOpaque(false);
        JLabel dataLabel = new JLabel("Data Points (Label: Value; indent for hierarchy):");
        dataLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dataLabel.setForeground(ThemeManager.getTextSecondary());
        dataPanel.add(dataLabel, BorderLayout.NORTH);

        dataArea = new JTextArea("Q1 Actual: 45\nQ2 Actual: 68\nQ3 Actual: 84\nQ4 Actual: 105", 10, 18);
        dataArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        dataArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        dataArea.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        JScrollPane dataScroll = new JScrollPane(dataArea);
        dataScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder()));
        dataPanel.add(dataScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(dataPanel);

        // Right: Live Graphic Preview
        JPanel previewContainer = new JPanel(new BorderLayout(0, 4));
        previewContainer.setOpaque(false);
        JLabel prevLabel = new JLabel("Real-Time High-DPI Visual Canvas Preview:");
        prevLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        prevLabel.setForeground(ThemeManager.getTextSecondary());
        previewContainer.add(prevLabel, BorderLayout.NORTH);

        previewImageLabel = new JLabel("", SwingConstants.CENTER);
        previewImageLabel.setBackground(ThemeManager.getSurfaceElevated());
        previewImageLabel.setOpaque(true);
        JScrollPane prevScroll = new JScrollPane(previewImageLabel);
        prevScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder()));
        previewContainer.add(prevScroll, BorderLayout.CENTER);
        splitPane.setRightComponent(previewContainer);

        main.add(splitPane, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        // --- Bottom Action Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(ThemeManager.getSurface());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorder()));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.addActionListener(e -> dispose());

        this.insertBtn = new JButton("Insert Chart into Document", VectorIcons.createIcon(VectorIcons.CHART, 14, Color.WHITE));
        insertBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        insertBtn.setBackground(new Color(37, 99, 235));
        insertBtn.setForeground(Color.WHITE);
        insertBtn.setOpaque(true);
        insertBtn.setBorderPainted(false);
        insertBtn.setFocusPainted(false);
        insertBtn.addActionListener(e -> {
            applyAndGenerateFile();
            confirmed = true;
            dispose();
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(insertBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        updatePreview();
    }

    public void loadExistingChart(File chartFile, ChartEngineService.ChartMetadata meta) {
        this.editingFile = chartFile;
        if (meta != null) {
            if (meta.title() != null && !meta.title().isBlank()) titleField.setText(meta.title());
            if (meta.rawData() != null && !meta.rawData().isBlank()) dataArea.setText(meta.rawData());
            if (meta.theme() != null) themeCombo.setSelectedItem(meta.theme());
            if (meta.cardStyle() != null) this.existingCardStyle = meta.cardStyle();
            if (meta.detailLevel() != null) this.existingDetailLevel = meta.detailLevel();
            this.existingShowValues = meta.showValues();
            this.existingShowLegend = meta.showLegend();
            if (meta.paletteHex() != null && !meta.paletteHex().isBlank()) {
                this.customPaletteHex = meta.paletteHex();
            }
            if (meta.type() != null) {
                String typeStr = ChartCatalog.label(meta.type()) + " chart";
                typeCombo.setSelectedItem(typeStr);
            }
        }
        setTitle("Edit Chart Data & Theme — Chart Studio");
        if (insertBtn != null) {
            insertBtn.setText("Update Chart on Page");
        }
        updatePreview();
    }

    public File getGeneratedChartFile() {
        return generatedChartFile;
    }

    public File getEditingFile() {
        return editingFile;
    }

    private void runAutoDetect() {
        String rawData = dataArea.getText();
        var rows = chartEngineService.parseRows(rawData);
        ChartType detected = chartEngineService.autoDetectType(rows);
        typeCombo.setSelectedItem(ChartCatalog.label(detected) + " chart");

        String detectedTitle = chartEngineService.extractTitleFromData(rawData);
        if (!detectedTitle.isBlank() && !detectedTitle.equals("Data Visualization")) {
            titleField.setText(detectedTitle);
        }
        updatePreview();
    }

    private void loadSample() {
        String sel = (String) typeCombo.getSelectedItem();
        ChartType type = (sel == null || sel.startsWith("Auto")) ? ChartType.BAR : ChartCatalog.parse(sel.replace(" chart", ""));
        String sample = ChartCatalog.info(type).sample();
        dataArea.setText(sample);
        titleField.setText(ChartCatalog.label(type) + " Performance Overview");
        updatePreview();
    }

    private void updatePreview() {
        if (previewImageLabel == null || dataArea == null) return;
        ChartType type = getSelectedChartType();
        ChartTheme theme = (ChartTheme) themeCombo.getSelectedItem();
        String title = titleField.getText();
        String rawData = dataArea.getText();
        int w = widthSpinner != null ? (Integer) widthSpinner.getValue() : 580;
        int h = heightSpinner != null ? (Integer) heightSpinner.getValue() : 290;

        BufferedImage img = chartEngineService.renderChartToImage(type, rawData, theme, title, Math.min(520, w), Math.min(280, h),
                existingCardStyle, existingDetailLevel, existingShowValues, existingShowLegend, customPaletteHex);
        previewImageLabel.setIcon(new ImageIcon(img));
    }

    private void applyAndGenerateFile() {
        ChartType type = getSelectedChartType();
        ChartTheme theme = (ChartTheme) themeCombo.getSelectedItem();
        String title = titleField.getText();
        String rawData = dataArea.getText();
        int w = widthSpinner != null ? (Integer) widthSpinner.getValue() : 580;
        int h = heightSpinner != null ? (Integer) heightSpinner.getValue() : 290;

        try {
            generatedChartFile = chartEngineService.renderChartToFile(type, rawData, theme, title, w, h,
                    existingCardStyle, existingDetailLevel, existingShowValues, existingShowLegend, customPaletteHex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private ChartType getSelectedChartType() {
        String typeStr = (String) typeCombo.getSelectedItem();
        if (typeStr == null || typeStr.startsWith("Auto")) return ChartType.AUTO;
        return ChartCatalog.parse(typeStr.replace(" chart", ""));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String generateChartHtml() {
        String title = titleField != null ? titleField.getText() : "Chart";
        int reqW = widthSpinner != null ? (Integer) widthSpinner.getValue() : 580;
        int reqH = heightSpinner != null ? (Integer) heightSpinner.getValue() : 290;
        int maxW = 620;
        int w = Math.min(reqW, maxW);
        int h = (int) Math.round(reqH * ((double) w / reqW));
        if (generatedChartFile == null || !generatedChartFile.exists()) {
            applyAndGenerateFile();
        }
        if (generatedChartFile != null && generatedChartFile.exists()) {
            String url;
            try {
                url = generatedChartFile.toURI().toURL().toExternalForm();
            } catch (Exception e) {
                url = "file:/" + generatedChartFile.getAbsolutePath().replace('\\', '/').replaceAll("^/+", "");
            }
            return "<p align='center' style='text-align:center;margin:16px auto;'>" +
                    "<img src='" + url + "' width='" + w + "' height='" + h + "'><br>" +
                    "<span style='font-size:11px;color:#64748b;font-style:italic;'>Figure: " + escapeHtml(title) + "</span>" +
                    "</p><p><br></p>";
        }

        return "<p><b>[Chart Generation Error]</b></p>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
