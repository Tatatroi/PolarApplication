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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.application.polarapplication.polar.PermissionHelper
import com.application.polarapplication.ui.Screen
import com.application.polarapplication.ui.history.HistoryScreen
import com.application.polarapplication.ui.planning.TargetSetupScreen
import com.application.polarapplication.ui.theme.Indigo
import com.application.polarapplication.ui.theme.PolarApplicationTheme
import com.application.polarapplication.ui.theme.dashboard.ActiveWorkoutScreen
import com.application.polarapplication.ui.theme.dashboard.DashboardScreen
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import com.application.polarapplication.ui.theme.dashboard.PeriodizationCalendarScreen
import com.application.polarapplication.ui.theme.devices.DevicesScreen
import com.application.polarapplication.ui.theme.profile.ProfileScreen
import com.application.polarapplication.ui.theme.progress.WorkoutDetailsScreen
import java.time.LocalDate

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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ascundem bara de navigare jos dacă suntem în ecranul de antrenament activ
    val isBottomBarVisible = currentRoute != Screen.ActiveWorkout.route

    // Scaffold este "scheletul" paginii care ne permite să punem bara de jos
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isBottomBarVisible) {
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
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Devices"
                            )
                        },
                        label = { Text("Devices") },
                        selected = currentRoute == Screen.Devices.route,
                        onClick = {
                            navController.navigate(Screen.Devices.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "History"
                            )
                        },
                        label = { Text("Istoric") },
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            navController.navigate(Screen.History.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil") },
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                        label = { Text("Plan") },
                        selected = currentRoute == Screen.PeriodizationCalendar.route
                                || currentRoute == Screen.TargetSetup.route,
                        onClick = {
                            navController.navigate(Screen.TargetSetup.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Aici se face schimbul efectiv între ecrane
        val sharedViewModel: DashboardViewModel = viewModel()
        val currentMaxHr by sharedViewModel.userMaxHr.collectAsState()
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                // Dashboard-ul rămâne la fel
                DashboardScreen(viewModel = sharedViewModel, onMaximizeWorkout = {
                    navController.navigate(Screen.ActiveWorkout.route)
                })
            }

            composable(Screen.Devices.route) {
                DevicesScreen(viewModel = sharedViewModel)
            }

            composable(Screen.History.route) {
                // Obținem ViewModel-ul pentru a accesa sesiunea selectată
                val selectedSession by sharedViewModel.selectedSession.collectAsState()

                if (selectedSession == null) {
                    // 1. Afișăm lista de antrenamente
                    HistoryScreen(
                        viewModel = sharedViewModel,
                        onSessionClick = { session ->
                            // Când dăm click, salvăm sesiunea în ViewModel
                            sharedViewModel.selectSession(session)
                        }
                    )
                } else {
                    // 2. Afișăm ecranul de detalii cu graficul
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Buton de "Înapoi" pentru a reveni la listă
                        TextButton(
                            onClick = { sharedViewModel.selectSession(null) },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("< Înapoi la listă", color = Indigo)
                        }

                        // Ecranul care conține graficul Vico
                        WorkoutDetailsScreen(session = selectedSession!!, maxHr = currentMaxHr)
                    }
                }
            }
            composable(Screen.ActiveWorkout.route) {
                val userGender by sharedViewModel.profileManager.gender.collectAsState()
                ActiveWorkoutScreen(
                    viewModel = sharedViewModel,
                    userGender = userGender,
                    onMinimizeClick = {
                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(viewModel = sharedViewModel)
            }

            composable(Screen.TargetSetup.route) {
                TargetSetupScreen(
                    viewModel = sharedViewModel,   // ← adaugă
                    onPlanGenerated = {
                        navController.navigate(Screen.PeriodizationCalendar.route) {
                            popUpTo(Screen.TargetSetup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.PeriodizationCalendar.route) {
                PeriodizationCalendarScreen(
                    viewModel = sharedViewModel,   // ← adaugă
                    onBack = {
                        navController.navigate(Screen.TargetSetup.route) {
                            popUpTo(Screen.PeriodizationCalendar.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
