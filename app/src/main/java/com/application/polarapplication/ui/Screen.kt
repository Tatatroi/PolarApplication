package com.application.polarapplication.ui

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Devices : Screen("devices", "Senzori", Icons.Default.Settings) // NOU
    object History : Screen("history", "Istoric", Icons.AutoMirrored.Filled.List)
}