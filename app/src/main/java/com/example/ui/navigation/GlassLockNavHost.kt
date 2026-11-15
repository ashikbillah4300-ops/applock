package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassBottomNavBar
import com.example.ui.components.MainNavTab
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.HiddenScreen
import com.example.ui.screens.IntrudersScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupScreen
import kotlinx.serialization.Serializable

@Serializable
data object SetupRoute

@Serializable
data class PermissionsRoute(val fromSettings: Boolean = false)

@Serializable
data object MainRoute

@Composable
fun GlassLockNavHost(
    isSetupComplete: Boolean,
    allCorePermissionsGranted: Boolean,
    intruderCount: Int,
    openHiddenTrigger: Int = 0,
    hideHiddenTabInNav: Boolean = false,
    onSetupFinished: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination: Any = when {
        !isSetupComplete -> SetupRoute
        !allCorePermissionsGranted -> PermissionsRoute(fromSettings = false)
        else -> MainRoute
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<SetupRoute> {
            SetupScreen(
                onSetupComplete = {
                    onSetupFinished()
                    navController.navigate(PermissionsRoute(fromSettings = false)) {
                        popUpTo<SetupRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<PermissionsRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<PermissionsRoute>()
            PermissionsScreen(
                isFromSettings = args.fromSettings,
                onContinue = {
                    if (args.fromSettings) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(MainRoute) {
                            popUpTo<PermissionsRoute> { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<MainRoute> {
            MainShellScreen(
                intruderCount = intruderCount,
                openHiddenTrigger = openHiddenTrigger,
                hideHiddenTabInNav = hideHiddenTabInNav,
                onOpenPermissions = {
                    navController.navigate(PermissionsRoute(fromSettings = true))
                }
            )
        }
    }
}

@Composable
private fun MainShellScreen(
    intruderCount: Int,
    openHiddenTrigger: Int,
    hideHiddenTabInNav: Boolean,
    onOpenPermissions: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainNavTab.APPS) }

    BackHandler(enabled = selectedTab != MainNavTab.APPS) {
        selectedTab = MainNavTab.APPS
    }

    LaunchedEffect(openHiddenTrigger) {
        if (openHiddenTrigger > 0) {
            selectedTab = MainNavTab.HIDDEN
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassBottomNavBar(
                    selectedTab = selectedTab,
                    intruderBadgeCount = intruderCount,
                    showHiddenTab = !hideHiddenTabInNav,
                    onSelectTab = { selectedTab = it }
                )
            }
        ) { innerPadding ->
            val bottomPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 4.dp)
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    MainNavTab.APPS -> AppsScreen(
                        onOpenPermissions = onOpenPermissions,
                        onOpenHiddenVault = { selectedTab = MainNavTab.HIDDEN },
                        bottomContentPadding = bottomPadding
                    )
                    MainNavTab.HIDDEN -> HiddenScreen(
                        bottomContentPadding = bottomPadding
                    )
                    MainNavTab.SETTINGS -> SettingsScreen(
                        onOpenPermissions = onOpenPermissions,
                        bottomContentPadding = bottomPadding
                    )
                    MainNavTab.INTRUDERS -> IntrudersScreen(
                        bottomContentPadding = bottomPadding
                    )
                }
            }
        }
    }
}
