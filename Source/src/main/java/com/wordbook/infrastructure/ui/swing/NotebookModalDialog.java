package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * State-of-the-art modal dialog for creating and customizing notebooks.
 * Features:
 * - Spacious, unclipped window sizing (980 x 700) with true 1:1.4 book proportions
 * - Fast-accessible one-click 16-color palette + custom HSB color picker
 * - Visual Design Card Gallery (No dropdowns!) with real mini 3D cover cards
 * - Real-time color synchronization across all 55+ design cards in the gallery
 * - Category filter pills + live search
 * - Live real-time title embossing on the 3D book cover
 */
public class NotebookModalDialog extends JDialog {

    public interface SaveCallback {
        void onSave(String title, String colorPayload);
    }

    // 16 Fast-accessible curated notebook cover colors
    public static final Color[] PRESET_COLORS = {
            new Color(0x25, 0x63, 0xEB), // Royal Azure
            new Color(0x1E, 0x3A, 0x8A), // Navy Midnight
            new Color(0x05, 0x96, 0x69), // Emerald Forest
            new Color(0x0D, 0x94, 0x88), // Jade Teal
            new Color(0xDC, 0x26, 0x26), // Crimson Velvet
            new Color(0xBE, 0x18, 0x5D), // Rose Wine
            new Color(0x7C, 0x3A, 0xED), // Royal Purple
            new Color(0x4F, 0x46, 0xE5), // Indigo
            new Color(0xEA, 0x58, 0x0C), // Sunset Coral
            new Color(0xD9, 0x77, 0x06), // Amber Ochre
            new Color(0x92, 0x40, 0x0E), // Warm Cognac
            new Color(0x78, 0x35, 0x0F), // Espresso Leather
            new Color(0x4D, 0x7C, 0x0F), // Olive Sage
            new Color(0x47, 0x55, 0x69), // Slate Steel
            new Color(0x0F, 0x17, 0x2A), // Obsidian Titanium
            new Color(0x02, 0x84, 0xC7)  // Arctic Blue
    };

    private final SaveCallback callback;
    private final boolean isEditMode;

    private Color activeColor;
    private CoverDesign activeDesign;
    private String currentCategoryFilter = "All";
    private String currentSearchQuery = "";

    // UI Components
    private JTextField titleField;
    private JTextField subtitleField;
    private JPanel palettePanel;
    private JLabel hexLabel;
    private JPanel swatchIndicator;
    private JLabel activeDesignLabel;
    private JPanel cardsGridContainer;
    private JScrollPane cardsScrollPane;
    private JPanel bookPreviewCard;
    private JTextField searchInputField;
    private JLabel resultCountLbl;
    private JButton searchClearBtn;

    private final List<DesignCardPanel> activeCardPanels = new ArrayList<>();

    public NotebookModalDialog(Window owner, String modalTitle, String initialTitle, String initialSubtitle,
                               String initialColorHex, String initialDesignId, SaveCallback callback) {
        super(owner, modalTitle, ModalityType.APPLICATION_MODAL);
        this.callback = callback;
        this.isEditMode = initialTitle != null && !initialTitle.isEmpty();

        // Initialize state
        Color parsedColor = new Color(0x25, 0x63, 0xEB);
        if (initialColorHex != null && !initialColorHex.isBlank()) {
            try {
                parsedColor = Color.decode(initialColorHex.trim());
            } catch (Exception ignored) {}
        }
        this.activeColor = parsedColor;
        this.activeDesign = CoverDesign.fromId(initialDesignId);

        initUi(initialTitle, initialSubtitle);
    }

    public NotebookModalDialog(Window owner, String modalTitle, String initialTitle,
                               String initialColorHex, String initialDesignId, SaveCallback callback) {
        this(owner, modalTitle, initialTitle, null, initialColorHex, initialDesignId, callback);
    }

