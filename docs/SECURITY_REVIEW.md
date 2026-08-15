# Security & Privacy Review

Static review of the source and manifest. Not a penetration test — no build has run, so nothing
here is confirmed by execution.

## Findings and fixes

| # | Finding | Severity | Status |
|---|---|---|---|
| S1 | `BIND_NOTIFICATION_LISTENER_SERVICE` declared as `<uses-permission>` | Medium | Fixed |
| S2 | Exported configure activity trusting its extras | Medium | Documented + guard specified |
| S3 | Billing entitlement included in cloud backup | Medium | Fixed |
| S4 | No ProGuard keep rules for serializers | High | Fixed |
| S5 | Feed cache written non-atomically | Low | Fixed |
| S6 | Photo decode unbounded | Medium | Fixed |

**S1.** `BIND_NOTIFICATION_LISTENER_SERVICE` is a signature permission the *system* holds in order
to bind our service. Declaring it in `<uses-permission>` grants nothing and appears in Play's
data-safety review as a permission we cannot justify. Replaced with a declared
`NotificationListenerService` the user enables in Settings. The listener reads media sessions only —
`onNotificationPosted` is deliberately not overridden, so the app never sees notification text.

**S3.** Restoring a cached entitlement onto a new device would either wrongly unlock the app or
wrongly lock a paying customer out. Play is the source of truth on a fresh install, so
`license.preferences_pb` is excluded from both cloud backup and device transfer.

**S4.** Without keep rules, R8 full mode strips `kotlinx.serialization` serializers. `WidgetSpec` is
persisted as JSON, so this breaks every placed widget on upgrade — and only in release builds, which
is the worst place for a bug to first appear.

**S5.** The feed cache is now written to a temp file and renamed. A widget update landing mid-write
must never read half a JSON document.

**S6.** `WidgetBitmapSource` samples during decode via `inSampleSize`. A 12-megapixel photo decoded
at full size is ~48 MB; on a 2 GB device that is a kill.

## Permissions

Nothing is requested at install. Everything is requested when a widget needing it is placed, with
the reason and the consequence of declining shown.

| Permission | Why | If declined |
|---|---|---|
| `RECEIVE_BOOT_COMPLETED` | Redraw widgets once after reboot. Receiver disabled when no clock widget is placed. | n/a |
| `SET_WALLPAPER` | Apply a Prism wallpaper. | Wallpapers can be saved and set manually |
| `INTERNET` | Weather, finance, crypto only. No traffic unless one is placed. | Those widgets show cached or placeholder |
| `READ_CALENDAR` | Draw agenda and month widgets. Read on device. | Month grid without event marks |
| `ACCESS_COARSE_LOCATION` | Which forecast to show. City level. | Set a city by hand |
| `ACTIVITY_RECOGNITION` | Read the step counter the phone already keeps. | Step widget shows a placeholder |
| Notification access (Settings) | Read the media session for Now Playing. | Music widget stays empty |

`ACCESS_FINE_LOCATION` is deliberately absent: weather does not need street-level precision, and
asking for it would be the single least defensible line in the data-safety form.

## Data handling

No accounts, no analytics SDK, no advertising SDK, no crash reporter, no remote config. Everything
the user configures is in the app's private storage. Colour matching, palette extraction, sunrise
calculation, and blur all run on device.

Three outbound requests exist at all, each triggered only by a placed widget: weather (coarse
coordinates), market data (ticker symbols), quotes. None carries an account, device, or advertising
identifier.

## Exported components

| Component | Exported | Why it must be, and what protects it |
|---|---|---|
| `MainActivity` | Yes | Launcher entry |
| `WidgetConfigureActivity` | Yes | The widget host launches it. Reads one extra, validates the id against `AppWidgetManager` and finishes if it is invalid or not ours |
| `PrismWidgetProvider`, `PrismCompactProvider` | Yes | Required for `APPWIDGET_UPDATE`. Handle only the framework's own actions |
| `BootReceiver` | Yes | `BOOT_COMPLETED` is system-sent. Disabled entirely when no minute-cadence widget is placed |
| `MediaSessionListener` | No | `exported="false"`, bound by the system under a signature permission |

## Input validation

Widget options are user-supplied strings persisted as JSON, so every read is defensive: timezone ids
via `runCatching { ZoneId.of(...) }`, countdown targets via `runCatching { LocalDateTime.parse(...) }`,
photo URIs through a `runCatching` decode that returns null on revoked access. A malformed option
degrades one widget to its placeholder; it never throws into the host process, which on some
launchers would take the home screen down with it.

## Not yet verified

Everything above is static. Still required before release: `lint --check Security` on a real build,
a Play pre-launch report, and a network capture confirming that a device with only local widgets
placed makes no outbound requests at all. That last one is a store-listing claim and should be
demonstrated, not asserted.
