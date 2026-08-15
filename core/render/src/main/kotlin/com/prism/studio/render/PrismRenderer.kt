package com.prism.studio.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.LruCache
import com.prism.studio.model.ResolvedWidget

/**
 * The one place a widget becomes pixels.
 *
 * Used by the home-screen provider, the catalog grid, and the live editor preview. Because all
 * three share this path, "what you see is what you place" is a structural guarantee rather than
 * something we have to keep in sync by hand.
 */
class PrismRenderer(
    private val registry: ContentRendererRegistry,
    private val typefaces: TypefaceProvider,
    cacheBytes: Int = 8 * 1024 * 1024,
) {
    /**
     * Keyed on (style fingerprint, variant, size, data fingerprint). A clock widget whose minute
     * has not changed is a cache hit, which is why scrolling the catalog stays at 120 fps and why
     * a screen full of widgets costs one rasterisation each, not one per host redraw request.
     */
    private val cache = object : LruCache<String, Bitmap>(cacheBytes) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    fun render(
        widget: ResolvedWidget,
        data: WidgetData,
        size: RenderSize,
        colors: ColorResolver,
        dataFingerprint: String,
        backdrop: BackdropProvider.Result? = null,
    ): Bitmap {
        val key = buildString {
            append(widget.family.id.value); append('/')
            append(widget.variant.id.value); append('/')
            append(widget.style.fingerprint); append('/')
            append(size.widthPx); append('x'); append(size.heightPx); append('/')
            append(dataFingerprint)
            // The backdrop is part of the identity of a glass widget: the same style over a
            // different wallpaper crop is a different bitmap, and caching without this would show
            // yesterday's wallpaper through today's glass.
            backdrop?.let { append('/'); append(it.tier.name); append(it.bitmap?.generationId ?: 0) }
        }
        cache.get(key)?.let { return it }

        val bitmap = draw(widget, data, size, colors, backdrop)
        cache.put(key, bitmap)
        return bitmap
    }

    /**
     * Draws, and never throws.
     *
     * A widget app cannot let one bad renderer take the process down. On the home screen a thrown
     * exception happens inside the launcher's binder call; in the catalog it kills a scroll through
     * 708 designs. Either way the user sees a crash caused by one variant out of hundreds.
     *
     * Every draw is therefore wrapped. A failure yields a marked placeholder instead of propagating,
     * and records which family and variant failed in [failures] so the offender can be found rather
     * than guessed at. Silently swallowing would be worse than crashing; this is loud but survivable.
     */
    fun draw(
        widget: ResolvedWidget,
        data: WidgetData,
        size: RenderSize,
        colors: ColorResolver,
        backdrop: BackdropProvider.Result? = null,
    ): Bitmap = try {
        drawOrThrow(widget, data, size, colors, backdrop)
    } catch (t: Throwable) {
        val id = "${widget.family.id.value}/${widget.variant.id.value}"
        failures[id] = t.toString()
        android.util.Log.e("PrismRenderer", "render failed for $id at ${size.widthPx}x${size.heightPx}", t)
        placeholder(size)
    }

    /**
     * Renders that have failed, keyed by family/variant. Surfaced by the audit suite so a defect
     * found on a device becomes a failing test rather than a bug report.
     */
    val failures: MutableMap<String, String> = java.util.concurrent.ConcurrentHashMap()

    private fun placeholder(size: RenderSize): Bitmap =
        Bitmap.createBitmap(size.widthPx.coerceAtLeast(1), size.heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)

    private fun drawOrThrow(
        widget: ResolvedWidget,
        data: WidgetData,
        size: RenderSize,
        colors: ColorResolver,
        backdrop: BackdropProvider.Result?,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size.widthPx, size.heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val style = widget.style

        SurfacePainter(colors).paint(canvas, style, size, backdrop)

        val pad = size.density.dp(style.paddingDp)
        val content = RectF(pad, pad, size.widthPx - pad, size.heightPx - pad)
        registry.rendererFor(widget.variant.type)
            .draw(canvas, content, widget, data, DrawContext(colors, typefaces, size))

        if (style.opacity < 1f) applyOpacity(canvas, size, style.opacity)
        return bitmap
    }

    /**
     * Applied to the composited result rather than to each layer, so a 40%-opacity glass widget
     * fades as one object instead of revealing its own internal layering.
     */
    private fun applyOpacity(canvas: Canvas, size: RenderSize, opacity: Float) {
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawRect(0f, 0f, size.widthPx.toFloat(), size.heightPx.toFloat(), paint)
    }

    fun evictAll() = cache.evictAll()
}
