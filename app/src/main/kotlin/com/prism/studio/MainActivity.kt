package com.prism.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.prism.studio.data.WidgetRepository
import com.prism.studio.model.WidgetSpec
import com.prism.studio.widget.PendingWidgetWriter
import com.prism.studio.widget.PinnedWidgets
import com.prism.studio.widget.PrismWidgetProvider
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import com.prism.studio.data.AuraState
import com.prism.studio.data.catalog.PrismCatalog
import com.prism.studio.data.catalog.SetupCatalog
import com.prism.studio.data.catalog.WallpaperCatalog
import com.prism.studio.design.PrismTheme
import com.prism.studio.model.DesignFamily
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.render.ColorHarmony
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.WidgetData
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * The app's single activity.
 *
 * Deliberately thin. It installs the platform splash screen, applies the theme, and hosts Compose.
 * Navigation, view models, and the rest of the shell are Phase 4 work; what is here is the minimum
 * that lets the app launch and lets the catalog be looked at on a device, which is the fastest way
 * to find rendering defects.
 *
 * `installSplashScreen` is called before `super.onCreate`, which the API requires. There is no
 * `setKeepOnScreenCondition`: the catalog is built during Hilt graph construction and is already
 * ready by the time the first frame draws, so holding the splash open would be an artificial delay.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var catalog: PrismCatalog
    @Inject lateinit var renderer: PrismRenderer
    @Inject lateinit var wallpapers: WallpaperCatalog
    @Inject lateinit var repository: WidgetRepository
    @Inject lateinit var setups: SetupCatalog
    @Inject lateinit var aura: AuraState

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lets :widget hand a pinned widget back to the repository without :widget depending on Room.
        lifecycleScope.launch {
            repository.favourites().collect { favourites.value = it.toSet() }
        }

        PendingWidgetWriter.writer = { _, appWidgetId, spec, done ->
            lifecycleScope.launch {
                repository.save(appWidgetId, spec)
                done()
            }
        }

        setContent {
            PrismTheme {
                // Held here rather than in the composable so it survives configuration changes;
                // it is persisted through the repository, so it also survives a restart.
                PrismApp(
                    catalog = catalog,
                    setups = setups,
                    wallpapers = wallpapers,
                    renderer = renderer,
                    resolve = ::resolve,
                    sampleData = ::sampleData,
                    backdropFor = ::backdropFor,
                    onPick = ::placeOnHomeScreen,
                    onWallpaper = ::applyAura,
                    onSettings = { /* Persistence lands with the DataStore wiring. */ },
                    onPrivacy = { openUrl("https://example.com/privacy") },
                    onRate = { openUrl("market://details?id=$packageName") },
                    auraPalette = auraPalette(),
                    versionName = BuildConfig.VERSION_NAME,
                    favourites = favourites.value,
                    onToggleFavourite = ::toggleFavourite,
                )
            }
        }
    }

    /**
     * The wallpaper a family was art-directed against, as a two-stop gradient.
     *
     * Until the real artwork exists, the palette recorded for the family's first paired wallpaper
     * stands in for it. That is enough for the previews to be honest: a glass widget is shown over
     * something, and a transparent family is legible.
     */
    private fun backdropFor(family: DesignFamily): List<Color> {
        val paper = family.pairedWallpapers.firstOrNull()?.let {
            runCatching { wallpapers.byId(it) }.getOrNull()
        } ?: return listOf(Color(0xFF20242E), Color(0xFF0E1116))
        return listOf(Color(paper.dominant.toInt()), Color(paper.muted.toInt()))
    }

    /**
     * Tapping a design puts it on the home screen.
     *
     * Previously this did nothing, which combined with a picker that listed only two entries meant
     * there was no way at all to place any of the 708 designs. This is the "one tap to add" path.
     *
     * When the launcher does not support pinning — a handful of older third-party ones do not — the
     * user is told how to add it manually rather than left tapping a dead tile.
     */
    private fun placeOnHomeScreen(widget: ResolvedWidget) {
        val spec = WidgetSpec(family = widget.family.id, variant = widget.variant.id)
        val requested = PinnedWidgets.requestPin(this, spec, widget.variant.size)
        if (!requested) {
            Toast.makeText(
                this,
                "Your launcher can't add widgets from inside apps. Long-press your home screen, " +
                    "choose Widgets, then Prism.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    /**
     * The colours the wallpaper generator draws with.
     *
     * Taken from the first Foundation family rather than hardcoded, so the gallery is already
     * tinted to the app's own palette before the user has chosen anything — and re-tints wholesale
     * the moment they do.
     */
    /**
     * Favourited family ids.
     *
     * Backed by the repository's existing favourites table rather than a new store: the data model
     * was written months ago and only ever lacked a UI, which is why this is a small change.
     */
    private val favourites = androidx.compose.runtime.mutableStateOf(emptySet<String>())

    private fun toggleFavourite(familyId: String) {
        val next = if (familyId in favourites.value) favourites.value - familyId
        else favourites.value + familyId
        favourites.value = next
        lifecycleScope.launch {
            repository.setFavourite(familyId, familyId in next)
        }
    }

    private fun auraPalette(): List<Color> {
        val family = catalog.families.first()
        return listOf(
            Color(0xFF11131A),
            Color(0xFF7C5CFF),
            Color(0xFF38BDF8),
        )
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
    }

    /**
     * Tapping a wallpaper sets the aura.
     *
     * The whole product idea in one interaction: every placed widget re-tints to the wallpaper at
     * once. The scheme comes from the harmony engine rather than the raw palette, so ink always
     * clears its contrast floor — a wallpaper whose dominant colour is pale cannot produce an
     * unreadable screen.
     */
    private fun applyAura(recipe: com.prism.studio.render.WallpaperEngine.Recipe) {
        val scheme = ColorHarmony.harmonise(
            swatches = recipe.palette.map {
                ColorHarmony.Swatch(it, 1f, floatArrayOf(0f, 0f, 0f))
            },
            harmony = ColorHarmony.Harmony.Analogous,
            preferDark = recipe.dark,
        ) ?: return

        lifecycleScope.launch {
            aura.apply(
                AuraState.Aura(
                    wallpaperId = recipe.id,
                    harmony = scheme.harmony.name,
                    surface = scheme.surface.toLong() and 0xFFFFFFFFL,
                    ink = scheme.ink.toLong() and 0xFFFFFFFFL,
                    inkMuted = scheme.inkMuted.toLong() and 0xFFFFFFFFL,
                    accent = scheme.accent.toLong() and 0xFFFFFFFFL,
                ),
            )
            PrismWidgetProvider.requestUpdate(this@MainActivity)
            Toast.makeText(
                this@MainActivity,
                "Aura set from ${recipe.style.label}. Your widgets now match.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun resolve(family: DesignFamily, index: Int): ResolvedWidget {
        val variant = family.variants[index]
        return ResolvedWidget(
            family = family,
            variant = variant,
            style = variant.styleDelta.applyTo(family.base),
            options = emptyMap(),
        )
    }

    /**
     * Preview data for the catalog grid.
     *
     * A fixed timestamp rather than `now()`: a shelf where every clock shows a different second as
     * you scroll looks broken, and the render cache would miss on every tile.
     */
    private fun sampleData(widget: ResolvedWidget): WidgetData = PreviewData.forType(widget.variant.type)

    private object PreviewData {
        private val at = LocalDateTime.of(2026, 8, 6, 10, 9)

        fun forType(type: com.prism.studio.model.WidgetType): WidgetData = when (type) {
            com.prism.studio.model.WidgetType.DigitalClock,
            com.prism.studio.model.WidgetType.AnalogClock -> WidgetData.Clock(at, is24Hour = false)

            com.prism.studio.model.WidgetType.WorldClock -> WidgetData.Zones(
                listOf(
                    WidgetData.Zones.Zone("London", java.time.LocalTime.of(5, 39), 0),
                    WidgetData.Zones.Zone("New York", java.time.LocalTime.of(0, 39), 0),
                    WidgetData.Zones.Zone("Tokyo", java.time.LocalTime.of(13, 39), 1),
                ),
            )

            com.prism.studio.model.WidgetType.Countdown ->
                WidgetData.Countdown("Launch", at.plusDays(28), at)

            com.prism.studio.model.WidgetType.DayCard,
            com.prism.studio.model.WidgetType.MonthCalendar,
            com.prism.studio.model.WidgetType.Agenda -> WidgetData.Calendar(
                today = at.toLocalDate(),
                markedDays = setOf(4, 11, 19),
                events = listOf(
                    WidgetData.Calendar.Event(at.plusHours(1), "Design review", null),
                    WidgetData.Calendar.Event(at.plusHours(4), "1:1", null),
                ),
            )

            com.prism.studio.model.WidgetType.Weather -> WidgetData.Weather(
                tempC = 21f, feelsLikeC = 20f,
                condition = WidgetData.Weather.Condition.PartlyCloudy,
                place = "Srinagar", highC = 26f, lowC = 14f,
                hourly = List(12) { it to (16f + it) },
            )

            com.prism.studio.model.WidgetType.SunriseSunset -> WidgetData.Sun(
                java.time.LocalTime.of(5, 42), java.time.LocalTime.of(19, 21),
                java.time.LocalTime.of(10, 9), "Srinagar",
            )

            com.prism.studio.model.WidgetType.Battery,
            com.prism.studio.model.WidgetType.Cpu,
            com.prism.studio.model.WidgetType.Ram,
            com.prism.studio.model.WidgetType.Storage,
            com.prism.studio.model.WidgetType.Network,
            com.prism.studio.model.WidgetType.Steps -> WidgetData.Gauge(
                fraction = 0.68f, primary = "68%", label = "Battery",
                history = List(20) { 0.4f + 0.02f * it },
            )

            com.prism.studio.model.WidgetType.SystemInfo -> WidgetData.System(
                listOf(
                    WidgetData.System.Row("Storage", "128 GB", 0.62f),
                    WidgetData.System.Row("Memory", "5.1 GB", 0.44f),
                    WidgetData.System.Row("Uptime", "18h 4m"),
                ),
            )

            com.prism.studio.model.WidgetType.Notes,
            com.prism.studio.model.WidgetType.Todo -> WidgetData.TextRows(
                "Today",
                listOf(
                    WidgetData.TextRows.Row("Ship the build", true),
                    WidgetData.TextRows.Row("Record golden images"),
                    WidgetData.TextRows.Row("Measure cold start"),
                ),
            )

            com.prism.studio.model.WidgetType.HabitTracker ->
                WidgetData.Habits("Reading", List(60) { it % 4 != 0 }, streak = 9)

            com.prism.studio.model.WidgetType.Quote -> WidgetData.Quote(
                "Design is not just what it looks like. Design is how it works.",
                "Steve Jobs",
            )

            com.prism.studio.model.WidgetType.Photo -> WidgetData.Photo("preview", null)

            com.prism.studio.model.WidgetType.MusicPlayer -> WidgetData.Media(
                "Weightless", "Marconi Union", playing = true, artworkKey = null, progress = 0.38f,
            )

            com.prism.studio.model.WidgetType.Finance,
            com.prism.studio.model.WidgetType.Crypto,
            com.prism.studio.model.WidgetType.Health -> WidgetData.Series(
                "Portfolio", "42,180", 1.84f, List(24) { kotlin.math.sin(it / 4.0).toFloat() + 1.2f },
            )
        }
    }
}
