package com.prism.studio.widget

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType
import com.prism.studio.render.WidgetData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Network-backed data, read from disk on the update path.
 *
 * `RefreshWorker` writes; the provider only ever reads. That separation is what lets
 * `PrismWidgetProvider` promise no I/O-blocking on updates — a file read of a few hundred bytes is
 * not the same class of operation as an HTTP request, and the cache is small by construction
 * (one file per feed type, overwritten in place).
 *
 * Stale data is served rather than hidden. A weather widget showing a two-hour-old temperature is
 * useful; one showing a spinner because the phone is on a train is not.
 */
object CachedFeed {

    private val json = Json { ignoreUnknownKeys = true }

    fun read(context: Context, type: WidgetType): WidgetDataSource.Snapshot? {
        val file = fileFor(context, type)
        if (!file.exists()) return null
        val raw = runCatching { file.readText() }.getOrNull() ?: return null

        return runCatching {
            when (type) {
                WidgetType.Weather -> {
                    val w = json.decodeFromString<CachedWeather>(raw)
                    WidgetDataSource.Snapshot(
                        WidgetData.Weather(
                            w.tempC, w.feelsLikeC,
                            WidgetData.Weather.Condition.valueOf(w.condition),
                            w.place, w.highC, w.lowC,
                            w.hourly.map { it.hour to it.tempC },
                        ),
                        "weather:${w.place}:${w.tempC.toInt()}:${w.condition}",
                    )
                }
                WidgetType.Finance, WidgetType.Crypto -> {
                    val s = json.decodeFromString<CachedSeries>(raw)
                    WidgetDataSource.Snapshot(
                        WidgetData.Series(s.label, s.value, s.changePercent, s.points),
                        "series:${s.label}:${s.value}",
                    )
                }
                WidgetType.Quote -> {
                    val q = json.decodeFromString<CachedQuote>(raw)
                    WidgetDataSource.Snapshot(
                        WidgetData.Quote(q.text, q.attribution),
                        "quote:${q.text.hashCode()}",
                    )
                }
                else -> null
            }
        }.getOrNull()
    }

    fun write(context: Context, type: WidgetType, contents: String) {
        // Write-then-rename: a widget update that lands mid-write must never see half a file.
        val target = fileFor(context, type)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(contents)
        tmp.renameTo(target)
    }

    private fun fileFor(context: Context, type: WidgetType) =
        File(context.filesDir, "feed_${type.name.lowercase()}.json").also { it.parentFile?.mkdirs() }

    @kotlinx.serialization.Serializable
    data class CachedWeather(
        val place: String, val tempC: Float, val feelsLikeC: Float, val condition: String,
        val highC: Float, val lowC: Float, val hourly: List<Hourly> = emptyList(), val fetchedAt: Long = 0,
    ) {
        @kotlinx.serialization.Serializable data class Hourly(val hour: Int, val tempC: Float)
    }

    @kotlinx.serialization.Serializable
    data class CachedSeries(
        val label: String, val value: String, val changePercent: Float,
        val points: List<Float> = emptyList(), val fetchedAt: Long = 0,
    )

    @kotlinx.serialization.Serializable
    data class CachedQuote(val text: String, val attribution: String? = null)
}

/**
 * Sunrise and sunset, computed locally.
 *
 * The NOAA sunrise/sunset equation. Measured against published almanac values it lands within
 * about four minutes at temperate latitudes (see `tools/verify_algorithms.py`) — the low-precision
 * form omits the equation of time's higher-order terms. Four minutes is well inside what a widget
 * needs, and it costs no network call, no API key, and no third party
 * learning where the user is. Coordinates come from the coarse location the user already granted,
 * or from a city they set by hand.
 *
 * Above the polar circles the sun may not rise or set at all. Rather than returning nonsense times,
 * the arc collapses to the appropriate extreme and the renderer shows a full or empty day.
 */
object SunTimes {

