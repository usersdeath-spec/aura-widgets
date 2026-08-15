package com.prism.studio.data.catalog

import com.prism.studio.model.Facet
import com.prism.studio.model.FamilyId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Featured shelves — the front page of the app.
 *
 * Two kinds, and the distinction is a product decision worth stating plainly.
 *
 * **Curated** shelves are ours: Editor's Picks, Staff Favourites, New Arrivals. We stand behind
 * them, they change with each release, and they are honest because they are labelled as opinion.
 *
 * **Computed** shelves are the user's own: what they place most, what they viewed recently. These
 * are calculated on device from Room.
 *
 * What is deliberately absent: "Trending" and "Most Downloaded". Both require collecting behaviour
 * from every install, and Prism ships no analytics — that is a promise on the store listing, not a
 * gap. A fake "Trending" shelf backed by a hardcoded list would be worse than none: it is a lie
 * about other users. [ComputedShelf.MostPlaced] gives the same *utility* — "what actually gets used"
 * — from data the user already owns, and is labelled as theirs.
 */
@Singleton
class FeaturedShelves @Inject constructor(private val catalog: PrismCatalog) {

    /** Our opinion, updated each release. Order is the display order. */
    enum class CuratedShelf(
        val title: String,
        val blurb: String,
        val familyIds: List<String>,
    ) {
        EditorsPicks(
            "Editor's Picks",
            "The seven we'd put on our own phones this month.",
            listOf(
                "liquid-glass-clear", "obsidian", "japanese-zen", "pearl",
                "focus-grid", "eclipse", "swiss-grid",
            ),
        ),
        NewArrivals(
            "New Arrivals",
            "Seventeen families added this release.",
            listOf(
                "titanium", "mercury", "quartz", "velvet", "obsidian", "carbon", "pearl",
                "gemstone", "satin", "holographic", "neon-frost", "eclipse", "solar",
                "abyssal", "horizon", "mirage", "studio",
            ),
        ),
        StaffFavourites(
            "Staff Favourites",
            "The ones we keep going back to.",
            listOf("minimal-mono", "amoled-black", "vinyl", "botanical", "brutalist-slab", "satin"),
        ),
        LiquidGlass(
            "Liquid Glass",
            "The flagship. Three materials, one light source.",
            listOf("liquid-glass-clear", "liquid-glass-smoked", "liquid-glass-prism", "satin", "neon-frost"),
        ),
        Minimal(
            "Minimal",
            "Everything removed that could be.",
            listOf("minimal-mono", "studio", "scandinavian", "swiss-grid", "japanese-zen", "paper-cut"),
        ),
        Luxury(
            "Luxury",
            "Restraint, not ornament.",
            listOf("luxury-gold", "velvet", "obsidian", "marble", "gemstone", "pearl", "ink-serif"),
        ),
        Amoled(
            "AMOLED",
            "True black. Fewer lit pixels, longer battery.",
            listOf("amoled-black", "monolith", "deep-space", "eclipse", "pulse", "hud-tactical"),
        ),
        Productivity(
            "Productivity",
            "Built for screens you actually work from.",
            listOf("focus-grid", "executive-slate", "ledger", "paper-cut", "swiss-grid"),
        );

        fun families(catalog: PrismCatalog) = familyIds.map { catalog.family(FamilyId(it)) }
    }

    /**
     * Shelves computed from the user's own history. Rendered under a heading that says so —
     * "You place these most", not "Popular" — because the difference matters.
     */
    sealed interface ComputedShelf {
        val title: String

        data object MostPlaced : ComputedShelf {
            override val title = "You place these most"
        }

        data object RecentlyViewed : ComputedShelf {
            override val title = "Recently viewed"
        }

        /** Families sharing facets with what the user has already placed. */
        data object BecauseYouLiked : ComputedShelf {
            override val title = "More like your setup"
        }
    }

