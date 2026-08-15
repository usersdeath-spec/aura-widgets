package com.prism.studio.model

import kotlinx.serialization.Serializable

/**
 * The complete visual description of a widget. Serializable, comparable, and hashable — the hash
 * doubles as the render cache key, so an unchanged style never re-rasterises.
 *
 * Every user-facing customisation control in the editor maps to exactly one field here. Adding a
 * control means adding a field, a default, and a slider; nothing else in the codebase changes.
 */
@Serializable
data class WidgetStyle(
    val surface: Surface = Surface.Solid(Palette.Ink),
    val stroke: Stroke? = null,
    val glow: Glow? = null,
    val shadow: Shadow? = null,
    val cornerRadiusDp: Float = 24f,
    val paddingDp: Float = 16f,
    /** Multiplier applied to the variant's intrinsic type scale. 0.8..1.4 in the editor. */
    val typeScale: Float = 1f,
    /** Extra tracking in em. Negative tightens — the Minimal Mono family leans on this. */
    val letterSpacingEm: Float = 0f,
    val fontFamily: FontFamilyToken = FontFamilyToken.Grotesk,
    val fontWeight: Int = 500,
    val alignment: Alignment = Alignment.Start,
    /** Vertical rhythm between stacked content rows, in dp. */
    val spacingDp: Float = 8f,
    val ink: ColorSpec = ColorSpec.Solid(Palette.Mist),
    /** Secondary ink for labels, units, and de-emphasised rows. */
    val inkMuted: ColorSpec = ColorSpec.Solid(Palette.MistDim),
    val accent: ColorSpec = ColorSpec.Solid(Palette.Violet),
    /** 0f = fully transparent widget, 1f = opaque. Applied last, to the composited bitmap. */
    val opacity: Float = 1f,
    val motion: WidgetMotion = WidgetMotion.None,
) {
    init {
        require(opacity in 0f..1f) { "opacity must be 0..1, was $opacity" }
        require(typeScale in 0.5f..2f) { "typeScale must be 0.5..2, was $typeScale" }
        require(cornerRadiusDp >= 0f) { "cornerRadiusDp must be >= 0" }
    }

    /** Cache key for the rasteriser. Size and data are mixed in by the renderer. */
    val fingerprint: Int get() = hashCode()
}

/** The widget's background layer. */
@Serializable
sealed interface Surface {
    @Serializable
    data object None : Surface

    @Serializable
    data class Solid(val color: ColorSpec) : Surface {
        constructor(argb: Long) : this(ColorSpec.Solid(argb))
    }

    @Serializable
    data class Gradient(
        val stops: List<GradientStop>,
        val angleDeg: Float = 135f,
        val kind: GradientKind = GradientKind.Linear,
    ) : Surface

    /**
     * Frosted glass. Android widgets cannot sample the wallpaper behind them, so we approximate:
     * a translucent fill + a light-source highlight + optional noise grain. The illusion holds
     * because [Stroke.Hairline] catches the edge the way real glass does.
     */
    @Serializable
    data class Glass(
        val tint: ColorSpec,
        val fillAlpha: Float = 0.18f,
        val highlightAlpha: Float = 0.28f,
        val grainAlpha: Float = 0.04f,
        val refraction: Float = 0f,
    ) : Surface

    /** Soft off-axis colour blobs behind a dark base. Powers Aurora, Cosmic, and Abstract. */
    @Serializable
    data class Mesh(
        val base: ColorSpec,
        val blobs: List<MeshBlob>,
        val blurDp: Float = 48f,
    ) : Surface

