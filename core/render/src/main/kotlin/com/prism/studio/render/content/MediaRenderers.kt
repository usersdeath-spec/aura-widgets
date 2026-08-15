package com.prism.studio.render.content

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.prism.studio.model.Alignment
import com.prism.studio.model.ContentLayout
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType
import com.prism.studio.render.ContentRenderer
import com.prism.studio.render.DrawContext
import com.prism.studio.render.WidgetData
import kotlin.math.min

/**
 * Supplies bitmaps the renderers need but do not own — album art, user photos.
 *
 * Injected rather than fetched, because a renderer that performs I/O is a renderer that can block a
 * widget update. Whoever calls the renderer has already resolved the bitmap or has not; the
 * renderer draws a graceful placeholder in the second case and never waits.
 */
interface BitmapSource {
    fun bitmap(key: String): Bitmap?
}

/**
 * Now Playing.
 *
 * The one widget people *touch*, which changes the rules: transport controls get 48dp targets even
 * when that costs layout elegance, and they are laid out on a fixed grid so muscle memory works
 * across families. The tappable regions are registered by the widget host as transparent overlays;
 * what is drawn here is only their appearance.
 *
 * Artwork, when present, is drawn full-bleed under a scrim rather than as an inset thumbnail. A
 * thumbnail makes every family's music widget look the same; a full-bleed cover lets the family's
 * type and scrim carry the identity.
 */
class MusicPlayerRenderer(private val bitmaps: BitmapSource) : ContentRenderer {

