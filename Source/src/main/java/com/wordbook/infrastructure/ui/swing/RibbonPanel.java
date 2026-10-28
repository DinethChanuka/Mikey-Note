package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.ChartEngineService;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Premium Modern Document Studio Ribbon Toolbar inspired by Microsoft Word & Apple Pages.
 * Features 6 dedicated tabs: Home, Insert, Chart Studio, Layout, Review, and File & Export.
 * Built with segmented pill navigation, distinctive tab accent colorways, rich quick style galleries,
 * visual page break controls, real-time image & chart customizers, and proofreading intelligence.
 */
public class RibbonPanel extends JPanel implements ThemeManager.ThemeChangeListener {

    public interface DocumentActionTarget {
        void execFormat(String command, Object arg);
        void insertHtml(String html);
        void insertText(String text);
        void showTableDialog();
        void showChartDialog();
        void exportHtml();
        void exportMarkdown();
        void printDocument();
        void showDbDiagnostics();
        void insertLocalImage();
        void insertWebImage();
        void insertQuickChart(String type, String data);
        int getWordCount();
        int getCharCount();
        void backToBookshelf();

        // Advanced Intelligent Engines
        void insertPageBreak();
        void showCitationDialog();
        void showGrammarDialog();
        void autoDetectStructure();
        void resizeSelectedImage(int widthPx);
        void alignSelectedImage(String align);
        void showImagePropertiesDialog();
        void insertCard(String cardType);

        // Page Layout & Formatting Controls
        void setPageFormat(String formatName);
        void setPageMargins(String marginName);
        void setPagePaperTone(String toneName);
        void autoFixAllGrammar();
        void replaceSelectedImageSource();

        // Table Tools (Microsoft Word-style contextual operations)
        void insertTableRowAbove();
        void insertTableRowBelow();
        void insertTableColumnLeft();
        void insertTableColumnRight();
        void deleteTableRow();
        void deleteTableColumn();
        void deleteTable();
        void applyTableStyle(String styleTheme);
        void setTableCellColor(Color color);

        // Shapes & Video Media
        void insertShape(String shapeType);
        void showVideoDialog();
        void openSelectedVideo();
        void deleteSelectedVideo();

        // Advanced Undo/Redo & AI Side Panel Integration
        AdvancedUndoManager getAdvancedUndoManager();
        void toggleAiPanel();
        void openAiTab(int index);
        void openScraperWithUrl(String url);

        // Smart Accessible Options
        void speakSelectionOrDocument();
        void toggleHighContrastMode();
        void toggleDyslexicFont();
        void showHeadingsNavigator();
        void runAccessibilityAudit();

        // Image Engine Controls
        void applyImageFilter(String filter);
        void setImageWrap(String wrap);

        // Word-Style Editing & Search Operations
        default void showFindDialog() {}
        default void showReplaceDialog() {}
        default void showWordCountDialog() {}

        // Paper Ruling & Spacing
        default void setPaperPattern(String pattern) {}
        default void setRuleSpacing(int spacing) {}

        // Chart Studio & Media Sizing
        default void insertQuickChart(String type, String data, String themeStr, int width, int height) {
            insertQuickChart(type, data);
        }
        default void stepSelectedImageSize(int deltaPx) {}

        // Contextual Chart Design Operations
        default void modifySelectedChartData() {}
        default void switchSelectedChartType(String typeStr) {}
        default void switchSelectedChartTheme(String themeStr) {}
        default void switchSelectedChartStyle(String styleStr) {}
        default void setChartDetailLevel(String detailLevel) {}
        default void toggleChartValues() {}
        default void toggleChartLegend() {}
        default void reRenderSelectedChart() {}
        default void showChartPaletteDialog() {}

        // Header & Footer Design Controls
        default void setHeaderStyle(String headerStyle) {}
        default void setFooterStyle(String footerStyle) {}
        default void setHeaderLeftText(String text) {}
        default void setHeaderRightText(String text) {}
        default void closeHeaderFooterSelection() {}
        default void setHeaderFooterSelected(boolean selected, String section, int pageIndex) {}
        default boolean isHeaderFooterSelected() { return false; }

        // Card & Shape Format Controls
        default void showCardShapeCustomizerDialog() {}
        default void applyActiveCardShapeTheme(String themeKey) {}
        default void alignSelectedCardShape(String align) {}
        default void deleteSelectedCardShape() {}
        default void toggleShapeFormatPanel() {}
        default void showShapeFormatPanel() {}
        default void hideShapeFormatPanel() {}
    }

    public static final String TAB_HOME = "Home";
    public static final String TAB_INSERT = "Insert";
    public static final String TAB_AI_STUDIO = "AI & Web Studio";
    public static final String TAB_CHART = "Chart Studio";
    public static final String TAB_LAYOUT = "Layout";
    public static final String TAB_REVIEW = "Review";
    public static final String TAB_FILE = "File & Export";
    public static final String TAB_TABLE_TOOLS = "Table Tools";
    public static final String TAB_PICTURE_FORMAT = "Picture Format";
    public static final String TAB_VIDEO_TOOLS = "Video Tools";
    public static final String TAB_CHART_DESIGN = "Chart Design";
    public static final String TAB_HEADER_FOOTER = "Header & Footer";
    public static final String TAB_CARD_SHAPE_FORMAT = "Card & Shape Format";

    private final DocumentActionTarget target;

