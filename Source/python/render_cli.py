#!/usr/bin/env python3
"""
Mikey Note — Render Helper CLI
==============================
Command line utility for fast chart rendering, image optimization,
component generation, and performance benchmarking.

Usage:
    python3 -m python.render_cli --benchmark
    python3 -m python.render_cli --demo
    python3 -m python.render_cli --chart-sample
    python3 -m python.render_cli --lqip assets/mikey_note_logo.png
    python3 -m python.render_cli --fit 1920 1080 --paper A4
"""

import sys
import os
import json
import argparse

# Add parent directory to path so python package can be imported directly
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from python.chart_fast_renderer import (
    render_bar_chart, render_line_chart, render_pie_donut_chart, render_kpi_card, PALETTES
)
from python.image_fast_loader import (
    compute_paper_fit, get_image_dimensions, generate_lqip_placeholder
)
from python.object_render_helpers import (
    render_callout_box, render_process_flow, render_math_formula
)
from python.fast_render_pipeline import FastRenderPipeline

def run_benchmark():
    print("==================================================")
    print("  Mikey Note Fast Render Pipeline Benchmark       ")
    print("==================================================")
    pipeline = FastRenderPipeline(max_workers=8)
    stats = pipeline.benchmark(iterations=1000)
    print(f"Iterations:             {stats['iterations']}")
    print(f"Total Time:             {stats['total_time_ms']} ms")
    print(f"Average Latency:        {stats['average_latency_us']} µs ({stats['average_latency_ms']} ms)")
    print(f"Throughput:             {stats['throughput_renders_per_sec']} renders / second")
    print(f"Performance Rating:     {stats['performance_grade']}")
    print("==================================================")

def run_demo(output_dir: str):
    os.makedirs(output_dir, exist_ok=True)
    print(f"[Demo] Generating sample rendered artifacts in: {output_dir}")

    # 1. Bar Chart
    bar_svg = render_bar_chart(["Jan", "Feb", "Mar", "Apr", "May", "Jun"], [42, 68, 85, 110, 142, 190],
                               title="Monthly Growth (KPI)", palette="corporate_blue")
    with open(os.path.join(output_dir, "sample_bar.svg"), "w") as f:
        f.write(bar_svg)

    # 2. Line Chart
    line_svg = render_line_chart(["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
                                 [12, 19, 15, 25, 32, 45, 52], title="Daily Active Editors",
                                 palette="emerald_mint")
    with open(os.path.join(output_dir, "sample_line.svg"), "w") as f:
        f.write(line_svg)

    # 3. Donut Chart
    donut_svg = render_pie_donut_chart(["Markdown", "Rich Text", "Code Snippets", "Charts"],
                                      [45, 30, 15, 10], title="Document Content Breakdown",
                                      palette="violet_purple", donut=True)
    with open(os.path.join(output_dir, "sample_donut.svg"), "w") as f:
        f.write(donut_svg)

    # 4. KPI Card
    kpi_svg = render_kpi_card("Storage Efficiency", "99.4%", delta="+4.2%", subtitle="SQLite WAL Cache Hit",
                              palette="amber_gold")
    with open(os.path.join(output_dir, "sample_kpi.svg"), "w") as f:
        f.write(kpi_svg)

    # 5. Process Flow
    flow_svg = render_process_flow(["Idea", "Outline", "Draft", "Review", "Publish"])
    with open(os.path.join(output_dir, "sample_flow.svg"), "w") as f:
        f.write(flow_svg)

    # 6. Combined HTML preview gallery
    html_gallery = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Mikey Note — Fast Rendered Components</title>
  <style>
    body {{ font-family: -apple-system, Segoe UI, Roboto, sans-serif; background: #f8fafc; padding: 32px; color: #1e293b; }}
    h1 {{ font-size: 24px; font-weight: 800; color: #0f172a; margin-bottom: 24px; }}
    .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 24px; }}
    .card {{ background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.03); }}
    .card h3 {{ font-size: 14px; font-weight: 700; color: #64748b; margin-top: 0; text-transform: uppercase; letter-spacing: 0.05em; }}
  </style>
</head>
<body>
  <h1>Mikey Note — Fast Rendered Object Preview</h1>
  <div class="grid">
    <div class="card"><h3>Bar Chart</h3>{bar_svg}</div>
    <div class="card"><h3>Line Chart</h3>{line_svg}</div>
    <div class="card"><h3>Donut Chart</h3>{donut_svg}</div>
    <div class="card"><h3>KPI Metric</h3>{kpi_svg}</div>
  </div>
  <div class="card" style="margin-top:24px;">
    <h3>Process Flow Element</h3>
    {flow_svg}
  </div>
</body>
</html>"""
    gallery_path = os.path.join(output_dir, "preview_gallery.html")
    with open(gallery_path, "w") as f:
        f.write(html_gallery)

    print(f"[Demo] Created preview gallery at: {gallery_path}")

def main():
    parser = argparse.ArgumentParser(description="Mikey Note Fast Render Helper CLI")
    parser.add_argument("--benchmark", action="store_true", help="Run 1000-object parallel render benchmark")
    parser.add_argument("--demo", action="store_true", help="Generate sample SVGs and HTML preview gallery")
    parser.add_argument("--output", default="temp/files/rendered_samples", help="Output directory for demo")
    parser.add_argument("--lqip", type=str, help="Generate base64 LQIP placeholder for an image")
    parser.add_argument("--fit", nargs=2, type=int, metavar=("WIDTH", "HEIGHT"), help="Calculate paper fit for dimensions")
    parser.add_argument("--paper", default="A4", choices=["A4", "LETTER", "LEGAL"], help="Paper format for --fit")

    args = parser.parse_args()

    if args.benchmark:
        run_benchmark()
    elif args.demo:
        run_demo(args.output)
    elif args.lqip:
        res = generate_lqip_placeholder(args.lqip)
        print(f"LQIP Data URI (Length {len(res)}): {res[:80]}...")
    elif args.fit:
        res = compute_paper_fit(args.fit[0], args.fit[1], paper_type=args.paper)
        print(json.dumps(res, indent=2))
    else:
        # Default action: run benchmark and demo
        run_benchmark()
        run_demo(args.output)

if __name__ == "__main__":
    main()
