package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CameraFront
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalGlassColors
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val transition = rememberInfiniteTransition(label = "glass_orb_drift")
    val driftX by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_x"
    )
    val driftY by transition.animateFloat(
        initialValue = 16f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = glass.gradientStops,
                    start = Offset.Zero,
                    end = Offset(1200f, 2200f)
                )
            )
    ) {
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(56.dp, BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }

        Box(modifier = Modifier.fillMaxSize().then(blurModifier)) {
            // Top-left Purple Orb
            Box(
                modifier = Modifier
                    .offset(x = (-40).dp + driftX.dp, y = (-30).dp + driftY.dp)
                    .size(280.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glass.orbPrimary.copy(alpha = if (glass.isDark) 0.45f else 0.35f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Center-right Electric Blue Orb
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 60.dp - driftX.dp, y = (-40).dp - driftY.dp)
                    .size(300.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glass.orbSecondary.copy(alpha = if (glass.isDark) 0.42f else 0.32f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Bottom-left Teal Orb
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp + driftX.dp, y = 40.dp - driftY.dp)
                    .size(320.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glass.orbTertiary.copy(alpha = if (glass.isDark) 0.40f else 0.30f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }

        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val fill = if (elevated) glass.cardFillElevated else glass.cardFill
    val borderBrush = Brush.linearGradient(
        colors = listOf(glass.cardBorderStart, glass.cardBorderEnd)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = glass.accentPrimary),
                onClick = onClick
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 16.dp else 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.45f else 0.12f),
                spotColor = glass.orbPrimary.copy(alpha = if (glass.isDark) 0.25f else 0.10f)
            )
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(clickModifier),
        content = content
    )
}

enum class MainNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps, "nav_tab_apps"),
    HIDDEN("Hidden", Icons.Filled.VisibilityOff, Icons.Outlined.VisibilityOff, "nav_tab_hidden"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings"),
    INTRUDERS("Intruders", Icons.Filled.CameraFront, Icons.Outlined.CameraFront, "nav_tab_intruders")
}

@Composable
fun GlassBottomNavBar(
    selectedTab: MainNavTab,
    intruderBadgeCount: Int,
    showHiddenTab: Boolean = true,
    onSelectTab: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(24.dp)
    val visibleTabs = remember(showHiddenTab) {
        if (showHiddenTab) MainNavTab.entries else MainNavTab.entries.filter { it != MainNavTab.HIDDEN }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.45f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(glass.cardBorderStart, glass.cardBorderEnd))
                    ),
                    shape
                ),
            shape = shape,
            color = glass.navBarFill
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleTabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_scale"
                    )
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) glass.accentPrimary else glass.textSecondary,
                        label = "nav_tint"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            glass.accentPrimary.copy(alpha = if (glass.isDark) 0.18f else 0.15f)
                        } else {
                            Color.Transparent
                        },
                        label = "nav_pill"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(18.dp))
                            .background(pillColor)
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 8.dp)
                            .testTag(tab.tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = tint,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (tab == MainNavTab.INTRUDERS && intruderBadgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-3).dp)
                                            .size(8.dp)
                                            .background(glass.danger, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassPinKeypad(
    enteredLength: Int,
    expectedLength: Int,
    isError: Boolean,
    showBiometricButton: Boolean,
    onDigitPress: (String) -> Unit,
    onBackspacePress: () -> Unit,
    onBiometricPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val offsets = listOf(-18f, 18f, -14f, 14f, -8f, 8f, 0f)
            offsets.forEach { target ->
                shakeOffset.animateTo(target, tween(42))
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated PIN indicator dots
        Row(
            modifier = Modifier
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(expectedLength) { index ->
                val filled = index < enteredLength
                val dotScale by animateFloatAsState(
                    targetValue = if (filled) 1.25f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "pin_dot_scale"
                )
                val dotColor by animateColorAsState(
                    targetValue = when {
                        isError -> glass.danger
                        filled -> glass.accentPrimary
                        else -> glass.textSecondary.copy(alpha = 0.35f)
                    },
                    label = "pin_dot_color"
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            width = 1.dp,
                            color = if (filled || isError) dotColor else glass.cardBorderStart,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { digit ->
                        GlassKeypadCircleButton(
                            label = digit,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigitPress(digit)
                            },
                            modifier = Modifier.testTag("pin_key_$digit")
                        )
                    }
                }
            }

            // Bottom row: Biometric / Spacer, 0, Backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBiometricButton) {
                    GlassKeypadIconButton(
                        icon = Icons.Filled.Fingerprint,
                        contentDescription = "Unlock with Biometrics",
                        tint = glass.accentPrimary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBiometricPress()
                        },
                        modifier = Modifier.testTag("pin_key_biometric")
                    )
                } else {
                    Spacer(modifier = Modifier.size(72.dp))
                }

                GlassKeypadCircleButton(
                    label = "0",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigitPress("0")
                    },
                    modifier = Modifier.testTag("pin_key_0")
                )

                GlassKeypadIconButton(
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = glass.textPrimary,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackspacePress()
                    },
                    modifier = Modifier.testTag("pin_key_backspace")
                )
            }
        }
    }
}

@Composable
private fun GlassKeypadCircleButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "key_scale"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(glass.keypadButtonFill)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(glass.cardBorderStart, glass.cardBorderEnd))
                ),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = glass.accentPrimary),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            color = glass.textPrimary
        )
    }
}

