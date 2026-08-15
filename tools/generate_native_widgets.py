#!/usr/bin/env python3
"""
Generates native RemoteViews widgets from the authored catalog.

WHY THIS EXISTS
---------------
Teardown of the two top-selling competitors (Glass Widgets, OneUI Widgets) showed they do not
rasterise anything. Each of their ~466 and ~309 designs is:

  * its own AppWidgetProvider,
  * with its own `_info.xml` declaring `previewLayout`,
  * pointing at a hand-authored RemoteViews layout of real Views.

That buys them four things our bitmap engine cannot have:

  1. **Crisp text at any size.** Real TextViews, hinted and subpixel-rendered by the platform.
  2. **Every design visible in the system widget picker**, each with a live preview, because
     `previewLayout` renders the actual widget. Ours showed five generic rows.
  3. **No bitmaps.** No allocation on scroll, no cache, no out-of-memory — which is our crash.
  4. **Self-updating clocks.** A `TextClock` ticks with zero code, zero workers and zero battery.

We keep the Canvas engine for what only it can do — glass, mesh, caustics, backdrop blur — but
anything that is "text on a shape" should be a real layout. This generator emits those from the
same `WidgetStyle` data the renderer uses, so there is still one source of truth.

WHAT IT EMITS, per family:
    res/drawable/nw_bg_<family>.xml     the surface, as a GradientDrawable
    res/layout/nw_clock_<family>.xml    a TextClock + date, styled by the family
    res/xml/nw_clock_<family>_info.xml  provider info, with previewLayout set
    generated/NativeClockProviders.kt   one provider class per family
    AndroidManifest.xml                 one <receiver> per family, merged by AGP
"""
import re, os, glob, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WIDGET = f"{ROOT}/widget/src/main"
CATALOG = f"{ROOT}/core/data/src/main/kotlin/com/prism/studio/data/catalog"

def argb(hexstr):
    """0xAARRGGBB -> #AARRGGBB"""
    v = int(hexstr, 16)
    return "#%08X" % (v & 0xFFFFFFFF)

# Body face per token. `@font/x` resolves to a downloadable Google font — 0 bytes of APK, licensed,
# cached system-wide. The competitors bundle 74 files and 3.5 MB to get less variety than this.
FONT_FOR = {
    "Grotesk": "@font/grotesk",
    "GroteskDisplay": "@font/grotesk_display",
    "Serif": "@font/serif",
    "Mono": "@font/mono",
    "Rounded": "@font/rounded",
    "Condensed": "@font/condensed",
}

# Display face per family, chosen for the family's own idea rather than assigned round-robin. This
# is the difference between 44 palettes of one clock and 44 clocks: a dot-matrix AMOLED clock and a
# stencil Brutalist clock are different designs, not different colours.
DISPLAY_FOR = {
    "amoled-black": "@font/display_dot",
    "monolith": "@font/display_stencil",
    "brutalist-slab": "@font/display_stencil",
    "swiss-grid": "@font/display_stencil",
    "cyberpunk-neon": "@font/display_tech",
    "hud-tactical": "@font/display_tech",
    "rgb-gaming": "@font/display_tech",
    "blueprint": "@font/display_tech",
    "carbon": "@font/display_tech",
    "deep-space": "@font/display_tech",
    "pixel-retro": "@font/display_pixel",
    "crt-amber": "@font/display_dot",
    "titanium": "@font/display_stencil",
    "chrome-liquid": "@font/display_stencil",
    "candy-pop": "@font/display_slab",
    "bauhaus-primary": "@font/display_slab",
    "holographic": "@font/display_slab",
    "solar": "@font/display_slab",
    "japanese-zen": "@font/display_script",
    "ink-serif": "@font/display_script",
    "velvet": "@font/display_script",
    "mirage": "@font/display_script",
    "luxury-gold": "@font/serif",
    "marble": "@font/serif",
    "gemstone": "@font/serif",
    "terracotta": "@font/serif",
}
GRAVITY_FOR = {"Start": "start|center_vertical", "Center": "center", "End": "end|center_vertical"}

