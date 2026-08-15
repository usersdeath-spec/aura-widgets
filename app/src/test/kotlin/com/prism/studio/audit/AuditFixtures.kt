package com.prism.studio.audit

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.prism.studio.data.catalog.PrismCatalog
import com.prism.studio.model.DesignFamily
import com.prism.studio.model.DynamicRole
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetSize
import com.prism.studio.model.WidgetStyle
import com.prism.studio.model.WidgetType
import com.prism.studio.render.ColorResolver
import com.prism.studio.render.ContentRendererRegistry
import com.prism.studio.render.Density
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.RenderSize
import com.prism.studio.render.TypefaceProvider
import com.prism.studio.render.WidgetData
import com.prism.studio.render.content.AgendaRenderer
import com.prism.studio.render.content.AnalogClockRenderer
import com.prism.studio.render.content.BitmapSource
import com.prism.studio.render.content.CountdownRenderer
import com.prism.studio.render.content.DayCardRenderer
import com.prism.studio.render.content.DigitalClockRenderer
import com.prism.studio.render.content.GaugeRenderer
import com.prism.studio.render.content.HabitTrackerRenderer
import com.prism.studio.render.content.MonthCalendarRenderer
import com.prism.studio.render.content.MusicPlayerRenderer
import com.prism.studio.render.content.NotesRenderer
import com.prism.studio.render.content.PhotoRenderer
import com.prism.studio.render.content.QuoteRenderer
import com.prism.studio.render.content.SeriesRenderer
import com.prism.studio.render.content.SunriseSunsetRenderer
import com.prism.studio.render.content.SystemInfoRenderer
import com.prism.studio.render.content.TodoRenderer
import com.prism.studio.render.content.WeatherRenderer
import com.prism.studio.render.content.WorldClockRenderer
import com.prism.studio.widget.Accessibility
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs
import kotlin.system.measureNanoTime

/**
 * Test scaffolding for the widget audit.
 *
 * Builds the full render matrix — every family, every variant, at the sizes a user can actually
 * resize to — and supplies representative sample data per widget type. Sample data is chosen to be
 * *awkward* rather than tidy: the longest plausible track title, a 31-day month starting on a
 * Sunday, 100% battery (the widest numeral string), a quote long enough to need three lines. Tidy
 * fixtures pass tests that real content fails.
 */
