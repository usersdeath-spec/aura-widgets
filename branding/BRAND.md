# Prism — Brand Guide

## The mark

A single white beam enters a triangular prism and leaves as three separated colours. It is the
product's thesis in one glyph: one home screen goes in, a spectrum of designs comes out.

Built to survive the icon grid rather than to look good in a case study — three shapes, no
illustration, legible at 48px. The beam sits on the horizontal centre line and the apex is inset far
enough that no adaptive mask on any OEM launcher clips it.

| Asset | File | Notes |
|---|---|---|
| App icon | `prism-icon.svg` | 512×512 store icon |
| Adaptive foreground | `icon-foreground.svg` | Mark inside the 66×66 safe zone |
| Adaptive background | `icon-background.svg` | Near-flat gradient — flat colour kills the parallax |
| Themed (Android 13+) | `icon-monochrome.svg` | Silhouette; spectrum survives as *three of something* |
| Wordmark | `prism-wordmark.svg` | Mark at cap height ahead of the word |

## Colour

The chrome is near-monochrome on purpose. A widget store is a gallery: anything the UI does with
colour competes with the artwork the user is trying to judge.

| Role | Value | Use |
|---|---|---|
| Ink | `#0B0D12` | App background (dark) |
| Ink Soft | `#141821` | Cards, sheets |
| Mist | `#E8E9ED` | Primary text on dark |
| Mist Dim | `#9AA0AC` | Secondary text |
| Refract Violet | `#7C5CFF` | Primary accent — selection, CTA |
| Refract Cyan | `#3FD8E0` | Secondary accent — sparingly |
| Refract Amber | `#FFB35C` | Third spectrum colour — mark only |

The three refraction colours appear together **only in the mark**. In the UI, violet carries
selection and the purchase call to action; the other two are for the logo and nothing else.

## Typography

| Role | Face | Why |
|---|---|---|
| Display | Bricolage Grotesque, SemiBold, −0.02em | Slightly irregular widths give family names a hand-set feel a neutral grotesk can't |
| Body | Inter Tight, Regular/Medium | Stays legible at 12sp under dense metadata |
| Mono | JetBrains Mono | System readouts, in-app and in-widget |

These are the same variable files the widget renderer loads, so a family name in the catalog is set
in the same metal as the widget beneath it. All subsetted to Latin + digits + common symbols,
roughly 40 KB each.

## Splash

Platform `SplashScreen` API only — no custom activity, no artificial delay. Icon on `#0B0D12`, the
system's own fade-out, and `setKeepOnScreenCondition` released as soon as the catalog is built
(single-digit milliseconds). A splash that lingers to show off branding is a splash users learn to
resent.

## Illustration style

There is almost none, and that is the style. Empty states use type and space rather than spot
illustrations, because a cartoon in an app whose whole proposition is restrained visual design
undercuts the proposition. The one exception: onboarding uses **real rendered widgets** as its
illustration — the product illustrating itself.

## Voice

Plain, specific, and never breathless. "True black surfaces. Fewer lit pixels, longer battery." not
"Experience the ultimate AMOLED aesthetic!" Every family note is under nine words and says what the
family *is*, not how it will make you feel. Superlatives are reserved for facts we can defend.

## What we never do

- No stock-photo people, no gradients-on-gradients, no drop shadows on text
- No countdown timers, no fake scarcity, no "limited time" anything
- No claims about other apps
- The three spectrum colours never appear together outside the mark
