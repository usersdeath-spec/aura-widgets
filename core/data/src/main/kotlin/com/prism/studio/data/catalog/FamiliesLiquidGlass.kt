package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * LIQUID GLASS — the flagship collection.
 *
 * Three families rather than one, because a single family caps at fifteen variants and this idea
 * supports more than that honestly. They share a material and a light source and differ in what
 * the glass is *made of*: Clear is a thin, bright pane; Smoked is a thick, dark plate; Prism is
 * cut glass that splits the light it passes.
 *
 * Originality note: the material here is derived from physical glass behaviour — inner shadow for
 * thickness, a caustic pool at the lower edge, a hairline that tracks a fixed light angle — not
 * from any other product's design language. The light sits at 315° across all three families, so a
 * home screen mixing them is lit from one direction, which is what makes a mixed screen read as
 * one set rather than three.
 *
 * Rendering, including the backdrop-blur tiers and their fallbacks, lives in `GlassPainter`.
 */

/** Thin, bright, barely there. Highest transparency in the app; leans hardest on the edge light. */
internal val LiquidGlassClear = family(
    id = "liquid-glass-clear",
    name = "Liquid Glass",
    note = "A thin bright pane. Light caught along one edge.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = Surface.LiquidGlass(
            tint = wall(WallpaperSlot.LightVibrant, 0xFFB9D3F2),
            depth = 0.28f, bodyAlpha = 0.13f, specularAlpha = 0.36f, specularAngleDeg = 315f,
            causticAlpha = 0.10f, innerShadowAlpha = 0.14f, grainAlpha = 0.028f, refraction = 0.22f,
        ),
        stroke = null,   // the edge light in GlassPainter replaces a conventional stroke
        shadow = shadow(16f, 5f, 0.22f),
        cornerRadiusDp = 30f, paddingDp = 20f, letterSpacingEm = -0.015f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 10f,
        ink = c(0xFFFFFFFF), inkMuted = c(0x99FFFFFF), accent = c(0xFFCDE4FF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("refract-01", "refract-02", "aurora-01"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring,
        batteryDelta = StyleDelta(motion = WidgetMotion.ArcSweep)),
    extras = listOf(
        v("lgc-small", "Clear Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("lgc-pill", "Time Pill", WidgetType.DigitalClock, WidgetSize.Banner, ContentLayout.Hero,
            StyleDelta(cornerRadiusDp = 999f, paddingDp = 14f, alignment = Alignment.Center)),
        v("lgc-dial", "Clear Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("lgc-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("lgc-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("lgc-finance", "Markets", WidgetType.Finance, WidgetSize.Wide, ContentLayout.Chart),
        v("lgc-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("lgc-photo", "Glass Frame", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
    ),
)

/** Thick, dark, heavy. Maximum depth: deepest blur, strongest inner shadow, a real caustic pool. */
internal val LiquidGlassSmoked = family(
    id = "liquid-glass-smoked",
    name = "Liquid Glass Smoked",
    note = "Thick dark plate. Deep blur, heavy inner shadow.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = Surface.LiquidGlass(
            tint = c(0xFF1A1D24),
            depth = 0.92f, bodyAlpha = 0.30f, specularAlpha = 0.22f, specularAngleDeg = 315f,
            causticAlpha = 0.16f, innerShadowAlpha = 0.30f, grainAlpha = 0.04f, refraction = 0.34f,
        ),
        stroke = null,
        shadow = shadow(26f, 10f, 0.42f),
        cornerRadiusDp = 26f, paddingDp = 20f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 550, spacingDp = 10f,
        ink = c(0xFFF2F5FA), inkMuted = c(0x8AF2F5FA), accent = c(0xFF9FC0E8),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("smoke-01", "slab-01", "refract-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("lgs-small", "Smoked Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("lgs-dial", "Smoked Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("lgs-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("lgs-cpu", "Processor", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Ring),
        v("lgs-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("lgs-finance", "Portfolio", WidgetType.Finance, WidgetSize.Wide, ContentLayout.Chart),
        v("lgs-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("lgs-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
    ),
)

/** Cut glass. Maximum refraction, and the accent shifts hue across the plate the way a prism does. */
internal val LiquidGlassPrism = family(
    id = "liquid-glass-prism",
    name = "Liquid Glass Prism",
    note = "Cut glass. Splits the light that passes through it.",
    mood = Mood.Bright,
    base = WidgetStyle(
        surface = Surface.LiquidGlass(
            tint = wall(WallpaperSlot.Vibrant, 0xFF8E7CFF),
            depth = 0.62f, bodyAlpha = 0.17f, specularAlpha = 0.40f, specularAngleDeg = 315f,
            causticAlpha = 0.22f, innerShadowAlpha = 0.20f, grainAlpha = 0.025f, refraction = 0.46f,
        ),
        stroke = null,
        shadow = shadow(20f, 7f, 0.3f),
        cornerRadiusDp = 22f, paddingDp = 20f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.GroteskDisplay, fontWeight = 600, spacingDp = 10f,
        ink = c(0xFFFFFFFF), inkMuted = c(0xA3FFFFFF), accent = c(0xFF7C5CFF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("refract-01", "aurora-02", "nebula-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("lgp-dated", "Prism Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("lgp-dial", "Prism Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("lgp-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("lgp-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("lgp-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("lgp-crypto", "Crypto", WidgetType.Crypto, WidgetSize.Wide, ContentLayout.Chart),
        v("lgp-photo", "Prism Frame", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
        v("lgp-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)
