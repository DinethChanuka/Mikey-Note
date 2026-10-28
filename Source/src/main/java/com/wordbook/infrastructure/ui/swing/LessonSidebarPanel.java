package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.dto.BookWithLessonsDto;
import com.wordbook.core.application.dto.LessonCreateRequest;
import com.wordbook.core.application.dto.LessonDto;
import com.wordbook.core.application.dto.LessonUpdateRequest;
import com.wordbook.core.application.service.BookService;
import com.wordbook.core.application.service.LessonService;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Premium Left Sidebar Panel displaying Notebook navigation, search & filter,
 * lesson cards with word counts, selection indicators, and management tools.
 */
public class LessonSidebarPanel extends JPanel {
    private final BookService bookService;
    private final LessonService lessonService;
    private final Consumer<String> onSelectLesson;
    private final Consumer<String> onDeleteLesson;
    private final Runnable onBackToShelf;

    private String currentBookId;
    private String activeLessonId;
    private String renamingLessonId = null;
    private List<LessonDto> cachedLessons = List.of();
    private JLabel bookTitleLabel;
    private JTextField filterField;
    private JPanel lessonsListPanel;
    private JPanel inlineCreatorPanel;
    private JTextField newLessonField;

    public LessonSidebarPanel(BookService bookService, LessonService lessonService,
                              Consumer<String> onSelectLesson, Consumer<String> onDeleteLesson, Runnable onBackToShelf) {
        this.bookService = bookService;
        this.lessonService = lessonService;
        this.onSelectLesson = onSelectLesson;
        this.onDeleteLesson = onDeleteLesson;
        this.onBackToShelf = onBackToShelf;

        setPreferredSize(new Dimension(264, 600));
        setMinimumSize(new Dimension(190, 160));
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(12, 12, 8, 12));

        // Top Navigation row
        JPanel navRow = new JPanel(new BorderLayout(8, 0));
        navRow.setOpaque(false);

        JButton backBtn = new JButton("Bookshelf", VectorIcons.create(VectorIcons.Type.BACK, 12, new Color(71, 85, 105)));
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        backBtn.setBackground(Color.WHITE);
        backBtn.addActionListener(e -> onBackToShelf.run());
        navRow.add(backBtn, BorderLayout.WEST);

        bookTitleLabel = new JLabel("Notebook", VectorIcons.create(VectorIcons.Type.BOOK, 15, new Color(37, 99, 235)), SwingConstants.RIGHT);
        bookTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bookTitleLabel.setForeground(new Color(30, 41, 59));
        navRow.add(bookTitleLabel, BorderLayout.CENTER);

        header.add(navRow);
        header.add(Box.createVerticalStrut(10));

        // New Lesson Hero Button
        JButton addLessonBtn = new JButton("New Lesson", VectorIcons.create(VectorIcons.Type.PLUS, 13, Color.WHITE));
        addLessonBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addLessonBtn.setBackground(new Color(37, 99, 235));
        addLessonBtn.setForeground(Color.WHITE);
        addLessonBtn.setOpaque(true);
        addLessonBtn.setBorderPainted(false);
        addLessonBtn.setFocusPainted(false);
        addLessonBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addLessonBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        addLessonBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addLessonBtn.addActionListener(e -> promptNewLesson());
        header.add(addLessonBtn);
        header.add(Box.createVerticalStrut(8));

