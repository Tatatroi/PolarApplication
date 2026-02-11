package com.application.polarapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.application.polarapplication.polar.PermissionHelper
import com.application.polarapplication.ui.Screen
import com.application.polarapplication.ui.history.HistoryScreen
import com.application.polarapplication.ui.theme.PolarApplicationTheme
import com.application.polarapplication.ui.theme.dashboard.DashboardScreen
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ui.theme.Indigo
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import com.application.polarapplication.ui.theme.devices.DevicesScreen
import com.application.polarapplication.ui.theme.progress.WorkoutDetailsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this)
        }

        setContent {
            PolarApplicationTheme {
                // Chemăm wrapper-ul de navigare în loc de un singur ecran
                MainNavigationWrapper()
            }
        }
    }
}

@Composable
fun MainNavigationWrapper() {
    val navController = rememberNavController()

    // Scaffold este "scheletul" paginii care ne permite să punem bara de jos
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Buton Dashboard
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == Screen.Dashboard.route,
                    onClick = {
                        navController.navigate(Screen.Dashboard.route) {
                            // Evită acumularea de ecrane în spate
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Devices") },
                    label = { Text("Devices") },
                    selected = currentRoute == Screen.Devices.route,
                    onClick = {
                        navController.navigate(Screen.Devices.route) {
                            launchSingleTop = true
                        }
                    }
                )

                // Buton Istoric
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                    label = { Text("Istoric") },
                    selected = currentRoute == Screen.History.route,
                    onClick = {
                        navController.navigate(Screen.History.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Aici se face schimbul efectiv între ecrane
        val sharedViewModel: DashboardViewModel = viewModel()
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                // Dashboard-ul rămâne la fel
                DashboardScreen(viewModel = sharedViewModel)
            }

            composable(Screen.Devices.route) {
                DevicesScreen(viewModel = sharedViewModel)
            }

            composable(Screen.History.route) {
                // Obținem ViewModel-ul pentru a accesa sesiunea selectată
                val dashboardViewModel: DashboardViewModel = viewModel()
                val selectedSession by dashboardViewModel.selectedSession.collectAsState()

                if (selectedSession == null) {
                    // 1. Afișăm lista de antrenamente
                    HistoryScreen(
                        viewModel = dashboardViewModel,
                        onSessionClick = { session ->
                            // Când dăm click, salvăm sesiunea în ViewModel
                            dashboardViewModel.selectSession(session)
                        }
                    )
                } else {
                    // 2. Afișăm ecranul de detalii cu graficul
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Buton de "Înapoi" pentru a reveni la listă
                        TextButton(
                            onClick = { dashboardViewModel.selectSession(null) },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("< Înapoi la listă", color = Indigo)
                        }

                        // Ecranul care conține graficul Vico
                        WorkoutDetailsScreen(session = selectedSession!!)
                    }
                }
            }
        }
    }
}