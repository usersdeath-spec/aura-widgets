package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * SIGNAL — six families that look like instruments.
 *
 * The shared discipline here is that the technical language has to mean something. A HUD bracket
 * marks a real value's bounds, a CRT scanline sits on the baseline grid, a blueprint's dimension
 * lines measure the widget they are drawn on. Decoration that only mimics an instrument is the
 * thing that makes this category look cheap, so none of it is allowed.
 */

/** Neon on wet asphalt. Glow is expensive to read, so it is confined to the accent and the stroke. */
internal val CyberpunkNeon = family(
    id = "cyberpunk-neon",
    name = "Cyberpunk Neon",
    note = "Neon on wet asphalt. Glow only where it means something.",
    mood = Mood.Bold,
    base = WidgetStyle(
        surface = grad(0f to 0xFF120A22, 1f to 0xFF05040C, angle = 110f),
        stroke = edge(1.25f, 0f to 0xFFFF2E88, 1f to 0xFF00E5FF),
        glow = glow(16f, 0xFFFF2E88, 0.5f),
        cornerRadiusDp = 6f, paddingDp = 16f, letterSpacingEm = 0.04f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 600, spacingDp = 8f,
        ink = c(0xFFF2E9FF), inkMuted = c(0x7AF2E9FF), accent = c(0xFF00E5FF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("neon-01", "neon-02", "neon-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("cn-stack", "Neon Stack", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("cn-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("cn-network", "Network", WidgetType.Network, WidgetSize.Small, ContentLayout.Chart),
        v("cn-crypto", "Crypto", WidgetType.Crypto, WidgetSize.Wide, ContentLayout.Chart),
        v("cn-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Aviation HUD: phosphor green, thin rules, brackets that mark real bounds rather than decorate. */
internal val HudTactical = family(
    id = "hud-tactical",
    name = "HUD Tactical",
    note = "Phosphor green instrument marks that measure something.",
    mood = Mood.Technical,
    base = WidgetStyle(
        surface = solid(0xE6050A07),
        stroke = hairline(0x8A5BFF9E, 1f),
        cornerRadiusDp = 2f, paddingDp = 16f, letterSpacingEm = 0.08f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 500, spacingDp = 7f,
        ink = c(0xFF5BFF9E), inkMuted = c(0x705BFF9E), accent = c(0xFF5BFF9E),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("hud-01", "hud-02", "grid-void-01"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, battery = ContentLayout.Ring),
    extras = listOf(
        v("ht-hero", "HUD Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("ht-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("ht-ram", "RAM", WidgetType.Ram, WidgetSize.Small, ContentLayout.Chart),
        v("ht-network", "Network", WidgetType.Network, WidgetSize.Small, ContentLayout.Chart),
        v("ht-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("ht-sun", "Sun", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)

/** RGB, but the colour tracks a value instead of cycling. A cycling widget is a battery bug. */
internal val RgbGaming = family(
    id = "rgb-gaming",
    name = "RGB Gaming",
    note = "Reactive colour that tracks the value, not a cycle.",
    mood = Mood.Bold,
    base = WidgetStyle(
        surface = solid(0xFF0A0A0F),
        stroke = edge(2f, 0f to 0xFFFF3B3B, 0.5f to 0xFF8B5CF6, 1f to 0xFF00D1FF),
        glow = glow(18f, 0xFF8B5CF6, 0.55f),
        cornerRadiusDp = 14f, paddingDp = 16f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 800, spacingDp = 8f,
        ink = c(0xFFFFFFFF), inkMuted = c(0x8AFFFFFF), accent = c(0xFF00D1FF),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("rig-01", "rig-02", "neon-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("rg-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Ring),
        v("rg-ram", "RAM", WidgetType.Ram, WidgetSize.Small, ContentLayout.Ring),
        v("rg-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.Ring),
        v("rg-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("rg-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Low-res on purpose: fixed 4px grid, no anti-aliased curves, palette limited to eight colours. */
internal val PixelRetro = family(
    id = "pixel-retro",
    name = "Pixel Retro",
    note = "Four-pixel grid, eight colours, no smooth curves.",
    mood = Mood.Playful,
    base = WidgetStyle(
        surface = solid(0xFF1D2B53),
        stroke = Stroke(4f, c(0xFF29ADFF)),
        cornerRadiusDp = 0f, paddingDp = 16f, letterSpacingEm = 0.06f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 700, spacingDp = 8f,
        ink = c(0xFFFFF1E8), inkMuted = c(0x8AFFF1E8), accent = c(0xFFFFEC27),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("pixel-01", "pixel-02", "pixel-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("pr-stack", "Pixel Stack", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("pr-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("pr-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("pr-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Amber phosphor terminal. Monospace everywhere, and the accent is the same amber at full lift. */
internal val CrtAmber = family(
    id = "crt-amber",
    name = "CRT Amber",
    note = "Amber phosphor terminal, monospaced throughout.",
    mood = Mood.Technical,
    base = WidgetStyle(
        surface = solid(0xFF120C04),
        stroke = hairline(0x4DFFB000, 1f),
        glow = glow(10f, 0xFFFFB000, 0.28f),
        cornerRadiusDp = 8f, paddingDp = 16f, letterSpacingEm = 0.05f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 500, spacingDp = 7f,
        ink = c(0xFFFFB000), inkMuted = c(0x70FFB000), accent = c(0xFFFFD873),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("crt-01", "crt-02", "grid-void-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge, notes = ContentLayout.Stack),
    extras = listOf(
        v("ca-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("ca-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("ca-network", "Network", WidgetType.Network, WidgetSize.Small, ContentLayout.Chart),
        v("ca-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/** Technical drawing: cyan grid, white line work, dimension marks that measure the widget itself. */
internal val Blueprint = family(
    id = "blueprint",
    name = "Blueprint",
    note = "Drafting paper. Dimension lines that measure the widget.",
    mood = Mood.Technical,
    base = WidgetStyle(
        surface = solid(0xFF0E2A4A),
        stroke = hairline(0x99A8D8FF, 1f),
        cornerRadiusDp = 2f, paddingDp = 18f, letterSpacingEm = 0.06f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 400, spacingDp = 8f,
        ink = c(0xFFE8F4FF), inkMuted = c(0x7AE8F4FF), accent = c(0xFF7EC8FF),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("draft-01", "draft-02", "grid-01"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("bp-hero", "Draft Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("bp-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("bp-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("bp-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("bp-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
    ),
)
