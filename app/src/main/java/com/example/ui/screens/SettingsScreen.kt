package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.domain.model.HiddenRehidePolicy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.security.PinSecurityManager
import com.example.domain.model.LockMethod
import com.example.domain.model.RelockPolicy
import com.example.domain.model.ThemeMode
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPatternLock
import com.example.ui.components.GlassPinKeypad
import com.example.ui.lock.LockScreenActivity
import com.example.ui.theme.LocalGlassColors

@Composable
fun SettingsScreen(
    onOpenPermissions: () -> Unit,
    bottomContentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalGlassColors.current

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setIntruderSelfieEnabled(granted)
        viewModel.refresh()
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = bottomContentPadding.calculateBottomPadding() + 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Security Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            // Credentials & Lock Method
            SectionHeader("Authentication")
            GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Primary Lock Method",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.textPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LockMethod.entries.forEach { method ->
                            val selected = state.settings.lockMethod == method
                            Surface(
                                onClick = {
                                    if (method == LockMethod.PATTERN && !state.hasPatternSet) {
                                        showPatternDialog = true
                                    } else {
                                        viewModel.setLockMethod(method)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    glass.accentPrimary.copy(alpha = 0.22f)
                                } else {
                                    glass.keypadButtonFill.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) glass.accentPrimary else glass.cardBorderEnd
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_method_${method.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (method == LockMethod.PIN) Icons.Filled.Pin else Icons.Filled.Gesture,
                                        contentDescription = method.label,
                                        tint = if (selected) glass.accentPrimary else glass.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = method.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) glass.accentPrimary else glass.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    SettingsActionRow(
                        icon = Icons.Filled.LockReset,
                        title = "Change PIN Code",
                        subtitle = "Currently ${state.pinLength} digits (PBKDF2 salted)",
                        testTag = "settings_change_pin",
                        onClick = { showChangePinDialog = true }
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.Gesture,
                        title = if (state.hasPatternSet) "Change Unlock Pattern" else "Set Unlock Pattern",
                        subtitle = if (state.hasPatternSet) "3x3 pattern configured" else "Not configured yet",
                        testTag = "settings_change_pattern",
                        onClick = { showPatternDialog = true }
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.HelpOutline,
                        title = "Security Recovery Question",
                        subtitle = state.securityQuestion,
                        testTag = "settings_security_question",
                        onClick = { showRecoveryDialog = true }
                    )

                    SettingsSwitchRow(
                        icon = Icons.Filled.Fingerprint,
                        title = "Biometric Unlock",
                        subtitle = if (state.biometricHardwareAvailable) {
                            "Allow fingerprint / face prompt"
                        } else {
                            "No enrolled biometrics detected on this device"
                        },
                        checked = state.settings.biometricEnabled,
                        testTag = "settings_biometric_switch",
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
            }

            // Relock Timing
            SectionHeader("Relock Timing")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Relock timing",
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Re-lock protected apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                    }
                    RelockPolicy.entries.forEach { policy ->
                        val selected = state.settings.relockPolicy == policy
                        Surface(
                            onClick = { viewModel.setRelockPolicy(policy) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                glass.accentPrimary.copy(alpha = 0.18f)
                            } else {
                                Color.Transparent
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) glass.accentPrimary else glass.cardBorderEnd
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_relock_${policy.name.lowercase()}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = policy.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) glass.accentPrimary else glass.textPrimary
                                )
                                Text(
                                    text = policy.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Stealth, Privacy & Anti-Uninstall
            SectionHeader("Stealth & Defense")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        icon = Icons.Filled.CameraFront,
                        title = "Intruder Selfie",
                        subtitle = if (state.permissions.cameraGranted) {
                            "Front-camera photo after 3 wrong tries"
                        } else {
                            "Tap to grant Camera permission"
                        },
                        checked = state.settings.intruderSelfieEnabled && state.permissions.cameraGranted,
                        testTag = "settings_intruder_switch",
                        onCheckedChange = { enable ->
                            if (enable && !state.permissions.cameraGranted) {
                                cameraLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                viewModel.setIntruderSelfieEnabled(enable)
                            }
                        }
                    )

                    SettingsSwitchRow(
                        icon = Icons.Filled.BugReport,
                        title = "Fake \"App Crashed\" Screen",
                        subtitle = "Shows crash decoy first. Long-press 'App info' to reveal GlassLock.",
                        checked = state.settings.fakeCrashEnabled,
                        testTag = "settings_fake_crash_switch",
                        onCheckedChange = { viewModel.setFakeCrashEnabled(it) }
                    )

                    SettingsSwitchRow(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "Device Admin Uninstall Shield",
                        subtitle = if (state.permissions.deviceAdminActive) {
                            "Active — blocks uninstallation without PIN"
                        } else {
                            "Tap to activate Device Admin protection"
                        },
                        checked = state.permissions.deviceAdminActive,
                        testTag = "settings_device_admin_switch",
                        onCheckedChange = { enable ->
                            if (enable) {
                                deviceAdminLauncher.launch(viewModel.enableDeviceAdminIntent())
                            } else {
                                viewModel.disableDeviceAdmin()
                            }
                        }
                    )
                }
            }

            // Hidden Vault & App Disguise
            SectionHeader("Hidden Vault & Disguise")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        icon = Icons.Filled.Calculate,
                        title = "Disguise App as \"Calculator\"",
                        subtitle = "Changes launcher icon & name to Calculator via activity-alias",
                        checked = state.isDisguiseActive,
                        testTag = "settings_calculator_disguise_switch",
                        onCheckedChange = { viewModel.setCalculatorDisguise(it) }
                    )

                    SettingsSwitchRow(
                        icon = Icons.Filled.VisibilityOff,
                        title = "Hide \"Hidden\" Tab in Bottom Bar",
                        subtitle = "Access Hidden Vault only via *#1234# dialer code or 5 logo taps",
                        checked = state.settings.hideHiddenTabInNav,
                        testTag = "settings_hide_hidden_tab_switch",
                        onCheckedChange = { viewModel.setHideHiddenTabInNav(it) }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Dialpad,
                            contentDescription = "Secret code",
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dialer Secret Code: *#1234#  •  Logo Gesture: Tap shield 5×",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                    }

                    Text(
                        text = "Re-hide Launched Hidden Apps",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.textPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HiddenRehidePolicy.entries.forEach { policy ->
                            val selected = state.settings.hiddenRehidePolicy == policy
                            Surface(
                                onClick = { viewModel.setHiddenRehidePolicy(policy) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    glass.accentPrimary.copy(alpha = 0.22f)
                                } else {
                                    glass.keypadButtonFill.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) glass.accentPrimary else glass.cardBorderEnd
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_rehide_${policy.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = policy.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) glass.accentPrimary else glass.textSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Appearance & Diagnostics
            SectionHeader("Appearance & System")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = "Theme",
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Glass Theme",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val selected = state.settings.themeMode == mode
                            Surface(
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    glass.accentPrimary.copy(alpha = 0.22f)
                                } else {
                                    glass.keypadButtonFill.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) glass.accentPrimary else glass.cardBorderEnd
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_theme_${mode.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) glass.accentPrimary else glass.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    SettingsActionRow(
                        icon = Icons.Filled.Security,
                        title = "Permissions & OEM Battery Guide",
                        subtitle = if (state.permissions.allCoreGranted) {
                            "All core permissions granted"
                        } else {
                            "Action needed for reliable background lock"
                        },
                        testTag = "settings_permissions_shortcut",
                        onClick = onOpenPermissions
                    )

                    OutlinedButton(
                        onClick = {
                            val previewIntent = LockScreenActivity.createIntent(
                                context = context,
                                targetPackage = context.packageName,
                                targetAppName = "GlassLock Preview",
                                isPreview = true
                            )
                            context.startActivity(previewIntent)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, glass.accentPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_preview_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Preview,
                            contentDescription = "Preview Lock Screen",
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preview Lock Overlay",
                            color = glass.accentPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            verifyCurrentPin = { viewModel.verifyCurrentPin(it) },
            onSaveNewPin = { newPin ->
                viewModel.updatePin(newPin)
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showPatternDialog) {
        SetPatternDialog(
            onSavePattern = { nodes ->
                viewModel.updatePattern(nodes)
                showPatternDialog = false
            },
            onDismiss = { showPatternDialog = false }
        )
    }

    if (showRecoveryDialog) {
        UpdateRecoveryDialog(
            currentQuestion = state.securityQuestion,
            onSave = { q, a ->
                viewModel.updateSecurityQuestion(q, a)
                showRecoveryDialog = false
            },
            onDismiss = { showRecoveryDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val glass = LocalGlassColors.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = glass.accentPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    val glass = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glass.accentPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = glass.accentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = glass.textSecondary)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = glass.textSecondary
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val glass = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glass.accentSecondary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = glass.accentSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = glass.textSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ChangePinDialog(
    verifyCurrentPin: (String) -> Boolean,
    onSaveNewPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassColors.current
    var stage by remember { mutableIntStateOf(0) } // 0 = verify old, 1 = new, 2 = confirm
    var pinLength by remember { mutableIntStateOf(4) }
    var buffer by remember { mutableStateOf("") }
    var firstNewPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (stage) {
                        0 -> "Enter Current PIN"
                        1 -> "Enter New PIN"
                        else -> "Confirm New PIN"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary
                )

                if (stage == 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 6).forEach { len ->
                            Surface(
                                onClick = {
                                    pinLength = len
                                    buffer = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (pinLength == len) glass.accentPrimary.copy(alpha = 0.22f) else glass.cardFill,
                                border = BorderStroke(1.dp, if (pinLength == len) glass.accentPrimary else glass.cardBorderEnd)
                            ) {
                                Text(
                                    text = "$len Digits",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                errorMsg?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = err, style = MaterialTheme.typography.bodyMedium, color = glass.danger)
                }

                GlassPinKeypad(
                    enteredLength = buffer.length,
                    expectedLength = pinLength,
                    isError = errorMsg != null,
                    showBiometricButton = false,
                    onDigitPress = { digit ->
                        val next = buffer + digit
                        errorMsg = null
                        when (stage) {
                            0 -> {
                                if (verifyCurrentPin(next)) {
                                    buffer = ""
                                    stage = 1
                                } else if (next.length >= 6) {
                                    buffer = ""
                                    errorMsg = "Incorrect current PIN"
                                } else {
                                    buffer = next
                                }
                            }
                            1 -> {
                                if (next.length == pinLength) {
                                    firstNewPin = next
                                    buffer = ""
                                    stage = 2
                                } else {
                                    buffer = next
                                }
                            }
                            else -> {
                                if (next.length == pinLength) {
                                    if (next == firstNewPin) {
                                        onSaveNewPin(next)
                                    } else {
                                        buffer = ""
                                        errorMsg = "PINs do not match"
                                    }
                                } else {
                                    buffer = next
                                }
                            }
                        }
                    },
                    onBackspacePress = {
                        if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
                    },
                    onBiometricPress = {}
                )

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = glass.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun SetPatternDialog(
    onSavePattern: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassColors.current
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (firstPattern == null) "Draw New Pattern" else "Confirm New Pattern",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMsg ?: "Connect at least 4 nodes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMsg != null) glass.danger else glass.textSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                GlassPatternLock(
                    isError = errorMsg != null,
                    onPatternCompleted = { nodes ->
                        if (nodes.size < 4) {
                            errorMsg = "Connect at least 4 dots"
                        } else if (firstPattern == null) {
                            firstPattern = nodes
                            errorMsg = null
                        } else if (firstPattern == nodes) {
                            onSavePattern(nodes)
                        } else {
                            errorMsg = "Patterns did not match. Draw again."
                            firstPattern = null
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = glass.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun UpdateRecoveryDialog(
    currentQuestion: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassColors.current
    var selectedQuestion by remember { mutableStateOf(currentQuestion) }
    var answer by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Update Recovery Question",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary
                )

                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = glass.keypadButtonFill,
                        border = BorderStroke(1.dp, glass.cardBorderStart),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedQuestion,
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
                                    selectedQuestion = q
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("New Recovery Answer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = glass.textSecondary)
                    }
                    Button(
                        onClick = { onSave(selectedQuestion, answer) },
                        enabled = answer.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = glass.accentPrimary,
                            contentColor = Color(0xFF042F2E)
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
