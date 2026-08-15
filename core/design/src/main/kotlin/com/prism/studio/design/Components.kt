package com.prism.studio.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The interaction primitives the shell is built from.
 *
 * The first build had none of these — every tile was a bare `clickable`, so a tap produced no
 * feedback at all and the whole app felt like a static list. Three small things do most of the work
 * of making an interface feel built rather than assembled: something reacts under your finger, a
 * selected thing looks selected, and motion is consistent everywhere.
 */

/**
 * Presses in slightly, springs back.
 *
 * Deliberately subtle — 3%, not 10%. A tile that visibly shrinks reads as a toy; one that barely
 * moves reads as a physical surface. Uses the shared [Motion.snappy] spring so every pressable
 * surface in the app responds identically.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = Motion.snappy(),
        label = "press",
    )
    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            // No ripple: the scale spring is the feedback, and a ripple on top of it reads as two
            // competing responses to one touch.
            indication = null,
            onClick = onClick,
        )
}

/**
 * A filter chip.
 *
 * Material3's own FilterChip is fine but visually generic; in a catalog whose whole proposition is
 * design quality, the chrome has to look considered. This one is a pill with a hairline border and
 * a filled selected state, which reads closer to the design tools this app is competing with.
 */
@Composable
fun PrismChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            .border(
                width = 0.5.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(999.dp),
            )
            .pressable(onClick = { haptics.tick(); onClick() })
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * A section header: title, count, and one line of context.
 *
 * The count matters more than it looks. "Liquid Glass · 15" tells the user there is depth behind the
 * row before they scroll it, which is the same job the competitors' "400+ WIDGETS" banner does — but
 * earned per section rather than claimed once.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = Space.base.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            count?.let {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
