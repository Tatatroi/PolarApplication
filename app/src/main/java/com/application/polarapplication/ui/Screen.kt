package com.application.polarapplication.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Devices : Screen("devices", "Senzori", Icons.Default.Settings)
    object History : Screen("history", "Istoric", Icons.AutoMirrored.Filled.List)

    object Profile : Screen("profile", "Profil", Icons.Default.Settings)

    object ActiveWorkout : Screen("active_workout", "Antrenament", Icons.Default.PlayArrow)

    object PeriodizationCalendar : Screen("periodization", "Calendar", Icons.Default.DateRange)

    object TargetSetup : Screen("target_setup", "Setup", Icons.Default.DateRange)

    object ActivePlan : Screen("active_plan", "Plan", Icons.Default.DateRange)
}
