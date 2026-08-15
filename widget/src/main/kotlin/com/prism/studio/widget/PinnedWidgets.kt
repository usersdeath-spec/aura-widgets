package com.prism.studio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.prism.studio.model.WidgetSize
import com.prism.studio.model.WidgetSpec
import kotlinx.serialization.json.Json
import java.io.File

/**
 * "One tap to add" — placing a widget from inside the app.
 *
 * This was the single missing feature that made the app unusable. Tapping a widget in the catalog
 * did nothing, and the system picker only ever listed two entries, because Android's widget picker
 * shows one row *per declared provider* — not one per design. With 708 designs and two providers,
 * there was no path from "I like this one" to "it is on my home screen".
 *
 * `requestPinAppWidget` (API 26+, which is our minSdk) is the answer, and it is the same API behind
 * every competitor's "1 TAP TO ADD WIDGET" claim. The flow:
 *
 *   1. The user taps a design. We stash its [WidgetSpec] against a one-time token.
 *   2. We ask the launcher to pin the provider matching that design's *size*.
 *   3. The launcher assigns an appWidgetId and fires our callback.
 *   4. [PinResultReceiver] pairs the id with the stashed spec, saves it, and draws.
 *
 * Step 1 exists because the pin callback carries only an id — there is nowhere to put the design,
 * so it has to be waiting on the other side.
 */
object PinnedWidgets {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    const val EXTRA_TOKEN = "com.prism.studio.PIN_TOKEN"
    const val ACTION_PINNED = "com.prism.studio.WIDGET_PINNED"

    /**
     * True when the launcher supports pinning. Most do; a few older third-party launchers do not,
     * and the UI needs to say so rather than appear to do nothing.
     */
    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /**
     * Asks the launcher to place this design.
     *
     * @return false when pinning is unsupported, so the caller can fall back to telling the user to
     *   long-press the home screen instead of silently doing nothing — which is what happened before.
     */
    fun requestPin(context: Context, spec: WidgetSpec, size: WidgetSize): Boolean {
        if (!isSupported(context)) return false

        val token = System.nanoTime().toString()
        stash(context, token, spec)

        val callback = PendingIntent.getBroadcast(
            context,
            token.hashCode(),
            Intent(context, PinResultReceiver::class.java).apply {
                action = ACTION_PINNED
                putExtra(EXTRA_TOKEN, token)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        // A preview in the pin dialog would be better, but RemoteViews previews are API 31+ and the
        // dialog is transient. The provider's own preview layout is used instead.
        return AppWidgetManager.getInstance(context).requestPinAppWidget(
            providerFor(context, size),
            Bundle(),
            callback,
        )
    }

    /**
     * Which provider to pin for a given footprint.
     *
     * One provider per size rather than one per design: the launcher needs a component to bind, and
     * the size determines the default cell footprint the user gets. The design itself is carried
     * separately, which is why 708 designs need five providers rather than 708.
     */
    fun providerFor(context: Context, size: WidgetSize): ComponentName {
        val cls = when (size) {
            WidgetSize.Small -> PrismCompactProvider::class.java
            WidgetSize.Wide -> PrismWidgetProvider::class.java
            WidgetSize.Tall -> PrismTallProvider::class.java
            WidgetSize.Large -> PrismLargeProvider::class.java
            WidgetSize.Banner -> PrismBannerProvider::class.java
        }
        return ComponentName(context, cls)
    }

    // ---- Pending spec storage ------------------------------------------------------------------
    // A file rather than an in-memory map: the launcher may kill and restart our process between the
    // request and the callback, and an in-memory stash loses the design exactly when it is needed.

    private fun stash(context: Context, token: String, spec: WidgetSpec) {
        pendingDir(context).mkdirs()
        File(pendingDir(context), token).writeText(json.encodeToString(WidgetSpec.serializer(), spec))
        prune(context)
    }

    internal fun take(context: Context, token: String): WidgetSpec? {
        val file = File(pendingDir(context), token)
        if (!file.exists()) return null
        val spec = runCatching { json.decodeFromString(WidgetSpec.serializer(), file.readText()) }.getOrNull()
        file.delete()
        return spec
    }

    /** A pin the user cancelled leaves a file behind. Anything older than an hour is abandoned. */
    private fun prune(context: Context) {
        val cutoff = System.currentTimeMillis() - 3_600_000
        pendingDir(context).listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    private fun pendingDir(context: Context) = File(context.filesDir, "pending_pins")
}

/**
 * Receives the launcher's confirmation that a widget was placed, and pairs the new id with the
 * design the user chose.
 */
class PinResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PinnedWidgets.ACTION_PINNED) return

        val token = intent.getStringExtra(PinnedWidgets.EXTRA_TOKEN) ?: return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val spec = PinnedWidgets.take(context, token) ?: return

        // goAsync: writing to Room and drawing the first frame both take longer than a receiver's
        // synchronous window, and dropping either leaves a blank widget on the user's home screen.
        val result = goAsync()
        PendingWidgetWriter.write(context, appWidgetId, spec) {
            PrismWidgetProvider.requestUpdate(context, intArrayOf(appWidgetId))
            result.finish()
        }
    }
}

/** Supplied by :app, which owns the repository. Keeps :widget free of a Room dependency here. */
object PendingWidgetWriter {
    @Volatile
    var writer: ((Context, Int, WidgetSpec, () -> Unit) -> Unit)? = null

    fun write(context: Context, appWidgetId: Int, spec: WidgetSpec, done: () -> Unit) {
        val w = writer
        if (w == null) done() else w(context, appWidgetId, spec, done)
    }
}
