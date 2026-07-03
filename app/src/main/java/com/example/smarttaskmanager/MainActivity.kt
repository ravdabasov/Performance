package com.example.smarttaskmanager

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smarttaskmanager.presentation.navigation.Screen
import com.example.smarttaskmanager.presentation.navigation.SmartTaskNavGraph
import com.example.smarttaskmanager.presentation.screen.auth.LoginScreen
import com.example.smarttaskmanager.presentation.theme.SmartTaskManagerTheme
import com.example.smarttaskmanager.presentation.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* nəticə lazım deyil */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SmartTaskManagerTheme {
                SmartTaskManagerRoot()
            }
        }
    }
}

private data class BottomNavItem(val screenRoute: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, "Ana səhifə", Icons.Filled.Home),
    BottomNavItem(Screen.TaskList.route, "Tasklar", Icons.Filled.FormatListBulleted),
    BottomNavItem(Screen.Completed.route, "Tamamlanan", Icons.Filled.CheckCircle),
    BottomNavItem(Screen.Statistics.route, "Statistika", Icons.Filled.BarChart),
    BottomNavItem(Screen.ChatList.route, "Söhbətlər", Icons.AutoMirrored.Filled.Chat)
)

/**
 * Tətbiqin girişi: əvvəlcə Firebase Auth vəziyyəti yoxlanılır.
 * Daxil olmayıbsa → LoginScreen, daxil olubsa → əsas tətbiq (bottom nav + FAB).
 */
@Composable
fun SmartTaskManagerRoot(authViewModel: AuthViewModel = hiltViewModel()) {
    val authState by authViewModel.authState.collectAsState()

    when {
        authState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        authState.user == null -> {
            LoginScreen(onAuthenticated = { })
        }
        else -> {
            MainAppScaffold()
        }
    }
}

@Composable
private fun MainAppScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Bütün top-level tab keçidləri (bottom nav VƏ Dashboard-dakı "Bütün taskları göstər"
    // düyməsi) eyni funksiyadan keçir ki, "Ana səhifə" tabı HƏMİŞƏ düzgün işləsin.
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.screenRoute,
                        onClick = { navigateToTab(item.screenRoute) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            // Tələb: "+" düyməsi yalnız Tasklar və Tamamlanan bölmələrində görünsün -
            // Ana səhifə, Statistika, Söhbətlər tab-larında görünməməlidir.
            val fabVisibleRoutes = setOf(Screen.TaskList.route, Screen.Completed.route)
            if (currentRoute in fabVisibleRoutes) {
                ExtendedFloatingActionButton(
                    text = { Text("Yeni task yarat") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = { navController.navigate(Screen.AddTask.route) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            SmartTaskNavGraph(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                onNavigateToTab = ::navigateToTab
            )
        }
    }
}
