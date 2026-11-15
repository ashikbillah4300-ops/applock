package com.example.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.permissions.PermissionsHelper
import com.example.data.repository.AppLockRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.data.repository.IntruderRepository
import com.example.data.security.PinSecurityManager
import com.example.domain.model.LockMethod
import com.example.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LockScreenUiState(
    val targetPackage: String = "",
    val targetAppName: String = "",
    val lockMethod: LockMethod = LockMethod.PIN,
    val expectedPinLength: Int = 4,
    val enteredPin: String = "",
    val wrongAttempts: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val showFakeCrashDecoy: Boolean = false,
    val biometricEnabled: Boolean = true,
    val intruderSelfieEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val securityQuestion: String = "",
    val isPreview: Boolean = false
)

@HiltViewModel
class LockScreenViewModel @Inject constructor(
    private val pinSecurityManager: PinSecurityManager,
    private val settingsDataStore: SettingsDataStore,
    private val appLockRepository: AppLockRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val intruderRepository: IntruderRepository,
    private val permissionsHelper: PermissionsHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockScreenUiState())
    val uiState: StateFlow<LockScreenUiState> = _uiState.asStateFlow()

    fun initialize(targetPackage: String, targetAppName: String, isPreview: Boolean) {
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val effectiveMethod = if (settings.lockMethod == LockMethod.PATTERN && pinSecurityManager.hasPattern()) {
                LockMethod.PATTERN
            } else {
                LockMethod.PIN
            }
            _uiState.value = LockScreenUiState(
                targetPackage = targetPackage,
                targetAppName = targetAppName,
                lockMethod = effectiveMethod,
                expectedPinLength = pinSecurityManager.getPinLength(),
                showFakeCrashDecoy = settings.fakeCrashEnabled,
                biometricEnabled = settings.biometricEnabled,
                intruderSelfieEnabled = settings.intruderSelfieEnabled,
                themeMode = settings.themeMode,
                securityQuestion = pinSecurityManager.getSecurityQuestion(),
                isPreview = isPreview
            )
        }
    }

    fun getTargetAppIcon() =
        _uiState.value.targetPackage.takeIf { it.isNotBlank() }?.let {
            installedAppsRepository.getAppIcon(it)
        }

    fun dismissFakeCrashDecoy() {
        _uiState.update { it.copy(showFakeCrashDecoy = false) }
    }

    fun toggleAuthMethod() {
        _uiState.update { state ->
            val next = if (state.lockMethod == LockMethod.PIN && pinSecurityManager.hasPattern()) {
                LockMethod.PATTERN
            } else {
                LockMethod.PIN
            }
            state.copy(lockMethod = next, enteredPin = "", isError = false, errorMessage = null)
        }
    }

    fun hasPattern(): Boolean = pinSecurityManager.hasPattern()

    fun onDigitPressed(
        digit: String,
        onUnlocked: () -> Unit,
        onTriggerIntruderCapture: (attemptCount: Int, methodLabel: String) -> Unit
    ) {
        val state = _uiState.value
        if (state.enteredPin.length >= state.expectedPinLength) return
        val next = state.enteredPin + digit
        if (next.length == state.expectedPinLength) {
            if (pinSecurityManager.verifyPin(next)) {
                markUnlocked()
                onUnlocked()
            } else {
                val failures = state.wrongAttempts + 1
                _uiState.update {
                    it.copy(
                        enteredPin = "",
                        wrongAttempts = failures,
                        isError = true,
                        errorMessage = "Wrong PIN ($failures)"
                    )
                }
                if (failures >= 3 && failures % 3 == 0) {
                    onTriggerIntruderCapture(failures, "PIN")
                }
            }
        } else {
            _uiState.update { it.copy(enteredPin = next, isError = false, errorMessage = null) }
        }
    }

    fun onBackspacePressed() {
        _uiState.update {
            if (it.enteredPin.isNotEmpty()) {
                it.copy(enteredPin = it.enteredPin.dropLast(1), isError = false, errorMessage = null)
            } else {
                it
            }
        }
    }

    fun onPatternEntered(
        nodes: List<Int>,
        onUnlocked: () -> Unit,
        onTriggerIntruderCapture: (attemptCount: Int, methodLabel: String) -> Unit
    ) {
        if (nodes.size < 4) {
            _uiState.update { it.copy(isError = true, errorMessage = "Connect at least 4 dots") }
            return
        }
        if (pinSecurityManager.verifyPattern(nodes)) {
            markUnlocked()
            onUnlocked()
        } else {
            val failures = _uiState.value.wrongAttempts + 1
            _uiState.update {
                it.copy(
                    wrongAttempts = failures,
                    isError = true,
                    errorMessage = "Wrong pattern ($failures)"
                )
            }
            if (failures >= 3 && failures % 3 == 0) {
                onTriggerIntruderCapture(failures, "Pattern")
            }
        }
    }

    fun onBiometricSucceeded(onUnlocked: () -> Unit) {
        markUnlocked()
        onUnlocked()
    }

    fun verifyRecoveryAndResetPin(answer: String, newPin: String): Boolean {
        if (!pinSecurityManager.verifySecurityAnswer(answer)) return false
        pinSecurityManager.savePin(newPin)
        markUnlocked()
        return true
    }

    fun createPhotoFileIfPermitted(): File? {
        val state = _uiState.value
        if (!state.intruderSelfieEnabled || !permissionsHelper.isCameraGranted()) {
            return null
        }
        return intruderRepository.createIntruderPhotoFile()
    }

    fun recordIntruderEvent(photoPath: String, attemptCount: Int, methodLabel: String) {
        val state = _uiState.value
        viewModelScope.launch {
            intruderRepository.logIntruderAttempt(
                packageName = state.targetPackage,
                appName = state.targetAppName.ifBlank { state.targetPackage },
                photoPath = photoPath,
                attemptCount = attemptCount,
                authMethodUsed = methodLabel
            )
        }
    }

    private fun markUnlocked() {
        val state = _uiState.value
        if (!state.isPreview && state.targetPackage.isNotBlank()) {
            appLockRepository.markTemporarilyUnlocked(state.targetPackage)
        }
    }
}
