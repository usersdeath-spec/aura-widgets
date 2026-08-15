package com.prism.studio.model

/**
 * The vocabulary people actually browse with.
 *
 * Users don't search for "Terracotta". They search for *dark*, *minimal*, *glass*, *gaming* — the
 * words in their head before they know what they want. Facets are that vocabulary, and every
 * family carries a set of them so a single tap narrows forty families to five.
 *
 * Facets are deliberately overlapping: AMOLED Black is Dark *and* Minimal *and* AMOLED. A taxonomy
 * that forces one label per family is a taxonomy that hides things.
 */
enum class Facet(val label: String, val group: FacetGroup) {
    // Style
    Minimal("Minimal", FacetGroup.Style),
    Glass("Glass", FacetGroup.Style),
    Futuristic("Futuristic", FacetGroup.Style),
    Retro("Retro", FacetGroup.Style),
    Textured("Textured", FacetGroup.Style),
    Geometric("Geometric", FacetGroup.Style),
    Editorial("Editorial", FacetGroup.Style),

    // Tone
    Dark("Dark", FacetGroup.Tone),
    Light("Light", FacetGroup.Tone),
    Amoled("AMOLED", FacetGroup.Tone),
    Colourful("Colourful", FacetGroup.Tone),
    Monochrome("Monochrome", FacetGroup.Tone),
    Warm("Warm", FacetGroup.Tone),
    Cool("Cool", FacetGroup.Tone),

    // Feel
    Luxury("Luxury", FacetGroup.Feel),
    Playful("Playful", FacetGroup.Feel),
    Calm("Calm", FacetGroup.Feel),
    Bold("Bold", FacetGroup.Feel),

    // Purpose
    Productivity("Productivity", FacetGroup.Purpose),
    Gaming("Gaming", FacetGroup.Purpose),
    Finance("Finance", FacetGroup.Purpose),
    Fitness("Fitness", FacetGroup.Purpose),
    Music("Music", FacetGroup.Purpose),
    Nature("Nature", FacetGroup.Purpose),
    Space("Space", FacetGroup.Purpose),
    Technical("Technical", FacetGroup.Purpose),
}

enum class FacetGroup(val label: String) {
    Style("Style"), Tone("Tone"), Feel("Feel"), Purpose("For"),
}

/**
 * A browse query.
 *
 * Facets within a group are OR-ed (Dark *or* Light), groups are AND-ed (Dark *and* Minimal). That
 * combination is what people expect from filter chips without being able to articulate it, and
 * getting it backwards is the most common way a filter UI feels broken.
 */
data class BrowseQuery(
    val text: String = "",
    val facets: Set<Facet> = emptySet(),
    val types: Set<WidgetType> = emptySet(),
    val sizes: Set<WidgetSize> = emptySet(),
    val favouritesOnly: Boolean = false,
) {
    val isEmpty: Boolean
        get() = text.isBlank() && facets.isEmpty() && types.isEmpty() && sizes.isEmpty() && !favouritesOnly

    fun matches(familyFacets: Set<Facet>): Boolean =
        facets.groupBy { it.group }.all { (_, group) -> group.any { it in familyFacets } }

    fun toggle(facet: Facet): BrowseQuery =
        copy(facets = if (facet in facets) facets - facet else facets + facet)
}
