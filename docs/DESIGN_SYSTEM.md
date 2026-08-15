# Prism — Design System

**59 families · 708 widgets · 143 wallpapers · 28 curated setups · 8 packs**

Counts are computed from the catalog at runtime (`PrismCatalog.widgetCount`), never written by
hand, and asserted in `CatalogIntegrityTest`.

## The ecosystem rule

Every family ships all seven pillars. This is enforced by the type system — `Core` cannot be
constructed without deciding how a family handles each one — and re-checked in CI.

| Pillar | Why it's mandatory |
|---|---|
| Clock | The most-placed widget in any customisation app, by a wide margin |
| Weather | The second |
| Calendar | The reason people look at a home screen instead of opening an app |
| Battery | The most-placed *system* widget |
| Music | The only widget people touch rather than read |
| Notes | The "why is this app on my phone" widget for a large minority |
| Productivity (Tasks) | What turns a pretty screen into a used one |

Past the seven, each family carries 3–8 **extras** chosen to suit its own idea — Ledger gets a
portfolio and a ticker, Pulse gets streaks and a race-day countdown, Vinyl gets a full-bleed
sleeve. Extras are never added to reach a count.

## The forty families

### Foundation — the three we lead with
| Family | Idea | Mood |
|---|---|---|
| Minimal Mono | No surface, no colour; type does all the work | Quiet |
| AMOLED Black | True black; fewer lit pixels, longer battery | Dark |
| Luxury Gold | Warm charcoal, a light serif, one gold line | Warm |

### Liquid Glass — the flagship collection
Three families, 45 widgets, one shared light source at 315°.

| Family | Material | Depth |
|---|---|---|
| Liquid Glass | A thin bright pane | 0.28 |
| Liquid Glass Smoked | A thick dark plate | 0.92 |
| Liquid Glass Prism | Cut glass that splits the light | 0.62 |

Six layers, drawn in the order light arrives: backdrop → body tint → inner shadow → specular sweep
→ caustic → edge light. The inner shadow is the one that matters most; without it glass looks like
a sticker no matter how good the blur is.

**Backdrop blur, honestly.** Android widgets cannot sample the pixels behind them, and reading the
user's wallpaper needs a permission we won't ask for. But when the user set one of *our* wallpapers
— which Setups makes the common case — we already hold the bitmap, and the host tells us the
widget's rect. Cropping and blurring that gives a genuine backdrop. Three tiers, chosen per render
and never announced:

| Tier | Condition | Result |
|---|---|---|
| True | Our wallpaper + known rect | Real cropped, blurred backdrop |
| Synthetic | Wallpaper palette known | Gradient built from the region's colours |
| Painted | Nothing known | Tinted plate carried by the edge light |

All three share the same specular, caustic, and edge treatment, so a screen mixing tiers still
reads as one family. `depth` drives blur radius, shadow spread, specular sharpness, and caustic
strength together — one editor slider, one coherent illusion.

Originality: the material is derived from how physical glass behaves, not from any other product's
design language.

### Glass & Material — surfaces that look like something
Frosted Crystal (ice) · Sea Glass (tumbled green) · Chrome Liquid (polished metal, hard reflection
horizon) · Marble (veining as a low-blur mesh) · Origami (one sheet, one crease) · Paper Cut (flat
stock, real shadows, nothing shiny)

### Structure & Restraint — rules, not textures
Scandinavian · Japanese Zen · Swiss Grid · Bauhaus Primary · Brutalist Slab · Ink Serif ·
Material You · Neumorph Soft

These borrow design traditions' **constraints**, not their artwork: Swiss Grid keeps the grid and
flush-left setting, Bauhaus keeps three primaries and geometric forms, Zen keeps asymmetry and
empty space. The rules are the reference; every mark is drawn here.

### Colour & Light — each pinned to a real light condition
Aurora (polar night) · Gradient Flow (one light direction at 135°) · Sunset Fade (late dusk, break
sitting high) · Candy Pop · Terracotta (earth pigments) · Botanical (greenhouse noon) · Nordic
Frost (overcast north) · Seasonal Bloom (palette follows the date)

### Depth — distance rather than surface
Cosmic Drift (nothing in focus) · Deep Space (one lit object against void) · Monolith (mass, no
accent, no glow). All near-black, so also the cheapest families to display on OLED.

### Signal — instruments
Cyberpunk Neon · HUD Tactical · RGB Gaming · Pixel Retro · CRT Amber · Blueprint

House rule for this category: **technical marks must measure something.** A HUD bracket marks real
bounds, a blueprint's dimension lines measure the widget they sit on, RGB tracks a value instead of
cycling (a cycling widget is a battery bug). Instrument cosplay is what makes this category look
cheap, so it isn't allowed.

### Purpose — organised around a task
Executive Slate · Ledger · Pulse · Vinyl · Focus Grid

Density has its own aesthetic rules: tighter padding, smaller type scale, more rows, muted surfaces
so the data is the loudest thing on screen. These still ship all seven pillars, so a user can
furnish an entire screen without leaving the language.

