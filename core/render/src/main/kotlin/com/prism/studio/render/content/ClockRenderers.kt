package com.prism.studio.render.content

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.prism.studio.model.Alignment
import com.prism.studio.model.ContentLayout
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType
import com.prism.studio.render.ContentRenderer
import com.prism.studio.render.DrawContext
import com.prism.studio.render.WidgetData
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * An analog clock that inherits its family's personality instead of being a generic dial.
 *
 * The dial is built from the family's own style rather than from clock convention: stroke weight
 * comes from the family's stroke, hand length from its padding, and tick treatment from its
 * `fontWeight` — a 300-weight family gets hairline ticks at the quarters only, an 800-weight family
 * gets full bars at every hour. That is why the Minimal Mono dial and the Brutalist Slab dial look
 * like they were drawn by the same hand as the rest of their families.
 *
 * There is no second hand. A second hand needs a redraw every second, which on a widget means
 * either a wildly inaccurate clock or an unacceptable battery cost, and a still second hand looks
 * broken. Removing it is the honest choice.
 */
class AnalogClockRenderer : ContentRenderer {

    override val type = WidgetType.AnalogClock

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val clock = data as? WidgetData.Clock ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        val radius = min(content.width(), content.height()) / 2f
        val cx = content.centerX()
        val cy = content.centerY()
        val heavy = style.fontWeight >= 600

        // Ticks. Heavy families mark every hour; light families mark only the quarters, because a
        // twelve-tick dial in a 300-weight family reads as busy at 2x2.
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeWidth = ctx.density.dp(if (heavy) 3f else 1.25f)
            color = muted
        }
        val step = if (heavy) 1 else 3
        for (hour in 0 until 12 step step) {
            val angle = Math.toRadians(hour * 30.0 - 90.0)
            val outer = radius * 0.94f
            val inner = radius * (if (hour % 3 == 0) 0.80f else 0.87f)
            tick.color = if (hour % 3 == 0) ink else muted
            canvas.drawLine(
                cx + cos(angle).toFloat() * inner, cy + sin(angle).toFloat() * inner,
                cx + cos(angle).toFloat() * outer, cy + sin(angle).toFloat() * outer,
                tick,
            )
        }

        val hourAngle = Math.toRadians((clock.now.hour % 12 + clock.now.minute / 60f) * 30.0 - 90.0)
        val minuteAngle = Math.toRadians(clock.now.minute * 6.0 - 90.0)

        hand(canvas, cx, cy, hourAngle, radius * 0.50f, ctx.density.dp(if (heavy) 5f else 3f), ink)
        hand(canvas, cx, cy, minuteAngle, radius * 0.78f, ctx.density.dp(if (heavy) 3.5f else 2f), ink)

        // The pin is the only accent-coloured element, which keeps the dial readable in families
        // whose accent is loud.
        canvas.drawCircle(cx, cy, ctx.density.dp(if (heavy) 4f else 2.5f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
    }

    private fun hand(canvas: Canvas, cx: Float, cy: Float, angle: Double, length: Float, width: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = width; strokeCap = Paint.Cap.ROUND; this.color = color
        }
        // A short counterweight past the pin. Clocks have them, and its absence is one of those
        // details people feel without being able to name.
        canvas.drawLine(
            cx - cos(angle).toFloat() * length * 0.16f, cy - sin(angle).toFloat() * length * 0.16f,
            cx + cos(angle).toFloat() * length, cy + sin(angle).toFloat() * length,
            paint,
        )
    }
}

/**
 * World clock. Rows of city + time, with the day offset shown only when it differs.
 *
 * "Tomorrow" and "Yesterday" markers appear as a small superscript rather than a second line,
 * because the whole point of this widget is scanning four rows at a glance and a two-line row
 * halves how many fit.
 */
class WorldClockRenderer : ContentRenderer {

