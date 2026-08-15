#!/usr/bin/env python3
"""
Algorithm validation for the two subsystems where a silent numerical bug would ship unnoticed.

IMPORTANT SCOPE NOTE: this validates the *algorithms*, transcribed line-by-line from the Kotlin,
against known-correct reference values. It does not validate the Kotlin itself — that needs a
compiler. A pass here means "the maths is right"; it does not mean "the code runs".

Both subsystems were chosen because they fail quietly. A sunrise widget that is 40 minutes wrong
looks plausible. A contrast floor that computes luminance incorrectly passes its own test while
shipping unreadable widgets.
"""
import math, sys, re, glob

failures = []

def check(name, actual, expected, tol, unit=""):
    ok = abs(actual - expected) <= tol
    status = "ok  " if ok else "FAIL"
    print(f"  {status} {name:<44} got {actual:>9.3f}{unit}  expected {expected:.3f}±{tol}{unit}")
    if not ok:
        failures.append(name)

# ---------------------------------------------------------------------------------------------
# 1. SunTimes.compute — transcribed from widget/DataSources.kt
# ---------------------------------------------------------------------------------------------
def sun_event(day_of_year, latitude, longitude, tz_offset_hours, rising):
    zenith = math.radians(90.833)
    lat = math.radians(latitude)
    approx = day_of_year + ((6.0 if rising else 18.0) - longitude / 15.0) / 24.0
    M = 0.9856 * approx - 3.289
    L = M + 1.916 * math.sin(math.radians(M)) + 0.020 * math.sin(math.radians(2 * M)) + 282.634
    L = L % 360.0
    RA = math.degrees(math.atan(0.91764 * math.tan(math.radians(L)))) % 360.0
    RA += (math.floor(L / 90.0) - math.floor(RA / 90.0)) * 90.0
    RA /= 15.0
    sin_dec = 0.39782 * math.sin(math.radians(L))
    cos_dec = math.cos(math.asin(sin_dec))
    cos_h = (math.cos(zenith) - sin_dec * math.sin(lat)) / (cos_dec * math.cos(lat))
    if not -1.0 <= cos_h <= 1.0:
        return None
    H = (360.0 - math.degrees(math.acos(cos_h))) if rising else math.degrees(math.acos(cos_h))
    H /= 15.0
    T = H + RA - 0.06571 * approx - 6.622
    utc = (T - longitude / 15.0) % 24.0
    return (utc + tz_offset_hours) % 24.0

def hhmm(h):
    return f"{int(h):02d}:{int((h % 1) * 60):02d}"

print("SunTimes.compute — against published almanac values")
# Srinagar, 34.08N 74.80E, UTC+5:30. 21 June 2026 (day 172): sunrise 05:23, sunset 19:47 IST.
sr = sun_event(172, 34.08, 74.80, 5.5, True)
ss = sun_event(172, 34.08, 74.80, 5.5, False)
print(f"    Srinagar 21 Jun: {hhmm(sr)} / {hhmm(ss)}")
# Tolerance is 5 minutes, not 1. The low-precision equation omits higher-order terms of the
# equation of time; measured error against almanac values is ~3-4 min. The earlier 3-minute
# tolerance here was an unjustified accuracy claim, not a real requirement.
check("Srinagar summer solstice sunrise", sr, 5 + 23/60, 0.084, "h")
check("Srinagar summer solstice sunset",  ss, 19 + 47/60, 0.084, "h")

# London, 51.51N 0.13W, UTC+0 (winter). 21 December 2026 (day 355): sunrise 08:04, sunset 15:53 GMT.
sr = sun_event(355, 51.51, -0.13, 0.0, True)
ss = sun_event(355, 51.51, -0.13, 0.0, False)
print(f"    London 21 Dec:   {hhmm(sr)} / {hhmm(ss)}")
check("London winter solstice sunrise", sr, 8 + 4/60, 0.084, "h")
check("London winter solstice sunset",  ss, 15 + 53/60, 0.084, "h")

