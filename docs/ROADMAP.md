# Prism — Phase Plan to Play Store Release

Re-scoped after Phase 1: the remaining phases optimise for **how the app feels**, not how many
widgets it contains. Widget count is now frozen at 504; nothing below adds to it.

| Phase | Deliverable | State |
|---|---|---|
| 0 | Architecture + working vertical slice | **Done** |
| 1 | Design system: 40 families, 474 widgets, 108 wallpapers, integrity tests | **Done** |
| 2 | Liquid Glass flagship, backdrop-blur pipeline, curated setups, discovery model | **Done** |
| 3a | Motion system, professional editor, local colour harmony | **Done** |
| 3b/4a | 17 new families, setup marketplace, featured shelves, widget packs | **Done** |
| 4b | Live home-screen preview + wallpaper experience | Deferred to 1.1 |
| 5 | Brand identity, onboarding, quality-audit harnesses, release documentation | **Done** |
| 6 | All 25 content renderers implemented | **Done** |
| 7 | Static validation pass — 15 defects fixed | **Done** |
| 8 | Algorithm verification, security review, production cleanup | **Done** |
| 9 | Compile, run, measure, RC1 | Blocked on a build environment |

Renderers moved from Phase 2 to Phase 6 deliberately. They are mechanical work against a proven
interface; motion, preview, and the shell are where the "luxury product" judgement lives, and
those decisions want to be made before the catalog is fully filled in.

---

## Phase 1 — complete

**40 design families, 474 widgets, 108 wallpapers, all cross-linked and asserted.**

What was built:

- **`FamilyDsl.kt`** — an authoring DSL where the seven-pillar ecosystem is *structural*. `Core`
  cannot be constructed without deciding how a family handles Clock, Weather, Calendar, Battery,
  Music, Notes, and Tasks, so an incomplete family is a compile error rather than a review miss.
- **36 new families** in six thematic files, plus the original four rewritten onto the DSL so they
  carry the full pillar set.
- **`WallpaperCatalog.kt`** — 108 pieces in 24 series, each with an art-direction palette brief and
  two-way family links.
- **`CatalogIntegrityTest.kt`** — 13 tests encoding the product's promises: pillar coverage, shelf
  size, globally unique ids, no duplicate type/layout/size signatures within a family, single
  collection membership, wallpaper links resolving in both directions, and a WCAG 3:1 contrast floor
  on opaque surfaces.
- **`PrismCatalog.Collection`** — six browsable shelves, because forty families in a flat list is a
  wall.

Numbers verified by parsing the catalog, not asserted by hand: 40 families, every one in the 10–15
variant band, 474 widgets total, 108 wallpapers with zero dangling references and zero orphans.

### Two judgement calls worth flagging

1. **474 widgets, not 500.** Families landed at 10–15 variants each based on how much their idea
   genuinely supports. Padding Monolith or Japanese Zen out to 15 would mean shipping variants that
   are the same design at a different size — which the duplicate-signature test now forbids
   outright. Quality-over-count is a build rule here, not a slogan.
2. **108 wallpapers, not exactly 100.** The series structure landed there naturally (24 series ×
   2–4 pieces). Trimming to a round 100 would have meant breaking up series, which is the thing
   that keeps the pack coherent.

### What Phase 1 does *not* include

The wallpaper artwork itself. `WallpaperCatalog` is the brief and the plumbing — ids, categories,
pairings, palettes, delivery format, asset-pack wiring. 108 original 4K pieces is an art
commission; `docs/WALLPAPERS.md` (Phase 6) will carry the per-series brief in the form an
illustrator can work from.

---

## Phase 2 — complete

**Liquid Glass flagship · backdrop blur · 16 curated setups · faceted discovery**

- **`Surface.LiquidGlass`** — a distinct surface from the single-plate `Glass`, with depth,
  specular angle, caustic, inner shadow, and refraction. `depth` drives four of those together, so
  one editor slider moves the whole illusion coherently.
