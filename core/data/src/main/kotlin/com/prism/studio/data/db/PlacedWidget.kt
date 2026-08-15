package com.prism.studio.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.prism.studio.model.WidgetSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One row per widget actually on the user's home screen, keyed by the host's appWidgetId.
 *
 * The spec is stored as JSON rather than as columns because [WidgetSpec] is a versioned document,
 * not a relational entity: new customisation fields must deserialise into old rows with defaults.
 * Room migrations then only ever concern the envelope, never the design schema.
 */
@Entity(tableName = "placed_widgets")
data class PlacedWidgetEntity(
    @PrimaryKey val appWidgetId: Int,
    @ColumnInfo(name = "spec_json") val specJson: String,
    @ColumnInfo(name = "family_id") val familyId: String,
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "placed_at") val placedAt: Long,
    @ColumnInfo(name = "last_rendered_at") val lastRenderedAt: Long = 0L,
)

/** Favourites and recents drive the two fastest paths into the catalog, so they get real tables. */
@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val key: String,          // "familyId/variantId" or "wallpaper/<id>"
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "used_at") val usedAt: Long,
)

@Dao
interface WidgetDao {
    @Query("SELECT * FROM placed_widgets WHERE appWidgetId = :id")
    suspend fun byId(id: Int): PlacedWidgetEntity?

    @Query("SELECT * FROM placed_widgets")
    suspend fun all(): List<PlacedWidgetEntity>

    @Query("SELECT * FROM placed_widgets")
    fun observeAll(): Flow<List<PlacedWidgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlacedWidgetEntity)

    @Query("DELETE FROM placed_widgets WHERE appWidgetId IN (:ids)")
    suspend fun delete(ids: List<Int>)

    @Query("UPDATE placed_widgets SET last_rendered_at = :at WHERE appWidgetId = :id")
    suspend fun markRendered(id: Int, at: Long)
}

/**
 * A look the user saved. Stored as a delta, so a preset created on one family applies cleanly to
 * any other — the same property that makes built-in presets worth shipping.
 */
@Entity(tableName = "user_presets")
data class UserPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "delta_json") val deltaJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface PresetDao {
    @Query("SELECT * FROM user_presets ORDER BY created_at DESC")
    fun observeAll(): Flow<List<UserPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserPresetEntity)

    @Query("DELETE FROM user_presets WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM favourites ORDER BY added_at DESC")
    fun observeFavourites(): Flow<List<FavouriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavourite(entity: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE key = :key")
    suspend fun removeFavourite(key: String)

    @Query("SELECT * FROM recents ORDER BY used_at DESC LIMIT 40")
    fun observeRecents(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun touchRecent(entity: RecentEntity)
}

class SpecConverters {
    @TypeConverter fun toJson(spec: WidgetSpec): String = json.encodeToString(spec)
    @TypeConverter fun fromJson(value: String): WidgetSpec = json.decodeFromString<WidgetSpec>(value)

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

@Database(
    entities = [
        PlacedWidgetEntity::class,
        FavouriteEntity::class,
        RecentEntity::class,
        UserPresetEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(SpecConverters::class)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun widgets(): WidgetDao
    abstract fun library(): LibraryDao
    abstract fun presets(): PresetDao
}
