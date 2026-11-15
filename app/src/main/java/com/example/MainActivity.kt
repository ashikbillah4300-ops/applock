package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.LockMethod
import com.example.receiver.SecretCodeReceiver
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPatternLock
import com.example.ui.components.GlassPinKeypad
import com.example.ui.navigation.GlassLockNavHost
import com.example.ui.navigation.MainUiState
import com.example.ui.navigation.MainViewModel
import com.example.ui.theme.GlassLockTheme
import com.example.ui.theme.LocalGlassColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var biometricPrompted = false
    private var openHiddenCounter by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra(SecretCodeReceiver.EXTRA_OPEN_HIDDEN_VAULT, false) == true) {
            openHiddenCounter++
        }

        setContent {
            val state by mainViewModel.uiState.collectAsStateWithLifecycle()

            GlassLockTheme(themeMode = state.themeMode) {
                if (!state.isVaultUnlocked) {
                    LaunchedEffect(state.biometricEnabled) {
                        if (state.biometricEnabled && !biometricPrompted) {
                            biometricPrompted = true
                            triggerVaultBiometric(state.isDisguiseActive)
                        }
                    }

                    VaultUnlockGate(
                        state = state,
                        onVerifyPin = { mainViewModel.verifyPin(it) },
                        onVerifyPattern = { mainViewModel.verifyPattern(it) },
                        onBiometricClick = { triggerVaultBiometric(state.isDisguiseActive) },
                        onRecoverPin = { answer, newPin ->
                            mainViewModel.recoverWithSecurityAnswer(answer, newPin)
                        }
                    )
                } else {
                    GlassLockNavHost(
                        isSetupComplete = state.isSetupComplete,
                        allCorePermissionsGranted = state.allCorePermissionsGranted,
                        intruderCount = state.intruderCount,
                        openHiddenTrigger = openHiddenCounter,
                        hideHiddenTabInNav = state.hideHiddenTabInNav,
                        onSetupFinished = { mainViewModel.markSetupFinished() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(SecretCodeReceiver.EXTRA_OPEN_HIDDEN_VAULT, false)) {
            openHiddenCounter++
        }
    }

    private fun triggerVaultBiometric(isDisguised: Boolean = false) {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    mainViewModel.unlockWithBiometric()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isDisguised) "Unlock Calculator" else "Unlock GlassLock")
            .setSubtitle(
                if (isDisguised) "Authenticate to continue"
                else "Authenticate to manage protected apps"
            )
            .setNegativeButtonText("Use PIN / Pattern")
            .setConfirmationRequired(false)
            .build()

        runCatching { prompt.authenticate(info) }
    }
}

@Composable
private fun VaultUnlockGate(
    state: MainUiState,
    onVerifyPin: (String) -> Boolean,
    onVerifyPattern: (List<Int>) -> Boolean,
    onBiometricClick: () -> Unit,
    onRecoverPin: (String, String) -> Boolean
) {
    val glass = LocalGlassColors.current
    var activeMethod by remember(state.lockMethod) { mutableStateOf(state.lockMethod) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showRecovery by remember { mutableStateOf(false) }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(glass.accentPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isDisguiseActive) Icons.Filled.Calculate else Icons.Filled.Shield,
                        contentDescription = if (state.isDisguiseActive) "Calculator" else "GlassLock Vault",
                        tint = glass.accentPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (state.isDisguiseActive) "Calculator" else "GlassLock Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMsg ?: if (state.isDisguiseActive) {
                        "Enter passcode to continue"
                    } else {
                        "Authenticate to configure app locks"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMsg != null) glass.danger else glass.textSecondary
                )
            }

            if (activeMethod == LockMethod.PIN) {
                GlassPinKeypad(
                    enteredLength = enteredPin.length,
                    expectedLength = state.expectedPinLength,
                    isError = errorMsg != null,
                    showBiometricButton = state.biometricEnabled,
                    onDigitPress = { digit ->
                        val next = enteredPin + digit
                        errorMsg = null
                        if (next.length >= state.expectedPinLength) {
                            if (!onVerifyPin(next)) {
                                enteredPin = ""
                                errorMsg = "Incorrect PIN. Try again."
                            }
                        } else {
                            enteredPin = next
                        }
                    },
                    onBackspacePress = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                            errorMsg = null
                        }
                    },
                    onBiometricPress = onBiometricClick
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassCard(elevated = true) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            GlassPatternLock(
                                isError = errorMsg != null,
                                onPatternCompleted = { nodes ->
                                    if (!onVerifyPattern(nodes)) {
                                        errorMsg = "Incorrect pattern. Try again."
                                    }
                                }
                            )
                        }
                    }
                    if (state.biometricEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(onClick = onBiometricClick) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = "Biometric unlock",
                                tint = glass.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use Fingerprint", color = glass.accentPrimary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.hasPatternSet) {
                    TextButton(
                        onClick = {
                            activeMethod = if (activeMethod == LockMethod.PIN) {
                                LockMethod.PATTERN
                            } else {
                                LockMethod.PIN
                            }
                            enteredPin = ""
                            errorMsg = null
                        }
                    ) {
                        Icon(
                            imageVector = if (activeMethod == LockMethod.PIN) Icons.Filled.Gesture else Icons.Filled.Pin,
                            contentDescription = "Switch lock method",
                            tint = glass.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeMethod == LockMethod.PIN) "Use Pattern" else "Use PIN",
                            color = glass.textSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                TextButton(
                    onClick = { showRecovery = true },
                    modifier = Modifier.testTag("vault_forgot_pin")
                ) {
                    Text("Forgot PIN?", color = glass.accentPrimary)
                }
            }
        }
    }

    if (showRecovery) {
        var answer by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var recoveryErr by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showRecovery = false }) {
            GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reset Master PIN",
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.textPrimary
                    )
                    Text(
                        text = state.securityQuestion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = glass.accentPrimary
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it; recoveryErr = null },
                        label = { Text("Security Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New PIN (4–6 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    recoveryErr?.let { Text(it, color = glass.danger) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRecovery = false }) {
                            Text("Cancel", color = glass.textSecondary)
                        }
                        Button(
                            onClick = {
                                if (newPin.length !in 4..6) {
                                    recoveryErr = "PIN must be 4 to 6 digits."
                                } else if (!onRecoverPin(answer, newPin)) {
                                    recoveryErr = "Incorrect answer."
                                } else {
                                    showRecovery = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = Color(0xFF042F2E)
                            )
                        ) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
