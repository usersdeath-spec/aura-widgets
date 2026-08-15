package com.prism.studio.model

import kotlinx.serialization.Serializable

/**
 * A complete home screen, not a widget.
 *
 * The insight behind this feature: almost nobody wants "a clock widget". They want their phone to
 * look a particular way, and assembling that out of a catalog is work most people won't finish.
 * A setup is the finished result — wallpaper, seven matched widgets, and their positions — applied
 * in one confirm.
 *
 * Setups are *derived*, not duplicated. A setup names a family and a wallpaper and a layout; the
 * widget specs are resolved from the family's pillar variants at apply time. Restyle a family in a
 * future update and every setup built on it improves with it, which is only possible because
 * pillar coverage is guaranteed by construction.
 */
@Serializable
data class HomeSetup(
    val id: String,
    val name: String,
    /** One line in the setup's own voice. Shown under the name in the gallery. */
    val tagline: String,
    val family: FamilyId,
    val wallpaperId: String,
    val layout: SetupLayout,
    /** Families whose widgets may be mixed in. Empty means single-family, which most setups are. */
    val accentFamilies: List<FamilyId> = emptyList(),
    /** Optional per-setup style tweak applied to every widget in it. */
    val delta: StyleDelta = StyleDelta(),
    val category: SetupCategory,
    val launcher: LauncherAdvice = LauncherAdvice(),
    /** Suggested icon pack. Display and link only — see [IconPackSuggestion]. */
    val iconPack: IconPackSuggestion? = null,
) {
    /**
     * The widgets this setup places, in order, using each family's guaranteed pillar variants.
     * Roles the layout doesn't use are skipped rather than crammed in.
     */
    fun placements(): List<SetupPlacement> = layout.cells.map { cell ->
        SetupPlacement(
            role = cell.role,
            spec = WidgetSpec(
                family = family,
                variant = VariantId("${family.value}-${cell.role.variantSuffix}"),
                userDelta = delta,
            ),
            cell = cell,
        )
    }
}

/**
 * How this setup wants the launcher configured.
 *
 * Advice, never automation. Prism does not change launcher settings — most launchers expose no API
 * for it, and an app that silently rearranges someone's home screen is an app they uninstall. The
 * setup preview shows the screen *as configured*, and this is presented as a short checklist the
 * user can follow in three taps if they want the preview to match exactly.
 */
@Serializable
data class LauncherAdvice(
    val gridColumns: Int = 4,
    val gridRows: Int = 6,
    val hideIconLabels: Boolean = false,
    val dockVisible: Boolean = true,
    /** Free text, one line, e.g. "Two pages, apps on the second". */
    val note: String? = null,
) {
    val gridLabel: String get() = "$gridColumns x $gridRows"
}

/**
 * An icon pack that suits this setup.
 *
 * **Display and link only.** Prism never bundles, mirrors, or redistributes third-party icon packs:
 * they are other people's copyrighted work, and shipping them would be both a licensing violation
 * and a Play policy violation. The card shows the pack's name and author and opens its Play listing
 * so the user buys or installs it from the people who made it. Nothing is downloaded by us, and a
 * setup is complete and correct whether or not the user ever installs the suggestion.
 */
@Serializable
data class IconPackSuggestion(
    val name: String,
    val author: String,
    /** Play Store package id. Opened via an intent; never fetched. */
    val packageId: String,
) {
    val playUrl: String get() = "https://play.google.com/store/apps/details?id=$packageId"
}

/**
 * How the setup gallery is browsed.
 *
 * These are the words people use to describe a home screen they want, which is not the same
 * vocabulary as the family facets — nobody says "I want a Textured home screen", but plenty of
 * people say "I want something Business".
 */
@Serializable
enum class SetupCategory(val label: String) {
    Minimal("Minimal"),
    Productivity("Productivity"),
    Gaming("Gaming"),
    Luxury("Luxury"),
    Amoled("AMOLED"),
    Glass("Glass"),
    LiquidGlass("Liquid Glass"),
    Nature("Nature"),
    Cyberpunk("Cyberpunk"),
    Business("Business"),
    Modern("Modern"),
    Futuristic("Futuristic"),
}

@Serializable
data class SetupPlacement(val role: SetupRole, val spec: WidgetSpec, val cell: SetupCell)

/**
 * The seven pillars again, this time as positions on a screen. The suffix matches the variant ids
 * `Core` generates, which is what lets a setup be a reference rather than a copy.
 */
@Serializable
enum class SetupRole(val variantSuffix: String, val label: String) {
    Clock("clock", "Clock"),
    Weather("weather", "Weather"),
    Calendar("month", "Calendar"),
    Battery("battery", "Battery"),
    Music("music", "Now Playing"),
    Notes("note", "Note"),
    Tasks("todo", "Tasks"),
}

/** A widget's home-screen footprint, in launcher cells. Origin is top-left of the widget area. */
@Serializable
data class SetupCell(val role: SetupRole, val x: Int, val y: Int, val w: Int, val h: Int)

/**
 * Where things sit.
 *
 * Four templates, each a real compositional idea rather than a random arrangement:
 *
 *  [Tower]   one tall column — a big clock at the top, everything stacked beneath. Reads calm.
 *  [Split]   clock across the top, two columns under it. The most balanced, hardest to get wrong.
 *  [Gallery] a large hero widget with small satellites. For families with strong artwork.
 *  [Dense]   maximum information, minimum air. For the Purpose families.
 *
 * All four assume the common 4-column launcher grid and leave the bottom two rows free for the
 * dock and app icons — a setup that buries the user's own apps is a setup they uninstall.
 */
@Serializable
enum class SetupLayout(val displayName: String, val cells: List<SetupCell>) {
    Tower(
        "Tower",
        listOf(
            SetupCell(SetupRole.Clock, 0, 0, 4, 2),
            SetupCell(SetupRole.Weather, 0, 2, 4, 2),
            SetupCell(SetupRole.Calendar, 0, 4, 4, 4),
            SetupCell(SetupRole.Battery, 0, 8, 2, 2),
            SetupCell(SetupRole.Music, 2, 8, 2, 2),
        ),
    ),
    Split(
        "Split",
        listOf(
            SetupCell(SetupRole.Clock, 0, 0, 4, 2),
            SetupCell(SetupRole.Weather, 0, 2, 2, 2),
            SetupCell(SetupRole.Battery, 2, 2, 2, 2),
            SetupCell(SetupRole.Tasks, 0, 4, 2, 4),
            SetupCell(SetupRole.Calendar, 2, 4, 2, 4),
            SetupCell(SetupRole.Music, 0, 8, 4, 2),
        ),
    ),
    Gallery(
        "Gallery",
        listOf(
            SetupCell(SetupRole.Calendar, 0, 0, 4, 4),
            SetupCell(SetupRole.Clock, 0, 4, 4, 2),
            SetupCell(SetupRole.Battery, 0, 6, 2, 2),
            SetupCell(SetupRole.Weather, 2, 6, 2, 2),
            SetupCell(SetupRole.Music, 0, 8, 4, 2),
        ),
    ),
    Dense(
        "Dense",
        listOf(
            SetupCell(SetupRole.Clock, 0, 0, 2, 2),
            SetupCell(SetupRole.Battery, 2, 0, 2, 2),
            SetupCell(SetupRole.Weather, 0, 2, 4, 2),
            SetupCell(SetupRole.Tasks, 0, 4, 2, 4),
            SetupCell(SetupRole.Notes, 2, 4, 2, 4),
            SetupCell(SetupRole.Calendar, 0, 8, 4, 4),
        ),
    );

    val widgetCount: Int get() = cells.size
}
