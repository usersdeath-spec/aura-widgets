package com.prism.studio.render.content

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.prism.studio.model.Alignment
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType
import com.prism.studio.render.ContentRenderer
import com.prism.studio.render.DrawContext
import com.prism.studio.render.WidgetData
import java.util.Locale
import kotlin.math.min

/**
 * Word wrapping for widget-sized boxes.
 *
 * `StaticLayout` would do this, but it wants a `TextPaint` and a measured width in a layout pass we
 * do not have, and it will happily produce a fifth line that runs off the bottom of a 2×2. This
 * wraps to a *line budget* instead: fit what fits, ellipsise the last line, never overflow. That is
 * the correct behaviour for a widget, where clipping is a visible defect rather than a scroll hint.
 */
internal object Wrap {

    fun lines(paint: Paint, text: String, maxWidth: Float, maxLines: Int): List<String> {
        val words = text.split(' ')
        val out = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) out += current.toString()
                current = StringBuilder(word)
                if (out.size == maxLines) break
            }
        }
        if (out.size < maxLines && current.isNotEmpty()) out += current.toString()

        return if (out.size >= maxLines && out.size < words.size) {
            out.take(maxLines).mapIndexed { i, line ->
                if (i == maxLines - 1) Text.ellipsize(paint, "$line …", maxWidth) else line
            }
        } else {
            out.take(maxLines)
        }
    }

    fun draw(canvas: Canvas, lines: List<String>, paint: Paint, box: RectF, lineHeight: Float, align: Alignment) {
        lines.forEachIndexed { i, line ->
            Text.drawAt(canvas, line, paint, box, box.top + lineHeight * (i + 1), align)
        }
    }
}

/** A note, wrapped to whatever fits. The most-used widget among people who use notes at all. */
class NotesRenderer : ContentRenderer {

    override val type = WidgetType.Notes

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val rows = data as? WidgetData.TextRows ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)

        var top = content.top
        rows.title?.let { title ->
            val size = ctx.density.dp(11f) * style.typeScale
            val p = Text.paint(ctx, style, size, muted, weight = 600).apply { letterSpacing = 0.1f }
            Text.drawAt(canvas, Text.ellipsize(p, title.uppercase(Locale.getDefault()), content.width()), p, content, content.top + size, style.alignment)
            top += size + ctx.density.dp(style.spacingDp)
        }

        val body = rows.rows.joinToString(" ") { it.text }
        if (body.isBlank()) return
        val size = ctx.density.dp(14f) * style.typeScale
        val paint = Text.paint(ctx, style, size, ink)
        val lineHeight = size * 1.35f
        val box = RectF(content.left, top, content.right, content.bottom)
        val maxLines = (box.height() / lineHeight).toInt().coerceAtLeast(1)
        Wrap.draw(canvas, Wrap.lines(paint, body, box.width(), maxLines), paint, box, lineHeight, style.alignment)
    }
}

/**
 * Task list.
 *
 * Completed rows are struck through and dimmed rather than hidden, because a list that empties as
 * you tick things is a list that shows you nothing at the end of a productive day. The checkbox
 * inherits the family's corner radius, so square families get square boxes.
 */
class TodoRenderer : ContentRenderer {

    override val type = WidgetType.Todo

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val rows = data as? WidgetData.TextRows ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        var top = content.top
        rows.title?.let { title ->
            val size = ctx.density.dp(11f) * style.typeScale
            val p = Text.paint(ctx, style, size, muted, weight = 600).apply { letterSpacing = 0.1f }
            Text.drawAt(canvas, title.uppercase(Locale.getDefault()), p, content, content.top + size, style.alignment)
            top += size + ctx.density.dp(style.spacingDp)
        }

        val rowHeight = ctx.density.dp(26f) * style.typeScale
        val capacity = ((content.bottom - top) / rowHeight).toInt().coerceAtLeast(1)
        val items = rows.rows.take(capacity)

        if (items.isEmpty()) {
            val p = Text.paint(ctx, style, ctx.density.dp(13f) * style.typeScale, muted)
            Text.drawOptical(canvas, "All clear", p, RectF(content.left, top, content.right, content.bottom), style.alignment)
            return
        }

        val boxSize = rowHeight * 0.5f
        val radius = min(ctx.density.dp(style.cornerRadiusDp) * 0.2f, boxSize / 2f)
        val textPaint = Text.paint(ctx, style, ctx.density.dp(13f) * style.typeScale, ink)
        val donePaint = Text.paint(ctx, style, ctx.density.dp(13f) * style.typeScale, muted).apply {
            isStrikeThruText = true
        }

        items.forEachIndexed { i, row ->
            val y = top + rowHeight * i
            val box = RectF(content.left, y + (rowHeight - boxSize) / 2f, content.left + boxSize, y + (rowHeight + boxSize) / 2f)
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = ctx.density.dp(1.5f)
                color = if (row.done) accent else muted
            }
            canvas.drawRoundRect(box, radius, radius, stroke)
            if (row.done) {
                val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = ctx.density.dp(1.8f)
                    strokeCap = Paint.Cap.ROUND
                    color = accent
                }
                canvas.drawLine(box.left + box.width() * 0.24f, box.centerY(), box.centerX() - box.width() * 0.02f, box.bottom - box.height() * 0.26f, tick)
                canvas.drawLine(box.centerX() - box.width() * 0.02f, box.bottom - box.height() * 0.26f, box.right - box.width() * 0.18f, box.top + box.height() * 0.26f, tick)
            }
            val paint = if (row.done) donePaint else textPaint
            val textLeft = box.right + ctx.density.dp(8f)
            canvas.drawText(
                Text.ellipsize(paint, row.text, content.right - textLeft),
                textLeft, y + rowHeight / 2f + paint.textSize * 0.36f, paint,
            )
        }
    }
}

