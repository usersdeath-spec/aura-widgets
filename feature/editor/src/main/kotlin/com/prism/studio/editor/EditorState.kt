package com.prism.studio.editor

import com.prism.studio.model.ColorSpec
import com.prism.studio.model.GradientKind
import com.prism.studio.model.GradientStop
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.StyleDelta
import com.prism.studio.model.Surface
import com.prism.studio.model.WidgetStyle
import com.prism.studio.render.ColorHarmony

/**
 * The editor's document model.
 *
 * Two decisions shape everything here:
 *
 * 1. **The document is a [StyleDelta], never a [WidgetStyle].** The user is editing the difference
 *    from their family, not a detached copy of it. Reset is one assignment, and a family restyled
 *    in a future update flows through to widgets the user already customised.
 *
 * 2. **History is a list of deltas, not a list of commands.** A delta is small, immutable, and
 *    comparable, so undo/redo is two indices into an array with no inverse operations to write and
 *    no way for the two directions to disagree. This is the same reason the render cache can key on
 *    a style fingerprint.
 */
data class EditorState(
    val widget: ResolvedWidget,
    val history: List<StyleDelta> = listOf(StyleDelta()),
    val cursor: Int = 0,
    val activePresetId: String? = null,
) {
    val delta: StyleDelta get() = history[cursor]

    /** The style the preview is currently showing. */
    val style: WidgetStyle get() = delta.applyTo(widget.style)

    val canUndo: Boolean get() = cursor > 0
    val canRedo: Boolean get() = cursor < history.lastIndex
    val isModified: Boolean get() = delta != StyleDelta()

    /**
     * Records a change.
     *
     * Redo history is dropped on a new edit, which is the behaviour every editor has and every user
     * expects. History is capped at 50 steps — deep enough that nobody hits the end while working,
     * shallow enough that the state object stays trivial to keep in a ViewModel across a rotation.
     */
    fun edit(transform: (StyleDelta) -> StyleDelta): EditorState {
        val next = transform(delta)
        if (next == delta) return this
        val trimmed = history.take(cursor + 1) + next
        val capped = if (trimmed.size > HISTORY_LIMIT) trimmed.takeLast(HISTORY_LIMIT) else trimmed
        return copy(history = capped, cursor = capped.lastIndex, activePresetId = null)
    }

    /**
     * Continuous gestures collapse into one history entry.
     *
     * Without this, dragging a radius slider once produces sixty undo steps and the undo button
     * becomes useless. Called on every frame of a drag; the first call pushes, the rest replace.
     */
    fun editContinuous(transform: (StyleDelta) -> StyleDelta): EditorState {
        val next = transform(delta)
        if (next == delta) return this
        return copy(history = history.take(cursor) + next, cursor = cursor, activePresetId = null)
    }

    fun undo(): EditorState = if (canUndo) copy(cursor = cursor - 1) else this
    fun redo(): EditorState = if (canRedo) copy(cursor = cursor + 1) else this

    /** Back to the family's own design. Recorded as a step, so it is itself undoable. */
    fun reset(): EditorState = edit { StyleDelta() }

    fun applyPreset(preset: StylePreset): EditorState =
        edit { preset.delta }.copy(activePresetId = preset.id)

    /**
     * "Match Wallpaper".
     *
     * Writes only colour fields, deliberately: someone who spent a minute tuning corner radius and
     * type weight and then taps Match Wallpaper expects new colours, not a new widget. Surfaces are
     * recoloured in place — a gradient stays a gradient, glass stays glass and only its tint moves.
     */
    fun applyScheme(scheme: ColorHarmony.Scheme): EditorState = edit { current ->
        current.copy(
            surface = recolour(style.surface, scheme.surface),
            ink = ColorSpec.Solid(scheme.ink.toLong() and 0xFFFFFFFFL),
            inkMuted = ColorSpec.Solid(scheme.inkMuted.toLong() and 0xFFFFFFFFL),
            accent = ColorSpec.Solid(scheme.accent.toLong() and 0xFFFFFFFFL),
        )
    }

    private fun recolour(surface: Surface, argb: Int): Surface {
        val spec = ColorSpec.Solid(argb.toLong() and 0xFFFFFFFFL)
        return when (surface) {
            is Surface.Solid -> Surface.Solid(spec)
            is Surface.Glass -> surface.copy(tint = spec)
            is Surface.LiquidGlass -> surface.copy(tint = spec)
            is Surface.Mesh -> surface.copy(base = spec)
            is Surface.Extruded -> surface.copy(base = spec)
            is Surface.Gradient -> surface.copy(
                stops = surface.stops.mapIndexed { i, stop ->
                    if (i == 0) GradientStop(stop.at, spec) else stop
                },
            )
            Surface.None -> Surface.None
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 50
    }
}

/**
 * A saved look.
 *
 * Built-in presets ship with the app and are the fastest path from "I like this family but not
 * that colour" to a finished widget. User presets are the same type persisted to Room, so a preset
 * the user saves behaves identically to one we authored — including being applicable across
 * families, since a delta never names a family.
 */
data class StylePreset(
    val id: String,
    val name: String,
    val delta: StyleDelta,
    val isUserCreated: Boolean = false,
) {
    companion object {
        /**
         * The built-ins.
         *
         * Six, each a *change of intent* rather than a colour swap — that is why they are worth
         * shipping over a colour picker. Every one is expressible as a small delta, which is the
         * test for whether a preset is a real idea or just a saved accident.
         */
        val BUILT_IN = listOf(
            StylePreset("preset-air", "Air", StyleDelta(opacity = 0.55f, paddingDp = 26f)),
            StylePreset("preset-solid", "Solid", StyleDelta(opacity = 1f, cornerRadiusDp = 12f)),
            StylePreset("preset-pill", "Pill", StyleDelta(cornerRadiusDp = 999f, paddingDp = 16f)),
            StylePreset("preset-loud", "Loud", StyleDelta(typeScale = 1.28f, fontWeight = 800, paddingDp = 10f)),
            StylePreset("preset-whisper", "Whisper", StyleDelta(typeScale = 0.86f, fontWeight = 300, opacity = 0.8f)),
            StylePreset("preset-edge", "Edge", StyleDelta(cornerRadiusDp = 2f, letterSpacingEm = 0.06f)),
        )
    }
}

/** Gradient editing, isolated because it is the only control with variable arity. */
object GradientEditing {

    fun stopsOf(surface: Surface): List<GradientStop> =
        (surface as? Surface.Gradient)?.stops ?: emptyList()

    /**
     * Adds a stop at [position], coloured by interpolating its neighbours.
     *
     * Inserting a stop should never change how the gradient looks — the user is adding a *handle*,
     * and the colour change is the next thing they do. Getting this wrong is the most common way a
     * gradient editor feels broken.
     */
    fun addStop(surface: Surface.Gradient, position: Float): Surface.Gradient {
        val at = position.coerceIn(0f, 1f)
        val before = surface.stops.lastOrNull { it.at <= at } ?: surface.stops.first()
        val after = surface.stops.firstOrNull { it.at >= at } ?: surface.stops.last()
        val t = if (after.at == before.at) 0f else (at - before.at) / (after.at - before.at)
        val colour = lerpSpec(before.color, after.color, t)
        return surface.copy(stops = (surface.stops + GradientStop(at, colour)).sortedBy { it.at })
    }

    /** A gradient needs two stops to exist, so the last two are not removable. */
    fun removeStop(surface: Surface.Gradient, index: Int): Surface.Gradient =
        if (surface.stops.size <= 2) surface
        else surface.copy(stops = surface.stops.filterIndexed { i, _ -> i != index })

    fun moveStop(surface: Surface.Gradient, index: Int, position: Float): Surface.Gradient =
        surface.copy(
            stops = surface.stops.mapIndexed { i, stop ->
                if (i == index) stop.copy(at = position.coerceIn(0f, 1f)) else stop
            }.sortedBy { it.at },
        )

    fun setKind(surface: Surface.Gradient, kind: GradientKind): Surface.Gradient =
        surface.copy(kind = kind)

    fun setAngle(surface: Surface.Gradient, degrees: Float): Surface.Gradient =
        surface.copy(angleDeg = ((degrees % 360f) + 360f) % 360f)

    private fun lerpSpec(a: ColorSpec, b: ColorSpec, t: Float): ColorSpec {
        val ca = (a as? ColorSpec.Solid)?.argb ?: return a
        val cb = (b as? ColorSpec.Solid)?.argb ?: return a
        fun channel(shift: Int): Long {
            val va = (ca shr shift) and 0xFF
            val vb = (cb shr shift) and 0xFF
            return (va + (vb - va) * t).toLong()
        }
        return ColorSpec.Solid(
            (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0),
        )
    }
}
