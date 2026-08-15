package com.prism.studio.data.catalog

import com.prism.studio.model.FamilyId
import com.prism.studio.model.HomeSetup
import com.prism.studio.model.IconPackSuggestion
import com.prism.studio.model.LauncherAdvice
import com.prism.studio.model.SetupCategory
import com.prism.studio.model.SetupLayout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Curated home screens — sixteen finished looks, applied in one confirm.
 *
 * Each is a family, a wallpaper chosen from that family's own pairings, and a layout template
 * matched to the family's character: Tower for calm families, Dense for the Purpose ones, Gallery
 * where the calendar or artwork deserves to be the hero.
 *
 * Taglines are written the way a person describes their own home screen, not the way a store
 * describes a product. "Nothing but the time" is what someone would actually say.
 */
@Singleton
class SetupCatalog @Inject constructor() {

    val setups: List<HomeSetup> = ALL
    private val index = setups.associateBy { it.id }

    fun byId(id: String): HomeSetup = index[id] ?: error("Unknown setup $id")
    fun forFamily(family: FamilyId): List<HomeSetup> = setups.filter { it.family == family }
    val count: Int get() = setups.size

    private companion object {

        fun setup(
            id: String,
            name: String,
            tagline: String,
            family: String,
            wallpaper: String,
            layout: SetupLayout,
            category: SetupCategory,
            launcher: LauncherAdvice = LauncherAdvice(),
            iconPack: IconPackSuggestion? = null,
        ) = HomeSetup(
            id = id, name = name, tagline = tagline, family = FamilyId(family),
            wallpaperId = wallpaper, layout = layout, category = category,
            launcher = launcher, iconPack = iconPack,
        )

        val ALL = listOf(
            setup("quiet-hours", "Quiet Hours", "Nothing but the time.",
                "minimal-mono", "mono-01", SetupLayout.Tower, SetupCategory.Minimal),

            setup("clear-morning", "Clear Morning", "Everything visible, nothing shouting.",
                "liquid-glass-clear", "refract-01", SetupLayout.Split, SetupCategory.LiquidGlass),

            setup("smoked", "Smoked", "Heavy glass over a dark room.",
                "liquid-glass-smoked", "smoke-01", SetupLayout.Split, SetupCategory.LiquidGlass),

            setup("prism", "Prism", "Light split into its parts.",
                "liquid-glass-prism", "refract-01", SetupLayout.Gallery, SetupCategory.LiquidGlass),

            setup("lights-out", "Lights Out", "Black on black. The battery notices.",
                "amoled-black", "void-01", SetupLayout.Split, SetupCategory.Amoled),

            setup("after-hours", "After Hours", "Charcoal and one gold line.",
                "luxury-gold", "gilt-01", SetupLayout.Tower, SetupCategory.Luxury),

            setup("north-light", "North Light", "Overcast, cold, extremely readable.",
                "nordic-frost", "fjord-01", SetupLayout.Split, SetupCategory.Modern),

            setup("paper-desk", "Paper Desk", "Cards on a desk. Real shadows.",
                "paper-cut", "paper-01", SetupLayout.Split, SetupCategory.Minimal),

            setup("tea-room", "Tea Room", "Ink on paper, and a lot of empty space.",
                "japanese-zen", "washi-01", SetupLayout.Tower, SetupCategory.Minimal),

            setup("polar", "Polar", "Cold light low on a dark sky.",
                "aurora", "aurora-01", SetupLayout.Gallery, SetupCategory.Modern),

            setup("late-dusk", "Late Dusk", "Warm above, cool below.",
                "sunset-fade", "dusk-01", SetupLayout.Tower, SetupCategory.Modern),

            setup("greenhouse", "Greenhouse", "Deep green under warm glass.",
                "botanical", "leaf-01", SetupLayout.Gallery, SetupCategory.Nature),

            setup("night-shift", "Night Shift", "Instruments, all of them reading something.",
                "hud-tactical", "hud-01", SetupLayout.Dense, SetupCategory.Futuristic),

            setup("wet-asphalt", "Wet Asphalt", "Neon, but only where it means something.",
                "cyberpunk-neon", "neon-01", SetupLayout.Dense, SetupCategory.Cyberpunk),

            setup("the-week", "The Week", "Everything I owe someone, on one screen.",
                "focus-grid", "focus-01", SetupLayout.Dense, SetupCategory.Productivity),

            setup("open-markets", "Open Markets", "Numbers first. Colour means gain or loss.",
                "ledger", "ledger-01", SetupLayout.Dense, SetupCategory.Business),

            // ---- Added in Phase 4 --------------------------------------------------------
            setup("volcanic", "Volcanic", "Black glass and one hard highlight.",
                "obsidian", "obsidian-01", SetupLayout.Split, SetupCategory.Luxury,
                LauncherAdvice(4, 6, hideIconLabels = true, note = "Labels off. The screen is quiet enough without them.")),

            setup("state-room", "State Room", "Deep dye, light serif, nothing hurried.",
                "velvet", "velvet-01", SetupLayout.Tower, SetupCategory.Luxury),

            setup("cold-open", "Cold Open", "Cut facets and cold daylight.",
                "quartz", "quartz-01", SetupLayout.Gallery, SetupCategory.Glass),

            setup("machined", "Machined", "Brushed metal, everything measured.",
                "titanium", "brushed-01", SetupLayout.Dense, SetupCategory.Modern),

            setup("weave", "Weave", "Twill, monospace, and every number you check.",
                "carbon", "carbon-01", SetupLayout.Dense, SetupCategory.Gaming,
                LauncherAdvice(5, 6, note = "Five columns keeps the gauges square.")),

            setup("totality", "Totality", "One dark disc, light only at the rim.",
                "eclipse", "eclipse-01", SetupLayout.Gallery, SetupCategory.Amoled),

            setup("first-light", "First Light", "A hot core, and the day ahead.",
                "solar", "solar-01", SetupLayout.Tower, SetupCategory.Modern),

            setup("deep-water", "Deep Water", "Contrast falling away with depth.",
                "abyssal", "abyss-01", SetupLayout.Split, SetupCategory.Nature),

            setup("seamless", "Seamless", "One soft light on a neutral backdrop.",
                "studio", "studio-01", SetupLayout.Split, SetupCategory.Minimal,
                LauncherAdvice(4, 5, hideIconLabels = true, note = "Fewer rows. Let the backdrop breathe.")),

            setup("diffusion", "Diffusion", "Neon with the edges taken off.",
                "neon-frost", "neonfrost-01", SetupLayout.Gallery, SetupCategory.Futuristic),

            setup("iridescent", "Iridescent", "The spectrum moving as you move.",
                "holographic", "holo-01", SetupLayout.Gallery, SetupCategory.Futuristic),

            setup("satin-finish", "Satin Finish", "Translucent, with the shine removed.",
                "satin", "satin-01", SetupLayout.Split, SetupCategory.Glass),
        )
    }
}
