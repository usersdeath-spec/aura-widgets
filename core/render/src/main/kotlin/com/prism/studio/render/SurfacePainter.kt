package com.prism.studio.render

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import com.prism.studio.model.GradientKind
import com.prism.studio.model.Surface
import com.prism.studio.model.WidgetStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws everything behind the content: shadow, surface, glow, stroke.
 *
 * Why a Canvas instead of Glance/RemoteViews composition: RemoteViews cannot blur, cannot draw
 * gradients with arbitrary stops, cannot stroke sub-pixel hairlines, and cannot composite mesh
 * blobs. Rasterising to a single Bitmap and handing the host one ImageView gives us full drawing
 * freedom, one view to inflate, and a RemoteViews payload that stays far below the 1 MB binder
 * limit at typical widget sizes. Interactive areas are layered back on top as transparent
 * click targets — see :widget/WidgetViewsFactory.
 */
class SurfacePainter(private val colors: ColorResolver) {

    /**
     * Liquid Glass is drawn by its own painter — six layers, its own light model — but it is still
     * a Surface, so it has to be reachable from here. Routing it rather than duplicating it keeps
     * one entry point for every surface kind.
     */
    private val glassPainter = GlassPainter(colors)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val effect = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * @param backdrop what sits behind a Liquid Glass surface, when it is known. Null means the
     *   Painted tier: no wallpaper bitmap and no palette, so the plate is carried by its tint and
     *   edge light alone. Ignored by every other surface kind.
     */
    fun paint(
        canvas: Canvas,
        style: WidgetStyle,
        size: RenderSize,
        backdrop: BackdropProvider.Result? = null,
    ) {
        val d = size.density
        val radius = d.dp(style.cornerRadiusDp)
        val inset = (style.stroke?.widthDp ?: 0f).let { d.dp(it) } / 2f
        val bounds = RectF(inset, inset, size.widthPx - inset, size.heightPx - inset)

        style.shadow?.let {
            effect.reset()
            effect.isAntiAlias = true
            effect.color = colors.resolve(it.color, it.alpha)
            effect.maskFilter = BlurMaskFilter(d.dp(it.radiusDp), BlurMaskFilter.Blur.NORMAL)
            canvas.save()
            canvas.translate(0f, d.dp(it.dy))
            canvas.drawRoundRect(bounds, radius, radius, effect)
            canvas.restore()
        }

        drawSurface(canvas, style.surface, bounds, radius, size, backdrop)

        style.glow?.let {
            effect.reset()
            effect.isAntiAlias = true
            effect.style = Paint.Style.STROKE
            effect.strokeWidth = d.dp(2f)
            effect.color = colors.resolve(it.color, it.alpha)
            effect.maskFilter = BlurMaskFilter(d.dp(it.radiusDp), BlurMaskFilter.Blur.OUTER)
            canvas.drawRoundRect(bounds, radius, radius, effect)
        }

        style.stroke?.let { s ->
            strokePaint.shader = null
            strokePaint.strokeWidth = d.dp(s.widthDp)
            strokePaint.color = colors.resolve(s.color)
            s.gradient?.let { stops ->
                strokePaint.shader = LinearGradient(
                    bounds.left, bounds.top, bounds.right, bounds.bottom,
                    stops.map { colors.resolve(it.color) }.toIntArray(),
                    stops.map { it.at }.toFloatArray(),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRoundRect(bounds, radius, radius, strokePaint)
        }
    }

    private fun drawSurface(
        canvas: Canvas,
        surface: Surface,
        bounds: RectF,
        radius: Float,
        size: RenderSize,
        backdrop: BackdropProvider.Result?,
    ) {
        when (surface) {
            Surface.None -> Unit

            is Surface.LiquidGlass ->
                glassPainter.paint(canvas, surface, bounds, radius, size, backdrop)

            is Surface.Solid -> {
                fill.reset(); fill.isAntiAlias = true
                fill.color = colors.resolve(surface.color)
                canvas.drawRoundRect(bounds, radius, radius, fill)
            }

            is Surface.Gradient -> {
                val cols = surface.stops.map { colors.resolve(it.color) }.toIntArray()
                val pos = surface.stops.map { it.at }.toFloatArray()
                fill.reset(); fill.isAntiAlias = true
                fill.shader = when (surface.kind) {
                    GradientKind.Linear -> {
                        val rad = Math.toRadians(surface.angleDeg.toDouble())
                        val dx = cos(rad).toFloat() * bounds.width() / 2f
                        val dy = sin(rad).toFloat() * bounds.height() / 2f
                        LinearGradient(
                            bounds.centerX() - dx, bounds.centerY() - dy,
                            bounds.centerX() + dx, bounds.centerY() + dy,
                            cols, pos, Shader.TileMode.CLAMP,
                        )
                    }
                    GradientKind.Radial -> RadialGradient(
                        bounds.centerX(), bounds.centerY(), maxOf(bounds.width(), bounds.height()) / 1.4f,
                        cols, pos, Shader.TileMode.CLAMP,
                    )
                    GradientKind.Sweep -> SweepGradient(bounds.centerX(), bounds.centerY(), cols, pos)
                }
                canvas.drawRoundRect(bounds, radius, radius, fill)
            }

            is Surface.Glass -> drawGlass(canvas, surface, bounds, radius, size)

            is Surface.Mesh -> {
                fill.reset(); fill.isAntiAlias = true
                fill.color = colors.resolve(surface.base)
                canvas.drawRoundRect(bounds, radius, radius, fill)

                val clip = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }
                canvas.save()
                canvas.clipPath(clip)
                surface.blobs.forEach { blob ->
                    effect.reset(); effect.isAntiAlias = true
                    effect.color = colors.resolve(blob.color)
                    effect.maskFilter = BlurMaskFilter(
                        size.density.dp(surface.blurDp), BlurMaskFilter.Blur.NORMAL,
                    )
                    canvas.drawCircle(
                        bounds.left + bounds.width() * blob.x,
                        bounds.top + bounds.height() * blob.y,
                        size.shortestSide * blob.radius,
                        effect,
                    )
                }
                canvas.restore()
            }

            is Surface.Extruded -> {
                val base = colors.resolve(surface.base)
                val depth = size.density.dp(surface.depthDp)
                val light = blend(base, Color.WHITE, 0.22f)
                val dark = blend(base, Color.BLACK, 0.30f)
                val dir = if (surface.inset) -1f else 1f

                effect.reset(); effect.isAntiAlias = true
                effect.maskFilter = BlurMaskFilter(depth, BlurMaskFilter.Blur.NORMAL)
                effect.color = dark
                canvas.save(); canvas.translate(depth * dir, depth * dir)
                canvas.drawRoundRect(bounds, radius, radius, effect); canvas.restore()

                effect.color = light
                canvas.save(); canvas.translate(-depth * dir, -depth * dir)
                canvas.drawRoundRect(bounds, radius, radius, effect); canvas.restore()

                fill.reset(); fill.isAntiAlias = true
                fill.color = base
                canvas.drawRoundRect(bounds, radius, radius, fill)
            }
        }
    }

    /**
     * Frosted glass without a backdrop.
     *
     * A widget has no access to the pixels beneath it, so a true backdrop blur is impossible on
     * Android. What sells glass instead is edge behaviour: a translucent body, a bright top-left
     * highlight falling off fast, a barely-there grain, and a hairline that brightens where light
     * would catch. Users read that as glass even though nothing is blurred.
     */
    private fun drawGlass(canvas: Canvas, glass: Surface.Glass, bounds: RectF, radius: Float, size: RenderSize) {
        val tint = colors.resolve(glass.tint)

        fill.reset(); fill.isAntiAlias = true
        fill.color = withAlpha(tint, glass.fillAlpha)
        canvas.drawRoundRect(bounds, radius, radius, fill)

        fill.shader = LinearGradient(
            bounds.left, bounds.top, bounds.right, bounds.bottom,
            intArrayOf(
                withAlpha(Color.WHITE, glass.highlightAlpha),
                withAlpha(Color.WHITE, glass.highlightAlpha * 0.15f),
                withAlpha(Color.WHITE, 0f),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, radius, radius, fill)
        fill.shader = null

        if (glass.grainAlpha > 0f) {
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) })
            canvas.drawBitmap(
                GrainTexture.get(size.density),
                null,
                bounds,
                Paint().apply { alpha = (glass.grainAlpha * 255).toInt() },
            )
            canvas.restore()
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun blend(a: Int, b: Int, t: Float): Int = Color.argb(
        255,
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt(),
    )
}
