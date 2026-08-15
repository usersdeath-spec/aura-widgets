package com.prism.studio.widget

import android.content.Context
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.render.WidgetData
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A rasterised widget is, to a screen reader, a picture of nothing.
 *
 * Every bitmap therefore ships with a spoken description built from the same data that drew it.
 * This is not optional polish: without it, an image-based widget architecture is inaccessible by
 * construction, and TalkBack users are a meaningful share of a customisation app's audience.
 */
object Accessibility {

    private val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

    fun describe(widget: ResolvedWidget, data: WidgetData, context: Context): String = when (data) {
        is WidgetData.Clock -> buildString {
            append(data.now.format(time))
            data.zoneLabel?.let { append(", ").append(it) }
            append(". ").append(data.now.format(date))
        }

        is WidgetData.Gauge -> buildString {
            data.label?.let { append(it).append(": ") }
            append(data.primary)
            data.detail?.let { append(", ").append(it) }
        }

        is WidgetData.Calendar -> data.today.format(date)

        is WidgetData.Weather ->
            "${data.place}, ${data.tempC.toInt()} degrees, ${data.condition.name}"

        is WidgetData.TextRows -> buildString {
            data.title?.let { append(it).append(". ") }
            append(data.rows.joinToString(". ") { it.text })
        }

        is WidgetData.Quote -> data.text + (data.attribution?.let { ", $it" } ?: "")

        // The six types added after this file was first written. Each was a missing branch in an
        // exhaustive `when` — a compile error, and had it compiled, six widget types that TalkBack
        // would have announced as "image".

        is WidgetData.Countdown -> buildString {
            append(data.label).append(": ")
            val minutes = kotlin.math.abs(data.totalMinutes)
            val days = minutes / (60 * 24)
            when {
                days > 1 -> append("$days days")
                minutes > 120 -> append("${minutes / 60} hours")
                else -> append("$minutes minutes")
            }
            append(if (data.elapsed) " since" else " remaining")
        }

        is WidgetData.Sun -> buildString {
            data.place?.let { append(it).append(". ") }
            append("Sunrise ").append(data.sunrise.format(time))
            append(", sunset ").append(data.sunset.format(time))
        }

        is WidgetData.Zones -> data.entries.joinToString(". ") { zone ->
            // Spelled out rather than read as digits: "London, 4 51 PM" is what a screen reader
            // makes of "16:51", which is worse than useless.
            val suffix = when {
                zone.dayOffset > 0 -> ", tomorrow"
                zone.dayOffset < 0 -> ", yesterday"
                else -> ""
            }
            "${zone.label}, ${zone.time.format(time)}$suffix"
        }

        is WidgetData.System -> data.rows.joinToString(". ") { "${it.label}: ${it.value}" }

        is WidgetData.Media -> buildString {
            append(if (data.playing) "Playing: " else "Paused: ")
            append(data.title)
            data.artist?.let { append(" by ").append(it) }
        }

        is WidgetData.Habits ->
            "${data.title}. ${data.streak} day streak"
        is WidgetData.Series -> "${data.label}, ${data.value}, ${data.changePercent} percent"
        is WidgetData.Photo -> data.caption ?: "Photo widget"
        is WidgetData.Placeholder -> data.prompt
    }
}
