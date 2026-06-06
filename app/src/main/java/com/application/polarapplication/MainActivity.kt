package com.application.polarapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.application.polarapplication.ai.chatbot.ChatBotScreen
import com.application.polarapplication.athletic.InitialTestScreen
import com.application.polarapplication.athletic.ProgressScreen
import com.application.polarapplication.polar.PermissionHelper
import com.application.polarapplication.services.WorkoutForegroundService
import com.application.polarapplication.ui.Screen
import com.application.polarapplication.ui.history.HistoryScreen
import com.application.polarapplication.ui.onboarding.OnboardingManager
import com.application.polarapplication.ui.onboarding.OnboardingScreen
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val navigateToLive = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1002)
            }
        }

        enableEdgeToEdge()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            ))
        }

        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this)
        }

        handleNavigateToLiveIntent(intent)

        setContent {
            PolarApplicationTheme {
                MainNavigationWrapper(navigateToLive = navigateToLive)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigateToLiveIntent(intent)
    }

    private fun handleNavigateToLiveIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(WorkoutForegroundService.EXTRA_NAVIGATE_TO_LIVE, false) == true) {
            navigateToLive.value = true
        }
    }
}

@Composable
fun MainNavigationWrapper(navigateToLive: MutableStateFlow<Boolean>) {
    val navController   = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute    = navBackStackEntry?.destination?.route
    val context         = LocalContext.current
    val onboardingManager = remember { OnboardingManager(context) }
    var showOnboarding  by remember { mutableStateOf(!onboardingManager.isCompleted) }
    val sharedViewModel: DashboardViewModel = viewModel()

    val shouldNavigateToLive by navigateToLive.collectAsState()
    LaunchedEffect(shouldNavigateToLive) {
        if (shouldNavigateToLive) {
            navigateToLive.value = false
            navController.navigate(Screen.ActiveWorkout.route) { launchSingleTop = true }
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            onComplete = { name, age, gender, height, weight, dobMillis ->
                sharedViewModel.saveUserProfile(
                    age             = age,
                    weight          = weight,
                    height          = height,
                    gender          = gender,
                    rhr             = 55,
                    customHrMax     = null,
                    profileImageUri = null,
                    dobMillis       = dobMillis,
                    availableDays   = emptySet(),
                    userName        = name
                )
                onboardingManager.markCompleted()
                showOnboarding = false
            }
        )
        return
    }

    val isBottomBarVisible = currentRoute != Screen.ActiveWorkout.route
    val showFab = currentRoute != Screen.AiChat.route &&
            currentRoute != Screen.ActiveWorkout.route

    // ── Starea poziției FAB — persistă cât timp e în memorie ─────────────────
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Scaffold fără FAB ─────────────────────────────────────────────────
        Scaffold(
            modifier  = Modifier.fillMaxSize(),
            bottomBar = {
                if (isBottomBarVisible) {
                    NavigationBar(
                        containerColor = Color(0xFF0D0D14),
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            icon     = { Icon(Icons.Default.Home, null) },
                            label    = { Text("Home") },
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick  = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            colors = navItemColors()
                        )
                        NavigationBarItem(
                            icon     = { Icon(Icons.Default.Sensors, null) },
                            label    = { Text("Devices") },
                            selected = currentRoute == Screen.Devices.route,
                            onClick  = { navController.navigate(Screen.Devices.route) { launchSingleTop = true } },
                            colors   = navItemColors()
                        )
                        NavigationBarItem(
                            icon     = { Icon(Icons.Default.History, null) },
                            label    = { Text("History") },
                            selected = currentRoute == Screen.History.route,
                            onClick  = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                            colors   = navItemColors()
                        )
                        NavigationBarItem(
                            icon     = { Icon(Icons.Default.Person, null) },
                            label    = { Text("Profile") },
                            selected = currentRoute == Screen.Profile.route,
                            onClick  = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } },
                            colors   = navItemColors()
                        )
                        NavigationBarItem(
                            icon     = { Icon(Icons.Default.DateRange, null) },
                            label    = { Text("Plan") },
                            selected = currentRoute == Screen.Plan.route ||
                                    currentRoute == Screen.TargetSetup.route ||
                                    currentRoute == Screen.PeriodizationCalendar.route,
                            onClick  = {
                                val hasCompetitionDate = sharedViewModel.competitionDate.value != null
                                if (hasCompetitionDate) {
                                    navController.navigate(Screen.Plan.route) { launchSingleTop = true }
                                } else {
                                    navController.navigate(Screen.TargetSetup.route) { launchSingleTop = true }
                                }
                            },
                            colors = navItemColors()
                        )
                    }
                }
            }
        ) { innerPadding ->
            val currentMaxHr by sharedViewModel.userMaxHr.collectAsState()

            NavHost(
                navController    = navController,
                startDestination = Screen.Dashboard.route,
                modifier         = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel         = sharedViewModel,
                        onMaximizeWorkout = { navController.navigate(Screen.ActiveWorkout.route) },
                        onNavigateToTest  = { navController.navigate(Screen.InitialTest.route) }
                    )
                }
                composable(Screen.Devices.route) {
                    DevicesScreen(viewModel = sharedViewModel)
                }
                composable(Screen.History.route) {
                    val selectedSession by sharedViewModel.selectedSession.collectAsState()
                    if (selectedSession == null) {
                        HistoryScreen(
                            viewModel             = sharedViewModel,
                            onSessionClick        = { session -> sharedViewModel.selectSession(session) },
                            onNavigateToProgress  = { navController.navigate(Screen.Progress.route) }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Indigo.copy(alpha = 0.08f))
                                    .border(1.dp, Indigo.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .clickable { sharedViewModel.selectSession(null) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBackIosNew,
                                        contentDescription = null,
                                        tint = Indigo,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Back to History",
                                        color = Indigo,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            WorkoutDetailsScreen(session = selectedSession!!, maxHr = currentMaxHr)
                        }
                    }
                }
                composable(
                    route      = Screen.ActiveWorkout.route,
                    deepLinks  = listOf(navDeepLink { uriPattern = "polar://active_workout" })
                ) {
                    val userGender by sharedViewModel.profileManager.gender.collectAsState()
                    ActiveWorkoutScreen(
                        viewModel       = sharedViewModel,
                        userGender      = userGender,
                        onMinimizeClick = {
                            navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                        }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel        = sharedViewModel,
                        onNavigateToTest = { navController.navigate(Screen.InitialTest.route) }
                    )
                }
                composable(Screen.TargetSetup.route) {
                    TargetSetupScreen(
                        viewModel        = sharedViewModel,
                        onPlanGenerated  = {
                            navController.navigate(Screen.PeriodizationCalendar.route) {
                                popUpTo(Screen.TargetSetup.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.PeriodizationCalendar.route) {
                    PeriodizationCalendarScreen(
                        viewModel = sharedViewModel,
                        onBack    = {
                            navController.navigate(Screen.TargetSetup.route) {
                                popUpTo(Screen.PeriodizationCalendar.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Plan.route) {
                    ActivePlanScreen(
                        viewModel          = sharedViewModel,
                        onGenerateNewPlan  = { navController.navigate(Screen.TargetSetup.route) }
                    )
                }
                composable(Screen.AiChat.route) {
                    ChatBotScreen(viewModel = sharedViewModel)
                }
                composable(Screen.InitialTest.route) {
                    InitialTestScreen(
                        viewModel       = sharedViewModel,
                        athleticMgr     = sharedViewModel.athleticProfileManager,
                        onTestComplete  = { navController.popBackStack() }
                    )
                }
                composable(Screen.Progress.route) {
                    ProgressScreen(
                        viewModel = sharedViewModel,
                        onBack    = { navController.popBackStack() }
                    )
                }
                composable(Screen.Plan.route) {
                    ActivePlanScreen(
                        viewModel         = sharedViewModel,
                        onGenerateNewPlan = { navController.navigate(Screen.TargetSetup.route) },
                        onViewCalendar    = { navController.navigate(Screen.PeriodizationCalendar.route) }  // <-- adaugă asta
                    )
                }
            }
        }

        // ── FAB DRAGGABLE — deasupra Scaffold ────────────────────────────────
        if (showFab) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Padding bottom = înălțimea navbar + spațiu
                    .padding(bottom = 90.dp, end = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                fabOffsetX += dragAmount.x
                                fabOffsetY += dragAmount.y
                            }
                        }
                ) {
                    FloatingActionButton(
                        onClick        = {
                            navController.navigate(Screen.AiChat.route) { launchSingleTop = true }
                        },
                        containerColor = Color(0xFF1A1A2E),
                        contentColor   = Color(0xFF818CF8),
                        shape          = RoundedCornerShape(16.dp)
                    ) {
                        Image(
                            painter            = painterResource(id = R.drawable.chatbotimage),
                            contentDescription = "AI Chat",
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// HELPER — culori navbar item
// ─────────────────────────────────────────────
@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor   = Color(0xFF818CF8),
    selectedTextColor   = Color(0xFF818CF8),
    unselectedIconColor = Color(0xFF555566),
    unselectedTextColor = Color(0xFF555566),
    indicatorColor      = Color(0xFF818CF8).copy(alpha = 0.12f)
)