package com.prism.studio.data

import com.prism.studio.data.db.FavouriteEntity
import com.prism.studio.data.db.PlacedWidgetEntity
import com.prism.studio.data.db.PrismDatabase
import com.prism.studio.data.db.RecentEntity
import com.prism.studio.data.db.SpecConverters
import com.prism.studio.model.FamilyCatalog
import com.prism.studio.model.ResolvedWidget
import com.prism.studio.model.WidgetSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single door between placed widgets and the rest of the app.
 *
 * Resolution (spec + catalog -> style) happens here so no caller ever has to remember the delta
 * layering rules, and so a future catalog update automatically re-resolves every placed widget.
 */
@Singleton
class WidgetRepository @Inject constructor(
    private val db: PrismDatabase,
    private val catalog: FamilyCatalog,
    private val aura: AuraState,
) {

    /**
     * The current aura, held so resolution stays synchronous.
     *
     * Resolution happens on the widget update path, which must not suspend — the provider has a
     * few milliseconds inside a binder call. Collecting once and caching keeps the aura available
     * without making every caller a coroutine.
     */
    @Volatile
    private var activeAura: AuraState.Aura? = null

    suspend fun observeAura() {
        aura.current.collect { activeAura = it }
    }

    /**
     * Layer order, and it matters: family base, then the variant's delta, then the aura, then the
     * user's own edits last. The user always wins over the aura; the aura always wins over the
     * family's default palette.
     */
    private fun applyAura(spec: WidgetSpec): WidgetSpec {
        val current = activeAura ?: return spec
        val auraDelta = current.asDelta()
        return spec.copy(
            userDelta = spec.userDelta.copy(
                ink = spec.userDelta.ink ?: auraDelta.ink,
                inkMuted = spec.userDelta.inkMuted ?: auraDelta.inkMuted,
                accent = spec.userDelta.accent ?: auraDelta.accent,
            ),
        )
    }
    suspend fun load(appWidgetId: Int): ResolvedWidget? =
        db.widgets().byId(appWidgetId)?.let { applyAura(decode(it)).resolve(catalog) }

    suspend fun loadAll(): Map<Int, ResolvedWidget> =
        db.widgets().all().associate { it.appWidgetId to applyAura(decode(it)).resolve(catalog) }

    fun observeAll(): Flow<Map<Int, ResolvedWidget>> =
        db.widgets().observeAll().map { rows ->
            rows.associate { it.appWidgetId to applyAura(decode(it)).resolve(catalog) }
        }

    suspend fun save(appWidgetId: Int, spec: WidgetSpec) {
        db.widgets().upsert(
            PlacedWidgetEntity(
                appWidgetId = appWidgetId,
                specJson = SpecConverters.json.encodeToString(spec),
                familyId = spec.family.value,
                variantId = spec.variant.value,
                placedAt = System.currentTimeMillis(),
            ),
        )
        touchRecent("${spec.family.value}/${spec.variant.value}")
    }

    /** Called from onDeleted. Leaving rows behind is the classic source of widget-app bloat. */
    suspend fun forget(appWidgetIds: List<Int>) = db.widgets().delete(appWidgetIds)

    fun favourites(): Flow<List<String>> =
        db.library().observeFavourites().map { list -> list.map { it.key } }

    suspend fun setFavourite(key: String, favourite: Boolean) {
        if (favourite) db.library().addFavourite(FavouriteEntity(key, System.currentTimeMillis()))
        else db.library().removeFavourite(key)
    }

    fun recents(): Flow<List<String>> =
        db.library().observeRecents().map { list -> list.map { it.key } }

    suspend fun touchRecent(key: String) =
        db.library().touchRecent(RecentEntity(key, System.currentTimeMillis()))

    private fun decode(entity: PlacedWidgetEntity): WidgetSpec =
        SpecConverters.json.decodeFromString<WidgetSpec>(entity.specJson)
}
