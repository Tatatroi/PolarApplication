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

    private var accumulatedTrimp = 0.0
    private var accumulatedCalories = 0.0

    // Timpul ultimei actualizări pentru integrare (calcul TRIMP)
    private var lastCalculationTime = 0L

    private var scanDisposable: Disposable? = null
    private val _availableDevices = MutableStateFlow<Set<PolarDeviceInfo>>(emptySet())
    val availableDevices = _availableDevices.asStateFlow()


    /* ================== HR FILTER ================== */

    private var lastHr: Double? = null
    private var lastTimestamp: Long? = null

    private val maxDeltaPerSec = 20.0


    /* ================== HRV ================== */

    private val rrBuffer = mutableListOf<Double>()
    private val maxRrSize = 60

    private var ppiDisposable: Disposable? = null

    private val workoutHeartRateSamples = mutableListOf<Int>()

    private var lastSampleTimestamp = 0L


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

        ppiDisposable = api.startOhrPPIStreaming(deviceId)
            .subscribe({ data ->

                val now = System.currentTimeMillis()

                // 1. Calculăm timpul scurs de la ultimul pachet (în secunde)
                val dt = if (lastTimestamp == null) {
                    1.0
                } else {
                    (now - lastTimestamp!!) / 1000.0
                }.coerceIn(0.1, 2.0) // Limităm la max 2 secunde să nu avem salturi uriașe

                lastTimestamp = now

                // Împărțim timpul la numărul de mostre din acest pachet
                // Astfel, TRIMP-ul și Caloriile se adună corect, bucată cu bucată
                val batchSize = data.samples.size
                val timePerSampleSec = if (batchSize > 0) dt / batchSize else 0.0
                val timePerSampleMin = timePerSampleSec / 60.0 // Convertim în minute pentru TRIMP

                for (sample in data.samples) {

                    /* --------- Quality filter --------- */
                    // Dacă senzorul zice că nu face contact, sărim
                    if (sample.skinContactSupported && !sample.skinContactStatus) continue

                    /* --------- RR interval --------- */
                    val rr = sample.ppi.toDouble()

                    // Filtru limite biologice (300ms = 200bpm, 1500ms = 40bpm)
                    if (rr < 300 || rr > 1500) continue

                    /* --------- Outlier RR filter --------- */
                    // Dacă sare brusc cu 300ms față de ultima bătaie, e eroare
                    val lastRr = rrBuffer.lastOrNull()
                    if (lastRr != null && abs(rr - lastRr) > 300) continue

                    /* --------- HR raw --------- */
                    val rawHr = 60000.0 / rr

                    /* --------- HRV buffer --------- */
                    rrBuffer.add(rr)
                    if (rrBuffer.size > maxRrSize) rrBuffer.removeAt(0)

                    val rmssd = if (rrBuffer.size >= 10) calcRMSSD(rrBuffer) else 0.0

                    /* --------- Adaptive alpha --------- */
                    val diff = if (lastHr != null) rawHr - lastHr!! else 0.0
                    val isOutlier = abs(diff) > 25

                    val alpha = when {
                        isOutlier -> 0.05       // Ignorăm spike-uri mari
                        abs(diff) > 10 -> 0.35  // Reacție rapidă la sprint
                        rmssd < 20 -> 0.30      // Efort intens (oboseală) -> reactiv
                        rmssd < 40 -> 0.20      // Efort mediu
                        else -> 0.12            // Repaus -> stabil
                    }

                    /* --------- EMA (Smoothing) --------- */
                    val target = if (lastHr == null) rawHr else alpha * rawHr + (1 - alpha) * lastHr!!

                    /* --------- Rate limiter --------- */
                    // Limităm schimbarea fizică pe secundă
                    val maxStep = maxDeltaPerSec * dt
                    val delta = (target - (lastHr ?: target)).coerceIn(-maxStep, maxStep)
                    val smooth = (lastHr ?: target) + delta

                    lastHr = smooth
                    val displayHr = smooth.toInt()
                    val zone = calcZone(displayHr, 200)

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSampleTimestamp >= 2000) {
                        workoutHeartRateSamples.add(displayHr)
                        lastSampleTimestamp = currentTime
                    }

                    /* ================== BOMPA METRICS CALCULATION ================== */

                    // 1. CNS Readiness (0-100)
                    // Formula: ln(RMSSD) * 20. Ex: ln(50)*20 = 78 (Fresh). ln(15)*20 = 54 (Tired)
                    val cnsRaw = if (rmssd > 1.0) kotlin.math.ln(rmssd) * 20.0 else 0.0
                    val cnsScore = cnsRaw.coerceIn(0.0, 100.0).toInt()

                    // 2. TRIMP (Training Impulse - Edwards)
                    // Puncte = Zona * Minute. Adunăm fracțiunea de timp a acestui sample.
                    if (displayHr > 0) {
                        accumulatedTrimp += zone * timePerSampleMin
                    }

                    // 3. Calories (Estimare simplă)
                    // Formula brută: HR * Timp * Factor (0.12 e o medie pt bărbați activi)
                    if (displayHr > 0) {
                        accumulatedCalories += displayHr * timePerSampleMin * 0.12
                    }

                    /* --------- UI UPDATE --------- */
                    _athleteVitals.value = AthleteVitals(
                        heartRate = displayHr,
                        rmssd = rmssd,
                        trainingZone = zone,
                        // Adăugăm noile valori în obiect
                        cnsScore = cnsScore,
                        trimpScore = accumulatedTrimp,
                        calories = accumulatedCalories.toInt()
                    )
                }

            }, { err ->
                Log.e("POLAR", err.toString())
            })
    }


    /* ================== CONTROL ================== */

    fun startScan() {
        // Verificăm dacă avem permisiunea de scanare înainte de a porni
        // Altfel, sistemul aruncă o SecurityException și închide aplicația
        try {
            _availableDevices.value = emptySet()
            scanDisposable = api.searchForDevice()
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(
                    { info ->
                        val currentSet = _availableDevices.value.toMutableSet()
                        currentSet.add(info)
                        _availableDevices.value = currentSet
                    },
                    { err -> Log.e("POLAR", "Scan error: ${err.message}") }
                )
        } catch (e: SecurityException) {
            Log.e("POLAR", "Lipsesc permisiunile de scanare: ${e.message}")
        }
    }

    fun stopScan() {
        scanDisposable?.dispose()
        scanDisposable = null
    }

    fun connectToDevice(id: String) {
        api.connectToDevice(id)
    }

    fun disconnectFromDevice(id: String) {
        api.disconnectFromDevice(id)
        reset()
    }

    fun getHrSamples(): List<Int> = workoutHeartRateSamples.toList()

    private fun reset() {

        lastHr = null
        lastTimestamp = null
        rrBuffer.clear()

        ppiDisposable?.dispose()

        _deviceState.value = DeviceState(false)
        _athleteVitals.value = AthleteVitals()
        accumulatedTrimp = 0.0
        accumulatedCalories = 0.0
        lastCalculationTime = 0L
        workoutHeartRateSamples.clear()
        lastSampleTimestamp = 0L
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

    fun prepareNewWorkout() {
        accumulatedTrimp = 0.0
        accumulatedCalories = 0.0
        workoutHeartRateSamples.clear()
        lastHr = null
        _athleteVitals.value = AthleteVitals()
    }
}