    override val type = WidgetType.WorldClock

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val zones = data as? WidgetData.Zones ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)

        val rows = zones.entries.take(6)
        if (rows.isEmpty()) return
        val rowHeight = content.height() / rows.size
        val textSize = min(rowHeight * 0.46f, ctx.density.dp(17f)) * style.typeScale
        val labelPaint = Text.paint(ctx, style, textSize * 0.72f, muted, weight = 600)
        val timePaint = Text.paint(ctx, style, textSize, ink)
        val markPaint = Text.paint(ctx, style, textSize * 0.5f, muted, weight = 600)

        rows.forEachIndexed { i, zone ->
            val box = RectF(content.left, content.top + rowHeight * i, content.right, content.top + rowHeight * (i + 1))
            Text.drawOptical(canvas, zone.label, labelPaint, box, Alignment.Start)
            val time = "%02d:%02d".format(zone.time.hour, zone.time.minute)
            Text.drawOptical(canvas, time, timePaint, box, Alignment.End)
            if (zone.dayOffset != 0) {
                val mark = if (zone.dayOffset > 0) "+1" else "−1"
                val markBox = RectF(box.left, box.top, box.right - timePaint.measureText(time) - ctx.density.dp(6f), box.bottom)
                Text.drawOptical(canvas, mark, markPaint, markBox, Alignment.End)
            }
        }
    }
}

/**
 * Countdown. The unit changes as the target approaches, which is the only thing that makes this
 * widget useful at both six months out and six hours out.
 *
 * Above 60 days it shows months; above 2 days, days; above 2 hours, hours; then minutes. A widget
 * that says "4,392 hours" is technically correct and completely useless, and one that says "0 days"
 * on the morning of the event is worse.
 */
class CountdownRenderer : ContentRenderer {

    override val type = WidgetType.Countdown

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val cd = data as? WidgetData.Countdown ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)

        val minutes = kotlin.math.abs(cd.totalMinutes)
        val (value, unit) = when {
            minutes > 60L * 24 * 60 -> (minutes / (60L * 24 * 30)) to "months"
            minutes > 60L * 24 * 2 -> (minutes / (60L * 24)) to "days"
            minutes > 120L -> (minutes / 60L) to "hours"
            else -> minutes to "minutes"
        }
        val suffix = if (cd.elapsed) "since" else "until"
        val unitLabel = if (value == 1L) unit.dropLast(1) else unit

        val labelSize = ctx.density.dp(11f) * style.typeScale
        val gap = ctx.density.dp(style.spacingDp)
        val labelPaint = Text.paint(ctx, style, labelSize, muted, weight = 600).apply { letterSpacing = 0.1f }

        Text.drawAt(
            canvas,
            Text.ellipsize(labelPaint, cd.label.uppercase(Locale.getDefault()), content.width()),
            labelPaint, content, content.top + labelSize, style.alignment,
        )

        val heroBox = RectF(content.left, content.top + labelSize + gap, content.right, content.bottom - labelSize - gap)
        val heroPaint = Text.paint(ctx, style, heroBox.height(), ink)
        heroPaint.textSize = Text.fitted(heroPaint, value.toString(), heroBox.width(), heroBox.height() * 0.92f, 14f) * style.typeScale
        Text.drawOptical(canvas, value.toString(), heroPaint, heroBox, style.alignment)

        Text.drawAt(canvas, "$unitLabel $suffix", labelPaint, content, content.bottom, style.alignment)
    }
}

/**
 * Day card. The date, large, with the weekday and month around it.
 *
 * Weekday and month are set in the family's muted ink at the same optical size, so the card reads
 * as one object rather than three stacked labels — the arrangement most physical desk calendars
 * settle on, for the same reason.
 */
class DayCardRenderer : ContentRenderer {

    override val type = WidgetType.DayCard

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val cal = data as? WidgetData.Calendar ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val locale = Locale.getDefault()

        val small = ctx.density.dp(11f) * style.typeScale
        val gap = ctx.density.dp(style.spacingDp * 0.5f)
        val smallPaint = Text.paint(ctx, style, small, muted, weight = 600).apply { letterSpacing = 0.1f }

        val weekday = cal.today.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
        Text.drawAt(canvas, weekday, smallPaint, content, content.top + small, style.alignment)

        val numberBox = RectF(content.left, content.top + small + gap, content.right, content.bottom - small - gap)
        val numberPaint = Text.paint(ctx, style, numberBox.height(), ink)
        val day = cal.today.dayOfMonth.toString()
        numberPaint.textSize = Text.fitted(numberPaint, day, numberBox.width(), numberBox.height() * 0.95f, 16f) * style.typeScale
        Text.drawOptical(canvas, day, numberPaint, numberBox, style.alignment)

        val month = cal.today.month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
        Text.drawAt(canvas, month, smallPaint, content, content.bottom, style.alignment)
    }
}
