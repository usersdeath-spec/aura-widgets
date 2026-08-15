package com.prism.studio.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prism.studio.design.WidgetPreview
import com.prism.studio.design.Motion
import com.prism.studio.design.Space
import com.prism.studio.design.rememberHaptics
import com.prism.studio.model.Alignment as InkAlignment
import com.prism.studio.model.FontFamilyToken
import com.prism.studio.model.Shadow
import com.prism.studio.model.ColorSpec
import com.prism.studio.render.ColorHarmony
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.WidgetData
import kotlin.math.roundToInt

/**
 * The editor.
 *
 * Structured like a design tool rather than a settings screen, which comes down to three things:
 *
 * 1. **The preview is pinned, not scrolled past.** It occupies the top third permanently. Every
 *    control the user touches is visible at the same time as its effect — the single largest
 *    difference between this and a list of preference rows.
 * 2. **Controls are grouped by intent** (Colour, Surface, Shape, Type), in the order people
 *    actually adjust things, and each group fits without scrolling.
 * 3. **Every change is undoable and nothing is committed until Apply.** The user can be reckless,
 *    which is what makes an editor feel like a tool instead of a form.
 *
 * Live rendering goes through [PrismRenderer.draw], bypassing the bitmap cache — a widget-sized
 * redraw is comfortably inside a frame on a mid-range device, so slider drags are continuous rather
 * than stepped.
 */
@Composable
fun EditorScreen(
    state: EditorState,
    sampleData: WidgetData,
    renderer: PrismRenderer,
    wallpaperSchemes: List<ColorHarmony.Scheme>,
    userPresets: List<StylePreset>,
    onState: (EditorState) -> Unit,
    onSavePreset: (String) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Colour", "Surface", "Shape", "Type")

    Column(modifier.fillMaxSize()) {

        // ---- Pinned preview -------------------------------------------------------------------
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Space.loose.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetPreview(
                widget = state.widget.copy(style = state.style),
                data = sampleData,
                renderer = renderer,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }

        // ---- Toolbar --------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.base.dp, vertical = Space.tight.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { haptics.tick(); onState(state.undo()) },
                enabled = state.canUndo,
            ) { Text("Undo") }

            TextButton(
                onClick = { haptics.tick(); onState(state.redo()) },
                enabled = state.canRedo,
            ) { Text("Redo") }

            Box(Modifier.weight(1f))

            // Reset only appears once there is something to reset — a permanently-disabled control
            // is clutter that teaches people to ignore that corner of the screen.
            AnimatedVisibility(
                visible = state.isModified,
                enter = fadeIn(Motion.enterSpec()),
                exit = fadeOut(Motion.exitSpec()),
            ) {
                TextButton(onClick = { haptics.select(); onState(state.reset()) }) { Text("Reset") }
            }
        }

        // ---- Presets --------------------------------------------------------------------------
        PresetRow(
            presets = StylePreset.BUILT_IN + userPresets,
            activeId = state.activePresetId,
            onPick = { haptics.select(); onState(state.applyPreset(it)) },
            onSave = { onSavePreset(it) },
            canSave = state.isModified,
        )

        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { haptics.tick(); tab = i },
                    text = { Text(label) },
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(Space.base.dp),
            verticalArrangement = Arrangement.spacedBy(Space.base.dp),
        ) {
            when (tab) {
                0 -> ColourControls(state, wallpaperSchemes, onState)
                1 -> SurfaceControls(state, onState)
                2 -> ShapeControls(state, onState)
                else -> TypeControls(state, onState)
            }
        }

        Box(Modifier.weight(1f))

        Button(
            onClick = { haptics.confirm(); onApply() },
            modifier = Modifier.fillMaxWidth().padding(Space.base.dp),
        ) { Text("Add to home screen") }
    }
}

/**
 * "Match Wallpaper" as four labelled options rather than one magic button.
 *
 * The harmony engine can produce four defensible schemes from the same wallpaper, and which one is
 * right is taste, not correctness. Showing them as chips with plain-language labels — "Blends in",
 * "Stands out" — lets the user pick an *intent*; showing one result would just make them tap it
 * repeatedly hoping for a different answer.
 */
