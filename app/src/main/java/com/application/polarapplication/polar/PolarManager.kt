package com.application.polarapplication.polar

import android.content.Context
import android.util.Log
import com.application.polarapplication.ai.analysis.StressDataStream
import com.application.polarapplication.ai.analysis.StressManager
import com.application.polarapplication.ai.model.AthleteVitals
import com.application.polarapplication.ai.model.DeviceState
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class PolarManager(context: Context) {

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState = _deviceState.asStateFlow()

    private val _athleteVitals = MutableStateFlow(AthleteVitals())
    val athleteVitals = _athleteVitals.asStateFlow()

    private var accumulatedTrimp = 0.0
    private var accumulatedCalories = 0.0

    private var scanDisposable: Disposable? = null
    private val _availableDevices = MutableStateFlow<Set<PolarDeviceInfo>>(emptySet())
    val availableDevices = _availableDevices.asStateFlow()

    // FIX: 5 secunde — suficient să acopere gap-urile dar fără lag mare
    private val hrCalculationWindowMs = 5000L
    private val rrTimestampBuffer = mutableListOf<Pair<Long, Double>>()

    private val rrMadBuffer = mutableListOf<Double>()
    private val madWindowSize = 15

    private val MIN_RR_MS = 300.0
    private val MAX_RR_MS = 1500.0
    private val MAX_HR_CHANGE_BPM_PER_SEC = 25.0

    private var lastHr: Double? = null
    private var lastTimestamp: Long? = null

    private var latestAccMagnitude: Double = 0.0

    private val stressManager = StressManager()
    private val stressDataStream = StressDataStream(stressManager)

    private val rrBuffer = mutableListOf<Double>()
    private val maxRrSize = 60

    private var ppiDisposable: Disposable? = null
    private var accDisposable: Disposable? = null

    // Samples pentru grafic — un sample la fiecare 5 secunde
    private val workoutHeartRateSamples = mutableListOf<Int>()
    private var lastSampleTimestamp = 0L
    private val MIN_SAMPLE_INTERVAL_MS = 4500L

    var userMaxHr: Int = 200

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
                _deviceState.value = DeviceState(isConnected = true, deviceId = info.deviceId)
            }
            override fun streamingFeaturesReady(id: String, features: Set<PolarBleApi.PolarDeviceDataType>) {
                if (features.contains(PolarBleApi.PolarDeviceDataType.PPI)) startPpiStreaming(id)
                if (features.contains(PolarBleApi.PolarDeviceDataType.ACC)) startAccStreaming(id)
            }
            override fun deviceDisconnected(info: PolarDeviceInfo) { reset() }
            override fun batteryLevelReceived(id: String, level: Int) {
                _deviceState.value = _deviceState.value.copy(batteryLevel = level)
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun startPpiStreaming(deviceId: String) {
        ppiDisposable = api.startOhrPPIStreaming(deviceId)
            .subscribe({ data ->
                val now = System.currentTimeMillis()
                val batchSize = data.samples.size

                val dt = if (lastTimestamp == null) {
                    1.0
                } else {
                    ((now - lastTimestamp!!) / 1000.0).coerceIn(0.1, 5.0)
                }
                lastTimestamp = now

                data.samples.forEachIndexed { index, sample ->
                    if (sample.skinContactSupported && !sample.skinContactStatus) return@forEachIndexed
                    val rr = sample.ppi.toDouble()
                    if (rr < MIN_RR_MS || rr > MAX_RR_MS) return@forEachIndexed

                    val sampleTime = now - (
                            (batchSize - 1 - index) * (dt * 1000.0 / batchSize)
                            ).toLong()

                    rrBuffer.add(rr)
                    if (rrBuffer.size > maxRrSize) rrBuffer.removeAt(0)

                    if (!isOutlierMAD(rr)) {
                        addRrToBuffer(sampleTime, rr)
                    }
                }

                val windowHr = calculateHrFromWindow()
                val currentHr = when {
                    windowHr != null -> windowHr
                    lastHr != null -> lastHr!!
                    else -> return@subscribe
                }

                val displayHr = applyRateLimit(currentHr, dt)
                val rmssd = calculateTemporalRMSSD()

                updateMetrics(displayHr, rmssd, dt)

                stressDataStream.onNewDataReceived(
                    bvp = rrTimestampBuffer.lastOrNull()?.second ?: rr2bvpProxy(),
                    acc = latestAccMagnitude
                )
            },
                { err -> android.util.Log.e("PPI_STREAM", "Stream ERROR: ${err.message}") },
                { android.util.Log.d("PPI_STREAM", "Stream COMPLETED") }
            )
    }

    private fun isOutlierMAD(rr: Double): Boolean {
        if (rrMadBuffer.size < 5) {
            rrMadBuffer.add(rr)
            return false
        }
        val sorted = rrMadBuffer.sorted()
        val median = sorted[sorted.size / 2]
        val mad = rrMadBuffer.map { abs(it - median) }.sorted()
            .let { it[it.size / 2] }.coerceAtLeast(10.0)
        val isOutlier = abs(rr - median) > 3.0 * mad
        rrMadBuffer.add(rr)
        if (rrMadBuffer.size > madWindowSize) rrMadBuffer.removeAt(0)
        return isOutlier
    }

    private fun addRrToBuffer(timestamp: Long, rr: Double) {
        rrTimestampBuffer.add(timestamp to rr)
        val cutoff = timestamp - hrCalculationWindowMs
        rrTimestampBuffer.removeAll { it.first < cutoff }
    }

    private fun calculateHrFromWindow(): Double? {
        if (rrTimestampBuffer.size < 2) return null
        val meanRr = rrTimestampBuffer.map { it.second }.average()
        return 60000.0 / meanRr
    }

    private fun applyRateLimit(newHr: Double, dt: Double): Int {
        val motionFactor = when {
            latestAccMagnitude > 3000 -> 0.2  // mișcare foarte bruscă (rotire mână, săritură)
            latestAccMagnitude > 2000 -> 0.4  // mișcare mare
            latestAccMagnitude > 1500 -> 0.7  // alergat/mișcare medie
            else                      -> 1.0  // stând/mers liniștit (~1050)
        }

        val maxChange = MAX_HR_CHANGE_BPM_PER_SEC * dt.coerceAtMost(2.0) * motionFactor

        val limitedHr = if (lastHr == null) {
            newHr
        } else {
            val delta = (newHr - lastHr!!).coerceIn(-maxChange, maxChange)
            lastHr!! + delta
        }
        lastHr = limitedHr
        return limitedHr.toInt()
    }

    private fun calculateTemporalRMSSD(): Double {
        val cutoff = System.currentTimeMillis() - 30_000L
        val recentRrs = rrTimestampBuffer.filter { it.first > cutoff }.map { it.second }
        if (recentRrs.size < 3) return 0.0
        val diffs = recentRrs.zipWithNext { a, b -> (b - a) * (b - a) }
        return sqrt(diffs.average())
    }

    private fun updateMetrics(hr: Int, rmssd: Double, dt: Double) {
        val zone = calcZone(hr, userMaxHr)
        val timePerSampleMin = dt / 60.0

        val cnsRaw = if (rmssd > 5.0 && rmssd < 150.0) {
            (kotlin.math.ln(rmssd) * 20.0).coerceIn(0.0, 100.0)
        } else 0.0
        val cnsScore = cnsRaw.toInt()

        accumulatedTrimp += zone * timePerSampleMin
        accumulatedCalories += hr * timePerSampleMin * 0.12

        val now = System.currentTimeMillis()
        if (now - lastSampleTimestamp >= MIN_SAMPLE_INTERVAL_MS) {
            workoutHeartRateSamples.add(hr)
            lastSampleTimestamp = now
            android.util.Log.d("HR_SAMPLES", "Sample added: $hr BPM, total=${workoutHeartRateSamples.size}")
        }

        _athleteVitals.value = AthleteVitals(
            heartRate = hr,
            rmssd = rmssd,
            trainingZone = zone,
            cnsScore = cnsScore,
            trimpScore = accumulatedTrimp,
            calories = accumulatedCalories.toInt(),
            stressScore = stressDataStream.currentStressScore,
            stressLevel = stressDataStream.currentStressLevel
        )
    }

    private fun rr2bvpProxy(): Double = rrTimestampBuffer.lastOrNull()?.second ?: 0.0

    private fun startAccStreaming(deviceId: String) {
        accDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
            .flatMapPublisher { settings -> api.startAccStreaming(deviceId, settings.maxSettings()) }
            .subscribe(
                { polarAccData ->
                    for (sample in polarAccData.samples) {
                        val x = sample.x.toDouble()
                        val y = sample.y.toDouble()
                        val z = sample.z.toDouble()
                        latestAccMagnitude = sqrt(x * x + y * y + z * z)
                        android.util.Log.d("ACC_MAG", "Magnitude: $latestAccMagnitude")
                    }
                },
                { error -> Log.e("POLAR_ACC", "ACC stream error: $error") }
            )
    }

    fun startScan() {
        try {
            _availableDevices.value = emptySet()
            scanDisposable = api.searchForDevice()
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(
                    { info ->
                        val current = _availableDevices.value.toMutableSet()
                        current.add(info)
                        _availableDevices.value = current
                    },
                    { err -> Log.e("POLAR", "Scan error: ${err.message}") }
                )
        } catch (e: SecurityException) {
            Log.e("POLAR", "Missing BT permissions: ${e.message}")
        }
    }

    fun stopScan() { scanDisposable?.dispose(); scanDisposable = null }
    fun connectToDevice(id: String) { api.connectToDevice(id) }
    fun disconnectFromDevice(id: String) { api.disconnectFromDevice(id); reset() }

    fun getHrSamples(): List<Int> {
        android.util.Log.d("HR_SAMPLES", "getHrSamples called, size=${workoutHeartRateSamples.size}")
        return workoutHeartRateSamples.toList()
    }

    fun prepareNewWorkout() {
        accumulatedTrimp = 0.0
        accumulatedCalories = 0.0
        workoutHeartRateSamples.clear()
        lastSampleTimestamp = 0L
        lastHr = null
        lastTimestamp = null
        rrTimestampBuffer.clear()
    }

    private fun reset() {
        lastHr = null
        lastTimestamp = null
        rrBuffer.clear()
        rrTimestampBuffer.clear()
        rrMadBuffer.clear()
        ppiDisposable?.dispose()
        accDisposable?.dispose()
        ppiDisposable = null
        accDisposable = null
        _deviceState.value = DeviceState(false)
        _athleteVitals.value = AthleteVitals()
        accumulatedTrimp = 0.0
        accumulatedCalories = 0.0
        workoutHeartRateSamples.clear()
        lastSampleTimestamp = 0L
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