#!/usr/bin/env python3
"""
Dagger/Hilt graph check.

Collects everything the graph *provides* (@Provides return types, @Inject constructors, @Binds) and
everything it *requires* (constructor parameters, @Inject fields), then reports requirements nothing
satisfies. That is the [Dagger/MissingBinding] error, found before KSP runs.

It also flags a bare `Context` requirement, which is the specific mistake that just failed the
build: Hilt binds Context only behind @ApplicationContext or @ActivityContext, because the two have
different lifetimes and injecting the wrong one leaks an Activity.

Type-erasure caveat: matching is by simple type name, so a generic wrapper is compared loosely.
Under-reporting is intended; a false alarm here would cost more than it saves.
"""
import re, glob, sys
from collections import defaultdict

BUILT_IN = {
    "Context", "Application", "CoroutineScope", "WorkerParameters", "SavedStateHandle",
    "HiltWorkerFactory", "Configuration", "Set", "Map", "String", "Int", "Boolean", "Long",
}

provides = {}          # simple type name -> where it comes from
requires = []          # (type, qualifier_present, file, owner)

def simple(t):
    t = t.strip().split("<")[0].split(".")[-1]
    return t.rstrip("?")

def split_params(sig):
    sig = sig.replace("->", "\u2192")
    depth, buf, out = 0, "", []
    for ch in sig:
        if ch in "([<": depth += 1
        elif ch in ")]>": depth -= 1
        if ch == "," and depth == 0:
            out.append(buf); buf = ""
        else:
            buf += ch
    if buf.strip(): out.append(buf)
    return out

def strip_comments(src):
    # A colon inside a comment ("Qualified deliberately: Hilt binds ...") otherwise reads as a
    # parameter type declaration. Found by this checker's own first false positive.
    src = re.sub(r'/\*[\s\S]*?\*/', '', src)
    return re.sub(r'//[^\n]*', '', src)

files = glob.glob("**/*.kt", recursive=True)

# ---- what the graph provides -----------------------------------------------------------------
for f in files:
    s = strip_comments(open(f).read())

    for m in re.finditer(r'@Provides[\s\S]{0,120}?fun\s+\w+\s*\([\s\S]*?\)\s*:\s*([\w.<>]+)', s):
        provides[simple(m.group(1))] = f
    for m in re.finditer(r'@Binds[\s\S]{0,120}?fun\s+\w+\s*\([\s\S]*?\)\s*:\s*([\w.<>]+)', s):
        provides[simple(m.group(1))] = f
    for m in re.finditer(r'class\s+(\w+)[\s\S]{0,200}?@Inject\s+constructor', s):
        provides[m.group(1)] = f
    for m in re.finditer(r'@HiltWorker[\s\S]{0,200}?class\s+(\w+)', s):
        provides[m.group(1)] = f

# ---- what the graph requires -----------------------------------------------------------------
for f in files:
    s = strip_comments(open(f).read())

    for m in re.finditer(r'(?:class\s+(\w+)[\s\S]{0,200}?)?@Inject\s+constructor\s*\(([\s\S]*?)\)\s*[:{]', s):
        owner = m.group(1) or "?"
        for p in split_params(m.group(2)):
            if "@Assisted" in p:
                continue
            tm = re.search(r':\s*([\w.<>?]+)', p)
            if tm:
                requires.append((simple(tm.group(1)), "@ApplicationContext" in p or "@ActivityContext" in p, f, owner))

    for m in re.finditer(r'@Provides[\s\S]{0,120}?fun\s+(\w+)\s*\(([\s\S]*?)\)\s*:', s):
        for p in split_params(m.group(2)):
            tm = re.search(r':\s*([\w.<>?]+)', p)
            if tm:
                requires.append((simple(tm.group(1)), "@ApplicationContext" in p or "@ActivityContext" in p, f, m.group(1)))

    for m in re.finditer(r'@Inject\s+lateinit\s+var\s+\w+\s*:\s*([\w.<>]+)', s):
        requires.append((simple(m.group(1)), False, f, "field injection"))

missing, unqualified = [], []
for t, qualified, f, owner in requires:
    if t == "Context" and not qualified:
        unqualified.append(f"{f}: {owner} requires a bare Context — Hilt binds it only behind "
                           f"@ApplicationContext or @ActivityContext")
    elif t not in provides and t not in BUILT_IN:
        missing.append(f"{f}: {owner} requires '{t}', which nothing provides")

for title, hits in (("UNQUALIFIED-CONTEXT", unqualified), ("MISSING-BINDING", missing)):
    hits = sorted(set(hits))
    if hits:
        print(f"\n== {title} ({len(hits)}) ==")
        for h in hits:
            print("  " + h)

print(f"\n{len(provides)} bindings provided, {len(requires)} requirements, "
      f"{len(set(missing)) + len(set(unqualified))} findings.")
sys.exit(1 if missing or unqualified else 0)
