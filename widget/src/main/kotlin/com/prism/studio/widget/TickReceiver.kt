package com.prism.studio.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager

/**
 * Minute-accurate clocks without an alarm.
 *
 * ACTION_TIME_TICK cannot be declared in the manifest, which is a feature rather than a
 * restriction: it forces the receiver to be registered only while the app process is alive and
 * the screen is on, so a clock widget costs nothing at all while the phone is in a pocket. When
 * the screen comes back on we redraw immediately, so the user never sees a stale minute.
 */
class TickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON -> PrismWidgetProvider.requestUpdate(context)
        }
    }

    companion object {
        private var registered: TickReceiver? = null

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        @Synchronized
        fun setEnabled(context: Context, enabled: Boolean) {
            val app = context.applicationContext
            if (enabled && registered == null) {
                registered = TickReceiver().also { app.registerReceiver(it, filter) }
            } else if (!enabled && registered != null) {
                runCatching { app.unregisterReceiver(registered) }
                registered = null
            }
            // Boot receiver only matters while widgets exist.
            app.packageManager.setComponentEnabledSetting(
                ComponentName(app, BootReceiver::class.java),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}

/** Redraws once after boot, then hands over to the tick receiver. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            PrismWidgetProvider.requestUpdate(context)
        }
    }
}
