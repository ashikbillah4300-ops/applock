package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.disguise.DisguiseManager
import com.example.data.local.SettingsDataStore
import com.example.data.permissions.PermissionsHelper
import com.example.data.security.PinSecurityManager
import com.example.domain.model.AppSettings
import com.example.domain.model.HiddenRehidePolicy
import com.example.domain.model.LockMethod
import com.example.domain.model.PermissionStatus
import com.example.domain.model.RelockPolicy
import com.example.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val permissions: PermissionStatus = PermissionStatus(),
    val hasPatternSet: Boolean = false,
    val pinLength: Int = 4,
    val biometricHardwareAvailable: Boolean = false,
    val securityQuestion: String = PinSecurityManager.PRESET_QUESTIONS.first(),
    val isDisguiseActive: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsDataStore: SettingsDataStore,
    private val pinSecurityManager: PinSecurityManager,
    private val permissionsHelper: PermissionsHelper,
    private val disguiseManager: DisguiseManager
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.settingsFlow,
        refreshTick
    ) { settings, _ ->
        val biometricStatus = BiometricManager.from(appContext).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        SettingsUiState(
            settings = settings,
            permissions = permissionsHelper.getPermissionStatus(),
            hasPatternSet = pinSecurityManager.hasPattern(),
            pinLength = pinSecurityManager.getPinLength(),
            biometricHardwareAvailable = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS,
            securityQuestion = pinSecurityManager.getSecurityQuestion(),
            isDisguiseActive = disguiseManager.isCalculatorDisguiseEnabled()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun refresh() {
        refreshTick.value++
    }

    fun setLockMethod(method: LockMethod) {
        viewModelScope.launch {
            settingsDataStore.setLockMethod(method)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBiometricEnabled(enabled)
        }
    }

    fun setRelockPolicy(policy: RelockPolicy) {
        viewModelScope.launch {
            settingsDataStore.setRelockPolicy(policy)
        }
    }

    fun setIntruderSelfieEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setIntruderSelfieEnabled(enabled)
        }
    }

    fun setFakeCrashEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setFakeCrashEnabled(enabled)
        }
    }

    fun setHideNotificationContent(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHideNotificationContent(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setCalculatorDisguise(enabled: Boolean) {
        viewModelScope.launch {
            disguiseManager.setCalculatorDisguise(enabled)
            settingsDataStore.setCalculatorDisguiseEnabled(enabled)
            refresh()
        }
    }

    fun setHideHiddenTabInNav(hidden: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHideHiddenTabInNav(hidden)
        }
    }

    fun setHiddenRehidePolicy(policy: HiddenRehidePolicy) {
        viewModelScope.launch {
            settingsDataStore.setHiddenRehidePolicy(policy)
        }
    }

    fun verifyCurrentPin(pin: String): Boolean = pinSecurityManager.verifyPin(pin)

    fun updatePin(newPin: String) {
        pinSecurityManager.savePin(newPin)
        refresh()
    }

    fun updatePattern(nodes: List<Int>) {
        pinSecurityManager.savePattern(nodes)
        viewModelScope.launch {
            settingsDataStore.setLockMethod(LockMethod.PATTERN)
            refresh()
        }
    }

    fun updateSecurityQuestion(question: String, answer: String) {
        pinSecurityManager.saveSecurityQuestion(question, answer)
        refresh()
    }

    fun enableDeviceAdminIntent(): Intent = permissionsHelper.createEnableDeviceAdminIntent()

    fun disableDeviceAdmin() {
        permissionsHelper.deactivateDeviceAdmin()
        refresh()
    }

    fun notificationListenerIntent(): Intent = permissionsHelper.createNotificationListenerIntent()
}