def parse_families():
    families = []
    for path in sorted(glob.glob(f"{CATALOG}/Families*.kt")):
        src = open(path).read()
        for block in re.split(r"\ninternal val \w+ = family\(", src)[1:]:
            base = block.split("wallpapers =")[0]
            fid = re.search(r'id = "([\w-]+)"', base).group(1)
            name = re.search(r'name = "([^"]+)"', base).group(1)

            solid = re.search(r"surface = solid\((0x[0-9A-Fa-f]+)\)", base)
            grad = re.search(r"surface = grad\(\s*([\s\S]*?)\)", base)
            stops = []
            if solid:
                stops = [solid.group(1)]
            elif grad:
                stops = re.findall(r"to (0x[0-9A-Fa-f]+)", grad.group(1))[:3]
            if not stops:
                # Glass, mesh and extruded surfaces stay with the Canvas renderer: a GradientDrawable
                # cannot express a caustic or a backdrop blur, and faking it would look worse than
                # the bitmap does.
                continue

            ink = re.search(r"ink = c\((0x[0-9A-Fa-f]+)\)", base)
            muted = re.search(r"inkMuted = c\((0x[0-9A-Fa-f]+)\)", base)
            radius = re.search(r"cornerRadiusDp = ([\d.]+)f", base)
            padding = re.search(r"paddingDp = ([\d.]+)f", base)
            font = re.search(r"fontFamily = FontFamilyToken\.(\w+)", base)
            weight = re.search(r"fontWeight = (\d+)", base)
            align = re.search(r"alignment = Alignment\.(\w+)", base)
            stroke = re.search(r"stroke = hairline\((0x[0-9A-Fa-f]+)", base)

            families.append({
                "id": fid, "name": name, "stops": stops,
                "ink": ink.group(1) if ink else "0xFFFFFFFF",
                "muted": muted.group(1) if muted else "0x99FFFFFF",
                "radius": float(radius.group(1)) if radius else 20.0,
                "padding": float(padding.group(1)) if padding else 16.0,
                "font": FONT_FOR.get(font.group(1) if font else "Grotesk", "@font/grotesk"),
                # Falls back to the body face, so a family without an entry is still coherent.
                "display": DISPLAY_FOR.get(fid, FONT_FOR.get(font.group(1) if font else "Grotesk", "@font/grotesk")),
                "bold": int(weight.group(1)) >= 600 if weight else False,
                "gravity": GRAVITY_FOR.get(align.group(1) if align else "Start", "start|center_vertical"),
                "stroke": stroke.group(1) if stroke else None,
            })
    return families

def background_xml(f):
    if len(f["stops"]) >= 2:
        mid = f'\n        android:centerColor="{argb(f["stops"][1])}"' if len(f["stops"]) >= 3 else ""
        fill = f'''    <gradient
        android:type="linear"
        android:angle="270"
        android:startColor="{argb(f["stops"][0])}"{mid}
        android:endColor="{argb(f["stops"][-1])}" />'''
    else:
        fill = f'    <solid android:color="{argb(f["stops"][0])}" />'
    stroke = f'\n    <stroke android:width="1dp" android:color="{argb(f["stroke"])}" />' if f["stroke"] else ""
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from the {f["name"]} family's WidgetStyle. Do not edit; run tools/generate_native_widgets.py. -->
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
{fill}
    <corners android:radius="{f["radius"]:.0f}dp" />{stroke}
</shape>
'''

# =================================================================================================
# COMPOSITIONS
# =================================================================================================
# The cross-check found our real gap: 44 widgets against their 510, and — more damaging — one clock
# design per family, where their Digital Clock 1/3/4/5 are genuinely different compositions.
#
# So a family now emits several *compositions* per type, not one. A composition is a different
# arrangement of the same data: stacked, split, banner, dial-less minimal. That is what their
# variety actually is, and it costs us layout templates rather than hand-authored files.
#
# 44 families x 5 clock compositions + 4 other types = ~400 native widgets from one generator.

def clock_stacked(f, key):
    """Hours over minutes, poster-style. No colon — one less thing to align at small sizes."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="hh"
        android:format24Hour="HH"
        android:textColor="{argb(f["ink"])}"
        android:textSize="44sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="mm"
        android:format24Hour="mm"
        android:textColor="{argb(f["muted"])}"
        android:textSize="44sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />'''

def clock_hero(f, key):
    """Time alone, as large as the cell allows. The most-placed widget shape there is."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="h:mm"
        android:format24Hour="HH:mm"
        android:textColor="{argb(f["ink"])}"
        android:textSize="46sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />'''

def clock_dated(f, key):
    """Time with the date beneath, in muted ink."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="h:mm"
        android:format24Hour="HH:mm"
        android:textColor="{argb(f["ink"])}"
        android:textSize="34sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="EEEE, d MMMM"
        android:format24Hour="EEEE, d MMMM"
        android:textColor="{argb(f["muted"])}"
        android:textSize="12sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}"
        android:paddingTop="2dp" />'''

def clock_banner(f, key):
    """A 4x1 strip: date on the left, time on the right. Fits above a row of app icons."""
    return f'''    <TextClock
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:format12Hour="EEE d MMM"
        android:format24Hour="EEE d MMM"
        android:textColor="{argb(f["muted"])}"
        android:textSize="13sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="h:mm"
        android:format24Hour="HH:mm"
        android:textColor="{argb(f["ink"])}"
        android:textSize="26sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />'''

