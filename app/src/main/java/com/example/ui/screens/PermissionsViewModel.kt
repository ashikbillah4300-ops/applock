package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.data.permissions.PermissionsHelper
import com.example.domain.model.PermissionStatus
import com.example.service.AppMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val permissionsHelper: PermissionsHelper
) : ViewModel() {

    private val _status = MutableStateFlow(permissionsHelper.getPermissionStatus())
    val status: StateFlow<PermissionStatus> = _status.asStateFlow()

    fun refreshStatus() {
        val updated = permissionsHelper.getPermissionStatus()
        _status.value = updated
        if (updated.usageAccessGranted && updated.overlayGranted) {
            AppMonitorService.start(appContext)
        }
    }

    fun usageAccessIntent(): Intent = permissionsHelper.createUsageAccessIntent()
    fun overlayIntent(): Intent = permissionsHelper.createOverlayIntent()
    fun batteryOptimizationIntent(): Intent = permissionsHelper.createBatteryOptimizationIntent()
    fun notificationSettingsIntent(): Intent = permissionsHelper.createAppNotificationSettingsIntent()
    fun deviceAdminIntent(): Intent = permissionsHelper.createEnableDeviceAdminIntent()
}
