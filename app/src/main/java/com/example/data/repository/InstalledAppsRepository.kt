package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.data.local.LockedAppDao
import com.example.data.local.LockedAppEntity
import com.example.data.local.SettingsDataStore
import com.example.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lockedAppDao: LockedAppDao,
    private val settingsDataStore: SettingsDataStore
) {
    private val rawInstalledApps = MutableStateFlow<List<RawAppEntry>>(emptyList())
    private val iconCache = ConcurrentHashMap<String, Drawable>()
    private val failedIcons = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private data class RawAppEntry(
        val packageName: String,
        val appName: String,
        val isSystemApp: Boolean,
        val isSystemCritical: Boolean
    )

    fun observeApps(): Flow<List<InstalledAppInfo>> {
        return combine(rawInstalledApps, lockedAppDao.observeLockedApps()) { installed, locked ->
            val lockedSet = locked.map { it.packageName }.toSet()
            installed.map { entry ->
                InstalledAppInfo(
                    packageName = entry.packageName,
                    appName = entry.appName,
                    isLocked = lockedSet.contains(entry.packageName),
                    isSystemApp = entry.isSystemApp,
                    isSystemCritical = entry.isSystemCritical,
                    icon = iconCache[entry.packageName]
                )
            }
        }
    }

    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_SHARED_LIBRARY_FILES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, PackageManager.GET_SHARED_LIBRARY_FILES)
        }

        val entriesMap = linkedMapOf<String, RawAppEntry>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
            if (entriesMap.containsKey(pkg)) continue

            val appInfo = info.activityInfo.applicationInfo
            val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
                ?: appInfo.loadLabel(pm).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isCritical = CRITICAL_PACKAGES.contains(pkg)
            entriesMap[pkg] = RawAppEntry(
                packageName = pkg,
                appName = label,
                isSystemApp = isSystem,
                isSystemCritical = isCritical
            )
        }

        // Ensure critical protection targets (Settings & Package Installer) appear even if hidden from launcher
        for (criticalPkg in CRITICAL_PACKAGES) {
            if (entriesMap.containsKey(criticalPkg)) continue
            val appInfo = getAppInfoWithSharedLibs(pm, criticalPkg) ?: continue

            val label = appInfo.loadLabel(pm).toString().ifBlank { criticalPkg }
            entriesMap[criticalPkg] = RawAppEntry(
                packageName = criticalPkg,
                appName = label,
                isSystemApp = true,
                isSystemCritical = true
            )
        }

        val sorted = entriesMap.values.sortedWith(
            compareByDescending<RawAppEntry> { it.isSystemCritical }
                .thenBy { it.appName.lowercase() }
        )
        rawInstalledApps.value = sorted
    }

    suspend fun seedDefaultCriticalLocksIfNeeded() = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settingsFlow.first()
        if (settings.criticalAppsSeeded) return@withContext

        if (rawInstalledApps.value.isEmpty()) {
            refreshInstalledApps()
        }

        val criticalEntities = rawInstalledApps.value
            .filter { it.isSystemCritical }
            .map {
                LockedAppEntity(
                    packageName = it.packageName,
                    appName = it.appName,
                    isLocked = true,
                    isSystemCritical = true
                )
            }

        if (criticalEntities.isNotEmpty()) {
            lockedAppDao.insertAllIgnore(criticalEntities)
        }
        settingsDataStore.markCriticalAppsSeeded()
    }

    fun getAppName(packageName: String): String {
        rawInstalledApps.value.firstOrNull { it.packageName == packageName }?.let {
            return it.appName
        }
        val pm = context.packageManager
        return getAppInfoWithSharedLibs(pm, packageName)
            ?.loadLabel(pm)
            ?.toString()
            ?: packageName
    }

    fun getAppIcon(packageName: String): Drawable? {
        iconCache[packageName]?.let { return it }
        if (failedIcons.contains(packageName)) return null
        val pm = context.packageManager
        val loaded = runCatching {
            val appInfo = getAppInfoWithSharedLibs(pm, packageName) ?: return@runCatching null
            if (appInfo.icon == 0) {
                return@runCatching pm.defaultActivityIcon
            }
            // Prefer package context so shared libraries and split resources are mounted
            val pkgDrawable = runCatching {
                val pkgContext = context.createPackageContext(
                    packageName,
                    Context.CONTEXT_IGNORE_SECURITY
                )
                pkgContext.resources.getDrawable(appInfo.icon, pkgContext.theme)
            }.getOrNull()
            pkgDrawable ?: pm.getApplicationIcon(appInfo)
        }.getOrNull()

        if (loaded != null) {
            iconCache[packageName] = loaded
        } else {
            failedIcons.add(packageName)
        }
        return loaded
    }

    private fun getAppInfoWithSharedLibs(
        pm: PackageManager,
        packageName: String
    ): ApplicationInfo? {
        val flags = PackageManager.GET_SHARED_LIBRARY_FILES or PackageManager.MATCH_DISABLED_COMPONENTS
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, flags)
            }
        }.getOrNull()
    }

    companion object {
        val CRITICAL_PACKAGES = setOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.oplus.packageinstaller"
        )
    }
}