    /**
     * The flagship surface: layered translucent glass with a real backdrop where the platform
     * allows one.
     *
     * Distinct from [Glass], which is a single translucent plate. Liquid Glass is built from
     * stacked layers — backdrop, body tint, specular sweep, inner edge light, caustic — and the
     * renderer degrades it in tiers rather than switching it off. See `GlassPainter` for the tier
     * rules and `Backdrop` for how a genuine blur is obtained without wallpaper-read permission.
     *
     * @property depth how far the plate sits above the wallpaper, 0..1. Drives shadow spread,
     *   specular sharpness, and backdrop blur radius together, so one control moves the whole
     *   illusion coherently.
     * @property specularAngleDeg where the light source sits. Fixed per family, never per variant,
     *   so a screen full of glass widgets is lit from one direction.
     * @property causticAlpha the bright pool that gathers at the lower edge of a thick plate.
     * @property innerShadowAlpha darkening just inside the far edge — what actually reads as
     *   thickness rather than a sticker.
     */
    @Serializable
    data class LiquidGlass(
        val tint: ColorSpec,
        val depth: Float = 0.5f,
        val bodyAlpha: Float = 0.16f,
        val specularAlpha: Float = 0.34f,
        val specularAngleDeg: Float = 315f,
        val causticAlpha: Float = 0.12f,
        val innerShadowAlpha: Float = 0.18f,
        val grainAlpha: Float = 0.03f,
        /** Edge bending. Above ~0.4 the plate starts reading as a lens rather than a pane. */
        val refraction: Float = 0.25f,
    ) : Surface {
        init {
            require(depth in 0f..1f) { "depth must be 0..1" }
        }

        /** Backdrop blur radius in dp, derived from depth. Thicker glass blurs more. */
        val blurRadiusDp: Float get() = 8f + depth * 24f
    }

    /** Two-tone extruded surface — the Neumorphism family and nothing else. */
    @Serializable
    data class Extruded(
        val base: ColorSpec,
        val depthDp: Float = 6f,
        val inset: Boolean = false,
    ) : Surface
}

@Serializable data class GradientStop(val at: Float, val color: ColorSpec)
@Serializable enum class GradientKind { Linear, Radial, Sweep }
@Serializable data class MeshBlob(val x: Float, val y: Float, val radius: Float, val color: ColorSpec)

@Serializable
data class Stroke(val widthDp: Float, val color: ColorSpec, val gradient: List<GradientStop>? = null) {
    companion object {
        /** 0.5dp hairline — the single most effective "expensive" cue on a home screen. */
        fun hairline(color: ColorSpec) = Stroke(0.5f, color)
    }
}

@Serializable data class Glow(val radiusDp: Float, val color: ColorSpec, val alpha: Float = 0.6f)
@Serializable data class Shadow(val radiusDp: Float, val dy: Float, val color: ColorSpec, val alpha: Float = 0.35f)

@Serializable
sealed interface ColorSpec {
    @Serializable data class Solid(val argb: Long) : ColorSpec

    /** Resolves against the device's Material You palette at render time. */
    @Serializable data class Dynamic(val role: DynamicRole, val fallback: Long) : ColorSpec

    /** Sampled from the current wallpaper — how a widget "belongs" to its background. */
    @Serializable data class FromWallpaper(val slot: WallpaperSlot, val fallback: Long) : ColorSpec
}

@Serializable enum class DynamicRole { Primary, OnPrimary, Secondary, Tertiary, Surface, OnSurface, Outline }
@Serializable enum class WallpaperSlot { Dominant, Vibrant, Muted, DarkMuted, LightVibrant }

@Serializable enum class FontFamilyToken { Grotesk, GroteskDisplay, Serif, Mono, Rounded, Condensed }
@Serializable enum class Alignment { Start, Center, End }

/**
 * Widgets cannot animate freely — RemoteViews only permits a handful of transitions. These are the
 * ones that are actually cheap and actually look good.
 */
@Serializable
enum class WidgetMotion {
    None,

    /** Cross-fades between two ImageViews on data change. ~0 battery cost. */
    CrossFade,

    /** Digits slide vertically when the minute rolls over. Clock families only. */
    DigitRoll,

    /** Progress arcs animate their sweep on redraw. Battery, steps, habits. */
    ArcSweep,
}

/** Base palette. Families override freely; these are the fallbacks and the app-chrome colours. */
object Palette {
    const val Ink = 0xFF0B0D12L
    const val InkSoft = 0xFF141821L
    const val Mist = 0xFFE8E9EDL
    const val MistDim = 0x99E8E9EDL
    const val Violet = 0xFF7C5CFFL
    const val Cyan = 0xFF3FD8E0L
    const val Amber = 0xFFFFB35CL
    const val Pure = 0xFFFFFFFFL
    const val Void = 0xFF000000L
}
