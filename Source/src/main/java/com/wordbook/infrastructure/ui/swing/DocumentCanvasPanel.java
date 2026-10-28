package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.ChartEngineService;
import com.wordbook.core.application.service.ChartEngineService.ChartMetadata;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;
import com.wordbook.core.application.service.DocumentStructureDetector;
import com.wordbook.core.application.service.GrammarEngineService;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.text.html.ImageView;

/**
 * Advanced Multi-Page Document Canvas rendering stacked sheets with drop shadows,
 * page break guides, page numbering ("Page X of Y"), cursor changes, click-and-type detection,
 * interactive chart & image editing studio, and live proofreading.
 */
public class DocumentCanvasPanel extends JPanel implements ThemeManager.ThemeChangeListener, Scrollable {

    public interface DocumentChangeHandler {
        void onContentChanged(String html, int words, int chars);
    }

    public enum PageFormat {
        A4("A4 Standard (740 × 960)", 740, 960),
        LETTER("US Letter (760 × 900)", 760, 900),
        LEGAL("US Legal (740 × 1120)", 740, 1120),
        COMPACT("Compact Note (640 × 840)", 640, 840),
        WIDE("Wide Canvas (880 × 1060)", 880, 1060);

        public final String label;
        public final int width;
        public final int height;
        PageFormat(String label, int width, int height) {
            this.label = label;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() { return label; }
    }

    public enum PageMargin {
        NORMAL("Normal Margins (1\")", 54, 54, 54, 54),
        NARROW("Narrow Margins (0.5\")", 36, 28, 36, 28),
        WIDE("Wide Margins (1.5\")", 72, 76, 72, 76);

        public final String label;
        public final int top;
        public final int left;
        public final int bottom;
        public final int right;
        PageMargin(String label, int top, int left, int bottom, int right) {
            this.label = label;
            this.top = top;
            this.left = left;
            this.bottom = bottom;
            this.right = right;
        }

        @Override
        public String toString() { return label; }
    }

    public enum PaperPattern {
        EMPTY("Empty / Blank"),
        SINGLE_RULE("Single Rule (Lined Paper)"),
        SQUARE_RULE("Square Rule (Grid / Graph)");

        public final String label;
        PaperPattern(String label) { this.label = label; }

        @Override
        public String toString() { return label; }
    }

    private final JTextPane textPane;
    private final HTMLEditorKit editorKit;
    private final DocumentChangeHandler changeHandler;
    private final Timer autoSaveTimer;
    private final Timer pageLayoutTimer;
    private volatile int cachedPageCount = 1;
    private final DocumentListener docListener = new DocumentListener() {
        @Override public void insertUpdate(DocumentEvent e) { onDocChange(); }
        @Override public void removeUpdate(DocumentEvent e) { onDocChange(); }
        @Override public void changedUpdate(DocumentEvent e) { onDocChange(); }
    };
    private final JPanel pageSheet;
    private final AdvancedUndoManager undoManager = new AdvancedUndoManager();

    /** Expose advanced undo manager for ribbon history dropdowns */
    public AdvancedUndoManager getAdvancedUndoManager() { return undoManager; }
    private final ChartEngineService chartEngineService = new ChartEngineService();
    private final GrammarEngineService grammarEngine = new GrammarEngineService();

    private boolean suppressEvents = false;
    private double zoomFactor = 1.0;
    private Element lastClickedImageElement = null;

    // Paper Patterns & Ruling Spacing
    private PaperPattern currentPaperPattern = PaperPattern.EMPTY;
    private int ruleSpacing = 28;

    // Interactive Media (Image & Chart) Selection, Resizing & Free Movement
    private Element selectedMediaElement = null;
    private Rectangle selectedMediaBounds = null;
    private int activeResizeHandle = -1; // 0..7 or -1
    private boolean isDraggingMedia = false;
    private boolean hasDraggedMedia = false;
    private Point dragStartPoint = null;
    private int initialDragWidth = 0;
    private int initialDragHeight = 0;
    private int liveResizeWidth = 0;
    private int liveResizeHeight = 0;
    private int liveResizeX = 0;
    private int liveResizeY = 0;
    private int dragTargetCaretPos = -1;
    private Point currentDragPoint = null;
    private Point dragOffsetInMedia = new Point();
    private final Map<String, Image> mediaImageCache = new ConcurrentHashMap<>();

    private static final int HANDLE_NW = 0;
    private static final int HANDLE_N = 1;
    private static final int HANDLE_NE = 2;
    private static final int HANDLE_E = 3;
    private static final int HANDLE_SE = 4;
    private static final int HANDLE_S = 5;
    private static final int HANDLE_SW = 6;
    private static final int HANDLE_W = 7;
    private static final int HANDLE_BODY = 8;
    private static final int HANDLE_SIZE = 8;

    public interface ContextualTabHandler {
        void showContextualTab(String tabName);
        void hideContextualTabs();
        default void showShapeFormatPanel(Element elem) {}
        default void hideShapeFormatPanel() {}
    }

    private ContextualTabHandler contextualTabHandler;

    public void setContextualTabHandler(ContextualTabHandler handler) {
        this.contextualTabHandler = handler;
    }

    private PageFormat currentPageFormat = PageFormat.A4;
    private PageMargin currentPageMargin = PageMargin.NORMAL;
    private Color currentPaperTone = Color.WHITE;

    // Global Header & Footer Configuration
    private String headerStyle = "MODERN_LINE";
    private String footerStyle = "PAGE_NUMBER_CENTER";
    private String headerLeftText = "Mikey Note Document";
    private String headerRightText = "Confidential";
    private String documentTitle = "Mikey Note Document";

    public String getHeaderStyle() { return headerStyle; }
    public void setHeaderStyle(String style) {
        this.headerStyle = style != null ? style : "MODERN_LINE";
        pageSheet.repaint();
    }

    public String getFooterStyle() { return footerStyle; }
    public void setFooterStyle(String style) {
        this.footerStyle = style != null ? style : "PAGE_NUMBER_CENTER";
        pageSheet.repaint();
    }

    public String getHeaderLeftText() { return headerLeftText; }
    public void setHeaderLeftText(String text) {
        this.headerLeftText = text;
        pageSheet.repaint();
    }

    public String getHeaderRightText() { return headerRightText; }
    public void setHeaderRightText(String text) {
        this.headerRightText = text;
        pageSheet.repaint();
    }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String title) {
        this.documentTitle = title;
        pageSheet.repaint();
    }

    // Word-Style Header & Footer Selection State
    private boolean isHeaderFooterSelected = false;
    private String activeHeaderFooterSection = "HEADER"; // "HEADER" or "FOOTER"
    private int activeHeaderFooterPage = 0;

    public boolean isHeaderFooterSelected() { return isHeaderFooterSelected; }
    public String getActiveHeaderFooterSection() { return activeHeaderFooterSection; }
    public int getActiveHeaderFooterPage() { return activeHeaderFooterPage; }

    public void setHeaderFooterSelected(boolean selected, String section, int pageIndex) {
        this.isHeaderFooterSelected = selected;
        if (section != null) this.activeHeaderFooterSection = section.toUpperCase();
        this.activeHeaderFooterPage = Math.max(0, pageIndex);
        if (selected && contextualTabHandler != null) {
            contextualTabHandler.showContextualTab(RibbonPanel.TAB_HEADER_FOOTER);
        }
        pageSheet.repaint();
        textPane.repaint();
    }

    public void closeHeaderFooterSelection() {
        if (!isHeaderFooterSelected) return;
        this.isHeaderFooterSelected = false;
        if (contextualTabHandler != null) {
            contextualTabHandler.hideContextualTabs();
        }
        pageSheet.repaint();
        textPane.repaint();
    }

    public int getPageUnit() {
        return currentPageFormat.height + 28;
    }

    public int getPageBodyHeight() {
        return Math.max(100, currentPageFormat.height - currentPageMargin.top - currentPageMargin.bottom);
    }

    public Dimension getPageSheetPreferredSize() {
        return pageSheet != null ? pageSheet.getPreferredSize() : new Dimension(0, 0);
    }

