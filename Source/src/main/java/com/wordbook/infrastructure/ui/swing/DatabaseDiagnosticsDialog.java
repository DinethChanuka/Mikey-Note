package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.dto.SystemStatsDto;
import com.wordbook.core.application.service.MigrationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Swing dialog presenting local storage health, disk metrics, and backups.
 */
public class DatabaseDiagnosticsDialog extends JDialog {
    private final MigrationService migrationService;
    private final String dbPath;

    public DatabaseDiagnosticsDialog(Frame owner, MigrationService migrationService, String dbPath) {
        super(owner, "Storage Telemetry & Health", true);
        this.migrationService = migrationService;
        this.dbPath = dbPath;

        setSize(500, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();

        SystemStatsDto stats = migrationService.getSystemStats();

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(18, 22, 18, 22));
        content.setBackground(theme.surface());

        JLabel header = new JLabel("Local Storage Engine Telemetry", VectorIcons.createIcon(VectorIcons.FOLDER, 18, theme.accent()), SwingConstants.LEFT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setForeground(theme.accent());
        content.add(header);
        content.add(Box.createVerticalStrut(14));

        double kb = stats.databaseSizeBytes() / 1024.0;
        String sizeStr = kb >= 1024 ? String.format("%.2f MB", kb / 1024.0) : String.format("%.1f KB", kb);

        addRow(content, "Storage File:", new File(stats.databasePath()).getAbsolutePath(), theme);
        addRow(content, "Disk Footprint:", sizeStr + " (" + stats.databaseSizeBytes() + " bytes)", theme);
        addRow(content, "Total Notebooks:", String.valueOf(stats.totalBooks()), theme);
        addRow(content, "Total Lessons:", String.valueOf(stats.totalLessons()), theme);
        addRow(content, "Total Words:", String.format("%,d", stats.totalWords()), theme);
        addRow(content, "Total Characters:", String.format("%,d", stats.totalCharacters()), theme);
        addRow(content, "Journal Mode:", "WAL (Write-Ahead Logging)", theme);
        addRow(content, "Data Integrity:", "Enforced Cascading Referentials", theme);
        addRow(content, "Runtime Platform:", "Java " + stats.javaVersion(), theme);

        add(content, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(theme.surface());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.border()));

        JButton backupBtn = new JButton("Backup Storage", VectorIcons.createIcon(VectorIcons.EXPORT, 14, theme.textPrimary()));
        backupBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backupBtn.setFocusPainted(false);
        backupBtn.addActionListener(e -> backup());

        JButton folderBtn = new JButton("Open Storage Folder", VectorIcons.createIcon(VectorIcons.FOLDER, 14, theme.textPrimary()));
        folderBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        folderBtn.setFocusPainted(false);
        folderBtn.addActionListener(e -> openFolder());

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(backupBtn);
        buttonPanel.add(folderBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, String label, String value, ThemeManager.Theme theme) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(500, 26));

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(theme.textSecondary());
        l.setPreferredSize(new Dimension(140, 22));

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        v.setForeground(theme.textPrimary());

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        panel.add(row);
        panel.add(Box.createVerticalStrut(4));
    }

    private void backup() {
        File source = new File(dbPath);
        if (!source.exists()) {
            ModernDialog.showMessage(this, "Storage file does not exist yet.", "Notice", ModernDialog.Type.INFO);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("notedown_backup_" + System.currentTimeMillis() + ".db"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(source.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                ModernDialog.showMessage(this, "Storage data backed up successfully!", "Success", ModernDialog.Type.SUCCESS);
            } catch (Exception ex) {
                ModernDialog.showMessage(this, "Backup error: " + ex.getMessage(), "Error", ModernDialog.Type.ERROR);
            }
        }
    }

    private void openFolder() {
        File f = new File(dbPath).getAbsoluteFile().getParentFile();
        if (f != null && f.exists()) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(f);
                } else {
                    Runtime.getRuntime().exec(new String[]{"open", f.getAbsolutePath()});
                }
            } catch (Exception ignored) {}
        }
    }
}

