package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.HiddenRehidePolicy
import com.example.domain.model.LockMethod
import com.example.domain.model.RelockPolicy
import com.example.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.glassLockDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "glasslock_settings"
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settingsFlow: Flow<AppSettings> = context.glassLockDataStore.data.map { prefs ->
        AppSettings(
            lockMethod = prefs[KEY_LOCK_METHOD]
                ?.let { runCatching { LockMethod.valueOf(it) }.getOrNull() }
                ?: LockMethod.PIN,
            biometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: true,
            relockPolicy = prefs[KEY_RELOCK_POLICY]
                ?.let { runCatching { RelockPolicy.valueOf(it) }.getOrNull() }
                ?: RelockPolicy.IMMEDIATE,
            intruderSelfieEnabled = prefs[KEY_INTRUDER_SELFIE] ?: true,
            fakeCrashEnabled = prefs[KEY_FAKE_CRASH] ?: false,
            hideNotificationContent = prefs[KEY_HIDE_NOTIFICATIONS] ?: false,
            isServiceEnabled = prefs[KEY_SERVICE_ENABLED] ?: true,
            themeMode = prefs[KEY_THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            criticalAppsSeeded = prefs[KEY_CRITICAL_APPS_SEEDED] ?: false,
            calculatorDisguiseEnabled = prefs[KEY_CALCULATOR_DISGUISE] ?: false,
            hideHiddenTabInNav = prefs[KEY_HIDE_HIDDEN_TAB] ?: false,
            hiddenRehidePolicy = prefs[KEY_HIDDEN_REHIDE_POLICY]
                ?.let { runCatching { HiddenRehidePolicy.valueOf(it) }.getOrNull() }
                ?: HiddenRehidePolicy.ON_SCREEN_OFF
        )
    }

    suspend fun setLockMethod(method: LockMethod) {
        context.glassLockDataStore.edit { it[KEY_LOCK_METHOD] = method.name }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.glassLockDataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setRelockPolicy(policy: RelockPolicy) {
        context.glassLockDataStore.edit { it[KEY_RELOCK_POLICY] = policy.name }
    }

    suspend fun setIntruderSelfieEnabled(enabled: Boolean) {
        context.glassLockDataStore.edit { it[KEY_INTRUDER_SELFIE] = enabled }
    }

    suspend fun setFakeCrashEnabled(enabled: Boolean) {
        context.glassLockDataStore.edit { it[KEY_FAKE_CRASH] = enabled }
    }

    suspend fun setHideNotificationContent(enabled: Boolean) {
        context.glassLockDataStore.edit { it[KEY_HIDE_NOTIFICATIONS] = enabled }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.glassLockDataStore.edit { it[KEY_SERVICE_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.glassLockDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun markCriticalAppsSeeded() {
        context.glassLockDataStore.edit { it[KEY_CRITICAL_APPS_SEEDED] = true }
    }

    suspend fun setCalculatorDisguiseEnabled(enabled: Boolean) {
        context.glassLockDataStore.edit { it[KEY_CALCULATOR_DISGUISE] = enabled }
    }

    suspend fun setHideHiddenTabInNav(enabled: Boolean) {
        context.glassLockDataStore.edit { it[KEY_HIDE_HIDDEN_TAB] = enabled }
    }

    suspend fun setHiddenRehidePolicy(policy: HiddenRehidePolicy) {
        context.glassLockDataStore.edit { it[KEY_HIDDEN_REHIDE_POLICY] = policy.name }
    }

    companion object {
        private val KEY_LOCK_METHOD = stringPreferencesKey("lock_method")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_RELOCK_POLICY = stringPreferencesKey("relock_policy")
        private val KEY_INTRUDER_SELFIE = booleanPreferencesKey("intruder_selfie_enabled")
        private val KEY_FAKE_CRASH = booleanPreferencesKey("fake_crash_enabled")
        private val KEY_HIDE_NOTIFICATIONS = booleanPreferencesKey("hide_notification_content")
        private val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_CRITICAL_APPS_SEEDED = booleanPreferencesKey("critical_apps_seeded")
        private val KEY_CALCULATOR_DISGUISE = booleanPreferencesKey("calculator_disguise_enabled")
        private val KEY_HIDE_HIDDEN_TAB = booleanPreferencesKey("hide_hidden_tab_in_nav")
        private val KEY_HIDDEN_REHIDE_POLICY = stringPreferencesKey("hidden_rehide_policy")
    }
}
