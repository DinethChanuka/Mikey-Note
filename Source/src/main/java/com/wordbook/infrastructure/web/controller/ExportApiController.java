package com.wordbook.infrastructure.web.controller;

import com.wordbook.core.application.service.ExportService;
import com.wordbook.infrastructure.web.router.HttpContextWrapper;
import com.wordbook.infrastructure.web.router.HttpRouter;

/**
 * Controller exposing document export endpoints (HTML, Markdown).
 */
public class ExportApiController {
    private final ExportService exportService;

    public ExportApiController(ExportService exportService) {
        this.exportService = exportService;
    }

    public void registerRoutes(HttpRouter router) {
        router.get("/api/lessons/:id/export/html", this::exportLessonHtml);
        router.get("/api/lessons/:id/export/md", this::exportLessonMarkdown);
        router.get("/api/books/:id/export/html", this::exportBookHtml);
    }

    private void exportLessonHtml(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        var res = exportService.exportLessonAsHtml(id);
        ctx.sendFileDownload(200, res.filename(), res.contentType(), res.content());
    }

    private void exportLessonMarkdown(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        var res = exportService.exportLessonAsMarkdown(id);
        ctx.sendFileDownload(200, res.filename(), res.contentType(), res.content());
    }

    private void exportBookHtml(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        var res = exportService.exportBookAsHtml(id);
        ctx.sendFileDownload(200, res.filename(), res.contentType(), res.content());
    }
}
