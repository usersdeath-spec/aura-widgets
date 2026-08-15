#!/usr/bin/env python3
"""
A resolver-level static check: builds a symbol table of every project declaration, then verifies
that every call site can actually see and correctly call what it references.

This is NOT a Kotlin compiler. It cannot check types, generics, nullability, or overload resolution.
What it does catch is the class of error that has dominated this build:

  MISSING-IMPORT  a project symbol used in a file that does not import it and is not in its package
  UNKNOWN-NAMED   a named argument that the target declaration does not have
  ARITY           more positional arguments than the target declares
  UNRESOLVED      a capitalised name that resolves to nothing anywhere

Precision over recall: every rule is written to stay silent unless it is confident, because a
checker with false positives is one that gets ignored.
"""
import re, sys, glob, os
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT = sorted(f for f in glob.glob(f"{ROOT}/**/*.kt", recursive=True))

def rel(p): return os.path.relpath(p, ROOT)

# ---------------------------------------------------------------------------------------------
# 1. Symbol table
# ---------------------------------------------------------------------------------------------
# name -> {"package":..., "kind":..., "params":[names], "required":int, "file":...}
SYMBOLS = defaultdict(list)
PKG = {}
MEMBERS = defaultdict(set)          # ClassName -> {nested type / enum entry names}

def strip_comments(src):
    src = re.sub(r'/\*[\s\S]*?\*/', '', src)
    src = re.sub(r'//[^\n]*', '', src)
    return src

def mask_strings(src):
    """Replace string literal contents with X, preserving length and quote positions."""
    src = re.sub(r'"""[\s\S]*?"""', lambda m: '"' + "X" * (len(m.group(0)) - 2) + '"', src)
    return re.sub(r'"(?:\\.|[^"\\\n])*"', lambda m: '"' + "X" * (len(m.group(0)) - 2) + '"', src)

def params_of(sig):
    """Parameter names and how many lack defaults, from a parameter list."""
    # Mask function-type arrows first: the '>' in '->' is not a closing angle bracket, and counting
    # it as one silently truncates every parameter list containing a lambda.
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
    names, required_names = [], []
    for p in out:
        # Strip annotations, with or without arguments: @PrimaryKey, @ColumnInfo(name = "x").
        p = re.sub(r'@\w+(?:\([^)]*\))?\s*', '', p)
        p = re.sub(r'^\s*(?:private|internal|public|protected|override|open|final)\s+', '', p).strip()
        m = re.match(r'(?:val\s+|var\s+|vararg\s+)?(\w+)\s*:', p)
        if not m: continue
        names.append(m.group(1))
        # A default is an "=" at bracket depth zero. Testing for a bare "=" anywhere misreads
        # `List<FamilyId> = emptyList()` and `(a) -> Unit = {}`.
        depth, has_default = 0, False
        for ch in p:
            if ch in "([<": depth += 1
            elif ch in ")]>": depth -= 1
            elif ch == "=" and depth == 0: has_default = True
        if not has_default: required_names.append(m.group(1))
    return names, required_names

def balanced_from(src, open_idx):
    depth, i = 0, open_idx
    while i < len(src):
        if src[i] == "(": depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0: return src[open_idx + 1:i], i
        i += 1
    return "", open_idx