    fun forToday(context: Context, widget: ResolvedWidget): WidgetDataSource.Snapshot {
        val lat = widget.options["lat"]?.toDoubleOrNull()
        val lon = widget.options["lon"]?.toDoubleOrNull()
        val place = widget.options["place"]

        if (lat == null || lon == null) {
            return WidgetDataSource.Snapshot(
                WidgetData.Placeholder("Set a location", "Choose"),
                "sun:unset",
            )
        }

        val today = LocalDate.now()
        val (sunrise, sunset) = compute(today, lat, lon)
        return WidgetDataSource.Snapshot(
            WidgetData.Sun(sunrise, sunset, LocalTime.now(), place),
            "sun:$today:${sunrise.hour}:${sunrise.minute}",
        )
    }

    /** Returns local sunrise and sunset. Polar day yields 00:00–23:59; polar night yields both at noon. */
    fun compute(date: LocalDate, latitude: Double, longitude: Double): Pair<LocalTime, LocalTime> {
        val dayOfYear = date.dayOfYear
        val zenith = Math.toRadians(90.833)  // includes atmospheric refraction and the sun's radius
        val latRad = Math.toRadians(latitude)

        fun event(rising: Boolean): LocalTime? {
            val approx = dayOfYear + ((if (rising) 6.0 else 18.0) - longitude / 15.0) / 24.0
            val meanAnomaly = 0.9856 * approx - 3.289
            var trueLong = meanAnomaly + 1.916 * sin(Math.toRadians(meanAnomaly)) +
                0.020 * sin(Math.toRadians(2 * meanAnomaly)) + 282.634
            trueLong = (trueLong + 360.0) % 360.0

            var rightAsc = Math.toDegrees(kotlin.math.atan(0.91764 * tan(Math.toRadians(trueLong))))
            rightAsc = (rightAsc + 360.0) % 360.0
            rightAsc += (Math.floor(trueLong / 90.0) - Math.floor(rightAsc / 90.0)) * 90.0
            rightAsc /= 15.0

            val sinDec = 0.39782 * sin(Math.toRadians(trueLong))
            val cosDec = cos(asin(sinDec))
            val cosHour = (cos(zenith) - sinDec * sin(latRad)) / (cosDec * cos(latRad))
            if (cosHour !in -1.0..1.0) return null   // sun never rises or never sets here today

            val hour = (if (rising) 360.0 - Math.toDegrees(acos(cosHour)) else Math.toDegrees(acos(cosHour))) / 15.0
            val meanTime = hour + rightAsc - 0.06571 * approx - 6.622
            val utc = ((meanTime - longitude / 15.0) % 24.0 + 24.0) % 24.0

            val offsetSeconds = ZoneId.systemDefault().rules
                .getOffset(Instant.now()).totalSeconds / 3600.0
            val local = ((utc + offsetSeconds) % 24.0 + 24.0) % 24.0
            return LocalTime.of(local.toInt(), ((local % 1.0) * 60).toInt())
        }

        val sunrise = event(rising = true)
        val sunset = event(rising = false)
        return when {
            sunrise != null && sunset != null -> sunrise to sunset
            latitude >= 0 && date.monthValue in 4..8 -> LocalTime.MIDNIGHT to LocalTime.of(23, 59)
            else -> LocalTime.NOON to LocalTime.NOON
        }
    }
}

/**
 * Calendar events for the agenda widget.
 *
 * Queries the next 24 hours through `CalendarContract.Instances`, which expands recurring events —
 * querying `Events` directly is the classic bug that makes a weekly standup appear once and then
 * vanish forever.
 *
 * Returns an empty list rather than throwing when the permission is absent, so a widget placed
 * before the grant simply shows "Nothing scheduled" instead of crashing the host.
 */
object CalendarReader {

    private val PROJECTION = arrayOf(
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.DISPLAY_COLOR,
        CalendarContract.Instances.ALL_DAY,
    )

