package com.wordbook.infrastructure.web.router;

@FunctionalInterface
public interface RouteHandler {
    void handle(HttpContextWrapper ctx) throws Exception;
}
