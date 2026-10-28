package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.dto.*;
import com.wordbook.core.application.service.*;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Native Swing Master Frame hosting the WordBook Desktop Studio,
 * MS Word-style ribbon, A4 canvas page, and SQLite synchronization.
 */
public class WordBookMainFrame extends JFrame implements RibbonPanel.DocumentActionTarget, ThemeManager.ThemeChangeListener {
    private static final String VIEW_SHELF = "shelf";
    private static final String VIEW_STUDIO = "studio";

    private final BookService bookService;
    private final LessonService lessonService;
    private final ExportService exportService;
    private final ChartEngineService chartEngineService;
    private final MigrationService migrationService;
    private final String dbPath;

    // Advanced Intelligent Engines
    private final CitationEngineService citationEngine = new CitationEngineService();
    private final GrammarEngineService grammarEngine = new GrammarEngineService();
    private final DocumentStructureDetector structureDetector = new DocumentStructureDetector();

    // View Components
    private CardLayout cardLayout;
    private JPanel rootCardPanel;
    private BookshelfPanel bookshelfPanel;
    private LessonSidebarPanel sidebarPanel;
    private RibbonPanel ribbonPanel;
    private DocumentCanvasPanel canvasPanel;
    private JScrollPane canvasScroll;
    private CardLayout centerCardLayout;
    private JPanel centerContainer;
    private JPanel emptyStatePanel;
    private AiSidePanel aiSidePanel;
    private ShapeFormatSidePanel shapeFormatSidePanel;
    private JPanel rightDeckPanel;
    private String activeRightPanel = null;
    private JSplitPane rightSplit;
    private JSplitPane splitPane;
    private boolean aiPanelVisible = false;
    private boolean shapeFormatVisible = false;

    // Status Bar Components
    private JLabel statusWordsLabel;
    private JLabel statusCharsLabel;
    private JLabel statusSyncLabel;
    private JLabel zoomPctLabel;
    private JSlider zoomSlider;

