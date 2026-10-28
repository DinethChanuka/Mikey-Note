#!/usr/bin/env python3
"""
Mikey Note — Object & Component Render Helpers
==============================================
Standalone, modular rendering helpers for every document object and UI component:
- Cards & Callouts (Info, Warning, Tip, Quote, Stat Tiles)
- Shapes & Flow Diagrams (Processes, Decision Nodes, Connectors)
- Mathematical Formulas & Scientific Symbols (Fractions, Radicals, Summations)
- Tables, Badges, and Ribbon Miniature Preview Cards
"""

import html
from typing import List, Dict, Any, Optional

THEME_PALETTES = {
    "corporate_blue": {"accent": "#2563eb", "bg": "#eff6ff", "border": "#bfdbfe", "text": "#1e40af"},
    "emerald_mint":   {"accent": "#059669", "bg": "#ecfdf5", "border": "#a7f3d0", "text": "#065f46"},
    "violet_purple":  {"accent": "#7c3aed", "bg": "#f5f3ff", "border": "#ddd6fe", "text": "#5b21b6"},
    "amber_gold":     {"accent": "#d97706", "bg": "#fffbeb", "border": "#fde68a", "text": "#92400e"},
    "crimson_rose":   {"accent": "#e11d48", "bg": "#fff1f2", "border": "#fecdd3", "text": "#9f1239"},
    "dark_slate":     {"accent": "#475569", "bg": "#f8fafc", "border": "#cbd5e1", "text": "#1e293b"},
    "clean_minimal":  {"accent": "#0f172a", "bg": "#ffffff", "border": "#e2e8f0", "text": "#0f172a"},
}

def render_callout_box(text: str, title: str = "NOTE", callout_type: str = "info",
                       palette: str = "corporate_blue") -> str:
    """
    Renders a modern, accessible callout box with a colored accent bar,
    vector icon, and clean typography.
    """
    theme = THEME_PALETTES.get(palette.lower().replace(" ", "_"), THEME_PALETTES["corporate_blue"])

    # Icons per type
    icons = {
        "info": '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>',
        "warning": '<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
        "tip": '<path d="M12 2v4"/><path d="m4.93 4.93 2.83 2.83"/><path d="M2 12h4"/><path d="M12 18a6 6 0 1 0 0-12 6 6 0 0 0 0 12Z"/>',
        "quote": '<path d="M3 21c3 0 7-1 7-8V5c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v6c0 1.1.9 2 2 2 0 4-1.5 6-3 8Z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.1-.9-2-2-2h-4c-1.1 0-2 .9-2 2v6c0 1.1.9 2 2 2 0 4-1.5 6-3 8Z"/>',
    }
    icon_svg = icons.get(callout_type.lower(), icons["info"])

    return f"""<div style="background:{theme['bg']};border:1px solid {theme['border']};border-left:4px solid {theme['accent']};border-radius:8px;padding:14px 18px;margin:16px 0;display:flex;gap:14px;align-items:flex-start;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;box-shadow:0 2px 6px rgba(0,0,0,0.02);">
  <div style="color:{theme['accent']};flex-shrink:0;margin-top:2px;">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{icon_svg}</svg>
  </div>
  <div style="flex:1;">
    <div style="font-weight:700;font-size:12px;letter-spacing:0.04em;text-transform:uppercase;color:{theme['accent']};margin-bottom:4px;">{html.escape(title)}</div>
    <div style="font-size:13.5px;line-height:1.55;color:#334155;">{html.escape(text)}</div>
  </div>
</div>"""

def render_process_flow(steps: List[str], palette: str = "corporate_blue",
                        width: int = 680, height: int = 90) -> str:
    """
    Renders an inline SVG Process Flow sequence with connected gradient steps.
    """
    theme = THEME_PALETTES.get(palette.lower().replace(" ", "_"), THEME_PALETTES["corporate_blue"])
    n = len(steps)
    if n == 0:
        return ""

    acc = theme["accent"]
    slot_w = width / n
    box_w = min(130, slot_w - 28)
    box_h = 44
    box_y = (height - box_h) / 2

    elements = []

    for i, step in enumerate(steps):
        bx = i * slot_w + (slot_w - box_w) / 2
        by = box_y

        # Arrow connector to next step
        if i < n - 1:
            ax1 = bx + box_w + 6
            ax2 = (i + 1) * slot_w + (slot_w - box_w) / 2 - 6
            ay = height / 2
            elements.append(f'<line x1="{ax1:.1f}" y1="{ay:.1f}" x2="{ax2:.1f}" y2="{ay:.1f}" stroke="{acc}" stroke-width="2" stroke-dasharray="3,3"/>')
            elements.append(f'<polygon points="{ax2:.1f},{ay-4:.1f} {ax2+5:.1f},{ay:.1f} {ax2:.1f},{ay+4:.1f}" fill="{acc}"/>')

        # Node Box
        elements.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{box_w:.1f}" height="{box_h:.1f}" rx="8" fill="#ffffff" stroke="{acc}" stroke-width="1.8"/>')
        # Number badge
        elements.append(f'<circle cx="{bx + 14:.1f}" cy="{by + box_h/2:.1f}" r="9" fill="{acc}"/>')
        elements.append(f'<text x="{bx + 14:.1f}" y="{by + box_h/2 + 3.5:.1f}" text-anchor="middle" font-size="10" font-weight="700" fill="#ffffff">{i+1}</text>')
        # Label
        short_lbl = step if len(step) <= 12 else step[:11] + "…"
        elements.append(f'<text x="{bx + 30:.1f}" y="{by + box_h/2 + 4:.1f}" font-size="11.5" font-weight="600" fill="#1e293b">{html.escape(short_lbl)}</text>')

    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="100%" height="{height}" style="max-width:{width}px;margin:16px auto;display:block;font-family:-apple-system,Segoe UI,Roboto,sans-serif;">
  {''.join(elements)}
</svg>"""

def render_math_formula(expression: str, width: int = 340, height: int = 68) -> str:
    """
    Renders high-DPI vector math notation (fractions, roots, integrals).
    """
    # Clean expression rendering
    clean_expr = html.escape(expression.strip())

    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="100%" height="{height}" style="max-width:{width}px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;margin:12px auto;display:block;">
  <text x="24" y="{height/2 + 5}" font-family="'Cambria Math', 'Times New Roman', serif" font-size="19" font-style="italic" fill="#0f172a">{clean_expr}</text>
  <rect x="{width - 56}" y="12" width="44" height="18" rx="4" fill="#e2e8f0"/>
  <text x="{width - 34}" y="24" text-anchor="middle" font-family="-apple-system,sans-serif" font-size="9" font-weight="700" fill="#64748b">MATH</text>
</svg>"""

def render_ribbon_preview_tile(label: str, category: str, color_hex: str = "#2563eb",
                               width: int = 68, height: int = 40) -> str:
    """
    Generates miniature visual preview cards matching the Mikey Note Ribbon Panel tiles.
    """
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <rect width="{width}" height="{height}" rx="6" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1"/>
  <rect x="4" y="4" width="{width-8}" height="{height-18}" rx="4" fill="{color_hex}" opacity="0.15"/>
  <rect x="8" y="8" width="16" height="3" rx="1.5" fill="{color_hex}"/>
  <rect x="8" y="14" width="28" height="2" rx="1" fill="#94a3b8"/>
  <text x="{width/2}" y="{height-4}" text-anchor="middle" font-family="-apple-system,sans-serif" font-size="8.5" font-weight="600" fill="#334155">{html.escape(label)}</text>
</svg>"""
