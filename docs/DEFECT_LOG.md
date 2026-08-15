# Defect Log — Phase 7 Static Validation

Fifteen defects found and fixed by static analysis and manual review. All would have been compile
errors or silent misbehaviour. None was found by running the app, because the app has not run.

| # | Defect | Found by | Severity |
|---|---|---|---|
| 1 | `:feature:wallpapers` `Wallpaper.kt` duplicated `WallpaperCatalog.WallpaperEntry` — two models for one concept | static: duplicate declaration | Design |
| 2 | `:app` used Room and WorkManager with neither declared as a dependency | static: dependency scan | Compile |
| 3 | `:core:render` used `androidx.core.graphics.ColorUtils` with no `core-ktx` dependency | static: dependency scan | Compile |
| 4 | `:widget` serialised the feed cache with no serialization plugin or runtime | static: dependency scan | Compile |
| 5 | `CatalogScreen` declared `private fun Modifier.width(...)` that called itself — infinite recursion | manual review | Runtime hang |
| 6 | `Theme.kt` declared a `Double.em` extension shadowing Compose's own | manual review | Compile/ambiguity |
| 7 | `Motion` named two different concepts in two packages | static: duplicate declaration | Readability |
| 8 | `StyleDelta` had no `shadow` field, so the editor's shadow slider was wired to a no-op | manual review | Silent no-op |
| 9 | `ColorHarmony.rgbToLab` called `ColorUtils.colorToLAB` twice, once into a discarded array | manual review | Compile |
| 10 | `WidgetAuditTest` referenced an `AuditFixtures` class that did not exist | manual review | Compile |
| 11 | `WidgetDataSource` referenced `CachedFeed`, `SunTimes`, `CalendarReader`, `LocalContent` — none existed | manual review | Compile |
| 12 | `RenderModule` injected `BitmapSource` with no binding provided | manual review | Hilt build failure |
| 13 | `:widget` referenced `RefreshWorker`, `DayRolloverWorker`, `WallpaperPalette`, `WidgetTapRouter` — none existed | reference pass | Compile |
| 14 | `EditorScreen` declared a private `Row` composable while importing Compose's `Row` | reference pass | Wrong-call risk |
| 15 | Audit suite sat in `:core:render` but needed `:core:data` and `:widget` — an upward dependency | reference pass | Architecture violation |

## Notes on three of them

**#1 and #15 are the interesting ones.** Both are cases where the code compiled in my head but
violated a rule the project set for itself. #15 in particular would have forced `:core:render` to
depend on `:core:data`, inverting the module graph that the whole architecture rests on. Moving the
audit to `:app` — the only module that legitimately sees everything — preserves it.

**#8 is the most dangerous.** A slider that moves and does nothing is worse than a missing control:
it looks like the app is broken in a way the user cannot diagnose, and it would have survived any
review that read the UI without reading the model.

**#11 and #13 are the same failure.** I wrote calling code and named the collaborators without
writing them. That is exactly the "no placeholders" failure the previous phase claimed to have
eliminated, and it survived because nothing had compiled. It is the clearest evidence in this log
for why "feature-complete in source" is not the same as "done".

## The reference pass, honestly

A deeper reference-resolution pass flagged 366 candidates. Most are false positives — it does not
understand wildcard imports, enum entries, nested types, aliases, or platform classes. Three real
defect classes came out of it (#13, #14, #15) and the rest was noise.

I have not shipped that pass as a gate, because a checker with a 99% false-positive rate is a
checker people learn to ignore. `tools/static_check.py` contains only checks that are precise
enough to act on, and it currently reports zero issues across 65 files.