class AuditFixtures {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val catalog = PrismCatalog()
    private val typefaces = TypefaceProvider(context)
    private val bitmaps = object : BitmapSource {
        override fun bitmap(key: String): Bitmap? =
            Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF556677.toInt()) }
    }
    private val registry = TestRenderers.registry(bitmaps)
    private val renderer = PrismRenderer(registry, typefaces)
    private val colors = ColorResolver(emptyMap(), emptyMap())

    data class Case(
        val family: DesignFamily,
        val widget: ResolvedWidget,
        val size: RenderSize,
        val style: WidgetStyle = widget.style,
    )

    /** Every widget at every size it can be resized to. This is the audit's actual surface area. */
    fun cases(smallestOnly: Boolean = false): List<Case> = catalog.families.flatMap { family ->
        family.variants.flatMap { variant ->
            val sizes = if (smallestOnly) listOf(WidgetSize.Small) else sizesFor(variant.size)
            sizes.map { size ->
                val widget = ResolvedWidget(family, variant, variant.styleDelta.applyTo(family.base), emptyMap())
                Case(family, widget, renderSize(size))
            }
        }
    }

    fun forEachWidget(smallestOnly: Boolean = false, block: (Case) -> Unit) {
        val failures = mutableListOf<String>()
        cases(smallestOnly).forEach { case ->
            runCatching { block(case) }.onFailure {
                failures += "${case.family.id.value}/${case.widget.variant.id.value} " +
                    "@${case.size.widthPx}x${case.size.heightPx}: ${it.message}"
            }
        }
        // Report every failing widget at once. Failing on the first means 700 fix-and-rerun cycles.
        check(failures.isEmpty()) {
            "${failures.size} widgets failed:\n" + failures.take(40).joinToString("\n")
        }
    }

    fun forEachFamily(block: (DesignFamily) -> Unit) = catalog.families.forEach(block)

    fun render(case: Case): Bitmap =
        renderer.draw(case.widget.copy(style = case.style), sampleFor(case.widget.variant.type), case.size, colors)

    fun renderFamilyHero(family: DesignFamily, theme: WidgetAuditTest.Theme): Bitmap {
        val variant = family.variants.first()
        val widget = ResolvedWidget(family, variant, variant.styleDelta.applyTo(family.base), emptyMap())
        return renderer.draw(widget, sampleFor(variant.type), renderSize(variant.size), resolverFor(theme))
    }

    fun describe(case: Case): String =
        Accessibility.describe(case.widget, sampleFor(case.widget.variant.type), context)

    /** Smallest rendered glyph, used by the legibility floor check. */
    fun smallestTypePx(case: Case): Float =
        case.size.density.dp(9f) * case.style.typeScale

    private fun sizesFor(declared: WidgetSize): List<WidgetSize> =
        listOf(WidgetSize.Small, declared, WidgetSize.Large).distinct()

    private fun renderSize(size: WidgetSize) = RenderSize(
        widthPx = size.cellsWide * 90,
        heightPx = size.cellsHigh * 90,
        density = Density(2f),
    )

    private fun resolverFor(theme: WidgetAuditTest.Theme): ColorResolver = when (theme) {
        WidgetAuditTest.Theme.Dynamic -> ColorResolver(
            com.prism.studio.model.DynamicRole.entries.associateWith { 0xFF7C5CFF.toInt() },
            emptyMap(),
        )
        else -> colors
    }

    /** Deliberately awkward sample data. Tidy fixtures pass tests that real content fails. */
    private fun sampleFor(type: WidgetType): WidgetData {
        val now = LocalDateTime.of(2026, 8, 5, 23, 58)
        return when (type) {
            WidgetType.DigitalClock, WidgetType.AnalogClock ->
                WidgetData.Clock(now, is24Hour = false, zoneLabel = null)

            WidgetType.WorldClock -> WidgetData.Zones(
                listOf(
                    WidgetData.Zones.Zone("Los Angeles", LocalTime.of(8, 58), -1),
                    WidgetData.Zones.Zone("London", LocalTime.of(16, 58), 0),
                    WidgetData.Zones.Zone("Tokyo", LocalTime.of(0, 58), 1),
                ),
            )

            WidgetType.Countdown ->
                WidgetData.Countdown("Something with a long name", now.plusDays(97), now)

            WidgetType.DayCard, WidgetType.MonthCalendar, WidgetType.Agenda -> WidgetData.Calendar(
                today = LocalDate.of(2026, 3, 31),
                monthAnchor = LocalDate.of(2026, 3, 1),
                markedDays = setOf(3, 14, 28),
                events = List(5) {
                    WidgetData.Calendar.Event(
                        now.plusHours(it.toLong()),
                        "Quarterly planning review with the platform team",
                        null,
                    )
                },
            )

            WidgetType.Weather -> WidgetData.Weather(
                tempC = -18f, feelsLikeC = -24f,
                condition = WidgetData.Weather.Condition.Storm,
                place = "Srinagar", highC = -11f, lowC = -23f,
                hourly = List(12) { it to (-20f + it) },
            )

            WidgetType.SunriseSunset ->
                WidgetData.Sun(LocalTime.of(5, 12), LocalTime.of(19, 48), LocalTime.of(23, 58), "Srinagar")

            WidgetType.Battery, WidgetType.Cpu, WidgetType.Ram,
            WidgetType.Storage, WidgetType.Network, WidgetType.Steps ->
                WidgetData.Gauge(1f, "100%", "Battery", "Charging", List(24) { it / 24f },
                    WidgetData.Gauge.State.Charging)

            WidgetType.SystemInfo -> WidgetData.System(
                List(6) { WidgetData.System.Row("Label $it", "Value $it", it / 6f) },
            )

            WidgetType.Notes, WidgetType.Todo -> WidgetData.TextRows(
                "Inbox",
                List(8) { WidgetData.TextRows.Row("A task with a fairly long description $it", it % 2 == 0) },
            )

            WidgetType.HabitTracker -> WidgetData.Habits("Morning pages", List(90) { it % 3 != 0 }, 14)

            WidgetType.Quote -> WidgetData.Quote(
                "Simplicity is about subtracting the obvious and adding the meaningful, " +
                    "which is considerably harder than it sounds.",
                "John Maeda",
            )

            WidgetType.Photo -> WidgetData.Photo("sample", "A caption long enough to need truncating")

            WidgetType.MusicPlayer -> WidgetData.Media(
                "A Song Title That Is Unreasonably Long For A Widget",
                "An Artist With A Long Name", playing = true, artworkKey = "sample", progress = 0.42f,
            )

            WidgetType.Finance, WidgetType.Crypto, WidgetType.Health -> WidgetData.Series(
                "PORTFOLIO", "1,284,003.55", -12.48f, List(30) { kotlin.math.sin(it / 3.0).toFloat() + 1f },
            )
        }
    }
}

/** Mirrors the app's DI registry so the audit tests the shipping renderer set, not a subset. */
object TestRenderers {
    fun registry(bitmaps: BitmapSource) = ContentRendererRegistry(
        buildList {
            add(DigitalClockRenderer())
            add(AnalogClockRenderer())
            add(WorldClockRenderer())
            add(CountdownRenderer())
            add(DayCardRenderer())
            add(MonthCalendarRenderer())
            add(AgendaRenderer())
            add(WeatherRenderer())
            add(SunriseSunsetRenderer())
            addAll(GaugeRenderer.all())
            add(SystemInfoRenderer())
            add(NotesRenderer())
            add(TodoRenderer())
            add(HabitTrackerRenderer())
            add(QuoteRenderer())
            add(MusicPlayerRenderer(bitmaps))
            add(PhotoRenderer(bitmaps))
            addAll(SeriesRenderer.all())
        },
    ).also { it.assertComplete() }
}
