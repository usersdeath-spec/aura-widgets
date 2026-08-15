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
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Weather condition glyphs, drawn as paths rather than shipped as a font or a drawable set.
 *
 * Two reasons this is worth the code. First, the glyph inherits the family's stroke weight and ink,
 * so a Brutalist Slab cloud is heavy and a Japanese Zen cloud is a hairline — an icon font would
 * make every family's weather widget look like the same widget wearing a different background.
 * Second, eight vector glyphs cost nothing to ship and scale perfectly to any widget size.
 */
internal object WeatherGlyph {

    fun draw(canvas: Canvas, box: RectF, condition: WidgetData.Weather.Condition, ctx: DrawContext, color: Int, weight: Int) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ctx.density.dp(if (weight >= 600) 3f else 1.8f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val r = min(box.width(), box.height()) / 2f
        val cx = box.centerX()
        val cy = box.centerY()

        when (condition) {
            WidgetData.Weather.Condition.Clear -> {
                canvas.drawCircle(cx, cy, r * 0.42f, stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    canvas.drawLine(
                        cx + cos(a).toFloat() * r * 0.62f, cy + sin(a).toFloat() * r * 0.62f,
                        cx + cos(a).toFloat() * r * 0.88f, cy + sin(a).toFloat() * r * 0.88f,
                        stroke,
                    )
                }
            }
            WidgetData.Weather.Condition.PartlyCloudy -> {
                canvas.drawCircle(cx + r * 0.28f, cy - r * 0.3f, r * 0.32f, stroke)
                cloud(canvas, RectF(box.left, cy - r * 0.1f, box.right, box.bottom), stroke)
            }
            WidgetData.Weather.Condition.Cloudy, WidgetData.Weather.Condition.Fog ->
                cloud(canvas, box, stroke)
            WidgetData.Weather.Condition.Rain -> {
                cloud(canvas, RectF(box.left, box.top, box.right, cy + r * 0.2f), stroke)
                for (i in 0 until 3) {
                    val x = box.left + box.width() * (0.3f + i * 0.2f)
                    canvas.drawLine(x, cy + r * 0.45f, x - r * 0.12f, cy + r * 0.85f, stroke)
                }
            }
            WidgetData.Weather.Condition.Storm -> {
                cloud(canvas, RectF(box.left, box.top, box.right, cy + r * 0.2f), stroke)
                val bolt = Path().apply {
                    moveTo(cx + r * 0.1f, cy + r * 0.3f)
                    lineTo(cx - r * 0.15f, cy + r * 0.62f)
                    lineTo(cx + r * 0.02f, cy + r * 0.62f)
                    lineTo(cx - r * 0.12f, cy + r * 0.95f)
                }
                canvas.drawPath(bolt, stroke)
            }
            WidgetData.Weather.Condition.Snow -> {
                cloud(canvas, RectF(box.left, box.top, box.right, cy + r * 0.2f), stroke)
                for (i in 0 until 3) {
                    canvas.drawCircle(box.left + box.width() * (0.32f + i * 0.18f), cy + r * 0.66f, ctx.density.dp(2f), fill)
                }
            }
            WidgetData.Weather.Condition.Wind -> {
                for (i in 0 until 3) {
                    val y = cy + (i - 1) * r * 0.34f
                    canvas.drawLine(box.left + r * 0.2f, y, box.right - r * (0.2f + i * 0.18f), y, stroke)
                }
            }
        }
    }

    private fun cloud(canvas: Canvas, box: RectF, paint: Paint) {
        val r = min(box.width(), box.height()) / 2f
        val cy = box.centerY()
        val path = Path().apply {
            addCircle(box.centerX() - r * 0.35f, cy, r * 0.36f, Path.Direction.CW)
            addCircle(box.centerX() + r * 0.28f, cy - r * 0.06f, r * 0.30f, Path.Direction.CW)
            addRoundRect(
                RectF(box.centerX() - r * 0.7f, cy - r * 0.1f, box.centerX() + r * 0.62f, cy + r * 0.36f),
                r * 0.3f, r * 0.3f, Path.Direction.CW,
            )
        }
        canvas.drawPath(path, paint)
    }
}

