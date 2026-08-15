package com.prism.studio.render

import android.graphics.Canvas
import android.graphics.RectF
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType

/**
 * Draws the foreground of one widget type inside an already-painted surface.
 *
 * There are ~24 of these — one per [WidgetType] — and together with 11 [com.prism.studio.model.ContentLayout]s
 * and 32 families they produce the whole catalog. A new design family adds a data file and zero
 * renderer code; a new widget type adds one renderer and appears across every existing family.
 */
interface ContentRenderer {
    val type: WidgetType

    /**
     * @param content the padded rectangle inside the surface. Never draw outside it.
     * @param data guaranteed to be the [WidgetData] subtype this renderer declares, or
     *   [WidgetData.Placeholder] when data is unavailable.
     */
    fun draw(canvas: Canvas, content: RectF, widget: ResolvedWidget, data: WidgetData, ctx: DrawContext)
}

/** Shared, pre-resolved drawing services. Constructed once per update batch. */
class DrawContext(
    val colors: ColorResolver,
    val typefaces: TypefaceProvider,
    val size: RenderSize,
) {
    val density: Density get() = size.density
}

/** Lookup from type to renderer. Registered in :app, injected everywhere. */
class ContentRendererRegistry(renderers: List<ContentRenderer>) {
    private val byType = renderers.associateBy { it.type }

    fun rendererFor(type: WidgetType): ContentRenderer =
        byType[type] ?: error("No ContentRenderer registered for $type")

    /** Fails fast at startup in debug builds rather than at draw time on a user's home screen. */
    fun assertComplete() {
        val missing = WidgetType.entries.filterNot { byType.containsKey(it) }
        check(missing.isEmpty()) { "Missing ContentRenderers: ${missing.joinToString()}" }
    }
}
