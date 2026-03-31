package com.application.polarapplication.ai.model

data class DeviceState(
    val isConnected: Boolean = false,
    val batteryLevel: Int = 0,
    val deviceId: String = ""
)
