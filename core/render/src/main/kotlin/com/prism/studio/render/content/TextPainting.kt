package com.prism.studio.render.content

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.prism.studio.model.Alignment
import com.prism.studio.model.WidgetStyle
import com.prism.studio.render.DrawContext

/**
 * Type-setting helpers shared by every content renderer.
 *
 * Widget typography lives or dies on optical alignment: a big numeral centred by its font metrics
 * looks low, because the metrics reserve room for descenders the digits never use. [drawOptical]
 * measures the actual ink instead, which is why Prism clocks sit correctly in their frames when
 * most widget apps' don't.
 */
internal object Text {

    fun paint(ctx: DrawContext, style: WidgetStyle, sizePx: Float, color: Int, weight: Int = style.fontWeight): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ctx.typefaces.get(style.fontFamily, weight)
            textSize = sizePx
            this.color = color
            letterSpacing = style.letterSpacingEm
            isSubpixelText = true
        }

    /** Draws [text] centred on the true ink bounds of the glyphs, not the font's line box. */
    fun drawOptical(canvas: Canvas, text: String, paint: Paint, bounds: RectF, align: Alignment) {
        val ink = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, ink)
        val x = when (align) {
            Alignment.Start -> bounds.left - ink.left
            Alignment.Center -> bounds.centerX() - ink.width() / 2f - ink.left
            Alignment.End -> bounds.right - ink.width() - ink.left
        }
        val y = bounds.centerY() + ink.height() / 2f - (ink.height() + ink.top).toFloat()
        canvas.drawText(text, x, y, paint)
    }

    /** Draws at a baseline, honouring alignment. For stacked rows where rhythm matters more. */
    fun drawAt(canvas: Canvas, text: String, paint: Paint, bounds: RectF, baselineY: Float, align: Alignment) {
        val w = paint.measureText(text)
        val x = when (align) {
            Alignment.Start -> bounds.left
            Alignment.Center -> bounds.centerX() - w / 2f
            Alignment.End -> bounds.right - w
        }
        canvas.drawText(text, x, baselineY, paint)
    }

    /**
     * Largest text size that fits [text] in [maxWidth], starting from [preferred] and never going
     * below [floor]. Keeps a 12-hour clock and a 24-hour clock optically identical in weight.
     */
    fun fitted(paint: Paint, text: String, maxWidth: Float, preferred: Float, floor: Float): Float {
        var size = preferred
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > floor) {
            size -= 1f
            paint.textSize = size
        }
        return size
    }

    /** Truncates with a middle ellipsis, which reads better than a trailing one for titles. */
    fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.take(end) + "…") > maxWidth) end--
        return text.take(end).trimEnd() + "…"
    }
}