- **`GlassPainter`** — six layers drawn in the order light arrives. The inner shadow is the layer
  that reads as *thickness*; it is the difference between glass and a sticker.
- **`Backdrop` + `StackBlur`** — real backdrop blur in the common case, via the fact that we hold
  the wallpaper bitmap whenever the user applied one of ours. Three tiers with shared upper layers,
  so falling back costs fidelity, never coherence. Crops are downsampled to 96px before blurring
  and cached per (wallpaper, rect, radius): eight glass widgets over one wallpaper blur once.
- **Three Liquid Glass families, 45 widgets** — Clear, Smoked, Prism. Covers every type on the
  brief: digital and analog clocks, weather, battery, calendar, music, notes, system info, finance,
  world clock.
- **`HomeSetup` + 16 curated setups** — derived from families rather than duplicated, so a family
  restyle flows into every setup built on it. Four layout templates, all leaving room for the
  user's own apps.
- **`Facet` / `BrowseQuery` + `PrismCatalog.browse()`** — 27 facets in four groups, OR within a
  group and AND across groups, plus facet counts so a chip that would return nothing can be
  disabled before it is tapped.
- **9 new integrity tests** — facet coverage (3–5 per family, every facet reaching ≥2 families),
  setup placements resolving, setup wallpapers being genuine family pairings, layouts staying
  inside the grid.

Catalog now: **42 families, 504 widgets, 108 wallpapers, 16 setups** — all verified by parsing, not
asserted by hand.

### Judgement call

Liquid Glass became three families rather than one expanded family. A family caps at 15 variants
because a longer shelf reads badly, and the brief listed ten distinct widget types plus the seven
pillars. Splitting by *material* — thin/bright, thick/dark, cut — gives each a real point of view
instead of one family padded to 25 variants. They share a light angle so a mixed screen still reads
as one set.

---

## Phase 3 — motion and feel (next, starting now)

The phase that decides whether this reads as a luxury product.

- **Motion spec first, code second.** One duration scale, one easing set, one spec document. Ad-hoc
  animation is the main reason apps feel busy rather than expensive.
- **Shared element transitions** — catalog tile → editor → preview, with the widget bitmap as the
  shared element. It is already one bitmap, which makes this unusually clean.
- **Micro-interactions** — chip selection, favourite toggle, slider detents, apply confirmation.
- **Haptics** — a small vocabulary bound to meaning, not sprinkled: selection tick on facet chips
  and slider detents, a single confirm on apply, nothing on scroll.
- **Loading states** — progressive reveal of preview bitmaps rather than spinners; skeletons only
  where a real wait exists.
- **Reduced-motion respected** throughout, and every animation cancellable by a scroll.

## Phase 4 — live home-screen preview

A realistic phone frame with the real wallpaper behind real widget bitmaps, at real cell geometry.
Wallpaper swaps, colour changes, and size changes update instantly, because previews and widgets
already share one render path. Setups preview as a whole screen before applying.

## Phase 5 — app shell

Onboarding (what it is, pick a look, place your first widget, the one-purchase promise), setup
gallery, family shelves, wallpaper gallery, favourites, recents, and search built on `BrowseQuery`.

## Phase 6 — remaining renderers + golden images

21 content renderers, then the golden-image harness across all 504 widgets. `assertComplete()`
keeps failing debug builds until every type is registered.

## Phase 7 — branding and Play assets

Icon set (adaptive, monochrome, legacy), platform splash, feature graphic, eight screenshots built
from real setups, listing copy, and the per-series wallpaper brief.

## Phase 8 — quality gates and release

Baseline profile, macrobenchmarks, TalkBack pass, RTL and locale sweep, 24-hour battery
instrumentation, billing verification on a closed track, launch checklist.

## Deliberately out of scope, permanently

No cloud sync, no accounts, no analytics SDK, no ad SDK, no remote config.

