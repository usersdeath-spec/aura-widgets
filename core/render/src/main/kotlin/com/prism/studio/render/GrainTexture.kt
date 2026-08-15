package com.prism.studio.render

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.random.Random

/**
 * A single 128x128 tileable noise bitmap, generated once and reused by every glass surface in the
 * app. Roughly 64 KB resident; regenerating it per widget would be one of the easiest ways to
 * make a widget app feel slow, so we don't.
 */
object GrainTexture {
    private var cached: Bitmap? = null

    @Synchronized
    fun get(density: Density): Bitmap = cached ?: build().also { cached = it }

    private fun build(): Bitmap {
        val n = 128
        val pixels = IntArray(n * n)
        val rng = Random(20260805)
        for (i in pixels.indices) {
            val v = rng.nextInt(96, 160)
            pixels[i] = Color.argb(255, v, v, v)
        }
        return Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888)
    }
}
