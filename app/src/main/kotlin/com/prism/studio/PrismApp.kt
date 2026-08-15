package com.prism.studio

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.prism.studio.data.license.LicenseManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Startup does three things and nothing else: wire DI, start the billing connection, and — in
 * debug only — assert that every widget type has a renderer.
 *
 * There is no eager catalog scan, no bitmap pre-warm, and no analytics handshake, because cold
 * start is the first impression a paid app makes.
 */
@HiltAndroidApp
class PrismApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var license: LicenseManager
    @Inject lateinit var typefaces: com.prism.studio.render.TypefaceProvider

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Renderer completeness is asserted where the registry is built (RenderModule), so a
        // missing renderer fails at graph construction rather than one frame later.
        license.start()
        // Fetch the downloadable faces once. Cached device-wide, so this is a one-time cost per
        // device, and it is what lets the widget renderer look fonts up synchronously.
        typefaces.warm(android.os.Handler(android.os.Looper.getMainLooper()))
    }
}