    public DocumentCanvasPanel(DocumentChangeHandler changeHandler) {
        this.changeHandler = changeHandler;

        setLayout(new GridBagLayout());
        setBackground(ThemeManager.getCanvasBackdrop());
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        ThemeManager.addListener(this);

        // Click on the gray canvas backdrop focuses the document
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                textPane.requestFocusInWindow();
            }
        });

        // Create the Multi-Page Paper Canvas with realistic Multi-Layer Drop Shadow
        pageSheet = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                int baseW = currentPageFormat.width;
                int h = super.getPreferredSize().height;
                int singleH = currentPageFormat.height;
                return new Dimension(baseW, Math.max(singleH, h));
            }

            @Override
            public Dimension getMaximumSize() {
                int baseW = currentPageFormat.width;
                return new Dimension(baseW, Integer.MAX_VALUE);
            }

            @Override
            public Dimension getMinimumSize() {
                int baseW = currentPageFormat.width;
                int singleH = currentPageFormat.height;
                return new Dimension(baseW, singleH);
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setClip(0, 0, getWidth(), getHeight());
                super.paintChildren(g2);
                g2.dispose();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int totalPages = getPageCount();
                int singlePageH = currentPageFormat.height;
                int pageGap = 28;
                int baseW = currentPageFormat.width;
                int pageW = baseW - 2;

                for (int p = 0; p < totalPages; p++) {
                    int pageY = p * (singlePageH + pageGap);

                    // 1. Multi-layered realistic page drop shadow for page p
                    g2.setColor(new Color(0, 0, 0, 8));
                    g2.fillRoundRect(8, pageY + 12, pageW - 6, singlePageH - 8, 8, 8);
                    g2.setColor(new Color(0, 0, 0, 18));
                    g2.fillRoundRect(4, pageY + 6, pageW - 2, singlePageH - 4, 6, 6);
                    g2.setColor(new Color(0, 0, 0, 28));
                    g2.fillRoundRect(2, pageY + 3, pageW, singlePageH - 2, 4, 4);

                    // 2. Paper sheet with custom paper tone
                    g2.setColor(currentPaperTone);
                    g2.fillRoundRect(0, pageY, pageW, singlePageH, 4, 4);
                    g2.setColor(new Color(203, 213, 225));
                    g2.drawRoundRect(0, pageY, pageW, singlePageH, 4, 4);

                    // 3. Render Header on Page p
                    if ("NONE".equalsIgnoreCase(headerStyle)) {
                        g2.setColor(new Color(248, 250, 252));
                        g2.fillRect(1, pageY + 1, pageW - 2, 18);
                        g2.setColor(new Color(226, 232, 240));
                        g2.drawLine(1, pageY + 19, pageW - 1, pageY + 19);

                        // Ruler inch tick marks
                        g2.setColor(new Color(148, 163, 184));
                        for (int x = 60; x < pageW - 60; x += 30) {
                            int tickH = ((x - 60) % 90 == 0) ? 8 : 4;
                            g2.drawLine(x, pageY + 19 - tickH, x, pageY + 19);
                        }
                    } else {
                        paintHeaderOnPage(g2, p, pageY, pageW, totalPages);
                    }

                    // 4. Render Paper Ruling Pattern
                    if (currentPaperPattern == PaperPattern.SINGLE_RULE) {
                        g2.setColor(new Color(226, 232, 240));
                        g2.setStroke(new BasicStroke(1f));
                        int startY = pageY + Math.max(34, currentPageMargin.top);
                        int endY = pageY + singlePageH - Math.max(28, currentPageMargin.bottom);
                        for (int y = startY; y < endY; y += ruleSpacing) {
                            g2.drawLine(2, y, pageW - 2, y);
                        }
                        int marginX = Math.max(36, currentPageMargin.left - 12);
                        g2.setColor(new Color(254, 202, 202));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawLine(marginX, pageY + 20, marginX, pageY + singlePageH - 4);
                    } else if (currentPaperPattern == PaperPattern.SQUARE_RULE) {
                        g2.setColor(new Color(232, 238, 246));
                        g2.setStroke(new BasicStroke(1f));
                        int startY = pageY + 24;
                        int endY = pageY + singlePageH - 4;
                        for (int y = startY; y < endY; y += ruleSpacing) {
                            g2.drawLine(2, y, pageW - 2, y);
                        }
                        for (int x = 2; x < pageW - 2; x += ruleSpacing) {
                            g2.drawLine(x, startY, x, endY);
                        }
                        int marginX = Math.max(36, currentPageMargin.left - 12);
                        g2.setColor(new Color(191, 219, 254));
                        g2.drawLine(marginX, startY, marginX, endY);
                    }

                    // 5. Render Footer on Page p
                    paintFooterOnPage(g2, p, pageY, pageW, singlePageH, totalPages);

                    // 6. Body Area Boundary Crop Marks (Microsoft Word Standard)
                    paintBodyCropMarks(g2, pageY, pageW, singlePageH);

                    // 7. Word-Style Header/Footer Selection Representation
                    if (isHeaderFooterSelected) {
                        paintHeaderFooterSelection(g2, p, pageY, pageW, singlePageH);
                    }

                    // 8. Between pages: draw canvas backdrop gap & page break indicator
                    if (p < totalPages - 1) {
                        int gapY = pageY + singlePageH;
                        g2.setColor(ThemeManager.getCanvasBackdrop());
                        g2.fillRect(0, gapY, baseW, pageGap);

                        g2.setColor(new Color(148, 163, 184, 150));
                        float[] dash = {4f, 4f};
                        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                        int midY = gapY + pageGap / 2;
                        g2.drawLine(24, midY, baseW / 2 - 45, midY);
                        g2.drawLine(baseW / 2 + 45, midY, baseW - 24, midY);
                        g2.setStroke(new BasicStroke(1f));

                        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        g2.setColor(new Color(100, 116, 139));
                        String pbText = "PAGE BREAK";
                        int pbW = g2.getFontMetrics().stringWidth(pbText);
                        g2.drawString(pbText, (baseW - pbW) / 2, midY + 3);
                    }
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        pageSheet.setOpaque(false);
        pageSheet.setPreferredSize(new Dimension(currentPageFormat.width, currentPageFormat.height));
        pageSheet.setBorder(new EmptyBorder(currentPageMargin.top, currentPageMargin.left, currentPageMargin.bottom, currentPageMargin.right));
        pageSheet.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        // Click anywhere on paper sheet automatically places caret at nearest typing spot
        pageSheet.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point rawPt = e.getPoint();
                int singlePageH = currentPageFormat.height;
                int pageGap = 28;
                int pageUnit = Math.max(100, singlePageH + pageGap);
                int p = rawPt.y / pageUnit;
                int yInPage = rawPt.y % pageUnit;

                // Double-click selection like Microsoft Word
                if (e.getClickCount() == 2) {
                    if (yInPage <= Math.max(34, currentPageMargin.top + 14)) {
                        setHeaderFooterSelected(true, "HEADER", p);
                        return;
                    } else if (yInPage >= singlePageH - Math.max(32, currentPageMargin.bottom + 14) && yInPage <= singlePageH) {
                        setHeaderFooterSelected(true, "FOOTER", p);
                        return;
                    } else if (isHeaderFooterSelected) {
                        closeHeaderFooterSelection();
                        return;
                    }
                }

                if (isHeaderFooterSelected && yInPage > currentPageMargin.top + 14 && yInPage < singlePageH - currentPageMargin.bottom - 14) {
                    closeHeaderFooterSelection();
                }

                Point pt = SwingUtilities.convertPoint(pageSheet, e.getPoint(), textPane);
                int pos = textPane.viewToModel2D(pt);
                if (pos >= 0) {
                    textPane.setCaretPosition(pos);
                } else {
                    textPane.setCaretPosition(textPane.getDocument().getLength());
                }
                textPane.requestFocusInWindow();

                // Double click in whitespace below text inserts blank lines ("Click and Type")
                if (e.getClickCount() == 2 && pt.y > textPane.getHeight() - 40 && !isHeaderFooterSelected) {
                    insertHtml("<p><br></p>");
                }
            }
        });

        // Editor Kit with Modern Typography & Word-Style Multi-Page Body Layout
        editorKit = new HTMLEditorKit() {
            @Override
            public ViewFactory getViewFactory() {
                return new HTMLFactory() {
                    @Override
                    public View create(Element elem) {
                        String name = elem.getName();
                        if ("body".equalsIgnoreCase(name)) {
                            return new PagedBodyView(elem, View.Y_AXIS);
                        } else if ("p".equalsIgnoreCase(name) || "h1".equalsIgnoreCase(name) || "h2".equalsIgnoreCase(name) || "h3".equalsIgnoreCase(name)) {
                            return new PagedParagraphView(elem);
                        } else if ("img".equalsIgnoreCase(name)) {
                            return new ModernImageView(elem);
                        }
                        return super.create(elem);
                    }
                };
            }
        };
        StyleSheet css = editorKit.getStyleSheet();
        css.addRule("body { font-family: 'Plus Jakarta Sans', Arial, sans-serif; font-size: 14pt; color: #1e293b; line-height: 1.65; }");
        css.addRule("p { font-size: 14pt; color: #1e293b; margin-top: 2px; margin-bottom: 10px; }");
        css.addRule("ol, ul { margin-top: 4px; margin-bottom: 10px; padding-left: 24px; }");
        css.addRule("li { margin-top: 2px; margin-bottom: 2px; }");
        css.addRule("figure { margin: 14px auto 10px auto; text-align: center; }");
        css.addRule("figcaption { font-size: 11pt; color: #64748b; font-style: italic; margin-top: 4px; margin-bottom: 8px; text-align: center; }");
        css.addRule("h1 { font-size: 26px; font-weight: bold; color: #0f172a; margin-top: 14px; margin-bottom: 8px; }");
        css.addRule("h2 { font-size: 20px; font-weight: bold; color: #1e293b; margin-top: 18px; margin-bottom: 6px; }");
        css.addRule("h3 { font-size: 16px; font-weight: bold; color: #334155; margin-top: 14px; margin-bottom: 4px; }");
        css.addRule("table { border-collapse: collapse; width: 100%; margin: 14px 0; border: 1px solid #cbd5e1; }");
        css.addRule("th, td { border: 1px solid #cbd5e1; padding: 8px 12px; font-size: 14pt; }");
        css.addRule("th { background-color: #f1f5f9; font-weight: bold; color: #0f172a; }");
        css.addRule("blockquote { border-left: 4px solid #2563eb; margin: 14px 0; padding: 6px 16px; color: #475569; background: #f8fafc; }");
        css.addRule("pre { background-color: #0f172a; color: #f8fafc; padding: 12px 16px; border-radius: 6px; font-family: monospace; }");
        css.addRule(".page-break-container { page-break-after: always; margin: 32px 0 20px 0; }");
        css.addRule(".callout-info { border-left: 4px solid #2563eb; background-color: #eff6ff; padding: 12px 16px; margin: 14px 0; border-radius: 4px; color: #1e3a8a; }");
        css.addRule(".callout-success { border-left: 4px solid #16a34a; background-color: #f0fdf4; padding: 12px 16px; margin: 14px 0; border-radius: 4px; color: #14532d; }");
        css.addRule(".callout-warning { border-left: 4px solid #f59e0b; background-color: #fffbeb; padding: 12px 16px; margin: 14px 0; border-radius: 4px; color: #78350f; }");
        css.addRule(".callout-quote { border-left: 4px solid #7c3aed; background-color: #faf5ff; padding: 12px 16px; margin: 14px 0; border-radius: 4px; color: #581c87; font-style: italic; }");

        textPane = new JTextPane() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paint(g);
                paintMediaOverlay(g);
                if (isHeaderFooterSelected) {
                    Graphics2D gDim = (Graphics2D) g.create();
                    gDim.setColor(new Color(255, 255, 255, 140));
                    gDim.fillRect(0, 0, getWidth(), getHeight());
                    gDim.dispose();
                }
            }

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int maxW = Math.max(200, currentPageFormat.width - currentPageMargin.left - currentPageMargin.right);
                return new Dimension(maxW, d.height);
            }
        };
        textPane.setEditorKit(editorKit);
        textPane.setContentType("text/html");
        textPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textPane.setBackground(currentPaperTone);
        textPane.setOpaque(false);
        textPane.setCaretColor(Color.BLACK);
        textPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        // Document Listener for Auto-Save and Word Counter (Debounced to prevent EDT lag)
        autoSaveTimer = new Timer(600, e -> triggerSave());
        autoSaveTimer.setRepeats(false);

        pageLayoutTimer = new Timer(150, e -> updateDynamicPageHeight());
        pageLayoutTimer.setRepeats(false);

        textPane.getDocument().addDocumentListener(docListener);
        textPane.getDocument().addUndoableEditListener(undoManager);

        // Global Keyboard Shortcuts (Ctrl/Cmd + S, Z, Y, B, I, U, Enter, Tab in tables, Backspace guard)
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (isHeaderFooterSelected) {
                        closeHeaderFooterSelection();
                        e.consume();
                        return;
                    }
                }
                boolean meta = e.isControlDown() || e.isMetaDown();
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (getActiveTable() != null) {
                        e.consume();
                        navigateTableCell(!e.isShiftDown());
                        return;
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    Element cell = getActiveTableCell();
                    if (cell != null) {
                        int pos = textPane.getCaretPosition();
                        if (pos <= cell.getStartOffset() || (cell.getEndOffset() - cell.getStartOffset() <= 1 && textPane.getSelectionStart() == textPane.getSelectionEnd())) {
                            e.consume();
                            return;
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    Element cell = getActiveTableCell();
                    if (cell != null) {
                        int pos = textPane.getCaretPosition();
                        if (pos >= cell.getEndOffset() - 1 && textPane.getSelectionStart() == textPane.getSelectionEnd()) {
                            e.consume();
                            return;
                        }
                    }
                } else if (meta && e.getKeyCode() == KeyEvent.VK_S) {
                    e.consume();
                    triggerSave();
                } else if (meta && e.getKeyCode() == KeyEvent.VK_Z) {
                    e.consume();
                    if (e.isShiftDown()) applyFormat("redo", null);
                    else applyFormat("undo", null);
                } else if (meta && e.getKeyCode() == KeyEvent.VK_Y) {
                    e.consume();
                    applyFormat("redo", null);
                } else if (meta && e.getKeyCode() == KeyEvent.VK_B) {
                    e.consume();
                    applyFormat("bold", null);
                } else if (meta && e.getKeyCode() == KeyEvent.VK_I) {
                    e.consume();
                    applyFormat("italic", null);
                } else if (meta && e.getKeyCode() == KeyEvent.VK_U) {
                    e.consume();
                    applyFormat("underline", null);
                } else if (meta && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    insertPageBreak();
                } else if (!meta && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (handleLoremCommand()) {
                        e.consume();
                    }
                }
            }
        });

        textPane.addCaretListener(e -> updateContextualTabState());

        // Dynamic Cursor & Hover Detection: Resize handles, Hand cursor over charts/images, Text cursor over body
        textPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (selectedMediaBounds != null) {
                    int handle = getMediaHandleAt(e.getPoint());
                    if (handle >= 0) {
                        Cursor cur = switch (handle) {
                            case HANDLE_NW -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                            case HANDLE_N -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                            case HANDLE_NE -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                            case HANDLE_E -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                            case HANDLE_SE -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                            case HANDLE_S -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                            case HANDLE_SW -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                            case HANDLE_W -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                            case HANDLE_BODY -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                            default -> Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
                        };
                        textPane.setCursor(cur);
                        textPane.setToolTipText(handle == HANDLE_BODY ? "Drag to move image/chart" : "Drag handle to resize image/chart");
                        return;
                    }
                }
                int pos = textPane.viewToModel2D(e.getPoint());
                if (pos >= 0) {
                    HTMLDocument doc = (HTMLDocument) textPane.getDocument();
                    Element elem = doc.getCharacterElement(pos);
                    if (elem != null && HTML.Tag.IMG.equals(elem.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
                        textPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
                        boolean isChart = src != null && src.toString().contains("charts/chart_");
                        textPane.setToolTipText(isChart ? "Chart: Click to select & resize/move or customize in Chart Studio" : "Image: Click to select & resize/move");
                        return;
                    }
                    Element vCard = findVideoCardAt(elem);
                    if (vCard != null) {
                        textPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        textPane.setToolTipText("Video: Click or Double-click to launch playback");
                        return;
                    }
                }
                textPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                textPane.setToolTipText(null);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (activeResizeHandle != -1 && selectedMediaElement != null && selectedMediaBounds != null) {
                    int dx = e.getX() - dragStartPoint.x;
                    int dy = e.getY() - dragStartPoint.y;
                    int maxPrintableW = getMaxPrintableWidth();
                    int newW = initialDragWidth;
                    int newH = initialDragHeight;
                    double aspect = (double) initialDragWidth / Math.max(1, initialDragHeight);

                    int startX = selectedMediaBounds.x;
                    int startY = selectedMediaBounds.y;
                    liveResizeX = startX;
                    liveResizeY = startY;

                    if (activeResizeHandle == HANDLE_E || activeResizeHandle == HANDLE_SE) {
                        newW = initialDragWidth + dx;
                        newH = (int) Math.round(newW / aspect);
                    } else if (activeResizeHandle == HANDLE_NE) {
                        newW = initialDragWidth + dx;
                        newH = (int) Math.round(newW / aspect);
                        liveResizeY = startY + (initialDragHeight - newH);
                    } else if (activeResizeHandle == HANDLE_W || activeResizeHandle == HANDLE_SW) {
                        newW = initialDragWidth - dx;
                        newH = (int) Math.round(newW / aspect);
                        liveResizeX = startX + (initialDragWidth - newW);
                    } else if (activeResizeHandle == HANDLE_NW) {
                        newW = initialDragWidth - dx;
                        newH = (int) Math.round(newW / aspect);
                        liveResizeX = startX + (initialDragWidth - newW);
                        liveResizeY = startY + (initialDragHeight - newH);
                    } else if (activeResizeHandle == HANDLE_S) {
                        newH = initialDragHeight + dy;
                        newW = (int) Math.round(newH * aspect);
                    } else if (activeResizeHandle == HANDLE_N) {
                        newH = initialDragHeight - dy;
                        newW = (int) Math.round(newH * aspect);
                        liveResizeY = startY + (initialDragHeight - newH);
                    }

                    liveResizeWidth = Math.max(60, Math.min(maxPrintableW, newW));
                    liveResizeHeight = Math.max(40, (int) Math.round(liveResizeWidth / aspect));
                    textPane.repaint();
                    return;
                }
                if (isDraggingMedia && selectedMediaElement != null) {
                    if (!hasDraggedMedia && dragStartPoint != null && dragStartPoint.distance(e.getPoint()) >= 8) {
                        hasDraggedMedia = true;
                    }
                    if (hasDraggedMedia) {
                        currentDragPoint = e.getPoint();
                        int pos = textPane.viewToModel2D(e.getPoint());
                        if (pos >= 0) {
                            dragTargetCaretPos = pos;
                        }
                        textPane.repaint();
                    }
                    return;
                }
            }
        });

        // Mouse click listener for context menus & contextual tool tabs (Tables, Charts, Images, Video)
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point pt = e.getPoint();
                Point sheetPt = SwingUtilities.convertPoint(textPane, pt, pageSheet);
                int singlePageH = currentPageFormat.height;
                int pageGap = 28;
                int pageUnit = Math.max(100, singlePageH + pageGap);
                int p = sheetPt.y / pageUnit;
                int yInPage = sheetPt.y % pageUnit;

                if (e.getClickCount() == 2) {
                    if (yInPage <= Math.max(34, currentPageMargin.top + 14)) {
                        setHeaderFooterSelected(true, "HEADER", p);
                        return;
                    } else if (yInPage >= singlePageH - Math.max(32, currentPageMargin.bottom + 14) && yInPage <= singlePageH) {
                        setHeaderFooterSelected(true, "FOOTER", p);
                        return;
                    } else if (isHeaderFooterSelected) {
                        closeHeaderFooterSelection();
                        return;
                    }
                }

                if (isHeaderFooterSelected && yInPage > currentPageMargin.top + 14 && yInPage < singlePageH - currentPageMargin.bottom - 14) {
                    closeHeaderFooterSelection();
                }

                handleMouseClick(e);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point pt = e.getPoint();

                    // 1. If media is already selected, check if clicking resize handles or body
                    if (selectedMediaBounds != null) {
                        int h = getMediaHandleAt(pt);
                        if (h >= 0 && h < 8) {
                            activeResizeHandle = h;
                            dragStartPoint = pt;
                            initialDragWidth = getElementWidth(selectedMediaElement, selectedMediaBounds.width);
                            initialDragHeight = getElementHeight(selectedMediaElement, selectedMediaBounds.height);
                            liveResizeWidth = initialDragWidth;
                            liveResizeHeight = initialDragHeight;
                            liveResizeX = selectedMediaBounds.x;
                            liveResizeY = selectedMediaBounds.y;
                            return;
                        } else if (h == HANDLE_BODY) {
                            boolean isImg = (selectedMediaElement != null && HTML.Tag.IMG.equals(selectedMediaElement.getAttributes().getAttribute(StyleConstants.NameAttribute)));
                            if (isImg) {
                                isDraggingMedia = true;
                                hasDraggedMedia = false;
                                dragStartPoint = pt;
                                dragOffsetInMedia = new Point(pt.x - selectedMediaBounds.x, pt.y - selectedMediaBounds.y);
                                currentDragPoint = pt;
                                dragTargetCaretPos = textPane.viewToModel2D(pt);
                                return;
                            } else {
                                int cellPos = textPane.viewToModel2D(pt);
                                if (cellPos >= 0) {
                                    textPane.setCaretPosition(cellPos);
                                    textPane.requestFocusInWindow();
                                }
                            }
                        }
                    }

                    // 2. Check if clicked directly on an Image / Chart (allows instant drag or selection)
                    Element hitImg = findImageElementAt(pt);
                    if (hitImg != null) {
                        selectedMediaElement = hitImg;
                        lastClickedImageElement = hitImg;
                        selectedMediaBounds = getElementBounds(hitImg);
                        if (selectedMediaBounds != null && selectedMediaBounds.contains(pt)) {
                            isDraggingMedia = true;
                            hasDraggedMedia = false;
                            dragStartPoint = pt;
                            dragOffsetInMedia = new Point(pt.x - selectedMediaBounds.x, pt.y - selectedMediaBounds.y);
                            currentDragPoint = pt;
                            dragTargetCaretPos = textPane.viewToModel2D(pt);
                        }
                        if (contextualTabHandler != null) {
                            Object src = hitImg.getAttributes().getAttribute(HTML.Attribute.SRC);
                            boolean isChart = src != null && (src.toString().contains("charts/chart_") || src.toString().contains(".wordbook/charts") || src.toString().contains("chart_"));
                            contextualTabHandler.showContextualTab(isChart ? RibbonPanel.TAB_CHART_DESIGN : RibbonPanel.TAB_CARD_SHAPE_FORMAT);
                            contextualTabHandler.showShapeFormatPanel(hitImg);
                        }
                        textPane.repaint();
                        return;
                    }

                    // 3. Check if clicked on a Table
                    Element hitCell = findCellAtPoint(pt);
                    if (hitCell != null) {
                        Element table = getEnclosingTable(hitCell);
                        if (table != null) {
                            selectedMediaElement = table;
                            selectedMediaBounds = getElementBounds(table);
                            int cellPos = textPane.viewToModel2D(pt);
                            if (cellPos >= 0) {
                                textPane.setCaretPosition(cellPos);
                            } else {
                                textPane.setCaretPosition(hitCell.getStartOffset());
                            }
                            textPane.requestFocusInWindow();
                            if (contextualTabHandler != null) {
                                boolean isCard = isCardOrShapeElement(table);
                                contextualTabHandler.showContextualTab(isCard ? RibbonPanel.TAB_CARD_SHAPE_FORMAT : RibbonPanel.TAB_TABLE_TOOLS);
                                contextualTabHandler.showShapeFormatPanel(table);
                            }
                            textPane.repaint();
                        }
                    } else if (selectedMediaElement != null) {
                        selectedMediaElement = null;
                        selectedMediaBounds = null;
                        textPane.repaint();
                    }

                    // 4. Check if clicked on a Shape or Card (div/span)
                    int clickPos = textPane.viewToModel2D(pt);
                    if (clickPos >= 0) {
                        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
                        Element chElem = doc.getCharacterElement(clickPos);
                        Element cardOrShape = findCardOrShapeAt(chElem);
                        if (cardOrShape != null && !HTML.Tag.TABLE.equals(cardOrShape.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
                            selectedMediaElement = cardOrShape;
                            selectedMediaBounds = getElementBounds(cardOrShape);
                            textPane.setCaretPosition(clickPos);
                            textPane.requestFocusInWindow();
                            if (contextualTabHandler != null) {
                                contextualTabHandler.showContextualTab(RibbonPanel.TAB_CARD_SHAPE_FORMAT);
                                contextualTabHandler.showShapeFormatPanel(cardOrShape);
                            }
                            textPane.repaint();
                        }

                        // Check if clicked inside a Page Break divider (prevent typing in rule!)
                        if (isPageBreakElement(chElem)) {
                            int afterBreak = chElem.getEndOffset();
                            if (afterBreak < doc.getLength()) {
                                textPane.setCaretPosition(afterBreak);
                                textPane.requestFocusInWindow();
                                return;
                            }
                        }
                    }
                }
                handleCellOrMediaClick(e);
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) showCanvasContextMenu(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (activeResizeHandle != -1 && selectedMediaElement != null) {
                    if (liveResizeWidth > 0 && Math.abs(liveResizeWidth - initialDragWidth) > 4) {
                        AttributeSet attrs = selectedMediaElement.getAttributes();
                        if (HTML.Tag.IMG.equals(attrs.getAttribute(StyleConstants.NameAttribute))) {
                            resizeImageElement(selectedMediaElement, liveResizeWidth);
                        } else if (HTML.Tag.TABLE.equals(attrs.getAttribute(StyleConstants.NameAttribute))) {
                            resizeTableElement(selectedMediaElement, liveResizeWidth);
                        }
                    }
                    activeResizeHandle = -1;
                    liveResizeWidth = 0;
                    liveResizeHeight = 0;
                    liveResizeX = 0;
                    liveResizeY = 0;
                    selectedMediaBounds = getElementBounds(selectedMediaElement);
                    textPane.repaint();
                }
                if (isDraggingMedia && selectedMediaElement != null) {
                    int target = dragTargetCaretPos;
                    boolean didDrag = hasDraggedMedia;
                    isDraggingMedia = false;
                    hasDraggedMedia = false;
                    dragTargetCaretPos = -1;
                    currentDragPoint = null;
                    if (didDrag && target >= 0) {
                        relocateMediaElement(selectedMediaElement, target);
                    }
                    textPane.repaint();
                }
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) showCanvasContextMenu(e);
            }
        });

        pageSheet.add(textPane, BorderLayout.CENTER);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(28, 20, 28, 20);
        add(pageSheet, gbc);
    }

    private boolean isPageBreakElement(Element elem) {
        if (elem == null) return false;
        Element curr = elem;
        while (curr != null) {
            AttributeSet attrs = curr.getAttributes();
            if (attrs != null) {
                Object cls = attrs.getAttribute(HTML.Attribute.CLASS);
                if (cls != null && cls.toString().contains("page-break")) return true;
                Object style = attrs.getAttribute(HTML.Attribute.STYLE);
                if (style != null && style.toString().contains("page-break")) return true;
            }
            curr = curr.getParentElement();
        }
        return false;
    }

    private Element findImageElementAt(Point pt) {
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        Element root = doc.getDefaultRootElement();
        Element found = scanForImageAt(root, pt);
        if (found != null) return found;

        int pos = textPane.viewToModel2D(pt);
        if (pos >= 0 && pos <= doc.getLength()) {
            Element elem = doc.getCharacterElement(pos);
            if (elem != null && HTML.Tag.IMG.equals(elem.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
                return elem;
            }
            if (pos > 0) {
                Element prev = doc.getCharacterElement(pos - 1);
                if (prev != null && HTML.Tag.IMG.equals(prev.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
                    return prev;
                }
            }
        }
        return null;
    }

    private Element scanForImageAt(Element elem, Point pt) {
        if (elem == null) return null;
        if (HTML.Tag.IMG.equals(elem.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
            Rectangle r = getElementBounds(elem);
            if (r != null) {
                Rectangle expanded = new Rectangle(r.x - 4, r.y - 4, r.width + 8, r.height + 8);
                if (expanded.contains(pt)) return elem;
            }
        }
        int count = elem.getElementCount();
        for (int i = 0; i < count; i++) {
            Element child = scanForImageAt(elem.getElement(i), pt);
            if (child != null) return child;
        }
        return null;
    }

    private Rectangle findViewBounds(View v, Element target, Shape alloc) {
        if (v == null || alloc == null || target == null) return null;
        if (v.getElement() == target) {
            return alloc.getBounds();
        }
        int n = v.getViewCount();
        for (int i = 0; i < n; i++) {
            Shape childAlloc = v.getChildAllocation(i, alloc);
            if (childAlloc != null) {
                Rectangle r = findViewBounds(v.getView(i), target, childAlloc);
                if (r != null && r.width > 0 && r.height > 0) return r;
            }
        }
        return null;
    }

    private Rectangle getElementBounds(Element elem) {
        if (elem == null) return null;
        try {
            // 1. Recursive exact View allocation (respects CSS centering, table layout, margins)
            View root = textPane.getUI().getRootView(textPane);
            if (root != null) {
                Insets insets = textPane.getInsets();
                Rectangle alloc = new Rectangle(
                        insets.left,
                        insets.top,
                        Math.max(1, textPane.getWidth() - insets.left - insets.right),
                        Math.max(1, textPane.getHeight() - insets.top - insets.bottom)
                );
                Rectangle viewR = findViewBounds(root, elem, alloc);
                if (viewR != null && viewR.width > 15 && viewR.height > 15) {
                    return viewR;
                }
            }

            // 2. Fallback to modelToView2D + dimension heuristics
            int start = elem.getStartOffset();
            Shape shape = textPane.getUI().getRootView(textPane).modelToView(start, Position.Bias.Forward, start + 1, Position.Bias.Backward, textPane.getBounds());
            if (shape != null) {
                Rectangle r = shape.getBounds();
                if (r.width > 20 && r.height > 20) return r;
            }
            java.awt.geom.Rectangle2D pt = textPane.modelToView2D(start);
            if (pt != null) {
                int w = getElementWidth(elem, 520);
                int h = 260;
                try {
                    Object hAttr = elem.getAttributes().getAttribute(HTML.Attribute.HEIGHT);
                    if (hAttr != null) h = Integer.parseInt(hAttr.toString());
                    else h = (int) (w * 0.52);
                } catch (Exception ignored) {}

                int x = (int) pt.getX();
                Element parent = elem.getParentElement();
                boolean isCentered = false;
                if (parent != null) {
                    Object align = parent.getAttributes().getAttribute(HTML.Attribute.ALIGN);
                    if (align != null && "center".equalsIgnoreCase(align.toString())) isCentered = true;
                    Object style = parent.getAttributes().getAttribute(HTML.Attribute.STYLE);
                    if (style != null && style.toString().contains("center")) isCentered = true;
                }
                if (isCentered || (x < 30 && textPane.getWidth() > w + 60)) {
                    x = Math.max(textPane.getInsets().left, (textPane.getWidth() - w) / 2);
                }
                return new Rectangle(x, (int) pt.getY(), w, h);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int getMediaHandleAt(Point pt) {
        if (selectedMediaBounds == null) return -1;
        Rectangle r = selectedMediaBounds;
        int hs = HANDLE_SIZE;
        int half = hs / 2;

        Rectangle[] handles = new Rectangle[] {
            new Rectangle(r.x - half, r.y - half, hs, hs), // NW = 0
            new Rectangle(r.x + r.width / 2 - half, r.y - half, hs, hs), // N = 1
            new Rectangle(r.x + r.width - half, r.y - half, hs, hs), // NE = 2
            new Rectangle(r.x + r.width - half, r.y + r.height / 2 - half, hs, hs), // E = 3
            new Rectangle(r.x + r.width - half, r.y + r.height - half, hs, hs), // SE = 4
            new Rectangle(r.x + r.width / 2 - half, r.y + r.height - half, hs, hs), // S = 5
            new Rectangle(r.x - half, r.y + r.height - half, hs, hs), // SW = 6
            new Rectangle(r.x - half, r.y + r.height / 2 - half, hs, hs), // W = 7
        };

        for (int i = 0; i < handles.length; i++) {
            if (handles[i].contains(pt)) return i;
        }

        if (r.contains(pt)) return HANDLE_BODY;
        return -1;
    }

    private Image getMediaImage(String src) {
        if (src == null || src.isEmpty()) return null;
        Image cached = mediaImageCache.get(src);
        if (cached != null) return cached;
        try {
            File f = getFileFromSrc(src);
            if (f != null && f.exists()) {
                BufferedImage bimg = ImageIO.read(f);
                if (bimg != null) {
                    mediaImageCache.put(src, bimg);
                    return bimg;
                }
            }
            URL u = new URL(src);
            BufferedImage bimg = ImageIO.read(u);
            if (bimg != null) {
                mediaImageCache.put(src, bimg);
                return bimg;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void paintMediaOverlay(Graphics g) {
        if (selectedMediaElement == null) return;
        selectedMediaBounds = getElementBounds(selectedMediaElement);
        if (selectedMediaBounds == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle r = new Rectangle(selectedMediaBounds);
        if (activeResizeHandle != -1 && liveResizeWidth > 0 && liveResizeHeight > 0) {
            r.width = liveResizeWidth;
            r.height = liveResizeHeight;
            if (liveResizeX > 0) r.x = liveResizeX;
            if (liveResizeY > 0) r.y = liveResizeY;

            // Live scaling preview: render image directly into new bounding box r
            try {
                AttributeSet attrs = selectedMediaElement.getAttributes();
                if (HTML.Tag.IMG.equals(attrs.getAttribute(StyleConstants.NameAttribute))) {
                    Object src = attrs.getAttribute(HTML.Attribute.SRC);
                    if (src != null) {
                        Image liveImg = getMediaImage(src.toString());
                        if (liveImg != null) {
                            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            Shape oldClip = g2.getClip();
                            g2.clip(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 8, 8));
                            g2.drawImage(liveImg, r.x, r.y, r.width, r.height, null);
                            g2.setClip(oldClip);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Ghost floating card following mouse cursor while dragging
        if (isDraggingMedia && hasDraggedMedia && currentDragPoint != null) {
            int ghostX = currentDragPoint.x - dragOffsetInMedia.x;
            int ghostY = currentDragPoint.y - dragOffsetInMedia.y;
            g2.setColor(new Color(2, 132, 199, 35));
            g2.fillRoundRect(ghostX, ghostY, r.width, r.height, 10, 10);
            g2.setColor(new Color(2, 132, 199, 200));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 4}, 0));
            g2.drawRoundRect(ghostX, ghostY, r.width, r.height, 10, 10);

            // Draw "Moving..." badge
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String moveText = "Move to Cursor";
            int mw = g2.getFontMetrics().stringWidth(moveText) + 16;
            g2.setColor(new Color(15, 23, 42, 220));
            g2.fillRoundRect(ghostX + (r.width - mw) / 2, ghostY + r.height / 2 - 12, mw, 24, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(moveText, ghostX + (r.width - mw) / 2 + 8, ghostY + r.height / 2 + 4);
        }

        // 1. Subtle Outer Glow
        g2.setColor(new Color(2, 132, 199, 45));
        g2.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
        g2.setColor(new Color(2, 132, 199, 90));
        g2.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);

        // 2. Vibrant Focus Selection Outline
        g2.setColor(new Color(2, 132, 199));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRect(r.x, r.y, r.width, r.height);

        // 3. Floating Dimensions & Type Pill Tag
        try {
            AttributeSet attrs = selectedMediaElement.getAttributes();
            boolean isTable = HTML.Tag.TABLE.equals(attrs.getAttribute(StyleConstants.NameAttribute));
            Object src = attrs.getAttribute(HTML.Attribute.SRC);
            boolean isChart = src != null && (src.toString().contains("charts/chart_") || src.toString().contains(".wordbook/charts"));
            String tagText;
            if (isTable) {
                tagText = "⊞ Table • " + r.width + " × " + r.height + " px";
            } else if (isChart) {
                tagText = "📊 Chart • " + r.width + " × " + r.height + " px";
            } else {
                tagText = "🖼️ Image • " + r.width + " × " + r.height + " px";
            }
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            int tagW = g2.getFontMetrics().stringWidth(tagText) + 16;
            int tagH = 20;
            int tagX = Math.max(4, r.x);
            int tagY = Math.max(4, r.y - tagH - 6);

            g2.setColor(new Color(15, 23, 42, 225));
            g2.fillRoundRect(tagX, tagY, tagW, tagH, 10, 10);
            g2.setColor(new Color(51, 65, 85));
            g2.drawRoundRect(tagX, tagY, tagW, tagH, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(tagText, tagX + 8, tagY + 14);
        } catch (Exception ignored) {}

        // 4. 8 Rounded Handles
        int hs = HANDLE_SIZE;
        int half = hs / 2;
        Point[] pts = new Point[] {
            new Point(r.x, r.y),
            new Point(r.x + r.width / 2, r.y),
            new Point(r.x + r.width, r.y),
            new Point(r.x + r.width, r.y + r.height / 2),
            new Point(r.x + r.width, r.y + r.height),
            new Point(r.x + r.width / 2, r.y + r.height),
            new Point(r.x, r.y + r.height),
            new Point(r.x, r.y + r.height / 2),
        };

        for (Point p : pts) {
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(p.x - half, p.y - half, hs, hs, 2, 2);
            g2.setColor(new Color(2, 132, 199));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(p.x - half, p.y - half, hs, hs, 2, 2);
        }

        // 5. Drag target insertion line marker
        if (isDraggingMedia && hasDraggedMedia && dragTargetCaretPos >= 0) {
            try {
                java.awt.geom.Rectangle2D dropPt = textPane.modelToView2D(dragTargetCaretPos);
                if (dropPt != null) {
                    int dy = (int) dropPt.getY();
                    int marginL = Math.max(10, currentPageMargin.left);
                    int marginR = Math.max(10, currentPageMargin.right);
                    int lineW = Math.max(100, textPane.getWidth() - marginL - marginR);

                    // Outer drop glow
                    g2.setColor(new Color(14, 165, 233, 60));
                    g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(marginL, dy, marginL + lineW, dy);

                    // Solid insertion bar
                    g2.setColor(new Color(2, 132, 199, 240));
                    g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(marginL, dy, marginL + lineW, dy);

                    // Drop pin left indicator
                    int[] lx = {marginL - 8, marginL + 2, marginL - 8};
                    int[] ly = {dy - 6, dy, dy + 6};
                    g2.fillPolygon(lx, ly, 3);

                    // Drop pin right indicator
                    int[] rx = {marginL + lineW + 8, marginL + lineW - 2, marginL + lineW + 8};
                    int[] ry = {dy - 6, dy, dy + 6};
                    g2.fillPolygon(rx, ry, 3);
                }
            } catch (Exception ignored) {}
        }

        g2.dispose();
    }

    private void resizeTableElement(Element elem, int newWidth) {
        if (elem == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int start = elem.getStartOffset();
            int end = elem.getEndOffset();
            String tableHtml = doc.getText(start, end - start);
            String newHtml;
            if (tableHtml.contains("width=")) {
                newHtml = tableHtml.replaceFirst("(?i)width=['\"]?\\d+['\"]?", "width='" + newWidth + "'");
            } else {
                newHtml = tableHtml.replaceFirst("(?i)<table", "<table width='" + newWidth + "'");
            }
            suppressEvents = true;
            try {
                doc.remove(start, end - start);
                editorKit.insertHTML(doc, start, newHtml, 0, 0, HTML.Tag.TABLE);
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // HEADER & FOOTER PAGE RENDERERS
    // =========================================================================

    private void paintHeaderOnPage(Graphics2D g2, int p, int pageY, int pageW, int totalPages) {
        if ("NONE".equalsIgnoreCase(headerStyle)) return;

        int headerTop = pageY + 6;
        String left = (headerLeftText != null && !headerLeftText.isEmpty()) ? headerLeftText : "Mikey Note Document";
        String right = (headerRightText != null && !headerRightText.isEmpty()) ? headerRightText : "Page " + (p + 1);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        switch (headerStyle != null ? headerStyle.toUpperCase() : "MODERN_LINE") {
            case "CLEAN_MINIMAL" -> {
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(left, 36, headerTop + 14);
                int rW = g2.getFontMetrics().stringWidth(right);
                g2.drawString(right, pageW - 36 - rW, headerTop + 14);
                g2.setColor(new Color(226, 232, 240));
                g2.drawLine(36, headerTop + 20, pageW - 36, headerTop + 20);
            }
            case "ACCENT_BANNER" -> {
                g2.setColor(new Color(37, 99, 235));
                g2.fillRoundRect(24, headerTop, pageW - 48, 18, 4, 4);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(left.toUpperCase(), 34, headerTop + 13);
                int rW = g2.getFontMetrics().stringWidth(right);
                g2.drawString(right, pageW - 34 - rW, headerTop + 13);
            }
            case "EDITORIAL_SPLIT" -> {
                g2.setColor(new Color(15, 23, 42));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(left.toUpperCase(), 36, headerTop + 12);
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                int rW = g2.getFontMetrics().stringWidth(right);
                g2.drawString(right, pageW - 36 - rW, headerTop + 12);
                g2.setColor(new Color(15, 23, 42));
                g2.drawLine(36, headerTop + 18, pageW - 36, headerTop + 18);
                g2.setColor(new Color(203, 213, 225));
                g2.drawLine(36, headerTop + 20, pageW - 36, headerTop + 20);
            }
            case "CORPORATE" -> {
                g2.setColor(new Color(241, 245, 249));
                g2.fillRect(1, pageY + 1, pageW - 2, 20);
                g2.setColor(new Color(203, 213, 225));
                g2.drawLine(1, pageY + 21, pageW - 1, pageY + 21);
                g2.setColor(new Color(51, 65, 85));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(left, 36, pageY + 14);
                g2.setColor(new Color(100, 116, 139));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                int rW = g2.getFontMetrics().stringWidth(right);
                g2.drawString(right, pageW - 36 - rW, pageY + 14);
            }
            default -> { // MODERN_LINE
                g2.setColor(new Color(14, 165, 233));
                g2.fillOval(36, headerTop + 6, 6, 6);
                g2.setColor(new Color(51, 65, 85));
                g2.drawString(left, 48, headerTop + 13);
                g2.setColor(new Color(148, 163, 184));
                int rW = g2.getFontMetrics().stringWidth(right);
                g2.drawString(right, pageW - 36 - rW, headerTop + 13);

                Paint oldPaint = g2.getPaint();
                g2.setPaint(new GradientPaint(36, 0, new Color(14, 165, 233, 180), pageW - 36, 0, new Color(99, 102, 241, 40)));
                g2.fillRect(36, headerTop + 19, pageW - 72, 2);
                g2.setPaint(oldPaint);
            }
        }
    }

    private void paintFooterOnPage(Graphics2D g2, int p, int pageY, int pageW, int singlePageH, int totalPages) {
        if ("NONE".equalsIgnoreCase(footerStyle)) return;

        int footerY = pageY + singlePageH - 24;
        String title = (documentTitle != null && !documentTitle.isEmpty()) ? documentTitle : "Mikey Note Document";
        String pageStr = "Page " + (p + 1) + " of " + totalPages;

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        switch (footerStyle != null ? footerStyle.toUpperCase() : "PAGE_NUMBER_CENTER") {
            case "SPLIT_TITLE_PAGE" -> {
                g2.setColor(new Color(226, 232, 240));
                g2.drawLine(36, footerY - 4, pageW - 36, footerY - 4);
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(title, 36, footerY + 12);
                int pW = g2.getFontMetrics().stringWidth(pageStr);
                g2.drawString(pageStr, pageW - 36 - pW, footerY + 12);
            }
            case "ACCENT_BAR" -> {
                g2.setColor(new Color(14, 165, 233));
                g2.fillRect(36, footerY + 14, pageW - 72, 3);
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(title, 36, footerY + 8);
                int pW = g2.getFontMetrics().stringWidth(pageStr);
                g2.drawString(pageStr, pageW - 36 - pW, footerY + 8);
            }
            case "DOCUMENT_INFO" -> {
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(24, footerY - 4, pageW - 24, footerY - 4);
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("Draft • Confidential", 36, footerY + 12);
                int pW = g2.getFontMetrics().stringWidth(pageStr);
                g2.drawString(pageStr, pageW - 36 - pW, footerY + 12);
            }
            case "MINIMAL_PAGE" -> {
                String s = "- " + (p + 1) + " -";
                int pW = g2.getFontMetrics().stringWidth(s);
                g2.setColor(new Color(148, 163, 184));
                g2.drawString(s, (pageW - pW) / 2, footerY + 12);
            }
            default -> { // PAGE_NUMBER_CENTER
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(24, footerY - 4, pageW - 24, footerY - 4);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(148, 163, 184));
                int pW = g2.getFontMetrics().stringWidth(pageStr);
                g2.drawString(pageStr, (pageW - pW) / 2, footerY + 12);
            }
        }
    }

    private void paintBodyCropMarks(Graphics2D g2, int pageY, int pageW, int singlePageH) {
        int left = currentPageMargin.left;
        int right = pageW - currentPageMargin.right;
        int top = pageY + currentPageMargin.top;
        int bottom = pageY + singlePageH - currentPageMargin.bottom;
        int markLen = 8;

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(new Color(148, 163, 184, 180)); // Subtle slate guide color

        // Top-left corner
        g2.drawLine(left - markLen, top, left, top);
        g2.drawLine(left, top - markLen, left, top);

        // Top-right corner
        g2.drawLine(right, top, right + markLen, top);
        g2.drawLine(right, top - markLen, right, top);

        // Bottom-left corner
        g2.drawLine(left - markLen, bottom, left, bottom);
        g2.drawLine(left, bottom, left, bottom + markLen);

        // Bottom-right corner
        g2.drawLine(right, bottom, right + markLen, bottom);
        g2.drawLine(right, bottom, right, bottom + markLen);

        g2.setStroke(oldStroke);
    }

    private void paintHeaderFooterSelection(Graphics2D g2, int p, int pageY, int pageW, int singlePageH) {
        boolean isThisPage = (p == activeHeaderFooterPage);
        boolean isHeader = "HEADER".equalsIgnoreCase(activeHeaderFooterSection);

        Color tealAccent = new Color(13, 148, 136);
        Color dashedColor = isThisPage ? tealAccent : new Color(13, 148, 136, 130);
        float[] dashPattern = {5f, 4f};
        Stroke dashedStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f);

        Stroke oldStroke = g2.getStroke();

        int left = Math.max(14, currentPageMargin.left - 8);
        int boxW = pageW - left - Math.max(14, currentPageMargin.right - 8);

        if (isHeader) {
            int boxY = pageY + 3;
            int boxH = Math.max(28, currentPageMargin.top + 8);

            if (isThisPage) {
                g2.setColor(new Color(13, 148, 136, 16));
                g2.fillRoundRect(left, boxY, boxW, boxH, 4, 4);
            }

            g2.setColor(dashedColor);
            g2.setStroke(dashedStroke);
            g2.drawRoundRect(left, boxY, boxW, boxH, 4, 4);

            String tagText = isThisPage ? ("Header - Page " + (p + 1)) : "Header";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tagW = fm.stringWidth(tagText) + 16;
            int tagH = 18;

            g2.setStroke(new BasicStroke(1f));
            g2.setColor(isThisPage ? tealAccent : new Color(13, 148, 136, 180));
            g2.fillRoundRect(left + 8, boxY - 9, tagW, tagH, 6, 6);

            g2.setColor(Color.WHITE);
            g2.drawString(tagText, left + 16, boxY + 4);

            // Bottom dashed divider line to clearly represent header selected like Word
            g2.setColor(new Color(13, 148, 136, 180));
            g2.setStroke(dashedStroke);
            g2.drawLine(left, boxY + boxH, left + boxW, boxY + boxH);
        } else {
            int boxY = pageY + singlePageH - Math.max(28, currentPageMargin.bottom + 12);
            int boxH = Math.max(28, currentPageMargin.bottom + 8);

            if (isThisPage) {
                g2.setColor(new Color(13, 148, 136, 16));
                g2.fillRoundRect(left, boxY, boxW, boxH, 4, 4);
            }

            g2.setColor(dashedColor);
            g2.setStroke(dashedStroke);
            g2.drawRoundRect(left, boxY, boxW, boxH, 4, 4);

            String tagText = isThisPage ? ("Footer - Page " + (p + 1)) : "Footer";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tagW = fm.stringWidth(tagText) + 16;
            int tagH = 18;

            g2.setStroke(new BasicStroke(1f));
            g2.setColor(isThisPage ? tealAccent : new Color(13, 148, 136, 180));
            g2.fillRoundRect(left + 8, boxY - 9, tagW, tagH, 6, 6);

            g2.setColor(Color.WHITE);
            g2.drawString(tagText, left + 16, boxY + 4);

            // Top dashed divider line to clearly represent footer selected like Word
            g2.setColor(new Color(13, 148, 136, 180));
            g2.setStroke(dashedStroke);
            g2.drawLine(left, boxY, left + boxW, boxY);
        }

        g2.setStroke(oldStroke);
    }

    public void showEditHeaderFooterDialog(String section) {
        boolean isHeader = "HEADER".equalsIgnoreCase(section);
        String title = isHeader ? "Edit Page Header" : "Edit Page Footer";
        
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout(16, 16));
        dlg.getContentPane().setBackground(ThemeManager.getSurface());

        JPanel form = new JPanel(new GridLayout(isHeader ? 2 : 1, 2, 10, 12));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(20, 24, 10, 24));

        JTextField fieldLeft = new JTextField(isHeader ? headerLeftText : documentTitle, 20);
        JTextField fieldRight = new JTextField(isHeader ? headerRightText : "", 20);

        if (isHeader) {
            form.add(new JLabel("Header Title (Left):"));
            form.add(fieldLeft);
            form.add(new JLabel("Tagline / Info (Right):"));
            form.add(fieldRight);
        } else {
            form.add(new JLabel("Document Title (Footer Left):"));
            form.add(fieldLeft);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setOpaque(false);
        JButton btnOk = new JButton("Apply");
        btnOk.setBackground(new Color(13, 148, 136));
        btnOk.setForeground(Color.WHITE);
        btnOk.addActionListener(e -> {
            if (isHeader) {
                setHeaderLeftText(fieldLeft.getText());
                setHeaderRightText(fieldRight.getText());
            } else {
                setDocumentTitle(fieldLeft.getText());
            }
            dlg.dispose();
            pageSheet.repaint();
        });

        JButton btnCancel = new JButton("Close");
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnOk);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void relocateMediaElement(Element elem, int targetPos) {
        if (elem == null || targetPos < 0) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int start = elem.getStartOffset();
            int end = elem.getEndOffset();
            if (targetPos >= start && targetPos <= end) {
                return;
            }
            AttributeSet attrs = elem.getAttributes();
            Object src = attrs.getAttribute(HTML.Attribute.SRC);
            if (src == null) return;
            int w = getElementWidth(elem, 520);
            int h = getElementHeight(elem, 290);

            Object dataFilter = attrs.getAttribute("data-filter");
            Object styleAttr = attrs.getAttribute(HTML.Attribute.STYLE);
            String filterAttrStr = dataFilter != null ? " data-filter='" + dataFilter + "'" : "";
            String styleAttrStr = styleAttr != null ? " style='" + styleAttr + "'" : " style='border:1px solid #cbd5e1;border-radius:8px;'";

            boolean isChart = src.toString().contains("charts/chart_") || src.toString().contains(".wordbook/charts");
            String imgHtml = isChart
                    ? "<p align='center' style='text-align:center;margin:16px auto;'><img src='" + src + "' width='" + w + "' height='" + h + "'" + filterAttrStr + styleAttrStr + "></p><p><br></p>"
                    : "<p align='center' style='text-align:center;margin:12px auto;'><img src='" + src + "' width='" + w + "' height='" + h + "'" + filterAttrStr + styleAttrStr + "></p><p><br></p>";

            Element p = elem.getParentElement();
            if (p != null && (p.getEndOffset() - p.getStartOffset() <= Math.max(end - start + 80, 120))) {
                start = p.getStartOffset();
                end = p.getEndOffset();
            }

            suppressEvents = true;
            try {
                // Remove the old media container
                doc.remove(start, end - start);

                // Adjust target position after removal
                int adjustedPos = targetPos;
                if (adjustedPos > start) {
                    adjustedPos -= (end - start);
                }
                adjustedPos = Math.max(0, Math.min(doc.getLength(), adjustedPos));

                Element targetChar = (adjustedPos >= 0 && adjustedPos <= doc.getLength()) ? doc.getCharacterElement(adjustedPos) : null;
                Element targetBlock = getTopBlockElement(targetChar);
                if (targetBlock == null && adjustedPos >= 0 && adjustedPos <= doc.getLength()) {
                    targetBlock = doc.getParagraphElement(adjustedPos);
                }

                if (targetBlock != null && !"body".equalsIgnoreCase(targetBlock.getName()) && !"html".equalsIgnoreCase(targetBlock.getName()) && !"head".equalsIgnoreCase(targetBlock.getName())) {
                    if (adjustedPos <= targetBlock.getStartOffset() + (targetBlock.getEndOffset() - targetBlock.getStartOffset()) / 2) {
                        doc.insertBeforeStart(targetBlock, imgHtml);
                    } else {
                        doc.insertAfterEnd(targetBlock, imgHtml);
                    }
                } else {
                    editorKit.insertHTML(doc, adjustedPos, imgHtml, 0, 0, HTML.Tag.P);
                }
            } finally {
                suppressEvents = false;
            }

            // Immediately re-select relocated media element
            Element newElem = findImageBySrc(doc.getDefaultRootElement(), src.toString());
            if (newElem != null) {
                selectedMediaElement = newElem;
                lastClickedImageElement = newElem;
                selectedMediaBounds = getElementBounds(newElem);
            }
            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
            pageSheet.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public PaperPattern getPaperPattern() { return currentPaperPattern; }

    public void setPaperPattern(PaperPattern pattern) {
        this.currentPaperPattern = pattern != null ? pattern : PaperPattern.EMPTY;
        pageSheet.repaint();
    }

    public void setPaperPattern(String patternName) {
        if (patternName == null) patternName = "EMPTY";
        if (patternName.toLowerCase().contains("single") || patternName.toLowerCase().contains("line")) {
            setPaperPattern(PaperPattern.SINGLE_RULE);
        } else if (patternName.toLowerCase().contains("square") || patternName.toLowerCase().contains("grid")) {
            setPaperPattern(PaperPattern.SQUARE_RULE);
        } else {
            setPaperPattern(PaperPattern.EMPTY);
        }
    }

    public int getRuleSpacing() { return ruleSpacing; }

    public void setRuleSpacing(int spacing) {
        this.ruleSpacing = Math.max(14, Math.min(80, spacing));
        pageSheet.repaint();
    }

    private void handleCellOrMediaClick(MouseEvent e) {
        Element hitCell = findCellAtPoint(e.getPoint());
        if (hitCell != null) {
            int pos = textPane.viewToModel2D(e.getPoint());
            if (pos >= 0) {
                textPane.setCaretPosition(pos);
            } else {
                textPane.setCaretPosition(hitCell.getStartOffset());
            }
            textPane.requestFocusInWindow();
            Element table = getEnclosingTable(hitCell);
            boolean isCard = isCardOrShapeElement(table);
            if (contextualTabHandler != null) {
                contextualTabHandler.showContextualTab(isCard ? RibbonPanel.TAB_CARD_SHAPE_FORMAT : RibbonPanel.TAB_TABLE_TOOLS);
            }
        }
    }

    private void handleMouseClick(MouseEvent e) {
        Point pt = e.getPoint();
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();

        // 1. Check for Table Cell Click
        Element hitCell = findCellAtPoint(pt);
        if (hitCell != null) {
            int pos = textPane.viewToModel2D(pt);
            if (pos >= 0) {
                textPane.setCaretPosition(pos);
            } else {
                textPane.setCaretPosition(hitCell.getStartOffset());
            }
            textPane.requestFocusInWindow();
            Element table = getEnclosingTable(hitCell);
            boolean isCard = isCardOrShapeElement(table);
            if (contextualTabHandler != null) {
                contextualTabHandler.showContextualTab(isCard ? RibbonPanel.TAB_CARD_SHAPE_FORMAT : RibbonPanel.TAB_TABLE_TOOLS);
            }
            if (e.getClickCount() == 2 && isCard) {
                openCardShapeEditor(table);
            }
            return;
        }

        // 1.5 Check for Shape or Card Click (div/span)
        int clickPos = textPane.viewToModel2D(pt);
        if (clickPos >= 0 && clickPos <= doc.getLength()) {
            Element chElem = doc.getCharacterElement(clickPos);
            Element cardOrShape = findCardOrShapeAt(chElem);
            if (cardOrShape != null && !HTML.Tag.TABLE.equals(cardOrShape.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
                textPane.setCaretPosition(clickPos);
                textPane.requestFocusInWindow();
                if (contextualTabHandler != null) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_CARD_SHAPE_FORMAT);
                }
                if (e.getClickCount() == 2) {
                    openCardShapeEditor(cardOrShape);
                }
                return;
            }
        }

        // 2. Check for IMG with pixel-perfect recursive element hit-testing
        Element hitImg = findImageElementAt(pt);
        if (hitImg != null) {
            lastClickedImageElement = hitImg;
            selectedMediaElement = hitImg;
            selectedMediaBounds = getElementBounds(hitImg);
            textPane.repaint();

            Object src = hitImg.getAttributes().getAttribute(HTML.Attribute.SRC);
            boolean isChart = src != null && (src.toString().contains("charts/chart_") || src.toString().contains(".wordbook/charts"));
            if (contextualTabHandler != null) {
                if (isChart) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_CHART_DESIGN);
                } else {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_PICTURE_FORMAT);
                }
            }
            if (e.getClickCount() == 2 && isChart) {
                modifySelectedChartData();
            }
            return;
        }

        // 3. Check for Video Card Click
        int pos = textPane.viewToModel2D(pt);
        if (pos >= 0 && pos <= doc.getLength()) {
            Element elem = doc.getCharacterElement(pos);
            Element videoCard = findVideoCardAt(elem);
            if (videoCard != null) {
                selectedMediaElement = null;
                selectedMediaBounds = null;
                textPane.repaint();
                if (contextualTabHandler != null) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_VIDEO_TOOLS);
                }
                if (e.getClickCount() == 2) {
                    playOrOpenVideo(videoCard);
                }
                return;
            }
        }

        selectedMediaElement = null;
        selectedMediaBounds = null;
        textPane.repaint();
        updateContextualTabState();
    }

    private long lastContextMenuTime = 0;

    public void showCanvasContextMenu(MouseEvent e) {
        long now = System.currentTimeMillis();
        if (now - lastContextMenuTime < 250) return;
        lastContextMenuTime = now;

        int pos = textPane.viewToModel2D(e.getPoint());
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        Element elem = pos >= 0 ? doc.getCharacterElement(pos) : null;

        boolean isImage = (elem != null && HTML.Tag.IMG.equals(elem.getAttributes().getAttribute(StyleConstants.NameAttribute)));
        Element videoCard = findVideoCardAt(elem);
        Element cardOrShape = findCardOrShapeAt(elem);
        Element tableCell = findParentTableCell(elem);
        String selText = textPane.getSelectedText();
        boolean hasTextSelection = selText != null && !selText.trim().isEmpty();

        if (!hasTextSelection && !isImage && videoCard == null && tableCell == null && cardOrShape == null && pos >= 0) {
            textPane.setCaretPosition(pos);
        }

        ModernPopupMenu menu = new ModernPopupMenu();

        if (isImage) {
            populateImageContextMenu(menu, elem);
        } else if (cardOrShape != null && isCardOrShapeElement(cardOrShape)) {
            populateCardShapeContextMenu(menu, cardOrShape);
        } else if (tableCell != null) {
            populateTableContextMenu(menu, tableCell);
        } else if (videoCard != null) {
            populateVideoContextMenu(menu, videoCard);
        } else if (hasTextSelection) {
            populateTextSelectionContextMenu(menu);
        } else {
            populateCanvasContextMenu(menu);
        }

        menu.show(textPane, e.getX(), e.getY());
    }

    private void showImagePopupIfApplicable(MouseEvent e) {
        showCanvasContextMenu(e);
    }

    private Element findParentTableCell(Element elem) {
        Element cur = elem;
        while (cur != null) {
            String name = cur.getName();
            if ("td".equalsIgnoreCase(name) || "th".equalsIgnoreCase(name)) {
                return cur;
            }
            cur = cur.getParentElement();
        }
        return null;
    }

    private Icon createThemeDotIcon(Color c) {
        return new Icon() {
            @Override public void paintIcon(Component cmp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x, y + 2, 10, 10);
                g2.setColor(new Color(0, 0, 0, 40));
                g2.drawOval(x, y + 2, 10, 10);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 12; }
            @Override public int getIconHeight() { return 14; }
        };
    }

    private void populateCardShapeContextMenu(ModernPopupMenu menu, Element cardOrShape) {
        menu.addHeader("Card & Shape");
        menu.addModernSeparator();

        menu.addModernItem("Edit Values & Text...", VectorIcons.createIcon(VectorIcons.EDIT, 12, new Color(37, 99, 235)), ev -> openCardShapeEditor(cardOrShape));

        ModernPopupMenu.ModernMenu themeMenu = menu.createSubMenu("Change Color & Theme", VectorIcons.createIcon(VectorIcons.PALETTE, 12, new Color(124, 58, 237)));
        for (Map.Entry<String, CardShapeCustomizerDialog.ThemePalette> entry : CardShapeCustomizerDialog.PALETTES.entrySet()) {
            String key = entry.getKey();
            CardShapeCustomizerDialog.ThemePalette pal = entry.getValue();
            themeMenu.addModernItem(pal.name(), createThemeDotIcon(pal.primary()), ev -> applyCardShapeTheme(cardOrShape, key));
        }

        ModernPopupMenu.ModernMenu alignMenu = menu.createSubMenu("Align", VectorIcons.createIcon(VectorIcons.ALIGN_CENTER, 12, new Color(71, 85, 105)));
        alignMenu.addModernItem("Align Left", null, ev -> alignCardShapeElement(cardOrShape, "left"));
        alignMenu.addModernItem("Align Center", null, ev -> alignCardShapeElement(cardOrShape, "center"));
        alignMenu.addModernItem("Align Right", null, ev -> alignCardShapeElement(cardOrShape, "right"));

        menu.addModernSeparator();
        menu.addDangerItem("Delete Card / Shape", VectorIcons.createIcon(VectorIcons.DELETE, 12, new Color(220, 38, 38)), ev -> deleteCardShapeElement(cardOrShape));
    }

    private void populateImageContextMenu(ModernPopupMenu menu, Element elem) {
        lastClickedImageElement = elem;
        AttributeSet attrs = elem.getAttributes();
        Object src = attrs.getAttribute(HTML.Attribute.SRC);
        String srcStr = src != null ? src.toString() : "";
        boolean isChart = srcStr.contains("charts/chart_") || srcStr.contains(".wordbook/charts");

        if (isChart) {
            File chartFile = getFileFromSrc(srcStr);
            ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
            if (meta == null) {
                meta = new ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290);
            }
            final ChartMetadata finalMeta = meta;

            menu.addHeader("Chart");
            menu.addModernSeparator();

            menu.addModernItem("Edit Chart...", VectorIcons.createIcon(VectorIcons.CHART, 12, new Color(37, 99, 235)), ev -> openChartEditor(elem, chartFile));

            ModernPopupMenu.ModernMenu themeMenu = menu.createSubMenu("Theme", VectorIcons.createIcon(VectorIcons.PALETTE, 12, new Color(71, 85, 105)));
            for (ChartTheme theme : ChartTheme.values()) {
                themeMenu.addModernItem(theme.getDisplayName(), null, ev -> switchChartTheme(elem, chartFile, finalMeta, theme));
            }

            ModernPopupMenu.ModernMenu typeMenu = menu.createSubMenu("Type", VectorIcons.createIcon(VectorIcons.CHART, 12, new Color(71, 85, 105)));
            typeMenu.addModernItem("Bar Chart", null, ev -> switchChartType(elem, chartFile, finalMeta, ChartType.BAR));
            typeMenu.addModernItem("Line Graph", null, ev -> switchChartType(elem, chartFile, finalMeta, ChartType.LINE));
            typeMenu.addModernItem("Pie / Donut", null, ev -> switchChartType(elem, chartFile, finalMeta, ChartType.PIE));
            typeMenu.addModernItem("Treemap", null, ev -> switchChartType(elem, chartFile, finalMeta, ChartType.TREE));
            typeMenu.addModernItem("Hierarchy", null, ev -> switchChartType(elem, chartFile, finalMeta, ChartType.DIAGRAM));

            ModernPopupMenu.ModernMenu resizeMenu = menu.createSubMenu("Resize", VectorIcons.createIcon(VectorIcons.DOCUMENT, 12, new Color(71, 85, 105)));
            resizeMenu.addModernItem("Small (240px)", null, ev -> resizeImageElement(elem, 240));
            resizeMenu.addModernItem("Medium (420px)", null, ev -> resizeImageElement(elem, 420));
            resizeMenu.addModernItem("Large (580px)", null, ev -> resizeImageElement(elem, 580));
            resizeMenu.addModernItem("Full Width (720px)", null, ev -> resizeImageElement(elem, 720));

            ModernPopupMenu.ModernMenu alignMenu = menu.createSubMenu("Align", VectorIcons.createIcon(VectorIcons.ALIGN_CENTER, 12, new Color(71, 85, 105)));
            alignMenu.addModernItem("Align Left", null, ev -> alignImageElement(elem, "left"));
            alignMenu.addModernItem("Align Center", null, ev -> alignImageElement(elem, "center"));
            alignMenu.addModernItem("Align Right", null, ev -> alignImageElement(elem, "right"));

            menu.addModernSeparator();
            menu.addDangerItem("Delete Chart", VectorIcons.createIcon(VectorIcons.DELETE, 12, new Color(220, 38, 38)), ev -> deleteImageElement(elem));

        } else {
            menu.addHeader("Image");
            menu.addModernSeparator();

            ModernPopupMenu.ModernMenu filterMenu = menu.createSubMenu("Filters", VectorIcons.createIcon(VectorIcons.PALETTE, 12, new Color(71, 85, 105)));
            filterMenu.addModernItem("Original (Reset)", null, ev -> applyImageFilter("Original"));
            filterMenu.addModernItem("Grayscale", null, ev -> applyImageFilter("Grayscale"));
            filterMenu.addModernItem("Sepia", null, ev -> applyImageFilter("Sepia"));
            filterMenu.addModernItem("Vivid Colors", null, ev -> applyImageFilter("Vivid"));
            filterMenu.addModernItem("High Contrast", null, ev -> applyImageFilter("High Contrast"));
            filterMenu.addModernItem("Invert Colors", null, ev -> applyImageFilter("Invert"));
            filterMenu.add(new JSeparator());
            filterMenu.addModernItem("Rounded (24px)", null, ev -> applyImageFilter("Large Rounded"));
            filterMenu.addModernItem("Circular / Oval", null, ev -> applyImageFilter("Oval"));
            filterMenu.addModernItem("Drop Shadow", null, ev -> applyImageFilter("Floating Shadow"));
            filterMenu.addModernItem("Frame Border", null, ev -> applyImageFilter("Border"));
            filterMenu.addModernItem("Vignette Focus", null, ev -> applyImageFilter("Vignette"));
            filterMenu.addModernItem("Soft Blur", null, ev -> applyImageFilter("Blur"));

            ModernPopupMenu.ModernMenu wrapMenu = menu.createSubMenu("Wrap", VectorIcons.createIcon(VectorIcons.ALIGN_JUSTIFY, 12, new Color(71, 85, 105)));
            wrapMenu.addModernItem("Inline", null, ev -> setImageWrap("center"));
            wrapMenu.addModernItem("Float Left", null, ev -> setImageWrap("float-left"));
            wrapMenu.addModernItem("Float Right", null, ev -> setImageWrap("float-right"));

            ModernPopupMenu.ModernMenu resizeMenu = menu.createSubMenu("Resize", VectorIcons.createIcon(VectorIcons.DOCUMENT, 12, new Color(71, 85, 105)));
            resizeMenu.addModernItem("Small (180px)", null, ev -> resizeImageElement(elem, 180));
            resizeMenu.addModernItem("Medium (360px)", null, ev -> resizeImageElement(elem, 360));
            resizeMenu.addModernItem("Large (540px)", null, ev -> resizeImageElement(elem, 540));
            resizeMenu.addModernItem("Full Width (700px)", null, ev -> resizeImageElement(elem, 700));

            ModernPopupMenu.ModernMenu alignMenu = menu.createSubMenu("Align", VectorIcons.createIcon(VectorIcons.ALIGN_CENTER, 12, new Color(71, 85, 105)));
            alignMenu.addModernItem("Align Left", null, ev -> alignImageElement(elem, "left"));
            alignMenu.addModernItem("Align Center", null, ev -> alignImageElement(elem, "center"));
            alignMenu.addModernItem("Align Right", null, ev -> alignImageElement(elem, "right"));

            menu.addModernItem("Replace Image...", VectorIcons.createIcon(VectorIcons.IMAGE, 12, new Color(71, 85, 105)), ev -> replaceSelectedImageSource());
            menu.addModernItem("Layout...", VectorIcons.createIcon(VectorIcons.SETTINGS, 12, new Color(71, 85, 105)), ev -> openImageProperties(elem));

            menu.addModernSeparator();
            menu.addDangerItem("Delete Image", VectorIcons.createIcon(VectorIcons.DELETE, 12, new Color(220, 38, 38)), ev -> deleteImageElement(elem));
        }
    }

    private void populateTableContextMenu(ModernPopupMenu menu, Element tableCell) {
        menu.addHeader("Table");
        menu.addModernSeparator();

        menu.addModernItem("Insert Row Above", null, ev -> insertTableRowAbove());
        menu.addModernItem("Insert Row Below", null, ev -> insertTableRowBelow());
        menu.addModernItem("Insert Col Left", null, ev -> insertTableColumnLeft());
        menu.addModernItem("Insert Col Right", null, ev -> insertTableColumnRight());

        menu.addModernSeparator();

        ModernPopupMenu.ModernMenu styleMenu = menu.createSubMenu("Table Style", VectorIcons.createIcon(VectorIcons.PALETTE, 12, new Color(71, 85, 105)));
        String[] styles = {"Corporate Blue", "Modern Slate", "Emerald Green", "Striped Zebra", "Minimalist"};
        for (String st : styles) {
            styleMenu.addModernItem(st, null, ev -> applyTableStyle(st));
        }

        menu.addModernSeparator();

        menu.addModernItem("Delete Row", null, ev -> deleteTableRow());
        menu.addModernItem("Delete Column", null, ev -> deleteTableColumn());
        menu.addDangerItem("Delete Table", VectorIcons.createIcon(VectorIcons.DELETE, 12, new Color(220, 38, 38)), ev -> deleteTable());
    }

    private void populateVideoContextMenu(ModernPopupMenu menu, Element videoCard) {
        menu.addHeader("Video");
        menu.addModernSeparator();

        menu.addModernItem("▶ Play Video", null, ev -> playOrOpenVideo(videoCard));
        menu.addModernItem("Edit Source...", null, ev -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof WordBookMainFrame mf) mf.showVideoDialog();
        });

        menu.addModernSeparator();
        menu.addDangerItem("Delete Video", VectorIcons.createIcon(VectorIcons.DELETE, 12, new Color(220, 38, 38)), ev -> deleteSelectedVideo());
    }

    private void populateTextSelectionContextMenu(ModernPopupMenu menu) {
        menu.addHeader("Text");
        menu.addModernSeparator();

        menu.addModernItem("Cut (Ctrl+X)", VectorIcons.createIcon(VectorIcons.Type.CUT, 12, new Color(71, 85, 105)), ev -> textPane.cut());
        menu.addModernItem("Copy (Ctrl+C)", VectorIcons.createIcon(VectorIcons.Type.COPY, 12, new Color(71, 85, 105)), ev -> textPane.copy());
        menu.addModernItem("Paste (Ctrl+V)", VectorIcons.createIcon(VectorIcons.Type.PASTE, 12, new Color(71, 85, 105)), ev -> textPane.paste());

        menu.addModernSeparator();

        ModernPopupMenu.ModernMenu fmtMenu = menu.createSubMenu("Format", VectorIcons.createIcon(VectorIcons.Type.BOLD, 12, new Color(71, 85, 105)));
        fmtMenu.addModernItem("Bold (Ctrl+B)", null, ev -> applyFormat("bold", null));
        fmtMenu.addModernItem("Italic (Ctrl+I)", null, ev -> applyFormat("italic", null));
        fmtMenu.addModernItem("Underline (Ctrl+U)", null, ev -> applyFormat("underline", null));
        fmtMenu.addModernItem("Strikethrough", null, ev -> applyFormat("strikethrough", null));
        fmtMenu.add(new JSeparator());
        fmtMenu.addModernItem("Clear Format", null, ev -> applyFormat("clear", null));

        ModernPopupMenu.ModernMenu headMenu = menu.createSubMenu("Headings", VectorIcons.createIcon(VectorIcons.DOCUMENT, 12, new Color(71, 85, 105)));
        headMenu.addModernItem("Heading 1", null, ev -> applyFormat("heading", "Heading 1"));
        headMenu.addModernItem("Heading 2", null, ev -> applyFormat("heading", "Heading 2"));
        headMenu.addModernItem("Heading 3", null, ev -> applyFormat("heading", "Heading 3"));
        headMenu.addModernItem("Normal", null, ev -> applyFormat("heading", "Normal"));

        ModernPopupMenu.ModernMenu listMenu = menu.createSubMenu("Lists", VectorIcons.createIcon(VectorIcons.Type.LIST_BULLET, 12, new Color(71, 85, 105)));
        listMenu.addModernItem("Bullet List", null, ev -> applyFormat("bullet-list", null));
        listMenu.addModernItem("Numbered List", null, ev -> applyFormat("numbered-list", null));
        listMenu.addModernItem("Checklist", null, ev -> applyFormat("checkbox", null));

        menu.addModernSeparator();

        menu.addModernItem("Proofread...", VectorIcons.createIcon(VectorIcons.CHECKMARK, 12, new Color(16, 185, 129)), ev -> runGrammarCheck());
        menu.addModernItem("🔊 Speak Aloud", null, ev -> speakSelectionOrDocument());
    }

    private void populateCanvasContextMenu(ModernPopupMenu menu) {
        menu.addHeader("Canvas");
        menu.addModernSeparator();

        menu.addModernItem("Undo (Ctrl+Z)", VectorIcons.createIcon(VectorIcons.Type.UNDO, 12, new Color(71, 85, 105)), ev -> { if (undoManager.canUndo()) undoManager.undo(); });
        menu.addModernItem("Redo (Ctrl+Y)", VectorIcons.createIcon(VectorIcons.Type.REDO, 12, new Color(71, 85, 105)), ev -> { if (undoManager.canRedo()) undoManager.redo(); });
        menu.addModernItem("Paste (Ctrl+V)", VectorIcons.createIcon(VectorIcons.Type.PASTE, 12, new Color(71, 85, 105)), ev -> textPane.paste());

        menu.addModernSeparator();

        ModernPopupMenu.ModernMenu insertMenu = menu.createSubMenu("Insert", VectorIcons.createIcon(VectorIcons.DOCUMENT, 12, new Color(71, 85, 105)));
        insertMenu.addModernItem("Image...", null, ev -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof WordBookMainFrame mf) mf.insertLocalImage();
        });
        insertMenu.addModernItem("Chart...", null, ev -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof WordBookMainFrame mf) mf.showChartDialog();
        });
        insertMenu.addModernItem("Table 3×3", null, ev -> insertHtml("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px 0;border:1px solid #cbd5e1;'><tr><th style='background:#f1f5f9;padding:8px 12px;font-weight:bold;'>Header 1</th><th style='background:#f1f5f9;padding:8px 12px;font-weight:bold;'>Header 2</th><th style='background:#f1f5f9;padding:8px 12px;font-weight:bold;'>Header 3</th></tr><tr><td style='padding:8px 12px;'>Cell 1</td><td style='padding:8px 12px;'>Cell 2</td><td style='padding:8px 12px;'>Cell 3</td></tr><tr><td style='padding:8px 12px;'>Cell 4</td><td style='padding:8px 12px;'>Cell 5</td><td style='padding:8px 12px;'>Cell 6</td></tr></table><p><br></p>"));
        insertMenu.addModernItem("Code Block", null, ev -> insertHtml("<pre style='background:#1e293b;color:#f8fafc;padding:12px;border-radius:6px;font-family:monospace;'>// Enter code here</pre><p><br></p>"));
        insertMenu.addModernItem("Divider", null, ev -> insertHtml("<hr style='border:none;border-top:1px solid #cbd5e1;margin:18px 0;'><p><br></p>"));

        menu.addModernSeparator();

        menu.addModernItem("Structure", null, ev -> runAutoDetectStructure());
        menu.addModernItem("Grammar", VectorIcons.createIcon(VectorIcons.CHECKMARK, 12, new Color(16, 185, 129)), ev -> autoFixAllGrammar());
        menu.addModernItem("🔊 Speak Aloud", null, ev -> speakSelectionOrDocument());

        menu.addModernSeparator();

        menu.addModernItem("Select All (Ctrl+A)", null, ev -> textPane.selectAll());
    }

    private File getFileFromSrc(String srcStr) {
        if (srcStr == null || srcStr.isBlank()) return null;
        try {
            if (srcStr.startsWith("file:")) {
                String clean = srcStr;
                while (clean.startsWith("file:////")) clean = "file:/" + clean.substring(8);
                while (clean.startsWith("file:///")) clean = "file:/" + clean.substring(7);
                try {
                    return new File(new java.net.URI(clean));
                } catch (Exception e) {
                    return new File(new java.net.URL(clean).toURI());
                }
            }
            return new File(srcStr);
        } catch (Exception ex) {
            return new File(srcStr.replaceAll("^file:/+", "/"));
        }
    }

    public void autoSelectChartFile(File chartFile) {
        if (chartFile == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = (HTMLDocument) textPane.getDocument();
                Element elem = findImageBySrc(doc.getDefaultRootElement(), chartFile.getName());
                if (elem != null) {
                    selectedMediaElement = elem;
                    lastClickedImageElement = elem;
                    selectedMediaBounds = getElementBounds(elem);
                    textPane.repaint();
                    if (contextualTabHandler != null) {
                        contextualTabHandler.showContextualTab(RibbonPanel.TAB_CHART_DESIGN);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private Element findImageBySrc(Element elem, String namePart) {
        if (elem == null) return null;
        if (HTML.Tag.IMG.equals(elem.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
            Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
            if (src != null && src.toString().contains(namePart)) {
                return elem;
            }
        }
        int count = elem.getElementCount();
        for (int i = 0; i < count; i++) {
            Element found = findImageBySrc(elem.getElement(i), namePart);
            if (found != null) return found;
        }
        return null;
    }

    public Element getSelectedOrLastImageElement() {
        if (selectedMediaElement != null) return selectedMediaElement;
        return lastClickedImageElement;
    }

    public ChartMetadata getSelectedChartMetadata() {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return null;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return null;
        File chartFile = getFileFromSrc(src.toString());
        return chartEngineService.readChartMetadata(chartFile);
    }

    public void modifySelectedChartData() {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        openChartEditor(elem, chartFile);
    }

    public void switchSelectedChartType(String typeStr) {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        ChartType newType = ChartCatalog.parse(typeStr);
        reRenderSelectedChartWith(elem, chartFile, newType, meta.theme(), meta.cardStyle(), meta.detailLevel(), meta.showValues(), meta.showLegend(), meta.paletteHex());
    }

    public void switchSelectedChartTheme(String themeStr) {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        ChartTheme newTheme = ChartCatalog.parseTheme(themeStr);
        reRenderSelectedChartWith(elem, chartFile, meta.type(), newTheme, meta.cardStyle(), meta.detailLevel(), meta.showValues(), meta.showLegend(), null);
    }

    public void showChartPaletteDialog() {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        ChartPalettePanel.ChartPaletteChoice choice = new ChartPalettePanel.ChartPaletteChoice(meta.theme(), meta.paletteHex());
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        if (ChartPalettePanel.showDialog(owner != null ? owner : this, choice)) {
            reRenderSelectedChartWith(elem, chartFile, meta.type(), choice.theme, meta.cardStyle(), meta.detailLevel(), meta.showValues(), meta.showLegend(), choice.paletteHex);
        }
    }

    public void switchSelectedChartStyle(String styleStr) {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        reRenderSelectedChartWith(elem, chartFile, meta.type(), meta.theme(), styleStr, meta.detailLevel(), meta.showValues(), meta.showLegend(), meta.paletteHex());
    }

    public void setChartDetailLevel(String detailLevel) {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        reRenderSelectedChartWith(elem, chartFile, meta.type(), meta.theme(), meta.cardStyle(), detailLevel, meta.showValues(), meta.showLegend(), meta.paletteHex());
    }

    public void toggleChartValues() {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        reRenderSelectedChartWith(elem, chartFile, meta.type(), meta.theme(), meta.cardStyle(), meta.detailLevel(), !meta.showValues(), meta.showLegend(), meta.paletteHex());
    }

    public void toggleChartLegend() {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        reRenderSelectedChartWith(elem, chartFile, meta.type(), meta.theme(), meta.cardStyle(), meta.detailLevel(), meta.showValues(), !meta.showLegend(), meta.paletteHex());
    }

    public void reRenderSelectedChart() {
        Element elem = getSelectedOrLastImageElement();
        if (elem == null) return;
        Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) return;
        File chartFile = getFileFromSrc(src.toString());
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        if (meta == null) {
            meta = new ChartEngineService.ChartMetadata(ChartType.BAR, "Chart", ChartTheme.CORPORATE_BLUE, "Item 1: 45\nItem 2: 70\nItem 3: 95", 580, 290, "MODERN_GLASS", "FULL", true, true, null);
        }

        reRenderSelectedChartWith(elem, chartFile, meta.type(), meta.theme(), meta.cardStyle(), meta.detailLevel(), meta.showValues(), meta.showLegend(), meta.paletteHex());
    }

    private void reRenderSelectedChartWith(Element elem, File chartFile, ChartType type, ChartTheme theme,
                                           String cardStyle, String detailLevel, boolean showValues, boolean showLegend) {
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        reRenderSelectedChartWith(elem, chartFile, type, theme, cardStyle, detailLevel, showValues, showLegend, meta != null ? meta.paletteHex() : null);
    }

    private void reRenderSelectedChartWith(Element elem, File chartFile, ChartType type, ChartTheme theme,
                                           String cardStyle, String detailLevel, boolean showValues, boolean showLegend, String paletteHex) {
        try {
            ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
            String rawData = meta != null ? meta.rawData() : "Item 1: 45\nItem 2: 70\nItem 3: 95";
            String title = meta != null ? meta.title() : "Chart";
            int w = getElementWidth(elem, 580);
            int h = (meta != null && meta.height() > 50) ? meta.height() : 290;

            File newFile = chartEngineService.renderChartToFile(type, rawData, theme, title, w, h, cardStyle, detailLevel, showValues, showLegend, paletteHex);
            String newUrl;
            try {
                newUrl = newFile.toURI().toURL().toExternalForm();
            } catch (Exception e) {
                newUrl = "file:/" + newFile.getAbsolutePath().replace('\\', '/').replaceAll("^/+", "");
            }
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            String newImgHtml = "<img src='" + newUrl + "' width='" + w + "' height='" + h + "'>";
            suppressEvents = true;
            try {
                doc.setOuterHTML(elem, newImgHtml);
            } finally {
                suppressEvents = false;
            }
            triggerSave();

            // Synchronously re-acquire selected element immediately
            Element newElem = findImageBySrc(doc.getDefaultRootElement(), newFile.getName());
            if (newElem != null) {
                selectedMediaElement = newElem;
                lastClickedImageElement = newElem;
                selectedMediaBounds = getElementBounds(newElem);
            }
            autoSelectChartFile(newFile);
            pageLayoutTimer.restart();
            textPane.repaint();
            pageSheet.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openChartEditor(Element elem, File chartFile) {
        ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        ChartDialog dlg = new ChartDialog(owner, chartEngineService);
        dlg.loadExistingChart(chartFile, meta);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            File newFile = dlg.getGeneratedChartFile();
            if (newFile != null && newFile.exists()) {
                String newUrl;
                try {
                    newUrl = newFile.toURI().toURL().toExternalForm();
                } catch (Exception e) {
                    newUrl = "file:/" + newFile.getAbsolutePath().replace('\\', '/').replaceAll("^/+", "");
                }
                int currentW = getElementWidth(elem, 580);
                try {
                    HTMLDocument doc = (HTMLDocument) textPane.getDocument();
                    String newImgHtml = "<img src='" + newUrl + "' width='" + currentW + "'>";
                    suppressEvents = true;
                    try {
                        doc.setOuterHTML(elem, newImgHtml);
                    } finally {
                        suppressEvents = false;
                    }
                    triggerSave();
                    Element newElem = findImageBySrc(doc.getDefaultRootElement(), newFile.getName());
                    if (newElem != null) {
                        selectedMediaElement = newElem;
                        lastClickedImageElement = newElem;
                        selectedMediaBounds = getElementBounds(newElem);
                    }
                    autoSelectChartFile(newFile);
                    pageLayoutTimer.restart();
                    textPane.repaint();
                    pageSheet.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void switchChartTheme(Element elem, File chartFile, ChartMetadata meta, ChartTheme newTheme) {
        reRenderSelectedChartWith(elem, chartFile, meta.type(), newTheme, meta.cardStyle(), meta.detailLevel(), meta.showValues(), meta.showLegend());
    }

    private void switchChartType(Element elem, File chartFile, ChartMetadata meta, ChartType newType) {
        reRenderSelectedChartWith(elem, chartFile, newType, meta.theme(), meta.cardStyle(), meta.detailLevel(), meta.showValues(), meta.showLegend());
    }

    public void replaceSelectedImageSource() {
        Element elem = getLastClickedImageElement();
        if (elem == null) {
            ModernDialog.showMessage(this, "Replace Image", "Click an image in the document first.", ModernDialog.MessageType.INFO);
            return;
        }

        String[] options = {"Choose Local File...", "Enter Web URL...", "Cancel"};
        int choice = ModernDialog.showOption(this, "Replace Image Source", "Select new image source:", options, 0);

        if (choice == 0) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select New Image File");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (PNG, JPG, SVG, WEBP, GIF)", "png", "jpg", "jpeg", "svg", "webp", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    updateImageSrc(elem, f.toURI().toURL().toExternalForm());
                } catch (Exception e) {
                    updateImageSrc(elem, "file:/" + f.getAbsolutePath().replace('\\', '/').replaceAll("^/+", ""));
                }
            }
        } else if (choice == 1) {
            String url = ModernDialog.showInput(this, "Replace Image URL", "Enter New Image URL (https://...):", "https://");
            if (url != null && !url.isBlank()) {
                updateImageSrc(elem, url.trim());
            }
        }
    }

    public void updateImageSrc(Element imgElem, String newSrc) {
        if (imgElem == null || newSrc == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int width = getElementWidth(imgElem, 520);
            String newImgHtml = "<img src='" + newSrc + "' width='" + width + "' style='border:1px solid #cbd5e1;border-radius:8px;'>";
            suppressEvents = true;
            try {
                doc.setOuterHTML(imgElem, newImgHtml);
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private int getElementWidth(Element elem, int defVal) {
        if (elem == null) return defVal;
        try {
            Object w = elem.getAttributes().getAttribute(HTML.Attribute.WIDTH);
            if (w != null) return Integer.parseInt(w.toString());
        } catch (Exception ignored) {}
        return defVal;
    }

    private int getElementHeight(Element elem, int defVal) {
        if (elem == null) return defVal;
        try {
            Object h = elem.getAttributes().getAttribute(HTML.Attribute.HEIGHT);
            if (h != null) return Integer.parseInt(h.toString());
        } catch (Exception ignored) {}
        return defVal;
    }

    public int getImageElementWidth(Element imgElem) {
        return getElementWidth(imgElem, 580);
    }

    public void resizeImageElement(Element imgElem, int newWidth) {
        if (imgElem == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            AttributeSet attrs = imgElem.getAttributes();
            Object src = attrs.getAttribute(HTML.Attribute.SRC);
            if (src == null) return;

            int oldW = getElementWidth(imgElem, 520);
            int oldH = getElementHeight(imgElem, 290);
            int newH = (int) Math.round((double) oldH * newWidth / Math.max(1, oldW));

            Object dataFilter = attrs.getAttribute("data-filter");
            Object styleAttr = attrs.getAttribute(HTML.Attribute.STYLE);
            String filterAttrStr = dataFilter != null ? " data-filter='" + dataFilter + "'" : "";
            String styleAttrStr = styleAttr != null ? " style='" + styleAttr + "'" : " style='border:1px solid #cbd5e1;border-radius:8px;'";

            boolean isChart = src.toString().contains("charts/chart_") || src.toString().contains(".wordbook/charts");
            String newImgHtml;
            String targetSrc = src.toString();
            if (isChart) {
                File chartFile = getFileFromSrc(src.toString());
                ChartMetadata meta = chartEngineService.readChartMetadata(chartFile);
                if (meta != null) {
                    File newFile = chartEngineService.renderChartToFile(meta.type(), meta.rawData(), meta.theme(), meta.title(), newWidth, newH, meta.cardStyle(), meta.detailLevel(), meta.showValues(), meta.showLegend(), meta.paletteHex());
                    String newUrl = newFile.toURI().toURL().toExternalForm();
                    newImgHtml = "<img src='" + newUrl + "' width='" + newWidth + "' height='" + newH + "'" + filterAttrStr + styleAttrStr + ">";
                    targetSrc = newFile.getName();
                } else {
                    newImgHtml = "<img src='" + src + "' width='" + newWidth + "' height='" + newH + "'" + filterAttrStr + styleAttrStr + ">";
                }
            } else {
                newImgHtml = "<img src='" + src + "' width='" + newWidth + "' height='" + newH + "'" + filterAttrStr + styleAttrStr + ">";
            }

            suppressEvents = true;
            try {
                doc.setOuterHTML(imgElem, newImgHtml);
            } finally {
                suppressEvents = false;
            }

            // Immediately re-select element to prevent stale references
            Element newElem = findImageBySrc(doc.getDefaultRootElement(), targetSrc);
            if (newElem != null) {
                selectedMediaElement = newElem;
                lastClickedImageElement = newElem;
                selectedMediaBounds = getElementBounds(newElem);
            }

            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
            pageSheet.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void alignImageElement(Element imgElem, String alignment) {
        if (imgElem == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element p = imgElem.getParentElement();
            SimpleAttributeSet sa = new SimpleAttributeSet();
            if ("center".equalsIgnoreCase(alignment)) StyleConstants.setAlignment(sa, StyleConstants.ALIGN_CENTER);
            else if ("right".equalsIgnoreCase(alignment)) StyleConstants.setAlignment(sa, StyleConstants.ALIGN_RIGHT);
            else StyleConstants.setAlignment(sa, StyleConstants.ALIGN_LEFT);

            suppressEvents = true;
            try {
                doc.setParagraphAttributes(p.getStartOffset(), p.getEndOffset() - p.getStartOffset(), sa, false);
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void deleteImageElement(Element imgElem) {
        if (imgElem == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element p = imgElem.getParentElement();
            suppressEvents = true;
            try {
                doc.remove(p.getStartOffset(), p.getEndOffset() - p.getStartOffset());
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Element getLastClickedImageElement() {
        if (lastClickedImageElement != null) return lastClickedImageElement;
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        for (int i = 0; i < doc.getLength(); i++) {
            Element elem = doc.getCharacterElement(i);
            if (HTML.Tag.IMG.equals(elem.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
                lastClickedImageElement = elem;
                return elem;
            }
        }
        return null;
    }

    public void openImagePropertiesDialog() {
        Element elem = getLastClickedImageElement();
        if (elem != null) {
            openImageProperties(elem);
        } else {
            ModernDialog.showMessage(this, "Image Layout", "No image currently selected. Click an image in the document first.", ModernDialog.MessageType.INFO);
        }
    }

    private void openImageProperties(Element imgElem) {
        if (imgElem == null) return;
        int width = getElementWidth(imgElem, 520);
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        ImagePropertiesDialog dlg = new ImagePropertiesDialog(owner, width, "Center", "Figure Illustration");
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            resizeImageElement(imgElem, dlg.getSelectedWidth());
            if (dlg.getSelectedAlign().toLowerCase().contains("left")) alignImageElement(imgElem, "left");
            else if (dlg.getSelectedAlign().toLowerCase().contains("right")) alignImageElement(imgElem, "right");
            else alignImageElement(imgElem, "center");
        }
    }

    // Page Format, Margins, and Paper Tone Setup
    public void setPageFormat(PageFormat fmt) {
        if (fmt == null) return;
        this.currentPageFormat = fmt;
        updateDynamicPageHeight();
        pageSheet.revalidate();
        pageSheet.repaint();
    }

    public void setPageMargin(PageMargin margin) {
        if (margin == null) return;
        this.currentPageMargin = margin;
        pageSheet.setBorder(new EmptyBorder(margin.top, margin.left, margin.bottom, margin.right));
        updateDynamicPageHeight();
        pageSheet.revalidate();
        pageSheet.repaint();
    }

    public void setPagePaperTone(String toneName) {
        if (toneName == null) return;
        switch (toneName.toLowerCase()) {
            case "warm cream" -> this.currentPaperTone = new Color(254, 252, 249);
            case "soft gray" -> this.currentPaperTone = new Color(248, 250, 252);
            default -> this.currentPaperTone = Color.WHITE;
        }
        textPane.setBackground(currentPaperTone);
        pageSheet.repaint();
    }

    public int getPageCount() {
        return Math.max(1, cachedPageCount);
    }

    private int countPageBreaks() {
        try {
            Document doc = textPane.getDocument();
            if (!(doc instanceof HTMLDocument htmlDoc)) return 1;
            Element root = htmlDoc.getDefaultRootElement();
            int count = countPageBreaksInElement(root);
            return Math.max(1, count + 1);
        } catch (Exception e) {
            return 1;
        }
    }

    private int countPageBreaksInElement(Element elem) {
        if (elem == null) return 0;
        AttributeSet attrs = elem.getAttributes();
        if (attrs != null) {
            Object style = attrs.getAttribute(HTML.Attribute.STYLE);
            if (style != null && style.toString().contains("page-break")) {
                return 1;
            }
            Object classAttr = attrs.getAttribute(HTML.Attribute.CLASS);
            if (classAttr != null && classAttr.toString().contains("page-break")) {
                return 1;
            }
        }
        int count = 0;
        int childCount = elem.getElementCount();
        for (int i = 0; i < childCount; i++) {
            count += countPageBreaksInElement(elem.getElement(i));
        }
        return count;
    }

    public int getMaxPrintableWidth() {
        int baseW = currentPageFormat.width;
        return Math.max(300, baseW - currentPageMargin.left - currentPageMargin.right - 30);
    }

    public String adjustMediaToPageSize(String html) {
        if (html == null || html.isEmpty()) return html;
        int maxW = getMaxPrintableWidth();
        if (maxW <= 0) maxW = 620;

        String res = html;

        // 1. Move any misplaced visual elements out of <head> into <body>
        java.util.regex.Matcher headMatcher = java.util.regex.Pattern.compile("(?is)<head>(.*?)</head>").matcher(res);
        if (headMatcher.find()) {
            String headContent = headMatcher.group(1);
            if (headContent.contains("<img") || headContent.contains("<div") || headContent.contains("<table") || headContent.contains("<p")) {
                java.util.regex.Matcher visualMatcher = java.util.regex.Pattern.compile("(?is)(<(?:div|p|table)[^>]*?>.*?</(?:div|p|table)>|<img[^>]*?>)").matcher(headContent);
                StringBuilder movedVisuals = new StringBuilder();
                while (visualMatcher.find()) {
                    movedVisuals.append(visualMatcher.group(1)).append("\n");
                }
                String cleanedHead = visualMatcher.replaceAll("");
                res = res.replace(headMatcher.group(0), "<head>" + cleanedHead + "</head>");
                if (movedVisuals.length() > 0) {
                    if (res.contains("<body")) {
                        res = res.replaceFirst("(?i)(<body[^>]*?>)", "$1\n" + java.util.regex.Matcher.quoteReplacement(movedVisuals.toString()));
                    } else {
                        res = movedVisuals + res;
                    }
                }
            }
        }

        // 2. Separate adjacent <img> and caption tags with line breaks so they never sit side-by-side
        res = res.replaceAll("(?i)(<img[^>]*?>)(\\s*)(<img[^>]*?>)", "$1<br><br>$3");
        res = res.replaceAll("(?i)(</span>)(\\s*)(<img[^>]*?>)", "$1<br><br>$3");
        res = res.replaceAll("(?i)(</div>)(\\s*)(<img[^>]*?>)", "$1<p><br></p>$3");
        res = res.replaceAll("(?i)(</p>)(\\s*)(<img[^>]*?>)", "$1<p><br></p>$3");

        // 3. Auto-fit ALL <img> tags to page printable size
        final int finalMaxW = maxW;
        java.util.regex.Pattern anyImgPattern = java.util.regex.Pattern.compile("(?i)<img\\b([^>]*?)>");
        java.util.regex.Matcher m = anyImgPattern.matcher(res);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                String attrs = m.group(1);
                java.util.regex.Matcher wm = java.util.regex.Pattern.compile("(?i)width=['\"]?(\\d+)['\"]?").matcher(attrs);
                java.util.regex.Matcher hm = java.util.regex.Pattern.compile("(?i)height=['\"]?(\\d+)['\"]?").matcher(attrs);

                if (wm.find()) {
                    int w = Integer.parseInt(wm.group(1));
                    if (w > finalMaxW) {
                        String updatedAttrs = attrs.replaceFirst("(?i)width=['\"]?\\d+['\"]?", "width='" + finalMaxW + "'");
                        if (hm.find()) {
                            int h = Integer.parseInt(hm.group(1));
                            int newH = Math.max(60, (int) Math.round((double) h * finalMaxW / w));
                            updatedAttrs = updatedAttrs.replaceFirst("(?i)height=['\"]?\\d+['\"]?", "height='" + newH + "'");
                        }
                        m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("<img" + updatedAttrs + ">"));
                        continue;
                    }
                } else {
                    // No width specified on <img> tag! Auto-detect and inject clamped width & height
                    int targetW = finalMaxW;
                    int targetH = 340;
                    java.util.regex.Matcher srcMatcher = java.util.regex.Pattern.compile("(?i)src=['\"]([^'\"]+)['\"]").matcher(attrs);
                    if (srcMatcher.find()) {
                        String srcVal = srcMatcher.group(1);
                        File f = getFileFromSrc(srcVal);
                        if (f != null && f.exists()) {
                            try {
                                BufferedImage bimg = ImageIO.read(f);
                                if (bimg != null && bimg.getWidth() > 0 && bimg.getHeight() > 0) {
                                    int origW = bimg.getWidth();
                                    int origH = bimg.getHeight();
                                    if (origW <= finalMaxW) {
                                        targetW = origW;
                                        targetH = origH;
                                    } else {
                                        targetW = finalMaxW;
                                        targetH = Math.max(60, (int) Math.round((double) origH * finalMaxW / origW));
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    String updatedAttrs = attrs + " width='" + targetW + "' height='" + targetH + "'";
                    m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("<img" + updatedAttrs + ">"));
                    continue;
                }
            } catch (Exception ignored) {}
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group(0)));
        }
        m.appendTail(sb);
        res = sb.toString();

        // Ensure video cards never exceed printable canvas width
        res = res.replaceAll("(?i)(class=['\"]wb-video-card['\"][^>]*?style=['\"])([^'\"]*?)(['\"])",
                "$1$2;max-width:100%;width:96%;margin:14px auto;$3");

        // 4. Ensure tables don't exceed maxW and have align='center'
        java.util.regex.Pattern tableWidthPattern = java.util.regex.Pattern.compile("(?i)(<table[^>]*?width=['\"]?)(\\d+)(['\"]?[^>]*?>)");
        java.util.regex.Matcher tm = tableWidthPattern.matcher(res);
        StringBuffer tsb = new StringBuffer();
        while (tm.find()) {
            try {
                int w = Integer.parseInt(tm.group(2));
                if (w > maxW) {
                    String fullTag = tm.group(0);
                    String replacement = fullTag.replaceFirst("(?i)width=['\"]?\\d+['\"]?", "width='100%'");
                    tm.appendReplacement(tsb, java.util.regex.Matcher.quoteReplacement(replacement));
                    continue;
                }
            } catch (Exception ignored) {}
            tm.appendReplacement(tsb, java.util.regex.Matcher.quoteReplacement(tm.group(0)));
        }
        tm.appendTail(tsb);
        res = tsb.toString();

        // Add align='center' to tables that don't specify align
        res = res.replaceAll("(?i)<table(?![^>]*?\\balign=)", "<table align='center'");

        // 5. Remove any old editable table page break rules or legacy div breaks
        res = res.replaceAll("(?i)<table[^>]*?>[\\s\\S]*?PAGE BREAK[\\s\\S]*?</table>",
                "<div class='page-break-container' contenteditable='false' style='page-break-before:always;margin:32px 0 24px 0;text-align:center;user-select:none;border-top:2px dashed #94a3b8;height:24px;line-height:24px;font-size:10px;font-weight:bold;color:#64748b;letter-spacing:1px;'>PAGE BREAK</div>");
        res = res.replaceAll("(?i)<div class=['\"]page-break-container['\"][^>]*?>[\\s\\S]*?</div>",
                "<div class='page-break-container' contenteditable='false' style='page-break-before:always;margin:32px 0 24px 0;text-align:center;user-select:none;border-top:2px dashed #94a3b8;height:24px;line-height:24px;font-size:10px;font-weight:bold;color:#64748b;letter-spacing:1px;'>PAGE BREAK</div>");

        return res;
    }

    public void insertPageBreak() {
        int nextPg = getPageCount() + 1;
        String pb = "<div class='page-break-container' contenteditable='false' style='page-break-before:always;margin:32px 0 24px 0;text-align:center;user-select:none;border-top:2px dashed #94a3b8;height:24px;line-height:24px;font-size:10px;font-weight:bold;color:#64748b;letter-spacing:1px;'>" +
                "PAGE BREAK" +
                "</div><p><br></p>";
        insertHtml(pb);
    }

    private static final String[] LOREM_DICTIONARY = {
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
        "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore",
        "magna", "aliqua", "ut", "enim", "ad", "minim", "veniam", "quis", "nostrud",
        "exercitation", "ullamco", "laboris", "nisi", "ut", "aliquip", "ex", "ea",
        "commodo", "consequat", "duis", "aute", "irure", "dolor", "in", "reprehenderit",
        "in", "voluptate", "velit", "esse", "cillum", "dolore", "eu", "fugiat", "nulla",
        "pariatur", "excepteur", "sint", "occaecat", "cupidatat", "non", "proident",
        "sunt", "in", "culpa", "qui", "officia", "deserunt", "mollit", "anim", "id",
        "est", "laborum", "at", "vero", "eos", "et", "accusamus", "et", "iusto",
        "odio", "dignissimos", "ducimus", "qui", "blanditiis", "praesentium", "voluptatum",
        "deleniti", "atque", "corrupti", "quos", "dolores", "et", "quas", "molestias",
        "excepturi", "sint", "occaecati", "cupiditate", "non", "provident", "similique",
        "sunt", "in", "culpa", "qui", "officia", "deserunt", "mollitia", "animi", "id",
        "est", "laborum", "et", "dolorum", "fuga", "harum", "quidem", "rerum", "facilis",
        "expedita", "distinctio", "nam", "libero", "tempore", "cum", "soluta", "nobis",
        "eligendi", "optio", "cumque", "nihil", "impedit", "quo", "minus", "quod",
        "maxime", "placeat", "facere", "possimus", "omnis", "voluptas", "assumenda",
        "repellendus", "temporibus", "autem", "quibusdam", "debitis", "necessitatibus",
        "saepe", "eveniet", "voluptates", "repudiandae", "recusandae", "itaque", "earum"
    };

    public static String generateLoremWords(int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int sentenceLen = 8 + (count % 5);
        int wordsInSentence = 0;
        boolean startOfSentence = true;

        for (int i = 0; i < count; i++) {
            String word = LOREM_DICTIONARY[i % LOREM_DICTIONARY.length];
            if (startOfSentence) {
                word = Character.toUpperCase(word.charAt(0)) + word.substring(1);
                startOfSentence = false;
            }

            sb.append(word);
            wordsInSentence++;

            if (i == count - 1) {
                sb.append(".");
            } else if (wordsInSentence >= sentenceLen) {
                sb.append(". ");
                wordsInSentence = 0;
                startOfSentence = true;
                sentenceLen = 8 + ((i * 7) % 7);
            } else if (wordsInSentence == 4 || wordsInSentence == 9) {
                sb.append(", ");
            } else {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public boolean handleLoremCommand() {
        try {
            int caret = textPane.getCaretPosition();
            if (caret <= 0) return false;

            int lookback = Math.min(100, caret);
            int startPos = caret - lookback;
            String textBefore = textPane.getDocument().getText(startPos, lookback);

            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?i)(?:^|[\\s\\u00A0])(=?lorem\\s*\\(?\\s*(\\d+)?\\s*\\)?\\s*)$"
            ).matcher(textBefore);

            if (m.find()) {
                String fullToken = m.group(1);
                String numStr = m.group(2);
                int count = (numStr != null && !numStr.isBlank()) ? Integer.parseInt(numStr) : 0;
                if (count == 0 && "lorem".equalsIgnoreCase(fullToken.trim())) {
                    count = 50;
                }
                if (count > 0 && count <= 5000) {
                    boolean confirm = ModernDialog.showConfirm(
                        this,
                        "Generate Lorem Ipsum",
                        "Create " + count + " sample words using Lorem Ipsum?",
                        ModernDialog.MessageType.CONFIRM
                    );
                    if (confirm) {
                        int tokenStart = caret - fullToken.length();
                        textPane.select(tokenStart, caret);
                        String loremText = generateLoremWords(count);
                        textPane.replaceSelection(loremText);
                        triggerSave();
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            // ignore and allow normal Enter
        }
        return false;
    }

    public void triggerSave() {
        if (suppressEvents) return;
        String html = getDocumentHtml();
        String text = getPlainText();
        int words = countWords(text);
        int chars = text.length();

        if (changeHandler != null) {
            changeHandler.onContentChanged(html, words, chars);
        }
    }

    public void loadHtml(String html) {
        suppressEvents = true;
        try {
            String processed = adjustMediaToPageSize(html);
            textPane.setText(processed != null ? processed : "<h1>Welcome</h1><p><br></p>");
            textPane.setCaretPosition(0);
            Document doc = textPane.getDocument();
            doc.removeDocumentListener(docListener);
            doc.addDocumentListener(docListener);
            doc.removeUndoableEditListener(undoManager);
            doc.addUndoableEditListener(undoManager);
            undoManager.discardAllEdits();
        } finally {
            suppressEvents = false;
        }
        updateDynamicPageHeight();
    }

    public void setDocumentHtml(String html) {
        loadHtml(html);
        triggerSave();
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public String getDocumentHtml() {
        return textPane.getText();
    }

    public String getPlainText() {
        try {
            return textPane.getDocument().getText(0, textPane.getDocument().getLength()).trim();
        } catch (BadLocationException e) {
            return "";
        }
    }

    public int getWordCount() {
        return countWords(getPlainText());
    }

    public int getCharCount() {
        return getPlainText().length();
    }

    public static int countWords(String s) {
        if (s == null) return 0;
        int count = 0;
        boolean inWord = false;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                inWord = false;
            } else if (!inWord) {
                inWord = true;
                count++;
            }
        }
        return count;
    }

    public void insertText(String text) {
        try {
            textPane.getDocument().insertString(textPane.getCaretPosition(), text, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public Element getTopBlockElement(Element elem) {
        if (elem == null) return null;
        Element current = elem;
        Element candidate = null;
        while (current != null) {
            Element parent = current.getParentElement();
            if (parent != null && "body".equalsIgnoreCase(parent.getName())) {
                return current;
            }
            if ("table".equalsIgnoreCase(current.getName())) {
                candidate = current;
            }
            current = parent;
        }
        return candidate;
    }

    public void insertHtml(String html) {
        try {
            html = adjustMediaToPageSize(html);
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int pos = textPane.getCaretPosition();
            Element charElem = (pos >= 0 && pos <= doc.getLength()) ? doc.getCharacterElement(pos) : null;
            Element target = getTopBlockElement(charElem);
            if (target == null && pos >= 0 && pos <= doc.getLength()) {
                target = doc.getParagraphElement(pos);
            }

            // Guard: Ensure target is never <head> or inside <head>
            boolean isInsideHead = false;
            Element p = target;
            while (p != null) {
                if ("head".equalsIgnoreCase(p.getName())) {
                    isInsideHead = true;
                    break;
                }
                p = p.getParentElement();
            }

            if (target != null && !isInsideHead && !"body".equalsIgnoreCase(target.getName()) && !"html".equalsIgnoreCase(target.getName())) {
                int lenBefore = doc.getLength();
                doc.insertAfterEnd(target, html);
                int lenAfter = doc.getLength();
                int insertedLen = lenAfter - lenBefore;
                int newCaret = Math.min(doc.getLength(), target.getEndOffset() + insertedLen - 1);
                if (html.contains("wb-video-card") && insertedLen > 2) {
                    newCaret = Math.min(doc.getLength(), target.getEndOffset() + 2);
                }
                if (newCaret >= 0) {
                    textPane.setCaretPosition(newCaret);
                }
            } else {
                Element body = null;
                for (Element r : doc.getRootElements()) {
                    for (int j = 0; j < r.getElementCount(); j++) {
                        if ("body".equalsIgnoreCase(r.getElement(j).getName())) {
                            body = r.getElement(j);
                            break;
                        }
                    }
                    if (body != null) break;
                }
                if (body != null) {
                    if (pos <= 1 && body.getElementCount() > 0) {
                        doc.insertAfterStart(body, html);
                    } else {
                        doc.insertBeforeEnd(body, html);
                    }
                } else {
                    editorKit.insertHTML(doc, Math.max(0, pos), html, 0, 0, HTML.Tag.P);
                }
                textPane.setCaretPosition(doc.getLength());
            }
            String insertName = "Insert Content";
            if (html.contains("<table") || html.contains("<TABLE")) insertName = "Insert Table";
            else if (html.contains("charts/chart_")) insertName = "Insert Chart";
            else if (html.contains("wb-video-card")) insertName = "Insert Video";
            else if (html.contains("<img") || html.contains("<IMG")) insertName = "Insert Image";
            else if (html.contains("wb-shape") || html.contains("shape")) insertName = "Insert Shape";
            else if (html.contains("wb-research-card")) insertName = "Insert Web Research";
            else if (html.contains("border-left:4px")) insertName = "Insert Callout";
            undoManager.registerNamedAction(insertName);

            triggerSave();
            updateDynamicPageHeight();

            // Auto-activate corresponding temporary contextual ribbon tab (like Microsoft Word)
            if (html.contains("<table") || html.contains("<TABLE")) {
                if (contextualTabHandler != null) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_TABLE_TOOLS);
                }
            } else if (html.contains("wb-video-card")) {
                if (contextualTabHandler != null) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_VIDEO_TOOLS);
                }
            } else if (html.contains("charts/chart_")) {
                if (contextualTabHandler != null) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_CHART_DESIGN);
                }
            } else if (html.contains("<img") || html.contains("<IMG")) {
                if (contextualTabHandler != null) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_PICTURE_FORMAT);
                }
            }
        } catch (Exception e) {
            try {
                HTMLDocument doc = (HTMLDocument) textPane.getDocument();
                doc.insertString(doc.getLength(), "\n" + html, null);
                triggerSave();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void runAutoDetectStructure() {
        String plain = getPlainText();
        if (plain == null || plain.isBlank()) {
            ModernDialog.showMessage(this, "Auto-Detect Structure", "Document is empty. Enter some text first.", ModernDialog.MessageType.INFO);
            return;
        }

        boolean choice = ModernDialog.showConfirm(this,
                "Smart Document Structure Detection",
                "Intelligent Structure Engine will analyze text and detect Topics (H1), Subtopics (H2/H3),\n" +
                        "Task Checklists, Key-Value Models, and Markdown Tables.\n\nConvert raw document structure now?",
                ModernDialog.MessageType.CONFIRM);

        if (choice) {
            DocumentStructureDetector detector = new DocumentStructureDetector();
            String structuredHtml = detector.autoStructureToHtml(plain);
            loadHtml(structuredHtml);
            triggerSave();
            ModernDialog.showMessage(this, "Intelligence Applied", "Document structure successfully detected and formatted!", ModernDialog.MessageType.SUCCESS);
        }
    }

    public void runGrammarCheck() {
        String plain = getPlainText();
        if (plain == null || plain.isBlank()) {
            ModernDialog.showMessage(this, "Proofreading Mode", "Document is empty. Write some text first to run proofreading.", ModernDialog.MessageType.INFO);
            return;
        }

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        GrammarDialog dlg = new GrammarDialog(owner, grammarEngine, plain, null);
        dlg.setVisible(true);

        if (dlg.isModified()) {
            String corrected = dlg.getCorrectedText();
            DocumentStructureDetector detector = new DocumentStructureDetector();
            loadHtml(detector.autoStructureToHtml(corrected));
            triggerSave();
        }
    }

    public void runGrammarCheck(GrammarEngineService engine) {
        runGrammarCheck();
    }

    public void autoDetectStructure(DocumentStructureDetector detector) {
        runAutoDetectStructure();
    }

    public void autoFixAllGrammar() {
        String plain = getPlainText();
        if (plain == null || plain.isBlank()) return;
        String fixed = grammarEngine.autoFixAll(plain);
        DocumentStructureDetector detector = new DocumentStructureDetector();
        loadHtml(detector.autoStructureToHtml(fixed));
        triggerSave();
        ModernDialog.showMessage(this, "Proofreading Clean", "All spelling, spacing, and grammar issues auto-corrected!", ModernDialog.MessageType.SUCCESS);
    }

    public void applyFormat(String command, Object arg) {
        StyledDocument doc = textPane.getStyledDocument();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        int len = end - start;

        MutableAttributeSet attr = new SimpleAttributeSet();

        switch (command) {
            case "paste" -> {
                textPane.paste();
                triggerSave();
                return;
            }
            case "copy" -> {
                textPane.copy();
                return;
            }
            case "cut" -> {
                textPane.cut();
                triggerSave();
                return;
            }
            case "undo" -> {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                    triggerSave();
                }
                return;
            }
            case "redo" -> {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                    triggerSave();
                }
                return;
            }
            case "indent" -> {
                SimpleAttributeSet p = new SimpleAttributeSet();
                int pos = textPane.getSelectionStart();
                Element elem = ((HTMLDocument) textPane.getDocument()).getParagraphElement(pos);
                float curIndent = StyleConstants.getLeftIndent(elem.getAttributes());
                StyleConstants.setLeftIndent(p, curIndent + 28f);
                textPane.setParagraphAttributes(p, false);
                triggerSave();
                return;
            }
            case "outdent" -> {
                SimpleAttributeSet p = new SimpleAttributeSet();
                int pos = textPane.getSelectionStart();
                Element elem = ((HTMLDocument) textPane.getDocument()).getParagraphElement(pos);
                float curIndent = StyleConstants.getLeftIndent(elem.getAttributes());
                StyleConstants.setLeftIndent(p, Math.max(0f, curIndent - 28f));
                textPane.setParagraphAttributes(p, false);
                triggerSave();
                return;
            }
            case "bold" -> StyleConstants.setBold(attr, !StyleConstants.isBold(textPane.getCharacterAttributes()));
            case "italic" -> StyleConstants.setItalic(attr, !StyleConstants.isItalic(textPane.getCharacterAttributes()));
            case "underline" -> StyleConstants.setUnderline(attr, !StyleConstants.isUnderline(textPane.getCharacterAttributes()));
            case "strikethrough" -> StyleConstants.setStrikeThrough(attr, !StyleConstants.isStrikeThrough(textPane.getCharacterAttributes()));
            case "font-family" -> StyleConstants.setFontFamily(attr, String.valueOf(arg));
            case "font-size" -> {
                try {
                    StyleConstants.setFontSize(attr, Integer.parseInt(String.valueOf(arg)));
                } catch (Exception ignored) {}
            }
            case "forecolor" -> {
                try {
                    StyleConstants.setForeground(attr, Color.decode(String.valueOf(arg)));
                } catch (Exception ignored) {}
            }
            case "hilitecolor" -> {
                try {
                    StyleConstants.setBackground(attr, Color.decode(String.valueOf(arg)));
                } catch (Exception ignored) {}
            }
            case "subscript" -> StyleConstants.setSubscript(attr, !StyleConstants.isSubscript(textPane.getCharacterAttributes()));
            case "superscript" -> StyleConstants.setSuperscript(attr, !StyleConstants.isSuperscript(textPane.getCharacterAttributes()));
            case "font-size-up" -> {
                int cur = 14;
                if (len > 0) {
                    Element elem = ((HTMLDocument) textPane.getDocument()).getCharacterElement(start);
                    cur = StyleConstants.getFontSize(elem.getAttributes());
                } else {
                    cur = StyleConstants.getFontSize(textPane.getCharacterAttributes());
                }
                if (cur <= 0) cur = 14;
                StyleConstants.setFontSize(attr, Math.min(cur + 2, 72));
            }
            case "font-size-down" -> {
                int cur = 14;
                if (len > 0) {
                    Element elem = ((HTMLDocument) textPane.getDocument()).getCharacterElement(start);
                    cur = StyleConstants.getFontSize(elem.getAttributes());
                } else {
                    cur = StyleConstants.getFontSize(textPane.getCharacterAttributes());
                }
                if (cur <= 0) cur = 14;
                StyleConstants.setFontSize(attr, Math.max(cur - 2, 8));
            }
            case "align-left" -> {
                SimpleAttributeSet p = new SimpleAttributeSet();
                StyleConstants.setAlignment(p, StyleConstants.ALIGN_LEFT);
                textPane.setParagraphAttributes(p, false);
                triggerSave();
                return;
            }
            case "align-center" -> {
                SimpleAttributeSet p = new SimpleAttributeSet();
                StyleConstants.setAlignment(p, StyleConstants.ALIGN_CENTER);
                textPane.setParagraphAttributes(p, false);
                triggerSave();
                return;
            }
            case "align-right" -> {
                SimpleAttributeSet p = new SimpleAttributeSet();
                StyleConstants.setAlignment(p, StyleConstants.ALIGN_RIGHT);
                textPane.setParagraphAttributes(p, false);
                triggerSave();
                return;
            }
            case "align-justify" -> {
                SimpleAttributeSet p = new SimpleAttributeSet();
                StyleConstants.setAlignment(p, StyleConstants.ALIGN_JUSTIFIED);
                textPane.setParagraphAttributes(p, false);
                triggerSave();
                return;
            }
            case "line-spacing" -> {
                try {
                    float val = Float.parseFloat(String.valueOf(arg));
                    SimpleAttributeSet p = new SimpleAttributeSet();
                    StyleConstants.setLineSpacing(p, val);
                    textPane.setParagraphAttributes(p, false);
                    triggerSave();
                } catch (Exception ignored) {}
                return;
            }
            case "clear" -> {
                SimpleAttributeSet cAttr = new SimpleAttributeSet();
                StyleConstants.setFontFamily(cAttr, "Calibri");
                StyleConstants.setFontSize(cAttr, 14);
                StyleConstants.setBold(cAttr, false);
                StyleConstants.setItalic(cAttr, false);
                StyleConstants.setUnderline(cAttr, false);
                StyleConstants.setStrikeThrough(cAttr, false);
                StyleConstants.setForeground(cAttr, new Color(30, 41, 59));
                StyleConstants.setBackground(cAttr, Color.WHITE);
                if (len > 0) doc.setCharacterAttributes(start, len, cAttr, true);
                else textPane.setCharacterAttributes(cAttr, true);
                triggerSave();
                return;
            }
            case "bullet-list" -> {
                if (len > 0) {
                    String sel = textPane.getSelectedText();
                    if (sel != null && !sel.isBlank()) {
                        String[] lines = sel.split("\n");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < lines.length; i++) {
                            if (!lines[i].trim().startsWith("• ")) sb.append("• ");
                            sb.append(lines[i]);
                            if (i < lines.length - 1) sb.append("\n");
                        }
                        textPane.replaceSelection(sb.toString());
                        triggerSave();
                        return;
                    }
                }
                insertText("\n• ");
                return;
            }
            case "numbered-list" -> {
                if (len > 0) {
                    String sel = textPane.getSelectedText();
                    if (sel != null && !sel.isBlank()) {
                        String[] lines = sel.split("\n");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < lines.length; i++) {
                            sb.append((i + 1)).append(". ").append(lines[i]);
                            if (i < lines.length - 1) sb.append("\n");
                        }
                        textPane.replaceSelection(sb.toString());
                        triggerSave();
                        return;
                    }
                }
                insertText("\n1. ");
                return;
            }
            case "checkbox" -> {
                if (len > 0) {
                    String sel = textPane.getSelectedText();
                    if (sel != null && !sel.isBlank()) {
                        String[] lines = sel.split("\n");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < lines.length; i++) {
                            if (!lines[i].trim().startsWith("[ ] ")) sb.append("[ ] ");
                            sb.append(lines[i]);
                            if (i < lines.length - 1) sb.append("\n");
                        }
                        textPane.replaceSelection(sb.toString());
                        triggerSave();
                        return;
                    }
                }
                insertText("\n[ ] ");
                return;
            }
            case "heading" -> {
                String val = String.valueOf(arg);
                String normalized = val.trim().replace(" ", "_").toUpperCase();
                if ("HEADING_1".equals(normalized) || "HEADING 1".equalsIgnoreCase(val)) {
                    StyleConstants.setFontSize(attr, 24);
                    StyleConstants.setBold(attr, true);
                    StyleConstants.setItalic(attr, false);
                    StyleConstants.setForeground(attr, new Color(15, 23, 42));
                } else if ("HEADING_2".equals(normalized) || "HEADING 2".equalsIgnoreCase(val)) {
                    StyleConstants.setFontSize(attr, 19);
                    StyleConstants.setBold(attr, true);
                    StyleConstants.setItalic(attr, false);
                    StyleConstants.setForeground(attr, new Color(30, 41, 59));
                } else if ("HEADING_3".equals(normalized) || "HEADING 3".equalsIgnoreCase(val)) {
                    StyleConstants.setFontSize(attr, 16);
                    StyleConstants.setBold(attr, true);
                    StyleConstants.setItalic(attr, false);
                    StyleConstants.setForeground(attr, new Color(51, 65, 85));
                } else if ("TITLE".equals(normalized)) {
                    StyleConstants.setFontSize(attr, 30);
                    StyleConstants.setBold(attr, true);
                    StyleConstants.setItalic(attr, false);
                    StyleConstants.setForeground(attr, new Color(15, 23, 42));
                } else if ("SUBTITLE".equals(normalized)) {
                    StyleConstants.setFontSize(attr, 16);
                    StyleConstants.setBold(attr, false);
                    StyleConstants.setItalic(attr, true);
                    StyleConstants.setForeground(attr, new Color(100, 116, 139));
                } else if ("QUOTE".equals(normalized)) {
                    StyleConstants.setFontSize(attr, 15);
                    StyleConstants.setBold(attr, false);
                    StyleConstants.setItalic(attr, true);
                    StyleConstants.setForeground(attr, new Color(37, 99, 235));
                } else { // NORMAL / PARAGRAPH
                    StyleConstants.setFontSize(attr, 14);
                    StyleConstants.setBold(attr, false);
                    StyleConstants.setItalic(attr, false);
                    StyleConstants.setForeground(attr, new Color(30, 41, 59));
                }
            }
        }

        if (len > 0) {
            doc.setCharacterAttributes(start, len, attr, false);
        } else {
            // Apply to enclosing paragraph if no selection
            int caretPos = textPane.getCaretPosition();
            Element p = doc.getParagraphElement(caretPos);
            if (p != null) {
                int pStart = p.getStartOffset();
                int pLen = Math.max(0, p.getEndOffset() - pStart - 1);
                if (pLen > 0) {
                    doc.setCharacterAttributes(pStart, pLen, attr, false);
                }
            }
            textPane.setCharacterAttributes(attr, false);
        }

        String humanAction = switch (command) {
            case "bold" -> "Format Bold";
            case "italic" -> "Format Italic";
            case "underline" -> "Format Underline";
            case "strikethrough" -> "Format Strikethrough";
            case "font-family" -> "Font: " + arg;
            case "font-size" -> "Font Size: " + arg;
            case "forecolor" -> "Text Color";
            case "hilitecolor" -> "Highlight";
            case "subscript" -> "Subscript";
            case "superscript" -> "Superscript";
            case "font-size-up" -> "Increase Font Size";
            case "font-size-down" -> "Decrease Font Size";
            case "align-left" -> "Align Left";
            case "align-center" -> "Align Center";
            case "align-right" -> "Align Right";
            case "align-justify" -> "Justify Text";
            case "heading" -> "Heading: " + arg;
            case "bullet-list" -> "Bullet List";
            case "numbered-list" -> "Numbered List";
            case "checklist" -> "Checklist";
            case "clear" -> "Clear Formatting";
            default -> null;
        };
        if (humanAction != null) {
            undoManager.registerNamedAction(humanAction);
        }

        triggerSave();
    }

    public void setZoom(double factor) {
        this.zoomFactor = factor;
        // Keep page paper sheet strictly fixed to currentPageFormat.width.
        // Scale typography comfortably so tables and cards fit within paper printable boundaries.
        try {
            int bodyPt = Math.max(9, Math.min(18, (int) Math.round(13 * factor)));
            int h1Px = Math.max(16, Math.min(30, (int) Math.round(24 * factor)));
            int h2Px = Math.max(13, Math.min(24, (int) Math.round(18 * factor)));
            int h3Px = Math.max(11, Math.min(18, (int) Math.round(15 * factor)));
            StyleSheet css = editorKit.getStyleSheet();
            css.addRule("body { font-size: " + bodyPt + "pt; }");
            css.addRule("th, td { font-size: " + bodyPt + "pt; }");
            css.addRule("h1 { font-size: " + h1Px + "px; }");
            css.addRule("h2 { font-size: " + h2Px + "px; }");
            css.addRule("h3 { font-size: " + h3Px + "px; }");
        } catch (Exception ignored) {}
        updateDynamicPageHeight();
        revalidate();
        repaint();
    }

    private void updateDynamicPageHeight() {
        if (suppressEvents) return;
        int textH = 0;
        try {
            if (textPane != null) {
                textH = textPane.getPreferredSize().height;
            }
        } catch (Exception ignored) {}

        int singlePageH = currentPageFormat.height;
        int pageGap = 28;
        int pageUnit = singlePageH + pageGap;
        int autoCount = (textH <= 0) ? 1 : Math.max(1, (textH + pageGap) / pageUnit + 1);

        int breakCount = countPageBreaks();
        cachedPageCount = Math.max(breakCount, autoCount);

        int baseW = currentPageFormat.width;
        int totalH = cachedPageCount * singlePageH + Math.max(0, cachedPageCount - 1) * pageGap;

        Dimension curPref = pageSheet.getPreferredSize();
        if (curPref == null || curPref.width != baseW || curPref.height != totalH) {
            pageSheet.setPreferredSize(new Dimension(baseW, totalH));
            pageSheet.revalidate();
        }
        pageSheet.repaint();
        if (textPane != null) textPane.repaint();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        setBackground(ThemeManager.getCanvasBackdrop());
        repaint();
    }

    // =========================================================================
    // CONTEXTUAL TOOL STATE & NAVIGATION
    // =========================================================================

    public void updateContextualTabState() {
        if (contextualTabHandler == null) return;

        // 1. Maintain active image or chart contextual tab while media is selected
        if (selectedMediaElement != null && HTML.Tag.IMG.equals(selectedMediaElement.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
            Object src = selectedMediaElement.getAttributes().getAttribute(HTML.Attribute.SRC);
            if (src != null && (src.toString().contains("charts/chart_") || src.toString().contains(".wordbook/charts") || src.toString().contains("chart_"))) {
                contextualTabHandler.showContextualTab(RibbonPanel.TAB_CHART_DESIGN);
            } else {
                contextualTabHandler.showContextualTab(RibbonPanel.TAB_PICTURE_FORMAT);
            }
            return;
        }

        int pos = textPane.getCaretPosition();
        if (pos >= 0 && pos <= textPane.getDocument().getLength()) {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element elem = doc.getCharacterElement(pos);
            if (getEnclosingTable(elem) != null) {
                contextualTabHandler.showContextualTab(RibbonPanel.TAB_TABLE_TOOLS);
                return;
            }
            if (elem != null && HTML.Tag.IMG.equals(elem.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
                Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
                if (src != null && (src.toString().contains("charts/chart_") || src.toString().contains(".wordbook/charts") || src.toString().contains("chart_"))) {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_CHART_DESIGN);
                } else {
                    contextualTabHandler.showContextualTab(RibbonPanel.TAB_PICTURE_FORMAT);
                }
                return;
            }
            Element videoCard = findVideoCardAt(elem);
            if (videoCard != null) {
                contextualTabHandler.showContextualTab(RibbonPanel.TAB_VIDEO_TOOLS);
                return;
            }
        }
        contextualTabHandler.hideContextualTabs();
    }

    // =========================================================================
    // ADVANCED TABLE TOOLS ENGINE (Word-style Contextual Operations)
    // =========================================================================

    public Element getActiveTable() {
        int pos = textPane.getCaretPosition();
        if (pos < 0 || pos > textPane.getDocument().getLength()) return null;
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        return getEnclosingTable(doc.getCharacterElement(pos));
    }

    public Element getActiveTableCell() {
        int pos = textPane.getCaretPosition();
        if (pos < 0 || pos > textPane.getDocument().getLength()) return null;
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        Element elem = doc.getCharacterElement(pos);
        Element cell = getEnclosingCell(elem);
        if (cell == null && pos > 0) {
            cell = getEnclosingCell(doc.getCharacterElement(pos - 1));
        }
        return cell;
    }

    public Element getActiveTableRow() {
        Element cell = getActiveTableCell();
        return cell != null ? getEnclosingRow(cell) : null;
    }

    public Element getEnclosingCell(Element elem) {
        while (elem != null) {
            Object name = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (HTML.Tag.TD.equals(name) || HTML.Tag.TH.equals(name)) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    public Element getEnclosingRow(Element elem) {
        while (elem != null) {
            Object name = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (HTML.Tag.TR.equals(name)) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    public Element getEnclosingTable(Element elem) {
        while (elem != null) {
            Object name = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (HTML.Tag.TABLE.equals(name)) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    public Element findCellAtPoint(Point pt) {
        int pos = textPane.viewToModel2D(pt);
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        if (pos >= 0 && pos <= doc.getLength()) {
            Element elem = doc.getCharacterElement(pos);
            Element cell = getEnclosingCell(elem);
            if (cell != null) return cell;
            if (pos > 0) {
                cell = getEnclosingCell(doc.getCharacterElement(pos - 1));
                if (cell != null) return cell;
            }
        }

        // Geometric hit test across all tables
        return findCellGeometrically(doc.getDefaultRootElement(), pt);
    }

    private Element findCellGeometrically(Element elem, Point pt) {
        if (elem.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.TABLE) {
            int rowCount = elem.getElementCount();
            for (int r = 0; r < rowCount; r++) {
                Element row = elem.getElement(r);
                int colCount = row.getElementCount();
                int rowTop = Integer.MAX_VALUE;
                int rowBottom = Integer.MIN_VALUE;

                for (int c = 0; c < colCount; c++) {
                    Element cell = row.getElement(c);
                    try {
                        Rectangle r1 = textPane.modelToView(cell.getStartOffset());
                        Rectangle r2 = textPane.modelToView(cell.getEndOffset() - 1);
                        if (r1 != null) {
                            rowTop = Math.min(rowTop, r1.y - 10);
                            rowBottom = Math.max(rowBottom, r1.y + r1.height + 15);
                        }
                        if (r2 != null) {
                            rowTop = Math.min(rowTop, r2.y - 10);
                            rowBottom = Math.max(rowBottom, r2.y + r2.height + 15);
                        }
                    } catch (Exception ignored) {}
                }

                if (rowTop != Integer.MAX_VALUE && pt.y >= rowTop && pt.y <= rowBottom) {
                    int w = Math.max(1, textPane.getWidth());
                    int colIdx = Math.min(colCount - 1, Math.max(0, (pt.x * colCount) / w));
                    return row.getElement(colIdx);
                }
            }
        }
        for (int i = 0; i < elem.getElementCount(); i++) {
            Element found = findCellGeometrically(elem.getElement(i), pt);
            if (found != null) return found;
        }
        return null;
    }

    public List<Element> getAllCells(Element table) {
        List<Element> list = new ArrayList<>();
        if (table == null) return list;
        collectCells(table, list);
        return list;
    }

    private void collectCells(Element elem, List<Element> list) {
        Object name = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        if (HTML.Tag.TD.equals(name) || HTML.Tag.TH.equals(name)) {
            list.add(elem);
            return;
        }
        for (int i = 0; i < elem.getElementCount(); i++) {
            collectCells(elem.getElement(i), list);
        }
    }

    public void navigateTableCell(boolean forward) {
        Element table = getActiveTable();
        Element curCell = getActiveTableCell();
        if (table == null || curCell == null) return;

        List<Element> cells = getAllCells(table);
        int idx = cells.indexOf(curCell);
        if (idx < 0) return;

        if (forward) {
            if (idx + 1 < cells.size()) {
                textPane.setCaretPosition(cells.get(idx + 1).getStartOffset());
            } else {
                // Last cell in table! Append new row below and move caret to first cell
                insertTableRowBelow();
            }
        } else {
            if (idx - 1 >= 0) {
                textPane.setCaretPosition(cells.get(idx - 1).getStartOffset());
            }
        }
    }

    public void insertTableRowAbove() {
        Element row = getActiveTableRow();
        Element table = getActiveTable();
        if (row == null || table == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int colCount = row.getElementCount();
            StringBuilder sb = new StringBuilder("<tr>");
            for (int c = 0; c < colCount; c++) {
                sb.append("<td style='padding:8px 12px;border:1px solid #cbd5e1;color:#334155;'>&nbsp;</td>");
            }
            sb.append("</tr>");
            doc.insertBeforeStart(row, sb.toString());
            triggerSave();
            updateDynamicPageHeight();
            if (contextualTabHandler != null) contextualTabHandler.showContextualTab(RibbonPanel.TAB_TABLE_TOOLS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void insertTableRowBelow() {
        Element row = getActiveTableRow();
        Element table = getActiveTable();
        if (row == null || table == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int colCount = row.getElementCount();
            StringBuilder sb = new StringBuilder("<tr>");
            for (int c = 0; c < colCount; c++) {
                sb.append("<td style='padding:8px 12px;border:1px solid #cbd5e1;color:#334155;'>&nbsp;</td>");
            }
            sb.append("</tr>");
            doc.insertAfterEnd(row, sb.toString());
            triggerSave();
            updateDynamicPageHeight();
            List<Element> cells = getAllCells(table);
            int curRowIdx = -1;
            for (int r = 0; r < table.getElementCount(); r++) {
                if (table.getElement(r) == row) {
                    curRowIdx = r;
                    break;
                }
            }
            if (curRowIdx >= 0 && (curRowIdx + 1) * colCount < cells.size()) {
                textPane.setCaretPosition(cells.get((curRowIdx + 1) * colCount).getStartOffset());
            }
            if (contextualTabHandler != null) contextualTabHandler.showContextualTab(RibbonPanel.TAB_TABLE_TOOLS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void insertTableColumnLeft() {
        insertTableColumn(true);
    }

    public void insertTableColumnRight() {
        insertTableColumn(false);
    }

    private void insertTableColumn(boolean left) {
        Element table = getActiveTable();
        Element cell = getActiveTableCell();
        Element row = getActiveTableRow();
        if (table == null || cell == null || row == null) return;

        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int refColIdx = 0;
            for (int c = 0; c < row.getElementCount(); c++) {
                if (row.getElement(c) == cell) {
                    refColIdx = c;
                    break;
                }
            }

            int insertIdx = left ? refColIdx : refColIdx + 1;
            StringBuilder sb = new StringBuilder();
            sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px 0;border:1px solid #cbd5e1;'>");

            int rowCount = table.getElementCount();
            for (int r = 0; r < rowCount; r++) {
                Element curRow = table.getElement(r);
                sb.append("<tr>");
                int colCount = curRow.getElementCount();
                for (int c = 0; c < colCount; c++) {
                    if (c == insertIdx) {
                        appendNewColCell(sb, r == 0, c + 1);
                    }
                    Element cElem = curRow.getElement(c);
                    String tag = (r == 0) ? "th" : "td";
                    String text = getElementPlainText(doc, cElem).trim();
                    if (text.isEmpty()) text = "&nbsp;";
                    sb.append("<").append(tag).append(" style='padding:8px 12px;border:1px solid #cbd5e1;'>").append(text).append("</").append(tag).append(">");
                }
                if (insertIdx >= colCount) {
                    appendNewColCell(sb, r == 0, colCount + 1);
                }
                sb.append("</tr>");
            }
            int startPos = table.getStartOffset();
            suppressEvents = true;
            try {
                doc.setOuterHTML(table, sb.toString());
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
            Element newTable = getEnclosingTable(doc.getCharacterElement(startPos));
            if (newTable != null) {
                List<Element> cells = getAllCells(newTable);
                if (!cells.isEmpty()) {
                    textPane.setCaretPosition(cells.get(0).getStartOffset());
                }
            }
            if (contextualTabHandler != null) contextualTabHandler.showContextualTab(RibbonPanel.TAB_TABLE_TOOLS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void appendNewColCell(StringBuilder sb, boolean isHeader, int colNum) {
        if (isHeader) {
            sb.append("<th style='background-color:#f1f5f9;padding:8px 12px;font-weight:bold;color:#0f172a;border:1px solid #cbd5e1;'>Header ").append(colNum).append("</th>");
        } else {
            sb.append("<td style='padding:8px 12px;border:1px solid #cbd5e1;color:#334155;'>&nbsp;</td>");
        }
    }

    public void deleteTableRow() {
        Element row = getActiveTableRow();
        Element table = getActiveTable();
        if (row == null || table == null) return;

        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            if (table.getElementCount() <= 1) {
                deleteTable();
                return;
            }
            suppressEvents = true;
            try {
                doc.remove(row.getStartOffset(), row.getEndOffset() - row.getStartOffset());
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void deleteTableColumn() {
        Element table = getActiveTable();
        Element cell = getActiveTableCell();
        Element row = getActiveTableRow();
        if (table == null || cell == null || row == null) return;

        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int colIdx = 0;
            for (int c = 0; c < row.getElementCount(); c++) {
                if (row.getElement(c) == cell) {
                    colIdx = c;
                    break;
                }
            }

            if (row.getElementCount() <= 1) {
                deleteTable();
                return;
            }

            StringBuilder sb = new StringBuilder("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px 0;border:1px solid #cbd5e1;'>");
            int rowCount = table.getElementCount();
            for (int r = 0; r < rowCount; r++) {
                Element curRow = table.getElement(r);
                sb.append("<tr>");
                int colCount = curRow.getElementCount();
                for (int c = 0; c < colCount; c++) {
                    if (c == colIdx) continue;
                    Element cElem = curRow.getElement(c);
                    String tag = (r == 0) ? "th" : "td";
                    String text = getElementPlainText(doc, cElem).trim();
                    if (text.isEmpty()) text = "&nbsp;";
                    sb.append("<").append(tag).append(" style='padding:8px 12px;border:1px solid #cbd5e1;'>").append(text).append("</").append(tag).append(">");
                }
                sb.append("</tr>");
            }
            int startPos = table.getStartOffset();
            suppressEvents = true;
            try {
                doc.setOuterHTML(table, sb.toString());
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
            Element newTable = getEnclosingTable(doc.getCharacterElement(startPos));
            if (newTable != null) {
                List<Element> cells = getAllCells(newTable);
                if (!cells.isEmpty()) {
                    textPane.setCaretPosition(cells.get(0).getStartOffset());
                }
            }
            if (contextualTabHandler != null) contextualTabHandler.showContextualTab(RibbonPanel.TAB_TABLE_TOOLS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void deleteTable() {
        Element table = getActiveTable();
        if (table == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            suppressEvents = true;
            try {
                doc.remove(table.getStartOffset(), table.getEndOffset() - table.getStartOffset());
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
            if (contextualTabHandler != null) contextualTabHandler.hideContextualTabs();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void applyTableStyle(String styleTheme) {
        Element table = getActiveTable();
        if (table == null) return;

        String headerBg = "#2563eb";
        String headerColor = "#ffffff";
        String borderColor = "#93c5fd";
        String altRowBg = "#eff6ff";

        if ("Modern Slate".equalsIgnoreCase(styleTheme)) {
            headerBg = "#475569"; headerColor = "#ffffff"; borderColor = "#cbd5e1"; altRowBg = "#f8fafc";
        } else if ("Emerald Green".equalsIgnoreCase(styleTheme)) {
            headerBg = "#059669"; headerColor = "#ffffff"; borderColor = "#a7f3d0"; altRowBg = "#ecfdf5";
        } else if ("Royal Violet".equalsIgnoreCase(styleTheme)) {
            headerBg = "#7c3aed"; headerColor = "#ffffff"; borderColor = "#ddd6fe"; altRowBg = "#faf5ff";
        } else if ("Dark Executive".equalsIgnoreCase(styleTheme)) {
            headerBg = "#0f172a"; headerColor = "#ffffff"; borderColor = "#334155"; altRowBg = "#f1f5f9";
        } else if ("Amber Warm".equalsIgnoreCase(styleTheme)) {
            headerBg = "#d97706"; headerColor = "#ffffff"; borderColor = "#fde68a"; altRowBg = "#fffbeb";
        } else if ("Striped Zebra".equalsIgnoreCase(styleTheme)) {
            headerBg = "#f1f5f9"; headerColor = "#0f172a"; borderColor = "#e2e8f0"; altRowBg = "#f8fafc";
        } else if ("Minimal Grid".equalsIgnoreCase(styleTheme)) {
            headerBg = "#ffffff"; headerColor = "#0f172a"; borderColor = "#cbd5e1"; altRowBg = "#ffffff";
        } else if ("Coral Vibrant".equalsIgnoreCase(styleTheme)) {
            headerBg = "#f43f5e"; headerColor = "#ffffff"; borderColor = "#fecdd3"; altRowBg = "#fff1f2";
        } else if ("Indigo Modern".equalsIgnoreCase(styleTheme)) {
            headerBg = "#4f46e5"; headerColor = "#ffffff"; borderColor = "#c7d2fe"; altRowBg = "#eef2ff";
        } else if ("Teal Professional".equalsIgnoreCase(styleTheme)) {
            headerBg = "#0d9488"; headerColor = "#ffffff"; borderColor = "#99f6e4"; altRowBg = "#f0fdfa";
        }

        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px 0;border:1px solid ").append(borderColor).append(";'>");

            int rowCount = table.getElementCount();
            for (int r = 0; r < rowCount; r++) {
                Element row = table.getElement(r);
                String rowBg = (r == 0) ? headerBg : (r % 2 == 1 ? "#ffffff" : altRowBg);
                sb.append("<tr style='background-color:").append(rowBg).append(";'>");

                int colCount = row.getElementCount();
                for (int c = 0; c < colCount; c++) {
                    Element cell = row.getElement(c);
                    String text = getElementPlainText(doc, cell).trim();
                    if (text.isEmpty()) text = "&nbsp;";

                    if (r == 0) {
                        sb.append("<th style='padding:8px 12px;font-weight:bold;color:").append(headerColor).append(";border:1px solid ").append(borderColor).append(";'>").append(text).append("</th>");
                    } else {
                        sb.append("<td style='padding:8px 12px;color:#1e293b;border:1px solid ").append(borderColor).append(";'>").append(text).append("</td>");
                    }
                }
                sb.append("</tr>");
            }
            sb.append("</table>");
            int startPos = table.getStartOffset();
            suppressEvents = true;
            try {
                doc.setOuterHTML(table, sb.toString());
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
            Element newTable = getEnclosingTable(doc.getCharacterElement(startPos));
            if (newTable != null) {
                List<Element> cells = getAllCells(newTable);
                if (!cells.isEmpty()) {
                    textPane.setCaretPosition(cells.get(0).getStartOffset());
                }
            }
            if (contextualTabHandler != null) contextualTabHandler.showContextualTab(RibbonPanel.TAB_TABLE_TOOLS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setTableCellColor(Color color) {
        if (color == null) return;
        Element cell = getActiveTableCell();
        if (cell == null) return;

        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            String text = getElementPlainText(doc, cell).trim();
            if (text.isEmpty()) text = "&nbsp;";
            String tag = cell.getName().equalsIgnoreCase("th") ? "th" : "td";
            String newCellHtml = "<" + tag + " style='padding:8px 12px;border:1px solid #cbd5e1;background-color:" + hex + ";'>" + text + "</" + tag + ">";
            suppressEvents = true;
            try {
                doc.setOuterHTML(cell, newCellHtml);
            } finally {
                suppressEvents = false;
            }
            triggerSave();
            pageLayoutTimer.restart();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getElementPlainText(HTMLDocument doc, Element elem) {
        try {
            int start = elem.getStartOffset();
            int end = elem.getEndOffset();
            return doc.getText(start, end - start).replace("\n", "").trim();
        } catch (Exception e) {
            return "";
        }
    }

    // =========================================================================
    // SHAPES & VIDEO INTEGRATION
    // =========================================================================

    public void insertShape(String shapeType) {
        if (shapeType == null) return;
        String html = switch (shapeType) {
            case "Rounded Card" ->
                "<div class='wb-card' data-card='rounded' data-theme='corporate' style='border:2px solid #3b82f6;background-color:#eff6ff;padding:14px 18px;border-radius:10px;margin:14px 0;color:#1e3a8a;'>" +
                "<div class='wb-card-title' style='font-weight:bold;font-size:16px;margin-bottom:6px;color:#1e40af;'>■ Rounded Card Box</div>" +
                "<div class='wb-card-sub' style='color:#1e3a8a;'>Card content description or key point.</div>" +
                "</div><p><br></p>";

            case "Sticky Note Card" ->
                "<div class='wb-card' data-card='sticky' data-theme='amber' style='border:1px solid #fde047;background-color:#fef9c3;padding:14px 18px;border-radius:8px;margin:14px 0;color:#854d0e;'>" +
                "<div class='wb-card-title' style='font-weight:bold;font-size:15px;margin-bottom:6px;color:#b45309;'>★ Sticky Memo</div>" +
                "<div class='wb-card-sub' style='color:#854d0e;'>Important note or follow-up reminder.</div>" +
                "</div><p><br></p>";

            case "Metric / KPI Pill" ->
                "<div class='wb-card' data-card='kpi' data-theme='corporate' style='border:1px solid #e2e8f0;background-color:#f8fafc;padding:12px 20px;border-radius:8px;margin:14px 0;text-align:center;width:280px;'>" +
                "<div class='wb-card-title' style='font-size:26px;font-weight:bold;color:#2563eb;'>98.4%</div>" +
                "<div class='wb-card-sub' style='font-size:12px;color:#64748b;font-weight:bold;text-transform:uppercase;'>Target Completion Rate</div>" +
                "</div><p><br></p>";

            case "Process Flow (Step A ➔ B ➔ C)" ->
                "<div class='wb-flow' data-flow='process' data-theme='corporate' style='text-align:center;margin:16px 0;padding:10px;'>" +
                "<span style='background-color:#2563eb;color:#ffffff;padding:8px 16px;border-radius:6px;font-weight:bold;'>Step 1: Ideate</span> " +
                "<span style='font-size:18px;color:#2563eb;font-weight:bold;'>➔</span> " +
                "<span style='background-color:#10b981;color:#ffffff;padding:8px 16px;border-radius:6px;font-weight:bold;'>Step 2: Build</span> " +
                "<span style='font-size:18px;color:#10b981;font-weight:bold;'>➔</span> " +
                "<span style='background-color:#7c3aed;color:#ffffff;padding:8px 16px;border-radius:6px;font-weight:bold;'>Step 3: Ship</span>" +
                "</div><p><br></p>";

            case "Decision Diamond" ->
                "<div class='wb-shape' data-shape='decision' data-theme='amber' style='text-align:center;margin:16px 0;'>" +
                "<span style='border:2px solid #f59e0b;background-color:#fffbeb;color:#b45309;padding:10px 24px;border-radius:24px;font-weight:bold;font-size:14px;'>" +
                "◆ Decision: Approve / Validate?</span>" +
                "</div><p><br></p>";

            case "Start / Terminal Pill" ->
                "<div class='wb-shape' data-shape='terminal' data-theme='emerald' style='text-align:center;margin:16px 0;'>" +
                "<span style='border:2px solid #10b981;background-color:#ecfdf5;color:#047857;padding:10px 28px;border-radius:24px;font-weight:bold;font-size:14px;'>" +
                "● Process Start / Complete</span>" +
                "</div><p><br></p>";

            case "Checkmark Badge (✓)" ->
                "<div class='wb-shape' data-shape='badge' data-theme='emerald' style='border-left:4px solid #16a34a;background-color:#f0fdf4;padding:12px 18px;border-radius:4px;margin:14px 0;color:#14532d;'>" +
                "<span class='wb-card-title' style='font-weight:bold;color:#15803d;'>✓ Verified Milestone:</span> <span class='wb-card-sub' style='color:#14532d;'>Quality standards achieved with 100% compliance.</span>" +
                "</div><p><br></p>";

            case "Star Rating (★★★★★)" ->
                "<div class='wb-shape' data-shape='rating' data-theme='amber' style='border:1px solid #fde68a;background-color:#fffbeb;padding:10px 16px;border-radius:6px;margin:14px 0;color:#b45309;'>" +
                "<span style='font-size:18px;color:#f59e0b;'>★★★★★</span> &bull; <b class='wb-card-title' style='color:#b45309;'>Top Evaluation Rating</b>" +
                "</div><p><br></p>";

            case "Warning Triangle (⚠)" ->
                "<div class='wb-shape' data-shape='warning' data-theme='rose' style='border-left:4px solid #ea580c;background-color:#fff7ed;padding:12px 18px;border-radius:4px;margin:14px 0;color:#9a3412;'><span class='wb-card-title' style='font-weight:bold;color:#c2410c;'>⚠ Attention Required:</span> <span class='wb-card-sub' style='color:#9a3412;'>Verify operational requirements before proceeding.</span></div><p><br></p>";

            case "Gradient Divider" ->
                "<div class='wb-shape' data-shape='divider' style='border-top:2px solid #3b82f6;margin:24px 0;padding-top:4px;'></div><p><br></p>";

            case "Dashed Line" ->
                "<div class='wb-shape' data-shape='dashed' style='border-top:2px dashed #94a3b8;margin:24px 0;padding-top:4px;'></div><p><br></p>";

            default ->
                "<div class='wb-shape' data-shape='box' style='border:1px solid #cbd5e1;padding:12px;border-radius:6px;margin:12px 0;'>Shape Box</div><p><br></p>";
        };
        insertHtml(html);
    }

    public Element findCardOrShapeAt(Element elem) {
        while (elem != null) {
            if (isCardOrShapeElement(elem)) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    public Element findCardOrShapeAtPoint(Point pt) {
        int pos = textPane.viewToModel2D(pt);
        if (pos >= 0) {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element chElem = doc.getCharacterElement(pos);
            return findCardOrShapeAt(chElem);
        }
        return null;
    }

    public boolean isCardOrShapeElement(Element elem) {
        if (elem == null) return false;
        AttributeSet attrs = elem.getAttributes();
        if (attrs == null) return false;

        Object name = attrs.getAttribute(StyleConstants.NameAttribute);
        if (name == null) name = elem.getName();
        String nameStr = name != null ? name.toString().toLowerCase() : "";

        Object classAttr = attrs.getAttribute(HTML.Attribute.CLASS);
        if (classAttr == null) classAttr = attrs.getAttribute("class");
        if (classAttr != null) {
            String cStr = classAttr.toString().toLowerCase();
            if (cStr.contains("wb-card") || cStr.contains("wb-shape") || cStr.contains("wb-flow")
                    || cStr.contains("wb-callout") || cStr.contains("wb-video-card")) {
                return true;
            }
        }

        if (attrs.getAttribute("data-card") != null || attrs.getAttribute("data-shape") != null
                || attrs.getAttribute("data-flow") != null || attrs.getAttribute("data-callout") != null
                || attrs.getAttribute("data-theme") != null) {
            return true;
        }

        if ("div".equals(nameStr) || "span".equals(nameStr) || "table".equals(nameStr) || "p".equals(nameStr)) {
            Object styleAttr = attrs.getAttribute(HTML.Attribute.STYLE);
            if (styleAttr == null) styleAttr = attrs.getAttribute("style");
            if (styleAttr != null) {
                String sStr = styleAttr.toString().toLowerCase();
                if ((sStr.contains("border-radius") && sStr.contains("background"))
                        || sStr.contains("border-left:4px") || sStr.contains("border-left:5px")) {
                    return true;
                }
            }
        }

        return false;
    }

    public void openCardShapeEditor(Element cardOrShape) {
        if (cardOrShape == null) return;
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();

        String text = getElementPlainText(doc, cardOrShape);
        String initialTitle = "";
        String initialValue = "";
        String initialSub = "";

        String[] lines = text.split("\n");
        java.util.List<String> validLines = new java.util.ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) validLines.add(trimmed);
        }

        if (validLines.size() == 1) {
            initialTitle = validLines.get(0);
        } else if (validLines.size() == 2) {
            initialTitle = validLines.get(0);
            initialSub = validLines.get(1);
        } else if (validLines.size() >= 3) {
            initialTitle = validLines.get(0);
            initialValue = validLines.get(1);
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < validLines.size(); i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(validLines.get(i));
            }
            initialSub = sb.toString();
        }

        String themeKey = "corporate";
        AttributeSet attrs = cardOrShape.getAttributes();
        Object themeAttr = attrs != null ? attrs.getAttribute("data-theme") : null;
        if (themeAttr != null) {
            themeKey = themeAttr.toString();
        } else {
            String styleStr = getElementStyle(cardOrShape).toLowerCase();
            if (styleStr.contains("#10b981") || styleStr.contains("#16a34a") || styleStr.contains("#ecfdf5")) {
                themeKey = "emerald";
            } else if (styleStr.contains("#7c3aed") || styleStr.contains("#8b5cf6") || styleStr.contains("#f5f3ff")) {
                themeKey = "violet";
            } else if (styleStr.contains("#f59e0b") || styleStr.contains("#b45309") || styleStr.contains("#fffbeb") || styleStr.contains("#fde047")) {
                themeKey = "amber";
            } else if (styleStr.contains("#ea580c") || styleStr.contains("#e11d48") || styleStr.contains("#fee2e2") || styleStr.contains("#fff7ed")) {
                themeKey = "rose";
            } else if (styleStr.contains("#0f172a") || styleStr.contains("#1e293b")) {
                themeKey = "slate";
            } else if (styleStr.contains("#e2e8f0") || styleStr.contains("#f8fafc")) {
                themeKey = "minimal";
            }
        }

        Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        CardShapeCustomizerDialog dialog = new CardShapeCustomizerDialog(
                parentFrame,
                initialTitle.isEmpty() ? "Executive Metric / Topic" : initialTitle,
                initialValue.isEmpty() ? "$128,450" : initialValue,
                initialSub.isEmpty() ? "Detailed description or key milestone summary." : initialSub,
                themeKey
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            applyCustomizedValues(cardOrShape, dialog.getTitleText(), dialog.getValueText(), dialog.getSubtitleText(), dialog.getSelectedThemeKey());
        }
    }

    public void applyCustomizedValues(Element elem, String title, String val, String sub, String themeKey) {
        if (elem == null) return;
        CardShapeCustomizerDialog.ThemePalette pal = CardShapeCustomizerDialog.getPalette(themeKey);
        String primaryHex = toHex(pal.primary());
        String bgHex = toHex(pal.bg());
        String borderHex = toHex(pal.border());
        String textHex = toHex(pal.text());

        String valHtml = (val != null && !val.trim().isEmpty())
                ? "<div class='wb-card-val' style='font-size:26px;font-weight:bold;color:" + primaryHex + ";margin:6px 0;'>" + escapeHtml(val) + "</div>"
                : "";
        String subHtml = (sub != null && !sub.trim().isEmpty())
                ? "<div class='wb-card-sub' style='font-size:13px;color:" + textHex + ";margin-top:4px;'>" + escapeHtml(sub) + "</div>"
                : "";

        String newHtml = "<div class='wb-card' data-card='custom' data-theme='" + themeKey + "' style='border:2px solid " + borderHex + ";background-color:" + bgHex + ";padding:16px 20px;border-radius:10px;margin:16px 0;color:" + textHex + ";'>" +
                "<div class='wb-card-title' style='font-weight:bold;font-size:15px;color:" + primaryHex + ";'>" + escapeHtml(title) + "</div>" +
                valHtml +
                subHtml +
                "</div>";

        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            suppressEvents = true;
            try {
                doc.setOuterHTML(elem, newHtml);
            } finally {
                suppressEvents = false;
            }
            selectedMediaElement = null;
            selectedMediaBounds = null;
            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void applyCardShapeTheme(Element elem, String themeKey) {
        if (elem == null) return;
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        String text = getElementPlainText(doc, elem);

        String[] lines = text.split("\n");
        java.util.List<String> valid = new java.util.ArrayList<>();
        for (String l : lines) {
            String tr = l.trim();
            if (!tr.isEmpty()) valid.add(tr);
        }

        String title = !valid.isEmpty() ? valid.get(0) : "Card Object";
        String val = valid.size() > 2 ? valid.get(1) : "";
        String sub = "";
        if (valid.size() == 2) {
            sub = valid.get(1);
        } else if (valid.size() > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < valid.size(); i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(valid.get(i));
            }
            sub = sb.toString();
        }

        applyCustomizedValues(elem, title, val, sub, themeKey);
    }

    public void applyActiveCardShapeTheme(String themeKey) {
        Element targetElem = selectedMediaElement;
        if (targetElem == null) {
            int pos = textPane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element chElem = doc.getCharacterElement(pos);
            targetElem = findCardOrShapeAt(chElem);
        }
        if (targetElem != null) {
            applyCardShapeTheme(targetElem, themeKey);
        } else {
            ModernDialog.showMessage(this, "Card & Shape", "Select or click inside a card, shape, or callout first.", ModernDialog.MessageType.INFO);
        }
    }

    public void alignCardShapeElement(Element elem, String align) {
        if (elem == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int start = elem.getStartOffset();
            int len = elem.getEndOffset() - start;
            SimpleAttributeSet alignAttr = new SimpleAttributeSet();
            int a = StyleConstants.ALIGN_LEFT;
            if ("center".equalsIgnoreCase(align)) a = StyleConstants.ALIGN_CENTER;
            else if ("right".equalsIgnoreCase(align)) a = StyleConstants.ALIGN_RIGHT;
            StyleConstants.setAlignment(alignAttr, a);
            doc.setParagraphAttributes(start, len, alignAttr, false);
            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void alignSelectedCardShape(String align) {
        Element targetElem = selectedMediaElement;
        if (targetElem == null) {
            int pos = textPane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element chElem = doc.getCharacterElement(pos);
            targetElem = findCardOrShapeAt(chElem);
        }
        if (targetElem != null) {
            alignCardShapeElement(targetElem, align);
        }
    }

    public void deleteCardShapeElement(Element elem) {
        if (elem == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int start = elem.getStartOffset();
            int end = elem.getEndOffset();
            suppressEvents = true;
            try {
                doc.remove(start, end - start);
            } finally {
                suppressEvents = false;
            }
            selectedMediaElement = null;
            selectedMediaBounds = null;
            triggerSave();
            pageLayoutTimer.restart();
            if (contextualTabHandler != null) contextualTabHandler.hideContextualTabs();
            textPane.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void deleteSelectedCardShape() {
        Element targetElem = selectedMediaElement;
        if (targetElem == null) {
            int pos = textPane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element chElem = doc.getCharacterElement(pos);
            targetElem = findCardOrShapeAt(chElem);
        }
        if (targetElem != null) {
            deleteCardShapeElement(targetElem);
        } else {
            ModernDialog.showMessage(this, "Delete", "Click on a card or shape to delete it.", ModernDialog.MessageType.INFO);
        }
    }

    public void showCardShapeCustomizerDialog() {
        Element targetElem = selectedMediaElement;
        if (targetElem == null) {
            int pos = textPane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element chElem = doc.getCharacterElement(pos);
            targetElem = findCardOrShapeAt(chElem);
        }
        if (targetElem != null) {
            openCardShapeEditor(targetElem);
        } else {
            Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            CardShapeCustomizerDialog dialog = new CardShapeCustomizerDialog(
                    parentFrame,
                    "Executive Milestone",
                    "$248,500",
                    "Key performance milestone achieved ahead of schedule.",
                    "corporate"
            );
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                CardShapeCustomizerDialog.ThemePalette pal = CardShapeCustomizerDialog.getPalette(dialog.getSelectedThemeKey());
                String primaryHex = toHex(pal.primary());
                String bgHex = toHex(pal.bg());
                String borderHex = toHex(pal.border());
                String textHex = toHex(pal.text());

                String html = "<div class='wb-card' data-card='custom' data-theme='" + dialog.getSelectedThemeKey() + "' style='border:2px solid " + borderHex + ";background-color:" + bgHex + ";padding:16px 20px;border-radius:10px;margin:16px 0;color:" + textHex + ";'>" +
                        "<div class='wb-card-title' style='font-weight:bold;font-size:15px;color:" + primaryHex + ";'>" + escapeHtml(dialog.getTitleText()) + "</div>" +
                        "<div class='wb-card-val' style='font-size:26px;font-weight:bold;color:" + primaryHex + ";margin:6px 0;'>" + escapeHtml(dialog.getValueText()) + "</div>" +
                        "<div class='wb-card-sub' style='font-size:13px;color:" + textHex + ";margin-top:4px;'>" + escapeHtml(dialog.getSubtitleText()) + "</div>" +
                        "</div><p><br></p>";
                insertHtml(html);
            }
        }
    }

    public Element getSelectedMediaElement() {
        return selectedMediaElement;
    }

    public Rectangle getSelectedMediaBounds() {
        return selectedMediaBounds;
    }

    public void duplicateCardShapeElement(Element elem) {
        if (elem == null) return;
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            AttributeSet attrs = elem.getAttributes();
            String htmlToInsert = null;

            if (HTML.Tag.IMG.equals(attrs.getAttribute(StyleConstants.NameAttribute))) {
                Object src = attrs.getAttribute(HTML.Attribute.SRC);
                Object w = attrs.getAttribute(HTML.Attribute.WIDTH);
                Object h = attrs.getAttribute(HTML.Attribute.HEIGHT);
                String style = getElementStyle(elem);
                htmlToInsert = "<p><img src='" + (src != null ? src : "") + "' width='" + (w != null ? w : "500") + "'" +
                        (h != null ? " height='" + h + "'" : "") +
                        (!style.isEmpty() ? " style='" + style + "'" : "") + "></p>";
            } else {
                String plain = getElementPlainText(doc, elem);
                String[] lines = plain.split("\n");
                String title = lines.length > 0 ? lines[0].trim() : "Duplicated Card";
                String val = lines.length > 1 ? lines[1].trim() : "";
                String sub = lines.length > 2 ? lines[2].trim() : "";

                Object themeAttr = attrs.getAttribute("data-theme");
                String themeKey = themeAttr != null ? themeAttr.toString() : "BLUE";
                CardShapeCustomizerDialog.ThemePalette pal = CardShapeCustomizerDialog.getPalette(themeKey);

                String primaryHex = toHex(pal.primary());
                String bgHex = toHex(pal.bg());
                String borderHex = toHex(pal.border());
                String textHex = toHex(pal.text());

                String valHtml = (!val.isEmpty())
                        ? "<div class='wb-card-val' style='font-size:26px;font-weight:bold;color:" + primaryHex + ";margin:6px 0;'>" + escapeHtml(val) + "</div>"
                        : "";
                String subHtml = (!sub.isEmpty())
                        ? "<div class='wb-card-sub' style='font-size:13px;color:" + textHex + ";margin-top:4px;'>" + escapeHtml(sub) + "</div>"
                        : "";
                htmlToInsert = "<div class='wb-card' data-card='custom' data-theme='" + themeKey + "' style='border:2px solid " + borderHex + ";background-color:" + bgHex + ";padding:16px 20px;border-radius:10px;margin:16px 0;color:" + textHex + ";'>" +
                        "<div class='wb-card-title' style='font-weight:bold;font-size:15px;color:" + primaryHex + ";'>" + escapeHtml(title) + "</div>" +
                        valHtml + subHtml + "</div><p><br></p>";
            }

            if (htmlToInsert != null) {
                doc.insertAfterEnd(elem, htmlToInsert);
                undoManager.registerNamedAction("Duplicate Object");
                triggerSave();
                pageLayoutTimer.restart();
                textPane.repaint();
                pageSheet.repaint();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void applyCardShapeFullFormatting(Element elem, String title, String val, String sub, String themeKey,
                                             Color bgColor, Color borderColor, int borderWidth, String borderStyle,
                                             int borderRadius, String shadowPreset, int width, String align) {
        if (elem == null) return;
        CardShapeCustomizerDialog.ThemePalette pal = CardShapeCustomizerDialog.getPalette(themeKey);
        String primaryHex = toHex(pal.primary());
        String bgHex = bgColor != null ? toHex(bgColor) : toHex(pal.bg());
        String borderHex = borderColor != null ? toHex(borderColor) : toHex(pal.border());
        String textHex = toHex(pal.text());
        String bStyle = (borderStyle != null && !borderStyle.isEmpty()) ? borderStyle : "solid";

        String shadowCss = "";
        if ("soft".equalsIgnoreCase(shadowPreset)) {
            shadowCss = "box-shadow:0 4px 12px rgba(0,0,0,0.08);";
        } else if ("elevation".equalsIgnoreCase(shadowPreset)) {
            shadowCss = "box-shadow:0 8px 24px rgba(0,0,0,0.14);";
        } else if ("glow".equalsIgnoreCase(shadowPreset)) {
            shadowCss = "box-shadow:0 0 14px " + borderHex + "66;";
        } else if ("depth".equalsIgnoreCase(shadowPreset)) {
            shadowCss = "box-shadow:0 14px 32px rgba(15,23,42,0.25);";
        }

        String widthCss = width > 0 ? "width:" + width + "px;" : "";
        String marginCss = "margin:16px 0;";
        if ("center".equalsIgnoreCase(align)) {
            marginCss = "margin:16px auto;text-align:center;";
        } else if ("right".equalsIgnoreCase(align)) {
            marginCss = "margin:16px 0 16px auto;text-align:right;";
        }

        String valHtml = (val != null && !val.trim().isEmpty())
                ? "<div class='wb-card-val' style='font-size:26px;font-weight:bold;color:" + primaryHex + ";margin:6px 0;'>" + escapeHtml(val) + "</div>"
                : "";
        String subHtml = (sub != null && !sub.trim().isEmpty())
                ? "<div class='wb-card-sub' style='font-size:13px;color:" + textHex + ";margin-top:4px;'>" + escapeHtml(sub) + "</div>"
                : "";

        String newHtml = "<div class='wb-card' data-card='custom' data-theme='" + themeKey + "' data-shadow='" + shadowPreset + "' style='border:" + borderWidth + "px " + bStyle + " " + borderHex + ";background-color:" + bgHex + ";padding:16px 20px;border-radius:" + borderRadius + "px;" + marginCss + shadowCss + widthCss + "color:" + textHex + ";'>" +
                "<div class='wb-card-title' style='font-weight:bold;font-size:15px;color:" + primaryHex + ";'>" + escapeHtml(title) + "</div>" +
                valHtml +
                subHtml +
                "</div>";

        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            int start = elem.getStartOffset();
            suppressEvents = true;
            try {
                doc.setOuterHTML(elem, newHtml);
            } finally {
                suppressEvents = false;
            }
            int pos = Math.min(doc.getLength() - 1, Math.max(0, start));
            Element newElem = findCardOrShapeAt(doc.getCharacterElement(pos));
            if (newElem != null) {
                selectedMediaElement = newElem;
                selectedMediaBounds = getElementBounds(newElem);
            }
            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
            pageSheet.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void applyImageFullFormatting(Element elem, int width, int height, Color borderColor, int borderWidth,
                                        String borderStyle, int borderRadius, String shadowPreset, String align) {
        if (elem == null) return;
        try {
            AttributeSet attrs = elem.getAttributes();
            Object src = attrs.getAttribute(HTML.Attribute.SRC);
            if (src == null) return;

            String bStyle = (borderStyle != null && !borderStyle.isEmpty()) ? borderStyle : "solid";
            String borderHex = borderColor != null ? toHex(borderColor) : "#cbd5e1";

            StringBuilder style = new StringBuilder();
            if (borderWidth > 0) {
                style.append("border:").append(borderWidth).append("px ").append(bStyle).append(" ").append(borderHex).append(";");
            }
            if (borderRadius > 0) {
                style.append("border-radius:").append(borderRadius).append("px;");
            }
            if ("soft".equalsIgnoreCase(shadowPreset)) {
                style.append("box-shadow:0 4px 12px rgba(0,0,0,0.08);");
            } else if ("elevation".equalsIgnoreCase(shadowPreset)) {
                style.append("box-shadow:0 8px 24px rgba(0,0,0,0.14);");
            } else if ("glow".equalsIgnoreCase(shadowPreset)) {
                style.append("box-shadow:0 0 14px rgba(37,99,235,0.3);");
            } else if ("depth".equalsIgnoreCase(shadowPreset)) {
                style.append("box-shadow:0 14px 32px rgba(15,23,42,0.25);");
            }

            String imgTag = "<img src='" + src + "' width='" + width + "'" + (height > 0 ? " height='" + height + "'" : "") + (style.length() > 0 ? " style='" + style + "'" : "") + ">";

            String alignAttr = (align != null && !align.isEmpty()) ? align : "center";
            String newImgHtml = "<p align='" + alignAttr + "'>" + imgTag + "</p>";

            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            Element parentP = elem.getParentElement();
            Element targetToReplace = (parentP != null && "p".equalsIgnoreCase(parentP.getName()) && parentP.getElementCount() <= 2) ? parentP : elem;

            suppressEvents = true;
            try {
                doc.setOuterHTML(targetToReplace, newImgHtml);
            } finally {
                suppressEvents = false;
            }

            Element newElem = findImageBySrc(doc.getDefaultRootElement(), src.toString());
            if (newElem != null) {
                selectedMediaElement = newElem;
                lastClickedImageElement = newElem;
                selectedMediaBounds = getElementBounds(newElem);
            }

            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
            pageSheet.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public String getElementStyle(Element elem) {
        if (elem == null) return "";
        AttributeSet attrs = elem.getAttributes();
        if (attrs == null) return "";
        Object s = attrs.getAttribute(HTML.Attribute.STYLE);
        if (s == null) s = attrs.getAttribute("style");
        return s != null ? s.toString() : "";
    }

    public Element findVideoCardAt(Element elem) {
        while (elem != null) {
            AttributeSet attrs = elem.getAttributes();
            if (attrs != null) {
                Object url = attrs.getAttribute("data-video-url");
                if (url != null) return elem;
                Object classAttr = attrs.getAttribute(HTML.Attribute.CLASS);
                if (classAttr != null && classAttr.toString().contains("wb-video-card")) return elem;
                Object classStr = attrs.getAttribute("class");
                if (classStr != null && classStr.toString().contains("wb-video-card")) return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    public void playOrOpenVideo(Element videoCard) {
        if (videoCard == null) return;
        try {
            AttributeSet attrs = videoCard.getAttributes();
            Object urlObj = attrs.getAttribute("data-video-url");
            String url = urlObj != null ? urlObj.toString() : "";
            if (url.isEmpty()) {
                String text = getElementPlainText((HTMLDocument) textPane.getDocument(), videoCard);
                int idx = text.indexOf("http");
                if (idx >= 0) url = text.substring(idx).trim();
            }

            if (!url.isEmpty()) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    File f = new File(url);
                    if (f.exists()) {
                        Desktop.getDesktop().open(f);
                    } else {
                        ModernDialog.showMessage(this, "Play Video", "Video file not found: " + url, ModernDialog.MessageType.WARNING);
                    }
                }
            }
        } catch (Exception ex) {
            ModernDialog.showMessage(this, "Error", "Could not open video: " + ex.getMessage(), ModernDialog.MessageType.ERROR);
        }
    }

    public void openSelectedVideo() {
        int pos = textPane.getCaretPosition();
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        Element elem = doc.getCharacterElement(pos);
        Element vCard = findVideoCardAt(elem);
        if (vCard != null) {
            playOrOpenVideo(vCard);
        } else {
            ModernDialog.showMessage(this, "Play Video", "Click on a video card in the document first.", ModernDialog.MessageType.INFO);
        }
    }

    public void deleteSelectedVideo() {
        int pos = textPane.getCaretPosition();
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        Element elem = doc.getCharacterElement(pos);
        Element vCard = findVideoCardAt(elem);
        if (vCard != null) {
            try {
                suppressEvents = true;
                try {
                    doc.remove(vCard.getStartOffset(), vCard.getEndOffset() - vCard.getStartOffset());
                } finally {
                    suppressEvents = false;
                }
                triggerSave();
                pageLayoutTimer.restart();
                if (contextualTabHandler != null) contextualTabHandler.hideContextualTabs();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // =========================================================================
    // Advanced Smart Accessible Options & Engine
    // =========================================================================
    private Process currentSpeechProcess = null;
    private boolean highContrastMode = false;
    private boolean dyslexicFontMode = false;

    public void speakSelectionOrDocument() {
        if (currentSpeechProcess != null && currentSpeechProcess.isAlive()) {
            currentSpeechProcess.destroy();
            currentSpeechProcess = null;
            ModernDialog.showMessage(this, "Read Aloud", "Speech reader stopped.", ModernDialog.MessageType.INFO);
            return;
        }

        String text = textPane.getSelectedText();
        if (text == null || text.isBlank()) {
            text = getPlainText();
        }
        if (text == null || text.isBlank()) {
            ModernDialog.showMessage(this, "Read Aloud", "No text to read. Document is empty.", ModernDialog.MessageType.INFO);
            return;
        }

        final String textToSpeak = text;
        new Thread(() -> {
            try {
                currentSpeechProcess = new ProcessBuilder("say", textToSpeak).start();
                currentSpeechProcess.waitFor();
            } catch (Exception ignored) {}
        }).start();

        ModernDialog.showMessage(this,
                "Read Aloud (Smart Accessibility)",
                "🔊 Reading document aloud...\n\n(Click 'Read Aloud' again at any time to pause/stop playback)",
                ModernDialog.MessageType.INFO);
    }

    public void toggleHighContrastMode() {
        highContrastMode = !highContrastMode;
        if (highContrastMode) {
            currentPaperTone = new Color(15, 23, 42);
            textPane.setBackground(new Color(15, 23, 42));
            textPane.setForeground(new Color(254, 240, 138));
            textPane.setCaretColor(new Color(254, 240, 138));
        } else {
            currentPaperTone = Color.WHITE;
            textPane.setBackground(Color.WHITE);
            textPane.setForeground(new Color(30, 41, 59));
            textPane.setCaretColor(new Color(30, 41, 59));
        }
        pageSheet.repaint();
        textPane.repaint();
    }

    public void toggleDyslexicFont() {
        dyslexicFontMode = !dyslexicFontMode;
        SimpleAttributeSet sa = new SimpleAttributeSet();
        if (dyslexicFontMode) {
            StyleConstants.setFontFamily(sa, "Verdana");
            StyleConstants.setFontSize(sa, 16);
            StyleConstants.setLineSpacing(sa, 0.4f);
        } else {
            StyleConstants.setFontFamily(sa, "Calibri");
            StyleConstants.setFontSize(sa, 14);
            StyleConstants.setLineSpacing(sa, 0.0f);
        }
        textPane.setCharacterAttributes(sa, false);
        textPane.setParagraphAttributes(sa, false);
        triggerSave();
    }

    public void showHeadingsNavigator() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        HeadingsOutlineDialog dlg = new HeadingsOutlineDialog(owner, textPane);
        dlg.setVisible(true);
    }

    public void runAccessibilityAudit() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        AccessibilityAuditDialog dlg = new AccessibilityAuditDialog(owner, textPane.getText(), this::setDocumentHtml);
        dlg.setVisible(true);
    }

    public void applyImageFilter(String filter) {
        Element elem = getLastClickedImageElement();
        if (elem == null) {
            ModernDialog.showMessage(this, "Image Filter", "Click an image in the document first.", ModernDialog.MessageType.INFO);
            return;
        }
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            AttributeSet attrs = elem.getAttributes();
            Object src = attrs.getAttribute(HTML.Attribute.SRC);
            if (src == null) return;
            int width = getElementWidth(elem, 520);
            int height = getElementHeight(elem, 290);

            String f = filter.toLowerCase().trim();
            String normFilter = "original";
            String css = "border-radius:8px;";

            if (f.contains("original") || f.contains("reset")) {
                normFilter = "original";
                css = "border-radius:8px;";
            } else if (f.contains("grayscale") || f.contains("mono")) {
                normFilter = "grayscale";
                css = "filter:grayscale(100%);border-radius:8px;";
            } else if (f.contains("sepia")) {
                normFilter = "sepia";
                css = "filter:sepia(85%);border-radius:8px;";
            } else if (f.contains("contrast")) {
                normFilter = "contrast";
                css = "filter:contrast(150%);border-radius:8px;";
            } else if (f.contains("vivid")) {
                normFilter = "vivid";
                css = "filter:saturate(180%) contrast(110%);border-radius:8px;";
            } else if (f.contains("invert")) {
                normFilter = "invert";
                css = "filter:invert(90%);border-radius:8px;";
            } else if (f.contains("blur")) {
                normFilter = "blur";
                css = "filter:blur(3px);border-radius:8px;";
            } else if (f.contains("oval") || f.contains("circle") || f.contains("pill")) {
                normFilter = "oval";
                css = "border-radius:50%;object-fit:cover;";
            } else if (f.contains("round")) {
                normFilter = "rounded";
                css = "border-radius:24px;";
            } else if (f.contains("shadow")) {
                normFilter = "shadow";
                css = "box-shadow:0 12px 28px rgba(0,0,0,0.28);border-radius:8px;";
            } else if (f.contains("border") || f.contains("frame")) {
                normFilter = "border";
                css = "border:4px solid #3b82f6;border-radius:8px;padding:2px;";
            } else if (f.contains("vignette")) {
                normFilter = "vignette";
                css = "box-shadow:inset 0 0 35px rgba(0,0,0,0.55), 0 6px 16px rgba(0,0,0,0.2);border-radius:8px;";
            }

            String newImgHtml = "<img src='" + src + "' width='" + width + "' height='" + height + "' data-filter='" + normFilter + "' filter='" + normFilter + "' class='wb-filter-" + normFilter + "' style='" + css + "'>";
            suppressEvents = true;
            try {
                doc.setOuterHTML(elem, newImgHtml);
            } finally {
                suppressEvents = false;
            }

            // Immediately re-select element to prevent stale references
            Element newElem = findImageBySrc(doc.getDefaultRootElement(), src.toString());
            if (newElem != null) {
                selectedMediaElement = newElem;
                lastClickedImageElement = newElem;
                selectedMediaBounds = getElementBounds(newElem);
            }

            undoManager.registerNamedAction("Filter: " + filter);
            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
            pageSheet.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setImageWrap(String wrap) {
        Element elem = getLastClickedImageElement();
        if (elem == null) {
            ModernDialog.showMessage(this, "Text Wrap", "Click an image in the document first.", ModernDialog.MessageType.INFO);
            return;
        }
        try {
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            AttributeSet attrs = elem.getAttributes();
            Object src = attrs.getAttribute(HTML.Attribute.SRC);
            if (src == null) return;
            int width = getElementWidth(elem, 520);
            int height = getElementHeight(elem, 290);
            Object dataFilter = attrs.getAttribute("data-filter");
            String filterAttrStr = dataFilter != null ? " data-filter='" + dataFilter + "' filter='" + dataFilter + "' class='wb-filter-" + dataFilter + "'" : "";

            String style = "border-radius:8px;";
            if ("float-left".equalsIgnoreCase(wrap)) {
                style += "float:left;margin:8px 16px 8px 0;";
            } else if ("float-right".equalsIgnoreCase(wrap)) {
                style += "float:right;margin:8px 0 8px 16px;";
            } else {
                style += "display:block;margin:12px auto;";
            }

            String newImgHtml = "<img src='" + src + "' width='" + width + "' height='" + height + "'" + filterAttrStr + " style='" + style + "'>";
            suppressEvents = true;
            try {
                doc.setOuterHTML(elem, newImgHtml);
            } finally {
                suppressEvents = false;
            }

            Element newElem = findImageBySrc(doc.getDefaultRootElement(), src.toString());
            if (newElem != null) {
                selectedMediaElement = newElem;
                lastClickedImageElement = newElem;
                selectedMediaBounds = getElementBounds(newElem);
            }

            undoManager.registerNamedAction("Wrap: " + wrap);
            triggerSave();
            pageLayoutTimer.restart();
            textPane.repaint();
            pageSheet.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onDocChange() {
        if (suppressEvents) return;
        pageLayoutTimer.restart();
        autoSaveTimer.restart();
    }

    // --- Scrollable Implementation for Centered Paper Canvas in JScrollPane ---
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 120;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            return ((JViewport) getParent()).getWidth() > getPreferredSize().width;
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    // =========================================================================
    // MULTI-PAGE BODY VIEW PAGINATION (Microsoft Word Standard)
    // =========================================================================

    private class PagedBodyView extends BlockView {
        private int[] lastOffsets;

        public PagedBodyView(Element elem, int axis) {
            super(elem, axis);
        }

        public int getChildOffset(int index) {
            if (lastOffsets != null && index >= 0 && index < lastOffsets.length) {
                return lastOffsets[index];
            }
            return 0;
        }

        @Override
        protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            super.layoutMajorAxis(targetSpan, axis, offsets, spans);
            int pageUnit = getPageUnit();
            int bodyH = getPageBodyHeight();
            int currentY = 0;
            boolean anyOffsetChanged = false;
            for (int i = 0; i < offsets.length; i++) {
                int childH = spans[i];
                int pageIndex = currentY / pageUnit;
                int pageBodyEnd = pageIndex * pageUnit + bodyH;
                if (currentY + childH > pageBodyEnd && currentY > pageIndex * pageUnit) {
                    currentY = (pageIndex + 1) * pageUnit;
                    pageIndex = currentY / pageUnit;
                }
                if (lastOffsets != null && i < lastOffsets.length && lastOffsets[i] != currentY) {
                    anyOffsetChanged = true;
                }
                offsets[i] = currentY;
                currentY += childH;
                if (i < getViewCount()) {
                    View cv = getView(i);
                    if (cv != null && isPageBreakElement(cv.getElement())) {
                        currentY = (pageIndex + 1) * pageUnit;
                    }
                }
            }
            lastOffsets = offsets.clone();
            if (anyOffsetChanged) {
                pageLayoutTimer.restart();
            }
        }

        @Override
        public float getPreferredSpan(int axis) {
            if (axis == View.Y_AXIS && lastOffsets != null && lastOffsets.length == getViewCount() && getViewCount() > 0) {
                int n = getViewCount();
                return lastOffsets[n - 1] + getView(n - 1).getPreferredSpan(axis);
            }
            return super.getPreferredSpan(axis);
        }
    }

    private class PagedParagraphView extends javax.swing.text.html.ParagraphView {
        private int[] lastOffsets;

        public PagedParagraphView(Element elem) {
            super(elem);
        }

        private boolean hasImageOrMedia(Element elem) {
            if (elem == null) return false;
            if (isCardOrShapeElement(elem)) return true;
            if (isPageBreakElement(elem)) return true;
            AttributeSet as = elem.getAttributes();
            if (as != null) {
                Object name = as.getAttribute(StyleConstants.NameAttribute);
                if (HTML.Tag.IMG.equals(name) || HTML.Tag.TABLE.equals(name) || HTML.Tag.TR.equals(name) || HTML.Tag.TD.equals(name)) {
                    return true;
                }
                Object cls = as.getAttribute(HTML.Attribute.CLASS);
                if (cls != null) {
                    String c = cls.toString();
                    if (c.contains("wb-video-card") || c.contains("wb-card") || c.contains("wb-shape") || c.contains("wb-flow") || c.contains("wb-callout") || c.contains("page-break")) {
                        return true;
                    }
                }
                Object style = as.getAttribute(HTML.Attribute.STYLE);
                if (style != null) {
                    String st = style.toString().toLowerCase();
                    if (st.contains("border-radius") || st.contains("background-color") || st.contains("border:") || st.contains("page-break")) {
                        return true;
                    }
                }
            }
            for (int i = 0; i < elem.getElementCount(); i++) {
                if (hasImageOrMedia(elem.getElement(i))) return true;
            }
            return false;
        }

        @Override
        protected void setPropertiesFromAttributes() {
            super.setPropertiesFromAttributes();
            AttributeSet as = getAttributes();
            String name = getElement().getName();
            float defaultSpacing = "p".equalsIgnoreCase(name) ? 0.28f : 0.08f;
            float spacing = defaultSpacing;
            if (as != null && as.isDefined(StyleConstants.LineSpacing)) {
                spacing = StyleConstants.getLineSpacing(as);
            }
            setLineSpacing(spacing);
        }

        private int getParentBlockY() {
            View parent = getParent();
            if (parent instanceof PagedBodyView pb) {
                for (int i = 0; i < pb.getViewCount(); i++) {
                    if (pb.getView(i) == this) return pb.getChildOffset(i);
                }
            }
            return -1;
        }

        @Override
        protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            super.layoutMajorAxis(targetSpan, axis, offsets, spans);
            if (hasImageOrMedia(getElement())) {
                // Media paragraphs (charts, images, figures) are placed atomically by PagedBodyView.
                // Do not split internally to prevent oscillating page displacement and visual glitching.
                lastOffsets = offsets.clone();
                return;
            }
            int pageUnit = getPageUnit();
            int bodyH = getPageBodyHeight();
            int parentY = getParentBlockY();
            if (parentY < 0) {
                lastOffsets = offsets.clone();
                return;
            }
            int currentY = 0;
            for (int i = 0; i < offsets.length; i++) {
                int rowH = spans[i];
                int absY = parentY + currentY;
                int pageIndex = absY / pageUnit;
                int pageBodyEnd = pageIndex * pageUnit + bodyH;
                if (absY + rowH > pageBodyEnd && absY > pageIndex * pageUnit) {
                    int nextBodyStart = (pageIndex + 1) * pageUnit;
                    currentY = nextBodyStart - parentY;
                }
                offsets[i] = currentY;
                currentY += rowH;
            }
            lastOffsets = offsets.clone();
        }

        @Override
        public float getPreferredSpan(int axis) {
            if (axis == View.Y_AXIS && lastOffsets != null && lastOffsets.length == getViewCount() && getViewCount() > 0) {
                int n = getViewCount();
                return lastOffsets[n - 1] + getView(n - 1).getPreferredSpan(axis);
            }
            return super.getPreferredSpan(axis);
        }
    }

    public static class ModernImageView extends ImageView {
        private static final Map<String, BufferedImage> FILTER_CACHE = new ConcurrentHashMap<>();

        public ModernImageView(Element elem) {
            super(elem);
            setLoadsSynchronously(true);
        }

        @Override
        public float getPreferredSpan(int axis) {
            float span = super.getPreferredSpan(axis);
            Container c = getContainer();
            int maxW = -1;
            if (c != null) {
                DocumentCanvasPanel canvas = (DocumentCanvasPanel) SwingUtilities.getAncestorOfClass(DocumentCanvasPanel.class, c);
                if (canvas != null) {
                    maxW = canvas.getMaxPrintableWidth();
                } else {
                    maxW = c.getWidth() - 32;
                }
            }
            if (maxW > 0) {
                if (axis == View.X_AXIS) {
                    if (span > maxW) return maxW;
                } else if (axis == View.Y_AXIS) {
                    float origW = super.getPreferredSpan(View.X_AXIS);
                    if (origW > maxW && origW > 0) {
                        return span * ((float) maxW / origW);
                    }
                }
            }
            return span;
        }

        @Override
        public URL getImageURL() {
            String src = (String) getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
            if (src == null || src.isBlank()) return null;
            try {
                if (src.startsWith("file:")) {
                    String clean = src;
                    while (clean.startsWith("file:////")) clean = "file:/" + clean.substring(8);
                    while (clean.startsWith("file:///")) clean = "file:/" + clean.substring(7);
                    try {
                        return new java.net.URI(clean).toURL();
                    } catch (Exception ex) {
                        return new java.net.URL(clean);
                    }
                }
                URL base = ((HTMLDocument) getDocument()).getBase();
                return new URL(base, src);
            } catch (Exception ex) {
                try {
                    String raw = src.replaceAll("^file:/+", "/");
                    File f = new File(raw);
                    if (f.exists()) {
                        return f.toURI().toURL();
                    }
                } catch (Exception ignored) {}
                return super.getImageURL();
            }
        }

        @Override
        public void paint(Graphics g, Shape a) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Rectangle rect = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
                if (rect.width <= 0 || rect.height <= 0) {
                    super.paint(g2, a);
                    return;
                }

                Image rawImg = getImage();
                if (rawImg == null || rawImg.getWidth(null) <= 0) {
                    URL u = getImageURL();
                    if (u != null) {
                        try {
                            BufferedImage bi = javax.imageio.ImageIO.read(u);
                            if (bi != null) {
                                rawImg = bi;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (rawImg == null) {
                    super.paint(g2, a);
                    return;
                }

                AttributeSet attrs = getElement().getAttributes();
                String filter = extractFilter(attrs);

                paintFiltered(g2, rawImg, rect, filter);
            } catch (Exception ex) {
                super.paint(g2, a);
            } finally {
                g2.dispose();
            }
        }

        private static String extractFilter(AttributeSet attrs) {
            if (attrs == null) return "original";
            Object df = attrs.getAttribute("data-filter");
            if (df != null && !df.toString().isBlank()) return normalize(df.toString());
            Object f = attrs.getAttribute("filter");
            if (f != null && !f.toString().isBlank()) return normalize(f.toString());
            Object cls = attrs.getAttribute(HTML.Attribute.CLASS);
            if (cls != null && cls.toString().contains("wb-filter-")) {
                String s = cls.toString();
                int idx = s.indexOf("wb-filter-");
                String sub = s.substring(idx + 10).split("[\\s'\"]")[0];
                return normalize(sub);
            }
            Object style = attrs.getAttribute(HTML.Attribute.STYLE);
            if (style != null) {
                String s = style.toString().toLowerCase();
                if (s.contains("grayscale")) return "grayscale";
                if (s.contains("sepia")) return "sepia";
                if (s.contains("saturate") || s.contains("vivid")) return "vivid";
                if (s.contains("contrast")) return "contrast";
                if (s.contains("invert")) return "invert";
                if (s.contains("blur")) return "blur";
                if (s.contains("border-radius:50%") || s.contains("border-radius: 50%")) return "oval";
                if (s.contains("border-radius:24px") || s.contains("border-radius: 24px")) return "rounded";
                if (s.contains("box-shadow")) return s.contains("inset") ? "vignette" : "shadow";
                if (s.contains("border:4px") || s.contains("border: 4px")) return "border";
            }
            return "original";
        }

        private static String normalize(String name) {
            if (name == null) return "original";
            String s = name.toLowerCase().trim();
            if (s.contains("gray") || s.contains("mono")) return "grayscale";
            if (s.contains("sepia")) return "sepia";
            if (s.contains("vivid")) return "vivid";
            if (s.contains("contrast")) return "contrast";
            if (s.contains("invert")) return "invert";
            if (s.contains("blur")) return "blur";
            if (s.contains("oval") || s.contains("circle")) return "oval";
            if (s.contains("round")) return "rounded";
            if (s.contains("shadow")) return "shadow";
            if (s.contains("border") || s.contains("frame")) return "border";
            if (s.contains("vignette")) return "vignette";
            return "original";
        }

        private void paintFiltered(Graphics2D g2, Image rawImg, Rectangle rect, String filter) {
            if ("rounded".equals(filter)) {
                Shape clip = new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, 24, 24);
                Shape old = g2.getClip();
                g2.clip(clip);
                g2.drawImage(rawImg, rect.x, rect.y, rect.width, rect.height, null);
                g2.setClip(old);
                g2.setColor(new Color(203, 213, 225));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(clip);
                return;
            }
            if ("oval".equals(filter)) {
                Shape clip = new Ellipse2D.Float(rect.x, rect.y, rect.width, rect.height);
                Shape old = g2.getClip();
                g2.clip(clip);
                g2.drawImage(rawImg, rect.x, rect.y, rect.width, rect.height, null);
                g2.setClip(old);
                g2.setColor(new Color(203, 213, 225));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(clip);
                return;
            }
            if ("shadow".equals(filter)) {
                g2.setColor(new Color(0, 0, 0, 16));
                g2.fillRoundRect(rect.x - 4, rect.y + 6, rect.width + 8, rect.height + 8, 14, 14);
                g2.setColor(new Color(0, 0, 0, 32));
                g2.fillRoundRect(rect.x - 2, rect.y + 4, rect.width + 4, rect.height + 6, 12, 12);
                g2.setColor(new Color(0, 0, 0, 55));
                g2.fillRoundRect(rect.x, rect.y + 2, rect.width, rect.height + 3, 10, 10);

                Shape clip = new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, 8, 8);
                Shape old = g2.getClip();
                g2.clip(clip);
                g2.drawImage(rawImg, rect.x, rect.y, rect.width, rect.height, null);
                g2.setClip(old);
                return;
            }
            if ("border".equals(filter)) {
                int bw = 4;
                g2.setColor(new Color(59, 130, 246));
                g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

                Shape clip = new RoundRectangle2D.Float(rect.x + bw, rect.y + bw, rect.width - bw * 2, rect.height - bw * 2, 6, 6);
                Shape old = g2.getClip();
                g2.clip(clip);
                g2.drawImage(rawImg, rect.x + bw, rect.y + bw, rect.width - bw * 2, rect.height - bw * 2, null);
                g2.setClip(old);
                return;
            }
            if ("vignette".equals(filter)) {
                Shape clip = new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, 8, 8);
                Shape old = g2.getClip();
                g2.clip(clip);
                g2.drawImage(rawImg, rect.x, rect.y, rect.width, rect.height, null);

                float cx = rect.x + rect.width / 2f;
                float cy = rect.y + rect.height / 2f;
                float radius = (float) Math.max(rect.width, rect.height) * 0.72f;
                Point2D center = new Point2D.Float(cx, cy);
                float[] dist = {0.45f, 1.0f};
                Color[] colors = {new Color(0, 0, 0, 0), new Color(0, 0, 0, 175)};
                RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, colors);
                g2.setPaint(rgp);
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);

                g2.setClip(old);
                g2.setColor(new Color(203, 213, 225, 120));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(clip);
                return;
            }

            // Pixel-level transformation filters (grayscale, sepia, vivid, contrast, invert, blur, or original)
            Shape clip = new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, 8, 8);
            Shape old = g2.getClip();
            g2.clip(clip);
            if (!"original".equals(filter)) {
                BufferedImage filtered = getOrCreateFilteredImage(rawImg, rect.width, rect.height, filter);
                if (filtered != null) {
                    g2.drawImage(filtered, rect.x, rect.y, rect.width, rect.height, null);
                } else {
                    g2.drawImage(rawImg, rect.x, rect.y, rect.width, rect.height, null);
                }
            } else {
                g2.drawImage(rawImg, rect.x, rect.y, rect.width, rect.height, null);
            }
            g2.setClip(old);
            g2.setColor(new Color(203, 213, 225, 120));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(clip);
        }

        private static BufferedImage getOrCreateFilteredImage(Image raw, int w, int h, String filter) {
            if (raw == null || w <= 0 || h <= 0) return null;
            String key = System.identityHashCode(raw) + "_" + filter + "_" + w + "x" + h;
            BufferedImage cached = FILTER_CACHE.get(key);
            if (cached != null) return cached;

            BufferedImage base = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = base.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bg.drawImage(raw, 0, 0, w, h, null);
            bg.dispose();

            BufferedImage res;
            if ("grayscale".equals(filter)) {
                res = applyGrayscale(base);
            } else if ("sepia".equals(filter)) {
                res = applySepia(base);
            } else if ("vivid".equals(filter)) {
                res = applyVivid(base);
            } else if ("contrast".equals(filter)) {
                res = applyContrast(base);
            } else if ("invert".equals(filter)) {
                res = applyInvert(base);
            } else if ("blur".equals(filter)) {
                res = applyBlur(base);
            } else {
                res = base;
            }

            FILTER_CACHE.put(key, res);
            if (FILTER_CACHE.size() > 50) FILTER_CACHE.clear();
            return res;
        }

        private static BufferedImage applyGrayscale(BufferedImage src) {
            int w = src.getWidth();
            int h = src.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = src.getRGB(x, y);
                    int a = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    out.setRGB(x, y, (a << 24) | (gray << 16) | (gray << 8) | gray);
                }
            }
            return out;
        }

        private static BufferedImage applySepia(BufferedImage src) {
            int w = src.getWidth();
            int h = src.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = src.getRGB(x, y);
                    int a = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    int tr = Math.min(255, (int) (0.393 * r + 0.769 * g + 0.189 * b));
                    int tg = Math.min(255, (int) (0.349 * r + 0.686 * g + 0.168 * b));
                    int tb = Math.min(255, (int) (0.272 * r + 0.534 * g + 0.131 * b));
                    out.setRGB(x, y, (a << 24) | (tr << 16) | (tg << 8) | tb);
                }
            }
            return out;
        }

        private static BufferedImage applyVivid(BufferedImage src) {
            int w = src.getWidth();
            int h = src.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            float[] hsb = new float[3];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = src.getRGB(x, y);
                    int a = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    Color.RGBtoHSB(r, g, b, hsb);
                    hsb[1] = Math.min(1.0f, hsb[1] * 1.6f);
                    hsb[2] = Math.min(1.0f, hsb[2] * 1.05f);
                    int vivid = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                    out.setRGB(x, y, (a << 24) | (vivid & 0x00ffffff));
                }
            }
            return out;
        }

        private static BufferedImage applyContrast(BufferedImage src) {
            int w = src.getWidth();
            int h = src.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            float factor = 1.45f;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = src.getRGB(x, y);
                    int a = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    int cr = Math.min(255, Math.max(0, (int) ((r - 128) * factor + 128)));
                    int cg = Math.min(255, Math.max(0, (int) ((g - 128) * factor + 128)));
                    int cb = Math.min(255, Math.max(0, (int) ((b - 128) * factor + 128)));
                    out.setRGB(x, y, (a << 24) | (cr << 16) | (cg << 8) | cb);
                }
            }
            return out;
        }

        private static BufferedImage applyInvert(BufferedImage src) {
            int w = src.getWidth();
            int h = src.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = src.getRGB(x, y);
                    int a = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    out.setRGB(x, y, (a << 24) | ((255 - r) << 16) | ((255 - g) << 8) | (255 - b));
                }
            }
            return out;
        }

        private static BufferedImage applyBlur(BufferedImage src) {
            float[] matrix = {
                1f/16f, 2f/16f, 1f/16f,
                2f/16f, 4f/16f, 2f/16f,
                1f/16f, 2f/16f, 1f/16f
            };
            BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, matrix), ConvolveOp.EDGE_NO_OP, null);
            return op.filter(src, null);
        }
    }
}
