module com.wordbook {
    requires java.sql;
    requires java.desktop;
    requires jdk.httpserver;
    requires java.logging;
    requires java.net.http;
    requires java.prefs;

    // Export public domain and application packages
    exports com.wordbook;
    exports com.wordbook.core.domain.model;
    exports com.wordbook.core.domain.repository;
    exports com.wordbook.core.domain.exception;
    exports com.wordbook.core.application.dto;
    exports com.wordbook.core.application.service;
    exports com.wordbook.infrastructure.persistence.sqlite;
    exports com.wordbook.infrastructure.web;
    exports com.wordbook.infrastructure.web.router;
    exports com.wordbook.infrastructure.web.controller;
    exports com.wordbook.infrastructure.web.json;
    exports com.wordbook.infrastructure.ui;
    exports com.wordbook.infrastructure.ui.swing;
}
