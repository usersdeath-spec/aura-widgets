package com.prism.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.prism.studio.render.content.BitmapSource

/**
 * Decodes album art and user-chosen photos for the two renderers that need pixels they do not own.
 *
 * Three things this does that a naive implementation would not:
 *
 * 1. **Samples during decode**, not after. `inSampleSize` means a 12-megapixel photo never fully
 *    enters memory — decoding then scaling is the difference between 48 MB and 1.5 MB per photo,
 *    and on a low-end device the difference between working and being killed.
 * 2. **Bounds the cache at 6 MB.** This process also holds the render cache; an unbounded bitmap
 *    cache is how a widget app gets reclaimed in the background and loses its widgets.
 * 3. **Never throws.** A revoked photo URI is routine — the user deleted the picture — and the
 *    renderer already draws a graceful placeholder for null.
 */
class WidgetBitmapSource(private val context: Context) : BitmapSource {

    private val cache = object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    override fun bitmap(key: String): Bitmap? {
        cache.get(key)?.let { return it }
        val decoded = decode(key) ?: return null
        cache.put(key, decoded)
        return decoded
    }

    private fun decode(key: String): Bitmap? = runCatching {
        val uri = Uri.parse(key)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    /** Targets the largest widget we can be asked to draw: 4 cells at 3x density. */
    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > TARGET_PX || height / sample > TARGET_PX) sample *= 2
        return sample
    }

    fun evictAll() = cache.evictAll()

    private companion object {
        const val TARGET_PX = 1080
    }
}