    // Navigation and Cards
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);
    private final JPanel tabBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final List<JButton> tabButtons = new ArrayList<>();
    private JButton undoButton;
    private JButton redoButton;
    private String activeTab = TAB_HOME;
    private String lastStandardTab = TAB_HOME;

    // Active color indicators
    private Color activeTextColor = new Color(30, 41, 59);
    private Color activeHighlightColor = new Color(254, 240, 138);
    private JPanel textColorSwatch;
    private JPanel hiliteColorSwatch;

    // Live statistics badges
    private JLabel statsWordsBadge;
    private JLabel statsCharsBadge;
    private JLabel statsReadTimeBadge;

    // Visual Preview Cards Lists
    private final List<ThumbCard> chartDesignCards = new ArrayList<>();
    private final List<ThumbCard> chartThemeCards = new ArrayList<>();
    private final List<ThumbCard> chartTypeCards = new ArrayList<>();
    private final List<ThumbCard> textStyleCards = new ArrayList<>();
    private final List<ThumbCard> headerDesignCards = new ArrayList<>();
    private final List<ThumbCard> footerDesignCards = new ArrayList<>();
    private final List<ThumbCard> cardTemplateCards = new ArrayList<>();
    private final List<ThumbCard> businessTemplateCards = new ArrayList<>();

    public RibbonPanel(DocumentActionTarget target) {
        this.target = target;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getRibbonBackground());
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorder()));
        ThemeManager.addListener(this);

        initTabBar();
        initCards();

        add(tabBarPanel, BorderLayout.NORTH);
        add(cardsPanel, BorderLayout.CENTER);

        selectTab(TAB_HOME);
    }

    private void initTabBar() {
        tabBarPanel.setOpaque(true);
        tabBarPanel.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));
        tabBarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorder()),
                new EmptyBorder(5, 14, 5, 14)
        ));

        // Quick Access Toolbar: Global Word-style Undo and Redo (accessible on all ribbon tabs)
        undoButton = createAdvancedUndoButton();
        redoButton = createAdvancedRedoButton();
        tabBarPanel.add(undoButton);
        tabBarPanel.add(redoButton);
        tabBarPanel.add(createTabSeparator());

        // 7 Standard Document Studio Tabs
        tabBarPanel.add(createTabButton(TAB_HOME, VectorIcons.Type.EDIT, new Color(37, 99, 235), false));
        tabBarPanel.add(createTabButton(TAB_INSERT, VectorIcons.Type.PLUS, new Color(124, 58, 237), false));
        tabBarPanel.add(createTabButton(TAB_AI_STUDIO, VectorIcons.Type.GEMINI, new Color(99, 102, 241), false));
        tabBarPanel.add(createTabButton(TAB_CHART, VectorIcons.Type.CHART, new Color(14, 165, 233), false));
        tabBarPanel.add(createTabButton(TAB_LAYOUT, VectorIcons.Type.DOCUMENT, new Color(13, 148, 136), false));
        tabBarPanel.add(createTabButton(TAB_REVIEW, VectorIcons.Type.CHECKMARK, new Color(22, 163, 74), false));
        tabBarPanel.add(createTabButton(TAB_FILE, VectorIcons.Type.EXPORT, new Color(71, 85, 105), false));

        // 6 Word-Style Contextual Tabs (appear dynamically with rich colored accents when objects are active)
        JButton btnTable = createTabButton(TAB_TABLE_TOOLS, VectorIcons.Type.TABLE, new Color(217, 119, 6), true);
        JButton btnPicture = createTabButton(TAB_PICTURE_FORMAT, VectorIcons.Type.IMAGE, new Color(124, 58, 237), true);
        JButton btnVideo = createTabButton(TAB_VIDEO_TOOLS, VectorIcons.Type.VIDEO, new Color(225, 29, 72), true);
        JButton btnChartDesign = createTabButton(TAB_CHART_DESIGN, VectorIcons.Type.CHART, new Color(14, 165, 233), true);
        JButton btnHeaderFooter = createTabButton(TAB_HEADER_FOOTER, VectorIcons.Type.DOCUMENT, new Color(13, 148, 136), true);
        JButton btnCardShape = createTabButton(TAB_CARD_SHAPE_FORMAT, VectorIcons.Type.SHAPE, new Color(14, 165, 233), true);

        btnTable.setVisible(false);
        btnPicture.setVisible(false);
        btnVideo.setVisible(false);
        btnChartDesign.setVisible(false);
        btnHeaderFooter.setVisible(false);
        btnCardShape.setVisible(false);

        tabBarPanel.add(btnTable);
        tabBarPanel.add(btnPicture);
        tabBarPanel.add(btnVideo);
        tabBarPanel.add(btnChartDesign);
        tabBarPanel.add(btnHeaderFooter);
        tabBarPanel.add(btnCardShape);
    }

    private JButton createTabButton(String tabName, VectorIcons.Type iconType, Color accentColor, boolean isContextual) {
        JButton btn = new JButton(tabName, VectorIcons.create(iconType, 14, ThemeManager.getTextPrimary()));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 16, 6, 16));
        btn.setOpaque(true);
        btn.putClientProperty("tabName", tabName);
        btn.putClientProperty("accentColor", accentColor);
        btn.putClientProperty("isContextual", isContextual);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!tabName.equals(activeTab)) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(226, 232, 240));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!tabName.equals(activeTab)) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));
                }
            }
        });

        btn.addActionListener(e -> selectTab(tabName));
        tabButtons.add(btn);
        return btn;
    }

    public void selectTab(String tabName) {
        this.activeTab = tabName;
        boolean isContextual = TAB_TABLE_TOOLS.equals(tabName) || TAB_PICTURE_FORMAT.equals(tabName) || TAB_VIDEO_TOOLS.equals(tabName) || TAB_CHART_DESIGN.equals(tabName) || TAB_HEADER_FOOTER.equals(tabName) || TAB_CARD_SHAPE_FORMAT.equals(tabName);
        if (!isContextual) {
            this.lastStandardTab = tabName;
        }

        cardLayout.show(cardsPanel, tabName);

        for (JButton btn : tabButtons) {
            String t = (String) btn.getClientProperty("tabName");
            Color accent = (Color) btn.getClientProperty("accentColor");
            if (accent == null) accent = ThemeManager.getAccent();

            if (tabName.equals(t)) {
                btn.setVisible(true);
                btn.setBackground(accent);
                btn.setForeground(Color.WHITE);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accent, 1, true),
                        new EmptyBorder(5, 15, 5, 15)
                ));
            } else {
                btn.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));
                btn.setForeground(ThemeManager.getTextPrimary());
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                btn.setBorder(new EmptyBorder(6, 16, 6, 16));
            }
        }
        tabBarPanel.revalidate();
        tabBarPanel.repaint();
    }

    public String getActiveTab() {
        return activeTab;
    }

    public void showContextualTab(String tabName) {
        for (JButton btn : tabButtons) {
            String t = (String) btn.getClientProperty("tabName");
            if (tabName.equals(t)) {
                btn.setVisible(true);
            }
        }
        selectTab(tabName);
    }

    public void hideContextualTabs() {
        boolean hadContextual = false;
        for (JButton btn : tabButtons) {
            Boolean isContextual = (Boolean) btn.getClientProperty("isContextual");
            if (Boolean.TRUE.equals(isContextual) && btn.isVisible()) {
                hadContextual = true;
                btn.setVisible(false);
            }
        }
        if (hadContextual && (TAB_TABLE_TOOLS.equals(activeTab) || TAB_PICTURE_FORMAT.equals(activeTab) || TAB_VIDEO_TOOLS.equals(activeTab) || TAB_CHART_DESIGN.equals(activeTab) || TAB_HEADER_FOOTER.equals(activeTab) || TAB_CARD_SHAPE_FORMAT.equals(activeTab))) {
            selectTab(lastStandardTab != null ? lastStandardTab : TAB_HOME);
        }
        tabBarPanel.revalidate();
        tabBarPanel.repaint();
    }

    private void initCards() {
        cardsPanel.setOpaque(true);
        cardsPanel.setBackground(ThemeManager.getSurface());
        cardsPanel.setPreferredSize(new Dimension(1200, 116));

        cardsPanel.add(wrapRibbonScroll(createPanelHome()), TAB_HOME);
        cardsPanel.add(wrapRibbonScroll(createPanelInsert()), TAB_INSERT);
        cardsPanel.add(wrapRibbonScroll(createPanelAiStudio()), TAB_AI_STUDIO);
        cardsPanel.add(wrapRibbonScroll(createPanelChart()), TAB_CHART);
        cardsPanel.add(wrapRibbonScroll(createPanelLayout()), TAB_LAYOUT);
        cardsPanel.add(wrapRibbonScroll(createPanelReview()), TAB_REVIEW);
        cardsPanel.add(wrapRibbonScroll(createPanelFileExport()), TAB_FILE);

        // Contextual Tool Panels
        cardsPanel.add(wrapRibbonScroll(createPanelTableTools()), TAB_TABLE_TOOLS);
        cardsPanel.add(wrapRibbonScroll(createPanelPictureFormat()), TAB_PICTURE_FORMAT);
        cardsPanel.add(wrapRibbonScroll(createPanelVideoTools()), TAB_VIDEO_TOOLS);
        cardsPanel.add(wrapRibbonScroll(createPanelChartDesign()), TAB_CHART_DESIGN);
        cardsPanel.add(wrapRibbonScroll(createPanelHeaderFooter()), TAB_HEADER_FOOTER);
        cardsPanel.add(wrapRibbonScroll(createPanelCardShapeFormat()), TAB_CARD_SHAPE_FORMAT);

        setSelectedChartDesignStyle("MODERN_GLASS");
        setSelectedChartTheme(ChartEngineService.ChartTheme.CORPORATE_BLUE.getDisplayName());
        setSelectedChartType(ChartType.COLUMN);
    }

    // =========================================================================
    // TAB 1: HOME (Typography, Formatting, Quick Styles Gallery, Paragraph)
    // =========================================================================
    // =========================================================================
    // TAB 1: HOME (Word 365 Layout: Clipboard, History, Font, Paragraph, Editing)
    // =========================================================================
    private JPanel createPanelHome() {
        JPanel ribbon = createRibbonContainer();

        // 1. Word-Style Clipboard Group (Hero Paste + Cut, Copy, Format Painter)
        JPanel grpClipboard = createSingleRowGroup(ribbon, "CLIPBOARD");
        JButton pasteHeroBtn = createBigRibbonButton("Paste", VectorIcons.Type.PASTE, "Paste from Clipboard (Ctrl+V)", e -> target.execFormat("paste", null));
        grpClipboard.add(pasteHeroBtn);

        JPanel clipActions = new JPanel(new GridLayout(3, 1, 1, 2));
        clipActions.setOpaque(false);
        JButton cutBtn = createCompactIconButton(VectorIcons.Type.CUT, "Cut Selection (Ctrl+X)", e -> target.execFormat("cut", null));
        JButton copyBtn = createCompactIconButton(VectorIcons.Type.COPY, "Copy Selection (Ctrl+C)", e -> target.execFormat("copy", null));
        JButton paintBtn = createCompactIconButton(VectorIcons.Type.FORMAT_PAINTER, "Format Painter", e -> {});
        clipActions.add(cutBtn);
        clipActions.add(copyBtn);
        clipActions.add(paintBtn);
        grpClipboard.add(clipActions);

        // 2. Font & Formatting (2-Tier Word Studio Layout)
        JPanel[] grpFontRows = createTwoRowGroup(ribbon, "FONT & FORMATTING");

        // Row 1: Font Family, Size (Default 14!), Size Step Buttons, Quick Styles
        ModernComboBox<String> fontCombo = new ModernComboBox<>(new String[]{
                "Calibri", "Plus Jakarta Sans", "Arial", "Segoe UI", "Georgia", "Times New Roman", "Courier New", "Monospaced"
        });
        fontCombo.setPreferredSize(new Dimension(120, 28));
        fontCombo.addActionListener(e -> target.execFormat("font-family", fontCombo.getSelectedItem()));
        grpFontRows[0].add(fontCombo);

        ModernComboBox<Integer> sizeCombo = new ModernComboBox<>(new Integer[]{8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 72});
        sizeCombo.setSelectedItem(14);
        sizeCombo.setPreferredSize(new Dimension(62, 28));
        sizeCombo.addActionListener(e -> target.execFormat("font-size", sizeCombo.getSelectedItem()));
        grpFontRows[0].add(sizeCombo);

        grpFontRows[0].add(createCompactIconButton(VectorIcons.Type.FONT_SIZE_UP, "Increase Font Size", e -> target.execFormat("font-size-up", null)));
        grpFontRows[0].add(createCompactIconButton(VectorIcons.Type.FONT_SIZE_DOWN, "Decrease Font Size", e -> target.execFormat("font-size-down", null)));

        ModernComboBox<String> styleCombo = new ModernComboBox<>(new String[]{
                "Normal", "Heading 1", "Heading 2", "Heading 3", "Title", "Quote"
        });
        styleCombo.setPreferredSize(new Dimension(100, 28));
        styleCombo.addActionListener(e -> target.execFormat("heading", styleCombo.getSelectedItem()));
        grpFontRows[0].add(styleCombo);

        // Row 2: In-line Styles (B, I, U, S, Sub, Super, Clear, Color Swatches)
        grpFontRows[1].add(createCompactIconButton(VectorIcons.Type.BOLD, "Bold (Ctrl+B)", e -> target.execFormat("bold", null)));
        grpFontRows[1].add(createCompactIconButton(VectorIcons.Type.ITALIC, "Italic (Ctrl+I)", e -> target.execFormat("italic", null)));
        grpFontRows[1].add(createCompactIconButton(VectorIcons.Type.UNDERLINE, "Underline (Ctrl+U)", e -> target.execFormat("underline", null)));
        grpFontRows[1].add(createCompactIconButton(VectorIcons.Type.STRIKETHROUGH, "Strikethrough", e -> target.execFormat("strikethrough", null)));
        grpFontRows[1].add(createCompactIconButton(VectorIcons.Type.SUBSCRIPT, "Subscript", e -> target.execFormat("subscript", null)));
        grpFontRows[1].add(createCompactIconButton(VectorIcons.Type.SUPERSCRIPT, "Superscript", e -> target.execFormat("superscript", null)));
        grpFontRows[1].add(createCompactIconButton(VectorIcons.Type.CLEAR_FORMAT, "Clear Formatting", e -> target.execFormat("clear", null)));

        // Color Pickers with live swatches
        JButton textColorBtn = createColorButtonWithSwatch("Font Color", activeTextColor, c -> {
            activeTextColor = c;
            updateColorSwatches();
            String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            target.execFormat("forecolor", hex);
        }, true);
        grpFontRows[1].add(textColorBtn);

        JButton hiliteColorBtn = createColorButtonWithSwatch("Highlight", activeHighlightColor, c -> {
            activeHighlightColor = c;
            updateColorSwatches();
            String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            target.execFormat("hilitecolor", hex);
        }, false);
        grpFontRows[1].add(hiliteColorBtn);

        // 4. Paragraph & Lists (2-Tier Compact Layout)
        JPanel[] grpParaRows = createTwoRowGroup(ribbon, "PARAGRAPH & LISTS");

        grpParaRows[0].add(createCompactIconButton(VectorIcons.Type.ALIGN_LEFT, "Align Left", e -> target.execFormat("align-left", null)));
        grpParaRows[0].add(createCompactIconButton(VectorIcons.Type.ALIGN_CENTER, "Align Center", e -> target.execFormat("align-center", null)));
        grpParaRows[0].add(createCompactIconButton(VectorIcons.Type.ALIGN_RIGHT, "Align Right", e -> target.execFormat("align-right", null)));
        grpParaRows[0].add(createCompactIconButton(VectorIcons.Type.ALIGN_JUSTIFY, "Justify", e -> target.execFormat("align-justify", null)));

        ModernComboBox<String> lineSpacingCombo = new ModernComboBox<>(new String[]{"1.0x", "1.15x", "1.5x", "2.0x"});
        lineSpacingCombo.setPreferredSize(new Dimension(68, 28));
        lineSpacingCombo.addActionListener(e -> {
            String val = (String) lineSpacingCombo.getSelectedItem();
            float factor = switch (val != null ? val : "1.0x") {
                case "1.15x" -> 0.15f;
                case "1.5x" -> 0.5f;
                case "2.0x" -> 1.0f;
                default -> 0.0f;
            };
            target.execFormat("line-spacing", factor);
        });
        grpParaRows[0].add(lineSpacingCombo);

        grpParaRows[1].add(createCompactIconButton(VectorIcons.Type.LIST_BULLET, "Bullet List", e -> target.execFormat("bullet-list", null)));
        grpParaRows[1].add(createCompactIconButton(VectorIcons.Type.LIST_NUMBERED, "Numbered List", e -> target.execFormat("numbered-list", null)));
        grpParaRows[1].add(createCompactIconButton(VectorIcons.Type.CHECKBOX, "Task Checklist", e -> target.execFormat("checkbox", null)));
        grpParaRows[1].add(createCompactIconButton(VectorIcons.Type.INDENT_DEC, "Decrease Indent", e -> target.execFormat("outdent", null)));
        grpParaRows[1].add(createCompactIconButton(VectorIcons.Type.INDENT_INC, "Increase Indent", e -> target.execFormat("indent", null)));

        // 5. Word-Style Text Styles Gallery (Normal, Heading 1, 2, 3, Title, Subtitle, Quote)
        JPanel grpStyles = createSingleRowGroup(ribbon, "STYLES GALLERY");
        addTextStyleCards(grpStyles);

        // 6. Word-Style Editing & Search Group
        JPanel[] grpEditRows = createTwoRowGroup(ribbon, "EDITING & SEARCH");
        JButton findBtn = createTextWithIconButton("Find...", VectorIcons.Type.FIND, "Find text in document (Ctrl+F)", e -> target.showFindDialog());
        findBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        grpEditRows[0].add(findBtn);

        JButton replaceBtn = createTextWithIconButton("Replace...", VectorIcons.Type.REPLACE, "Find & replace text (Ctrl+H)", e -> target.showReplaceDialog());
        replaceBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        grpEditRows[0].add(replaceBtn);

        JButton wordCountBtn = createTextWithIconButton("Word Count", VectorIcons.Type.WORD_COUNT, "View document statistics & readability", e -> target.showWordCountDialog());
        wordCountBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        grpEditRows[1].add(wordCountBtn);

        JButton proofBtn = createTextWithIconButton("Proofread", VectorIcons.Type.CHECKMARK, "Open Proofreading & Grammar Check", e -> target.showGrammarDialog());
        proofBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        proofBtn.setForeground(new Color(22, 163, 74));
        grpEditRows[1].add(proofBtn);

        JButton autoStructBtn = createCompactIconButton(VectorIcons.Type.REFRESH, "Auto-detect structure & markdown elements", e -> target.autoDetectStructure());
        grpEditRows[1].add(autoStructBtn);

        return ribbon;
    }

    // =========================================================================
    // TAB: AI & WEB STUDIO (Embedded Live Browsers & AI Acceleration)
    // =========================================================================
    private JPanel createPanelAiStudio() {
        JPanel ribbon = createRibbonContainer();

        // 1. Live Embedded AI Models (Real vector brand icons & live browsers)
        JPanel grpModels = createSingleRowGroup(ribbon, "LIVE EMBEDDED AI MODELS");
        grpModels.add(createAiTileButton("Claude 3.7", "Anthropic AI", VectorIcons.Type.CLAUDE, new Color(204, 86, 54), 0));
        grpModels.add(createAiTileButton("ChatGPT 4o", "OpenAI Studio", VectorIcons.Type.CHATGPT, new Color(16, 163, 127), 1));
        grpModels.add(createAiTileButton("Gemini 2.5", "Google AI", VectorIcons.Type.GEMINI, new Color(37, 99, 235), 2));
        grpModels.add(createAiTileButton("DeepSeek R1", "Reasoning AI", VectorIcons.Type.DEEPSEEK, new Color(14, 165, 233), 3));
        grpModels.add(createAiTileButton("Perplexity", "Deep Research", VectorIcons.Type.PERPLEXITY, new Color(13, 148, 136), 4));
        grpModels.add(createAiTileButton("Web Scraper", "Live Extraction", VectorIcons.Type.SCRAPER, new Color(124, 58, 237), 5));

        // 2. Document Prompt Accelerators (1-Click Prompts)
        JPanel[] grpPrompts = createTwoRowGroup(ribbon, "INTELLIGENT PROMPTS");
        grpPrompts[0].add(createTextWithIconButton("Summarize", VectorIcons.Type.DOCUMENT, "Copy prompt to summarize document", e -> {
            target.toggleAiPanel();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection("Please summarize the following document into key insights:\n\n"), null);
        }));
        grpPrompts[0].add(createTextWithIconButton("Explain Deeply", VectorIcons.Type.REFRESH, "Copy prompt to explain core concepts in depth", e -> {
            target.toggleAiPanel();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection("Please explain the core concepts in this text in depth with examples:\n\n"), null);
        }));
        grpPrompts[1].add(createTextWithIconButton("Polish Prose", VectorIcons.Type.EDIT, "Copy prompt to improve tone and flow", e -> {
            target.toggleAiPanel();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection("Please polish and improve the readability of this text:\n\n"), null);
        }));
        grpPrompts[1].add(createTextWithIconButton("Extract Table", VectorIcons.Type.TABLE, "Copy prompt to structure text into table", e -> {
            target.toggleAiPanel();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection("Please convert the relevant data in this text into a clean HTML table:\n\n"), null);
        }));

        // 3. Side Panel & Web View Controls
        JPanel grpControls = createSingleRowGroup(ribbon, "PANEL & BROWSER");
        JButton togglePanelBtn = createTextWithIconButton("Toggle Side Panel", VectorIcons.Type.SETTINGS, "Show/Hide Embedded AI Studio Browser", e -> target.toggleAiPanel());
        togglePanelBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        togglePanelBtn.setForeground(new Color(99, 102, 241));
        grpControls.add(togglePanelBtn);

        JButton scrapeUrlBtn = createTextWithIconButton("Scrape Web URL...", VectorIcons.Type.SCRAPER, "Extract and summarize live website content", e -> {
            String url = ModernDialog.showInput(this, "Web Scraper Studio", "Enter Web URL to Scrape:", "https://");
            if (url != null && !url.trim().isEmpty()) {
                target.openScraperWithUrl(url.trim());
            }
        });
        grpControls.add(scrapeUrlBtn);

        return ribbon;
    }

    // =========================================================================
    // TAB 2: INSERT (Pages, Tables, Media, Charts, Cards, Citations, Symbols)
    // =========================================================================
    private JPanel createPanelInsert() {
        JPanel ribbon = createRibbonContainer();

        // 1. Pages & Breaks Group
        JPanel grpPages = createSingleRowGroup(ribbon, "PAGES & BREAKS");
        JButton pageBreakBtn = createBigRibbonButton("Page Break", VectorIcons.Type.PAGE_BREAK, "Insert interactive page break at cursor", e -> target.insertPageBreak());
        pageBreakBtn.setForeground(new Color(124, 58, 237));
        grpPages.add(pageBreakBtn);

        JButton headerFooterBtn = createBigRibbonButton("Header & Footer", VectorIcons.Type.DOCUMENT, "Design Page Headers and Footers", e -> {
            target.setHeaderFooterSelected(true, "HEADER", 0);
            showContextualTab(TAB_HEADER_FOOTER);
        });
        headerFooterBtn.setForeground(new Color(13, 148, 136));
        grpPages.add(headerFooterBtn);

        JPanel pageSub = new JPanel(new GridLayout(2, 1, 2, 2));
        pageSub.setOpaque(false);
        JButton dividerBtn = createVisualRibbonButton("― Divider", VectorIcons.Type.DIVIDER, new Color(100, 116, 139), "Insert Horizontal Rule Divider", e -> target.insertHtml("<hr style='border:none;border-top:1px solid #cbd5e1;margin:18px 0;'><p><br></p>"));
        JButton blankGapBtn = createVisualRibbonButton("Blank Gap", VectorIcons.Type.LINE, new Color(100, 116, 139), "Insert vertical spacing gap", e -> target.insertHtml("<p style='margin:24px 0;'><br></p>"));
        pageSub.add(dividerBtn);
        pageSub.add(blankGapBtn);
        grpPages.add(pageSub);

        // 2. Tables Group (Table Builder + Quick Grids + Contextual Switch)
        JPanel[] grpTables = createTwoRowGroup(ribbon, "TABLES");
        JButton tableBuilderBtn = createVisualRibbonButton("Table Builder...", VectorIcons.Type.TABLE, new Color(37, 99, 235), "Open interactive Table Builder Dialog", e -> target.showTableDialog());
        tableBuilderBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        grpTables[0].add(tableBuilderBtn);
        grpTables[0].add(createVisualRibbonButton("2×2 Grid", VectorIcons.Type.GRID_2X2, new Color(37, 99, 235), "Quick 2x2 Table", e -> insertQuickGrid(2, 2)));
        grpTables[0].add(createVisualRibbonButton("3×3 Grid", VectorIcons.Type.GRID_3X3, new Color(37, 99, 235), "Quick 3x3 Table", e -> insertQuickGrid(3, 3)));

        grpTables[1].add(createVisualRibbonButton("4×4 Grid", VectorIcons.Type.TABLE, new Color(37, 99, 235), "Quick 4x4 Table", e -> insertQuickGrid(4, 4)));
        grpTables[1].add(createVisualRibbonButton("5×5 Grid", VectorIcons.Type.TABLE, new Color(37, 99, 235), "Quick 5x5 Table", e -> insertQuickGrid(5, 5)));
        grpTables[1].add(createVisualRibbonButton("Table Tools", VectorIcons.Type.SETTINGS, new Color(217, 119, 6), "Open Table contextual design tools", e -> showContextualTab(TAB_TABLE_TOOLS)));

        // 3. Illustrations & Media Group (Visual Representation Preview Cards)
        JPanel grpMedia = createSingleRowGroup(ribbon, "ILLUSTRATIONS & MEDIA");
        record MediaOption(String code, String label, String tooltip, Runnable action) {}
        MediaOption[] mediaOpts = new MediaOption[]{
                new MediaOption("LOCAL_PHOTO", "Local Photo", "Insert image file from your computer", target::insertLocalImage),
                new MediaOption("WEB_IMAGE", "Web Image", "Insert image from web URL", target::insertWebImage),
                new MediaOption("CHART_STUDIO", "Chart Studio", "Switch to interactive Chart Studio tab", () -> selectTab(TAB_CHART)),
                new MediaOption("VIDEO_MEDIA", "Video Media", "Embed Web or Local Video Media Card", target::showVideoDialog),
                new MediaOption("WEB_SCRAPER", "Web Scraper", "Extract and summarize live website content", () -> target.openScraperWithUrl("")),
                new MediaOption("LAYOUT_STUDIO", "Layout Studio", "Image Layout & Sizing Studio", target::showImagePropertiesDialog)
        };
        for (MediaOption opt : mediaOpts) {
            ThumbCard card = ThumbCard.ofMedia(opt.code, opt.label, opt.tooltip, 68, 40);
            card.setOnClick(opt.action);
            grpMedia.add(card);
        }

        // 4. Shapes & Diagram Flows (Visual Representation Miniature Cards)
        JPanel grpShapes = createSingleRowGroup(ribbon, "SHAPES & FLOW");
        record ShapeOption(String code, String label, String tooltip, String shapeType) {}
        ShapeOption[] shapeOpts = new ShapeOption[]{
                new ShapeOption("CARD_BOX", "Card Box", "Rounded Card Box with Header", "Rounded Card"),
                new ShapeOption("PROCESS_FLOW", "Process Flow", "Multi-Step Process Flow (Step 1 ➔ 2 ➔ 3)", "Process Flow (Step A ➔ B ➔ C)"),
                new ShapeOption("DECISION_NODE", "Decision", "Decision Diamond Node", "Decision Diamond"),
                new ShapeOption("TERMINAL_PILL", "Terminal", "Start / Terminal Pill", "Start / Terminal Pill"),
                new ShapeOption("MILESTONE_BADGE", "Milestone", "Checkmark Milestone Badge (✓)", "Checkmark Badge (✓)"),
                new ShapeOption("STAR_RATING", "5-Star", "Star Rating Badge (★★★★★)", "Star Rating (★★★★★)")
        };
        for (ShapeOption opt : shapeOpts) {
            ThumbCard card = ThumbCard.ofShape(opt.code, opt.label, opt.tooltip, 68, 40);
            card.setOnClick(() -> target.insertShape(opt.shapeType));
            grpShapes.add(card);
        }

        // 5. Visual Cards & Callouts (Visual Realistic Miniature Cards)
        JPanel grpCards = createSingleRowGroup(ribbon, "CARDS & CALLOUTS");
        record CardInsertOption(String code, String label, String tooltip, String cardType) {}
        CardInsertOption[] cardOpts = new CardInsertOption[]{
                new CardInsertOption("KPI", "Single KPI", "Key Performance Metric Card with Trend", "KPI"),
                new CardInsertOption("MULTI_KPI", "Multi-KPI", "3-Column Executive Metric Matrix", "MultiKPI"),
                new CardInsertOption("SPOTLIGHT", "Spotlight", "Topic Spotlight Feature Card", "Spotlight"),
                new CardInsertOption("COMPARISON", "Comparison", "Side-by-Side Pros vs Cons Matrix", "Comparison"),
                new CardInsertOption("PROGRESS", "Progress Bar", "Milestone Progress Bar with %", "Progress"),
                new CardInsertOption("TAKEAWAYS", "Takeaways", "Executive Key Takeaways & Summary Box", "Takeaways")
        };
        for (CardInsertOption opt : cardOpts) {
            ThumbCard card = ThumbCard.ofCardTemplate(opt.code, opt.label, opt.tooltip, 68, 40);
            card.setOnClick(() -> target.insertCard(opt.cardType));
            grpCards.add(card);
        }

        // 6. Annotation & Symbols (Visual Representation Cards)
        JPanel grpRef = createSingleRowGroup(ribbon, "REFERENCES & SYMBOLS");
        ThumbCard citeCard = ThumbCard.ofReference("CITATIONS", "Citations", "Manage Academic Citations & Bibliography", 64, 40);
        citeCard.setOnClick(target::showCitationDialog);
        grpRef.add(citeCard);

        ThumbCard dateCard = ThumbCard.ofReference("DATE_STAMP", "Date Stamp", "Insert Current Date", 64, 40);
        dateCard.setOnClick(() -> {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            target.insertText(dateStr);
        });
        grpRef.add(dateCard);

        ThumbCard mathCard = ThumbCard.ofReference("MATH_SYMBOLS", "Math Symbols", "Insert Math Notation (∑, √, π, ∞...)", 68, 40);
        mathCard.setOnClick(() -> showMathSymbolsPopup(mathCard));
        grpRef.add(mathCard);

        ThumbCard iconCard = ThumbCard.ofReference("ICON_SYMBOLS", "Icon Badges", "Insert Common Badges & Symbols (✓, ★, ➔, ±...)", 68, 40);
        iconCard.setOnClick(() -> showIconSymbolsPopup(iconCard));
        grpRef.add(iconCard);

        return ribbon;
    }

    private void showMathSymbolsPopup(Component invoker) {
        ModernPopupMenu menu = new ModernPopupMenu();
        menu.addHeader("Math Notation");
        menu.addModernSeparator();
        String[] syms = {"∑ Summation", "√ Square Root", "π Pi", "∞ Infinity", "≈ Approximately", "≠ Not Equal", "≤ Less Than or Equal", "≥ Greater Than or Equal", "÷ Divide", "× Multiply", "∆ Delta", "∫ Integral"};
        String[] chars = {"∑", "√", "π", "∞", "≈", "≠", "≤", "≥", "÷", "×", "∆", "∫"};
        for (int i = 0; i < syms.length; i++) {
            final String c = chars[i];
            menu.addModernItem(syms[i], null, e -> target.insertText(c + " "));
        }
        menu.show(invoker, 0, invoker.getHeight());
    }

    private void showIconSymbolsPopup(Component invoker) {
        ModernPopupMenu menu = new ModernPopupMenu();
        menu.addHeader("Badges & Icons");
        menu.addModernSeparator();
        String[] names = {"✓ Checkmark", "★ 5-Star", "➔ Arrow Right", "± Plus-Minus", "⚡ Lightning Bolt", "📌 Pushpin", "⚠ Warning", "🔒 Security Lock", "■ Square Bullet", "● Circle Bullet"};
        String[] chars = {"✓", "★", "➔", "±", "⚡", "📌", "⚠", "🔒", "■", "●"};
        for (int i = 0; i < names.length; i++) {
            final String c = chars[i];
            menu.addModernItem(names[i], null, e -> target.insertText(c + " "));
        }
        menu.show(invoker, 0, invoker.getHeight());
    }

    // =========================================================================
    // CONTEXTUAL TAB: CARD & SHAPE FORMAT
    // =========================================================================
    private JPanel createPanelCardShapeFormat() {
        JPanel ribbon = createRibbonContainer();

        // 1. Text & Values Customization
        JPanel grpEdit = createSingleRowGroup(ribbon, "VALUES & TEXT");
        JButton formatPaneBtn = createHeroStudioButton("Format Pane", "Side Panel", VectorIcons.Type.SETTINGS, new Color(14, 165, 233), "Open Word-style Format Shape sidebar panel on the right side", e -> target.toggleShapeFormatPanel());
        grpEdit.add(formatPaneBtn);
        JButton customBtn = createHeroStudioButton("Customize", "Values & Text", VectorIcons.Type.EDIT, new Color(14, 165, 233), "Edit title, metric value, subtitle & theme in live dialog", e -> target.showCardShapeCustomizerDialog());
        grpEdit.add(customBtn);

        // 2. Color Themes & Palettes (Clickable palette cards)
        JPanel grpThemes = createSingleRowGroup(ribbon, "COLOR THEMES");
        for (Map.Entry<String, CardShapeCustomizerDialog.ThemePalette> entry : CardShapeCustomizerDialog.PALETTES.entrySet()) {
            String key = entry.getKey();
            CardShapeCustomizerDialog.ThemePalette pal = entry.getValue();
            ThumbCard palCard = new ThumbCard(pal.name(), "Switch to " + pal.name() + " palette", (g2, r) -> {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pal.bg());
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
                g2.setColor(pal.border());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
                g2.setColor(pal.primary());
                g2.fillRoundRect(r.x + 4, r.y + 4, 4, r.height - 8, 2, 2);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.drawString("Aa 123", r.x + 12, r.y + 22);
            }, 64, 40);
            palCard.setOnClick(() -> target.applyActiveCardShapeTheme(key));
            grpThemes.add(palCard);
        }

        // 3. Alignment & Delete
        JPanel[] grpLayout = createTwoRowGroup(ribbon, "LAYOUT & ACTIONS");
        grpLayout[0].add(createVisualRibbonButton("Align Left", VectorIcons.Type.ALIGN_LEFT, new Color(71, 85, 105), "Align card left", e -> target.alignSelectedCardShape("left")));
        grpLayout[0].add(createVisualRibbonButton("Align Center", VectorIcons.Type.ALIGN_CENTER, new Color(71, 85, 105), "Align card center", e -> target.alignSelectedCardShape("center")));
        grpLayout[0].add(createVisualRibbonButton("Align Right", VectorIcons.Type.ALIGN_RIGHT, new Color(71, 85, 105), "Align card right", e -> target.alignSelectedCardShape("right")));

        grpLayout[1].add(createVisualRibbonButton("Edit Dialog...", VectorIcons.Type.SETTINGS, new Color(14, 165, 233), "Open Full Customizer", e -> target.showCardShapeCustomizerDialog()));
        grpLayout[1].add(createVisualRibbonButton("Delete Object", VectorIcons.Type.DELETE, new Color(225, 29, 72), "Remove card or shape from document", e -> target.deleteSelectedCardShape()));

        return ribbon;
    }

    // =========================================================================
    // CONTEXTUAL TAB 1: TABLE TOOLS (Design & Layout Inspired by Microsoft Word)
    // =========================================================================
    private JPanel createPanelTableTools() {
        JPanel ribbon = createRibbonContainer();

        // 1. Table Quick Grid Builder
        JPanel grpInsert = createSingleRowGroup(ribbon, "TABLE BUILDER");
        ModernComboBox<String> quickInsertCombo = new ModernComboBox<>(new String[]{
                "Insert Grid...",
                "2 × 2 Grid",
                "3 × 3 Grid",
                "4 × 4 Grid",
                "5 × 5 Grid",
                "Custom Dialog..."
        });
        quickInsertCombo.setPreferredSize(new Dimension(135, 28));
        quickInsertCombo.addActionListener(e -> {
            if (quickInsertCombo.getSelectedIndex() <= 0) return;
            String choice = (String) quickInsertCombo.getSelectedItem();
            quickInsertCombo.setSelectedIndex(0);
            switch (choice) {
                case "2 × 2 Grid" -> insertQuickGrid(2, 2);
                case "3 × 3 Grid" -> insertQuickGrid(3, 3);
                case "4 × 4 Grid" -> insertQuickGrid(4, 4);
                case "5 × 5 Grid" -> insertQuickGrid(5, 5);
                case "Custom Dialog..." -> target.showTableDialog();
            }
        });
        grpInsert.add(quickInsertCombo);
        grpInsert.add(createCompactIconButtonWithText("2×2", "Quick 2x2 Table", e -> insertQuickGrid(2, 2)));
        grpInsert.add(createCompactIconButtonWithText("3×3", "Quick 3x3 Table", e -> insertQuickGrid(3, 3)));
        grpInsert.add(createCompactIconButtonWithText("4×4", "Quick 4x4 Table", e -> insertQuickGrid(4, 4)));

        // 2. Table Design Gallery (Word 365 1-Click Themes)
        JPanel grpStyles = createSingleRowGroup(ribbon, "TABLE DESIGN STYLES");
        ModernComboBox<String> styleCombo = new ModernComboBox<>(new String[]{
                "Choose Style...",
                "Corporate Blue",
                "Modern Slate",
                "Emerald Green",
                "Royal Violet",
                "Dark Executive",
                "Amber Warm",
                "Striped Zebra",
                "Minimal Grid",
                "Coral Vibrant",
                "Indigo Modern",
                "Teal Professional"
        });
        styleCombo.setPreferredSize(new Dimension(150, 28));
        styleCombo.addActionListener(e -> {
            if (styleCombo.getSelectedIndex() <= 0) return;
            String choice = (String) styleCombo.getSelectedItem();
            styleCombo.setSelectedIndex(0);
            target.applyTableStyle(choice);
        });
        grpStyles.add(styleCombo);
        grpStyles.add(createCompactIconButtonWithText("Blue", "Apply Corporate Blue Theme", e -> target.applyTableStyle("Corporate Blue")));
        grpStyles.add(createCompactIconButtonWithText("Slate", "Apply Modern Slate Theme", e -> target.applyTableStyle("Modern Slate")));
        grpStyles.add(createCompactIconButtonWithText("Emerald", "Apply Emerald Green Theme", e -> target.applyTableStyle("Emerald Green")));
        grpStyles.add(createCompactIconButtonWithText("Zebra", "Apply Striped Zebra Theme", e -> target.applyTableStyle("Striped Zebra")));

        // 3. Rows & Columns
        JPanel grpRowsCols = createSingleRowGroup(ribbon, "ROWS & COLUMNS");
        grpRowsCols.add(createIconButton(VectorIcons.Type.ROW_ABOVE, "Insert Row Above", e -> target.insertTableRowAbove()));
        grpRowsCols.add(createIconButton(VectorIcons.Type.ROW_BELOW, "Insert Row Below", e -> target.insertTableRowBelow()));
        grpRowsCols.add(createIconButton(VectorIcons.Type.COL_LEFT, "Insert Column Left", e -> target.insertTableColumnLeft()));
        grpRowsCols.add(createIconButton(VectorIcons.Type.COL_RIGHT, "Insert Column Right", e -> target.insertTableColumnRight()));

        // 4. Delete Elements
        JPanel grpDelete = createSingleRowGroup(ribbon, "DELETE");
        grpDelete.add(createIconButton(VectorIcons.Type.DELETE_ROW, "Delete Current Row", e -> target.deleteTableRow()));
        grpDelete.add(createIconButton(VectorIcons.Type.DELETE_COL, "Delete Current Column", e -> target.deleteTableColumn()));
        grpDelete.add(createIconButton(VectorIcons.Type.DELETE_TABLE, "Delete Entire Table", e -> target.deleteTable()));

        // 5. Cell Shading & Preset Colors
        JPanel grpCell = createSingleRowGroup(ribbon, "CELL SHADING & ALIGN");
        JButton colorBtn = createTextWithIconButton("Shading...", VectorIcons.Type.PALETTE, "Custom Cell Background Color", e -> {
            Color chosen = ModernColorPickerDialog.showDialog(this, "Table Cell Shading", new Color(239, 246, 255));
            if (chosen != null) target.setTableCellColor(chosen);
        });
        grpCell.add(colorBtn);

        ModernComboBox<String> tintCombo = new ModernComboBox<>(new String[]{
                "Preset Tints...",
                "Sky Blue Tint",
                "Soft Mint Tint",
                "Pale Amber Tint",
                "Rose Blush Tint",
                "Pure White",
                "Soft Gray Tint"
        });
        tintCombo.setPreferredSize(new Dimension(130, 28));
        tintCombo.addActionListener(e -> {
            if (tintCombo.getSelectedIndex() <= 0) return;
            String choice = (String) tintCombo.getSelectedItem();
            tintCombo.setSelectedIndex(0);
            Color c = switch (choice) {
                case "Sky Blue Tint" -> new Color(239, 246, 255);
                case "Soft Mint Tint" -> new Color(240, 253, 244);
                case "Pale Amber Tint" -> new Color(254, 252, 232);
                case "Rose Blush Tint" -> new Color(255, 241, 242);
                case "Soft Gray Tint" -> new Color(248, 250, 252);
                default -> Color.WHITE;
            };
            target.setTableCellColor(c);
        });
        grpCell.add(tintCombo);

        grpCell.add(createIconButton(VectorIcons.Type.ALIGN_LEFT, "Align Left", e -> target.execFormat("align-left", null)));
        grpCell.add(createIconButton(VectorIcons.Type.ALIGN_CENTER, "Align Center", e -> target.execFormat("align-center", null)));
        grpCell.add(createIconButton(VectorIcons.Type.ALIGN_RIGHT, "Align Right", e -> target.execFormat("align-right", null)));

        // 6. Dismiss / Done
        JPanel grpDone = createSingleRowGroup(ribbon, "CLOSE");
        JButton doneBtn = createTextWithIconButton("Done", VectorIcons.Type.CLOSE, "Close Table Tools", e -> hideContextualTabs());
        doneBtn.setForeground(new Color(217, 119, 6));
        grpDone.add(doneBtn);

        return ribbon;
    }

    // =========================================================================
    // CONTEXTUAL TAB 2: PICTURE FORMAT (Word-style Image Formatting)
    // =========================================================================
    private JPanel createPanelPictureFormat() {
        JPanel ribbon = createRibbonContainer();

        // 1. Image Sizing
        JPanel grpSize = createSingleRowGroup(ribbon, "PICTURE SIZE");
        grpSize.add(createCompactIconButtonWithText("240px", "Small Thumbnail (240px)", e -> target.resizeSelectedImage(240)));
        grpSize.add(createCompactIconButtonWithText("420px", "Medium Photo (420px)", e -> target.resizeSelectedImage(420)));
        grpSize.add(createCompactIconButtonWithText("580px", "Standard Width (580px)", e -> target.resizeSelectedImage(580)));
        grpSize.add(createCompactIconButtonWithText("720px", "Full Page Width (720px)", e -> target.resizeSelectedImage(720)));
        JButton studioBtn = createTextWithIconButton("Studio...", VectorIcons.Type.SETTINGS, "Advanced Image Sizing & Layout Studio", e -> target.showImagePropertiesDialog());
        grpSize.add(studioBtn);

        // 2. Alignment
        JPanel grpAlign = createSingleRowGroup(ribbon, "ALIGNMENT");
        grpAlign.add(createIconButton(VectorIcons.Type.ALIGN_LEFT, "Align Left", e -> target.alignSelectedImage("left")));
        grpAlign.add(createIconButton(VectorIcons.Type.ALIGN_CENTER, "Align Center", e -> target.alignSelectedImage("center")));
        grpAlign.add(createIconButton(VectorIcons.Type.ALIGN_RIGHT, "Align Right", e -> target.alignSelectedImage("right")));

        // 3. Image Filters & Effects (Two Row Gallery)
        JPanel[] grpFilters = createTwoRowGroup(ribbon, "IMAGE FILTERS & EFFECTS");
        grpFilters[0].add(createCompactIconButtonWithText("Original", "Reset to original image colors", e -> target.applyImageFilter("Original")));
        grpFilters[0].add(createCompactIconButtonWithText("B&W Mono", "Apply Grayscale black and white filter", e -> target.applyImageFilter("Grayscale")));
        grpFilters[0].add(createCompactIconButtonWithText("Sepia", "Apply warm vintage Sepia filter", e -> target.applyImageFilter("Sepia")));
        grpFilters[0].add(createCompactIconButtonWithText("Vivid", "Apply rich saturated Vivid color filter", e -> target.applyImageFilter("Vivid")));
        grpFilters[0].add(createCompactIconButtonWithText("Contrast", "Apply High Dynamic Contrast filter", e -> target.applyImageFilter("High Contrast")));
        grpFilters[0].add(createCompactIconButtonWithText("Invert", "Invert image colors", e -> target.applyImageFilter("Invert")));

        grpFilters[1].add(createCompactIconButtonWithText("Rounded", "Large 24px soft rounded corners", e -> target.applyImageFilter("Large Rounded")));
        grpFilters[1].add(createCompactIconButtonWithText("Oval", "Circular / Oval badge shape", e -> target.applyImageFilter("Oval")));
        grpFilters[1].add(createCompactIconButtonWithText("Shadow", "Elevated floating drop shadow", e -> target.applyImageFilter("Floating Shadow")));
        grpFilters[1].add(createCompactIconButtonWithText("Border", "Clean modern accent frame border", e -> target.applyImageFilter("Border")));
        grpFilters[1].add(createCompactIconButtonWithText("Vignette", "Soft dramatic vignette focus", e -> target.applyImageFilter("Vignette")));
        grpFilters[1].add(createCompactIconButtonWithText("Blur", "Subtle artistic background blur", e -> target.applyImageFilter("Blur")));

        // 4. Text Wrap
        JPanel grpWrap = createSingleRowGroup(ribbon, "TEXT WRAP");
        grpWrap.add(createCompactIconButtonWithText("Float ⮜", "Float Left (Text wraps around)", e -> target.setImageWrap("float-left")));
        grpWrap.add(createCompactIconButtonWithText("Inline", "In-Line with Text (Centered)", e -> target.setImageWrap("center")));
        grpWrap.add(createCompactIconButtonWithText("Float ⮞", "Float Right (Text wraps around)", e -> target.setImageWrap("float-right")));

        // 5. Source & Replace
        JPanel grpSrc = createSingleRowGroup(ribbon, "PICTURE SOURCE");
        JButton replaceBtn = createTextWithIconButton("Replace...", VectorIcons.Type.REFRESH, "Replace with new image file or URL", e -> target.replaceSelectedImageSource());
        grpSrc.add(replaceBtn);

        // 4. Dismiss / Done
        JPanel grpDone = createSingleRowGroup(ribbon, "CLOSE");
        JButton doneBtn = createTextWithIconButton("Done", VectorIcons.Type.CLOSE, "Close Picture Tools", e -> hideContextualTabs());
        doneBtn.setForeground(new Color(124, 58, 237));
        grpDone.add(doneBtn);

        return ribbon;
    }

    // =========================================================================
    // CONTEXTUAL TAB 3: VIDEO TOOLS (Video Launch & Media Controls)
    // =========================================================================
    private JPanel createPanelVideoTools() {
        JPanel ribbon = createRibbonContainer();

        // 1. Playback Group
        JPanel grpPlay = createSingleRowGroup(ribbon, "PLAYBACK");
        JButton playBtn = createTextWithIconButton("▶ Play Video", VectorIcons.Type.VIDEO, "Launch in Web Browser or System Player", e -> target.openSelectedVideo());
        playBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        playBtn.setForeground(new Color(225, 29, 72));
        grpPlay.add(playBtn);

        // 2. Media Settings
        JPanel grpSettings = createSingleRowGroup(ribbon, "MEDIA SETTINGS");
        JButton editBtn = createTextWithIconButton("Edit Source...", VectorIcons.Type.EDIT, "Change Video URL or Title", e -> target.showVideoDialog());
        grpSettings.add(editBtn);
        JButton delBtn = createTextWithIconButton("Delete Video", VectorIcons.Type.DELETE, "Remove video card from document", e -> target.deleteSelectedVideo());
        grpSettings.add(delBtn);

        // 3. Dismiss / Done
        JPanel grpDone = createSingleRowGroup(ribbon, "CLOSE");
        JButton doneBtn = createTextWithIconButton("Done", VectorIcons.Type.CLOSE, "Close Video Tools", e -> hideContextualTabs());
        doneBtn.setForeground(new Color(225, 29, 72));
        grpDone.add(doneBtn);

        return ribbon;
    }

    // =========================================================================
    // TAB 3: CHART STUDIO (Visual Customizer, 1-Click Gallery, Palettes, Data Cards)
    // =========================================================================
    private JPanel createPanelChart() {
        JPanel ribbon = createRibbonContainer();

        // 1. Chart Studio Hero Action & Fast Presets
        JPanel grpBuilder = createSingleRowGroup(ribbon, "CHART STUDIO");
        JButton chartStudioBtn = createHeroStudioButton("Chart Studio", "Interactive Studio", VectorIcons.Type.CHART, new Color(14, 165, 233), "Open live interactive Chart Generator Studio with live data & themes", e -> target.showChartDialog());
        grpBuilder.add(chartStudioBtn);

        JPanel fastPresets = new JPanel(new GridLayout(2, 1, 2, 2));
        fastPresets.setOpaque(false);
        JButton quickBarBtn = createVisualRibbonButton("⚡ Quick Bar", VectorIcons.Type.CHART_BAR, new Color(14, 165, 233), "Quickly insert a balanced Corporate Blue Bar Chart", e -> target.insertQuickChart("Bar", "Q1 Actual: 52\nQ2 Actual: 74\nQ3 Actual: 88\nQ4 Actual: 105", "Corporate Blue", 580, 290));
        JButton quickLineBtn = createVisualRibbonButton("📈 Quick Line", VectorIcons.Type.CHART_LINE, new Color(16, 185, 129), "Quickly insert a balanced Growth Trend Line Chart", e -> target.insertQuickChart("Line", "Jan: 24\nFeb: 38\nMar: 52\nApr: 68\nMay: 85\nJun: 102", "Emerald Mint", 580, 290));
        fastPresets.add(quickBarBtn);
        fastPresets.add(quickLineBtn);
        grpBuilder.add(fastPresets);

        // 2. Insert Chart by Type (Visual Representations of All 15 Real Chart Shapes)
        JPanel grpTypes = createSingleRowGroup(ribbon, "INSERT CHART TYPE");
        addChartTypeCards(grpTypes, false);

        // 3. Color Palettes & Themes (Visual Miniature Preview Cards)
        JPanel grpThemes = createSingleRowGroup(ribbon, "COLOR PALETTES");
        addChartThemeCards(grpThemes, false);

        // 4. Data & KPI Cards (Visual Realistic Miniature Cards)
        JPanel grpCards = createSingleRowGroup(ribbon, "DATA & KPI CARDS");
        addCardTemplateCards(grpCards);

        // 5. Pre-configured Business & Study Templates (Visual Realistic Miniature Cards)
        JPanel grpTemplates = createSingleRowGroup(ribbon, "BUSINESS TEMPLATES");
        addBusinessTemplateCards(grpTemplates);

        return ribbon;
    }

    // =========================================================================
    // CONTEXTUAL TAB 4: CHART DESIGN (Modify Data, Chart Types, Palettes, Dynamic Styles)
    // =========================================================================
    private JPanel createPanelChartDesign() {
        JPanel ribbon = createRibbonContainer();

        // 1. Data Modification & Fast Edit Group
        JPanel grpData = createSingleRowGroup(ribbon, "DATA & VALUES");
        JButton editDataBtn = createHeroStudioButton("Modify Data", "Live Values", VectorIcons.Type.EDIT, new Color(14, 165, 233), "Edit raw chart numbers, labels & title in-place", e -> target.modifySelectedChartData());
        grpData.add(editDataBtn);

        JPanel dataSub = new JPanel(new GridLayout(2, 1, 2, 2));
        dataSub.setOpaque(false);
        dataSub.add(createVisualRibbonButton("⚡ Re-render", VectorIcons.Type.REFRESH, new Color(14, 165, 233), "Re-render chart graphic", e -> target.reRenderSelectedChart()));
        dataSub.add(createVisualRibbonButton("Studio...", VectorIcons.Type.SETTINGS, new Color(100, 116, 139), "Open Full Chart Dialog", e -> target.showChartDialog()));
        grpData.add(dataSub);

        // 2. Chart Card Designs & Visual Styles (Dynamically painted for the currently selected chart type)
        JPanel grpDesigns = createSingleRowGroup(ribbon, "CHART DESIGNS");
        addChartDesignCards(grpDesigns, true);

        // 3. Color Palettes (Visual Representations with Mini Bars & Palette Studio)
        JPanel grpThemes = createSingleRowGroup(ribbon, "COLOR PALETTES");
        addChartThemeCards(grpThemes, true);

        // 4. Change Chart Type (Visual Miniature Representations of Real Chart Vector Shapes)
        JPanel grpTypes = createSingleRowGroup(ribbon, "CHART TYPE");
        addChartTypeCards(grpTypes, true);

        // 5. Detail Reduction & Alignment (Resizing is handled directly on canvas via 8 corner/side drag handles)
        JPanel[] grpDetail = createTwoRowGroup(ribbon, "DETAIL & ALIGN");
        grpDetail[0].add(createVisualRibbonButton("Full Details", VectorIcons.Type.CHECKMARK, new Color(16, 185, 129), "Full title, subtitle, grid, values, labels & legend", e -> target.setChartDetailLevel("FULL")));
        grpDetail[0].add(createVisualRibbonButton("Compact", VectorIcons.Type.COLUMNS, new Color(14, 165, 233), "Streamlined layout for smaller widget cards", e -> target.setChartDetailLevel("COMPACT")));
        grpDetail[0].add(createVisualRibbonButton("Minimal KPI", VectorIcons.Type.KPI_CARD, new Color(245, 158, 11), "Minimalist KPI badge with headline stat & sparkline", e -> target.setChartDetailLevel("MINIMAL")));
        grpDetail[0].add(createVisualRibbonButton("Align Left", VectorIcons.Type.ALIGN_LEFT, new Color(71, 85, 105), "Align chart left", e -> target.alignSelectedImage("left")));

        grpDetail[1].add(createVisualRibbonButton("Toggle Values", VectorIcons.Type.EDIT, new Color(100, 116, 139), "Show / Hide numerical values above chart bars/points", e -> target.toggleChartValues()));
        grpDetail[1].add(createVisualRibbonButton("Toggle Legend", VectorIcons.Type.TABLE, new Color(100, 116, 139), "Show / Hide legend box on pie and donut charts", e -> target.toggleChartLegend()));
        grpDetail[1].add(createVisualRibbonButton("Align Center", VectorIcons.Type.ALIGN_CENTER, new Color(71, 85, 105), "Align chart center", e -> target.alignSelectedImage("center")));

        return ribbon;
    }

    // =========================================================================
    // CONTEXTUAL TAB 5: HEADER & FOOTER DESIGN
    // =========================================================================
    private JPanel createPanelHeaderFooter() {
        JPanel ribbon = createRibbonContainer();

        // 1. Global Header Designs
        JPanel grpHeader = createSingleRowGroup(ribbon, "GLOBAL HEADER DESIGNS");
        addHeaderDesignCards(grpHeader);

        // 2. Global Footer Designs
        JPanel grpFooter = createSingleRowGroup(ribbon, "GLOBAL FOOTER DESIGNS");
        addFooterDesignCards(grpFooter);

        // 3. Header Text & Options
        JPanel[] grpOpts = createTwoRowGroup(ribbon, "HEADER & FOOTER OPTIONS");
        JTextField titleField = new JTextField("Mikey Note Document", 14);
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleField.setPreferredSize(new Dimension(140, 26));
        titleField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { target.setHeaderLeftText(titleField.getText()); }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });
        titleField.addActionListener(e -> target.setHeaderLeftText(titleField.getText()));
        grpOpts[0].add(new JLabel("Title:"));
        grpOpts[0].add(titleField);
        grpOpts[0].add(createCompactIconButtonWithText("Set", "Update document title in header & footer", e -> {
            target.setHeaderLeftText(titleField.getText());
        }));

        JTextField rightField = new JTextField("Confidential", 12);
        rightField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rightField.setPreferredSize(new Dimension(110, 26));
        rightField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { target.setHeaderRightText(rightField.getText()); }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });
        rightField.addActionListener(e -> target.setHeaderRightText(rightField.getText()));
        grpOpts[1].add(new JLabel("Right:"));
        grpOpts[1].add(rightField);
        grpOpts[1].add(createCompactIconButtonWithText("Set", "Update right tagline in header", e -> {
            target.setHeaderRightText(rightField.getText());
        }));

        grpOpts[1].add(createVisualRibbonButton("✕ Close", VectorIcons.Type.CLOSE, new Color(225, 29, 72), "Close Header & Footer tools", e -> {
            target.closeHeaderFooterSelection();
            hideContextualTabs();
        }));

        return ribbon;
    }

    // =========================================================================
    // TAB 4: LAYOUT (Page Breaks, Paper Size, Margins, Spacing, Paper Tone)
    // =========================================================================
    private JPanel createPanelLayout() {
        JPanel ribbon = createRibbonContainer();

        // 1. Paper Ruling & Grid Pattern
        JPanel grpRuling = createSingleRowGroup(ribbon, "PAPER RULING PATTERN");
        ModernComboBox<String> ruleCombo = new ModernComboBox<>(new String[]{
                "Blank / Empty Page",
                "Single Rule (Notebook Lined)",
                "Square Rule (Quad Grid / Graph)"
        });
        ruleCombo.setPreferredSize(new Dimension(195, 30));
        ruleCombo.addActionListener(e -> {
            String sel = (String) ruleCombo.getSelectedItem();
            target.setPaperPattern(sel);
        });
        grpRuling.add(ruleCombo);
        grpRuling.add(createCompactIconButtonWithText("Blank", "Empty Blank Paper", e -> {
            ruleCombo.setSelectedIndex(0);
            target.setPaperPattern("EMPTY");
        }));
        grpRuling.add(createCompactIconButtonWithText("≡ Lined", "Single Rule Notebook Paper", e -> {
            ruleCombo.setSelectedIndex(1);
            target.setPaperPattern("SINGLE_RULE");
        }));
        grpRuling.add(createCompactIconButtonWithText("# Grid", "Square Rule Graph Paper", e -> {
            ruleCombo.setSelectedIndex(2);
            target.setPaperPattern("SQUARE_RULE");
        }));

        // 2. Line Sizing Adjustment
        JPanel grpRuleSize = createSingleRowGroup(ribbon, "LINE SIZING ADJUSTMENT");
        ModernComboBox<String> ruleSizeCombo = new ModernComboBox<>(new String[]{
                "Narrow Spacing (20px)",
                "College Spacing (24px)",
                "Standard Spacing (28px)",
                "Wide Spacing (32px)",
                "Large Spacing (36px)"
        });
        ruleSizeCombo.setSelectedIndex(2);
        ruleSizeCombo.setPreferredSize(new Dimension(170, 30));
        ruleSizeCombo.addActionListener(e -> {
            int idx = ruleSizeCombo.getSelectedIndex();
            int spacing = switch (idx) {
                case 0 -> 20;
                case 1 -> 24;
                case 3 -> 32;
                case 4 -> 36;
                default -> 28;
            };
            target.setRuleSpacing(spacing);
        });
        grpRuleSize.add(ruleSizeCombo);
        grpRuleSize.add(createCompactIconButtonWithText("-", "Narrower Ruling Spacing", e -> {
            int cur = ruleSizeCombo.getSelectedIndex();
            if (cur > 0) ruleSizeCombo.setSelectedIndex(cur - 1);
        }));
        grpRuleSize.add(createCompactIconButtonWithText("+", "Wider Ruling Spacing", e -> {
            int cur = ruleSizeCombo.getSelectedIndex();
            if (cur < 4) ruleSizeCombo.setSelectedIndex(cur + 1);
        }));

        // 3. Page Breaks
        JPanel grpBreak = createSingleRowGroup(ribbon, "PAGE BREAKS");
        JButton addBreakBtn = createTextWithIconButton("+ Page Break", VectorIcons.Type.PAGE_BREAK, "Insert an interactive page break to start a new page", e -> target.insertPageBreak());
        addBreakBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBreakBtn.setForeground(new Color(13, 148, 136));
        grpBreak.add(addBreakBtn);

        // 4. Paper Size
        JPanel grpSize = createSingleRowGroup(ribbon, "PAGE SIZE");
        ModernComboBox<String> sizeCombo = new ModernComboBox<>(new String[]{
                "A4 (740 × 960)",
                "US Letter (760 × 900)",
                "US Legal (740 × 1120)",
                "Compact Note (640 × 840)",
                "Wide Canvas (880 × 1060)"
        });
        sizeCombo.setPreferredSize(new Dimension(175, 30));
        sizeCombo.addActionListener(e -> {
            String sel = (String) sizeCombo.getSelectedItem();
            target.setPageFormat(sel);
        });
        grpSize.add(sizeCombo);

        // 5. Margins
        JPanel grpMargins = createSingleRowGroup(ribbon, "PAGE MARGINS");
        ModernComboBox<String> marginCombo = new ModernComboBox<>(new String[]{
                "Normal (1.0\")",
                "Narrow (0.5\")",
                "Wide (1.5\")"
        });
        marginCombo.setPreferredSize(new Dimension(140, 30));
        marginCombo.addActionListener(e -> {
            String sel = (String) marginCombo.getSelectedItem();
            target.setPageMargins(sel);
        });
        grpMargins.add(marginCombo);

        // 6. Paper Tone & Line Spacing
        JPanel grpTone = createSingleRowGroup(ribbon, "PAPER TONE");
        ModernComboBox<String> toneCombo = new ModernComboBox<>(new String[]{
                "Pure White Paper",
                "Warm Cream Paper",
                "Soft Gray Paper"
        });
        toneCombo.setPreferredSize(new Dimension(150, 30));
        toneCombo.addActionListener(e -> {
            String sel = (String) toneCombo.getSelectedItem();
            if (sel != null && sel.contains("Cream")) target.setPagePaperTone("warm cream");
            else if (sel != null && sel.contains("Gray")) target.setPagePaperTone("soft gray");
            else target.setPagePaperTone("white");
        });
        grpTone.add(toneCombo);

        return ribbon;
    }

    // =========================================================================
    // TAB 5: REVIEW & PROOF (Grammar Mode, Auto-Fix, Structure Detection, Metrics)
    // =========================================================================
    private JPanel createPanelReview() {
        JPanel ribbon = createRibbonContainer();

        // 1. Proofreading Mode
        JPanel grpProof = createSingleRowGroup(ribbon, "PROOFREADING & GRAMMAR");
        JButton grammarHero = createTextWithIconButton("Grammar Mode", VectorIcons.Type.CHECKMARK, "Open proofreading studio to review each issue", e -> target.showGrammarDialog());
        grammarHero.setFont(new Font("Segoe UI", Font.BOLD, 12));
        grammarHero.setForeground(new Color(22, 163, 74));
        grpProof.add(grammarHero);

        JButton fixAllBtn = createTextWithIconButton("1-Click Auto-Fix All", VectorIcons.Type.REFRESH, "Automatically correct all repeated words, typos, and spacing flaws", e -> target.autoFixAllGrammar());
        fixAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        grpProof.add(fixAllBtn);

        // 2. Intelligent Document Structure Detection
        JPanel grpStruct = createSingleRowGroup(ribbon, "STRUCTURE DETECTOR");
        JButton structBtn = createTextWithIconButton("Auto-Structure", VectorIcons.Type.REFRESH, "Auto-detect topics, subtopics, checklists, models, and markdown tables", e -> target.autoDetectStructure());
        structBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        grpStruct.add(structBtn);

        // 3. Live Metrics (2-Tier Compact)
        JPanel[] grpMetricRows = createTwoRowGroup(ribbon, "LIVE TELEMETRY");

        statsWordsBadge = new JLabel("0 words");
        statsWordsBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statsWordsBadge.setForeground(ThemeManager.getTextPrimary());
        statsWordsBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(3, 7, 3, 7)
        ));

        statsCharsBadge = new JLabel("0 chars");
        statsCharsBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsCharsBadge.setForeground(ThemeManager.getTextSecondary());
        statsCharsBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(3, 7, 3, 7)
        ));

        statsReadTimeBadge = new JLabel("~0 min read");
        statsReadTimeBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsReadTimeBadge.setForeground(new Color(37, 99, 235));
        statsReadTimeBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(3, 7, 3, 7)
        ));

        JButton refreshStatsBtn = createIconButton(VectorIcons.Type.REFRESH, "Recalculate Document Statistics", e -> updateLiveStats());

        grpMetricRows[0].add(statsWordsBadge);
        grpMetricRows[0].add(statsCharsBadge);
        grpMetricRows[1].add(statsReadTimeBadge);
        grpMetricRows[1].add(refreshStatsBtn);

        // 4. Smart Accessible Options Group
        JPanel grpA11y = createSingleRowGroup(ribbon, "SMART ACCESSIBILITY");
        JButton speakBtn = createTextWithIconButton("🔊 Read Aloud", VectorIcons.Type.CHECKMARK, "Accessible Text-to-Speech: Listen to document or selection", e -> target.speakSelectionOrDocument());
        speakBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        speakBtn.setForeground(new Color(37, 99, 235));
        grpA11y.add(speakBtn);

        JButton contrastBtn = createTextWithIconButton("👁 High Contrast", VectorIcons.Type.SETTINGS, "Toggle accessible high-contrast reading canvas", e -> target.toggleHighContrastMode());
        contrastBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        grpA11y.add(contrastBtn);

        JButton dyslexicBtn = createTextWithIconButton("📖 Dyslexia Font", VectorIcons.Type.DOCUMENT, "Toggle high-legibility accessible font & line spacing", e -> target.toggleDyslexicFont());
        dyslexicBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        grpA11y.add(dyslexicBtn);

        JButton outlineBtn = createTextWithIconButton("📑 Section Outline", VectorIcons.Type.DOCUMENT, "Open accessible Headings Navigator (H1, H2, H3)", e -> target.showHeadingsNavigator());
        outlineBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        grpA11y.add(outlineBtn);

        JButton auditBtn = createTextWithIconButton("🛡️ A11y Audit", VectorIcons.Type.REFRESH, "Run 1-Click WCAG 2.1 Accessibility Audit & Auto-Fixer", e -> target.runAccessibilityAudit());
        auditBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        auditBtn.setForeground(new Color(22, 163, 74));
        grpA11y.add(auditBtn);

        return ribbon;
    }

    // =========================================================================
    // TAB 6: FILE & EXPORT (Export HTML/Markdown, Print/PDF, Diagnostics, Bookshelf)
    // =========================================================================
    private JPanel createPanelFileExport() {
        JPanel ribbon = createRibbonContainer();

        // 1. Publishing & Print
        JPanel grpExport = createSingleRowGroup(ribbon, "PUBLISHING & PRINT");
        JButton printHero = createTextWithIconButton("Print / PDF", VectorIcons.Type.PRINT, "Print Document or Save as PDF", e -> target.printDocument());
        printHero.setFont(new Font("Segoe UI", Font.BOLD, 12));
        printHero.setForeground(new Color(37, 99, 235));
        grpExport.add(printHero);

        grpExport.add(createTextWithIconButton("Export HTML", VectorIcons.Type.EXPORT, "Download standalone HTML document", e -> target.exportHtml()));
        grpExport.add(createTextWithIconButton("Export Markdown", VectorIcons.Type.EXPORT, "Download pure Markdown document", e -> target.exportMarkdown()));

        // 2. Storage & Navigation
        JPanel grpNav = createSingleRowGroup(ribbon, "STORAGE & SYSTEM");
        grpNav.add(createTextWithIconButton("Storage Telemetry", VectorIcons.Type.SETTINGS, "SQLite WAL Storage Diagnostics & Integrity", e -> target.showDbDiagnostics()));
        grpNav.add(createTextWithIconButton("Back to Bookshelf", VectorIcons.Type.BOOK, "Save and return to book library", e -> target.backToBookshelf()));

        return ribbon;
    }

    public void updateLiveStats() {
        int words = target.getWordCount();
        int chars = target.getCharCount();
        int readTime = Math.max(1, (int) Math.ceil(words / 200.0));

        if (statsWordsBadge != null) statsWordsBadge.setText(words + (words == 1 ? " word" : " words"));
        if (statsCharsBadge != null) statsCharsBadge.setText(chars + " chars");
        if (statsReadTimeBadge != null) statsReadTimeBadge.setText("~" + (words == 0 ? 0 : readTime) + " min read");
    }

    // =========================================================================
    // UI Helpers & Component Factories
    // =========================================================================
    private JPanel createRibbonContainer() {
        JPanel ribbon = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        ribbon.setOpaque(true);
        ribbon.setBackground(ThemeManager.getSurface());
        return ribbon;
    }

    private JComponent wrapRibbonScroll(JPanel ribbon) {
        JScrollPane scroll = new JScrollPane(ribbon);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.getHorizontalScrollBar().setUnitIncrement(28);
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 5));
        scroll.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ThemeManager.isDark() ? new Color(71, 85, 105) : new Color(203, 213, 225);
                this.trackColor = ThemeManager.getSurface();
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
        scroll.addMouseWheelListener(e -> {
            if (e.getScrollType() == java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                int totalScroll = e.getUnitsToScroll() * scroll.getHorizontalScrollBar().getUnitIncrement();
                scroll.getHorizontalScrollBar().setValue(scroll.getHorizontalScrollBar().getValue() + totalScroll);
                e.consume();
            }
        });
        return scroll;
    }

    private JPanel createSingleRowGroup(JPanel container, String title) {
        JPanel outer = new JPanel(new BorderLayout(0, 3));
        outer.setOpaque(true);
        outer.setBackground(ThemeManager.isDark() ? new Color(24, 33, 47) : new Color(250, 252, 255));
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(226, 232, 240), 1, true),
                new EmptyBorder(3, 6, 2, 6)
        ));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
        tools.setOpaque(false);
        outer.add(tools, BorderLayout.CENTER);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 9));
        label.setForeground(ThemeManager.isDark() ? new Color(148, 163, 184) : new Color(100, 116, 139));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        outer.add(label, BorderLayout.SOUTH);

        container.add(outer);
        return tools;
    }

    private JPanel[] createTwoRowGroup(JPanel container, String title) {
        JPanel outer = new JPanel(new BorderLayout(0, 2));
        outer.setOpaque(true);
        outer.setBackground(ThemeManager.isDark() ? new Color(24, 33, 47) : new Color(250, 252, 255));
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(226, 232, 240), 1, true),
                new EmptyBorder(3, 6, 2, 6)
        ));

        JPanel rowsBox = new JPanel();
        rowsBox.setLayout(new BoxLayout(rowsBox, BoxLayout.Y_AXIS));
        rowsBox.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        row1.setOpaque(false);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        row2.setOpaque(false);

        rowsBox.add(row1);
        rowsBox.add(row2);
        outer.add(rowsBox, BorderLayout.CENTER);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 9));
        label.setForeground(ThemeManager.isDark() ? new Color(148, 163, 184) : new Color(100, 116, 139));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        outer.add(label, BorderLayout.SOUTH);

        container.add(outer);
        return new JPanel[]{row1, row2};
    }

    private JButton createBigRibbonButton(String label, VectorIcons.Type type, String tooltip, java.awt.event.ActionListener al) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(0, 2));
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(4, 6, 4, 6)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel iconLabel = new JLabel(VectorIcons.create(type, 22, ThemeManager.getTextPrimary()), SwingConstants.CENTER);
        JLabel textLabel = new JLabel(label, SwingConstants.CENTER);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        textLabel.setForeground(ThemeManager.getTextPrimary());

        btn.add(iconLabel, BorderLayout.CENTER);
        btn.add(textLabel, BorderLayout.SOUTH);

        FontMetrics fm = textLabel.getFontMetrics(textLabel.getFont());
        int w = Math.max(56, fm.stringWidth(label) + 18);
        btn.setPreferredSize(new Dimension(w, 64));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(239, 246, 255));
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true),
                            new EmptyBorder(4, 4, 4, 4)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                        new EmptyBorder(4, 4, 4, 4)
                ));
            }
        });

        btn.addActionListener(al);
        return btn;
    }

    private JButton createAiTileButton(String modelName, String subtitle, VectorIcons.Type iconType, Color brandColor, int tabIndex) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(8, 2));
        btn.setPreferredSize(new Dimension(136, 62));
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Open live embedded " + modelName + " browser");

        JLabel iconLabel = new JLabel(VectorIcons.create(iconType, 22, brandColor));
        iconLabel.setPreferredSize(new Dimension(28, 28));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        textPanel.setOpaque(false);
        JLabel nameLbl = new JLabel(modelName);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLbl.setForeground(ThemeManager.getTextPrimary());

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLbl.setForeground(ThemeManager.getTextSecondary());

        textPanel.add(nameLbl);
        textPanel.add(subLbl);

        btn.add(iconLabel, BorderLayout.WEST);
        btn.add(textPanel, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(241, 245, 249));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(brandColor, 1, true),
                        new EmptyBorder(6, 8, 6, 8)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                        new EmptyBorder(6, 8, 6, 8)
                ));
            }
        });

        btn.addActionListener(e -> target.openAiTab(tabIndex));
        return btn;
    }

    private JButton createIconButton(VectorIcons.Type type, String tooltip, java.awt.event.ActionListener al) {
        JButton btn = new JButton(VectorIcons.create(type, 16, ThemeManager.getTextPrimary()));
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(38, 34));
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(239, 246, 255));
                    btn.setBorder(BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true));
            }
        });

        btn.addActionListener(al);
        return btn;
    }

    private JButton createCompactIconButton(VectorIcons.Type type, String tooltip, java.awt.event.ActionListener al) {
        JButton btn = new JButton(VectorIcons.create(type, 15, ThemeManager.getTextPrimary()));
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(34, 32));
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(239, 246, 255));
                    btn.setBorder(BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true));
            }
        });

        btn.addActionListener(al);
        return btn;
    }

    private JButton createCompactIconButtonWithText(String symbol, String tooltip, java.awt.event.ActionListener al) {
        JButton btn = new JButton(symbol);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(ThemeManager.getTextPrimary());
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        FontMetrics fm = btn.getFontMetrics(btn.getFont());
        int w = (symbol.length() <= 2) ? 32 : Math.max(34, fm.stringWidth(symbol) + 16);
        btn.setPreferredSize(new Dimension(w, 28));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(239, 246, 255));
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true),
                            new EmptyBorder(2, 6, 2, 6)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                        new EmptyBorder(2, 6, 2, 6)
                ));
            }
        });

        btn.addActionListener(al);
        return btn;
    }

    private JButton createVisualRibbonButton(String text, VectorIcons.Type iconType, Color iconColor, String tooltip, java.awt.event.ActionListener al) {
        Icon icon = (iconType != null) ? VectorIcons.create(iconType, 14, iconColor != null ? iconColor : ThemeManager.getTextPrimary()) : null;
        return createVisualRibbonButton(text, icon, tooltip, al);
    }

    private JButton createVisualRibbonButton(String text, Icon icon, String tooltip, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text, icon);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(ThemeManager.getTextPrimary());
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        FontMetrics fm = btn.getFontMetrics(btn.getFont());
        int textW = fm.stringWidth(text);
        int iconW = (icon != null) ? icon.getIconWidth() + 6 : 0;
        int w = textW + iconW + 18;
        btn.setPreferredSize(new Dimension(w, 28));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(239, 246, 255));
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true),
                            new EmptyBorder(3, 8, 3, 8)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                        new EmptyBorder(3, 8, 3, 8)
                ));
            }
        });

        btn.addActionListener(al);
        return btn;
    }

    private static class PaletteSwatchIcon implements Icon {
        private final Color[] colors;
        private final int width;
        private final int height;

        public PaletteSwatchIcon(Color[] colors, int width, int height) {
            this.colors = colors;
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int stripeW = Math.max(2, (width - (colors.length - 1)) / colors.length);
            int curX = x;
            for (int i = 0; i < colors.length; i++) {
                g2.setColor(colors[i]);
                int w = (i == colors.length - 1) ? (x + width - curX) : stripeW;
                g2.fillRoundRect(curX, y, Math.max(1, w), height, 2, 2);
                curX += w + 1;
            }
            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawRoundRect(x, y, width - 1, height - 1, 3, 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }
        @Override
        public int getIconHeight() { return height; }
    }

    private JButton createPaletteSwatchButton(String themeName, Color[] swatchColors, String tooltip, java.awt.event.ActionListener al) {
        Icon swatchIcon = new PaletteSwatchIcon(swatchColors, 18, 12);
        return createVisualRibbonButton(themeName, swatchIcon, tooltip, al);
    }

    private JButton createHeroStudioButton(String title, String subtitle, VectorIcons.Type iconType, Color accentColor, String tooltip, java.awt.event.ActionListener al) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(8, 2));
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1, true),
                new EmptyBorder(6, 10, 6, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(VectorIcons.create(iconType, 24, accentColor));
        iconLabel.setPreferredSize(new Dimension(30, 30));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        textPanel.setOpaque(false);
        JLabel nameLbl = new JLabel(title);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLbl.setForeground(ThemeManager.getTextPrimary());

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLbl.setForeground(accentColor);

        textPanel.add(nameLbl);
        textPanel.add(subLbl);

        btn.add(iconLabel, BorderLayout.WEST);
        btn.add(textPanel, BorderLayout.CENTER);

        FontMetrics fm = nameLbl.getFontMetrics(nameLbl.getFont());
        int w = fm.stringWidth(title) + 30 + 34;
        btn.setPreferredSize(new Dimension(Math.max(136, w), 64));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(240, 249, 255));
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(accentColor.darker(), 1, true),
                            new EmptyBorder(6, 10, 6, 12)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 1, true),
                        new EmptyBorder(6, 10, 6, 12)
                ));
            }
        });

        btn.addActionListener(al);
        return btn;
    }

    private JButton createTextWithIconButton(String label, VectorIcons.Type type, String tooltip, java.awt.event.ActionListener al) {
        JButton btn = new JButton(label, VectorIcons.create(type, 13, ThemeManager.getTextPrimary()));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(ThemeManager.getTextPrimary());
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 14, 32));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(239, 246, 255));
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true),
                            new EmptyBorder(3, 8, 3, 8)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getSurface());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                        new EmptyBorder(3, 8, 3, 8)
                ));
            }
        });

        btn.addActionListener(al);
        return btn;
    }

    private JButton createColorButtonWithSwatch(String label, Color initialColor, java.util.function.Consumer<Color> onColorPicked, boolean isText) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(4, 0));
        btn.setToolTipText("Select " + label);
        btn.setFocusPainted(false);
        btn.setBackground(ThemeManager.getSurface());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btn.setPreferredSize(new Dimension(94, 28));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(VectorIcons.create(VectorIcons.Type.PALETTE, 12, ThemeManager.getTextSecondary()));
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        textLabel.setForeground(ThemeManager.getTextPrimary());

        JPanel textWithSwatch = new JPanel(new BorderLayout(0, 1));
        textWithSwatch.setOpaque(false);
        textWithSwatch.add(textLabel, BorderLayout.CENTER);

        JPanel swatch = new JPanel();
        swatch.setPreferredSize(new Dimension(textLabel.getPreferredSize().width, 3));
        swatch.setBackground(initialColor);
        if (isText) textColorSwatch = swatch;
        else hiliteColorSwatch = swatch;
        textWithSwatch.add(swatch, BorderLayout.SOUTH);

        btn.add(iconLabel, BorderLayout.WEST);
        btn.add(textWithSwatch, BorderLayout.CENTER);

        btn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Choose " + label, initialColor);
            if (chosen != null) onColorPicked.accept(chosen);
        });

        return btn;
    }

    private void updateColorSwatches() {
        if (textColorSwatch != null) textColorSwatch.setBackground(activeTextColor);
        if (hiliteColorSwatch != null) hiliteColorSwatch.setBackground(activeHighlightColor);
    }

    private void insertQuickGrid(int rows, int cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px 0;border:1px solid #cbd5e1;'>");
        for (int r = 0; r < rows; r++) {
            sb.append("<tr>");
            for (int c = 0; c < cols; c++) {
                if (r == 0) sb.append("<th style='background-color:#f1f5f9;padding:8px 12px;font-weight:bold;color:#0f172a;'>Header ").append(c + 1).append("</th>");
                else sb.append("<td style='padding:8px 12px;color:#334155;'>Cell</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table><p><br></p>");
        target.insertHtml(sb.toString());
    }

    private JButton createAdvancedUndoButton() {
        JButton btn = new JButton(VectorIcons.create(VectorIcons.Type.UNDO, 14, null));
        btn.setText("▾");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        btn.setHorizontalTextPosition(SwingConstants.LEFT);
        btn.setIconTextGap(3);
        btn.setToolTipText("Undo (Ctrl+Z) — Click ▾ or right-click for history");
        btn.setPreferredSize(new Dimension(42, 28));
        btn.setMargin(new Insets(2, 4, 2, 4));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateQuickAccessBtnTheme(btn);

        final boolean[] arrowClicked = {false};

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getX() > btn.getWidth() - 14) {
                    arrowClicked[0] = true;
                    AdvancedUndoManager um = target.getAdvancedUndoManager();
                    if (um != null) {
                        um.showHistoryPopup(btn, true, () -> target.execFormat("undo", null));
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    arrowClicked[0] = true;
                    AdvancedUndoManager um = target.getAdvancedUndoManager();
                    if (um != null) {
                        um.showHistoryPopup(btn, true, () -> target.execFormat("undo", null));
                    }
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(226, 232, 240));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                updateQuickAccessBtnTheme(btn);
            }
        });

        btn.addActionListener(e -> {
            if (arrowClicked[0]) {
                arrowClicked[0] = false;
                return;
            }
            target.execFormat("undo", null);
        });

        AdvancedUndoManager um = target.getAdvancedUndoManager();
        if (um != null) {
            boolean canUndo = um.canUndoAction();
            btn.setEnabled(canUndo);
            btn.setToolTipText(canUndo ? "Undo: " + um.getUndoName() + " (Ctrl+Z) — Click ▾ for history" : "Nothing to undo");
            um.addStateListener(() -> {
                boolean u = um.canUndoAction();
                btn.setEnabled(u);
                btn.setToolTipText(u ? "Undo: " + um.getUndoName() + " (Ctrl+Z) — Click ▾ for history" : "Nothing to undo");
                btn.repaint();
            });
        } else {
            btn.setEnabled(false);
            btn.setToolTipText("Nothing to undo");
        }
        return btn;
    }

    private JButton createAdvancedRedoButton() {
        JButton btn = new JButton(VectorIcons.create(VectorIcons.Type.REDO, 14, null));
        btn.setText("▾");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        btn.setHorizontalTextPosition(SwingConstants.LEFT);
        btn.setIconTextGap(3);
        btn.setToolTipText("Redo (Ctrl+Y) — Click ▾ or right-click for history");
        btn.setPreferredSize(new Dimension(42, 28));
        btn.setMargin(new Insets(2, 4, 2, 4));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateQuickAccessBtnTheme(btn);

        final boolean[] arrowClicked = {false};

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getX() > btn.getWidth() - 14) {
                    arrowClicked[0] = true;
                    AdvancedUndoManager um = target.getAdvancedUndoManager();
                    if (um != null) {
                        um.showHistoryPopup(btn, false, () -> target.execFormat("redo", null));
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    arrowClicked[0] = true;
                    AdvancedUndoManager um = target.getAdvancedUndoManager();
                    if (um != null) {
                        um.showHistoryPopup(btn, false, () -> target.execFormat("redo", null));
                    }
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(226, 232, 240));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                updateQuickAccessBtnTheme(btn);
            }
        });

        btn.addActionListener(e -> {
            if (arrowClicked[0]) {
                arrowClicked[0] = false;
                return;
            }
            target.execFormat("redo", null);
        });

        AdvancedUndoManager um = target.getAdvancedUndoManager();
        if (um != null) {
            boolean canRedo = um.canRedoAction();
            btn.setEnabled(canRedo);
            btn.setToolTipText(canRedo ? "Redo: " + um.getRedoName() + " (Ctrl+Y) — Click ▾ for history" : "Nothing to redo");
            um.addStateListener(() -> {
                boolean r = um.canRedoAction();
                btn.setEnabled(r);
                btn.setToolTipText(r ? "Redo: " + um.getRedoName() + " (Ctrl+Y) — Click ▾ for history" : "Nothing to redo");
                btn.repaint();
            });
        } else {
            btn.setEnabled(false);
            btn.setToolTipText("Nothing to redo");
        }
        return btn;
    }

    private void updateQuickAccessBtnTheme(JButton btn) {
        if (btn == null) return;
        btn.setBackground(ThemeManager.isDark() ? new Color(30, 41, 59) : new Color(255, 255, 255));
        btn.setForeground(ThemeManager.getTextPrimary());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(2, 5, 2, 5)
        ));
    }

    private JComponent createTabSeparator() {
        JPanel sep = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(203, 213, 225));
                g.fillRect(3, 4, 1, getHeight() - 8);
            }
        };
        sep.setOpaque(false);
        sep.setPreferredSize(new Dimension(8, 28));
        return sep;
    }

    private void addChartDesignCards(JPanel container, boolean isContextual) {
        for (ChartCatalog.StyleInfo style : ChartCatalog.styles()) {
            ThumbCard card = ThumbCard.ofDesign(
                    style.code(),
                    style.shortLabel(),
                    style.label() + " — " + style.hint(),
                    66,
                    40
            );
            card.setOnClick(() -> {
                target.switchSelectedChartStyle(style.code());
                setSelectedChartDesignStyle(style.code());
            });
            chartDesignCards.add(card);
            container.add(card);
        }
    }

    public void setSelectedChartDesignStyle(String styleCode) {
        if (styleCode == null || chartDesignCards.isEmpty()) return;
        String normalized = styleCode.trim().replace(" ", "_").toUpperCase();
        for (ThumbCard card : chartDesignCards) {
            String c = card.getStyleCode();
            boolean match = c != null && (c.equalsIgnoreCase(normalized) || c.replace(" ", "_").equalsIgnoreCase(normalized));
            card.setSelected(match);
        }
    }

    private void addChartThemeCards(JPanel container, boolean isContextual) {
        for (ChartTheme theme : ChartTheme.values()) {
            String shortLabel = switch (theme) {
                case CORPORATE_BLUE -> "Corporate";
                case EMERALD_GREEN -> "Emerald";
                case CYBERPUNK_NEON -> "Cyberpunk";
                case SUNSET_CORAL -> "Sunset";
                case SLATE_DARK -> "Slate";
                case WARM_PASTEL -> "Pastel";
            };
            ThumbCard card = ThumbCard.ofTheme(
                    theme,
                    shortLabel,
                    theme.getDisplayName() + " Theme Palette",
                    64,
                    40
            );
            card.setOnClick(() -> {
                if (isContextual) {
                    target.switchSelectedChartTheme(theme.getDisplayName());
                } else {
                    target.insertQuickChart("Bar", "Q1: 52\nQ2: 74\nQ3: 88\nQ4: 105", theme.getDisplayName(), 580, 290);
                }
                setSelectedChartTheme(theme.getDisplayName());
            });
            chartThemeCards.add(card);
            container.add(card);
        }

        ThumbCard studioCard = ThumbCard.ofPaletteStudio(
                "Studio...",
                "Custom Palette Studio & Series Color Picker",
                64,
                40
        );
        studioCard.setOnClick(() -> {
            target.showChartPaletteDialog();
        });
        chartThemeCards.add(studioCard);
        container.add(studioCard);
    }

    public void setSelectedChartTheme(String themeName) {
        if (themeName == null || chartThemeCards.isEmpty()) return;
        String clean = themeName.trim();
        for (ThumbCard card : chartThemeCards) {
            String t = card.getThemeName();
            boolean match = t != null && t.equalsIgnoreCase(clean);
            card.setSelected(match);
        }
    }

    private void addChartTypeCards(JPanel container, boolean isContextual) {
        for (ChartCatalog.TypeInfo info : ChartCatalog.types()) {
            ChartType type = info.type();
            ThumbCard card = ThumbCard.ofType(
                    type,
                    info.label(),
                    info.label() + " — " + info.hint(),
                    62,
                    40
            );
            card.setOnClick(() -> {
                if (isContextual) {
                    target.switchSelectedChartType(info.label());
                } else {
                    target.insertQuickChart(info.label(), info.sample());
                }
                setSelectedChartType(type);
            });
            chartTypeCards.add(card);
            container.add(card);
        }
    }

    public void setSelectedChartType(ChartType type) {
        if (type == null || chartTypeCards.isEmpty()) return;
        for (ThumbCard card : chartTypeCards) {
            boolean match = card.getChartType() == type;
            card.setSelected(match);
        }
        updateChartDesignCardsForType(type);
    }

    public void setSelectedChartType(String typeStr) {
        if (typeStr == null) return;
        setSelectedChartType(ChartCatalog.parse(typeStr));
    }

    public void updateChartDesignCardsForType(ChartType type) {
        if (type == null) type = ChartType.COLUMN;
        for (ThumbCard card : chartDesignCards) {
            card.setDesignType(type);
        }
    }

    private void addHeaderDesignCards(JPanel container) {
        record HeaderOption(String code, String label, String tooltip) {}
        HeaderOption[] options = new HeaderOption[]{
                new HeaderOption("MODERN_LINE", "Modern Line", "Document Title & Right Tagline with Blue Rule"),
                new HeaderOption("CLEAN_MINIMAL", "Minimal", "Minimalist Title and Clean Page Layout"),
                new HeaderOption("ACCENT_BANNER", "Banner", "Full-width Gradient Header Banner"),
                new HeaderOption("EDITORIAL_SPLIT", "Editorial", "Editorial Magazine Split Tag and Title"),
                new HeaderOption("CORPORATE", "Corporate", "Corporate Formal Double-Rule Header"),
                new HeaderOption("NONE", "None", "No running header on pages")
        };
        for (HeaderOption opt : options) {
            ThumbCard card = ThumbCard.ofHeaderDesign(opt.code, opt.label, opt.tooltip, 68, 40);
            card.setOnClick(() -> {
                target.setHeaderStyle(opt.code);
                setSelectedHeaderDesign(opt.code);
            });
            headerDesignCards.add(card);
            container.add(card);
        }
        setSelectedHeaderDesign("MODERN_LINE");
    }

    public void setSelectedHeaderDesign(String headerStyle) {
        if (headerStyle == null || headerDesignCards.isEmpty()) return;
        String clean = headerStyle.trim();
        for (ThumbCard card : headerDesignCards) {
            String s = card.getHeaderStyle();
            boolean match = s != null && s.equalsIgnoreCase(clean);
            card.setSelected(match);
        }
    }

    private void addFooterDesignCards(JPanel container) {
        record FooterOption(String code, String label, String tooltip) {}
        FooterOption[] options = new FooterOption[]{
                new FooterOption("PAGE_NUMBER_CENTER", "Centered", "Centered Colored Pill Page Number"),
                new FooterOption("SPLIT_TITLE_PAGE", "Split Page", "Document Title Left, Page Number Right"),
                new FooterOption("ACCENT_BAR", "Accent Bar", "Bottom Gradient Accent Bar"),
                new FooterOption("DOCUMENT_INFO", "Doc Info", "Document Title, Timestamp & Page Number"),
                new FooterOption("MINIMAL_PAGE", "Minimal", "Clean Right-Aligned Page Number"),
                new FooterOption("NONE", "None", "No running footer on pages")
        };
        for (FooterOption opt : options) {
            ThumbCard card = ThumbCard.ofFooterDesign(opt.code, opt.label, opt.tooltip, 68, 40);
            card.setOnClick(() -> {
                target.setFooterStyle(opt.code);
                setSelectedFooterDesign(opt.code);
            });
            footerDesignCards.add(card);
            container.add(card);
        }
        setSelectedFooterDesign("PAGE_NUMBER_CENTER");
    }

    public void setSelectedFooterDesign(String footerStyle) {
        if (footerStyle == null || footerDesignCards.isEmpty()) return;
        String clean = footerStyle.trim();
        for (ThumbCard card : footerDesignCards) {
            String s = card.getFooterStyle();
            boolean match = s != null && s.equalsIgnoreCase(clean);
            card.setSelected(match);
        }
    }

    private void addTextStyleCards(JPanel container) {
        record StyleOption(String styleName, String label, String sample, String tooltip) {}
        StyleOption[] options = new StyleOption[]{
                new StyleOption("Normal", "Normal", "AaBb", "Standard body paragraph text (14pt)"),
                new StyleOption("Heading 1", "Heading 1", "H1", "Primary chapter / major section heading (24pt Bold)"),
                new StyleOption("Heading 2", "Heading 2", "H2", "Sub-section heading (19pt Bold)"),
                new StyleOption("Heading 3", "Heading 3", "H3", "Minor sub-heading (16pt Semi-bold)"),
                new StyleOption("Title", "Title", "Title", "Main document title heading (30pt Major)"),
                new StyleOption("Subtitle", "Subtitle", "Subtitle", "Document subtitle / byline (15pt Italic)"),
                new StyleOption("Quote", "Quote", "Quote", "Indented blockquote callout with left accent bar")
        };
        for (StyleOption opt : options) {
            ThumbCard card = ThumbCard.ofTextStyle(opt.styleName, opt.label, opt.sample, opt.tooltip, 64, 40);
            card.setOnClick(() -> {
                target.execFormat("heading", opt.styleName);
                setSelectedTextStyle(opt.styleName);
            });
            textStyleCards.add(card);
            container.add(card);
        }
        setSelectedTextStyle("Normal");
    }

    public void setSelectedTextStyle(String textStyle) {
        if (textStyle == null || textStyleCards.isEmpty()) return;
        String clean = textStyle.trim();
        for (ThumbCard card : textStyleCards) {
            String s = card.getTextStyle();
            boolean match = s != null && s.equalsIgnoreCase(clean);
            card.setSelected(match);
        }
    }

    private void addCardTemplateCards(JPanel container) {
        record CardOption(String code, String label, String tooltip, String cardType) {}
        CardOption[] options = new CardOption[]{
                new CardOption("KPI", "Metric KPI", "High-Impact Key Performance Metric Card with Sparkline", "KPI"),
                new CardOption("COMPARISON", "Comparison", "Side-by-Side Pros vs Cons Comparison Matrix", "Comparison"),
                new CardOption("PROGRESS", "Progress Bar", "Target Milestone Progress Bar with Completion %", "Progress"),
                new CardOption("TAKEAWAYS", "Takeaways", "Executive Key Takeaways & Summary Box", "Takeaways")
        };
        for (CardOption opt : options) {
            ThumbCard card = ThumbCard.ofCardTemplate(opt.code, opt.label, opt.tooltip, 68, 40);
            card.setOnClick(() -> {
                target.insertCard(opt.cardType);
            });
            cardTemplateCards.add(card);
            container.add(card);
        }
    }

    private void addBusinessTemplateCards(JPanel container) {
        record BusinessOption(String code, String label, String tooltip, String chartType, String sampleData, String theme) {}
        BusinessOption[] options = new BusinessOption[]{
                new BusinessOption("SALES", "Qtr Sales", "Pre-configured 4-Quarter Revenue & Sales Bar Chart",
                        "Column", "Q1 Actual: 145000\nQ2 Actual: 210000\nQ3 Actual: 280000\nQ4 Actual: 395000", "Corporate Blue"),
                new BusinessOption("VELOCITY", "Velocity", "Sprint Completion Velocity Trend Line Chart",
                        "Line", "Sprint 1: 32\nSprint 2: 45\nSprint 3: 42\nSprint 4: 58\nSprint 5: 64", "Emerald Mint"),
                new BusinessOption("MARKET_SHARE", "Market Share", "Segment Market Breakdown Donut Chart",
                        "Donut", "Enterprise: 45\nMid-Market: 30\nSMB: 25", "Sunset Coral"),
                new BusinessOption("ORG_CHART", "Org Chart", "Organizational Hierarchy Tree Diagram",
                        "Diagram", "CEO\n  VP Product\n    Design Lead\n    Eng Lead\n  VP Sales\n    Account Exec", "Slate Minimal")
        };
        for (BusinessOption opt : options) {
            ThumbCard card = ThumbCard.ofBusinessTemplate(opt.code, opt.label, opt.tooltip, 68, 40);
            card.setOnClick(() -> {
                target.insertQuickChart(opt.chartType, opt.sampleData, opt.theme, 580, 290);
            });
            businessTemplateCards.add(card);
            container.add(card);
        }
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        setBackground(ThemeManager.getRibbonBackground());
        tabBarPanel.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));
        cardsPanel.setBackground(ThemeManager.getSurface());
        if (undoButton != null) updateQuickAccessBtnTheme(undoButton);
        if (redoButton != null) updateQuickAccessBtnTheme(redoButton);
        selectTab(activeTab);
        repaint();
    }
}
