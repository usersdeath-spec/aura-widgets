package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * Authoring DSL for design families.
 *
 * Every family in Prism is a *complete ecosystem*: a user who picks a family must find a matching
 * widget for each of the seven things people actually put on a home screen. [Core] makes that
 * structural rather than a matter of discipline — a family cannot be constructed without deciding
 * how it handles all seven, and [CatalogIntegrityTest] fails the build if one is missing.
 *
 * The seven pillars, and why these seven:
 *   Clock        — the most-placed widget in every customisation app, by a wide margin.
 *   Weather      — the second.
 *   Calendar     — the reason people open their home screen rather than an app.
 *   Battery      — the most-placed *system* widget.
 *   Music        — the only widget people touch rather than read.
 *   Notes        — the "why is this app on my phone" widget for a large minority.
 *   Productivity — a to-do list; the thing that turns a pretty screen into a used one.
 *
 * Everything past the seven is [extras]: the designs that make a family feel authored rather than
 * generated. Families carry 3–8 of them, chosen to suit the family's own idea, never to hit a count.
 */
internal fun family(
    id: String,
    name: String,
    note: String,
    mood: Mood,
    base: WidgetStyle,
    wallpapers: List<String>,
    core: Core,
    extras: List<WidgetVariant> = emptyList(),
): DesignFamily {
    val variants = core.build(id) + extras
    require(variants.size in 10..15) {
        "Family $id has ${variants.size} variants; the shelf reads badly outside 10..15"
    }
    return DesignFamily(
        id = FamilyId(id),
        name = name,
        note = note,
        mood = mood,
        base = base,
        variants = variants,
        pairedWallpapers = wallpapers,
    )
}

/**
 * The seven pillars, expressed as the layout decision each family makes for them.
 *
 * A family's personality lives in these choices as much as in its palette: Swiss Grid puts its
 * clock in [ContentLayout.Split] because a grid wants two aligned blocks, while Luxury Gold uses
 * [ContentLayout.Hero] because jewellery wants one object. Sizes are fixed per pillar so that a
 * user switching families keeps the same home-screen geometry.
 */
internal data class Core(
    val clock: ContentLayout = ContentLayout.HeroLabelled,
    val clockSize: WidgetSize = WidgetSize.Wide,
    val clockDelta: StyleDelta = StyleDelta(),
    val weather: ContentLayout = ContentLayout.Split,
    val weatherDelta: StyleDelta = StyleDelta(),
    val calendar: ContentLayout = ContentLayout.Grid,
    val calendarDelta: StyleDelta = StyleDelta(),
    val battery: ContentLayout = ContentLayout.HeroWithGauge,
    val batterySize: WidgetSize = WidgetSize.Small,
    val batteryDelta: StyleDelta = StyleDelta(),
    val music: ContentLayout = ContentLayout.Controls,
    val musicDelta: StyleDelta = StyleDelta(),
    val notes: ContentLayout = ContentLayout.Stack,
    val notesDelta: StyleDelta = StyleDelta(),
    val todo: ContentLayout = ContentLayout.Stack,
    val todoDelta: StyleDelta = StyleDelta(),
) {
    fun build(familyId: String): List<WidgetVariant> = listOf(
        WidgetVariant(VariantId("$familyId-clock"), "Clock", WidgetType.DigitalClock, clockSize, clock, clockDelta),
        WidgetVariant(VariantId("$familyId-weather"), "Weather", WidgetType.Weather, WidgetSize.Wide, weather, weatherDelta),
        WidgetVariant(VariantId("$familyId-month"), "Month", WidgetType.MonthCalendar, WidgetSize.Large, calendar, calendarDelta),
        WidgetVariant(VariantId("$familyId-battery"), "Battery", WidgetType.Battery, batterySize, battery, batteryDelta),
        WidgetVariant(VariantId("$familyId-music"), "Now Playing", WidgetType.MusicPlayer, WidgetSize.Wide, music, musicDelta),
        WidgetVariant(VariantId("$familyId-note"), "Note", WidgetType.Notes, WidgetSize.Wide, notes, notesDelta),
        WidgetVariant(VariantId("$familyId-todo"), "Tasks", WidgetType.Todo, WidgetSize.Tall, todo, todoDelta),
    )

    companion object {
        /** The seven types every family must cover. Asserted in tests, not just documented. */
        val PILLARS = listOf(
            WidgetType.DigitalClock, WidgetType.Weather, WidgetType.MonthCalendar,
            WidgetType.Battery, WidgetType.MusicPlayer, WidgetType.Notes, WidgetType.Todo,
        )
    }
}

/** Terse constructor for the family-specific extras. */
internal fun v(
    id: String,
    name: String,
    type: WidgetType,
    size: WidgetSize,
    layout: ContentLayout,
    delta: StyleDelta = StyleDelta(),
) = WidgetVariant(VariantId(id), name, type, size, layout, delta)

// ---------------------------------------------------------------------------------------------
// Colour shorthand. Long hex literals are the bulk of an authored family, so they get one letter.
// ---------------------------------------------------------------------------------------------

internal fun c(argb: Long): ColorSpec = ColorSpec.Solid(argb)
internal fun dyn(role: DynamicRole, fallback: Long): ColorSpec = ColorSpec.Dynamic(role, fallback)
internal fun wall(slot: WallpaperSlot, fallback: Long): ColorSpec = ColorSpec.FromWallpaper(slot, fallback)

internal fun solid(argb: Long): Surface = Surface.Solid(c(argb))

internal fun grad(vararg pairs: Pair<Float, Long>, angle: Float = 135f, kind: GradientKind = GradientKind.Linear) =
    Surface.Gradient(pairs.map { GradientStop(it.first, c(it.second)) }, angle, kind)

internal fun glass(tint: ColorSpec, fill: Float = 0.18f, highlight: Float = 0.28f, grain: Float = 0.035f) =
    Surface.Glass(tint, fill, highlight, grain)

internal fun mesh(base: Long, vararg blobs: Triple<Float, Float, Long>, radius: Float = 0.55f, blur: Float = 48f) =
    Surface.Mesh(c(base), blobs.map { MeshBlob(it.first, it.second, radius, c(it.third)) }, blur)

internal fun hairline(argb: Long, width: Float = 0.5f) = Stroke(width, c(argb))
internal fun edge(width: Float, vararg pairs: Pair<Float, Long>) =
    Stroke(width, c(pairs.first().second), pairs.map { GradientStop(it.first, c(it.second)) })

internal fun shadow(radius: Float, dy: Float, alpha: Float = 0.3f, argb: Long = 0xFF000000) =
    Shadow(radius, dy, c(argb), alpha)

internal fun glow(radius: Float, argb: Long, alpha: Float = 0.6f) = Glow(radius, c(argb), alpha)
