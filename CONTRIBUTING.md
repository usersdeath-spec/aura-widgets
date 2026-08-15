# Contributing to Prism

## Developer setup

```bash
git clone <repo> && cd prism
# JDK 17 and Android SDK 35 required.
./gradlew :app:assembleDebug
```

Fonts are not in the repo (licensing hygiene, not secrecy). Download the six variable families
listed in `TypefaceProvider` and place them in `core/render/src/main/assets/fonts/`. A missing font
falls back to the system face — the app builds and runs, it just looks wrong.

Wallpaper artwork lives in the `:wallpaper_pack` asset pack and is not required for a debug build;
the app shows placeholder tiles without it.

```bash
./gradlew test                              # JVM: catalog, style resolution, harmony maths
./gradlew :core:render:testDebugUnitTest    # Robolectric: widget audit + golden images
./gradlew :core:render:verifyGolden         # Full 708-widget golden comparison
./gradlew connectedCheck                    # Compose UI tests
./gradlew :macrobenchmark:connectedCheck    # Startup and scroll benchmarks
```

## Adding a design family

The most common contribution, and it should take an afternoon.

1. Copy an existing file in `core/data/.../catalog/Families*.kt`.
2. Write one `WidgetStyle` and 10–15 variants using the `family()` DSL.
3. Add it to `PrismCatalog.ALL` and a `Collection`.
4. Tag it in `FamilyFacets.kt` (3–5 facets).
5. Pair it with at least three wallpapers, both directions.
6. Run `./gradlew test` — the integrity suite checks all of the above.
7. Record golden images: `./gradlew :core:render:recordGolden --tests "*<family-id>*"`.

No renderer, UI, or migration changes. If your family needs a code change, that is a signal the
idea belongs in a new `ContentLayout` or `Surface` that other families can use too.

### It must pass the checklist

1. Reads at 2×2. Test the smallest size first; if it only works large, it's a wallpaper.
2. Legible over both a white photograph and pure black.
3. One idea, stated in `note`, under nine words.
4. All seven pillars — enforced by `Core`.
5. No variant is another variant at a different size.
6. Ink clears 3:1 contrast on opaque surfaces.
7. Distinct from every existing family by *behaviour*, not hue. If it differs only in colour from
   something we ship, it is a variant or a tint, not a family. See the `Gemstone` precedent.

## Code standards

- Kotlin official style; `./gradlew ktlintCheck` in CI.
- Comments explain **why**, never what. A comment restating the code is deleted in review.
- Public types in `:core:*` carry KDoc explaining the decision behind them.
- No new dependency without a note in the PR describing what it replaces and what it costs in
  method count and startup time.
- Modules depend upward only. A feature module importing another feature module fails the build.

## Pull requests

Include: what changed, why, screenshots or golden diffs for anything visual, and the test that would
have caught the bug you fixed. PRs that change rendering without golden updates are not reviewed.

## What we say no to

Cloud sync, accounts, analytics, ads, remote config, and anything that adds a permission. Each costs
startup time, trust, or both, and none makes a single widget look better. Proposals to add them need
to argue against that, not around it.
