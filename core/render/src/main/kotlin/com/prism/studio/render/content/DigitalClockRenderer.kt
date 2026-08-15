package com.prism.studio.render.content

import android.graphics.Canvas
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

/**
 * The most-placed widget type in any customisation app, so it gets the most care.
 *
 * Three layouts are supported and each is a genuinely different design idea rather than the same
 * clock at three sizes: [ContentLayout.Hero] is time alone at maximum scale; [ContentLayout.Split]
 * stacks hours over minutes for the tall families; [ContentLayout.HeroLabelled] adds the date in
 * muted ink beneath.
 */
class DigitalClockRenderer : ContentRenderer {

    override val type = WidgetType.DigitalClock

    private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

    override fun draw(
        canvas: Canvas,
        content: RectF,
        widget: ResolvedWidget,
        data: WidgetData,
        ctx: DrawContext,
    ) {
        val clock = data as? WidgetData.Clock ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)

        val hour = if (clock.is24Hour) {
            "%02d".format(clock.now.hour)
        } else {
            val h = clock.now.hour % 12
            (if (h == 0) 12 else h).toString()
        }
        val minute = "%02d".format(clock.now.minute)

        when (widget.variant.layout) {
            ContentLayout.Split -> {
                // Hours above minutes, both flush to the alignment edge. Reads as a poster, and
                // sidesteps the colon entirely — one less element to get wrong at small sizes.
                val rowHeight = content.height() / 2f
                val paint = Text.paint(ctx, style, rowHeight, ink)
                val size = Text.fitted(paint, "88", content.width(), rowHeight * 1.05f, rowHeight * 0.4f)
                paint.textSize = size * style.typeScale

                Text.drawOptical(canvas, hour, paint, RectF(content.left, content.top, content.right, content.top + rowHeight), style.alignment)
                Text.drawOptical(canvas, minute, paint.also { it.color = muted }, RectF(content.left, content.top + rowHeight, content.right, content.bottom), style.alignment)
            }

            ContentLayout.HeroLabelled -> {
                val dateSize = ctx.density.dp(11f) * style.typeScale
                val datePaint = Text.paint(ctx, style, dateSize, muted, weight = maxOf(400, style.fontWeight - 200))
                val gap = ctx.density.dp(style.spacingDp)
                val timeBox = RectF(content.left, content.top, content.right, content.bottom - dateSize - gap)

                drawHero(canvas, "$hour:$minute", timeBox, widget, ctx, ink)
                Text.drawAt(
                    canvas,
                    Text.ellipsize(datePaint, clock.now.format(dateFmt), content.width()),
                    datePaint, content, content.bottom, style.alignment,
                )
            }

            else -> drawHero(canvas, "$hour:$minute", content, widget, ctx, ink)
        }

        clock.zoneLabel?.let { label ->
            val p = Text.paint(ctx, style, ctx.density.dp(9f) * style.typeScale, muted, weight = 600)
            p.letterSpacing = 0.14f
            Text.drawAt(canvas, label.uppercase(Locale.getDefault()), p, content, content.top + p.textSize, Alignment.End)
        }
    }

    private fun drawHero(canvas: Canvas, text: String, box: RectF, widget: ResolvedWidget, ctx: DrawContext, color: Int) {
        val style = widget.style
        val paint = Text.paint(ctx, style, box.height(), color)
        paint.textSize = Text.fitted(paint, text, box.width(), box.height() * 0.92f, box.height() * 0.3f) * style.typeScale
        Text.drawOptical(canvas, text, paint, box, style.alignment)
    }
}
