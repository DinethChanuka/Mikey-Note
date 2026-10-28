package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.InternetScraperService;
import com.wordbook.infrastructure.web.ProxyRoutingManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI Side Panel — Fast-accessible tabs for Claude, ChatGPT, Gemini, DeepSeek,
 * and a built-in Internet Scraping & Research Engine.
 *
 * Uses JavaFX WebView (JFXPanel) when available for live in-app browsing,
 * and features a dedicated Internet Scraper with one-click document insertion.
 */
public class AiSidePanel extends JPanel {

    private static final int PANEL_MIN_W = 280;
    private static final int PANEL_MAX_W = 750;

    public record AiService(String id, String name, VectorIcons.Type iconType, Color accent, String url) {}

    public static final List<AiService> AI_SERVICES = List.of(
            new AiService("claude",     "Claude",     VectorIcons.Type.CLAUDE,     new Color(204, 86, 54),  "https://claude.ai"),
            new AiService("chatgpt",    "ChatGPT",    VectorIcons.Type.CHATGPT,    new Color(16, 163, 127), "https://chatgpt.com"),
            new AiService("gemini",     "Gemini",     VectorIcons.Type.GEMINI,     new Color(99, 102, 241), "https://gemini.google.com"),
            new AiService("deepseek",   "DeepSeek",   VectorIcons.Type.DEEPSEEK,   new Color(2, 132, 199),  "https://chat.deepseek.com"),
            new AiService("perplexity", "Perplexity", VectorIcons.Type.PERPLEXITY, new Color(13, 148, 136), "https://www.perplexity.ai"),
            new AiService("scraper",    "Scraper",    VectorIcons.Type.SCRAPER,    new Color(14, 165, 233), "https://en.wikipedia.org/wiki/Artificial_intelligence")
    );

    private JPanel tabBar;
    private final List<JButton> tabButtons = new ArrayList<>();
    private JPanel contentArea;
    private CardLayout contentCards;
    private int activeTabIndex = 0;

    private final Map<String, JComponent> webPanels = new LinkedHashMap<>();
    private final Map<String, Object> webEngines = new LinkedHashMap<>();
    private final InternetScraperService scraperService = new InternetScraperService();

    private Consumer<String> insertHtmlCallback;
    private Runnable closeCallback;

    // Resize handle state
    private int resizingStartX;
    private int panelStartW;
    private boolean resizing = false;

    // Scraper UI Components
    private JTextField scraperUrlField;
    private JButton scrapeBtn;
    private JLabel scraperStatusLbl;
    private JTextArea scraperOutputArea;
    private JButton insertScrapedBtn;
    private JButton copyScrapedBtn;
    private JComboBox<String> insertFormatCombo;
    private InternetScraperService.ScrapedArticle lastScrapedArticle = null;

    public AiSidePanel() {
        this(null);
    }

