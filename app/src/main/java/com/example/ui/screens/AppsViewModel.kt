package com.example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.permissions.PermissionsHelper
import com.example.data.repository.AppLockRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.domain.model.AppFilter
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.PermissionStatus
import com.example.service.AppMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val lockedCount: Int = 0,
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val selectedFilter: AppFilter = AppFilter.ALL,
    val isServiceEnabled: Boolean = true,
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val installedAppsRepository: InstalledAppsRepository,
    private val appLockRepository: AppLockRepository,
    private val settingsDataStore: SettingsDataStore,
    private val permissionsHelper: PermissionsHelper
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(AppFilter.ALL)
    private val permissionStatus = MutableStateFlow(permissionsHelper.getPermissionStatus())
    private val isLoading = MutableStateFlow(true)

    val uiState: StateFlow<AppsUiState> = combine(
        installedAppsRepository.observeApps(),
        searchQuery,
        selectedFilter,
        settingsDataStore.settingsFlow,
        permissionStatus
    ) { allApps, query, filter, settings, perms ->
        val filtered = allApps.filter { app ->
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.LOCKED -> app.isLocked
                AppFilter.SYSTEM -> app.isSystemApp || app.isSystemCritical
            }
            val matchesQuery = query.isBlank() ||
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        AppsUiState(
            apps = filtered,
            lockedCount = allApps.count { it.isLocked },
            totalCount = allApps.size,
            searchQuery = query,
            selectedFilter = filter,
            isServiceEnabled = settings.isServiceEnabled,
            permissionStatus = perms,
            isLoading = isLoading.value && allApps.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppsUiState()
    )

    init {
        viewModelScope.launch {
            installedAppsRepository.refreshInstalledApps()
            installedAppsRepository.seedDefaultCriticalLocksIfNeeded()
            isLoading.value = false
            refreshPermissionsAndService()
        }
    }

    fun refreshPermissionsAndService() {
        val status = permissionsHelper.getPermissionStatus()
        permissionStatus.value = status
        viewModelScope.launch {
            val enabled = uiState.value.isServiceEnabled
            if (enabled && status.usageAccessGranted && status.overlayGranted) {
                AppMonitorService.start(appContext)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(filter: AppFilter) {
        selectedFilter.value = filter
    }

    fun toggleAppLock(app: InstalledAppInfo, lock: Boolean) {
        viewModelScope.launch {
            appLockRepository.setAppLocked(
                packageName = app.packageName,
                appName = app.appName,
                isLocked = lock,
                isSystemCritical = app.isSystemCritical
            )
        }
    }

    fun toggleServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setServiceEnabled(enabled)
            if (enabled) {
                AppMonitorService.start(appContext)
            } else {
                AppMonitorService.stop(appContext)
            }
        }
    }
}
