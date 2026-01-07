package com.application.polarapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.application.polarapplication.polar.PermissionHelper
import com.application.polarapplication.ui.theme.PolarApplicationTheme
import com.application.polarapplication.ui.theme.dashboard.DashboardScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable full screen UI
        enableEdgeToEdge()

        // 2. Request permissions immediately on startup
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this)
        }

        setContent {
            PolarApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Load the Dashboard
                    // The ViewModel inside DashboardScreen will handle PolarManager
                    DashboardScreen()
                }
            }
        }
    }
}