package com.prism.studio.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.render.ColorResolver
import com.prism.studio.render.Density
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.RenderSize
import com.prism.studio.render.WidgetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * A widget, rendered into Compose.
 *
 * THE BACKDROP IS NOT DECORATION. A widget is designed to sit on a wallpaper, and a large part of
 * the catalog is transparent by construction: Minimal Mono has no surface at all, and the seven
 * glass families are translucent. Drawn on a flat card, Minimal Mono's white type on a light card
 * is invisible, and glass has nothing to be glass *over* — the whole family reads as a grey
 * rectangle. Every preview therefore gets a backdrop standing in for the wallpaper it was art-
 * directed against.
 *
 * This was the single biggest reason the catalog looked wrong on device: the widgets were fine, the
 * surface behind them was missing.
 */
@Composable
fun WidgetPreview(
    widget: ResolvedWidget,
    data: WidgetData,
    renderer: PrismRenderer,
    modifier: Modifier = Modifier,
    backdrop: Brush? = null,
    cornerRadiusDp: Int = 18,
) {
    val context = LocalContext.current
    var bitmap by remember(widget.style.fingerprint, widget.variant.id) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    var sizePx by remember { mutableStateOf(0 to 0) }

    LaunchedEffect(widget.style.fingerprint, widget.variant.id, sizePx, data) {
        val (w, h) = sizePx
        if (w == 0 || h == 0) return@LaunchedEffect
        // Throttled to four concurrent rasterisations. A fast fling through 59 shelves otherwise
        // launches a render for every tile it passes, and dozens of simultaneous bitmap allocations
        // is the other half of the scroll crash: capping the size lowered each one's cost, this
        // caps how many exist at once.
        bitmap = withContext(Dispatchers.Default) {
            renderGate.withPermit {
            renderer.render(
                widget = widget,
                data = data,
                // Capped deliberately. At full device density a shelf tile rasterises to roughly
                // 900 KB, and a fast scroll through 59 families allocates dozens of them before the
                // cache can evict — which is the out-of-memory crash seen while scrolling. A preview
                // is displayed at ~92dp tall; rendering it above 1.5x buys no visible quality and
                // costs four times the memory.
                size = RenderSize(
                    widthPx = w.coerceAtMost(MAX_PREVIEW_PX),
                    heightPx = h.coerceAtMost(MAX_PREVIEW_PX),
                    density = Density(context.resources.displayMetrics.density.coerceAtMost(1.5f)),
                ),
                colors = ColorResolver.forDevice(context),
                dataFingerprint = "preview",
            )
            }
        }
    }

    // Fade in as each bitmap arrives rather than blocking the shelf on all of them. The screen
    // assembles, which reads as fast even when the total time is unchanged.
    val ready by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = Motion.enterSpec(Motion.quick),
        label = "previewFade",
    )

    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .then(if (backdrop != null) Modifier.background(backdrop) else Modifier)
            .onSizeChanged { sizePx = it.width to it.height },
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "${widget.family.name}, ${widget.variant.name}",
                modifier = Modifier.fillMaxSize().alpha(ready),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/**
 * Ceiling on a preview's rasterised size. Home-screen widgets are unaffected — they render at full
 * density through the widget provider, which draws a handful at a time rather than a scrolling wall.
 */
private const val MAX_PREVIEW_PX = 480

/** Shared across every preview on screen; see the note at the render call. */
private val renderGate = Semaphore(4)
