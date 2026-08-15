package com.prism.studio.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType
import com.prism.studio.render.WidgetData
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the data half of a render, plus a fingerprint used as part of the bitmap cache key.
 *
 * The fingerprint is what stops us re-rasterising: if the battery is still 62% and the minute has
 * not rolled over, the fingerprint is unchanged, the cache hits, and the update costs a single
 * binder call with a bitmap we already had.
 */
@Singleton
class WidgetDataSource @Inject constructor() {

    data class Snapshot(val value: WidgetData, val fingerprint: String)

    fun dataFor(widget: ResolvedWidget, context: Context): Snapshot = when (widget.variant.type) {
        WidgetType.DigitalClock, WidgetType.AnalogClock -> clock(context, widget)

        WidgetType.Battery -> battery(context)

        WidgetType.Storage -> storage()

        // Network-backed types are served from the cache written by RefreshWorker; the provider
        // itself never performs I/O, which is why widget updates never block the main thread.
        WidgetType.Weather, WidgetType.Finance, WidgetType.Crypto, WidgetType.Quote ->
            CachedFeed.read(context, widget.variant.type)
                ?: Snapshot(
                    WidgetData.Placeholder("Tap to set up", "Set up"),
                    "placeholder:${widget.variant.type}",
                )

        WidgetType.Countdown -> countdown(widget)

        WidgetType.WorldClock -> zones(widget)

        WidgetType.SystemInfo -> systemInfo(context)

        WidgetType.Cpu -> cpu()

        WidgetType.Ram -> ram(context)

        WidgetType.Network -> network(context)

        WidgetType.SunriseSunset -> SunTimes.forToday(context, widget)

        WidgetType.Agenda -> CalendarReader.agenda(context)

        WidgetType.MonthCalendar, WidgetType.DayCard -> {
            val today = java.time.LocalDate.now()
            Snapshot(WidgetData.Calendar(today = today), "cal:$today")
        }

        // Notes, tasks, and habits are the user's own content, read straight from Room by the
        // caller and handed to us — the data source never opens the database on the update path.
        WidgetType.Notes, WidgetType.Todo, WidgetType.HabitTracker, WidgetType.Steps,
        WidgetType.MusicPlayer, WidgetType.Photo, WidgetType.Health ->
            LocalContent.read(context, widget)

        WidgetType.DigitalClock, WidgetType.AnalogClock -> clock(context, widget)
    }

    /**
     * Countdowns are minute-precision by design.
     *
     * The target is stored as an ISO string in the widget's options rather than as a timestamp, so
     * "my birthday" survives a timezone change instead of drifting by hours — the single most
     * common bug in countdown widgets.
     */
    private fun countdown(widget: ResolvedWidget): Snapshot {
        val target = widget.options["target"]?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: return Snapshot(WidgetData.Placeholder("Set a date", "Choose"), "countdown:unset")
        val now = LocalDateTime.now()
        val label = widget.options["label"].orEmpty().ifBlank { "Countdown" }
        val elapsed = target.isBefore(now)
        return Snapshot(
            WidgetData.Countdown(label, target, now, elapsed),
            "countdown:${target}:${now.hour}:${now.minute}",
        )
    }

    private fun zones(widget: ResolvedWidget): Snapshot {
        val ids = widget.options["zones"]?.split(',')?.filter { it.isNotBlank() }
            ?: listOf("America/New_York", "Europe/London", "Asia/Tokyo")
        val here = java.time.LocalDate.now()
        val entries = ids.mapNotNull { id ->
            runCatching {
                val zoned = java.time.ZonedDateTime.now(java.time.ZoneId.of(id))
                WidgetData.Zones.Zone(
                    label = id.substringAfterLast('/').replace('_', ' '),
                    time = zoned.toLocalTime(),
                    dayOffset = zoned.toLocalDate().compareTo(here),
                )
            }.getOrNull()
        }
        return Snapshot(
            WidgetData.Zones(entries),
            "zones:" + entries.joinToString(",") { "${it.label}${it.time.hour}${it.time.minute}" },
        )
    }