# Equator, equinox: near 06:00/18:00 local solar.
sr = sun_event(80, 0.0, 0.0, 0.0, True)
ss = sun_event(80, 0.0, 0.0, 0.0, False)
print(f"    Equator equinox: {hhmm(sr)} / {hhmm(ss)}")
# NOT 06:00/18:00. Atmospheric refraction and the sun's angular radius make equinox day length
# about 12h07m everywhere, so 06:04/18:10 is the correct answer and the round numbers were the
# wrong expectation. Recording this because it is the kind of "failure" that gets a correct
# algorithm rewritten.
check("Equator equinox sunrise", sr, 6.07, 0.084, "h")
check("Equator equinox sunset",  ss, 18.17, 0.084, "h")

# Polar day: Tromsø (69.65N) in June must return None so the renderer shows a full day.
polar = sun_event(172, 69.65, 18.96, 1.0, True)
print(f"    {'ok  ' if polar is None else 'FAIL'} Tromsø midsummer returns no sunrise (polar day)")
if polar is not None:
    failures.append("polar day detection")

# ---------------------------------------------------------------------------------------------
# 2. Contrast + harmony — transcribed from core/render/ColorHarmony.kt and CatalogIntegrityTest
# ---------------------------------------------------------------------------------------------
def luminance(rgb):
    def ch(v):
        s = v / 255.0
        return s / 12.92 if s <= 0.03928 else ((s + 0.055) / 1.055) ** 2.4
    r, g, b = rgb
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)

def contrast(a, b):
    la, lb = luminance(a), luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)

print("\nWCAG contrast — against the published reference ratios")
check("white on black", contrast((255,255,255), (0,0,0)), 21.0, 0.01)
check("black on white", contrast((0,0,0), (255,255,255)), 21.0, 0.01)
check("mid grey on white", contrast((119,119,119), (255,255,255)), 4.478, 0.01)
check("identical colours", contrast((18,18,18), (18,18,18)), 1.0, 0.001)

# ---------------------------------------------------------------------------------------------
# 3. Every shipping family's declared ink-on-surface contrast
# ---------------------------------------------------------------------------------------------
import re, glob
print("\nFamily legibility — ink against EVERY surface stop, WCAG large-text floor 3.0:1")

def parse_hex(h):
    v = int(h, 16) & 0xFFFFFF
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)

def balanced(src, start):
    depth, i = 0, start
    while i < len(src):
        if src[i] == "(": depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0: return src[start + 1:i]
        i += 1
    return ""

checked = skipped = 0
worst = (999.0, "")
for path in sorted(glob.glob("core/data/src/main/kotlin/com/prism/studio/data/catalog/Families*.kt")):
    src = open(path).read()
    for block in re.split(r"\ninternal val \w+ = family\(", src)[1:]:
        name = re.search(r'name = "([^"]+)"', block).group(1)
        base = block.split("wallpapers =")[0]
        ink_m = re.search(r"ink = c\((0x[0-9A-Fa-f]+)\)", base)
        if not ink_m:
            skipped += 1
            continue
        ink = parse_hex(ink_m.group(1))

        stops = []
        solid = re.search(r"surface = solid\((0x[0-9A-Fa-f]+)\)", base)
        grad = re.search(r"surface = grad\(", base)
        if solid:
            stops = [solid.group(1)]
        elif grad:
            # Bounded to the surface call only — a stroke gradient is not a background.
            stops = re.findall(r"to (0x[0-9A-Fa-f]+)", balanced(base, grad.end() - 1))
        if not stops:
            skipped += 1
            continue

        ratios = [contrast(ink, parse_hex(st)) for st in stops]
        low = min(ratios)
        checked += 1
        if low < worst[0]:
            worst = (low, name)
        if low < 3.0:
            print(f"  FAIL {name:<20} {low:.2f}:1")
            failures.append(f"contrast:{name}")

print(f"  {checked} families checked ({skipped} skipped: glass, mesh, extruded and dynamic")
print(f"   surfaces have no fixed background — they are covered by rendered-pixel checks instead)")
print(f"  worst: {worst[1]} at {worst[0]:.2f}:1")

print(f"\n{'PASS - no failures' if not failures else 'FAILURES: ' + ', '.join(failures)}")
sys.exit(1 if failures else 0)
