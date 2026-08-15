package com.prism.studio.render.content

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.prism.studio.model.Alignment
import com.prism.studio.model.ContentLayout
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType
import com.prism.studio.render.ContentRenderer
import com.prism.studio.render.DrawContext
import com.prism.studio.render.WidgetData
import java.util.Locale
import kotlin.math.max

/**
 * One renderer, six widget types.
 *
 * Battery, CPU, RAM, storage, network, and steps are all "a fraction, a number, and a label".
 * Collapsing them into a single [WidgetData.Gauge] renderer is what makes the catalog arithmetic
 * work: authoring a family means choosing colours and layouts, not re-drawing a progress ring six
 * times. Type-specific behaviour is limited to the state colour rule below.
 */
class GaugeRenderer(override val type: WidgetType) : ContentRenderer {

    companion object {
        /** Every gauge-shaped type, instantiated together at registry construction. */
        val types = listOf(
            WidgetType.Battery, WidgetType.Cpu, WidgetType.Ram,
            WidgetType.Storage, WidgetType.Network, WidgetType.Steps,
        )
        fun all(): List<ContentRenderer> = types.map { GaugeRenderer(it) }
    }

    override fun draw(
        canvas: Canvas,
        content: RectF,
        widget: ResolvedWidget,
        data: WidgetData,
        ctx: DrawContext,
    ) {
        val gauge = data as? WidgetData.Gauge ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = accentFor(gauge, ctx, widget)

        when (widget.variant.layout) {
            ContentLayout.Ring -> drawRing(canvas, content, gauge, widget, ctx, ink, muted, accent)
            ContentLayout.HeroWithGauge -> drawBar(canvas, content, gauge, widget, ctx, ink, muted, accent)
            ContentLayout.Chart -> drawSparkline(canvas, content, gauge, widget, ctx, ink, muted, accent)
            else -> drawHero(canvas, content, gauge, widget, ctx, ink, muted)
        }
    }

    /**
     * Low and critical states recolour the indicator only — never the whole widget. A battery
     * widget that turns red at 15% is alarming; a widget whose arc turns red is informative.
     */
    private fun accentFor(gauge: WidgetData.Gauge, ctx: DrawContext, widget: ResolvedWidget): Int =
        when (gauge.state) {
            WidgetData.Gauge.State.Critical -> 0xFFFF5A5A.toInt()
            WidgetData.Gauge.State.Low -> 0xFFFFA23E.toInt()
            WidgetData.Gauge.State.Charging -> 0xFF56E39F.toInt()
            WidgetData.Gauge.State.Normal -> ctx.colors.resolve(widget.style.accent)
        }

    private fun drawRing(
        canvas: Canvas, content: RectF, gauge: WidgetData.Gauge, widget: ResolvedWidget,
        ctx: DrawContext, ink: Int, muted: Int, accent: Int,
    ) {
        val style = widget.style
        val stroke = ctx.density.dp(max(6f, content.width() * 0.055f))
        val d = minOf(content.width(), content.height()) - stroke
        val box = RectF(
            content.centerX() - d / 2f, content.centerY() - d / 2f,
            content.centerX() + d / 2f, content.centerY() + d / 2f,
        )
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; strokeWidth = stroke
            color = muted; alpha = 60
        }
        canvas.drawArc(box, -90f, 360f, false, track)

        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND; color = accent
        }
        canvas.drawArc(box, -90f, 360f * gauge.fraction.coerceIn(0f, 1f), false, arc)

        val inner = RectF(box.left + stroke * 1.6f, box.top + stroke * 1.6f, box.right - stroke * 1.6f, box.bottom - stroke * 1.6f)
        val paint = Text.paint(ctx, style, inner.height() * 0.6f, ink, weight = style.fontWeight)
        paint.textSize = Text.fitted(paint, gauge.primary, inner.width(), inner.height() * 0.6f, 10f) * style.typeScale
        Text.drawOptical(canvas, gauge.primary, paint, inner, Alignment.Center)
    }

    private fun drawBar(
        canvas: Canvas, content: RectF, gauge: WidgetData.Gauge, widget: ResolvedWidget,
        ctx: DrawContext, ink: Int, muted: Int, accent: Int,
    ) {
        val style = widget.style
        val barHeight = ctx.density.dp(8f)
        val gap = ctx.density.dp(style.spacingDp)
        val labelSize = ctx.density.dp(10f) * style.typeScale

        gauge.label?.let {
            val p = Text.paint(ctx, style, labelSize, muted, weight = 600)
            p.letterSpacing = 0.12f
            Text.drawAt(canvas, it.uppercase(Locale.getDefault()), p, content, content.top + labelSize, style.alignment)
        }

        val valueBox = RectF(
            content.left, content.top + labelSize + gap,
            content.right, content.bottom - barHeight - gap,
        )
        val paint = Text.paint(ctx, style, valueBox.height(), ink)
        paint.textSize = Text.fitted(paint, gauge.primary, valueBox.width(), valueBox.height() * 0.95f, 12f) * style.typeScale
        Text.drawOptical(canvas, gauge.primary, paint, valueBox, style.alignment)

        val r = barHeight / 2f
        val track = RectF(content.left, content.bottom - barHeight, content.right, content.bottom)
        canvas.drawRoundRect(track, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; alpha = 55 })
        val filled = RectF(track.left, track.top, track.left + track.width() * gauge.fraction.coerceIn(0f, 1f), track.bottom)
        if (filled.width() > 0f) {
            canvas.drawRoundRect(filled, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        }
    }

    private fun drawSparkline(
        canvas: Canvas, content: RectF, gauge: WidgetData.Gauge, widget: ResolvedWidget,
        ctx: DrawContext, ink: Int, muted: Int, accent: Int,
    ) {
        if (gauge.history.size < 2) return drawHero(canvas, content, gauge, widget, ctx, ink, muted)
        val chart = RectF(content.left, content.centerY(), content.right, content.bottom)
        val path = android.graphics.Path()
        gauge.history.forEachIndexed { i, v ->
            val x = chart.left + chart.width() * i / (gauge.history.size - 1f)
            val y = chart.bottom - chart.height() * v.coerceIn(0f, 1f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ctx.density.dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = accent
        })
        val top = RectF(content.left, content.top, content.right, content.centerY() - ctx.density.dp(4f))
        drawHero(canvas, top, gauge, widget, ctx, ink, muted)
    }

    private fun drawHero(
        canvas: Canvas, content: RectF, gauge: WidgetData.Gauge, widget: ResolvedWidget,
        ctx: DrawContext, ink: Int, muted: Int,
    ) {
        val style = widget.style
        val paint = Text.paint(ctx, style, content.height(), ink)
        paint.textSize = Text.fitted(paint, gauge.primary, content.width(), content.height() * 0.85f, 12f) * style.typeScale
        Text.drawOptical(canvas, gauge.primary, paint, content, style.alignment)
    }
}
