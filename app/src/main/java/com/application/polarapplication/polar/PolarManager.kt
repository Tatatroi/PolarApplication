package com.application.polarapplication.polar

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PolarManager(context: Context) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _heartRate = MutableStateFlow(0)

    private val _rmssd = MutableStateFlow(0.0)
    val rmssd: StateFlow<Double> = _rmssd.asStateFlow()
    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    private var hrDisposable: Disposable? = null
    private val rrBuffer = mutableListOf<Int>()
    private val maxBufferSize = 100

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            )
        )
    }

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "Connected: ${polarDeviceInfo.deviceId}")
                _isConnected.value = true // CRITICAL: Updates the UI state
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "Disconnected: ${polarDeviceInfo.deviceId}")
                _isConnected.value = false // CRITICAL: Updates the UI state
                _heartRate.value = 0
                hrDisposable?.dispose()
            }

            override fun hrFeatureReady(identifier: String) {
                Log.d("POLAR", "HR feature ready for $identifier")
                hrDisposable = api.startHrStreaming(identifier)
                    .subscribe(
                        { hrData: PolarHrData ->
                            val hr = hrData.samples.last().hr
                            _heartRate.value = hr // CRITICAL: Streams HR to Dashboard

                            val rrList = hrData.samples.flatMap { it.rrsMs }
                            rrBuffer.addAll(rrList)

                            if (rrBuffer.size > maxBufferSize) {
                                rrBuffer.subList(0, rrBuffer.size - maxBufferSize).clear()
                            }

                            if (rrBuffer.size >= 2) {
                                val calculatedRmssd = calculateRmssd(rrBuffer)
                                _rmssd.value = calculatedRmssd
                            }
                        },
                        { error -> Log.e("POLAR_HR", "HR stream error: $error") }
                    )
            }
        })
    }

    fun connectToDevice(deviceId: String) {
        try {
            api.connectToDevice(deviceId)
        } catch (e: Exception) {
            Log.e("POLAR", "Connection error: ${e.message}")
        }
    }

    fun disconnectFromDevice(deviceId: String) {
        try {
            api.disconnectFromDevice(deviceId)
            // Manually update states in case the callback takes a moment
            _isConnected.value = false
            _heartRate.value = 0
            hrDisposable?.dispose()
        } catch (e: Exception) {
            Log.e("POLAR", "Disconnect error: ${e.message}")
        }
    }

    private fun calculateRmssd(rrList: List<Int>): Double {
        if (rrList.size < 2) return 0.0
        val diffs = rrList.zipWithNext { a, b ->
            val diff = (b - a).toDouble()
            diff * diff
        }
        return kotlin.math.sqrt(diffs.average())
    }
}