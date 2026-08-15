package com.prism.studio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.prism.studio.data.WidgetRepository
import com.prism.studio.render.ColorResolver
import com.prism.studio.render.Density
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.RenderSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The home-screen host for every Prism widget.
 *
 * There is exactly one provider class rather than one per design, because the host cares only
 * about size and update policy — the design is data. A second provider exists only for the
 * keyguard/small-cell variants; see [PrismCompactProvider].
 *
 * The RemoteViews we hand back is deliberately trivial: a FrameLayout containing one ImageView
 * (the rasterised widget) plus up to four transparent click targets. Everything expressive happens
 * in :core:render, which means the binder payload is a bitmap and a handful of PendingIntents.
 */
@AndroidEntryPoint
open class PrismWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var repository: WidgetRepository
    @Inject lateinit var renderer: PrismRenderer
    @Inject lateinit var dataSource: WidgetDataSource
    @Inject lateinit var catalog: com.prism.studio.model.FamilyCatalog

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // A BroadcastReceiver lives only as long as onReceive. Launching a coroutine and returning
        // lets Android tear the process down before the bitmap exists, leaving the widget showing
        // an empty initialLayout — which is exactly the black rectangle seen on device.
        //
        // goAsync() holds the receiver open until finish(). Its ~10s budget is ample for work that
        // has to fit in a frame anyway.
        val pending = goAsync()
        scope.launch {
            try {
                renderAll(context, manager, ids.toList())
            } catch (t: Throwable) {
                android.util.Log.e("PrismWidgetProvider", "render batch failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // Resizing changes the pixel budget, so the cached bitmap is no longer valid.
        val pending = goAsync()
        scope.launch {
            try {
                renderAll(context, manager, listOf(appWidgetId))
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                repository.forget(appWidgetIds.toList())
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        // Last widget removed: stop every alarm and worker this app owns.
        UpdateScheduler.cancelAll(context)
    }

    private suspend fun renderAll(context: Context, manager: AppWidgetManager, ids: List<Int>) {
        val widgets = repository.loadAll()
        val colors = ColorResolver.forDevice(context, WallpaperPalette.current(context))

        ids.forEach { id ->
            // Added from the system picker rather than from inside the app, so nothing was saved
            // against this id. Fall back to the first Foundation design instead of drawing an empty
            // rectangle: an unconfigured widget must still look like a widget, and the user can
            // change it from the editor afterwards.
            val resolved = widgets[id] ?: defaultWidget()
            val options = manager.getAppWidgetOptions(id)
            val size = measure(context, options)
            val data = dataSource.dataFor(resolved, context)

            val bitmap = renderer.render(
                widget = resolved,
                data = data.value,
                size = size,
                colors = colors,
                dataFingerprint = data.fingerprint,
            )

            val views = RemoteViews(context.packageName, R.layout.widget_canvas).apply {
                setImageViewBitmap(R.id.canvas, bitmap)
                setContentDescription(R.id.canvas, Accessibility.describe(resolved, data.value, context))
                setOnClickPendingIntent(R.id.canvas, launchIntent(context, resolved, id))
            }
            manager.updateAppWidget(id, views)
        }

        UpdateScheduler.reschedule(context, widgets.values.map { it.variant.type })
    }

    /**
     * Widget hosts report their size in dp and lie about it more often than you would like, so we
     * clamp to something sane before allocating a bitmap. A 4x4 widget at 3x density is roughly
     * 1080x1080 px / 4.4 MB, comfortably under the binder transaction ceiling; anything larger is
     * capped rather than risking a TransactionTooLargeException on a budget device.
     */
    private fun measure(context: Context, options: Bundle): RenderSize {
        val dm = context.resources.displayMetrics
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160).coerceIn(40, 640)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160).coerceIn(40, 640)
        val scale = dm.density.coerceAtMost(3f)
        return RenderSize(
            widthPx = (widthDp * scale).toInt(),
            heightPx = (heightDp * scale).toInt(),
            density = Density(scale),
        )
    }

    private fun launchIntent(
        context: Context,
        resolved: com.prism.studio.model.ResolvedWidget,
        appWidgetId: Int,
    ): PendingIntent {
        // Tapping a widget opens what it is about — the calendar app for a calendar widget, the
        // Prism editor for decorative ones. Deep links are resolved by WidgetTapRouter.
        val intent = WidgetTapRouter.intentFor(context, resolved, appWidgetId)
        return PendingIntent.getActivity(
            context, appWidgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * What an unconfigured widget shows.
     *
     * Deliberately a real design rather than a placeholder: the widget the user just dropped on
     * their home screen is the first thing they see of this app, and "tap to configure" on a blank
     * card is how a paid app gets refunded.
     */
    private fun defaultWidget(): com.prism.studio.model.ResolvedWidget {
        val family = catalog.families.first()
        val variant = family.variants.first()
        return com.prism.studio.model.ResolvedWidget(
            family = family,
            variant = variant,
            style = variant.styleDelta.applyTo(family.base),
            options = emptyMap(),
        )
    }

    companion object {
        /** Asks the host to redraw specific widgets. Safe to call from anywhere. */
        fun requestUpdate(context: Context, ids: IntArray? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PrismWidgetProvider::class.java)
            val targets = ids ?: manager.getAppWidgetIds(component)
            if (targets.isEmpty()) return
            context.sendBroadcast(
                Intent(context, PrismWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, targets)
                },
            )
        }
    }
}

/**
 * One provider per footprint.
 *
 * Android's widget picker lists one row per declared provider, so two providers meant the picker
 * showed exactly two entries regardless of how many designs exist. Five footprints give the user
 * the right default cell size when they add from the picker, and give [PinnedWidgets] something to
 * bind when they add from inside the app. The design is carried in Room against the assigned id, so
 * this stays at five rather than growing with the catalog.
 */
@AndroidEntryPoint
class PrismCompactProvider : PrismWidgetProvider()

@AndroidEntryPoint
class PrismTallProvider : PrismWidgetProvider()

@AndroidEntryPoint
class PrismLargeProvider : PrismWidgetProvider()

@AndroidEntryPoint
class PrismBannerProvider : PrismWidgetProvider()