    public AiSidePanel(Consumer<String> insertHtmlCallback) {
        this.insertHtmlCallback = insertHtmlCallback;
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(248, 250, 252));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(203, 213, 225)),
                new EmptyBorder(0, 0, 0, 0)
        ));
        setPreferredSize(new Dimension(440, 640));
        setMinimumSize(new Dimension(PANEL_MIN_W, 160));

        buildTabBar();
        buildContent();
        buildResizeHandle();

        // Default to the intelligent native Scraper tab (instant, crash-proof native Swing)
        int defaultTab = 0;
        for (int i = 0; i < AI_SERVICES.size(); i++) {
            if ("scraper".equals(AI_SERVICES.get(i).id())) {
                defaultTab = i;
                break;
            }
        }
        selectTab(defaultTab);
        ThemeManager.addListener(t -> applyTheme(t));
    }

    private JSplitPane findAncestorSplitPane() {
        Container p = getParent();
        while (p != null) {
            if (p instanceof JSplitPane sp) return sp;
            p = p.getParent();
        }
        return null;
    }

    public void setInsertHtmlCallback(Consumer<String> callback) {
        this.insertHtmlCallback = callback;
    }

    public void setCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }

    private void buildTabBar() {
        if (tabBar == null) {
            tabBar = new JPanel(new BorderLayout(0, 6));
            add(tabBar, BorderLayout.NORTH);
        } else {
            tabBar.removeAll();
        }

        tabBar.setOpaque(true);
        tabBar.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));
        tabBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        // Header Row: Title & Action Pills
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLbl = new JLabel("✨ AI ASSISTANT & WEB RESEARCH");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLbl.setForeground(new Color(100, 116, 139));
        topHeader.add(titleLbl, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightControls.setOpaque(false);

        JButton widthToggleBtn = new JButton("⇲ Width") {
            @Override
            public Dimension getPreferredSize() { return new Dimension(68, 20); }
        };
        widthToggleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        widthToggleBtn.setFocusPainted(false);
        widthToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        widthToggleBtn.setToolTipText("Toggle between standard (400px) and wide (540px) side panel");
        widthToggleBtn.addActionListener(e -> {
            int currentW = getWidth();
            int newW = (currentW < 460) ? 540 : 380;
            setPreferredSize(new Dimension(newW, getHeight()));
            JSplitPane sp = findAncestorSplitPane();
            if (sp != null) {
                int maxAllowed = Math.max(PANEL_MIN_W, (int) (sp.getWidth() * 0.45));
                newW = Math.min(newW, maxAllowed);
                sp.setDividerLocation(sp.getWidth() - newW);
            }
            revalidate();
            repaint();
        });
        rightControls.add(widthToggleBtn);

        topHeader.add(rightControls, BorderLayout.EAST);
        tabBar.add(topHeader, BorderLayout.NORTH);

        // Segmented Tab Row: 5 equal columns using GridLayout to prevent wrapping/cutting
        JPanel tabsGrid = new JPanel(new GridLayout(1, AI_SERVICES.size(), 3, 0));
        tabsGrid.setOpaque(false);
        tabButtons.clear();

        for (int i = 0; i < AI_SERVICES.size(); i++) {
            AiService svc = AI_SERVICES.get(i);
            final int idx = i;
            JButton btn = createTabBtn(svc, idx);
            tabButtons.add(btn);
            tabsGrid.add(btn);
        }
        tabBar.add(tabsGrid, BorderLayout.CENTER);
        tabBar.revalidate();
        tabBar.repaint();
    }

    private JButton createTabBtn(AiService svc, int idx) {
        boolean active = (activeTabIndex == idx);
        JButton btn = new JButton(svc.name(), VectorIcons.create(svc.iconType(), 14, active ? Color.WHITE : svc.accent())) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isCur = (activeTabIndex == idx);
                if (isCur) {
                    g2.setColor(svc.accent());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    g2.setColor(ThemeManager.isDark() ? new Color(30, 41, 59) : new Color(226, 232, 240));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else {
                    g2.setColor(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(255, 255, 255));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(226, 232, 240));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setForeground(active ? Color.WHITE : (ThemeManager.isDark() ? new Color(226, 232, 240) : new Color(51, 65, 85)));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setToolTipText(svc.name() + " — " + svc.url());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 30));
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.addActionListener(e -> selectTab(idx));
        return btn;
    }

    private void buildContent() {
        contentCards = new CardLayout();
        contentArea = new JPanel(contentCards);
        contentArea.setOpaque(false);

        // Build Scraper panel upfront
        JComponent scraper = buildScraperPanel();
        webPanels.put("scraper", scraper);
        contentArea.add(scraper, "scraper");

        add(contentArea, BorderLayout.CENTER);
    }

    /**
     * Builds the dedicated Intelligent Internet Scraping Studio Tab.
     */
    private JComponent buildScraperPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Top Search / URL Bar + Presets
        JPanel topBox = new JPanel(new BorderLayout(0, 6));
        topBox.setOpaque(false);

        // Header Title
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel hdr = new JLabel("🌐 Intelligent Web Research Studio");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hdr.setForeground(new Color(13, 148, 136));
        titleRow.add(hdr, BorderLayout.WEST);

        JLabel subHdr = new JLabel("Reader-Mode Cleansing");
        subHdr.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        subHdr.setForeground(new Color(100, 116, 139));
        titleRow.add(subHdr, BorderLayout.EAST);
        topBox.add(titleRow, BorderLayout.NORTH);

        // URL Input Row
        JPanel urlRow = new JPanel(new BorderLayout(6, 0));
        urlRow.setOpaque(false);

        scraperUrlField = new JTextField("https://en.wikipedia.org/wiki/Artificial_intelligence");
        scraperUrlField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        scraperUrlField.setPreferredSize(new Dimension(240, 32));
        scraperUrlField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        urlRow.add(scraperUrlField, BorderLayout.CENTER);

        scrapeBtn = new JButton("⚡ Scrape & Analyze");
        scrapeBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        scrapeBtn.setBackground(new Color(13, 148, 136));
        scrapeBtn.setForeground(Color.WHITE);
        scrapeBtn.setOpaque(true);
        scrapeBtn.setBorderPainted(false);
        scrapeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        scrapeBtn.addActionListener(e -> executeScrape());
        urlRow.add(scrapeBtn, BorderLayout.EAST);
        topBox.add(urlRow, BorderLayout.CENTER);

        // Quick Preset Research Chips
        JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        chipsRow.setOpaque(false);
        chipsRow.add(createChip("📖 Wikipedia AI", "https://en.wikipedia.org/wiki/Artificial_intelligence"));
        chipsRow.add(createChip("📄 arXiv Attention", "https://arxiv.org/abs/1706.03762"));
        chipsRow.add(createChip("💻 GitHub Trending", "https://github.com/trending"));
        chipsRow.add(createChip("🌐 MDN Web", "https://developer.mozilla.org/en-US/"));
        chipsRow.add(createChip("📰 Tech News", "https://techcrunch.com/"));
        topBox.add(chipsRow, BorderLayout.SOUTH);

        p.add(topBox, BorderLayout.NORTH);

        // Center: Output / Extracted Content Area
        JPanel centerBox = new JPanel(new BorderLayout(0, 6));
        centerBox.setOpaque(false);

        scraperStatusLbl = new JLabel("Enter any URL above or click a research chip to scrape.");
        scraperStatusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        scraperStatusLbl.setForeground(new Color(100, 116, 139));
        centerBox.add(scraperStatusLbl, BorderLayout.NORTH);

        scraperOutputArea = new JTextArea();
        scraperOutputArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        scraperOutputArea.setLineWrap(true);
        scraperOutputArea.setWrapStyleWord(true);
        scraperOutputArea.setMargin(new Insets(10, 10, 10, 10));
        scraperOutputArea.setText("Intelligent Web Research Studio Ready.\n\n" +
                "• Reader-Mode Isolation: Automatically extracts article body, stripping ads, trackers & clutter.\n" +
                "• Table Extraction: Converts web data tables into clean document tables.\n" +
                "• AI Summarization: Extracts Title, Abstract, Headings & Key Takeaways.\n" +
                "• 1-Click Insertion: Embed as a Modern Research Card, Executive Brief, or Tables.");

        JScrollPane scroll = new JScrollPane(scraperOutputArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true));
        centerBox.add(scroll, BorderLayout.CENTER);

        p.add(centerBox, BorderLayout.CENTER);

        // Bottom Action Toolbar with Format Selector
        JPanel bottomBox = new JPanel(new BorderLayout(0, 4));
        bottomBox.setOpaque(false);

        // Format Selection Row
        JPanel formatRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        formatRow.setOpaque(false);
        JLabel fmtLbl = new JLabel("Insert Style:");
        fmtLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        formatRow.add(fmtLbl);

        insertFormatCombo = new JComboBox<>(new String[]{
                "📑 Research Card (Visual)",
                "📌 Executive Brief (Bullets)",
                "📊 Extracted Tables Only",
                "📝 Clean Markdown"
        });
        insertFormatCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        insertFormatCombo.setPreferredSize(new Dimension(175, 26));
        formatRow.add(insertFormatCombo);
        bottomBox.add(formatRow, BorderLayout.NORTH);

        // Buttons Row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        actionRow.setOpaque(false);

        copyScrapedBtn = new JButton("📋 Copy Text");
        copyScrapedBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyScrapedBtn.addActionListener(e -> {
            String text = scraperOutputArea.getText();
            if (!text.isBlank()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                ModernDialog.showMessage(this, "Scraped text copied to clipboard!", "Copied", ModernDialog.Type.SUCCESS);
            }
        });
        actionRow.add(copyScrapedBtn);

        insertScrapedBtn = new JButton("📥 Insert to Document");
        insertScrapedBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        insertScrapedBtn.setBackground(new Color(37, 99, 235));
        insertScrapedBtn.setForeground(Color.WHITE);
        insertScrapedBtn.setOpaque(true);
        insertScrapedBtn.setBorderPainted(false);
        insertScrapedBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        insertScrapedBtn.addActionListener(e -> insertScrapedToDocument());
        actionRow.add(insertScrapedBtn);

        // Quick AI Prompt Button
        JButton sendToAiBtn = new JButton("🤖 Ask AI with this");
        sendToAiBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sendToAiBtn.setBackground(new Color(124, 58, 237));
        sendToAiBtn.setForeground(Color.WHITE);
        sendToAiBtn.setOpaque(true);
        sendToAiBtn.setBorderPainted(false);
        sendToAiBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendToAiBtn.addActionListener(e -> sendScrapedToActiveAi());
        actionRow.add(sendToAiBtn);

        bottomBox.add(actionRow, BorderLayout.SOUTH);
        p.add(bottomBox, BorderLayout.SOUTH);

        return p;
    }

    private JButton createChip(String label, String targetUrl) {
        JButton chip = new JButton(label);
        chip.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chip.setForeground(ThemeManager.isDark() ? new Color(148, 163, 184) : new Color(71, 85, 105));
        chip.setBackground(ThemeManager.isDark() ? new Color(30, 41, 59) : new Color(241, 245, 249));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        chip.setFocusPainted(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addActionListener(e -> {
            scraperUrlField.setText(targetUrl);
            executeScrape();
        });
        return chip;
    }

    private void executeScrape() {
        String url = scraperUrlField.getText().trim();
        if (url.isBlank()) {
            ModernDialog.showMessage(this, "Please enter a valid website URL to scrape.", "URL Required", ModernDialog.Type.WARNING);
            return;
        }

        scrapeBtn.setEnabled(false);
        scraperStatusLbl.setText("⏳ Scraping & parsing " + url + "...");

        SwingWorker<InternetScraperService.ScrapedArticle, Void> worker = new SwingWorker<>() {
            @Override
            protected InternetScraperService.ScrapedArticle doInBackground() throws Exception {
                return scraperService.scrapeUrl(url);
            }

            @Override
            protected void done() {
                scrapeBtn.setEnabled(true);
                try {
                    lastScrapedArticle = get();
                    scraperStatusLbl.setText("✔ Extracted: " + lastScrapedArticle.title() + " (~" + lastScrapedArticle.estimatedReadingTimeMinutes() + " min read)");
                    scraperOutputArea.setText(lastScrapedArticle.toMarkdown());
                    scraperOutputArea.setCaretPosition(0);
                } catch (Exception ex) {
                    scraperStatusLbl.setText("❌ Failed to scrape URL: " + ex.getMessage());
                    scraperOutputArea.setText("Scraping error:\n" + ex.getMessage() + "\n\nPlease check the URL or your network connection.");
                }
            }
        };
        worker.execute();
    }

    private void insertScrapedToDocument() {
        if (lastScrapedArticle == null) {
            String text = scraperOutputArea.getText().trim();
            if (text.isBlank()) {
                ModernDialog.showMessage(this, "No content scraped yet. Enter a URL and click Scrape first.", "Notice", ModernDialog.Type.INFO);
                return;
            }
            if (insertHtmlCallback != null) {
                insertHtmlCallback.accept("<pre style='background:#f1f5f9;padding:12px;border-radius:6px;'>" + text + "</pre><p><br></p>");
            }
            return;
        }

        if (insertHtmlCallback != null) {
            int fmtIdx = (insertFormatCombo != null) ? insertFormatCombo.getSelectedIndex() : 0;
            String htmlToInsert = switch (fmtIdx) {
                case 1 -> lastScrapedArticle.toExecutiveSummaryHtml();
                case 2 -> {
                    if (lastScrapedArticle.tables() != null && !lastScrapedArticle.tables().isEmpty()) {
                        yield String.join("<p><br></p>", lastScrapedArticle.tables()) + "<p><br></p>";
                    } else {
                        yield lastScrapedArticle.toExecutiveSummaryHtml();
                    }
                }
                case 3 -> "<div style='background:#f8fafc;border:1px solid #e2e8f0;padding:14px;border-radius:8px;'><pre style='font-family:monospace;white-space:pre-wrap;'>" +
                        lastScrapedArticle.toMarkdown() + "</pre></div><p><br></p>";
                default -> lastScrapedArticle.toDocumentHtml();
            };
            insertHtmlCallback.accept(htmlToInsert);
            ModernDialog.showMessage(this, "Content successfully inserted into document!", "Success", ModernDialog.Type.SUCCESS);
        }
    }

    private void sendScrapedToActiveAi() {
        if (lastScrapedArticle == null) {
            String content = scraperOutputArea.getText().trim();
            if (content.isBlank()) {
                ModernDialog.showMessage(this, "Please scrape content first before asking AI.", "Notice", ModernDialog.Type.INFO);
                return;
            }
            String prompt = "Please summarize the following web research article and list key insights:\n\n" + content;
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(prompt), null);
            selectTab(0);
            return;
        }

        String prompt = lastScrapedArticle.toPromptText();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(prompt), null);

        // Switch to Claude (tab 0)
        selectTab(0);
        ModernDialog.showMessage(this,
                "Enriched research prompt copied to clipboard!\n\nSwitched to Claude. Simply press Ctrl+V / Cmd+V to submit in your browser.",
                "AI Prompt Ready", ModernDialog.Type.SUCCESS);
    }

    /**
     * Builds an AI service panel (loads live embedded JavaFX WebView on-demand, fallback launch card otherwise).
     */
    private JComponent buildAiServicePanel(AiService svc) {
        // Embedded WebViews for modern external AI chats (Claude/ChatGPT) frequently trigger
        // native media segmentation faults (SIGSEGV) in WebKit on Apple Silicon Java 24.
        // Stylized Native AI Launch Cards are stable, fast, and prevent native VM crashes.
        if (!Boolean.getBoolean("notedown.webview.force")) {
            return buildFallbackCard(svc);
        }
        try {
            return buildJfxWebViewPanel(svc);
        } catch (Throwable t) {
            return buildFallbackCard(svc);
        }
    }

    /**
     * Instantiates live JavaFX WebView inside Swing JFXPanel with full modern navigation toolbar.
     */
    private JComponent buildJfxWebViewPanel(AiService svc) throws Exception {
        Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
        JComponent jfxPanel = (JComponent) jfxPanelClass.getDeclaredConstructor().newInstance();

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setOpaque(true);
        navBar.setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(241, 245, 249));
        navBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        leftNav.setOpaque(false);

        JButton backBtn = new JButton(VectorIcons.create(VectorIcons.Type.BACK, 12, null));
        backBtn.setPreferredSize(new Dimension(28, 24));
        backBtn.setToolTipText("Back");
        backBtn.setFocusPainted(false);
        backBtn.setBackground(ThemeManager.getSurface());
        leftNav.add(backBtn);

        JButton fwdBtn = new JButton(VectorIcons.create(VectorIcons.Type.COL_RIGHT, 12, null));
        fwdBtn.setPreferredSize(new Dimension(28, 24));
        fwdBtn.setToolTipText("Forward");
        fwdBtn.setFocusPainted(false);
        fwdBtn.setBackground(ThemeManager.getSurface());
        leftNav.add(fwdBtn);

        JButton reloadBtn = new JButton(VectorIcons.create(VectorIcons.Type.REFRESH, 12, null));
        reloadBtn.setPreferredSize(new Dimension(28, 24));
        reloadBtn.setToolTipText("Reload");
        reloadBtn.setFocusPainted(false);
        reloadBtn.setBackground(ThemeManager.getSurface());
        leftNav.add(reloadBtn);

        JLabel svcTitle = new JLabel(" " + svc.name(), VectorIcons.create(svc.iconType(), 15, svc.accent()), SwingConstants.LEFT);
        svcTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        svcTitle.setForeground(svc.accent());
        leftNav.add(svcTitle);

        navBar.add(leftNav, BorderLayout.WEST);

        JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 1));
        rightNav.setOpaque(false);

        JButton zoomOutBtn = new JButton(VectorIcons.create(VectorIcons.Type.ZOOM_OUT, 12, null));
        zoomOutBtn.setPreferredSize(new Dimension(26, 24));
        zoomOutBtn.setToolTipText("Zoom Out");
        zoomOutBtn.setFocusPainted(false);
        zoomOutBtn.setBackground(ThemeManager.getSurface());
        rightNav.add(zoomOutBtn);

        JButton zoomInBtn = new JButton(VectorIcons.create(VectorIcons.Type.ZOOM_IN, 12, null));
        zoomInBtn.setPreferredSize(new Dimension(26, 24));
        zoomInBtn.setToolTipText("Zoom In");
        zoomInBtn.setFocusPainted(false);
        zoomInBtn.setBackground(ThemeManager.getSurface());
        rightNav.add(zoomInBtn);

        JButton promptBtn = new JButton("📋 Prompt");
        promptBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        promptBtn.setPreferredSize(new Dimension(72, 24));
        promptBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        promptBtn.setToolTipText("Copy structured research prompt to paste into " + svc.name());
        promptBtn.addActionListener(e -> {
            String pText = (lastScrapedArticle != null)
                    ? lastScrapedArticle.toPromptText()
                    : "Please assist in analyzing and writing content for my Mikey Note document studio project:\n";
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pText), null);
            ModernDialog.showMessage(container, "Prompt copied! Press Ctrl+V / Cmd+V in the AI chat to paste.", "Prompt Ready", ModernDialog.Type.SUCCESS);
        });
        rightNav.add(promptBtn);

        JButton proxyBtn = new JButton(ProxyRoutingManager.getStatusBadge());
        proxyBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        proxyBtn.setPreferredSize(new Dimension(86, 24));
        proxyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        proxyBtn.setToolTipText(ProxyRoutingManager.getStatusTooltip());
        proxyBtn.setFocusPainted(false);
        proxyBtn.setBackground(ThemeManager.getSurface());
        proxyBtn.addActionListener(e -> {
            ProxyRoutingManager.showRoutingDialog(container, () -> {
                proxyBtn.setText(ProxyRoutingManager.getStatusBadge());
                proxyBtn.setToolTipText(ProxyRoutingManager.getStatusTooltip());
                try {
                    Class<?> pClass = Class.forName("javafx.application.Platform");
                    pClass.getMethod("runLater", Runnable.class).invoke(null, (Runnable) () -> {
                        try {
                            Object eng = webEngines.get(svc.id());
                            if (eng != null) {
                                eng.getClass().getMethod("reload").invoke(eng);
                            }
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            });
        });
        rightNav.add(proxyBtn);

        JButton openExtBtn = new JButton("↗ External");
        openExtBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        openExtBtn.setPreferredSize(new Dimension(80, 24));
        openExtBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openExtBtn.setToolTipText("Open " + svc.name() + " in your default browser");
        openExtBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(svc.url()));
            } catch (Exception ignored) {}
        });
        rightNav.add(openExtBtn);

        navBar.add(rightNav, BorderLayout.EAST);

        container.add(navBar, BorderLayout.NORTH);
        container.add(jfxPanel, BorderLayout.CENTER);

        // Schedule FX initialization on JavaFX thread
        Class<?> platformClass = Class.forName("javafx.application.Platform");
        platformClass.getMethod("runLater", Runnable.class).invoke(null, (Runnable) () -> {
            try {
                Class<?> sceneClass = Class.forName("javafx.scene.Scene");
                Class<?> webViewClass = Class.forName("javafx.scene.web.WebView");
                Class<?> webEngineClass = Class.forName("javafx.scene.web.WebEngine");

                Object webView = webViewClass.getDeclaredConstructor().newInstance();
                Object webEngine = webViewClass.getMethod("getEngine").invoke(webView);

                // Store active webEngine for reload/routing
                webEngines.put(svc.id(), webEngine);

                // Apply active VPN / Proxy routing
                ProxyRoutingManager.applyRouting();

                // Set modern genuine desktop User-Agent (avoid bot block tokens like NoteDown)
                webEngineClass.getMethod("setUserAgent", String.class).invoke(webEngine,
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

                // Execute script cleanup on page completion
                try {
                    Object loadWorker = webEngineClass.getMethod("getLoadWorker").invoke(webEngine);
                    Object stateProp = loadWorker.getClass().getMethod("stateProperty").invoke(loadWorker);
                    Class<?> changeListenerClass = Class.forName("javafx.beans.value.ChangeListener");
                    java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                        if ("changed".equals(method.getName())) {
                            Object newState = args[2];
                            if (newState != null && "SUCCEEDED".equals(newState.toString())) {
                                try {
                                    webEngineClass.getMethod("executeScript", String.class).invoke(webEngine,
                                            "try { Object.defineProperty(navigator, 'webdriver', {get: () => undefined}); } catch(e){}");
                                } catch (Exception ignored) {}
                            }
                        }
                        return null;
                    };
                    Object listener = java.lang.reflect.Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[]{changeListenerClass},
                            handler);
                    stateProp.getClass().getMethod("addListener", changeListenerClass).invoke(stateProp, listener);
                } catch (Exception ignored) {}

                webEngineClass.getMethod("load", String.class).invoke(webEngine, svc.url());

                Object scene = sceneClass.getDeclaredConstructor(
                        Class.forName("javafx.scene.Parent"), double.class, double.class
                ).newInstance(webView, 440.0, 600.0);

                jfxPanelClass.getMethod("setScene",
                        Class.forName("javafx.scene.Scene")).invoke(jfxPanel, scene);

                backBtn.addActionListener(ev -> {
                    try {
                        platformClass.getMethod("runLater", Runnable.class).invoke(null, (Runnable) () -> {
                            try {
                                Object history = webEngineClass.getMethod("getHistory").invoke(webEngine);
                                history.getClass().getMethod("go", int.class).invoke(history, -1);
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception ignored) {}
                });

                fwdBtn.addActionListener(ev -> {
                    try {
                        platformClass.getMethod("runLater", Runnable.class).invoke(null, (Runnable) () -> {
                            try {
                                Object history = webEngineClass.getMethod("getHistory").invoke(webEngine);
                                history.getClass().getMethod("go", int.class).invoke(history, 1);
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception ignored) {}
                });

                reloadBtn.addActionListener(ev -> {
                    try {
                        platformClass.getMethod("runLater", Runnable.class).invoke(null, (Runnable) () -> {
                            try {
                                webEngineClass.getMethod("reload").invoke(webEngine);
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception ignored) {}
                });

                zoomInBtn.addActionListener(ev -> {
                    try {
                        platformClass.getMethod("runLater", Runnable.class).invoke(null, (Runnable) () -> {
                            try {
                                double cur = (double) webViewClass.getMethod("getZoom").invoke(webView);
                                webViewClass.getMethod("setZoom", double.class).invoke(webView, Math.min(2.0, cur + 0.15));
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception ignored) {}
                });

                zoomOutBtn.addActionListener(ev -> {
                    try {
                        platformClass.getMethod("runLater", Runnable.class).invoke(null, (Runnable) () -> {
                            try {
                                double cur = (double) webViewClass.getMethod("getZoom").invoke(webView);
                                webViewClass.getMethod("setZoom", double.class).invoke(webView, Math.max(0.5, cur - 0.15));
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception ignored) {}
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return container;
    }

    /**
     * Fallback stylized launch card when WebView is not yet rendered.
     */
    private JComponent buildFallbackCard(AiService svc) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 20, 24, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel icon = new JLabel(VectorIcons.create(svc.iconType(), 48, svc.accent()));
        p.add(icon, gbc);

        JLabel name = new JLabel(svc.name());
        name.setFont(new Font("Segoe UI", Font.BOLD, 22));
        name.setForeground(svc.accent());
        p.add(name, gbc);

        JLabel sub = new JLabel("<html><center>Fast-Accessible AI Workspace<br>" + svc.url() + "</center></html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(100, 116, 139));
        p.add(sub, gbc);

        JButton launch = new JButton("Open " + svc.name() + " in Browser ↗");
        launch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        launch.setBackground(svc.accent());
        launch.setForeground(Color.WHITE);
        launch.setOpaque(true);
        launch.setBorderPainted(false);
        launch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        launch.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(svc.url()));
            } catch (Exception ex) {
                ModernDialog.showMessage(p, "Open: " + svc.url(), svc.name(), ModernDialog.Type.INFO);
            }
        });
        p.add(launch, gbc);

        return p;
    }

    private void buildResizeHandle() {
        JPanel handle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(148, 163, 184, 120));
                int cy = getHeight() / 2;
                for (int i = -16; i <= 16; i += 6) {
                    g2.fillOval(1, cy + i, 3, 3);
                }
                g2.dispose();
            }
        };
        handle.setPreferredSize(new Dimension(6, 0));
        handle.setOpaque(false);
        handle.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
        handle.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                resizing = true;
                resizingStartX = e.getXOnScreen();
                panelStartW = getPreferredSize().width;
            }
            @Override public void mouseReleased(MouseEvent e) { resizing = false; }
        });
        handle.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (!resizing) return;
                int delta = resizingStartX - e.getXOnScreen();
                int newW = Math.max(PANEL_MIN_W, Math.min(PANEL_MAX_W, panelStartW + delta));
                setPreferredSize(new Dimension(newW, getHeight()));
                JSplitPane sp = findAncestorSplitPane();
                if (sp != null) {
                    int maxAllowed = Math.max(PANEL_MIN_W, (int) (sp.getWidth() * 0.48));
                    newW = Math.min(newW, maxAllowed);
                    sp.setDividerLocation(sp.getWidth() - newW);
                }
                revalidate();
            }
        });
        add(handle, BorderLayout.WEST);
    }

    public void selectTab(int idx) {
        if (idx < 0 || idx >= AI_SERVICES.size()) return;
        activeTabIndex = idx;
        AiService svc = AI_SERVICES.get(idx);

        // On-demand creation of embedded WebView for AI services if not already created
        if (!webPanels.containsKey(svc.id())) {
            JComponent panel = buildAiServicePanel(svc);
            webPanels.put(svc.id(), panel);
            contentArea.add(panel, svc.id());
            contentArea.revalidate();
            contentArea.repaint();
        }

        contentCards.show(contentArea, svc.id());

        // Update button active visuals
        tabBar.removeAll();
        buildTabBar();
        tabBar.revalidate();
        tabBar.repaint();
    }

    public void openScraperWithUrl(String url) {
        int scraperIdx = 0;
        for (int i = 0; i < AI_SERVICES.size(); i++) {
            if ("scraper".equals(AI_SERVICES.get(i).id())) {
                scraperIdx = i;
                break;
            }
        }
        selectTab(scraperIdx);
        if (url != null && !url.isBlank()) {
            scraperUrlField.setText(url);
            executeScrape();
        }
    }

    private void applyTheme(ThemeManager.Theme t) {
        setBackground(ThemeManager.isDark() ? new Color(15, 23, 42) : new Color(248, 250, 252));
        tabBar.removeAll();
        buildTabBar();
        tabBar.revalidate();
        tabBar.repaint();
        repaint();
    }
}
