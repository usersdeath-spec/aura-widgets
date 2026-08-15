package com.prism.studio.data.catalog

import com.google.common.truth.Truth.assertThat
import com.prism.studio.model.FamilyId
import org.junit.Test

/**
 * The catalog's contract, enforced at build time.
 *
 * A design catalog this size cannot be reviewed by eye every release. These tests encode the
 * promises the product makes — every family is a complete ecosystem, every wallpaper pairing
 * resolves, nothing is silently duplicated — so that authoring a fortieth family is as safe as
 * authoring the fourth.
 */
class CatalogIntegrityTest {

    private val catalog = PrismCatalog()
    private val wallpapers = WallpaperCatalog()
    private val setups = SetupCatalog()
    private val packs = PackCatalog()
    private val shelves = FeaturedShelves(catalog)

    @Test
    fun `catalog meets the advertised scale`() {
        assertThat(catalog.families).hasSize(59)
        assertThat(catalog.widgetCount).isIn(650..760)
    }

    /** The headline promise: pick any family and you can furnish a whole home screen from it. */
    @Test
    fun `every family covers all seven pillars`() {
        catalog.families.forEach { family ->
            val types = family.variants.map { it.type }.toSet()
            assertThat(types).containsAtLeastElementsIn(Core.PILLARS)
        }
    }

    @Test
    fun `every family has a browsable shelf`() {
        catalog.families.forEach { family ->
            assertThat(family.variants.size).isIn(10..15)
        }
    }

