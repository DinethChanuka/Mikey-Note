package com.wordbook.infrastructure.ui;

import com.wordbook.core.application.dto.SystemStatsDto;
import com.wordbook.core.application.service.MigrationService;
import com.wordbook.infrastructure.web.WordBookHttpServer;
import com.wordbook.infrastructure.ui.swing.ModernDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modern Swing control dashboard providing visual database inspection,
 * quick browser launching, backups, and live server telemetry.
 */
public class WordBookDesktopGui {
    private final WordBookHttpServer server;
    private final MigrationService migrationService;
    private final String dbPath;

    private JFrame frame;
    private JLabel statusLabel;
    private JLabel booksCountLabel;
    private JLabel lessonsCountLabel;
    private JLabel wordsCountLabel;
    private JLabel dbSizeLabel;
    private JTextArea logArea;

    public WordBookDesktopGui(WordBookHttpServer server, MigrationService migrationService, String dbPath) {
        this.server = server;
        this.migrationService = migrationService;
        this.dbPath = dbPath;
    }

    public void launch() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[GUI] Headless environment detected; skipping Swing dashboard.");
            return;
        }

        SwingUtilities.invokeLater(this::initAndShowGui);
    }

    private void initAndShowGui() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        frame = new JFrame("Mikey Note Companion — Java 24 + SQLite Helper");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(620, 520);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("[GUI] Companion window closed. Mikey Note server continues running in background.");
            }
        });

        try {
            File logoFile = new File("assets/mikey_note_logo.png");
            if (!logoFile.exists()) logoFile = new File("assets/logo.png");
            if (!logoFile.exists()) logoFile = new File("assets/notedown_logo.png");
            if (logoFile.exists()) frame.setIconImage(javax.imageio.ImageIO.read(logoFile));
        } catch (Exception ignored) {}

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));
        mainPanel.setBackground(new Color(248, 250, 252));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📝 Mikey Note Helper & SQLite Server");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(30, 58, 138));

        statusLabel = new JLabel("● Online (Port " + server.getPort() + ")");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(new Color(22, 163, 74));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center: Stats Cards + Actions
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);

        // Stats Grid
        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        statsGrid.setOpaque(false);

        booksCountLabel = createStatCard(statsGrid, "Total Books", "0");
        lessonsCountLabel = createStatCard(statsGrid, "Total Lessons", "0");
        wordsCountLabel = createStatCard(statsGrid, "Total Words", "0");
        dbSizeLabel = createStatCard(statsGrid, "SQLite Storage", "0 KB");

        centerPanel.add(statsGrid, BorderLayout.NORTH);

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);

        JButton openBrowserBtn = new JButton("🌐 Open Mikey Note in Browser");
        openBrowserBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        openBrowserBtn.setBackground(new Color(37, 99, 235));
        openBrowserBtn.setForeground(Color.BLACK);
        openBrowserBtn.addActionListener(e -> openBrowser());

        JButton backupBtn = new JButton("💾 Backup Database");
        backupBtn.addActionListener(e -> backupDatabase());

        JButton openFolderBtn = new JButton("📁 Database Folder");
        openFolderBtn.addActionListener(e -> openDbFolder());

        JButton refreshBtn = new JButton("🔄 Refresh Stats");
        refreshBtn.addActionListener(e -> refreshStats());

        JButton stopBtn = new JButton("🛑 Stop Server");
        stopBtn.setForeground(new Color(220, 38, 38));
        stopBtn.addActionListener(e -> {
            boolean confirm = ModernDialog.showConfirm(frame, "Stop Mikey Note server and exit?", "Exit Mikey Note", ModernDialog.Type.WARNING);
            if (confirm) {
                System.exit(0);
            }
        });

        buttonPanel.add(openBrowserBtn);
        buttonPanel.add(backupBtn);
        buttonPanel.add(openFolderBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(stopBtn);

        centerPanel.add(buttonPanel, BorderLayout.CENTER);

        // Activity Log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        JLabel logTitle = new JLabel("System Telemetry:");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logTitle.setForeground(new Color(71, 85, 105));

        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(255, 255, 255));
        JScrollPane scrollPane = new JScrollPane(logArea);

        logPanel.add(logTitle, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(logPanel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Footer
        JLabel footerLabel = new JLabel("SQLite Database Path: " + new File(dbPath).getAbsolutePath());
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footerLabel.setForeground(new Color(100, 116, 139));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        log("WordBook Server initialized on port " + server.getPort());
        log("SQLite database active: " + dbPath);
        refreshStats();
    }

    private JLabel createStatCard(JPanel container, String title, String initialVal) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel tLbl = new JLabel(title);
        tLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tLbl.setForeground(new Color(100, 116, 139));

        JLabel vLbl = new JLabel(initialVal);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        vLbl.setForeground(new Color(15, 23, 42));

        card.add(tLbl, BorderLayout.NORTH);
        card.add(vLbl, BorderLayout.CENTER);
        container.add(card);
        return vLbl;
    }

    public void refreshStats() {
        try {
            SystemStatsDto stats = migrationService.getSystemStats();
            booksCountLabel.setText(String.valueOf(stats.totalBooks()));
            lessonsCountLabel.setText(String.valueOf(stats.totalLessons()));
            wordsCountLabel.setText(String.format("%,d", stats.totalWords()));

            double kb = stats.databaseSizeBytes() / 1024.0;
            if (kb >= 1024) {
                dbSizeLabel.setText(String.format("%.2f MB", kb / 1024.0));
            } else {
                dbSizeLabel.setText(String.format("%.1f KB", kb));
            }
            log("Telemetry updated: " + stats.totalBooks() + " books, " + stats.totalLessons() + " lessons.");
        } catch (Exception e) {
            log("Failed to refresh stats: " + e.getMessage());
        }
    }

    public void openBrowser() {
        String url = "http://localhost:" + server.getPort() + "/";
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                log("Opened browser to " + url);
            } else {
                // Fallback for macOS/Linux CLI
                Runtime.getRuntime().exec(new String[]{"open", url});
                log("Dispatched 'open' command for " + url);
            }
        } catch (Exception e) {
            log("Unable to launch browser: " + e.getMessage());
            ModernDialog.showMessage(frame, "Mikey Note is running at: " + url + "\nPlease open this in your browser.", "Mikey Note", ModernDialog.Type.INFO);
        }
    }

    private void backupDatabase() {
        File source = new File(dbPath);
        if (!source.exists()) {
            ModernDialog.showMessage(frame, "No database file found to backup.", "Warning", ModernDialog.Type.WARNING);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("wordbook_backup_" + System.currentTimeMillis() + ".db"));
        int res = chooser.showSaveDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log("Database backup saved to: " + dest.getAbsolutePath());
                ModernDialog.showMessage(frame, "Database successfully backed up!", "Success", ModernDialog.Type.SUCCESS);
            } catch (IOException ex) {
                log("Backup failed: " + ex.getMessage());
                ModernDialog.showMessage(frame, "Failed to copy database: " + ex.getMessage(), "Error", ModernDialog.Type.ERROR);
            }
        }
    }

    private void openDbFolder() {
        File file = new File(dbPath).getAbsoluteFile();
        File parent = file.getParentFile();
        if (parent != null && parent.exists()) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(parent);
                } else {
                    Runtime.getRuntime().exec(new String[]{"open", parent.getAbsolutePath()});
                }
            } catch (Exception e) {
                log("Could not open folder: " + e.getMessage());
            }
        }
    }

    private void log(String message) {
        if (logArea != null) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + time + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
        System.out.println("[GUI] " + message);
    }
}
