package com.wordbook.infrastructure.web.controller;

import com.wordbook.core.application.dto.ApiResponse;
import com.wordbook.core.application.service.ChartEngineService;
import com.wordbook.core.domain.model.ChartDefinition.ChartType;
import com.wordbook.infrastructure.web.router.HttpContextWrapper;
import com.wordbook.infrastructure.web.router.HttpRouter;

import java.util.Map;

/**
 * Controller exposing endpoints for server-side SVG chart rendering.
 */
public class ChartApiController {
    private final ChartEngineService chartEngineService;

    public ChartApiController(ChartEngineService chartEngineService) {
        this.chartEngineService = chartEngineService;
    }

    public void registerRoutes(HttpRouter router) {
        router.post("/api/charts/render", this::renderChart);
        router.get("/api/charts/sample", this::getSamples);
    }

    private void renderChart(HttpContextWrapper ctx) throws Exception {
        Map<String, Object> body = ctx.readBodyAsJson();
        String typeStr = (String) body.get("type");
        String data = (String) body.get("data");

        ChartType type = ChartType.fromString(typeStr);
        String svg = chartEngineService.render(type, data);

        ctx.sendJson(200, ApiResponse.ok(Map.of(
                "type", type.getCode(),
                "svg", svg
        )));
    }

    private void getSamples(HttpContextWrapper ctx) throws Exception {
        Map<String, String> samples = Map.of(
                "bar", "Maths: 80\nScience: 65\nHistory: 50\nArt: 90",
                "pie", "Maths: 40\nScience: 30\nHistory: 20\nArt: 10",
                "line", "Jan: 10\nFeb: 25\nMar: 18\nApr: 40\nMay: 35",
                "tree", "School\n  Maths\n    Algebra: 5\n    Geometry: 3\n  Science\n    Physics: 4\n    Biology: 6",
                "diagram", "Plant\n  Roots\n  Stem\n  Leaves\n    Chlorophyll\n    Stomata"
        );
        ctx.sendJson(200, ApiResponse.ok(samples));
    }
}
