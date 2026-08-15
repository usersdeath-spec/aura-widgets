package com.prism.studio.data.catalog

import com.prism.studio.model.BrowseQuery
import com.prism.studio.model.DesignFamily
import com.prism.studio.model.Facet
import com.prism.studio.model.FamilyCatalog
import com.prism.studio.model.FamilyId
import com.prism.studio.model.Mood
import com.prism.studio.model.WidgetType
import com.prism.studio.model.WidgetVariant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The authored catalog: 59 families, ~708 widgets.
 *
 * Families are Kotlin values rather than JSON so the compiler enforces the schema and a typo in a
 * variant id is a build failure, not a crash on someone's home screen. The whole catalog is a few
 * hundred KB of immutable objects built once at process start.
 *
 * Adding a family: write the file, add it to [ALL], ship. No renderer, UI, or migration changes.
 */
@Singleton
class PrismCatalog @Inject constructor() : FamilyCatalog {

    override val families: List<DesignFamily> = ALL

    private val index = families.associateBy { it.id }

    init {
        require(index.size == families.size) { "Duplicate family ids in catalog" }
    }

    override fun family(id: FamilyId): DesignFamily =
        index[id] ?: error("Unknown family ${id.value}")

    /**
     * Matches family name, variant name, widget type, mood, and collection.
     *
     * Deliberately forgiving: someone typing "clock" wants every clock in the app, and someone
     * typing "dark" wants a mood. Ranking puts family-name hits first because that is almost
     * always what a two-word query means.
     */
    override fun search(query: String): List<Pair<DesignFamily, WidgetVariant>> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return emptyList()

        fun rank(fam: DesignFamily, v: WidgetVariant): Int = when {
            fam.name.lowercase(Locale.ROOT).startsWith(q) -> 0
            fam.name.lowercase(Locale.ROOT).contains(q) -> 1
            v.name.lowercase(Locale.ROOT).contains(q) -> 2
            v.type.name.lowercase(Locale.ROOT).contains(q) -> 3
            fam.mood.name.lowercase(Locale.ROOT).contains(q) -> 4
            collectionOf(fam.id).label.lowercase(Locale.ROOT).contains(q) -> 5
            else -> -1
        }

