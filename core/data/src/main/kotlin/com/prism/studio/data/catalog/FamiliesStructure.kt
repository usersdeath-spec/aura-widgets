package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * STRUCTURE & RESTRAINT — eight families where the idea is a set of rules, not a texture.
 *
 * Each one borrows a real design tradition's *constraints* rather than its look: Swiss Grid keeps
 * the grid and the flush-left setting, Bauhaus keeps the three primaries and the circle-square-
 * triangle vocabulary, Zen keeps asymmetry and empty space. Nothing here reproduces existing
 * artwork; the traditions supply the rules and the drawing is ours.
 */

/** Birch, wool, low sun. Warm off-white, never pure white, and generous air. */
internal val Scandinavian = family(
    id = "scandinavian",
    name = "Scandinavian",
    note = "Warm off-white, wide margins, one soft accent.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = solid(0xFFF4F1EC),
        stroke = hairline(0x14000000),
        shadow = shadow(10f, 4f, 0.1f),
        cornerRadiusDp = 20f, paddingDp = 24f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 450, spacingDp = 12f,
        ink = c(0xFF2C2E33), inkMuted = c(0x7A2C2E33), accent = c(0xFF9CAF97),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("birch-01", "birch-02", "linen-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("sc-dated", "Time & Date", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("sc-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("sc-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("sc-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack,
            StyleDelta(fontFamily = FontFamilyToken.Serif, fontWeight = 400)),
        v("sc-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)

/** Sumi ink on washi. Asymmetric by rule: nothing is centred, and the accent appears once. */
internal val JapaneseZen = family(
    id = "japanese-zen",
    name = "Japanese Zen",
    note = "Ink on paper. Asymmetric, sparse, one red mark.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = solid(0xFFF7F4EC),
        stroke = hairline(0x1A1B1B1B),
        cornerRadiusDp = 4f, paddingDp = 26f, letterSpacingEm = 0.02f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 300, spacingDp = 14f,
        alignment = Alignment.Start,
        ink = c(0xFF1B1B1B), inkMuted = c(0x661B1B1B), accent = c(0xFFB33A2B),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("washi-01", "washi-02", "sumi-01"),
    core = Core(clock = ContentLayout.Hero, calendar = ContentLayout.Grid, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("jz-dial", "Ink Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("jz-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("jz-sun", "Sun", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("jz-season", "Season", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
    ),
)

/** International typographic style: flush left, ragged right, one red, and a visible grid. */
internal val SwissGrid = family(
    id = "swiss-grid",
    name = "Swiss Grid",
    note = "Flush left, ragged right, one red, visible grid.",
    mood = Mood.Technical,
    base = WidgetStyle(
        surface = solid(0xFFF2F2F0),
        stroke = hairline(0x33000000, 1f),
        cornerRadiusDp = 0f, paddingDp = 14f, letterSpacingEm = -0.025f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 700, spacingDp = 6f,
        alignment = Alignment.Start,
        ink = c(0xFF111111), inkMuted = c(0x8C111111), accent = c(0xFFE10600),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("grid-01", "grid-02", "grid-03"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, weather = ContentLayout.Split),
    extras = listOf(
        v("sw-hero", "Grid Time", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.Hero),
        v("sw-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("sw-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("sw-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("sw-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Three primaries, geometric forms, zero gradients. The accent rotates per variant by design. */
internal val BauhausPrimary = family(
    id = "bauhaus-primary",
    name = "Bauhaus Primary",
    note = "Red, yellow, blue. Circles, squares, hard edges.",
    mood = Mood.Bold,
    base = WidgetStyle(
        surface = solid(0xFFEFEAE0),
        stroke = Stroke(2f, c(0xFF16161A)),
        cornerRadiusDp = 2f, paddingDp = 16f, letterSpacingEm = -0.03f,
        fontFamily = FontFamilyToken.GroteskDisplay, fontWeight = 700, spacingDp = 8f,
        ink = c(0xFF16161A), inkMuted = c(0x8016161A), accent = c(0xFFE03C31),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("bauhaus-01", "bauhaus-02", "bauhaus-03"),
    core = Core(
        clock = ContentLayout.Hero,
        battery = ContentLayout.Ring, batteryDelta = StyleDelta(accent = ColorSpec.Solid(0xFF1E5AA8)),
        weatherDelta = StyleDelta(accent = ColorSpec.Solid(0xFFF2B705)),
    ),
    extras = listOf(
        v("bh-dial", "Circle Clock", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("bh-stack", "Stacked Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("bh-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("bh-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
    ),
)

/** Concrete. Heavy slab type, no radius, and shadows that sit hard rather than diffusing. */
internal val BrutalistSlab = family(
    id = "brutalist-slab",
    name = "Brutalist Slab",
    note = "Concrete blocks and type that fills the frame.",
    mood = Mood.Bold,
    base = WidgetStyle(
        surface = solid(0xFF2E2E2B),
        stroke = Stroke(3f, c(0xFFEDE9DF)),
        cornerRadiusDp = 0f, paddingDp = 12f, letterSpacingEm = -0.04f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 800, spacingDp = 4f,
        ink = c(0xFFEDE9DF), inkMuted = c(0x8CEDE9DF), accent = c(0xFFFFD400),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("concrete-01", "concrete-02", "concrete-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge, todo = ContentLayout.Stack),
    extras = listOf(
        v("bs-stack", "Block Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("bs-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("bs-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
        v("bs-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Editorial: a magazine page reduced to a widget. High-contrast serif, rules, small caps labels. */
internal val InkSerif = family(
    id = "ink-serif",
    name = "Ink Serif",
    note = "Editorial setting: high-contrast serif and hairline rules.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = solid(0xFF12100E),
        stroke = hairline(0x33F0E9DC),
        cornerRadiusDp = 6f, paddingDp = 22f, letterSpacingEm = 0.01f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 300, spacingDp = 12f,
        ink = c(0xFFF0E9DC), inkMuted = c(0x8AF0E9DC), accent = c(0xFFC0A062),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("ink-01", "ink-02", "paper-03"),
    core = Core(clock = ContentLayout.HeroLabelled, notes = ContentLayout.Stack),
    extras = listOf(
        v("is-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("is-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("is-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("is-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("is-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/** Every colour comes from the device. The one family that looks different on every phone. */
internal val MaterialYou = family(
    id = "material-you",
    name = "Material You",
    note = "Every colour taken from your wallpaper, live.",
    mood = Mood.Bright,
    base = WidgetStyle(
        surface = Surface.Solid(dyn(DynamicRole.Surface, 0xFF1B1B1F)),
        stroke = null,
        cornerRadiusDp = 28f, paddingDp = 20f, letterSpacingEm = -0.005f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 10f,
        ink = dyn(DynamicRole.OnSurface, 0xFFE5E1E6),
        inkMuted = dyn(DynamicRole.Outline, 0x99E5E1E6),
        accent = dyn(DynamicRole.Primary, 0xFFB9C3FF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("mesh-01", "mesh-02", "mesh-03"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("my-small", "Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("my-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("my-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("my-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("my-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
    ),
)

/** Soft extrusion. Only works on a mid-tone base, so the palette is fixed and the ink is low-contrast. */
internal val NeumorphSoft = family(
    id = "neumorph-soft",
    name = "Neumorph Soft",
    note = "Controls pressed out of a single soft surface.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = Surface.Extruded(c(0xFFE0E3EA), depthDp = 7f),
        stroke = null,
        cornerRadiusDp = 26f, paddingDp = 22f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Rounded, fontWeight = 600, spacingDp = 10f,
        ink = c(0xFF4A5162), inkMuted = c(0x8A4A5162), accent = c(0xFF6D7FF0),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("soft-01", "soft-02", "soft-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring, music = ContentLayout.Controls),
    extras = listOf(
        v("ns-dial", "Soft Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("ns-inset", "Inset Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split,
            StyleDelta(surface = Surface.Extruded(ColorSpec.Solid(0xFFE0E3EA), depthDp = 6f, inset = true))),
        v("ns-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("ns-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
    ),
)
