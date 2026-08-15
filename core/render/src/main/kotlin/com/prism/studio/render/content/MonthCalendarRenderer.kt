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
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * A full month in a 4x4 widget without looking cramped.
 *
 * The trick is that the grid is drawn on an optical baseline grid rather than a box grid: rows are
 * spaced by cap height, not by cell height, so the numerals sit evenly even though the month has
 * five or six rows depending on where it starts. Week start follows the device locale.
 */
class MonthCalendarRenderer : ContentRenderer {

    override val type = WidgetType.MonthCalendar

    override fun draw(
        canvas: Canvas,
        content: RectF,
        widget: ResolvedWidget,
        data: WidgetData,
        ctx: DrawContext,
    ) {
        val cal = data as? WidgetData.Calendar ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)
        val locale = Locale.getDefault()
        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

        val month = cal.monthAnchor
        val titleSize = ctx.density.dp(13f) * style.typeScale
        val titlePaint = Text.paint(ctx, style, titleSize, ink, weight = maxOf(style.fontWeight, 600))
        val title = month.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.titlecase(locale) } + " " + month.year
        Text.drawAt(canvas, title, titlePaint, content, content.top + titleSize, style.alignment)

        val gridTop = content.top + titleSize + ctx.density.dp(style.spacingDp)
        val colWidth = content.width() / 7f
        val headerSize = ctx.density.dp(8.5f) * style.typeScale
        val headerPaint = Text.paint(ctx, style, headerSize, muted, weight = 600).apply { letterSpacing = 0.08f }

        for (i in 0 until 7) {
            val day = firstDayOfWeek.plus(i.toLong())
            val label = day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
            val box = RectF(content.left + colWidth * i, gridTop, content.left + colWidth * (i + 1), gridTop + headerSize)
            Text.drawOptical(canvas, label, headerPaint, box, Alignment.Center)
        }

        val daysInMonth = month.lengthOfMonth()
        val offset = (month.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val rows = ((daysInMonth + offset + 6) / 7)
        val bodyTop = gridTop + headerSize + ctx.density.dp(6f)
        val rowHeight = (content.bottom - bodyTop) / rows
        val daySize = minOf(rowHeight * 0.62f, colWidth * 0.62f) * style.typeScale
        val dayPaint = Text.paint(ctx, style, daySize, muted, weight = style.fontWeight)
        val todayPaint = Text.paint(ctx, style, daySize, ctx.colors.resolve(style.ink), weight = 700)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }

        for (day in 1..daysInMonth) {
            val index = day - 1 + offset
            val col = index % 7
            val row = index / 7
            val box = RectF(
                content.left + colWidth * col, bodyTop + rowHeight * row,
                content.left + colWidth * (col + 1), bodyTop + rowHeight * (row + 1),
            )
            val isToday = month.withDayOfMonth(day) == cal.today

            if (isToday) {
                canvas.drawCircle(box.centerX(), box.centerY(), daySize * 0.78f, dot)
            }
            Text.drawOptical(
                canvas, day.toString(),
                if (isToday) todayPaint else dayPaint,
                box, Alignment.Center,
            )
            if (!isToday && day in cal.markedDays) {
                canvas.drawCircle(box.centerX(), box.bottom - ctx.density.dp(2f), ctx.density.dp(1.5f), dot)
            }
        }
    }
}
