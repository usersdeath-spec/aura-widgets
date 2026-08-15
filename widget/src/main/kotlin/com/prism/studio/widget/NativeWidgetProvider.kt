package com.prism.studio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.RemoteViews

/**
 * Base class for the generated per-design widgets.
 *
 * The layout itself is self-sufficient — a `TextClock` is driven by the platform, so there is
 * nothing to refresh and no worker, alarm, bitmap or battery cost. What [onUpdate] *does* have to do
 * is attach a tap target, because a widget that does nothing when tapped feels broken, and all 572
 * of these previously did nothing at all.
 *
 * Where the tap goes is derived from the layout name rather than from stored state: the generator
 * encodes the widget's kind in it (`nw_clock_hero_marble`, `nw_date_card_obsidian`), so a clock can
 * open the alarms screen and a date widget the calendar without this class needing to look anything
 * up. That keeps the update path free of both Room and Hilt, which is what lets these widgets cost
 * nothing to run.
 */
open class NativeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val info = manager.getAppWidgetInfo(ids.firstOrNull() ?: return) ?: return
        val layoutId = info.initialLayout
        val layoutName = runCatching { context.resources.getResourceEntryName(layoutId) }
            .getOrDefault("")

        val intent = destinationFor(context, layoutName)
        val pending = PendingIntent.getActivity(
            context,
            layoutName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, layoutId)
            // The tap target is the root, so the whole widget is tappable rather than just the text.
            views.setOnClickPendingIntent(android.R.id.background, pending)
            runCatching { manager.updateAppWidget(id, views) }
        }
    }

    /**
     * Where a tap goes, by widget kind.
     *
     * Same rule as the Canvas widgets: open the thing the widget is about. A clock opens the alarms
     * screen, a date opens the calendar. Every candidate is checked with `resolveActivity` first,
     * because none of these is guaranteed to exist on every device and a PendingIntent to nothing
     * is worse than no tap target at all.
     */
    private fun destinationFor(context: Context, layoutName: String): Intent {
        // Settings actions are guaranteed by the platform, so they are returned without probing:
        // resolveActivity would return null for them on Android 11+ without a <queries> entry, and
        // every tap would silently fall back to launching the app.
        fun settings(action: String) = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val calendar = Intent(Intent.ACTION_VIEW).setData(
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
        )

        return when {
            layoutName.startsWith("nw_date_card") ||
                layoutName.startsWith("nw_month_year") ||
                layoutName.startsWith("nw_date_wide") ||
                layoutName.startsWith("nw_year_progress") ->
                calendar.takeIf { it.resolveActivity(context.packageManager) != null }
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?: settings(Settings.ACTION_DATE_SETTINGS)

            // Everything else is a clock, including the world clocks and the elapsed timer.
            // Date & time rather than the alarm app, matching what the category leader does and
            // what someone tapping a clock actually wants.
            else -> settings(Settings.ACTION_DATE_SETTINGS)
        }
    }

}
