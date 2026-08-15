package com.prism.studio.data.catalog

import com.prism.studio.model.FamilyId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The wallpaper collection: 143 original pieces in 41 series.
 *
 * Every family in [PrismCatalog] names at least three wallpapers it was art-directed alongside,
 * and every wallpaper names the families it belongs to. That two-way link is what powers "Apply
 * the whole look" — wallpaper and matching widgets in two taps — and it is enforced by
 * `CatalogIntegrityTest`, so a wallpaper can never be referenced by a family without existing.
 *
 * Series, not singles. Artwork is commissioned in runs of two to four variations on one idea,
 * because a user who likes a wallpaper usually wants the same thing in a different weight, not
 * something unrelated. It is also how 108 pieces stay coherent instead of reading as a stock pack.
 *
 * The palettes below are the **art direction brief**, not extracted data: they tell the artist the
 * intended dominant, vibrant, and muted values, and they let the app lay out and colour-match
 * before any artwork exists. At pack build time a Palette pass over the delivered art overwrites
 * them with measured values. See `docs/WALLPAPERS.md` for the full brief per series.
 */
@Singleton
class WallpaperCatalog @Inject constructor() {

    val wallpapers: List<WallpaperEntry> = ALL
    private val index = wallpapers.associateBy { it.id }

    fun byId(id: String): WallpaperEntry = index[id] ?: error("Unknown wallpaper $id")
    fun byCategory(category: Category): List<WallpaperEntry> = wallpapers.filter { it.category == category }
    fun forFamily(family: FamilyId): List<WallpaperEntry> =
        wallpapers.filter { family.value in it.families }