    // Active State & Asynchronous Persistence
    private String activeBookId;
    private String activeLessonId;
    private final ExecutorService autoSaveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WordBook-AutoSaveWorker");
        t.setDaemon(true);
        return t;
    });

    public WordBookMainFrame(BookService bookService,
                             LessonService lessonService,
                             ExportService exportService,
                             ChartEngineService chartEngineService,
                             MigrationService migrationService,
                             String dbPath) {
        this.bookService = bookService;
        this.lessonService = lessonService;
        this.exportService = exportService;
        this.chartEngineService = chartEngineService;
        this.migrationService = migrationService;
        this.dbPath = dbPath;

        initFrame();
        ThemeManager.addListener(this);
    }
    private void initFrame() {
        setTitle("Mikey Note — Modern Document Studio");
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int winW = Math.min(1600, Math.max(1360, (int) (screenSize.width * 0.88)));
            int winH = Math.min(1020, Math.max(860, (int) (screenSize.height * 0.88)));
            setSize(winW, winH);
        } catch (Exception e) {
            setSize(1440, 900);
        }
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Load Window Logo
        try {
            File logoFile = new File("assets/mikey_note_logo.png");
            if (!logoFile.exists()) {
                logoFile = new File("assets/logo.png");
            }
            if (!logoFile.exists()) {
                logoFile = new File("assets/notedown_logo.png");
            }
            if (logoFile.exists()) {
                setIconImage(ImageIO.read(logoFile));
            }
        } catch (Exception ignored) {}

        // Menu Bar
        setJMenuBar(createMenuBar());

        // Card Layout for Shelf vs Studio
        cardLayout = new CardLayout();
        rootCardPanel = new JPanel(cardLayout);

        // 1. Shelf View
        bookshelfPanel = new BookshelfPanel(bookService, this::openBook, this::showDbDiagnostics);
        rootCardPanel.add(bookshelfPanel, VIEW_SHELF);

        // 2. Studio View
        JPanel studioPanel = new JPanel(new BorderLayout());

        // Center Work Area (Sidebar | Canvas | AI Panel)
        sidebarPanel = new LessonSidebarPanel(bookService, lessonService, this::openLesson, this::deleteLesson, this::backToShelf);

        canvasPanel = new DocumentCanvasPanel(this::onDocumentContentChanged);

        // Top Ribbon
        ribbonPanel = new RibbonPanel(this);
        studioPanel.add(ribbonPanel, BorderLayout.NORTH);

        canvasPanel.setContextualTabHandler(new DocumentCanvasPanel.ContextualTabHandler() {
            @Override
            public void showContextualTab(String tabName) {
                ribbonPanel.showContextualTab(tabName);
                if (RibbonPanel.TAB_CHART_DESIGN.equals(tabName)) {
                    ChartEngineService.ChartMetadata meta = canvasPanel.getSelectedChartMetadata();
                    if (meta != null) {
                        if (meta.cardStyle() != null) {
                            ribbonPanel.setSelectedChartDesignStyle(meta.cardStyle());
                        }
                        if (meta.theme() != null) {
                            ribbonPanel.setSelectedChartTheme(meta.theme().getDisplayName());
                        }
                        if (meta.type() != null) {
                            ribbonPanel.setSelectedChartType(meta.type());
                        }
                    }
                }
            }
            @Override
            public void hideContextualTabs() {
                ribbonPanel.hideContextualTabs();
            }
            @Override
            public void showShapeFormatPanel(Element elem) {
                WordBookMainFrame.this.showShapeFormatPanel(elem);
            }
            @Override
            public void hideShapeFormatPanel() {
                WordBookMainFrame.this.hideShapeFormatPanel();
            }
        });
        canvasScroll = new JScrollPane(canvasPanel);
        canvasScroll.setBorder(null);
        canvasScroll.getVerticalScrollBar().setUnitIncrement(24);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(24);
        canvasScroll.setMinimumSize(new Dimension(340, 160));
        canvasScroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);

        // Smooth Ctrl/Cmd + Mouse Wheel Zoom
        canvasScroll.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                e.consume();
                int rotation = e.getWheelRotation();
                if (zoomSlider != null) {
                    int step = 5;
                    int newVal = Math.max(50, Math.min(200, zoomSlider.getValue() - rotation * step));
                    zoomSlider.setValue(newVal);
                }
            }
        });

        centerCardLayout = new CardLayout();
        centerContainer = new JPanel(centerCardLayout);
        centerContainer.add(canvasScroll, "CANVAS");
        emptyStatePanel = createEmptyLessonStatePanel();
        centerContainer.add(emptyStatePanel, "EMPTY");

        // AI Side Panel
        aiSidePanel = new AiSidePanel(this::insertHtml);
        aiSidePanel.setVisible(true);
        aiSidePanel.setMinimumSize(new Dimension(280, 160));
        aiSidePanel.setCloseCallback(this::toggleAiPanel);

        // Word-Style Shape & Picture Format Side Panel
        shapeFormatSidePanel = new ShapeFormatSidePanel(canvasPanel);
        shapeFormatSidePanel.setVisible(true);
        shapeFormatSidePanel.setMinimumSize(new Dimension(280, 160));
        shapeFormatSidePanel.setCloseCallback(this::hideShapeFormatPanel);

        // Right Deck Container holding AI Panel and Shape Format Panel
        rightDeckPanel = new JPanel(new CardLayout());
        rightDeckPanel.add(aiSidePanel, "AI");
        rightDeckPanel.add(shapeFormatSidePanel, "SHAPE_FORMAT");
        rightDeckPanel.setVisible(false);
        rightDeckPanel.setMinimumSize(new Dimension(280, 160));

        // Right split: center container (canvas or empty state) | Right Deck
        rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerContainer, rightDeckPanel);
        rightSplit.setContinuousLayout(true);
        rightSplit.setResizeWeight(1.0);
        rightSplit.setDividerSize(0);
        rightSplit.setBorder(null);

        // Left split: lessons sidebar | right split
        sidebarPanel.setMinimumSize(new Dimension(190, 160));
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, rightSplit);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerSize(5);
        studioPanel.add(splitPane, BorderLayout.CENTER);

        // Bottom Status Bar
        studioPanel.add(createStatusBar(), BorderLayout.SOUTH);

        rootCardPanel.add(studioPanel, VIEW_STUDIO);

        setContentPane(rootCardPanel);
        setupGlobalKeyBindings();
        cardLayout.show(rootCardPanel, VIEW_SHELF);
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem miNewBook = new JMenuItem("New Notebook...", VectorIcons.createIcon(VectorIcons.PLUS, 13, new Color(71, 85, 105)));
        miNewBook.addActionListener(e -> backToShelf());
        JMenuItem miExportHtml = new JMenuItem("Export as HTML...", VectorIcons.createIcon(VectorIcons.EXPORT, 13, new Color(71, 85, 105)));
        miExportHtml.addActionListener(e -> exportHtml());
        JMenuItem miExportMd = new JMenuItem("Export as Markdown...", VectorIcons.createIcon(VectorIcons.EXPORT, 13, new Color(71, 85, 105)));
        miExportMd.addActionListener(e -> exportMarkdown());
        JMenuItem miSettings = new JMenuItem("Settings & Themes...", VectorIcons.createIcon(VectorIcons.SETTINGS, 13, new Color(71, 85, 105)));
        miSettings.addActionListener(e -> showSettingsDialog());
        JMenuItem miDbDiag = new JMenuItem("Storage Telemetry & Health...", VectorIcons.createIcon(VectorIcons.FOLDER, 13, new Color(71, 85, 105)));
        miDbDiag.addActionListener(e -> showDbDiagnostics());
        JMenuItem miExit = new JMenuItem("Exit");
        miExit.addActionListener(e -> System.exit(0));

        fileMenu.add(miNewBook);
        fileMenu.addSeparator();
        fileMenu.add(miExportHtml);
        fileMenu.add(miExportMd);
        fileMenu.addSeparator();
        fileMenu.add(miSettings);
        fileMenu.add(miDbDiag);
        fileMenu.addSeparator();
        fileMenu.add(miExit);
        mb.add(fileMenu);

        // Insert Menu
        JMenu insertMenu = new JMenu("Insert");
        JMenuItem miTable = new JMenuItem("Table...", VectorIcons.createIcon(VectorIcons.TABLE, 13, new Color(71, 85, 105)));
        miTable.addActionListener(e -> showTableDialog());
        JMenuItem miChart = new JMenuItem("Vector Chart...", VectorIcons.createIcon(VectorIcons.CHART, 13, new Color(71, 85, 105)));
        miChart.addActionListener(e -> showChartDialog());
        insertMenu.add(miTable);
        insertMenu.add(miChart);
        mb.add(insertMenu);

        // Settings Menu
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem miTheme = new JMenuItem("Preferences & Themes...", VectorIcons.createIcon(VectorIcons.SETTINGS, 13, new Color(71, 85, 105)));
        miTheme.addActionListener(e -> showSettingsDialog());
        settingsMenu.add(miTheme);
        mb.add(settingsMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem miAbout = new JMenuItem("About Mikey Note");
        miAbout.addActionListener(e -> {
            ModernDialog.showMessage(this, "About Mikey Note",
                    "Mikey Note Studio v2.0.0\nModular Clean Architecture in Java 24\nHigh-Performance Local Embedded Storage (WAL mode)\nModern Ribbon & Vector Graphics Studio",
                    ModernDialog.MessageType.INFO);
        });
        helpMenu.add(miAbout);
        mb.add(helpMenu);

        return mb;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 32));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(203, 213, 225)),
            new EmptyBorder(0, 12, 0, 12)));
        bar.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));

        // Left: document stats
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        statusWordsLabel = new JLabel("0 words");
        statusWordsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusWordsLabel.setForeground(new Color(100, 116, 139));

        JLabel divider1 = new JLabel("|");
        divider1.setForeground(new Color(203, 213, 225));

        statusCharsLabel = new JLabel("0 chars");
        statusCharsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusCharsLabel.setForeground(new Color(100, 116, 139));

        JLabel divider2 = new JLabel("|");
        divider2.setForeground(new Color(203, 213, 225));

        statusSyncLabel = new JLabel("Saved",
            VectorIcons.createIcon(VectorIcons.CHECKMARK, 12, new Color(22, 163, 74)),
            SwingConstants.LEFT);
        statusSyncLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusSyncLabel.setForeground(new Color(22, 163, 74));

        left.add(statusWordsLabel);
        left.add(divider1);
        left.add(statusCharsLabel);
        left.add(divider2);
        left.add(statusSyncLabel);
        bar.add(left, BorderLayout.WEST);

        // Center: Premium AI Vector Model Quick-Access Buttons
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        center.setOpaque(false);
        record AiShortcut(String name, VectorIcons.Type iconType, Color brandColor, int tabIdx) {}
        AiShortcut[] aiShortcuts = {
            new AiShortcut("Claude", VectorIcons.Type.CLAUDE, new Color(204, 86, 54), 0),
            new AiShortcut("ChatGPT", VectorIcons.Type.CHATGPT, new Color(16, 163, 127), 1),
            new AiShortcut("Gemini", VectorIcons.Type.GEMINI, new Color(37, 99, 235), 2),
            new AiShortcut("DeepSeek", VectorIcons.Type.DEEPSEEK, new Color(14, 165, 233), 3),
            new AiShortcut("Perplexity", VectorIcons.Type.PERPLEXITY, new Color(13, 148, 136), 4),
            new AiShortcut("Scraper", VectorIcons.Type.SCRAPER, new Color(124, 58, 237), 5)
        };
        for (AiShortcut ai : aiShortcuts) {
            JButton aiBtn = new JButton(ai.name(), VectorIcons.create(ai.iconType(), 13, ai.brandColor())) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean hover = getModel().isRollover();
                    if (hover) {
                        g2.setColor(new Color(ai.brandColor().getRed(), ai.brandColor().getGreen(), ai.brandColor().getBlue(), 25));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(new Color(ai.brandColor().getRed(), ai.brandColor().getGreen(), ai.brandColor().getBlue(), 90));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            aiBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            aiBtn.setForeground(ThemeManager.getTextPrimary());
            aiBtn.setFocusPainted(false);
            aiBtn.setBorderPainted(false);
            aiBtn.setContentAreaFilled(false);
            aiBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            aiBtn.setToolTipText("Open live embedded " + ai.name() + " in side panel");
            aiBtn.addActionListener(e -> {
                if (!aiPanelVisible) toggleAiPanel();
                aiSidePanel.selectTab(ai.tabIdx());
            });
            center.add(aiBtn);
        }
        bar.add(center, BorderLayout.CENTER);

        // Right: zoom controls + AI panel toggle
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        // AI Toggle button
        JButton aiToggle = new JButton("AI ▶") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = aiPanelVisible ? new Color(37, 99, 235) :
                    (getModel().isRollover() ? new Color(226, 232, 240) : new Color(241, 245, 249));
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(aiPanelVisible ? Color.WHITE : new Color(71, 85, 105));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(aiPanelVisible ? "AI ◀" : "AI ▶",
                    (getWidth() - fm.stringWidth(aiPanelVisible ? "AI ◀" : "AI ▶")) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
            @Override public String getText() { return aiPanelVisible ? "AI \u25C0" : "AI \u25B6"; }
        };
        aiToggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        aiToggle.setFocusPainted(false);
        aiToggle.setBorderPainted(false);
        aiToggle.setContentAreaFilled(false);
        aiToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        aiToggle.setPreferredSize(new Dimension(60, 22));
        aiToggle.setToolTipText("Toggle AI Side Panel (Claude, ChatGPT, Gemini, DeepSeek)");
        aiToggle.addActionListener(e -> { toggleAiPanel(); aiToggle.repaint(); });
        right.add(aiToggle);

        JLabel zoomLbl = new JLabel("Zoom:");
        zoomLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        zoomLbl.setForeground(new Color(100, 116, 139));
        right.add(zoomLbl);

        zoomPctLabel = new JLabel("100%");
        zoomPctLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        zoomPctLabel.setForeground(new Color(71, 85, 105));
        zoomPctLabel.setPreferredSize(new Dimension(34, 16));

        zoomSlider = new JSlider(50, 200, 100);
        zoomSlider.setOpaque(false);
        zoomSlider.setPreferredSize(new Dimension(110, 20));
        zoomSlider.addChangeListener(e -> {
            int pct = zoomSlider.getValue();
            double factor = pct / 100.0;
            canvasPanel.setZoom(factor);
            zoomPctLabel.setText(pct + "%");
        });
        right.add(zoomSlider);
        right.add(zoomPctLabel);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    @Override
    public void toggleAiPanel() {
        if (aiPanelVisible && "AI".equals(activeRightPanel)) {
            aiPanelVisible = false;
            rightDeckPanel.setVisible(false);
            activeRightPanel = null;
            rightSplit.setDividerSize(0);
            rightSplit.setDividerLocation(1.0);
        } else {
            aiPanelVisible = true;
            shapeFormatVisible = false;
            CardLayout cl = (CardLayout) rightDeckPanel.getLayout();
            cl.show(rightDeckPanel, "AI");
            rightDeckPanel.setVisible(true);
            activeRightPanel = "AI";
            rightSplit.setDividerSize(5);
            int totalW = rightSplit.getWidth();
            if (totalW <= 0) totalW = getWidth() - 250;
            int maxAllowedAiW = Math.max(280, (int) (totalW * 0.42));
            int targetAiW = Math.min(maxAllowedAiW, Math.max(280, Math.min(400, (int) (totalW * 0.33))));
            rightSplit.setDividerLocation(totalW - targetAiW);
        }
        rightSplit.revalidate();
        rightSplit.repaint();
    }

    public void showShapeFormatPanel(Element elem) {
        if (shapeFormatSidePanel == null) return;
        shapeFormatSidePanel.loadElement(elem);
        CardLayout cl = (CardLayout) rightDeckPanel.getLayout();
        cl.show(rightDeckPanel, "SHAPE_FORMAT");
        rightDeckPanel.setVisible(true);
        activeRightPanel = "SHAPE_FORMAT";
        shapeFormatVisible = true;
        aiPanelVisible = false;
        rightSplit.setDividerSize(5);
        int totalW = rightSplit.getWidth();
        if (totalW <= 0) totalW = getWidth() - 250;
        int maxAllowedFormatW = Math.max(280, (int) (totalW * 0.42));
        int targetW = Math.min(maxAllowedFormatW, Math.max(280, Math.min(380, (int) (totalW * 0.32))));
        rightSplit.setDividerLocation(totalW - targetW);
        rightSplit.revalidate();
        rightSplit.repaint();
    }

    public void hideShapeFormatPanel() {
        shapeFormatVisible = false;
        if ("SHAPE_FORMAT".equals(activeRightPanel)) {
            rightDeckPanel.setVisible(false);
            activeRightPanel = null;
            rightSplit.setDividerSize(0);
            rightSplit.setDividerLocation(1.0);
            rightSplit.revalidate();
            rightSplit.repaint();
        }
    }

    @Override
    public void toggleShapeFormatPanel() {
        if (shapeFormatVisible && "SHAPE_FORMAT".equals(activeRightPanel)) {
            hideShapeFormatPanel();
        } else {
            Element elem = canvasPanel.getSelectedMediaElement();
            if (elem == null) {
                int pos = canvasPanel.getTextPane().getCaretPosition();
                HTMLDocument doc = (HTMLDocument) canvasPanel.getTextPane().getDocument();
                Element ch = doc.getCharacterElement(pos);
                elem = canvasPanel.findCardOrShapeAt(ch);
            }
            showShapeFormatPanel(elem);
        }
    }

    @Override
    public void showShapeFormatPanel() {
        Element elem = canvasPanel.getSelectedMediaElement();
        if (elem == null) {
            int pos = canvasPanel.getTextPane().getCaretPosition();
            HTMLDocument doc = (HTMLDocument) canvasPanel.getTextPane().getDocument();
            Element ch = doc.getCharacterElement(pos);
            elem = canvasPanel.findCardOrShapeAt(ch);
        }
        showShapeFormatPanel(elem);
    }

    public void showSettingsDialog() {
        SettingsDialog dlg = new SettingsDialog(this, migrationService, dbPath);
        dlg.setVisible(true);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        if (rootCardPanel != null) {
            rootCardPanel.setBackground(newTheme.background());
        }
        if (emptyStatePanel != null) {
            emptyStatePanel.setBackground(newTheme.background());
        }
        repaint();
    }

    public void openBook(String bookId) {
        this.activeBookId = bookId;
        sidebarPanel.loadBook(bookId);

        List<LessonDto> lessons = lessonService.getLessonsForBook(bookId);
        if (!lessons.isEmpty()) {
            openLesson(lessons.get(0).id());
        } else {
            showEmptyLessonState();
        }

        cardLayout.show(rootCardPanel, VIEW_STUDIO);
    }

    public void openLesson(String lessonId) {
        // Flush previous lesson if needed
        flushCurrentLesson();

        this.activeLessonId = lessonId;
        LessonDto lesson = lessonService.getLessonById(lessonId);
        sidebarPanel.setActiveLesson(lessonId);
        canvasPanel.loadHtml(lesson.html());

        updateStatsDisplay(lesson.wordCount(), lesson.charCount());
        if (centerCardLayout != null && centerContainer != null) {
            centerCardLayout.show(centerContainer, "CANVAS");
        }
    }

    public void backToShelf() {
        flushCurrentLesson();
        activeBookId = null;
        activeLessonId = null;
        bookshelfPanel.refreshBooks();
        cardLayout.show(rootCardPanel, VIEW_SHELF);
    }

    public void deleteLesson(String lessonId) {
        if (lessonId == null) return;
        boolean wasActive = lessonId.equals(activeLessonId);
        if (wasActive) {
            activeLessonId = null; // Prevent flushCurrentLesson from trying to update deleted lesson
        }
        try {
            lessonService.deleteLesson(lessonId);
        } catch (Exception ignored) {}

        if (activeBookId != null) {
            List<LessonDto> remaining = lessonService.getLessonsForBook(activeBookId);
            if (!remaining.isEmpty()) {
                if (wasActive) {
                    openLesson(remaining.get(0).id());
                } else {
                    sidebarPanel.refreshLessons();
                }
            } else {
                showEmptyLessonState();
            }
        }
    }

    private void showEmptyLessonState() {
        activeLessonId = null;
        updateStatsDisplay(0, 0);
        sidebarPanel.setActiveLesson(null);
        sidebarPanel.refreshLessons();
        if (centerCardLayout != null && centerContainer != null) {
            centerCardLayout.show(centerContainer, "EMPTY");
        }
    }

    private JPanel createEmptyLessonStatePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(ThemeManager.getAppBackground());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLbl = new JLabel(VectorIcons.create(VectorIcons.Type.DOCUMENT, 48, new Color(37, 99, 235)));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLbl);
        card.add(Box.createVerticalStrut(14));

        JLabel titleLbl = new JLabel("No Lessons in this Notebook");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(ThemeManager.getTextPrimary());
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(8));

        JLabel descLbl = new JLabel("<html><center style='color:#64748b;font-size:12px;line-height:1.45;width:340px;'>"
                + "All lessons have been deleted, or this notebook is empty.<br>Create a lesson to start writing, formatting, and embedding charts.</center></html>");
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(descLbl);
        card.add(Box.createVerticalStrut(18));

        JButton createBtn = new JButton("＋ Create New Lesson");
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        createBtn.setBackground(new Color(37, 99, 235));
        createBtn.setForeground(Color.WHITE);
        createBtn.setOpaque(true);
        createBtn.setBorderPainted(false);
        createBtn.setFocusPainted(false);
        createBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        createBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        createBtn.addActionListener(e -> {
            if (sidebarPanel != null) {
                sidebarPanel.promptNewLesson();
            }
        });
        card.add(createBtn);
        card.add(Box.createVerticalStrut(10));

        JLabel hintLbl = new JLabel("Shortcut: ⌘N / Ctrl+N");
        hintLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintLbl.setForeground(new Color(148, 163, 184));
        hintLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(hintLbl);

        p.add(card);
        return p;
    }

    private void flushCurrentLesson() {
        if (activeLessonId != null) {
            try {
                String html = canvasPanel.getDocumentHtml();
                int words = canvasPanel.getWordCount();
                int chars = canvasPanel.getCharCount();
                lessonService.updateLesson(activeLessonId, new LessonUpdateRequest(null, html, words, chars, null));
            } catch (Exception ignored) {
                // Ignore if lesson was deleted or unavailable
            }
        }
    }

    private void onDocumentContentChanged(String html, int words, int chars) {
        if (activeLessonId == null) return;
        final String targetId = activeLessonId;

        statusSyncLabel.setIcon(VectorIcons.createIcon(VectorIcons.REFRESH, 13, new Color(217, 119, 6)));
        statusSyncLabel.setText("Saving locally...");
        statusSyncLabel.setForeground(new Color(217, 119, 6));

        updateStatsDisplay(words, chars);

        autoSaveExecutor.submit(() -> {
            try {
                lessonService.updateLesson(targetId, new LessonUpdateRequest(null, html, words, chars, null));
                SwingUtilities.invokeLater(() -> {
                    if (targetId.equals(activeLessonId)) {
                        statusSyncLabel.setIcon(VectorIcons.createIcon(VectorIcons.CHECKMARK, 13, new Color(22, 163, 74)));
                        statusSyncLabel.setText("Saved locally");
                        statusSyncLabel.setForeground(new Color(22, 163, 74));
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (targetId.equals(activeLessonId)) {
                        if (e instanceof com.wordbook.core.domain.exception.EntityNotFoundException) {
                            return; // Lesson was deleted concurrently
                        }
                        statusSyncLabel.setIcon(null);
                        statusSyncLabel.setText("Save error: " + e.getMessage());
                        statusSyncLabel.setForeground(new Color(220, 38, 38));
                    }
                });
            }
        });
    }

    private void updateStatsDisplay(int words, int chars) {
        int pages = canvasPanel != null ? canvasPanel.getPageCount() : 1;
        statusWordsLabel.setText("Page 1 of " + pages + "  •  " + words + (words == 1 ? " word" : " words"));
        statusCharsLabel.setText(chars + (chars == 1 ? " character" : " characters"));
    }

    // --- RibbonPanel.DocumentActionTarget Implementations ---

    @Override
    public void execFormat(String command, Object arg) {
        canvasPanel.applyFormat(command, arg);
    }

    @Override
    public void insertHtml(String html) {
        canvasPanel.insertHtml(html);
    }

    @Override
    public void insertText(String text) {
        canvasPanel.insertText(text);
    }

    @Override
    public void showTableDialog() {
        TableDialog dlg = new TableDialog(this);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            canvasPanel.insertHtml(dlg.generateTableHtml());
        }
    }

    @Override
    public void showChartDialog() {
        ChartDialog dlg = new ChartDialog(this, chartEngineService);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            canvasPanel.insertHtml(dlg.generateChartHtml());
            File f = dlg.getGeneratedChartFile();
            if (f != null && f.exists()) {
                canvasPanel.autoSelectChartFile(f);
            }
        }
    }

    @Override
    public void exportHtml() {
        if (activeLessonId == null) {
            ModernDialog.showMessage(this, "Export Document", "Select a lesson first to export.", ModernDialog.MessageType.WARNING);
            return;
        }
        flushCurrentLesson();
        var export = exportService.exportLessonAsHtml(activeLessonId);

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(export.filename()));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(chooser.getSelectedFile().toPath(), export.content());
                ModernDialog.showMessage(this, "Export Success", "Document successfully exported as HTML!", ModernDialog.MessageType.SUCCESS);
            } catch (Exception ex) {
                ModernDialog.showMessage(this, "Export Error", "Export failed: " + ex.getMessage(), ModernDialog.MessageType.ERROR);
            }
        }
    }

    @Override
    public void exportMarkdown() {
        if (activeLessonId == null) {
            ModernDialog.showMessage(this, "Export Document", "Select a lesson first to export.", ModernDialog.MessageType.WARNING);
            return;
        }
        flushCurrentLesson();
        var export = exportService.exportLessonAsMarkdown(activeLessonId);

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(export.filename()));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(chooser.getSelectedFile().toPath(), export.content());
                ModernDialog.showMessage(this, "Export Success", "Document successfully exported as Markdown!", ModernDialog.MessageType.SUCCESS);
            } catch (Exception ex) {
                ModernDialog.showMessage(this, "Export Error", "Export failed: " + ex.getMessage(), ModernDialog.MessageType.ERROR);
            }
        }
    }

    @Override
    public void printDocument() {
        try {
            boolean complete = canvasPanel.getTextPane().print();
            if (complete) {
                ModernDialog.showMessage(this, "Print & PDF", "Document printed / saved successfully.", ModernDialog.MessageType.SUCCESS);
            }
        } catch (Exception ex) {
            ModernDialog.showMessage(this, "Print Error", "Print error: " + ex.getMessage(), ModernDialog.MessageType.ERROR);
        }
    }

    @Override
    public void showDbDiagnostics() {
        DatabaseDiagnosticsDialog dlg = new DatabaseDiagnosticsDialog(this, migrationService, dbPath);
        dlg.setVisible(true);
    }

    @Override
    public void insertLocalImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Image to Insert");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (PNG, JPG, SVG, WEBP, GIF)", "png", "jpg", "jpeg", "svg", "webp", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                String url = f.toURI().toURL().toExternalForm();
                int maxW = canvasPanel.getMaxPrintableWidth();
                if (maxW <= 0) maxW = 580;
                int targetW = maxW;
                int targetH = 340;
                try {
                    java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(f);
                    if (bimg != null && bimg.getWidth() > 0 && bimg.getHeight() > 0) {
                        int origW = bimg.getWidth();
                        int origH = bimg.getHeight();
                        if (origW <= maxW) {
                            targetW = origW;
                            targetH = origH;
                        } else {
                            targetW = maxW;
                            targetH = (int) Math.round((double) origH * targetW / origW);
                        }
                    }
                } catch (Exception ignored) {}

                String html = "<p align='center' style='text-align:center;margin:16px auto;'>" +
                        "<img src='" + url + "' width='" + targetW + "' height='" + targetH + "' style='border:1px solid #cbd5e1;border-radius:8px;'><br>" +
                        "<span style='font-size:12px;color:#64748b;font-style:italic;'>Figure: " + f.getName() + "</span>" +
                        "</p><p><br></p>";
                canvasPanel.insertHtml(html);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void insertWebImage() {
        String url = ModernDialog.showInput(this, "Insert Web Image", "Enter Web Image URL (https://...):", "https://");
        if (url != null && !url.isBlank()) {
            int maxW = canvasPanel.getMaxPrintableWidth();
            if (maxW <= 0) maxW = 580;
            String html = "<p align='center' style='text-align:center;margin:16px auto;'>" +
                    "<img src='" + url.trim() + "' width='" + maxW + "' style='border:1px solid #cbd5e1;border-radius:8px;'><br>" +
                    "<span style='font-size:12px;color:#64748b;font-style:italic;'>Web Image</span>" +
                    "</p><p><br></p>";
            canvasPanel.insertHtml(html);
        }
    }

    @Override
    public void insertQuickChart(String typeStr, String data) {
        insertQuickChart(typeStr, data, "Corporate Blue", 580, 290);
    }

    @Override
    public void insertQuickChart(String typeStr, String data, String themeStr, int width, int height) {
        ChartType type = ChartCatalog.parse(typeStr);
        ChartTheme theme = ChartCatalog.parseTheme(themeStr);

        int maxW = canvasPanel.getMaxPrintableWidth();
        int reqW = width > 100 ? width : 580;
        int reqH = height > 50 ? height : 290;
        int w = Math.min(reqW, maxW);
        int h = (int) Math.round(reqH * ((double) w / reqW));

        try {
            File f = chartEngineService.renderChartToFile(type, data, theme, typeStr + " Chart", w, h);
            String url = f.toURI().toURL().toExternalForm();
            String html = "<p align='center' style='text-align:center;margin:16px auto;'><img src='" + url + "' width='" + w + "' height='" + h + "'><br><span style='font-size:11px;color:#64748b;font-style:italic;'>Figure: " + typeStr + " Chart (" + theme.getDisplayName() + ")</span></p><p><br></p>";
            canvasPanel.insertHtml(html);
            canvasPanel.autoSelectChartFile(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getWordCount() {
        return canvasPanel.getWordCount();
    }

    @Override
    public int getCharCount() {
        return canvasPanel.getCharCount();
    }

    @Override
    public void backToBookshelf() {
        backToShelf();
    }

    @Override
    public void insertPageBreak() {
        canvasPanel.insertPageBreak();
    }

    @Override
    public void showCitationDialog() {
        CitationDialog dlg = new CitationDialog(this, citationEngine);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            canvasPanel.insertHtml(dlg.generateHtmlToInsert());
        }
    }

    @Override
    public void showGrammarDialog() {
        canvasPanel.runGrammarCheck(grammarEngine);
    }

    @Override
    public void autoDetectStructure() {
        canvasPanel.autoDetectStructure(structureDetector);
    }

    @Override
    public void resizeSelectedImage(int widthPx) {
        var elem = canvasPanel.getLastClickedImageElement();
        if (elem != null) canvasPanel.resizeImageElement(elem, widthPx);
        else canvasPanel.openImagePropertiesDialog();
    }

    @Override
    public void stepSelectedImageSize(int deltaPx) {
        var elem = canvasPanel.getLastClickedImageElement();
        if (elem != null) {
            int currentW = canvasPanel.getImageElementWidth(elem);
            if (currentW <= 0) currentW = 580;
            int newW = Math.max(120, Math.min(1100, currentW + deltaPx));
            canvasPanel.resizeImageElement(elem, newW);
        } else {
            canvasPanel.openImagePropertiesDialog();
        }
    }

    @Override
    public void alignSelectedImage(String align) {
        var elem = canvasPanel.getLastClickedImageElement();
        if (elem != null) canvasPanel.alignImageElement(elem, align);
        else canvasPanel.applyFormat("align-" + align, null);
    }

    @Override
    public void showImagePropertiesDialog() {
        canvasPanel.openImagePropertiesDialog();
    }

    @Override
    public void insertCard(String cardType) {
        String html = switch (cardType) {
            case "KPI" -> "<table class='wb-card' data-card='kpi' data-theme='corporate' align='center' cellpadding='14' cellspacing='0' style='background-color:#f8fafc;border:1px solid #cbd5e1;border-radius:12px;margin:16px auto;width:340px;'>" +
                    "<tr><td>" +
                    "<div class='wb-card-sub' style='font-size:11px;font-weight:bold;color:#64748b;letter-spacing:0.5px;'>KEY PERFORMANCE METRIC</div>" +
                    "<div class='wb-card-title' style='font-size:32px;font-weight:bold;color:#0f172a;margin:4px 0;'>$128,450</div>" +
                    "<div><span style='background-color:#dcfce7;color:#15803d;font-size:11px;font-weight:bold;padding:2px 8px;border-radius:10px;'>▲ +18.4%</span> <span style='font-size:12px;color:#64748b;'>vs previous quarter</span></div>" +
                    "</td></tr></table><p><br></p>";

            case "MultiKPI", "3KPI" -> "<table class='wb-card' data-card='multikpi' data-theme='corporate' align='center' cellpadding='14' cellspacing='10' style='width:100%;margin:16px auto;'>" +
                    "<tr>" +
                    "<td style='background-color:#f8fafc;border:1px solid #cbd5e1;border-radius:12px;width:33%;vertical-align:top;padding:14px;'>" +
                    "<div class='wb-card-sub' style='font-size:11px;font-weight:bold;color:#64748b;'>ANNUAL REVENUE</div>" +
                    "<div class='wb-card-title' style='font-size:24px;font-weight:bold;color:#0f172a;margin:3px 0;'>$1.42M</div>" +
                    "<span style='background-color:#dcfce7;color:#15803d;font-size:10px;font-weight:bold;padding:2px 6px;border-radius:6px;'>▲ +24.2% YoY</span>" +
                    "</td>" +
                    "<td style='background-color:#f8fafc;border:1px solid #cbd5e1;border-radius:12px;width:33%;vertical-align:top;padding:14px;'>" +
                    "<div class='wb-card-sub' style='font-size:11px;font-weight:bold;color:#64748b;'>ACTIVE USERS</div>" +
                    "<div class='wb-card-title' style='font-size:24px;font-weight:bold;color:#0f172a;margin:3px 0;'>84,200</div>" +
                    "<span style='background-color:#dbeafe;color:#1d4ed8;font-size:10px;font-weight:bold;padding:2px 6px;border-radius:6px;'>▲ +12.5% MoM</span>" +
                    "</td>" +
                    "<td style='background-color:#f8fafc;border:1px solid #cbd5e1;border-radius:12px;width:33%;vertical-align:top;padding:14px;'>" +
                    "<div class='wb-card-sub' style='font-size:11px;font-weight:bold;color:#64748b;'>NET RETENTION</div>" +
                    "<div class='wb-card-title' style='font-size:24px;font-weight:bold;color:#0f172a;margin:3px 0;'>118.5%</div>" +
                    "<span style='background-color:#fef3c7;color:#b45309;font-size:10px;font-weight:bold;padding:2px 6px;border-radius:6px;'>★ Top Tier</span>" +
                    "</td>" +
                    "</tr></table><p><br></p>";

            case "Topic", "Spotlight" -> "<table class='wb-card' data-card='spotlight' data-theme='corporate' align='center' cellpadding='16' cellspacing='0' style='background:#f8fafc;border-left:5px solid #2563eb;border-top:1px solid #cbd5e1;border-right:1px solid #cbd5e1;border-bottom:1px solid #cbd5e1;border-radius:10px;margin:16px auto;width:100%;'>" +
                    "<tr><td>" +
                    "<span style='background-color:#2563eb;color:#ffffff;font-size:10px;font-weight:bold;padding:3px 10px;border-radius:12px;'>SPOTLIGHT TOPIC</span>" +
                    "<h2 class='wb-card-title' style='color:#0f172a;margin:10px 0 6px 0;font-size:18px;'>Core System Topology & Invariants</h2>" +
                    "<p class='wb-card-sub' style='color:#475569;margin:4px 0 10px 0;font-size:13px;'>Detailed exploration of decoupled service layers, reactive undo, and embedded local SQLite WAL transactions.</p>" +
                    "<ul style='margin:6px 0;padding-left:20px;color:#334155;font-size:13px;'><li>Event-driven architecture with zero memory leaks</li><li>Sub-millisecond local query execution without network reliance</li></ul>" +
                    "</td></tr></table><p><br></p>";

            case "Summary", "Takeaways" -> "<table class='wb-card' data-card='summary' data-theme='emerald' align='center' cellpadding='16' cellspacing='0' style='background-color:#f0fdf4;border:1px solid #86efac;border-radius:10px;margin:16px auto;width:100%;'>" +
                    "<tr><td>" +
                    "<b class='wb-card-title' style='color:#166534;font-size:14px;'>📌 Executive Key Takeaways</b>" +
                    "<ul class='wb-card-sub' style='margin:8px 0;padding-left:20px;color:#14532d;font-size:13px;'>" +
                    "<li><b>Milestone Delivered:</b> Completed ahead of schedule with all invariant checks validated.</li>" +
                    "<li><b>Data Integrity:</b> 100% of end-to-end tests passing without regressions.</li>" +
                    "<li><b>Next Objective:</b> Deploy canary release to production environment.</li>" +
                    "</ul>" +
                    "</td></tr></table><p><br></p>";

            case "Comparison" -> "<table class='wb-card' data-card='comparison' data-theme='corporate' align='center' cellpadding='14' cellspacing='0' style='background:#f8fafc;border:1px solid #cbd5e1;border-radius:12px;margin:16px auto;width:100%;'>" +
                    "<tr>" +
                    "<td style='width:50%;border-right:1px solid #e2e8f0;padding:16px;vertical-align:top;'>" +
                    "<span style='background:#fee2e2;color:#b91c1c;font-size:10px;font-weight:bold;padding:3px 8px;border-radius:6px;'>TRADITIONAL ARCHITECTURE</span>" +
                    "<h3 style='color:#0f172a;margin:8px 0;'>Legacy Centralized</h3>" +
                    "<ul style='color:#475569;margin:8px 0;padding-left:18px;font-size:13px;'><li>External cloud vendor lock-in</li><li>Network roundtrip latency</li><li>Recurring infrastructure fees</li></ul>" +
                    "</td>" +
                    "<td style='width:50%;padding:16px;vertical-align:top;'>" +
                    "<span style='background:#dcfce7;color:#15803d;font-size:10px;font-weight:bold;padding:3px 8px;border-radius:6px;'>NOTEDOWN HYBRID</span>" +
                    "<h3 style='color:#0f172a;margin:8px 0;'>Embedded Zero-Latency WAL</h3>" +
                    "<ul style='color:#475569;margin:8px 0;padding-left:18px;font-size:13px;'><li>Instant SQLite local ACID transactions</li><li>100% offline & zero cloud dependency</li><li>Zero monthly subscription cost</li></ul>" +
                    "</td>" +
                    "</tr></table><p><br></p>";

            case "Progress" -> "<table class='wb-card' data-card='progress' data-theme='corporate' align='center' cellpadding='14' cellspacing='0' style='background-color:#f8fafc;border:1px solid #cbd5e1;border-radius:12px;margin:16px auto;width:380px;'><tr><td>" +
                    "<div class='wb-card-title' style='font-size:14px;font-weight:bold;color:#0f172a;'>Sprint Milestone: Phase 2 Delivery</div>" +
                    "<div class='wb-card-sub' style='font-size:12px;color:#64748b;margin-bottom:8px;'>Target Completion: 80% &bull; 16 of 20 tasks done</div>" +
                    "<table align='center' cellpadding='0' cellspacing='0' style='width:100%;height:10px;background-color:#e2e8f0;border-radius:5px;'>" +
                    "<tr><td style='width:80%;background-color:#2563eb;border-radius:5px;'></td><td></td></tr></table>" +
                    "</td></tr></table><p><br></p>";

            default -> "<div class='wb-card' data-card='general' data-theme='corporate' style='background-color:#f8fafc;border:1px solid #cbd5e1;padding:12px;margin:12px auto;border-radius:8px;max-width:580px;'><div class='wb-card-title' style='font-weight:bold;color:#0f172a;'>Card Content</div></div><p><br></p>";
        };
        canvasPanel.insertHtml(html);
    }

    @Override
    public void setPageFormat(String formatName) {
        if (formatName == null) formatName = "A4";
        DocumentCanvasPanel.PageFormat fmt;
        if (formatName.contains("Letter")) fmt = DocumentCanvasPanel.PageFormat.LETTER;
        else if (formatName.contains("Legal")) fmt = DocumentCanvasPanel.PageFormat.LEGAL;
        else if (formatName.contains("Compact")) fmt = DocumentCanvasPanel.PageFormat.COMPACT;
        else if (formatName.contains("Wide")) fmt = DocumentCanvasPanel.PageFormat.WIDE;
        else fmt = DocumentCanvasPanel.PageFormat.A4;
        canvasPanel.setPageFormat(fmt);
    }

    @Override
    public void setPageMargins(String marginName) {
        if (marginName == null) marginName = "Normal";
        DocumentCanvasPanel.PageMargin margin;
        if (marginName.contains("Narrow")) margin = DocumentCanvasPanel.PageMargin.NARROW;
        else if (marginName.contains("Wide")) margin = DocumentCanvasPanel.PageMargin.WIDE;
        else margin = DocumentCanvasPanel.PageMargin.NORMAL;
        canvasPanel.setPageMargin(margin);
    }

    @Override
    public void setPagePaperTone(String toneName) {
        canvasPanel.setPagePaperTone(toneName);
    }

    @Override
    public void setPaperPattern(String pattern) {
        canvasPanel.setPaperPattern(pattern);
    }

    @Override
    public void setRuleSpacing(int spacing) {
        canvasPanel.setRuleSpacing(spacing);
    }

    @Override
    public void autoFixAllGrammar() {
        canvasPanel.autoFixAllGrammar();
    }

    @Override
    public void replaceSelectedImageSource() {
        canvasPanel.replaceSelectedImageSource();
    }

    @Override
    public void insertTableRowAbove() {
        canvasPanel.insertTableRowAbove();
    }

    @Override
    public void insertTableRowBelow() {
        canvasPanel.insertTableRowBelow();
    }

    @Override
    public void insertTableColumnLeft() {
        canvasPanel.insertTableColumnLeft();
    }

    @Override
    public void insertTableColumnRight() {
        canvasPanel.insertTableColumnRight();
    }

    @Override
    public void deleteTableRow() {
        canvasPanel.deleteTableRow();
    }

    @Override
    public void deleteTableColumn() {
        canvasPanel.deleteTableColumn();
    }

    @Override
    public void deleteTable() {
        canvasPanel.deleteTable();
    }

    @Override
    public void applyTableStyle(String styleTheme) {
        canvasPanel.applyTableStyle(styleTheme);
    }

    @Override
    public void setTableCellColor(Color color) {
        canvasPanel.setTableCellColor(color);
    }

    @Override
    public void insertShape(String shapeType) {
        canvasPanel.insertShape(shapeType);
    }

    @Override
    public void showVideoDialog() {
        VideoDialog dlg = new VideoDialog(this);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            canvasPanel.insertHtml(dlg.generateVideoHtml());
        }
    }

    @Override
    public void openSelectedVideo() {
        canvasPanel.openSelectedVideo();
    }

    @Override
    public void deleteSelectedVideo() {
        canvasPanel.deleteSelectedVideo();
    }

    @Override
    public AdvancedUndoManager getAdvancedUndoManager() {
        return canvasPanel != null ? canvasPanel.getAdvancedUndoManager() : null;
    }

    @Override
    public void openAiTab(int index) {
        if (!aiPanelVisible) toggleAiPanel();
        aiSidePanel.selectTab(index);
    }

    @Override
    public void openScraperWithUrl(String url) {
        if (!aiPanelVisible) toggleAiPanel();
        aiSidePanel.openScraperWithUrl(url);
    }

    @Override
    public void speakSelectionOrDocument() {
        canvasPanel.speakSelectionOrDocument();
    }

    @Override
    public void toggleHighContrastMode() {
        canvasPanel.toggleHighContrastMode();
    }

    @Override
    public void toggleDyslexicFont() {
        canvasPanel.toggleDyslexicFont();
    }

    @Override
    public void showHeadingsNavigator() {
        canvasPanel.showHeadingsNavigator();
    }

    @Override
    public void runAccessibilityAudit() {
        canvasPanel.runAccessibilityAudit();
    }

    @Override
    public void applyImageFilter(String filter) {
        canvasPanel.applyImageFilter(filter);
    }

    @Override
    public void setImageWrap(String wrap) {
        canvasPanel.setImageWrap(wrap);
    }

    @Override
    public void showFindDialog() {
        if (canvasPanel != null && canvasPanel.getTextPane() != null) {
            FindReplaceDialog dlg = new FindReplaceDialog(this, canvasPanel.getTextPane(), false);
            dlg.setVisible(true);
        }
    }

    @Override
    public void showReplaceDialog() {
        if (canvasPanel != null && canvasPanel.getTextPane() != null) {
            FindReplaceDialog dlg = new FindReplaceDialog(this, canvasPanel.getTextPane(), true);
            dlg.setVisible(true);
        }
    }

    @Override
    public void showWordCountDialog() {
        int words = getWordCount();
        int chars = getCharCount();
        int readTime = Math.max(1, (int) Math.ceil(words / 200.0));

        JDialog dlg = new JDialog(this, "Document Statistics", true);
        dlg.setSize(380, 260);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        content.setBackground(ThemeManager.getSurface());

        JLabel titleLbl = new JLabel("Word & Readability Count", VectorIcons.create(VectorIcons.Type.WORD_COUNT, 18, new Color(37, 99, 235)), SwingConstants.LEFT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(ThemeManager.getTextPrimary());
        content.add(titleLbl, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 8));
        grid.setOpaque(false);

        grid.add(createStatLabel("Total Words:"));
        grid.add(createStatValue(String.format("%,d", words)));
        grid.add(createStatLabel("Total Characters:"));
        grid.add(createStatValue(String.format("%,d", chars)));
        grid.add(createStatLabel("Reading Time:"));
        grid.add(createStatValue("~" + readTime + " min"));
        grid.add(createStatLabel("Average Word Length:"));
        double avg = words > 0 ? (double) chars / words : 0;
        grid.add(createStatValue(String.format("%.1f characters", avg)));

        content.add(grid, BorderLayout.CENTER);

        JButton okBtn = new JButton("OK");
        okBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        okBtn.setPreferredSize(new Dimension(80, 28));
        okBtn.addActionListener(e -> dlg.dispose());
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnBar.setOpaque(false);
        btnBar.add(okBtn);
        content.add(btnBar, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    private JLabel createStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(ThemeManager.getTextSecondary());
        return l;
    }

    private JLabel createStatValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(ThemeManager.getTextPrimary());
        return l;
    }

    private void setupGlobalKeyBindings() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Save: Ctrl/Cmd + S
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey), "doc-save");
        am.put("doc-save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (canvasPanel != null) {
                    canvasPanel.triggerSave();
                    pulseSyncStatus("Saved ✓");
                }
            }
        });

        // New Lesson: Ctrl/Cmd + N
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKey), "lesson-new");
        am.put("lesson-new", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (sidebarPanel != null) {
                    sidebarPanel.promptNewLesson();
                }
            }
        });

        // Open Bookshelf: Ctrl/Cmd + O
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKey), "doc-open");
        am.put("doc-open", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                backToShelf();
            }
        });

        // Find: Ctrl/Cmd + F
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutKey), "doc-find");
        am.put("doc-find", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showFindDialog();
            }
        });

        // Replace: Ctrl/Cmd + H
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutKey), "doc-replace");
        am.put("doc-replace", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showReplaceDialog();
            }
        });

        // Bold: Ctrl/Cmd + B
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutKey), "format-bold");
        am.put("format-bold", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                execFormat("bold", null);
            }
        });

        // Italic: Ctrl/Cmd + I
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutKey), "format-italic");
        am.put("format-italic", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                execFormat("italic", null);
            }
        });

        // Underline: Ctrl/Cmd + U
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, shortcutKey), "format-underline");
        am.put("format-underline", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                execFormat("underline", null);
            }
        });

        // Undo: Ctrl/Cmd + Z
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutKey), "format-undo");
        am.put("format-undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                execFormat("undo", null);
            }
        });

        // Redo: Ctrl/Cmd + Y or Ctrl/Cmd + Shift + Z
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutKey), "format-redo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutKey | KeyEvent.SHIFT_DOWN_MASK), "format-redo");
        am.put("format-redo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                execFormat("redo", null);
            }
        });

        // Print: Ctrl/Cmd + P
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, shortcutKey), "doc-print");
        am.put("doc-print", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                printDocument();
            }
        });

        // Toggle AI Panel: Ctrl/Cmd + Shift + A
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcutKey | KeyEvent.SHIFT_DOWN_MASK), "toggle-ai");
        am.put("toggle-ai", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleAiPanel();
            }
        });

        // Scraper: Ctrl/Cmd + Shift + S
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey | KeyEvent.SHIFT_DOWN_MASK), "open-scraper");
        am.put("open-scraper", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openScraperWithUrl("");
            }
        });

        // Fullscreen Toggle: F11
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggle-fullscreen");
        am.put("toggle-fullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullscreen();
            }
        });

        // Zoom In: Ctrl/Cmd + EQUALS, Ctrl/Cmd + PLUS
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutKey), "zoom-in");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, shortcutKey), "zoom-in");
        am.put("zoom-in", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (zoomSlider != null) {
                    zoomSlider.setValue(Math.min(200, zoomSlider.getValue() + 10));
                }
            }
        });

        // Zoom Out: Ctrl/Cmd + MINUS
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutKey), "zoom-out");
        am.put("zoom-out", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (zoomSlider != null) {
                    zoomSlider.setValue(Math.max(50, zoomSlider.getValue() - 10));
                }
            }
        });

        // Reset Zoom: Ctrl/Cmd + 0
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, shortcutKey), "zoom-reset");
        am.put("zoom-reset", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (zoomSlider != null) {
                    zoomSlider.setValue(100);
                }
            }
        });
    }

    private void pulseSyncStatus(String text) {
        if (statusSyncLabel != null) {
            statusSyncLabel.setText(text);
            statusSyncLabel.setForeground(new Color(37, 99, 235));
            Timer t = new Timer(1800, ev -> {
                statusSyncLabel.setText("Saved");
                statusSyncLabel.setForeground(new Color(22, 163, 74));
            });
            t.setRepeats(false);
            t.start();
        }
    }

    private void toggleFullscreen() {
        int state = getExtendedState();
        if ((state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
            setExtendedState(JFrame.NORMAL);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    // Contextual Chart Design Operations
    @Override
    public void modifySelectedChartData() {
        canvasPanel.modifySelectedChartData();
    }

    @Override
    public void switchSelectedChartType(String typeStr) {
        canvasPanel.switchSelectedChartType(typeStr);
        ribbonPanel.setSelectedChartType(ChartCatalog.parse(typeStr));
    }

    @Override
    public void switchSelectedChartTheme(String themeStr) {
        canvasPanel.switchSelectedChartTheme(themeStr);
        ribbonPanel.setSelectedChartTheme(themeStr);
    }

    @Override
    public void switchSelectedChartStyle(String styleStr) {
        canvasPanel.switchSelectedChartStyle(styleStr);
        ribbonPanel.setSelectedChartDesignStyle(styleStr);
    }

    @Override
    public void setChartDetailLevel(String detailLevel) {
        canvasPanel.setChartDetailLevel(detailLevel);
    }

    @Override
    public void toggleChartValues() {
        canvasPanel.toggleChartValues();
    }

    @Override
    public void toggleChartLegend() {
        canvasPanel.toggleChartLegend();
    }

    @Override
    public void reRenderSelectedChart() {
        canvasPanel.reRenderSelectedChart();
    }

    @Override
    public void showChartPaletteDialog() {
        canvasPanel.showChartPaletteDialog();
    }

    @Override
    public void setHeaderStyle(String headerStyle) {
        canvasPanel.setHeaderStyle(headerStyle);
    }

    @Override
    public void setFooterStyle(String footerStyle) {
        canvasPanel.setFooterStyle(footerStyle);
    }

    @Override
    public void setHeaderLeftText(String text) {
        canvasPanel.setHeaderLeftText(text);
    }

    @Override
    public void setHeaderRightText(String text) {
        canvasPanel.setHeaderRightText(text);
    }

    @Override
    public void closeHeaderFooterSelection() {
        canvasPanel.closeHeaderFooterSelection();
    }

    @Override
    public void setHeaderFooterSelected(boolean selected, String section, int pageIndex) {
        canvasPanel.setHeaderFooterSelected(selected, section, pageIndex);
    }

    @Override
    public boolean isHeaderFooterSelected() {
        return canvasPanel.isHeaderFooterSelected();
    }

    @Override
    public void showCardShapeCustomizerDialog() {
        canvasPanel.showCardShapeCustomizerDialog();
    }

    @Override
    public void applyActiveCardShapeTheme(String themeKey) {
        canvasPanel.applyActiveCardShapeTheme(themeKey);
    }

    @Override
    public void alignSelectedCardShape(String align) {
        canvasPanel.alignSelectedCardShape(align);
    }

    @Override
    public void deleteSelectedCardShape() {
        canvasPanel.deleteSelectedCardShape();
    }

    public JScrollPane getCanvasScroll() {
        return canvasScroll;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public JSplitPane getRightSplit() {
        return rightSplit;
    }

    public JSlider getZoomSlider() {
        return zoomSlider;
    }
}
