package com.application.polarapplication.polar

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.disposables.Disposable

class PolarManager(context: Context) {

    private var hrDisposable: Disposable? = null

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            )
        )
    }

    init {
        api.setApiCallback(object : PolarBleApiCallback() {

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "Connected: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "Disconnected: ${polarDeviceInfo.deviceId}")
                hrDisposable?.dispose()
            }

            override fun hrFeatureReady(identifier: String) {
                Log.d("POLAR", "HR feature ready for $identifier")

                hrDisposable = api.startHrStreaming(identifier)
                    .subscribe(
                        { hrData: PolarHrData ->
                            val hr = hrData.samples.last().hr
                            Log.d("POLAR_HR", "Heart Rate: $hr bpm")
                        },
                        { error ->
                            Log.e("POLAR_HR", "HR stream error: $error")
                        }
                    )
            }
        })
    }

    fun connectToDevice(deviceId: String) {
        api.connectToDevice(deviceId)
    }

    fun disconnectFromDevice(deviceId: String) {
        api.disconnectFromDevice(deviceId)
    }
}
