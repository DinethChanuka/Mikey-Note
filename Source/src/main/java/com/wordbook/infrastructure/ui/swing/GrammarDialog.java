package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.GrammarEngineService;
import com.wordbook.core.application.service.GrammarEngineService.GrammarIssue;
import com.wordbook.core.application.service.GrammarEngineService.ReadabilityStats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * High-Level Interactive Grammar & Proofreading Studio Dialog.
 * Features real-time readability metrics (Flesch score & Grade level),
 * category pills (All, Spelling, Grammar, Punctuation, Style),
 * suggestion breakdown, and 1-click batch fix.
 */
public class GrammarDialog extends JDialog {
    private final GrammarEngineService grammarEngine;
    private final Runnable onDocumentUpdated;

    private String currentText;
    private List<GrammarIssue> allIssues = new ArrayList<>();
    private DefaultListModel<GrammarIssue> listModel;
    private JList<GrammarIssue> issueList;

    private JLabel ruleLabel;
    private JLabel originalBadge;
    private JLabel suggestionBadge;
    private JTextArea contextArea;
    private JLabel countLabel;

    // Readability scorecard labels
    private JLabel fleschBadge;
    private JLabel gradeBadge;
    private JLabel readingLevelLbl;

    private String activeCategoryFilter = "All";
    private boolean modified = false;

