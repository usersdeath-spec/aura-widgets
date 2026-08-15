# Release Notes — 1.0.0 (draft)

**Not yet released.** This is the intended text for the first Play listing, held until a build
exists and the checklist is filled.

---

## Prism 1.0

**Buy once. Unlock everything. Forever.**

Prism is a home screen studio: 59 design families, 708 widgets, 143 original wallpapers, and 28
complete home screen setups you can apply in one tap.

**Complete families.** Every family ships a matching clock, weather, calendar, battery, music,
notes, and tasks widget, so you can furnish a whole screen without anything looking borrowed.

**Liquid Glass.** Three materials — clear, smoked, and cut — sharing one light source, with real
backdrop blur where Android allows it and honest fallbacks where it doesn't.

**Setups.** Browse finished home screens the way you'd browse interiors. Wallpaper, matched widgets,
a suggested grid, a palette. Preview before you commit.

**Match Wallpaper.** One tap builds a balanced colour scheme from your wallpaper, on your device, in
four flavours from "blends in" to "stands out".

**A real editor.** Live preview, colour, gradients, blur, radius, shadows, typography, presets, undo
and redo. Save your own looks and reuse them across any family.

**Light on your battery.** Clocks ride the system tick that already fires. Battery widgets update on
events. Everything else shares one background task — and if nothing you've placed needs it, Prism
schedules no background work at all.

**Private by design.** No accounts, no analytics, no trackers, and no network access unless you
place a weather, finance, or crypto widget.

Requires Android 8.0 or newer.

---

### Known limitations at 1.0

Stated here because they will otherwise be discovered as bugs:

- Widget backup restores your widgets and their styles, but not their positions. New widgets get new
  ids from the launcher, and the launcher owns placement.
- Backdrop blur is real only when your wallpaper is one of Prism's. With your own wallpaper, glass
  families use a colour-matched fallback.
- The analog clocks have no second hand. Updating one every second would cost more battery than the
  hand is worth.
- CPU widgets report this app's processor use, not the whole system's. Android has restricted
  system-wide access since Oreo, and the widget label says so.
- Launcher settings in setups are advice you follow yourself. No launcher offers an API for it.
