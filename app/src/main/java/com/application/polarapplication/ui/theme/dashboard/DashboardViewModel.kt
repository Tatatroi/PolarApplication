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

    val rmssd: StateFlow<Double> = polarManager.rmssd

    fun toggleConnection(deviceId: String) {
        if (isConnected.value) {
            polarManager.disconnectFromDevice(deviceId)
        } else {
            polarManager.connectToDevice(deviceId)
        }
    }
}