package com.wordbook;

import com.wordbook.core.application.dto.SystemStatsDto;
import com.wordbook.core.application.service.*;
import com.wordbook.infrastructure.persistence.sqlite.*;
import com.wordbook.infrastructure.ui.WordBookDesktopGui;
import com.wordbook.infrastructure.ui.swing.ThemeManager;
import com.wordbook.infrastructure.web.WordBookHttpServer;
import com.wordbook.infrastructure.web.json.SimpleJson;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Main application entry point for WordBook (Modular Java Architecture + SQLite).
 */
public class WordBookApplication {
    public static final String VERSION = "2.0.0";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_DB_FILE = "wordbook.db";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String host = "127.0.0.1";
        String dbPath = DEFAULT_DB_FILE;
        boolean headless = false;
        boolean statsOnly = false;
        boolean migrateOnly = false;
        String importFile = null;

        // Parse CLI parameters
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--port", "-p" -> {
                    if (i + 1 < args.length) port = Integer.parseInt(args[++i]);
                }
                case "--host", "-h" -> {
                    if (i + 1 < args.length) host = args[++i];
                }
                case "--db", "-d" -> {
                    if (i + 1 < args.length) dbPath = args[++i];
                }
                case "--headless" -> headless = true;
                case "--stats" -> statsOnly = true;
                case "--migrate" -> migrateOnly = true;
                case "--import" -> {
                    if (i + 1 < args.length) importFile = args[++i];
                }
                case "--help" -> {
                    printHelp();
                    return;
                }
            }
        }

        printBanner(port, dbPath);

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // 1. If running with GUI, display Mikey Note Dedicated Startup Loading JFrame
        com.wordbook.infrastructure.ui.swing.NoteDownLoadingFrame loadingFrame = null;
        if (!headless && !GraphicsEnvironment.isHeadless()) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            // Ensure ThemeManager initializes to white/light theme default
            ThemeManager.getInstance();
            loadingFrame = new com.wordbook.infrastructure.ui.swing.NoteDownLoadingFrame();
            loadingFrame.setVisible(true);
            loadingFrame.updateProgress(15, "Initializing Mikey Note Core Engine...");
        }

        // 2. Initialize Persistence Infrastructure (Local Storage Engine)
        if (loadingFrame != null) loadingFrame.updateProgress(35, "Connecting to Local Storage Engine (WAL mode)...");
        SqliteConnectionManager connectionManager = new SqliteConnectionManager(dbPath);
        SqliteSchemaManager schemaManager = new SqliteSchemaManager(connectionManager);

        System.out.println("[DB] Initializing local storage database schema...");
        schemaManager.initializeSchema();
        System.out.println("[DB] Schema migrations applied successfully.");

        if (migrateOnly) {
            System.out.println("[DB] Migration completed. Exiting as requested.");
            if (loadingFrame != null) loadingFrame.dispose();
            connectionManager.close();
            return;
        }

        // 3. Instantiate Repositories (Ports & Adapters)
        if (loadingFrame != null) loadingFrame.updateProgress(55, "Mounting Invariants & Repository Ports...");
        SqliteBookRepository bookRepository = new SqliteBookRepository(connectionManager);
        SqliteLessonRepository lessonRepository = new SqliteLessonRepository(connectionManager);
        SqliteUnitOfWork unitOfWork = new SqliteUnitOfWork(connectionManager);

        // 4. Instantiate Domain Services
        if (loadingFrame != null) loadingFrame.updateProgress(75, "Mounting Document Pipeline & Services...");
        BookService bookService = new BookService(bookRepository, lessonRepository, unitOfWork);
        LessonService lessonService = new LessonService(bookRepository, lessonRepository);
        ExportService exportService = new ExportService(bookRepository, lessonRepository);
        ChartEngineService chartEngineService = new ChartEngineService();
        MigrationService migrationService = new MigrationService(bookRepository, lessonRepository, unitOfWork, dbPath);

        // Handle CLI Commands
        if (importFile != null) {
            if (loadingFrame != null) loadingFrame.dispose();
            handleImport(importFile, migrationService);
            connectionManager.close();
            return;
        }

        if (statsOnly) {
            if (loadingFrame != null) loadingFrame.dispose();
            printStats(migrationService);
            connectionManager.close();
            return;
        }

        // Seed initial default welcome book if database is empty
        seedInitialDataIfEmpty(bookService, lessonService);

        // 5. Locate Static Assets Directory
        Path staticDir = resolveStaticDirectory();
        System.out.println("[Service] Serving static assets from: " + staticDir.toAbsolutePath());

        // 6. Start Embedded HTTP Server (background service)
        if (loadingFrame != null) loadingFrame.updateProgress(90, "Starting Background Services & Engine...");
        WordBookHttpServer server = null;
        for (int p = port; p <= port + 20; p++) {
            try {
                WordBookHttpServer s = new WordBookHttpServer(
                        p, host, staticDir,
                        bookService, lessonService, exportService,
                        chartEngineService, migrationService
                );
                s.start();
                server = s;
                break;
            } catch (java.net.BindException be) {
                if (p == port) {
                    System.out.println("[Service] Port " + port + " is busy, finding open port...");
                }
            } catch (IOException e) {
                System.err.println("[Error] Failed to start HTTP server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        if (server == null) {
            System.err.println("[Error] Could not bind to any port in range " + port + "-" + (port + 20));
            System.exit(1);
        }

        final WordBookHttpServer runningServer = server;

        // 7. Register Graceful Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Stopping WordBook server...");
            runningServer.stop(1);
            connectionManager.close();
            System.out.println("[Shutdown] Clean shutdown completed.");
        }));

        System.out.println("\n========================================================");
        System.out.println("  Mikey Note Native Desktop Studio: Running on JFrame");
        System.out.println("  Theme:                          White Theme (Default)");
        System.out.println("  Vector Icon Engine:             Enabled (SVG Quality)");
        System.out.println("  Cover Watermarks:               50+ Designs Active");
        System.out.println("  Color Picker:                   Global Customized HSV Canvas");
        System.out.println("========================================================\n");

        // 8. Launch Native Mikey Note Studio JFrame via Loading Transition (No Browser)
        if (!headless && !GraphicsEnvironment.isHeadless()) {
            final String finalDbPath = dbPath;
            final com.wordbook.infrastructure.ui.swing.NoteDownLoadingFrame finalLoadingFrame = loadingFrame;
            SwingUtilities.invokeLater(() -> {
                com.wordbook.infrastructure.ui.swing.WordBookMainFrame mainFrame =
                        new com.wordbook.infrastructure.ui.swing.WordBookMainFrame(
                                bookService, lessonService, exportService,
                                chartEngineService, migrationService, finalDbPath
                        );

                if (finalLoadingFrame != null) {
                    finalLoadingFrame.finishAndLaunch(() -> mainFrame.setVisible(true));
                } else {
                    mainFrame.setVisible(true);
                }
            });
        }

        // Keep main server thread alive until process is terminated
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void seedInitialDataIfEmpty(BookService bookService, LessonService lessonService) {
        if (bookService.getTotalBooksCount() == 0) {
            System.out.println("[DB] Empty database detected. Seeding welcome notebook...");
            var book = bookService.createBook(new com.wordbook.core.application.dto.BookCreateRequest(
                    "welcome",
                    "Welcome to Mikey Note",
                    "#2563eb|FLOWERS"
            ));

            String welcomeHtml = """
                <h1>Welcome to Mikey Note</h1>
                <p>Mikey Note is powered by a <b>high-performance modular Java architecture</b> and a reliable local <b>embedded storage system</b>!</p>
                <h3>Features in Mikey Note:</h3>
                <ul>
                  <li><b>ACID Local Storage:</b> All your books, lessons, and formatting are saved locally with transaction safety.</li>
                  <li><b>Clean Modular Architecture:</b> Domain-driven design with separate Core Domain, Application, and Persistence layers.</li>
                  <li><b>Rich Word-Style Ribbon:</b> Full formatting toolbar with fonts, colors, tables, checkboxes, quotes, code blocks, and symbols.</li>
                  <li><b>Server-Side Chart Engine:</b> High-res vector Bar, Line, Pie, Treemap, and Diagram charts.</li>
                  <li><b>Instant Auto-Save:</b> Continuous background synchronization directly to local storage.</li>
                  <li><b>50+ Vector Cover Designs:</b> Choose watermarks and custom color palettes for your notebook covers.</li>
                </ul>
                <p>Feel free to edit this lesson, create tables, or add new notebooks!</p>
            """;

            lessonService.createLesson(book.id(), new com.wordbook.core.application.dto.LessonCreateRequest(
                    "getting-started",
                    "Getting Started",
                    welcomeHtml,
                    0
            ));
        }
    }

    private static Path resolveStaticDirectory() {
        Path cur = Paths.get(".").toAbsolutePath().normalize();
        // 1. Current directory directly
        if (Files.exists(cur.resolve("loadwindow/index.html")) || Files.exists(cur.resolve("index.html"))) {
            return cur;
        }
        // 2. Source subdirectory (when launched from workspace root)
        Path srcDir = cur.resolve("Source");
        if (Files.exists(srcDir.resolve("loadwindow/index.html")) || Files.exists(srcDir.resolve("index.html"))) {
            return srcDir;
        }
        // 3. Resources directory
        Path res = cur.resolve("src/main/resources");
        if (Files.exists(res.resolve("loadwindow/index.html")) || Files.exists(res.resolve("static/index.html"))) {
            return res;
        }
        // 4. Source/src/main/resources
        Path srcRes = srcDir.resolve("src/main/resources");
        if (Files.exists(srcRes.resolve("loadwindow/index.html")) || Files.exists(srcRes.resolve("static/index.html"))) {
            return srcRes;
        }
        return cur;
    }

    @SuppressWarnings("unchecked")
    private static void handleImport(String filePath, MigrationService migrationService) {
        System.out.println("[Import] Importing data from " + filePath + "...");
        try {
            String json = Files.readString(Paths.get(filePath));
            Map<String, Object> data = SimpleJson.parseObject(json);
            Object booksObj = data.get("books");
            if (booksObj instanceof List<?> list) {
                int count = migrationService.importBooks((List<Map<String, Object>>) list);
                System.out.println("[Import] Successfully imported " + count + " books into SQLite!");
            } else {
                System.err.println("[Import] Failed: 'books' array not found in JSON.");
            }
        } catch (Exception e) {
            System.err.println("[Import] Error reading file: " + e.getMessage());
        }
    }

    private static void printStats(MigrationService migrationService) {
        SystemStatsDto stats = migrationService.getSystemStats();
        System.out.println("\n--- Mikey Note Database Diagnostics ---");
        System.out.println("SQLite Database:    " + stats.databasePath());
        System.out.println("Database File Size: " + stats.databaseSizeBytes() + " bytes (" + (stats.databaseSizeBytes() / 1024) + " KB)");
        System.out.println("Total Books:        " + stats.totalBooks());
        System.out.println("Total Lessons:      " + stats.totalLessons());
        System.out.println("Total Words:        " + stats.totalWords());
        System.out.println("Total Characters:   " + stats.totalCharacters());
        System.out.println("Java Runtime:       " + stats.javaVersion());
        System.out.println("--------------------------------------\n");
    }

    private static void printBanner(int port, String dbPath) {
        System.out.println("""
              _   _       _       ____                     \s
             | \\ | | ___ | |_ ___|  _ \\  _____      ___ __ \s
             |  \\| |/ _ \\| __/ _ \\ | | |/ _ \\ \\ /\\ / / '_ \\\s
             | |\\  | (_) | ||  __/ |_| | (_) \\ V  V /| | | |
             |_| \\_|\\___/ \\__\\___|____/ \\___/ \\_/\\_/ |_| |_|
                         Mikey Note Studio • Java 24 + SQLite v%s
            """.formatted(VERSION));
    }

    private static void printHelp() {
        System.out.println("""
            Mikey Note - Modular Java & SQLite Document Studio

            Usage: java -jar wordbook.jar [options]
                   ./run.sh [options]

            Options:
              -p, --port <number>    HTTP port to bind (default: 8080)
              -h, --host <string>    Host interface to bind (default: 127.0.0.1)
              -d, --db <path>        SQLite database file path (default: wordbook.db)
              --headless             Run without Swing desktop GUI
              --stats                Print database telemetry and exit
              --migrate              Run database migrations and exit
              --import <file>        Import JSON backup file into SQLite and exit
              --help                 Display this help message
            """);
    }
}
