"""
Mikey Note — Python Rendering & Image Helper Suite
==================================================
Standalone, modular helpers for fast chart generation, image fitting & LQIP preloading,
and document object rendering.
"""

from .chart_fast_renderer import (
    render_bar_chart,
    render_line_chart,
    render_pie_donut_chart,
    render_kpi_card,
    get_palette,
    PALETTES
)

from .image_fast_loader import (
    compute_paper_fit,
    get_image_dimensions,
    generate_lqip_placeholder,
    apply_image_filter,
    PAPER_DIMENSIONS,
    DEFAULT_MARGINS
)

from .object_render_helpers import (
    render_callout_box,
    render_process_flow,
    render_math_formula,
    render_ribbon_preview_tile,
    THEME_PALETTES
)

from .fast_render_pipeline import FastRenderPipeline

__all__ = [
    "render_bar_chart",
    "render_line_chart",
    "render_pie_donut_chart",
    "render_kpi_card",
    "get_palette",
    "PALETTES",
    "compute_paper_fit",
    "get_image_dimensions",
    "generate_lqip_placeholder",
    "apply_image_filter",
    "PAPER_DIMENSIONS",
    "DEFAULT_MARGINS",
    "render_callout_box",
    "render_process_flow",
    "render_math_formula",
    "render_ribbon_preview_tile",
    "THEME_PALETTES",
    "FastRenderPipeline"
]