## How a family is authored

```kotlin
internal val Marble = family(
    id = "marble", name = "Marble",
    note = "Cool stone with veining that never repeats.",
    mood = Mood.Bright,
    base = WidgetStyle(/* one palette, one type treatment, one surface */),
    wallpapers = listOf("stone-01", "stone-02", "stone-03"),
    core = Core(clock = ContentLayout.Hero, /* the seven decisions */),
    extras = listOf(v("mb-dial", "Marble Dial", AnalogClock, Small, Dial), /* ... */),
)
```

Roughly 25 lines. No renderer code, no UI code, no migration. The `family()` builder rejects
anything outside 10–15 variants, because a shelf reads badly at either extreme.

## Shipping checklist

A family ships only when it passes all of these:

1. **Reads at 2×2.** Tested at its smallest size first. If the idea only works large, it is a
   wallpaper, not a family.
2. **Legible over both** a white photograph and pure black.
3. **One idea, stated in `note`** in under nine words. Vague note, vague family.
4. **All seven pillars** — enforced by `Core`.
5. **No variant is another variant at a different size.** Enforced by the duplicate-signature test.
6. **Ink clears 3:1 contrast** against opaque surfaces — enforced in `CatalogIntegrityTest`;
   glass and mesh families go to golden-image review instead, since their background is the user's
   wallpaper.
7. **At least three paired wallpapers**, and each resolves — enforced both directions.

### Precious Materials — how one real material behaves under light
Titanium (brushed, anisotropic) · Mercury (a mirror that curves) · Quartz (hard facets) · Velvet
(matte pile, no specular) · Obsidian (near-black, one razor highlight) · Carbon (woven twill) ·
Pearl (nacre, hue shifts with angle) · Gemstone (one cut, three tints) · Satin (translucent with
the shine removed)

### Phenomena — an optical event, not a material
Holographic (diffraction) · Neon Frost (neon diffused through frosting) · Eclipse (occlusion) ·
Solar (a hot core bleeding out) · Abyssal (attenuation with depth) · Horizon (one hard divide) ·
Mirage (heat displacement) · Studio (seamless backdrop, one key light)

House rule: a phenomenon has a *mechanism*. You can say what is happening and why the widget looks
that way. "A nice purple gradient" is not a phenomenon and is not admitted here.

## Curated setups

Sixteen finished home screens: a family, one of its paired wallpapers, and a layout template.
Setups are **derived, not duplicated** — they name a family and resolve widget specs from its
guaranteed pillar variants at apply time, so restyling a family in a future update improves every
setup built on it.

Each setup carries a category (the twelve words people use to describe a home screen they want),
launcher advice, a suggested icon pack, and a palette. **Launcher advice is advice, never
automation** — most launchers expose no API for it, and an app that silently rearranges someone's
home screen is one they uninstall. **Icon packs are display-and-link only**: Prism never bundles or
redistributes third-party packs, and a setup is complete whether or not the suggestion is installed.

Four layout templates, each a real compositional idea: **Tower** (calm, one column), **Split**
(balanced, hardest to get wrong), **Gallery** (a hero widget with satellites), **Dense** (maximum
information, for the Purpose families). All four leave the bottom rows free — a setup that buries
the user's own apps is a setup they uninstall.

## Discovery

Users don't search for "Terracotta"; they search for *dark*, *minimal*, *glass*, *gaming*. Every
family carries 3–5 facets across four groups (Style, Tone, Feel, For). Facets **within** a group
are OR-ed, groups are AND-ed — what people expect from filter chips without being able to say so,
and getting it backwards is the most common way a filter UI feels broken.

Facets are hand-tagged. Deriving them from palette maths was the first attempt; it produced Luxury
Gold tagged "Dark, Monochrome" — true of its pixels, useless to a browsing human.

## Wallpapers

108 pieces in 24 series. **Series, not singles**: artwork is commissioned in runs of two to four
variations on one idea, because someone who likes a wallpaper usually wants the same thing in a
different weight. It is also how 108 pieces stay coherent instead of reading as a stock pack.

Every family names ≥3 wallpapers; every wallpaper names ≥1 family. Both directions are asserted, so
"Apply the whole look" can never dead-end and no artwork ships orphaned.

Delivery: AVIF at 1440×3120 with a JPEG sibling below API 31, shipped in the `:wallpaper_pack`
asset pack rather than the base APK — roughly 150 MB in AVIF against 380 MB in JPEG. Palettes in
`WallpaperCatalog` are the **art-direction brief** right now; a Palette pass at pack build time
overwrites them with measured values from the delivered art.

## Originality

No family reproduces another product's designs, and no wallpaper is derived from existing artwork.
Where a family names a tradition (Bauhaus, Swiss, Zen), it takes the tradition's rules — palette
limits, grid behaviour, asymmetry — and draws everything from those rules. Fonts are licensed
open-source variable families, subsetted and bundled.
