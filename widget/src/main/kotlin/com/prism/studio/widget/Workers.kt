package com.prism.studio.widget

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.provider.CalendarContract
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WallpaperSlot
import com.prism.studio.model.WidgetType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The single periodic worker shared by every 15-minute widget.
 *
 * One worker for all of them, not one each: fifteen placed widgets still means four wakeups an
 * hour. It fetches only the feeds something on screen actually needs, writes them through
 * [CachedFeed], and then asks the host to redraw. If no network-backed widget is placed,
 * `UpdateScheduler` never enqueues this at all.
 *
 * Failures retry with backoff rather than immediately: a phone with no signal should cost one
 * failed attempt per window, not a tight loop.
 */
@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: com.prism.studio.data.WidgetRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val placed = runCatching { repository.loadAll() }.getOrNull() ?: return Result.retry()
        val needed = placed.values.map { it.variant.type }.filterNot { it.isLocalOnly }.toSet()
        if (needed.isEmpty()) return Result.success()

        var anyFailed = false
        needed.forEach { type ->
            // Feed clients are injected per type in the app module; a failure for one feed must not
            // discard the others, because a stale crypto price should not blank the weather.
            val fetched = runCatching { FeedClients.fetch(applicationContext, type) }.getOrNull()
            if (fetched != null) CachedFeed.write(applicationContext, type, fetched) else anyFailed = true
        }

        PrismWidgetProvider.requestUpdate(applicationContext)
        return if (anyFailed) Result.retry() else Result.success()
    }
}

/**
 * Fires once at local midnight for the widgets whose content is a date.
 *
 * Month grids, day cards, quotes, and storage readouts change at most once a day, and waking them
 * on the 15-minute schedule would be pure waste. This is the cheapest cadence in the app: one
 * wakeup per day, no network.
 */
@HiltWorker
class DayRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        PrismWidgetProvider.requestUpdate(applicationContext)
        return Result.success()
    }
}

/**
 * Network clients for the four feed-backed widget types.
 *
 * Stubbed to return null until the providers are chosen and their keys are in place. Returning null
 * is a real behaviour, not a placeholder: `CachedFeed` keeps serving the last good value and the
 * renderer shows its configured-but-waiting state, which is exactly what should happen on a phone
 * with no signal.
 */
internal object FeedClients {
    suspend fun fetch(context: Context, type: WidgetType): String? = when (type) {
        WidgetType.Weather, WidgetType.Finance, WidgetType.Crypto, WidgetType.Quote -> null
        else -> null
    }
}

/**
 * The wallpaper palette, read once per update batch.
 *
 * Reading the system wallpaper requires a permission we do not request, so this reads the palette
 * of the Prism wallpaper the user applied, if any. When they are using their own wallpaper the map
 * is empty and every `ColorSpec.FromWallpaper` falls back to its declared default — which is why
 * that fallback exists on every one of them.
 */
object WallpaperPalette {

    @Volatile
    private var cached: Map<WallpaperSlot, Int> = emptyMap()

    fun current(context: Context): Map<WallpaperSlot, Int> = cached

    /** Called by the wallpaper feature after applying one of ours. */
    fun publish(palette: Map<WallpaperSlot, Int>) { cached = palette }

    fun clear() { cached = emptyMap() }
}

/**
 * Where a tap goes.
 *
 * A widget should open the thing it is about, not always the app that drew it. A calendar widget
 * that opens Prism instead of the calendar is a widget people delete. Decorative and system widgets
 * open the editor, because for those "I want to change this" is the only plausible intent.
 */
object WidgetTapRouter {

    /**
     * Where a tap goes.
     *
     * Two lessons are baked in here, both learned the hard way on device.
     *
     * **1. Do not probe system Settings screens.** Android 11 package-visibility filtering makes
     * `resolveActivity` return null for anything outside our own package unless it is declared in
     * `<queries>`. Gating every destination behind that check meant every candidate resolved to
     * null, the list came back empty, and every widget fell through to the style editor regardless
     * of its type. `android.provider.Settings` actions ship with the platform on every certified
     * device, so they are returned directly. Only third-party apps — music, fitness, weather — are
     * probed, and those are declared in the manifest.
     *
     * **2. A clock opens Date & time, not the alarm app.** Glass Widgets' analog clock opens the
     * Date & time settings screen, which is the right call: someone tapping a clock widget wants
     * the clock, not to set an alarm, and Date & time is where the format, timezone and dual-clock
     * controls live.
     */
    fun intentFor(context: Context, widget: ResolvedWidget, appWidgetId: Int): Intent {
        // Guaranteed by the platform. Returned without probing, for the reason above.
        fun settings(action: String) =
            Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Third-party. Probed, and declared in <queries> so the probe can succeed.
        fun firstInstalled(vararg options: Intent): Intent? = options.firstOrNull {
            it.resolveActivity(context.packageManager) != null
        }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return when (widget.variant.type) {
            WidgetType.DigitalClock, WidgetType.AnalogClock,
            WidgetType.WorldClock, WidgetType.Countdown ->
                settings(Settings.ACTION_DATE_SETTINGS)

            WidgetType.MonthCalendar, WidgetType.DayCard, WidgetType.Agenda ->
                firstInstalled(
                    Intent(Intent.ACTION_VIEW).setData(
                        CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
                    ),
                ) ?: settings(Settings.ACTION_DATE_SETTINGS)

            WidgetType.Battery ->
                settings(Settings.ACTION_BATTERY_SAVER_SETTINGS)

            WidgetType.Storage ->
                settings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)

            WidgetType.Ram, WidgetType.Cpu, WidgetType.SystemInfo ->
                settings(Settings.ACTION_DEVICE_INFO_SETTINGS)

            WidgetType.Network ->
                settings(Settings.ACTION_DATA_USAGE_SETTINGS)

            WidgetType.Weather, WidgetType.SunriseSunset ->
                firstInstalled(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather")),
                ) ?: settings(Settings.ACTION_DATE_SETTINGS)

            WidgetType.MusicPlayer ->
                firstInstalled(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
                ) ?: settings(Settings.ACTION_SOUND_SETTINGS)

            WidgetType.Steps, WidgetType.Health ->
                firstInstalled(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_FITNESS),
                ) ?: appIntent(context)

            WidgetType.Photo ->
                firstInstalled(
                    Intent(Intent.ACTION_VIEW).setType("image/*"),
                ) ?: appIntent(context)

            // Our own data. Nowhere else to send anyone.
            WidgetType.Notes, WidgetType.Todo, WidgetType.HabitTracker,
            WidgetType.Quote, WidgetType.Finance, WidgetType.Crypto ->
                appIntent(context)
        }
    }

    private fun appIntent(context: Context): Intent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}


