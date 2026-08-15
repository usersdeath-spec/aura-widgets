package com.prism.studio.model

import kotlinx.serialization.Serializable

/** Stable identifier. Never renamed — it is persisted in Room and referenced by placed widgets. */
@JvmInline @Serializable value class FamilyId(val value: String)
@JvmInline @Serializable value class VariantId(val value: String)

/**
 * A design family is an authored point of view: one palette, one type treatment, one surface
 * language, one layout philosophy. Users browse families first because that is how taste works —
 * nobody wants "a battery widget", they want "the battery widget that matches the rest".
 */
@Serializable
data class DesignFamily(
    val id: FamilyId,
    val name: String,
    /** One line, shown under the family name. Written as a designer's note, not marketing copy. */
    val note: String,
    val mood: Mood,
    /** The family's signature style. Variants start here and diverge deliberately. */
    val base: WidgetStyle,
    val variants: List<WidgetVariant>,
    /** Wallpaper ids that were art-directed alongside this family. Drives "Pair with wallpaper". */
    val pairedWallpapers: List<String> = emptyList(),
) {
    init {
        require(variants.isNotEmpty()) { "Family ${id.value} has no variants" }
        require(variants.map { it.id }.toSet().size == variants.size) {
            "Family ${id.value} has duplicate variant ids"
        }
    }

    fun variant(id: VariantId): WidgetVariant =
        variants.firstOrNull { it.id == id }
            ?: error("Variant ${id.value} not found in family ${this.id.value}")
}

@Serializable
enum class Mood { Quiet, Bold, Warm, Cool, Dark, Bright, Playful, Technical }

/**
 * One widget in a family: a content type, a size, and the deltas from the family's base style.
 *
 * Variants store a *transform*, not a full style, so re-tuning a family's palette updates all
 * thirteen of its widgets at once. This is the difference between maintaining 32 families and
 * maintaining 420 unrelated designs.
 */
@Serializable
data class WidgetVariant(
    val id: VariantId,
    val name: String,
    val type: WidgetType,
    val size: WidgetSize,
    val layout: ContentLayout,
    /** Applied on top of [DesignFamily.base]. Kept small on purpose. */
    val styleDelta: StyleDelta = StyleDelta(),
)

/**
 * How a content renderer arranges its elements inside the surface. The renderer decides what to
 * draw; the layout decides where. Roughly a dozen of these cover every widget in the catalog.
 */
@Serializable
enum class ContentLayout {
    /** One enormous value, nothing else. The most-used layout in the whole app. */
    Hero,

    /** Big value with a small label above it. */
    HeroLabelled,

    /** Value left, supporting detail right, baseline-aligned. */
    Split,

    /** Value on top of a horizontal bar or arc. */
    HeroWithGauge,

    /** Circular progress with the value in the middle. */
    Ring,

    /** Vertical list of rows — agenda, todo, world clock. */
    Stack,

    /** 7-column grid — month calendar, habit tracker. */
    Grid,

    /** Full-bleed art with text overlaid on a scrim. */
    Overlay,

    /** Dial face with hands. Analog clocks only. */
    Dial,

    /** Sparkline or bar chart with a value caption. Finance, crypto, steps. */
    Chart,

    /** Icon row with tap targets. Music transport, quick actions. */
    Controls,
}

/**
 * Sparse override of [WidgetStyle]. Nulls mean "inherit". Applied by [StyleDelta.applyTo].
 */
@Serializable
data class StyleDelta(
    val surface: Surface? = null,
    val stroke: Stroke? = null,
    val clearStroke: Boolean = false,
    val glow: Glow? = null,
    val shadow: Shadow? = null,
    val clearShadow: Boolean = false,
    val cornerRadiusDp: Float? = null,
    val paddingDp: Float? = null,
    val typeScale: Float? = null,
    val letterSpacingEm: Float? = null,
    val fontFamily: FontFamilyToken? = null,
    val fontWeight: Int? = null,
    val alignment: Alignment? = null,
    val spacingDp: Float? = null,
    val ink: ColorSpec? = null,
    val inkMuted: ColorSpec? = null,
    val accent: ColorSpec? = null,
    val opacity: Float? = null,
    val motion: WidgetMotion? = null,
) {
    fun applyTo(base: WidgetStyle): WidgetStyle = base.copy(
        surface = surface ?: base.surface,
        stroke = if (clearStroke) null else stroke ?: base.stroke,
        glow = glow ?: base.glow,
        shadow = if (clearShadow) null else shadow ?: base.shadow,
        cornerRadiusDp = cornerRadiusDp ?: base.cornerRadiusDp,
        paddingDp = paddingDp ?: base.paddingDp,
        typeScale = typeScale ?: base.typeScale,
        letterSpacingEm = letterSpacingEm ?: base.letterSpacingEm,
        fontFamily = fontFamily ?: base.fontFamily,
        fontWeight = fontWeight ?: base.fontWeight,
        alignment = alignment ?: base.alignment,
        spacingDp = spacingDp ?: base.spacingDp,
        ink = ink ?: base.ink,
        inkMuted = inkMuted ?: base.inkMuted,
        accent = accent ?: base.accent,
        opacity = opacity ?: base.opacity,
        motion = motion ?: base.motion,
    )
}

/**
 * A concrete, placed widget: which variant, plus whatever the user changed in the editor.
 *
 * [userDelta] is stored separately from the resolved style so that a family restyle in a future
 * app update flows through to already-placed widgets without discarding the user's own edits.
 */
@Serializable
data class WidgetSpec(
    val family: FamilyId,
    val variant: VariantId,
    val userDelta: StyleDelta = StyleDelta(),
    /** Type-specific settings: timezone, countdown target, note id, ticker symbol, photo uri. */
    val options: Map<String, String> = emptyMap(),
) {
    fun resolve(catalog: FamilyCatalog): ResolvedWidget {
        val fam = catalog.family(family)
        val v = fam.variant(variant)
        val style = userDelta.applyTo(v.styleDelta.applyTo(fam.base))
        return ResolvedWidget(fam, v, style, options)
    }
}

/** Everything the renderer needs, with nothing left to look up. */
data class ResolvedWidget(
    val family: DesignFamily,
    val variant: WidgetVariant,
    val style: WidgetStyle,
    val options: Map<String, String>,
)

/** Read model over all authored families. Implementations live in :core:data. */
interface FamilyCatalog {
    val families: List<DesignFamily>
    fun family(id: FamilyId): DesignFamily
    fun search(query: String): List<Pair<DesignFamily, WidgetVariant>>
    fun byType(type: WidgetType): List<Pair<DesignFamily, WidgetVariant>>
}
