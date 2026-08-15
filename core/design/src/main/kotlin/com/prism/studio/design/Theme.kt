package com.prism.studio.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import android.os.Build

/**
 * The app's own chrome, which is deliberately the quietest surface in the product.
 *
 * A widget store is a gallery. Anything the UI does with colour competes with the artwork the user
 * is trying to judge, so the chrome is near-monochrome — ink, mist, and a single refracted accent
 * that appears only on selection and on the purchase call to action. Dynamic colour is honoured
 * because users of a customisation app expect it, but it is applied to controls, never to the
 * canvas behind a widget preview.
 */

private val Ink = Color(0xFF0B0D12)
private val InkSoft = Color(0xFF141821)
private val InkLift = Color(0xFF1D222E)
private val Mist = Color(0xFFE8E9ED)
private val MistDim = Color(0xFF9AA0AC)
private val Refract = Color(0xFF7C5CFF)
private val RefractCool = Color(0xFF3FD8E0)

private val PrismDark = darkColorScheme(
    primary = Refract,
    onPrimary = Color.White,
    secondary = RefractCool,
    background = Ink,
    onBackground = Mist,
    surface = InkSoft,
    onSurface = Mist,
    surfaceVariant = InkLift,
    onSurfaceVariant = MistDim,
    outline = Color(0xFF2A303C),
)

private val PrismLight = lightColorScheme(
    primary = Refract,
    onPrimary = Color.White,
    secondary = Color(0xFF0F9BA6),
    background = Color(0xFFF7F7F9),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDEEF2),
    onSurfaceVariant = Color(0xFF5A6070),
    outline = Color(0xFFD7DAE2),
)

/**
 * Typefaces.
 *
 * The design calls for Bricolage Grotesque (display) and Inter Tight (body) — the same variable
 * files the widget renderer loads, so a family name in the catalog is set in the same metal as the
 * widget beneath it.
 *
 * Those files are NOT in the repository: they are licensed assets delivered separately (SIL OFL,
 * but not ours to redistribute here). Referencing `R.font.*` for files that do not exist is a hard
 * compile error, so until they land the app uses the platform's own faces.
 *
 * SWAPPING THEM IN IS A THREE-LINE CHANGE. Drop the .ttf files into
 * `core/design/src/main/res/font/` as `bricolage_grotesque_variable.ttf` and
 * `inter_tight_variable.ttf`, then replace the two bodies below with:
 *
 *     private val Display = FontFamily(Font(R.font.bricolage_grotesque_variable, FontWeight.SemiBold))
 *     private val Body = FontFamily(Font(R.font.inter_tight_variable, FontWeight.Normal))
 *
 * Nothing else changes: every type style below already references these two values, and the widget
 * renderer resolves its own faces separately through TypefaceProvider, which already falls back to
 * the system face when an asset is missing.
 *
 * The fallbacks are deliberate rather than arbitrary. SansSerif is Roboto on most devices, which is
 * a neutral grotesk — the right stand-in for Inter Tight. Display uses the same family at a heavier
 * weight rather than reaching for Serif or Monospace, because a mismatched display face is more
 * visually wrong than a slightly plain one.
 */
private val Display = FontFamily.SansSerif
private val Body = FontFamily.SansSerif

// Both are SansSerif on purpose while the bundled fonts are absent: the app chrome should be the
// quietest thing on screen, and two system faces fighting each other in the shelf headers is worse
// than one. The widget renderer resolves its own faces separately, and those DO differ per family.

private val PrismTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.015).em),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.01.em),
)

/** Spacing scale. Four values, used everywhere; a fifth would mean the layout is confused. */
object Space {
    val hair = 4
    val tight = 8
    val base = 16
    val loose = 24
    val section = 40
}

val LocalCheckerboard = staticCompositionLocalOf { true }

@Composable
fun PrismTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> PrismDark
        else -> PrismLight
    }
    CompositionLocalProvider(LocalCheckerboard provides true) {
        MaterialTheme(colorScheme = scheme, typography = PrismTypography, content = content)
    }
}
