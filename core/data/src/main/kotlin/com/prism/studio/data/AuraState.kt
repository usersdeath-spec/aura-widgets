package com.prism.studio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prism.studio.model.ColorSpec
import com.prism.studio.model.StyleDelta
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.auraStore by preferencesDataStore("aura")

/**
 * THE AURA.
 *
 * This is the feature the product is named after, and the one thing none of the four top-selling
 * competitors do. All of them sell the same proposition: here are 300–500 widgets, pick some. The
 * user is then responsible for making them look like they belong together — and on someone else's
 * wallpaper, they usually don't.
 *
 * An aura is the colour a screen gives off. Choose a wallpaper and **every widget adopts it at
 * once**: surfaces, ink, and accents all re-derive from that wallpaper's palette through the
 * on-device harmony engine, with a hard contrast floor so nothing becomes unreadable.
 *
 * Why this is defensible rather than a gimmick:
 *
 *  * It is the one thing a 400-widget list cannot do. Their widgets are baked layouts with fixed
 *    colours; ours are style data, so retinting all 708 is a map over a data class.
 *  * It runs entirely on device — no network, no account — which is also the privacy claim.
 *  * It is a ten-second demo. Swipe a wallpaper, watch the whole screen change together. That is
 *    the moment someone shows a friend, which is the only marketing that works at ₹99.
 *
 * The aura is stored as a [StyleDelta] because that is already how user edits are represented: it
 * layers under their own customisations rather than overwriting them, so a widget the user hand-
 * tuned keeps its edits and only inherits what they did not set.
 */
@Singleton
class AuraState @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val wallpaperKey = stringPreferencesKey("aura_wallpaper")
    private val harmonyKey = stringPreferencesKey("aura_harmony")
    private val surfaceKey = longPreferencesKey("aura_surface")
    private val inkKey = longPreferencesKey("aura_ink")
    private val mutedKey = longPreferencesKey("aura_ink_muted")
    private val accentKey = longPreferencesKey("aura_accent")

    /**
     * The current aura, or null when the user has not chosen one.
     *
     * Null is a real state, not an empty default: a user who wants each family's authored palette
     * should get exactly that, and silently tinting everything on first launch would replace the
     * design work they paid for.
     */
    val current: Flow<Aura?> = context.auraStore.data.map { prefs ->
        val surface = prefs[surfaceKey] ?: return@map null
        Aura(
            wallpaperId = prefs[wallpaperKey],
            harmony = prefs[harmonyKey] ?: "Analogous",
            surface = surface,
            ink = prefs[inkKey] ?: 0xFFFFFFFF,
            inkMuted = prefs[mutedKey] ?: 0x99FFFFFF,
            accent = prefs[accentKey] ?: surface,
        )
    }

    suspend fun apply(aura: Aura) {
        context.auraStore.edit { prefs ->
            aura.wallpaperId?.let { prefs[wallpaperKey] = it }
            prefs[harmonyKey] = aura.harmony
            prefs[surfaceKey] = aura.surface
            prefs[inkKey] = aura.ink
            prefs[mutedKey] = aura.inkMuted
            prefs[accentKey] = aura.accent
        }
    }

    /** Back to every family's own authored palette. */
    suspend fun clear() {
        context.auraStore.edit { it.clear() }
    }

    data class Aura(
        val wallpaperId: String?,
        val harmony: String,
        val surface: Long,
        val ink: Long,
        val inkMuted: Long,
        val accent: Long,
    ) {
        /**
         * The delta every widget inherits.
         *
         * Colour only. A user who spent time on corner radius and type weight expects an aura to
         * change the colour of their screen, not to reset their widget — and an aura that quietly
         * flattened those choices would be the fastest possible way to lose their trust in it.
         */
        fun asDelta(): StyleDelta = StyleDelta(
            ink = ColorSpec.Solid(ink),
            inkMuted = ColorSpec.Solid(inkMuted),
            accent = ColorSpec.Solid(accent),
        )
    }
}
