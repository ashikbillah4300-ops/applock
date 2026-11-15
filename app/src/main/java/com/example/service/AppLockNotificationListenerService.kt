package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.GlassLockApp
import com.example.MainActivity
import com.example.R
import com.example.data.local.SettingsDataStore
import com.example.data.repository.AppLockRepository
import com.example.data.repository.InstalledAppsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppLockNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var appLockRepository: AppLockRepository
    @Inject lateinit var installedAppsRepository: InstalledAppsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return

        if (pkg == packageName || notification.isOngoing) return

        scope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            if (!settings.isServiceEnabled || !settings.hideNotificationContent) return@launch
            if (!appLockRepository.isPackageLockedInDb(pkg)) return@launch

            // Suppress original notification preview and show masked GlassLock placeholder
            runCatching { cancelNotification(notification.key) }
            postMaskedNotification(pkg)
        }
    }

    private fun postMaskedNotification(lockedPackage: String) {
        val appName = installedAppsRepository.getAppName(lockedPackage)
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            lockedPackage.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val masked = NotificationCompat.Builder(this, GlassLockApp.CHANNEL_PRIVACY_MASKED)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(appName)
            .setContentText("Notification content hidden by GlassLock")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(MASKED_ID_BASE + (lockedPackage.hashCode() and 0x0FFF), masked)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val MASKED_ID_BASE = 20_000
    }
}
