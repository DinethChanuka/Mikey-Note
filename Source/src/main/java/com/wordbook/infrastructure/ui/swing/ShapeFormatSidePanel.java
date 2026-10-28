package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
 * Microsoft Word-style Shape & Picture Format Sidebar Panel.
 * Appears docked on the right side when an object (card, shape, image, chart)
 * is selected or when the user opens the "Card & Shape Format" menu/ribbon tab.
 *
 * Provides real-time formatting controls:
 * - Quick Color Themes & Palettes (Corporate Blue, Emerald, Violet, Amber, Crimson, Slate, Minimal)
 * - Fill & Background (Color swatch, Transparency slider)
 * - Line & Border (Style, Color swatch, Width spinner, Corner Radius slider)
 * - Shadow & Elevation (Soft, Elevation, Glow, Depth)
 * - Size & Dimensions (Width, Height, Lock aspect ratio)
 * - Alignment (Left, Center, Right)
 * - Live Text & Metric Editing (Title, Highlight Metric Value, Subtitle)
 * - Quick Actions (Duplicate, Edit in Modal Dialog, Delete Object)
 */
public class ShapeFormatSidePanel extends JPanel implements ThemeManager.ThemeChangeListener {

    private final DocumentCanvasPanel canvasPanel;
    private Runnable closeCallback;

    private Element currentElement;
    private boolean isImageElement = false;
    private boolean updatingUi = false;

    // Header controls
    private final JLabel headerTitleLabel;
    private final JLabel headerBadgeLabel;
    private final JButton closeBtn;
    private final JButton widthToggleBtn;

    // Content container & empty state
    private final JPanel contentCardsPanel;
    private final JPanel emptyStatePanel;

    // Themes
    private String selectedThemeKey = "BLUE";

    // Fill & Line state
    private Color currentFillColor = new Color(239, 246, 255);
    private Color currentBorderColor = new Color(59, 130, 246);
    private int currentBorderWidth = 2;
    private String currentBorderStyle = "solid";
    private int currentBorderRadius = 10;
    private String currentShadowPreset = "soft";

    // Size & Alignment state
    private int currentWidth = 500;
    private int currentHeight = 200;
    private String currentAlign = "center";
    private boolean lockAspectRatio = false;
    private double originalAspectRatio = 1.0;

    // UI Input Components
    private final JPanel fillSwatchPreview;
    private final JPanel borderSwatchPreview;
    private final JSpinner borderWidthSpinner;
    private final JComboBox<String> borderStyleCombo;
    private final JSlider radiusSlider;
    private final JLabel radiusLabel;
    private final JComboBox<String> shadowCombo;

    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JCheckBox aspectCheckbox;

    private final JTextField titleField;
    private final JTextField valueField;
    private final JTextField subtitleField;
    private final JPanel textSectionPanel;

    private final Timer debouncedApplyTimer;