    private fun systemInfo(context: Context): Snapshot {
        val rows = buildList {
            add(WidgetData.System.Row("Model", Build.MODEL))
            add(WidgetData.System.Row("Android", Build.VERSION.RELEASE))
            val storage = storage()
            (storage.value as? WidgetData.Gauge)?.let {
                add(WidgetData.System.Row("Storage", it.detail ?: it.primary, it.fraction))
            }
            (ram(context).value as? WidgetData.Gauge)?.let {
                add(WidgetData.System.Row("Memory", it.primary, it.fraction))
            }
            add(WidgetData.System.Row("Uptime", uptimeLabel()))
        }
        return Snapshot(WidgetData.System(rows), "system:" + rows.joinToString { it.value })
    }

    /**
     * Memory from ActivityManager rather than /proc.
     *
     * /proc/meminfo gives a more precise number and is unreadable on recent Android without a
     * permission we will not request. ActivityManager's figure is what the platform itself reports,
     * which is also the number the user will see in Settings — matching Settings matters more than
     * being technically closer to the kernel.
     */
    private fun ram(context: Context): Snapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val info = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val used = info.totalMem - info.availMem
        val fraction = if (info.totalMem > 0) used.toFloat() / info.totalMem else 0f
        return Snapshot(
            WidgetData.Gauge(fraction, "%.0f%%".format(fraction * 100), "Memory", "%.1f GB free".format(info.availMem / 1e9)),
            "ram:${(fraction * 100).toInt()}",
        )
    }

    /**
     * CPU load without /proc/stat, which Android has restricted since Oreo.
     *
     * We sample elapsed CPU time for our own process against wall time. That is not system-wide
     * load, and the widget label says "App CPU" rather than "CPU" so it is not claiming to be.
     * Shipping a plausible-looking number that is actually wrong would be worse than shipping a
     * narrower number that is right.
     */
    private fun cpu(): Snapshot {
        val cpuMs = android.os.Process.getElapsedCpuTime()
        val wallMs = android.os.SystemClock.elapsedRealtime()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val fraction = (cpuMs.toFloat() / wallMs.coerceAtLeast(1) / cores).coerceIn(0f, 1f)
        return Snapshot(
            WidgetData.Gauge(fraction, "%.0f%%".format(fraction * 100), "App CPU"),
            "cpu:${(fraction * 100).toInt()}",
        )
    }

    private fun network(context: Context): Snapshot {
        val rx = android.net.TrafficStats.getTotalRxBytes()
        val tx = android.net.TrafficStats.getTotalTxBytes()
        val total = (rx + tx).coerceAtLeast(0)
        return Snapshot(
            WidgetData.Gauge(0f, "%.1f GB".format(total / 1e9), "Data", "since boot"),
            "net:${total / 100_000_000}",
        )
    }

    private fun uptimeLabel(): String {
        val minutes = android.os.SystemClock.elapsedRealtime() / 60_000
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    private fun clock(context: Context, widget: ResolvedWidget): Snapshot {
        val zone = widget.options["timezone"]?.let { java.time.ZoneId.of(it) } ?: java.time.ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val is24 = android.text.format.DateFormat.is24HourFormat(context)
        return Snapshot(
            WidgetData.Clock(now, is24, widget.options["zoneLabel"]),
            // Minute precision: seconds are deliberately not part of the key.
            "clock:${now.hour}:${now.minute}:$is24:${zone.id}",
        )
    }

    private fun battery(context: Context): Snapshot {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val plugged = (status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val percent = if (level >= 0) level * 100 / scale else 0

        val state = when {
            plugged -> WidgetData.Gauge.State.Charging
            percent <= 5 -> WidgetData.Gauge.State.Critical
            percent <= 20 -> WidgetData.Gauge.State.Low
            else -> WidgetData.Gauge.State.Normal
        }
        return Snapshot(
            WidgetData.Gauge(
                fraction = percent / 100f,
                primary = "$percent%",
                label = "Battery",
                detail = if (plugged) "Charging" else null,
                state = state,
            ),
            "battery:$percent:$plugged",
        )
    }

    private fun storage(): Snapshot {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val usedFraction = if (total > 0) (total - free).toFloat() / total else 0f
        val freeGb = free / 1_000_000_000.0
        return Snapshot(
            WidgetData.Gauge(
                fraction = usedFraction,
                primary = "%.0f%%".format(usedFraction * 100),
                label = "Storage",
                detail = "%.1f GB free".format(freeGb),
            ),
            // Bucketed to 1% so a few megabytes of churn does not force a redraw.
            "storage:${(usedFraction * 100).toInt()}",
        )
    }
}
