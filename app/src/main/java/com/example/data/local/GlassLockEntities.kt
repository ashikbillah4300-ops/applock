package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isLocked: Boolean = true,
    val isSystemCritical: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "intruder_events")
data class IntruderEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val photoPath: String,
    val attemptCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val authMethodUsed: String
)

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val packageHash: String,
    val encryptedPayload: String,
    val updatedAt: Long = System.currentTimeMillis()
)