    public ShapeFormatSidePanel(DocumentCanvasPanel canvasPanel) {
        this.canvasPanel = canvasPanel;
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(320, 200));
        setPreferredSize(new Dimension(380, 600));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(226, 232, 240)));

        ThemeManager.addListener(this);

        debouncedApplyTimer = new Timer(80, e -> performApply());
        debouncedApplyTimer.setRepeats(false);

        // --- 1. Top Header Bar (Word-style Format Shape title) ---
        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setOpaque(false);
        headerBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(12, 16, 12, 12)
        ));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);

        JLabel iconLabel = new JLabel(VectorIcons.create(VectorIcons.Type.SHAPE, 16, new Color(14, 165, 233)));
        titleBox.add(iconLabel);

        headerTitleLabel = new JLabel("Format Shape");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerTitleLabel.setForeground(new Color(15, 23, 42));
        titleBox.add(headerTitleLabel);

        headerBadgeLabel = new JLabel("Card");
        headerBadgeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        headerBadgeLabel.setForeground(new Color(100, 116, 139));
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
        widthToggleBtn.setToolTipText("Toggle standard / expanded width");
        widthToggleBtn.addActionListener(e -> {
            int curW = getWidth();
            int newW = (curW < 420) ? 480 : 380;
            setPreferredSize(new Dimension(newW, getHeight()));
            Container parent = getParent();
            if (parent instanceof JSplitPane sp) {
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

        // --- 2. Scrollable Body containing Formatting Sections ---
        contentCardsPanel = new JPanel();
        contentCardsPanel.setLayout(new BoxLayout(contentCardsPanel, BoxLayout.Y_AXIS));
        contentCardsPanel.setOpaque(false);
        contentCardsPanel.setBorder(new EmptyBorder(12, 14, 20, 14));

        // Section A: Theme Palettes (Quick Styles)
        JPanel themeSection = createSectionCard("COLOR THEMES & QUICK STYLES");
        JPanel themesGrid = new JPanel(new GridLayout(2, 4, 6, 6));
        themesGrid.setOpaque(false);

        for (Map.Entry<String, CardShapeCustomizerDialog.ThemePalette> entry : CardShapeCustomizerDialog.PALETTES.entrySet()) {
            String key = entry.getKey();
            CardShapeCustomizerDialog.ThemePalette pal = entry.getValue();
            JButton palBtn = createPaletteButton(key, pal);
            themesGrid.add(palBtn);
        }
        themeSection.add(themesGrid);
        contentCardsPanel.add(themeSection);
        contentCardsPanel.add(Box.createVerticalStrut(10));

        // Section B: Fill & Color
        JPanel fillSection = createSectionCard("FILL & BACKGROUND");
        JPanel fillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        fillRow.setOpaque(false);

        JLabel fillLbl = new JLabel("Fill Color:");
        fillLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fillRow.add(fillLbl);

        fillSwatchPreview = createColorSwatch("Pick Background Fill Color", c -> {
            currentFillColor = c;
            triggerDebouncedApply();
        });
        fillRow.add(fillSwatchPreview);

        fillSection.add(fillRow);
        contentCardsPanel.add(fillSection);
        contentCardsPanel.add(Box.createVerticalStrut(10));

        // Section C: Line & Border
        JPanel borderSection = createSectionCard("LINE & BORDER OUTLINE");
        JPanel bStyleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        bStyleRow.setOpaque(false);

        JLabel bStyleLbl = new JLabel("Line Style:");
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
        borderSection.add(bStyleRow);

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
        borderSection.add(bWeightRow);

        // Corner Radius
        JPanel radiusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        radiusRow.setOpaque(false);
        JLabel radLbl = new JLabel("Corner Radius:");
        radLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        radiusRow.add(radLbl);

        radiusSlider = new JSlider(0, 30, currentBorderRadius);
        radiusSlider.setPreferredSize(new Dimension(140, 24));
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
        borderSection.add(radiusRow);

        contentCardsPanel.add(borderSection);
        contentCardsPanel.add(Box.createVerticalStrut(10));

        // Section D: Shadow & Effects
        JPanel shadowSection = createSectionCard("SHADOW & ELEVATION");
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
        shadowSection.add(shadowRow);

        contentCardsPanel.add(shadowSection);
        contentCardsPanel.add(Box.createVerticalStrut(10));

        // Section E: Size & Alignment
        JPanel sizeSection = createSectionCard("SIZE & ALIGNMENT");
        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        sizeRow.setOpaque(false);

        JLabel wLbl = new JLabel("W:");
        wLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sizeRow.add(wLbl);

        widthSpinner = new JSpinner(new SpinnerNumberModel(520, 50, 1200, 10));
        widthSpinner.setPreferredSize(new Dimension(65, 26));

        heightSpinner = new JSpinner(new SpinnerNumberModel(200, 30, 1600, 10));
        heightSpinner.setPreferredSize(new Dimension(65, 26));

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

        aspectCheckbox = new JCheckBox("Lock Ratio", false);
        aspectCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        aspectCheckbox.setOpaque(false);
        aspectCheckbox.addActionListener(e -> {
            lockAspectRatio = aspectCheckbox.isSelected();
            if (lockAspectRatio && currentHeight > 0) {
                originalAspectRatio = (double) currentWidth / (double) currentHeight;
            }
        });
        sizeRow.add(aspectCheckbox);
        sizeSection.add(sizeRow);

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
        sizeSection.add(alignRow);

        contentCardsPanel.add(sizeSection);
        contentCardsPanel.add(Box.createVerticalStrut(10));

        // Section F: Card Text & Metrics (Live Text Editing)
        textSectionPanel = createSectionCard("CARD TEXT & METRICS");
        textSectionPanel.setLayout(new BoxLayout(textSectionPanel, BoxLayout.Y_AXIS));

        JLabel tLbl = new JLabel("Title / Topic:");
        tLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tLbl.setForeground(new Color(71, 85, 105));
        tLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textSectionPanel.add(tLbl);

        titleField = new JTextField();
        titleField.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleField.getDocument().addDocumentListener(createFieldDocListener());
        textSectionPanel.add(titleField);
        textSectionPanel.add(Box.createVerticalStrut(6));

        JLabel vLbl = new JLabel("Metric / Key Value (e.g. $128,450):");
        vLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        vLbl.setForeground(new Color(71, 85, 105));
        vLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textSectionPanel.add(vLbl);

        valueField = new JTextField();
        valueField.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valueField.setForeground(new Color(37, 99, 235));
        valueField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        valueField.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueField.getDocument().addDocumentListener(createFieldDocListener());
        textSectionPanel.add(valueField);
        textSectionPanel.add(Box.createVerticalStrut(6));

        JLabel sLbl = new JLabel("Subtitle / Description:");
        sLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sLbl.setForeground(new Color(71, 85, 105));
        sLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textSectionPanel.add(sLbl);

        subtitleField = new JTextField();
        subtitleField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        subtitleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleField.getDocument().addDocumentListener(createFieldDocListener());
        textSectionPanel.add(subtitleField);

        contentCardsPanel.add(textSectionPanel);
        contentCardsPanel.add(Box.createVerticalStrut(10));

        // Section G: Actions
        JPanel actionsSection = createSectionCard("ACTIONS");
        JPanel actRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actRow.setOpaque(false);

        JButton duplicateBtn = new JButton("Duplicate", VectorIcons.create(VectorIcons.Type.PLUS, 12, new Color(71, 85, 105)));
        styleActionButton(duplicateBtn, new Color(241, 245, 249), new Color(15, 23, 42));
        duplicateBtn.setToolTipText("Clone this object in the document");
        duplicateBtn.addActionListener(e -> {
            if (currentElement != null) {
                canvasPanel.duplicateCardShapeElement(currentElement);
            }
        });
        actRow.add(duplicateBtn);

        JButton dialogBtn = new JButton("Modal...", VectorIcons.create(VectorIcons.Type.SETTINGS, 12, new Color(14, 165, 233)));
        styleActionButton(dialogBtn, new Color(239, 246, 255), new Color(2, 132, 199));
        dialogBtn.setToolTipText("Open full Card & Shape Dialog");
        dialogBtn.addActionListener(e -> {
            if (currentElement != null) {
                canvasPanel.openCardShapeEditor(currentElement);
            }
        });
        actRow.add(dialogBtn);

        JButton deleteBtn = new JButton("Delete", VectorIcons.create(VectorIcons.Type.DELETE, 12, new Color(225, 29, 72)));
        styleActionButton(deleteBtn, new Color(254, 242, 242), new Color(225, 29, 72));
        deleteBtn.setToolTipText("Remove object from document");
        deleteBtn.addActionListener(e -> {
            if (currentElement != null) {
                canvasPanel.deleteCardShapeElement(currentElement);
                showEmptyState();
            }
        });
        actRow.add(deleteBtn);
        actionsSection.add(actRow);

        contentCardsPanel.add(actionsSection);

        // Empty state overlay when nothing is selected
        emptyStatePanel = new JPanel(new GridBagLayout());
        emptyStatePanel.setOpaque(false);
        emptyStatePanel.setBorder(new EmptyBorder(32, 20, 32, 20));
        JPanel emptyInner = new JPanel();
        emptyInner.setLayout(new BoxLayout(emptyInner, BoxLayout.Y_AXIS));
        emptyInner.setOpaque(false);

        JLabel emptyIcon = new JLabel(VectorIcons.create(VectorIcons.Type.SHAPE, 36, new Color(148, 163, 184)));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptyIcon);
        emptyInner.add(Box.createVerticalStrut(12));

        JLabel emptyTitle = new JLabel("No Object Selected");
        emptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        emptyTitle.setForeground(new Color(71, 85, 105));
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptyTitle);
        emptyInner.add(Box.createVerticalStrut(6));

        JLabel emptySub = new JLabel("<html><center>Left-click any card, shape, image, or chart to open its formatting tools.</center></html>");
        emptySub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        emptySub.setForeground(new Color(148, 163, 184));
        emptySub.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptySub);

        emptyStatePanel.add(emptyInner);

        JScrollPane scrollPane = new JScrollPane(contentCardsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JPanel mainDeck = new JPanel(new CardLayout());
        mainDeck.setOpaque(false);
        mainDeck.add(scrollPane, "CONTENT");
        mainDeck.add(emptyStatePanel, "EMPTY");

        add(mainDeck, BorderLayout.CENTER);
        showEmptyState();
    }

    public void setCloseCallback(Runnable callback) {
        this.closeCallback = callback;
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

    private void triggerDebouncedApply() {
        debouncedApplyTimer.restart();
    }

    private void performApply() {
        if (currentElement == null) return;
        if (isImageElement) {
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
        } else {
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
        }
    }

    /**
     * Loads an element into the formatting panel, populating all fields.
     */
    public void loadElement(Element elem) {
        if (elem == null) {
            showEmptyState();
            return;
        }

        this.currentElement = elem;
        updatingUi = true;

        try {
            AttributeSet attrs = elem.getAttributes();
            Object nameAttr = attrs != null ? attrs.getAttribute(StyleConstants.NameAttribute) : null;
            this.isImageElement = HTML.Tag.IMG.equals(nameAttr);

            Container parent = emptyStatePanel.getParent();
            if (parent != null && parent.getLayout() instanceof CardLayout cl) {
                cl.show(parent, "CONTENT");
            }

            if (isImageElement) {
                headerTitleLabel.setText("Format Picture");
                headerBadgeLabel.setText("Image");
                textSectionPanel.setVisible(false);

                // Dimensions
                Rectangle b = canvasPanel.getSelectedMediaBounds();
                if (b != null && b.width > 0) {
                    currentWidth = b.width;
                    currentHeight = b.height;
                } else {
                    Object wObj = attrs.getAttribute(HTML.Attribute.WIDTH);
                    Object hObj = attrs.getAttribute(HTML.Attribute.HEIGHT);
                    try { currentWidth = wObj != null ? Integer.parseInt(wObj.toString()) : 500; } catch (Exception ignored) {}
                    try { currentHeight = hObj != null ? Integer.parseInt(hObj.toString()) : 300; } catch (Exception ignored) {}
                }
                originalAspectRatio = currentHeight > 0 ? (double) currentWidth / currentHeight : 1.0;
                widthSpinner.setValue(currentWidth);
                heightSpinner.setValue(currentHeight);

                parseInlineStyle(elem);
            } else {
                headerTitleLabel.setText("Format Shape");
                headerBadgeLabel.setText("Card / Shape");
                textSectionPanel.setVisible(true);

                // Parse Text
                HTMLDocument doc = (HTMLDocument) canvasPanel.getTextPane().getDocument();
                String plain = canvasPanel.getElementPlainText(doc, elem);
                String[] lines = plain.split("\n");
                java.util.List<String> valid = new java.util.ArrayList<>();
                for (String l : lines) {
                    String trim = l.trim();
                    if (!trim.isEmpty()) valid.add(trim);
                }
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

                // Theme
                Object themeAttr = attrs.getAttribute("data-theme");
                if (themeAttr != null) {
                    selectedThemeKey = themeAttr.toString().toUpperCase();
                }

                parseInlineStyle(elem);

                Rectangle b = canvasPanel.getSelectedMediaBounds();
                if (b != null && b.width > 0) {
                    currentWidth = b.width;
                    currentHeight = b.height;
                    widthSpinner.setValue(currentWidth);
                    heightSpinner.setValue(currentHeight);
                }
            }

            fillSwatchPreview.repaint();
            borderSwatchPreview.repaint();
            borderWidthSpinner.setValue(currentBorderWidth);
            borderStyleCombo.setSelectedItem(currentBorderStyle);
            radiusSlider.setValue(currentBorderRadius);
            radiusLabel.setText(currentBorderRadius + " px");
            shadowCombo.setSelectedItem(currentShadowPreset);
        } finally {
            updatingUi = false;
        }

        revalidate();
        repaint();
    }

    private void parseInlineStyle(Element elem) {
        String style = canvasPanel.getElementStyle(elem).toLowerCase();

        // Background color
        if (style.contains("background-color:")) {
            String hex = extractCssValue(style, "background-color:");
            Color c = parseHex(hex);
            if (c != null) currentFillColor = c;
        }

        // Border width & color
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

        // Border radius
        if (style.contains("border-radius:")) {
            String rVal = extractCssValue(style, "border-radius:");
            try { currentBorderRadius = Integer.parseInt(rVal.replace("px", "").trim()); } catch (Exception ignored) {}
        }

        // Box shadow preset detection
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

    public void showEmptyState() {
        this.currentElement = null;
        Container parent = emptyStatePanel.getParent();
        if (parent != null && parent.getLayout() instanceof CardLayout cl) {
            cl.show(parent, "EMPTY");
        }
    }

    private void setAlignment(String align) {
        this.currentAlign = align;
        if (currentElement != null) {
            canvasPanel.alignCardShapeElement(currentElement, align);
        }
    }

    private JPanel createSectionCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLbl.setForeground(new Color(100, 116, 139));
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(8));

        return card;
    }

    private JButton createPaletteButton(String key, CardShapeCustomizerDialog.ThemePalette pal) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Background
                g2.setColor(pal.bg());
                g2.fillRoundRect(1, 1, w - 2, h - 2, 6, 6);

                // Border
                boolean isSel = key.equalsIgnoreCase(selectedThemeKey);
                g2.setColor(isSel ? new Color(14, 165, 233) : pal.border());
                g2.setStroke(new BasicStroke(isSel ? 2.2f : 1.0f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 6, 6);

                // Accent Stripe
                g2.setColor(pal.primary());
                g2.fillRoundRect(3, 3, 5, h - 6, 3, 3);

                // Mini text preview
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.setColor(pal.text());
                g2.drawString("Aa", 12, h / 2 + 3);

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(72, 32));
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(pal.name());
        btn.addActionListener(e -> {
            selectedThemeKey = key;
            currentFillColor = pal.bg();
            currentBorderColor = pal.border();
            fillSwatchPreview.repaint();
            borderSwatchPreview.repaint();
            if (currentElement != null) {
                canvasPanel.applyActiveCardShapeTheme(key);
            }
            repaint();
        });
        return btn;
    }

    private JPanel createColorSwatch(String tooltip, java.util.function.Consumer<Color> onPick) {
        JPanel swatch = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(tooltip.contains("Fill") ? currentFillColor : currentBorderColor);
                g2.fillRoundRect(2, 2, w - 4, h - 4, 6, 6);
                g2.setColor(new Color(203, 213, 225));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(2, 2, w - 4, h - 4, 6, 6);
                g2.dispose();
            }
        };
        swatch.setPreferredSize(new Dimension(32, 24));
        swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        swatch.setToolTipText(tooltip);
        swatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Color cur = tooltip.contains("Fill") ? currentFillColor : currentBorderColor;
                Color chosen = ModernColorPickerDialog.showDialog(ShapeFormatSidePanel.this, tooltip, cur);
                if (chosen != null) {
                    onPick.accept(chosen);
                    swatch.repaint();
                }
            }
        });
        return swatch;
    }

    private JButton createAlignButton(String text, VectorIcons.Type iconType, Runnable onClick) {
        JButton btn = new JButton(VectorIcons.create(iconType, 13, new Color(71, 85, 105)));
        btn.setPreferredSize(new Dimension(36, 26));
        btn.setBackground(new Color(241, 245, 249));
        btn.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Align " + text);
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private void styleActionButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        boolean dark = ThemeManager.isDark();
        setBackground(dark ? new Color(15, 23, 42) : new Color(248, 250, 252));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, dark ? new Color(51, 65, 85) : new Color(226, 232, 240)));
        headerTitleLabel.setForeground(dark ? new Color(248, 250, 252) : new Color(15, 23, 42));
        repaint();
    }
}
