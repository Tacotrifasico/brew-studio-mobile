package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.V60Icon
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcViewModel

class MainActivity : ComponentActivity() {
    private val baristaViewModel: BaristaCalcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Full Edge-to-Edge immersion support
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                BrewStudioAppShell(viewModel = baristaViewModel)
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Home : Screen("home", "Taller", Icons.Filled.Home, Icons.Outlined.Home)
    object Brew : Screen("brew", "Preparar", V60Icon, V60Icon)
    class CataScreenConfig : Screen("cata", "Cata", Icons.Filled.Favorite, Icons.Outlined.Favorite)
    object Lab : Screen("lab", "Laboratorio", Icons.Filled.Science, Icons.Outlined.Science)
    object Storage : Screen("storage", "Almacen", Icons.Filled.Inventory, Icons.Outlined.Inventory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewStudioAppShell(viewModel: BaristaCalcViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val calcState by viewModel.state.collectAsState()
    
    // Listen for events to show in Snackbar
    LaunchedEffect(calcState.snackbarMessage) {
        calcState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    val navigationItems = listOf(
        Screen.Home,
        Screen.Brew,
        Screen.CataScreenConfig(),
        Screen.Lab,
        Screen.Storage
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = TextPrincipal,
                    contentColor = MainBackground,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(12.dp)
                )
            }
        },
        bottomBar = {
            // Elegant M3 custom bottom navigation bar respecting gesture insets
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(1.dp, BordeSuave, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars),
                windowInsets = WindowInsets(0, 0, 0, 0) // manual handling via custom padding
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navigationItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.activeIcon else screen.inactiveIcon,
                                contentDescription = screen.title,
                                tint = if (isSelected) AcentoPrincipal else TextSecundario,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = AcentoSuave
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing // Prevent content underflow under top notches/status bars
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSection = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Brew.route) {
                BrewScreen(viewModel = viewModel)
            }
            composable(Screen.CataScreenConfig().route) {
                CataScreen(
                    viewModel = viewModel,
                    onNavigateToSection = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Lab.route) {
                LabScreen(
                    viewModel = viewModel,
                    onNavigateToSection = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Storage.route) {
                StorageScreen(viewModel = viewModel)
            }
            composable("social") {
                val socialViewModel: com.example.ui.viewmodel.SocialViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                com.example.ui.user.UserScreen(viewModel = socialViewModel, onBack = { navController.popBackStack() })
            }

        }
    }
}