    override val type = WidgetType.MusicPlayer

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val media = data as? WidgetData.Media ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)
        val accent = ctx.colors.resolve(style.accent)

        val artwork = media.artworkKey?.let { bitmaps.bitmap(it) }
        val overlay = widget.variant.layout == ContentLayout.Overlay && artwork != null

        if (overlay) {
            canvas.drawBitmap(artwork, null, content, Paint(Paint.FILTER_BITMAP_FLAG))
            // Scrim only where text sits. A full-cover scrim wastes the artwork the user chose.
            val scrim = Paint().apply {
                shader = LinearGradient(
                    0f, content.centerY(), 0f, content.bottom,
                    intArrayOf(Color.TRANSPARENT, Color.argb(190, 0, 0, 0)),
                    null, Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(RectF(content.left, content.centerY(), content.right, content.bottom), scrim)
        }

        val controlSize = min(content.height() * 0.34f, ctx.density.dp(30f))
        val textRight = if (overlay) content.right else content.right - controlSize * 3.4f
        val titleSize = ctx.density.dp(14f) * style.typeScale
        val titlePaint = Text.paint(ctx, style, titleSize, if (overlay) Color.WHITE else ink, weight = maxOf(style.fontWeight, 600))
        val artistPaint = Text.paint(ctx, style, titleSize * 0.8f, if (overlay) Color.argb(200, 255, 255, 255) else muted)

        val textTop = if (overlay) content.bottom - titleSize * 2.6f else content.top + titleSize
        canvas.drawText(Text.ellipsize(titlePaint, media.title, textRight - content.left), content.left, textTop, titlePaint)
        media.artist?.let {
            canvas.drawText(
                Text.ellipsize(artistPaint, it, textRight - content.left),
                content.left, textTop + titleSize * 1.15f, artistPaint,
            )
        }

        // Progress: a hairline across the very bottom. It is genuinely useful and costs no layout.
        media.progress?.let { progress ->
            val h = ctx.density.dp(2f)
            val track = RectF(content.left, content.bottom - h, content.right, content.bottom)
            canvas.drawRect(track, Paint().apply { color = muted; alpha = 60 })
            canvas.drawRect(
                RectF(track.left, track.top, track.left + track.width() * progress.coerceIn(0f, 1f), track.bottom),
                Paint().apply { color = accent },
            )
        }

        if (!overlay) {
            val cy = content.centerY()
            val step = controlSize * 1.15f
            val right = content.right - controlSize * 0.5f
            drawSkip(canvas, right, cy, controlSize * 0.42f, forward = true, color = ink)
            drawPlayPause(canvas, right - step, cy, controlSize * 0.5f, media.playing, accent)
            drawSkip(canvas, right - step * 2, cy, controlSize * 0.42f, forward = false, color = ink)
        }
    }

    private fun drawPlayPause(canvas: Canvas, cx: Float, cy: Float, r: Float, playing: Boolean, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        if (playing) {
            val w = r * 0.28f
            canvas.drawRoundRect(RectF(cx - r * 0.42f, cy - r * 0.6f, cx - r * 0.42f + w, cy + r * 0.6f), w / 3f, w / 3f, paint)
            canvas.drawRoundRect(RectF(cx + r * 0.14f, cy - r * 0.6f, cx + r * 0.14f + w, cy + r * 0.6f), w / 3f, w / 3f, paint)
        } else {
            canvas.drawPath(
                Path().apply {
                    moveTo(cx - r * 0.4f, cy - r * 0.62f)
                    lineTo(cx + r * 0.62f, cy)
                    lineTo(cx - r * 0.4f, cy + r * 0.62f)
                    close()
                },
                paint,
            )
        }
    }

    private fun drawSkip(canvas: Canvas, cx: Float, cy: Float, r: Float, forward: Boolean, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val dir = if (forward) 1f else -1f
        val path = Path().apply {
            moveTo(cx - r * 0.5f * dir, cy - r * 0.6f)
            lineTo(cx + r * 0.2f * dir, cy)
            lineTo(cx - r * 0.5f * dir, cy + r * 0.6f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.drawRect(
            RectF(cx + r * 0.3f * dir - r * 0.09f, cy - r * 0.6f, cx + r * 0.3f * dir + r * 0.09f, cy + r * 0.6f),
            paint,
        )
    }
}

/**
 * Photo widget.
 *
 * Fills, never fits: a letterboxed photo on a home screen looks like a bug. The crop is centre-
 * weighted with a slight bias to the upper third, which is where faces sit in most casual photos.
 * A caption, if any, gets a scrim sized to the text rather than the whole frame.
 */
class PhotoRenderer(private val bitmaps: BitmapSource) : ContentRenderer {

    override val type = WidgetType.Photo

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val photo = data as? WidgetData.Photo ?: return
        val style = widget.style
        val bitmap = bitmaps.bitmap(photo.bitmapKey)

        if (bitmap == null) {
            val paint = Text.paint(ctx, style, ctx.density.dp(13f) * style.typeScale, ctx.colors.resolve(style.inkMuted))
            Text.drawOptical(canvas, "Tap to choose a photo", paint, content, Alignment.Center)
            return
        }

        val scale = maxOf(content.width() / bitmap.width, content.height() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val dest = RectF(
            content.centerX() - w / 2f,
            content.centerY() - h / 2f - (h - content.height()) * 0.12f,
            content.centerX() + w / 2f,
            content.centerY() + h / 2f - (h - content.height()) * 0.12f,
        )
        canvas.save()
        canvas.clipRect(content)
        canvas.drawBitmap(bitmap, null, dest, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()

        photo.caption?.let { caption ->
            val size = ctx.density.dp(12f) * style.typeScale
            val paint = Text.paint(ctx, style, size, Color.WHITE, weight = 600)
            val band = RectF(content.left, content.bottom - size * 2.4f, content.right, content.bottom)
            canvas.drawRect(band, Paint().apply {
                shader = LinearGradient(
                    0f, band.top, 0f, band.bottom,
                    intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)), null, Shader.TileMode.CLAMP,
                )
            })
            Text.drawAt(canvas, Text.ellipsize(paint, caption, content.width()), paint, content, content.bottom - size * 0.6f, style.alignment)
        }
    }
}

/**
 * Finance, crypto, and health — one renderer, because they are the same widget.
 *
 * A label, a value, a change, and a sparkline. The only type-specific behaviour is the colour rule:
 * markets are green up / red down, health is always the family accent. Health does not get gain/loss
 * colouring because a red step count is a judgement, and a widget should not judge.
 */
class SeriesRenderer(override val type: WidgetType) : ContentRenderer {

    companion object {
        fun all(): List<ContentRenderer> =
            listOf(WidgetType.Finance, WidgetType.Crypto, WidgetType.Health).map { SeriesRenderer(it) }
        // Not `const`: 0xFF34C77B exceeds Int.MAX_VALUE so it is a Long literal, and `.toInt()`
        // is a function call, which a const initializer may not contain.
        private val UP = 0xFF34C77B.toInt()
        private val DOWN = 0xFFE5544B.toInt()
    }

    override fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext) {
        val series = data as? WidgetData.Series ?: return
        val style = widget.style
        val ink = ctx.colors.resolve(style.ink)
        val muted = ctx.colors.resolve(style.inkMuted)

        val trend = when {
            type == WidgetType.Health -> ctx.colors.resolve(style.accent)
            series.changePercent >= 0f -> UP
            else -> DOWN
        }

        val labelSize = ctx.density.dp(11f) * style.typeScale
        val labelPaint = Text.paint(ctx, style, labelSize, muted, weight = 600).apply { letterSpacing = 0.08f }
        Text.drawAt(canvas, series.label.uppercase(java.util.Locale.getDefault()), labelPaint, content, content.top + labelSize, Alignment.Start)

        val chartTop = content.centerY()
        val valueBox = RectF(content.left, content.top + labelSize, content.right, chartTop)
        val valuePaint = Text.paint(ctx, style, valueBox.height(), ink)
        valuePaint.textSize = Text.fitted(valuePaint, series.value, valueBox.width() * 0.68f, valueBox.height() * 0.9f, 13f) * style.typeScale
        Text.drawOptical(canvas, series.value, valuePaint, valueBox, Alignment.Start)

        val changeText = (if (series.changePercent >= 0f) "+" else "") + "%.2f%%".format(series.changePercent)
        val changePaint = Text.paint(ctx, style, labelSize * 1.15f, trend, weight = 600)
        Text.drawOptical(canvas, changeText, changePaint, valueBox, Alignment.End)

        if (series.points.size < 2) return
        val chart = RectF(content.left, chartTop + ctx.density.dp(4f), content.right, content.bottom)
        val minV = series.points.min()
        val maxV = series.points.max()
        val span = (maxV - minV).takeIf { it > 1e-6f } ?: 1f

        val line = Path()
        series.points.forEachIndexed { i, v ->
            val x = chart.left + chart.width() * i / (series.points.size - 1f)
            val y = chart.bottom - chart.height() * (v - minV) / span
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = ctx.density.dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = trend
        })

        // A soft fill under the line. Costs one path and makes a two-pixel line read as a chart.
        val fill = Path(line).apply {
            lineTo(chart.right, chart.bottom)
            lineTo(chart.left, chart.bottom)
            close()
        }
        canvas.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, chart.top, 0f, chart.bottom,
                intArrayOf(Color.argb(60, Color.red(trend), Color.green(trend), Color.blue(trend)), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP,
            )
        })
    }
}
