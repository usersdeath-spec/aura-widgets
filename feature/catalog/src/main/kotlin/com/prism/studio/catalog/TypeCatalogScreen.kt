package com.prism.studio.catalog

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prism.studio.design.SectionHeader
import com.prism.studio.design.Space
import com.prism.studio.design.WidgetPreview
import com.prism.studio.design.pressable
import com.prism.studio.model.DesignFamily
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetType
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.WidgetData

/**
 * Browse by widget type.
 *
 * This is the shape the competitors use, and tearing down Glass Widgets showed why: their catalog
 * is grouped Battery, Apps, Calendar, Clock, Compass, Contacts, Earbuds, Weather — by **what the
 * widget does**, never by what it looks like. Their provider names bear it out too: 11
 * `new_clock_digi`, 6 `battery_percentage_with_progress`, 6 `ear_buds_battery`.
 *
 * That matches how someone actually arrives at this app. Nobody opens it wanting "a Marble widget";
 * they want a battery widget and then choose which one they like. Family-first browsing asks them
 * to pick an aesthetic before they have picked a function, which is backwards, and with 59 families
 * it is also a very long scroll before you find the thing you came for.
 *
 * Family-first still exists on the other tab, because once someone has settled on a look they do
 * want the matching set — that is what our seven-pillar rule is for, and it is the thing the
 * competitors cannot offer.
 */
@Composable
fun TypeCatalogScreen(
    families: List<DesignFamily>,
    resolve: (DesignFamily, Int) -> ResolvedWidget,
    sampleData: (ResolvedWidget) -> WidgetData,
    renderer: PrismRenderer,
    backdropFor: (DesignFamily) -> List<Color>,
    onPick: (ResolvedWidget) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Built once: for each type, every family's take on it. This is the whole point — sixty
    // different battery widgets on one row, which is exactly what the competitors ship and what our
    // family-first shelves were hiding.
    val byType = remember(families) {
        WidgetType.entries.mapNotNull { type ->
            val designs = families.flatMap { family ->
                family.variants.withIndex()
                    .filter { it.value.type == type }
                    .map { resolve(family, it.index) }
            }
            if (designs.isEmpty()) null else type to designs
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Space.section.dp),
        verticalArrangement = Arrangement.spacedBy(Space.loose.dp),
    ) {
        item(key = "header") {
            Column(Modifier.padding(top = Space.base.dp)) {
                Text(
                    "By type",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(horizontal = Space.base.dp),
                )
                Text(
                    "Every family's take on the same widget, side by side",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = Space.base.dp,
                        vertical = Space.hair.dp,
                    ),
                )
            }
        }

        items(byType, key = { it.first.name }) { (type, designs) ->
            Column {
                SectionHeader(label(type), designs.size, blurb(type))

                LazyRow(
                    contentPadding = PaddingValues(
                        horizontal = Space.base.dp,
                        vertical = Space.tight.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
                ) {
                    items(designs.size) { index ->
                        val widget = designs[index]
                        val width = (SHELF_HEIGHT_DP * widget.variant.size.aspectRatio)
                            .coerceIn(96f, 300f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Space.hair.dp),
                        ) {
                            WidgetPreview(
                                widget = widget,
                                data = sampleData(widget),
                                renderer = renderer,
                                backdrop = Brush.linearGradient(backdropFor(widget.family)),
                                modifier = Modifier
                                    .height(SHELF_HEIGHT_DP.dp)
                                    .width(width.dp)
                                    .pressable(onClick = { onPick(widget) }),
                            )
                            // The family name under each tile, so a row of sixty batteries is
                            // still navigable — and so someone who likes one can find its matching
                            // set on the family tab.
                            Text(
                                widget.family.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(width.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Plain names. `MonthCalendar` is a type identifier, not something to show a person. */
private fun label(type: WidgetType): String = when (type) {
    WidgetType.DigitalClock -> "Digital Clock"
    WidgetType.AnalogClock -> "Analog Clock"
    WidgetType.WorldClock -> "World Clock"
    WidgetType.Countdown -> "Countdown"
    WidgetType.DayCard -> "Date"
    WidgetType.MonthCalendar -> "Calendar"
    WidgetType.Agenda -> "Agenda"
    WidgetType.Weather -> "Weather"
    WidgetType.SunriseSunset -> "Sunrise & Sunset"
    WidgetType.Battery -> "Battery"
    WidgetType.Cpu -> "Processor"
    WidgetType.Ram -> "Memory"
    WidgetType.Storage -> "Storage"
    WidgetType.Network -> "Network"
    WidgetType.SystemInfo -> "Device Info"
    WidgetType.Notes -> "Notes"
    WidgetType.Todo -> "Tasks"
    WidgetType.HabitTracker -> "Habits"
    WidgetType.Quote -> "Quotes"
    WidgetType.Photo -> "Photo"
    WidgetType.MusicPlayer -> "Music"
    WidgetType.Finance -> "Finance"
    WidgetType.Crypto -> "Crypto"
    WidgetType.Steps -> "Steps"
    WidgetType.Health -> "Activity"
}

private fun blurb(type: WidgetType): String = when (type) {
    WidgetType.Battery -> "Opens battery settings"
    WidgetType.Storage -> "Opens storage settings"
    WidgetType.Ram, WidgetType.Cpu, WidgetType.SystemInfo -> "Opens device info"
    WidgetType.Network -> "Opens data usage"
    WidgetType.DigitalClock, WidgetType.AnalogClock,
    WidgetType.WorldClock, WidgetType.Countdown -> "Opens your alarms"
    WidgetType.MonthCalendar, WidgetType.DayCard, WidgetType.Agenda -> "Opens your calendar"
    WidgetType.MusicPlayer -> "Opens your music app"
    WidgetType.Weather, WidgetType.SunriseSunset -> "Opens weather"
    else -> "Opens Aura"
}

private const val SHELF_HEIGHT_DP = 100f
