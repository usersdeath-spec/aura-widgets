package com.prism.studio.render

import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import com.prism.studio.model.ColorSpec
import com.prism.studio.model.DynamicRole
import com.prism.studio.model.WallpaperSlot

/**
 * Turns a [ColorSpec] into an actual ARGB int.
 *
 * Dynamic and wallpaper-derived colours are resolved once per render pass and cached, because
 * reading the wallpaper palette is comparatively expensive and must never happen inside a draw loop.
 */
class ColorResolver(
    private val dynamic: Map<DynamicRole, Int>,
    private val wallpaper: Map<WallpaperSlot, Int>,
) {
    @ColorInt
    fun resolve(spec: ColorSpec): Int = when (spec) {
        is ColorSpec.Solid -> spec.argb.toInt()
        is ColorSpec.Dynamic -> dynamic[spec.role] ?: spec.fallback.toInt()
        is ColorSpec.FromWallpaper -> wallpaper[spec.slot] ?: spec.fallback.toInt()
    }

    /** Multiplies a resolved colour's alpha. Used for muted ink and layered glass. */
    @ColorInt
    fun resolve(spec: ColorSpec, alphaScale: Float): Int {
        val c = resolve(spec)
        val a = (Color.alpha(c) * alphaScale).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
    }

    companion object {
        /**
         * Builds a resolver for the current device state. Call once per update batch and share it
         * across every widget being redrawn.
         */
        fun forDevice(context: Context, wallpaperPalette: Map<WallpaperSlot, Int> = emptyMap()): ColorResolver {
            val dyn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mapOf(
                    DynamicRole.Primary to context.getColor(android.R.color.system_accent1_200),
                    DynamicRole.OnPrimary to context.getColor(android.R.color.system_accent1_900),
                    DynamicRole.Secondary to context.getColor(android.R.color.system_accent2_200),
                    DynamicRole.Tertiary to context.getColor(android.R.color.system_accent3_200),
                    DynamicRole.Surface to context.getColor(android.R.color.system_neutral1_900),
                    DynamicRole.OnSurface to context.getColor(android.R.color.system_neutral1_50),
                    DynamicRole.Outline to context.getColor(android.R.color.system_neutral2_400),
                )
            } else {
                emptyMap()
            }
            return ColorResolver(dyn, wallpaperPalette)
        }
    }
}