        return families
            .flatMap { fam -> fam.variants.map { fam to it } }
            .map { it to rank(it.first, it.second) }
            .filter { it.second >= 0 }
            .sortedBy { it.second }
            .map { it.first }
    }

    override fun byType(type: WidgetType): List<Pair<DesignFamily, WidgetVariant>> =
        families.flatMap { fam -> fam.variants.filter { it.type == type }.map { fam to it } }

    /** The facets a family is tagged with. Empty is a build failure, not a runtime state. */
    fun facetsOf(id: FamilyId): Set<Facet> = FAMILY_FACETS[id.value].orEmpty()

    /**
     * The one browse entry point the UI uses.
     *
     * Facets narrow families; text and type/size narrow variants within them. Doing it in that
     * order matters: "dark clock" should mean "clocks from dark families", not "anything matching
     * dark plus anything matching clock", which is what a flat OR search returns and why most
     * filter UIs feel random.
     */
    fun browse(query: BrowseQuery): List<Pair<DesignFamily, WidgetVariant>> {
        val candidateFamilies = families.filter { query.matches(facetsOf(it.id)) }

        val pairs = candidateFamilies.flatMap { fam -> fam.variants.map { fam to it } }
            .filter { (_, v) -> query.types.isEmpty() || v.type in query.types }
            .filter { (_, v) -> query.sizes.isEmpty() || v.size in query.sizes }

        if (query.text.isBlank()) return pairs

        val allowed = pairs.toSet()
        return search(query.text).filter { it in allowed }
    }

    /** Facet counts for the filter chips, so a chip that would return nothing can be disabled. */
    fun facetCounts(query: BrowseQuery): Map<Facet, Int> =
        Facet.entries.associateWith { facet ->
            families.count { query.toggle(facet).matches(facetsOf(it.id)) }
        }

    fun byMood(mood: Mood): List<DesignFamily> = families.filter { it.mood == mood }

    fun byCollection(collection: Collection): List<DesignFamily> =
        collection.familyIds.map { family(FamilyId(it)) }

    fun collectionOf(id: FamilyId): Collection =
        Collection.entries.first { id.value in it.familyIds }

    /** Shown on the store listing and the About screen. Never hand-counted. */
    val widgetCount: Int get() = families.sumOf { it.variants.size }

    /**
     * How the catalog is grouped for browsing.
     *
     * Six shelves, because a flat list of forty families is a wall and more than about six
     * top-level groups stops being navigable on a phone. Grouping is by *design intent* rather
     * than by colour, so "I want something quiet" and "I want something technical" are one tap.
     */
    enum class Collection(val label: String, val blurb: String, val familyIds: List<String>) {
        Foundation(
            "Foundation",
            "The three we lead with. Between them, the whole range.",
            listOf("minimal-mono", "amoled-black", "luxury-gold"),
        ),
        LiquidGlass(
            "Liquid Glass",
            "The flagship. Layered translucency with a real backdrop where the platform allows one.",
            listOf("liquid-glass-clear", "liquid-glass-smoked", "liquid-glass-prism"),
        ),
        Material(
            "Glass & Material",
            "Surfaces that look like something: ice, stone, metal, paper.",
            listOf("frosted-crystal", "sea-glass", "chrome-liquid", "marble", "origami", "paper-cut"),
        ),
        Structure(
            "Structure & Restraint",
            "Families built on rules rather than textures.",
            listOf(
                "scandinavian", "japanese-zen", "swiss-grid", "bauhaus-primary",
                "brutalist-slab", "ink-serif", "material-you", "neumorph-soft",
            ),
        ),
        Light(
            "Colour & Light",
            "Each one pinned to a real light condition.",
            listOf(
                "aurora", "gradient-flow", "sunset-fade", "candy-pop",
                "terracotta", "botanical", "nordic-frost", "seasonal-bloom",
            ),
        ),
        Depth(
            "Depth",
            "Near-black families about distance. Also the cheapest to display.",
            listOf("cosmic-drift", "deep-space", "monolith"),
        ),
        Signal(
            "Signal",
            "Instruments, where every technical mark measures something.",
            listOf("cyberpunk-neon", "hud-tactical", "rgb-gaming", "pixel-retro", "crt-amber", "blueprint"),
        ),
        Materials(
            "Precious Materials",
            "Nine families, each pinned to how one real material behaves under light.",
            listOf(
                "titanium", "mercury", "quartz", "velvet", "obsidian",
                "carbon", "pearl", "gemstone", "satin",
            ),
        ),
        Phenomena(
            "Phenomena",
            "Built on an optical event rather than a material — diffraction, occlusion, depth.",
            listOf(
                "holographic", "neon-frost", "eclipse", "solar",
                "abyssal", "horizon", "mirage", "studio",
            ),
        ),
        Purpose(
            "Purpose",
            "Organised around a task: work, money, training, music, planning.",
            listOf("executive-slate", "ledger", "pulse", "vinyl", "focus-grid"),
        ),
    }

    internal companion object {
        val ALL: List<DesignFamily> = listOf(
            // Foundation
            MinimalMono, AmoledBlack, LuxuryGold,
            // Liquid Glass — the flagship collection
            LiquidGlassClear, LiquidGlassSmoked, LiquidGlassPrism,
            // Glass & Material
            FrostedCrystal, SeaGlass, ChromeLiquid, Marble, Origami, PaperCut,
            // Structure & Restraint
            Scandinavian, JapaneseZen, SwissGrid, BauhausPrimary, BrutalistSlab, InkSerif,
            MaterialYou, NeumorphSoft,
            // Colour & Light
            Aurora, GradientFlow, SunsetFade, CandyPop, Terracotta, Botanical, NordicFrost,
            SeasonalBloom,
            // Depth
            CosmicDrift, DeepSpace, Monolith,
            // Signal
            CyberpunkNeon, HudTactical, RgbGaming, PixelRetro, CrtAmber, Blueprint,
            // Purpose
            ExecutiveSlate, Ledger, Pulse, Vinyl, FocusGrid,
            // Precious Materials
            Titanium, Mercury, Quartz, Velvet, Obsidian, Carbon, Pearl, Gemstone, Satin,
            // Phenomena
            Holographic, NeonFrost, Eclipse, Solar, Abyssal, Horizon, Mirage, Studio,
        )
    }
}
