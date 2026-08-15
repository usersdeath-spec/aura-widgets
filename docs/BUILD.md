# Building Prism

## Requirements

- JDK 17 (not 21 — AGP 8.7 targets 17)
- Android SDK 35, build-tools 35.0.0
- Android Studio Ladybug or newer
- ~4 GB free for the Gradle cache on first sync

## First build

```bash
# Windows
.\setup.ps1
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./setup.sh
./gradlew :app:assembleDebug
```

`setup` writes `local.properties` with your SDK path. That file is machine-specific and is
gitignored, so **every freshly extracted copy needs it once** — without it Gradle fails with
"SDK location not found" before compiling anything.

Set `ANDROID_HOME` once for your user account and no future copy will need the script at all;
`setup.ps1` offers to do that.

Two things are **not** in the repository and the build tolerates both:

**Fonts.** Six variable families go in `core/render/src/main/assets/fonts/` with the filenames in
`TypefaceProvider`. Missing fonts fall back to the system face — the app builds and runs, it just
looks wrong. Not committed for licensing hygiene; all six are SIL OFL.

**Wallpapers.** The `:wallpaper_pack` asset pack is empty. The wallpaper gallery shows placeholder
tiles; widgets and the catalog are unaffected.

## Expect a first-build session

This source has never been through the Kotlin compiler. It has been through structural static
analysis (`python3 tools/static_check.py`, clean) and a manual review pass, both of which caught
real defects — but neither resolves types, checks signatures, or validates Compose or Hilt code
generation.

Budget an hour or two of import fixes, signature mismatches, and Compose/Hilt annotation-processing
errors on the first build. Known-likely areas, in the order I would expect them to surface:

1. **Hilt graph.** `RenderModule` provides `BitmapSource`, `TypefaceProvider`, `PrismRenderer`,
   `FamilyCatalog`, and `PrismDatabase`. `@HiltWorker` workers need `HiltWorkerFactory`, which
   `PrismApp` supplies. Missing bindings surface as `[Dagger/MissingBinding]` at KSP time.
2. **Compose signatures.** `WidgetPreview` and `EditorScreen` pass `Modifier` through several
   layers; `Modifier.width` in `CatalogScreen` was one already-fixed shadowing bug of that shape.
3. **Desugaring.** `java.time.temporal.WeekFields` and `LocalTime` need
   `isCoreLibraryDesugaringEnabled` in every module that touches them — set in `:app` and
   `:core:render`; `:widget` and `:core:model` may need it too.
4. **Room schema export.** `exportSchema = true` needs `room.schemaLocation` in `ksp { arg(...) }`
   or Room warns and skips migration tests.
5. **`WidgetType` exhaustiveness.** `WidgetDataSource.dataFor` is an exhaustive `when` over 25
   entries. Adding a type breaks it deliberately — that is the point.

## Verifying a build

```bash
python3 tools/static_check.py             # structural pass — must stay clean
./gradlew test                            # catalog integrity: 31 tests, JVM only
./gradlew :app:testDebugUnitTest          # widget audit across the full render matrix
./gradlew :app:assembleDebug              # debug APK
```

## First run on a device

The order that finds problems fastest:

1. Launch. Onboarding requests no permissions, so a crash here is a DI or theme problem.
2. Scroll the catalog. Every preview is a real render — anything that draws wrong draws wrong here
   first, cheaply, without touching the home screen.
3. Open the editor, drag every slider. Live rendering bypasses the cache, so this is also the
   fastest way to find a renderer that is slow.
4. Place one widget of each of the 25 types. This is the only way to exercise `RemoteViews`
   measurement, binder payload size, and the update path.
5. Apply a setup. Exercises the catalog, the wallpaper applier, and multi-widget placement together.

## Release build

See `docs/RELEASE_CHECKLIST.md`. Do not produce a release build until every measurement cell in it
is filled from real hardware.
