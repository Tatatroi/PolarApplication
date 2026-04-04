package com.application.polarapplication

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.application.polarapplication.ai.chatbot.ChatBotScreen
import com.application.polarapplication.polar.PermissionHelper
import com.application.polarapplication.ui.Screen
import com.application.polarapplication.ui.history.HistoryScreen
import com.application.polarapplication.ui.planning.ActivePlanScreen
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1002
                )
            }
        }

        enableEdgeToEdge()

        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this)
        }

        setContent {
            PolarApplicationTheme {
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

    val isBottomBarVisible = currentRoute != Screen.ActiveWorkout.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Sensors, contentDescription = "Devices") },
                        label = { Text("Devices") },
                        selected = currentRoute == Screen.Devices.route,
                        onClick = {
                            navController.navigate(Screen.Devices.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            navController.navigate(Screen.History.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Plan") },
                        label = { Text("Plan") },
                        selected = currentRoute == Screen.Plan.route ||
                                currentRoute == Screen.TargetSetup.route,
                        onClick = {
                            navController.navigate(Screen.Plan.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute != Screen.AiChat.route &&
                currentRoute != Screen.ActiveWorkout.route
            ) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.AiChat.route) {
                            launchSingleTop = true
                        }
                    },
                    containerColor = Color(0xFF1A1A2E),
                    contentColor = Color(0xFF818CF8),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chatbotimage),
                        contentDescription = "Asistent AI",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        val sharedViewModel: DashboardViewModel = viewModel()
        val currentMaxHr by sharedViewModel.userMaxHr.collectAsState()
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = sharedViewModel, onMaximizeWorkout = {
                    navController.navigate(Screen.ActiveWorkout.route)
                })
            }

            composable(Screen.Devices.route) {
                DevicesScreen(viewModel = sharedViewModel)
            }

            composable(Screen.History.route) {
                val selectedSession by sharedViewModel.selectedSession.collectAsState()

                if (selectedSession == null) {
                    HistoryScreen(
                        viewModel = sharedViewModel,
                        onSessionClick = { session ->
                            sharedViewModel.selectSession(session)
                        }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TextButton(
                            onClick = { sharedViewModel.selectSession(null) },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("< Înapoi la listă", color = Indigo)
                        }
                        WorkoutDetailsScreen(session = selectedSession!!, maxHr = currentMaxHr)
                    }
                }
            }
            composable(
                route = Screen.ActiveWorkout.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "polar://active_workout" }
                )
            ) {
                val userGender by sharedViewModel.profileManager.gender.collectAsState()
                ActiveWorkoutScreen(
                    viewModel      = sharedViewModel,
                    userGender     = userGender,
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
                    viewModel = sharedViewModel,
                    onPlanGenerated = {
                        navController.navigate(Screen.PeriodizationCalendar.route) {
                            popUpTo(Screen.TargetSetup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.PeriodizationCalendar.route) {
                PeriodizationCalendarScreen(
                    viewModel = sharedViewModel,
                    onBack = {
                        navController.navigate(Screen.TargetSetup.route) {
                            popUpTo(Screen.PeriodizationCalendar.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Plan.route) {
                ActivePlanScreen(
                    viewModel         = sharedViewModel,
                    onGenerateNewPlan = {
                        navController.navigate(Screen.TargetSetup.route)
                    }
                )
            }

            composable(Screen.AiChat.route) {
                ChatBotScreen(viewModel = sharedViewModel)
            }
        }
    }
}
