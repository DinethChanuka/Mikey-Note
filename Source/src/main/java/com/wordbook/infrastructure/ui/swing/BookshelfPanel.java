package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.dto.BookCreateRequest;
import com.wordbook.core.application.dto.BookDto;
import com.wordbook.core.application.service.BookService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Swing Bookshelf panel rendering 3D colored notebook cards
 * with 55+ vector cover designs (watermarks), custom color picker integration,
 * quality vector icons, and no emojis or database driver branding.
 */
public class BookshelfPanel extends JPanel implements ThemeManager.ThemeChangeListener {

    private final BookService bookService;
    private final Consumer<String> onOpenBook;
    private final Runnable onOpenSettings;

    private JPanel topBar;
    private JLabel titleLbl;
    private JPanel shelfGrid;
    private JScrollPane scrollPane;

    public BookshelfPanel(BookService bookService, Consumer<String> onOpenBook, Runnable onOpenSettings) {
        this.bookService = bookService;
        this.onOpenBook = onOpenBook;
        this.onOpenSettings = onOpenSettings;

        setLayout(new BorderLayout());
        setBackground(ThemeManager.getShelfBackground());
        ThemeManager.addListener(this);

        // Top Toolbar (White/Light default, clean vector icons)
        topBar = new JPanel(new BorderLayout());
        topBar.setBackground(ThemeManager.getSurface());
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorder()),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JPanel brandPnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPnl.setOpaque(false);
        JLabel iconLbl = new JLabel(VectorIcons.create(VectorIcons.Type.BOOK, 22, ThemeManager.getAccent()));
        titleLbl = new JLabel("Mikey Note — Notebooks");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 17));
        titleLbl.setForeground(ThemeManager.getTextPrimary());
        brandPnl.add(iconLbl);
        brandPnl.add(titleLbl);
        topBar.add(brandPnl, BorderLayout.WEST);

        // Actions: Settings & New Notebook (No Emojis, No SQLite mentioned)
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        JButton settingsBtn = new JButton("Preferences");
        settingsBtn.setIcon(VectorIcons.create(VectorIcons.Type.SETTINGS, 15, ThemeManager.getTextSecondary()));
        settingsBtn.addActionListener(e -> onOpenSettings.run());
        actionBtns.add(settingsBtn);

        JButton newBookBtn = new JButton("New Notebook");
        newBookBtn.setIcon(VectorIcons.create(VectorIcons.Type.PLUS, 15, Color.WHITE));
        newBookBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        newBookBtn.setBackground(ThemeManager.getAccent());
        newBookBtn.setForeground(Color.WHITE);
        newBookBtn.addActionListener(e -> promptNewBook());
        actionBtns.add(newBookBtn);

        topBar.add(actionBtns, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Scrollable Shelf Grid
        shelfGrid = new ResponsiveShelfPanel(new FlowLayout(FlowLayout.LEFT, 24, 24));
        shelfGrid.setBackground(ThemeManager.getShelfBackground());
        shelfGrid.setBorder(new EmptyBorder(24, 28, 24, 28));

        scrollPane = new JScrollPane(shelfGrid);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        add(scrollPane, BorderLayout.CENTER);

        refreshBooks();
    }

    public void refreshBooks() {
        shelfGrid.removeAll();
        List<BookDto> books = bookService.getAllBooks();

        for (BookDto b : books) {
            shelfGrid.add(createBookCard(b));
        }

        // "New Notebook" Card
        JPanel newCard = createNewBookPlaceholderCard();
        shelfGrid.add(newCard);

        shelfGrid.revalidate();
        shelfGrid.repaint();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        setBackground(ThemeManager.getShelfBackground());
        topBar.setBackground(ThemeManager.getSurface());
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorder()),
                new EmptyBorder(12, 20, 12, 20)
        ));
        titleLbl.setForeground(ThemeManager.getTextPrimary());
        shelfGrid.setBackground(ThemeManager.getShelfBackground());
        refreshBooks();
    }

    private JPanel createBookCard(BookDto b) {
        // Parse color, cover design, and subtopic
        String raw = b.color();
        String colorHex = "#2563eb";
        String designId = "NONE";
        String subtopic = "";

        if (raw != null) {
            String[] parts = raw.split("\\|", -1);
            colorHex = parts[0].trim();
            if (parts.length > 1) {
                designId = parts[1].trim();
            }
            if (parts.length > 2) {
                subtopic = parts[2].trim();
            }
        }

        Color coverColor;
        try {
            coverColor = Color.decode(colorHex);
        } catch (Exception e) {
            coverColor = new Color(37, 99, 235);
        }

        final CoverDesign coverDesign = CoverDesign.fromId(designId);
        final Color finalCoverColor = coverColor;

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 10;
                int h = getHeight() - 10;

                // 1. Realistic Multi-pass Drop Shadow
                g2.setColor(new Color(0, 0, 0, 36));
                g2.fillRoundRect(5, 7, w, h, 14, 14);

                // 2. 3D Page Depth (Cream stacked pages on right edge)
                g2.setColor(new Color(245, 243, 237));
                g2.fillRoundRect(w - 5, 4, 8, h - 8, 6, 6);
                g2.setColor(new Color(214, 211, 201));
                for (int ly = 12; ly < h - 12; ly += 6) {
                    g2.drawLine(w - 3, ly, w - 1, ly);
                }

                // 3. Base Colored Cover
                g2.setColor(finalCoverColor);
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // 4. Render Vector Cover Design / Watermark centered on front cover
                int spineW = 16;
                int frontW = w - spineW;
                int frontH = h - 68;
                if (frontH > 40) {
                    Graphics2D gDesign = (Graphics2D) g2.create();
                    gDesign.clip(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
                    double scale = Math.min((frontW - 12) / CoverDesign.REF_WIDTH, (frontH - 12) / CoverDesign.REF_HEIGHT) * 0.86;
                    double drawW = CoverDesign.REF_WIDTH * scale;
                    double drawH = CoverDesign.REF_HEIGHT * scale;
                    double drawX = spineW + (frontW - drawW) / 2.0;
                    double drawY = 8 + (frontH - drawH) / 2.0;
                    gDesign.translate(drawX, drawY);
                    gDesign.scale(scale, scale);
                    coverDesign.render(gDesign, (int)CoverDesign.REF_WIDTH, (int)CoverDesign.REF_HEIGHT, finalCoverColor);
                    gDesign.dispose();
                }

                // 5. Glossy Highlight & Spine Lighting
                GradientPaint gloss = new GradientPaint(
                        0, 0, new Color(255, 255, 255, 45),
                        w, h, new Color(0, 0, 0, 45)
                );
                g2.setPaint(gloss);
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // 6. Realistic Leather Spine with Stitching
                GradientPaint spine = new GradientPaint(
                        0, 0, new Color(0, 0, 0, 70),
                        14, 0, new Color(255, 255, 255, 20)
                );
                g2.setPaint(spine);
                g2.fillRoundRect(0, 0, 14, h, 12, 12);

                // Stitch Line
                g2.setColor(new Color(255, 255, 255, 55));
                Stroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f);
                g2.setStroke(dashed);
                g2.drawLine(15, 4, 15, h - 4);

                // 7. Gold Bookmark Ribbon
                int rw = 16, rh = 28, rx = w - 32;
                g2.setColor(new Color(245, 158, 11));
                Polygon ribbon = new Polygon();
                ribbon.addPoint(rx, 0);
                ribbon.addPoint(rx + rw, 0);
                ribbon.addPoint(rx + rw, rh);
                ribbon.addPoint(rx + rw / 2, rh - 6);
                ribbon.addPoint(rx, rh);
                g2.fillPolygon(ribbon);

                g2.dispose();
            }
        };

        card.setPreferredSize(new Dimension(185, 245));
        card.setLayout(new BorderLayout(8, 8));
        card.setBorder(new EmptyBorder(12, 22, 16, 14));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top button bar (Customize Design & Delete)
        JPanel topPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        topPnl.setOpaque(false);

        JButton editBtn = new JButton();
        editBtn.setIcon(VectorIcons.create(VectorIcons.Type.PALETTE, 12, Color.WHITE));
        editBtn.setToolTipText("Customize Cover Design & Color");
        editBtn.setBackground(new Color(0, 0, 0, 110));
        editBtn.setBorder(null);
        editBtn.setPreferredSize(new Dimension(24, 22));
        editBtn.setFocusPainted(false);
        editBtn.addActionListener(e -> promptCustomizeCover(b));
        topPnl.add(editBtn);

        JButton delBtn = new JButton();
        delBtn.setIcon(VectorIcons.create(VectorIcons.Type.DELETE, 12, Color.WHITE));
        delBtn.setToolTipText("Delete Notebook");
        delBtn.setBackground(new Color(0, 0, 0, 110));
        delBtn.setBorder(null);
        delBtn.setPreferredSize(new Dimension(24, 22));
        delBtn.setFocusPainted(false);
        delBtn.addActionListener(e -> {
            boolean confirm = ModernDialog.showConfirm(
                    this,
                    "Confirm Delete",
                    "Delete notebook '" + b.title() + "' and all its lessons? This action cannot be undone.",
                    ModernDialog.MessageType.WARNING
            );
            if (confirm) {
                bookService.deleteBook(b.id());
                refreshBooks();
            }
        });
        topPnl.add(delBtn);

        card.add(topPnl, BorderLayout.NORTH);

        // Title, Subtopic and Lesson Count bottom with dynamic auto-adjust font size
        JPanel textPnl = new JPanel();
        textPnl.setLayout(new BoxLayout(textPnl, BoxLayout.Y_AXIS));
        textPnl.setOpaque(false);

        int titleFontSize = b.title().length() > 20 ? 12 : 14;
        JLabel titleLabel = new JLabel("<html><body style='width: 135px; font-family: sans-serif; font-size: "
                + titleFontSize + "px; font-weight: bold; line-height: 1.15; color: #ffffff;'>"
                + escapeHtml(b.title()) + "</body></html>");
        textPnl.add(titleLabel);

        if (!subtopic.isEmpty()) {
            textPnl.add(Box.createVerticalStrut(2));
            JLabel subtopicLabel = new JLabel("<html><body style='width: 135px; font-family: sans-serif; font-size: 10px; font-style: italic; color: #fef08a;'>"
                    + escapeHtml(subtopic) + "</body></html>");
            textPnl.add(subtopicLabel);
        }

        textPnl.add(Box.createVerticalStrut(3));
        JLabel countLbl = new JLabel(b.lessonCount() + (b.lessonCount() == 1 ? " lesson" : " lessons"));
        countLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        countLbl.setForeground(new Color(255, 255, 255, 210));
        countLbl.setIcon(VectorIcons.create(VectorIcons.Type.DOCUMENT, 11, new Color(255, 255, 255, 200)));
        textPnl.add(countLbl);

        card.add(textPnl, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!topPnl.getBounds().contains(e.getPoint())) {
                    onOpenBook.accept(b.id());
                }
            }
        });

        return card;
    }

    private JPanel createNewBookPlaceholderCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 8;
                int h = getHeight() - 8;

                g2.setColor(ThemeManager.getSurface());
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // Dashed border with accent color
                g2.setColor(ThemeManager.getAccent());
                Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0);
                g2.setStroke(dashed);
                g2.drawRoundRect(2, 2, w - 4, h - 4, 10, 10);
                g2.dispose();
            }
        };

        card.setPreferredSize(new Dimension(185, 245));
        card.setLayout(new GridBagLayout());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel plusIcon = new JLabel(VectorIcons.create(VectorIcons.Type.PLUS, 32, ThemeManager.getAccent()));
        plusIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel("New Notebook");
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(ThemeManager.getAccent());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(plusIcon);
        inner.add(Box.createVerticalStrut(8));
        inner.add(label);
        card.add(inner);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                promptNewBook();
            }
        });

        return card;
    }

    private void promptNewBook() {
        NotebookModalDialog.showModal(
                SwingUtilities.getWindowAncestor(this),
                "Create New Notebook",
                null,
                null,
                null,
                (title, colorPayload) -> {
                    var created = bookService.createBook(new BookCreateRequest(null, title, colorPayload));
                    refreshBooks();
                    onOpenBook.accept(created.id());
                }
        );
    }

    private void promptCustomizeCover(BookDto b) {
        String raw = b.color();
        String colorHex = "#2563eb";
        String designId = "NONE";
        String subtopic = "";
        if (raw != null) {
            String[] p = raw.split("\\|", -1);
            colorHex = p[0].trim();
            if (p.length > 1) designId = p[1].trim();
            if (p.length > 2) subtopic = p[2].trim();
        }

        NotebookModalDialog.showModal(
                SwingUtilities.getWindowAncestor(this),
                "Customize Cover: " + b.title(),
                b.title(),
                subtopic,
                colorHex,
                designId,
                (title, colorPayload) -> {
                    bookService.updateBook(b.id(), new com.wordbook.core.application.dto.BookUpdateRequest(title, colorPayload));
                    refreshBooks();
                }
        );
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static class ResponsiveShelfPanel extends JPanel implements Scrollable {
        public ResponsiveShelfPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
