package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.InstalledAppsRepository
import com.example.data.security.PinSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SetupStep {
    CREATE_PIN,
    CONFIRM_PIN,
    BACKUP_PATTERN,
    SECURITY_QUESTION
}

data class SetupUiState(
    val step: SetupStep = SetupStep.CREATE_PIN,
    val pinLength: Int = 4,
    val firstPin: String = "",
    val confirmPin: String = "",
    val pinError: String? = null,
    val patternNodes: List<Int>? = null,
    val patternError: String? = null,
    val selectedQuestion: String = PinSecurityManager.PRESET_QUESTIONS.first(),
    val securityAnswer: String = "",
    val biometricEnabled: Boolean = true,
    val answerError: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val pinSecurityManager: PinSecurityManager,
    private val settingsDataStore: SettingsDataStore,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun setPinLength(length: Int) {
        if (length !in 4..6) return
        _uiState.update {
            it.copy(
                pinLength = length,
                firstPin = "",
                confirmPin = "",
                pinError = null,
                step = SetupStep.CREATE_PIN
            )
        }
    }

    fun onDigitPressed(digit: String) {
        val state = _uiState.value
        when (state.step) {
            SetupStep.CREATE_PIN -> {
                if (state.firstPin.length >= state.pinLength) return
                val updated = state.firstPin + digit
                if (updated.length == state.pinLength) {
                    _uiState.update {
                        it.copy(
                            firstPin = updated,
                            confirmPin = "",
                            pinError = null,
                            step = SetupStep.CONFIRM_PIN
                        )
                    }
                } else {
                    _uiState.update { it.copy(firstPin = updated, pinError = null) }
                }
            }
            SetupStep.CONFIRM_PIN -> {
                if (state.confirmPin.length >= state.pinLength) return
                val updated = state.confirmPin + digit
                if (updated.length == state.pinLength) {
                    if (updated == state.firstPin) {
                        _uiState.update {
                            it.copy(
                                confirmPin = updated,
                                pinError = null,
                                step = SetupStep.BACKUP_PATTERN
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                confirmPin = "",
                                pinError = "PINs do not match. Try again."
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(confirmPin = updated, pinError = null) }
                }
            }
            else -> Unit
        }
    }

    fun onBackspacePressed() {
        val state = _uiState.value
        when (state.step) {
            SetupStep.CREATE_PIN -> {
                if (state.firstPin.isNotEmpty()) {
                    _uiState.update { it.copy(firstPin = it.firstPin.dropLast(1), pinError = null) }
                }
            }
            SetupStep.CONFIRM_PIN -> {
                if (state.confirmPin.isNotEmpty()) {
                    _uiState.update { it.copy(confirmPin = it.confirmPin.dropLast(1), pinError = null) }
                } else {
                    _uiState.update { it.copy(step = SetupStep.CREATE_PIN, firstPin = "", pinError = null) }
                }
            }
            else -> Unit
        }
    }

    fun onPatternDrawn(nodes: List<Int>) {
        if (nodes.size < 4) {
            _uiState.update { it.copy(patternError = "Connect at least 4 dots for your pattern.") }
            return
        }
        _uiState.update {
            it.copy(
                patternNodes = nodes,
                patternError = null,
                step = SetupStep.SECURITY_QUESTION
            )
        }
    }

    fun skipPattern() {
        _uiState.update {
            it.copy(
                patternNodes = null,
                patternError = null,
                step = SetupStep.SECURITY_QUESTION
            )
        }
    }

    fun selectQuestion(question: String) {
        _uiState.update { it.copy(selectedQuestion = question) }
    }

    fun updateSecurityAnswer(answer: String) {
        _uiState.update { it.copy(securityAnswer = answer, answerError = null) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    fun goBackStep() {
        _uiState.update { state ->
            when (state.step) {
                SetupStep.CREATE_PIN -> state
                SetupStep.CONFIRM_PIN -> state.copy(step = SetupStep.CREATE_PIN, firstPin = "", confirmPin = "", pinError = null)
                SetupStep.BACKUP_PATTERN -> state.copy(step = SetupStep.CONFIRM_PIN, confirmPin = "", pinError = null)
                SetupStep.SECURITY_QUESTION -> state.copy(step = SetupStep.BACKUP_PATTERN, patternError = null)
            }
        }
    }

    fun completeSetup(onFinished: () -> Unit) {
        val state = _uiState.value
        if (state.securityAnswer.trim().length < 2) {
            _uiState.update { it.copy(answerError = "Please enter an answer (at least 2 characters).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            pinSecurityManager.saveInitialSetup(
                pin = state.firstPin,
                securityQuestion = state.selectedQuestion,
                securityAnswer = state.securityAnswer,
                patternNodes = state.patternNodes
            )
            settingsDataStore.setBiometricEnabled(state.biometricEnabled)
            settingsDataStore.setServiceEnabled(true)
            installedAppsRepository.seedDefaultCriticalLocksIfNeeded()
            _uiState.update { it.copy(isSaving = false) }
            onFinished()
        }
    }
}
