package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * PHENOMENA — eight families built on an optical event rather than a material.
 *
 * Each is a thing light *does*: diffraction, attenuation with depth, occlusion, heat displacement,
 * diffusion through frosting. The rule that keeps them apart from the Colour & Light collection is
 * that a phenomenon has a *mechanism* — you can say what is happening and why the widget looks that
 * way. "A nice purple gradient" is not a phenomenon and would not be admitted here.
 */

/** Diffraction. A rainbow that travels across the surface as a sweep, never a fixed gradient. */
internal val Holographic = family(
    id = "holographic",
    name = "Holographic",
    note = "Diffraction. The spectrum travels across the surface.",
    mood = Mood.Playful,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFF7CE0FF, 0.18f to 0xFFB58CFF, 0.38f to 0xFFFF8FC8,
            0.58f to 0xFFFFD48F, 0.78f to 0xFF8FFFC4, 1f to 0xFF7CE0FF,
            kind = GradientKind.Sweep,
        ),
        stroke = edge(1f, 0f to 0xCCFFFFFF, 1f to 0x33FFFFFF),
        shadow = shadow(16f, 6f, 0.22f),
        cornerRadiusDp = 22f, paddingDp = 20f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.GroteskDisplay, fontWeight = 700, spacingDp = 9f,
        ink = c(0xFF161020), inkMuted = c(0x99161020), accent = c(0xFF2C1B45),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("holo-01", "holo-02", "refract-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("ho-small", "Holo Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("ho-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("ho-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("ho-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("ho-photo", "Holo Frame", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
    ),
)

/** Neon diffused through frosting: all glow, no hard edge. The inverse of Cyberpunk Neon. */
internal val NeonFrost = family(
    id = "neon-frost",
    name = "Neon Frost",
    note = "Neon behind frosted glass. Glow with no hard edge.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = Surface.LiquidGlass(
            tint = c(0xFF2A1B4A),
            depth = 0.7f, bodyAlpha = 0.34f, specularAlpha = 0.12f, specularAngleDeg = 315f,
            causticAlpha = 0.26f, innerShadowAlpha = 0.16f, grainAlpha = 0.07f, refraction = 0.3f,
        ),
        stroke = hairline(0x59C9A0FF, 0.75f),
        glow = glow(22f, 0xFF9A6BFF, 0.4f),
        cornerRadiusDp = 26f, paddingDp = 20f, letterSpacingEm = 0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 10f,
        ink = c(0xFFF4EEFF), inkMuted = c(0x99F4EEFF), accent = c(0xFFC9A0FF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("neonfrost-01", "neonfrost-02", "neon-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("nf-small", "Frost Neon", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("nf-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("nf-crypto", "Crypto", WidgetType.Crypto, WidgetSize.Wide, ContentLayout.Chart),
        v("nf-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("nf-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
    ),
)

/** Occlusion. Everything is dark except a bright ring at the rim — light only where it escapes. */
internal val Eclipse = family(
    id = "eclipse",
    name = "Eclipse",
    note = "A dark disc with light escaping only at the rim.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFF07080B, 0.72f to 0xFF0B0D12, 0.94f to 0xFF3A2A16, 1f to 0xFFBE7619,
            kind = GradientKind.Radial,
        ),
        stroke = edge(1.25f, 0f to 0xFFFFC46B, 0.5f to 0x66FF9A3C, 1f to 0x1AFF9A3C),
        glow = glow(20f, 0xFFFFB04C, 0.4f),
        cornerRadiusDp = 999f, paddingDp = 24f, letterSpacingEm = 0.02f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 400, spacingDp = 10f,
        alignment = Alignment.Center,
        ink = c(0xFFF6EEE2), inkMuted = c(0x8AF6EEE2), accent = c(0xFFFFB04C),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("eclipse-01", "eclipse-02", "orbit-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("ec-dial", "Corona Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("ec-sun", "Sun Path", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("ec-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("ec-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("ec-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/** A hot core bleeding outward. Chroma falls off with radius; the centre is nearly white. */
internal val Solar = family(
    id = "solar",
    name = "Solar",
    note = "A hot core bleeding outward into red.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFFCE7A05, 0.22f to 0xFFC26A00, 0.6f to 0xFFE8562C, 1f to 0xFF7A1A12,
            kind = GradientKind.Radial,
        ),
        stroke = hairline(0x59FFE3A8),
        glow = glow(24f, 0xFFFF8A3C, 0.45f),
        cornerRadiusDp = 20f, paddingDp = 20f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 700, spacingDp = 9f,
        ink = c(0xFFFFF6EA), inkMuted = c(0xA8FFF6EA), accent = c(0xFFFFE3A8),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("solar-01", "solar-02", "dusk-02"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("so-dated", "Solar Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("so-sun", "Sunrise & Sunset", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("so-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("so-health", "Activity", WidgetType.Health, WidgetSize.Wide, ContentLayout.Chart),
        v("so-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Attenuation with depth: contrast and chroma both fall as you go down the frame. */
internal val Abyssal = family(
    id = "abyssal",
    name = "Abyssal",
    note = "Light failing with depth. Contrast falls as you descend.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = grad(0f to 0xFF12455E, 0.45f to 0xFF0A2739, 1f to 0xFF03101A, angle = 90f),
        stroke = hairline(0x338FD4E8),
        shadow = shadow(20f, 8f, 0.4f),
        cornerRadiusDp = 22f, paddingDp = 20f, letterSpacingEm = 0f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 450, spacingDp = 10f,
        ink = c(0xFFE4F2F8), inkMuted = c(0x708FD4E8), accent = c(0xFF4FC3D9),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("abyss-01", "abyss-02", "tide-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("ab2-small", "Deep Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("ab2-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("ab2-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("ab2-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("ab2-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
    ),
)

/** One hard line divides warm from cool. Landscape logic applied to a rectangle. */
internal val Horizon = family(
    id = "horizon",
    name = "Horizon",
    note = "One hard line. Warm above it, cool below.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFFD47026, 0.49f to 0xFFD56E4A, 0.5f to 0xFF1E3A56, 1f to 0xFF0C1C2E,
            angle = 90f,
        ),
        stroke = hairline(0x40FFFFFF),
        shadow = shadow(14f, 5f, 0.28f),
        cornerRadiusDp = 12f, paddingDp = 18f, letterSpacingEm = -0.015f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 550, spacingDp = 9f,
        ink = c(0xFFFDF3E8), inkMuted = c(0x99FDF3E8), accent = c(0xFFE9A56B),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("horizon-01", "horizon-02", "fjord-02"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("hz-hero", "Horizon Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("hz-sun", "Sunrise & Sunset", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("hz-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("hz-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("hz-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
    ),
)

/** Heat displacement: a desaturated ghost of the content, offset and faint. */
internal val Mirage = family(
    id = "mirage",
    name = "Mirage",
    note = "Heat shimmer. Everything doubled and slightly wrong.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = grad(0f to 0xFFE8DCC4, 0.55f to 0xFFD9C29E, 1f to 0xFFC2A886, angle = 100f),
        stroke = hairline(0x40FFFFFF),
        shadow = shadow(16f, 6f, 0.18f),
        cornerRadiusDp = 18f, paddingDp = 20f, letterSpacingEm = 0.02f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 400, spacingDp = 10f,
        ink = c(0xFF3E3222), inkMuted = c(0x6E3E3222), accent = c(0xFF9C7A4E),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("mirage-01", "mirage-02", "clay-02"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("mi-dated", "Mirage Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("mi-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("mi-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("mi-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("mi-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Seamless backdrop, one soft key light, neutral grey. A photographic studio, not a mood. */
internal val Studio = family(
    id = "studio",
    name = "Studio",
    note = "Seamless backdrop, one soft key light, neutral grey.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = grad(0f to 0xFFEFEFF1, 0.6f to 0xFFE2E2E6, 1f to 0xFFD2D2D8, angle = 90f),
        stroke = null,
        shadow = shadow(18f, 7f, 0.12f),
        cornerRadiusDp = 18f, paddingDp = 22f, letterSpacingEm = -0.015f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 10f,
        ink = c(0xFF1C1D21), inkMuted = c(0x7A1C1D21), accent = c(0xFF5A5D66),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("studio-01", "studio-02", "paper-01"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("su-small", "Studio Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("su-dial", "Studio Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("su-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("su-photo", "Photo", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
        v("su-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
    ),
)
