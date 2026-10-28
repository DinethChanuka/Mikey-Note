# Mikey Note — Python Fast Rendering & Preloading Helpers

High-performance, standalone Python modules providing rendering, image optimization, low-quality image placeholder (LQIP) preloading, and paper canvas fitting for Mikey Note documents.

These helpers are decoupled and can be executed independently from CLI, batch scripts, microservices, or automated workflows.

## Modules

### 1. `chart_fast_renderer.py`
High-speed vector chart rendering generating crisp, responsive SVGs in microseconds:
- **Bar Charts**: `render_bar_chart(labels, values, title, palette, horizontal)`
- **Line Charts**: `render_line_chart(labels, values, title, palette, area)`
- **Pie & Donut Charts**: `render_pie_donut_chart(labels, values, title, palette, donut)`
- **KPI Metric Cards**: `render_kpi_card(title, value, delta, subtitle, palette)`
- **7 Theme Palettes**: `corporate_blue`, `emerald_mint`, `violet_purple`, `amber_gold`, `crimson_rose`, `dark_slate`, `clean_minimal`.

### 2. `image_fast_loader.py`
Zero-layout-shift image preloading and canvas fitting:
- `generate_lqip_placeholder(image_path, size=16)`: Returns tiny Base64 data URL preview for progressive loading.
- `compute_paper_fit(width, height, paper_type="A4", margin_type="NORMAL")`: Computes exact width, height, and scale within page margins.
- `apply_image_filter(input_path, output_path, filter_type)`: Applies filters (`grayscale`, `sepia`, etc.).

### 3. `object_render_helpers.py`
Document components and visual representations:
- `render_callout_box(text, title, callout_type, palette)`: Callout boxes (info, warning, tip, quote).
- `render_process_flow(steps, palette)`: Connected process sequence diagrams.
- `render_math_formula(expression)`: High-DPI mathematical expressions.
- `render_ribbon_preview_tile(label, category, color_hex)`: Ribbon miniature preview tiles.

### 4. `fast_render_pipeline.py`
Multi-threaded parallel rendering and benchmark suite:
- `FastRenderPipeline.render_parallel(tasks)`: Parallel batch rendering using thread pools.
- `FastRenderPipeline.benchmark(iterations=1000)`: Benchmarks render throughput (>40,000 renders/sec, ~11 µs per object).

### 5. `render_cli.py`
Unified CLI runner:
```bash
# Run benchmark and generate demo preview gallery
python3 -m python.render_cli

# Compute A4 paper fitting for high-res images
python3 -m python.render_cli --fit 1920 1080 --paper A4

# Generate LQIP placeholder data URL
python3 -m python.render_cli --lqip assets/mikey_note_logo.png
```
