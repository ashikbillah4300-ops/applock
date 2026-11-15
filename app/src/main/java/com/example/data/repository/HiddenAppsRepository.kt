package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.local.HiddenAppDao
import com.example.data.local.HiddenAppEntity
import com.example.data.local.SettingsDataStore
import com.example.data.security.VaultCryptoHelper
import com.example.data.shizuku.ShizukuManager
import com.example.data.shizuku.ShizukuOpResult
import com.example.data.shizuku.ShizukuStatus
import com.example.domain.model.HiddenAppInfo
import com.example.domain.model.HiddenRehidePolicy
import com.example.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class EncryptedHiddenPayload(
    val packageName: String,
    val appName: String,
    val hiddenAt: Long,
    val isTemporarilyLaunched: Boolean = false,
    val launchExpiresAt: Long = 0L
)

sealed interface HiddenActionResult {
    data class Success(val message: String) : HiddenActionResult
    data class Error(val message: String) : HiddenActionResult
}

@Singleton
class HiddenAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hiddenAppDao: HiddenAppDao,
    private val vaultCryptoHelper: VaultCryptoHelper,
    private val shizukuManager: ShizukuManager,
    private val settingsDataStore: SettingsDataStore,
    private val installedAppsRepository: InstalledAppsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeHiddenApps(): Flow<List<HiddenAppInfo>> {
        return hiddenAppDao.observeHiddenApps().map { entities ->
            entities.mapNotNull { entity ->
                val payload = decryptPayload(entity) ?: return@mapNotNull null
                HiddenAppInfo(
                    packageName = payload.packageName,
                    appName = payload.appName,
                    hiddenAt = payload.hiddenAt,
                    isTemporarilyLaunched = payload.isTemporarilyLaunched,
                    launchExpiresAt = payload.launchExpiresAt,
                    isSystemDisabled = isPackageDisabledOnSystem(payload.packageName)
                )
            }
        }
    }

    fun getProtectedCriticalPackages(): Set<String> {
        val critical = mutableSetOf(
            context.packageName,
            ShizukuManager.SHIZUKU_PACKAGE,
            "android",
            "com.android.settings",
            "com.android.systemui",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.contacts",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.android.providers.settings"
        )
        // Dynamically resolve active default launcher(s) so user never locks themselves out
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val pm = context.packageManager
        val homeActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(homeIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(homeIntent, 0)
        }
        homeActivities.forEach { info ->
            info.activityInfo?.packageName?.let { critical.add(it) }
        }
        return critical
    }

    fun isSafeToHide(packageName: String): Boolean {
        return !getProtectedCriticalPackages().contains(packageName)
    }

    suspend fun hideApps(apps: List<InstalledAppInfo>): HiddenActionResult = withContext(Dispatchers.IO) {
        if (apps.isEmpty()) return@withContext HiddenActionResult.Error("No apps selected.")
        val status = shizukuManager.refreshStatus()
        if (status != ShizukuStatus.READY) {
            return@withContext HiddenActionResult.Error(
                "Shizuku is not ready. Start Shizuku and grant permission first."
            )
        }

        val protected = getProtectedCriticalPackages()
        var hiddenCount = 0
        var lastError: String? = null

        for (app in apps) {
            if (protected.contains(app.packageName)) {
                lastError = "${app.appName} is system-critical and cannot be hidden."
                continue
            }
            when (val res = shizukuManager.hidePackage(app.packageName)) {
                is ShizukuOpResult.Success -> {
                    saveEncryptedHiddenApp(
                        EncryptedHiddenPayload(
                            packageName = app.packageName,
                            appName = app.appName,
                            hiddenAt = System.currentTimeMillis()
                        )
                    )
                    hiddenCount++
                }
                is ShizukuOpResult.Failure -> {
                    lastError = res.reason
                }
            }
        }

        installedAppsRepository.refreshInstalledApps()

        return@withContext if (hiddenCount > 0 && lastError == null) {
            HiddenActionResult.Success("Hidden $hiddenCount app${if (hiddenCount == 1) "" else "s"}.")
        } else if (hiddenCount > 0) {
            HiddenActionResult.Success("Hidden $hiddenCount app(s). Note: $lastError")
        } else {
            HiddenActionResult.Error(lastError ?: "Could not hide selected apps.")
        }
    }

    suspend fun unhideApp(packageName: String): HiddenActionResult = withContext(Dispatchers.IO) {
        when (val res = shizukuManager.unhidePackage(packageName)) {
            is ShizukuOpResult.Success -> {
                hiddenAppDao.deleteByHash(vaultCryptoHelper.hashPackageName(packageName))
                installedAppsRepository.refreshInstalledApps()
                HiddenActionResult.Success("App restored to launcher.")
            }
            is ShizukuOpResult.Failure -> HiddenActionResult.Error(res.reason)
        }
    }

    suspend fun unhideAll(): HiddenActionResult = withContext(Dispatchers.IO) {
        val all = getAllDecrypted()
        if (all.isEmpty()) return@withContext HiddenActionResult.Success("No hidden apps.")
        var restored = 0
        var lastError: String? = null

        for (entry in all) {
            when (val res = shizukuManager.unhidePackage(entry.packageName)) {
                is ShizukuOpResult.Success -> {
                    hiddenAppDao.deleteByHash(vaultCryptoHelper.hashPackageName(entry.packageName))
                    restored++
                }
                is ShizukuOpResult.Failure -> {
                    lastError = res.reason
                }
            }
        }
        installedAppsRepository.refreshInstalledApps()

        return@withContext if (lastError == null) {
            HiddenActionResult.Success("Restored $restored hidden app${if (restored == 1) "" else "s"}.")
        } else {
            HiddenActionResult.Error("Restored $restored app(s), but failed on others: $lastError")
        }
    }

    suspend fun launchTemporarily(packageName: String): Result<Intent> = withContext(Dispatchers.IO) {
        val currentEntry = getAllDecrypted().firstOrNull { it.packageName == packageName }
            ?: return@withContext Result.failure(IllegalStateException("App not found in Hidden Vault."))

        when (val res = shizukuManager.unhidePackage(packageName)) {
            is ShizukuOpResult.Success -> Unit
            is ShizukuOpResult.Failure -> return@withContext Result.failure(IllegalStateException(res.reason))
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return@withContext Result.failure(
            IllegalStateException("No launchable activity found for ${currentEntry.appName}.")
        )

        val settings = settingsDataStore.settingsFlow.first()
        val now = System.currentTimeMillis()
        val expiresAt = when (settings.hiddenRehidePolicy) {
            HiddenRehidePolicy.ON_SCREEN_OFF -> Long.MAX_VALUE
            else -> now + settings.hiddenRehidePolicy.durationMillis
        }

        saveEncryptedHiddenApp(
            currentEntry.copy(
                isTemporarilyLaunched = true,
                launchExpiresAt = expiresAt
            )
        )
        Result.success(launchIntent)
    }

    suspend fun rehideExpiredOrTempApps(forceScreenOff: Boolean) = withContext(Dispatchers.IO) {
        val all = getAllDecrypted()
        if (all.isEmpty()) return@withContext
        val settings = settingsDataStore.settingsFlow.first()
        val now = System.currentTimeMillis()

        for (entry in all) {
            if (!entry.isTemporarilyLaunched) continue
            val shouldRehide = when {
                forceScreenOff && settings.hiddenRehidePolicy == HiddenRehidePolicy.ON_SCREEN_OFF -> true
                forceScreenOff -> true // Also re-hide if screen turns off after launch
                entry.launchExpiresAt in 1..now -> true
                else -> false
            }
            if (shouldRehide && shizukuManager.refreshStatus() == ShizukuStatus.READY) {
                if (shizukuManager.hidePackage(entry.packageName) is ShizukuOpResult.Success) {
                    saveEncryptedHiddenApp(
                        entry.copy(isTemporarilyLaunched = false, launchExpiresAt = 0L)
                    )
                }
            }
        }
    }

    suspend fun reconcileOnStartupOrReboot(isBootOrUpdate: Boolean = false) = withContext(Dispatchers.IO) {
        val all = getAllDecrypted()
        if (all.isEmpty()) return@withContext
        val status = shizukuManager.refreshStatus()
        val now = System.currentTimeMillis()

        for (entry in all) {
            val isDisabled = isPackageDisabledOnSystem(entry.packageName)
            val shouldBeHidden = !entry.isTemporarilyLaunched ||
                isBootOrUpdate ||
                (entry.launchExpiresAt in 1..now)

            if (shouldBeHidden && !isDisabled && status == ShizukuStatus.READY) {
                if (shizukuManager.hidePackage(entry.packageName) is ShizukuOpResult.Success) {
                    saveEncryptedHiddenApp(
                        entry.copy(isTemporarilyLaunched = false, launchExpiresAt = 0L)
                    )
                }
            } else if (!isDisabled && entry.isTemporarilyLaunched && !shouldBeHidden) {
                // Keep temp launched state intact
                continue
            } else if (isDisabled && entry.isTemporarilyLaunched) {
                // Already disabled on system, normalize Room state
                saveEncryptedHiddenApp(
                    entry.copy(isTemporarilyLaunched = false, launchExpiresAt = 0L)
                )
            }
        }
    }

    private fun isPackageDisabledOnSystem(packageName: String): Boolean {
        return runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
            !info.enabled
        }.getOrDefault(false)
    }

    private suspend fun getAllDecrypted(): List<EncryptedHiddenPayload> {
        return hiddenAppDao.getAllHiddenApps().mapNotNull { decryptPayload(it) }
    }

    private suspend fun saveEncryptedHiddenApp(payload: EncryptedHiddenPayload) {
        val hash = vaultCryptoHelper.hashPackageName(payload.packageName)
        val rawJson = json.encodeToString(payload)
        val encrypted = vaultCryptoHelper.encryptString(rawJson)
        hiddenAppDao.upsertHiddenApp(
            HiddenAppEntity(
                packageHash = hash,
                encryptedPayload = encrypted,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun decryptPayload(entity: HiddenAppEntity): EncryptedHiddenPayload? {
        val plaintext = vaultCryptoHelper.decryptString(entity.encryptedPayload) ?: return null
        return runCatching { json.decodeFromString<EncryptedHiddenPayload>(plaintext) }.getOrNull()
    }
}