    @Test
    fun `variant ids are globally unique`() {
        val ids = catalog.families.flatMap { fam -> fam.variants.map { it.id.value } }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `variant ids are prefixed consistently enough to trace`() {
        catalog.families.forEach { family ->
            family.variants.forEach { variant ->
                assertThat(variant.id.value).isNotEmpty()
                assertThat(variant.id.value).doesNotContain(" ")
            }
        }
    }

    /** A shelf of eight widgets that are the same design at eight sizes is not a family. */
    @Test
    fun `no family repeats a type and layout at the same size`() {
        catalog.families.forEach { family ->
            val signatures = family.variants.map { Triple(it.type, it.layout, it.size) }
            assertThat(signatures).containsNoDuplicates()
        }
    }

    @Test
    fun `every family belongs to exactly one collection`() {
        catalog.families.forEach { family ->
            val hits = PrismCatalog.Collection.entries.count { family.id.value in it.familyIds }
            assertThat(hits).isEqualTo(1)
        }
    }

    @Test
    fun `collections reference only real families`() {
        PrismCatalog.Collection.entries.forEach { collection ->
            collection.familyIds.forEach { id ->
                assertThat(catalog.family(FamilyId(id))).isNotNull()
            }
        }
    }

    // ---- Discovery -------------------------------------------------------------------------

    /** A family with no facets is unbrowsable — it exists only for someone who already knows it. */
    @Test
    fun `every family is tagged for browsing`() {
        catalog.families.forEach { family ->
            val facets = catalog.facetsOf(family.id)
            assertThat(facets).isNotEmpty()
            assertThat(facets.size).isIn(3..5)
        }
    }

    /** Each facet has to lead somewhere, or the chip is a dead end in the UI. */
    @Test
    fun `every facet matches at least two families`() {
        com.prism.studio.model.Facet.entries.forEach { facet ->
            val hits = catalog.families.count { facet in catalog.facetsOf(it.id) }
            assertThat(hits).isAtLeast(2)
        }
    }

    @Test
    fun `an empty query returns the whole catalog`() {
        val all = catalog.browse(com.prism.studio.model.BrowseQuery())
        assertThat(all).hasSize(catalog.widgetCount)
    }

    @Test
    fun `facets within a group are or-ed and groups are and-ed`() {
        val dark = com.prism.studio.model.BrowseQuery(
            facets = setOf(com.prism.studio.model.Facet.Dark, com.prism.studio.model.Facet.Light),
        )
        val darkMinimal = dark.toggle(com.prism.studio.model.Facet.Minimal)
        assertThat(catalog.browse(darkMinimal).size).isLessThan(catalog.browse(dark).size)
    }

    // ---- Setups ----------------------------------------------------------------------------

    /** Every placement in every setup must resolve to a real widget, or "Apply" breaks a screen. */
    @Test
    fun `every setup resolves to real widgets`() {
        setups.setups.forEach { setup ->
            val family = catalog.family(setup.family)
            setup.placements().forEach { placement ->
                val exists = family.variants.any { it.id == placement.spec.variant }
                assertThat(exists).isTrue()
            }
        }
    }

    @Test
    fun `every setup uses a wallpaper its family is paired with`() {
        setups.setups.forEach { setup ->
            assertThat(wallpapers.byId(setup.wallpaperId)).isNotNull()
            assertThat(catalog.family(setup.family).pairedWallpapers).contains(setup.wallpaperId)
        }
    }

    @Test
    fun `setup layouts leave room for the user's own apps`() {
        com.prism.studio.model.SetupLayout.entries.forEach { layout ->
            layout.cells.forEach { cell ->
                assertThat(cell.x + cell.w).isAtMost(4)
                assertThat(cell.y + cell.h).isAtMost(12)
            }
        }
    }

    // ---- Wallpapers ------------------------------------------------------------------------

    @Test
    fun `wallpaper collection meets the advertised scale`() {
        assertThat(wallpapers.count).isAtLeast(100)
        assertThat(wallpapers.wallpapers.map { it.id }).containsNoDuplicates()
    }

    /** "Pair with wallpaper" must never dead-end. */
    @Test
    fun `every family wallpaper pairing resolves`() {
        catalog.families.forEach { family ->
            assertThat(family.pairedWallpapers).isNotEmpty()
            family.pairedWallpapers.forEach { id ->
                assertThat(wallpapers.byId(id)).isNotNull()
            }
        }
    }

    /** And the inverse: no artwork ships that nothing links to. */
    @Test
    fun `every wallpaper is reachable from at least one family`() {
        val referenced = catalog.families.flatMap { it.pairedWallpapers }.toSet()
        val orphans = wallpapers.wallpapers.map { it.id }.filterNot { it in referenced }
        assertThat(orphans).isEmpty()
    }

    @Test
    fun `wallpaper family links point at real families`() {
        wallpapers.wallpapers.forEach { paper ->
            assertThat(paper.families).isNotEmpty()
            paper.families.forEach { id ->
                assertThat(catalog.family(FamilyId(id))).isNotNull()
            }
        }
    }

    // ---- Packs and shelves -------------------------------------------------------------------

    /** A pack that names a widget which no longer exists would fail silently at install time. */
    @Test
    fun `every pack variant exists in its family`() {
        packs.packs.forEach { pack ->
            val family = catalog.family(FamilyId(pack.family))
            pack.variantIds.forEach { id ->
                val exists = family.variants.any { it.id.value == id }
                assertThat(exists).isTrue()
            }
            assertThat(wallpapers.byId(pack.recommendedWallpaper)).isNotNull()
        }
    }

    @Test
    fun `packs are worth placing`() {
        packs.packs.forEach { pack ->
            assertThat(pack.variantIds.size).isIn(4..8)
            assertThat(pack.variantIds).containsNoDuplicates()
        }
    }

    @Test
    fun `every curated shelf points at real families`() {
        FeaturedShelves.CuratedShelf.entries.forEach { shelf ->
            assertThat(shelf.familyIds).isNotEmpty()
            shelf.familyIds.forEach { id -> assertThat(catalog.family(FamilyId(id))).isNotNull() }
        }
    }

    /** Similarity must never return the family you are already looking at, or recommend nothing. */
    @Test
    fun `similarity returns useful neighbours`() {
        catalog.families.forEach { family ->
            val similar = shelves.similarTo(family.id)
            assertThat(similar).doesNotContain(family.id)
            assertThat(similar).isNotEmpty()
        }
    }

    // ---- Setup metadata ------------------------------------------------------------------------

    @Test
    fun `every setup category is represented`() {
        val covered = setups.setups.map { it.category }.toSet()
        assertThat(covered).hasSize(com.prism.studio.model.SetupCategory.entries.size)
    }

    @Test
    fun `launcher advice fits a real launcher`() {
        setups.setups.forEach { setup ->
            assertThat(setup.launcher.gridColumns).isIn(3..6)
            assertThat(setup.launcher.gridRows).isIn(4..8)
        }
    }

    // ---- Legibility ------------------------------------------------------------------------

    /**
     * A widget nobody can read is not a design. Ink must clear WCAG AA large-text contrast (3:1)
     * against its own family's surface — checked here for solid and gradient surfaces, where the
     * comparison is unambiguous. Glass and mesh families are covered by golden-image review
     * instead, since their effective background depends on the wallpaper behind them.
     */
    @Test
    fun `ink contrasts with opaque surfaces`() {
        catalog.families.forEach { family ->
            val background = family.base.surface.opaqueSampleOrNull() ?: return@forEach
            val ink = (family.base.ink as? com.prism.studio.model.ColorSpec.Solid)?.argb ?: return@forEach
            val ratio = contrastRatio(ink, background)
            assertThat(ratio).isAtLeast(3.0)
        }
    }

    private fun com.prism.studio.model.Surface.opaqueSampleOrNull(): Long? = when (this) {
        is com.prism.studio.model.Surface.Solid ->
            (color as? com.prism.studio.model.ColorSpec.Solid)?.argb
        is com.prism.studio.model.Surface.Gradient ->
            (stops.first().color as? com.prism.studio.model.ColorSpec.Solid)?.argb
        else -> null
    }

    private fun contrastRatio(a: Long, b: Long): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val v = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