---

## Phase 3a — complete

**Motion system · professional editor · local colour harmony**

- **`docs/MOTION.md`** — the spec, written before any animation code. One governing rule (*motion
  explains, it does not perform*), four durations, three easings, three springs. Nothing in the app
  calls `tween(300)` directly.
- **`Motion.kt`** — the tokens, plus a staggered-reveal helper capped at eight items. Past the cap
  the last row is waiting on a queue rather than arriving, and "assembling" turns into "slow".
- **`Haptics.kt`** — five signals bound to meanings, and nothing on scroll. The failure mode with
  haptics is sprinkling: buzz on every tap and the user disables system haptics within a day,
  losing the two moments where feedback actually helps.
- **`Loading.kt`** — shimmer that travels at a fixed *screen-space rate* rather than a fixed
  duration, widget-shaped skeletons that never let the layout jump, and empty states built as
  invitations (what this is for, why it's empty, one action that fills it).
- **`ColorHarmony.kt`** — "Match Wallpaper", entirely local: k-means in Lab space, role assignment
  by lightness and chroma rather than population, then four named harmony rules with a hard
  contrast floor.
- **`EditorState.kt`** — the editor document. History is a list of deltas rather than commands, so
  undo/redo is two indices with no inverse operations to write. Continuous gestures collapse into
  one history entry, so a slider drag is one undo step instead of sixty.
- **`EditorScreen.kt`** — pinned preview, four intent-grouped tabs, preset row, per-detent haptics,
  undo/redo/reset, save-your-own presets.
- **Room v2** — `user_presets` table. A user preset is the same type as a built-in, so a look built
  on Liquid Glass drops onto Terracotta unchanged.

### Three decisions worth flagging

1. **"Match Wallpaper" is four chips, not one button.** The engine produces four defensible schemes
   from the same wallpaper and which is right is taste, not correctness. Labels are plain language
   — "Blends in", "Stands out" — so the user picks an *intent*. One magic button would just get
   tapped repeatedly in hope of a different answer.
2. **Match Wallpaper writes only colour fields.** Someone who spent a minute tuning radius and
   weight expects new colours, not a new widget. Surfaces recolour in place: a gradient stays a
   gradient, glass stays glass and only its tint moves.
3. **Contrast beats fidelity, always.** The harmoniser pushes lightness until ink clears 4.5:1 and
   accent clears 3:1 against the surface, even when that moves away from the wallpaper's actual
   colours. A beautiful unreadable widget is a bug, and the naive dominant-colour version of this
   feature produces them constantly.

### Deferred deliberately

Icon picker moved to Phase 4 — it depends on the icon-pack integration that the Setup gallery
(3b) introduces, and building it twice would be waste.

---

## Phase 3b — setup depth and new families (next, starting now)

- **15 new families** from the brief: Holographic Prism, Frosted Titanium, Liquid Metal, Crystal
  Frost, Velvet Dark, Midnight Carbon, Aurora Flow, Frosted Neon, Minimal Studio, Monochrome Pro,
  Luxury Marble, Satin Glass, Solar Flare, Eclipse, Ocean Depths. Each authored to the same
  checklist, each distinct from its neighbours — several of these sit close to existing families
  and will be pushed apart deliberately rather than shipped as near-duplicates.
- **Liquid Glass expansion** to small/medium/large clock sizes, agenda, countdown, quotes, health,
  and multiple tint options across all three families.
- **Setup metadata**: suggested icon pack, launcher settings, recommended grid size, and the
  extracted palette per setup — plus favouriting setups.
- **Wallpaper pairings** extended to cover the new families.

---

## Phase 3b / 4a — complete

**17 new families · setup marketplace · featured shelves · widget packs**

Catalog now: **59 families · 708 widgets · 143 wallpapers · 28 setups · 8 packs**, all verified by
parsing and by 31 integrity tests.

### The family list needed editing before it could be built

You asked for 30 families across two messages. Thirteen of them were the same ideas under different
names, and shipping them all would have produced exactly the "recolour of another family" problem
you asked me to avoid. What I did with each:

| Requested | Outcome |
|---|---|
| Frosted Titanium + Titanium | **Titanium** — brushed, anisotropic highlight (distinct from Chrome Liquid's mirror horizon) |
| Liquid Metal + Mercury | **Mercury** — curved mirror, radial pooling |
| Crystal Frost + Arctic Glass + Quartz | **Quartz** — hard facets. Arctic Glass and Crystal Frost were Frosted Crystal, which already ships |
| Velvet Dark + Velvet | **Velvet** — matte pile, specular removed entirely |
| Midnight Carbon + Obsidian | **Obsidian** *and* **Carbon** — genuinely different: volcanic glass vs woven twill |
| Emerald + Ruby + Sapphire | **Gemstone** — one family, three tints. They differ only in hue |
| Aurora Flow + Aurora Elite | Dropped — Aurora ships already |
| Luxury Marble | Dropped — Marble ships already |
| Monochrome Pro | Dropped — Minimal Mono ships already |
| Ocean Depths + Oceanic | **Abyssal** — attenuation with depth |
| Minimal Studio | **Studio** — a photographic studio, not a mood |
| Holographic Prism | **Holographic** — diffraction across the surface, vs Prism's edge splitting |
| Frosted Neon | **Neon Frost** — neon diffused through frosting; the inverse of Cyberpunk Neon |
| Satin Glass | **Satin** — translucent with the specular removed |
| Solar Flare / Eclipse / Horizon / Mirage / Pearl | Kept as-is, each a distinct optical mechanism |

Seventeen families instead of thirty, and the catalog is stronger for it. Every one passes the same
checklist, and each is pinned to a behaviour no other family has.

### Two product decisions you should sign off on

**1. There is no "Trending" or "Most Downloaded" shelf.** Both require collecting behaviour from
every install, and Prism ships no analytics — that is a promise on the store listing, not an
oversight. A fake Trending shelf backed by a hardcoded list is worse than none: it is a lie about
other users. Instead, `ComputedShelf.MostPlaced` gives the same *utility* — what actually gets used
— from the user's own Room data, labelled honestly as "You place these most".

**2. Icon packs are display-and-link only, and launcher settings are advice, not automation.**
Bundling third-party icon packs would be both a licensing violation and a Play policy violation; the
card shows the pack and opens its Play listing so the author gets paid. Launcher settings are a
three-tap checklist because most launchers expose no API, and an app that silently rearranges
someone's home screen is one they uninstall. Every setup is complete and correct without either.

### Also built

- **Setup metadata** — category (all twelve of your browse words, each covered by ≥1 setup),
  `LauncherAdvice` (grid, labels, dock, note), `IconPackSuggestion`, palette.
- **`FeaturedShelves`** — eight curated shelves plus three computed ones, and a similarity engine
  that weights facet matches by rarity: matching on "Luxury" says more than matching on "Dark",
  because eleven families are Dark and four are Luxury. Without that weight, "similar" returns
  every dark family in the catalog.
- **`PackCatalog`** — eight packs. A pack is a *toolkit for a purpose*; a setup is a *whole screen*.
  Someone who already loves their wallpaper wants a pack.
- **12 new integrity tests** — pack variants resolving, shelf families existing, similarity never
  returning the family you are looking at, setup category coverage, launcher advice fitting a real
  launcher.

---

## Phase 4b — live preview and wallpaper experience (next)

The realistic home-screen preview, wallpaper favourites and recents, related and similar wallpapers,
and matching-widget suggestions per wallpaper. Animated wallpaper previews will be a *slow pan over
the still* rather than video — a video-backed gallery would cost tens of megabytes per piece and
battery on scroll, for an effect a Ken Burns pan delivers at zero cost.

## Phase 6 — performance and testing, in detail

Your audit list becomes a gated checklist: startup under 400 ms cold on a mid-range device, widget
render under 8 ms cached / 25 ms cold, base AAB under 15 MB, zero periodic work when only local
widgets are placed, Room queries indexed and measured, recomposition counts asserted in tests. The
test pyramid: JVM unit (catalog, style resolution, harmony maths), Robolectric golden images across
all 708 widgets, Compose UI tests for the editor and galleries, macrobenchmarks for startup and
scroll, and a 24-hour battery run.

---

## Phase 5 — complete

**Brand identity · first-launch experience · quality audits · release documentation**

- **Brand** — adaptive icon (foreground/background/themed), wordmark, and `BRAND.md`. The themed
  icon is a real silhouette, not a desaturated logo: the spectrum survives as *three of something*
  once the system paints it one colour, which is the test a themed icon has to pass.
- **Onboarding** — four steps, and the user is looking at a real rendered widget by step two. No
  feature carousel, **no permission requests at all**, skippable from step one.
- **`WidgetAuditTest`** — the widget audit as a test suite. Bleed, measured contrast, legibility
  floor, emptiness, determinism, render budget, and light/dark/dynamic across every widget.
- **`tools/wallpaper_audit.py`** — resolution, aspect, banding, DCT blocking, clipping, weight,
  palette extraction, top-third luminance. Exits non-zero, drops into CI.
- **Release documentation** — `CONTRIBUTING.md`, `CHANGELOG.md`, `VERSIONING.md`,
  `RELEASE_CHECKLIST.md`, three issue templates, an ASCII system diagram in `ARCHITECTURE.md`.
- **Play assets** — `PLAY_LISTING.md` (titles, descriptions, ASO, feature graphic and screenshot
  concepts, FAQ), plus privacy policy and terms templates.
- **`FUTURE.md`** — every expansion item checked against the actual code, including two that *would*
  require a redesign, named so they get decided rather than drifted into.

### Decisions worth flagging

**The audits are harnesses, not sign-offs.** A manual review of 708 widgets across five sizes, two
themes and dynamic colour is roughly 7,000 screenshots — nobody does that twice, so a manual audit
decays from the day it is signed. Both audits are executable and run on every build, which turns the
audit from a milestone into a property of the codebase. **They have not been run yet**: there is no
delivered wallpaper artwork, and 21 content renderers are still unimplemented, so a green run today
would be measuring almost nothing.

**Onboarding requests zero permissions.** Prism needs none to show a clock, a battery widget, or a
wallpaper. Calendar, location, and activity are requested at the moment a widget needing them is
placed, with the reason and the consequence of declining stated. An onboarding permission wall is
one of the most common reasons a paid app is refunded inside the first minute.

**The release checklist has empty measurement cells, and they block release.** Every budget —
400 ms cold start, 15 MB base AAB, 8 ms cached render, zero wakeups for local-only widgets — has a
blank next to it. I can specify budgets from the architecture; I cannot measure them without
hardware. A budget without a measurement is a wish, and I would rather hand you a gate than a claim.

---

## What remains before launch

Honestly, three things:

1. **21 content renderers** (Phase 6). `AnalogClock`, `Weather`, `Agenda`, `MusicPlayer`, `Notes`,
   `Todo`, and the rest. Mechanical work against a proven interface — `assertComplete()` fails debug
   builds until every type is registered — but it is real work, and roughly 60% of the catalog does
   not draw yet.
2. **The artwork.** 143 original wallpapers is an art commission. Every id, category, pairing,
   palette brief, delivery format, and quality gate exists; the images do not.
3. **Measurement on hardware** (Phase 7). Fill the checklist, run the 24-hour battery test, review
   the pre-launch report, and only then make the battery claim on the listing.

Everything else — architecture, catalog, rendering engine, editor, motion, setups, discovery,
branding, documentation, store listing — is done.

---

## Phase 6 — complete

**All 25 content renderers implemented. No placeholders left in the render path.**

| Group | Renderers |
|---|---|
| Time | DigitalClock, AnalogClock, WorldClock, Countdown |
| Calendar | DayCard, MonthCalendar, Agenda |
| Environment | Weather, SunriseSunset |
| Device | Gauge (Battery, CPU, RAM, Storage, Network, Steps), SystemInfo |
| Text | Notes, Todo, HabitTracker, Quote |
| Media | MusicPlayer, Photo |
| Series | Finance, Crypto, Health |

`assertComplete()` now runs inside the DI provider rather than at app start, so a missing renderer
fails at graph construction — the earliest possible point.

### Decisions inside the renderers

- **No second hand on the analog clock.** It needs a redraw every second, which on a widget means
  either a wildly inaccurate clock or an unacceptable battery cost, and a frozen second hand looks
  broken. Removing it is the honest choice.
- **The analog dial inherits family personality.** Tick treatment is driven by the family's
  `fontWeight`: heavy families mark every hour, light families mark quarters only. A twelve-tick
  dial in a 300-weight family reads as busy at 2×2.
- **Weather glyphs are drawn as paths, not shipped as a font.** They inherit the family's stroke
  weight, so a Brutalist cloud is heavy and a Zen cloud is a hairline. An icon font would make every
  family's weather widget look like the same widget in a different frame.
- **Countdown changes unit as the target approaches** — months, days, hours, minutes. "4,392 hours"
  is correct and useless; "0 days" on the morning of the event is worse.
- **Quote sizes type from the quote's own length**, targeting 60–95% fill, so a twelve-word quote
  and a sixty-word one both look placed rather than dropped in.
- **Completed tasks are struck through, not hidden.** A list that empties as you tick things shows
  you nothing at the end of a productive day.
- **CPU is labelled "App CPU".** `/proc/stat` has been restricted since Oreo, so the widget reports
  our own process time against wall time. That is not system-wide load, and the label says so.
  Shipping a plausible number that is actually wrong is worse than a narrower number that is right.
- **RAM comes from ActivityManager**, so the figure matches what the user sees in Settings. Matching
  Settings matters more than being closer to the kernel.
- **Countdown targets are stored as local date-times, not timestamps**, so "my birthday" survives a
  timezone change instead of drifting by hours.

### Data source completed

`WidgetDataSource` now returns real data for every local type — countdown, world clock, system info,
CPU, RAM, network, storage, battery, clock — with no I/O on the update path. Network-backed types
(weather, finance, crypto, quotes) read the cache written by `RefreshWorker`, exactly as designed.

---

## Phase 7 — what I cannot do from here, stated plainly

Everything remaining needs things this environment does not have, and I would rather hand you an
accurate blocker list than a green checkmark I cannot justify.

**Requires a build environment (Gradle, Android SDK, JDK 17):**
- Compiling. This code has never been through a compiler. Expect a first-build session of import
  fixes and signature mismatches — I have been careful, but nobody writes 60 files of Kotlin without
  a compile error.
- Recording golden references. The harness exists; the references have to be generated from a real
  first run and reviewed by eye, which is a human judgement call and not automatable.
- Producing an AAB, and therefore RC1.

**Requires physical devices:**
- Every cell in the `RELEASE_CHECKLIST.md` budget table. Cold start, memory, render times, scroll
  frames, and the 24-hour battery run. I specified those budgets from the architecture; measuring
  them is the whole point and estimates would defeat it.
- Low-end and flagship QA, orientation, and screen-size passes.
- TalkBack pass. `Accessibility.describe()` covers every type, but whether it *sounds* right is a
  listening test.

**Requires people and money:**
- 143 original wallpapers. Every id, pairing, palette brief, format, and quality gate exists. The
  images do not, and generating them is not something I should fake.
- Legal review of the privacy policy and terms.
- A Play Console account, signing keys, and a closed test track.

### The honest status

**Release Candidate: not reached, and it would be wrong to label it one.** RC means "we believe this
is shippable and have evidence." What exists is a complete, coherent, fully-specified implementation
with every quality gate written as executable code. What is missing is the evidence — a compile, a
run, a measurement, and artwork.

The nearest true statement: **feature-complete in source, pending first build.**

### The order I would tackle it in

1. Open in Android Studio, fix the first-build errors, get a debug APK on a phone.
2. Place one widget from each of the 25 types. Fix what looks wrong — there will be some.
3. Record goldens, review every one by eye, commit them.
4. Run the audit suites; fix what they catch.
5. Commission the artwork; run `wallpaper_audit.py` on delivery.
6. Fill the budget table on real hardware; optimise against measurements.
7. Then, and only then, RC1.

---

## Phase 7 — static validation complete, RC1 not reached

I could not build. This environment has JDK 21 and no network — no Kotlin compiler, no Gradle, no
Android SDK, and no way to fetch them. Rather than stop, I ran the validation that *is* possible
here and fixed what it found.

### What ran

`tools/static_check.py` — a structural checker written for this codebase. Delimiter balance,
duplicate top-level declarations, unresolved project imports, module dependencies against actual
usage, Compose import hygiene, leftover debug code. **65 files, currently zero issues.** It found
six real defects on its first run.

A second, deeper reference-resolution pass flagged 366 candidates, of which three were real
(`docs/DEFECT_LOG.md` explains why the rest were noise and why I did not ship that pass as a gate).

### What was fixed — 15 defects

Full table in `docs/DEFECT_LOG.md`. The three that matter most:

- **Four classes referenced but never written** (`RefreshWorker`, `DayRolloverWorker`,
  `WallpaperPalette`, `WidgetTapRouter`), plus four more in `WidgetDataSource` (`CachedFeed`,
  `SunTimes`, `CalendarReader`, `LocalContent`). I had written calling code and named collaborators
  without writing them. That is precisely the "no placeholders" failure the previous phase claimed
  to have eliminated, and it survived because nothing had compiled. It is the best evidence in this
  log for why I was right not to call the last phase done.
- **The editor's shadow slider was wired to a no-op** because `StyleDelta` had no shadow field. A
  control that moves and does nothing is worse than a missing one.
- **The audit suite sat in `:core:render` but needed `:core:data` and `:widget`** — an upward
  dependency that would have inverted the module graph the whole architecture rests on. Moved to
  `:app`, the only module that legitimately sees everything.

Also implemented while fixing: a real NOAA sunrise/sunset calculation, a `CalendarContract.Instances`
reader (querying `Events` directly is the classic bug that makes a weekly standup appear once and
then vanish), an atomic write-then-rename feed cache, and a bounded photo decoder that samples
during decode rather than after.

### Deliverables produced

`docs/BUILD.md` (including the five places I expect the first build to break, in order),
`docs/TESTING_REPORT.md`, `docs/PERFORMANCE_REPORT.md`, `docs/DEFECT_LOG.md`,
`docs/RELEASE_NOTES.md` with a known-limitations section.

### Status: not a Release Candidate

RC means "we believe this is shippable and have evidence". After this phase there is *more*
evidence than before — one automated gate passing, fifteen defects removed — but still none of the
three kinds that matter: **it has not compiled, it has not run, and nothing has been measured.**

The accurate description remains **feature-complete in source, statically validated, pending first
build.** I would expect an hour or two of compile errors on the first `assembleDebug`, and I would
be surprised if the first run of 25 widget types produced no visual defects.

`docs/BUILD.md` has the exact order I would work through it. If you get it building and paste me the
errors, I will work them with you.

---

## Phase 8 — verified what could be verified, RC1 still not reached

I cannot compile. But two subsystems are pure mathematics, and mathematics can be executed. I
transcribed both from the Kotlin and ran them against published reference values. **That found six
real bugs**, which is the point of doing it rather than asserting correctness.

### `tools/verify_algorithms.py` — runs, passes

Validates the *algorithms*, not the Kotlin. A pass means the maths is right; it does not mean the
code runs.

**Sunrise/sunset** against almanac values for Srinagar (summer solstice), London (winter solstice),
the equator (equinox), and Tromsø (polar day). All within four minutes.

Two apparent failures on first run were *my test expectations*, not the algorithm:
- I claimed the equator equinox gives 06:00/18:00. It does not — refraction and the sun's angular
  radius make equinox day length about 12h07m everywhere, so the computed 06:04/18:10 was correct
  and the round numbers were wrong. Recording it because this is exactly how a correct algorithm
  gets "fixed" into a broken one.
- The code comment claimed accuracy "to about a minute". Measurement says three to four. The
  comment now says four. An overclaim in a comment is a defect.

**Contrast maths** against the published WCAG reference ratios: 21:1 for white on black, 4.478:1 for
`#777` on white. Exact.

**Every family's declared legibility**, ink against *every* surface stop rather than just the first.
This found the real bugs: **eight families whose gradient spanned a wider luminance range than their
ink could survive.** Worst was Solar at **1.03:1** — cream numerals on a white-hot core, effectively
invisible, and it would have shipped.

Remediated by compressing the offending stop's lightness (hue and saturation untouched) by the
minimum that clears 3:1 — `tools/fix_contrast.py`, 12 stops adjusted. Solar's core came back as a
dull olive, which is contrast-correct and design-wrong, so I hand-corrected it to a saturated amber.
Candy Pop needed hand work too: white on bright cyan is one of the worst pairings in common use, and
it is everywhere in apps that never measured it.

All 44 families with a fixed background now clear 3:1. Worst is Gradient Flow at 3.05:1. The other
15 use glass, mesh, extruded or dynamic surfaces, which have no fixed background — those are covered
by the rendered-pixel checks in the audit suite instead.

### Security review — six findings, all fixed

Full detail in `docs/SECURITY_REVIEW.md`. The two that mattered:

- **`BIND_NOTIFICATION_LISTENER_SERVICE` was declared as a `<uses-permission>`.** It grants nothing
  — the system holds it — and appears in Play's data-safety review as a permission we cannot
  justify. Replaced with a properly declared `NotificationListenerService` that reads media sessions
  only and never overrides `onNotificationPosted`.
- **No ProGuard keep rules for `kotlinx.serialization`.** R8 full mode would strip the serializers,
  and since `WidgetSpec` is persisted as JSON, every placed widget would break on upgrade — in
  release builds only. Worst possible place for a bug to first appear.

Also fixed: billing entitlement excluded from backup (restoring a stale one either wrongly unlocks
or wrongly locks a paying customer out), atomic feed-cache writes, bounded photo decoding.

### Production cleanup

Deleted a duplicate wallpaper model. Added the missing resources the manifest referenced but that
did not exist: `themes.xml`, `colors.xml`, `data_extraction_rules.xml`, `backup_rules.xml`, adaptive
icon XML. Added `proguard-rules.pro`, `lint.xml` (four issue classes promoted to errors), Room schema
export, and lint gating on release builds.

### Status: still not RC1

Two automated gates now pass — `static_check.py` (66 files, zero issues) and
`verify_algorithms.py`. That is more evidence than any previous phase produced, and it caught defects
that would have shipped.

It is still not the evidence RC1 requires. **The project has not compiled, has not run, and nothing
has been measured on hardware.** Everything in `PERFORMANCE_REPORT.md` is still empty, and I am not
going to fill it with plausible numbers.

Accurate status: **feature-complete, statically validated, algorithmically verified, pending first
build.**
