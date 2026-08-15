# Future Expansion — What the Architecture Already Allows

Each item below is checked against the current code: what exists, what's missing, and whether it
needs an architectural change. The answer is "no redesign" in every case, which was the point of the
data-driven catalog and the single render path.

## More families, wallpapers, seasonal collections — **ready today**

A family is one data file plus one line in `PrismCatalog.ALL`. A wallpaper is one line in a series.
Seasonal collections already have a working precedent: `SeasonalBloom` switches palette by date, so
a Halloween or Diwali collection is authoring work, not engineering.

## Tablet and foldable widgets — **small, additive**

Needs: two more `WidgetSize` entries (6×3, 8×4) and a handful of wide-format `ContentLayout`s. The
renderer already takes arbitrary pixel dimensions and the provider already measures from host
options, so nothing structural moves. The real work is design — a 4×2 layout stretched to 8×4 looks
sparse, so wide sizes want their own compositions.

Effort: one release. Risk: low.

## Wear OS companion — **new module, shared core**

`:core:model` and `:core:render` are pure enough to compile for Wear: the renderer draws to a
Canvas, which a watch face complication can consume. What a companion needs on top is a
`:wear` module, a complication provider, and a per-family constraint set for round displays.

Effort: two releases. Risk: medium — the constraint is design, not code. Most families assume a
rectangle, and the ones built on edge behaviour (Liquid Glass, Eclipse) will need round-specific
tuning rather than a scaled crop.

## Backup & restore — **ready today, deliberately unbuilt**

Everything a user owns is already a serialisable `WidgetSpec` plus a Room row. Export is a JSON dump
of placed widgets, favourites, and presets; import is a deserialise. Forward compatibility is
already pinned by test, so a backup taken today restores into next year's version.

The one genuine constraint is Android's: restored widgets get *new* `appWidgetId`s, so import maps
old ids to new ones and cannot restore positions — the launcher owns those. Import restores the
widgets and their styles; the user re-places them. Any claim beyond that would be a lie.

Effort: one release.

## Cloud sync — **possible, and the wrong trade**

Technically it's backup & restore plus a store. Practically it means accounts, a server, a privacy
policy that no longer says "we hold nothing about you", and a running cost against a one-time
purchase. The listing's strongest differentiator is that Prism holds no data; sync spends that to
solve a problem local backup already mostly solves.

If it is ever built: end-to-end encrypted, opt-in, with the app fully functional when it is off, and
a privacy policy update shipped before the feature.

## What would actually require a redesign

Two things, worth naming so they are decided deliberately rather than drifted into:

1. **Per-widget live animation.** Motion beyond `RemoteViews` transitions needs a foreground service
   or a `RemoteViewsService` redraw loop, and the battery cost breaks the promise on the listing.
   This is a no.
2. **User-authored widgets from primitives** (a KWGT-style editor). The current model is
   `family + variant + delta`, which is what makes 708 widgets maintainable. Arbitrary user
   composition means a scene graph, a persistence format for it, and a rendering path for
   user-authored trees — a different product, and one that would make everything above harder.
