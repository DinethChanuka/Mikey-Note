package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.ChartEngineService;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Intelligent Microsoft Word-style Format Sidebar Panel.
 * Dynamically adapts to show relevant properties for the currently selected object:
 *
 * 1. PICTURE: Image filters (B&W, Sepia, Vivid, Invert), aspect scaling,
 *    fit page width, photo borders, corner rounding, drop shadows, and captions.
 * 2. CHART: Chart type switcher (Bar, Line, Area, Pie, Donut, KPI),
 *    color themes (Corporate, Emerald, Violet, etc.), data labels/legend toggles,
 *    and direct "Edit Chart Data..." dialog access.
 * 3. METRIC_CARD: KPI title, large metric value, subtitle, color themes,
 *    fill color, border outline, corner radius, and elevation.
 * 4. SHAPE / CALLOUT: Callout styles (Info, Tip, Warning, Danger, Quote),
 *    fill/accent color, border stroke, corner radius, title & content text.
 * 5. TABLE: Table themes (Clean Blue, Zebra, Minimalist), row/column insertion
 *    and deletion tools, cell coloring, and grid borders.
 * 6. VIDEO: Dimensions, alignment, playback launcher, and removal.
 */
public class ShapeFormatSidePanel extends JPanel implements ThemeManager.ThemeChangeListener {

    public enum ObjectType {
        PICTURE("Format Picture", "Image", VectorIcons.Type.IMAGE, new Color(37, 99, 235)),
        CHART("Format Chart", "Chart / Graph", VectorIcons.Type.CHART, new Color(16, 185, 129)),
        METRIC_CARD("Format Metric Card", "KPI Card", VectorIcons.Type.KPI_CARD, new Color(139, 92, 246)),
        SHAPE("Format Shape", "Shape / Callout", VectorIcons.Type.SHAPE, new Color(14, 165, 233)),
        TABLE("Format Table", "Table", VectorIcons.Type.TABLE, new Color(245, 158, 11)),
        VIDEO("Format Video", "Video Media", VectorIcons.Type.VIDEO, new Color(239, 68, 68));

        public final String title;
        public final String badge;
        public final VectorIcons.Type iconType;
        public final Color accent;

        ObjectType(String title, String badge, VectorIcons.Type iconType, Color accent) {
            this.title = title;
            this.badge = badge;
            this.iconType = iconType;
            this.accent = accent;
        }
    }

    private final DocumentCanvasPanel canvasPanel;
    private Runnable closeCallback;

    private Element currentElement;
    private ObjectType currentObjectType = ObjectType.METRIC_CARD;
    private boolean updatingUi = false;

    // Header controls
    private final JLabel headerIconLabel;
    private final JLabel headerTitleLabel;
    private final JLabel headerBadgeLabel;
    private final JButton closeBtn;
    private final JButton widthToggleBtn;

    // Body Deck & Containers
    private final JPanel mainDeck;
    private final JPanel emptyStatePanel;
    private final JPanel contentCardsPanel;

    // Dynamic Sections (Shown / Hidden based on ObjectType)
    private final JPanel pictureFilterSection;
    private final JPanel chartTypeSection;
    private final JPanel chartThemeSection;
    private final JPanel chartOptionsSection;
    private final JPanel cardThemesSection;
    private final JPanel fillSection;
    private final JPanel borderSection;
    private final JPanel shadowSection;
    private final JPanel sizeSection;
    private final JPanel cardTextSection;
    private final JPanel shapeTextSection;
    private final JPanel tableToolsSection;
    private final JPanel actionsSection;

    // Current State
    private String selectedThemeKey = "BLUE";
    private Color currentFillColor = new Color(239, 246, 255);
    private Color currentBorderColor = new Color(59, 130, 246);
    private int currentBorderWidth = 2;
    private String currentBorderStyle = "solid";
    private int currentBorderRadius = 10;
    private String currentShadowPreset = "soft";

    private int currentWidth = 520;
    private int currentHeight = 220;
    private String currentAlign = "center";
    private boolean lockAspectRatio = false;
    private double originalAspectRatio = 1.0;

    // UI Input Controls
    private JPanel fillSwatchPreview;
    private JPanel borderSwatchPreview;
    private JSpinner borderWidthSpinner;
    private JComboBox<String> borderStyleCombo;
    private JSlider radiusSlider;
    private JLabel radiusLabel;
    private JComboBox<String> shadowCombo;

    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private JCheckBox aspectCheckbox;

    // Card text fields
    private JTextField titleField;
    private JTextField valueField;
    private JTextField subtitleField;

    // Shape text fields
    private JTextField shapeTitleField;
    private JTextArea shapeContentArea;

    // Chart controls
    private JComboBox<String> chartTypeCombo;
    private JCheckBox chartShowValuesCheck;
    private JCheckBox chartShowLegendCheck;

    private final Timer debouncedApplyTimer;

