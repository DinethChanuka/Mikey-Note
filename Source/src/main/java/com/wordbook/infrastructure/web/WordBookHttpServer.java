package com.wordbook.infrastructure.web;

import com.sun.net.httpserver.HttpServer;
import com.wordbook.core.application.service.*;
import com.wordbook.infrastructure.web.controller.*;
import com.wordbook.infrastructure.web.router.HttpRouter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Embedded HTTP Server hosting the WordBook REST API and static web client.
 */
public class WordBookHttpServer {
    private static final Logger LOGGER = Logger.getLogger(WordBookHttpServer.class.getName());

    private final int port;
    private final String host;
    private final Path staticDir;
    private final BookService bookService;
    private final LessonService lessonService;
    private final ExportService exportService;
    private final ChartEngineService chartEngineService;
    private final MigrationService migrationService;

    private HttpServer server;
    private ExecutorService executor;

    public WordBookHttpServer(int port, String host, Path staticDir,
                              BookService bookService,
                              LessonService lessonService,
                              ExportService exportService,
                              ChartEngineService chartEngineService,
                              MigrationService migrationService) {
        this.port = port;
        this.host = (host != null && !host.isBlank()) ? host : "0.0.0.0";
        this.staticDir = staticDir;
        this.bookService = bookService;
        this.lessonService = lessonService;
        this.exportService = exportService;
        this.chartEngineService = chartEngineService;
        this.migrationService = migrationService;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }

        InetSocketAddress address = new InetSocketAddress(host, port);
        server = HttpServer.create(address, 0);

        HttpRouter router = new HttpRouter();

        // Register API Controllers
        new BookApiController(bookService).registerRoutes(router);
        new LessonApiController(lessonService).registerRoutes(router);
        new ExportApiController(exportService).registerRoutes(router);
        new ChartApiController(chartEngineService).registerRoutes(router);
        new DatabaseApiController(migrationService).registerRoutes(router);

        // Register Static Front-End Controller
        new StaticAssetController(staticDir).registerRoutes(router);

        server.createContext("/", router);

        // Use modern Java 24 Virtual Threads for high-throughput request handling
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);

        server.start();
        LOGGER.info("WordBook Server started at http://" + (host.equals("0.0.0.0") ? "localhost" : host) + ":" + port);
    }

    public synchronized void stop(int delaySeconds) {
        if (server != null) {
            LOGGER.info("Stopping WordBook Server...");
            server.stop(Math.max(0, delaySeconds));
            server = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    public int getPort() {
        return (server != null) ? server.getAddress().getPort() : port;
    }

    public String getHost() {
        return host;
    }

    public boolean isRunning() {
        return server != null;
    }
}
