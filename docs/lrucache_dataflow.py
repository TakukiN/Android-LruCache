#!/usr/bin/env python3
"""
LruCache Data Flow Diagram - Refined Version
Data Flow Cartography - Visualizing the journey of image data through cache architecture
"""

from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import math

# Register fonts
FONT_DIR = r"C:\Users\owner\.claude\plugins\cache\anthropic-agent-skills\example-skills\00756142ab04\skills\canvas-design\canvas-fonts"
pdfmetrics.registerFont(TTFont('Jura-Light', f'{FONT_DIR}/Jura-Light.ttf'))
pdfmetrics.registerFont(TTFont('Jura-Medium', f'{FONT_DIR}/Jura-Medium.ttf'))
pdfmetrics.registerFont(TTFont('IBMPlexMono', f'{FONT_DIR}/IBMPlexMono-Regular.ttf'))
pdfmetrics.registerFont(TTFont('IBMPlexMono-Bold', f'{FONT_DIR}/IBMPlexMono-Bold.ttf'))

# Refined color palette
COLORS = {
    'bg': HexColor('#0d1117'),
    'bg_card': HexColor('#161b22'),
    'amber': HexColor('#f59e0b'),
    'amber_light': HexColor('#fbbf24'),
    'amber_dim': HexColor('#92400e'),
    'teal': HexColor('#14b8a6'),
    'teal_light': HexColor('#5eead4'),
    'teal_dim': HexColor('#0f766e'),
    'silver': HexColor('#8b949e'),
    'silver_light': HexColor('#c9d1d9'),
    'white': HexColor('#f0f6fc'),
    'grid': HexColor('#21262d'),
    'divider': HexColor('#30363d'),
}

def draw_rounded_rect(c, x, y, w, h, r, stroke_color=None, fill_color=None, stroke_width=1.5):
    """Draw a rounded rectangle"""
    c.saveState()
    if fill_color:
        c.setFillColor(fill_color)
    if stroke_color:
        c.setStrokeColor(stroke_color)
        c.setLineWidth(stroke_width)

    path = c.beginPath()
    path.moveTo(x + r, y)
    path.lineTo(x + w - r, y)
    path.arcTo(x + w - 2*r, y, x + w, y + 2*r, -90, 90)
    path.lineTo(x + w, y + h - r)
    path.arcTo(x + w - 2*r, y + h - 2*r, x + w, y + h, 0, 90)
    path.lineTo(x + r, y + h)
    path.arcTo(x, y + h - 2*r, x + 2*r, y + h, 90, 90)
    path.lineTo(x, y + r)
    path.arcTo(x, y, x + 2*r, y + 2*r, 180, 90)
    path.close()

    if fill_color and stroke_color:
        c.drawPath(path, fill=1, stroke=1)
    elif fill_color:
        c.drawPath(path, fill=1, stroke=0)
    elif stroke_color:
        c.drawPath(path, fill=0, stroke=1)
    c.restoreState()

def draw_arrow(c, x1, y1, x2, y2, color, width=2, arrow_size=10):
    """Draw an arrow from (x1,y1) to (x2,y2)"""
    c.saveState()
    c.setStrokeColor(color)
    c.setFillColor(color)
    c.setLineWidth(width)
    c.setLineCap(1)

    c.line(x1, y1, x2, y2)

    angle = math.atan2(y2 - y1, x2 - x1)
    ax1 = x2 - arrow_size * math.cos(angle - math.pi/6)
    ay1 = y2 - arrow_size * math.sin(angle - math.pi/6)
    ax2 = x2 - arrow_size * math.cos(angle + math.pi/6)
    ay2 = y2 - arrow_size * math.sin(angle + math.pi/6)

    path = c.beginPath()
    path.moveTo(x2, y2)
    path.lineTo(ax1, ay1)
    path.lineTo(ax2, ay2)
    path.close()
    c.drawPath(path, fill=1, stroke=0)
    c.restoreState()