        // Inline Lesson Creator Bar (No separate modal window)
        inlineCreatorPanel = new JPanel(new BorderLayout(6, 0));
        inlineCreatorPanel.setOpaque(false);
        inlineCreatorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true),
                new EmptyBorder(4, 6, 4, 6)
        ));
        inlineCreatorPanel.setPreferredSize(new Dimension(180, 36));
        inlineCreatorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        inlineCreatorPanel.setVisible(false);

        newLessonField = new JTextField();
        newLessonField.putClientProperty("JTextField.placeholderText", "New lesson title...");
        newLessonField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        newLessonField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(3, 6, 3, 6)
        ));
        newLessonField.addActionListener(e -> submitInlineNewLesson());
        newLessonField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    cancelInlineNewLesson();
                }
            }
        });
        inlineCreatorPanel.add(newLessonField, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnRow.setOpaque(false);

        JButton createBtn = new JButton("Add");
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        createBtn.setBackground(new Color(37, 99, 235));
        createBtn.setForeground(Color.WHITE);
        createBtn.setOpaque(true);
        createBtn.setBorderPainted(false);
        createBtn.setFocusPainted(false);
        createBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createBtn.addActionListener(e -> submitInlineNewLesson());
        btnRow.add(createBtn);

        JButton cancelBtn = new JButton("✕");
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cancelBtn.setForeground(new Color(100, 116, 139));
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> cancelInlineNewLesson());
        btnRow.add(cancelBtn);

        inlineCreatorPanel.add(btnRow, BorderLayout.EAST);
        header.add(inlineCreatorPanel);
        header.add(Box.createVerticalStrut(6));

        // Search / Filter Input
        JPanel searchContainer = new JPanel(new BorderLayout(4, 0));
        searchContainer.setOpaque(false);
        searchContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel searchIcon = new JLabel(VectorIcons.create(VectorIcons.Type.FIND, 12, new Color(148, 163, 184)));
        searchContainer.add(searchIcon, BorderLayout.WEST);

        filterField = new JTextField();
        filterField.putClientProperty("JTextField.placeholderText", "Filter lessons...");
        filterField.setToolTipText("Search lessons in this notebook");
        filterField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        searchContainer.add(filterField, BorderLayout.CENTER);

        header.add(searchContainer);
        add(header, BorderLayout.NORTH);

        // Lessons List Container
        lessonsListPanel = new JPanel();
        lessonsListPanel.setLayout(new BoxLayout(lessonsListPanel, BoxLayout.Y_AXIS));
        lessonsListPanel.setBorder(new EmptyBorder(6, 10, 8, 10));

        JScrollPane scrollPane = new JScrollPane(lessonsListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Theme Support
        applyTheme(ThemeManager.getInstance().getCurrentTheme());
        ThemeManager.getInstance().addListener(this::applyTheme);
    }

    public void applyTheme(ThemeManager.Theme theme) {
        setBackground(theme.surface());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, theme.border()));
        if (getComponentCount() > 0 && getComponent(0) instanceof JPanel hdr) {
            hdr.setBackground(theme.isDark() ? new Color(24, 33, 47) : new Color(248, 250, 252));
        }
        bookTitleLabel.setForeground(theme.accent());
        lessonsListPanel.setBackground(theme.surface());
        refreshLessons();
    }

    public void loadBook(String bookId) {
        this.currentBookId = bookId;
        refreshLessons();
    }

    public void refreshLessons() {
        if (currentBookId == null) return;
        try {
            BookWithLessonsDto bookData = bookService.getBookWithLessons(currentBookId);
            bookTitleLabel.setText(bookData.title());
            this.cachedLessons = bookData.lessons();
        } catch (Exception ignored) {}
        applyFilter();
    }

    private void applyFilter() {
        if (currentBookId == null) return;
        lessonsListPanel.removeAll();

        String query = filterField != null ? filterField.getText().trim().toLowerCase() : "";

        for (LessonDto l : cachedLessons) {
            if (!query.isEmpty() && !l.title().toLowerCase().contains(query)) {
                continue;
            }
            lessonsListPanel.add(createLessonItem(l));
            lessonsListPanel.add(Box.createVerticalStrut(6));
        }

        lessonsListPanel.revalidate();
        lessonsListPanel.repaint();
    }

    public void setActiveLesson(String lessonId) {
        this.activeLessonId = lessonId;
        refreshLessons();
    }

    private JPanel createLessonItem(LessonDto lesson) {
        boolean isSelected = lesson.id().equals(activeLessonId);
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();

        // In-line Renaming Item
        if (lesson.id().equals(renamingLessonId)) {
            JPanel editItem = new JPanel(new BorderLayout(6, 0));
            editItem.setOpaque(false);
            editItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            editItem.setPreferredSize(new Dimension(180, 44));
            editItem.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(59, 130, 246), 1, true),
                    new EmptyBorder(4, 8, 4, 6)
            ));

            JTextField renameField = new JTextField(lesson.title());
            renameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            renameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                    new EmptyBorder(2, 6, 2, 6)
            ));

            Runnable saveRename = () -> {
                String newTitle = renameField.getText().trim();
                if (!newTitle.isEmpty() && !newTitle.equals(lesson.title())) {
                    lessonService.updateLesson(lesson.id(), new LessonUpdateRequest(newTitle, null, null, null, null));
                }
                renamingLessonId = null;
                refreshLessons();
            };
            Runnable cancelRename = () -> {
                renamingLessonId = null;
                refreshLessons();
            };

            renameField.addActionListener(e -> saveRename.run());
            renameField.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        cancelRename.run();
                    }
                }
            });
            editItem.add(renameField, BorderLayout.CENTER);

            JPanel actPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 4));
            actPanel.setOpaque(false);

            JButton okBtn = new JButton("✓");
            okBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            okBtn.setForeground(new Color(22, 163, 74));
            okBtn.setContentAreaFilled(false);
            okBtn.setBorderPainted(false);
            okBtn.setFocusPainted(false);
            okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            okBtn.setToolTipText("Save name (Enter)");
            okBtn.addActionListener(e -> saveRename.run());
            actPanel.add(okBtn);

            JButton noBtn = new JButton("✕");
            noBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            noBtn.setForeground(new Color(239, 68, 68));
            noBtn.setContentAreaFilled(false);
            noBtn.setBorderPainted(false);
            noBtn.setFocusPainted(false);
            noBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            noBtn.setToolTipText("Cancel (Esc)");
            noBtn.addActionListener(e -> cancelRename.run());
            actPanel.add(noBtn);

            editItem.add(actPanel, BorderLayout.EAST);

            SwingUtilities.invokeLater(() -> {
                renameField.requestFocusInWindow();
                renameField.selectAll();
            });
            return editItem;
        }

        JPanel item = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Background
                Color bg = isSelected
                        ? (theme.isDark() ? new Color(37, 99, 235, 55) : new Color(239, 246, 255))
                        : (getBackground());
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Border
                if (isSelected) {
                    g2.setColor(new Color(147, 197, 253));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                    // Left active accent bar
                    g2.setColor(theme.accent());
                    g2.fillRoundRect(0, 4, 4, h - 8, 4, 4);
                } else {
                    g2.setColor(theme.isDark() ? new Color(51, 65, 85) : new Color(241, 245, 249));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        item.setOpaque(false);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setPreferredSize(new Dimension(180, 44));
        item.setBorder(new EmptyBorder(4, 12, 4, 8));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Center: Document Icon, Title & Word count subtitle
        JPanel contentPanel = new JPanel(new BorderLayout(6, 0));
        contentPanel.setOpaque(false);

        JLabel docIcon = new JLabel(VectorIcons.create(
                VectorIcons.Type.DOCUMENT, 16, isSelected ? theme.accent() : theme.textSecondary()));
        contentPanel.add(docIcon, BorderLayout.WEST);

        JPanel textStack = new JPanel(new GridLayout(2, 1, 0, 1));
        textStack.setOpaque(false);

        JLabel titleLbl = new JLabel(lesson.title());
        titleLbl.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 13));
        titleLbl.setForeground(isSelected ? theme.accent() : theme.textPrimary());
        titleLbl.setToolTipText(lesson.title());

        // Live Word / Char count badge
        int words = lesson.wordCount();
        String snippet = words > 0 ? (words + (words == 1 ? " word" : " words")) : (lesson.charCount() > 0 ? lesson.charCount() + " chars" : "Draft");
        JLabel subLbl = new JLabel(snippet);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLbl.setForeground(ThemeManager.getTextSecondary());

        textStack.add(titleLbl);
        textStack.add(subLbl);
        contentPanel.add(textStack, BorderLayout.CENTER);

        item.add(contentPanel, BorderLayout.CENTER);

        // Right: Edit & Delete buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 6));
        actions.setOpaque(false);

        JButton renBtn = new JButton(VectorIcons.create(VectorIcons.Type.EDIT, 12, theme.textSecondary()));
        renBtn.setPreferredSize(new Dimension(24, 24));
        renBtn.setMargin(new Insets(1, 1, 1, 1));
        renBtn.setToolTipText("Rename lesson inline");
        renBtn.setFocusPainted(false);
        renBtn.setContentAreaFilled(false);
        renBtn.setBorderPainted(false);
        renBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        renBtn.addActionListener(e -> promptRename(lesson));
        actions.add(renBtn);

        JButton delBtn = new JButton(VectorIcons.create(VectorIcons.Type.DELETE, 12, new Color(239, 68, 68)));
        delBtn.setPreferredSize(new Dimension(24, 24));
        delBtn.setMargin(new Insets(1, 1, 1, 1));
        delBtn.setToolTipText("Delete lesson");
        delBtn.setFocusPainted(false);
        delBtn.setContentAreaFilled(false);
        delBtn.setBorderPainted(false);
        delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delBtn.addActionListener(e -> {
            boolean confirmed = ModernDialog.showConfirm(
                    this,
                    "Delete Lesson",
                    "Are you sure you want to delete lesson '" + lesson.title() + "'? This action cannot be undone.",
                    ModernDialog.MessageType.WARNING
            );
            if (confirmed) {
                if (onDeleteLesson != null) {
                    onDeleteLesson.accept(lesson.id());
                } else {
                    lessonService.deleteLesson(lesson.id());
                    refreshLessons();
                }
            }
        });
        actions.add(delBtn);

        item.add(actions, BorderLayout.EAST);

        // Hover effect & Click
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    item.setBackground(theme.isDark() ? new Color(51, 65, 85, 120) : new Color(241, 245, 249));
                    item.repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    item.setBackground(theme.surface());
                    item.repaint();
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!actions.getBounds().contains(e.getPoint())) {
                    activeLessonId = lesson.id();
                    onSelectLesson.accept(lesson.id());
                    refreshLessons();
                }
            }
        });

        return item;
    }

    public void promptNewLesson() {
        if (currentBookId == null) return;
        boolean visible = !inlineCreatorPanel.isVisible();
        inlineCreatorPanel.setVisible(visible);
        if (visible) {
            newLessonField.setText("");
            newLessonField.requestFocusInWindow();
        }
        revalidate();
        repaint();
    }

    private void submitInlineNewLesson() {
        if (currentBookId == null) return;
        String title = newLessonField.getText().trim();
        if (title.isEmpty()) {
            title = "Untitled Lesson";
        }
        var created = lessonService.createLesson(currentBookId, new LessonCreateRequest(null, title, null, null));
        inlineCreatorPanel.setVisible(false);
        newLessonField.setText("");
        activeLessonId = created.id();
        refreshLessons();
        onSelectLesson.accept(created.id());
    }

    private void cancelInlineNewLesson() {
        inlineCreatorPanel.setVisible(false);
        newLessonField.setText("");
        revalidate();
        repaint();
    }

    private void promptRename(LessonDto lesson) {
        this.renamingLessonId = lesson.id();
        refreshLessons();
    }
}
