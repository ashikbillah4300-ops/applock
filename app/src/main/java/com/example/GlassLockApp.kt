package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlassLockApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            CHANNEL_MONITOR_SERVICE,
            getString(R.string.notification_channel_service_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notification_channel_service_desc)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val privacyChannel = NotificationChannel(
            CHANNEL_PRIVACY_MASKED,
            getString(R.string.notification_channel_privacy_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_privacy_desc)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(privacyChannel)
    }

    companion object {
        const val CHANNEL_MONITOR_SERVICE = "glasslock_monitor_service"
        const val CHANNEL_PRIVACY_MASKED = "glasslock_privacy_masked"
    }
}