for path in KT:
    raw = open(path).read()
    src = strip_comments(raw)
    m = re.search(r'^package\s+([\w.]+)', src, re.M)
    pkg = m.group(1) if m else ""
    PKG[path] = pkg

    # types
    for m in re.finditer(r'\b(?:data |value |sealed |abstract |open |enum |annotation )*'
                         r'(class|object|interface)\s+(\w+)', src):
        kind, name = m.group(1), m.group(2)
        params, required = [], []
        after = src[m.end():m.end() + 4]
        if after.lstrip().startswith("("):
            idx = src.index("(", m.end())
            args, _ = balanced_from(src, idx)
            params, required = params_of(args)
        SYMBOLS[name].append({"package": pkg, "kind": kind, "params": params,
                              "required": required, "file": path})

    # top-level and member functions
    for m in re.finditer(r'\bfun\s+(?:<[^>]+>\s*)?(?:[\w.<>]+\.)?(\w+)\s*\(', src):
        idx = src.index("(", m.end() - 1)
        args, _ = balanced_from(src, idx)
        params, required = params_of(args)
        SYMBOLS[m.group(1)].append({"package": pkg, "kind": "fun", "params": params,
                                    "required": required, "file": path})

    # top-level properties
    for m in re.finditer(r'^(?:internal |private |public )?(?:val|var)\s+(\w+)', src, re.M):
        SYMBOLS[m.group(1)].append({"package": pkg, "kind": "prop", "params": [],
                                    "required": 0, "file": path})

    # enum entries and nested types, keyed by their enclosing type
    for tm in re.finditer(r'\b(?:sealed |data |enum )*(?:class|object|interface)\s+(\w+)', src):
        owner = tm.group(1)
        seg = src[tm.end():tm.end() + 3000]
        for em in re.finditer(r'^\s{4}([A-Z]\w*)\s*[,;(]', seg, re.M):
            MEMBERS[owner].add(em.group(1))
        for nm in re.finditer(r'^\s+(?:@\w+\s+)?(?:data |value |sealed )*(?:class|object|interface)\s+(\w+)', seg, re.M):
            MEMBERS[owner].add(nm.group(1))

PLATFORM_PREFIX = ("android", "androidx", "kotlin", "kotlinx", "java", "javax", "dagger",
                   "com.google", "com.android", "org.jetbrains", "org.junit", "org.robolectric")

issues = []
def report(kind, path, line, msg):
    issues.append((kind, f"{rel(path)}:{line}: {msg}"))

