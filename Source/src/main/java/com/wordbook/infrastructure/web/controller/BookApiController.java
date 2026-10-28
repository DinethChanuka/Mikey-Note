package com.wordbook.infrastructure.web.controller;

import com.wordbook.core.application.dto.ApiResponse;
import com.wordbook.core.application.dto.BookCreateRequest;
import com.wordbook.core.application.dto.BookDto;
import com.wordbook.core.application.dto.BookUpdateRequest;
import com.wordbook.core.application.service.BookService;
import com.wordbook.infrastructure.web.router.HttpContextWrapper;
import com.wordbook.infrastructure.web.router.HttpRouter;

import java.util.List;
import java.util.Map;

/**
 * Controller exposing RESTful endpoints for Book aggregate.
 */
public class BookApiController {
    private final BookService bookService;

    public BookApiController(BookService bookService) {
        this.bookService = bookService;
    }

    public void registerRoutes(HttpRouter router) {
        router.get("/api/books", this::getAllBooks);
        router.post("/api/books", this::createBook);
        router.get("/api/books/:id", this::getBookById);
        router.put("/api/books/:id", this::updateBook);
        router.delete("/api/books/:id", this::deleteBook);
    }

    private void getAllBooks(HttpContextWrapper ctx) throws Exception {
        List<BookDto> books = bookService.getAllBooks();
        ctx.sendJson(200, ApiResponse.ok(books));
    }

    private void getBookById(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        var bookWithLessons = bookService.getBookWithLessons(id);
        ctx.sendJson(200, ApiResponse.ok(bookWithLessons));
    }

    private void createBook(HttpContextWrapper ctx) throws Exception {
        Map<String, Object> body = ctx.readBodyAsJson();
        String id = (String) body.get("id");
        String title = (String) body.get("title");
        String color = (String) body.get("color");

        BookCreateRequest req = new BookCreateRequest(id, title, color);
        BookDto created = bookService.createBook(req);
        ctx.sendJson(201, ApiResponse.ok("Book created", created));
    }

    private void updateBook(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        Map<String, Object> body = ctx.readBodyAsJson();
        String title = (String) body.get("title");
        String color = (String) body.get("color");

        BookUpdateRequest req = new BookUpdateRequest(title, color);
        BookDto updated = bookService.updateBook(id, req);
        ctx.sendJson(200, ApiResponse.ok("Book updated", updated));
    }

    private void deleteBook(HttpContextWrapper ctx) throws Exception {
        String id = ctx.getParam("id");
        boolean deleted = bookService.deleteBook(id);
        ctx.sendJson(200, ApiResponse.ok("Book deleted", Map.of("id", id, "deleted", deleted)));
    }
}
