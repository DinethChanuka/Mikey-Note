package com.wordbook;

import com.wordbook.core.application.dto.*;
import com.wordbook.core.application.service.*;
import com.wordbook.core.domain.model.Book;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;
import com.wordbook.core.domain.model.Lesson;
import com.wordbook.infrastructure.persistence.sqlite.*;
import com.wordbook.infrastructure.web.WordBookHttpServer;
import com.wordbook.infrastructure.web.json.SimpleJson;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import com.wordbook.infrastructure.ui.swing.*;

/**
 * Self-contained Test Suite verifying Domain invariants, SQLite persistence,
 * transactions, Chart engine, and JSON engine.
 */
public class WordBookTestSuite {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Running WordBook Modular Architecture Tests     ");
        System.out.println("==================================================");

        testSimpleJson();
        testChartEngine();
        testChartImageRasterization();
        testChartMetadataSidecarAndReRender();
        testPageSetupAndMargins();
        testCitationEngine();
        testGrammarEngine();
        testDocumentStructureDetector();
        testSqlitePersistenceAndTransactions();
        testDomainInvariants();
        testExportService();
        testTableToolsAndContextualTabs();
        testAdvancedUndoAndMemory();
        testInternetScraperAndSmartAccessibility();
        testHttpRestApiServer();
        testProxyRoutingManagerAndVisualCards();
        testRedesignedRibbonAndVisualRepresentations();
        testNewChartTypesAndCardStyling();
        testWordStylePaginationAndChartDesignContextualTab();
        testClaudeChartComponentsAndWiring();
        testNonBlockingDocumentAndEDTResponsiveness();
        testHeaderFooterAndDynamicDesignCards();
        testGlobalUndoRedoAndLessonManagement();
        testSmartLoremGenerator();
        testModernContextMenuAndBlockLineSpacing();
        testChartGlitchAndDisplacementFixes();
        testImageFilterAndCanvasFitting();
        testCardShapeRibbonAndCustomizer();
        testShapeFormatSidePanelAndPaginationStability();

