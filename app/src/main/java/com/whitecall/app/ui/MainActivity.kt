package com.whitecall.app.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.whitecall.app.R
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.ui.components.OnboardingWizardDialog
import com.whitecall.app.ui.history.BlockedLogScreen
import com.whitecall.app.ui.navigation.NavDestination
import com.whitecall.app.ui.settings.SettingsScreen
import com.whitecall.app.ui.theme.StatusActive
import com.whitecall.app.ui.theme.StatusInactive
import com.whitecall.app.ui.theme.StatusScheduled
import com.whitecall.app.ui.theme.WhiteCallTheme
import com.whitecall.app.ui.whitelist.WhiteListScreen
import com.whitecall.app.util.LocaleHelper
import com.whitecall.app.util.PermissionHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WhiteCallApplication
        LocaleHelper.applyLanguage(app.preferences.appLanguage)

        setContent {
            val appTheme by app.preferences.appThemeFlow.collectAsState()
            val appLanguage by app.preferences.appLanguageFlow.collectAsState()

            // Reactively update locale when changed in settings
            LaunchedEffect(appLanguage) {
                LocaleHelper.applyLanguage(appLanguage)
            }

            val darkTheme = when (appTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            WhiteCallTheme(darkTheme = darkTheme) {
                var showOnboarding by remember {
                    mutableStateOf(!app.preferences.isOnboardingCompleted && !PermissionHelper.isCallScreeningRoleHeld(this@MainActivity))
                }

                MainScreen(app = app)

                if (showOnboarding) {
                    OnboardingWizardDialog(
                        onDismiss = {
                            app.preferences.isOnboardingCompleted = true
                            showOnboarding = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(app: WhiteCallApplication) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isProtectionEnabled by app.preferences.protectionEnabledFlow.collectAsState()
    val scheduleSettings by app.preferences.scheduleSettingsFlow.collectAsState()

    val isProtectionActive = if (scheduleSettings.isEnabled) {
        scheduleSettings.isScheduleActive()
    } else {
        isProtectionEnabled
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Status Pill
                    val (statusColor, statusText) = when {
                        isProtectionActive && scheduleSettings.isEnabled ->
                            Pair(StatusScheduled, stringResource(R.string.protection_status_scheduled))
                        isProtectionActive ->
                            Pair(StatusActive, stringResource(R.string.protection_status_active))
                        else ->
                            Pair(StatusInactive, stringResource(R.string.protection_status_inactive))
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavDestination.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.iconRes),
                                contentDescription = stringResource(screen.titleRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(screen.titleRes),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.WhiteList.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavDestination.WhiteList.route) {
                WhiteListScreen()
            }
            composable(NavDestination.BlockedLog.route) {
                BlockedLogScreen()
            }
            composable(NavDestination.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