/**
 * Weather.
 *
 * Temperature is the hero and everything else is subordinate, because that is the only number
 * anyone reads from a weather widget at a glance. High/low and the place name sit in muted ink; the
 * condition glyph sits opposite the temperature so the two balance rather than stack.
 *
 * The hourly strip appears only in [ContentLayout.Chart] and only when there is room for at least
 * five points — four points is a chart that misleads.
 */
class WeatherRenderer : ContentRenderer {

    override val type = WidgetType.Weather

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val w = data as? WidgetData.Weather ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        val labelSize = ctx.density.dp(11f) * style.typeScale
        val labelPaint = Text.paint(ctx, style, labelSize, muted, weight = 600)
        val glyphSize = min(content.height() * 0.55f, content.width() * 0.3f)

        when (widget.variant.layout) {
            ContentLayout.Chart -> {
                val top = RectF(content.left, content.top, content.right, content.centerY())
                drawSplit(canvas, top, w, widget, ctx, ink, muted, labelPaint, glyphSize * 0.8f)
                if (w.hourly.size >= 5) {
                    drawHourly(canvas, RectF(content.left, content.centerY(), content.right, content.bottom), w, ctx, accent, muted, style)
                }
            }
            else -> drawSplit(canvas, content, w, widget, ctx, ink, muted, labelPaint, glyphSize)
        }
    }

    private fun drawSplit(
        canvas: Canvas, box: RectF, w: WidgetData.Weather, widget: ResolvedWidget,
        ctx: DrawContext, ink: Int, muted: Int, labelPaint: Paint, glyphSize: Float,
    ) {
        val style = widget.style
        val glyphBox = RectF(box.right - glyphSize, box.centerY() - glyphSize / 2f, box.right, box.centerY() + glyphSize / 2f)
        WeatherGlyph.draw(canvas, glyphBox, w.condition, ctx, ink, style.fontWeight)

        val textArea = RectF(box.left, box.top, glyphBox.left - ctx.density.dp(8f), box.bottom)
        val gap = ctx.density.dp(style.spacingDp * 0.5f)

        Text.drawAt(
            canvas,
            Text.ellipsize(labelPaint, w.place, textArea.width()),
            labelPaint, textArea, textArea.top + labelPaint.textSize, Alignment.Start,
        )

        val tempBox = RectF(textArea.left, textArea.top + labelPaint.textSize + gap, textArea.right, textArea.bottom - labelPaint.textSize - gap)
        val temp = "${w.tempC.toInt()}°"
        val tempPaint = Text.paint(ctx, style, tempBox.height(), ink)
        tempPaint.textSize = Text.fitted(tempPaint, temp, tempBox.width(), tempBox.height() * 0.95f, 14f) * style.typeScale
        Text.drawOptical(canvas, temp, tempPaint, tempBox, Alignment.Start)

        Text.drawAt(
            canvas, "${w.highC.toInt()}° / ${w.lowC.toInt()}°",
            labelPaint, textArea, textArea.bottom, Alignment.Start,
        )
    }

    private fun drawHourly(
        canvas: Canvas, box: RectF, w: WidgetData.Weather, ctx: DrawContext,
        accent: Int, muted: Int, style: com.prism.studio.model.WidgetStyle,
    ) {
        val points = w.hourly.take(12)
        val minT = points.minOf { it.second }
        val maxT = points.maxOf { it.second }
        val span = (maxT - minT).takeIf { it > 0.5f } ?: 1f
        val chart = RectF(box.left, box.top + ctx.density.dp(12f), box.right, box.bottom - ctx.density.dp(12f))

        val path = Path()
        points.forEachIndexed { i, (_, t) ->
            val x = chart.left + chart.width() * i / (points.size - 1f)
            val y = chart.bottom - chart.height() * (t - minT) / span
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = ctx.density.dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = accent
        })

        // Hour labels at the ends only. Labelling every point turns a glanceable line into a table.
        val hourPaint = Text.paint(ctx, style, ctx.density.dp(9f) * style.typeScale, muted, weight = 500)
        Text.drawAt(canvas, "%02d".format(points.first().first), hourPaint, box, box.bottom, Alignment.Start)
        Text.drawAt(canvas, "%02d".format(points.last().first), hourPaint, box, box.bottom, Alignment.End)
    }
}

