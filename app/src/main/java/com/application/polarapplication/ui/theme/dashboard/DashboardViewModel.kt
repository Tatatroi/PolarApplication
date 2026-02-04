package com.application.polarapplication.ui.theme.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.application.polarapplication.polar.PolarManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val polarManager = PolarManager(application)
    val uiState: StateFlow<DashboardUiState> = combine(
        polarManager.deviceState,
        polarManager.athleteVitals
    ) { device, vitals ->
        DashboardUiState(device = device, vitals = vitals)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun toggleConnection(deviceId: String) {
        if (uiState.value.device.isConnected) {
            polarManager.disconnectFromDevice(deviceId)
        } else {
            polarManager.connectToDevice(deviceId)
        }
    }
}