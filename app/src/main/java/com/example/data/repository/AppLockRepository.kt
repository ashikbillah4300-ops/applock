package com.example.data.repository

import com.example.data.local.LockedAppDao
import com.example.data.local.LockedAppEntity
import com.example.data.local.SettingsDataStore
import com.example.domain.model.RelockPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockRepository @Inject constructor(
    private val lockedAppDao: LockedAppDao,
    private val settingsDataStore: SettingsDataStore
) {
    // Maps packageName -> lastActiveTimestampMillis while temporarily unlocked
    private val temporaryUnlockMap = ConcurrentHashMap<String, Long>()

    @Volatile
    private var lastForegroundPackage: String? = null

    fun observeLockedApps(): Flow<List<LockedAppEntity>> = lockedAppDao.observeLockedApps()

    suspend fun setAppLocked(
        packageName: String,
        appName: String,
        isLocked: Boolean,
        isSystemCritical: Boolean
    ) {
        if (isLocked) {
            lockedAppDao.upsertApp(
                LockedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    isLocked = true,
                    isSystemCritical = isSystemCritical
                )
            )
            temporaryUnlockMap.remove(packageName)
        } else {
            lockedAppDao.removeApp(packageName)
            temporaryUnlockMap.remove(packageName)
        }
    }

    suspend fun isPackageLockedInDb(packageName: String): Boolean =
        lockedAppDao.isAppLocked(packageName)

    suspend fun shouldLockPackage(packageName: String): Boolean {
        val settings = settingsDataStore.settingsFlow.first()
        if (!settings.isServiceEnabled) return false
        if (!lockedAppDao.isAppLocked(packageName)) return false

        val now = System.currentTimeMillis()
        val lastActiveAt = temporaryUnlockMap[packageName]

        // Handle transitions away from previously unlocked apps
        val previous = lastForegroundPackage
        if (previous != null && previous != packageName) {
            when (settings.relockPolicy) {
                RelockPolicy.IMMEDIATE -> temporaryUnlockMap.remove(previous)
                RelockPolicy.ONE_MINUTE -> {
                    // Stamp the exact time the user left the previous unlocked app
                    if (temporaryUnlockMap.containsKey(previous)) {
                        temporaryUnlockMap[previous] = now
                    }
                }
                RelockPolicy.ON_SCREEN_OFF -> Unit
            }
        }
        lastForegroundPackage = packageName

        if (lastActiveAt == null) {
            return true
        }

        return when (settings.relockPolicy) {
            RelockPolicy.IMMEDIATE -> false // Still inside the unlocked app
            RelockPolicy.ONE_MINUTE -> {
                val expired = (now - lastActiveAt) > ONE_MINUTE_MS
                if (expired) {
                    temporaryUnlockMap.remove(packageName)
                    true
                } else {
                    temporaryUnlockMap[packageName] = now
                    false
                }
            }
            RelockPolicy.ON_SCREEN_OFF -> false
        }
    }

    fun markTemporarilyUnlocked(packageName: String) {
        val now = System.currentTimeMillis()
        temporaryUnlockMap[packageName] = now
        lastForegroundPackage = packageName
    }

    fun onForegroundLeftToUnmonitoredApp(currentPkg: String) {
        val previous = lastForegroundPackage
        if (previous != null && previous != currentPkg) {
            lastForegroundPackage = currentPkg
        }
    }

    suspend fun applyBackgroundTransition(currentPkg: String) {
        val settings = settingsDataStore.settingsFlow.first()
        val previous = lastForegroundPackage
        if (previous != null && previous != currentPkg) {
            when (settings.relockPolicy) {
                RelockPolicy.IMMEDIATE -> temporaryUnlockMap.remove(previous)
                RelockPolicy.ONE_MINUTE -> {
                    if (temporaryUnlockMap.containsKey(previous)) {
                        temporaryUnlockMap[previous] = System.currentTimeMillis()
                    }
                }
                RelockPolicy.ON_SCREEN_OFF -> Unit
            }
        }
        lastForegroundPackage = currentPkg
    }

    fun onScreenTurnedOff() {
        temporaryUnlockMap.clear()
        lastForegroundPackage = null
    }

    companion object {
        private const val ONE_MINUTE_MS = 60_000L
    }
}
