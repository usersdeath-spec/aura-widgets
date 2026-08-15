package com.prism.studio.render

/**
 * Widgets are rasterised at a fixed pixel size chosen by the host, so everything authored in dp
 * is converted through one place. Keeping this explicit (rather than leaning on Resources) means
 * the renderer stays testable on the JVM and identical between preview and widget.
 */
@JvmInline
value class Density(val scale: Float) {
    fun dp(value: Float): Float = value * scale
    fun sp(value: Float): Float = value * scale   // widgets ignore font scaling by design; see docs
    companion object {
        /** Previews render at a fixed 3x and are downscaled by the image loader. */
        val Preview = Density(3f)
    }
}

/** Target canvas in pixels, plus the density needed to interpret the style. */
data class RenderSize(val widthPx: Int, val heightPx: Int, val density: Density) {
    init {
        require(widthPx > 0 && heightPx > 0) { "RenderSize must be positive: ${widthPx}x$heightPx" }
    }
    val shortestSide: Int get() = minOf(widthPx, heightPx)
}
