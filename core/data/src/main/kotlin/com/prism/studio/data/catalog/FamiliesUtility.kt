package com.prism.studio.data.catalog

import com.prism.studio.model.*

/**
 * PURPOSE — five families organised around a task rather than a look.
 *
 * These exist because a real home screen is not a mood board. Someone tracking a portfolio, a
 * training block, or a week of deadlines needs density, and density has its own aesthetic rules:
 * tighter padding, smaller type scale, more rows, muted surfaces so the data is the loudest thing
 * on screen. Each still ships the full seven-pillar set so a user can furnish a whole screen from
 * one family without leaving its language.
 */

/** Boardroom: slate, one steel accent, and the tightest padding in the app outside Swiss Grid. */
internal val ExecutiveSlate = family(
    id = "executive-slate",
    name = "Executive Slate",
    note = "Slate and steel. Dense, quiet, businesslike.",
    mood = Mood.Technical,
    base = WidgetStyle(
        surface = grad(0f to 0xFF232830, 1f to 0xFF171A20, angle = 100f),
        stroke = hairline(0x26C7D2E0),
        shadow = shadow(12f, 4f, 0.3f),
        cornerRadiusDp = 12f, paddingDp = 16f, letterSpacingEm = -0.01f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 550, spacingDp = 7f, typeScale = 0.95f,
        ink = c(0xFFE7ECF3), inkMuted = c(0x8AE7ECF3), accent = c(0xFF5B8DEF),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("slate-01", "slate-02", "grid-02"),
    core = Core(clock = ContentLayout.HeroLabelled, todo = ContentLayout.Stack, notes = ContentLayout.Stack),
    extras = listOf(
        v("es-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("es-world", "World Clock", WidgetType.WorldClock, WidgetSize.Tall, ContentLayout.Stack),
        v("es-countdown", "Deadline", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("es-finance", "Markets", WidgetType.Finance, WidgetSize.Wide, ContentLayout.Chart),
        v("es-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
    ),
)

/** Ledger paper: green-on-cream, tabular figures, and gain/loss as the only colour in the family. */
internal val Ledger = family(
    id = "ledger",
    name = "Ledger",
    note = "Ruled paper and tabular figures. Colour means gain or loss.",
    mood = Mood.Quiet,
    base = WidgetStyle(
        surface = solid(0xFFF3F1E7),
        stroke = hairline(0x2E1E4032),
        cornerRadiusDp = 6f, paddingDp = 16f, letterSpacingEm = 0f,
        fontFamily = FontFamilyToken.Mono, fontWeight = 500, spacingDp = 7f, typeScale = 0.95f,
        ink = c(0xFF1E2A24), inkMuted = c(0x7A1E2A24), accent = c(0xFF2E7D5B),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("ledger-01", "ledger-02", "paper-02"),
    core = Core(clock = ContentLayout.Split, clockSize = WidgetSize.Small, todo = ContentLayout.Stack),
    extras = listOf(
        v("ld-finance", "Portfolio", WidgetType.Finance, WidgetSize.Wide, ContentLayout.Chart),
        v("ld-crypto", "Crypto", WidgetType.Crypto, WidgetSize.Wide, ContentLayout.Chart),
        v("ld-finance-sm", "Ticker", WidgetType.Finance, WidgetSize.Small, ContentLayout.HeroLabelled),
        v("ld-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("ld-countdown", "Countdown", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
    ),
)

/** Training: one ring per metric, high-contrast on black, numbers readable mid-run at arm's length. */
internal val Pulse = family(
    id = "pulse",
    name = "Pulse",
    note = "One ring per metric, readable at arm's length.",
    mood = Mood.Bold,
    base = WidgetStyle(
        surface = solid(0xFF0C0D10),
        stroke = null,
        cornerRadiusDp = 24f, paddingDp = 18f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 700, spacingDp = 8f, typeScale = 1.05f,
        ink = c(0xFFFFFFFF), inkMuted = c(0x8AFFFFFF), accent = c(0xFFFF3B5C),
        motion = WidgetMotion.ArcSweep,
    ),
    wallpapers = listOf("pulse-01", "pulse-02", "void-01"),
    core = Core(clock = ContentLayout.Hero, battery = ContentLayout.Ring),
    extras = listOf(
        v("pu-steps", "Steps", WidgetType.Steps, WidgetSize.Small, ContentLayout.Ring),
        v("pu-health", "Activity", WidgetType.Health, WidgetSize.Wide, ContentLayout.Chart),
        v("pu-habit", "Streak", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("pu-countdown", "Race Day", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("pu-sun", "Daylight", WidgetType.SunriseSunset, WidgetSize.Wide, ContentLayout.Split),
    ),
)

/** Record sleeve: square-first, heavy sans, and the only family where music is the largest widget. */
internal val Vinyl = family(
    id = "vinyl",
    name = "Vinyl",
    note = "Sleeve proportions. Music is the biggest thing here.",
    mood = Mood.Warm,
    base = WidgetStyle(
        surface = solid(0xFF17130F),
        stroke = hairline(0x33E8DCC8),
        cornerRadiusDp = 4f, paddingDp = 16f, letterSpacingEm = -0.02f,
        fontFamily = FontFamilyToken.Condensed, fontWeight = 700, spacingDp = 8f,
        ink = c(0xFFF3E9D8), inkMuted = c(0x8AF3E9D8), accent = c(0xFFE9704B),
        motion = WidgetMotion.CrossFade,
    ),
    wallpapers = listOf("sleeve-01", "sleeve-02", "grain-01"),
    core = Core(
        clock = ContentLayout.Hero,
        music = ContentLayout.Controls, musicDelta = StyleDelta(paddingDp = 14f),
        battery = ContentLayout.HeroWithGauge,
    ),
    extras = listOf(
        v("vi-music-lg", "Sleeve", WidgetType.MusicPlayer, WidgetSize.Large, ContentLayout.Overlay),
        v("vi-music-sm", "Transport", WidgetType.MusicPlayer, WidgetSize.Banner, ContentLayout.Controls),
        v("vi-photo", "Cover", WidgetType.Photo, WidgetSize.Large, ContentLayout.Overlay),
        v("vi-quote", "Lyric Card", WidgetType.Quote, WidgetSize.Wide, ContentLayout.Stack),
    ),
)

/** Planning: the densest family in the app. Small type scale, tight rows, everything on one grid. */
internal val FocusGrid = family(
    id = "focus-grid",
    name = "Focus Grid",
    note = "The densest family. Small type, tight rows, one grid.",
    mood = Mood.Technical,
    base = WidgetStyle(
        surface = solid(0xFF15161A),
        stroke = hairline(0x24FFFFFF),
        cornerRadiusDp = 14f, paddingDp = 14f, letterSpacingEm = -0.005f,
        fontFamily = FontFamilyToken.Grotesk, fontWeight = 500, spacingDp = 6f, typeScale = 0.9f,
        ink = c(0xFFEDEFF3), inkMuted = c(0x7AEDEFF3), accent = c(0xFF64D2A0),
        motion = WidgetMotion.None,
    ),
    wallpapers = listOf("focus-01", "focus-02", "grid-03"),
    core = Core(
        clock = ContentLayout.Split, clockSize = WidgetSize.Small,
        todo = ContentLayout.Stack, notes = ContentLayout.Stack,
    ),
    extras = listOf(
        v("fg-agenda", "Agenda", WidgetType.Agenda, WidgetSize.Tall, ContentLayout.Stack),
        v("fg-habit", "Habits", WidgetType.HabitTracker, WidgetSize.Large, ContentLayout.Grid),
        v("fg-countdown", "Deadline", WidgetType.Countdown, WidgetSize.Wide, ContentLayout.HeroLabelled),
        v("fg-week", "Week", WidgetType.MonthCalendar, WidgetSize.Wide, ContentLayout.Grid),
        v("fg-storage", "Storage", WidgetType.Storage, WidgetSize.Small, ContentLayout.HeroWithGauge),
    ),
)
