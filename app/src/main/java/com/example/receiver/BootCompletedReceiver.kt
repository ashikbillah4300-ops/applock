package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SettingsDataStore
import com.example.data.repository.HiddenAppsRepository
import com.example.data.security.PinSecurityManager
import com.example.service.AppMonitorService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var pinSecurityManager: PinSecurityManager
    @Inject lateinit var hiddenAppsRepository: HiddenAppsRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context ?: return
        val action = intent?.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == ACTION_QUICKBOOT_POWERON
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = settingsDataStore.settingsFlow.first()
                    if (settings.isServiceEnabled && pinSecurityManager.isSetupComplete()) {
                        AppMonitorService.start(appContext)
                    }
                    if (pinSecurityManager.isSetupComplete()) {
                        hiddenAppsRepository.reconcileOnStartupOrReboot(isBootOrUpdate = true)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
