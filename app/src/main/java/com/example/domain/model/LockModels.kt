package com.example.domain.model

import android.graphics.drawable.Drawable

enum class LockMethod(val label: String) {
    PIN("PIN Code"),
    PATTERN("Pattern")
}

enum class RelockPolicy(val label: String, val description: String) {
    IMMEDIATE("Immediately", "Re-lock as soon as you leave the app"),
    ONE_MINUTE("After 1 min", "Stay unlocked for 60 seconds after leaving"),
    ON_SCREEN_OFF("On screen off", "Stay unlocked until your screen turns off")
}

enum class HiddenRehidePolicy(val label: String, val durationMillis: Long) {
    ON_SCREEN_OFF("On screen off", 0L),
    TWO_MINUTES("After 2 min", 2 * 60_000L),
    FIVE_MINUTES("After 5 min", 5 * 60_000L),
    FIFTEEN_MINUTES("After 15 min", 15 * 60_000L)
}

enum class ThemeMode(val label: String) {
    DARK("Dark Glass"),
    LIGHT("Light Glass"),
    SYSTEM("System")
}

enum class AppFilter(val label: String) {
    ALL("All"),
    LOCKED("Locked"),
    SYSTEM("System")
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean,
    val isSystemApp: Boolean,
    val isSystemCritical: Boolean,
    val icon: Drawable? = null
)

data class HiddenAppInfo(
    val packageName: String,
    val appName: String,
    val hiddenAt: Long,
    val isTemporarilyLaunched: Boolean = false,
    val launchExpiresAt: Long = 0L,
    val isSystemDisabled: Boolean = true
)

data class AppSettings(
    val lockMethod: LockMethod = LockMethod.PIN,
    val biometricEnabled: Boolean = true,
    val relockPolicy: RelockPolicy = RelockPolicy.IMMEDIATE,
    val intruderSelfieEnabled: Boolean = true,
    val fakeCrashEnabled: Boolean = false,
    val isServiceEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val criticalAppsSeeded: Boolean = false,
    val calculatorDisguiseEnabled: Boolean = false,
    val hideHiddenTabInNav: Boolean = false,
    val hiddenRehidePolicy: HiddenRehidePolicy = HiddenRehidePolicy.ON_SCREEN_OFF
)

data class PermissionStatus(
    val usageAccessGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val deviceAdminActive: Boolean = false,
    val cameraGranted: Boolean = false
) {
    val allCoreGranted: Boolean
        get() = usageAccessGranted && overlayGranted && notificationsGranted && batteryOptimizationIgnored
}

data class IntruderAttempt(
    val id: Long,
    val packageName: String,
    val appName: String,
    val photoPath: String,
    val attemptCount: Int,
    val timestamp: Long,
    val authMethodUsed: String
)
