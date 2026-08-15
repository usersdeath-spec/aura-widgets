package com.prism.studio.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prism.studio.model.RefreshCadence
import com.prism.studio.model.WidgetType
import java.util.concurrent.TimeUnit

/**
 * Battery policy, in one file.
 *
 * The rule that matters: **never schedule work for a cadence nobody has placed.** A user with only
 * clock and battery widgets triggers zero periodic work and zero network access for the life of the
 * install — clocks ride the system's existing minute tick, and battery is event-driven.
 *
 * Concretely:
 *   - [RefreshCadence.Minute]  -> ACTION_TIME_TICK, registered at runtime and only while the screen
 *                                is on. The system already sends it; we add nothing.
 *   - [RefreshCadence.Event]   -> ACTION_BATTERY_CHANGED, note edits, media session callbacks.
 *   - [RefreshCadence.Quarter] -> one coalesced periodic worker for *all* such widgets, network
 *                                constrained, backing off rather than retrying tightly.
 *   - [RefreshCadence.Daily]   -> one exact-ish alarm at local midnight, set by DayRolloverWorker.
 *
 * One worker for all quarter-hour widgets, not one per widget: fifteen placed widgets still means
 * four wakeups an hour.
 */
object UpdateScheduler {

    private const val QUARTER_WORK = "prism.refresh.quarter"
    private const val DAILY_WORK = "prism.refresh.daily"

    fun reschedule(context: Context, placedTypes: List<WidgetType>) {
        val cadences = placedTypes.map { it.refresh }.toSet()
        val wm = WorkManager.getInstance(context)

        if (RefreshCadence.Quarter in cadences) {
            val needsNetwork = placedTypes.any { !it.isLocalOnly }
            wm.enqueueUniquePeriodicWork(
                QUARTER_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(if (needsNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    )
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .build(),
            )
        } else {
            wm.cancelUniqueWork(QUARTER_WORK)
        }

        if (RefreshCadence.Daily in cadences) {
            wm.enqueueUniquePeriodicWork(
                DAILY_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DayRolloverWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(millisUntilMidnight(), TimeUnit.MILLISECONDS)
                    .build(),
            )
        } else {
            wm.cancelUniqueWork(DAILY_WORK)
        }

        TickReceiver.setEnabled(context, RefreshCadence.Minute in cadences)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(QUARTER_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK)
        TickReceiver.setEnabled(context, false)
    }

    private fun millisUntilMidnight(): Long {
        val now = java.time.LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        return java.time.Duration.between(now, midnight).toMillis().coerceAtLeast(60_000)
    }
}
