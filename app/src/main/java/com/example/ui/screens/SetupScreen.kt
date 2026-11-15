package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.security.PinSecurityManager
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPatternLock
import com.example.ui.components.GlassPinKeypad
import com.example.ui.theme.LocalGlassColors

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalGlassColors.current

    BackHandler(enabled = state.step != SetupStep.CREATE_PIN) {
        viewModel.goBackStep()
    }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.step != SetupStep.CREATE_PIN) {
                        IconButton(
                            onClick = { viewModel.goBackStep() },
                            modifier = Modifier.testTag("setup_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = glass.textPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Step Indicator Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SetupStep.entries.forEach { step ->
                            val active = step.ordinal <= state.step.ordinal
                            Box(
                                modifier = Modifier
                                    .width(if (step == state.step) 24.dp else 10.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) glass.accentPrimary
                                        else glass.textSecondary.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(glass.accentPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "GlassLock Shield",
                        tint = glass.accentPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "setup_step_anim"
                ) { currentStep ->
                    when (currentStep) {
                        SetupStep.CREATE_PIN, SetupStep.CONFIRM_PIN -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (currentStep == SetupStep.CREATE_PIN) {
                                        "Create Master PIN"
                                    } else {
                                        "Confirm Your PIN"
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = glass.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (currentStep == SetupStep.CREATE_PIN) {
                                        "Choose 4 or 6 digits. Stored salted & hashed inside Android Keystore."
                                    } else {
                                        "Re-enter your ${state.pinLength}-digit PIN to verify."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary,
                                    textAlign = TextAlign.Center
                                )

                                if (currentStep == SetupStep.CREATE_PIN) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(4, 6).forEach { len ->
                                            val selected = state.pinLength == len
                                            Surface(
                                                onClick = { viewModel.setPinLength(len) },
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (selected) {
                                                    glass.accentPrimary.copy(alpha = 0.22f)
                                                } else {
                                                    glass.cardFill
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selected) glass.accentPrimary else glass.cardBorderEnd
                                                ),
                                                modifier = Modifier.testTag("setup_pin_length_$len")
                                            ) {
                                                Text(
                                                    text = "$len Digits",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected) glass.accentPrimary else glass.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                state.pinError?.let { error ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.danger
                                    )
                                }

                                GlassPinKeypad(
                                    enteredLength = if (currentStep == SetupStep.CREATE_PIN) {
                                        state.firstPin.length
                                    } else {
                                        state.confirmPin.length
                                    },
                                    expectedLength = state.pinLength,
                                    isError = state.pinError != null,
                                    showBiometricButton = false,
                                    onDigitPress = { viewModel.onDigitPressed(it) },
                                    onBackspacePress = { viewModel.onBackspacePressed() },
                                    onBiometricPress = {}
                                )
                            }
                        }

                        SetupStep.BACKUP_PATTERN -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Optional Unlock Pattern",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = glass.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Draw a 4+ dot pattern if you want to switch between PIN and Pattern later.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                state.patternError?.let { err ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = err, color = glass.danger, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                                GlassCard(elevated = true) {
                                    Box(modifier = Modifier.padding(16.dp)) {
                                        GlassPatternLock(
                                            isError = state.patternError != null,
                                            onPatternCompleted = { viewModel.onPatternDrawn(it) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(
                                    onClick = { viewModel.skipPattern() },
                                    modifier = Modifier.testTag("setup_skip_pattern")
                                ) {
                                    Text(
                                        text = "Skip for now",
                                        color = glass.accentPrimary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        SetupStep.SECURITY_QUESTION -> {
                            var dropdownExpanded by remember { mutableStateOf(false) }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Recovery & Protection",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = glass.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Set a security question to reset your PIN if you ever forget it.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            text = "Security Question",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = glass.textPrimary
                                        )

                                        Box {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = glass.keypadButtonFill,
                                                border = BorderStroke(1.dp, glass.cardBorderStart),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { dropdownExpanded = true }
                                                    .testTag("setup_question_dropdown")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = state.selectedQuestion,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = glass.textPrimary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Filled.ArrowDropDown,
                                                        contentDescription = "Select question",
                                                        tint = glass.textSecondary
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = dropdownExpanded,
                                                onDismissRequest = { dropdownExpanded = false }
                                            ) {
                                                PinSecurityManager.PRESET_QUESTIONS.forEach { q ->
                                                    DropdownMenuItem(
                                                        text = { Text(q) },
                                                        onClick = {
                                                            viewModel.selectQuestion(q)
                                                            dropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = state.securityAnswer,
                                            onValueChange = { viewModel.updateSecurityAnswer(it) },
                                            label = { Text("Your Answer") },
                                            singleLine = true,
                                            isError = state.answerError != null,
                                            supportingText = state.answerError?.let { err ->
                                                { Text(err, color = glass.danger) }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = glass.accentPrimary,
                                                unfocusedBorderColor = glass.cardBorderEnd,
                                                focusedTextColor = glass.textPrimary,
                                                unfocusedTextColor = glass.textPrimary,
                                                focusedLabelColor = glass.accentPrimary,
                                                unfocusedLabelColor = glass.textSecondary
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("setup_security_answer_input")
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Fingerprint,
                                                contentDescription = "Biometrics",
                                                tint = glass.accentPrimary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Enable Biometric Unlock",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = glass.textPrimary
                                                )
                                                Text(
                                                    text = "Use fingerprint or face unlock alongside PIN",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = glass.textSecondary
                                                )
                                            }
                                            Switch(
                                                checked = state.biometricEnabled,
                                                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                                modifier = Modifier.testTag("setup_biometric_switch")
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Auto-protected",
                                            tint = glass.success,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Settings & Package Installer will be locked automatically to block bypasses.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glass.textSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { viewModel.completeSetup(onSetupComplete) },
                                    enabled = !state.isSaving,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = glass.accentPrimary,
                                        contentColor = Color(0xFF042F2E)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("setup_complete_button")
                                ) {
                                    Text(
                                        text = if (state.isSaving) "Securing Vault..." else "Continue to Permissions",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