@Composable
private fun ColourControls(
    state: EditorState,
    schemes: List<ColorHarmony.Scheme>,
    onState: (EditorState) -> Unit,
) {
    val haptics = rememberHaptics()

    if (schemes.isNotEmpty()) {
        Text("Match your wallpaper", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
        ) {
            schemes.forEach { scheme ->
                FilterChip(
                    selected = false,
                    onClick = { haptics.select(); onState(state.applyScheme(scheme)) },
                    label = { Text(scheme.harmony.label) },
                )
            }
        }
        Text(
            schemes.first().harmony.blurb,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Detented("Opacity", state.style.opacity, 0.2f..1f, onState = onState, state = state) { d, v ->
        d.copy(opacity = v)
    }
}

@Composable
private fun SurfaceControls(state: EditorState, onState: (EditorState) -> Unit) {
    // Blur is only meaningful on the glass surfaces; showing a dead slider elsewhere would teach
    // the user that controls in this app sometimes do nothing.
    val glass = state.style.surface as? com.prism.studio.model.Surface.LiquidGlass
    if (glass != null) {
        Text("Glass", style = MaterialTheme.typography.titleMedium)
        Detented("Depth", glass.depth, 0f..1f, onState = onState, state = state) { d, v ->
            d.copy(surface = glass.copy(depth = v))
        }
        Detented("Body", glass.bodyAlpha, 0.05f..0.5f, onState = onState, state = state) { d, v ->
            d.copy(surface = glass.copy(bodyAlpha = v))
        }
        Detented("Highlight", glass.specularAlpha, 0f..0.6f, onState = onState, state = state) { d, v ->
            d.copy(surface = glass.copy(specularAlpha = v))
        }
    }

    val shadow = state.style.shadow ?: Shadow(12f, 4f, ColorSpec.Solid(0xFF000000), 0.25f)
    Text("Shadow", style = MaterialTheme.typography.titleMedium)
    Detented("Spread", shadow.radiusDp, 0f..40f, "dp", onState, state) { d, v ->
        d.copy(shadow = shadow.copy(radiusDp = v))
    }
    Detented("Offset", shadow.dy, 0f..20f, "dp", onState, state) { d, v ->
        d.copy(shadow = shadow.copy(dy = v))
    }
    Detented("Strength", shadow.alpha, 0f..0.7f, "", onState, state) { d, v ->
        d.copy(shadow = shadow.copy(alpha = v))
    }
}

@Composable
private fun ShapeControls(state: EditorState, onState: (EditorState) -> Unit) {
    Detented("Corner radius", state.style.cornerRadiusDp, 0f..48f, "dp", onState, state) { d, v ->
        d.copy(cornerRadiusDp = v)
    }
    Detented("Padding", state.style.paddingDp, 4f..40f, "dp", onState, state) { d, v ->
        d.copy(paddingDp = v)
    }
    Detented("Spacing", state.style.spacingDp, 2f..24f, "dp", onState, state) { d, v ->
        d.copy(spacingDp = v)
    }
}

@Composable
private fun TypeControls(state: EditorState, onState: (EditorState) -> Unit) {
    val haptics = rememberHaptics()

    Text("Typeface", style = MaterialTheme.typography.titleMedium)
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
    ) {
        FontFamilyToken.entries.forEach { token ->
            FilterChip(
                selected = state.style.fontFamily == token,
                onClick = { haptics.select(); onState(state.edit { it.copy(fontFamily = token) }) },
                label = { Text(token.name) },
            )
        }
    }

    Detented("Size", state.style.typeScale, 0.7f..1.4f, "x", onState, state) { d, v ->
        d.copy(typeScale = v)
    }
    Detented("Weight", state.style.fontWeight.toFloat(), 200f..800f, "", onState, state, steps = 6) { d, v ->
        d.copy(fontWeight = (v / 100).roundToInt() * 100)
    }
    Detented("Tracking", state.style.letterSpacingEm, -0.08f..0.16f, "em", onState, state) { d, v ->
        d.copy(letterSpacingEm = v)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.tight.dp)) {
        InkAlignment.entries.forEach { align ->
            FilterChip(
                selected = state.style.alignment == align,
                onClick = { haptics.select(); onState(state.edit { it.copy(alignment = align) }) },
                label = { Text(align.name) },
            )
        }
    }
}

/**
 * A slider that ticks.
 *
 * Two things make this feel like a tool rather than a form control. First, the whole drag is one
 * undo step — [EditorState.editContinuous] during the gesture, a single commit on release. Second,
 * a haptic tick each time the value crosses a round number, which is the tactile equivalent of a
 * ruler's markings and makes it possible to land on 24dp without staring at the number.
 */
@Composable
private fun Detented(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    onState: (EditorState) -> Unit,
    state: EditorState,
    steps: Int = 0,
    transform: (com.prism.studio.model.StyleDelta, Float) -> com.prism.studio.model.StyleDelta,
) {
    val haptics = rememberHaptics()
    var lastDetent by remember { mutableStateOf(detentOf(value, range)) }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                format(value, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            valueRange = range,
            steps = steps,
            onValueChange = { next ->
                val detent = detentOf(next, range)
                if (detent != lastDetent) { haptics.tick(); lastDetent = detent }
                onState(state.editContinuous { transform(it, next) })
            },
            onValueChangeFinished = {
                // Commit the gesture as one history entry.
                onState(state.edit { transform(it, value) })
            },
            colors = SliderDefaults.colors(),
        )
    }
}

private fun detentOf(value: Float, range: ClosedFloatingPointRange<Float>): Int {
    val span = range.endInclusive - range.start
    return ((value - range.start) / span * 20f).roundToInt()
}

private fun format(value: Float, unit: String): String = when {
    unit == "dp" || unit.isEmpty() && value > 10f -> "${value.roundToInt()}$unit"
    else -> "%.2f%s".format(value, unit)
}

/**
 * Presets, including the user's own.
 *
 * "Save current" appears only when there is something worth saving, and a user preset behaves
 * identically to a built-in one — including applying across families, since a delta never names a
 * family. Someone can build a look on Liquid Glass and drop it onto Terracotta.
 */
@Composable
private fun PresetRow(
    presets: List<StylePreset>,
    activeId: String?,
    onPick: (StylePreset) -> Unit,
    onSave: (String) -> Unit,
    canSave: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.base.dp, vertical = Space.tight.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
    ) {
        presets.forEach { preset ->
            FilterChip(
                selected = preset.id == activeId,
                onClick = { onPick(preset) },
                label = { Text(preset.name) },
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            )
        }
        AnimatedVisibility(
            visible = canSave,
            enter = fadeIn(Motion.enterSpec()),
            exit = fadeOut(Motion.exitSpec()),
        ) {
            TextButton(onClick = { onSave("My style") }) { Text("Save current") }
        }
    }
}
