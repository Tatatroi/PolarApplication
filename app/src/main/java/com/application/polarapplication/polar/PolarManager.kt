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
    private val rrBuffer = mutableListOf<Int>()
    private val maxBufferSize = 100   // last 100 heart beats (~90–100 sec)

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

            // implementation without RMSSD
//            override fun hrFeatureReady(identifier: String) {
//                Log.d("POLAR", "HR feature ready for $identifier")
//
//                hrDisposable = api.startHrStreaming(identifier)
//                    .subscribe(
//                        { hrData: PolarHrData ->
//                            val hr = hrData.samples.last().hr
//                            Log.d("POLAR_HR", "Heart Rate: $hr bpm")
//                        },
//                        { error ->
//                            Log.e("POLAR_HR", "HR stream error: $error")
//                        }
//                    )
//            }

            override fun hrFeatureReady(identifier: String) {
                Log.d("POLAR", "HR feature ready for $identifier")

                hrDisposable = api.startHrStreaming(identifier)
                    .subscribe(
                        { hrData: PolarHrData ->

                            // HR (BPM)
                            val hr = hrData.samples.last().hr
                            Log.d("POLAR_HR", "HR: $hr bpm")

                            // RR-intervalele primite în acest pachet
                            val rrList = hrData.samples.flatMap { it.rrsMs }
                            Log.d("POLAR_RR", "RR incoming: $rrList")

                            // Adaugăm noile RR-uri în buffer
                            rrBuffer.addAll(rrList)

                            // Păstrăm doar ultimele 100 RR (sau cât vrei)
                            if (rrBuffer.size > maxBufferSize) {
                                rrBuffer.subList(0, rrBuffer.size - maxBufferSize).clear()
                            }

                            Log.d("POLAR_RR_BUFFER", "RR Buffer: $rrBuffer")

                            //  Calculăm RMSSD pe întreg bufferul, nu pe pachetul instant
                            if (rrBuffer.size >= 2) {
                                val rmssd = calculateRmssd(rrBuffer)
                                Log.d("POLAR_RMSSD", "RMSSD (buffer): $rmssd")
                            }

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

    private fun calculateRmssd(rrList: List<Int>): Double {
        if (rrList.size < 2) return 0.0

        val diffs = rrList.zipWithNext { a, b ->
            val diff = (b - a).toDouble()
            diff * diff
        }

        val mean = diffs.sum() / diffs.size
        return kotlin.math.sqrt(mean)
    }



}
