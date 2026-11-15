package com.example.ui.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.LockMethod
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPatternLock
import com.example.ui.components.GlassPinKeypad
import com.example.ui.theme.GlassLockTheme
import com.example.ui.theme.LocalGlassColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LockScreenActivity : FragmentActivity() {

    private val viewModel: LockScreenViewModel by viewModels()
    private var unlockedSuccessfully = false
    private var biometricTriggeredOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        handleNewIntent(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            BackHandler(enabled = true) {
                if (state.isPreview) {
                    finish()
                } else {
                    navigateToHomeScreen()
                }
            }

            GlassLockTheme(themeMode = state.themeMode) {
                LaunchedEffect(state.biometricEnabled, state.showFakeCrashDecoy) {
                    if (state.biometricEnabled && !state.showFakeCrashDecoy && !biometricTriggeredOnce) {
                        biometricTriggeredOnce = true
                        showBiometricPrompt()
                    }
                }

                LockOverlayContent(
                    state = state,
                    onDigitPress = { digit ->
                        viewModel.onDigitPressed(
                            digit = digit,
                            onUnlocked = { finishUnlocked() },
                            onTriggerIntruderCapture = { attempts, method ->
                                captureIntruderSelfie(attempts, method)
                            }
                        )
                    },
                    onBackspace = { viewModel.onBackspacePressed() },
                    onPatternCompleted = { nodes ->
                        viewModel.onPatternEntered(
                            nodes = nodes,
                            onUnlocked = { finishUnlocked() },
                            onTriggerIntruderCapture = { attempts, method ->
                                captureIntruderSelfie(attempts, method)
                            }
                        )
                    },
                    onBiometricClick = { showBiometricPrompt() },
                    onToggleMethod = { viewModel.toggleAuthMethod() },
                    onDismissFakeCrash = {
                        viewModel.dismissFakeCrashDecoy()
                        if (state.biometricEnabled && !biometricTriggeredOnce) {
                            biometricTriggeredOnce = true
                            showBiometricPrompt()
                        }
                    },
                    onCloseToHome = {
                        if (state.isPreview) finish() else navigateToHomeScreen()
                    },
                    onRecoverPin = { answer, newPin ->
                        val ok = viewModel.verifyRecoveryAndResetPin(answer, newPin)
                        if (ok) finishUnlocked()
                        ok
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        biometricTriggeredOnce = false
        handleNewIntent(intent)
    }

    private fun handleNewIntent(intent: Intent) {
        val pkg = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val appName = intent.getStringExtra(EXTRA_TARGET_APP_NAME).orEmpty().ifBlank { pkg }
        val isPreview = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)
        activeTargetPackage = pkg
        unlockedSuccessfully = false
        viewModel.initialize(pkg, appName, isPreview)
    }

    override fun onResume() {
        super.onResume()
        activeTargetPackage = viewModel.uiState.value.targetPackage
    }

    override fun onStop() {
        super.onStop()
        if (!unlockedSuccessfully && !isChangingConfigurations) {
            activeTargetPackage = null
            finish()
        }
    }

    override fun onDestroy() {
        activeTargetPackage = null
        super.onDestroy()
    }

    private fun finishUnlocked() {
        unlockedSuccessfully = true
        activeTargetPackage = null
        finish()
    }

    private fun navigateToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.onBiometricSucceeded { finishUnlocked() }
                }
            }
        )

        val appName = viewModel.uiState.value.targetAppName.ifBlank { "Protected App" }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $appName")
            .setSubtitle("Verify identity with biometrics")
            .setNegativeButtonText("Use PIN / Pattern")
            .setConfirmationRequired(false)
            .build()

        runCatching { prompt.authenticate(promptInfo) }
    }

    private fun captureIntruderSelfie(attemptCount: Int, methodLabel: String) {
        val outputFile = viewModel.createPhotoFileIfPermitted()
        if (outputFile == null) {
            viewModel.recordIntruderEvent("", attemptCount, methodLabel)
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        viewModel.recordIntruderEvent("", attemptCount, methodLabel)
                        return@runCatching
                    }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        imageCapture
                    )

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(this),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                cameraProvider.unbindAll()
                                viewModel.recordIntruderEvent(
                                    outputFile.absolutePath,
                                    attemptCount,
                                    methodLabel
                                )
                            }

                            override fun onError(exception: ImageCaptureException) {
                                cameraProvider.unbindAll()
                                viewModel.recordIntruderEvent("", attemptCount, methodLabel)
                            }
                        }
                    )
                }.onFailure {
                    viewModel.recordIntruderEvent("", attemptCount, methodLabel)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    @Composable
    private fun LockOverlayContent(
        state: LockScreenUiState,
        onDigitPress: (String) -> Unit,
        onBackspace: () -> Unit,
        onPatternCompleted: (List<Int>) -> Unit,
        onBiometricClick: () -> Unit,
        onToggleMethod: () -> Unit,
        onDismissFakeCrash: () -> Unit,
        onCloseToHome: () -> Unit,
        onRecoverPin: (String, String) -> Boolean
    ) {
        val glass = LocalGlassColors.current
        var showRecoveryDialog by remember { mutableStateOf(false) }

        if (state.showFakeCrashDecoy) {
            FakeCrashDecoyScreen(
                appName = state.targetAppName,
                onCloseApp = onCloseToHome,
                onRevealRealLock = onDismissFakeCrash
            )
            return
        }

        GlassBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Locked App Identity Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    GlassCard(elevated = true, cornerRadius = 22.dp) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            AppIconImage(
                                drawable = null,
                                contentDescription = state.targetAppName,
                                size = 58.dp,
                                packageName = state.targetPackage.takeIf { !state.isPreview }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = state.targetAppName.ifBlank { "Protected App" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.errorMessage != null) {
                                state.errorMessage
                            } else {
                                "Protected by GlassLock"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.errorMessage != null) glass.danger else glass.textSecondary
                        )
                    }
                }

                // Center Authentication Input (PIN or Pattern)
                AnimatedContent(
                    targetState = state.lockMethod,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "lock_auth_mode"
                ) { method ->
                    if (method == LockMethod.PIN) {
                        GlassPinKeypad(
                            enteredLength = state.enteredPin.length,
                            expectedLength = state.expectedPinLength,
                            isError = state.isError,
                            showBiometricButton = state.biometricEnabled,
                            onDigitPress = onDigitPress,
                            onBackspacePress = onBackspace,
                            onBiometricPress = onBiometricClick
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GlassCard(elevated = true) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    GlassPatternLock(
                                        isError = state.isError,
                                        onPatternCompleted = onPatternCompleted
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
                                    Text(
                                        text = "Use Fingerprint",
                                        color = glass.accentPrimary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Utility Actions: Switch Method / Forgot PIN
                Row(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.hasPattern()) {
                        TextButton(
                            onClick = onToggleMethod,
                            modifier = Modifier.testTag("lock_switch_method")
                        ) {
                            Icon(
                                imageVector = if (state.lockMethod == LockMethod.PIN) {
                                    Icons.Filled.Gesture
                                } else {
                                    Icons.Filled.Pin
                                },
                                contentDescription = "Switch lock method",
                                tint = glass.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.lockMethod == LockMethod.PIN) "Use Pattern" else "Use PIN",
                                color = glass.textSecondary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(
                        onClick = { showRecoveryDialog = true },
                        modifier = Modifier.testTag("lock_forgot_pin")
                    ) {
                        Text(
                            text = "Forgot PIN?",
                            color = glass.accentPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (showRecoveryDialog) {
            RecoveryDialog(
                question = state.securityQuestion,
                onRecover = onRecoverPin,
                onDismiss = { showRecoveryDialog = false }
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun FakeCrashDecoyScreen(
        appName: String,
        onCloseApp: () -> Unit,
        onRevealRealLock: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1F2229),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "System alert",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${appName.ifBlank { "App" }} keeps stopping",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = onRevealRealLock
                            )
                        )
                    }

                    Text(
                        text = "The application has encountered an unexpected error and had to close.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2D313B))
                                .combinedClickable(
                                    onClick = onCloseApp,
                                    onLongClick = onRevealRealLock
                                )
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                                .testTag("fake_crash_app_info"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "App info",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF93C5FD)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2D313B))
                                .combinedClickable(
                                    onClick = onCloseApp,
                                    onLongClick = onRevealRealLock
                                )
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                                .testTag("fake_crash_close_app"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Close app",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFF8FAFC)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RecoveryDialog(
        question: String,
        onRecover: (String, String) -> Boolean,
        onDismiss: () -> Unit
    ) {
        val glass = LocalGlassColors.current
        var answer by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = onDismiss) {
            GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Recover Access",
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.textPrimary
                    )
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyLarge,
                        color = glass.accentPrimary
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = {
                            answer = it
                            error = null
                        },
                        label = { Text("Security Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                newPin = input
                                error = null
                            }
                        },
                        label = { Text("New PIN (4–6 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { err ->
                        Text(text = err, color = glass.danger, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = glass.textSecondary)
                        }
                        Button(
                            onClick = {
                                if (newPin.length !in 4..6) {
                                    error = "Enter 4 to 6 digits for the new PIN."
                                } else if (!onRecover(answer, newPin)) {
                                    error = "Incorrect security answer."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentPrimary,
                                contentColor = Color(0xFF042F2E)
                            )
                        ) {
                            Text("Reset & Unlock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        private const val EXTRA_TARGET_APP_NAME = "extra_target_app_name"
        private const val EXTRA_IS_PREVIEW = "extra_is_preview"

        @Volatile
        private var activeTargetPackage: String? = null

        fun isShowingFor(packageName: String): Boolean = activeTargetPackage == packageName

        fun createIntent(
            context: Context,
            targetPackage: String,
            targetAppName: String,
            isPreview: Boolean = false
        ): Intent {
            return Intent(context, LockScreenActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(EXTRA_TARGET_APP_NAME, targetAppName)
                putExtra(EXTRA_IS_PREVIEW, isPreview)
            }
        }
    }
}
