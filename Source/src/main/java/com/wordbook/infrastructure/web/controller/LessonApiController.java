package com.wordbook.infrastructure.web.controller;

import com.wordbook.core.application.dto.ApiResponse;
import com.wordbook.core.application.dto.LessonCreateRequest;
import com.wordbook.core.application.dto.LessonDto;
import com.wordbook.core.application.dto.LessonUpdateRequest;
import com.wordbook.core.application.service.LessonService;
import com.wordbook.infrastructure.web.router.HttpContextWrapper;
import com.wordbook.infrastructure.web.router.HttpRouter;

import java.util.List;
import java.util.Map;

/**
 * Controller exposing RESTful endpoints for Lesson documents.
 */
public class LessonApiController {
    private final LessonService lessonService;

    public LessonApiController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    public void registerRoutes(HttpRouter router) {
        router.get("/api/books/:bookId/lessons", this::getLessonsForBook);
        router.post("/api/books/:bookId/lessons", this::createLesson);
        router.get("/api/lessons/:id", this::getLessonById);
        router.put("/api/lessons/:id", this::updateLesson);
        router.delete("/api/lessons/:id", this::deleteLesson);
    }

    private void getLessonsForBook(HttpContextWrapper ctx) throws Exception {
        String bookId = ctx.getParam("bookId");
        List<LessonDto> lessons = lessonService.getLessonsForBook(bookId);
        ctx.sendJson(200, ApiResponse.ok(lessons));
    }

    private void getLessonById(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        LessonDto lesson = lessonService.getLessonById(id);
        ctx.sendJson(200, ApiResponse.ok(lesson));
    }

    private void createLesson(HttpContextWrapper ctx) throws Exception {
        String bookId = ctx.getParam("bookId");
        Map<String, Object> body = ctx.readBodyAsJson();
        String id = (String) body.get("id");
        String title = (String) body.get("title");
        String html = (String) body.get("html");
        Integer sortOrder = body.get("sortOrder") instanceof Number n ? n.intValue() : null;

        LessonCreateRequest req = new LessonCreateRequest(id, title, html, sortOrder);
        LessonDto created = lessonService.createLesson(bookId, req);
        ctx.sendJson(201, ApiResponse.ok("Lesson created", created));
    }

    private void updateLesson(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        Map<String, Object> body = ctx.readBodyAsJson();
        String title = (String) body.get("title");
        String html = (String) body.get("html");
        Integer words = body.get("wordCount") instanceof Number n ? n.intValue() : null;
        Integer chars = body.get("charCount") instanceof Number n ? n.intValue() : null;
        Integer sortOrder = body.get("sortOrder") instanceof Number n ? n.intValue() : null;

        LessonUpdateRequest req = new LessonUpdateRequest(title, html, words, chars, sortOrder);
        LessonDto updated = lessonService.updateLesson(id, req);
        ctx.sendJson(200, ApiResponse.ok("Lesson updated", updated));
    }

    private void deleteLesson(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        boolean deleted = lessonService.deleteLesson(id);
        ctx.sendJson(200, ApiResponse.ok("Lesson deleted", Map.of("id", id, "deleted", deleted)));
    }
}