    fun agenda(context: Context, hours: Long = 24): WidgetDataSource.Snapshot {
        val today = LocalDate.now()
        if (!hasPermission(context)) {
            return WidgetDataSource.Snapshot(
                WidgetData.Calendar(today = today),
                "agenda:nopermission:$today",
            )
        }

        val start = System.currentTimeMillis()
        val end = start + hours * 3_600_000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .let { ContentUris.appendId(it, start); ContentUris.appendId(it, end); it.build() }

        val events = mutableListOf<WidgetData.Calendar.Event>()
        runCatching {
            context.contentResolver.query(uri, PROJECTION, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { cursor ->
                    while (cursor.moveToNext() && events.size < 12) {
                        val begin = cursor.getLong(0)
                        val title = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: "(No title)"
                        val color = cursor.getInt(2).toLong() and 0xFFFFFFFFL
                        events += WidgetData.Calendar.Event(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), ZoneId.systemDefault()),
                            title,
                            color.takeIf { it != 0L },
                        )
                    }
                }
        }

        return WidgetDataSource.Snapshot(
            WidgetData.Calendar(
                today = today,
                markedDays = events.map { it.at.dayOfMonth }.toSet(),
                events = events,
            ),
            "agenda:$today:" + events.joinToString { "${it.at.hour}${it.title.hashCode()}" },
        )
    }

    private fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}

/**
 * User-owned content: notes, tasks, habits, steps, the current media session, and chosen photos.
 *
 * All of it is either already in Room or supplied by a system service, so nothing here reaches the
 * network. The caller passes a pre-loaded [Content] snapshot on the update path; this object only
 * shapes it into [WidgetData] and produces the honest placeholder when a widget has not been
 * configured yet.
 */
object LocalContent {

    /** Loaded once per update batch by the provider, never per widget. */
    data class Content(
        val notes: Map<String, Pair<String, List<String>>> = emptyMap(),
        val tasks: Map<String, Pair<String, List<Pair<String, Boolean>>>> = emptyMap(),
        val habits: Map<String, Triple<String, List<Boolean>, Int>> = emptyMap(),
        val steps: Int? = null,
        val stepGoal: Int = 10_000,
        val media: WidgetData.Media? = null,
    )

    @Volatile
    private var current: Content = Content()

    fun publish(content: Content) { current = content }

    fun read(context: Context, widget: ResolvedWidget): WidgetDataSource.Snapshot {
        val id = widget.options["contentId"].orEmpty()
        return when (widget.variant.type) {
            WidgetType.Notes -> current.notes[id]?.let { (title, lines) ->
                WidgetDataSource.Snapshot(
                    WidgetData.TextRows(title, lines.map { WidgetData.TextRows.Row(it) }),
                    "note:$id:${lines.hashCode()}",
                )
            } ?: unset("Tap to write a note")

            WidgetType.Todo -> current.tasks[id]?.let { (title, rows) ->
                WidgetDataSource.Snapshot(
                    WidgetData.TextRows(title, rows.map { WidgetData.TextRows.Row(it.first, it.second) }),
                    "todo:$id:${rows.hashCode()}",
                )
            } ?: unset("Tap to add tasks")

            WidgetType.HabitTracker -> current.habits[id]?.let { (title, days, streak) ->
                WidgetDataSource.Snapshot(WidgetData.Habits(title, days, streak), "habit:$id:$streak:${days.size}")
            } ?: unset("Tap to start a habit")

            WidgetType.Steps, WidgetType.Health -> current.steps?.let { steps ->
                WidgetDataSource.Snapshot(
                    WidgetData.Gauge(
                        (steps.toFloat() / current.stepGoal).coerceIn(0f, 1f),
                        "%,d".format(steps), "Steps", "of ${"%,d".format(current.stepGoal)}",
                    ),
                    "steps:$steps",
                )
            } ?: unset("Allow activity access")

            WidgetType.MusicPlayer -> current.media?.let {
                WidgetDataSource.Snapshot(it, "media:${it.title}:${it.playing}:${(it.progress ?: 0f) * 20}")
            } ?: unset("Nothing playing")

            WidgetType.Photo -> widget.options["photoUri"]?.let {
                WidgetDataSource.Snapshot(WidgetData.Photo(it, widget.options["caption"]), "photo:$it")
            } ?: unset("Tap to choose a photo")

            else -> unset("Tap to set up")
        }
    }

    private fun unset(prompt: String) =
        WidgetDataSource.Snapshot(WidgetData.Placeholder(prompt, "Set up"), "unset:${prompt.hashCode()}")
}
