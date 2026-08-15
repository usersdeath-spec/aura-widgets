package com.prism.studio.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.LruCache

/**
 * Where a real backdrop blur comes from, given that Android widgets cannot sample the pixels
 * behind them.
 *
 * The trick is that we very often already *have* those pixels. If the user set one of Prism's own
 * wallpapers — which the Setups feature makes the common case — we hold the source bitmap, and the
 * widget host tells us the widget's position on the screen. Cropping the wallpaper to that rect and
 * blurring it gives a genuine backdrop, not an approximation.
 *
 * Three tiers, chosen per render and never announced to the user:
 *
 *   [Tier.True]       our wallpaper + known widget rect  → real cropped, blurred backdrop
 *   [Tier.Synthetic]  wallpaper palette known            → gradient built from the region's colours
 *   [Tier.Painted]    nothing known                      → tinted plate, the Tier-C fallback
 *
 * Falling back is a downgrade in fidelity, never in coherence: all three tiers use the same
 * specular, caustic, and edge treatment on top, so a screen mixing tiers still reads as one family.
 */
class BackdropProvider(
    private val wallpapers: WallpaperSource,
    cacheBytes: Int = 6 * 1024 * 1024,
) {
    enum class Tier { True, Synthetic, Painted }

    /**
     * Blurred crops are keyed by wallpaper, rect, and radius. A home screen of eight glass widgets
     * over one wallpaper produces eight small bitmaps once, then hits cache on every later update —
     * which is what keeps a glass-heavy screen as cheap to refresh as a flat one.
     */
    private val cache = object : LruCache<String, Bitmap>(cacheBytes) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    data class Request(
        /** Widget bounds in screen pixels, as reported by the host. Null when unknown. */
        val screenRect: Rect?,
        val screenWidthPx: Int,
        val screenHeightPx: Int,
        val blurRadiusPx: Float,
        val size: RenderSize,
    )

    data class Result(val tier: Tier, val bitmap: Bitmap?, val sampledColors: IntArray?)

    fun obtain(request: Request): Result {
        val rect = request.screenRect
        val source = wallpapers.currentBitmap()

        if (rect != null && source != null) {
            val key = "${wallpapers.currentId()}/${rect.flattenToString()}/${request.blurRadiusPx.toInt()}"
            cache.get(key)?.let { return Result(Tier.True, it, null) }
            val cropped = cropToWidget(source, rect, request) ?: return synthetic(request)
            val blurred = StackBlur.blur(cropped, request.blurRadiusPx.toInt().coerceIn(1, 64))
            cache.put(key, blurred)
            return Result(Tier.True, blurred, null)
        }
        return synthetic(request)
    }

    private fun synthetic(request: Request): Result {
        val palette = wallpapers.currentPalette()
        if (palette.isEmpty()) return Result(Tier.Painted, null, null)
        return Result(Tier.Synthetic, null, palette.toIntArray())
    }

    /**
     * Maps the widget's screen rect onto the wallpaper bitmap, which is almost never the same size
     * as the screen — launchers scroll and scale it. We crop proportionally and downsample hard
     * before blurring, because a 32px blur on a 1080px crop and the same blur on a 108px crop are
     * visually identical once scaled back up, and one of them is a hundred times cheaper.
     */
    private fun cropToWidget(source: Bitmap, rect: Rect, request: Request): Bitmap? {
        if (request.screenWidthPx <= 0 || request.screenHeightPx <= 0) return null
        val sx = source.width.toFloat() / request.screenWidthPx
        val sy = source.height.toFloat() / request.screenHeightPx
        val left = (rect.left * sx).toInt().coerceIn(0, source.width - 1)
        val top = (rect.top * sy).toInt().coerceIn(0, source.height - 1)
        val width = (rect.width() * sx).toInt().coerceAtLeast(1).coerceAtMost(source.width - left)
        val height = (rect.height() * sy).toInt().coerceAtLeast(1).coerceAtMost(source.height - top)

        val crop = runCatching { Bitmap.createBitmap(source, left, top, width, height) }.getOrNull() ?: return null
        val target = 96
        val scale = target.toFloat() / maxOf(crop.width, crop.height).coerceAtLeast(1)
        if (scale >= 1f) return crop
        return Bitmap.createScaledBitmap(
            crop,
            (crop.width * scale).toInt().coerceAtLeast(1),
            (crop.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    fun evictAll() = cache.evictAll()

    /** Supplied by :core:data. Returns null when the user's wallpaper is not one of ours. */
    interface WallpaperSource {
        fun currentId(): String?
        fun currentBitmap(): Bitmap?
        fun currentPalette(): List<Int>
    }
}

/**
 * Three-pass box blur, which converges on a Gaussian and is fast enough to run on a 96px crop
 * inside a widget update without threading gymnastics.
 *
 * RenderEffect would be the modern answer, but it only applies to a View hierarchy being drawn by
 * the system — not to an offscreen Bitmap we are rasterising ourselves — and RenderScript is
 * deprecated. On a crop this small the arithmetic is trivial, so a plain implementation with no
 * dependencies wins on every axis that matters here.
 */
internal object StackBlur {

    fun blur(source: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return source
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val r = radius.coerceAtMost(minOf(w, h) / 2).coerceAtLeast(1)
        repeat(3) {
            boxBlurHorizontal(pixels, w, h, r)
            boxBlurVertical(pixels, w, h, r)
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    private fun boxBlurHorizontal(pixels: IntArray, w: Int, h: Int, r: Int) {
        val out = IntArray(w)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var a = 0; var rr = 0; var g = 0; var b = 0; var n = 0
                for (k in -r..r) {
                    val xx = (x + k).coerceIn(0, w - 1)
                    val p = pixels[row + xx]
                    a += Color.alpha(p); rr += Color.red(p); g += Color.green(p); b += Color.blue(p); n++
                }
                out[x] = Color.argb(a / n, rr / n, g / n, b / n)
            }
            System.arraycopy(out, 0, pixels, row, w)
        }
    }

    private fun boxBlurVertical(pixels: IntArray, w: Int, h: Int, r: Int) {
        val out = IntArray(h)
        for (x in 0 until w) {
            for (y in 0 until h) {
                var a = 0; var rr = 0; var g = 0; var b = 0; var n = 0
                for (k in -r..r) {
                    val yy = (y + k).coerceIn(0, h - 1)
                    val p = pixels[yy * w + x]
                    a += Color.alpha(p); rr += Color.red(p); g += Color.green(p); b += Color.blue(p); n++
                }
                out[y] = Color.argb(a / n, rr / n, g / n, b / n)
            }
            for (y in 0 until h) pixels[y * w + x] = out[y]
        }
    }
}

/** Draws a bitmap backdrop scaled to fill, used by [GlassPainter]. */
internal fun Canvas.drawBackdrop(bitmap: Bitmap, dest: android.graphics.RectF, paint: android.graphics.Paint) {
    drawBitmap(bitmap, null, dest, paint)
}
