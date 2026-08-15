package com.prism.studio.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prism.studio.design.Space
import com.prism.studio.design.pressable

/**
 * Settings.
 *
 * The app had none, which was a real gap rather than a cosmetic one: all four competitors ship a
 * settings tab, and looking at what is actually in theirs shows why. Almost every row is a widget
 * *preference* — 24-hour clock, °F, week starts Monday, date format, which music app to open — not
 * an app preference. Without this screen a user who wants a 24-hour clock has no way to get one,
 * across every widget they own.
 *
 * Two things here that theirs do not have, and both follow from the aura:
 *
 *  * **Aura strength**, because our widgets take colour from the wallpaper and some people want
 *    that at half volume rather than all or nothing.
 *  * **A privacy row that says something true.** Theirs list a policy link; ours states the claim
 *    on the screen, because "no accounts, no analytics, no trackers" is the differentiator and it
 *    should be visible without opening a web page.
 *
 * Deliberately absent: the "Exclusive Gift" and "More Apps" cross-promo panels that occupy the top
 * of two competitors\' settings screens. Those exist to funnel users into the studio\'s other paid
 * apps, and in an app sold as "buy once, nothing else to buy" they would contradict the pitch.
 */
@Composable
fun SettingsScreen(
    state: SettingsState,
    versionName: String,
    widgetCount: Int,
    familyCount: Int,
    onState: (SettingsState) -> Unit,
    onPrivacy: () -> Unit,
    onRate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.base.dp,
            end = Space.base.dp,
            bottom = Space.section.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.base.dp),
    ) {
        item {
            Column(Modifier.padding(top = Space.base.dp, bottom = Space.tight.dp)) {
                Text("Settings", style = MaterialTheme.typography.displaySmall)
                Text(
                    "$familyCount families · $widgetCount designs · v$versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.hair.dp),
                )
            }
        }

        item {
            SettingsGroup("Widgets") {
                SwitchRow(
                    icon = Icons.Filled.Schedule,
                    title = "24-hour clock",
                    subtitle = "Applies to every clock you have placed",
                    checked = state.use24Hour,
                    onChange = { onState(state.copy(use24Hour = it)) },
                )
                SwitchRow(
                    icon = Icons.Filled.Thermostat,
                    title = "Temperature in °F",
                    subtitle = "Weather and forecast widgets",
                    checked = state.fahrenheit,
                    onChange = { onState(state.copy(fahrenheit = it)) },
                )
                SwitchRow(
                    icon = Icons.Filled.Schedule,
                    title = "Week starts on Monday",
                    subtitle = "Month grids and habit trackers",
                    checked = state.mondayFirst,
                    onChange = { onState(state.copy(mondayFirst = it)) },
                )
            }
        }

        item {
            SettingsGroup("Aura") {
                SwitchRow(
                    icon = Icons.Filled.Palette,
                    title = "Match widgets to wallpaper",
                    subtitle = "Every widget takes its colour from the wallpaper you pick",
                    checked = state.auraEnabled,
                    onChange = { onState(state.copy(auraEnabled = it)) },
                )
                if (state.auraEnabled) {
                    StrengthRow(
                        strength = state.auraStrength,
                        onChange = { onState(state.copy(auraStrength = it)) },
                    )
                }
                SwitchRow(
                    icon = Icons.Filled.Brightness6,
                    title = "Keep text readable",
                    subtitle = "Nudges colours until text clears the contrast floor. " +
                        "Turning this off can make widgets hard to read.",
                    checked = state.enforceContrast,
                    onChange = { onState(state.copy(enforceContrast = it)) },
                )
            }
        }

        item {
            SettingsGroup("About") {
                // Stated on the screen rather than hidden behind a link, because it is the claim
                // that justifies a paid app with no in-app purchases.
                InfoRow(
                    icon = Icons.Filled.Shield,
                    title = "No accounts. No analytics. No trackers.",
                    subtitle = "Colour matching and wallpaper generation run on this device. " +
                        "Nothing is uploaded.",
                    onClick = onPrivacy,
                )
                InfoRow(
                    icon = Icons.Filled.Star,
                    title = "Rate Aura Widgets",
                    subtitle = "One purchase, everything unlocked, forever",
                    onClick = onRate,
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.hair.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Space.hair.dp),
        )
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        ) { content() }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = { onChange(!checked) })
            .padding(Space.base.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.base.dp),
    ) {
        IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(Space.base.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.base.dp),
    ) {
        IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Three steps rather than a slider: nobody can tell 40% aura from 45%, and a slider implies they can. */
@Composable
private fun StrengthRow(strength: SettingsState.AuraStrength, onChange: (SettingsState.AuraStrength) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(Space.base.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
    ) {
        SettingsState.AuraStrength.entries.forEach { option ->
            val selected = option == strength
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .pressable(onClick = { onChange(option) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Every preference in one immutable object.
 *
 * Widget preferences are global rather than per-widget on purpose: a user who wants a 24-hour clock
 * wants it on all nine clocks they placed, and making them set it nine times is the kind of thing
 * that generates a one-star review about "settings not working".
 */
data class SettingsState(
    val use24Hour: Boolean = false,
    val fahrenheit: Boolean = false,
    val mondayFirst: Boolean = true,
    val auraEnabled: Boolean = true,
    val auraStrength: AuraStrength = AuraStrength.Full,
    val enforceContrast: Boolean = true,
) {
    enum class AuraStrength(val label: String, val fraction: Float) {
        Subtle("Subtle", 0.35f),
        Balanced("Balanced", 0.7f),
        Full("Full", 1f),
    }
}
