package com.prism.studio.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Search and favourites.
 *
 * Both were gaps the competitors' screenshots made obvious: all three open their widget tab with a
 * search field, and all three put a heart on every tile. With 572 native widgets across 44 families
 * those are no longer nice-to-haves — a catalog this size is unusable without a way to jump to a
 * specific design, and unrewarding without a way to keep the ones you liked.
 *
 * One deliberate difference: the favourites toggle lives in the search row rather than on every
 * tile. A heart on each of 572 tiles is 572 tap targets competing with the tile's own tap, and the
 * competitors' galleries visibly suffer for it — their hearts overlap the artwork. Ours filters.
 */
@Composable
fun SearchRow(
    query: String,
    favouritesOnly: Boolean,
    resultCount: Int,
    onQuery: (String) -> Unit,
    onFavouritesOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    Row(
        modifier.fillMaxWidth().padding(horizontal = Space.base.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )

            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        // Names the vocabulary rather than saying "Search". A user who does not
                        // know the app has no idea what is searchable otherwise.
                        "Clock, glass, amoled, minimal…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(Motion.enterSpec(Motion.quick)),
                exit = fadeOut(Motion.exitSpec()),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .pressable(onClick = { haptics.tick(); onQuery("") }),
                )
            }
        }

        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (favouritesOnly) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                )
                .pressable(onClick = { haptics.select(); onFavouritesOnly(!favouritesOnly) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (favouritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favourites only",
                tint = if (favouritesOnly) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    // The count is the feedback that a filter did something. Without it, a query that matches
    // nothing looks identical to a screen that failed to load.
    AnimatedVisibility(
        visible = query.isNotEmpty() || favouritesOnly,
        enter = fadeIn(Motion.enterSpec(Motion.quick)),
        exit = fadeOut(Motion.exitSpec()),
    ) {
        Text(
            if (resultCount == 0) "Nothing matches" else "$resultCount designs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.base.dp, vertical = Space.hair.dp),
        )
    }
}