/**
 * Sunrise and sunset as an arc with the sun's current position on it.
 *
 * The arc is the widget: times alone are a two-line text widget, but an arc shows *how much
 * daylight is left* without arithmetic. Before dawn and after dusk the marker parks at the
 * respective end and the arc dims, rather than the marker disappearing and leaving a puzzle.
 */
class SunriseSunsetRenderer : ContentRenderer {

    override val type = WidgetType.SunriseSunset

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val sun = data as? WidgetData.Sun ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        val labelSize = ctx.density.dp(10f) * style.typeScale
        val labelPaint = Text.paint(ctx, style, labelSize, muted, weight = 600)
        val arcBox = RectF(
            content.left + ctx.density.dp(6f), content.top,
            content.right - ctx.density.dp(6f), content.bottom + content.height() * 0.7f,
        )

        val daylight = sun.dayProgress.coerceIn(0f, 1f)
        val isDay = sun.dayProgress in 0f..1f

        canvas.drawArc(arcBox, 180f, 180f, false, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = ctx.density.dp(1.5f)
            color = muted
            alpha = 90
        })
        canvas.drawArc(arcBox, 180f, 180f * daylight, false, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = ctx.density.dp(2.5f)
            strokeCap = Paint.Cap.ROUND
            color = if (isDay) accent else muted
        })

        val angle = Math.toRadians(180.0 + 180.0 * daylight)
        val rx = arcBox.width() / 2f
        val ry = arcBox.height() / 2f
        canvas.drawCircle(
            arcBox.centerX() + cos(angle).toFloat() * rx,
            arcBox.centerY() + sin(angle).toFloat() * ry,
            ctx.density.dp(if (isDay) 5f else 3.5f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isDay) accent else muted },
        )

        val fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        Text.drawAt(canvas, sun.sunrise.format(fmt), labelPaint, content, content.bottom, Alignment.Start)
        Text.drawAt(canvas, sun.sunset.format(fmt), labelPaint, content, content.bottom, Alignment.End)
        sun.place?.let {
            val p = Text.paint(ctx, style, labelSize, ink, weight = 600)
            Text.drawAt(canvas, Text.ellipsize(p, it, content.width() * 0.6f), p, content, content.top + labelSize, Alignment.Center)
        }
    }
}

/**
 * Agenda: the next few events, each with a time, a title, and its calendar's colour.
 *
 * Rows are sized so that whatever fits, fits *completely* — a half-visible fourth row is worse than
 * three rows and white space, because it reads as broken rather than as a list that continues.
 */
class AgendaRenderer : ContentRenderer {

    override val type = WidgetType.Agenda

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val cal = data as? WidgetData.Calendar ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        val rowHeight = ctx.density.dp(34f) * style.typeScale
        val capacity = (content.height() / rowHeight).toInt().coerceAtLeast(1)
        val events = cal.events.take(capacity)

        if (events.isEmpty()) {
            val p = Text.paint(ctx, style, ctx.density.dp(13f) * style.typeScale, muted)
            Text.drawOptical(canvas, "Nothing scheduled", p, content, style.alignment)
            return
        }

        val timePaint = Text.paint(ctx, style, ctx.density.dp(11f) * style.typeScale, muted, weight = 600)
        val titlePaint = Text.paint(ctx, style, ctx.density.dp(14f) * style.typeScale, ink)
        val bar = Paint(Paint.ANTI_ALIAS_FLAG)
        val fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        events.forEachIndexed { i, event ->
            val top = content.top + rowHeight * i
            val barWidth = ctx.density.dp(2.5f)
            bar.color = event.colorArgb?.toInt() ?: accent
            canvas.drawRoundRect(
                RectF(content.left, top + rowHeight * 0.12f, content.left + barWidth, top + rowHeight * 0.88f),
                barWidth / 2f, barWidth / 2f, bar,
            )
            val textLeft = content.left + barWidth + ctx.density.dp(8f)
            val box = RectF(textLeft, top, content.right, top + rowHeight)
            canvas.drawText(event.at.format(fmt), textLeft, top + timePaint.textSize, timePaint)
            canvas.drawText(
                Text.ellipsize(titlePaint, event.title, box.width()),
                textLeft, top + timePaint.textSize + titlePaint.textSize + ctx.density.dp(2f), titlePaint,
            )
        }
    }
}