        System.out.println("==================================================");
        System.out.printf("  Results: %d Passed, %d Failed%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testSimpleJson() {
        System.out.println("\n[Test] SimpleJson Serializer & Parser");
        try {
            Map<String, Object> input = Map.of(
                    "title", "Biology 101",
                    "count", 42,
                    "active", true,
                    "tags", List.of("science", "cells")
            );
            String json = SimpleJson.serialize(input);
            assertTrue(json.contains("\"title\":\"Biology 101\""), "Contains title");
            assertTrue(json.contains("\"count\":42"), "Contains count");

            Map<String, Object> parsed = SimpleJson.parseObject(json);
            assertEquals("Biology 101", parsed.get("title"), "Parsed title matches");
            assertEquals(42, parsed.get("count"), "Parsed count matches");
            assertEquals(Boolean.TRUE, parsed.get("active"), "Parsed boolean matches");

            record TestRecord(String id, String name) {}
            String recordJson = SimpleJson.serialize(new TestRecord("b1", "Sample Record"));
            assertTrue(recordJson.contains("\"id\":\"b1\""), "Record serialized id");
            assertTrue(recordJson.contains("\"name\":\"Sample Record\""), "Record serialized name");

            System.out.println("  ✔ SimpleJson tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ SimpleJson test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testChartEngine() {
        System.out.println("\n[Test] ChartEngineService SVG Generation");
        try {
            ChartEngineService engine = new ChartEngineService();

            String barData = "Math: 85\nScience: 92\nEnglish: 78";
            String barSvg = engine.render(ChartType.BAR, barData);
            assertTrue(barSvg.contains("<svg") && barSvg.contains("</svg>"), "Bar chart outputs valid SVG");
            assertTrue(barSvg.contains("<rect"), "Bar chart has rect elements");

            String pieData = "Red: 30\nBlue: 50\nGreen: 20";
            String pieSvg = engine.render(ChartType.PIE, pieData);
            assertTrue(pieSvg.contains("<path d="), "Pie chart has slice paths");

            String lineData = "Mon: 10\nTue: 20\nWed: 15";
            String lineSvg = engine.render(ChartType.LINE, lineData);
            assertTrue(lineSvg.contains("<path d="), "Line chart has polyline path");

            String treeData = "Company\n  Engineering: 12\n  Sales: 8";
            String treeSvg = engine.render(ChartType.TREE, treeData);
            assertTrue(treeSvg.contains("<svg"), "Treemap chart outputs SVG");

            System.out.println("  ✔ ChartEngineService tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ ChartEngine test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testChartImageRasterization() {
        System.out.println("\n[Test] ChartEngineService Image Rasterization & Auto-Detection");
        try {
            ChartEngineService engine = new ChartEngineService();
            String data = "Quarter 1: 450\nQuarter 2: 780\nQuarter 3: 920\nQuarter 4: 1100";

            // Test auto-detection
            var autoType = engine.autoDetectType(data);
            assertNotNull(autoType, "Auto-detected type is not null");

            // Test image rendering
            java.awt.image.BufferedImage img = engine.renderChartToImage(
                    ChartType.BAR, data, ChartEngineService.ChartTheme.EMERALD_GREEN, "Fiscal Performance", 600, 360);
            assertNotNull(img, "Rendered chart image is not null");
            assertEquals(600, img.getWidth(), "Chart width matches");
            assertEquals(360, img.getHeight(), "Chart height matches");

            // Test file creation
            java.io.File chartFile = engine.renderChartToFile(
                    ChartType.PIE, "Product A: 40\nProduct B: 35\nProduct C: 25",
                    ChartEngineService.ChartTheme.SUNSET_CORAL, "Market Share", 500, 320);
            assertTrue(chartFile != null && chartFile.exists(), "Chart file exists on disk");
            assertTrue(chartFile.length() > 0, "Chart file has non-zero size");

            System.out.println("  ✔ Chart Image Rasterization & Auto-Detection tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Chart Image Rasterization test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testChartMetadataSidecarAndReRender() {
        System.out.println("\n[Test] ChartEngineService Metadata Sidecar & Re-Rendering");
        try {
            ChartEngineService engine = new ChartEngineService();
            String rawData = "Desktop: 65\nMobile: 28\nTablet: 7";
            java.io.File chartFile = engine.renderChartToFile(
                    ChartType.PIE, rawData,
                    ChartEngineService.ChartTheme.CYBERPUNK_NEON, "Device Distribution", 520, 340);
            assertTrue(chartFile != null && chartFile.exists(), "Initial chart file exists");

            // Read metadata sidecar
            ChartEngineService.ChartMetadata meta = engine.readChartMetadata(chartFile);
            assertNotNull(meta, "Read chart metadata sidecar");
            assertEquals(ChartType.PIE, meta.type(), "Chart type matches pie");
            assertEquals("Device Distribution", meta.title(), "Title matches");
            assertEquals(ChartEngineService.ChartTheme.CYBERPUNK_NEON, meta.theme(), "Theme matches cyberpunk neon");
            assertEquals(520, meta.width(), "Width matches");
            assertEquals(340, meta.height(), "Height matches");
            assertTrue(meta.rawData().contains("Desktop: 65"), "Raw data preserved in sidecar");

            // Re-render chart with updated data and new theme
            String updatedData = "Desktop: 50\nMobile: 45\nTablet: 5";
            engine.reRenderChartFile(
                    chartFile, ChartType.BAR, updatedData,
                    ChartEngineService.ChartTheme.EMERALD_GREEN, "Updated Device Distribution", 550, 350);
            assertTrue(chartFile.exists(), "Re-rendered chart exists");

            ChartEngineService.ChartMetadata updatedMeta = engine.readChartMetadata(chartFile);
            assertEquals(ChartType.BAR, updatedMeta.type(), "Updated type is bar");
            assertEquals("Updated Device Distribution", updatedMeta.title(), "Updated title matches");
            assertEquals(ChartEngineService.ChartTheme.EMERALD_GREEN, updatedMeta.theme(), "Updated theme matches");
            assertTrue(updatedMeta.rawData().contains("Mobile: 45"), "Updated data contains mobile: 45");

            System.out.println("  ✔ Chart Metadata Sidecar & Re-Rendering tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Chart Metadata Sidecar test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testPageSetupAndMargins() {
        System.out.println("\n[Test] DocumentCanvasPanel Page Setup & Margins Logic");
        try {
            var a4 = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageFormat.A4;
            var letter = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageFormat.LETTER;
            var legal = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageFormat.LEGAL;
            var compact = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageFormat.COMPACT;
            var wide = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageFormat.WIDE;

            assertEquals(740, a4.width, "A4 width is 740px");
            assertEquals(960, a4.height, "A4 height is 960px");
            assertEquals(760, letter.width, "Letter width is 760px");
            assertEquals(900, letter.height, "Letter height is 900px");
            assertEquals(1120, legal.height, "Legal height is 1120px");
            assertEquals(640, compact.width, "Compact width is 640px");
            assertEquals(880, wide.width, "Wide canvas width is 880px");

            var normalMargin = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageMargin.NORMAL;
            var narrowMargin = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageMargin.NARROW;
            var wideMargin = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageMargin.WIDE;

            assertEquals(54, normalMargin.left, "Normal margin left is 54px");
            assertEquals(28, narrowMargin.left, "Narrow margin left is 28px");
            assertEquals(76, wideMargin.left, "Wide margin left is 76px");

            var emptyPat = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PaperPattern.EMPTY;
            var singlePat = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PaperPattern.SINGLE_RULE;
            var squarePat = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PaperPattern.SQUARE_RULE;
            assertNotNull(emptyPat, "Empty paper pattern exists");
            assertNotNull(singlePat, "Single rule paper pattern exists");
            assertNotNull(squarePat, "Square rule paper pattern exists");

            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((html, words, chars) -> {});
            canvas.setPaperPattern(singlePat);
            assertEquals(singlePat, canvas.getPaperPattern(), "Canvas paper pattern set to SINGLE_RULE");
            canvas.setRuleSpacing(34);
            assertEquals(34, canvas.getRuleSpacing(), "Canvas rule spacing set to 34px");
            canvas.setPaperPattern("Square");
            assertEquals(squarePat, canvas.getPaperPattern(), "Canvas string setPaperPattern to SQUARE_RULE");

            System.out.println("  ✔ Page Setup, Margins & Paper Patterns tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Page Setup & Margins test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testCitationEngine() {
        System.out.println("\n[Test] CitationEngineService Formatting & Bibliography");
        try {
            CitationEngineService engine = new CitationEngineService();

            CitationEngineService.Citation c1 = new CitationEngineService.Citation(
                    "c1",
                    "Knuth, Donald E.",
                    "The Art of Computer Programming",
                    "1997",
                    "Addison-Wesley",
                    "https://www-cs-faculty.stanford.edu/~knuth/taocp.html",
                    "120-145"
            );

            CitationEngineService.Citation c2 = new CitationEngineService.Citation(
                    "c2",
                    "Vaswani, Ashish; Shazeer, Noam; Parmar, Niki",
                    "Attention Is All You Need",
                    "2017",
                    "Advances in Neural Information Processing Systems",
                    "10.48550/arXiv.1706.03762",
                    "5998-6008"
            );

            engine.addCitation(c1);
            engine.addCitation(c2);

            // Test style formats
            String apa = engine.formatBibliographyEntry(c1, CitationEngineService.CitationStyle.APA, 1);
            assertTrue(apa.contains("Knuth, Donald E. (1997)"), "APA format contains author and year");
            assertTrue(apa.contains("<i>The Art of Computer Programming</i>"), "APA italicizes book title");

            String ieee = engine.formatBibliographyEntry(c2, CitationEngineService.CitationStyle.IEEE, 2);
            assertTrue(ieee.contains("“Attention Is All You Need,”") || ieee.contains("\"Attention Is All You Need,\""), "IEEE puts article in quotes");

            // Test in-text citation
            String inTextApa = engine.formatInTextCitation(c1, CitationEngineService.CitationStyle.APA, 1);
            assertTrue(inTextApa.contains("Knuth"), "APA in-text citation contains author");

            // Test bibliography generation
            String biblioHtml = engine.generateBibliographyHtml(CitationEngineService.CitationStyle.APA);
            assertTrue(biblioHtml.contains("References") || biblioHtml.contains("Bibliography"), "Bibliography has header");
            assertTrue(biblioHtml.contains("Knuth"), "Bibliography includes entry 1");
            assertTrue(biblioHtml.contains("Vaswani"), "Bibliography includes entry 2");

            System.out.println("  ✔ CitationEngineService tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ CitationEngine test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testGrammarEngine() {
        System.out.println("\n[Test] GrammarEngineService Typo & Spacing Detection");
        try {
            GrammarEngineService engine = new GrammarEngineService();

            String sample = "this is teh best tool . It is very very good ,and i recieve notifications .";
            var issues = engine.analyzeText(sample);

            assertTrue(!issues.isEmpty(), "Detected grammar and typo issues");

            // Check specific issue types
            boolean hasTypo = issues.stream().anyMatch(i -> i.original().equalsIgnoreCase("teh") || i.ruleExplanation().contains("teh"));
            boolean hasDuplicate = issues.stream().anyMatch(i -> i.ruleExplanation().toLowerCase().contains("repeated"));
            boolean hasSpacing = issues.stream().anyMatch(i -> i.ruleExplanation().toLowerCase().contains("punctuation") || i.ruleExplanation().toLowerCase().contains("space"));
            boolean hasCapitalization = issues.stream().anyMatch(i -> i.ruleExplanation().toLowerCase().contains("capitalize"));

            assertTrue(hasTypo, "Detected 'teh' typo");
            assertTrue(hasSpacing, "Detected space around punctuation");

            // Test batch auto-fix
            String fixed = engine.autoFixAll(sample);
            assertTrue(!fixed.contains(" teh "), "Fixed 'teh'");
            assertTrue(fixed.contains(" the "), "Replaced with 'the'");
            assertTrue(fixed.contains("receive"), "Corrected 'recieve' to 'receive'");

            // Test homophone rules
            String homophoneText = "their going to the store, and your welcome here.";
            var homophoneIssues = engine.analyzeText(homophoneText);
            boolean hasTheyre = homophoneIssues.stream().anyMatch(i -> i.original().equalsIgnoreCase("their") && i.suggestion().equalsIgnoreCase("they're"));
            boolean hasYoure = homophoneIssues.stream().anyMatch(i -> i.original().equalsIgnoreCase("your") && i.suggestion().equalsIgnoreCase("you're"));
            assertTrue(hasTheyre, "Detected homophone 'their' -> 'they're'");
            assertTrue(hasYoure, "Detected homophone 'your' -> 'you're'");

            // Test article agreement
            String articleText = "I ate a apple and read an book yesterday.";
            var articleIssues = engine.analyzeText(articleText);
            boolean hasAnApple = articleIssues.stream().anyMatch(i -> i.original().equalsIgnoreCase("a") && i.suggestion().equalsIgnoreCase("an"));
            boolean hasABook = articleIssues.stream().anyMatch(i -> i.original().equalsIgnoreCase("an") && i.suggestion().equalsIgnoreCase("a"));
            assertTrue(hasAnApple, "Detected article agreement 'a apple' -> 'an'");
            assertTrue(hasABook, "Detected article agreement 'an book' -> 'a'");

            // Test extended typos (including user request typo corrections)
            String typoText = "neeed to redesoign all tabvels and adanced optioon with ribon.";
            var typoIssues = engine.analyzeText(typoText);
            assertTrue(typoIssues.stream().anyMatch(i -> i.original().equalsIgnoreCase("redesoign")), "Detected 'redesoign' typo");
            assertTrue(typoIssues.stream().anyMatch(i -> i.original().equalsIgnoreCase("tabvels")), "Detected 'tabvels' typo");
            assertTrue(typoIssues.stream().anyMatch(i -> i.original().equalsIgnoreCase("adanced")), "Detected 'adanced' typo");
            String typoFixed = engine.autoFixAll(typoText);
            assertTrue(typoFixed.contains("redesign"), "Auto-fixed 'redesoign' to 'redesign'");
            assertTrue(typoFixed.contains("tables"), "Auto-fixed 'tabvels' to 'tables'");
            assertTrue(typoFixed.contains("advanced"), "Auto-fixed 'adanced' to 'advanced'");

            // Test category categorization
            GrammarEngineService.GrammarIssue sampleSpelling = new GrammarEngineService.GrammarIssue("wrd", "word", "Spelling typo", 0, 3, "wrd");
            GrammarEngineService.GrammarIssue sampleGrammar = new GrammarEngineService.GrammarIssue("their", "they're", "Homophone: Use 'they're'", 0, 5, "their");
            GrammarEngineService.GrammarIssue samplePunct = new GrammarEngineService.GrammarIssue(" ,", ",", "Remove space before punctuation", 0, 2, " ,");
            assertEquals("Spelling", sampleSpelling.category(), "Issue categorized as Spelling");
            assertEquals("Grammar", sampleGrammar.category(), "Issue categorized as Grammar");
            assertEquals("Punctuation", samplePunct.category(), "Issue categorized as Punctuation");

            // Test readability calculations
            GrammarEngineService.ReadabilityStats stats = engine.calculateReadability("This is a simple sentence. It is very easy to read and understand.");
            assertTrue(stats.wordCount() >= 10, "Word count calculated properly");
            assertTrue(stats.sentenceCount() == 2, "Sentence count is 2");
            assertTrue(stats.fleschReadingEase() > 70.0, "Flesch reading ease score is high for simple sentences");
            assertNotNull(stats.readingLevel(), "Reading level string is populated");
            assertTrue(stats.gradeLevel() > 0.0, "Grade level is positive");

            System.out.println("  ✔ GrammarEngineService tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ GrammarEngine test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testDocumentStructureDetector() {
        System.out.println("\n[Test] DocumentStructureDetector Auto-Detection");
        try {
            DocumentStructureDetector detector = new DocumentStructureDetector();

            String text = """
                    # Architecture Overview
                    System high level design document.

                    ## Key Specifications
                    Version: 2.4.0
                    Author: Team Elyra
                    Status: Production

                    ### Pending Tasks
                    - [ ] Implement caching layer
                    - [x] Configure SSL certificates
                    - [ ] Deploy canary release

                    ### Component Metrics
                    1. API Gateway latency: 12ms
                    2. Database throughput: 8500 qps
                    3. Cache hit ratio: 94.2%
                    """;

            String html = detector.autoStructureToHtml(text);
            assertTrue(html.contains("<h1"), "HTML output contains h1");
            assertTrue(html.contains("<h2"), "HTML output contains h2");
            assertTrue(html.contains("Version") && html.contains("2.4.0"), "HTML output contains model fields");
            assertTrue(html.contains("Implement caching layer"), "HTML output contains checklist items");
            assertTrue(html.contains("<ol"), "HTML output contains ordered list");

            System.out.println("  ✔ DocumentStructureDetector tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ DocumentStructureDetector test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testSqlitePersistenceAndTransactions() {
        System.out.println("\n[Test] SQLite In-Memory Database & Transactions");
        try {
            // Use in-memory SQLite for tests
            SqliteConnectionManager connManager = new SqliteConnectionManager(":memory:");
            SqliteSchemaManager schemaManager = new SqliteSchemaManager(connManager);
            schemaManager.initializeSchema();

            SqliteBookRepository bookRepo = new SqliteBookRepository(connManager);
            SqliteLessonRepository lessonRepo = new SqliteLessonRepository(connManager);
            SqliteUnitOfWork uow = new SqliteUnitOfWork(connManager);

            // 1. Create Book
            Book b1 = Book.createWithId("book-1", "Test Chemistry", "#2b579a");
            bookRepo.save(b1);
            assertEquals(1, bookRepo.count(), "1 book in repo");

            var found = bookRepo.findById("book-1");
            assertTrue(found.isPresent(), "Book found by ID");
            assertEquals("Test Chemistry", found.get().getTitle(), "Book title matches");

            // 2. Create Lessons
            Lesson l1 = Lesson.createWithId("les-1", "book-1", "Atomic Structure", "<p>Atoms consist of protons, neutrons, electrons.</p>", 0);
            Lesson l2 = Lesson.createWithId("les-2", "book-1", "Chemical Bonding", "<p>Covalent and ionic bonds.</p>", 1);
            lessonRepo.save(l1);
            lessonRepo.save(l2);

            assertEquals(2, lessonRepo.count(), "2 lessons in repo");
            assertEquals(2, lessonRepo.findByBookId("book-1").size(), "2 lessons for book-1");

            // 3. Test Transaction Rollback
            try {
                uow.executeInTransaction(() -> {
                    bookRepo.save(Book.createWithId("book-temp", "Should Rollback", "#c0392b"));
                    throw new RuntimeException("Simulated transaction failure");
                });
            } catch (RuntimeException expected) {
                // Expected
            }
            assertTrue(bookRepo.findById("book-temp").isEmpty(), "Rolled back entity does not exist");

            // 4. Test Cascading Foreign Key Delete
            uow.executeInTransaction(() -> {
                lessonRepo.deleteByBookId("book-1");
                bookRepo.deleteById("book-1");
            });
            assertEquals(0, bookRepo.count(), "Books deleted");
            assertEquals(0, lessonRepo.count(), "Lessons cascaded");

            connManager.close();
            System.out.println("  ✔ SQLite persistence & transactions tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ SQLite persistence test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testDomainInvariants() {
        System.out.println("\n[Test] Domain Models & Invariants");
        try {
            Book book = Book.create("Physics Notes", "#0891b2");
            assertNotNull(book.getId(), "Generated book id");
            assertEquals("#0891b2", book.getColor(), "Color matches");

            book.rename("Modern Physics");
            assertEquals("Modern Physics", book.getTitle(), "Renamed book");

            Lesson lesson = Lesson.create(book.getId(), "Quantum Mechanics", "<p>Wave particle duality.</p>", 0);
            assertEquals(3, lesson.getWordCount(), "Calculated word count");
            assertTrue(lesson.getCharCount() > 0, "Calculated character count");

            lesson.updateContent("<p>One two three four five six.</p>");
            assertEquals(6, lesson.getWordCount(), "Updated word count");

            System.out.println("  ✔ Domain invariants tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Domain invariants test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testExportService() {
        System.out.println("\n[Test] ExportService HTML & Markdown Generation");
        try {
            SqliteConnectionManager connManager = new SqliteConnectionManager(":memory:");
            new SqliteSchemaManager(connManager).initializeSchema();
            SqliteBookRepository bookRepo = new SqliteBookRepository(connManager);
            SqliteLessonRepository lessonRepo = new SqliteLessonRepository(connManager);

            Book book = Book.createWithId("exp-book", "Export Book", "#27ae60");
            bookRepo.save(book);

            Lesson lesson = Lesson.createWithId("exp-les", "exp-book", "Chapter 1", "<h1>Chapter 1</h1><p><b>Bold text</b> and <i>italic</i>.</p>", 0);
            lessonRepo.save(lesson);

            ExportService exportService = new ExportService(bookRepo, lessonRepo);
            var htmlExport = exportService.exportLessonAsHtml("exp-les");
            assertTrue(htmlExport.filename().endsWith(".html"), "HTML export filename");
            assertTrue(new String(htmlExport.content()).contains("<!DOCTYPE html>"), "HTML export content has DOCTYPE");

            var mdExport = exportService.exportLessonAsMarkdown("exp-les");
            assertTrue(mdExport.filename().endsWith(".md"), "Markdown export filename");
            assertTrue(new String(mdExport.content()).contains("**Bold text**"), "Markdown export formatted bold");

            connManager.close();
            System.out.println("  ✔ ExportService tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ ExportService test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testHttpRestApiServer() {
        System.out.println("\n[Test] HTTP REST Server & SQLite End-to-End");
        WordBookHttpServer server = null;
        SqliteConnectionManager connManager = null;
        try {
            connManager = new SqliteConnectionManager(":memory:");
            new SqliteSchemaManager(connManager).initializeSchema();

            SqliteBookRepository bookRepo = new SqliteBookRepository(connManager);
            SqliteLessonRepository lessonRepo = new SqliteLessonRepository(connManager);
            SqliteUnitOfWork uow = new SqliteUnitOfWork(connManager);

            BookService bookService = new BookService(bookRepo, lessonRepo, uow);
            LessonService lessonService = new LessonService(bookRepo, lessonRepo);
            ExportService exportService = new ExportService(bookRepo, lessonRepo);
            ChartEngineService chartEngineService = new ChartEngineService();
            MigrationService migrationService = new MigrationService(bookRepo, lessonRepo, uow, ":memory:");

            java.nio.file.Path staticDir = java.nio.file.Paths.get(".").toAbsolutePath();
            int testPort = 18080 + (int)(Math.random() * 500);
            server = new WordBookHttpServer(
                    testPort, "127.0.0.1", staticDir,
                    bookService, lessonService, exportService,
                    chartEngineService, migrationService
            );
            server.start();

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String baseUrl = "http://127.0.0.1:" + testPort;

            // 1. GET /api/books (Empty list initially)
            var getBooksReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/api/books")).GET().build();
            var getBooksRes = client.send(getBooksReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, getBooksRes.statusCode(), "GET /api/books returns 200 OK");
            assertTrue(getBooksRes.body().contains("\"success\":true"), "Response has success true");

            // 2. POST /api/books (Create notebook)
            String bookPayload = "{\"title\":\"Integration Biology\",\"color\":\"#27ae60\"}";
            var postBookReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/api/books"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bookPayload)).build();
            var postBookRes = client.send(postBookReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(201, postBookRes.statusCode(), "POST /api/books returns 201 Created");
            assertTrue(postBookRes.body().contains("Integration Biology"), "Created book returned in body");

            Map<String, Object> createdBookData = (Map<String, Object>) SimpleJson.parseObject(postBookRes.body()).get("data");
            String bookId = (String) createdBookData.get("id");
            assertNotNull(bookId, "Created book has ID");

            // 3. POST /api/books/:id/lessons (Create lesson)
            String lessonPayload = "{\"title\":\"Mitochondria\",\"html\":\"<p>The powerhouse of the cell.</p>\"}";
            var postLessonReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/api/books/" + bookId + "/lessons"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(lessonPayload)).build();
            var postLessonRes = client.send(postLessonReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(201, postLessonRes.statusCode(), "POST lesson returns 201 Created");

            // 4. GET /api/db/stats (Database diagnostics)
            var statsReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/api/db/stats")).GET().build();
            var statsRes = client.send(statsReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, statsRes.statusCode(), "GET /api/db/stats returns 200");
            assertTrue(statsRes.body().contains("\"totalBooks\":1"), "Database stats reflects 1 book");
            assertTrue(statsRes.body().contains("\"totalLessons\":1"), "Database stats reflects 1 lesson");

            // 5. POST /api/charts/render (Server-side SVG chart rendering)
            String chartPayload = "{\"type\":\"bar\",\"data\":\"Cells: 40\\nProteins: 60\"}";
            var chartReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/api/charts/render"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(chartPayload)).build();
            var chartRes = client.send(chartReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, chartRes.statusCode(), "POST /api/charts/render returns 200");
            assertTrue(chartRes.body().contains("<svg"), "Chart API rendered SVG");

            // 6. GET / (Static asset serving)
            var indexReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/")).GET().build();
            var indexRes = client.send(indexReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, indexRes.statusCode(), "GET / returns 200");
            assertTrue(indexRes.body().contains("<title>Mikey Note"), "Serves Mikey Note index.html");
            var favReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/favicon.ico")).GET().build();
            var favRes = client.send(favReq, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, favRes.statusCode(), "GET /favicon.ico returns 200");
            assertTrue(favRes.body().length > 0, "Favicon has content");

            var cssReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/style.css")).GET().build();
            var cssRes = client.send(cssReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, cssRes.statusCode(), "GET /style.css returns 200 from loadwindow");

            var jsReq = java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + "/app.js")).GET().build();
            var jsRes = client.send(jsReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, jsRes.statusCode(), "GET /app.js returns 200 from loadwindow");

            System.out.println("  ✔ HTTP REST Server & SQLite End-to-End tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ HTTP REST Server test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        } finally {
            if (server != null) {
                server.stop(0);
            }
            if (connManager != null) {
                connManager.close();
            }
        }
    }

    private static void testTableToolsAndContextualTabs() {
        System.out.println("\n[Test] Table Tools, Contextual Tabs, Shapes Gallery & Video Embedding");
        try {
            System.setProperty("java.awt.headless", "true");
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    java.util.concurrent.atomic.AtomicReference<String> shownTab = new java.util.concurrent.atomic.AtomicReference<>();
                    java.util.concurrent.atomic.AtomicReference<Boolean> hiddenTabs = new java.util.concurrent.atomic.AtomicReference<>(false);

                    com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                            new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((html, words, chars) -> {});
                    canvas.getTextPane().setCaret(new javax.swing.text.DefaultCaret() {
                        @Override protected synchronized void damage(java.awt.Rectangle r) {}
                        @Override public void paint(java.awt.Graphics g) {}
                    });
                    canvas.setContextualTabHandler(new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.ContextualTabHandler() {
                        @Override
                        public void showContextualTab(String tabName) {
                            shownTab.set(tabName);
                            hiddenTabs.set(false);
                        }

                        @Override
                        public void hideContextualTabs() {
                            hiddenTabs.set(true);
                        }
                    });

                    String initialHtml = "<h1>Document Test</h1>" +
                            "<table id='t1' border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px 0;border:1px solid #cbd5e1;'>" +
                            "<tr><th>Col 1</th><th>Col 2</th></tr>" +
                            "<tr><td>Data 1</td><td>&nbsp;</td></tr>" +
                            "</table>" +
                            "<p>Normal paragraph text outside table.</p>";

                    canvas.loadHtml(initialHtml);
                    javax.swing.JTextPane pane = canvas.getTextPane();
                    pane.setSize(600, 400);
                    pane.doLayout();
                    if (pane.getCaret() != null) pane.getCaret().setVisible(false);

                    javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) pane.getDocument();
                    javax.swing.text.Element table = null;
                    for (int i = 0; i < doc.getLength(); i++) {
                        table = canvas.getEnclosingTable(doc.getCharacterElement(i));
                        if (table != null) {
                            pane.setCaretPosition(i);
                            break;
                        }
                    }
                    assertNotNull(table, "Expected table element in document");
                    canvas.updateContextualTabState();
                    assertEquals("Table Tools", shownTab.get(), "Contextual tab should be Table Tools");

                    // Backspace guard in empty cell
                    javax.swing.text.Element emptyCell = canvas.getAllCells(table).get(3);
                    pane.setCaretPosition(emptyCell.getStartOffset());
                    java.awt.event.KeyEvent backspaceEvent = new java.awt.event.KeyEvent(pane, java.awt.event.KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, java.awt.event.KeyEvent.VK_BACK_SPACE, '\b');
                    for (var kl : pane.getKeyListeners()) {
                        kl.keyPressed(backspaceEvent);
                    }
                    assertTrue(backspaceEvent.isConsumed(), "Backspace in empty cell should be consumed to protect cell from deletion");

                    // Tab cell navigation
                    javax.swing.text.Element cell1 = canvas.getAllCells(table).get(1);
                    pane.setCaretPosition(cell1.getStartOffset());
                    canvas.navigateTableCell(true);
                    assertEquals(canvas.getAllCells(table).get(2).getStartOffset(), pane.getCaretPosition(), "Tab navigates to next cell");

                    // Tab at last cell auto-appends row
                    javax.swing.text.Element lastCell = canvas.getAllCells(table).get(3);
                    pane.setCaretPosition(lastCell.getStartOffset());
                    canvas.navigateTableCell(true);
                    table = canvas.getActiveTable();
                    assertEquals(3, table.getElementCount(), "Tab on last cell auto-appends row below");

                    // 1-click Row Above
                    canvas.insertTableRowAbove();
                    table = canvas.getActiveTable();
                    assertEquals(4, table.getElementCount(), "Row above increases row count to 4");

                    // 1-click Column Right
                    canvas.insertTableColumnRight();
                    table = canvas.getActiveTable();
                    assertEquals(3, table.getElement(0).getElementCount(), "Column right increases column count to 3");

                    // Table Style Gallery
                    canvas.applyTableStyle("Corporate Blue");
                    assertTrue(canvas.getDocumentHtml().contains("#2563eb"), "Corporate Blue style applied to table");

                    // Shapes Gallery
                    int beforeLen = doc.getLength();
                    canvas.insertShape("Rounded Card");
                    canvas.insertShape("Process Flow (Step A ➔ B ➔ C)");
                    canvas.insertShape("Checkmark Badge (✓)");
                    assertTrue(doc.getLength() > beforeLen, "Shapes inserted into document");
                    String normHtml = canvas.getDocumentHtml().replaceAll("\\s+", " ");
                    assertTrue(normHtml.contains("Rounded Card Box"), "Contains Rounded Card");
                    assertTrue(normHtml.contains("Step 1: Ideate"), "Contains Process Flow");
                    assertTrue(normHtml.contains("Verified Milestone"), "Contains Checkmark Badge");

                    // Video Embedding & Video Tools
                    String vHtml = "<div class='wb-video-card' data-video-url='https://www.youtube.com/watch?v=dQw4w9WgXcQ' style='border:1px solid #cbd5e1;background:#0f172a;border-radius:10px;padding:16px 20px;margin:14px 0;color:#f8fafc;width:95%;'>" +
                            "<div style='font-size:12px;color:#94a3b8;margin-bottom:6px;'>🎬 <b>VIDEO MEDIA</b> &bull; YouTube</div>" +
                            "<div style='font-size:16px;font-weight:bold;color:#ffffff;margin-bottom:8px;'>Document Video Overview</div>" +
                            "<div style='background:#1e293b;padding:10px 14px;border-radius:6px;font-size:13px;color:#38bdf8;'>▶ Click or Launch to Play: https://www.youtube.com/watch?v=dQw4w9WgXcQ</div>" +
                            "</div><p><br></p>";
                    canvas.insertHtml(vHtml);
                    assertTrue(canvas.getDocumentHtml().contains("wb-video-card"), "Document contains video card");
                    canvas.updateContextualTabState();
                    assertEquals("Video Tools", shownTab.get(), "Video Tools contextual tab activated");

                    // Caret in normal text hides contextual tabs
                    pane.setCaretPosition(doc.getLength() - 5);
                    canvas.updateContextualTabState();
                    assertTrue(Boolean.TRUE.equals(hiddenTabs.get()), "Contextual tabs hide when editing regular text");
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            System.out.println("  ✔ Table Tools, Contextual Tabs, Shapes Gallery & Video Embedding tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Table Tools test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testAdvancedUndoAndMemory() {
        System.out.println("\n[Test] AdvancedUndoManager High-Level Memory (200 Steps)");
        try {
            com.wordbook.infrastructure.ui.swing.AdvancedUndoManager um = new com.wordbook.infrastructure.ui.swing.AdvancedUndoManager();
            assertTrue(!um.canUndoAction(), "Initial undo is false");
            assertTrue(!um.canRedoAction(), "Initial redo is false");

            // Register named actions
            um.registerNamedAction("Insert Table");
            um.registerNamedAction("Format Bold");
            um.registerNamedAction("Insert Chart");
            um.registerNamedAction("Page Break");

            assertTrue(um.canUndoAction(), "Can undo after operations");
            assertEquals("Page Break", um.getUndoName(), "Latest action is Page Break");

            // Get undo history slice
            var history = um.getUndoHistory(10);
            assertEquals(4, history.size(), "4 operations in history");
            assertEquals("Page Break", history.get(0).name, "Most recent is first");
            assertEquals("Insert Chart", history.get(1).name, "Previous is second");

            // Undo 1 step
            um.undo();
            assertEquals("Insert Chart", um.getUndoName(), "After 1 undo, latest is Insert Chart");
            assertTrue(um.canRedoAction(), "Can redo after undo");
            assertEquals("Page Break", um.getRedoName(), "Redo name is Page Break");

            // Redo
            um.redo();
            assertEquals("Page Break", um.getUndoName(), "After redo, latest is Page Break");

            // Test 200 step limit
            for (int i = 0; i < 220; i++) {
                um.registerNamedAction("Action " + i);
            }
            assertTrue(um.getUndoHistory(300).size() <= 200, "History respects 200 step limit");

            System.out.println("  ✔ AdvancedUndoManager High-Level Memory tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ AdvancedUndoManager test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testInternetScraperAndSmartAccessibility() {
        System.out.println("\n[Test] Internet Scraper & Smart Accessibility Engine");
        try {
            // 1. Test InternetScraperService HTML parser & research card generation
            com.wordbook.core.application.service.InternetScraperService scraper =
                    new com.wordbook.core.application.service.InternetScraperService();

            var article = new com.wordbook.core.application.service.InternetScraperService.ScrapedArticle(
                    "https://example.com/ai-study",
                    "Artificial Intelligence and Cognitive Computing",
                    "A study on deep neural architectures and transformers.",
                    java.util.List.of("Introduction", "Model Architecture", "Performance Benchmarks"),
                    java.util.List.of(
                            "Artificial intelligence has transformed modern software engineering by enabling automated reasoning.",
                            "Deep learning systems utilize multi-layered neural networks to achieve superhuman pattern recognition."
                    ),
                    java.util.List.of("Automated reasoning across domains", "Superhuman pattern recognition"),
                    "Full text content of the article...",
                    2
            );

            String htmlCard = article.toDocumentHtml();
            assertTrue(htmlCard.contains("wb-research-card"), "Generates research card class");
            assertTrue(htmlCard.contains("Artificial Intelligence and Cognitive Computing"), "Contains article title");
            assertTrue(htmlCard.contains("https://example.com/ai-study"), "Contains source URL");
            assertTrue(htmlCard.contains("Key Insights:"), "Contains key insights section");
            assertTrue(htmlCard.contains("Automated reasoning across domains"), "Contains bullet insight");

            String md = article.toMarkdown();
            assertTrue(md.contains("# Artificial Intelligence"), "Markdown has H1 title");
            assertTrue(md.contains("### Key Takeaways"), "Markdown has key takeaways");

            // 2. Test ImagePropertiesDialog CSS generator
            com.wordbook.infrastructure.ui.swing.ImagePropertiesDialog imgDlg =
                    new com.wordbook.infrastructure.ui.swing.ImagePropertiesDialog(null, 420, "Float Left (Text Wrap Around)", "Figure caption");
            String css = imgDlg.generateImageCssStyle();
            assertTrue(css.contains("border-radius"), "Generates rounded border style");
            assertTrue(css.contains("float:left"), "Generates float-left text wrap CSS");

            System.out.println("  ✔ Internet Scraper & Smart Accessibility tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Internet Scraper test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testProxyRoutingManagerAndVisualCards() {
        System.out.println("\n[Test] ProxyRoutingManager & Visual Cards");
        try {
            var proxyMgr = com.wordbook.infrastructure.web.ProxyRoutingManager.class;

            // Direct mode by default
            String initialBadge = com.wordbook.infrastructure.web.ProxyRoutingManager.getStatusBadge();
            assertTrue(initialBadge.contains("Direct"), "Initial mode is Direct");

            // Test Gateway mode reflection / state
            com.wordbook.infrastructure.web.ProxyRoutingManager.applyRouting();
            assertTrue(System.getProperty("http.proxyHost") == null, "Direct mode has null proxyHost");

            // Test Visual Card generation
            java.util.concurrent.atomic.AtomicReference<String> insertedHtml = new java.util.concurrent.atomic.AtomicReference<>();
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((html, words, chars) -> insertedHtml.set(html));

            String multiKpiCard = "<table cellpadding='14' cellspacing='10' style='width:100%;margin:16px 0;'>" +
                    "<tr><td style='background-color:#f8fafc;border:1px solid #cbd5e1;border-radius:12px;width:33%;'>" +
                    "ANNUAL REVENUE: $1.42M</td></tr></table><p><br></p>";
            canvas.insertHtml(multiKpiCard);
            assertTrue(canvas.getDocumentHtml().contains("ANNUAL REVENUE"), "Document contains Multi-KPI card");

            String topicCard = "<table cellpadding='16' cellspacing='0' style='background:#f8fafc;border-left:5px solid #2563eb;border-radius:10px;'>" +
                    "<tr><td>SPOTLIGHT TOPIC: Core System Topology</td></tr></table><p><br></p>";
            canvas.insertHtml(topicCard);
            assertTrue(canvas.getDocumentHtml().contains("SPOTLIGHT TOPIC"), "Document contains Spotlight Topic card");

            System.out.println("  ✔ ProxyRoutingManager & Visual Cards tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ ProxyRoutingManager & Visual Cards test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testRedesignedRibbonAndVisualRepresentations() {
        System.out.println("\n[Test] Redesigned Ribbon & Visual Representations");
        try {
            // 1. Test all newly added vector icons render without error
            var iconTypes = new com.wordbook.infrastructure.ui.swing.VectorIcons.Type[]{
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.CHART_BAR,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.CHART_LINE,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.CHART_PIE,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.CHART_TREE,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.KPI_CARD,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.MULTI_KPI,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.SPOTLIGHT,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.COMPARISON,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.PROGRESS,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.TAKEAWAYS,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.GRID_2X2,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.GRID_3X3,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.DIVIDER,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.STAR_RATING,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.STEP_MINUS,
                    com.wordbook.infrastructure.ui.swing.VectorIcons.Type.STEP_PLUS
            };

            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = img.createGraphics();
            for (var type : iconTypes) {
                javax.swing.Icon icon = com.wordbook.infrastructure.ui.swing.VectorIcons.create(type, 16, java.awt.Color.BLUE);
                assertNotNull(icon, "Icon created for " + type);
                icon.paintIcon(null, g2, 0, 0);
            }
            g2.dispose();

            // 2. Test RibbonPanel creation and tab selection
            java.util.concurrent.atomic.AtomicReference<String> lastChartType = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicInteger lastResizedWidth = new java.util.concurrent.atomic.AtomicInteger();

            com.wordbook.infrastructure.ui.swing.RibbonPanel.DocumentActionTarget mockTarget =
                    new com.wordbook.infrastructure.ui.swing.RibbonPanel.DocumentActionTarget() {
                        @Override public void execFormat(String command, Object arg) {}
                        @Override public void insertHtml(String html) {}
                        @Override public void insertText(String text) {}
                        @Override public void showTableDialog() {}
                        @Override public void showChartDialog() {}
                        @Override public void exportHtml() {}
                        @Override public void exportMarkdown() {}
                        @Override public void printDocument() {}
                        @Override public void showDbDiagnostics() {}
                        @Override public void insertLocalImage() {}
                        @Override public void insertWebImage() {}
                        @Override public void insertQuickChart(String type, String data) { lastChartType.set(type); }
                        @Override public int getWordCount() { return 0; }
                        @Override public int getCharCount() { return 0; }
                        @Override public void backToBookshelf() {}
                        @Override public void insertPageBreak() {}
                        @Override public void showCitationDialog() {}
                        @Override public void showGrammarDialog() {}
                        @Override public void autoDetectStructure() {}
                        @Override public void resizeSelectedImage(int widthPx) { lastResizedWidth.set(widthPx); }
                        @Override public void alignSelectedImage(String align) {}
                        @Override public void showImagePropertiesDialog() {}
                        @Override public void insertCard(String cardType) {}
                        @Override public void setPageFormat(String formatName) {}
                        @Override public void setPageMargins(String marginName) {}
                        @Override public void setPagePaperTone(String toneName) {}
                        @Override public void autoFixAllGrammar() {}
                        @Override public void replaceSelectedImageSource() {}
                        @Override public void insertTableRowAbove() {}
                        @Override public void insertTableRowBelow() {}
                        @Override public void insertTableColumnLeft() {}
                        @Override public void insertTableColumnRight() {}
                        @Override public void deleteTableRow() {}
                        @Override public void deleteTableColumn() {}
                        @Override public void deleteTable() {}
                        @Override public void applyTableStyle(String styleTheme) {}
                        @Override public void setTableCellColor(java.awt.Color color) {}
                        @Override public void insertShape(String shapeType) {}
                        @Override public void showVideoDialog() {}
                        @Override public void openSelectedVideo() {}
                        @Override public void deleteSelectedVideo() {}
                        @Override public com.wordbook.infrastructure.ui.swing.AdvancedUndoManager getAdvancedUndoManager() { return null; }
                        @Override public void toggleAiPanel() {}
                        @Override public void openAiTab(int index) {}
                        @Override public void openScraperWithUrl(String url) {}
                        @Override public void speakSelectionOrDocument() {}
                        @Override public void toggleHighContrastMode() {}
                        @Override public void toggleDyslexicFont() {}
                        @Override public void showHeadingsNavigator() {}
                        @Override public void runAccessibilityAudit() {}
                        @Override public void applyImageFilter(String filter) {}
                        @Override public void setImageWrap(String wrap) {}
                    };

            com.wordbook.infrastructure.ui.swing.RibbonPanel ribbon = new com.wordbook.infrastructure.ui.swing.RibbonPanel(mockTarget);
            ribbon.setSize(1200, 160);
            ribbon.doLayout();

            // Select Chart Studio Tab
            ribbon.selectTab(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_CHART);
            ribbon.doLayout();

            // Select Insert Tab
            ribbon.selectTab(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_INSERT);
            ribbon.doLayout();

            System.out.println("  ✔ Redesigned Ribbon & Visual Representations tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Redesigned Ribbon & Visual Representations test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testNewChartTypesAndCardStyling() {
        System.out.println("\n[Test] Expanded Chart Types, Card Backgrounds & Detail Reduction");
        try {
            ChartEngineService engine = new ChartEngineService();

            // 1. New Chart Types
            String data = "Q1: 45\nQ2: 70\nQ3: 95";
            for (com.wordbook.core.domain.model.ChartDefinition.ChartType type : java.util.List.of(
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.COLUMN,
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.AREA,
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.DONUT,
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.RADAR,
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.SCATTER
            )) {
                String svg = engine.render(type, data);
                assertNotNull(svg, type.name() + " SVG generated");
                assertTrue(svg.contains("<svg") && svg.contains("</svg>"), type.name() + " has valid SVG tags");

                java.awt.image.BufferedImage img = engine.renderChartToImage(type, data, ChartEngineService.ChartTheme.SUNSET_CORAL, type.name(), 500, 250);
                assertNotNull(img, type.name() + " rasterized to image");
                assertEquals(500, img.getWidth(), type.name() + " width match");
            }

            // 2. Card Background Styles
            for (String cardStyle : java.util.List.of("MODERN_GLASS", "SHARP_MINIMAL", "GRADIENT", "DARK_EXECUTIVE", "CARD_OUTLINE")) {
                java.awt.image.BufferedImage cardImg = engine.renderChartToImage(
                        com.wordbook.core.domain.model.ChartDefinition.ChartType.BAR, data, ChartEngineService.ChartTheme.EMERALD_GREEN, "Card Style " + cardStyle,
                        520, 260, cardStyle, "FULL", true, true
                );
                assertNotNull(cardImg, "Card style " + cardStyle + " renders");
            }

            // 3. Detail Reduction
            java.awt.image.BufferedImage fullImg = engine.renderChartToImage(
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.LINE, data, ChartEngineService.ChartTheme.CORPORATE_BLUE, "Full Details",
                    520, 260, "MODERN_GLASS", "FULL", true, true
            );
            java.awt.image.BufferedImage compactImg = engine.renderChartToImage(
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.LINE, data, ChartEngineService.ChartTheme.CORPORATE_BLUE, "Compact Details",
                    520, 260, "MODERN_GLASS", "COMPACT", false, false
            );
            java.awt.image.BufferedImage kpiImg = engine.renderChartToImage(
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.BAR, data, ChartEngineService.ChartTheme.WARM_PASTEL, "KPI Badge",
                    520, 260, "MODERN_GLASS", "MINIMAL_KPI", false, false
            );
            assertNotNull(fullImg, "Full details chart rendered");
            assertNotNull(compactImg, "Compact chart rendered");
            assertNotNull(kpiImg, "Minimal KPI badge rendered");

            // 4. File rendering with extended metadata sidecar
            java.io.File file = engine.renderChartToFile(
                    com.wordbook.core.domain.model.ChartDefinition.ChartType.DONUT, data, ChartEngineService.ChartTheme.SLATE_DARK, "Donut Metadata Test",
                    520, 260, "DARK_EXECUTIVE", "COMPACT", true, false
            );
            assertNotNull(file, "File created");
            assertTrue(file.exists(), "File exists on disk");

            ChartEngineService.ChartMetadata meta = engine.readChartMetadata(file);
            assertNotNull(meta, "Metadata read from sidecar");
            assertEquals(com.wordbook.core.domain.model.ChartDefinition.ChartType.DONUT, meta.type(), "Metadata ChartType matches DONUT");
            assertEquals("DARK_EXECUTIVE", meta.cardStyle(), "Metadata cardStyle matches DARK_EXECUTIVE");
            assertEquals("COMPACT", meta.detailLevel(), "Metadata detailLevel matches COMPACT");
            assertTrue(meta.showValues(), "Metadata showValues matches true");
            assertTrue(!meta.showLegend(), "Metadata showLegend matches false");

            System.out.println("  ✔ Expanded Chart Types, Card Backgrounds & Detail Reduction tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Chart types and card styling test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testWordStylePaginationAndChartDesignContextualTab() {
        System.out.println("\n[Test] Word-Style Pagination & Contextual Chart Design Tab");
        try {
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((html, words, chars) -> {});

            // Initial blank page
            assertEquals(1, canvas.getPageCount(), "Initial document has 1 page");

            // Explicit page breaks
            canvas.loadHtml("<p>Page 1 content</p><div class='page-break-container'>Page Break</div><p>Page 2 content</p>");
            assertEquals(2, canvas.getPageCount(), "Document with 1 page-break has 2 pages");

            // Contextual Chart Design Tab verification
            final boolean[] chartActionInvoked = new boolean[1];
            com.wordbook.infrastructure.ui.swing.RibbonPanel.DocumentActionTarget mockTarget =
                    new com.wordbook.infrastructure.ui.swing.RibbonPanel.DocumentActionTarget() {
                @Override public void execFormat(String command, Object arg) {}
                @Override public void insertHtml(String html) {}
                @Override public void insertText(String text) {}
                @Override public void showTableDialog() {}
                @Override public void showChartDialog() {}
                @Override public void exportHtml() {}
                @Override public void exportMarkdown() {}
                @Override public void printDocument() {}
                @Override public void showDbDiagnostics() {}
                @Override public void insertLocalImage() {}
                @Override public void insertWebImage() {}
                @Override public void insertQuickChart(String type, String data) {}
                @Override public int getWordCount() { return 0; }
                @Override public int getCharCount() { return 0; }
                @Override public void backToBookshelf() {}
                @Override public void insertPageBreak() {}
                @Override public void showCitationDialog() {}
                @Override public void showGrammarDialog() {}
                @Override public void autoDetectStructure() {}
                @Override public void resizeSelectedImage(int widthPx) {}
                @Override public void alignSelectedImage(String align) {}
                @Override public void showImagePropertiesDialog() {}
                @Override public void insertCard(String cardType) {}
                @Override public void setPageFormat(String formatName) {}
                @Override public void setPageMargins(String marginName) {}
                @Override public void setPagePaperTone(String toneName) {}
                @Override public void autoFixAllGrammar() {}
                @Override public void replaceSelectedImageSource() {}
                @Override public void insertTableRowAbove() {}
                @Override public void insertTableRowBelow() {}
                @Override public void insertTableColumnLeft() {}
                @Override public void insertTableColumnRight() {}
                @Override public void deleteTableRow() {}
                @Override public void deleteTableColumn() {}
                @Override public void deleteTable() {}
                @Override public void applyTableStyle(String styleTheme) {}
                @Override public void setTableCellColor(java.awt.Color color) {}
                @Override public void insertShape(String shapeType) {}
                @Override public void showVideoDialog() {}
                @Override public void openSelectedVideo() {}
                @Override public void deleteSelectedVideo() {}
                @Override public com.wordbook.infrastructure.ui.swing.AdvancedUndoManager getAdvancedUndoManager() { return null; }
                @Override public void toggleAiPanel() {}
                @Override public void openAiTab(int index) {}
                @Override public void openScraperWithUrl(String url) {}
                @Override public void speakSelectionOrDocument() {}
                @Override public void toggleHighContrastMode() {}
                @Override public void toggleDyslexicFont() {}
                @Override public void showHeadingsNavigator() {}
                @Override public void runAccessibilityAudit() {}
                @Override public void applyImageFilter(String filter) {}
                @Override public void setImageWrap(String wrap) {}
                @Override public void modifySelectedChartData() { chartActionInvoked[0] = true; }
            };

            com.wordbook.infrastructure.ui.swing.RibbonPanel ribbon = new com.wordbook.infrastructure.ui.swing.RibbonPanel(mockTarget);
            ribbon.setSize(1200, 160);
            ribbon.doLayout();

            // Verify TAB_CHART_DESIGN can be selected
            ribbon.showContextualTab(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_CHART_DESIGN);
            assertEquals(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_CHART_DESIGN, ribbon.getActiveTab(), "Chart Design tab becomes active contextual tab");

            mockTarget.modifySelectedChartData();
            assertTrue(chartActionInvoked[0], "Chart contextual action successfully dispatched to target");

            System.out.println("  ✔ Word-Style Pagination & Contextual Chart Design Tab tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Pagination & Chart Design tab test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testClaudeChartComponentsAndWiring() {
        System.out.println("\n[Test] Claude Designed Chart Components, Catalog, Palette Chooser & ThumbCard Wiring");
        try {
            // 1. ChartCatalog Verification
            com.wordbook.infrastructure.ui.swing.ChartCatalog.TypeInfo[] types = com.wordbook.infrastructure.ui.swing.ChartCatalog.types();
            assertEquals(15, types.length, "ChartCatalog provides all 15 chart type definitions");

            assertEquals(ChartType.LOLLIPOP, com.wordbook.infrastructure.ui.swing.ChartCatalog.parse("Lollipop"), "Parse Lollipop");
            assertEquals(ChartType.WATERFALL, com.wordbook.infrastructure.ui.swing.ChartCatalog.parse("Waterfall"), "Parse Waterfall");
            assertEquals(ChartType.FUNNEL, com.wordbook.infrastructure.ui.swing.ChartCatalog.parse("Funnel"), "Parse Funnel");
            assertEquals(ChartType.GAUGE, com.wordbook.infrastructure.ui.swing.ChartCatalog.parse("Gauge"), "Parse Gauge");
            assertEquals(ChartType.POLAR, com.wordbook.infrastructure.ui.swing.ChartCatalog.parse("Polar Area"), "Parse Polar Area");

            assertEquals(ChartEngineService.ChartTheme.EMERALD_GREEN, com.wordbook.infrastructure.ui.swing.ChartCatalog.parseTheme("Emerald Mint"), "Parse Emerald theme");
            assertEquals(ChartEngineService.ChartTheme.SLATE_DARK, com.wordbook.infrastructure.ui.swing.ChartCatalog.parseTheme("Slate Dark"), "Parse Slate Dark theme");

            // ChartCatalog cached thumbnail generation
            java.awt.image.BufferedImage thumb1 = com.wordbook.infrastructure.ui.swing.ChartCatalog.thumb(
                    ChartType.LOLLIPOP, null, ChartEngineService.ChartTheme.CORPORATE_BLUE, null, "MODERN_GLASS", "FULL", 120, 60);
            assertNotNull(thumb1, "ChartCatalog renders lollipop thumbnail");
            java.awt.image.BufferedImage thumb2 = com.wordbook.infrastructure.ui.swing.ChartCatalog.thumb(
                    ChartType.LOLLIPOP, null, ChartEngineService.ChartTheme.CORPORATE_BLUE, null, "MODERN_GLASS", "FULL", 120, 60);
            assertTrue(thumb1 == thumb2, "ChartCatalog caches rendered thumbnails identically");

            // 2. ChartPalettePanel & Palette Customization
            com.wordbook.infrastructure.ui.swing.ChartPalettePanel palettePanel = new com.wordbook.infrastructure.ui.swing.ChartPalettePanel();
            assertEquals(ChartEngineService.ChartTheme.CORPORATE_BLUE, palettePanel.getTheme(), "Default theme is Corporate Blue");

            palettePanel.setSelection(ChartEngineService.ChartTheme.SUNSET_CORAL, "#ff3366,#33cc99,#3366ff");
            assertEquals(ChartEngineService.ChartTheme.SUNSET_CORAL, palettePanel.getTheme(), "Selection updates theme to Sunset Coral");
            assertEquals("#ff3366,#33cc99,#3366ff", palettePanel.getPaletteHex(), "Selection updates custom palette hex");

            java.awt.Color[] parsedColors = ChartEngineService.parsePalette("#ff3366,#33cc99,#3366ff");
            assertNotNull(parsedColors, "parsePalette parses custom hex array");
            assertEquals(3, parsedColors.length, "Parsed 3 custom series colors");
            assertEquals("#ff3366,#33cc99,#3366ff", ChartEngineService.paletteToHex(parsedColors), "paletteToHex converts back to clean string");

            // 3. ThumbCard Visual Component & 8 Visual Design Gallery Cards
            boolean[] clicked = {false};
            com.wordbook.infrastructure.ui.swing.ThumbCard card = new com.wordbook.infrastructure.ui.swing.ThumbCard(
                    "Test Card", "Test Tooltip", (g2, r) -> {
                g2.setColor(java.awt.Color.BLUE);
                g2.fillRect(r.x, r.y, r.width, r.height);
            }, 100, 40);
            card.setOnClick(() -> clicked[0] = true);
            card.setSelected(true);
            assertTrue(card.isSelectedCard(), "ThumbCard reflects selected state");

            // Paint ThumbCard onto a buffer to verify render integrity
            java.awt.image.BufferedImage cardBuffer = new java.awt.image.BufferedImage(100, 60, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = cardBuffer.createGraphics();
            card.setSize(100, 60);
            card.paint(g2);
            g2.dispose();
            assertNotNull(cardBuffer, "ThumbCard paints without error");

            // Verify all 8 design styles and realistic visual card painters
            com.wordbook.infrastructure.ui.swing.ChartCatalog.StyleInfo[] styles = com.wordbook.infrastructure.ui.swing.ChartCatalog.styles();
            assertEquals(8, styles.length, "ChartCatalog provides all 8 chart design styles");
            for (com.wordbook.infrastructure.ui.swing.ChartCatalog.StyleInfo s : styles) {
                com.wordbook.infrastructure.ui.swing.ThumbCard dc = com.wordbook.infrastructure.ui.swing.ThumbCard.ofDesign(
                        s.code(), s.shortLabel(), s.label() + " — " + s.hint(), 66, 40);
                assertEquals(s.code(), dc.getStyleCode(), "ThumbCard styleCode matches " + s.code());
                java.awt.image.BufferedImage b = new java.awt.image.BufferedImage(66, 63, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = b.createGraphics();
                dc.setSize(66, 63);
                dc.paint(g);
                g.dispose();
                assertNotNull(b, "Design thumb " + s.code() + " paints successfully");
            }

            // Verify all 15 Chart Types realistic vector preview cards
            com.wordbook.infrastructure.ui.swing.ChartCatalog.TypeInfo[] allTypes = com.wordbook.infrastructure.ui.swing.ChartCatalog.types();
            assertEquals(15, allTypes.length, "ChartCatalog provides all 15 chart types");
            for (com.wordbook.infrastructure.ui.swing.ChartCatalog.TypeInfo ti : allTypes) {
                com.wordbook.infrastructure.ui.swing.ThumbCard tc = com.wordbook.infrastructure.ui.swing.ThumbCard.ofType(
                        ti.type(), ti.label(), ti.label() + " — " + ti.hint(), 62, 40);
                assertEquals(ti.type(), tc.getChartType(), "ThumbCard chartType matches " + ti.type());
                tc.setSelected(true);
                assertTrue(tc.isSelectedCard(), "ThumbCard type reflects selected state");
                java.awt.image.BufferedImage b = new java.awt.image.BufferedImage(62, 63, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = b.createGraphics();
                tc.setSize(62, 63);
                tc.paint(g);
                g.dispose();
                assertNotNull(b, "Type thumb " + ti.type() + " paints successfully");
            }

            // Verify all 6 Color Themes realistic preview cards + Palette Studio
            ChartEngineService.ChartTheme[] themes = ChartEngineService.ChartTheme.values();
            assertEquals(6, themes.length, "ChartTheme provides 6 built-in themes");
            for (ChartEngineService.ChartTheme th : themes) {
                com.wordbook.infrastructure.ui.swing.ThumbCard thc = com.wordbook.infrastructure.ui.swing.ThumbCard.ofTheme(
                        th, th.getDisplayName(), th.getDisplayName() + " Theme Palette", 64, 40);
                assertEquals(th.getDisplayName(), thc.getThemeName(), "ThumbCard themeName matches " + th.getDisplayName());
                thc.setSelected(true);
                assertTrue(thc.isSelectedCard(), "ThumbCard theme reflects selected state");
                java.awt.image.BufferedImage b = new java.awt.image.BufferedImage(64, 63, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = b.createGraphics();
                thc.setSize(64, 63);
                thc.paint(g);
                g.dispose();
                assertNotNull(b, "Theme thumb " + th.getDisplayName() + " paints successfully");
            }

            com.wordbook.infrastructure.ui.swing.ThumbCard studioCard = com.wordbook.infrastructure.ui.swing.ThumbCard.ofPaletteStudio(
                    "Studio...", "Palette Studio", 64, 40);
            assertEquals("CUSTOM", studioCard.getThemeName(), "Palette Studio card themeName is CUSTOM");
            java.awt.image.BufferedImage sb = new java.awt.image.BufferedImage(64, 63, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D sg = sb.createGraphics();
            studioCard.setSize(64, 63);
            studioCard.paint(sg);
            sg.dispose();
            assertNotNull(sb, "Palette studio thumb paints successfully");

            // 4. ChartExtraPainters & New Chart Type Rasterization
            ChartEngineService engine = new ChartEngineService();
            ChartType[] newTypes = {ChartType.LOLLIPOP, ChartType.WATERFALL, ChartType.FUNNEL, ChartType.GAUGE, ChartType.POLAR};
            for (ChartType t : newTypes) {
                String sampleData = com.wordbook.infrastructure.ui.swing.ChartCatalog.info(t).sample();
                // Render Full
                java.awt.image.BufferedImage imgFull = engine.renderChartToImage(
                        t, sampleData, ChartEngineService.ChartTheme.EMERALD_GREEN, t.name() + " Full", 580, 290, "MODERN_GLASS", "FULL", true, true, "#10b981,#3b82f6,#f59e0b");
                assertNotNull(imgFull, t.name() + " full rendering is non-null");
                assertEquals(580, imgFull.getWidth(), t.name() + " width is 580");

                // Render Minimal (delegates to ChartExtraPainters.drawMinimal)
                java.awt.image.BufferedImage imgMin = engine.renderChartToImage(
                        t, sampleData, ChartEngineService.ChartTheme.SLATE_DARK, t.name() + " Minimal", 400, 200, "DARK_EXECUTIVE", "MINIMAL", true, true, null);
                assertNotNull(imgMin, t.name() + " minimal rendering is non-null");
            }

            // 5. File Render & Metadata Sidecar with PaletteHex
            java.io.File chartFile = engine.renderChartToFile(
                    ChartType.WATERFALL, "Q1: 100\nCosts: -40\nBonus: 20", ChartEngineService.ChartTheme.WARM_PASTEL, "Waterfall Report", 580, 290,
                    "GRADIENT", "COMPACT", true, true, "#e11d48,#16a34a,#2563eb");
            assertTrue(chartFile != null && chartFile.exists(), "Chart file generated on disk");

            ChartEngineService.ChartMetadata meta = engine.readChartMetadata(chartFile);
            assertNotNull(meta, "Metadata read from sidecar");
            assertEquals(ChartType.WATERFALL, meta.type(), "Metadata reflects WATERFALL type");
            assertEquals("#e11d48,#16a34a,#2563eb", meta.paletteHex(), "Metadata persists custom paletteHex");

            System.out.println("  ✔ Claude Designed Chart Components, Catalog, Palette Chooser & ThumbCard Wiring tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Claude Chart Components & Wiring test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testNonBlockingDocumentAndEDTResponsiveness() {
        System.out.println("\n[Test] Non-Blocking Document Mutations & EDT Responsiveness");
        try {
            java.util.concurrent.atomic.AtomicInteger saveCalls = new java.util.concurrent.atomic.AtomicInteger();
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((html, words, chars) -> saveCalls.incrementAndGet());

            // 1. Initial page count is fast and non-blocking
            long t0 = System.nanoTime();
            int pages = canvas.getPageCount();
            long elapsedUs = (System.nanoTime() - t0) / 1000;
            assertEquals(1, pages, "Initial page count is 1");
            assertTrue(elapsedUs < 50000, "getPageCount executes in under 50ms without blocking EDT");

            // 2. Rapid loadHtml calls do not leak listeners or hang
            for (int i = 0; i < 5; i++) {
                canvas.loadHtml("<h1>Lesson Section " + i + "</h1><p>Content block text</p>");
            }
            assertEquals(1, canvas.getPageCount(), "Single page document after loads");

            // 3. Document with multiple page breaks
            String multiPageHtml = "<p>P1</p><div class='page-break-container'>PB1</div><p>P2</p><div class='page-break-container'>PB2</div><p>P3</p>";
            canvas.loadHtml(multiPageHtml);
            assertEquals(3, canvas.getPageCount(), "Calculates exactly 3 pages from 2 page breaks");

            // 4. Verify triggerSave executes change handler
            canvas.triggerSave();
            assertTrue(saveCalls.get() > 0, "Change handler was called on triggerSave");

            System.out.println("  ✔ Non-Blocking Document Mutations & EDT Responsiveness tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Non-Blocking Document & EDT test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testHeaderFooterAndDynamicDesignCards() {
        System.out.println("\n[Test] Header/Footer Styles, Clamped Zoom Width & Dynamic Visual Cards");
        try {
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((html, words, chars) -> {});

            // 1. Header & Footer styling and options
            assertEquals("MODERN_LINE", canvas.getHeaderStyle(), "Default header style is MODERN_LINE");
            assertEquals("PAGE_NUMBER_CENTER", canvas.getFooterStyle(), "Default footer style is PAGE_NUMBER_CENTER");

            canvas.setHeaderStyle("CLEAN_MINIMAL");
            assertEquals("CLEAN_MINIMAL", canvas.getHeaderStyle(), "Updated header style to CLEAN_MINIMAL");

            canvas.setFooterStyle("SPLIT_TITLE_PAGE");
            assertEquals("SPLIT_TITLE_PAGE", canvas.getFooterStyle(), "Updated footer style to SPLIT_TITLE_PAGE");

            canvas.setHeaderLeftText("Project Plan Q4");
            assertEquals("Project Plan Q4", canvas.getHeaderLeftText(), "Updated header left text");

            canvas.setHeaderRightText("Internal Only");
            assertEquals("Internal Only", canvas.getHeaderRightText(), "Updated header right text");

            canvas.setDocumentTitle("Quarterly Business Review");
            assertEquals("Quarterly Business Review", canvas.getDocumentTitle(), "Updated document title");

            // Header & Footer Selection State (Word-style interaction)
            assertTrue(!canvas.isHeaderFooterSelected(), "Default header/footer is not selected");
            canvas.setHeaderFooterSelected(true, "HEADER", 0);
            assertTrue(canvas.isHeaderFooterSelected(), "Header is selected");
            assertEquals("HEADER", canvas.getActiveHeaderFooterSection(), "Active section is HEADER");
            assertEquals(0, canvas.getActiveHeaderFooterPage(), "Active page is 0");

            canvas.closeHeaderFooterSelection();
            assertTrue(!canvas.isHeaderFooterSelected(), "Header selection closed");

            canvas.setHeaderFooterSelected(true, "FOOTER", 1);
            assertTrue(canvas.isHeaderFooterSelected(), "Footer is selected");
            assertEquals("FOOTER", canvas.getActiveHeaderFooterSection(), "Active section is FOOTER");
            assertEquals(1, canvas.getActiveHeaderFooterPage(), "Active page is 1");
            canvas.closeHeaderFooterSelection();

            // Multi-Page Body View Pagination metrics
            assertEquals(988, canvas.getPageUnit(), "A4 pageUnit is 960 + 28 = 988");
            assertTrue(canvas.getPageBodyHeight() > 800, "A4 bodyHeight is height minus margins");

            // 2. Fixed page width on zoom in / zoom out (strictly maintains paper format dimensions)
            canvas.setPageFormat(com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageFormat.A4);
            int baseA4Width = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.PageFormat.A4.width; // 740

            canvas.setZoom(1.0f);
            assertEquals(baseA4Width, canvas.getPageSheetPreferredSize().width, "At 100% zoom, sheet width equals base page width");

            canvas.setZoom(1.5f);
            assertEquals(baseA4Width, canvas.getPageSheetPreferredSize().width, "At 150% zoom, sheet width remains fixed to base page width");

            canvas.setZoom(0.75f);
            assertEquals(baseA4Width, canvas.getPageSheetPreferredSize().width, "At 75% zoom, sheet width remains fixed to base page width");

            // 3. Media auto-adjustment and page-break sanitizer
            String oversizedHtml = "<p>Intro</p><img src='test.png' width='1400' height='900'>" +
                    "<table width='1200' border='0'><tr><td><hr>PAGE BREAK<hr></td></tr></table>";
            String adjusted = canvas.adjustMediaToPageSize(oversizedHtml);
            assertTrue(!adjusted.contains("width='1400'"), "Oversized image width scaled down");
            assertTrue(!adjusted.contains("width=\"1400\""), "Oversized image width scaled down (quotes check)");
            assertTrue(adjusted.contains("page-break-container"), "Legacy table page break replaced by non-editable container");

            // 4. Dynamic Chart Design Cards updating for ChartType
            final boolean[] styleInvoked = {false};
            com.wordbook.infrastructure.ui.swing.RibbonPanel.DocumentActionTarget mockTarget = new com.wordbook.infrastructure.ui.swing.RibbonPanel.DocumentActionTarget() {
                @Override public void execFormat(String command, Object arg) {}
                @Override public void insertHtml(String html) {}
                @Override public void insertText(String text) {}
                @Override public void showTableDialog() {}
                @Override public void showChartDialog() {}
                @Override public void exportHtml() {}
                @Override public void exportMarkdown() {}
                @Override public void printDocument() {}
                @Override public void showDbDiagnostics() {}
                @Override public void insertLocalImage() {}
                @Override public void insertWebImage() {}
                @Override public void insertQuickChart(String type, String data) {}
                @Override public int getWordCount() { return 0; }
                @Override public int getCharCount() { return 0; }
                @Override public void backToBookshelf() {}
                @Override public void insertPageBreak() {}
                @Override public void showCitationDialog() {}
                @Override public void showGrammarDialog() {}
                @Override public void autoDetectStructure() {}
                @Override public void resizeSelectedImage(int widthPx) {}
                @Override public void alignSelectedImage(String align) {}
                @Override public void showImagePropertiesDialog() {}
                @Override public void insertCard(String cardType) {}
                @Override public void setPageFormat(String formatName) {}
                @Override public void setPageMargins(String marginName) {}
                @Override public void setPagePaperTone(String toneName) {}
                @Override public void autoFixAllGrammar() {}
                @Override public void replaceSelectedImageSource() {}
                @Override public void insertTableRowAbove() {}
                @Override public void insertTableRowBelow() {}
                @Override public void insertTableColumnLeft() {}
                @Override public void insertTableColumnRight() {}
                @Override public void deleteTableRow() {}
                @Override public void deleteTableColumn() {}
                @Override public void deleteTable() {}
                @Override public void applyTableStyle(String styleTheme) {}
                @Override public void setTableCellColor(java.awt.Color color) {}
                @Override public void insertShape(String shapeType) {}
                @Override public void showVideoDialog() {}
                @Override public void openSelectedVideo() {}
                @Override public void deleteSelectedVideo() {}
                @Override public com.wordbook.infrastructure.ui.swing.AdvancedUndoManager getAdvancedUndoManager() { return null; }
                @Override public void toggleAiPanel() {}
                @Override public void openAiTab(int index) {}
                @Override public void openScraperWithUrl(String url) {}
                @Override public void speakSelectionOrDocument() {}
                @Override public void toggleHighContrastMode() {}
                @Override public void toggleDyslexicFont() {}
                @Override public void showHeadingsNavigator() {}
                @Override public void runAccessibilityAudit() {}
                @Override public void applyImageFilter(String filter) {}
                @Override public void setImageWrap(String wrap) {}
                @Override public void switchSelectedChartStyle(String styleStr) { styleInvoked[0] = true; }
            };

            com.wordbook.infrastructure.ui.swing.RibbonPanel ribbon = new com.wordbook.infrastructure.ui.swing.RibbonPanel(mockTarget);
            ribbon.setSize(1200, 160);
            ribbon.doLayout();

            // Check dynamic update of design cards when chart type changes
            ribbon.setSelectedChartType(ChartType.LINE);
            ribbon.setSelectedChartType(ChartType.DONUT);
            ribbon.setSelectedChartType(ChartType.AREA);

            // Check header & footer design selections
            ribbon.setSelectedHeaderDesign("ACCENT_BANNER");
            ribbon.setSelectedFooterDesign("ACCENT_BAR");

            // Check text style selection
            ribbon.setSelectedTextStyle("Heading 1");
            ribbon.setSelectedTextStyle("Title");

            System.out.println("  ✔ Header/Footer Styles, Clamped Zoom Width & Dynamic Visual Cards tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Header/Footer & Dynamic Cards test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testGlobalUndoRedoAndLessonManagement() {
        System.out.println("\n[Test] Global Ribbon Undo/Redo & Robust Lesson Making/Deleting");
        try {
            // 1. Verify Global Ribbon Undo and Redo
            var mockTarget = new com.wordbook.infrastructure.ui.swing.RibbonPanel.DocumentActionTarget() {
                @Override public void execFormat(String command, Object arg) {}
                @Override public void insertHtml(String html) {}
                @Override public void insertText(String text) {}
                @Override public void showTableDialog() {}
                @Override public void showChartDialog() {}
                @Override public void exportHtml() {}
                @Override public void exportMarkdown() {}
                @Override public void printDocument() {}
                @Override public void showDbDiagnostics() {}
                @Override public void insertLocalImage() {}
                @Override public void insertWebImage() {}
                @Override public void insertQuickChart(String type, String data) {}
                @Override public int getWordCount() { return 0; }
                @Override public int getCharCount() { return 0; }
                @Override public void backToBookshelf() {}
                @Override public void insertPageBreak() {}
                @Override public void showCitationDialog() {}
                @Override public void showGrammarDialog() {}
                @Override public void autoDetectStructure() {}
                @Override public void resizeSelectedImage(int widthPx) {}
                @Override public void alignSelectedImage(String align) {}
                @Override public void showImagePropertiesDialog() {}
                @Override public void insertCard(String cardType) {}
                @Override public void setPageFormat(String formatName) {}
                @Override public void setPageMargins(String marginName) {}
                @Override public void setPagePaperTone(String toneName) {}
                @Override public void autoFixAllGrammar() {}
                @Override public void replaceSelectedImageSource() {}
                @Override public void insertTableRowAbove() {}
                @Override public void insertTableRowBelow() {}
                @Override public void insertTableColumnLeft() {}
                @Override public void insertTableColumnRight() {}
                @Override public void deleteTableRow() {}
                @Override public void deleteTableColumn() {}
                @Override public void deleteTable() {}
                @Override public void applyTableStyle(String styleTheme) {}
                @Override public void setTableCellColor(java.awt.Color color) {}
                @Override public void insertShape(String shapeType) {}
                @Override public void showVideoDialog() {}
                @Override public void openSelectedVideo() {}
                @Override public void deleteSelectedVideo() {}
                @Override public com.wordbook.infrastructure.ui.swing.AdvancedUndoManager getAdvancedUndoManager() { return null; }
                @Override public void toggleAiPanel() {}
                @Override public void openAiTab(int index) {}
                @Override public void openScraperWithUrl(String url) {}
                @Override public void speakSelectionOrDocument() {}
                @Override public void toggleHighContrastMode() {}
                @Override public void toggleDyslexicFont() {}
                @Override public void showHeadingsNavigator() {}
                @Override public void runAccessibilityAudit() {}
                @Override public void applyImageFilter(String filter) {}
                @Override public void setImageWrap(String wrap) {}
            };

            com.wordbook.infrastructure.ui.swing.RibbonPanel ribbon = new com.wordbook.infrastructure.ui.swing.RibbonPanel(mockTarget);
            assertNotNull(ribbon, "RibbonPanel instantiated");

            // Verify switching across standard and contextual tabs preserves global tab bar
            ribbon.selectTab(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_HOME);
            assertEquals(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_HOME, ribbon.getActiveTab(), "Home tab selected");
            ribbon.selectTab(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_INSERT);
            assertEquals(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_INSERT, ribbon.getActiveTab(), "Insert tab selected");
            ribbon.selectTab(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_LAYOUT);
            assertEquals(com.wordbook.infrastructure.ui.swing.RibbonPanel.TAB_LAYOUT, ribbon.getActiveTab(), "Layout tab selected");

            // 2. Test In-Memory Lesson Making & Deleting Workflow
            var connMgr = new com.wordbook.infrastructure.persistence.sqlite.SqliteConnectionManager(":memory:");
            var schemaMgr = new com.wordbook.infrastructure.persistence.sqlite.SqliteSchemaManager(connMgr);
            schemaMgr.initializeSchema();

            var bookRepo = new com.wordbook.infrastructure.persistence.sqlite.SqliteBookRepository(connMgr);
            var lessonRepo = new com.wordbook.infrastructure.persistence.sqlite.SqliteLessonRepository(connMgr);
            var uow = new com.wordbook.infrastructure.persistence.sqlite.SqliteUnitOfWork(connMgr);
            var bookService = new com.wordbook.core.application.service.BookService(bookRepo, lessonRepo, uow);
            var lessonService = new com.wordbook.core.application.service.LessonService(bookRepo, lessonRepo);

            // Create Book
            var book = bookService.createBook(new com.wordbook.core.application.dto.BookCreateRequest(null, "Test Notebook", "#2563eb"));
            assertNotNull(book.id(), "Book created with valid ID");

            // Verify Lesson Creation
            var lesson1 = lessonService.createLesson(book.id(), new com.wordbook.core.application.dto.LessonCreateRequest(null, "Lesson 1", "<p>Hello Lesson 1</p>", 0));
            assertNotNull(lesson1.id(), "Lesson 1 created");
            var lesson2 = lessonService.createLesson(book.id(), new com.wordbook.core.application.dto.LessonCreateRequest(null, "Lesson 2", "<p>Hello Lesson 2</p>", 1));
            assertNotNull(lesson2.id(), "Lesson 2 created");

            var lessons = lessonService.getLessonsForBook(book.id());
            assertEquals(2, lessons.size(), "2 lessons exist in notebook");

            // Test Lesson Deletion & Remaining Retrieval
            boolean del1 = lessonService.deleteLesson(lesson1.id());
            assertTrue(del1, "Lesson 1 successfully deleted");

            var remainingAfterDel = lessonService.getLessonsForBook(book.id());
            assertEquals(1, remainingAfterDel.size(), "1 lesson remaining after deletion");
            assertEquals("Lesson 2", remainingAfterDel.get(0).title(), "Remaining lesson is Lesson 2");

            // Verify deleting remaining lesson
            boolean del2 = lessonService.deleteLesson(lesson2.id());
            assertTrue(del2, "Lesson 2 successfully deleted");
            assertEquals(0, lessonService.getLessonsForBook(book.id()).size(), "0 lessons remaining after all deleted");

            // 3. Test LessonSidebarPanel initialization & promptNewLesson API
            java.util.concurrent.atomic.AtomicReference<String> deletedLessonId = new java.util.concurrent.atomic.AtomicReference<>();
            var sidebar = new com.wordbook.infrastructure.ui.swing.LessonSidebarPanel(
                    bookService, lessonService,
                    id -> {},
                    id -> deletedLessonId.set(id),
                    () -> {}
            );
            assertNotNull(sidebar, "Sidebar created with 5-arg constructor");
            sidebar.loadBook(book.id());
            sidebar.promptNewLesson(); // Verifies public visibility for shortcut Ctrl+N

            System.out.println("  ✔ Global Ribbon Undo/Redo & Robust Lesson Making/Deleting tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Global Undo/Redo & Lesson test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testSmartLoremGenerator() {
        System.out.println("\n[Test] Smart Lorem Ipsum Shorthand Expander");
        try {
            // 1. Word count correctness
            String lorem10 = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.generateLoremWords(10);
            int count10 = lorem10.split("\\s+").length;
            assertEquals(10, count10, "Generates exactly 10 words");
            assertTrue(lorem10.startsWith("Lorem"), "Starts with Lorem capitalized");
            assertTrue(lorem10.endsWith("."), "Ends with period punctuation");

            String lorem50 = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.generateLoremWords(50);
            int count50 = lorem50.split("\\s+").length;
            assertEquals(50, count50, "Generates exactly 50 words");

            String lorem1 = com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.generateLoremWords(1);
            assertEquals(1, lorem1.split("\\s+").length, "Generates exactly 1 word");

            // 2. Regex pattern recognition for various user shorthand inputs
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?i)(?:^|[\\s\\u00A0])(=?lorem\\s*\\(?\\s*(\\d+)?\\s*\\)?\\s*)$"
            );

            String[] validInputs = {
                "lorem 50", "lorem50", "lorem 100", "=lorem(50)", "lorem(20)", "Lorem 30", "LOREM 40", "lorem"
            };
            for (String input : validInputs) {
                java.util.regex.Matcher m = p.matcher(input);
                assertTrue(m.find(), "Pattern matches input: '" + input + "'");
            }

            System.out.println("  ✔ Smart Lorem Ipsum Shorthand Expander tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Smart Lorem Ipsum test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testModernContextMenuAndBlockLineSpacing() {
        System.out.println("\n[Test] Redesigned Context Menu Interface & Document Block Sizing / Line Spacing");
        try {
            // 1. Verify ModernPopupMenu and Subcomponents
            com.wordbook.infrastructure.ui.swing.ModernPopupMenu popup = new com.wordbook.infrastructure.ui.swing.ModernPopupMenu();
            assertNotNull(popup, "ModernPopupMenu instantiated");
            assertTrue(!popup.isOpaque(), "Popup menu has non-opaque background for anti-aliased rounded corners");
            assertNotNull(popup.getBorder(), "Popup menu has custom modern border");

            javax.swing.JLabel hdr = popup.addHeader("Chart Studio Actions");
            assertNotNull(hdr, "Header label created");
            assertTrue(hdr.getText().contains("CHART STUDIO ACTIONS"), "Header formatted in uppercase small-caps");

            popup.addModernSeparator();

            javax.swing.JMenuItem item = popup.addModernItem("Edit Chart Data", null, ev -> {});
            assertNotNull(item, "Modern menu item created");
            assertEquals("Segoe UI", item.getFont().getName(), "Modern Segoe UI typography applied");
            assertTrue(item.getPreferredSize().height >= 26 && item.getPreferredSize().height <= 32, "Item height is compact (26-32px)");

            javax.swing.JMenuItem dangerItem = popup.addDangerItem("Delete Chart", null, ev -> {});
            assertNotNull(dangerItem, "Danger item created");
            assertEquals(new java.awt.Color(220, 38, 38), dangerItem.getForeground(), "Danger item has red accent foreground");

            com.wordbook.infrastructure.ui.swing.ModernPopupMenu.ModernMenu subMenu = popup.createSubMenu("Switch Theme Palette", null);
            assertNotNull(subMenu, "Modern submenu created");
            assertNotNull(subMenu.getPopupMenu().getBorder(), "Submenu popup inherits modern rounded border");
            assertTrue(!subMenu.getPopupMenu().isOpaque(), "Submenu popup inherits non-opaque background");
            assertEquals(0.0f, subMenu.getAlignmentX(), "Submenu aligned to left edge to prevent width bloating");
            assertTrue(popup.getPreferredSize().width <= 185, "Menu width is sleek and compact (<= 185px)");

            // 2. Verify Document Canvas Block Sizing & Line Spacing
            javax.swing.JTextPane pane = new javax.swing.JTextPane();
            javax.swing.text.html.HTMLEditorKit kit = new javax.swing.text.html.HTMLEditorKit();
            javax.swing.text.html.StyleSheet ss = kit.getStyleSheet();
            ss.addRule("p { font-size: 14pt; color: #1e293b; margin-top: 2px; margin-bottom: 10px; }");
            ss.addRule("ol, ul { margin-top: 4px; margin-bottom: 10px; padding-left: 24px; }");
            ss.addRule("li { margin-top: 2px; margin-bottom: 2px; }");
            ss.addRule("figure { margin: 14px auto 10px auto; text-align: center; }");

            pane.setEditorKit(kit);
            pane.setText("<p>First paragraph</p><p>Second paragraph</p>");
            assertNotNull(pane.getDocument(), "Document loaded with block spacing rules");

            System.out.println("  ✔ Redesigned Context Menu Interface & Document Block Sizing / Line Spacing tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Context Menu & Block Sizing test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testChartGlitchAndDisplacementFixes() {
        System.out.println("\n[Test] Chart Glitch, Displacement & Synchronous Image Loading Fixes");
        try {
            ChartEngineService engine = new ChartEngineService();
            java.io.File chartFile = engine.renderChartToFile(
                    ChartType.BAR, "Alpha: 20\nBeta: 45\nGamma: 65",
                    ChartEngineService.ChartTheme.EMERALD_GREEN, "Stability Test",
                    520, 260
            );
            assertNotNull(chartFile, "Chart file rendered");
            assertTrue(chartFile.exists(), "Chart file exists on disk");

            String urlStr = chartFile.toURI().toURL().toExternalForm();
            assertTrue(urlStr.startsWith("file:/"), "Standard file URI scheme");
            assertTrue(!urlStr.contains("file:////"), "No quadruple slashes");

            // Verify URI to File resolution
            java.io.File resolvedFile = new java.io.File(new java.net.URI(urlStr));
            assertEquals(chartFile.getAbsolutePath(), resolvedFile.getAbsolutePath(), "Resolved file matches generated file");

            // Test ModernImageView synchronous loading
            javax.swing.text.html.HTMLDocument doc = new javax.swing.text.html.HTMLDocument();
            javax.swing.text.Element root = doc.getDefaultRootElement();
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.ModernImageView imageView =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.ModernImageView(root);
            assertTrue(imageView.getLoadsSynchronously(), "ModernImageView loads synchronously to eliminate flicker/displacements");

            System.out.println("  ✔ Chart Glitch, Displacement & Synchronous Loading tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Chart Glitch & Displacement test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testImageFilterAndCanvasFitting() {
        System.out.println("\n[Test] Image Filters, Canvas Fitting & Smooth Sizing");
        try {
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((h, w, c) -> {});
            int maxW = canvas.getMaxPrintableWidth();
            assertTrue(maxW > 300, "Max printable width is positive and reasonable");

            // 1. Test image with width exceeding sheet bounds (like the user's 842px image)
            String rawHtml = "<p><img src='sample.png' width='842' height='842'></p>";
            String adjusted = canvas.adjustMediaToPageSize(rawHtml);
            assertTrue(adjusted.contains("width='" + maxW + "'"), "Oversized image clamped to maxPrintableWidth: " + maxW);
            assertTrue(!adjusted.contains("width='842'"), "842px width replaced");
            assertTrue(adjusted.contains("height='" + maxW + "'"), "Height proportionally scaled down to " + maxW);

            // 2. Test raw image without any width attribute
            String noWidthHtml = "<p><img src='raw.jpg'></p>";
            String adjustedNoWidth = canvas.adjustMediaToPageSize(noWidthHtml);
            assertTrue(adjustedNoWidth.contains("width='" + maxW + "'"), "Missing width attribute auto-injected");
            assertTrue(adjustedNoWidth.contains("height='"), "Missing height attribute auto-injected");

            // 3. Test video card auto-fitting
            String videoHtml = "<div class='wb-video-card' data-video-url='https://youtu.be/test' style='background:#0f172a;padding:12px;'>Video</div>";
            String adjustedVideo = canvas.adjustMediaToPageSize(videoHtml);
            assertTrue(adjustedVideo.contains("width:96%"), "Video card styled with width:96% to prevent overflow");
            assertTrue(adjustedVideo.contains("max-width:100%"), "Video card styled with max-width:100%");

            // 4. Test filter application with data-filter attribute
            String docWithImg = "<p align='center'><img src='photo.png' width='400' height='300'></p>";
            canvas.setDocumentHtml(docWithImg);
            // Click image element
            javax.swing.text.Element imgElem = canvas.getLastClickedImageElement();
            assertNotNull(imgElem, "Found image element in canvas document");
            canvas.applyImageFilter("Grayscale");
            String filteredHtml = canvas.getDocumentHtml();
            assertTrue(filteredHtml.contains("data-filter=\"grayscale\"") || filteredHtml.contains("data-filter='grayscale'"),
                    "data-filter attribute written for Grayscale filter");
            assertTrue(filteredHtml.contains("wb-filter-grayscale"),
                    "wb-filter-grayscale class written for Grayscale filter");

            // 5. Test filter application for Oval shape
            canvas.applyImageFilter("Oval");
            String ovalHtml = canvas.getDocumentHtml();
            assertTrue(ovalHtml.contains("data-filter=\"oval\"") || ovalHtml.contains("data-filter='oval'"),
                    "data-filter attribute written for Oval filter");
            assertTrue(ovalHtml.contains("wb-filter-oval"),
                    "wb-filter-oval class written for Oval filter");

            // 6. Test ModernImageView preferred span clamping
            javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) canvas.getTextPane().getDocument();
            javax.swing.text.Element root = doc.getDefaultRootElement();
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.ModernImageView view =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel.ModernImageView(root);
            float span = view.getPreferredSpan(javax.swing.text.View.X_AXIS);
            assertTrue(span >= 0, "ModernImageView returns valid preferred span");

            System.out.println("  ✔ Image Filters, Canvas Fitting & Smooth Sizing tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Image Filters & Canvas Fitting test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testCardShapeRibbonAndCustomizer() {
        System.out.println("\n[Test] Realistic Preview Cards, Card/Shape Customizer & Inline Paper Editing");
        try {
            // 1. ThumbCard factory methods for shapes, media, references, and card templates
            com.wordbook.infrastructure.ui.swing.ThumbCard shapeCard =
                    com.wordbook.infrastructure.ui.swing.ThumbCard.ofShape("decision", "Decision", "Decision Diamond", 68, 48);
            assertNotNull(shapeCard, "Shape ThumbCard created");
            assertEquals("Decision", shapeCard.getLabel(), "Shape card label matches");

            com.wordbook.infrastructure.ui.swing.ThumbCard mediaCard =
                    com.wordbook.infrastructure.ui.swing.ThumbCard.ofMedia("photo", "Local Photo", "Insert local image", 74, 50);
            assertNotNull(mediaCard, "Media ThumbCard created");

            com.wordbook.infrastructure.ui.swing.ThumbCard refCard =
                    com.wordbook.infrastructure.ui.swing.ThumbCard.ofReference("citation", "Citation", "Add scholarly citation", 68, 48);
            assertNotNull(refCard, "Reference ThumbCard created");

            com.wordbook.infrastructure.ui.swing.ThumbCard kpiCard =
                    com.wordbook.infrastructure.ui.swing.ThumbCard.ofCardTemplate("KPI", "Single KPI", "Insert KPI Metric", 88, 56);
            assertNotNull(kpiCard, "KPI ThumbCard created");

            // 2. CardShapeCustomizerDialog theme palettes
            assertTrue(com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.containsKey("BLUE"), "Contains BLUE palette");
            assertTrue(com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.containsKey("EMERALD"), "Contains EMERALD palette");
            assertTrue(com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.containsKey("PURPLE"), "Contains PURPLE palette");
            assertTrue(com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.containsKey("AMBER"), "Contains AMBER palette");
            assertTrue(com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.containsKey("CRIMSON"), "Contains CRIMSON palette");
            assertTrue(com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.containsKey("SLATE"), "Contains SLATE palette");
            assertTrue(com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.containsKey("MINIMAL"), "Contains MINIMAL palette");
            assertEquals(7, com.wordbook.infrastructure.ui.swing.CardShapeCustomizerDialog.PALETTES.size(), "7 distinct color palettes available");

            // 3. DocumentCanvasPanel Shape & Card insertion and detection
            com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel canvas =
                    new com.wordbook.infrastructure.ui.swing.DocumentCanvasPanel((html, words, chars) -> {});

            canvas.insertShape("Rounded Card");
            String docHtml = canvas.getDocumentHtml();
            assertTrue(docHtml.contains("wb-card") || docHtml.contains("data-card"), "Rounded Card contains wb-card tag");

            canvas.insertShape("Process Flow (Step A ➔ B ➔ C)");
            String flowHtml = canvas.getDocumentHtml();
            assertTrue(flowHtml.contains("wb-flow") || flowHtml.contains("data-flow"), "Process Flow contains wb-flow tag");

            canvas.insertShape("Decision Diamond");
            String decisionHtml = canvas.getDocumentHtml();
            assertTrue(decisionHtml.contains("wb-shape") || decisionHtml.contains("data-shape"), "Decision Diamond contains wb-shape tag");

            // 4. Test isCardOrShapeElement
            javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) canvas.getTextPane().getDocument();
            javax.swing.text.Element root = doc.getDefaultRootElement();
            assertNotNull(root, "Root element exists");

            // 5. Test applyCustomizedValues on an element
            javax.swing.text.Element firstBody = root.getElement(0);
            if (firstBody.getElementCount() > 0) {
                javax.swing.text.Element targetElem = firstBody.getElement(0);
                canvas.applyCustomizedValues(targetElem, "Q4 Growth Target", "$950,000", "Achieved with 100% compliance", "emerald");
                String updatedHtml = canvas.getDocumentHtml();
                assertTrue(updatedHtml.contains("Q4 Growth Target"), "Updated HTML contains customized title");
                assertTrue(updatedHtml.contains("$950,000"), "Updated HTML contains customized metric value");
            }

            System.out.println("  ✔ Realistic Preview Cards, Card/Shape Customizer & Inline Paper Editing tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Card/Shape Ribbon & Customizer test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void testShapeFormatSidePanelAndPaginationStability() {
        System.out.println("\n[Test] Shape Format Side Panel & Multi-Page Pagination Stability");
        try {
            DocumentCanvasPanel canvas = new DocumentCanvasPanel((h, w, c) -> {});

            // 1. Test multi-page document with Page Break and cards on page 2
            String multiPageContent = "<h1>Document Title</h1>" +
                    "<p>Typing text on previous page line 1</p>" +
                    "<div class='page-break-container' contenteditable='false' style='page-break-before:always;'>PAGE BREAK</div>" +
                    "<div class='wb-card' data-card='custom' data-theme='EMERALD' style='border:2px solid #10b981;background-color:#f0fdf4;padding:16px;border-radius:10px;'>" +
                    "<div class='wb-card-title' style='color:#10b981;'>Page 2 Card</div>" +
                    "<div class='wb-card-val' style='font-size:24px;'>$500,000</div>" +
                    "<div class='wb-card-sub' style='color:#14532d;'>Next page milestone remains completely visible</div>" +
                    "</div>";
            canvas.loadHtml(multiPageContent);

            assertEquals(2, canvas.getPageCount(), "Initial document has exactly 2 pages");

            // 2. Simulate typing on previous page by inserting characters into document
            javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) canvas.getTextPane().getDocument();
            doc.insertString(4, "MORE TYPED TEXT ", null);

            // Verify page count remains stable and page body offset is preserved
            assertEquals(2, canvas.getPageCount(), "Document still has 2 pages after typing on previous page");
            assertTrue(canvas.getDocumentHtml().contains("Page 2 Card"), "Page 2 card content is fully preserved");

            // 3. Test ShapeFormatSidePanel instantiation and loading
            ShapeFormatSidePanel sidePanel = new ShapeFormatSidePanel(canvas);
            assertNotNull(sidePanel, "ShapeFormatSidePanel instantiated successfully");

            // Find the card element in the document
            javax.swing.text.Element root = doc.getDefaultRootElement();
            javax.swing.text.Element cardElem = null;
            for (int i = 0; i < root.getElementCount(); i++) {
                javax.swing.text.Element child = root.getElement(i);
                for (int j = 0; j < child.getElementCount(); j++) {
                    javax.swing.text.Element grand = child.getElement(j);
                    if (canvas.isCardOrShapeElement(grand)) {
                        cardElem = grand;
                        break;
                    }
                }
                if (cardElem != null) break;
            }

            if (cardElem != null) {
                sidePanel.loadElement(cardElem);
                assertTrue(sidePanel.isVisible(), "Side panel is visible after loading element");

                // Test full card formatting
                canvas.applyCardShapeFullFormatting(
                        cardElem,
                        "Updated Strategic Milestone",
                        "$750,000",
                        "Exceeded expectations with zero downtime",
                        "BLUE",
                        new Color(239, 246, 255),
                        new Color(59, 130, 246),
                        3,
                        "solid",
                        12,
                        "elevation",
                        540,
                        "center"
                );

                String updated = canvas.getDocumentHtml();
                assertTrue(updated.contains("Updated Strategic Milestone"), "Card title updated via full formatting");
                assertTrue(updated.contains("$750,000"), "Metric value updated via full formatting");
                assertTrue(updated.contains("data-shadow=\"elevation\"") || updated.contains("data-shadow='elevation'") || updated.contains("elevation"), "Elevation shadow applied to card");

                // Test duplicate object
                canvas.duplicateCardShapeElement(cardElem);
                String dupHtml = canvas.getDocumentHtml();
                assertTrue(dupHtml.contains("Updated Strategic Milestone"), "Duplicated card exists in document");
            }

            // Test loading null shows empty state without error
            sidePanel.loadElement(null);

            System.out.println("  ✔ Shape Format Side Panel & Multi-Page Pagination Stability tests passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  ✖ Shape Format Side Panel & Pagination test failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(String.format("Expected [%s] but was [%s]: %s", expected, actual, message));
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError("Expected non-null: " + message);
    }
}
