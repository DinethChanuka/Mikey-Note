package com.wordbook.infrastructure.ui.swing;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

/**
 * Word-style Find and Replace floating dialog with live match count,
 * forward/backward navigation, case matching, whole-word matching,
 * and high-visibility document highlighting.
 */
public class FindReplaceDialog extends JDialog {

    private final JTextPane textPane;
    private final JTextField findField = new JTextField();
    private final JTextField replaceField = new JTextField();
    private final JCheckBox matchCaseCheck = new JCheckBox("Match case");
    private final JCheckBox wholeWordCheck = new JCheckBox("Match whole word");
    private final JLabel statusLabel = new JLabel("Enter text to find...");
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final Highlighter.HighlightPainter matchPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(254, 240, 138, 180));
    private final Highlighter.HighlightPainter currentPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(251, 191, 36, 230));

    private int lastFoundIndex = -1;

    public FindReplaceDialog(Frame owner, JTextPane textPane, boolean startOnReplace) {
        super(owner, "Find & Replace", false);
        this.textPane = textPane;

        setSize(480, 260);
        setResizable(false);
        setLocationRelativeTo(owner);

        initUI();

        if (startOnReplace) {
            tabbedPane.setSelectedIndex(1);
        }

        // Prepopulate with selected text if any
        String sel = textPane.getSelectedText();
        if (sel != null && !sel.isBlank() && !sel.contains("\n")) {
            findField.setText(sel.trim());
        }

        findField.requestFocusInWindow();
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(new EmptyBorder(12, 14, 12, 14));
        content.setBackground(ThemeManager.getSurface());

        // Header with vector icon
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        JLabel iconLbl = new JLabel(VectorIcons.create(VectorIcons.Type.FIND, 18, new Color(37, 99, 235)));
        JLabel titleLbl = new JLabel("Document Search & Replace");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(ThemeManager.getTextPrimary());
        header.add(iconLbl);
        header.add(titleLbl);
        content.add(header, BorderLayout.NORTH);

        // Tabs: Find vs Replace
        JPanel findPanel = createFindPanel();
        JPanel replacePanel = createReplacePanel();

        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabbedPane.addTab("Find", VectorIcons.create(VectorIcons.Type.FIND, 12, ThemeManager.getTextSecondary()), findPanel);
        tabbedPane.addTab("Replace", VectorIcons.create(VectorIcons.Type.REPLACE, 12, ThemeManager.getTextSecondary()), replacePanel);
        content.add(tabbedPane, BorderLayout.CENTER);

        // Status bar & close
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(ThemeManager.getTextSecondary());
        bottomBar.add(statusLabel, BorderLayout.WEST);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn, BorderLayout.EAST);

        content.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(content);

        // Key listeners
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    findNext(true);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        };
        findField.addKeyListener(enterListener);
        replaceField.addKeyListener(enterListener);
    }

    private JPanel createFindPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 6, 6, 6));

        JPanel fields = new JPanel(new GridLayout(2, 1, 4, 4));
        fields.setOpaque(false);

        JPanel fRow = new JPanel(new BorderLayout(8, 0));
        fRow.setOpaque(false);
        JLabel lbl = new JLabel("Find what:");
        lbl.setPreferredSize(new Dimension(80, 26));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fRow.add(lbl, BorderLayout.WEST);
        fRow.add(findField, BorderLayout.CENTER);
        fields.add(fRow);

        JPanel optRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        optRow.setOpaque(false);
        matchCaseCheck.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        wholeWordCheck.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        optRow.add(matchCaseCheck);
        optRow.add(wholeWordCheck);
        fields.add(optRow);

        p.add(fields, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        btnPanel.setOpaque(false);

        JButton nextBtn = new JButton("Find Next");
        nextBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        nextBtn.addActionListener(e -> findNext(true));

        JButton prevBtn = new JButton("Find Previous");
        prevBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        prevBtn.addActionListener(e -> findNext(false));

        JButton highlightBtn = new JButton("Highlight All");
        highlightBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        highlightBtn.addActionListener(e -> highlightAll());

        btnPanel.add(nextBtn);
        btnPanel.add(prevBtn);
        btnPanel.add(highlightBtn);
        p.add(btnPanel, BorderLayout.EAST);

        return p;
    }

    private JPanel createReplacePanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 6, 6, 6));

        JPanel fields = new JPanel(new GridLayout(3, 1, 4, 4));
        fields.setOpaque(false);

        JPanel fRow = new JPanel(new BorderLayout(8, 0));
        fRow.setOpaque(false);
        JLabel lblFind = new JLabel("Find what:");
        lblFind.setPreferredSize(new Dimension(84, 26));
        lblFind.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fRow.add(lblFind, BorderLayout.WEST);
        // shared findField
        fRow.add(findField, BorderLayout.CENTER);
        fields.add(fRow);

        JPanel rRow = new JPanel(new BorderLayout(8, 0));
        rRow.setOpaque(false);
        JLabel lblRep = new JLabel("Replace with:");
        lblRep.setPreferredSize(new Dimension(84, 26));
        lblRep.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rRow.add(lblRep, BorderLayout.WEST);
        rRow.add(replaceField, BorderLayout.CENTER);
        fields.add(rRow);

        JPanel optRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        optRow.setOpaque(false);
        optRow.add(matchCaseCheck);
        optRow.add(wholeWordCheck);
        fields.add(optRow);

        p.add(fields, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 3, 3));
        btnPanel.setOpaque(false);

        JButton nextBtn = new JButton("Find Next");
        nextBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        nextBtn.addActionListener(e -> findNext(true));

        JButton repBtn = new JButton("Replace");
        repBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        repBtn.addActionListener(e -> replaceCurrent());

        JButton repAllBtn = new JButton("Replace All");
        repAllBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        repAllBtn.addActionListener(e -> replaceAll());

        JButton clearBtn = new JButton("Clear Marks");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.addActionListener(e -> clearHighlights());

        btnPanel.add(nextBtn);
        btnPanel.add(repBtn);
        btnPanel.add(repAllBtn);
        btnPanel.add(clearBtn);
        p.add(btnPanel, BorderLayout.EAST);

        return p;
    }

    private void findNext(boolean forward) {
        String target = findField.getText();
        if (target == null || target.isEmpty()) {
            statusLabel.setText("Please enter search text.");
            return;
        }

        try {
            String text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
            if (text.isEmpty()) {
                statusLabel.setText("Document is empty.");
                return;
            }

            int caret = textPane.getCaretPosition();
            Pattern p = buildPattern(target);
            Matcher m = p.matcher(text);

            int matchStart = -1;
            int matchEnd = -1;

            if (forward) {
                int searchStart = caret;
                if (m.find(searchStart)) {
                    matchStart = m.start();
                    matchEnd = m.end();
                } else if (searchStart > 0 && m.find(0)) { // wrap around
                    matchStart = m.start();
                    matchEnd = m.end();
                    statusLabel.setText("Passed end of document; continuing from top.");
                }
            } else {
                int lastStart = -1;
                int lastEnd = -1;
                while (m.find()) {
                    if (m.start() < caret) {
                        lastStart = m.start();
                        lastEnd = m.end();
                    }
                }
                if (lastStart >= 0) {
                    matchStart = lastStart;
                    matchEnd = lastEnd;
                } else {
                    // wrap around to end
                    m.reset();
                    while (m.find()) {
                        lastStart = m.start();
                        lastEnd = m.end();
                    }
                    if (lastStart >= 0) {
                        matchStart = lastStart;
                        matchEnd = lastEnd;
                        statusLabel.setText("Passed top of document; continuing from bottom.");
                    }
                }
            }

            if (matchStart >= 0) {
                textPane.setSelectionStart(matchStart);
                textPane.setSelectionEnd(matchEnd);
                textPane.requestFocusInWindow();
                lastFoundIndex = matchStart;
                statusLabel.setText("Found match at character " + matchStart);
            } else {
                statusLabel.setText("No matches found for \"" + target + "\".");
            }
        } catch (BadLocationException e) {
            statusLabel.setText("Error searching: " + e.getMessage());
        }
    }

    private void replaceCurrent() {
        String target = findField.getText();
        String replacement = replaceField.getText();
        if (target == null || target.isEmpty()) return;

        String sel = textPane.getSelectedText();
        boolean matches = sel != null && (matchCaseCheck.isSelected() ? sel.equals(target) : sel.equalsIgnoreCase(target));

        if (matches) {
            textPane.replaceSelection(replacement != null ? replacement : "");
            statusLabel.setText("Replaced current occurrence.");
            findNext(true);
        } else {
            findNext(true);
        }
    }

    private void replaceAll() {
        String target = findField.getText();
        String replacement = replaceField.getText();
        if (target == null || target.isEmpty()) return;

        try {
            String text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
            Pattern p = buildPattern(target);
            Matcher m = p.matcher(text);

            int count = 0;
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (m.find()) {
                sb.append(text, lastEnd, m.start());
                sb.append(replacement != null ? replacement : "");
                lastEnd = m.end();
                count++;
            }
            sb.append(text.substring(lastEnd));

            if (count > 0) {
                textPane.setText(sb.toString());
                statusLabel.setText("Replaced " + count + " occurrence(s).");
                clearHighlights();
            } else {
                statusLabel.setText("No occurrences found to replace.");
            }
        } catch (BadLocationException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void highlightAll() {
        clearHighlights();
        String target = findField.getText();
        if (target == null || target.isEmpty()) return;

        try {
            String text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
            Pattern p = buildPattern(target);
            Matcher m = p.matcher(text);
            Highlighter h = textPane.getHighlighter();

            int count = 0;
            while (m.find()) {
                h.addHighlight(m.start(), m.end(), matchPainter);
                count++;
            }
            statusLabel.setText("Highlighted " + count + " match(es).");
        } catch (BadLocationException e) {
            statusLabel.setText("Highlight error: " + e.getMessage());
        }
    }

    private void clearHighlights() {
        Highlighter h = textPane.getHighlighter();
        for (Highlighter.Highlight hl : h.getHighlights()) {
            if (hl.getPainter() == matchPainter || hl.getPainter() == currentPainter) {
                h.removeHighlight(hl);
            }
        }
    }

    private Pattern buildPattern(String query) {
        String regex = Pattern.quote(query);
        if (wholeWordCheck.isSelected()) {
            regex = "\\b" + regex + "\\b";
        }
        int flags = matchCaseCheck.isSelected() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        return Pattern.compile(regex, flags);
    }
}
