# Testing Report

**Status: tests written, tests not run.** No Kotlin compiler, Android SDK, or device has been
available in this environment. Everything below is an inventory and a plan, not a result. No test in
this project has ever passed, because none has ever executed.

## What exists

| Suite | Location | Tests | Executed |
|---|---|---|---|
| Catalog integrity | `core/data/src/test` | 31 | No |
| Widget audit | `app/src/test` (Robolectric) | 8 checks × full render matrix | No |
| Golden images | harness written, references absent | — | No |
| Compose UI | not written | — | — |
| Macrobenchmark | not written | — | — |

## Catalog integrity (31 tests)

Runs on the JVM with no Android dependency. Encodes the product's promises so a 60th family is as
safe to add as the 4th: 59 families and 708 widgets in range; all seven pillars per family; 10–15
variants per shelf; globally unique variant ids; no duplicate type/layout/size signature within a
family; exactly one collection per family; 3–5 facets per family and every facet reaching ≥2
families; setup placements resolving; setup wallpapers being genuine family pairings; launcher
advice inside real grid bounds; pack variants existing; every wallpaper reachable and every pairing
resolving in both directions; WCAG 3:1 ink contrast on opaque surfaces.

## Widget audit (Robolectric)

Renders every family × variant at three sizes and asserts: no ink outside the padded content box;
measured contrast ≥ 3:1 from *rendered pixels* rather than declared colours; nothing below the 11sp
legibility floor at the smallest size; nothing rasterising to near-nothing; identical output for
identical input; cold render under 25 ms; light, dark, and dynamic colour all resolving; a non-empty
spoken description for every widget.

Sample data is deliberately awkward — a 31-day month starting on a Sunday, −18°C, 100% battery, a
track title too long for any widget, a three-line quote. Tidy fixtures pass tests that real content
fails.

`forEachWidget` collects every failure and reports them together. Failing on the first would mean
700 fix-and-rerun cycles.

## Static analysis — the one thing that *has* run

`tools/static_check.py`, 65 files, currently clean. It checks delimiter balance, duplicate top-level
declarations, unresolved project imports, module dependency declarations against actual usage,
Compose import hygiene, and leftover TODO/debug markers.

It found 6 real defects on first run, all now fixed. It cannot resolve types or check signatures,
so a clean run means "no structural defects", not "compiles".

## Not written yet

- **Compose UI tests** for the editor, catalog, and onboarding.
- **Macrobenchmarks** for cold start and catalog scroll, with a generated baseline profile.
- **Room migration test** from schema v1 to v2 (the `user_presets` addition).
- **`WidgetSpec` forward-compatibility test** — pins the rule that a v1 JSON blob deserialises into
  the current schema. This one matters more than its size suggests, because widgets outlive app
  versions.

## Honest assessment

The test *design* is the strongest part of this work: the invariants are the ones that actually
break in a catalog this size, and they are written as executable checks rather than as a document.
The test *results* do not exist. Anyone reading this should treat the suite as unproven code that
happens to be about testing until it has run green once.