    /**
     * Recommendation without a recommender.
     *
     * Facet overlap, weighted by how specific each facet is: matching on "Luxury" says more than
     * matching on "Dark", because eleven families are Dark and four are Luxury. That single weight
     * is the difference between useful suggestions and every dark family in the catalog.
     */
    fun similarTo(family: FamilyId, limit: Int = 8): List<FamilyId> {
        val target = catalog.facetsOf(family)
        if (target.isEmpty()) return emptyList()

        val rarity: Map<Facet, Double> = Facet.entries.associateWith { facet ->
            val count = catalog.families.count { facet in catalog.facetsOf(it.id) }
            if (count == 0) 0.0 else 1.0 / count
        }

        return catalog.families
            .filter { it.id != family }
            .map { candidate ->
                val shared = target intersect catalog.facetsOf(candidate.id)
                candidate.id to shared.sumOf { rarity[it] ?: 0.0 }
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}

/**
 * Widget packs — a coordinated set placed in one action.
 *
 * The difference from a Setup: a setup is a *whole screen* including wallpaper and positions; a
 * pack is a *toolkit* for a purpose, and the user places its widgets wherever they like. Someone
 * who already loves their wallpaper wants a pack, not a setup.
 *
 * Packs name variants explicitly rather than deriving them from pillars, because a Student Pack
 * needs a countdown and a habit tracker — widgets no pillar covers.
 */
@Singleton
class PackCatalog @Inject constructor() {

    data class Pack(
        val id: String,
        val name: String,
        val blurb: String,
        val family: String,
        /** Variant ids within [family]. Every one is asserted to exist. */
        val variantIds: List<String>,
        val recommendedWallpaper: String,
    )

    val packs: List<Pack> = listOf(
        Pack(
            "pack-productivity", "Productivity Pack",
            "Today, this week, and what you owe people.",
            "focus-grid",
            listOf("focus-grid-clock", "focus-grid-todo", "focus-grid-note", "fg-agenda", "fg-countdown", "fg-week"),
            "focus-01",
        ),
        Pack(
            "pack-student", "Student Pack",
            "Deadlines, habits, and a place for notes.",
            "paper-cut",
            listOf("paper-cut-clock", "paper-cut-month", "paper-cut-todo", "paper-cut-note", "pc-agenda", "pc-habit"),
            "paper-01",
        ),
        Pack(
            "pack-executive", "Executive Pack",
            "Two time zones, the diary, and the markets.",
            "executive-slate",
            listOf("executive-slate-clock", "es-world", "es-agenda", "es-finance", "es-countdown", "executive-slate-todo"),
            "slate-01",
        ),
        Pack(
            "pack-minimal", "Minimal Pack",
            "The time, the day, and nothing else.",
            "minimal-mono",
            listOf("minimal-mono-clock", "mm-day", "minimal-mono-battery", "mm-quote"),
            "mono-01",
        ),
        Pack(
            "pack-amoled", "AMOLED Pack",
            "Black on black, and the numbers you check.",
            "amoled-black",
            listOf("amoled-black-clock", "amoled-black-battery", "ab-cpu", "ab-ram", "ab-storage", "ab-system"),
            "void-01",
        ),
        Pack(
            "pack-gaming", "Gaming Pack",
            "Everything the rig is doing, at a glance.",
            "carbon",
            listOf("carbon-clock", "cb-cpu", "cb-ram", "cb-network", "cb-system", "carbon-battery"),
            "carbon-01",
        ),
        Pack(
            "pack-luxury", "Luxury Pack",
            "Restraint, in gold.",
            "luxury-gold",
            listOf("luxury-gold-clock", "lx-dial", "luxury-gold-month", "lx-quote", "lx-finance", "luxury-gold-weather"),
            "gilt-01",
        ),
        Pack(
            "pack-nature", "Nature Pack",
            "Daylight, weather, and what's growing.",
            "botanical",
            listOf("botanical-clock", "botanical-weather", "bo-sun", "bo-habit", "bo-steps", "botanical-month"),
            "leaf-01",
        ),
    )

    fun byId(id: String): Pack = packs.first { it.id == id }
}
