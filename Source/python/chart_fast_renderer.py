#!/usr/bin/env python3
"""
Mikey Note — Fast Vector Chart Renderer
========================================
High-performance, zero-dependency SVG & HTML chart generator.
Renders beautiful, responsive charts in sub-milliseconds with modern glassmorphism,
smooth gradients, crisp typography, and Mikey Note color palettes.
"""

import math
import html
import json
from typing import List, Dict, Any, Optional, Tuple

# Mikey Note Standard Palettes
PALETTES = {
    "corporate_blue": ["#2563eb", "#38bdf8", "#1d4ed8", "#60a5fa", "#93c5fd"],
    "emerald_mint":   ["#059669", "#10b981", "#34d399", "#6ee7b7", "#a7f3d0"],
    "violet_purple":  ["#7c3aed", "#8b5cf6", "#a78bfa", "#c4b5fd", "#ddd6fe"],
    "amber_gold":     ["#d97706", "#f59e0b", "#fbbf24", "#fcd34d", "#fde68a"],
    "crimson_rose":   ["#e11d48", "#f43f5e", "#fb7185", "#fda4af", "#fecdd3"],
    "dark_slate":     ["#334155", "#475569", "#64748b", "#94a3b8", "#cbd5e1"],
    "clean_minimal":  ["#1e293b", "#3b82f6", "#10b981", "#f59e0b", "#64748b"],
}

def get_palette(name: str) -> List[str]:
    return PALETTES.get(name.lower().replace(" ", "_"), PALETTES["corporate_blue"])

def _svg_wrapper(inner_svg: str, width: int, height: int, title: str = "") -> str:
    """Wraps chart elements into a styled SVG with ambient card styling."""
    title_elem = ""
    if title:
        title_elem = f'<text x="24" y="32" font-family="-apple-system, Segoe UI, Roboto, sans-serif" font-size="15" font-weight="600" fill="#0f172a">{html.escape(title)}</text>'

    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="100%" height="auto" style="max-width:{width}px;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;box-shadow:0 4px 16px rgba(0,0,0,0.04);font-family:-apple-system,Segoe UI,Roboto,sans-serif;">
  <defs>
    <filter id="card-shadow" x="-5%" y="-5%" width="110%" height="115%">
      <feDropShadow dx="0" dy="2" stdDeviation="3" flood-opacity="0.06"/>
    </filter>
  </defs>
  {title_elem}
  {inner_svg}
