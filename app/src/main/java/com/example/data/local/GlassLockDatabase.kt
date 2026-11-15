package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LockedAppEntity::class,
        IntruderEventEntity::class,
        HiddenAppEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GlassLockDatabase : RoomDatabase() {
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun intruderEventDao(): IntruderEventDao
    abstract fun hiddenAppDao(): HiddenAppDao
}
