# Performance Report

**Status: no measurements taken.** Every cell below is empty because no build has run on hardware.
Filling them with estimates would defeat the purpose of the exercise, and an estimate that later
turns out wrong in a store listing is a refund.

## Budgets and results

| Metric | Budget | Measured | Device |
|---|---|---|---|
| Cold start, P50 | 400 ms | — | — |
| Cold start, P90 | 700 ms | — | — |
| Warm start | 180 ms | — | — |
| Catalog scroll, dropped frames | < 1% | — | — |
| Editor slider, frame time | < 8 ms | — | — |
| Widget render, cached | 8 ms | — | — |
| Widget render, cold | 25 ms | — | — |
| Backdrop blur, 96px crop | 6 ms | — | — |
| Resident memory, catalog open | 120 MB | — | — |
| Base AAB (no wallpapers) | 15 MB | — | — |
| Wallpaper pack | 180 MB | — | — |
| Battery, 24 h, 8 widgets placed | < 1% attributable | — | — |
| Background wakeups, local-only widgets | 0/hour | — | — |

Test matrix when hardware is available: one low-end device (2 GB RAM, 60 Hz), one mid-range, one
flagship at 120 Hz, and one tablet.

## Where the architecture expects to spend

Written before measuring, so it can be checked against reality rather than rationalised afterwards.
If a measurement contradicts one of these, the design assumption was wrong and gets revisited.

**Should be cheap:**
- Cached widget renders. Keyed on style fingerprint + size + data fingerprint; an unchanged minute
  is a map lookup.
- Idle battery with local-only widgets. Clocks ride `ACTION_TIME_TICK`, battery is event-driven, and
  `UpdateScheduler` enqueues nothing when no 15-minute widget is placed. This one is a store-listing
  claim and must be verified before it is made.
- Catalog scroll. Previews are cached bitmaps; the shelf recycles.

**Expected hot spots, in the order I would profile them:**
1. **`StackBlur`** — three box-blur passes over a 96×96 crop is ~200k operations in pure Kotlin. Per
   widget, per wallpaper change. If this misses budget, the fix is a wider downsample before
   blurring, not a native library.
2. **Cold catalog build** — 59 families of immutable objects constructed at process start. Should be
   single-digit milliseconds, but it is on the startup path, so it gets measured first.
3. **Editor live rendering** — deliberately bypasses the cache on every slider frame. The most
   likely place to miss 8 ms, and the most visible when it does.
4. **`TypefaceProvider`** — `Typeface.create` per (family, weight) is memoised, but the first render
   of a screenful of mixed families pays it all at once.
5. **Binder payload** — a 4×4 widget at 3× is roughly 4.4 MB of bitmap. Under the transaction limit,
   but placing many large widgets simultaneously is worth watching.

## Method

Macrobenchmark for start-up and scroll, Perfetto for frame timing, `dumpsys batterystats` over 24 h
with eight widgets placed, and the audit suite's own render timing for the per-widget budget.
Optimise only against a measurement that missed a budget.
