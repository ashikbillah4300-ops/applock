package com.example.data.repository

import android.content.Context
import com.example.data.local.IntruderEventDao
import com.example.data.local.IntruderEventEntity
import com.example.domain.model.IntruderAttempt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntruderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intruderEventDao: IntruderEventDao
) {
    fun observeIntruderAttempts(): Flow<List<IntruderAttempt>> {
        return intruderEventDao.observeAllEvents().map { list ->
            list.map { entity ->
                IntruderAttempt(
                    id = entity.id,
                    packageName = entity.packageName,
                    appName = entity.appName,
                    photoPath = entity.photoPath,
                    attemptCount = entity.attemptCount,
                    timestamp = entity.timestamp,
                    authMethodUsed = entity.authMethodUsed
                )
            }
        }
    }

    fun createIntruderPhotoFile(): File {
        val dir = File(context.filesDir, "intruders")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "intruder_${System.currentTimeMillis()}.jpg")
    }

    suspend fun logIntruderAttempt(
        packageName: String,
        appName: String,
        photoPath: String,
        attemptCount: Int,
        authMethodUsed: String
    ) {
        intruderEventDao.insertEvent(
            IntruderEventEntity(
                packageName = packageName,
                appName = appName,
                photoPath = photoPath,
                attemptCount = attemptCount,
                authMethodUsed = authMethodUsed
            )
        )
    }

    suspend fun deleteAttempt(attempt: IntruderAttempt) = withContext(Dispatchers.IO) {
        intruderEventDao.deleteById(attempt.id)
        if (attempt.photoPath.isNotBlank()) {
            runCatching { File(attempt.photoPath).delete() }
        }
    }

    suspend fun clearAllAttempts() = withContext(Dispatchers.IO) {
        intruderEventDao.deleteAll()
        val dir = File(context.filesDir, "intruders")
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }
}
