package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * GLASS & MATERIAL — six families about what a surface is made of.
 *
 * These share a technique (translucency, edge light, layered depth) and differ in the material
 * they are pretending to be. Keeping them in one file makes it obvious when two of them have
 * drifted close enough to be redundant, which is the failure mode this whole category invites.
 */

/** Ice, not water: colder tint, sharper corners, and a highlight that falls off fast. */
internal val FrostedCrystal = family(
    id = "frosted-crystal",
    name = "Frosted Crystal",
    note = "Ice-white panes with cold light along the edge.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = glass(c(0xFFD8E6F5), fill = 0.28f, highlight = 0.44f, grain = 0.06f),
        stroke = edge(1f, 0f to 0xC2FFFFFF, 0.5f to 0x3DFFFFFF, 1f to 0x14FFFFFF),
        shadow = shadow(16f, 5f, 0.22f),
        cornerRadiusDp = 18f, paddingDp = 18f, letterSpacingEm = -0.012f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 550, spacingDp = 9f,
        ink = c(0xFF0D1B29), inkMuted = c(0x8A0D1B29), accent = c(0xFF2F6FA8),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("frost-01", "frost-02", "frost-03"),
    core = Core(battery = ContentLayout.Ring, clockDelta = StyleDelta(fontWeight = 400)),
    extras = listOf(
        v("fc-dial", "Frost Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("fc-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("fc-sun", "Sunrise", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("fc-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.Ring),
        v("fc-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Sea-worn glass: soft, warm-green, low contrast, everything rounded until it stops being a rectangle. */
internal val SeaGlass = family(
    id = "sea-glass",
    name = "Sea Glass",
    note = "Tumbled green glass. Soft edges, quiet contrast.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = glass(c(0xFF7FB0A3), fill = 0.30f, highlight = 0.20f, grain = 0.08f),
        stroke = hairline(0x59FFFFFF, 0.75f),
        shadow = shadow(20f, 8f, 0.18f),
        cornerRadiusDp = 34f, paddingDp = 22f, letterSpacingEm = -0.005f,
        fontFamily = FontFamilyToken.Rounded, fontWeight = 500, spacingDp = 10f,
        ink = c(0xFFF3F8F6), inkMuted = c(0x9EF3F8F6), accent = c(0xFFBFE3D6),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("tide-01", "tide-02", "tide-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring, todo = ContentLayout.Stack),
    extras = listOf(
        v("sg-pill", "Time Pill", WidgetType.DigitalClock, WidgetSize.Banner, ContentLayout.Hero,
            StyleDelta(cornerRadiusDp = 999f, paddingDp = 14f, alignment = Alignment.Center)),
        v("sg-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("sg-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("sg-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
    ),
)

/** Polished metal. The gradient is a reflection, not decoration — three stops, hard mid-break. */
internal val ChromeLiquid = family(
    id = "chrome-liquid",
    name = "Chrome Liquid",
    note = "Polished metal with a hard horizon in the reflection.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFF8992AC, 0.48f to 0xFF6E7A88, 0.52f to 0xFF39424E, 1f to 0xFF8792A2,
            angle = 95f,
        ),
        stroke = edge(1f, 0f to 0xCCFFFFFF, 1f to 0x33000000),
        shadow = shadow(18f, 7f, 0.4f),
        cornerRadiusDp = 20f, paddingDp = 18f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 700, spacingDp = 8f,
        ink = c(0xFFFFFFFF), inkMuted = c(0xA8FFFFFF), accent = c(0xFFE6EDF5),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("chrome-01", "chrome-02", "chrome-03"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("cl-hero", "Chrome Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("cl-cpu", "Processor", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("cl-ram", "Memory", WidgetType.Ram, WidgetSize.Small, ContentLayout.Chart),
        v("cl-network", "Network", WidgetType.Network, WidgetSize.Small, ContentLayout.Chart),
        v("cl-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/** Stone. Veining is a mesh surface at very low blur, which is closer to marble than any gradient. */
internal val Marble = family(
    id = "marble",
    name = "Marble",
    note = "Cool stone with veining that never repeats.",
    mood = Mood.Bright,
    base = WidgetStyle(
        surface = mesh(
            0xFFF2F0EC,
            Triple(0.18f, 0.24f, 0x33889AA8), Triple(0.72f, 0.36f, 0x2A6E7C88),
            Triple(0.44f, 0.82f, 0x22A0A8B0),
            radius = 0.62f, blur = 26f,
        ),
        stroke = hairline(0x1F000000, 0.75f),
        shadow = shadow(14f, 4f, 0.14f),
        cornerRadiusDp = 16f, paddingDp = 20f, letterSpacingEm = 0.005f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 400, spacingDp = 10f,
        ink = c(0xFF1B1F26), inkMuted = c(0x8C1B1F26), accent = c(0xFF4A5A6B),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("stone-01", "stone-02", "stone-03"),
    core = Core(clock = ContentLayout.Hero, calendar = ContentLayout.Grid, music = ContentLayout.Controls),
    extras = listOf(
        v("mb-dial", "Marble Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("mb-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("mb-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("mb-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("mb-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Folded paper. Every widget reads as one sheet creased once; the crease is the gradient break. */
internal val Origami = family(
    id = "origami",
    name = "Origami",
    note = "One sheet, one crease, one shadow.",
    mood = Mood.Playful,
    base = WidgetStyle(
        surface = grad(0f to 0xFFF6F1E7, 0.49f to 0xFFF6F1E7, 0.5f to 0xFFE7DFD0, 1f to 0xFFDDD3C1, angle = 118f),
        stroke = null,
        shadow = shadow(12f, 6f, 0.2f),
        cornerRadiusDp = 8f, paddingDp = 18f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 600, spacingDp = 8f,
        ink = c(0xFF2A2620), inkMuted = c(0x8A2A2620), accent = c(0xFFD2542E),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("fold-01", "fold-02", "fold-03"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("og-hero", "Folded Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("og-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("og-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("og-photo", "Photo Fold", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
    ),
)

/** Layered card stock: no gradients at all, only stacked shadows doing the work. */
internal val PaperCut = family(
    id = "paper-cut",
    name = "Paper Cut",
    note = "Flat card stock, real shadows, nothing shiny.",
    mood = Mood.Bright,
    base = WidgetStyle(
        surface = solid(0xFFFBFAF7),
        stroke = null,
        shadow = shadow(10f, 5f, 0.16f),
        cornerRadiusDp = 12f, paddingDp = 18f, letterSpacingEm = -0.015f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 600, spacingDp = 8f,
        ink = c(0xFF15171C), inkMuted = c(0x7A15171C), accent = c(0xFFE4572E),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("paper-01", "paper-02", "paper-03"),
    core = Core(clock = ContentLayout.HeroLabelled, todo = ContentLayout.Stack, notes = ContentLayout.Stack),
    extras = listOf(
        v("pc-small", "Paper Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("pc-day", "Day Card", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("pc-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("pc-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("pc-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
    ),
)
