package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * COLOUR & LIGHT — eight families built on the mesh and gradient surfaces.
 *
 * The risk in this category is that everything becomes "a nice gradient". Each family is therefore
 * pinned to a specific light condition — polar night, late dusk, overcast north, greenhouse noon —
 * and its blob positions and angles are derived from that, not chosen for prettiness.
 */

/** Polar night: cold blobs low on the frame, dark base above, as the real thing appears. */
internal val Aurora = family(
    id = "aurora",
    name = "Aurora",
    note = "Cold light low on a dark sky.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = mesh(
            0xFF070B14,
            Triple(0.18f, 0.86f, 0xFF2BE0A8), Triple(0.62f, 0.94f, 0xFF3C7CE8),
            Triple(0.88f, 0.72f, 0xFF8B5CF6),
            radius = 0.7f, blur = 56f,
        ),
        stroke = hairline(0x24FFFFFF),
        cornerRadiusDp = 26f, paddingDp = 20f, letterSpacingEm = -0.015f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 10f,
        ink = c(0xFFF2F6FF), inkMuted = c(0x99F2F6FF), accent = c(0xFF2BE0A8),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("aurora-01", "aurora-02", "aurora-03"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("au-small", "Aurora Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("au-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("au-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("au-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("au-photo", "Photo", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
    ),
)

/** Smooth two-stop gradients at a fixed 135°, so a wall of these widgets shares one light direction. */
internal val GradientFlow = family(
    id = "gradient-flow",
    name = "Gradient Flow",
    note = "Two stops, one light direction, no ornament.",
    mood = Mood.Bright,
    base = WidgetStyle(
        surface = grad(0f to 0xFF6A5AE0, 1f to 0xFF31A2AA, angle = 135f),
        stroke = hairline(0x2EFFFFFF),
        shadow = shadow(16f, 6f, 0.24f),
        cornerRadiusDp = 24f, paddingDp = 20f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 600, spacingDp = 10f,
        ink = c(0xFFFFFFFF), inkMuted = c(0xA3FFFFFF), accent = c(0xFFFFFFFF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("flow-01", "flow-02", "flow-03"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("gf-dated", "Time & Date", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("gf-warm", "Warm Flow", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split,
            StyleDelta(surface = Surface.Gradient(
                listOf(GradientStop(0f, ColorSpec.Solid(0xFFFF7A59)), GradientStop(1f, ColorSpec.Solid(0xFFB65CFF))), 135f))),
        v("gf-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("gf-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
    ),
)

/** Late dusk: warm above, cool below, with the break sitting high the way real sunsets do. */
internal val SunsetFade = family(
    id = "sunset-fade",
    name = "Sunset Fade",
    note = "Warm above, cool below, break sitting high.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = grad(0f to 0xFFD47300, 0.32f to 0xFFF25772, 1f to 0xFF3B2A6E, angle = 90f),
        stroke = hairline(0x2EFFFFFF),
        shadow = shadow(16f, 6f, 0.28f),
        cornerRadiusDp = 22f, paddingDp = 20f, letterSpacingEm = -0.015f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 550, spacingDp = 10f,
        ink = c(0xFFFFF6EE), inkMuted = c(0xA8FFF6EE), accent = c(0xFFFFD9A0),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("dusk-01", "dusk-02", "dusk-03"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("sf-sun", "Sunrise & Sunset", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("sf-small", "Dusk Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("sf-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("sf-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
    ),
)

/** Bright, saturated, rounded. The only family in the app that is allowed to be loud and friendly. */
internal val CandyPop = family(
    id = "candy-pop",
    name = "Candy Pop",
    note = "Saturated, rounded, unapologetically loud.",
    mood = Mood.Playful,
    base = WidgetStyle(
        surface = solid(0xFFE83C86),
        stroke = Stroke(2.5f, c(0xFF1A1026)),
        shadow = shadow(0f, 5f, 0.9f, 0xFF1A1026),
        cornerRadiusDp = 28f, paddingDp = 18f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.Rounded, fontWeight = 800, spacingDp = 8f,
        ink = c(0xFFFFFFFF), inkMuted = c(0xC2FFFFFF), accent = c(0xFFFFE45E),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("candy-01", "candy-02", "candy-03"),
    core = Core(
        clock = ContentLayout.Hero, battery = ContentLayout.Ring,
        weatherDelta = StyleDelta(surface = Surface.Solid(ColorSpec.Solid(0xFF1B7E9E))),
        todoDelta = StyleDelta(surface = Surface.Solid(ColorSpec.Solid(0xFF3E9E33))),
    ),
    extras = listOf(
        v("cp-small", "Pop Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("cp-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("cp-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("cp-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Fired clay: earth pigments only, matte surfaces, and type set slightly wide. */
internal val Terracotta = family(
    id = "terracotta",
    name = "Terracotta",
    note = "Earth pigments, matte surfaces, wide setting.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = solid(0xFFC96F4A),
        stroke = hairline(0x33FFFFFF),
        shadow = shadow(12f, 5f, 0.2f),
        cornerRadiusDp = 18f, paddingDp = 20f, letterSpacingEm = 0.015f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 400, spacingDp = 10f,
        ink = c(0xFFFDF3EA), inkMuted = c(0xA3FDF3EA), accent = c(0xFF4A5D3F),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("clay-01", "clay-02", "clay-03"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("tc-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("tc-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("tc-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("tc-sun", "Sun", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)

/** Greenhouse noon: deep green base, warm light from top-left, everything slightly overexposed. */
internal val Botanical = family(
    id = "botanical",
    name = "Botanical",
    note = "Deep green under warm glasshouse light.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = mesh(
            0xFF12281C,
            Triple(0.2f, 0.16f, 0x66C9E86B), Triple(0.82f, 0.7f, 0x4D2F7D52),
            radius = 0.66f, blur = 44f,
        ),
        stroke = hairline(0x2ED8F0C0),
        cornerRadiusDp = 22f, paddingDp = 20f, letterSpacingEm = -0.005f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 400, spacingDp = 10f,
        ink = c(0xFFF1F7E8), inkMuted = c(0x99F1F7E8), accent = c(0xFFC9E86B),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("leaf-01", "leaf-02", "leaf-03"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("bo-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
        v("bo-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("bo-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("bo-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
    ),
)

/** Overcast north light: no warm tones anywhere, very low chroma, very high legibility. */
internal val NordicFrost = family(
    id = "nordic-frost",
    name = "Nordic Frost",
    note = "Overcast north light. Cold, low chroma, very legible.",
    mood = Mood.Cool,
    base = WidgetStyle(
        surface = solid(0xFF1B2430),
        stroke = hairline(0x33A9C0D6),
        cornerRadiusDp = 16f, paddingDp = 18f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 9f,
        ink = c(0xFFE4EDF5), inkMuted = c(0x8AE4EDF5), accent = c(0xFF7FB2D9),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("fjord-01", "fjord-02", "fjord-03"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("nf-small", "Frost Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("nf-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("nf-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("nf-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/** The only family whose palette is a function of the date. Four palettes, switched at the solstice. */
internal val SeasonalBloom = family(
    id = "seasonal-bloom",
    name = "Seasonal Bloom",
    note = "Palette changes with the season, on its own.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = grad(0f to 0xFFF7CFD8, 1f to 0xFFE2A9C0, angle = 120f),
        stroke = hairline(0x40FFFFFF),
        shadow = shadow(12f, 4f, 0.16f),
        cornerRadiusDp = 24f, paddingDp = 20f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Rounded, fontWeight = 550, spacingDp = 10f,
        ink = c(0xFF3B2430), inkMuted = c(0x8A3B2430), accent = c(0xFFB6577C),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("bloom-01", "bloom-02", "bloom-03", "bloom-04"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("sb-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("sb-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("sb-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("sb-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
    ),
)
