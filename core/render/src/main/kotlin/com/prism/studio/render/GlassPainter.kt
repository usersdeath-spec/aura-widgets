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
import com.prism.studio.model.Surface
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Liquid Glass renderer.
 *
 * Glass is convincing when the *edges* behave, not when the middle is blurry. Six layers, drawn in
 * the order light actually arrives, and every one of them is doing a specific job:
 *
 *  1. **Backdrop** — the real blurred wallpaper crop when we have it (see [BackdropProvider]),
 *     a colour-matched gradient when we only have a palette, nothing when we have neither.
 *  2. **Body tint** — low-alpha colour that gives the plate a material rather than a filter.
 *  3. **Inner shadow** — darkening just inside the far edge. This is the layer that reads as
 *     *thickness*; without it glass looks like a sticker no matter how good the blur is.
 *  4. **Specular sweep** — a directional highlight from a fixed family light angle, falling off
 *     inside the first 45% of the diagonal.
 *  5. **Caustic** — the bright pool that gathers along the lower edge of a thick plate. Scaled by
 *     depth, so shallow plates don't get one.
 *  6. **Edge light** — a hairline that brightens where the light source is and fades opposite.
 *
 * Depth drives blur radius, shadow spread, specular sharpness, and caustic strength together, so
 * the editor exposes one slider and the whole illusion stays coherent as it moves.
 */