def clock_seconds(f, key):
    """With seconds. TextClock ticks these itself, so it costs nothing that a minute clock doesn't."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="h:mm:ss"
        android:format24Hour="HH:mm:ss"
        android:textColor="{argb(f["ink"])}"
        android:textSize="30sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />'''

def date_card(f, key):
    """Weekday, big day number, month. The arrangement every desk calendar settles on."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="EEE"
        android:format24Hour="EEE"
        android:textColor="{argb(f["muted"])}"
        android:textSize="13sp"
        android:letterSpacing="0.12"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="d"
        android:format24Hour="d"
        android:textColor="{argb(f["ink"])}"
        android:textSize="48sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="bold" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="MMMM"
        android:format24Hour="MMMM"
        android:textColor="{argb(f["muted"])}"
        android:textSize="13sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}" />'''

def month_year(f, key):
    """Month and year stacked — a calendar widget that needs no calendar permission."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="MMMM"
        android:format24Hour="MMMM"
        android:textColor="{argb(f["ink"])}"
        android:textSize="26sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="yyyy"
        android:format24Hour="yyyy"
        android:textColor="{argb(f["muted"])}"
        android:textSize="15sp"
        android:letterSpacing="0.2"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}" />'''


def world_dual(f, key):
    """Two zones. TextClock takes android:timeZone, so each ticks itself — a world clock that
    costs exactly as much as a normal clock, which is nothing."""
    return f'''    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="LONDON"
            android:textColor="{argb(f["muted"])}"
            android:textSize="11sp"
            android:letterSpacing="0.14"
            android:fontFamily="{f["font"]}" />

        <TextClock
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:timeZone="Europe/London"
            android:format12Hour="h:mm"
            android:format24Hour="HH:mm"
            android:textColor="{argb(f["ink"])}"
            android:textSize="26sp"
            android:includeFontPadding="false"
            android:fontFamily="{f["display"]}" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingTop="6dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="NEW YORK"
            android:textColor="{argb(f["muted"])}"
            android:textSize="11sp"
            android:letterSpacing="0.14"
            android:fontFamily="{f["font"]}" />

        <TextClock
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:timeZone="America/New_York"
            android:format12Hour="h:mm"
            android:format24Hour="HH:mm"
            android:textColor="{argb(f["ink"])}"
            android:textSize="26sp"
            android:includeFontPadding="false"
            android:fontFamily="{f["display"]}" />
    </LinearLayout>'''

def world_trio(f, key):
    """Three zones stacked. The classic travel widget, still with no code behind it."""
    rows = ""
    for label, zone in (("TOKYO", "Asia/Tokyo"), ("LONDON", "Europe/London"), ("LOS ANGELES", "America/Los_Angeles")):
        rows += f'''    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingBottom="4dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="{label}"
            android:textColor="{argb(f["muted"])}"
            android:textSize="10sp"
            android:letterSpacing="0.12"
            android:fontFamily="{f["font"]}" />

        <TextClock
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:timeZone="{zone}"
            android:format12Hour="h:mm"
            android:format24Hour="HH:mm"
            android:textColor="{argb(f["ink"])}"
            android:textSize="20sp"
            android:includeFontPadding="false"
            android:fontFamily="{f["display"]}" />
    </LinearLayout>

'''
    return rows.rstrip()

def clock_ampm(f, key):
    """Time with AM/PM as a separate, smaller element. The arrangement most watch faces use, and it
    only works when the two can be styled independently — which a single format string cannot do."""
    return f'''    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="bottom">

        <TextClock
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:format12Hour="h:mm"
            android:format24Hour="HH:mm"
            android:textColor="{argb(f["ink"])}"
            android:textSize="40sp"
            android:includeFontPadding="false"
            android:fontFamily="{f["display"]}"
            android:textStyle="{"bold" if f["bold"] else "normal"}" />

        <TextClock
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:format12Hour="a"
            android:format24Hour=""
            android:textColor="{argb(f["muted"])}"
            android:textSize="14sp"
            android:includeFontPadding="false"
            android:paddingStart="4dp"
            android:paddingBottom="6dp"
            android:fontFamily="{f["font"]}" />
    </LinearLayout>'''