@Composable
private fun GlassKeypadIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "icon_key_scale"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(glass.keypadButtonFill.copy(alpha = if (glass.isDark) 0.08f else 0.45f))
            .border(
                BorderStroke(1.dp, glass.cardBorderEnd),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = glass.accentPrimary),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun GlassPatternLock(
    isError: Boolean,
    onPatternCompleted: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    val haptic = LocalHapticFeedback.current
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchOffset by remember { mutableStateOf<Offset?>(null) }

    val lineColor by animateColorAsState(
        targetValue = if (isError) glass.danger else glass.accentPrimary,
        label = "pattern_line_color"
    )

    BoxWithConstraints(
        modifier = modifier
            .size(276.dp)
            .testTag("pattern_lock_grid")
    ) {
        val sizePx = with(LocalDensity.current) { maxWidth.toPx() }
        val step = sizePx / 3f
        val centers = remember(sizePx) {
            (0 until 9).map { idx ->
                val row = idx / 3
                val col = idx % 3
                Offset(col * step + step / 2f, row * step + step / 2f)
            }
        }
        val hitRadius = step * 0.34f

        fun findHitNode(pos: Offset): Int? {
            return centers.indexOfFirst { center ->
                hypot(pos.x - center.x, pos.y - center.y) <= hitRadius
            }.takeIf { it >= 0 }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(centers) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes.clear()
                            currentTouchOffset = offset
                            findHitNode(offset)?.let { idx ->
                                selectedNodes.add(idx)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDrag = { change, _ ->
                            val pos = change.position
                            currentTouchOffset = pos
                            findHitNode(pos)?.let { idx ->
                                if (!selectedNodes.contains(idx)) {
                                    selectedNodes.add(idx)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onDragEnd = {
                            currentTouchOffset = null
                            if (selectedNodes.isNotEmpty()) {
                                val result = selectedNodes.toList()
                                onPatternCompleted(result)
                            }
                            selectedNodes.clear()
                        },
                        onDragCancel = {
                            currentTouchOffset = null
                            selectedNodes.clear()
                        }
                    )
                }
        ) {
            // Draw connecting lines
            if (selectedNodes.isNotEmpty()) {
                val path = Path().apply {
                    val firstCenter = centers[selectedNodes.first()]
                    moveTo(firstCenter.x, firstCenter.y)
                    for (i in 1 until selectedNodes.size) {
                        val c = centers[selectedNodes[i]]
                        lineTo(c.x, c.y)
                    }
                    currentTouchOffset?.let { pointer ->
                        lineTo(pointer.x, pointer.y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor.copy(alpha = 0.85f),
                    style = Stroke(
                        width = 10f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw 9 frosted nodes
            centers.forEachIndexed { idx, center ->
                val isSelected = selectedNodes.contains(idx)
                val nodeFill = when {
                    isSelected && isError -> glass.danger.copy(alpha = 0.28f)
                    isSelected -> glass.accentPrimary.copy(alpha = 0.25f)
                    else -> glass.keypadButtonFill
                }
                val outerBorder = when {
                    isSelected && isError -> glass.danger
                    isSelected -> glass.accentPrimary
                    else -> glass.cardBorderStart
                }
                drawCircle(
                    color = nodeFill,
                    radius = 28.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = outerBorder,
                    radius = 28.dp.toPx(),
                    center = center,
                    style = Stroke(width = if (isSelected) 2.5.dp.toPx() else 1.2.dp.toPx())
                )
                drawCircle(
                    color = if (isSelected) lineColor else glass.textSecondary.copy(alpha = 0.55f),
                    radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

@Composable
fun AppIconImage(
    drawable: Drawable?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    packageName: String? = null
) {
    val glass = LocalGlassColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var resolvedDrawable by remember(packageName, drawable) { mutableStateOf(drawable) }

    LaunchedEffect(packageName, drawable) {
        if (resolvedDrawable == null && !packageName.isNullOrBlank()) {
            resolvedDrawable = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val pm = context.packageManager
                    val flags = android.content.pm.PackageManager.GET_SHARED_LIBRARY_FILES or
                        android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(
                            packageName,
                            android.content.pm.PackageManager.ApplicationInfoFlags.of(flags.toLong())
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(packageName, flags)
                    }
                    if (appInfo.icon == 0) {
                        pm.defaultActivityIcon
                    } else {
                        runCatching {
                            val pkgContext = context.createPackageContext(
                                packageName,
                                android.content.Context.CONTEXT_IGNORE_SECURITY
                            )
                            pkgContext.resources.getDrawable(appInfo.icon, pkgContext.theme)
                        }.getOrNull() ?: pm.getApplicationIcon(appInfo)
                    }
                }.getOrNull()
            }
        }
    }

    val bitmap: ImageBitmap? = remember(resolvedDrawable) {
        resolvedDrawable?.let { d ->
            runCatching {
                val width = d.intrinsicWidth.coerceIn(64, 192)
                val height = d.intrinsicHeight.coerceIn(64, 192)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(bmp)
                d.setBounds(0, 0, canvas.width, canvas.height)
                d.draw(canvas)
                bmp.asImageBitmap()
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(glass.accentSecondary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = contentDescription,
                tint = glass.accentPrimary,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
