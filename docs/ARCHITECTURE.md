# Prism — Architecture

## The one idea everything else follows from

A widget is **data**, not a layout.

```
WidgetSpec (family + variant + user delta)
      │
      ├─ resolve against FamilyCatalog ──► ResolvedWidget (family, variant, WidgetStyle)
      │
      └─ + WidgetData ──► PrismRenderer ──► Bitmap ──► RemoteViews (one ImageView)
                                    │
                                    └────► Compose Image (catalog tile / live editor preview)
```

That single arrow into two destinations is the load-bearing decision. The home-screen widget and
the in-app preview are produced by the *same* function, so a preview cannot drift from the thing
it previews — a class of bug that plagues every widget app that mocks its gallery.

### Why a Canvas, not RemoteViews composition (or Glance)

RemoteViews cannot blur, cannot draw arbitrary gradient stops, cannot stroke a 0.5dp hairline
cleanly, cannot composite mesh blobs, and cannot centre a numeral on its ink bounds. Those five
things *are* the product. Rasterising to a bitmap and handing the host one `ImageView` gives us
full drawing freedom, one view to inflate, and a small binder payload.

Three costs come with that choice, and each is paid explicitly:

| Cost | Mitigation |
|---|---|
| Bitmaps are opaque to screen readers | `Accessibility.describe()` builds a spoken string from the same data that drew the pixels. Every widget ships with one. |
| Bitmaps cost binder transaction size | Size is clamped in `PrismWidgetProvider.measure()`; a 4×4 at 3× is ~4.4 MB, well under the ceiling, and larger requests are capped. |
| Re-rasterising is expensive | `PrismRenderer` caches on `(style fingerprint, variant, size, data fingerprint)`. An unchanged minute is a cache hit. |

Glance is still the right tool for the interactive layer — checkbox taps, music transport — and is
composited over the bitmap as transparent targets rather than replacing it.

## Catalog arithmetic

The scope target (350–450 widgets) is only tractable if widgets are combinatorial:

```
32 design families  ×  ~13 variants each  =  ~416 widgets
        │                     │
        │                     └─ a WidgetType + a ContentLayout + a small StyleDelta
        └─ one WidgetStyle, one palette, one type treatment
```

Implemented once, shared by all of them:

- **24 `ContentRenderer`s** — one per `WidgetType`. Six of the system types collapse into a single
  `GaugeRenderer`, so the real count is closer to 19.
- **11 `ContentLayout`s** — Hero, Split, Ring, Grid, Stack, Chart, and friends.
- **6 `Surface` kinds** — Solid, Gradient, Glass, Mesh, Extruded, None.

Adding a family = one Kotlin file + one line in `PrismCatalog.ALL`. No renderer changes, no UI
changes, no migration. Adding a widget type = one renderer, and it appears in every existing family.

## System diagram

```
                          ┌──────────────────────────────────────────┐
   USER                   │                  :app                    │
   ACTION ───────────────►│   navigation · DI · billing · splash     │
                          └───────┬───────────────────────┬──────────┘
                                  │                       │
                ┌─────────────────┴──────┐      ┌─────────┴──────────┐
                │      feature layer     │      │      :widget       │
                │  onboarding · catalog  │      │ AppWidgetProvider  │
                │  editor · wallpapers   │      │ UpdateScheduler    │
                └───────┬────────────────┘      │ WidgetDataSource   │
                        │                       └─────────┬──────────┘
                        │  ResolvedWidget + WidgetData    │
                        └───────────────┬─────────────────┘
                                        ▼
                          ┌──────────────────────────────┐
                          │        :core:render          │
                          │  PrismRenderer               │
                          │   ├─ SurfacePainter          │
                          │   ├─ GlassPainter ─► Backdrop│──► wallpaper crop + blur
                          │   ├─ ContentRenderer ×24     │
                          │   └─ ColorHarmony            │
                          └──────────────┬───────────────┘
                                         │ Bitmap
                        ┌────────────────┴────────────────┐
                        ▼                                 ▼
                 Compose Image                   RemoteViews (one ImageView)
              (catalog · editor)                    (home screen)

   ┌──────────────┐        ┌──────────────┐        ┌──────────────┐
   │ :core:model  │◄───────│ :core:data   │───────►│ :core:design │
   │ WidgetStyle  │        │ Room·DataStore│       │ theme·motion │
   │ StyleDelta   │        │ catalog·setups│       │ haptics      │
   │ Facet·Setup  │        │ license       │       │ loading      │
   └──────────────┘        └──────────────┘        └──────────────┘
```

The single bitmap at the centre is the whole design: one render path feeds both the home screen and
every preview in the app, which is why a preview cannot drift from the widget it promises.

## Module graph

```
:app ─────────► composition root, navigation, DI wiring
  │
  ├─ :feature:catalog ─┐
  ├─ :feature:editor ──┼──► :core:design  (Compose M3 theme — app chrome only)
  ├─ :feature:wallpapers ┘
  │
  ├─ :widget ──────────► providers, scheduling, data sources
  │
  ├─ :core:render ─────► Canvas engine, ContentRenderers, typography
  ├─ :core:data ───────► Room, DataStore, catalog data, licensing
  └─ :core:model ──────► immutable domain types (no Android deps beyond graphics primitives)
```

Dependencies point one way only. Features never see each other. `:core:model` is pure enough to
unit-test the entire style-resolution system on the JVM.

## Battery policy

Stated plainly because it is a selling point, not an implementation detail:

| Cadence | Mechanism | Cost when phone is idle |
|---|---|---|
| Minute (clocks) | `ACTION_TIME_TICK`, registered at runtime, screen-on only | zero |
| Event (battery, notes, music) | System broadcasts and callbacks | zero |
| 15 min (weather, CPU, network) | **One** coalesced `PeriodicWorkRequest` for all such widgets | four wakeups/hour, network-constrained |
| Daily (month, quote, storage) | One worker at local midnight | one wakeup/day |

`updatePeriodMillis` is `0` in every provider XML — the platform's own alarm is inexact, floors at
30 minutes, and wakes the device. `UpdateScheduler.reschedule()` is called after every render and
cancels work for cadences nobody has placed. A user with only clock and battery widgets triggers
**no periodic work at all** for the life of the install.

## Data flow for one update

1. Host broadcasts `APPWIDGET_UPDATE` (or `TickReceiver` requests one).
2. `PrismWidgetProvider` loads all placed specs from Room and resolves them against the catalog.
3. `WidgetDataSource` returns a `(WidgetData, fingerprint)` snapshot — **no I/O**; network-backed
   types read a cache written earlier by `RefreshWorker`.
4. `PrismRenderer` returns a cached bitmap, or draws one.
5. One `RemoteViews` per widget, with a content description and a tap intent.
6. `UpdateScheduler` re-derives which cadences are still needed.

Nothing in that path touches the main thread and nothing blocks on the network.

## Testing strategy

- `:core:model` — pure JVM. Delta layering, catalog integrity (duplicate ids, orphan variants),
  every family's variant list against its declared count.
- `:core:render` — Robolectric golden-image tests. Each family × layout renders to a bitmap and is
  compared against a checked-in reference with a small perceptual tolerance. This is the regression
  net for the whole catalog: a change to `SurfacePainter` that breaks Luxury Gold fails CI.
- `:widget` — scheduler tests asserting that a given set of placed types produces exactly the
  expected enqueued work, including the "nothing placed → nothing scheduled" case.
- `:core:data` — Room migration tests and a `WidgetSpec` forward-compatibility test that
  deserialises a v1 JSON blob into the current schema.
