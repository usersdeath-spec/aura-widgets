package com.prism.studio

import android.content.Context
import androidx.room.Room
import com.prism.studio.data.catalog.PrismCatalog
import com.prism.studio.data.db.PrismDatabase
import com.prism.studio.model.FamilyCatalog
import com.prism.studio.render.ContentRenderer
import com.prism.studio.render.ContentRendererRegistry
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.TypefaceProvider
import com.prism.studio.WidgetBitmapSource
import com.prism.studio.render.content.AgendaRenderer
import com.prism.studio.render.content.AnalogClockRenderer
import com.prism.studio.render.content.BitmapSource
import com.prism.studio.render.content.CountdownRenderer
import com.prism.studio.render.content.DayCardRenderer
import com.prism.studio.render.content.DigitalClockRenderer
import com.prism.studio.render.content.GaugeRenderer
import com.prism.studio.render.content.HabitTrackerRenderer
import com.prism.studio.render.content.MonthCalendarRenderer
import com.prism.studio.render.content.MusicPlayerRenderer
import com.prism.studio.render.content.NotesRenderer
import com.prism.studio.render.content.PhotoRenderer
import com.prism.studio.render.content.QuoteRenderer
import com.prism.studio.render.content.SeriesRenderer
import com.prism.studio.render.content.SunriseSunsetRenderer
import com.prism.studio.render.content.SystemInfoRenderer
import com.prism.studio.render.content.TodoRenderer
import com.prism.studio.render.content.WeatherRenderer
import com.prism.studio.render.content.WorldClockRenderer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The composition root.
 *
 * Renderers are registered here — one list, one place to look when a widget type is missing. This
 * is also where the renderer's memory budget is set: 8 MB holds roughly forty 4x2 widget bitmaps
 * at 3x density, which comfortably covers a heavily-decorated home screen plus a scrolling catalog.
 */
@Module
@InstallIn(SingletonComponent::class)
object RenderModule {

    @Provides
    @Singleton
    fun renderers(bitmaps: BitmapSource): ContentRendererRegistry = ContentRendererRegistry(
        buildList {
            // Time
            add(DigitalClockRenderer())
            add(AnalogClockRenderer())
            add(WorldClockRenderer())
            add(CountdownRenderer())

            // Calendar
            add(DayCardRenderer())
            add(MonthCalendarRenderer())
            add(AgendaRenderer())

            // Environment
            add(WeatherRenderer())
            add(SunriseSunsetRenderer())

            // Device — Battery, Cpu, Ram, Storage, Network, Steps share one renderer, because
            // they are all "a fraction, a number, and a label".
            addAll(GaugeRenderer.all())
            add(SystemInfoRenderer())

            // Text
            add(NotesRenderer())
            add(TodoRenderer())
            add(HabitTrackerRenderer())
            add(QuoteRenderer())

            // Media — the two renderers that need bitmaps they do not own.
            add(MusicPlayerRenderer(bitmaps))
            add(PhotoRenderer(bitmaps))

            // Series — Finance, Crypto, Health.
            addAll(SeriesRenderer.all())
        },
    ).also {
        // All 25 WidgetTypes are covered as of 1.0. This assertion is what makes that a fact
        // rather than a claim: adding a type without a renderer fails the next debug launch.
        it.assertComplete()
    }

    /**
     * Album art and user photos, decoded and cached outside the renderer.
     *
     * Bounded at 6 MB and sampled down to the largest widget size we can be asked to draw. An
     * unbounded bitmap cache in a process that also holds the render cache is how a widget app ends
     * up being killed in the background and losing its widgets.
     */
    @Provides
    @Singleton
    fun bitmapSource(@ApplicationContext context: Context): BitmapSource = WidgetBitmapSource(context)

    @Provides
    @Singleton
    fun typefaces(@ApplicationContext context: Context): TypefaceProvider = TypefaceProvider(context)

    @Provides
    @Singleton
    fun renderer(registry: ContentRendererRegistry, typefaces: TypefaceProvider): PrismRenderer =
        PrismRenderer(
        registry,
        typefaces,
        // Raised from 8 MB. The catalog scrolls through 708 designs; at 8 MB the cache thrashed,
        // re-rasterising constantly and holding many in flight at once. 24 MB holds a comfortable
        // scroll window of capped-size previews and is still a fraction of a normal heap.
        cacheBytes = 24 * 1024 * 1024,
    )

    @Provides
    @Singleton
    fun catalog(impl: PrismCatalog): FamilyCatalog = impl

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PrismDatabase =
        Room.databaseBuilder(context, PrismDatabase::class.java, "prism.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
}
