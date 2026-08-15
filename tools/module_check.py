#!/usr/bin/env python3
"""
Module boundary check.

Every Kotlin file lives in a Gradle module, and may only import from modules that module actually
declares a dependency on. Two failures this catches, both of which the Kotlin compiler reports only
as "Unresolved reference" on the package's first segment — which is a confusing way to learn you
have a layering problem:

  UNDECLARED  the import is legitimate but the dependency is missing from build.gradle.kts
  LAYERING    a feature module importing another feature module, which the architecture forbids

`api(...)` dependencies propagate transitively; `implementation(...)` does not, and that difference
is modelled here because it is exactly where "it worked in that module" comes from.
"""
import re, os, glob, sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# module path -> {"api": [...], "impl": [...], "packages": {...}}
modules = {}
for build in glob.glob(f"{ROOT}/*/build.gradle.kts") + glob.glob(f"{ROOT}/*/*/build.gradle.kts"):
    mdir = os.path.dirname(build)
    name = ":" + os.path.relpath(mdir, ROOT).replace(os.sep, ":")
    src = open(build).read()
    modules[name] = {
        "dir": mdir,
        "api": re.findall(r'api\(project\("([^"]+)"\)\)', src),
        "impl": re.findall(r'implementation\(project\("([^"]+)"\)\)', src),
        "packages": set(),
    }

for name, m in modules.items():
    for f in glob.glob(f"{m['dir']}/src/**/*.kt", recursive=True):
        pm = re.search(r'^package\s+([\w.]+)', open(f).read(), re.M)
        if pm:
            m["packages"].add(pm.group(1))

def owner_of(package):
    for name, m in modules.items():
        if package in m["packages"]:
            return name
    return None

def visible_modules(name, seen=None):
    """Direct deps, plus everything reachable through api() chains."""
    if seen is None:
        seen = set()
    if name in seen:
        return set()
    seen.add(name)
    out = set()
    m = modules.get(name)
    if not m:
        return out
    for dep in m["api"] + m["impl"]:
        out.add(dep)
        out |= {d for d in visible_modules(dep, seen) if d in modules and dep in modules
                and d in modules[dep]["api"]}
    return out

issues = []
for name, m in modules.items():
    allowed = visible_modules(name)
    for f in glob.glob(f"{m['dir']}/src/**/*.kt", recursive=True):
        src = open(f).read()
        for im in re.findall(r'^import\s+(com\.prism\.studio\.[\w.]+)', src, re.M):
            pkg = im.rsplit(".", 1)[0]
            target = owner_of(pkg)
            if target is None or target == name:
                continue
            rel = os.path.relpath(f, ROOT)
            if target not in allowed:
                kind = "LAYERING" if name.startswith(":feature") and target.startswith(":feature") \
                    else "UNDECLARED"
                issues.append((kind, f"{rel}: imports {im} from {target}, "
                                     f"which {name} does not depend on"))
            elif name.startswith(":feature") and target.startswith(":feature"):
                issues.append(("LAYERING", f"{rel}: {name} depends on {target}; "
                                           f"feature modules must not see each other"))

for kind in ("LAYERING", "UNDECLARED"):
    hits = sorted({m for k, m in issues if k == kind})
    if hits:
        print(f"\n== {kind} ({len(hits)}) ==")
        for h in hits:
            print("  " + h)

print(f"\n{len(modules)} modules checked, {len(set(issues))} findings.")
sys.exit(1 if issues else 0)
