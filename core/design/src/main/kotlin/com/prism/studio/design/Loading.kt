package com.prism.studio.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Waiting states.
 *
 * Two rules, both learned from how a widget catalog actually behaves:
 *
 * 1. **Skeletons only where a real wait exists.** Widget previews rasterise in a few milliseconds,
 *    so a skeleton there would flash and be worse than nothing. Skeletons appear for wallpaper
 *    decoding and first catalog build, and nowhere else.
 * 2. **Progressive reveal beats spinners.** A catalog shelf shows each preview the instant its
 *    bitmap is ready, fading in over [Motion.quick]. The screen assembles rather than blocking,
 *    which reads as fast even when total time is unchanged.
 */

/**
 * A shimmer that travels at a fixed screen-space rate rather than a fixed duration.
 *
 * Duration-based shimmer makes small placeholders look frantic and large ones look stalled, because
 * the highlight crosses different distances in the same time. Deriving duration from width keeps
 * the perceived speed constant across a 2×2 tile and a full-bleed wallpaper card.
 */
@Composable
fun Modifier.shimmer(widthDp: Int = 240): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val durationMs = (widthDp * 4).coerceIn(700, 2200)
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
    val widthPx = widthDp * 3f

    return background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(progress * widthPx, 0f),
            end = Offset((progress + 0.6f) * widthPx, 0f),
        ),
    )
}

/** Placeholder shaped like the widget it is standing in for, so the layout never jumps. */
@Composable
fun WidgetSkeleton(aspectRatio: Float, cornerRadiusDp: Int = 20, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .then(Modifier.shimmer()),
    )
}

/**
 * Empty states are invitations, not apologies.
 *
 * Three parts, always: what this space is for, why it is empty right now, and one action that fills
 * it. No illustration of a sad box, no "Nothing here yet" full stop — an empty favourites screen
 * that doesn't tell you how to favourite something is a dead end.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space.section.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.tight.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
