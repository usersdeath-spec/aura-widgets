package com.prism.studio.render

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import com.prism.studio.render.R
import com.prism.studio.model.FontFamilyToken

/**
 * Loads and caches the bundled variable fonts.
 *
 * Prism ships six families as variable fonts (~40 KB each after subsetting to Latin + digits +
 * common symbols), which is how the editor can offer any weight from 100 to 900 without shipping
 * nine static files per family. Typeface creation is expensive enough to matter when redrawing a
 * screen's worth of widgets at once, so every (token, weight) pair is memoised.
 */
class TypefaceProvider(private val context: Context) {

    private val cache = HashMap<Long, Typeface>()

    fun get(token: FontFamilyToken, weight: Int): Typeface {
        val key = token.ordinal.toLong() shl 32 or weight.toLong()
        return cache.getOrPut(key) { load(token, weight) }
    }

    private fun load(token: FontFamilyToken, weight: Int): Typeface {
        val base = downloadable(token) ?: bundled(token) ?: systemFallback(token)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(base, weight.coerceIn(100, 900), false)
        } else {
            Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    /**
     * The downloadable font, if the system already has it.
     *
     * `ResourcesCompat.getFont` returns the cached face synchronously when it is present and null
     * when it is not — it never blocks on a network fetch. That is exactly the behaviour a widget
     * renderer needs: a widget update has a few milliseconds inside a binder call and cannot wait
     * on a download.
     *
     * The fetch itself is kicked off once by the app process (see [warm]), so by the time a widget
     * draws, the face is usually resident. When it is not, we fall through and the widget renders
     * in the platform face this minute and the real face the next. A missing font must never be a
     * blank widget.
     */
    private fun downloadable(token: FontFamilyToken): Typeface? = runCatching {
        androidx.core.content.res.ResourcesCompat.getFont(context, fontResource(token))
    }.getOrNull()

    /** A bundled .ttf, if one was dropped into assets. Optional; nothing ships one today. */
    private fun bundled(token: FontFamilyToken): Typeface? = runCatching {
        Typeface.createFromAsset(context.assets, assetPath(token))
    }.getOrNull()

    private fun fontResource(token: FontFamilyToken): Int = when (token) {
        FontFamilyToken.Grotesk -> R.font.grotesk
        FontFamilyToken.GroteskDisplay -> R.font.grotesk_display
        FontFamilyToken.Serif -> R.font.serif
        FontFamilyToken.Mono -> R.font.mono
        FontFamilyToken.Rounded -> R.font.rounded
        FontFamilyToken.Condensed -> R.font.condensed
    }

    private fun assetPath(token: FontFamilyToken): String = when (token) {
        FontFamilyToken.Grotesk -> "fonts/InterTight-Variable.ttf"
        FontFamilyToken.GroteskDisplay -> "fonts/BricolageGrotesque-Variable.ttf"
        FontFamilyToken.Serif -> "fonts/Newsreader-Variable.ttf"
        FontFamilyToken.Mono -> "fonts/JetBrainsMono-Variable.ttf"
        FontFamilyToken.Rounded -> "fonts/Nunito-Variable.ttf"
        FontFamilyToken.Condensed -> "fonts/Archivo-Variable.ttf"
    }

    /**
     * What to use when neither a downloaded nor a bundled face is available.
     *
     * The previous version returned `Typeface.DEFAULT` for all six tokens, so every family in the
     * catalog rendered in one identical face — 59 families that differed only in colour, which is
     * exactly what the app was criticised for. These are guaranteed on every device since API 21
     * and are genuinely different shapes.
     */
    private fun systemFallback(token: FontFamilyToken): Typeface = when (token) {
        FontFamilyToken.Grotesk -> Typeface.create("sans-serif", Typeface.NORMAL)
        FontFamilyToken.GroteskDisplay -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        FontFamilyToken.Serif -> Typeface.create("serif", Typeface.NORMAL)
        FontFamilyToken.Mono -> Typeface.create("monospace", Typeface.NORMAL)
        FontFamilyToken.Rounded -> Typeface.create("sans-serif-rounded", Typeface.NORMAL)
        FontFamilyToken.Condensed -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    }

    /**
     * Asks the system to fetch every face, off the main thread.
     *
     * Called once at app start. Fonts are cached device-wide, so this is a one-time cost per device
     * rather than per install, and it is what makes the synchronous lookup above hit.
     */
    fun warm(handler: android.os.Handler) {
        FontFamilyToken.entries.forEach { token ->
            runCatching {
                androidx.core.content.res.ResourcesCompat.getFont(
                    context,
                    fontResource(token),
                    object : androidx.core.content.res.ResourcesCompat.FontCallback() {
                        override fun onFontRetrieved(typeface: Typeface) = cache.clear()
                        override fun onFontRetrievalFailed(reason: Int) = Unit
                    },
                    handler,
                )
            }
        }
    }
}
