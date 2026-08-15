package com.prism.studio.widget

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import com.prism.studio.render.WidgetData

/**
 * Supplies the Now Playing widget.
 *
 * Android exposes active media sessions only to an enabled notification listener. That is a heavy
 * grant to ask for, so three things are true here by design:
 *
 * 1. The service is declared but does nothing until the user enables it in Settings. Every other
 *    widget works regardless.
 * 2. It reads **only** the media session — never notification content. `onNotificationPosted` is
 *    deliberately not overridden, so the app never sees a single notification's text.
 * 3. Nothing is stored or transmitted. The current track is held in memory, handed to the renderer,
 *    and replaced by the next one.
 */
class MediaSessionListener : NotificationListenerService() {

    private val sessionManager by lazy { getSystemService(MediaSessionManager::class.java) }
    private val component by lazy { ComponentName(this, MediaSessionListener::class.java) }

    private val callback = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        publish(controllers.orEmpty())
    }

    override fun onListenerConnected() {
        runCatching {
            sessionManager.addOnActiveSessionsChangedListener(callback, component)
            publish(sessionManager.getActiveSessions(component))
        }
    }

    override fun onListenerDisconnected() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(callback) }
        LocalContent.publish(LocalContent.Content())
    }

    /** Picks the session that is actually playing, falling back to the most recently active one. */
    private fun publish(controllers: List<MediaController>) {
        val controller = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()

        val media = controller?.metadata?.let { metadata ->
            val duration = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
            val position = controller.playbackState?.position ?: 0L
            WidgetData.Media(
                title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                    .ifBlank { "Unknown track" },
                artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST),
                playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
                artworkKey = null,   // art comes from the session bitmap, cached by WidgetBitmapSource
                progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else null,
            )
        }

        LocalContent.publish(LocalContent.Content(media = media))
        PrismWidgetProvider.requestUpdate(applicationContext)
    }
}
