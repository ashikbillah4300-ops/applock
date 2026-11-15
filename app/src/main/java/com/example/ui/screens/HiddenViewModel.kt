package com.example.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.HiddenActionResult
import com.example.data.repository.HiddenAppsRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.data.security.PinSecurityManager
import com.example.data.shizuku.ShizukuManager
import com.example.data.shizuku.ShizukuStatus
import com.example.domain.model.HiddenAppInfo
import com.example.domain.model.HiddenRehidePolicy
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.LockMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HiddenUiState(
    val isGateUnlocked: Boolean = false,
    val lockMethod: LockMethod = LockMethod.PIN,
    val expectedPinLength: Int = 4,
    val biometricEnabled: Boolean = true,
    val hasPatternSet: Boolean = false,
    val shizukuStatus: ShizukuStatus = ShizukuStatus.NOT_INSTALLED,
    val hiddenApps: List<HiddenAppInfo> = emptyList(),
    val pickableApps: List<InstalledAppInfo> = emptyList(),
    val rehidePolicy: HiddenRehidePolicy = HiddenRehidePolicy.ON_SCREEN_OFF,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class HiddenViewModel @Inject constructor(
    private val hiddenAppsRepository: HiddenAppsRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val shizukuManager: ShizukuManager,
    private val pinSecurityManager: PinSecurityManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val gateUnlocked = MutableStateFlow(false)
    private val isBusy = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<Pair<String, Boolean>?>(null)

    val uiState: StateFlow<HiddenUiState> = combine(
        gateUnlocked,
        shizukuManager.status,
        hiddenAppsRepository.observeHiddenApps(),
        installedAppsRepository.observeApps(),
        settingsDataStore.settingsFlow,
        isBusy,
        statusMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val unlocked = values[0] as Boolean
        val shizuku = values[1] as ShizukuStatus
        val hiddenList = values[2] as List<HiddenAppInfo>
        val installed = values[3] as List<InstalledAppInfo>
        val settings = values[4] as com.example.domain.model.AppSettings
        val busy = values[5] as Boolean
        val msgPair = values[6] as Pair<String, Boolean>?

        val hiddenPackages = hiddenList.map { it.packageName }.toSet()
        val protectedPackages = hiddenAppsRepository.getProtectedCriticalPackages()
        val pickable = installed.filter {
            !hiddenPackages.contains(it.packageName) && !protectedPackages.contains(it.packageName)
        }

        HiddenUiState(
            isGateUnlocked = unlocked,
            lockMethod = if (settings.lockMethod == LockMethod.PATTERN && pinSecurityManager.hasPattern()) {
                LockMethod.PATTERN
            } else {
                LockMethod.PIN
            },
            expectedPinLength = pinSecurityManager.getPinLength(),
            biometricEnabled = settings.biometricEnabled,
            hasPatternSet = pinSecurityManager.hasPattern(),
            shizukuStatus = shizuku,
            hiddenApps = hiddenList,
            pickableApps = pickable,
            rehidePolicy = settings.hiddenRehidePolicy,
            isBusy = busy,
            statusMessage = msgPair?.first,
            isError = msgPair?.second ?: false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HiddenUiState()
    )

    fun refreshState() {
        shizukuManager.refreshStatus()
        viewModelScope.launch {
            hiddenAppsRepository.reconcileOnStartupOrReboot(isBootOrUpdate = false)
        }
    }

    fun lockGate() {
        gateUnlocked.value = false
    }

    fun verifyPin(pin: String): Boolean {
        val ok = pinSecurityManager.verifyPin(pin)
        if (ok) {
            gateUnlocked.value = true
            refreshState()
        }
        return ok
    }

    fun verifyPattern(nodes: List<Int>): Boolean {
        val ok = pinSecurityManager.verifyPattern(nodes)
        if (ok) {
            gateUnlocked.value = true
            refreshState()
        }
        return ok
    }

    fun unlockWithBiometric() {
        gateUnlocked.value = true
        refreshState()
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun shizukuOrStoreIntent(): Intent = shizukuManager.createOpenShizukuOrStoreIntent()

    fun developerSettingsIntent(): Intent = shizukuManager.createDeveloperSettingsIntent()

    fun hideSelectedApps(apps: List<InstalledAppInfo>) {
        viewModelScope.launch {
            isBusy.value = true
            when (val res = hiddenAppsRepository.hideApps(apps)) {
                is HiddenActionResult.Success -> statusMessage.value = res.message to false
                is HiddenActionResult.Error -> statusMessage.value = res.message to true
            }
            isBusy.value = false
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            isBusy.value = true
            when (val res = hiddenAppsRepository.unhideApp(packageName)) {
                is HiddenActionResult.Success -> statusMessage.value = res.message to false
                is HiddenActionResult.Error -> statusMessage.value = res.message to true
            }
            isBusy.value = false
        }
    }

    fun unhideAll() {
        viewModelScope.launch {
            isBusy.value = true
            when (val res = hiddenAppsRepository.unhideAll()) {
                is HiddenActionResult.Success -> statusMessage.value = res.message to false
                is HiddenActionResult.Error -> statusMessage.value = res.message to true
            }
            isBusy.value = false
        }
    }

    fun launchHiddenApp(packageName: String, onLaunchReady: (Intent) -> Unit) {
        viewModelScope.launch {
            isBusy.value = true
            val result = hiddenAppsRepository.launchTemporarily(packageName)
            isBusy.value = false
            result.onSuccess { intent ->
                onLaunchReady(intent)
            }.onFailure { err ->
                statusMessage.value = (err.message ?: "Unable to launch app.") to true
            }
        }
    }

    fun clearMessage() {
        statusMessage.value = null
    }
}
