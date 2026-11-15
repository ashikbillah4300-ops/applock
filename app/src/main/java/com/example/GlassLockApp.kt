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

        manager.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val CHANNEL_MONITOR_SERVICE = "glasslock_monitor_service"
    }
}
