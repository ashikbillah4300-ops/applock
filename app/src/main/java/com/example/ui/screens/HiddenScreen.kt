package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.shizuku.ShizukuStatus
import com.example.domain.model.HiddenAppInfo
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.LockMethod
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPatternLock
import com.example.ui.components.GlassPinKeypad
import com.example.ui.theme.LocalGlassColors

@Composable
fun HiddenScreen(
    bottomContentPadding: PaddingValues,
    viewModel: HiddenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalGlassColors.current

    var showAppPicker by remember { mutableStateOf(false) }
    var pendingWarningApps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    var showUnhideAllConfirm by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshState()
    }

    // Re-lock Hidden tab when leaving the tab
    DisposableEffect(Unit) {
        onDispose {
            viewModel.lockGate()
        }
    }

    if (!state.isGateUnlocked) {
        HiddenTabAuthGate(
            state = state,
            bottomContentPadding = bottomContentPadding,
            onVerifyPin = { viewModel.verifyPin(it) },
            onVerifyPattern = { viewModel.verifyPattern(it) },
            onBiometricUnlocked = { viewModel.unlockWithBiometric() }
        )
        return
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header Card
            GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(glass.accentSecondary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = "Hidden Vault",
                                tint = glass.accentSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hidden Vault",
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "${state.hiddenApps.size} hidden • Re-hide: ${state.rehidePolicy.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshState() },
                            modifier = Modifier.testTag("hidden_refresh_shizuku")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh Shizuku status",
                                tint = glass.accentPrimary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showAppPicker = true },
                            enabled = state.shizukuStatus == ShizukuStatus.READY && !state.isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = Color(0xFF042F2E)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hidden_add_apps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddModerator,
                                contentDescription = "Hide apps",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hide apps",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.hiddenApps.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showUnhideAllConfirm = true },
                                enabled = state.shizukuStatus == ShizukuStatus.READY && !state.isBusy,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, glass.warning),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("hidden_unhide_all_button")
                            ) {
                                Text(
                                    text = "Unhide all",
                                    color = glass.warning,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Status / Error Banner
            AnimatedVisibility(visible = state.statusMessage != null) {
                state.statusMessage?.let { msg ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        onClick = { viewModel.clearMessage() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                contentDescription = "Status",
                                tint = if (state.isError) glass.danger else glass.success,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Shizuku Onboarding / Restart Card when not READY
            AnimatedVisibility(visible = state.shizukuStatus != ShizukuStatus.READY) {
                ShizukuOnboardingCard(
                    status = state.shizukuStatus,
                    hasHiddenApps = state.hiddenApps.isNotEmpty(),
                    onOpenShizuku = { runCatching { context.startActivity(viewModel.shizukuOrStoreIntent()) } },
                    onOpenDevOptions = { runCatching { context.startActivity(viewModel.developerSettingsIntent()) } },
                    onGrantPermission = { viewModel.requestShizukuPermission() },
                    onRefresh = { viewModel.refreshState() },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isBusy) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = glass.accentPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (state.hiddenApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "No hidden apps",
                                tint = glass.accentPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Hidden Apps Yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap \"Hide apps\" to disable selected apps via Shizuku so they vanish from the launcher and notifications.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 2.dp,
                        bottom = bottomContentPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.hiddenApps, key = { it.packageName }) { app ->
                        HiddenAppRowCard(
                            app = app,
                            shizukuReady = state.shizukuStatus == ShizukuStatus.READY,
                            busy = state.isBusy,
                            onLaunch = {
                                viewModel.launchHiddenApp(app.packageName) { intent ->
                                    runCatching { context.startActivity(intent) }
                                }
                            },
                            onUnhide = { viewModel.unhideApp(app.packageName) }
                        )
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        HideAppsPickerDialog(
            apps = state.pickableApps,
            onConfirmSelection = { selected ->
                showAppPicker = false
                if (selected.isNotEmpty()) {
                    pendingWarningApps = selected
                }
            },
            onDismiss = { showAppPicker = false }
        )
    }

    pendingWarningApps?.let { appsToHide ->
        AlertDialog(
            onDismissRequest = { pendingWarningApps = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = glass.warning
                )
            },
            title = { Text("Hide ${appsToHide.size} App${if (appsToHide.size == 1) "" else "s"}?") },
            text = {
                Text(
                    "Hidden apps are disabled for user 0 via Shizuku. They disappear from your launcher, stop receiving notifications, and cannot update until unhidden. Keep Shizuku available to restore them anytime."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hideSelectedApps(appsToHide)
                        pendingWarningApps = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = glass.accentPrimary,
                        contentColor = Color(0xFF042F2E)
                    )
                ) {
                    Text("Hide Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingWarningApps = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUnhideAllConfirm) {
        AlertDialog(
            onDismissRequest = { showUnhideAllConfirm = false },
            title = { Text("Unhide All Apps?") },
            text = { Text("All ${state.hiddenApps.size} hidden apps will be re-enabled and visible in your launcher again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unhideAll()
                        showUnhideAllConfirm = false
                    }
                ) {
                    Text("Unhide All", color = glass.warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnhideAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HiddenTabAuthGate(
    state: HiddenUiState,
    bottomContentPadding: PaddingValues,
    onVerifyPin: (String) -> Boolean,
    onVerifyPattern: (List<Int>) -> Boolean,
    onBiometricUnlocked: () -> Unit
) {
    val glass = LocalGlassColors.current
    val context = LocalContext.current
    var activeMethod by remember(state.lockMethod) { mutableStateOf(state.lockMethod) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var autoPrompted by remember { mutableStateOf(false) }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity ?: return
        val bm = BiometricManager.from(activity)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hidden Vault")
            .setSubtitle("Verify identity to access hidden apps")
            .setNegativeButtonText("Use PIN / Pattern")
            .setConfirmationRequired(false)
            .build()
        runCatching { prompt.authenticate(info) }
    }

    LaunchedEffect(state.biometricEnabled) {
        if (state.biometricEnabled && !autoPrompted) {
            autoPrompted = true
            triggerBiometric()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = bottomContentPadding.calculateBottomPadding() + 12.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(glass.accentSecondary.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Hidden Vault Locked",
                    tint = glass.accentPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Hidden Vault Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary
            )
            Text(
                text = errorMsg ?: "Verify PIN, pattern, or fingerprint to view hidden apps",
                style = MaterialTheme.typography.bodyMedium,
                color = if (errorMsg != null) glass.danger else glass.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                    onBiometricPress = { triggerBiometric() }
                )
            } else {
                GlassCard(elevated = true) {
                    Box(modifier = Modifier.padding(14.dp)) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { triggerBiometric() }) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Fingerprint",
                            tint = glass.accentPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Fingerprint", color = glass.accentPrimary)
                    }
                }
            }

            if (state.hasPatternSet) {
                TextButton(
                    onClick = {
                        activeMethod = if (activeMethod == LockMethod.PIN) LockMethod.PATTERN else LockMethod.PIN
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
            }
        }
    }
}

@Composable
private fun ShizukuOnboardingCard(
    status: ShizukuStatus,
    hasHiddenApps: Boolean,
    onOpenShizuku: () -> Unit,
    onOpenDevOptions: () -> Unit,
    onGrantPermission: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    GlassCard(elevated = true, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Shizuku setup required",
                    tint = glass.warning,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (status) {
                        ShizukuStatus.NOT_INSTALLED -> "Install Shizuku to Hide Apps"
                        ShizukuStatus.NOT_RUNNING -> if (hasHiddenApps) {
                            "Shizuku Stopped — Restart Required"
                        } else {
                            "Start Shizuku via Wireless Debugging"
                        }
                        ShizukuStatus.PERMISSION_REQUIRED -> "Grant Shizuku Permission"
                        ShizukuStatus.READY -> "Shizuku Ready"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary
                )
            }

            Text(
                text = when (status) {
                    ShizukuStatus.NOT_INSTALLED ->
                        "1. Install Shizuku from Play Store or GitHub.\n2. Enable Developer Options → Wireless Debugging.\n3. Pair & start Shizuku, then return and grant permission."
                    ShizukuStatus.NOT_RUNNING ->
                        "Shizuku service is not running (reboots stop Wireless Debugging). Open Shizuku → Start via Wireless debugging (Android 11+) or ADB, then tap Check."
                    ShizukuStatus.PERMISSION_REQUIRED ->
                        "Shizuku is running! Tap \"Grant permission\" below so GlassLock can disable and re-enable selected apps."
                    ShizukuStatus.READY -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (status) {
                    ShizukuStatus.NOT_INSTALLED -> {
                        Button(
                            onClick = onOpenShizuku,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = Color(0xFF042F2E)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_install_button")
                        ) {
                            Text("Get Shizuku", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenDevOptions,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeveloperMode,
                                contentDescription = "Developer options",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dev Options")
                        }
                    }

                    ShizukuStatus.NOT_RUNNING -> {
                        Button(
                            onClick = onOpenShizuku,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = Color(0xFF042F2E)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_open_button")
                        ) {
                            Text("Open Shizuku", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenDevOptions,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Wireless Debug")
                        }
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Check")
                        }
                    }

                    ShizukuStatus.PERMISSION_REQUIRED -> {
                        Button(
                            onClick = onGrantPermission,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = Color(0xFF042F2E)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_grant_permission_button")
                        ) {
                            Text("Grant permission", fontWeight = FontWeight.Bold)
                        }
                    }

                    ShizukuStatus.READY -> Unit
                }
            }
        }
    }
}

@Composable
private fun HiddenAppRowCard(
    app: HiddenAppInfo,
    shizukuReady: Boolean,
    busy: Boolean,
    onLaunch: () -> Unit,
    onUnhide: () -> Unit
) {
    val glass = LocalGlassColors.current
    GlassCard(
        elevated = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hidden_card_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                drawable = null,
                contentDescription = app.appName,
                size = 44.dp,
                packageName = app.packageName
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (app.isTemporarilyLaunched) {
                        glass.warning.copy(alpha = 0.18f)
                    } else {
                        glass.success.copy(alpha = 0.16f)
                    }
                ) {
                    Text(
                        text = if (app.isTemporarilyLaunched) "Temp Open" else "Hidden",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (app.isTemporarilyLaunched) glass.warning else glass.success,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onLaunch,
                enabled = shizukuReady && !busy,
                modifier = Modifier.testTag("hidden_launch_${app.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Launch,
                    contentDescription = "Launch temporarily",
                    tint = if (shizukuReady) glass.accentPrimary else glass.textSecondary
                )
            }

            IconButton(
                onClick = onUnhide,
                enabled = shizukuReady && !busy,
                modifier = Modifier.testTag("hidden_unhide_${app.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = "Unhide app",
                    tint = if (shizukuReady) glass.textPrimary else glass.textSecondary
                )
            }
        }
    }
}

@Composable
private fun HideAppsPickerDialog(
    apps: List<InstalledAppInfo>,
    onConfirmSelection: (List<InstalledAppInfo>) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassColors.current
    var query by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateListOf<String>() }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            elevated = true,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Apps to Hide",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "System-critical apps are safely excluded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close picker",
                            tint = glass.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = glass.textSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = glass.accentPrimary,
                        unfocusedBorderColor = glass.cardBorderEnd,
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = selectedPackages.contains(app.packageName)
                        GlassCard(
                            onClick = {
                                if (checked) selectedPackages.remove(app.packageName)
                                else selectedPackages.add(app.packageName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("picker_app_${app.packageName}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconImage(
                                    drawable = app.icon,
                                    contentDescription = app.appName,
                                    size = 38.dp,
                                    packageName = app.packageName
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = glass.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (!selectedPackages.contains(app.packageName)) {
                                                selectedPackages.add(app.packageName)
                                            }
                                        } else {
                                            selectedPackages.remove(app.packageName)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val selected = apps.filter { selectedPackages.contains(it.packageName) }
                        onConfirmSelection(selected)
                    },
                    enabled = selectedPackages.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = glass.accentPrimary,
                        contentColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("picker_confirm_hide_button")
                ) {
                    Text(
                        text = "Hide Selected (${selectedPackages.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
