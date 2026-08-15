package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * FOUNDATION — the four families the app leads with.
 *
 * These are the ones on the store listing and the first screen of onboarding, chosen because
 * between them they demonstrate the range: no surface at all, a true-black surface, and an
 * ornamented one. The translucent end of the range is now its own collection — see
 * `FamiliesLiquidGlass.kt`. If a user only ever installs from this file, they
 * should still feel they got a designed product.
 */

/**
 * MINIMAL MONO — the family that has to be perfect, because it hides nothing.
 *
 * No surface, no colour, no ornament: type carries the whole idea. The deliberate risk is negative
 * tracking at display sizes (-0.04em), applied only where numerals are large enough to survive it.
 * It is the reason these read as designed rather than as default system text.
 */
internal val MinimalMono = family(
    id = "minimal-mono",
    name = "Minimal Mono",
    note = "No surface, no colour. Type does all the work.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = Surface.None,
        cornerRadiusDp = 0f, paddingDp = 6f, letterSpacingEm = -0.04f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 6f,
        ink = c(0xFFFFFFFF), inkMuted = c(0x8AFFFFFF), accent = c(0xFFFFFFFF),
        motion = WidgetMotion.DigitRoll,
    ),
    wallpapers = listOf("mono-01", "mono-02", "void-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("mm-stacked", "Stacked Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split,
            StyleDelta(fontWeight = 300, letterSpacingEm = -0.06f)),
        v("mm-dated", "Time & Date", WidgetType.DigitalClock, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("mm-centre", "Centred Time", WidgetType.DigitalClock, WidgetSize.Banner, ContentLayout.Hero,
            StyleDelta(alignment = Alignment.Center)),
        v("mm-dial", "Hairline Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("mm-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("mm-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("mm-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack,
            StyleDelta(fontFamily = FontFamilyToken.Serif, letterSpacingEm = 0f, fontWeight = 400)),
    ),
)

/**
 * AMOLED BLACK — designed around a hardware fact.
 *
 * On an OLED panel a pure-black pixel is an unlit pixel, so this family uses true #000 and keeps
 * lit pixels to a minimum: thin strokes, small accents, no fills. It is genuinely the lowest-power
 * family in the app, and the catalog copy says so, because that is a real reason to choose it.
 */
internal val AmoledBlack = family(
    id = "amoled-black",
    name = "AMOLED Black",
    note = "True black surfaces. Fewer lit pixels, longer battery.",
    mood = Mood.Dark,
    base = WidgetStyle(
        surface = solid(0xFF000000),
        stroke = hairline(0x2EFFFFFF),
        cornerRadiusDp = 22f, paddingDp = 18f, letterSpacingEm = 0.01f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 500, spacingDp = 8f,
        ink = c(0xFFF2F4F8), inkMuted = c(0x70F2F4F8), accent = c(0xFFF2F4F8),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("void-01", "void-02", "grid-void-01"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.HeroWithGauge),
    extras = listOf(
        v("ab-small", "Black Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Split),
        v("ab-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("ab-cpu", "CPU", WidgetType.Cpu, WidgetSize.Small, ContentLayout.Chart),
        v("ab-ram", "RAM", WidgetType.Ram, WidgetSize.Small, ContentLayout.Chart),
        v("ab-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
        v("ab-network", "Network", WidgetType.Network, WidgetSize.Small, ContentLayout.Chart),
        v("ab-system", "System", WidgetType.SystemInfo, WidgetSize.Tall, ContentLayout.Stack),
    ),
)

/**
 * LUXURY GOLD — restraint is what makes it read as expensive.
 *
 * Gold goes wrong when it is used as a fill. Here it is a hairline and a single accent mark on a
 * warm near-black with a barely-perceptible vertical gradient; the type is a high-contrast serif at
 * light weight with open tracking. The 14dp radius is deliberate: sharp corners read as jewellery,
 * soft ones as plastic.
 */
internal val LuxuryGold = family(
    id = "luxury-gold",
    name = "Luxury Gold",
    note = "Warm charcoal, a light serif, and one gold line.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = grad(0f to 0xFF16130F, 1f to 0xFF0C0A08, angle = 90f),
        stroke = edge(0.75f, 0f to 0xFFE7CE7A, 0.55f to 0xFFB8912F, 1f to 0x66C9A227),
        shadow = shadow(14f, 5f, 0.4f),
        cornerRadiusDp = 14f, paddingDp = 20f, letterSpacingEm = 0.02f,
        fontFamily = FontFamilyToken.Serif, fontWeight = 300, spacingDp = 12f,
        alignment = Alignment.Center,
        ink = c(0xFFF3EBDC), inkMuted = c(0x8FD8C9A8), accent = c(0xFFC9A227),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("gilt-01", "gilt-02", "smoke-01"),
    core = Core(clock = ContentLayout.HeroLabelled, battery = ContentLayout.Ring),
    extras = listOf(
        v("lx-dial", "Gold Dial", WidgetType.AnalogClock, WidgetSize.Small, ContentLayout.Dial),
        v("lx-small", "Gold Time", WidgetType.DigitalClock, WidgetSize.Small, ContentLayout.Hero),
        v("lx-day", "Day", WidgetType.DayCard, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("lx-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("lx-quote", "Quote", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
        v("lx-finance", "Portfolio", WidgetType.Finance, WidgetSize.Wide, ContentLayout.Chart),
        v("lx-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("lx-sun", "Sun", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)
