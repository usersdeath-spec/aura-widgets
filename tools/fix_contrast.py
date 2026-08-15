#!/usr/bin/env python3
"""
Automated legibility remediation.

Finding: 8 families declare a surface gradient whose luminance range is wider than any single ink
colour can survive. Text is drawn across the whole widget, so a stop the ink cannot clear is a
region where the widget is unreadable — regardless of how good the gradient looks empty.

Remediation: compress the offending stop's lightness (HSL L, hue and saturation untouched) by the
minimum amount that clears WCAG 3:1 against the family's ink. Compressing rather than recolouring
preserves the family's identity — Solar stays a hot core bleeding to red, it just stops being
white-hot in the middle where the numerals sit.

Writes the corrected hex values back into the family sources and prints every change.
"""
import re, glob, colorsys, sys

FLOOR = 3.05  # a hair above 3.0 so rounding never lands a family back under the bar

def lum(rgb):
    def ch(v):
        s = v / 255
        return s / 12.92 if s <= 0.03928 else ((s + 0.055) / 1.055) ** 2.4
    r, g, b = rgb
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)

def contrast(a, b):
    la, lb = lum(a), lum(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)

def to_rgb(hexstr):
    v = int(hexstr, 16) & 0xFFFFFF
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)

def to_hex(rgb, original):
    alpha = int(original, 16) >> 24 & 0xFF
    return f"0x{alpha:02X}{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"

def shift_lightness(rgb, delta):
    r, g, b = [c / 255 for c in rgb]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    l = max(0.0, min(1.0, l + delta))
    return tuple(int(round(c * 255)) for c in colorsys.hls_to_rgb(h, l, s))

def balanced(src, start):
    """Extract the argument list of a call whose '(' is at `start`."""
    depth, i = 0, start
    while i < len(src):
        if src[i] == '(': depth += 1
        elif src[i] == ')':
            depth -= 1
            if depth == 0: return src[start + 1:i], i
        i += 1
    return "", start

changes = []
for path in sorted(glob.glob("core/data/src/main/kotlin/com/prism/studio/data/catalog/Families*.kt")):
    src = open(path).read()
    out = src
    for block in re.split(r'\ninternal val \w+ = family\(', src)[1:]:
        name = re.search(r'name = "([^"]+)"', block).group(1)
        base = block.split("wallpapers =")[0]
        ink_m = re.search(r'ink = c\((0x[0-9A-Fa-f]+)\)', base)
        if not ink_m:
            continue
        ink = to_rgb(ink_m.group(1))

        m = re.search(r'surface = grad\(', base)
        if not m:
            continue
        args, _ = balanced(base, m.end() - 1)
        stops = re.findall(r'to (0x[0-9A-Fa-f]+)', args)
        if not stops:
            continue

        for stop in stops:
            rgb = to_rgb(stop)
            ratio = contrast(ink, rgb)
            if ratio >= FLOOR:
                continue
            # Move away from the ink's luminance: darken if ink is light, lighten if ink is dark.
            direction = -0.01 if lum(ink) > lum(rgb) or lum(ink) > 0.4 else 0.01
            fixed, steps = rgb, 0
            while contrast(ink, fixed) < FLOOR and steps < 100:
                fixed = shift_lightness(fixed, direction)
                steps += 1
            new_hex = to_hex(fixed, stop)
            if new_hex.lower() == stop.lower():
                continue
            # Replace only inside this family's surface declaration.
            old_args = args
            new_args = old_args.replace(stop, new_hex)
            out = out.replace(old_args, new_args, 1)
            args = new_args
            changes.append((name, stop, new_hex, ratio, contrast(ink, fixed)))
    if out != src:
        open(path, 'w').write(out)

if not changes:
    print("No contrast remediation needed.")
else:
    print(f"{len(changes)} gradient stops adjusted:\n")
    for name, old, new, before, after in changes:
        print(f"  {name:<18} {old} -> {new}   {before:.2f}:1 -> {after:.2f}:1")
