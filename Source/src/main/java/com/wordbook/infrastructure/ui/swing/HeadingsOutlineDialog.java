package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Smart Accessible Headings & Outline Navigator Dialog.
 * Allows quick keyboard-friendly jumping through document structure (H1, H2, H3),
 * reading section titles, and navigating complex documents effortlessly.
 */
public class HeadingsOutlineDialog extends JDialog {

    public record HeadingEntry(String tag, String text, int offset) {
        @Override
        public String toString() {
            String indent = switch (tag.toLowerCase()) {
                case "h2" -> "   • ";
                case "h3" -> "      - ";
                default   -> "▶ ";
            };
            return indent + text + "  [" + tag.toUpperCase() + "]";
        }
    }

    private final JTextPane textPane;
    private final DefaultListModel<HeadingEntry> listModel = new DefaultListModel<>();
    private final JList<HeadingEntry> headingsList;
    private final JTextField searchField;

    public HeadingsOutlineDialog(Frame owner, JTextPane textPane) {
        super(owner, "Document Outline & Headings Navigator (Accessibility)", false);
        this.textPane = textPane;
        setSize(440, 480);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(14, 16, 14, 16));
        root.setBackground(ThemeManager.getSurface());

        // Header + Search
        JPanel topBox = new JPanel(new BorderLayout(6, 6));
        topBox.setOpaque(false);

        JLabel titleLbl = new JLabel("📑 Document Headings & Section Navigator");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(ThemeManager.getTextPrimary());
        topBox.add(titleLbl, BorderLayout.NORTH);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchField.putClientProperty("JTextField.placeholderText", "Filter headings (type to search)...");
        searchField.getDocument().addDocumentListener(new SimpleDocumentListener(this::filterHeadings));
        topBox.add(searchField, BorderLayout.CENTER);

        root.add(topBox, BorderLayout.NORTH);

        // Headings List
        headingsList = new JList<>(listModel);
        headingsList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        headingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        headingsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(6, 10, 6, 10));
                if (value instanceof HeadingEntry he) {
                    if ("h1".equalsIgnoreCase(he.tag)) {
                        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                        if (!isSelected) l.setForeground(new Color(37, 99, 235));
                    } else if ("h2".equalsIgnoreCase(he.tag)) {
                        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        if (!isSelected) l.setForeground(new Color(51, 65, 85));
                    } else {
                        l.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                        if (!isSelected) l.setForeground(new Color(100, 116, 139));
                    }
                }
                return l;
            }
        });

        // Double click or Enter to jump
        headingsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) jumpToSelectedHeading();
            }
        });
        headingsList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) jumpToSelectedHeading();
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) dispose();
            }
        });

        JScrollPane scroll = new JScrollPane(headingsList);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorder()));
        root.add(scroll, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bottomBar.setOpaque(false);

        JButton closeBtn = new JButton("Close (Esc)");
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn);

        JButton jumpBtn = new JButton("Jump to Section ➔", VectorIcons.createIcon(VectorIcons.DOCUMENT, 13, Color.WHITE));
        jumpBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        jumpBtn.setBackground(new Color(37, 99, 235));
        jumpBtn.setForeground(Color.WHITE);
        jumpBtn.setOpaque(true);
        jumpBtn.setBorderPainted(false);
        jumpBtn.addActionListener(e -> jumpToSelectedHeading());
        bottomBar.add(jumpBtn);

        root.add(bottomBar, BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);

        scanHeadings();
    }

    private final List<HeadingEntry> allHeadings = new ArrayList<>();

    private void scanHeadings() {
        allHeadings.clear();
        listModel.clear();
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();

        collectHeadingsRecursive(doc.getDefaultRootElement(), doc);

        if (allHeadings.isEmpty()) {
            setTitle("Document Outline — No headings detected");
        } else {
            setTitle("Document Outline — " + allHeadings.size() + " sections");
            for (HeadingEntry he : allHeadings) {
                listModel.addElement(he);
            }
            headingsList.setSelectedIndex(0);
        }
    }

    private void collectHeadingsRecursive(Element elem, HTMLDocument doc) {
        if (elem == null) return;
        String name = elem.getName();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.equals("h1") || lower.equals("h2") || lower.equals("h3")) {
                try {
                    int start = elem.getStartOffset();
                    int end = elem.getEndOffset();
                    String text = doc.getText(start, Math.max(0, end - start)).trim();
                    if (!text.isEmpty()) {
                        allHeadings.add(new HeadingEntry(lower, text, start));
                    }
                } catch (Exception ignored) {}
            }
        }
        for (int i = 0; i < elem.getElementCount(); i++) {
            collectHeadingsRecursive(elem.getElement(i), doc);
        }
    }

    private void filterHeadings() {
        String query = searchField.getText().trim().toLowerCase();
        listModel.clear();
        for (HeadingEntry he : allHeadings) {
            if (query.isEmpty() || he.text.toLowerCase().contains(query)) {
                listModel.addElement(he);
            }
        }
        if (!listModel.isEmpty()) {
            headingsList.setSelectedIndex(0);
        }
    }

    private void jumpToSelectedHeading() {
        HeadingEntry sel = headingsList.getSelectedValue();
        if (sel == null) return;

        textPane.requestFocusInWindow();
        textPane.setCaretPosition(Math.min(textPane.getDocument().getLength(), sel.offset));
        try {
            Rectangle r = textPane.modelToView(sel.offset);
            if (r != null) {
                textPane.scrollRectToVisible(r);
            }
        } catch (Exception ignored) {}
        dispose();
    }
}