    public GrammarDialog(Frame owner, GrammarEngineService grammarEngine, String initialText, Runnable onDocumentUpdated) {
        super(owner, "Grammar & Proofreading Studio", true);
        this.grammarEngine = grammarEngine;
        this.currentText = initialText != null ? initialText : "";
        this.onDocumentUpdated = onDocumentUpdated;

        setSize(740, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(12, 10));
        main.setBorder(new EmptyBorder(14, 16, 12, 16));
        main.setBackground(ThemeManager.getSurface());

        // Header Panel: Title + Readability Scorecard
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel("Grammar, Spelling & Style Studio", VectorIcons.createIcon(VectorIcons.CHECKMARK, 16, new Color(22, 163, 74)), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleRow.add(titleLabel, BorderLayout.WEST);

        countLabel = new JLabel("Scanning...");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        countLabel.setForeground(new Color(220, 38, 38));
        titleRow.add(countLabel, BorderLayout.EAST);
        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(10));

        // Readability Scorecard Strip
        JPanel scorecard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        scorecard.setBackground(ThemeManager.getSurfaceElevated());
        scorecard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        JLabel scoreTitle = new JLabel("Readability:");
        scoreTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        scoreTitle.setForeground(ThemeManager.getTextSecondary());
        scorecard.add(scoreTitle);

        fleschBadge = createBadge("Ease: --/100", new Color(37, 99, 235), new Color(239, 246, 255));
        scorecard.add(fleschBadge);

        gradeBadge = createBadge("Grade: --", new Color(124, 58, 237), new Color(245, 243, 255));
        scorecard.add(gradeBadge);

        readingLevelLbl = new JLabel("Analyzing...");
        readingLevelLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        readingLevelLbl.setForeground(ThemeManager.getTextSecondary());
        scorecard.add(readingLevelLbl);

        headerPanel.add(scorecard);
        headerPanel.add(Box.createVerticalStrut(8));

        // Category Filter Pills Row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterRow.setOpaque(false);
        String[] cats = {"All", "Spelling", "Grammar", "Punctuation", "Style"};
        ButtonGroup bg = new ButtonGroup();
        for (String cat : cats) {
            JToggleButton btn = new JToggleButton(cat, "All".equals(cat));
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                activeCategoryFilter = cat;
                applyFilter();
            });
            bg.add(btn);
            filterRow.add(btn);
        }
        headerPanel.add(filterRow);

        main.add(headerPanel, BorderLayout.NORTH);

        // Center: Split List vs Details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.44);

        listModel = new DefaultListModel<>();
        issueList = new JList<>(listModel);
        issueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        issueList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(6, 8, 6, 8));
                if (value instanceof GrammarIssue gi) {
                    l.setText("<html><font color='#64748b'>[" + gi.category() + "]</font> <b>" + gi.original() + "</b> → <font color='#16a34a'>" + gi.suggestion() + "</font></html>");
                }
                if (isSelected) {
                    l.setBackground(ThemeManager.getAccent());
                    l.setForeground(Color.WHITE);
                }
                return l;
            }
        });
        issueList.addListSelectionListener(e -> showSelectedIssue());

        JScrollPane listScroll = new JScrollPane(issueList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Detected Issues"));
        splitPane.setLeftComponent(listScroll);

        // Right Details Panel
        JPanel detailPanel = new JPanel(new BorderLayout(0, 10));
        detailPanel.setOpaque(false);
        detailPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Suggestion Details"),
                new EmptyBorder(8, 8, 8, 8)
        ));

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        badgeRow.setOpaque(false);
        originalBadge = new JLabel();
        originalBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        originalBadge.setForeground(new Color(220, 38, 38));

        suggestionBadge = new JLabel();
        suggestionBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        suggestionBadge.setForeground(new Color(22, 163, 74));

        badgeRow.add(new JLabel("Original:"));
        badgeRow.add(originalBadge);
        badgeRow.add(new JLabel("Suggested Fix:"));
        badgeRow.add(suggestionBadge);
        detailPanel.add(badgeRow, BorderLayout.NORTH);

        contextArea = new JTextArea();
        contextArea.setWrapStyleWord(true);
        contextArea.setLineWrap(true);
        contextArea.setEditable(false);
        contextArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        contextArea.setBackground(ThemeManager.getSurfaceElevated());
        contextArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel centerText = new JPanel(new BorderLayout(0, 4));
        centerText.setOpaque(false);
        ruleLabel = new JLabel("Rule Explanation");
        ruleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        ruleLabel.setForeground(new Color(37, 99, 235));
        centerText.add(ruleLabel, BorderLayout.NORTH);
        centerText.add(new JScrollPane(contextArea), BorderLayout.CENTER);
        detailPanel.add(centerText, BorderLayout.CENTER);

        splitPane.setRightComponent(detailPanel);
        main.add(splitPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(ThemeManager.getSurface());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorder()));

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        JButton fixBtn = new JButton("Fix Selected Issue", VectorIcons.createIcon(VectorIcons.CHECKMARK, 13, new Color(37, 99, 235)));
        fixBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fixBtn.addActionListener(e -> fixSelected());

        JButton fixAllBtn = new JButton("Fix All Issues (1-Click)", VectorIcons.createIcon(VectorIcons.CHECKMARK, 13, Color.WHITE));
        fixAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fixAllBtn.setBackground(new Color(22, 163, 74));
        fixAllBtn.setForeground(Color.WHITE);
        fixAllBtn.setOpaque(true);
        fixAllBtn.setBorderPainted(false);
        fixAllBtn.addActionListener(e -> fixAll());

        buttonPanel.add(closeBtn);
        buttonPanel.add(fixBtn);
        buttonPanel.add(fixAllBtn);
        main.add(buttonPanel, BorderLayout.SOUTH);

        add(main, BorderLayout.CENTER);

        rescan();
    }

    private JLabel createBadge(String text, Color fg, Color bg) {
        JLabel badge = new JLabel(text);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(fg);
        badge.setBackground(bg);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(2, 6, 2, 6));
        return badge;
    }

    private void rescan() {
        allIssues = grammarEngine.analyzeText(currentText);
        ReadabilityStats stats = grammarEngine.calculateReadability(currentText);

        fleschBadge.setText("Ease: " + stats.fleschReadingEase() + "/100");
        gradeBadge.setText("Grade: " + stats.gradeLevel());
        readingLevelLbl.setText("• " + stats.readingLevel() + " (" + stats.wordCount() + " words)");

        applyFilter();
    }

    private void applyFilter() {
        listModel.clear();
        for (GrammarIssue gi : allIssues) {
            if ("All".equals(activeCategoryFilter) || gi.category().equalsIgnoreCase(activeCategoryFilter)) {
                listModel.addElement(gi);
            }
        }

        countLabel.setText(allIssues.isEmpty() ? "No issues found! Document is clean." : allIssues.size() + " total issue(s)");
        countLabel.setForeground(allIssues.isEmpty() ? new Color(22, 163, 74) : new Color(220, 38, 38));

        if (!listModel.isEmpty()) {
            issueList.setSelectedIndex(0);
        } else {
            originalBadge.setText("-");
            suggestionBadge.setText("-");
            ruleLabel.setText("All checks passed!");
            contextArea.setText(allIssues.isEmpty()
                    ? "No grammar, spelling, or punctuation issues detected."
                    : "No issues in category '" + activeCategoryFilter + "'.");
        }
    }

    private void showSelectedIssue() {
        GrammarIssue sel = issueList.getSelectedValue();
        if (sel == null) return;

        originalBadge.setText(sel.original());
        suggestionBadge.setText(sel.suggestion());
        ruleLabel.setText(sel.ruleExplanation());
        contextArea.setText("Context in text:\n\"" + sel.sentenceContext() + "\"");
    }

    private void fixSelected() {
        GrammarIssue sel = issueList.getSelectedValue();
        if (sel == null) return;

        currentText = currentText.replaceFirst(java.util.regex.Pattern.quote(sel.original()), java.util.regex.Matcher.quoteReplacement(sel.suggestion()));
        modified = true;
        rescan();
        if (onDocumentUpdated != null) onDocumentUpdated.run();
    }

    private void fixAll() {
        currentText = grammarEngine.autoFixAll(currentText);
        modified = true;
        rescan();
        if (onDocumentUpdated != null) onDocumentUpdated.run();
        ModernDialog.showMessage(this, "All detected issues were automatically fixed!", "Proofreading Complete", ModernDialog.Type.SUCCESS);
    }

    public boolean isModified() {
        return modified;
    }

    public String getCorrectedText() {
        return currentText;
    }
}
