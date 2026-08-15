# Versioning & Release Strategy

## Version numbers

`MAJOR.MINOR.PATCH`, with meanings specific to a design product:

- **MAJOR** — a change to what the purchase includes, or a redesign of the app itself. We do not
  expect to ship one.
- **MINOR** — new families, wallpapers, setups, or features. The regular release.
- **PATCH** — fixes only. No new content.

`versionCode` is `MAJOR * 10000 + MINOR * 100 + PATCH`, so 1.4.2 is 10402. Monotonic, readable in a
Play console list, and leaves room.

## Release cadence

A content release roughly every two months: two to four new families, ten to fifteen wallpapers, a
few setups. Seasonal collections in March, June, September, and December — Seasonal Bloom already
switches palette by date, so the pattern is established.

Patch releases whenever something is broken. A widget that renders wrong on someone's home screen
is visible every time they unlock their phone, which makes it more urgent than the same bug inside
an app.

## Compatibility rules

Because widgets outlive app versions, three rules are permanent:

1. **A variant id is never renamed or removed.** Someone has it on their home screen. Retiring a
   design means hiding it from the catalog while keeping it resolvable.
2. **`WidgetSpec` only ever gains optional fields.** Old JSON must deserialise into the new schema
   with defaults; there is a forward-compatibility test that pins this.
3. **Restyling a family is allowed and encouraged** — user deltas are stored separately, so
   improvements reach already-placed widgets without discarding anyone's edits.

## Release train

1. Content freeze, then `./gradlew verifyGolden` — visual diffs reviewed by eye.
2. Internal track, one week, on a low-end and a flagship device.
3. Closed track, ~50 testers, one week. Watch pre-launch report and ANR/crash rates.
4. Staged rollout: 10% → 25% → 50% → 100% over five days, halting on any crash-rate regression.
