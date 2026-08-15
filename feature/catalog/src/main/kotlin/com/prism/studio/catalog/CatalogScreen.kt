package com.prism.studio.catalog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.prism.studio.design.Motion
import com.prism.studio.design.PrismChip
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import com.prism.studio.design.SearchRow
import com.prism.studio.design.SectionHeader
import com.prism.studio.design.Space
import com.prism.studio.design.WidgetPreview
import com.prism.studio.design.pressable
import com.prism.studio.model.DesignFamily
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.WidgetData
import kotlinx.coroutines.delay

/**
 * The widget catalog.
 *
 * Rebuilt after seeing the first version on a device next to what it competes with. That version was
 * a flat list of 59 identical shelves with no entry point, no filtering, no hierarchy and no
 * feedback on touch — technically a catalog, and it read as a debug screen.
 *
 * What changed, and why each one:
 *
 * **A hero row.** The first screenful now shows large previews from the flagship collection rather
 * than whatever family happens to sort first. Every competing listing leads with its best work; an
 * app whose first impression is an alphabetical list is throwing away the only screen most people
 * look at.
 *
 * **Filter chips.** 59 families is a wall. The chips are the words people actually browse with —
 * Glass, AMOLED, Minimal, Luxury — and narrow the list to something a person can finish looking at.
 *
 * **Touch feedback.** Every tile presses in and springs back. Its absence is most of what made the
 * old build feel unfinished: nothing on screen acknowledged being touched.
 *
 * **Staggered entrance.** Shelves fade and rise as they arrive, capped so late rows never feel
 * queued. The screen assembles instead of appearing whole, which reads as fast.
 */
