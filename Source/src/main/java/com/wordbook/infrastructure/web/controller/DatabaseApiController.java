package com.wordbook.infrastructure.web.controller;

import com.wordbook.core.application.dto.ApiResponse;
import com.wordbook.core.application.dto.BookWithLessonsDto;
import com.wordbook.core.application.dto.SystemStatsDto;
import com.wordbook.core.application.service.MigrationService;
import com.wordbook.infrastructure.web.json.SimpleJson;
import com.wordbook.infrastructure.web.router.HttpContextWrapper;
import com.wordbook.infrastructure.web.router.HttpRouter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing database diagnostics, SQLite statistics, and JSON backups/imports.
 */
public class DatabaseApiController {
    private final MigrationService migrationService;

    public DatabaseApiController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public void registerRoutes(HttpRouter router) {
        router.get("/api/db/stats", this::getStats);
        router.get("/api/db/export", this::exportBackup);
        router.post("/api/db/import", this::importBackup);
    }

    private void getStats(HttpContextWrapper ctx) throws Exception {
        SystemStatsDto stats = migrationService.getSystemStats();
        ctx.sendJson(200, ApiResponse.ok(stats));
    }

    private void exportBackup(HttpContextWrapper ctx) throws Exception {
        List<BookWithLessonsDto> data = migrationService.exportAllData();
        Map<String, Object> payload = Map.of(
                "version", "wordbook-v2-sqlite",
                "exportedAt", System.currentTimeMillis(),
                "books", data
        );
        String json = SimpleJson.serialize(payload);
        String filename = "wordbook_backup_" + System.currentTimeMillis() + ".json";
        ctx.sendFileDownload(200, filename, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private void importBackup(HttpContextWrapper ctx) throws Exception {
        Map<String, Object> body = ctx.readBodyAsJson();
        Object booksObj = body.get("books");
        if (!(booksObj instanceof List<?> list)) {
            ctx.sendError(400, "Invalid payload: 'books' array is required.");
            return;
        }

        List<Map<String, Object>> booksList = (List<Map<String, Object>>) list;
        int imported = migrationService.importBooks(booksList);
        ctx.sendJson(200, ApiResponse.ok("Successfully imported books into SQLite", Map.of("importedBooks", imported)));
    }
}
