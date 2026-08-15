package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * PRECIOUS MATERIALS — nine families, each pinned to how one real material behaves under light.
 *
 * This category is where a catalog goes wrong fastest. "Emerald, Ruby, Sapphire" are one family
 * with three tints, not three families; "Arctic Glass" and "Frosted Crystal" are one idea named
 * twice. Everything here earns its place by a *behaviour* no other family has: brushed metal is
 * anisotropic, satin has no specular at all, nacre shifts hue with angle, obsidian is near-black
 * with a razor highlight. If two families would differ only in hue, they are one family with a
 * tint delta — see [Gemstone].
 */

/** Brushed metal. Anisotropic: the highlight is a stretched band along the grain, never a point. */
internal val Titanium = family(
    id = "titanium",
    name = "Titanium",
    note = "Brushed metal. The highlight stretches along the grain.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = grad(0f to 0xFF878D98, 0.46f to 0xFF5C626B, 0.54f to 0xFF6E747D, 1f to 0xFF484D55, angle = 0f),
        stroke = edge(1f, 0f to 0x99FFFFFF, 1f to 0x33000000),
        shadow = shadow(14f, 5f, 0.34f),
        cornerRadiusDp = 16f, paddingDp = 18f, letterSpacingEm = 0.01f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 600, spacingDp = 8f,
        ink = c(0xFFF4F6F9), inkMuted = c(0x99F4F6F9), accent = c(0xFFCBD4DE),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("brushed-01", "brushed-02", "chrome-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("ti-small", "Titanium Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("ti-dial", "Titanium Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("ti-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("ti-cpu", "Processor", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("ti-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
    ),
)

/** Liquid metal. A mirror that curves: radial reflection with a bright pool at the centre. */
internal val Mercury = family(
    id = "mercury",
    name = "Mercury",
    note = "A mirror that curves. Reflection pools at the centre.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFFEDF2F7, 0.32f to 0xFF9AA6B4, 0.68f to 0xFF5B647E, 1f to 0xFF737E8B,
            angle = 90f, kind = GradientKind.Radial,
        ),
        stroke = edge(1.25f, 0f to 0xE6FFFFFF, 1f to 0x40202830),
        shadow = shadow(20f, 8f, 0.4f),
        cornerRadiusDp = 999f, paddingDp = 22f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 600, spacingDp = 8f,
        alignment = Alignment.Center,
        ink = c(0xFF11161C), inkMuted = c(0x8A11161C), accent = c(0xFF2E3944),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("mercury-01", "mercury-02", "chrome-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("mc-dial", "Droplet Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("mc-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("mc-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.Ring),
        v("mc-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("mc-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)

/** Faceted crystal. Hard-edged planes, not soft frost — the opposite of Frosted Crystal. */
internal val Quartz = family(
    id = "quartz",
    name = "Quartz",
    note = "Cut facets with hard edges. Nothing is soft.",
    mood = Mood.Bright,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFFF6F8FB, 0.34f to 0xFFDCE4EE, 0.35f to 0xFFC7D2E0,
            0.68f to 0xFFE8EEF6, 0.69f to 0xFFD2DCE8, 1f to 0xFFF2F5F9,
            angle = 118f,
        ),
        stroke = hairline(0x66FFFFFF, 1f),
        shadow = shadow(12f, 4f, 0.16f),
        cornerRadiusDp = 4f, paddingDp = 18f, letterSpacingEm = 0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 9f,
        ink = c(0xFF1A2430), inkMuted = c(0x8A1A2430), accent = c(0xFF4C7FB8),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("quartz-01", "quartz-02", "frost-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("qz-small", "Facet Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("qz-dial", "Facet Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("qz-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("qz-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("qz-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)

/** Matte pile. No specular anywhere; the only lift is a faint sheen at the grazing edge. */
internal val Velvet = family(
    id = "velvet",
    name = "Velvet",
    note = "Matte pile. No shine — only a sheen at the edge.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = grad(0f to 0xFF3A1F4A, 0.7f to 0xFF241030, 1f to 0xFF2E1740, angle = 105f),
        stroke = hairline(0x40C9A9E0, 0.75f),
        shadow = shadow(22f, 8f, 0.45f),
        cornerRadiusDp = 20f, paddingDp = 22f, letterSpacingEm = 0.015f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 300, spacingDp = 12f,
        ink = c(0xFFF2E9F7), inkMuted = c(0x8AF2E9F7), accent = c(0xFFC9A9E0),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("velvet-01", "velvet-02", "smoke-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("vl-dated", "Velvet Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("vl-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("vl-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("vl-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("vl-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
    ),
)

/** Volcanic glass: near-black body, one razor-thin specular, conchoidal break at the edge. */
internal val Obsidian = family(
    id = "obsidian",
    name = "Obsidian",
    note = "Near-black glass with one razor highlight.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = grad(0f to 0xFF15161B, 0.5f to 0xFF08090C, 1f to 0xFF101218, angle = 128f),
        stroke = edge(1f, 0f to 0xCCFFFFFF, 0.12f to 0x1AFFFFFF, 1f to 0x0DFFFFFF),
        shadow = shadow(24f, 9f, 0.55f),
        cornerRadiusDp = 18f, paddingDp = 20f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 9f,
        ink = c(0xFFEDEFF4), inkMuted = c(0x7AEDEFF4), accent = c(0xFF8FA3BF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("obsidian-01", "obsidian-02", "void-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("ob-small", "Obsidian Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("ob-dial", "Obsidian Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("ob-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("ob-finance", "Markets", WidgetType.Finance, WidgetSize.Wide, ContentLayout.Chart),
        v("ob-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/** Woven twill. The only family with a directional weave, and the most technical of the darks. */
internal val Carbon = family(
    id = "carbon",
    name = "Carbon",
    note = "Woven twill. Structure you can read at arm's length.",
    mood = Mood.Technical,
    base = WidgetStyle(
        surface = mesh(
            0xFF14161A,
            Triple(0.25f, 0.25f, 0x2E3A4048), Triple(0.75f, 0.75f, 0x2E3A4048),
            radius = 0.5f, blur = 18f,
        ),
        stroke = hairline(0x33C8D2DE, 0.75f),
        shadow = shadow(14f, 5f, 0.4f),
        cornerRadiusDp = 10f, paddingDp = 16f, letterSpacingEm = 0.03f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 500, spacingDp = 7f,
        ink = c(0xFFDFE5EC), inkMuted = c(0x7ADFE5EC), accent = c(0xFF57D1A0),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("carbon-01", "carbon-02", "grid-void-01"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("cb-hero", "Carbon Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("cb-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("cb-ram", "RAM", WidgetType.Ram, WidgetSize.Small, ContentLayout.Chart),
        v("cb-network", "Network", WidgetType.Network, WidgetSize.Small, ContentLayout.Chart),
        v("cb-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/** Nacre. Hue shifts across the surface with viewing angle — a sweep gradient, nothing else. */
internal val Pearl = family(
    id = "pearl",
    name = "Pearl",
    note = "Nacre. The hue moves as your eye moves.",
    mood = Mood.Bright,
    base = WidgetStyle(
        surface = grad(
            0f to 0xFFFFF4F0, 0.28f to 0xFFEDE6FA, 0.55f to 0xFFDFF3F0,
            0.8f to 0xFFFDF0E2, 1f to 0xFFFFF4F0,
            kind = GradientKind.Sweep,
        ),
        stroke = hairline(0x59FFFFFF, 1f),
        shadow = shadow(14f, 5f, 0.14f),
        cornerRadiusDp = 26f, paddingDp = 20f, letterSpacingEm = -0.005f,
        fontFamily = FontFamilyToken.Rounded, fontWeight = 500, spacingDp = 10f,
        ink = c(0xFF3A3140), inkMuted = c(0x8A3A3140), accent = c(0xFF9C7FC4),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("pearl-01", "pearl-02", "bloom-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("pl-dated", "Pearl Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("pl-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("pl-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("pl-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("pl-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
    ),
)

/**
 * GEMSTONE — one family, three tints.
 *
 * Emerald, Ruby, and Sapphire were requested as separate families. They are not: they share a
 * surface, a type treatment, a layout language, and a light model, and differ only in hue. Shipping
 * them as three would be exactly the recolouring the catalog rules exist to prevent, and would make
 * the family list longer while making the app feel thinner. They are variants here, and the editor's
 * colour controls give any of the three to any widget in the family.
 */
internal val Gemstone = family(
    id = "gemstone",
    name = "Gemstone",
    note = "Deep cut stone. Three tints, one cut.",
    mood = Mood.Bold,
    base = WidgetStyle(
        surface = grad(0f to 0xFF0E3B2E, 0.45f to 0xFF10604A, 1f to 0xFF072620, angle = 122f),
        stroke = edge(1f, 0f to 0xB3FFFFFF, 0.4f to 0x33FFFFFF, 1f to 0x14FFFFFF),
        shadow = shadow(18f, 7f, 0.42f),
        cornerRadiusDp = 8f, paddingDp = 20f, letterSpacingEm = 0.01f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 400, spacingDp = 10f,
        ink = c(0xFFF2FBF7), inkMuted = c(0x99F2FBF7), accent = c(0xFF7FE3BE),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("gem-01", "gem-02", "gem-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("gm-ruby", "Ruby Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled,
            StyleDelta(
                surface = grad(0f to 0xFF4A0E1E, 0.45f to 0xFF7A1730, 1f to 0xFF2C0713, angle = 122f),
                accent = ColorSpec.Solid(0xFFE38FA6),
            )),
        v("gm-sapphire", "Sapphire Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split,
            StyleDelta(
                surface = grad(0f to 0xFF0E2450, 0.45f to 0xFF163C7A, 1f to 0xFF07152E, angle = 122f),
                accent = ColorSpec.Solid(0xFF8FB4E3),
            )),
        v("gm-dial", "Gem Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("gm-finance", "Portfolio", WidgetType.Finance, WidgetSize.Wide, ContentLayout.Chart),
        v("gm-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Satin: translucent with the specular removed entirely. Even diffuse light, no edge highlight. */
internal val Satin = family(
    id = "satin",
    name = "Satin",
    note = "Translucent, but with the shine taken out.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = Surface.LiquidGlass(
            tint = wall(WallpaperSlot.Muted, 0xFFB8BCC6),
            depth = 0.45f, bodyAlpha = 0.34f, specularAlpha = 0.04f, specularAngleDeg = 315f,
            causticAlpha = 0f, innerShadowAlpha = 0.12f, grainAlpha = 0.06f, refraction = 0.08f,
        ),
        stroke = hairline(0x2EFFFFFF, 0.75f),
        shadow = shadow(14f, 5f, 0.2f),
        cornerRadiusDp = 24f, paddingDp = 20f, letterSpacingEm = -0.005f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 450, spacingDp = 10f,
        ink = c(0xFFFFFFFF), inkMuted = c(0x8AFFFFFF), accent = c(0xFFE4E8EF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("satin-01", "satin-02", "refract-02"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("st-small", "Satin Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("st-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("st-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("st-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("st-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)