</svg>"""

def render_bar_chart(labels: List[str], values: List[float], title: str = "",
                     palette: str = "corporate_blue", width: int = 600, height: int = 340,
                     horizontal: bool = False) -> str:
    """Renders high-speed vertical or horizontal Bar Chart SVG."""
    colors = get_palette(palette)
    n = len(values)
    if n == 0:
        return _svg_wrapper("<text x='50%' y='50%' text-anchor='middle' fill='#94a3b8'>No Data</text>", width, height, title)

    max_val = max(values) if max(values) > 0 else 1.0
    top_pad = 54 if title else 28
    bottom_pad = 48
    left_pad = 55
    right_pad = 30

    chart_w = width - left_pad - right_pad
    chart_h = height - top_pad - bottom_pad

    elements = []

    # Grid lines (4 horizontal ticks)
    for i in range(5):
        y = top_pad + (chart_h / 4) * i
        tick_val = max_val * (4 - i) / 4
        elements.append(f'<line x1="{left_pad}" y1="{y}" x2="{width - right_pad}" y2="{y}" stroke="#f1f5f9" stroke-width="1.2"/>')
        elements.append(f'<text x="{left_pad - 8}" y="{y + 4}" text-anchor="end" font-size="10" fill="#94a3b8">{tick_val:g}</text>')

    if not horizontal:
        bar_slot = chart_w / n
        bar_w = min(48, bar_slot * 0.65)
        for i, (lbl, val) in enumerate(zip(labels, values)):
            col = colors[i % len(colors)]
            bar_h = (val / max_val) * chart_h
            bx = left_pad + i * bar_slot + (bar_slot - bar_w) / 2
            by = top_pad + chart_h - bar_h

            # Bar with rounded top
            elements.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{bar_w:.1f}" height="{bar_h:.1f}" rx="5" ry="5" fill="{col}" filter="url(#card-shadow)"/>')
            # Value label
            elements.append(f'<text x="{bx + bar_w/2:.1f}" y="{by - 6:.1f}" text-anchor="middle" font-size="11" font-weight="600" fill="#334155">{val:g}</text>')
            # X-Axis label
            short_lbl = lbl if len(lbl) <= 10 else lbl[:9] + "…"
            elements.append(f'<text x="{bx + bar_w/2:.1f}" y="{height - bottom_pad + 18}" text-anchor="middle" font-size="11" fill="#64748b">{html.escape(short_lbl)}</text>')
    else:
        # Horizontal Bars
        bar_slot = chart_h / n
        bar_h = min(36, bar_slot * 0.65)
        for i, (lbl, val) in enumerate(zip(labels, values)):
            col = colors[i % len(colors)]
            bar_w = (val / max_val) * chart_w
            bx = left_pad
            by = top_pad + i * bar_slot + (bar_slot - bar_h) / 2

            elements.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{bar_w:.1f}" height="{bar_h:.1f}" rx="4" ry="4" fill="{col}"/>')
            elements.append(f'<text x="{bx + bar_w + 8:.1f}" y="{by + bar_h/2 + 4:.1f}" font-size="11" font-weight="600" fill="#334155">{val:g}</text>')
            elements.append(f'<text x="{left_pad - 8}" y="{by + bar_h/2 + 4:.1f}" text-anchor="end" font-size="11" fill="#64748b">{html.escape(lbl)}</text>')

    return _svg_wrapper("\n  ".join(elements), width, height, title)

def render_line_chart(labels: List[str], values: List[float], title: str = "",
                      palette: str = "emerald_mint", width: int = 600, height: int = 340,
                      area: bool = True) -> str:
    """Renders high-speed Smooth Spline / Area Line Chart SVG."""
    colors = get_palette(palette)
    line_col = colors[0]
    n = len(values)
    if n < 2:
        return render_bar_chart(labels, values, title, palette, width, height)

    max_val = max(values) if max(values) > 0 else 1.0
    min_val = min(0.0, min(values))
    val_range = (max_val - min_val) if (max_val - min_val) > 0 else 1.0

    top_pad = 54 if title else 28
    bottom_pad = 48
    left_pad = 55
    right_pad = 30
    chart_w = width - left_pad - right_pad
    chart_h = height - top_pad - bottom_pad

    pts: List[Tuple[float, float]] = []
    step_x = chart_w / (n - 1)
    for i, val in enumerate(values):
        px = left_pad + i * step_x
        py = top_pad + chart_h - ((val - min_val) / val_range) * chart_h
        pts.append((px, py))

    elements = []
    # Grid lines
    for i in range(5):
        y = top_pad + (chart_h / 4) * i
        tick_val = min_val + val_range * (4 - i) / 4
        elements.append(f'<line x1="{left_pad}" y1="{y}" x2="{width - right_pad}" y2="{y}" stroke="#f1f5f9" stroke-width="1.2"/>')
        elements.append(f'<text x="{left_pad - 8}" y="{y + 4}" text-anchor="end" font-size="10" fill="#94a3b8">{tick_val:g}</text>')

    # Construct Smooth Cubic Bezier Path
    path_d = f"M {pts[0][0]:.1f} {pts[0][1]:.1f}"
    for i in range(len(pts) - 1):
        p0 = pts[i]
        p1 = pts[i + 1]
        c1x = p0[0] + (p1[0] - p0[0]) * 0.45
        c1y = p0[1]
        c2x = p0[0] + (p1[0] - p0[0]) * 0.55
        c2y = p1[1]
        path_d += f" C {c1x:.1f} {c1y:.1f}, {c2x:.1f} {c2y:.1f}, {p1[0]:.1f} {p1[0]:.1f}"

    # Area Gradient fill
    if area:
        area_d = f"{path_d} L {pts[-1][0]:.1f} {top_pad + chart_h} L {pts[0][0]:.1f} {top_pad + chart_h} Z"
        grad_id = f"area-grad-{hash(title) % 10000}"
        elements.append(f"""<defs>
    <linearGradient id="{grad_id}" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="{line_col}" stop-opacity="0.32"/>
      <stop offset="100%" stop-color="{line_col}" stop-opacity="0.0"/>
    </linearGradient>
  </defs>""")
        elements.append(f'<path d="{area_d}" fill="url(#{grad_id})"/>')

    # Line stroke
    elements.append(f'<path d="{path_d}" fill="none" stroke="{line_col}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>')

    # Data Points & Labels
    for i, (px, py) in enumerate(pts):
        val = values[i]
        lbl = labels[i] if i < len(labels) else ""
        elements.append(f'<circle cx="{px:.1f}" cy="{py:.1f}" r="4.5" fill="#ffffff" stroke="{line_col}" stroke-width="2.5"/>')
        elements.append(f'<text x="{px:.1f}" y="{py - 9:.1f}" text-anchor="middle" font-size="11" font-weight="600" fill="#1e293b">{val:g}</text>')
        short_lbl = lbl if len(lbl) <= 8 else lbl[:7] + "…"
        elements.append(f'<text x="{px:.1f}" y="{height - bottom_pad + 18}" text-anchor="middle" font-size="11" fill="#64748b">{html.escape(short_lbl)}</text>')

    return _svg_wrapper("\n  ".join(elements), width, height, title)

def render_pie_donut_chart(labels: List[str], values: List[float], title: str = "",
                           palette: str = "violet_purple", width: int = 500, height: int = 340,
                           donut: bool = True) -> str:
    """Renders high-speed Donut or Pie Chart with animated slice aesthetics."""
    colors = get_palette(palette)
    total = sum(values) if sum(values) > 0 else 1.0

    cx = width * 0.40
    cy = height * 0.52
    r = min(width, height) * 0.36
    inner_r = r * 0.58 if donut else 0.0

    elements = []
    start_angle = -math.pi / 2

    legend_x = width * 0.68
    legend_y = 60

    for i, (lbl, val) in enumerate(zip(labels, values)):
        col = colors[i % len(colors)]
        fraction = val / total
        slice_angle = fraction * 2 * math.pi
        end_angle = start_angle + slice_angle

        # Arc coords
        x1 = cx + r * math.cos(start_angle)
        y1 = cy + r * math.sin(start_angle)
        x2 = cx + r * math.cos(end_angle)
        y2 = cy + r * math.sin(end_angle)

        large_arc = 1 if slice_angle > math.pi else 0

        if inner_r > 0:
            x3 = cx + inner_r * math.cos(end_angle)
            y3 = cy + inner_r * math.sin(end_angle)
            x4 = cx + inner_r * math.cos(start_angle)
            y4 = cy + inner_r * math.sin(start_angle)
            path_d = f"M {x1:.2f} {y1:.2f} A {r:.2f} {r:.2f} 0 {large_arc} 1 {x2:.2f} {y2:.2f} L {x3:.2f} {y3:.2f} A {inner_r:.2f} {inner_r:.2f} 0 {large_arc} 0 {x4:.2f} {y4:.2f} Z"
        else:
            path_d = f"M {cx:.2f} {cy:.2f} L {x1:.2f} {y1:.2f} A {r:.2f} {r:.2f} 0 {large_arc} 1 {x2:.2f} {y2:.2f} Z"

        elements.append(f'<path d="{path_d}" fill="{col}" stroke="#ffffff" stroke-width="2"/>')

        # Legend entry
        pct_str = f"{fraction * 100:.1f}%"
        ly = legend_y + i * 26
        elements.append(f'<rect x="{legend_x}" y="{ly}" width="12" height="12" rx="3" fill="{col}"/>')
        elements.append(f'<text x="{legend_x + 18}" y="{ly + 10}" font-size="11" fill="#475569">{html.escape(lbl)} ({pct_str})</text>')

        start_angle = end_angle

    if donut:
        elements.append(f'<text x="{cx}" y="{cy - 4}" text-anchor="middle" font-size="16" font-weight="700" fill="#0f172a">{total:g}</text>')
        elements.append(f'<text x="{cx}" y="{cy + 14}" text-anchor="middle" font-size="10" font-weight="500" fill="#94a3b8">TOTAL</text>')

    return _svg_wrapper("\n  ".join(elements), width, height, title)

def render_kpi_card(title: str, value: str, delta: str = "", subtitle: str = "",
                    palette: str = "corporate_blue", width: int = 240, height: int = 140) -> str:
    """Renders a standalone visual KPI Metric Card."""
    colors = get_palette(palette)
    acc = colors[0]

    delta_elem = ""
    if delta:
        is_pos = delta.startswith("+") or "up" in delta.lower() or "gain" in delta.lower()
        d_bg = "#dcfce7" if is_pos else "#fee2e2"
        d_fg = "#15803d" if is_pos else "#b91c1c"
        delta_elem = f'<rect x="{width - 76}" y="20" width="60" height="20" rx="10" fill="{d_bg}"/><text x="{width - 46}" y="34" text-anchor="middle" font-size="10" font-weight="700" fill="{d_fg}">{html.escape(delta)}</text>'

    sub_elem = ""
    if subtitle:
        sub_elem = f'<text x="20" y="{height - 20}" font-size="11" fill="#64748b">{html.escape(subtitle)}</text>'

    inner = f"""
  <rect x="0" y="0" width="{width}" height="{height}" rx="12" fill="#ffffff" stroke="#e2e8f0" stroke-width="1.2"/>
  <rect x="0" y="0" width="6" height="{height}" rx="3" fill="{acc}"/>
  <text x="20" y="32" font-size="11" font-weight="700" letter-spacing="0.5" fill="{acc}">{html.escape(title.upper())}</text>
  {delta_elem}
  <text x="20" y="78" font-size="30" font-weight="800" fill="#0f172a">{html.escape(value)}</text>
  {sub_elem}
"""
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" height="{height}" style="font-family:-apple-system,Segoe UI,Roboto,sans-serif;">{inner}</svg>'
