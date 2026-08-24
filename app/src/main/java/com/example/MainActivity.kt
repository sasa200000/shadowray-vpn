package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.VpnState
import com.example.ui.screens.AppFilterScreen
import com.example.ui.screens.ConfigsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShadowRayTheme
import com.example.ui.theme.StatusConnecting
import com.example.ui.viewmodel.VpnViewModel
import com.example.util.LocalizationHelper

sealed class Screen(val route: String, val titleKey: String, val icon: ImageVector) {
    object Home : Screen("home", "nav_home", Icons.Default.Shield)
    object Configs : Screen("configs", "nav_configs", Icons.Default.ListAlt)
    object Subscriptions : Screen("subscriptions", "nav_subscriptions", Icons.Default.RssFeed)
    object Settings : Screen("settings", "nav_settings", Icons.Default.Settings)
    object Logs : Screen("logs", "nav_logs", Icons.Default.Article)
    object AppFilter : Screen("app_filter", "settings_app_filter", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleConnect(this)
        } else {
            Toast.makeText(this, "VPN permission is required to create tunnel", Toast.LENGTH_SHORT).show()
        }
    }

    fun prepareVpnAndConnect() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            viewModel.toggleConnect(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val vpnState by viewModel.vpnState.collectAsState()
            val allConfigs by viewModel.allConfigs.collectAsState()

            ShadowRayTheme(themeMode = settings.themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val navItems = listOf(
                    Screen.Home,
                    Screen.Configs,
                    Screen.Subscriptions,
                    Screen.Settings,
                    Screen.Logs
                )

                val showBottomNav = currentRoute != Screen.AppFilter.route

                Scaffold(
                    topBar = {
                        if (showBottomNav) {
                            ShadowRayTopBar(
                                vpnState = vpnState,
                                language = settings.language
                            )
                        }
                    },
                    bottomBar = {
                        if (showBottomNav) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    .testTag("bottom_nav_bar")
                            ) {
                                navItems.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    val title = LocalizationHelper.getString(screen.titleKey, settings.language)

                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Home.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            if (screen == Screen.Configs && allConfigs.isNotEmpty()) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = NeonCyan,
                                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                                        ) {
                                                            Text(
                                                                text = allConfigs.size.toString(),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(screen.icon, contentDescription = title)
                                                }
                                            } else {
                                                Icon(screen.icon, contentDescription = title)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonCyan,
                                            selectedTextColor = NeonCyan,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = NeonCyan.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_${screen.route}")
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToConfigs = {
                                    navController.navigate(Screen.Configs.route)
                                }
                            )
                        }

                        composable(Screen.Configs.route) {
                            ConfigsScreen(viewModel = viewModel)
                        }

                        composable(Screen.Subscriptions.route) {
                            SubscriptionsScreen(viewModel = viewModel)
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToAppFilter = {
                                    navController.navigate(Screen.AppFilter.route)
                                }
                            )
                        }

                        composable(Screen.Logs.route) {
                            LogsScreen(viewModel = viewModel)
                        }

                        composable(Screen.AppFilter.route) {
                            AppFilterScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowRayTopBar(
    vpnState: VpnState,
    language: com.example.model.AppLanguage
) {
    val isConnected = vpnState is VpnState.Connected
    val isConnecting = vpnState is VpnState.Connecting

    val statusDotColor = when {
        isConnected -> ElectricEmerald
        isConnecting -> StatusConnecting
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan, ElectricEmerald)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF04111E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = LocalizationHelper.getString("app_name", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            // Live Status Indicator Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusDotColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = when {
                            isConnected -> LocalizationHelper.getString("status_connected", language)
                            isConnecting -> LocalizationHelper.getString("status_connecting", language)
                            else -> LocalizationHelper.getString("status_disconnected", language)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = statusDotColor
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