def draw_dashed_line(c, x1, y1, x2, y2, color, width=1.5):
    """Draw a dashed line"""
    c.saveState()
    c.setStrokeColor(color)
    c.setLineWidth(width)
    c.setDash([5, 4])
    c.line(x1, y1, x2, y2)
    c.restoreState()

def draw_flow_diagram(c, width, height):
    """Main diagram drawing function"""

    # Background
    c.setFillColor(COLORS['bg'])
    c.rect(0, 0, width, height, fill=1, stroke=0)

    # Subtle dot grid
    c.setFillColor(COLORS['grid'])
    for x in range(40, int(width), 40):
        for y in range(40, int(height), 40):
            c.circle(x, y, 1, fill=1, stroke=0)

    # ==================== HEADER ====================
    c.setFillColor(COLORS['white'])
    c.setFont('Jura-Medium', 32)
    c.drawString(60, height - 65, "LruCache Data Flow Architecture")

    c.setFillColor(COLORS['silver'])
    c.setFont('Jura-Light', 13)
    c.drawString(60, height - 92, "Comparing file-based loading versus memory-cached retrieval in Android image processing")

    # Horizontal divider
    c.setStrokeColor(COLORS['divider'])
    c.setLineWidth(1)
    c.line(60, height - 115, width - 60, height - 115)

    # ==================== CENTER DIVIDER ====================
    center_x = width / 2
    c.setStrokeColor(COLORS['divider'])
    c.setLineWidth(1)
    c.setDash([8, 6])
    c.line(center_x, height - 140, center_x, 120)
    c.setDash([])

    # ==================== LEFT PANEL: WITHOUT CACHE ====================
    left_center = 230
    panel_top = height - 155

    # Panel header
    c.setFillColor(COLORS['amber'])
    c.setFont('IBMPlexMono-Bold', 16)
    c.drawCentredString(left_center, panel_top, "WITHOUT LruCache")

    c.setFillColor(COLORS['amber_light'])
    c.setFont('Jura-Light', 11)
    c.drawCentredString(left_center, panel_top - 22, "File I/O on every access")

    # Total time badge
    draw_rounded_rect(c, left_center - 70, panel_top - 60, 140, 30, 6,
                      stroke_color=COLORS['amber'], fill_color=COLORS['bg_card'], stroke_width=2)
    c.setFillColor(COLORS['amber_light'])
    c.setFont('IBMPlexMono-Bold', 16)
    c.drawCentredString(left_center, panel_top - 50, "11,078 ms")

    # Component 1: Storage
    comp_y = panel_top - 140
    draw_rounded_rect(c, left_center - 80, comp_y, 160, 65, 8,
                      stroke_color=COLORS['amber'], fill_color=COLORS['bg_card'], stroke_width=2)
    c.setFillColor(COLORS['amber'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(left_center, comp_y + 45, "FILE STORAGE")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(left_center, comp_y + 28, "assets/sample.png")
    c.setFillColor(COLORS['silver'])
    c.setFont('Jura-Light', 9)
    c.drawCentredString(left_center, comp_y + 12, "860 KB · PNG compressed")

    # Arrow 1
    draw_arrow(c, left_center, comp_y - 5, left_center, comp_y - 35, COLORS['amber'], 2)
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 8)
    c.drawCentredString(left_center + 50, comp_y - 22, "read stream")

    # Component 2: BitmapFactory
    comp_y2 = comp_y - 100
    draw_rounded_rect(c, left_center - 80, comp_y2, 160, 65, 8,
                      stroke_color=COLORS['amber'], fill_color=COLORS['bg_card'], stroke_width=2)
    c.setFillColor(COLORS['amber'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(left_center, comp_y2 + 45, "BitmapFactory")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(left_center, comp_y2 + 28, "decodeStream()")
    c.setFillColor(COLORS['amber_dim'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(left_center, comp_y2 + 12, "~50-100 ms")

    # Arrow 2
    draw_arrow(c, left_center, comp_y2 - 5, left_center, comp_y2 - 35, COLORS['amber'], 2)
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 8)
    c.drawCentredString(left_center + 50, comp_y2 - 22, "decode")

    # Component 3: Bitmap
    comp_y3 = comp_y2 - 100
    draw_rounded_rect(c, left_center - 80, comp_y3, 160, 65, 8,
                      stroke_color=COLORS['silver'], fill_color=COLORS['bg_card'], stroke_width=1.5)
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(left_center, comp_y3 + 45, "Bitmap")
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(left_center, comp_y3 + 28, "953 × 586 ARGB_8888")
    c.setFont('Jura-Light', 9)
    c.drawCentredString(left_center, comp_y3 + 12, "2.1 MB in memory")

    # Arrow 3
    draw_arrow(c, left_center, comp_y3 - 5, left_center, comp_y3 - 35, COLORS['silver'], 2)

    # Component 4: Image Processing
    comp_y4 = comp_y3 - 105
    draw_rounded_rect(c, left_center - 90, comp_y4, 180, 70, 8,
                      stroke_color=COLORS['amber'], fill_color=COLORS['bg_card'], stroke_width=2)
    c.setFillColor(COLORS['amber'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(left_center, comp_y4 + 52, "ImageProcessor")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(left_center, comp_y4 + 35, "superResolution2x()")
    c.setFillColor(COLORS['silver'])
    c.setFont('Jura-Light', 9)
    c.drawCentredString(left_center, comp_y4 + 18, "Bicubic interpolation")
    c.setFillColor(COLORS['amber_light'])
    c.setFont('IBMPlexMono-Bold', 10)
    c.drawCentredString(left_center, comp_y4 + 3, "~10,500 ms")

    # Arrow 4
    draw_arrow(c, left_center, comp_y4 - 5, left_center, comp_y4 - 35, COLORS['amber'], 2)

    # Component 5: Result
    comp_y5 = comp_y4 - 95
    draw_rounded_rect(c, left_center - 80, comp_y5, 160, 55, 8,
                      stroke_color=COLORS['silver'], fill_color=COLORS['bg_card'], stroke_width=1.5)
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(left_center, comp_y5 + 38, "Result Bitmap")
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(left_center, comp_y5 + 22, "1906 × 1172")
    c.setFont('Jura-Light', 9)
    c.drawCentredString(left_center, comp_y5 + 8, "8.5 MB · 2× upscaled")

    # Repeat loop indicator (left)
    loop_x = left_center + 110
    c.setStrokeColor(COLORS['amber_dim'])
    c.setLineWidth(1.5)
    c.setDash([5, 4])
    c.line(loop_x, comp_y - 10, loop_x + 30, comp_y - 10)
    c.line(loop_x + 30, comp_y - 10, loop_x + 30, comp_y5 + 30)
    c.line(loop_x + 30, comp_y5 + 30, loop_x, comp_y5 + 30)
    c.setDash([])

    # Loop arrow back
    draw_arrow(c, loop_x, comp_y5 + 30, loop_x - 25, comp_y5 + 30, COLORS['amber_dim'], 1.5, 7)

    c.setFillColor(COLORS['amber_dim'])
    c.setFont('IBMPlexMono', 8)
    c.saveState()
    c.translate(loop_x + 45, comp_y3)
    c.rotate(90)
    c.drawCentredString(0, 0, "× 5 iterations")
    c.restoreState()

    # ==================== RIGHT PANEL: WITH CACHE ====================
    right_center = width - 230

    # Panel header
    c.setFillColor(COLORS['teal'])
    c.setFont('IBMPlexMono-Bold', 16)
    c.drawCentredString(right_center, panel_top, "WITH LruCache")

    c.setFillColor(COLORS['teal_light'])
    c.setFont('Jura-Light', 11)
    c.drawCentredString(right_center, panel_top - 22, "Memory-cached source image")

    # Total time badge
    draw_rounded_rect(c, right_center - 70, panel_top - 60, 140, 30, 6,
                      stroke_color=COLORS['teal'], fill_color=COLORS['bg_card'], stroke_width=2)
    c.setFillColor(COLORS['teal_light'])
    c.setFont('IBMPlexMono-Bold', 16)
    c.drawCentredString(right_center, panel_top - 50, "10,470 ms")

    # Component 1: Storage (dimmed - first access only)
    comp_y = panel_top - 140
    draw_rounded_rect(c, right_center - 80, comp_y, 160, 50, 8,
                      stroke_color=COLORS['silver'], fill_color=COLORS['bg_card'], stroke_width=1)
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 10)
    c.drawCentredString(right_center, comp_y + 32, "FILE STORAGE")
    c.setFillColor(COLORS['silver'])
    c.setFont('Jura-Light', 9)
    c.drawCentredString(right_center, comp_y + 15, "first access only")

    # Arrow: Storage to Cache (dashed, first load)
    draw_dashed_line(c, right_center, comp_y - 5, right_center, comp_y - 40, COLORS['silver'])
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 8)
    c.drawCentredString(right_center + 55, comp_y - 25, "initial load")

    # Component 2: LruCache (prominent)
    cache_y = comp_y - 115
    draw_rounded_rect(c, right_center - 95, cache_y, 190, 90, 10,
                      stroke_color=COLORS['teal'], fill_color=COLORS['bg_card'], stroke_width=3)
    c.setFillColor(COLORS['teal'])
    c.setFont('IBMPlexMono-Bold', 13)
    c.drawCentredString(right_center, cache_y + 70, "LruCache<String, Bitmap>")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(right_center, cache_y + 52, "android.util.LruCache")
    c.setFillColor(COLORS['teal_light'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(right_center, cache_y + 35, 'key: "source_image"')
    c.setFillColor(COLORS['silver'])
    c.setFont('Jura-Light', 9)
    c.drawCentredString(right_center, cache_y + 18, "size: maxMemory / 8")
    c.setFillColor(COLORS['teal_light'])
    c.setFont('IBMPlexMono-Bold', 10)
    c.drawCentredString(right_center, cache_y + 3, "< 1 ms access")

    # Arrow: Cache to Bitmap
    draw_arrow(c, right_center, cache_y - 5, right_center, cache_y - 35, COLORS['teal'], 2.5)
    c.setFillColor(COLORS['teal'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(right_center + 45, cache_y - 22, "get()")

    # Component 3: Bitmap
    comp_y3 = cache_y - 100
    draw_rounded_rect(c, right_center - 80, comp_y3, 160, 65, 8,
                      stroke_color=COLORS['teal'], fill_color=COLORS['bg_card'], stroke_width=1.5)
    c.setFillColor(COLORS['teal_light'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(right_center, comp_y3 + 45, "Bitmap")
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(right_center, comp_y3 + 28, "953 × 586 ARGB_8888")
    c.setFillColor(COLORS['teal_dim'])
    c.setFont('Jura-Light', 9)
    c.drawCentredString(right_center, comp_y3 + 12, "from cache · no I/O")

    # Arrow 3
    draw_arrow(c, right_center, comp_y3 - 5, right_center, comp_y3 - 35, COLORS['silver'], 2)

    # Component 4: Image Processing
    comp_y4 = comp_y3 - 105
    draw_rounded_rect(c, right_center - 90, comp_y4, 180, 70, 8,
                      stroke_color=COLORS['teal'], fill_color=COLORS['bg_card'], stroke_width=2)
    c.setFillColor(COLORS['teal'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(right_center, comp_y4 + 52, "ImageProcessor")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(right_center, comp_y4 + 35, "superResolution2x()")
    c.setFillColor(COLORS['silver'])
    c.setFont('Jura-Light', 9)
    c.drawCentredString(right_center, comp_y4 + 18, "Bicubic interpolation")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono-Bold', 10)
    c.drawCentredString(right_center, comp_y4 + 3, "~10,470 ms")

    # Arrow 4
    draw_arrow(c, right_center, comp_y4 - 5, right_center, comp_y4 - 35, COLORS['teal'], 2)

    # Component 5: Result
    comp_y5 = comp_y4 - 95
    draw_rounded_rect(c, right_center - 80, comp_y5, 160, 55, 8,
                      stroke_color=COLORS['silver'], fill_color=COLORS['bg_card'], stroke_width=1.5)
    c.setFillColor(COLORS['silver_light'])
    c.setFont('IBMPlexMono-Bold', 11)
    c.drawCentredString(right_center, comp_y5 + 38, "Result Bitmap")
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 9)
    c.drawCentredString(right_center, comp_y5 + 22, "1906 × 1172")
    c.setFont('Jura-Light', 9)
    c.drawCentredString(right_center, comp_y5 + 8, "8.5 MB · 2× upscaled")

    # Repeat loop indicator (right - from cache)
    loop_x_r = right_center + 110
    c.setStrokeColor(COLORS['teal_dim'])
    c.setLineWidth(1.5)
    c.setDash([5, 4])
    c.line(loop_x_r, cache_y + 45, loop_x_r + 30, cache_y + 45)
    c.line(loop_x_r + 30, cache_y + 45, loop_x_r + 30, comp_y5 + 30)
    c.line(loop_x_r + 30, comp_y5 + 30, loop_x_r, comp_y5 + 30)
    c.setDash([])

    draw_arrow(c, loop_x_r, comp_y5 + 30, loop_x_r - 25, comp_y5 + 30, COLORS['teal_dim'], 1.5, 7)

    c.setFillColor(COLORS['teal_dim'])
    c.setFont('IBMPlexMono', 8)
    c.saveState()
    c.translate(loop_x_r + 45, comp_y4 + 30)
    c.rotate(90)
    c.drawCentredString(0, 0, "× 5 from cache")
    c.restoreState()

    # ==================== BOTTOM: COMPARISON SUMMARY ====================
    summary_y = 75

    # Divider
    c.setStrokeColor(COLORS['divider'])
    c.setLineWidth(1)
    c.line(60, summary_y + 55, width - 60, summary_y + 55)

    # Summary title
    c.setFillColor(COLORS['white'])
    c.setFont('Jura-Medium', 14)
    c.drawString(60, summary_y + 30, "Benchmark Summary")

    # Left comparison
    c.setFillColor(COLORS['amber'])
    c.setFont('IBMPlexMono', 10)
    c.drawString(60, summary_y + 8, "Without cache:")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('Jura-Light', 10)
    c.drawString(170, summary_y + 8, "Storage → Decode → Process = 11,078 ms")

    c.setFillColor(COLORS['teal'])
    c.setFont('IBMPlexMono', 10)
    c.drawString(60, summary_y - 12, "With cache:")
    c.setFillColor(COLORS['silver_light'])
    c.setFont('Jura-Light', 10)
    c.drawString(170, summary_y - 12, "LruCache → Process = 10,470 ms")

    # Right stats
    c.setFillColor(COLORS['white'])
    c.setFont('IBMPlexMono-Bold', 12)
    c.drawString(520, summary_y + 8, "Time saved:")
    c.setFillColor(COLORS['teal_light'])
    c.drawString(620, summary_y + 8, "608 ms per iteration")

    c.setFillColor(COLORS['white'])
    c.setFont('IBMPlexMono-Bold', 12)
    c.drawString(520, summary_y - 12, "Speedup:")
    c.setFillColor(COLORS['teal_light'])
    c.drawString(620, summary_y - 12, "1.06× (6% faster)")

    # Footer
    c.setFillColor(COLORS['silver'])
    c.setFont('IBMPlexMono', 8)
    c.drawCentredString(width / 2, 25, "Data Flow Cartography  ·  Android LruCache Benchmark  ·  953×586 PNG  ·  5 iterations average")

def main():
    output_path = r"C:\Users\owner\AndroidStudioProjects\LruCache\docs\lrucache_dataflow.pdf"

    width, height = landscape(A3)
    c = canvas.Canvas(output_path, pagesize=landscape(A3))

    draw_flow_diagram(c, width, height)

    c.save()
    print(f"PDF saved to: {output_path}")

if __name__ == "__main__":
    main()