class GlassPainter(private val colors: ColorResolver) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun paint(
        canvas: Canvas,
        glass: Surface.LiquidGlass,
        bounds: RectF,
        radius: Float,
        size: RenderSize,
        backdrop: BackdropProvider.Result?,
    ) {
        val clip = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)

        drawBackdropLayer(canvas, glass, bounds, backdrop)
        drawBody(canvas, glass, bounds)
        drawInnerShadow(canvas, glass, bounds, radius, size)
        drawSpecular(canvas, glass, bounds)
        drawCaustic(canvas, glass, bounds, size)
        if (glass.grainAlpha > 0f) drawGrain(canvas, glass, bounds, size)

        canvas.restore()
        drawEdgeLight(canvas, glass, bounds, radius, size)
    }

    // 1 -------------------------------------------------------------------------------------

    private fun drawBackdropLayer(
        canvas: Canvas,
        glass: Surface.LiquidGlass,
        bounds: RectF,
        backdrop: BackdropProvider.Result?,
    ) {
        when (backdrop?.tier) {
            BackdropProvider.Tier.True -> backdrop.bitmap?.let {
                paint.reset(); paint.isAntiAlias = true; paint.isFilterBitmap = true
                canvas.drawBackdrop(it, bounds, paint)
                // Refraction: a second copy, offset and faint, bends what shows through the edges.
                if (glass.refraction > 0f) {
                    val shift = bounds.width() * 0.012f * glass.refraction
                    paint.alpha = (70 * glass.refraction).toInt().coerceIn(0, 255)
                    canvas.save()
                    canvas.translate(shift, -shift)
                    canvas.drawBackdrop(it, bounds, paint)
                    canvas.restore()
                }
            }

            /**
             * No wallpaper bitmap, but we know its colours. Building a soft gradient from the
             * sampled swatches gets us most of the perceptual benefit — the plate still looks like
             * it belongs to what is behind it, which is the actual point of a backdrop.
             */
            BackdropProvider.Tier.Synthetic -> backdrop.sampledColors?.takeIf { it.isNotEmpty() }?.let { swatches ->
                paint.reset(); paint.isAntiAlias = true
                paint.shader = LinearGradient(
                    bounds.left, bounds.top, bounds.right, bounds.bottom,
                    swatches, null, Shader.TileMode.CLAMP,
                )
                canvas.drawRect(bounds, paint)
                paint.shader = null
            }

            // Tier C: nothing behind. The body tint below carries the whole plate.
            else -> Unit
        }
    }

    // 2 -------------------------------------------------------------------------------------

    private fun drawBody(canvas: Canvas, glass: Surface.LiquidGlass, bounds: RectF) {
        paint.reset(); paint.isAntiAlias = true
        paint.color = alpha(colors.resolve(glass.tint), glass.bodyAlpha)
        canvas.drawRect(bounds, paint)
    }

    // 3 -------------------------------------------------------------------------------------

    /**
     * Drawn as a blurred stroke clipped to the plate, which puts the darkening *inside* the edge
     * rather than around it — the difference between a plate with thickness and a plate with a
     * drop shadow.
     */
    private fun drawInnerShadow(
        canvas: Canvas,
        glass: Surface.LiquidGlass,
        bounds: RectF,
        radius: Float,
        size: RenderSize,
    ) {
        if (glass.innerShadowAlpha <= 0f) return
        val spread = size.density.dp(4f + glass.depth * 10f)
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = spread
        paint.color = alpha(Color.BLACK, glass.innerShadowAlpha)
        paint.maskFilter = BlurMaskFilter(spread, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(bounds, radius, radius, paint)
        paint.maskFilter = null
        paint.style = Paint.Style.FILL
    }

    // 4 -------------------------------------------------------------------------------------

    private fun drawSpecular(canvas: Canvas, glass: Surface.LiquidGlass, bounds: RectF) {
        val rad = Math.toRadians(glass.specularAngleDeg.toDouble())
        val dx = cos(rad).toFloat() * bounds.width() / 2f
        val dy = sin(rad).toFloat() * bounds.height() / 2f
        // Sharper highlight on thicker glass: the falloff point moves inward as depth rises.
        val falloff = 0.5f - glass.depth * 0.18f

        paint.reset(); paint.isAntiAlias = true
        paint.shader = LinearGradient(
            bounds.centerX() - dx, bounds.centerY() - dy,
            bounds.centerX() + dx, bounds.centerY() + dy,
            intArrayOf(
                alpha(Color.WHITE, glass.specularAlpha),
                alpha(Color.WHITE, glass.specularAlpha * 0.18f),
                alpha(Color.WHITE, 0f),
            ),
            floatArrayOf(0f, falloff, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(bounds, paint)
        paint.shader = null
    }

    // 5 -------------------------------------------------------------------------------------

    private fun drawCaustic(canvas: Canvas, glass: Surface.LiquidGlass, bounds: RectF, size: RenderSize) {
        val strength = glass.causticAlpha * glass.depth
        if (strength <= 0.01f) return
        paint.reset(); paint.isAntiAlias = true
        paint.shader = RadialGradient(
            bounds.centerX(), bounds.bottom, bounds.width() * 0.55f,
            intArrayOf(
                alpha(colors.resolve(glass.tint), strength * 2.2f),
                alpha(Color.WHITE, strength),
                alpha(Color.WHITE, 0f),
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(bounds, paint)
        paint.shader = null
    }

    // 6 -------------------------------------------------------------------------------------

    /**
     * Drawn outside the clip so the stroke is not half-eaten by it. Brightest at the light source,
     * nearly invisible opposite — the single most effective "this is expensive" cue on a home
     * screen, and the reason the plate still reads as glass in Tier C.
     */
    private fun drawEdgeLight(
        canvas: Canvas,
        glass: Surface.LiquidGlass,
        bounds: RectF,
        radius: Float,
        size: RenderSize,
    ) {
        val rad = Math.toRadians(glass.specularAngleDeg.toDouble())
        val dx = cos(rad).toFloat() * bounds.width() / 2f
        val dy = sin(rad).toFloat() * bounds.height() / 2f

        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size.density.dp(0.9f)
        paint.shader = LinearGradient(
            bounds.centerX() - dx, bounds.centerY() - dy,
            bounds.centerX() + dx, bounds.centerY() + dy,
            intArrayOf(
                alpha(Color.WHITE, 0.62f + glass.depth * 0.2f),
                alpha(Color.WHITE, 0.16f),
                alpha(Color.WHITE, 0.05f),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, radius, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    private fun drawGrain(canvas: Canvas, glass: Surface.LiquidGlass, bounds: RectF, size: RenderSize) {
        canvas.drawBitmap(
            GrainTexture.get(size.density), null, bounds,
            Paint().apply { alpha = (glass.grainAlpha * 255).toInt() },
        )
    }

    private fun alpha(color: Int, a: Float): Int = Color.argb(
        (a * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color),
    )
}