    fun search(query: String): List<WallpaperEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return wallpapers.filter {
            it.title.lowercase().contains(q) || it.category.label.lowercase().contains(q)
        }
    }

    val count: Int get() = wallpapers.size

    enum class Category(val label: String) {
        AmoledBlack("AMOLED Black"), Minimal("Minimal"), Abstract("Abstract"), Glass("Glass"),
        Aurora("Aurora"), Gradient("Gradient"), Nature("Nature"), Mountains("Mountains"),
        Forest("Forest"), Space("Space"), Galaxy("Galaxy"), Cyberpunk("Cyberpunk"), Neon("Neon"),
        Luxury("Luxury"), Material("Material"), Japanese("Japanese"), Scandinavian("Scandinavian"),
        Gaming("Gaming"), Retro("Retro"), Dark("Dark"), Light("Light"), Seasonal("Seasonal"),
        Texture("Texture"), Technical("Technical"),
    }

    /**
     * One piece of artwork.
     *
     * [assetPath] points into the `:wallpaper_pack` asset pack, never the base APK. Artwork is
     * delivered as AVIF at 1440x3120 with a JPEG sibling for API < 31; at that resolution a
     * 108-piece pack lands near 150 MB in AVIF against roughly 380 MB in JPEG, which is the whole
     * reason for the format choice.
     */
    data class WallpaperEntry(
        val id: String,
        val title: String,
        val category: Category,
        val dominant: Long,
        val vibrant: Long,
        val muted: Long,
        /** Mean luminance of the top third — decides light or dark status-bar icons. */
        val topLuminance: Float,
        val families: List<String>,
    ) {
        val assetPath: String get() = "wallpapers/$id.avif"
        val fallbackPath: String get() = "wallpapers/$id.jpg"
        val prefersLightStatusIcons: Boolean get() = topLuminance < 0.5f
    }

    private companion object {

        /**
         * Declares a series: one idea, several weights. Ids are `<prefix>-01`, `-02`, and so on,
         * which is exactly the form families reference.
         */
        fun series(
            prefix: String,
            category: Category,
            dominant: Long,
            vibrant: Long,
            muted: Long,
            topLuminance: Float,
            families: List<String>,
            vararg titles: String,
        ): List<WallpaperEntry> = titles.mapIndexed { i, title ->
            WallpaperEntry(
                id = "%s-%02d".format(prefix, i + 1),
                title = title,
                category = category,
                dominant = dominant,
                vibrant = vibrant,
                muted = muted,
                topLuminance = topLuminance,
                families = families,
            )
        }

        val ALL: List<WallpaperEntry> = buildList {
            // ---- Void & dark -------------------------------------------------------------
            addAll(series("void", Category.AmoledBlack, 0xFF000000, 0xFF2A2A30, 0xFF141418, 0.04f,
                listOf("amoled-black", "minimal-mono", "monolith", "pulse"),
                "Absolute", "Absolute II"))
            addAll(series("grid-void", Category.AmoledBlack, 0xFF000000, 0xFF1F4A3A, 0xFF101714, 0.06f,
                listOf("amoled-black", "hud-tactical", "crt-amber"),
                "Faint Grid"))
            addAll(series("slab", Category.Dark, 0xFF101215, 0xFF3A414B, 0xFF1B1F25, 0.08f,
                listOf("monolith", "executive-slate"),
                "Slab", "Slab II"))
            addAll(series("smoke", Category.Dark, 0xFF14110E, 0xFF5C4A2E, 0xFF241E17, 0.1f,
                listOf("luxury-gold", "ink-serif"),
                "Smoke"))

            // ---- Minimal & mono ----------------------------------------------------------
            addAll(series("mono", Category.Minimal, 0xFF0C0C0E, 0xFFE8E8EA, 0xFF3A3A3E, 0.09f,
                listOf("minimal-mono", "swiss-grid"),
                "Fold", "Fold II"))
            addAll(series("grid", Category.Technical, 0xFFF2F2F0, 0xFFE10600, 0xFFCFCFCB, 0.88f,
                listOf("swiss-grid", "blueprint", "focus-grid"),
                "Ruled", "Ruled II", "Ruled III"))
            addAll(series("paper", Category.Light, 0xFFFBFAF7, 0xFFE4572E, 0xFFD9D5CC, 0.93f,
                listOf("paper-cut", "ledger", "ink-serif"),
                "Card Stock", "Card Stock II", "Card Stock III"))
            addAll(series("linen", Category.Texture, 0xFFF4F1EC, 0xFF9CAF97, 0xFFD8D2C7, 0.9f,
                listOf("scandinavian"),
                "Linen"))
            addAll(series("birch", Category.Scandinavian, 0xFFEFEAE1, 0xFFB9A489, 0xFFD5CCBE, 0.87f,
                listOf("scandinavian", "paper-cut"),
                "Birch", "Birch II"))

            // ---- Glass & material --------------------------------------------------------
            addAll(series("refract", Category.Glass, 0xFF1B2436, 0xFF9FC6FF, 0xFF44566F, 0.22f,
                listOf("liquid-glass", "frosted-crystal"),
                "Refraction", "Refraction II"))
            addAll(series("frost", Category.Glass, 0xFFDDE9F5, 0xFF2F6FA8, 0xFFB6C8D8, 0.85f,
                listOf("frosted-crystal", "nordic-frost"),
                "Frost", "Frost II", "Frost III"))
            addAll(series("tide", Category.Glass, 0xFF6E9E92, 0xFFBFE3D6, 0xFF48695F, 0.5f,
                listOf("sea-glass", "botanical"),
                "Tide", "Tide II", "Tide III"))
            addAll(series("chrome", Category.Abstract, 0xFF6E7A88, 0xFFE6EDF5, 0xFF39424E, 0.55f,
                listOf("chrome-liquid"),
                "Chrome", "Chrome II", "Chrome III"))
            addAll(series("stone", Category.Texture, 0xFFF2F0EC, 0xFF889AA8, 0xFFD2D0CB, 0.9f,
                listOf("marble"),
                "Vein", "Vein II", "Vein III"))
            addAll(series("fold", Category.Abstract, 0xFFEFE7D8, 0xFFD2542E, 0xFFCFC4B0, 0.86f,
                listOf("origami"),
                "Crease", "Crease II", "Crease III"))
            addAll(series("soft", Category.Material, 0xFFE0E3EA, 0xFF6D7FF0, 0xFFC3C8D4, 0.87f,
                listOf("neumorph-soft"),
                "Soft", "Soft II", "Soft III"))
            addAll(series("grain", Category.Texture, 0xFF17130F, 0xFFE9704B, 0xFF382E26, 0.11f,
                listOf("vinyl"),
                "Grain"))

            // ---- Colour & light ----------------------------------------------------------
            addAll(series("aurora", Category.Aurora, 0xFF070B14, 0xFF2BE0A8, 0xFF1B3350, 0.07f,
                listOf("aurora", "liquid-glass"),
                "Polar", "Polar II", "Polar III"))
            addAll(series("flow", Category.Gradient, 0xFF6A5AE0, 0xFF3FC1C9, 0xFF463C96, 0.42f,
                listOf("gradient-flow", "material-you"),
                "Flow", "Flow II", "Flow III"))
            addAll(series("dusk", Category.Gradient, 0xFFF2617A, 0xFFFFB86B, 0xFF3B2A6E, 0.62f,
                listOf("sunset-fade"),
                "Dusk", "Dusk II", "Dusk III"))
            addAll(series("mesh", Category.Material, 0xFF1B1B1F, 0xFFB9C3FF, 0xFF3A3A45, 0.15f,
                listOf("material-you", "gradient-flow"),
                "Mesh", "Mesh II", "Mesh III"))
            addAll(series("candy", Category.Abstract, 0xFFFF5DA2, 0xFFFFE45E, 0xFF4CC9F0, 0.6f,
                listOf("candy-pop"),
                "Pop", "Pop II", "Pop III"))
            addAll(series("clay", Category.Nature, 0xFFC96F4A, 0xFF4A5D3F, 0xFF8C5237, 0.5f,
                listOf("terracotta"),
                "Clay", "Clay II", "Clay III"))
            addAll(series("leaf", Category.Forest, 0xFF12281C, 0xFFC9E86B, 0xFF2F7D52, 0.14f,
                listOf("botanical", "sea-glass"),
                "Canopy", "Canopy II", "Canopy III"))
            addAll(series("fjord", Category.Mountains, 0xFF1B2430, 0xFF7FB2D9, 0xFF34434F, 0.18f,
                listOf("nordic-frost", "frosted-crystal"),
                "Fjord", "Fjord II", "Fjord III"))
            addAll(series("bloom", Category.Seasonal, 0xFFE2A9C0, 0xFFB6577C, 0xFFC79BAE, 0.78f,
                listOf("seasonal-bloom"),
                "Spring", "Summer", "Autumn", "Winter"))

            // ---- Depth -------------------------------------------------------------------
            addAll(series("nebula", Category.Galaxy, 0xFF05060D, 0xFF8B5CF6, 0xFF2A2350, 0.05f,
                listOf("cosmic-drift"),
                "Drift", "Drift II", "Drift III"))
            addAll(series("orbit", Category.Space, 0xFF000000, 0xFF3E7BFF, 0xFF13203A, 0.04f,
                listOf("deep-space"),
                "Orbit", "Orbit II"))

            // ---- Signal ------------------------------------------------------------------
            addAll(series("neon", Category.Cyberpunk, 0xFF120A22, 0xFFFF2E88, 0xFF2A1B44, 0.1f,
                listOf("cyberpunk-neon", "rgb-gaming"),
                "Wet Asphalt", "Wet Asphalt II", "Wet Asphalt III"))
            addAll(series("hud", Category.Technical, 0xFF050A07, 0xFF5BFF9E, 0xFF15251C, 0.06f,
                listOf("hud-tactical"),
                "Reticle", "Reticle II"))
            addAll(series("rig", Category.Gaming, 0xFF0A0A0F, 0xFF00D1FF, 0xFF241A3A, 0.07f,
                listOf("rgb-gaming"),
                "Rig", "Rig II"))
            addAll(series("pixel", Category.Retro, 0xFF1D2B53, 0xFFFFEC27, 0xFF29ADFF, 0.2f,
                listOf("pixel-retro"),
                "Sprite", "Sprite II", "Sprite III"))
            addAll(series("crt", Category.Retro, 0xFF120C04, 0xFFFFB000, 0xFF3A2A0E, 0.08f,
                listOf("crt-amber"),
                "Phosphor", "Phosphor II"))
            addAll(series("draft", Category.Technical, 0xFF0E2A4A, 0xFF7EC8FF, 0xFF1D3E63, 0.16f,
                listOf("blueprint"),
                "Drafting", "Drafting II"))

            // ---- Purpose -----------------------------------------------------------------
            addAll(series("slate", Category.Dark, 0xFF232830, 0xFF5B8DEF, 0xFF343B45, 0.16f,
                listOf("executive-slate"),
                "Slate", "Slate II"))
            addAll(series("ledger", Category.Light, 0xFFF3F1E7, 0xFF2E7D5B, 0xFFD4D2C4, 0.9f,
                listOf("ledger"),
                "Ruled Ledger", "Ruled Ledger II"))
            addAll(series("pulse", Category.Dark, 0xFF0C0D10, 0xFFFF3B5C, 0xFF23262E, 0.06f,
                listOf("pulse"),
                "Pulse", "Pulse II"))
            addAll(series("sleeve", Category.Abstract, 0xFF17130F, 0xFFE9704B, 0xFF3A2E24, 0.1f,
                listOf("vinyl"),
                "Sleeve", "Sleeve II"))
            addAll(series("focus", Category.Dark, 0xFF15161A, 0xFF64D2A0, 0xFF262A31, 0.09f,
                listOf("focus-grid"),
                "Focus", "Focus II"))

            // ---- Culture & craft ---------------------------------------------------------
            addAll(series("washi", Category.Japanese, 0xFFF7F4EC, 0xFFB33A2B, 0xFFDCD6C6, 0.92f,
                listOf("japanese-zen"),
                "Washi", "Washi II"))
            addAll(series("sumi", Category.Japanese, 0xFFF2EEE4, 0xFF1B1B1B, 0xFFBFB9AA, 0.89f,
                listOf("japanese-zen", "ink-serif"),
                "Sumi"))
            addAll(series("bauhaus", Category.Abstract, 0xFFEFEAE0, 0xFFE03C31, 0xFF1E5AA8, 0.85f,
                listOf("bauhaus-primary"),
                "Primary", "Primary II", "Primary III"))
            addAll(series("concrete", Category.Texture, 0xFF2E2E2B, 0xFFFFD400, 0xFF4A4A45, 0.2f,
                listOf("brutalist-slab"),
                "Cast", "Cast II", "Cast III"))
            addAll(series("ink", Category.Dark, 0xFF12100E, 0xFFC0A062, 0xFF2A2521, 0.08f,
                listOf("ink-serif"),
                "Ink", "Ink II"))
            addAll(series("gilt", Category.Luxury, 0xFF0C0A08, 0xFFC9A227, 0xFF2A2318, 0.06f,
                listOf("luxury-gold"),
                "Gilt", "Gilt II"))

            // ---- Precious materials (Phase 4) ---------------------------------------------
            addAll(series("brushed", Category.Texture, 0xFF6E747D, 0xFFCBD4DE, 0xFF484D55, 0.45f,
                listOf("titanium"), "Brushed", "Brushed II"))
            addAll(series("mercury", Category.Abstract, 0xFF9AA6B4, 0xFFEDF2F7, 0xFF3F4854, 0.62f,
                listOf("mercury"), "Droplet", "Droplet II"))
            addAll(series("quartz", Category.Glass, 0xFFDCE4EE, 0xFF4C7FB8, 0xFFC7D2E0, 0.87f,
                listOf("quartz"), "Facet", "Facet II"))
            addAll(series("velvet", Category.Luxury, 0xFF2E1740, 0xFFC9A9E0, 0xFF3A1F4A, 0.12f,
                listOf("velvet"), "Pile", "Pile II"))
            addAll(series("obsidian", Category.Dark, 0xFF0E1014, 0xFF8FA3BF, 0xFF1A1D24, 0.05f,
                listOf("obsidian"), "Conchoidal", "Conchoidal II"))
            addAll(series("carbon", Category.Technical, 0xFF14161A, 0xFF57D1A0, 0xFF272C33, 0.07f,
                listOf("carbon"), "Twill", "Twill II"))
            addAll(series("pearl", Category.Light, 0xFFF3ECF6, 0xFF9C7FC4, 0xFFDFD6E4, 0.91f,
                listOf("pearl"), "Nacre", "Nacre II"))
            addAll(series("gem", Category.Luxury, 0xFF10604A, 0xFF7FE3BE, 0xFF072620, 0.16f,
                listOf("gemstone"), "Emerald", "Ruby", "Sapphire"))
            addAll(series("satin", Category.Glass, 0xFFB8BCC6, 0xFFE4E8EF, 0xFF7E838D, 0.6f,
                listOf("satin"), "Satin", "Satin II"))

            // ---- Phenomena (Phase 4) ------------------------------------------------------
            addAll(series("holo", Category.Abstract, 0xFFB58CFF, 0xFF7CE0FF, 0xFFFF8FC8, 0.68f,
                listOf("holographic"), "Diffraction", "Diffraction II"))
            addAll(series("neonfrost", Category.Neon, 0xFF2A1B4A, 0xFFC9A0FF, 0xFF1A1030, 0.12f,
                listOf("neon-frost"), "Diffused", "Diffused II"))
            addAll(series("eclipse", Category.Space, 0xFF07080B, 0xFFFFB04C, 0xFF3A2A16, 0.04f,
                listOf("eclipse"), "Corona", "Corona II"))
            addAll(series("solar", Category.Abstract, 0xFFE8562C, 0xFFFFC24A, 0xFF7A1A12, 0.55f,
                listOf("solar"), "Chromosphere", "Chromosphere II"))
            addAll(series("abyss", Category.Nature, 0xFF0A2739, 0xFF4FC3D9, 0xFF03101A, 0.18f,
                listOf("abyssal"), "Attenuation", "Attenuation II"))
            addAll(series("horizon", Category.Mountains, 0xFF1E3A56, 0xFFE9A56B, 0xFF0C1C2E, 0.6f,
                listOf("horizon"), "Divide", "Divide II"))
            addAll(series("mirage", Category.Seasonal, 0xFFD9C29E, 0xFF9C7A4E, 0xFFC2A886, 0.82f,
                listOf("mirage"), "Shimmer", "Shimmer II"))
            addAll(series("studio", Category.Light, 0xFFE2E2E6, 0xFF5A5D66, 0xFFD2D2D8, 0.89f,
                listOf("studio"), "Seamless", "Seamless II"))
        }
    }
}
