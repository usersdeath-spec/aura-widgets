package com.prism.studio.model

import kotlinx.serialization.Serializable

/**
 * Every widget in Prism is one of these content types.
 *
 * A type describes *what data is shown*, never *how it looks* — appearance lives entirely in
 * [WidgetStyle]. That split is what lets 32 design families x ~13 variants produce 400+ widgets
 * from ~24 content renderers instead of 400 hand-written layouts.
 *
 * @property refresh how often the widget's pixels can meaningfully change. The scheduler groups
 *   widgets by this value so we never wake the device more often than the slowest useful cadence.
 * @property requiresPermission runtime permission needed before the type can show real data.
 */
@Serializable
enum class WidgetType(
    val refresh: RefreshCadence,
    val requiresPermission: String? = null,
) {
    DigitalClock(RefreshCadence.Minute),
    AnalogClock(RefreshCadence.Minute),
    WorldClock(RefreshCadence.Minute),
    Countdown(RefreshCadence.Minute),

    DayCard(RefreshCadence.Daily),
    MonthCalendar(RefreshCadence.Daily),
    Agenda(RefreshCadence.Quarter, "android.permission.READ_CALENDAR"),
    SunriseSunset(RefreshCadence.Daily, "android.permission.ACCESS_COARSE_LOCATION"),

    Weather(RefreshCadence.Quarter, "android.permission.ACCESS_COARSE_LOCATION"),
    Battery(RefreshCadence.Event),
    Cpu(RefreshCadence.Quarter),
    Ram(RefreshCadence.Quarter),
    Storage(RefreshCadence.Daily),
    Network(RefreshCadence.Quarter),
    SystemInfo(RefreshCadence.Quarter),

    Notes(RefreshCadence.Event),
    Todo(RefreshCadence.Event),
    HabitTracker(RefreshCadence.Event),
    Quote(RefreshCadence.Daily),
    Photo(RefreshCadence.Event),
    MusicPlayer(RefreshCadence.Event, "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"),

    Finance(RefreshCadence.Quarter),
    Crypto(RefreshCadence.Quarter),
    Steps(RefreshCadence.Quarter, "android.permission.ACTIVITY_RECOGNITION"),
    Health(RefreshCadence.Quarter, "android.permission.ACTIVITY_RECOGNITION");

    /** True when the widget can be redrawn with zero I/O — used to skip WorkManager entirely. */
    val isLocalOnly: Boolean
        get() = this !in setOf(Weather, Finance, Crypto, Quote)
}

/** How often a widget needs new pixels. Never expressed in milliseconds at the call site. */
@Serializable
enum class RefreshCadence(val approxMinutes: Int) {
    /** Driven by ACTION_TIME_TICK while the screen is on. Costs nothing while asleep. */
    Minute(1),

    /** Coalesced 15-minute WorkManager window — the platform minimum, and plenty. */
    Quarter(15),

    /** One alarm at local midnight. */
    Daily(1440),

    /** Redrawn only when something actually changes (battery level, note edited, track changed). */
    Event(Int.MAX_VALUE),
}

/** Home-screen footprint in cells, used for preview aspect ratio and layout selection. */
@Serializable
enum class WidgetSize(val cellsWide: Int, val cellsHigh: Int) {
    Small(2, 2),
    Wide(4, 2),
    Tall(2, 4),
    Large(4, 4),
    Banner(4, 1);

    val aspectRatio: Float get() = cellsWide.toFloat() / cellsHigh.toFloat()
}
