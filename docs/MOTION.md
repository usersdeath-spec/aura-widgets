# Prism — Motion Spec

Written before any animation code, because ad-hoc motion is the main reason apps feel busy rather
than expensive: every screen invents its own duration and the result reads as noise even when each
piece is fine in isolation.

## The governing rule

**Motion explains, it does not perform.** Every animation answers one of two questions:

- *Where did this come from?* — shared elements, sheet entrances, list reveals
- *What just changed?* — selection, value changes, state transitions

Anything that answers neither is deleted, however nice it looks.

## Tokens

Four durations. Nothing calls `tween(300)` directly; if a motion needs a value that isn't here,
either the value is wrong or the token set is incomplete, and both are conversations rather than
local decisions.

| Token | ms | Used for |
|---|---|---|
| `instant` | 90 | Live preview responding to a slider — must not feel animated |
| `quick` | 180 | Selection, ripple, chip toggle, reduced-motion cross-fades |
| `standard` | 280 | Anything travelling across the screen |
| `deliberate` | 460 | The two moments that should feel like events: applying a setup, completing the purchase |

Three easings: `enter` decelerates hard (arriving things should look like they were already
moving), `exit` accelerates (leaving things get out of the way), `standardEasing` is symmetric for
things that stay and change shape.

Three springs, for anything a finger is directly responsible for: `gentle` (sheets, hero
transitions), `snappy` (controls), `taut` (preview reacting to a drag — near-zero overshoot, because
a preview that wobbles reads as inaccurate).

## Signature transitions

**Catalog tile → editor.** The widget bitmap is the shared element. This is unusually clean here
because the widget genuinely *is* one bitmap — the same object grows into the editor's preview slot
rather than being re-created, so there is no cross-fade seam to hide.

**Setup card → live preview.** The wallpaper is the shared element and the widgets settle onto it
in a stagger, which reads as the screen assembling itself.

**Shelf reveal.** Items rise 12dp and fade, staggered 28ms, capped at eight. Past the cap the last
row is waiting on a queue rather than arriving, and the effect turns from "assembling" into "slow".

## Haptics

Five signals, each bound to a meaning. The failure mode is sprinkling: buzz on every tap, and within
a day the user disables system haptics entirely — losing the two moments where feedback genuinely
helps. **Nothing on scroll.**

| Signal | When |
|---|---|
| Tick | Slider crossing a detent, facet chip toggling |
| Select | Picking a widget, wallpaper, or preset |
| Confirm | Widget placed, setup applied |
| Reject | An action that cannot proceed |
| Long press | Entering reorder or a context menu |

## Loading

Skeletons only where a real wait exists — widget previews rasterise in milliseconds, so a skeleton
there would flash and be worse than nothing. Skeletons are for wallpaper decode and first catalog
build.

Progressive reveal beats spinners everywhere else: each preview appears the instant its bitmap is
ready. The screen assembles rather than blocking, which reads as fast even when total time is
unchanged.

Shimmer travels at a fixed screen-space rate, not a fixed duration. Duration-based shimmer makes
small placeholders look frantic and large ones look stalled.

## Empty states

Three parts, always: what this space is for, why it's empty, and one action that fills it. An empty
favourites screen that doesn't say how to favourite something is a dead end.

## Reduced motion

Transitions collapse to cross-fades at `quick`; springs become linear tweens. Everything still
changes state *visibly* — removing motion entirely makes an interface harder to follow, not easier.
Nothing is ever simply cut.