    private void initUi(String initialTitle, String initialSubtitle) {
        // Spacious dialog geometry to ensure complete unclipped rendering
        setSize(1020, 760);
        setMinimumSize(new Dimension(980, 720));
        setLocationRelativeTo(getOwner());
        setResizable(true);

        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(theme.surface());

        // 1. Header with icon and subtitle
        JPanel header = new JPanel(new BorderLayout(14, 4));
        header.setBackground(theme.isDark() ? new Color(24, 32, 47) : new Color(248, 250, 252));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border()),
                new EmptyBorder(12, 24, 12, 24)
        ));

        JLabel iconLabel = new JLabel(VectorIcons.create(VectorIcons.Type.BOOK, 26, theme.accent()));
        JPanel headerTextPnl = new JPanel(new GridLayout(2, 1, 0, 2));
        headerTextPnl.setOpaque(false);

        JLabel titleLbl = new JLabel(isEditMode ? "Customize Notebook Cover" : "Create New Notebook");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(theme.textPrimary());

        JLabel subtitleLbl = new JLabel("Fast accessible color palette, interactive design cards gallery, and live 3D cover preview");
        subtitleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLbl.setForeground(theme.textSecondary());

        headerTextPnl.add(titleLbl);
        headerTextPnl.add(subtitleLbl);

        header.add(iconLabel, BorderLayout.WEST);
        header.add(headerTextPnl, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        // 2. Main Center Body: Left Config & Card Gallery vs Right Live 3D Cover Preview
        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setBackground(theme.surface());
        body.setBorder(new EmptyBorder(10, 24, 10, 24));

        // Left Config & Design Gallery (takes center)
        body.add(createLeftControls(theme, initialTitle, initialSubtitle), BorderLayout.CENTER);

        // Right 3D Book Preview (fixed width east pane with plenty of margin)
        body.add(createRightPreviewContainer(theme), BorderLayout.EAST);

        root.add(body, BorderLayout.CENTER);

        // 3. Bottom Button Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        bottomBar.setBackground(theme.isDark() ? new Color(24, 32, 47) : new Color(248, 250, 252));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.border()));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(isEditMode ? "Save Changes" : "Create Notebook",
                VectorIcons.create(isEditMode ? VectorIcons.Type.CHECKMARK : VectorIcons.Type.PLUS, 14, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color btnBg = getModel().isPressed()
                        ? theme.accent().darker()
                        : (getModel().isRollover() ? theme.accent().brighter() : theme.accent());
                g2.setColor(btnBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(isEditMode ? 145 : 165, 38));
        saveBtn.setOpaque(false);
        saveBtn.setContentAreaFilled(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> doSave());

        // Keyboard actions: Enter saves, Escape closes
        getRootPane().setDefaultButton(saveBtn);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        bottomBar.add(cancelBtn);
        bottomBar.add(saveBtn);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createLeftControls(ThemeManager.Theme theme, String initialTitle, String initialSubtitle) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Top Section: Title, Subtopic & Color Palette
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // --- 1. TITLE INPUT ---
        JPanel titleHeaderPnl = new JPanel(new BorderLayout());
        titleHeaderPnl.setOpaque(false);
        titleHeaderPnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel titleLbl = new JLabel("Notebook Title:");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(theme.textPrimary());
        titleHeaderPnl.add(titleLbl, BorderLayout.WEST);

        topSection.add(titleHeaderPnl);
        topSection.add(Box.createVerticalStrut(3));

        titleField = new JTextField(initialTitle != null ? initialTitle : "");
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleField.setPreferredSize(new Dimension(400, 30));
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        titleField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1, true),
                new EmptyBorder(3, 10, 3, 10)
        ));
        titleField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateLiveTitle(); }
            public void removeUpdate(DocumentEvent e) { updateLiveTitle(); }
            public void changedUpdate(DocumentEvent e) { updateLiveTitle(); }
        });
        topSection.add(titleField);

        topSection.add(Box.createVerticalStrut(5));

        // --- 1B. SUBTOPIC / SUBTITLE INPUT ---
        JPanel subtitleHeaderPnl = new JPanel(new BorderLayout());
        subtitleHeaderPnl.setOpaque(false);
        subtitleHeaderPnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        JLabel subtitleLbl = new JLabel("Subtopic / Subtitle (Optional):");
        subtitleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        subtitleLbl.setForeground(theme.textPrimary());
        subtitleHeaderPnl.add(subtitleLbl, BorderLayout.WEST);

        topSection.add(subtitleHeaderPnl);
        topSection.add(Box.createVerticalStrut(2));

        subtitleField = new JTextField(initialSubtitle != null ? initialSubtitle : "");
        subtitleField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleField.setPreferredSize(new Dimension(400, 30));
        subtitleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        subtitleField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1, true),
                new EmptyBorder(3, 10, 3, 10)
        ));
        subtitleField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateLiveTitle(); }
            public void removeUpdate(DocumentEvent e) { updateLiveTitle(); }
            public void changedUpdate(DocumentEvent e) { updateLiveTitle(); }
        });
        topSection.add(subtitleField);

        topSection.add(Box.createVerticalStrut(6));

        // --- 2. FAST ACCESSIBLE COLOR PALETTE ---
        JPanel colorHeaderPnl = new JPanel(new BorderLayout());
        colorHeaderPnl.setOpaque(false);
        colorHeaderPnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel colorLbl = new JLabel("Cover Color Theme (Instant Selection):");
        colorLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        colorLbl.setForeground(theme.textPrimary());
        colorHeaderPnl.add(colorLbl, BorderLayout.WEST);

        // Badge showing currently active color chip + hex
        JPanel badgePnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badgePnl.setOpaque(false);

        swatchIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(activeColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(0, 0, 0, 30));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };
        swatchIndicator.setPreferredSize(new Dimension(18, 18));

        hexLabel = new JLabel(formatHex(activeColor));
        hexLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        hexLabel.setForeground(theme.textSecondary());

        badgePnl.add(swatchIndicator);
        badgePnl.add(hexLabel);
        colorHeaderPnl.add(badgePnl, BorderLayout.EAST);

        topSection.add(colorHeaderPnl);
        topSection.add(Box.createVerticalStrut(4));

        // 16 Swatch Palette + Custom Color Button
        palettePanel = createFastPaletteGrid(theme);
        palettePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        topSection.add(palettePanel);

        panel.add(topSection, BorderLayout.NORTH);

        // --- 3. VISUAL DESIGN CARD GALLERY (NO DROPDOWN!) ---
        JPanel gallerySection = new JPanel(new BorderLayout(0, 8));
        gallerySection.setOpaque(false);

        // Gallery Header: Title + Active Badge
        JPanel galleryHeaderPnl = new JPanel(new BorderLayout());
        galleryHeaderPnl.setOpaque(false);

        JLabel galleryLbl = new JLabel("Cover Watermark Design Gallery (" + CoverDesign.ALL_DESIGNS.size() + "+ Creative Designs):");
        galleryLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        galleryLbl.setForeground(theme.textPrimary());
        galleryHeaderPnl.add(galleryLbl, BorderLayout.WEST);

        activeDesignLabel = new JLabel(activeDesign.getDisplayName() + " • " + activeDesign.getCategory());
        activeDesignLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        activeDesignLabel.setForeground(theme.accent());
        galleryHeaderPnl.add(activeDesignLabel, BorderLayout.EAST);

        gallerySection.add(galleryHeaderPnl, BorderLayout.NORTH);

        // Center: Full-width Search Bar + Redesigned Filter Tabs + Scrollable Cards
        JPanel centerWrap = new JPanel(new BorderLayout(0, 8));
        centerWrap.setOpaque(false);

        JPanel controlsGroup = new JPanel();
        controlsGroup.setLayout(new BoxLayout(controlsGroup, BoxLayout.Y_AXIS));
        controlsGroup.setOpaque(false);

        // 1. Prominent Full-width Search Bar
        controlsGroup.add(createSearchBar(theme));
        controlsGroup.add(Box.createVerticalStrut(6));

        // 2. Redesigned Category Filter Tabs
        controlsGroup.add(createCategoryChips(theme));

        centerWrap.add(controlsGroup, BorderLayout.NORTH);

        // Center: Scrollable Design Cards Grid
        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setBackground(theme.surface());

        cardsGridContainer = new JPanel(new GridLayout(0, 5, 8, 8));
        cardsGridContainer.setBackground(theme.surface());
        gridWrap.add(cardsGridContainer, BorderLayout.NORTH);

        cardsScrollPane = new JScrollPane(gridWrap);
        cardsScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        cardsScrollPane.getVerticalScrollBar().setUnitIncrement(24);
        cardsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cardsScrollPane.getViewport().setBackground(theme.surface());

        centerWrap.add(cardsScrollPane, BorderLayout.CENTER);
        gallerySection.add(centerWrap, BorderLayout.CENTER);
        panel.add(gallerySection, BorderLayout.CENTER);

        // Populate initial cards
        rebuildDesignCards(theme);

        return panel;
    }

    private JPanel createFastPaletteGrid(ThemeManager.Theme theme) {
        JPanel container = new JPanel(new BorderLayout(10, 6));
        container.setOpaque(false);

        // 2 rows of 8 color chips
        JPanel grid = new JPanel(new GridLayout(2, 8, 8, 8));
        grid.setOpaque(false);

        for (Color c : PRESET_COLORS) {
            JButton chip = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    boolean isSelected = isColorMatch(c, activeColor);
                    int w = getWidth();
                    int h = getHeight();

                    // Base color fill
                    g2.setColor(c);
                    g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);

                    // Border / Outline
                    if (isSelected) {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2.0f));
                        g2.drawRoundRect(3, 3, w - 7, h - 7, 7, 7);

                        // Selected checkmark in center
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(w / 2 - 4, h / 2, w / 2 - 1, h / 2 + 3);
                        g2.drawLine(w / 2 - 1, h / 2 + 3, w / 2 + 4, h / 2 - 3);
                    } else if (getModel().isRollover()) {
                        g2.setColor(new Color(255, 255, 255, 140));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
                    } else {
                        g2.setColor(new Color(0, 0, 0, 30));
                        g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
                    }
                    g2.dispose();
                }
            };

            chip.setPreferredSize(new Dimension(30, 30));
            chip.setFocusPainted(false);
            chip.setContentAreaFilled(false);
            chip.setBorderPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.setToolTipText("Select " + formatHex(c));
            chip.addActionListener(e -> selectColor(c));
            grid.add(chip);
        }

        container.add(grid, BorderLayout.CENTER);

        // "Custom Color..." button
        JButton customBtn = new JButton("Custom...", VectorIcons.create(VectorIcons.Type.PALETTE, 13, theme.textPrimary()));
        customBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        customBtn.setFocusPainted(false);
        customBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        customBtn.setToolTipText("Pick any custom color using full 2D HSB canvas");
        customBtn.addActionListener(e -> {
            Color chosen = ModernColorPickerDialog.showDialog(this, "Choose Custom Notebook Color", activeColor);
            if (chosen != null) {
                selectColor(chosen);
            }
        });

        container.add(customBtn, BorderLayout.EAST);
        return container;
    }

    private JPanel createSearchBar(ThemeManager.Theme theme) {
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(theme.isDark() ? new Color(30, 41, 59) : new Color(248, 250, 252));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        searchBar.setPreferredSize(new Dimension(500, 34));

        JLabel searchIcon = new JLabel(VectorIcons.create(VectorIcons.Type.SEARCH, 15, theme.accent()));
        searchBar.add(searchIcon, BorderLayout.WEST);

        searchInputField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !(getTopLevelAncestor() != null && hasFocus())) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    g2.setColor(theme.textSecondary());
                    g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("Search designs by keyword (flower, star, robot, leaf, space, tech, art)...", 4, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        searchInputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchInputField.setForeground(theme.textPrimary());
        searchInputField.setOpaque(false);
        searchInputField.setBorder(null);
        searchInputField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onSearchChanged(); }
            public void removeUpdate(DocumentEvent e) { onSearchChanged(); }
            public void changedUpdate(DocumentEvent e) { onSearchChanged(); }
        });
        searchInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !searchInputField.getText().isEmpty()) {
                    searchInputField.setText("");
                    e.consume();
                }
            }
        });
        searchBar.add(searchInputField, BorderLayout.CENTER);

        JPanel eastPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        eastPnl.setOpaque(false);

        resultCountLbl = new JLabel(CoverDesign.ALL_DESIGNS.size() + " designs");
        resultCountLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        resultCountLbl.setForeground(theme.textSecondary());
        eastPnl.add(resultCountLbl);

        searchClearBtn = new JButton("×");
        searchClearBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        searchClearBtn.setForeground(theme.textSecondary());
        searchClearBtn.setPreferredSize(new Dimension(20, 20));
        searchClearBtn.setFocusPainted(false);
        searchClearBtn.setBorder(null);
        searchClearBtn.setContentAreaFilled(false);
        searchClearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchClearBtn.setVisible(false);
        searchClearBtn.addActionListener(e -> {
            searchInputField.setText("");
            searchInputField.requestFocus();
        });
        eastPnl.add(searchClearBtn);

        searchBar.add(eastPnl, BorderLayout.EAST);
        return searchBar;
    }

    private void onSearchChanged() {
        String text = searchInputField != null ? searchInputField.getText().trim() : "";
        if (searchClearBtn != null) {
            searchClearBtn.setVisible(!text.isEmpty());
        }
        filterSearch(text);
    }

    private JPanel createCategoryChips(ThemeManager.Theme theme) {
        JPanel chips = new JPanel(new GridLayout(1, 6, 6, 0));
        chips.setOpaque(false);
        chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        chips.setPreferredSize(new Dimension(560, 30));

        String[] categories = {"All", "Nature", "Celestial", "Tech", "Geometric", "Arts"};
        ButtonGroup bg = new ButtonGroup();

        for (String cat : categories) {
            int count = 0;
            if ("All".equals(cat)) {
                count = CoverDesign.ALL_DESIGNS.size();
            } else {
                for (CoverDesign cd : CoverDesign.ALL_DESIGNS) {
                    if (cd.getCategory().equalsIgnoreCase(cat)) count++;
                }
            }

            final String fCat = cat;
            String tabLabel = cat + " (" + count + ")";

            JToggleButton btn = new JToggleButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    boolean sel = isSelected();
                    int w = getWidth();
                    int h = getHeight();
                    if (sel) {
                        g2.setColor(theme.accent());
                        g2.fillRoundRect(0, 0, w, h, 14, 14);
                    } else if (getModel().isRollover()) {
                        g2.setColor(theme.isDark() ? new Color(40, 53, 76) : new Color(226, 232, 240));
                        g2.fillRoundRect(0, 0, w, h, 14, 14);
                        g2.setColor(theme.border());
                        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                    } else {
                        g2.setColor(theme.isDark() ? new Color(26, 36, 52) : new Color(241, 245, 249));
                        g2.fillRoundRect(0, 0, w, h, 14, 14);
                        g2.setColor(theme.border());
                        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                    }

                    // Directly draw centered text to guarantee zero truncation
                    g2.setFont(new Font("Segoe UI", sel ? Font.BOLD : Font.PLAIN, 11));
                    g2.setColor(sel ? Color.WHITE : theme.textPrimary());
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (w - fm.stringWidth(tabLabel)) / 2;
                    int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(tabLabel, tx, ty);

                    g2.dispose();
                }
            };

            btn.setSelected("All".equals(cat));
            btn.setFont(new Font("Segoe UI", btn.isSelected() ? Font.BOLD : Font.PLAIN, 11));
            btn.setForeground(btn.isSelected() ? Color.WHITE : theme.textPrimary());
            btn.setHorizontalAlignment(SwingConstants.CENTER);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setMargin(new Insets(2, 4, 2, 4));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                btn.setForeground(Color.WHITE);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                for (Component c : chips.getComponents()) {
                    if (c instanceof JToggleButton tb && tb != btn) {
                        tb.setForeground(theme.textPrimary());
                        tb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    }
                }
                this.currentCategoryFilter = fCat;
                rebuildDesignCards(theme);
            });

            bg.add(btn);
            chips.add(btn);
        }

        return chips;
    }

    private void filterSearch(String query) {
        this.currentSearchQuery = query != null ? query.trim().toLowerCase() : "";
        rebuildDesignCards(ThemeManager.getInstance().getCurrentTheme());
    }

    private void rebuildDesignCards(ThemeManager.Theme theme) {
        cardsGridContainer.removeAll();
        activeCardPanels.clear();

        for (CoverDesign cd : CoverDesign.ALL_DESIGNS) {
            // Category check
            if (!"All".equals(currentCategoryFilter) && !cd.getCategory().equalsIgnoreCase(currentCategoryFilter)) {
                continue;
            }
            // Search query check with rich keywords
            if (!currentSearchQuery.isEmpty() && !cd.matchesSearch(currentSearchQuery)) {
                continue;
            }

            DesignCardPanel card = new DesignCardPanel(cd, theme);
            activeCardPanels.add(card);
        }

        if (resultCountLbl != null) {
            resultCountLbl.setText(activeCardPanels.size() + (activeCardPanels.size() == 1 ? " design" : " designs"));
        }

        if (activeCardPanels.isEmpty()) {
            JPanel emptyPnl = new JPanel(new GridBagLayout());
            emptyPnl.setOpaque(false);
            emptyPnl.setPreferredSize(new Dimension(500, 160));

            JPanel inner = new JPanel();
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.setOpaque(false);

            JLabel noRes = new JLabel("No designs match \"" + currentSearchQuery + "\"");
            noRes.setFont(new Font("Segoe UI", Font.BOLD, 13));
            noRes.setForeground(theme.textPrimary());
            noRes.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel hint = new JLabel("Try searching by keyword like 'flower', 'star', 'robot', 'tree', 'cyber', or 'geometry'.");
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            hint.setForeground(theme.textSecondary());
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);

            inner.add(noRes);
            inner.add(Box.createVerticalStrut(6));
            inner.add(hint);
            emptyPnl.add(inner);

            cardsGridContainer.setLayout(new BorderLayout());
            cardsGridContainer.add(emptyPnl, BorderLayout.CENTER);
        } else {
            cardsGridContainer.setLayout(new GridLayout(0, 5, 8, 8));
            for (DesignCardPanel card : activeCardPanels) {
                cardsGridContainer.add(card);
            }
        }

        cardsGridContainer.revalidate();
        cardsGridContainer.repaint();
    }

    /**
     * Interactive miniature visual design card inside the gallery.
     */
    private class DesignCardPanel extends JPanel {
        private final CoverDesign design;
        private final ThemeManager.Theme theme;
        private boolean isHovered = false;

        public DesignCardPanel(CoverDesign design, ThemeManager.Theme theme) {
            this.design = design;
            this.theme = theme;
            setPreferredSize(new Dimension(96, 122));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(design.getDisplayName() + " (" + design.getCategory() + ")");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectDesign(design);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            boolean isSelected = (design == activeDesign);
            int w = getWidth();
            int h = getHeight();

            int bookW = w - 10;
            int bookH = h - 28;
            int bookX = 5;
            int bookY = isHovered ? 2 : 4;

            // 1. Hover/Selected Card Background Glow
            if (isSelected) {
                g2.setColor(new Color(37, 99, 235, 35));
                g2.fillRoundRect(2, 1, w - 4, h - 2, 8, 8);
                g2.setColor(new Color(37, 99, 235));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(2, 1, w - 5, h - 3, 8, 8);
            } else if (isHovered) {
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(2, 1, w - 4, h - 2, 8, 8);
            }

            // 2. Mini Drop Shadow
            g2.setColor(new Color(0, 0, 0, 35));
            g2.fillRoundRect(bookX + 2, bookY + 3, bookW, bookH, 8, 8);

            // 3. Mini Hardcover in ACTIVE COLOR
            g2.setColor(activeColor);
            g2.fillRoundRect(bookX, bookY, bookW, bookH, 6, 6);

            // 4. Render the vector watermark design scaled to fit 100% COMPLETELY within the mini card front
            int miniSpineW = 10;
            int frontMiniW = bookW - miniSpineW;
            Graphics2D gDesign = (Graphics2D) g2.create();
            gDesign.clip(new RoundRectangle2D.Float(bookX, bookY, bookW, bookH, 6, 6));

            double targetW = frontMiniW - 8;
            double targetH = bookH - 10;
            double scale = Math.min(targetW / CoverDesign.REF_WIDTH, targetH / CoverDesign.REF_HEIGHT) * 0.86;
            double drawW = CoverDesign.REF_WIDTH * scale;
            double drawH = CoverDesign.REF_HEIGHT * scale;
            double offsetX = bookX + miniSpineW + (frontMiniW - drawW) / 2.0;
            double offsetY = bookY + (bookH - drawH) / 2.0;

            gDesign.translate(offsetX, offsetY);
            gDesign.scale(scale, scale);
            design.render(gDesign, (int)CoverDesign.REF_WIDTH, (int)CoverDesign.REF_HEIGHT, activeColor);
            gDesign.dispose();

            // 5. Mini Spine
            GradientPaint spine = new GradientPaint(
                    bookX, 0, new Color(0, 0, 0, 75),
                    bookX + 10, 0, new Color(255, 255, 255, 20)
            );
            g2.setPaint(spine);
            g2.fillRect(bookX, bookY, 10, bookH);

            // 6. Surface Gloss
            GradientPaint gloss = new GradientPaint(
                    bookX, bookY, new Color(255, 255, 255, 45),
                    bookX + bookW, bookY + bookH, new Color(0, 0, 0, 45)
            );
            g2.setPaint(gloss);
            g2.fillRoundRect(bookX, bookY, bookW, bookH, 6, 6);

            // 7. Mini Selected Checkmark Badge (Top Right)
            if (isSelected) {
                g2.setColor(new Color(37, 99, 235));
                g2.fillOval(bookX + bookW - 14, bookY - 3, 16, 16);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(bookX + bookW - 10, bookY + 5, bookX + bookW - 7, bookY + 8);
                g2.drawLine(bookX + bookW - 7, bookY + 8, bookX + bookW - 3, bookY + 3);
            }

            // 8. Design Name Label underneath
            g2.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            String name = design.getDisplayName();
            if (fm.stringWidth(name) > w - 4) {
                while (fm.stringWidth(name + "..") > w - 4 && name.length() > 2) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "..";
            }
            int tx = (w - fm.stringWidth(name)) / 2;
            g2.setColor(isSelected ? new Color(37, 99, 235) : theme.textPrimary());
            g2.drawString(name, tx, h - 6);

            g2.dispose();
        }
    }

    private JPanel createRightPreviewContainer(ThemeManager.Theme theme) {
        // Dedicated fixed-width container (310px) to guarantee zero clipping of the 3D book cover
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setPreferredSize(new Dimension(310, 520));
        container.setMinimumSize(new Dimension(310, 500));
        container.setBackground(theme.isDark() ? new Color(18, 24, 38) : new Color(241, 245, 249));
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Preview Header (Properly stacked to prevent any collision)
        JPanel topRow = new JPanel(new GridLayout(2, 1, 0, 2));
        topRow.setOpaque(false);

        JLabel previewTitle = new JLabel("Live 3D Cover Preview");
        previewTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        previewTitle.setForeground(theme.textPrimary());

        JLabel ratioBadge = new JLabel("Standard Notebook • 1:1.4 Ratio");
        ratioBadge.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        ratioBadge.setForeground(theme.textSecondary());

        topRow.add(previewTitle);
        topRow.add(ratioBadge);
        container.add(topRow, BorderLayout.NORTH);

        // Center wrap with centered book cover
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);

        bookPreviewCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                renderRealisticBookCover((Graphics2D) g, getWidth(), getHeight());
            }
        };

        // Realistic Notebook aspect ratio: Width 200, Height 280
        bookPreviewCard.setPreferredSize(new Dimension(200, 280));
        bookPreviewCard.setMinimumSize(new Dimension(200, 280));
        bookPreviewCard.setMaximumSize(new Dimension(200, 280));
        bookPreviewCard.setOpaque(false);

        centerWrap.add(bookPreviewCard);
        container.add(centerWrap, BorderLayout.CENTER);

        return container;
    }

    private static class TitleLayoutResult {
        final List<String> lines;
        final Font font;
        final int lineHeight;
        final int totalHeight;

        TitleLayoutResult(List<String> lines, Font font, int lineHeight, int totalHeight) {
            this.lines = lines;
            this.font = font;
            this.lineHeight = lineHeight;
            this.totalHeight = totalHeight;
        }
    }

    private TitleLayoutResult layoutTitleText(Graphics2D g2, String text, int maxW) {
        if (text == null || text.isBlank()) {
            text = "Untitled Notebook";
        }
        text = text.trim();

        // 1. Try single line: from 15pt down to 11pt
        for (int pt = 15; pt >= 11; pt--) {
            Font f = new Font("Segoe UI", Font.BOLD, pt);
            FontMetrics fm = g2.getFontMetrics(f);
            if (fm.stringWidth(text) <= maxW) {
                return new TitleLayoutResult(List.of(text), f, fm.getHeight(), fm.getHeight());
            }
        }

        // 2. Try 2 lines: from 13pt down to 10pt
        String[] words = text.split("\\s+");
        if (words.length > 1) {
            for (int pt = 13; pt >= 10; pt--) {
                Font f = new Font("Segoe UI", Font.BOLD, pt);
                FontMetrics fm = g2.getFontMetrics(f);

                int bestSplit = -1;
                int minDiff = Integer.MAX_VALUE;
                for (int i = 1; i < words.length; i++) {
                    StringBuilder l1 = new StringBuilder();
                    for (int j = 0; j < i; j++) {
                        if (j > 0) l1.append(" ");
                        l1.append(words[j]);
                    }
                    StringBuilder l2 = new StringBuilder();
                    for (int j = i; j < words.length; j++) {
                        if (j > i) l2.append(" ");
                        l2.append(words[j]);
                    }
                    int w1 = fm.stringWidth(l1.toString());
                    int w2 = fm.stringWidth(l2.toString());
                    if (w1 <= maxW && w2 <= maxW) {
                        int diff = Math.abs(w1 - w2);
                        if (diff < minDiff) {
                            minDiff = diff;
                            bestSplit = i;
                        }
                    }
                }

                if (bestSplit != -1) {
                    StringBuilder l1 = new StringBuilder();
                    for (int j = 0; j < bestSplit; j++) {
                        if (j > 0) l1.append(" ");
                        l1.append(words[j]);
                    }
                    StringBuilder l2 = new StringBuilder();
                    for (int j = bestSplit; j < words.length; j++) {
                        if (j > bestSplit) l2.append(" ");
                        l2.append(words[j]);
                    }
                    int lh = (int)(fm.getHeight() * 0.95);
                    return new TitleLayoutResult(List.of(l1.toString(), l2.toString()), f, lh, lh * 2);
                }
            }
        }

        // 3. Try 3 lines if multi-word: 10pt down to 9pt
        if (words.length >= 3) {
            for (int pt = 10; pt >= 9; pt--) {
                Font f = new Font("Segoe UI", Font.BOLD, pt);
                FontMetrics fm = g2.getFontMetrics(f);
                List<String> lines = new ArrayList<>();
                StringBuilder cur = new StringBuilder();
                boolean possible = true;
                for (String w : words) {
                    String cand = cur.length() == 0 ? w : cur + " " + w;
                    if (fm.stringWidth(cand) <= maxW) {
                        cur = new StringBuilder(cand);
                    } else {
                        if (cur.length() > 0) {
                            lines.add(cur.toString());
                            cur = new StringBuilder(w);
                            if (fm.stringWidth(w) > maxW) { possible = false; break; }
                        } else {
                            possible = false;
                            break;
                        }
                    }
                }
                if (cur.length() > 0) lines.add(cur.toString());
                if (possible && lines.size() <= 3) {
                    int lh = (int)(fm.getHeight() * 0.95);
                    return new TitleLayoutResult(lines, f, lh, lh * lines.size());
                }
            }
        }

        // 4. Fallback: Wrap greedy at 9pt and truncate words if too long
        Font f = new Font("Segoe UI", Font.BOLD, 9);
        FontMetrics fm = g2.getFontMetrics(f);
        List<String> fallbackLines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String cand = cur.length() == 0 ? w : cur + " " + w;
            if (fm.stringWidth(cand) <= maxW) {
                cur = new StringBuilder(cand);
            } else {
                if (cur.length() > 0) {
                    fallbackLines.add(cur.toString());
                    cur = new StringBuilder();
                }
                String truncated = w;
                while (fm.stringWidth(truncated + "..") > maxW && truncated.length() > 2) {
                    truncated = truncated.substring(0, truncated.length() - 1);
                }
                cur.append(truncated.equals(w) ? w : truncated + "..");
            }
            if (fallbackLines.size() >= 2) break;
        }
        if (cur.length() > 0 && fallbackLines.size() < 3) {
            fallbackLines.add(cur.toString());
        }
        int lh = (int)(fm.getHeight() * 0.95);
        return new TitleLayoutResult(fallbackLines, f, lh, lh * fallbackLines.size());
    }

    /**
     * Renders a realistic 3D notebook cover with drop shadow, spine band,
     * stitch line, foil embossed live title (auto-sized and multi-line wrapped),
     * optional subtopic foil subtitle, perfectly centered watermark design, and page edge depth.
     */
    private void renderRealisticBookCover(Graphics2D g, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int padX = 8;
        int padY = 8;
        int bw = width - (padX * 2);
        int bh = height - (padY * 2);

        // 1. Multi-pass Atmospheric Soft Drop Shadow
        for (int i = 6; i >= 1; i--) {
            int alpha = 8 + (7 - i) * 5;
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRoundRect(padX + 2, padY + 4 + (i * 2), bw, bh, 16, 16);
        }

        // 2. Page Stack Depth (Right Edge Layering)
        g2.setColor(new Color(245, 243, 237));
        g2.fillRoundRect(padX + bw - 6, padY + 4, 8, bh - 8, 6, 6);
        g2.setColor(new Color(214, 211, 201));
        for (int lineY = padY + 12; lineY < padY + bh - 12; lineY += 6) {
            g2.drawLine(padX + bw - 4, lineY, padX + bw - 1, lineY);
        }

        // 3. Main Hardcover Base in ACTIVE COLOR
        g2.setColor(activeColor);
        g2.fillRoundRect(padX, padY, bw, bh, 14, 14);

        // Calculate title & subtitle layout to dynamically position the divider
        String title = titleField != null ? titleField.getText().trim() : "";
        if (title.isEmpty()) title = "Untitled Notebook";
        String subtitle = subtitleField != null ? subtitleField.getText().trim() : "";

        int spineW = 22;
        int titleAreaX = padX + spineW + 12;
        int titleAreaW = bw - spineW - 22;

        TitleLayoutResult titleLayout = layoutTitleText(g2, title, titleAreaW);

        int totalBlockH = titleLayout.totalHeight + (!subtitle.isEmpty() ? 15 : 0) + 16;
        int titleAreaY = padY + bh - totalBlockH - 12;

        // 4. Vector Watermark Design (Centred in Upper Canvas between spine and right edge)
        if (activeDesign != null) {
            int frontX = padX + spineW;
            int frontY = padY + 6;
            int frontW = bw - spineW;
            int upperH = (titleAreaY - 14) - frontY;
            if (upperH > 50) {
                Graphics2D gDesign = (Graphics2D) g2.create();
                gDesign.clip(new RoundRectangle2D.Float(padX, padY, bw, bh, 14, 14));
                double scale = Math.min((frontW - 16) / CoverDesign.REF_WIDTH, (upperH - 16) / CoverDesign.REF_HEIGHT) * 0.88;
                double drawW = CoverDesign.REF_WIDTH * scale;
                double drawH = CoverDesign.REF_HEIGHT * scale;
                double drawX = frontX + (frontW - drawW) / 2.0;
                double drawY = frontY + (upperH - drawH) / 2.0;
                gDesign.translate(drawX, drawY);
                gDesign.scale(scale, scale);
                activeDesign.render(gDesign, (int)CoverDesign.REF_WIDTH, (int)CoverDesign.REF_HEIGHT, activeColor);
                gDesign.dispose();
            }
        }

        // 5. Realistic Leather Spine (Left Band)
        GradientPaint spineGrad = new GradientPaint(
                padX, 0, new Color(0, 0, 0, 95),
                padX + spineW, 0, new Color(255, 255, 255, 25)
        );
        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(padX, padY, bw, bh, 14, 14));
        g2.setPaint(spineGrad);
        g2.fillRect(padX, padY, spineW, bh);

        // Golden / White Stitching line
        g2.setColor(new Color(255, 255, 255, 80));
        Stroke dashed = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);
        g2.setStroke(dashed);
        g2.drawLine(padX + spineW, padY + 8, padX + spineW, padY + bh - 8);

        // 6. Gold Bookmark Ribbon (Hanging from top)
        int rw = 14, rh = 26, rx = padX + bw - 36;
        g2.setColor(new Color(245, 158, 11));
        Polygon ribbon = new Polygon();
        ribbon.addPoint(rx, padY);
        ribbon.addPoint(rx + rw, padY);
        ribbon.addPoint(rx + rw, padY + rh);
        ribbon.addPoint(rx + rw / 2, padY + rh - 5);
        ribbon.addPoint(rx, padY + rh);
        g2.fillPolygon(ribbon);

        // Ribbon highlight
        g2.setColor(new Color(251, 191, 36));
        g2.drawLine(rx + 2, padY, rx + 2, padY + rh - 4);

        // 7. Live Embossed / Foil Title & Subtitle on Cover
        // Subtle decorative horizontal divider rule
        g2.setColor(new Color(255, 255, 255, 80));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(titleAreaX, titleAreaY - 6, titleAreaX + titleAreaW, titleAreaY - 6);

        // Main Title lines with debossed shadow + foil fill
        int curY = titleAreaY + titleLayout.lineHeight - 2;
        g2.setFont(titleLayout.font);
        for (String line : titleLayout.lines) {
            // Shadow / deboss
            g2.setColor(new Color(0, 0, 0, 110));
            g2.drawString(line, titleAreaX + 1, curY + 1);

            // Foil text
            g2.setColor(Color.WHITE);
            g2.drawString(line, titleAreaX, curY);

            curY += titleLayout.lineHeight;
        }

        // Subtopic / Subtitle (if specified)
        if (!subtitle.isEmpty()) {
            curY += 2;
            Font subFont = new Font("Segoe UI", Font.ITALIC, 10);
            FontMetrics subFm = g2.getFontMetrics(subFont);
            String dispSub = subtitle;
            while (subFm.stringWidth(dispSub) > titleAreaW && dispSub.length() > 3) {
                dispSub = dispSub.substring(0, dispSub.length() - 4) + "..";
            }
            g2.setFont(subFont);
            g2.setColor(new Color(0, 0, 0, 95));
            g2.drawString(dispSub, titleAreaX + 1, curY + 1);
            g2.setColor(new Color(254, 240, 138, 225)); // Gold metallic foil
            g2.drawString(dispSub, titleAreaX, curY);
            curY += 13;
        }

        // NoteDown brand subtitle in small caps
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        g2.setColor(new Color(255, 255, 255, 160));
        g2.drawString("NOTEDOWN STUDIO", titleAreaX, curY + 10);

        // 8. Overall Gloss & Lighting Reflection
        GradientPaint gloss = new GradientPaint(
                padX, padY, new Color(255, 255, 255, 50),
                padX + bw, padY + bh, new Color(0, 0, 0, 60)
        );
        g2.setPaint(gloss);
        g2.fillRoundRect(padX, padY, bw, bh, 14, 14);

        // Cover border outline
        g2.setColor(new Color(255, 255, 255, 45));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(padX, padY, bw - 1, bh - 1, 14, 14);

        g2.setClip(oldClip);
        g2.dispose();
    }

    private void selectColor(Color c) {
        this.activeColor = c;
        if (hexLabel != null) hexLabel.setText(formatHex(c));
        if (swatchIndicator != null) swatchIndicator.repaint();
        if (palettePanel != null) palettePanel.repaint();

        // Repaint all miniature design cards in the gallery so they immediately reflect the new color!
        for (DesignCardPanel p : activeCardPanels) {
            p.repaint();
        }

        refreshLivePreview();
    }

    private void selectDesign(CoverDesign cd) {
        this.activeDesign = cd;
        if (activeDesignLabel != null) {
            activeDesignLabel.setText(cd.getDisplayName() + " • " + cd.getCategory());
        }

        // Repaint all cards to update selection rings
        for (DesignCardPanel p : activeCardPanels) {
            p.repaint();
        }

        refreshLivePreview();
    }

    private void updateLiveTitle() {
        if (bookPreviewCard != null) {
            bookPreviewCard.repaint();
        }
    }

    private void refreshLivePreview() {
        if (bookPreviewCard != null) {
            bookPreviewCard.repaint();
        }
    }

    private boolean isColorMatch(Color a, Color b) {
        if (a == null || b == null) return false;
        return a.getRed() == b.getRed() && a.getGreen() == b.getGreen() && a.getBlue() == b.getBlue();
    }

    private String formatHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void doSave() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            titleField.requestFocus();
            titleField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(239, 68, 68), 2, true),
                    new EmptyBorder(5, 11, 5, 11)
            ));
            return;
        }

        String subtitle = subtitleField != null ? subtitleField.getText().trim().replace("|", " ") : "";
        String hex = formatHex(activeColor);
        String designId = activeDesign != null ? activeDesign.getId() : "NONE";
        String payload = hex + "|" + designId + (subtitle.isEmpty() ? "" : "|" + subtitle);

        dispose();
        callback.onSave(title, payload);
    }

    public static void showModal(Window owner, String modalTitle, String initialTitle, String initialSubtitle,
                                 String initialColorHex, String initialDesignId, SaveCallback callback) {
        NotebookModalDialog dlg = new NotebookModalDialog(owner, modalTitle, initialTitle, initialSubtitle, initialColorHex, initialDesignId, callback);
        dlg.setVisible(true);
    }

    public static void showModal(Window owner, String modalTitle, String initialTitle,
                                 String initialColorHex, String initialDesignId, SaveCallback callback) {
        showModal(owner, modalTitle, initialTitle, null, initialColorHex, initialDesignId, callback);
    }
}