    public ShapeFormatSidePanel(DocumentCanvasPanel canvasPanel) {
        this.canvasPanel = canvasPanel;
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(280, 160));
        setPreferredSize(new Dimension(360, 600));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(226, 232, 240)));

        ThemeManager.addListener(this);

        debouncedApplyTimer = new Timer(75, e -> performApply());
        debouncedApplyTimer.setRepeats(false);

        // --- 1. Top Header Bar ---
        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setOpaque(false);
        headerBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(10, 14, 10, 10)
        ));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);

        headerIconLabel = new JLabel(VectorIcons.create(VectorIcons.Type.SHAPE, 16, new Color(14, 165, 233)));
        titleBox.add(headerIconLabel);

        headerTitleLabel = new JLabel("Format Shape");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerTitleLabel.setForeground(new Color(15, 23, 42));
        titleBox.add(headerTitleLabel);

        headerBadgeLabel = new JLabel("Card");
        headerBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        headerBadgeLabel.setForeground(new Color(100, 116, 139));
        headerBadgeLabel.setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(1, 5, 1, 5)
        ));
        titleBox.add(headerBadgeLabel);

        headerBar.add(titleBox, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        headerActions.setOpaque(false);

        widthToggleBtn = new JButton(VectorIcons.create(VectorIcons.Type.SETTINGS, 13, new Color(100, 116, 139)));
        widthToggleBtn.setPreferredSize(new Dimension(26, 26));
        widthToggleBtn.setBorder(BorderFactory.createEmptyBorder());
        widthToggleBtn.setContentAreaFilled(false);
        widthToggleBtn.setFocusPainted(false);
        widthToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        widthToggleBtn.setToolTipText("Toggle standard (340px) / expanded (440px) width");
        widthToggleBtn.addActionListener(e -> {
            int curW = getWidth();
            int newW = (curW < 380) ? 440 : 340;
            setPreferredSize(new Dimension(newW, getHeight()));
            JSplitPane sp = findAncestorSplitPane();
            if (sp != null) {
                int maxAllowed = Math.max(280, (int) (sp.getWidth() * 0.45));
                newW = Math.min(newW, maxAllowed);
                sp.setDividerLocation(sp.getWidth() - newW);
            }
            revalidate();
            repaint();
        });
        headerActions.add(widthToggleBtn);

        closeBtn = new JButton(VectorIcons.create(VectorIcons.Type.CLOSE, 13, new Color(100, 116, 139)));
        closeBtn.setPreferredSize(new Dimension(26, 26));
        closeBtn.setBorder(BorderFactory.createEmptyBorder());
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText("Close Format Pane");
        closeBtn.addActionListener(e -> {
            if (closeCallback != null) closeCallback.run();
        });
        headerActions.add(closeBtn);

        headerBar.add(headerActions, BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        // --- 2. Main Content Panels ---
        contentCardsPanel = new JPanel();
        contentCardsPanel.setLayout(new BoxLayout(contentCardsPanel, BoxLayout.Y_AXIS));
        contentCardsPanel.setOpaque(false);
        contentCardsPanel.setBorder(new EmptyBorder(12, 14, 24, 14));

        // Section: Picture Filters
        pictureFilterSection = createPictureFilterSection();
        contentCardsPanel.add(pictureFilterSection);

        // Section: Chart Type
        chartTypeSection = createChartTypeSection();
        contentCardsPanel.add(chartTypeSection);

        // Section: Chart Themes
        chartThemeSection = createChartThemeSection();
        contentCardsPanel.add(chartThemeSection);

        // Section: Chart Display Options
        chartOptionsSection = createChartOptionsSection();
        contentCardsPanel.add(chartOptionsSection);

        // Section: Card Themes
        cardThemesSection = createCardThemesSection();
        contentCardsPanel.add(cardThemesSection);

        // Section: Fill & Background
        fillSection = createFillSection();
        contentCardsPanel.add(fillSection);

        // Section: Line & Border
        borderSection = createBorderSection();
        contentCardsPanel.add(borderSection);

        // Section: Shadow & Elevation
        shadowSection = createShadowSection();
        contentCardsPanel.add(shadowSection);

        // Section: Size & Alignment
        sizeSection = createSizeSection();
        contentCardsPanel.add(sizeSection);

        // Section: Card Text
        cardTextSection = createCardTextSection();
        contentCardsPanel.add(cardTextSection);

        // Section: Shape Text
        shapeTextSection = createShapeTextSection();
        contentCardsPanel.add(shapeTextSection);

        // Section: Table Tools
        tableToolsSection = createTableToolsSection();
        contentCardsPanel.add(tableToolsSection);

        // Section: Actions
        actionsSection = createActionsSection();
        contentCardsPanel.add(actionsSection);

        // ScrollPane with smooth scrolling
        JScrollPane scrollPane = new JScrollPane(contentCardsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // Empty state overlay
        emptyStatePanel = createEmptyStatePanel();

        mainDeck = new JPanel(new CardLayout());
        mainDeck.setOpaque(false);
        mainDeck.add(scrollPane, "CONTENT");
        mainDeck.add(emptyStatePanel, "EMPTY");

        add(mainDeck, BorderLayout.CENTER);
        showEmptyState();
    }

    private JSplitPane findAncestorSplitPane() {
        Container p = getParent();
        while (p != null) {
            if (p instanceof JSplitPane sp) return sp;
            p = p.getParent();
        }
        return null;
    }

    public void setCloseCallback(Runnable callback) {
        this.closeCallback = callback;
    }

    // =========================================================================
    // SECTION CREATION HELPERS
    // =========================================================================

    private JPanel createPictureFilterSection() {
        JPanel section = createSectionCard("PICTURE FILTERS & STYLES");
        JPanel grid = new JPanel(new GridLayout(2, 3, 6, 6));
        grid.setOpaque(false);

        String[][] filters = {
                {"Original", "Reset to original colors"},
                {"Grayscale", "Black & white monochrome"},
                {"Sepia", "Vintage warm tone"},
                {"Vivid", "High color saturation"},
                {"Contrast", "High dynamic contrast"},
                {"Invert", "Inverted colors"}
        };

        for (String[] f : filters) {
            String name = f[0];
            String tip = f[1];
            JButton btn = new JButton(name);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(15, 23, 42));
            btn.setFocusPainted(false);
            btn.setToolTipText(tip);
            btn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(226, 232, 240), 1, true),
                    new EmptyBorder(5, 4, 5, 4)
            ));
            btn.addActionListener(e -> {
                if (canvasPanel != null) {
                    canvasPanel.applyImageFilter(name);
                }
            });
            grid.add(btn);
        }

        section.add(grid);
        return section;
    }

    private JPanel createChartTypeSection() {
        JPanel section = createSectionCard("CHART VISUALIZATION TYPE");
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel lbl = new JLabel("Chart Type:");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(lbl, BorderLayout.WEST);

        String[] types = {"Bar Chart", "Line Chart", "Area Chart", "Pie Chart", "Donut Chart", "KPI Card"};
        chartTypeCombo = new JComboBox<>(types);
        chartTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chartTypeCombo.addActionListener(e -> {
            if (updatingUi) return;
            String sel = (String) chartTypeCombo.getSelectedItem();
            if (sel != null && canvasPanel != null) {
                canvasPanel.switchSelectedChartType(sel);
            }
        });
        row.add(chartTypeCombo, BorderLayout.CENTER);
        section.add(row);
        return section;
    }

    private JPanel createChartThemeSection() {
        JPanel section = createSectionCard("CHART COLOR THEMES");
        JPanel grid = new JPanel(new GridLayout(2, 4, 6, 6));
        grid.setOpaque(false);

        String[][] themes = {
                {"Blue", "Corporate Blue", "#2563eb"},
                {"Emerald", "Emerald Mint", "#10b981"},
                {"Violet", "Violet Purple", "#8b5cf6"},
                {"Amber", "Amber Gold", "#f59e0b"},
                {"Crimson", "Crimson Rose", "#e11d48"},
                {"Slate", "Dark Slate", "#334155"},
                {"Minimal", "Clean Minimal", "#64748b"}
        };

        for (String[] t : themes) {
            String shortName = t[0];
            String fullName = t[1];
            Color col = Color.decode(t[2]);

            JButton btn = new JButton(shortName);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(30, 41, 59));
            btn.setFocusPainted(false);
            btn.setToolTipText("Apply " + fullName + " palette");
            btn.setBorder(new CompoundBorder(
                    new LineBorder(col, 2, true),
                    new EmptyBorder(4, 2, 4, 2)
            ));
            btn.addActionListener(e -> {
                if (canvasPanel != null) {
                    canvasPanel.switchSelectedChartTheme(fullName);
                }
            });
            grid.add(btn);
        }

        section.add(grid);
        return section;
    }

    private JPanel createChartOptionsSection() {
        JPanel section = createSectionCard("CHART DISPLAY OPTIONS");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        row.setOpaque(false);

        chartShowValuesCheck = new JCheckBox("Show Values", true);
        chartShowValuesCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chartShowValuesCheck.setOpaque(false);
        chartShowValuesCheck.addActionListener(e -> {
            if (updatingUi) return;
            if (canvasPanel != null) canvasPanel.toggleChartValues();
        });
        row.add(chartShowValuesCheck);

        chartShowLegendCheck = new JCheckBox("Show Legend", true);
        chartShowLegendCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chartShowLegendCheck.setOpaque(false);
        chartShowLegendCheck.addActionListener(e -> {
            if (updatingUi) return;
            if (canvasPanel != null) canvasPanel.toggleChartLegend();
        });
        row.add(chartShowLegendCheck);

        section.add(row);
        return section;
    }

    private JPanel createCardThemesSection() {
        JPanel section = createSectionCard("COLOR THEMES & QUICK STYLES");
        JPanel grid = new JPanel(new GridLayout(2, 4, 6, 6));
        grid.setOpaque(false);

        for (Map.Entry<String, CardShapeCustomizerDialog.ThemePalette> entry : CardShapeCustomizerDialog.PALETTES.entrySet()) {
            String key = entry.getKey();
            CardShapeCustomizerDialog.ThemePalette pal = entry.getValue();
            JButton palBtn = createPaletteButton(key, pal);
            grid.add(palBtn);
        }
        section.add(grid);
        return section;
    }

    private JPanel createFillSection() {
        JPanel section = createSectionCard("FILL & BACKGROUND");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        row.setOpaque(false);

        JLabel fillLbl = new JLabel("Fill Color:");
        fillLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(fillLbl);

        fillSwatchPreview = createColorSwatch("Pick Background Fill Color", c -> {
            currentFillColor = c;
            triggerDebouncedApply();
        });
        row.add(fillSwatchPreview);

        section.add(row);
        return section;
    }

    private JPanel createBorderSection() {
        JPanel section = createSectionCard("LINE & BORDER OUTLINE");

        JPanel bStyleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        bStyleRow.setOpaque(false);

        JLabel bStyleLbl = new JLabel("Style:");
        bStyleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bStyleRow.add(bStyleLbl);

        borderStyleCombo = new JComboBox<>(new String[]{"solid", "dashed", "dotted", "none"});
        borderStyleCombo.setPreferredSize(new Dimension(85, 26));
        borderStyleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        borderStyleCombo.addActionListener(e -> {
            if (updatingUi) return;
            currentBorderStyle = (String) borderStyleCombo.getSelectedItem();
            triggerDebouncedApply();
        });
        bStyleRow.add(borderStyleCombo);

        JLabel bColorLbl = new JLabel("Color:");
        bColorLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bStyleRow.add(bColorLbl);

        borderSwatchPreview = createColorSwatch("Pick Border Outline Color", c -> {
            currentBorderColor = c;
            triggerDebouncedApply();
        });
        bStyleRow.add(borderSwatchPreview);
        section.add(bStyleRow);

        JPanel bWeightRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        bWeightRow.setOpaque(false);

        JLabel bWeightLbl = new JLabel("Weight (px):");
        bWeightLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bWeightRow.add(bWeightLbl);

        borderWidthSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 16, 1));
        borderWidthSpinner.setPreferredSize(new Dimension(55, 26));
        borderWidthSpinner.addChangeListener(e -> {
            if (updatingUi) return;
            currentBorderWidth = (Integer) borderWidthSpinner.getValue();
            triggerDebouncedApply();
        });
        bWeightRow.add(borderWidthSpinner);
        section.add(bWeightRow);

        // Corner Radius
        JPanel radiusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        radiusRow.setOpaque(false);
        JLabel radLbl = new JLabel("Corner Radius:");
        radLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        radiusRow.add(radLbl);

        radiusSlider = new JSlider(0, 40, currentBorderRadius);
        radiusSlider.setPreferredSize(new Dimension(130, 24));
        radiusSlider.setOpaque(false);
        radiusLabel = new JLabel(currentBorderRadius + " px");
        radiusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        radiusSlider.addChangeListener(e -> {
            currentBorderRadius = radiusSlider.getValue();
            radiusLabel.setText(currentBorderRadius + " px");
            if (!updatingUi && !radiusSlider.getValueIsAdjusting()) {
                triggerDebouncedApply();
            }
        });
        radiusRow.add(radiusSlider);
        radiusRow.add(radiusLabel);
        section.add(radiusRow);

        return section;
    }

    private JPanel createShadowSection() {
        JPanel section = createSectionCard("SHADOW & ELEVATION");
        JPanel shadowRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        shadowRow.setOpaque(false);

        JLabel shadowLbl = new JLabel("Preset:");
        shadowLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        shadowRow.add(shadowLbl);

        shadowCombo = new JComboBox<>(new String[]{"none", "soft", "elevation", "glow", "depth"});
        shadowCombo.setPreferredSize(new Dimension(140, 26));
        shadowCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        shadowCombo.addActionListener(e -> {
            if (updatingUi) return;
            currentShadowPreset = (String) shadowCombo.getSelectedItem();
            triggerDebouncedApply();
        });
        shadowRow.add(shadowCombo);
        section.add(shadowRow);
        return section;
    }

    private JPanel createSizeSection() {
        JPanel section = createSectionCard("SIZE & ALIGNMENT");
        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        sizeRow.setOpaque(false);

        JLabel wLbl = new JLabel("W:");
        wLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sizeRow.add(wLbl);

        widthSpinner = new JSpinner(new SpinnerNumberModel(520, 40, 1600, 10));
        widthSpinner.setPreferredSize(new Dimension(65, 26));
        widthSpinner.addChangeListener(e -> {
            if (updatingUi) return;
            currentWidth = (Integer) widthSpinner.getValue();
            if (lockAspectRatio && originalAspectRatio > 0) {
                updatingUi = true;
                currentHeight = (int) Math.round(currentWidth / originalAspectRatio);
                heightSpinner.setValue(currentHeight);
                updatingUi = false;
            }
            triggerDebouncedApply();
        });
        sizeRow.add(widthSpinner);

        JLabel hLbl = new JLabel("H:");
        hLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sizeRow.add(hLbl);

        heightSpinner = new JSpinner(new SpinnerNumberModel(200, 20, 1600, 10));
        heightSpinner.setPreferredSize(new Dimension(65, 26));
        heightSpinner.addChangeListener(e -> {
            if (updatingUi) return;
            currentHeight = (Integer) heightSpinner.getValue();
            if (lockAspectRatio && originalAspectRatio > 0) {
                updatingUi = true;
                currentWidth = (int) Math.round(currentHeight * originalAspectRatio);
                widthSpinner.setValue(currentWidth);
                updatingUi = false;
            }
            triggerDebouncedApply();
        });
        sizeRow.add(heightSpinner);

        aspectCheckbox = new JCheckBox("Lock", false);
        aspectCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        aspectCheckbox.setOpaque(false);
        aspectCheckbox.addActionListener(e -> {
            lockAspectRatio = aspectCheckbox.isSelected();
            if (lockAspectRatio && currentHeight > 0) {
                originalAspectRatio = (double) currentWidth / (double) currentHeight;
            }
        });
        sizeRow.add(aspectCheckbox);
        section.add(sizeRow);

        // Alignment Buttons
        JPanel alignRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        alignRow.setOpaque(false);
        JLabel alLbl = new JLabel("Align:");
        alLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        alignRow.add(alLbl);

        JButton btnLeft = createAlignButton("Left", VectorIcons.Type.ALIGN_LEFT, () -> setAlignment("left"));
        JButton btnCenter = createAlignButton("Center", VectorIcons.Type.ALIGN_CENTER, () -> setAlignment("center"));
        JButton btnRight = createAlignButton("Right", VectorIcons.Type.ALIGN_RIGHT, () -> setAlignment("right"));
        alignRow.add(btnLeft);
        alignRow.add(btnCenter);
        alignRow.add(btnRight);
        section.add(alignRow);

        // Quick Fit Buttons
        JPanel fitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        fitRow.setOpaque(false);
        JButton btnFitMargin = new JButton("Fit Page (560px)");
        btnFitMargin.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnFitMargin.setBackground(Color.WHITE);
        btnFitMargin.addActionListener(e -> {
            widthSpinner.setValue(560);
        });
        fitRow.add(btnFitMargin);

        JButton btnScaleDown = new JButton("-10%");
        btnScaleDown.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnScaleDown.setBackground(Color.WHITE);
        btnScaleDown.addActionListener(e -> {
            int nw = Math.max(80, (int) (currentWidth * 0.9));
            widthSpinner.setValue(nw);
        });
        fitRow.add(btnScaleDown);

        JButton btnScaleUp = new JButton("+10%");
        btnScaleUp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnScaleUp.setBackground(Color.WHITE);
        btnScaleUp.addActionListener(e -> {
            int nw = Math.min(1200, (int) (currentWidth * 1.1));
            widthSpinner.setValue(nw);
        });
        fitRow.add(btnScaleUp);

        section.add(fitRow);
        return section;
    }

    private JPanel createCardTextSection() {
        JPanel section = createSectionCard("CARD TEXT & METRICS");
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        JLabel tLbl = new JLabel("Title / Topic:");
        tLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tLbl.setForeground(new Color(71, 85, 105));
        tLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(tLbl);

        titleField = new JTextField();
        titleField.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleField.getDocument().addDocumentListener(createFieldDocListener());
        section.add(titleField);
        section.add(Box.createVerticalStrut(6));

        JLabel vLbl = new JLabel("Highlight Metric Value (e.g. $128,450):");
        vLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        vLbl.setForeground(new Color(71, 85, 105));
        vLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(vLbl);

        valueField = new JTextField();
        valueField.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valueField.setForeground(new Color(37, 99, 235));
        valueField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        valueField.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueField.getDocument().addDocumentListener(createFieldDocListener());
        section.add(valueField);
        section.add(Box.createVerticalStrut(6));

        JLabel sLbl = new JLabel("Subtitle / Description:");
        sLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sLbl.setForeground(new Color(71, 85, 105));
        sLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sLbl);

        subtitleField = new JTextField();
        subtitleField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        subtitleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleField.getDocument().addDocumentListener(createFieldDocListener());
        section.add(subtitleField);

        return section;
    }

    private JPanel createShapeTextSection() {
        JPanel section = createSectionCard("SHAPE & CALLOUT TEXT");
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        JLabel tLbl = new JLabel("Header / Label:");
        tLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tLbl.setForeground(new Color(71, 85, 105));
        tLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(tLbl);

        shapeTitleField = new JTextField();
        shapeTitleField.setFont(new Font("Segoe UI", Font.BOLD, 12));
        shapeTitleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        shapeTitleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        shapeTitleField.getDocument().addDocumentListener(createFieldDocListener());
        section.add(shapeTitleField);
        section.add(Box.createVerticalStrut(6));

        JLabel cLbl = new JLabel("Content / Message:");
        cLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cLbl.setForeground(new Color(71, 85, 105));
        cLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(cLbl);

        shapeContentArea = new JTextArea(3, 20);
        shapeContentArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        shapeContentArea.setLineWrap(true);
        shapeContentArea.setWrapStyleWord(true);
        shapeContentArea.getDocument().addDocumentListener(createFieldDocListener());
        JScrollPane sp = new JScrollPane(shapeContentArea);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        section.add(sp);

        return section;
    }

    private JPanel createTableToolsSection() {
        JPanel section = createSectionCard("TABLE FORMATTING & TOOLS");

        // Table Styles Grid
        JLabel styleLbl = new JLabel("Table Preset Style:");
        styleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        styleLbl.setForeground(new Color(71, 85, 105));
        section.add(styleLbl);

        JPanel stylesGrid = new JPanel(new GridLayout(2, 3, 6, 6));
        stylesGrid.setOpaque(false);
        String[] tStyles = {"Corporate Blue", "Modern Slate", "Emerald Green", "Striped Zebra", "Minimalist"};
        for (String st : tStyles) {
            JButton btn = new JButton(st);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setBackground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                if (canvasPanel != null) canvasPanel.applyTableStyle(st);
            });
            stylesGrid.add(btn);
        }
        section.add(stylesGrid);
        section.add(Box.createVerticalStrut(10));

        // Row/Col Operations
        JLabel opsLbl = new JLabel("Row & Column Operations:");
        opsLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        opsLbl.setForeground(new Color(71, 85, 105));
        section.add(opsLbl);

        JPanel opsGrid = new JPanel(new GridLayout(3, 2, 6, 6));
        opsGrid.setOpaque(false);

        JButton btnRowBelow = new JButton("+ Row Below");
        btnRowBelow.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRowBelow.addActionListener(e -> { if (canvasPanel != null) canvasPanel.insertTableRowBelow(); });
        opsGrid.add(btnRowBelow);

        JButton btnRowAbove = new JButton("+ Row Above");
        btnRowAbove.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRowAbove.addActionListener(e -> { if (canvasPanel != null) canvasPanel.insertTableRowAbove(); });
        opsGrid.add(btnRowAbove);

        JButton btnColRight = new JButton("+ Col Right");
        btnColRight.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnColRight.addActionListener(e -> { if (canvasPanel != null) canvasPanel.insertTableColumnRight(); });
        opsGrid.add(btnColRight);

        JButton btnColLeft = new JButton("+ Col Left");
        btnColLeft.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnColLeft.addActionListener(e -> { if (canvasPanel != null) canvasPanel.insertTableColumnLeft(); });
        opsGrid.add(btnColLeft);

        JButton btnDelRow = new JButton("Delete Row");
        btnDelRow.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDelRow.setForeground(new Color(225, 29, 72));
        btnDelRow.addActionListener(e -> { if (canvasPanel != null) canvasPanel.deleteTableRow(); });
        opsGrid.add(btnDelRow);

        JButton btnDelCol = new JButton("Delete Col");
        btnDelCol.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDelCol.setForeground(new Color(225, 29, 72));
        btnDelCol.addActionListener(e -> { if (canvasPanel != null) canvasPanel.deleteTableColumn(); });
        opsGrid.add(btnDelCol);

        section.add(opsGrid);
        return section;
    }

    private JPanel createActionsSection() {
        JPanel section = createSectionCard("ACTIONS");
        JPanel actRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actRow.setOpaque(false);

        JButton duplicateBtn = new JButton("Duplicate", VectorIcons.create(VectorIcons.Type.PLUS, 12, new Color(71, 85, 105)));
        styleActionButton(duplicateBtn, new Color(241, 245, 249), new Color(15, 23, 42));
        duplicateBtn.setToolTipText("Clone this object in the document");
        duplicateBtn.addActionListener(e -> {
            if (currentElement != null && canvasPanel != null) {
                canvasPanel.duplicateCardShapeElement(currentElement);
            }
        });
        actRow.add(duplicateBtn);

        JButton dialogBtn = new JButton("Edit Data...", VectorIcons.create(VectorIcons.Type.SETTINGS, 12, new Color(14, 165, 233)));
        styleActionButton(dialogBtn, new Color(239, 246, 255), new Color(2, 132, 199));
        dialogBtn.setToolTipText("Open full editor dialog");
        dialogBtn.addActionListener(e -> {
            if (canvasPanel == null) return;
            if (currentObjectType == ObjectType.CHART) {
                canvasPanel.modifySelectedChartData();
            } else if (currentElement != null) {
                canvasPanel.openCardShapeEditor(currentElement);
            }
        });
        actRow.add(dialogBtn);

        JButton deleteBtn = new JButton("Delete", VectorIcons.create(VectorIcons.Type.DELETE, 12, new Color(225, 29, 72)));
        styleActionButton(deleteBtn, new Color(254, 242, 242), new Color(225, 29, 72));
        deleteBtn.setToolTipText("Remove object from document");
        deleteBtn.addActionListener(e -> {
            if (currentElement != null && canvasPanel != null) {
                if (currentObjectType == ObjectType.TABLE) {
                    canvasPanel.deleteTable();
                } else if (currentObjectType == ObjectType.VIDEO) {
                    canvasPanel.deleteSelectedVideo();
                } else {
                    canvasPanel.deleteCardShapeElement(currentElement);
                }
                showEmptyState();
            }
        });
        actRow.add(deleteBtn);
        section.add(actRow);
        return section;
    }

    private JPanel createEmptyStatePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(32, 20, 32, 20));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel icon = new JLabel(VectorIcons.create(VectorIcons.Type.SHAPE, 38, new Color(148, 163, 184)));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(icon);
        inner.add(Box.createVerticalStrut(14));

        JLabel title = new JLabel("No Object Selected");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(71, 85, 105));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(title);
        inner.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("<html><center>Left-click any picture, chart, card, shape, or table to view its properties.</center></html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(148, 163, 184));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(sub);

        p.add(inner);
        return p;
    }

    // =========================================================================
    // OBJECT SELECTION & RELEVANT PROPERTY ORCHESTRATION
    // =========================================================================

    public void loadElement(Element elem) {
        if (elem == null) {
            showEmptyState();
            return;
        }

        this.currentElement = elem;
        this.currentObjectType = detectObjectType(elem);
        this.updatingUi = true;

        try {
            // 1. Update Header
            headerTitleLabel.setText(currentObjectType.title);
            headerBadgeLabel.setText(currentObjectType.badge);
            headerBadgeLabel.setForeground(currentObjectType.accent);
            headerIconLabel.setIcon(VectorIcons.create(currentObjectType.iconType, 16, currentObjectType.accent));

            // 2. Dynamically Show ONLY Relevant Sections for this Object
            configureSectionsForObjectType(currentObjectType);

            // 3. Populate Properties
            AttributeSet attrs = elem.getAttributes();
            parseInlineStyle(elem);

            Rectangle bounds = canvasPanel != null ? canvasPanel.getSelectedMediaBounds() : null;
            if (bounds != null && bounds.width > 0) {
                currentWidth = bounds.width;
                currentHeight = bounds.height;
            } else if (attrs != null) {
                Object wObj = attrs.getAttribute(HTML.Attribute.WIDTH);
                Object hObj = attrs.getAttribute(HTML.Attribute.HEIGHT);
                try { if (wObj != null) currentWidth = Integer.parseInt(wObj.toString()); } catch (Exception ignored) {}
                try { if (hObj != null) currentHeight = Integer.parseInt(hObj.toString()); } catch (Exception ignored) {}
            }
            originalAspectRatio = currentHeight > 0 ? (double) currentWidth / currentHeight : 1.0;

            widthSpinner.setValue(currentWidth);
            heightSpinner.setValue(currentHeight);

            // Text extraction for cards and shapes
            if (currentObjectType == ObjectType.METRIC_CARD || currentObjectType == ObjectType.SHAPE) {
                HTMLDocument doc = (HTMLDocument) canvasPanel.getTextPane().getDocument();
                String plain = canvasPanel.getElementPlainText(doc, elem);
                String[] lines = plain.split("\n");
                java.util.List<String> valid = new java.util.ArrayList<>();
                for (String l : lines) {
                    String trim = l.trim();
                    if (!trim.isEmpty()) valid.add(trim);
                }

                if (currentObjectType == ObjectType.METRIC_CARD) {
                    if (valid.size() == 1) {
                        titleField.setText(valid.get(0));
                        valueField.setText("");
                        subtitleField.setText("");
                    } else if (valid.size() == 2) {
                        titleField.setText(valid.get(0));
                        valueField.setText("");
                        subtitleField.setText(valid.get(1));
                    } else if (valid.size() >= 3) {
                        titleField.setText(valid.get(0));
                        valueField.setText(valid.get(1));
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < valid.size(); i++) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(valid.get(i));
                        }
                        subtitleField.setText(sb.toString());
                    } else {
                        titleField.setText("");
                        valueField.setText("");
                        subtitleField.setText("");
                    }
                } else {
                    if (!valid.isEmpty()) {
                        shapeTitleField.setText(valid.get(0));
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < valid.size(); i++) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(valid.get(i));
                        }
                        shapeContentArea.setText(sb.toString());
                    } else {
                        shapeTitleField.setText("");
                        shapeContentArea.setText("");
                    }
                }

                if (attrs != null) {
                    Object themeAttr = attrs.getAttribute("data-theme");
                    if (themeAttr != null) {
                        selectedThemeKey = themeAttr.toString().toUpperCase();
                    }
                }
            }

            fillSwatchPreview.repaint();
            borderSwatchPreview.repaint();
            borderWidthSpinner.setValue(currentBorderWidth);
            borderStyleCombo.setSelectedItem(currentBorderStyle);
            radiusSlider.setValue(currentBorderRadius);
            radiusLabel.setText(currentBorderRadius + " px");
            shadowCombo.setSelectedItem(currentShadowPreset);

            CardLayout cl = (CardLayout) mainDeck.getLayout();
            cl.show(mainDeck, "CONTENT");

        } finally {
            this.updatingUi = false;
        }

        revalidate();
        repaint();
    }

    private void configureSectionsForObjectType(ObjectType type) {
        // Toggle visibility of each section cleanly
        pictureFilterSection.setVisible(type == ObjectType.PICTURE);
        chartTypeSection.setVisible(type == ObjectType.CHART);
        chartThemeSection.setVisible(type == ObjectType.CHART);
        chartOptionsSection.setVisible(type == ObjectType.CHART);

        cardThemesSection.setVisible(type == ObjectType.METRIC_CARD);
        fillSection.setVisible(type == ObjectType.METRIC_CARD || type == ObjectType.SHAPE);
        borderSection.setVisible(type != ObjectType.TABLE);
        shadowSection.setVisible(type != ObjectType.TABLE);
        sizeSection.setVisible(type != ObjectType.TABLE);

        cardTextSection.setVisible(type == ObjectType.METRIC_CARD);
        shapeTextSection.setVisible(type == ObjectType.SHAPE);
        tableToolsSection.setVisible(type == ObjectType.TABLE);
        actionsSection.setVisible(true);
    }

    private ObjectType detectObjectType(Element elem) {
        if (elem == null) return ObjectType.SHAPE;

        // Check if Table
        Element cur = elem;
        while (cur != null) {
            String name = cur.getName();
            if ("table".equalsIgnoreCase(name) || "td".equalsIgnoreCase(name) || "th".equalsIgnoreCase(name) || "tr".equalsIgnoreCase(name)) {
                return ObjectType.TABLE;
            }
            cur = cur.getParentElement();
        }

        AttributeSet attrs = elem.getAttributes();
        if (attrs == null) return ObjectType.SHAPE;

        Object nameAttr = attrs.getAttribute(StyleConstants.NameAttribute);
        String nameStr = nameAttr != null ? nameAttr.toString().toLowerCase() : elem.getName().toLowerCase();

        // Check if IMG
        if (HTML.Tag.IMG.equals(nameAttr) || "img".equals(nameStr)) {
            if (canvasPanel != null && canvasPanel.getSelectedChartMetadata() != null) {
                return ObjectType.CHART;
            }
            Object srcObj = attrs.getAttribute(HTML.Attribute.SRC);
            String src = srcObj != null ? srcObj.toString() : "";
            if (src.contains("charts/") || src.contains(".chart.png") || src.contains("chart_") || attrs.getAttribute("data-chart") != null) {
                return ObjectType.CHART;
            }
            return ObjectType.PICTURE;
        }

        // Check classes & data attributes
        Object classAttr = attrs.getAttribute(HTML.Attribute.CLASS);
        if (classAttr == null) classAttr = attrs.getAttribute("class");
        String classStr = classAttr != null ? classAttr.toString().toLowerCase() : "";

        if (classStr.contains("wb-video-card") || attrs.getAttribute("data-video") != null) {
            return ObjectType.VIDEO;
        }

        if (classStr.contains("chart") || attrs.getAttribute("data-chart") != null) {
            return ObjectType.CHART;
        }

        if (classStr.contains("wb-callout") || classStr.contains("wb-flow") || classStr.contains("wb-shape")
                || attrs.getAttribute("data-callout") != null || attrs.getAttribute("data-shape") != null) {
            return ObjectType.SHAPE;
        }

        if (classStr.contains("wb-card") || attrs.getAttribute("data-card") != null) {
            return ObjectType.METRIC_CARD;
        }

        return ObjectType.METRIC_CARD;
    }

    public void showEmptyState() {
        this.currentElement = null;
        CardLayout cl = (CardLayout) mainDeck.getLayout();
        cl.show(mainDeck, "EMPTY");
        headerTitleLabel.setText("Format Pane");
        headerBadgeLabel.setText("Ready");
        headerBadgeLabel.setForeground(new Color(100, 116, 139));
        headerIconLabel.setIcon(VectorIcons.create(VectorIcons.Type.SHAPE, 16, new Color(148, 163, 184)));
    }

    private void triggerDebouncedApply() {
        debouncedApplyTimer.restart();
    }

    private void performApply() {
        if (currentElement == null || canvasPanel == null) return;

        if (currentObjectType == ObjectType.PICTURE) {
            canvasPanel.applyImageFullFormatting(
                    currentElement,
                    currentWidth,
                    currentHeight,
                    currentBorderColor,
                    currentBorderWidth,
                    currentBorderStyle,
                    currentBorderRadius,
                    currentShadowPreset,
                    currentAlign
            );
        } else if (currentObjectType == ObjectType.METRIC_CARD) {
            canvasPanel.applyCardShapeFullFormatting(
                    currentElement,
                    titleField.getText(),
                    valueField.getText(),
                    subtitleField.getText(),
                    selectedThemeKey,
                    currentFillColor,
                    currentBorderColor,
                    currentBorderWidth,
                    currentBorderStyle,
                    currentBorderRadius,
                    currentShadowPreset,
                    currentWidth,
                    currentAlign
            );
        } else if (currentObjectType == ObjectType.SHAPE) {
            canvasPanel.applyCardShapeFullFormatting(
                    currentElement,
                    shapeTitleField.getText(),
                    "",
                    shapeContentArea.getText(),
                    selectedThemeKey,
                    currentFillColor,
                    currentBorderColor,
                    currentBorderWidth,
                    currentBorderStyle,
                    currentBorderRadius,
                    currentShadowPreset,
                    currentWidth,
                    currentAlign
            );
        }
    }

    private void setAlignment(String align) {
        this.currentAlign = align;
        if (currentElement != null && canvasPanel != null) {
            canvasPanel.alignCardShapeElement(currentElement, align);
        }
    }

    private DocumentListener createFieldDocListener() {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onDoc(); }
            @Override public void removeUpdate(DocumentEvent e) { onDoc(); }
            @Override public void changedUpdate(DocumentEvent e) { onDoc(); }
            private void onDoc() {
                if (updatingUi) return;
                triggerDebouncedApply();
            }
        };
    }

    private void parseInlineStyle(Element elem) {
        if (canvasPanel == null) return;
        String style = canvasPanel.getElementStyle(elem).toLowerCase();

        if (style.contains("background-color:")) {
            String hex = extractCssValue(style, "background-color:");
            Color c = parseHex(hex);
            if (c != null) currentFillColor = c;
        }

        if (style.contains("border:")) {
            String bVal = extractCssValue(style, "border:");
            String[] parts = bVal.split("\\s+");
            for (String p : parts) {
                if (p.endsWith("px")) {
                    try { currentBorderWidth = Integer.parseInt(p.replace("px", "")); } catch (Exception ignored) {}
                } else if (p.equals("solid") || p.equals("dashed") || p.equals("dotted") || p.equals("none")) {
                    currentBorderStyle = p;
                } else if (p.startsWith("#")) {
                    Color c = parseHex(p);
                    if (c != null) currentBorderColor = c;
                }
            }
        }

        if (style.contains("border-radius:")) {
            String rVal = extractCssValue(style, "border-radius:");
            try { currentBorderRadius = Integer.parseInt(rVal.replace("px", "").trim()); } catch (Exception ignored) {}
        }

        if (style.contains("box-shadow:")) {
            if (style.contains("0 8px 24px")) currentShadowPreset = "elevation";
            else if (style.contains("0 0 14px")) currentShadowPreset = "glow";
            else if (style.contains("0 14px 32px")) currentShadowPreset = "depth";
            else currentShadowPreset = "soft";
        } else {
            currentShadowPreset = "none";
        }
    }

    private String extractCssValue(String style, String key) {
        int idx = style.indexOf(key);
        if (idx == -1) return "";
        int start = idx + key.length();
        int end = style.indexOf(";", start);
        if (end == -1) end = style.length();
        return style.substring(start, end).trim();
    }

    private Color parseHex(String hex) {
        if (hex == null || !hex.startsWith("#")) return null;
        try {
            if (hex.length() == 7) {
                return Color.decode(hex);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    // UI COMPONENT FACTORIES & STYLING
    // =========================================================================

    private JPanel createSectionCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tLbl = new JLabel(title);
        tLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tLbl.setForeground(new Color(100, 116, 139));
        tLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tLbl);
        card.add(Box.createVerticalStrut(8));

        return card;
    }

    private JButton createPaletteButton(String themeKey, CardShapeCustomizerDialog.ThemePalette pal) {
        JButton btn = new JButton(themeKey.substring(0, 1).toUpperCase() + themeKey.substring(1).toLowerCase());
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setBackground(pal.bg());
        btn.setForeground(pal.primary());
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new LineBorder(pal.border(), 2, true),
                new EmptyBorder(4, 3, 4, 3)
        ));
        btn.setToolTipText(pal.name() + " Style Palette");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            selectedThemeKey = themeKey.toUpperCase();
            currentFillColor = pal.bg();
            currentBorderColor = pal.border();
            fillSwatchPreview.repaint();
            borderSwatchPreview.repaint();
            triggerDebouncedApply();
        });
        return btn;
    }

    private JPanel createColorSwatch(String tooltip, java.util.function.Consumer<Color> onColorChosen) {
        JPanel swatch = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = tooltip.contains("Background") ? currentFillColor : currentBorderColor;
                g2.setColor(c != null ? c : Color.WHITE);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 6, 6);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 6, 6);
                g2.dispose();
            }
        };
        swatch.setPreferredSize(new Dimension(32, 24));
        swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        swatch.setToolTipText(tooltip);
        swatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Color cur = tooltip.contains("Background") ? currentFillColor : currentBorderColor;
                Color chosen = ModernColorPickerDialog.showDialog(ShapeFormatSidePanel.this, tooltip, cur);
                if (chosen != null) {
                    onColorChosen.accept(chosen);
                    swatch.repaint();
                }
            }
        });
        return swatch;
    }

    private JButton createAlignButton(String text, VectorIcons.Type icon, Runnable action) {
        JButton btn = new JButton(VectorIcons.create(icon, 12, new Color(71, 85, 105)));
        btn.setPreferredSize(new Dimension(30, 26));
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setToolTipText("Align " + text);
        btn.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(2, 2, 2, 2)
        ));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void styleActionButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new LineBorder(bg.darker(), 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        setBackground(newTheme.isDark() ? new Color(15, 23, 42) : new Color(248, 250, 252));
        revalidate();
        repaint();
    }
}
