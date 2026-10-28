package com.wordbook.infrastructure.web.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.wordbook.core.domain.exception.WordBookException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RESTful router matching HTTP methods and URI path templates (e.g. /api/books/:id).
 */
public class HttpRouter implements HttpHandler {

    private record Route(
            String method,
            String pathTemplate,
            Pattern regex,
            List<String> paramNames,
            RouteHandler handler
    ) {}

    private final List<Route> routes = new ArrayList<>();
    private RouteHandler notFoundHandler = ctx -> ctx.sendError(404, "Endpoint not found: " + ctx.getPath());

    public HttpRouter get(String path, RouteHandler handler) {
        addRoute("GET", path, handler);
        return this;
    }

    public HttpRouter post(String path, RouteHandler handler) {
        addRoute("POST", path, handler);
        return this;
    }

    public HttpRouter put(String path, RouteHandler handler) {
        addRoute("PUT", path, handler);
        return this;
    }

    public HttpRouter delete(String path, RouteHandler handler) {
        addRoute("DELETE", path, handler);
        return this;
    }

    public HttpRouter setNotFoundHandler(RouteHandler handler) {
        this.notFoundHandler = handler;
        return this;
    }

    private void addRoute(String method, String pathTemplate, RouteHandler handler) {
        List<String> paramNames = new ArrayList<>();
        StringBuilder regexBuilder = new StringBuilder("^");
        String[] segments = pathTemplate.split("/");

        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            regexBuilder.append("/");
            if (seg.startsWith(":")) {
                paramNames.add(seg.substring(1));
                regexBuilder.append("([^/]+)");
            } else {
                regexBuilder.append(Pattern.quote(seg));
            }
        }
        if (pathTemplate.endsWith("/")) {
            regexBuilder.append("/?");
        }
        regexBuilder.append("$");

        Pattern regex = Pattern.compile(regexBuilder.toString());
        routes.add(new Route(method.toUpperCase(Locale.ROOT), pathTemplate, regex, paramNames, handler));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String path = exchange.getRequestURI().getPath();

        // Standard CORS pre-flight handler
        if ("OPTIONS".equals(method)) {
            HttpContextWrapper ctx = new HttpContextWrapper(exchange, Collections.emptyMap());
            ctx.sendNoContent();
            return;
        }

        for (Route route : routes) {
            if (!route.method.equals(method)) continue;
            Matcher matcher = route.regex.matcher(path);
            if (matcher.matches()) {
                Map<String, String> params = new LinkedHashMap<>();
                for (int i = 0; i < route.paramNames.size(); i++) {
                    params.put(route.paramNames.get(i), matcher.group(i + 1));
                }
                HttpContextWrapper ctx = new HttpContextWrapper(exchange, params);
                try {
                    route.handler.handle(ctx);
                } catch (WordBookException wbe) {
                    ctx.sendError(wbe.getStatusCode(), wbe.getMessage());
                } catch (IllegalArgumentException iae) {
                    ctx.sendError(400, iae.getMessage());
                } catch (Exception e) {
                    ctx.sendError(500, "Internal Server Error: " + e.getMessage());
                }
                return;
            }
        }

        // No matching route found
        HttpContextWrapper ctx = new HttpContextWrapper(exchange, Collections.emptyMap());
        try {
            notFoundHandler.handle(ctx);
        } catch (Exception e) {
            ctx.sendError(500, e.getMessage());
        }
    }
}
