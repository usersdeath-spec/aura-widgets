# 1.2.1 — fixes from device testing

Four issues, all found by using the app rather than by any gate.

## 1. Content drew under the status bar and the notch

`enableEdgeToEdge()` makes the window full-screen, which is correct — but every screen must then
consume the status-bar inset itself. Removing the `Scaffold` from `CatalogScreen` removed the only
thing doing it, so "59 families · 708 designs" sat behind the clock. Both insets are now consumed.

## 2. Widgets rendered blank — two causes, one of them the real one

**The missing spec.** A widget added from the system picker never goes through `PinnedWidgets`, so
no `WidgetSpec` was saved against its id and the provider did `widgets[id] ?: return@forEach`. Now
falls back to a real design.

**The actual bug: the render never finished.** A `BroadcastReceiver` is alive only for the duration
of `onReceive`. `onUpdate` did `scope.launch { render() }` and returned immediately, leaving Android
free to tear the process down before the bitmap existed. The widget kept whatever `initialLayout`
drew — an empty `ImageView`. That is the black rectangle, and no amount of fallback design would
have fixed it.

`goAsync()` is the documented answer: it holds the receiver open until `finish()`, with a ~10 second
budget for work that has to fit in a frame anyway. Applied to `onUpdate`, `onAppWidgetOptionsChanged`
and `onDeleted` — the last of which was silently failing to clean up deleted widgets for the same
reason.

## 3a. Adding a widget opened a settings screen

Dropping a widget launched the style editor. The Glass Widgets teardown shows only **113 of their
466** providers declare `android:configure`, and always for something genuinely unset — an age
tracker needs a date, a contact widget needs a contact. A style editor is not that: the user chose
the style when they picked the widget.

`android:configure` removed from all five Canvas providers. Restyling stays on the launcher's own
reconfigure action.

## 3b. All 572 native widgets did nothing when tapped

Worse than the wrong destination: they had **no tap target at all**. `NativeWidgetProvider.onUpdate`
was empty, so nothing was ever bound.

They are now tappable, with the destination derived from the layout name — the generator already
encodes the kind in it (`nw_clock_hero_marble`, `nw_date_card_obsidian`), so a clock opens alarms and
a date opens the calendar without the provider looking anything up. That keeps the update path free
of Room and Hilt, which is what lets these widgets cost nothing to run.

## 3c. Tapping a Canvas widget opened the style editor

Correct criticism, and the competitors get this right. Their battery widget opens battery settings.
Ours opened "adjust colour and shape" — wrong twice: the user placed the widget for the information,
and having chosen from 572 designs they are not looking to tune corner radius on the one they picked.

`WidgetTapRouter` now routes by type: battery → battery settings, storage → storage settings,
CPU/RAM/system → device info, network → data usage, clocks → alarms, calendar → calendar, music →
the media app, weather → weather. Every destination is checked with `resolveActivity` first, because
none is guaranteed to exist and a PendingIntent to nothing feels broken.

Editing still lives on the host's own reconfigure action, which is where Android puts it.

## 4. Browsing was organised the wrong way round

The Glass Widgets teardown settled this. Their catalog groups **by what the widget does** — Battery,
Apps, Calendar, Clock, Compass, Contacts, Earbuds, Weather — never by appearance. Their provider
names confirm it: 11 `new_clock_digi`, 6 `battery_percentage_with_progress`, 6 `ear_buds_battery`.

That matches how someone arrives here. Nobody opens this app wanting "a Marble widget"; they want a
battery widget and then pick one they like. Family-first asked for an aesthetic before a function,
which is backwards — and with 59 families it is a long scroll before reaching what you came for.

**Widgets** is now the first tab and groups by type: every family's battery, side by side, with the
family name under each tile. **Styles** keeps family-first browsing plus the 28 setups, because once
someone has chosen a look they do want the matching set — which is what the seven-pillar rule is for
and the thing the competitors cannot offer.

## 5. Widget names were all "Aura Widgets"

The five Canvas providers shared one label, so different designs showed the same name on the home
screen. Now named by footprint: Aura Glass — Wide / Small / Tall / Large / Banner.

The 572 generated widgets already carry unique labels. One label covering many designs is inherent
to the bitmap path — one provider serves 708 designs — which is the cost of the expressive surfaces
that path exists for.


---

# 1.2.2 — why the tap fix did not work

The routing added in 1.2.1 was correct and still every widget opened the style editor. One line
explains it.

## Package visibility, Android 11+

`WidgetTapRouter` ended with:

```kotlin
candidates.firstOrNull { it.resolveActivity(context.packageManager) != null } ?: editorIntent(...)
```

Since API 30, **`resolveActivity` returns null for anything outside your own package** unless the
app declares what it intends to look for. Every candidate resolved to null, `firstOrNull` returned
null, and the fallback ran — every time, for every widget type. The routing logic was never
reached; it was being silently discarded.

Two changes:

**A narrow `<queries>` block** in the manifest naming the intents a widget routes to. Not
`QUERY_ALL_PACKAGES`, which Play requires justification for and which we do not need.

**Stop probing system Settings at all.** `android.provider.Settings` actions ship with the platform
on every certified device, so they are returned directly rather than checked. Only third-party
destinations — music, fitness, photos — are probed, and those are the ones `<queries>` covers.

## Clocks open Date & time, not alarms

Glass Widgets' analog clock opens **Date & time**, and that is the better call: someone tapping a
clock wants the clock, not to set an alarm, and Date & time is where format, timezone and dual-clock
live. Applied to both paths — the Canvas widgets and all 572 native ones.

## Destination map

| Widget | Opens |
|---|---|
| Any clock, world clock, countdown, elapsed | Date & time |
| Calendar, date, month, day-of-year | Calendar app, falling back to Date & time |
| Battery | Battery settings |
| Storage | Storage settings |
| CPU, memory, device info | About phone |
| Network | Data usage |
| Music | Your music app |
| Weather, sunrise | Weather |
| Notes, tasks, habits, quotes | Aura |
