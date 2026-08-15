package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * DEPTH — three families about distance rather than surface.
 *
 * All three are near-black, which on OLED means they are also among the cheapest to display. The
 * difference between them is where the eye is asked to focus: Cosmic Drift keeps everything soft
 * and far away, Deep Space puts one hard-edged object against emptiness, Monolith removes the
 * object and leaves only mass.
 */

/** Nebula: nothing in focus, no hard edges anywhere, colour arriving from off-frame. */
internal val CosmicDrift = family(
    id = "cosmic-drift",
    name = "Cosmic Drift",
    note = "Nothing in focus. Colour arriving from off-frame.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = mesh(
            0xFF05060D,
            Triple(-0.1f, 0.2f, 0xFF6C4BD6), Triple(1.1f, 0.78f, 0xFFD64B96),
            Triple(0.5f, 1.2f, 0xFF2E6BD6),
            radius = 0.85f, blur = 72f,
        ),
        stroke = hairline(0x1FFFFFFF),
        cornerRadiusDp = 28f, paddingDp = 20f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 400, spacingDp = 10f,
        ink = c(0xFFF3F0FF), inkMuted = c(0x8AF3F0FF), accent = c(0xFFB39BFF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("nebula-01", "nebula-02", "nebula-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("cd-dated", "Drift Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("cd-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("cd-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("cd-photo", "Photo", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
    ),
)

/** One lit object against void. Glow is used exactly once per widget, on the accent only. */
internal val DeepSpace = family(
    id = "deep-space",
    name = "Deep Space",
    note = "One lit object, everything else void.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = solid(0xFF000000),
        stroke = hairline(0x2E7FA8FF),
        glow = glow(14f, 0xFF3E7BFF, 0.35f),
        cornerRadiusDp = 20f, paddingDp = 18f, letterSpacingEm = 0.02f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 400, spacingDp = 9f,
        ink = c(0xFFDCE7FF), inkMuted = c(0x70DCE7FF), accent = c(0xFF3E7BFF),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("orbit-01", "orbit-02", "void-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("ds-dial", "Orbit Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("ds-sun", "Sun Path", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("ds-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("ds-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("ds-network", "Network", WidgetType.Network, WidgetSize.Small, ContentLayout.Chart),
    ),
)

/** Mass without light. Near-black slabs, one degree of separation from the wallpaper, no accent colour. */
internal val Monolith = family(
    id = "monolith",
    name = "Monolith",
    note = "Slabs a shade off black. No accent, no glow.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = grad(0f to 0xFF14161A, 1f to 0xFF090A0C, angle = 90f),
        stroke = hairline(0x1AFFFFFF),
        shadow = shadow(20f, 8f, 0.5f),
        cornerRadiusDp = 10f, paddingDp = 20f, letterSpacingEm = -0.03f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 600, spacingDp = 8f,
        ink = c(0xFFCED3DA), inkMuted = c(0x6ECED3DA), accent = c(0xFFCED3DA),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("slab-01", "slab-02", "void-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge, todo = ContentLayout.Stack),
    extras = listOf(
        v("mo-stack", "Stacked Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("mo-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("mo-ram", "RAM", WidgetType.Ram, WidgetSize.Small, ContentLayout.Chart),
        v("mo-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("mo-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
    ),
)
