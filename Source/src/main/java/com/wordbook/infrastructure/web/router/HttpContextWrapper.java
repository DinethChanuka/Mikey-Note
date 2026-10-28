package com.wordbook.infrastructure.web.router;

import com.sun.net.httpserver.HttpExchange;
import com.wordbook.infrastructure.web.json.SimpleJson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Convenient wrapper over JDK HttpExchange providing path parameter extraction,
 * CORS support, JSON serialization/deserialization, and standard HTTP response helpers.
 */
public class HttpContextWrapper {
    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private Map<String, String> queryParams;
    private String cachedBody;

    public HttpContextWrapper(HttpExchange exchange, Map<String, String> pathParams) {
        this.exchange = exchange;
        this.pathParams = pathParams != null ? pathParams : Collections.emptyMap();
    }

    public String getMethod() {
        return exchange.getRequestMethod().toUpperCase(Locale.ROOT);
    }

    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    public URI getUri() {
        return exchange.getRequestURI();
    }

    public String getParam(String name) {
        return pathParams.get(name);
    }

    public String getQueryParam(String name) {
        if (queryParams == null) {
            queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
        }
        return queryParams.get(name);
    }

    public String readBodyAsString() throws IOException {
        if (cachedBody != null) return cachedBody;
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            cachedBody = baos.toString(StandardCharsets.UTF_8);
            return cachedBody;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readBodyAsJson() throws IOException {
        String body = readBodyAsString();
        if (body == null || body.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        return SimpleJson.parseObject(body);
    }

    public void addCorsHeaders() {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, X-Requested-With");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    public void sendJson(int statusCode, Object data) throws IOException {
        addCorsHeaders();
        String json = SimpleJson.serialize(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void sendBytes(int statusCode, String contentType, byte[] data) throws IOException {
        addCorsHeaders();
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    public void sendFileDownload(int statusCode, String filename, String contentType, byte[] data) throws IOException {
        addCorsHeaders();
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    public void sendText(int statusCode, String text) throws IOException {
        addCorsHeaders();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void sendNoContent() throws IOException {
        addCorsHeaders();
        exchange.sendResponseHeaders(204, -1);
    }

    public void sendError(int statusCode, String message) throws IOException {
        addCorsHeaders();
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("error", message);
        err.put("statusCode", statusCode);
        sendJson(statusCode, err);
    }

    private static Map<String, String> parseQueryParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>();
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }
}
