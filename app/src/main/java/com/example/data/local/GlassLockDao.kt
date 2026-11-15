package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps WHERE isLocked = 1 ORDER BY appName ASC")
    fun observeLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT packageName FROM locked_apps WHERE isLocked = 1")
    suspend fun getLockedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE packageName = :packageName AND isLocked = 1)")
    suspend fun isAppLocked(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: LockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(apps: List<LockedAppEntity>)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun removeApp(packageName: String)
}

@Dao
interface IntruderEventDao {
    @Query("SELECT * FROM intruder_events ORDER BY timestamp DESC")
    fun observeAllEvents(): Flow<List<IntruderEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: IntruderEventEntity): Long

    @Query("DELETE FROM intruder_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM intruder_events")
    suspend fun deleteAll()
}

@Dao
interface HiddenAppDao {
    @Query("SELECT * FROM hidden_apps ORDER BY updatedAt DESC")
    fun observeHiddenApps(): Flow<List<HiddenAppEntity>>

    @Query("SELECT * FROM hidden_apps")
    suspend fun getAllHiddenApps(): List<HiddenAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHiddenApp(entity: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE packageHash = :packageHash")
    suspend fun deleteByHash(packageHash: String)

    @Query("DELETE FROM hidden_apps")
    suspend fun deleteAll()
}
