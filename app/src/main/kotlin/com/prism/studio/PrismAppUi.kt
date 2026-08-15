package com.prism.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prism.studio.catalog.CatalogScreen
import com.prism.studio.catalog.TypeCatalogScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import com.prism.studio.data.catalog.FeaturedShelves
import com.prism.studio.data.catalog.PrismCatalog
import com.prism.studio.design.rememberHaptics
import com.prism.studio.model.Facet
import com.prism.studio.data.catalog.SetupCatalog
import com.prism.studio.data.catalog.WallpaperCatalog
import com.prism.studio.settings.SettingsScreen
import com.prism.studio.settings.SettingsState
import com.prism.studio.wallpapers.WallpapersScreen
import com.prism.studio.design.Space
import com.prism.studio.design.WidgetPreview
import com.prism.studio.model.DesignFamily
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.WidgetData

/**
 * The app shell.
 *
 * One endless scroll of 59 shelves was the wrong shape: it gave no sense of what the app contains
 * and no way to reach anything specific. Three destinations instead — the widget catalog, the
 * finished home screens, and the wallpapers — which is also how every app in this category is
 * organised, because it matches how people shop for this: "show me everything" versus "give me a
 * finished look" are different intents.
 */
@Composable
fun PrismApp(
    catalog: PrismCatalog,
    setups: SetupCatalog,
    wallpapers: WallpaperCatalog,
    renderer: PrismRenderer,
    resolve: (DesignFamily, Int) -> ResolvedWidget,
    sampleData: (ResolvedWidget) -> WidgetData,
    backdropFor: (DesignFamily) -> List<Color>,
    onPick: (ResolvedWidget) -> Unit,
    onWallpaper: (com.prism.studio.render.WallpaperEngine.Recipe) -> Unit,
    onSettings: (SettingsState) -> Unit,
    onPrivacy: () -> Unit,
    onRate: () -> Unit,
    auraPalette: List<Color>,
    versionName: String,
    favourites: Set<String>,
    onToggleFavourite: (String) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(SettingsState()) }
    val tabs = listOf("Widgets", "Styles", "Wallpapers", "Settings")
    val haptics = rememberHaptics()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { haptics.tick(); tab = index },
                        icon = { Icon(TAB_ICONS[index], contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { insets ->
        // Consume BOTH insets. The bottom one clears the navigation bar; the top one clears the
        // status bar and the notch, which edge-to-edge otherwise draws straight through.
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    top = insets.calculateTopPadding(),
                    bottom = insets.calculateBottomPadding(),
                ),
        ) {
            when (tab) {
                0 -> TypeCatalogScreen(
                    families = catalog.families,
                    resolve = resolve,
                    sampleData = sampleData,
                    renderer = renderer,
                    backdropFor = backdropFor,
                    onPick = onPick,
                )

                1 -> StylesTab(
                    catalog = catalog,
                    setups = setups,
                    renderer = renderer,
                    resolve = resolve,
                    sampleData = sampleData,
                    backdropFor = backdropFor,
                    favourites = favourites,
                    onToggleFavourite = onToggleFavourite,
                    onPick = onPick,
                )

                2 -> WallpapersScreen(
                    palette = auraPalette,
                    onPick = onWallpaper,
                )

                else -> SettingsScreen(
                    state = settings,
                    versionName = versionName,
                    widgetCount = catalog.widgetCount,
                    familyCount = catalog.families.size,
                    onState = { settings = it; onSettings(it) },
                    onPrivacy = onPrivacy,
                    onRate = onRate,
                )
            }
        }
    }
}

/**
 * Finished home screens.
 *
 * A setup is shown by its own clock over its own wallpaper — the fastest honest summary of what
 * applying it would look like, without pretending to be a full home-screen mockup we cannot yet
 * render.
 */
/**
 * The Styles tab: curated setups first, then the full family catalog.
 *
 * Setups lost their own tab when type-first browsing took the front position, and dropping them
 * would have thrown away 28 finished home screens. They belong here anyway — someone on this tab is
 * choosing a look, and a finished look is the fastest possible answer to that.
 */
@Composable
private fun StylesTab(
    catalog: PrismCatalog,
    setups: SetupCatalog,
    renderer: PrismRenderer,
    resolve: (DesignFamily, Int) -> ResolvedWidget,
    sampleData: (ResolvedWidget) -> WidgetData,
    backdropFor: (DesignFamily) -> List<Color>,
    favourites: Set<String>,
    onToggleFavourite: (String) -> Unit,
    onPick: (ResolvedWidget) -> Unit,
) {
    CatalogScreen(
        families = catalog.families,
        resolve = resolve,
        sampleData = sampleData,
        renderer = renderer,
        backdropFor = backdropFor,
        facets = BROWSE_FACETS.map { it.label },
        familiesForFacet = { label ->
            val facet = BROWSE_FACETS.first { it.label == label }
            catalog.families.filter { facet in catalog.facetsOf(it.id) }
        },
        featured = FeaturedShelves.CuratedShelf.EditorsPicks.families(catalog),
        favourites = favourites,
        onToggleFavourite = onToggleFavourite,
        onPick = onPick,
    )
}

@Suppress("unused")
@Composable
private fun SetupsScreen(
    catalog: PrismCatalog,
    setups: SetupCatalog,
    renderer: PrismRenderer,
    resolve: (DesignFamily, Int) -> ResolvedWidget,
    sampleData: (ResolvedWidget) -> WidgetData,
    backdropFor: (DesignFamily) -> List<Color>,
    onPick: (ResolvedWidget) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(Space.base.dp),
        verticalArrangement = Arrangement.spacedBy(Space.loose.dp),
    ) {
        items(setups.setups, key = { it.id }) { setup ->
            val family = catalog.family(setup.family)
            val widget = resolve(family, 0)
            Column(verticalArrangement = Arrangement.spacedBy(Space.hair.dp)) {
                Text(setup.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    setup.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WidgetPreview(
                    widget = widget,
                    data = sampleData(widget),
                    renderer = renderer,
                    backdrop = Brush.linearGradient(backdropFor(family)),
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
                Text(
                    "${setup.category.label} · ${family.name} · grid ${setup.launcher.gridLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val TAB_ICONS = listOf(
    Icons.Filled.GridView,
    Icons.Filled.Palette,
    Icons.Filled.Image,
    Icons.Filled.Settings,
)

/**
 * The browse vocabulary offered as chips.
 *
 * Seven, not twenty-seven. A filter row long enough to scroll is a filter row nobody reads, and
 * these are the words the market audit found people actually search: the same terms the top-selling
 * competitors put in their app titles.
 */
private val BROWSE_FACETS = listOf(
    Facet.Glass,
    Facet.Amoled,
    Facet.Minimal,
    Facet.Luxury,
    Facet.Dark,
    Facet.Colourful,
    Facet.Gaming,
)
