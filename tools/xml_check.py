#!/usr/bin/env python3
"""
XML validity and resource-reference check.

Added after a generator wrote `android:label="Clock & Date"` into 44 receivers: a bare `&` starts an
entity, so the manifest was malformed and AAPT would have rejected all 308 widgets. None of the
Kotlin-level checkers can see that, because it is not Kotlin.

Checks:
  MALFORMED   any XML in the project that will not parse
  DANGLING    @layout / @drawable / @xml / @string reference with no matching file or declaration
  DUPLICATE   two <receiver> elements with the same android:name
  UNLABELLED  a widget receiver with no android:label, which shows as the package name in the picker
"""
import glob, os, re, sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
issues = []

xml_files = [f for f in glob.glob(f"{ROOT}/**/*.xml", recursive=True) if "/build/" not in f]

for path in xml_files:
    try:
        ET.parse(path)
    except Exception as e:
        issues.append(("MALFORMED", f"{os.path.relpath(path, ROOT)}: {e}"))

# Resource inventory across every module.
existing = {"string": set(), "style": set(), "color": set()}
for kind in ("layout", "drawable", "xml", "mipmap", "font"):
    existing[kind] = set()
for path in glob.glob(f"{ROOT}/**/src/main/res/**/*", recursive=True):
    if not os.path.isfile(path):
        continue
    kind = path.split("/res/")[1].split("/")[0].split("-")[0]
    if kind in existing:
        existing[kind].add(os.path.basename(path).rsplit(".", 1)[0])
for path in glob.glob(f"{ROOT}/**/src/main/res/values*/*.xml", recursive=True):
    src = open(path).read()
    for tag in ("string", "style", "color"):
        existing[tag] |= set(re.findall(rf'<{tag} name="([\w.]+)"', src))

for path in xml_files:
    if "/res/" not in path and "AndroidManifest" not in path:
        continue
    src = open(path).read()
    for kind, name in re.findall(r'@(layout|drawable|xml|string|style|color|mipmap|font)/([\w.]+)', src):
        pool = existing.get(kind, set())
        if name in pool:
            continue
        # mipmap and drawable are interchangeable at reference time on many projects.
        if kind in ("drawable", "mipmap") and name in existing["drawable"] | existing["mipmap"]:
            continue
        issues.append(("DANGLING", f"{os.path.relpath(path, ROOT)}: @{kind}/{name}"))

for path in xml_files:
    if "AndroidManifest" not in path:
        continue
    src = open(path).read()
    names = re.findall(r'<receiver\s+android:name="([\w.]+)"', src)
    for name in {n for n in names if names.count(n) > 1}:
        issues.append(("DUPLICATE", f"{os.path.relpath(path, ROOT)}: receiver {name}"))
    for block in re.findall(r'<receiver[\s\S]*?</receiver>', src):
        if "appwidget.provider" in block and "android:label" not in block:
            who = re.search(r'android:name="([\w.]+)"', block)
            issues.append(("UNLABELLED",
                           f"{os.path.relpath(path, ROOT)}: {who.group(1) if who else '?'}"))

# A class named only in a manifest has no code path referencing it, so R8 full mode removes it.
# The failure appears only in release builds, which is where it is most expensive to find.
proguard = ""
for rules in glob.glob(f"{ROOT}/*/proguard-rules.pro"):
    proguard += open(rules).read()

manifest_classes = set()
for path in xml_files:
    if "AndroidManifest" not in path:
        continue
    manifest_classes |= set(re.findall(r'android:name="(com\.prism[\w.]+)"', open(path).read()))

for cls in sorted(manifest_classes):
    leaf = cls.split(".")[-1]
    # Class names are UpperCamelCase. An action is lowercase (com.foo.bar) or SCREAMING_SNAKE
    # (com.foo.WIDGET_PINNED) — neither is a class, and flagging them is noise.
    if "." not in cls or leaf[0].islower() or leaf.isupper() or "_" in leaf:
        continue
    pkg = cls.rsplit(".", 1)[0]
    kept = (f"-keep class {cls}" in proguard
            or f"-keep class {pkg}.**" in proguard
            or f"-keep class {pkg}.* " in proguard)
    if not kept:
        issues.append(("UNKEPT", f"{cls}: named in a manifest but not kept by ProGuard — "
                                 f"R8 full mode will strip it from release builds"))

by_kind = {}
for kind, msg in issues:
    by_kind.setdefault(kind, []).append(msg)
for kind in ("MALFORMED", "DANGLING", "DUPLICATE", "UNLABELLED", "UNKEPT"):
    if kind in by_kind:
        hits = sorted(set(by_kind[kind]))
        print(f"\n== {kind} ({len(hits)}) ==")
        for h in hits[:20]:
            print("  " + h)

unprinted = {k for k, _ in issues} - {"MALFORMED", "DANGLING", "DUPLICATE", "UNLABELLED", "UNKEPT"}
if unprinted:
    raise SystemExit(f"xml_check bug: issue kinds not in the print order: {sorted(unprinted)}")

print(f"\n{len(xml_files)} XML files checked, {len(set(issues))} findings.")
sys.exit(1 if issues else 0)
