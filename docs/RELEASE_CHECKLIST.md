# Release Checklist

Gates, not suggestions. Anything unchecked blocks the rollout.

## Build

- [ ] `versionName` and `versionCode` bumped per `VERSIONING.md`
- [ ] `CHANGELOG.md` updated in user language
- [ ] R8 full mode on, `isShrinkResources = true`
- [ ] Baseline profile regenerated from the scripted journey
- [ ] Debug logging stripped; no `Log.d` in release
- [ ] Signing via Play App Signing; upload key in CI secrets only
- [ ] AAB, not APK; asset pack `:wallpaper_pack` set to fast-follow delivery

## Budgets

| Metric | Ceiling | Measured |
|---|---|---|
| Cold start (mid-range, P50) | 400 ms | |
| Cold start (mid-range, P90) | 700 ms | |
| Base AAB (no wallpaper pack) | 15 MB | |
| Wallpaper pack | 180 MB | |
| Widget render, cached | 8 ms | |
| Widget render, cold | 25 ms | |
| Catalog scroll, dropped frames | < 1% | |
| Resident memory, catalog open | 120 MB | |
| Background wakeups, local-only widgets | 0/hour | |

Empty cells block release. A budget without a measurement is a wish.

## Tests

- [ ] `./gradlew test` green
- [ ] `verifyGolden` green across all 708 widgets; every diff reviewed, not just approved
- [ ] Compose UI tests green
- [ ] Macrobenchmarks within budget
- [ ] 24-hour battery run on a low-end device, drain attributable to Prism under 1%

## Quality audits

- [ ] `WidgetAuditTest` green (bleed, contrast, legibility, determinism, themes)
- [ ] `tools/wallpaper_audit.py` clean on all delivered artwork
- [ ] Measured palettes written back into `WallpaperCatalog`

## Accessibility

- [ ] TalkBack pass over every widget type — each bitmap has a spoken description
- [ ] Font scale 200%: no clipping in app UI
- [ ] Touch targets ≥ 48dp
- [ ] Colour-blind check: no state conveyed by hue alone (battery states carry shape and text too)
- [ ] Contrast ≥ 4.5:1 for app text, ≥ 3:1 for widget ink
- [ ] Keyboard and D-pad traversal of the catalog and editor

## Localisation

- [ ] RTL layout pass
- [ ] 24-hour and 12-hour clocks
- [ ] Non-Gregorian week starts
- [ ] Long month names (German, Russian) in the calendar grid
- [ ] No hardcoded strings — lint gate

## Compliance

- [ ] Data safety form matches `PRIVACY_POLICY.md` exactly
- [ ] Permissions declared match those actually requested; no unused declarations
- [ ] No third-party artwork or icon packs bundled
- [ ] Font licences listed in About
- [ ] Target SDK meets Play's current requirement
- [ ] Store listing claims verified against the code (counts read from the catalog, battery claims
      backed by the 24-hour run)

## Post-launch

- [ ] Staged rollout at 10%, crash-free rate watched for 24 hours before advancing
- [ ] Pre-launch report reviewed on all device tiers
- [ ] Rollback plan: previous AAB retained, halt criteria agreed in advance