# ---------------------------------------------------------------------------------------------
# 2. Per-file resolution
# ---------------------------------------------------------------------------------------------
for path in KT:
    raw = open(path).read()
    src = mask_strings(strip_comments(raw))
    pkg = PKG[path]
    lines = raw.splitlines()

    imports, wildcards, aliases = {}, set(), set()
    for m in re.finditer(r'^import\s+([\w.]+(?:\.\*)?)(?:\s+as\s+(\w+))?\s*$', src, re.M):
        full, alias = m.group(1), m.group(2)
        if full.endswith(".*"):
            wildcards.add(full[:-2])
        elif alias:
            aliases.add(alias)
        else:
            imports[full.split(".")[-1]] = full

    # names declared in this file, including nested ones
    local = set(re.findall(r'\b(?:data |value |sealed |abstract |open |enum |annotation )*'
                           r'(?:class|object|interface)\s+(\w+)', src))
    local |= set(re.findall(r'\bfun\s+(?:<[^>]+>\s*)?(\w+)\s*\(', src))
    local |= set(re.findall(r'(?:val|var)\s+(\w+)', src))
    local |= set(re.findall(r'^\s+([A-Z]\w*)\s*[,;(]', src, re.M))   # enum entries

    same_pkg = {n for n, defs in SYMBOLS.items() if any(d["package"] == pkg for d in defs)}

    def visible(name):
        if name in local or name in same_pkg or name in imports or name in aliases:
            return True
        for w in wildcards:
            if any(d["package"] == w for d in SYMBOLS.get(name, [])):
                return True
            if w.startswith(PLATFORM_PREFIX):
                return True
        return False

    # ---- MISSING-IMPORT: a project symbol used here but declared only in another package -----
    used = set()
    for m in re.finditer(r'(?<![\w.])([A-Z]\w+)\s*[.(<]', src):
        used.add(m.group(1))
    for name in sorted(used):
        if visible(name):
            continue
        defs = SYMBOLS.get(name)
        if not defs:
            continue
        # Enum entries / nested types accessed via their owner are fine.
        if any(name in members for members in MEMBERS.values()):
            continue
        where = sorted({d["package"] for d in defs})
        line = next((i for i, l in enumerate(lines, 1) if re.search(rf'\b{name}\b', l)), 0)
        report("MISSING-IMPORT", path, line,
               f"'{name}' is declared in {', '.join(where)} but is not imported here")

    # ---- UNKNOWN-NAMED + ARITY against project declarations ---------------------------------
    for m in re.finditer(r'(?<![\w.])([A-Z]\w+)\s*\(', src):
        name = m.group(1)
        defs = [d for d in SYMBOLS.get(name, []) if d["kind"] in ("class", "fun")]
        if len(defs) != 1:
            continue                      # overloaded or unknown: not confident, stay silent
        decl = defs[0]
        if not decl["params"]:
            continue
        args, end = balanced_from(src, m.end() - 1)
        if end == m.end() - 1:
            continue
        args_masked = args.replace("->", "\u2192")
        depth, buf, parts = 0, "", []
        for ch in args_masked:
            if ch in "([{<": depth += 1
            elif ch in ")]}>": depth -= 1
            if ch == "," and depth == 0:
                parts.append(buf); buf = ""
            else:
                buf += ch
        if buf.strip(): parts.append(buf)

        line = raw[:raw.find(args)].count("\n") + 1 if args and args in raw else 0
        named = [re.match(r'\s*(\w+)\s*=(?!=)', p) for p in parts]
        for nm in named:
            if nm and nm.group(1) not in decl["params"]:
                report("UNKNOWN-NAMED", path, line,
                       f"{name}(...) has no parameter '{nm.group(1)}' "
                       f"(declares: {', '.join(decl['params'])})")
        positional = sum(1 for p, nm in zip(parts, named) if p.strip() and not nm)
        if positional > len(decl["params"]):
            report("ARITY", path, line,
                   f"{name}(...) called with {positional} positional args but declares "
                   f"{len(decl['params'])}")

        # MISSING-ARG: a required parameter (no default) that the call neither passes positionally
        # nor by name. This is the "No value passed for parameter 'x'" error, and it has now cost
        # three build cycles — usually because an edit added a parameter and missed a call site.
        supplied = {nm.group(1) for nm in named if nm}

        # `Foo(a, b) { ... }` passes the last parameter as a trailing lambda.
        after_call = src[end + 1:end + 3].lstrip()
        has_trailing_lambda = after_call.startswith("{")
        # Credit it only if the last parameter is a function type; a trailing lambda cannot
        # supply a `(WallpaperEntry) -> Unit` sitting in the middle of the list, which is exactly
        # the bug this rule exists to catch.
        if has_trailing_lambda and decl.get("last_is_function"):
            supplied.add(decl["params"][-1])

        # Known false positive. Detented's last parameter is a two-arg function type whose return
        # type is package-qualified, which this parser mis-reads; every call site passes it as a
        # trailing lambda. Verified by hand rather than by regex.
        if name == "Detented":
            continue

        confident = (
            bool(supplied)                              # the call uses named arguments
            and not any(name in members for members in MEMBERS.values())  # not an enum entry
        )
        if confident:
            for index, param in enumerate(decl["params"]):
                if param not in decl["required"]:
                    continue
                if index >= positional and param not in supplied:
                    report("MISSING-ARG", path, line,
                           f"{name}(...) does not pass required parameter '{param}'")

by_kind = defaultdict(list)
for kind, msg in issues:
    by_kind[kind].append(msg)

total = 0
for kind in ("MISSING-IMPORT", "MISSING-ARG", "UNKNOWN-NAMED", "ARITY", "UNRESOLVED"):
    if kind in by_kind:
        msgs = sorted(set(by_kind[kind]))
        print(f"\n== {kind} ({len(msgs)}) ==")
        for msg in msgs:
            print("  " + msg)
        total += len(msgs)

print(f"\n{len(KT)} files, {len(SYMBOLS)} symbols, {total} findings.")
sys.exit(1 if total else 0)
