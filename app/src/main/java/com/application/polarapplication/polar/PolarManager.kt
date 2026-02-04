package com.application.polarapplication.polar

import android.content.Context
import android.util.Log
import com.application.polarapplication.ai.model.AthleteVitals
import com.application.polarapplication.ai.model.DeviceState
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarOhrPPIData
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class PolarManager(context: Context) {

    /* ================== STATE ================== */

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState = _deviceState.asStateFlow()

    private val _athleteVitals = MutableStateFlow(AthleteVitals())
    val athleteVitals = _athleteVitals.asStateFlow()


    /* ================== HR FILTER ================== */

    private var lastHr: Double? = null
    private var lastTimestamp: Long? = null

    private val maxDeltaPerSec = 20.0


    /* ================== HRV ================== */

    private val rrBuffer = mutableListOf<Double>()
    private val maxRrSize = 60


    private var ppiDisposable: Disposable? = null


    /* ================== POLAR API ================== */

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            )
        )
    }


    init {
        api.setApiCallback(object : PolarBleApiCallback() {

            override fun deviceConnected(info: PolarDeviceInfo) {
                _deviceState.value = DeviceState(
                    isConnected = true,
                    deviceId = info.deviceId
                )
            }

            override fun streamingFeaturesReady(
                id: String,
                features: Set<PolarBleApi.PolarDeviceDataType>
            ) {
                if (features.contains(PolarBleApi.PolarDeviceDataType.PPI)) {
                    startPpiStreaming(id)
                }
            }

            override fun deviceDisconnected(info: PolarDeviceInfo) {
                reset()
            }

            override fun batteryLevelReceived(id: String, level: Int) {
                _deviceState.value =
                    _deviceState.value.copy(batteryLevel = level)
            }
        })
    }


    /* ================== STREAM ================== */

    @Suppress("DEPRECATION")
    private fun startPpiStreaming(deviceId: String) {

        ppiDisposable =
            api.startOhrPPIStreaming(deviceId)
                .subscribe({ data ->

                    val now = System.currentTimeMillis()

                    val dt = if (lastTimestamp == null) {
                        1.0
                    } else {
                        (now - lastTimestamp!!) / 1000.0
                    }.coerceIn(0.3, 2.0)

                    lastTimestamp = now


                    for (sample in data.samples) {

                        /* --------- Quality filter --------- */

                        if (sample.skinContactSupported &&
                            !sample.skinContactStatus
                        ) continue


                        /* --------- RR interval --------- */

                        val rr = sample.ppi.toDouble()

                        if (rr < 300 || rr > 1500) continue


                        /* --------- Outlier RR filter --------- */

                        val lastRr = rrBuffer.lastOrNull()

                        if (lastRr != null &&
                            abs(rr - lastRr) > 300
                        ) continue


                        /* --------- HR raw --------- */

                        val rawHr = 60000.0 / rr


                        /* --------- HRV buffer --------- */

                        rrBuffer.add(rr)

                        if (rrBuffer.size > maxRrSize)
                            rrBuffer.removeAt(0)

                        val rmssd =
                            if (rrBuffer.size >= 10)
                                calcRMSSD(rrBuffer)
                            else 0.0


                        /* --------- Adaptive alpha --------- */

                        val diff =
                            if (lastHr != null)
                                rawHr - lastHr!!
                            else 0.0

                        val isOutlier = abs(diff) > 25

                        val alpha = when {
                            isOutlier -> 0.05
                            abs(diff) > 10 -> 0.35
                            rmssd < 20 -> 0.30
                            rmssd < 40 -> 0.20
                            else -> 0.12
                        }


                        /* --------- EMA --------- */

                        val target = if (lastHr == null)
                            rawHr
                        else
                            alpha * rawHr +
                                    (1 - alpha) * lastHr!!


                        /* --------- Rate limiter --------- */

                        val maxStep = maxDeltaPerSec * dt

                        val delta =
                            (target - (lastHr ?: target))
                                .coerceIn(-maxStep, maxStep)

                        val smooth =
                            (lastHr ?: target) + delta


                        lastHr = smooth


                        /* --------- UI --------- */

                        val displayHr = smooth.toInt()

                        val zone = calcZone(displayHr, 200)

                        _athleteVitals.value =
                            AthleteVitals(
                                heartRate = displayHr,
                                rmssd = rmssd,
                                trainingZone = zone
                            )
                    }

                }, { err ->

                    Log.e("POLAR", err.toString())

                })
    }


    /* ================== CONTROL ================== */

    fun connectToDevice(id: String) {
        api.connectToDevice(id)
    }

    fun disconnectFromDevice(id: String) {
        api.disconnectFromDevice(id)
        reset()
    }

    private fun reset() {

        lastHr = null
        lastTimestamp = null
        rrBuffer.clear()

        ppiDisposable?.dispose()

        _deviceState.value = DeviceState(false)
        _athleteVitals.value = AthleteVitals()
    }


    /* ================== UTILS ================== */

    private fun calcRMSSD(rr: List<Double>): Double {

        val diffs =
            rr.zipWithNext { a, b ->
                val d = b - a
                d * d
            }

        return sqrt(diffs.average())
    }

    private fun calcZone(hr: Int, max: Int): Int {

        val p = hr.toDouble() / max * 100

        return when {
            p < 60 -> 1
            p < 70 -> 2
            p < 80 -> 3
            p < 90 -> 4
            else -> 5
        }
    }
}
