package com.wordbook.infrastructure.web.controller;

import com.wordbook.infrastructure.web.router.HttpContextWrapper;
import com.wordbook.infrastructure.web.router.HttpRouter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Controller serving static front-end assets (HTML, CSS, JS) with path validation,
 * MIME type resolution, and security checks against directory traversal.
 */
public class StaticAssetController {
    private final Path rootDir;

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry(".html", "text/html; charset=UTF-8"),
            Map.entry(".htm", "text/html; charset=UTF-8"),
            Map.entry(".css", "text/css; charset=UTF-8"),
            Map.entry(".js", "application/javascript; charset=UTF-8"),
            Map.entry(".json", "application/json; charset=UTF-8"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".ico", "image/x-icon")
    );

    public StaticAssetController(Path rootDir) {
        this.rootDir = rootDir.toAbsolutePath().normalize();
    }

    public void registerRoutes(HttpRouter router) {
        router.setNotFoundHandler(this::serveAssetOrNotFound);
    }

    public void serveAssetOrNotFound(HttpContextWrapper ctx) throws Exception {
        String path = ctx.getPath();
        if (path == null || path.isEmpty() || "/".equals(path) || "index.html".equals(path)
                || "/loadwindow".equals(path) || "/loadwindow/".equals(path) || "loadwindow".equals(path) || "loadwindow/".equals(path)) {
            if (Files.exists(rootDir.resolve("loadwindow/index.html"))) {
                path = "loadwindow/index.html";
            } else {
                path = "index.html";
            }
        }

        // Clean query parameters and leading slashes
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Path target = rootDir.resolve(path).normalize();

        // Enforce boundary check against directory traversal
        if (!target.startsWith(rootDir)) {
            ctx.sendError(403, "Access Denied");
            return;
        }

        File file = target.toFile();

        // Fallback 1: loadwindow/index.html for root index.html request
        if ((!file.exists() || file.isDirectory()) && "index.html".equals(path)) {
            Path lwTarget = rootDir.resolve("loadwindow/index.html").normalize();
            if (lwTarget.toFile().exists()) {
                target = lwTarget;
                file = target.toFile();
            }
        }

        // Fallback 2: Favicon resolution
        if ((!file.exists() || file.isDirectory()) && (path.endsWith("favicon.ico") || path.endsWith("favicon.png"))) {
            Path assetFavicon = rootDir.resolve("assets").resolve(path.substring(path.lastIndexOf('/') + 1)).normalize();
            if (assetFavicon.toFile().exists() && !assetFavicon.toFile().isDirectory()) {
                target = assetFavicon;
                file = target.toFile();
            }
        }

        // Fallback 3: If requesting loadwindow/<asset> and not found in loadwindow, fallback to rootDir/<asset>
        if ((!file.exists() || file.isDirectory()) && path.startsWith("loadwindow/")) {
            String stripped = path.substring("loadwindow/".length());
            Path rootFallback = rootDir.resolve(stripped).normalize();
            if (rootFallback.startsWith(rootDir) && rootFallback.toFile().exists() && !rootFallback.toFile().isDirectory()) {
                target = rootFallback;
                file = target.toFile();
            }
        }

        // Fallback 4: If requesting root <asset> and not found in root, fallback to loadwindow/<asset>
        if (!file.exists() || file.isDirectory()) {
            Path lwAsset = rootDir.resolve("loadwindow").resolve(path).normalize();
            if (lwAsset.startsWith(rootDir) && lwAsset.toFile().exists() && !lwAsset.toFile().isDirectory()) {
                target = lwAsset;
                file = target.toFile();
            }
        }

        if (!file.exists() || file.isDirectory()) {
            ctx.sendError(404, "File not found: " + ctx.getPath());
            return;
        }

        String mimeType = determineMimeType(file.getName());
        byte[] bytes = Files.readAllBytes(target);
        ctx.sendBytes(200, mimeType, bytes);
    }

    private String determineMimeType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            String ext = filename.substring(dot).toLowerCase();
            String mime = MIME_TYPES.get(ext);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }
}
