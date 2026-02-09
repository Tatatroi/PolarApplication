package com.application.polarapplication.ui.theme.dashboard

import com.application.polarapplication.ai.model.AthleteVitals
import com.application.polarapplication.ai.model.DeviceState

class DashboardUiState (
    val vitals: AthleteVitals = AthleteVitals(),
    val device: DeviceState = DeviceState(),
    val isWorkoutActive: Boolean = false
)
{}