# Competitive Audit — Paid Widget Apps, Play Store India

Four listings, read from store pages captured 09 Aug 2026.

| App | Publisher | Price | Rating | Reviews | Size | Rank | Claim |
|---|---|---|---|---|---|---|---|
| Everything Widgets | JustNewDesigns | ₹45 (from ₹180) | 4.7 | 9K | **10 MB** | — | 400+ widgets "inspired by NOTHING" |
| Glass Widgets | AppsLab Co. | ₹90 | 4.5 | 5K | **15 MB** | **#4 top paid** | 350+ "premium glass-style finish" |
| OneUI Widgets | JustNewDesigns | ₹100 | 4.4 | 3K | **12 MB** | **#5 top paid** | 300+ "inspired by OneUI" |
| Material You Widgets | AppsLab Co. | ₹90 | 4.5 | 5K | **12 MB** | **#9 top paid** | 300+ "Material You and M3 expressive" |

Two publishers own four of the top slots. This is a duopoly of small studios, not a market with an
incumbent giant — which is good news, and also means the playbook is visible and repeatable.

---

## Finding 1 — every winner is named after a design language people already search for

Nothing OS. OneUI. Material You. Glass. Not one of them sells "our original design families".

This is the single most important finding, and it directly contradicts how Prism is positioned.
Our catalog is 59 families called Mirage, Abyssal, Terracotta, Obsidian. Those are *portfolio*
names. Nobody types "terracotta widgets" into Play search. People type **glass widgets**, **amoled
widgets**, **material you widgets**, **minimal widgets** — and the apps named after those terms are
the ones ranking.

Prism's Collections already carry the right words (Liquid Glass, AMOLED, Minimal, Luxury). The
error is that the *store-facing surface* is family names rather than collection names.

**What I would change:** lead with the collection, not the family. "Prism — Glass, AMOLED & Minimal
Widgets" as the store title. Family names stay inside the app, where they add character; they come
off the shop window, where they only cost search traffic.

**What I would not change:** actually imitating Nothing's or Samsung's design language. Two of these
four apps do exactly that, and it is a real legal exposure — trade dress and design-language
imitation is a takedown risk that lands on the app, not the idea. *Material You* is different: it
is Google's own published, openly-licensed design system, and our `material-you` family already
targets it legitimately. Glass, AMOLED, minimal and gradient are generic categories that nobody
owns. Those four are the honest, defensible version of this strategy, and they cover three of the
four listings above.

## Finding 2 — 10 to 15 MB, all of them

Everything Widgets ships **300+ wallpapers inside a 10 MB app**. Ours plans a 150 MB wallpaper pack.

That gap is not a rounding error; it is a different product decision. At 10 MB the app installs
instantly on a mid-range phone over mobile data, which matters enormously at a ₹45–₹100 price point
where the purchase is impulsive.

**What I would change:** drop the AVIF-at-1440×3120 plan. Ship wallpapers at 1080×2400, aggressively
compressed, and fetch the full-resolution version on demand only when the user actually applies one.
Base install target: **under 15 MB**, wallpaper pack optional.

## Finding 3 — "No KWGT. One tap to add."

Three of the four listings put this in the screenshots as a headline. The real competitor is not
another widget pack — it is KWGT, which is powerful and miserable to use. Their entire pitch is
*you do not have to learn anything*.

Prism does one-tap placement already. We simply never said so. It belongs in the short description.

## Finding 4 — the screenshot formula is a dense diagonal collage

Every listing leads with 30–50 widgets tiled at an angle across a phone, high contrast, with big
callouts: "400+ Widgets", "Works on any Android", "Supports Dark & Light".

The collage *is* the demo: it proves scale and quality in one glance, before any text is read.

Earlier I argued for screenshots of real setups and "no mockups". That was right about honesty and
wrong about the first screenshot. The correct compromise: **screenshot 1 is the collage** (real
renders, arranged — not fabricated widgets), and screenshots 2–8 are the real product.

## Finding 5 — the pricing band, and the discount tactic

₹45–₹100, i.e. roughly $0.55–$1.20. Extremely low. At that price, ranking and volume are everything.

Everything Widgets shows "₹45, list ₹180, 75% off, offer ends 12/08/26" — a discount that, judging
by how these listings run, is close to permanent. It works, and I still would not recommend it: a
fake countdown is the kind of thing that shows up in reviews and it contradicts the trust position
we are selling. A **genuine** launch price (₹99 for the first month, then ₹149) gets most of the
psychological benefit and is true.

## Finding 6 — all four are paid AND carry in-app purchases

Every one says "In-app purchases" under the title. Ours says: one purchase, nothing locked, ever.

That is a real differentiator and worth keeping — it is the thing to put in the short description,
because none of these four can say it. It also means lower revenue per user, which is the trade.

---

## What actually needs to change in Prism

Ordered by impact on someone deciding whether to spend ₹99.

1. **Store title and collection-led positioning.** Free, and probably the largest single lever.
2. **Fix the in-app catalog so it looks like the product.** Widgets currently render on flat cards,
   so the glass families have nothing to be glass *over* and Minimal Mono's white type is invisible.
   Every competitor screenshot shows widgets on a wallpaper. This is a bug, not a design choice.
3. **Cut install size to under 15 MB.**
4. **Say "one tap to add, no KWGT needed" in the listing.**
5. **Collage as screenshot 1.**
6. **Launch at ₹99 with a real, dated introductory price.**

## What not to copy

- Their design languages (Nothing, OneUI) — legal exposure, and it makes the app derivative by
  construction.
- Permanent "75% off, offer ends" countdowns.
- "In-app purchases" on a paid app.

The 4.7-star, 9K-review leader charges ₹45 and ships 10 MB. We do not beat that on count — they are
at 400 and we are at 708, and nobody chooses on that. We beat it on the two things their reviews
cannot claim: **complete matching sets** (every family ships all seven pillars) and **nothing locked,
ever**.
