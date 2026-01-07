package com.application.polarapplication.ui.theme.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.application.polarapplication.polar.PolarManager
import kotlinx.coroutines.flow.StateFlow

// Use AndroidViewModel to pass the context to PolarManager
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val polarManager = PolarManager(application)

    // Bridge the PolarManager flows to the UI
    val isConnected: StateFlow<Boolean> = polarManager.isConnected
    val heartRate: StateFlow<Int> = polarManager.heartRate

    fun toggleConnection(deviceId: String) {
        // If already connected, you could implement disconnect logic here
        polarManager.connectToDevice(deviceId)
    }
}