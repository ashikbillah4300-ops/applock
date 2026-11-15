package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppFilter
import com.example.domain.model.InstalledAppInfo
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassColors

import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AppsScreen(
    onOpenPermissions: () -> Unit,
    onOpenHiddenVault: () -> Unit,
    bottomContentPadding: PaddingValues,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalGlassColors.current
    val haptic = LocalHapticFeedback.current
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastLogoTapMs by remember { mutableLongStateOf(0L) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionsAndService()
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

            // Protection Shield Header Card
            GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                (if (state.isServiceEnabled) glass.accentPrimary else glass.textSecondary)
                                    .copy(alpha = 0.16f)
                            )
                            .clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastLogoTapMs > 2500L) {
                                    logoTapCount = 1
                                } else {
                                    logoTapCount++
                                }
                                lastLogoTapMs = now
                                if (logoTapCount >= 5) {
                                    logoTapCount = 0
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenHiddenVault()
                                }
                            }
                            .testTag("apps_shield_logo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "GlassLock Shield",
                            tint = if (state.isServiceEnabled) glass.accentPrimary else glass.textSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GlassLock",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary
                        )
                        Text(
                            text = if (state.isServiceEnabled) {
                                "${state.lockedCount} of ${state.totalCount} apps guarded"
                            } else {
                                "Protection paused"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.isServiceEnabled) glass.accentPrimary else glass.warning
                        )
                    }

                    Switch(
                        checked = state.isServiceEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleServiceEnabled(it)
                        },
                        modifier = Modifier.testTag("apps_service_toggle")
                    )
                }
            }

            // Missing core permissions banner
            AnimatedVisibility(visible = !state.permissionStatus.allCoreGranted) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    onClick = onOpenPermissions
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Permissions needed",
                            tint = glass.warning,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup incomplete",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.warning
                            )
                            Text(
                                text = "Tap to grant Usage Access, Overlay, or Battery exemption",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frosted Search Bar
            GlassCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search installed apps...",
                            color = glass.textSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = glass.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = glass.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppFilter.entries.forEach { filter ->
                    val selected = state.selectedFilter == filter
                    Surface(
                        onClick = { viewModel.onFilterSelected(filter) },
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
                        modifier = Modifier.testTag("apps_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = when (filter) {
                                AppFilter.ALL -> "All (${state.totalCount})"
                                AppFilter.LOCKED -> "Locked (${state.lockedCount})"
                                AppFilter.SYSTEM -> "System"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) glass.accentPrimary else glass.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = glass.accentPrimary)
                    }
                }

                state.apps.isEmpty() -> {
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
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = "No apps",
                                    tint = glass.accentSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No matching apps found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "Try clearing your search or switching back to All.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 2.dp,
                            bottom = bottomContentPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.apps,
                            key = { it.packageName }
                        ) { app ->
                            AppLockRowItem(
                                app = app,
                                onToggle = { locked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.toggleAppLock(app, locked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockRowItem(
    app: InstalledAppInfo,
    onToggle: (Boolean) -> Unit
) {
    val glass = LocalGlassColors.current
    GlassCard(
        elevated = app.isLocked,
        onClick = { onToggle(!app.isLocked) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_card_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                drawable = app.icon,
                contentDescription = app.appName,
                size = 44.dp,
                packageName = app.packageName
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isSystemCritical) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = glass.accentSecondary.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "PROTECT",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (app.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = if (app.isLocked) "Locked" else "Unlocked",
                tint = if (app.isLocked) glass.accentPrimary else glass.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = app.isLocked,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("toggle_app_${app.packageName}")
            )
        }
    }
}
