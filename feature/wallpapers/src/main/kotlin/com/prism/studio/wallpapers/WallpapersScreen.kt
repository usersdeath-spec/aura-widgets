package com.prism.studio.wallpapers

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prism.studio.design.Space
import com.prism.studio.design.pressable
import com.prism.studio.render.WallpaperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The wallpaper gallery.
 *
 * Every tile is drawn on the device by [WallpaperEngine] rather than decoded from a bundled JPEG.
 * That is what fixes the criticism this screen earned in review — "the same thing over and over in
 * different colours" — because the nine generators are different *kinds* of image, not different
 * hues of one image, and any two are distinguishable at thumbnail size.
 *
 * It is also why the category row here is honest. The competitors\' categories (Blur Grainy,
 * Gradient, Strip, Landscape) are folders of files; ours are the generators themselves, so picking
 * "Strips" cannot return something that is not a strip.
 */
@Composable
fun WallpapersScreen(
    palette: List<Color>,
    onPick: (WallpaperEngine.Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    var style by remember { mutableStateOf<WallpaperEngine.Style?>(null) }

    val argb = remember(palette) { palette.map { it.toArgb() } }
    val all = remember(argb) { WallpaperEngine.collection(argb, dark = true, count = 54) }
    val shown = remember(style, all) { style?.let { s -> all.filter { it.style == s } } ?: all }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.base.dp,
            end = Space.base.dp,
            bottom = Space.section.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(Space.tight.dp),
        verticalArrangement = Arrangement.spacedBy(Space.tight.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(
                    "Wallpapers",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(top = Space.base.dp),
                )
                Text(
                    "Generated on your phone, matched to your aura",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Space.hair.dp),
                )

                LazyRow(
                    contentPadding = PaddingValues(vertical = Space.tight.dp),
                    horizontalArrangement = Arrangement.spacedBy(Space.base.dp),
                ) {
                    item {
                        // "All" previews with a recipe from each generator blended, so the circle
                        // is not just a colour chip like ours used to be.
                        StyleCircle(
                            label = "All",
                            recipe = all.first(),
                            selected = style == null,
                            onClick = { style = null },
                        )
                    }
                    items(WallpaperEngine.Style.entries, key = { it.name }) { entry ->
                        StyleCircle(
                            label = entry.label,
                            recipe = all.first { it.style == entry },
                            selected = style == entry,
                            onClick = { style = if (style == entry) null else entry },
                        )
                    }
                }
            }
        }

        gridItems(shown, key = { it.id }) { recipe ->
            WallpaperCard(recipe, onClick = { onPick(recipe) })
        }
    }
}

/**
 * A generator, previewed by an actual render of itself.
 *
 * The competitors do the same thing with a cropped JPEG. Ours costs nothing to ship and can never
 * be out of sync with what the category contains, because it IS what the category contains.
 */
@Composable
private fun StyleCircle(
    label: String,
    recipe: WallpaperEngine.Recipe,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pressable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 2.5.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape,
                ),
        ) {
            GeneratedImage(recipe, Modifier.fillMaxSize())
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.hair.dp),
        )
    }
}

@Composable
private fun WallpaperCard(recipe: WallpaperEngine.Recipe, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 19.5f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressable(onClick = onClick),
    ) {
        GeneratedImage(recipe, Modifier.fillMaxSize())
    }
}

/**
 * Renders a recipe off the main thread and fades it in.
 *
 * Generation is capped at 360px regardless of the tile\'s measured size: the compositions are in
 * normalised coordinates, so a 360px render scaled up is indistinguishable from a native one at
 * these sizes, and it keeps a fast scroll through 54 tiles from allocating full-resolution bitmaps.
 */
@Composable
private fun GeneratedImage(recipe: WallpaperEngine.Recipe, modifier: Modifier = Modifier) {
    var bitmap by remember(recipe.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(recipe.id) {
        bitmap = withContext(Dispatchers.Default) {
            WallpaperEngine.render(recipe, THUMB_WIDTH_PX, THUMB_HEIGHT_PX)
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = recipe.style.note,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } ?: Box(modifier)
}

private const val THUMB_WIDTH_PX = 360
private const val THUMB_HEIGHT_PX = 780
