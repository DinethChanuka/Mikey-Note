package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.CitationEngineService;
import com.wordbook.core.application.service.CitationEngineService.Citation;
import com.wordbook.core.application.service.CitationEngineService.CitationStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modern citation manager and bibliography insertion dialog.
 */
public class CitationDialog extends JDialog {
    private final CitationEngineService citationEngine;

    private JComboBox<CitationStyle> styleCombo;
    private JComboBox<Citation> existingCombo;

    private JTextField authorField;
    private JTextField titleField;
    private JTextField yearField;
    private JTextField sourceField;
    private JTextField urlField;
    private JTextField pagesField;

    private JLabel inTextPreviewLabel;
    private JLabel bibPreviewLabel;

    private boolean insertBibliography = false;
    private boolean confirmed = false;

    public CitationDialog(Frame owner, CitationEngineService citationEngine) {
        super(owner, "Citation & Bibliography Studio", true);
        this.citationEngine = citationEngine;
        setSize(640, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(16, 18, 14, 18));
        main.setBackground(ThemeManager.getSurface());

        // Top Header
        JPanel top = new JPanel(new GridLayout(2, 1, 4, 6));
        top.setOpaque(false);

        JPanel styleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        styleRow.setOpaque(false);
        styleRow.add(new JLabel("Citation Style:"));
        styleCombo = new ModernComboBox<>(CitationStyle.values());
        styleCombo.setPreferredSize(new Dimension(200, 28));
        styleCombo.addActionListener(e -> updatePreviews());
        styleRow.add(styleCombo);

        styleRow.add(new JLabel("Load Existing:"));
        DefaultComboBoxModel<Citation> model = new DefaultComboBoxModel<>();
        for (Citation c : citationEngine.getRegisteredCitations()) model.addElement(c);
        existingCombo = new ModernComboBox<>(model);
        existingCombo.setPreferredSize(new Dimension(220, 28));
        existingCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Citation c) {
                    l.setText(c.getFirstAuthorLastName() + " (" + c.year() + ") - " + c.title());
                }
                return l;
            }
        });
        existingCombo.addActionListener(e -> {
            Citation sel = (Citation) existingCombo.getSelectedItem();
            if (sel != null) fillFields(sel);
        });
        styleRow.add(existingCombo);
        top.add(styleRow);

        main.add(top, BorderLayout.NORTH);

        // Center Fields Form
        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Citation Details"),
                new EmptyBorder(8, 8, 8, 8)
        ));

        authorField = new JTextField("Goodfellow, Ian, Bengio, Yoshua, & Courville, Aaron");
        titleField = new JTextField("Deep Learning");
        yearField = new JTextField("2016");
        sourceField = new JTextField("MIT Press");
        urlField = new JTextField("https://www.deeplearningbook.org");
        pagesField = new JTextField("120-135");

        authorField.getDocument().addDocumentListener(new SimpleDocListener(this::updatePreviews));
        titleField.getDocument().addDocumentListener(new SimpleDocListener(this::updatePreviews));
        yearField.getDocument().addDocumentListener(new SimpleDocListener(this::updatePreviews));
        sourceField.getDocument().addDocumentListener(new SimpleDocListener(this::updatePreviews));
        urlField.getDocument().addDocumentListener(new SimpleDocListener(this::updatePreviews));
        pagesField.getDocument().addDocumentListener(new SimpleDocListener(this::updatePreviews));

        form.add(new JLabel("Author(s):"));
        form.add(authorField);
        form.add(new JLabel("Title of Work:"));
        form.add(titleField);
        form.add(new JLabel("Publication Year:"));
        form.add(yearField);
        form.add(new JLabel("Publisher / Journal:"));
        form.add(sourceField);
        form.add(new JLabel("URL / DOI (optional):"));
        form.add(urlField);
        form.add(new JLabel("Page Range (optional):"));
        form.add(pagesField);

        // Previews Container
        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);
        centerContainer.add(form, BorderLayout.CENTER);

        JPanel previewBox = new JPanel(new GridLayout(2, 1, 4, 6));
        previewBox.setOpaque(false);
        previewBox.setBorder(BorderFactory.createTitledBorder("Real-Time Citation Preview"));

        inTextPreviewLabel = new JLabel("In-Text Anchor: ");
        inTextPreviewLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inTextPreviewLabel.setForeground(new Color(37, 99, 235));

        bibPreviewLabel = new JLabel("Bibliography Entry: ");
        bibPreviewLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bibPreviewLabel.setForeground(ThemeManager.getTextSecondary());

        previewBox.add(inTextPreviewLabel);
        previewBox.add(bibPreviewLabel);
        centerContainer.add(previewBox, BorderLayout.SOUTH);

        main.add(centerContainer, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        // Bottom Actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(ThemeManager.getSurface());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorder()));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton bibBtn = new JButton("Insert Full Bibliography", VectorIcons.createIcon(VectorIcons.BOOK, 13, new Color(71, 85, 105)));
        bibBtn.addActionListener(e -> {
            saveCurrentCitation();
            insertBibliography = true;
            confirmed = true;
            dispose();
        });

        JButton insertBtn = new JButton("Insert In-Text Citation", VectorIcons.createIcon(VectorIcons.DOCUMENT, 13, Color.WHITE));
        insertBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        insertBtn.setBackground(new Color(37, 99, 235));
        insertBtn.setForeground(Color.WHITE);
        insertBtn.setOpaque(true);
        insertBtn.setBorderPainted(false);
        insertBtn.addActionListener(e -> {
            saveCurrentCitation();
            insertBibliography = false;
            confirmed = true;
            dispose();
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(bibBtn);
        buttonPanel.add(insertBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        updatePreviews();
    }

    private void fillFields(Citation c) {
        authorField.setText(c.author());
        titleField.setText(c.title());
        yearField.setText(c.year());
        sourceField.setText(c.source());
        urlField.setText(c.urlOrDoi());
        pagesField.setText(c.pages());
        updatePreviews();
    }

    private Citation getCitationFromFields() {
        return new Citation(
                "cit-" + System.currentTimeMillis(),
                authorField.getText(),
                titleField.getText(),
                yearField.getText(),
                sourceField.getText(),
                urlField.getText(),
                pagesField.getText()
        );
    }

    private void saveCurrentCitation() {
        citationEngine.addCitation(getCitationFromFields());
    }

    private void updatePreviews() {
        if (inTextPreviewLabel == null) return;
        Citation c = getCitationFromFields();
        CitationStyle s = (CitationStyle) styleCombo.getSelectedItem();
        if (s == null) s = CitationStyle.APA;

        inTextPreviewLabel.setText("<html>In-Text Citation: <b>" + citationEngine.formatInTextCitation(c, s, 1) + "</b></html>");
        bibPreviewLabel.setText("<html>Bibliography: " + citationEngine.formatBibliographyEntry(c, s, 1) + "</html>");
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isInsertBibliography() {
        return insertBibliography;
    }

    public String generateHtmlToInsert() {
        CitationStyle s = (CitationStyle) styleCombo.getSelectedItem();
        if (s == null) s = CitationStyle.APA;

        if (insertBibliography) {
            return citationEngine.generateBibliographyHtml(null, s);
        } else {
            Citation c = getCitationFromFields();
            int idx = citationEngine.getRegisteredCitations().size();
            return " " + citationEngine.formatInTextCitation(c, s, idx) + " ";
        }
    }

    private record SimpleDocListener(Runnable callback) implements javax.swing.event.DocumentListener {
        public void insertUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
    }
}
