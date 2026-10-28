package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.dto.SystemStatsDto;
import com.wordbook.core.application.service.MigrationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Settings dialog providing theme switching (Light / Dark / Cream / Nordic),
 * editor preferences, and local storage telemetry without exposing database driver internals.
 */
public class SettingsDialog extends JDialog {

    private final MigrationService migrationService;

    public SettingsDialog(Window owner, MigrationService migrationService) {
        this(owner, migrationService, null);
    }

    public SettingsDialog(Window owner, MigrationService migrationService, String dbPath) {
        super(owner, "Mikey Note Settings", ModalityType.APPLICATION_MODAL);
        this.migrationService = migrationService;
        initUi();
    }

    private void initUi() {
        setSize(460, 420);
        setResizable(false);
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(ThemeManager.getSurface());
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        header.setOpaque(false);
        JLabel iconLbl = new JLabel(VectorIcons.create(VectorIcons.Type.SETTINGS, 22, ThemeManager.getAccent()));
        JLabel titleLbl = new JLabel("Application Preferences");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLbl.setForeground(ThemeManager.getTextPrimary());
        header.add(iconLbl);
        header.add(titleLbl);
        root.add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // 1. Theme Setting
        JPanel themeGrp = createSectionPanel("Interface Theme");
        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        themeRow.setOpaque(false);
        themeRow.add(new JLabel("Color Theme:"));

        ModernComboBox<ThemeManager.Theme> themeCombo = new ModernComboBox<>(ThemeManager.Theme.values());
        themeCombo.setSelectedItem(ThemeManager.getCurrentTheme());
        themeCombo.addActionListener(e -> {
            ThemeManager.Theme selected = (ThemeManager.Theme) themeCombo.getSelectedItem();
            if (selected != null) {
                ThemeManager.setTheme(selected);
                root.setBackground(ThemeManager.getSurface());
                titleLbl.setForeground(ThemeManager.getTextPrimary());
                root.repaint();
            }
        });
        themeRow.add(themeCombo);
        themeGrp.add(themeRow);
        content.add(themeGrp);
        content.add(Box.createVerticalStrut(14));

        // 2. Editor Defaults
        JPanel editorGrp = createSectionPanel("Editor & Typography");
        JPanel fontRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        fontRow.setOpaque(false);
        fontRow.add(new JLabel("Default Font:"));

        ModernComboBox<String> fontCombo = new ModernComboBox<>(new String[]{
                "Plus Jakarta Sans", "Calibri", "Arial", "Georgia", "Times New Roman", "JetBrains Mono"
        });
        fontCombo.setSelectedItem("Calibri");
        fontRow.add(fontCombo);
        editorGrp.add(fontRow);
        content.add(editorGrp);
        content.add(Box.createVerticalStrut(14));

        // 3. Local Storage Telemetry (No SQLite mentioned)
        JPanel storageGrp = createSectionPanel("Storage & Reliability");
        try {
            SystemStatsDto stats = migrationService.getSystemStats();
            double kb = stats.databaseSizeBytes() / 1024.0;
            String sizeStr = kb >= 1024 ? String.format("%.2f MB", kb / 1024.0) : String.format("%.1f KB", kb);

            JLabel stat1 = new JLabel("Engine: Local ACID Engine (Transaction Safe)");
            stat1.setFont(new Font("SansSerif", Font.PLAIN, 12));
            stat1.setForeground(ThemeManager.getTextSecondary());
            JLabel stat2 = new JLabel("Local Storage Used: " + sizeStr + " (" + stats.totalBooks() + " notebooks, " + stats.totalLessons() + " lessons)");
            stat2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            stat2.setForeground(ThemeManager.getTextSecondary());

            storageGrp.add(stat1);
            storageGrp.add(Box.createVerticalStrut(4));
            storageGrp.add(stat2);
        } catch (Exception ignored) {}

        content.add(storageGrp);
        root.add(content, BorderLayout.CENTER);

        // Footer Button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        JButton closeBtn = new JButton("Save & Close");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        closeBtn.setBackground(ThemeManager.getAccent());
        closeBtn.setForeground(Color.WHITE);
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createSectionPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ThemeManager.getBorder(), 1, true), title),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return p;
    }
}