def year_progress(f, key):
    """Week number and day of year. Niche, and the sort of thing people screenshot — which is the
    only marketing that works at this price."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="'WEEK' w"
        android:format24Hour="'WEEK' w"
        android:textColor="{argb(f["muted"])}"
        android:textSize="12sp"
        android:letterSpacing="0.14"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="D"
        android:format24Hour="D"
        android:textColor="{argb(f["ink"])}"
        android:textSize="46sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="DAY OF YEAR"
        android:textColor="{argb(f["muted"])}"
        android:textSize="10sp"
        android:letterSpacing="0.16"
        android:fontFamily="{f["font"]}" />'''

def stopwatch(f, key):
    """A Chronometer, which counts by itself. Same trick as TextClock: the platform drives it, so a
    live-ticking widget needs no worker, no alarm and no battery."""
    return f'''    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="ELAPSED"
        android:textColor="{argb(f["muted"])}"
        android:textSize="11sp"
        android:letterSpacing="0.16"
        android:fontFamily="{f["font"]}" />

    <Chronometer
        android:id="@+id/nw_chrono"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="{argb(f["ink"])}"
        android:textSize="34sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />'''

def date_wide(f, key):
    """A 4x1 date strip. Weekday and full date, nothing else — the widget people put above a dock."""
    return f'''    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="EEEE"
        android:format24Hour="EEEE"
        android:textColor="{argb(f["ink"])}"
        android:textSize="22sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />

    <TextClock
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:gravity="end"
        android:format12Hour="d MMMM yyyy"
        android:format24Hour="d MMMM yyyy"
        android:textColor="{argb(f["muted"])}"
        android:textSize="13sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}" />'''

# (composition id, label suffix, builder, orientation, cells w, cells h, min w dp, min h dp)
COMPOSITIONS = [
    ("clock_hero",    "Clock",          clock_hero,    "vertical",   4, 2, 180,  90),
    ("clock_stacked", "Stacked Clock",  clock_stacked, "vertical",   2, 2,  90,  90),
    ("clock_dated",   "Clock & Date",   clock_dated,   "vertical",   4, 2, 180,  90),
    ("clock_banner",  "Clock Banner",   clock_banner,  "horizontal", 4, 1, 180,  40),
    ("clock_seconds", "Clock Seconds",  clock_seconds, "vertical",   4, 2, 180,  90),
    ("date_card",     "Date",           date_card,     "vertical",   2, 2,  90,  90),
    ("month_year",    "Month",          month_year,    "vertical",   2, 2,  90,  90),
    ("world_dual",    "World Clock",    world_dual,    "vertical",   4, 2, 180,  90),
    ("world_trio",    "World Clock 3",  world_trio,    "vertical",   4, 2, 180,  90),
    ("clock_ampm",    "Clock AM/PM",    clock_ampm,    "vertical",   4, 2, 180,  90),
    ("year_progress", "Day of Year",    year_progress, "vertical",   2, 2,  90,  90),
    ("stopwatch",     "Elapsed",        stopwatch,     "vertical",   2, 2,  90,  90),
    ("date_wide",     "Date Strip",     date_wide,     "horizontal", 4, 1, 180,  40),
]

def layout_xml_for(f, comp):
    comp_id, label, builder, orientation, cw, ch, mw, mh = comp
    key = f["id"].replace("-", "_")
    gravity = "center" if orientation == "vertical" and cw == ch else f["gravity"]
    if orientation == "horizontal":
        gravity = "center_vertical"
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  {f["name"]} — {label}. Generated; do not edit.
  Run tools/generate_native_widgets.py to regenerate.

  Real Views, not a rasterised bitmap: text is hinted and subpixel-rendered by the platform, the
  system draws the preview in the widget picker, and a TextClock ticks itself with no worker, no
  alarm and no battery cost.
-->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/background"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="{orientation}"
    android:gravity="{gravity}"
    android:background="@drawable/nw_bg_{key}"
    android:padding="{f["padding"]:.0f}dp">

{builder(f, key)}
</LinearLayout>
'''

def layout_xml(f):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  {f["name"]} clock. Generated; do not edit.

  A TextClock rather than a TextView: the platform ticks it, so this widget needs no worker, no
  alarm, no broadcast receiver and no code at all. It is also the reason the time is never a minute
  stale, which a scheduled bitmap redraw cannot guarantee.
-->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="{f["gravity"]}"
    android:background="@drawable/nw_bg_{f["id"].replace("-", "_")}"
    android:padding="{f["padding"]:.0f}dp">

    <TextClock
        android:id="@+id/nw_time"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="h:mm"
        android:format24Hour="HH:mm"
        android:textColor="{argb(f["ink"])}"
        android:textSize="34sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["display"]}"
        android:textStyle="{"bold" if f["bold"] else "normal"}" />

    <TextClock
        android:id="@+id/nw_date"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format12Hour="EEE, d MMM"
        android:format24Hour="EEE, d MMM"
        android:textColor="{argb(f["muted"])}"
        android:textSize="12sp"
        android:includeFontPadding="false"
        android:fontFamily="{f["font"]}"
        android:paddingTop="2dp" />
</LinearLayout>
'''

def info_xml_for(name, cw, ch, mw, mh):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  previewLayout is the whole point: the system widget picker renders the real widget, so every one
  of these is browsable and previewable outside our app. Generated; do not edit.
-->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="{mw}dp"
    android:minHeight="{mh}dp"
    android:targetCellWidth="{cw}"
    android:targetCellHeight="{ch}"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen|keyguard"
    android:initialLayout="@layout/{name}"
    android:previewLayout="@layout/{name}"
    android:description="@string/widget_description_medium"
    android:updatePeriodMillis="0" />
'''

def info_xml(f):
    key = f["id"].replace("-", "_")
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  previewLayout is the whole point: the system widget picker renders the real widget, so all of
  these designs are browsable outside our app. Generated; do not edit.
-->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="90dp"
    android:targetCellWidth="4"
    android:targetCellHeight="2"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen|keyguard"
    android:initialLayout="@layout/nw_clock_{key}"
    android:previewLayout="@layout/nw_clock_{key}"
    android:description="@string/widget_description_medium"
    android:updatePeriodMillis="0" />
'''

def xml_escape(text):
    """Escape for an XML attribute. `&` in a label like "Clock & Date" otherwise starts an entity
    and AAPT rejects the whole manifest."""
    return (text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace('"', "&quot;"))


def main():
    families = parse_families()
    os.makedirs(f"{WIDGET}/res/drawable", exist_ok=True)
    os.makedirs(f"{WIDGET}/res/layout", exist_ok=True)
    os.makedirs(f"{WIDGET}/res/xml", exist_ok=True)
    gen_dir = f"{WIDGET}/kotlin/com/prism/studio/widget/generated"
    os.makedirs(gen_dir, exist_ok=True)

    receivers, providers = [], []
    for f in families:
        key = f["id"].replace("-", "_")
        open(f"{WIDGET}/res/drawable/nw_bg_{key}.xml", "w").write(background_xml(f))

        for comp in COMPOSITIONS:
            comp_id, label, _builder, _orientation, cw, ch, mw, mh = comp
            name = f"nw_{comp_id}_{key}"
            cls = "".join(p.capitalize() for p in f["id"].split("-")) + \
                  "".join(p.capitalize() for p in comp_id.split("_")) + "Provider"

            open(f"{WIDGET}/res/layout/{name}.xml", "w").write(layout_xml_for(f, comp))
            open(f"{WIDGET}/res/xml/{name}_info.xml", "w").write(info_xml_for(name, cw, ch, mw, mh))

            display = xml_escape(f'{f["name"]} {label}')
            providers.append((cls, display))
            receivers.append(f'''        <receiver
            android:name="com.prism.studio.widget.generated.{cls}"
            android:exported="true"
            android:label="{display}">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/{name}_info" />
        </receiver>''')

    body = "\n\n".join(
        f'/** {name} clock. */\nclass {cls} : NativeWidgetProvider()' for cls, name in providers
    )
    open(f"{gen_dir}/NativeClockProviders.kt", "w").write(f'''package com.prism.studio.widget.generated

import com.prism.studio.widget.NativeWidgetProvider

/**
 * One provider per design, generated from the catalog.
 *
 * Android's widget picker lists one row per declared provider, so this is what makes every design
 * browsable from the home screen rather than only inside our app. Each has its own label and its
 * own previewLayout, so the picker shows the actual widget.
 *
 * Generated by tools/generate_native_widgets.py. Do not edit.
 */

{body}
''')

    open(f"{WIDGET}/AndroidManifest.xml", "w").write(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!-- Generated receivers, merged into the app manifest by AGP. Do not edit. -->\n'
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <application>\n' + "\n".join(receivers) + '\n    </application>\n</manifest>\n'
    )

    print(f"{len(families)} families x {len(COMPOSITIONS)} compositions = {len(providers)} native widgets")
    print(f"  skipped: glass/mesh/extruded families stay on the Canvas renderer")

if __name__ == "__main__":
    main()
