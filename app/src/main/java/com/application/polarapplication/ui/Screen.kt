package com.application.polarapplication.ui

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object History : Screen("history", "Istoric")
}