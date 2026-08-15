package com.prism.studio.render

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The data half of a render. Renderers are pure functions of (ResolvedWidget, WidgetData, size),
 * which means the exact same code path draws the live home-screen widget and the in-app preview.
 *
 * One source of truth for pixels is the single most valuable property of this architecture: a
 * preview can never lie about what the user is about to place on their home screen.
 */
sealed interface WidgetData {
    data class Clock(
        val now: LocalDateTime,
        val is24Hour: Boolean,
        val zoneLabel: String? = null,
    ) : WidgetData

    data class Calendar(
        val today: LocalDate,
        val monthAnchor: LocalDate = today.withDayOfMonth(1),
        val markedDays: Set<Int> = emptySet(),
        val events: List<Event> = emptyList(),
    ) : WidgetData {
        data class Event(val at: LocalDateTime, val title: String, val colorArgb: Long?)
    }

    data class Gauge(
        /** 0f..1f. */
        val fraction: Float,
        val primary: String,
        val label: String? = null,
        val detail: String? = null,
        /** Optional recent history for Chart layouts. Normalised 0..1. */
        val history: List<Float> = emptyList(),
        val state: State = State.Normal,
    ) : WidgetData {
        enum class State { Normal, Charging, Low, Critical }
    }

    data class Weather(
        val tempC: Float,
        val feelsLikeC: Float,
        val condition: Condition,
        val place: String,
        val highC: Float,
        val lowC: Float,
        val hourly: List<Pair<Int, Float>> = emptyList(),
    ) : WidgetData {
        enum class Condition { Clear, PartlyCloudy, Cloudy, Rain, Storm, Snow, Fog, Wind }
    }

    data class TextRows(val title: String?, val rows: List<Row>) : WidgetData {
        data class Row(val text: String, val done: Boolean = false, val accent: Boolean = false)
    }

    data class Quote(val text: String, val attribution: String?) : WidgetData

    data class Photo(val bitmapKey: String, val caption: String?) : WidgetData

    data class Series(
        val label: String,
        val value: String,
        val changePercent: Float,
        val points: List<Float>,
    ) : WidgetData

    data class Countdown(
        val label: String,
        val target: LocalDateTime,
        val now: LocalDateTime,
        /** True once the target has passed — the renderer switches to elapsed, not negative. */
        val elapsed: Boolean = false,
    ) : WidgetData {
        val totalMinutes: Long get() = java.time.Duration.between(now, target).toMinutes()
    }

    data class Sun(
        val sunrise: java.time.LocalTime,
        val sunset: java.time.LocalTime,
        val now: java.time.LocalTime,
        val place: String?,
    ) : WidgetData {
        /** 0f at sunrise, 1f at sunset. Negative or >1 before dawn and after dusk. */
        val dayProgress: Float
            get() {
                val start = sunrise.toSecondOfDay().toFloat()
                val end = sunset.toSecondOfDay().toFloat()
                if (end <= start) return 0f
                return (now.toSecondOfDay() - start) / (end - start)
            }
    }

    data class Zones(val entries: List<Zone>) : WidgetData {
        data class Zone(val label: String, val time: java.time.LocalTime, val dayOffset: Int)
    }

    data class System(val rows: List<Row>) : WidgetData {
        data class Row(val label: String, val value: String, val fraction: Float? = null)
    }

    data class Media(
        val title: String,
        val artist: String?,
        val playing: Boolean,
        val artworkKey: String?,
        /** 0f..1f, or null when the session reports no duration. */
        val progress: Float?,
    ) : WidgetData

    data class Habits(
        val title: String,
        /** One entry per day, most recent last. Length drives the grid width. */
        val days: List<Boolean>,
        val streak: Int,
    ) : WidgetData

    /** Shown when a permission is missing or a fetch failed. Never an error string on the home screen. */
    data class Placeholder(val prompt: String, val actionLabel: String?) : WidgetData
}
