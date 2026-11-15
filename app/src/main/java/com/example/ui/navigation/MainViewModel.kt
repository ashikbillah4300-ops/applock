package com.example.ui.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.disguise.DisguiseManager
import com.example.data.local.SettingsDataStore
import com.example.data.permissions.PermissionsHelper
import com.example.data.repository.HiddenAppsRepository
import com.example.data.repository.IntruderRepository
import com.example.data.security.PinSecurityManager
import com.example.domain.model.LockMethod
import com.example.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isSetupComplete: Boolean = false,
    val allCorePermissionsGranted: Boolean = false,
    val isVaultUnlocked: Boolean = false,
    val lockMethod: LockMethod = LockMethod.PIN,
    val expectedPinLength: Int = 4,
    val biometricEnabled: Boolean = true,
    val hasPatternSet: Boolean = false,
    val securityQuestion: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val intruderCount: Int = 0,
    val hideHiddenTabInNav: Boolean = false,
    val isDisguiseActive: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val pinSecurityManager: PinSecurityManager,
    private val settingsDataStore: SettingsDataStore,
    private val permissionsHelper: PermissionsHelper,
    private val hiddenAppsRepository: HiddenAppsRepository,
    private val disguiseManager: DisguiseManager,
    intruderRepository: IntruderRepository
) : ViewModel() {

    private val vaultUnlocked = MutableStateFlow(!pinSecurityManager.isSetupComplete())
    private val setupCompletedInSession = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                hiddenAppsRepository.reconcileOnStartupOrReboot(isBootOrUpdate = false)
            }
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        settingsDataStore.settingsFlow,
        intruderRepository.observeIntruderAttempts(),
        vaultUnlocked,
        setupCompletedInSession
    ) { settings, intruders, unlocked, completedNow ->
        val setupDone = pinSecurityManager.isSetupComplete() || completedNow
        val perms = permissionsHelper.getPermissionStatus()
        MainUiState(
            isSetupComplete = setupDone,
            allCorePermissionsGranted = perms.allCoreGranted,
            isVaultUnlocked = unlocked || !setupDone,
            lockMethod = if (settings.lockMethod == LockMethod.PATTERN && pinSecurityManager.hasPattern()) {
                LockMethod.PATTERN
            } else {
                LockMethod.PIN
            },
            expectedPinLength = pinSecurityManager.getPinLength(),
            biometricEnabled = settings.biometricEnabled,
            hasPatternSet = pinSecurityManager.hasPattern(),
            securityQuestion = pinSecurityManager.getSecurityQuestion(),
            themeMode = settings.themeMode,
            intruderCount = intruders.size,
            hideHiddenTabInNav = settings.hideHiddenTabInNav,
            isDisguiseActive = settings.calculatorDisguiseEnabled || disguiseManager.isCalculatorDisguiseEnabled()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            isSetupComplete = pinSecurityManager.isSetupComplete(),
            isVaultUnlocked = !pinSecurityManager.isSetupComplete(),
            expectedPinLength = pinSecurityManager.getPinLength(),
            isDisguiseActive = disguiseManager.isCalculatorDisguiseEnabled()
        )
    )

    fun verifyPin(pin: String): Boolean {
        val ok = pinSecurityManager.verifyPin(pin)
        if (ok) vaultUnlocked.value = true
        return ok
    }

    fun verifyPattern(nodes: List<Int>): Boolean {
        val ok = pinSecurityManager.verifyPattern(nodes)
        if (ok) vaultUnlocked.value = true
        return ok
    }

    fun unlockWithBiometric() {
        vaultUnlocked.value = true
    }

    fun recoverWithSecurityAnswer(answer: String, newPin: String): Boolean {
        if (!pinSecurityManager.verifySecurityAnswer(answer)) return false
        pinSecurityManager.savePin(newPin)
        vaultUnlocked.value = true
        return true
    }

    fun markSetupFinished() {
        setupCompletedInSession.value = true
        vaultUnlocked.value = true
    }
}