/**
 * Habit tracker: a grid of days, filled where the habit was kept.
 *
 * Cells are square and the grid width is derived from the *available* width rather than fixed at
 * seven, so a 4×2 shows a fortnight and a 4×4 shows a quarter. The streak count sits above in the
 * accent colour, because the streak is the thing that makes anyone open the widget.
 */
class HabitTrackerRenderer : ContentRenderer {

    override val type = WidgetType.HabitTracker

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val habits = data as? WidgetData.Habits ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        val headerSize = ctx.density.dp(12f) * style.typeScale
        val headerPaint = Text.paint(ctx, style, headerSize, ink, weight = 600)
        Text.drawAt(canvas, Text.ellipsize(headerPaint, habits.title, content.width() * 0.7f), headerPaint, content, content.top + headerSize, Alignment.Start)

        val streakPaint = Text.paint(ctx, style, headerSize, accent, weight = 700)
        Text.drawAt(canvas, "${habits.streak}", streakPaint, content, content.top + headerSize, Alignment.End)

        val gridTop = content.top + headerSize + ctx.density.dp(style.spacingDp)
        val gridHeight = content.bottom - gridTop
        val columns = (content.width() / ctx.density.dp(16f)).toInt().coerceIn(7, 26)
        val cell = content.width() / columns
        val rows = (gridHeight / cell).toInt().coerceAtLeast(1)
        val capacity = columns * rows
        val days = habits.days.takeLast(capacity)

        val gap = cell * 0.16f
        val radius = min(ctx.density.dp(style.cornerRadiusDp) * 0.18f, (cell - gap) / 2f)
        val on = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val off = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; alpha = 55 }

        days.forEachIndexed { i, kept ->
            val col = i % columns
            val row = i / columns
            val box = RectF(
                content.left + cell * col + gap / 2f, gridTop + cell * row + gap / 2f,
                content.left + cell * (col + 1) - gap / 2f, gridTop + cell * (row + 1) - gap / 2f,
            )
            canvas.drawRoundRect(box, radius, radius, if (kept) on else off)
        }
    }
}

/**
 * Quote. Type size is chosen from the quote's own length rather than fixed.
 *
 * A twelve-word quote set at the same size as a sixty-word one wastes the widget; sixty words at
 * the twelve-word size overflows. The renderer picks the largest size at which the text fills
 * between 60% and 95% of the available lines, which is the range where a quote looks *placed*
 * rather than dropped in.
 */
class QuoteRenderer : ContentRenderer {

    override val type = WidgetType.Quote

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val quote = data as? WidgetData.Quote ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)

        val attributionSize = ctx.density.dp(10f) * style.typeScale
        val bodyBottom = if (quote.attribution != null) {
            content.bottom - attributionSize - ctx.density.dp(style.spacingDp)
        } else {
            content.bottom
        }
        val box = RectF(content.left, content.top, content.right, bodyBottom)

        var size = ctx.density.dp(22f) * style.typeScale
        val floor = ctx.density.dp(11f)
        var lines: List<String>
        val paint = Text.paint(ctx, style, size, ink)
        while (true) {
            paint.textSize = size
            val lineHeight = size * 1.32f
            val maxLines = (box.height() / lineHeight).toInt().coerceAtLeast(1)
            lines = Wrap.lines(paint, quote.text, box.width(), maxLines)
            val fill = lines.size / maxLines.toFloat()
            val complete = lines.joinToString(" ").length >= quote.text.length - 2
            if ((complete && fill >= 0.6f) || size <= floor) break
            if (!complete) size -= 1f else break
        }

        Wrap.draw(canvas, lines, paint, box, size * 1.32f, style.alignment)

        quote.attribution?.let {
            val p = Text.paint(ctx, style, attributionSize, muted, weight = 600).apply { letterSpacing = 0.08f }
            Text.drawAt(canvas, Text.ellipsize(p, "— $it", content.width()), p, content, content.bottom, style.alignment)
        }
    }
}

/** Device readouts as label/value rows, with an optional inline bar where a fraction exists. */
class SystemInfoRenderer : ContentRenderer {

    override val type = WidgetType.SystemInfo

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val system = data as? WidgetData.System ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        val rowHeight = ctx.density.dp(24f) * style.typeScale
        val rows = system.rows.take((content.height() / rowHeight).toInt().coerceAtLeast(1))
        val labelPaint = Text.paint(ctx, style, ctx.density.dp(11f) * style.typeScale, muted, weight = 600)
        val valuePaint = Text.paint(ctx, style, ctx.density.dp(12f) * style.typeScale, ink)

        rows.forEachIndexed { i, row ->
            val top = content.top + rowHeight * i
            val box = RectF(content.left, top, content.right, top + rowHeight)
            Text.drawAt(canvas, row.label, labelPaint, box, top + rowHeight * 0.44f, Alignment.Start)
            Text.drawAt(canvas, row.value, valuePaint, box, top + rowHeight * 0.44f, Alignment.End)

            row.fraction?.let { fraction ->
                val barTop = top + rowHeight * 0.62f
                val barHeight = ctx.density.dp(3f)
                val track = RectF(box.left, barTop, box.right, barTop + barHeight)
                canvas.drawRoundRect(track, barHeight / 2f, barHeight / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; alpha = 50 })
                val filled = RectF(track.left, track.top, track.left + track.width() * fraction.coerceIn(0f, 1f), track.bottom)
                canvas.drawRoundRect(filled, barHeight / 2f, barHeight / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
            }
        }
    }
}
