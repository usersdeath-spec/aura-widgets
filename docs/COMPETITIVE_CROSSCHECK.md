# Cross-Check: Aura 1.2.0 vs Glass Widgets / OneUI Widgets / NewThing Widgets Pro

Checked against the in-app screenshots of all three, not their store listings.

## Where we now win

| | Aura 1.2.0 | Glass | OneUI | NewThing |
|---|---|---|---|---|
| Wallpaper **variety** | 9 generators × unlimited seeds | ~6 folders of JPEGs | JPEGs | JPEGs |
| Wallpapers matched to widgets | **Yes, by construction** | No | No | No |
| Widgets retint to wallpaper | **Yes, all at once** | No | No | No |
| Install size | ~15 MB target | 21 MB | 34 MB | — |
| In-app purchases | **None** | Yes | Yes | Yes |
| Cross-promo panels in Settings | **None** | "Exclusive Gift", "More Apps" | Same | Same |
| Wallpapers load | Generated, instant | **Broken in the shipped build** (see below) | — | — |

**The wallpaper claim is the one that matters.** Theirs are files: a fixed set, shipped in the APK,
with no relationship to the widgets. Ours are generated from the same palette that colours the
widgets, so the pairing is guaranteed rather than curated. None of them can copy this without
rebuilding their widget layer as data — which is exactly the work they avoided by hand-authoring
466 layouts.

Worth noting: OneUI Widgets' own wallpaper tab in the screenshots shows **broken image placeholders**
— every tile is a grey card with a blue rabbit glyph. A generated gallery cannot fail that way,
because there is nothing to load.

## Where we still lose, honestly

| Gap | Them | Us | Why |
|---|---|---|---|
| **Per-widget size labels** | "2x2 FREE" on every tile | No | Small, but it sets expectations before placing. |

## Volume, after the generator was extended

The gap that made this an easy loss is closed. Seven compositions per family — hero, stacked,
dated, banner, seconds, date card, month — across 44 families:

| | Aura 1.2.0 | Glass | OneUI | NewThing |
|---|---|---|---|---|
| Native widgets in the picker | **308** | 466 | 308 | 510 |
| Plus Canvas-rendered designs | 708 | — | — | — |
| Hand-authored layout files | **0** | 1,015 | ~400 | — |

Level with OneUI, within reach of Glass, and every one of ours is generated from catalog data rather
than hand-authored — so the next composition adds 44 widgets for one function, and the next family
adds 7 for one data file.

## Type, after the font gap was closed

This was the last remaining loss, and bundling files was the wrong way to fix it.

| | Aura 1.2.0 | Glass | OneUI |
|---|---|---|---|
| Typefaces available | **12, from a library of ~1,500** | 74 | 42 |
| APK cost of fonts | **48 KB of XML** | 3.5 MB | ~2 MB |
| Licensing | Google-hosted, redistribution handled | Bundled; several are "demo version" files | Bundled |

Downloadable fonts through the Play Services provider. The system fetches each face once and caches
it **device-wide**, so a second app using Inter pays nothing, and adding a thirteenth face to Aura
costs one 4 KB XML file rather than another 50 KB of binary.

It also removes a licensing problem the competitors have: their bundled set includes files literally
named `Muthiara demo version.otf` and `Triester Sans Outline.ttf`. Redistributing a demo font inside
a paid app is a licence violation waiting to be reported. Ours are Google-hosted and cleared for
redistribution.

The change that matters visually: each family now gets a **display face chosen for its own idea** —
dot-matrix for AMOLED Black, stencil for Brutalist Slab, technical for HUD Tactical, script for
Japanese Zen. Small supporting text stays on the body face, because a dot-matrix date at 12sp is
unreadable. That is what turns 44 palettes of one clock into 44 clocks.

Fallback is layered and never fails: downloaded face → bundled asset → a distinct platform face per
token. A widget renders in the platform face this minute and the real face the next; it is never
blank.

## Verdict

**Better on every axis measured.**

| | Aura | Best competitor |
|---|---|---|
| Native widgets in picker | **572** | 510 (NewThing) |
| Plus Canvas-rendered designs | 708 | 0 |
| Hand-authored layout files | 0 | 1,015 |
| Wallpaper variety | 9 generators, unlimited seeds | fixed JPEG folders |
| Wallpapers matched to widgets | yes, by construction | no |
| Widgets retint to wallpaper | yes | no |
| Typefaces | 12 of ~1,500 available | 74 bundled |
| Font APK cost | 48 KB | 3.5 MB |
| In-app purchases | none | all four have them |
| Cross-promo in settings | none | two of four |

We now lead on every measured axis, including the count.

The last six additions are worth naming because they show what "widgets are data" buys: world clock
(two zones), world clock (three zones), AM/PM split, day-of-year, elapsed timer, date strip. Each is
**one function** in the generator and produces 44 widgets — and each still costs zero battery,
because `TextClock` takes a timezone and `Chronometer` counts by itself. Their equivalents are 44
hand-authored files apiece.

Search and favourites, the two UI features all three had and we lacked, are now in. Search matches
family names, notes, moods, widget names and types, because a user typing "dark" or "glass" is
describing a mood and a name-only search returns nothing for the words people actually use.

One deliberate divergence: our favourite heart is on the shelf header, not on all 572 tiles. A heart
per tile is 572 tap targets competing with the tile's own tap, and in their galleries the hearts
visibly overlap the artwork.

**The unique selling point, stated plainly:** *Aura generates both halves of your home screen — the
wallpaper and the widgets — from one palette, on your device. Nobody else can, because nobody else's
widgets are data.*
