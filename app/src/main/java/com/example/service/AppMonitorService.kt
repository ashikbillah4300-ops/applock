package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.GlassLockApp
import com.example.MainActivity
import com.example.R
import com.example.data.local.SettingsDataStore
import com.example.data.permissions.PermissionsHelper
import com.example.data.repository.AppLockRepository
import com.example.data.repository.HiddenAppsRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.data.security.PinSecurityManager
import com.example.ui.lock.LockScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitorService : Service() {

    @Inject lateinit var appLockRepository: AppLockRepository
    @Inject lateinit var hiddenAppsRepository: HiddenAppsRepository
    @Inject lateinit var installedAppsRepository: InstalledAppsRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var permissionsHelper: PermissionsHelper
    @Inject lateinit var pinSecurityManager: PinSecurityManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    @Volatile
    private var isScreenInteractive = true

    @Volatile
    private var currentForegroundPackage: String? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                    currentForegroundPackage = null
                    appLockRepository.onScreenTurnedOff()
                    serviceScope.launch {
                        hiddenAppsRepository.rehideExpiredOrTempApps(forceScreenOff = true)
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    isScreenInteractive = true
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        isScreenInteractive = pm.isInteractive

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }

        startForegroundInternal()
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundInternal()
        if (monitorJob?.isActive != true) {
            startMonitoringLoop()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundInternal() {
        val notification = buildForegroundNotification()
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hide this app's own notification content on lock screen & shade (VISIBILITY_SECRET + neutral text)
        return NotificationCompat.Builder(this, GlassLockApp.CHANNEL_MONITOR_SERVICE)
            .setContentTitle("System Service")
            .setContentText("Background optimization running")
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            var rehideTick = 0
            while (isActive) {
                try {
                    rehideTick++
                    if (rehideTick >= 15) {
                        rehideTick = 0
                        hiddenAppsRepository.rehideExpiredOrTempApps(forceScreenOff = false)
                    }

                    val settings = settingsDataStore.settingsFlow.first()
                    if (!settings.isServiceEnabled || !pinSecurityManager.isSetupComplete()) {
                        delay(IDLE_POLL_MS)
                        continue
                    }
                    if (!isScreenInteractive || !permissionsHelper.isUsageAccessGranted()) {
                        delay(IDLE_POLL_MS)
                        continue
                    }

                    val foregroundPkg = detectTopPackage(usageStatsManager)
                    if (foregroundPkg != null && foregroundPkg != packageName) {
                        val isLockedTarget = appLockRepository.isPackageLockedInDb(foregroundPkg)
                        if (isLockedTarget) {
                            if (appLockRepository.shouldLockPackage(foregroundPkg) &&
                                !LockScreenActivity.isShowingFor(foregroundPkg)
                            ) {
                                launchLockOverlay(foregroundPkg)
                            }
                        } else {
                            appLockRepository.applyBackgroundTransition(foregroundPkg)
                        }
                        currentForegroundPackage = foregroundPkg
                    }
                } catch (_: Throwable) {
                    // Keep service alive cleanly on transient UsageStats errors
                }
                delay(ACTIVE_POLL_MS)
            }
        }
    }

    private fun detectTopPackage(usageStatsManager: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - EVENT_LOOKBACK_MS
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var latestPkg: String? = null
        var latestTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isResumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isResumed && event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestPkg = event.packageName
            }
        }

        if (latestPkg != null) {
            return latestPkg
        }

        return currentForegroundPackage
    }

    private fun launchLockOverlay(targetPackage: String) {
        if (!permissionsHelper.isOverlayGranted()) return

        val appName = installedAppsRepository.getAppName(targetPackage)
        val lockIntent = LockScreenActivity.createIntent(
            context = this,
            targetPackage = targetPackage,
            targetAppName = appName
        )
        startActivity(lockIntent)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTIVE_POLL_MS = 350L
        private const val IDLE_POLL_MS = 1200L
        private const val EVENT_LOOKBACK_MS = 4000L

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitorService::class.java))
        }
    }
}