@Composable
fun CatalogScreen(
    families: List<DesignFamily>,
    resolve: (DesignFamily, Int) -> ResolvedWidget,
    sampleData: (ResolvedWidget) -> WidgetData,
    renderer: PrismRenderer,
    backdropFor: (DesignFamily) -> List<Color>,
    facets: List<String>,
    familiesForFacet: (String) -> List<DesignFamily>,
    featured: List<DesignFamily>,
    favourites: Set<String>,
    onToggleFavourite: (String) -> Unit,
    onPick: (ResolvedWidget) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeFacet by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var favouritesOnly by remember { mutableStateOf(false) }

    /**
     * Search matches the family name, its one-line note, and its facets — not just the name.
     * Someone typing "dark" or "glass" is describing a mood, and a name-only search would return
     * nothing for the words people actually use.
     */
    val shown = remember(activeFacet, families, query, favouritesOnly, favourites) {
        var result = activeFacet?.let(familiesForFacet) ?: families
        if (favouritesOnly) result = result.filter { it.id.value in favourites }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { family ->
                family.name.lowercase().contains(q) ||
                    family.note.lowercase().contains(q) ||
                    family.mood.name.lowercase().contains(q) ||
                    family.variants.any { it.name.lowercase().contains(q) } ||
                    family.variants.any { it.type.name.lowercase().contains(q) }
            }
        }
        result
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Space.section.dp),
        verticalArrangement = Arrangement.spacedBy(Space.loose.dp),
    ) {
        item(key = "header") {
            Column(Modifier.padding(top = Space.base.dp)) {
                Text(
                    "Widgets",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(horizontal = Space.base.dp),
                )
                Text(
                    "${families.size} families · ${families.sumOf { it.variants.size }} designs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Space.base.dp, vertical = Space.hair.dp),
                )

                SearchRow(
                    query = query,
                    favouritesOnly = favouritesOnly,
                    resultCount = shown.sumOf { it.variants.size },
                    onQuery = { query = it },
                    onFavouritesOnly = { favouritesOnly = it },
                    modifier = Modifier.padding(vertical = Space.tight.dp),
                )

                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Space.base.dp, vertical = Space.tight.dp),
                    horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
                ) {
                    PrismChip("All", activeFacet == null, onClick = { activeFacet = null })
                    facets.forEach { facet ->
                        PrismChip(
                            label = facet,
                            selected = activeFacet == facet,
                            onClick = { activeFacet = if (activeFacet == facet) null else facet },
                        )
                    }
                }
            }
        }

        if (activeFacet == null && query.isBlank() && !favouritesOnly && featured.isNotEmpty()) {
            item(key = "featured") {
                Column(verticalArrangement = Arrangement.spacedBy(Space.tight.dp)) {
                    SectionHeader("Featured", subtitle = "What we would put on our own phones")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Space.base.dp),
                        horizontalArrangement = Arrangement.spacedBy(Space.base.dp),
                    ) {
                        items(featured, key = { it.id.value }) { family ->
                            val widget = resolve(family, 0)
                            Column(
                                Modifier.width(240.dp),
                                verticalArrangement = Arrangement.spacedBy(Space.hair.dp),
                            ) {
                                WidgetPreview(
                                    widget = widget,
                                    data = sampleData(widget),
                                    renderer = renderer,
                                    backdrop = Brush.linearGradient(backdropFor(family)),
                                    cornerRadiusDp = 22,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .pressable(onClick = { onPick(widget) }),
                                )
                                Text(family.name, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        if (shown.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Nothing matches that yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(Space.section.dp),
                )
            }
        }

        itemsIndexed(shown, key = { _, family -> family.id.value }) { index, family ->
            FamilyShelf(
                family = family,
                index = index,
                favourite = family.id.value in favourites,
                onToggleFavourite = { onToggleFavourite(family.id.value) },
                resolve = resolve,
                sampleData = sampleData,
                renderer = renderer,
                backdropColors = backdropFor(family),
                onPick = onPick,
            )
        }
    }
}

@Composable
private fun FamilyShelf(
    family: DesignFamily,
    index: Int,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    resolve: (DesignFamily, Int) -> ResolvedWidget,
    sampleData: (ResolvedWidget) -> WidgetData,
    renderer: PrismRenderer,
    backdropColors: List<Color>,
    onPick: (ResolvedWidget) -> Unit,
) {
    val backdrop = Brush.linearGradient(
        colors = if (backdropColors.size >= 2) backdropColors
        else listOf(Color(0xFF20242E), Color(0xFF0E1116)),
    )

    // Rise and fade on arrival, capped by Motion.staggerDelay so the twentieth shelf does not wait
    // in a queue behind the first nineteen.
    var visible by remember(family.id) { mutableStateOf(false) }
    val appear by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = Motion.enterSpec(Motion.standard),
        label = "shelfAppear",
    )
    LaunchedEffect(family.id) {
        delay(Motion.staggerDelay(index).toLong())
        visible = true
    }

    Column(Modifier.alpha(appear)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(family.name, family.variants.size, family.note, Modifier.weight(1f))
            Icon(
                if (favourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favourite) "Remove from favourites" else "Add to favourites",
                tint = if (favourite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = Space.base.dp)
                    .size(22.dp)
                    .pressable(onClick = onToggleFavourite),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = Space.base.dp, vertical = Space.tight.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
        ) {
            // Each family starts on a different widget type: every variant list begins clock,
            // weather, month, so an unrotated catalog showed the same clock 59 times and read as
            // one design recoloured.
            val offset = family.id.value.hashCode().let { if (it < 0) -it else it } % family.variants.size

            items(family.variants.size) { position ->
                val widget = resolve(family, (position + offset) % family.variants.size)
                val width = (SHELF_HEIGHT_DP * widget.variant.size.aspectRatio).coerceIn(96f, 300f)

                WidgetPreview(
                    widget = widget,
                    data = sampleData(widget),
                    renderer = renderer,
                    backdrop = backdrop,
                    modifier = Modifier
                        .height(SHELF_HEIGHT_DP.dp)
                        .width(width.dp)
                        .pressable(onClick = { onPick(widget) }),
                )
            }
        }
    }
}

/**
 * Shelf height. At 132dp one and a half widgets were visible and a 59-family catalog felt both empty
 * and endless. At 100dp four or five show per shelf, which is the density every competing store
 * listing leads with, because density is the proof of value.
 */
private const val SHELF_HEIGHT_DP = 100f
