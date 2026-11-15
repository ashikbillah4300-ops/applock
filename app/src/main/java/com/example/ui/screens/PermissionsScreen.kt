package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassColors

@Composable
fun PermissionsScreen(
    isFromSettings: Boolean,
    onContinue: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    val glass = LocalGlassColors.current
    var oemExpanded by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshStatus()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshStatus()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshStatus()
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshStatus()
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
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isFromSettings) {
                        IconButton(
                            onClick = onContinue,
                            modifier = Modifier.testTag("permissions_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = glass.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shield Permissions",
                            style = MaterialTheme.typography.headlineMedium,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Grant these system accesses so GlassLock can guard apps reliably.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                PermissionRowCard(
                    stepLabel = "STEP 1 • REQUIRED",
                    title = "Usage Access",
                    description = "Detects which app opens in the foreground so locked apps trigger immediately.",
                    icon = Icons.Filled.QueryStats,
                    granted = status.usageAccessGranted,
                    testTag = "perm_usage_access",
                    onGrantClick = {
                        runCatching { context.startActivity(viewModel.usageAccessIntent()) }
                    }
                )

                PermissionRowCard(
                    stepLabel = "STEP 2 • REQUIRED",
                    title = "Display Over Other Apps",
                    description = "Allows the full-screen glass lock overlay to appear on top of protected apps.",
                    icon = Icons.Filled.Layers,
                    granted = status.overlayGranted,
                    testTag = "perm_overlay",
                    onGrantClick = {
                        runCatching { context.startActivity(viewModel.overlayIntent()) }
                    }
                )

                PermissionRowCard(
                    stepLabel = "STEP 3 • REQUIRED",
                    title = "Notifications",
                    description = "Shows a silent low-priority service badge so Android keeps protection alive.",
                    icon = Icons.Filled.Notifications,
                    granted = status.notificationsGranted,
                    testTag = "perm_notifications",
                    onGrantClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            runCatching { context.startActivity(viewModel.notificationSettingsIntent()) }
                        }
                    }
                )

                PermissionRowCard(
                    stepLabel = "STEP 4 • REQUIRED",
                    title = "Battery Optimization Exemption",
                    description = "Prevents Doze and OEM killers from stopping background app detection.",
                    icon = Icons.Filled.BatteryChargingFull,
                    granted = status.batteryOptimizationIgnored,
                    testTag = "perm_battery",
                    onGrantClick = {
                        runCatching { context.startActivity(viewModel.batteryOptimizationIntent()) }
                    }
                )

                Text(
                    text = "EXTRA HARDENING (OPTIONAL)",
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.accentPrimary,
                    modifier = Modifier.padding(top = 6.dp)
                )

                PermissionRowCard(
                    stepLabel = "ANTI-UNINSTALL",
                    title = "Device Administrator",
                    description = "Blocks casual uninstallation of GlassLock without disabling admin first.",
                    icon = Icons.Filled.AdminPanelSettings,
                    granted = status.deviceAdminActive,
                    testTag = "perm_device_admin",
                    onGrantClick = {
                        deviceAdminLauncher.launch(viewModel.deviceAdminIntent())
                    }
                )

                PermissionRowCard(
                    stepLabel = "INTRUDER SELFIE",
                    title = "Front Camera Access",
                    description = "Captures an intruder selfie locally after 3 consecutive failed attempts.",
                    icon = Icons.Filled.CameraFront,
                    granted = status.cameraGranted,
                    testTag = "perm_camera",
                    onGrantClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                // OEM Battery-Killer Guide
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { oemExpanded = !oemExpanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OEM Background Protection Tips",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "Xiaomi / MIUI, Samsung One UI, Oppo / Realme",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary
                                )
                            }
                            Icon(
                                imageVector = if (oemExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle OEM guide",
                                tint = glass.textSecondary
                            )
                        }

                        AnimatedVisibility(visible = oemExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OemTipRow(
                                    brand = "Xiaomi / MIUI / HyperOS",
                                    steps = "Enable 'Autostart' in Security -> Manage Apps -> GlassLock, and set Battery Saver to 'No restrictions'."
                                )
                                OemTipRow(
                                    brand = "Samsung One UI",
                                    steps = "Go to Device Care -> Battery -> Background usage limits -> Never sleeping apps and add GlassLock."
                                )
                                OemTipRow(
                                    brand = "Oppo / Realme / OnePlus",
                                    steps = "Open App Info -> Battery usage -> Allow background activity and Allow auto-launch, then lock the app card in Recents."
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        viewModel.refreshStatus()
                        onContinue()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status.allCoreGranted) glass.accentPrimary else glass.accentSecondary,
                        contentColor = if (status.allCoreGranted) Color(0xFF042F2E) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("permissions_continue_button")
                ) {
                    Text(
                        text = when {
                            isFromSettings -> "Done"
                            status.allCoreGranted -> "Activate GlassLock Shield"
                            else -> "Continue Anyway (Grant Later)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    stepLabel: String,
    title: String,
    description: String,
    icon: ImageVector,
    granted: Boolean,
    testTag: String,
    onGrantClick: () -> Unit
) {
    val glass = LocalGlassColors.current
    GlassCard(
        elevated = !granted,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        (if (granted) glass.success else glass.accentPrimary).copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (granted) glass.success else glass.accentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) glass.success else glass.accentSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (granted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = glass.success.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Granted",
                            tint = glass.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                TextButton(
                    onClick = onGrantClick,
                    modifier = Modifier.testTag(testTag)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Action needed",
                            tint = glass.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Grant",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.accentPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OemTipRow(brand: String, steps: String) {
    val glass = LocalGlassColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(glass.keypadButtonFill.copy(alpha = 0.45f))
            .padding(12.dp)
    ) {
        Text(
            text = brand,
            style = MaterialTheme.typography.labelLarge,
            color = glass.accentPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = steps,
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textSecondary
        )
    }
}
