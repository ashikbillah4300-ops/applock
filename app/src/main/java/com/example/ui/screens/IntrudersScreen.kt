package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.IntruderAttempt
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntrudersScreen(
    bottomContentPadding: PaddingValues,
    viewModel: IntrudersViewModel = hiltViewModel()
) {
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()
    val glass = LocalGlassColors.current
    var selectedAttempt by remember { mutableStateOf<IntruderAttempt?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault())
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

            GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
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
                            .background(glass.danger.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraFront,
                            contentDescription = "Intruder Snapshots",
                            tint = glass.danger,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Intruder Log",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "${attempts.size} captured attempt${if (attempts.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                    }
                    if (attempts.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("intruders_clear_all")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear all intruder logs",
                                tint = glass.danger
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (attempts.isEmpty()) {
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
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(glass.success.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = "No intruders",
                                    tint = glass.success,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "All Clear — No Intruders",
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "After 3 consecutive failed unlock attempts, GlassLock silently snaps a front-camera selfie and logs it here.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(attempts, key = { it.id }) { attempt ->
                        IntruderAttemptCard(
                            attempt = attempt,
                            formattedTime = dateFormatter.format(Date(attempt.timestamp)),
                            onClick = { selectedAttempt = attempt },
                            onDelete = { viewModel.deleteAttempt(attempt) }
                        )
                    }
                }
            }
        }
    }

    selectedAttempt?.let { attempt ->
        Dialog(onDismissRequest = { selectedAttempt = null }) {
            GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attempt.appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary
                            )
                            Text(
                                text = dateFormatter.format(Date(attempt.timestamp)),
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.textSecondary
                            )
                        }
                        IconButton(onClick = { selectedAttempt = null }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close preview",
                                tint = glass.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val photoFile = File(attempt.photoPath)
                    if (attempt.photoPath.isNotBlank() && photoFile.exists()) {
                        AsyncImage(
                            model = photoFile,
                            contentDescription = "Intruder snapshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(glass.keypadButtonFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Photo unavailable (camera permission or hardware missing)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${attempt.attemptCount} failed ${attempt.authMethodUsed} attempts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.danger
                        )
                        TextButton(
                            onClick = {
                                viewModel.deleteAttempt(attempt)
                                selectedAttempt = null
                            }
                        ) {
                            Text("Delete", color = glass.danger)
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Intruder Log?") },
            text = { Text("This deletes all recorded failed-attempt logs and local selfie snapshots.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear All", color = glass.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun IntruderAttemptCard(
    attempt: IntruderAttempt,
    formattedTime: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = LocalGlassColors.current
    val photoFile = remember(attempt.photoPath) {
        attempt.photoPath.takeIf { it.isNotBlank() }?.let { File(it) }
    }

    GlassCard(
        elevated = true,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("intruder_card_${attempt.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photoFile != null && photoFile.exists()) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = "Intruder photo for ${attempt.appName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(glass.danger.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraFront,
                        contentDescription = "Attempt logged",
                        tint = glass.danger,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = glass.danger.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${attempt.attemptCount} wrong ${attempt.authMethodUsed} tries",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.danger,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete log entry",
                    tint = glass.textSecondary
                )
            }
        }
    }
}
