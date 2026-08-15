# Prism

A premium Android home-screen customisation app. One purchase, everything unlocked, forever.

**Start here:** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) explains the one idea the whole
codebase follows from. [`docs/ROADMAP.md`](docs/ROADMAP.md) is honest about what is built and what
is not.

## The short version

A widget is data, not a layout. A `WidgetSpec` (family + variant + the user's own edits) resolves
into a `WidgetStyle`, which a Canvas engine rasterises into a bitmap. That bitmap goes into a
one-`ImageView` `RemoteViews` on the home screen — and into the catalog tile and the live editor
preview. Same code path, so a preview can never lie.

32 families × ~13 variants ≈ 416 widgets, built from ~19 content renderers and 11 layouts.

## Build

```bash
./gradlew :app:assembleDebug
./gradlew test                    # JVM: style resolution, catalog integrity
./gradlew :core:render:testDebugUnitTest   # Robolectric golden images
```

Requires JDK 17, Android SDK 35. Bundled variable fonts go in `core/render/src/main/assets/fonts/`;
see `TypefaceProvider` for the expected filenames.

## Adding a design family

1. Copy `core/data/.../catalog/MinimalMono.kt`.
2. Write one `WidgetStyle` and 10–15 `WidgetVariant`s.
3. Add it to `PrismCatalog.ALL`.
4. Run the family checklist in `docs/ROADMAP.md`.

No renderer, UI, or migration changes. That is the entire point of the architecture.

## Layout

```
core/model     immutable domain types — the whole design language
core/render    Canvas engine + content renderers + typography
core/data      Room, DataStore, the authored catalog, licensing
core/design    the app's own Compose theme (deliberately quiet)
feature/*      catalog, editor, wallpapers
widget         AppWidgetProvider, update scheduling, data sources
branding       original icon and wordmark (SVG)
```
