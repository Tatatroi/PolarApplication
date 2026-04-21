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

    /* ================== STATE ================== */

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState = _deviceState.asStateFlow()

    private val _athleteVitals = MutableStateFlow(AthleteVitals())
    val athleteVitals = _athleteVitals.asStateFlow()

    private var accumulatedTrimp = 0.0
    private var accumulatedCalories = 0.0
    private var lastCalculationTime = 0L

    private var scanDisposable: Disposable? = null
    private val _availableDevices = MutableStateFlow<Set<PolarDeviceInfo>>(emptySet())
    val availableDevices = _availableDevices.asStateFlow()

    /* ================== HR CALCULATION — FEREASTRA TEMPORALA ================== */

    // Fereastra temporală pentru calcul HR — ultimele 4 secunde
    private val hrCalculationWindowMs = 4000L
    private val rrTimestampBuffer = mutableListOf<Pair<Long, Double>>() // (timestamp, rr_ms)

    // MAD outlier detection buffer
    private val rrMadBuffer = mutableListOf<Double>()
    private val madWindowSize = 15

    // Constante fiziologice
    private val MIN_RR_MS = 300.0              // 200 BPM maxim
    private val MAX_RR_MS = 1500.0             // 40 BPM minim
    private val MAX_HR_CHANGE_BPM_PER_SEC = 25.0 // Limită fiziologică realistă

    // State HR
    private var lastHr: Double? = null
    private var lastTimestamp: Long? = null

    /* ================== ACC ================== */

    private var latestAccMagnitude: Double = 0.0

    /* =============== AI ENGINE ================ */

    private val stressManager = StressManager()
    private val stressDataStream = StressDataStream(stressManager)

    /* ================== HRV ================== */

    private val rrBuffer = mutableListOf<Double>()
    private val maxRrSize = 60

    private var ppiDisposable: Disposable? = null
    private var accDisposable: Disposable? = null

    private val workoutHeartRateSamples = mutableListOf<Int>()
    private var lastSampleTimestamp = 0L

    var userMaxHr: Int = 200

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
                    deviceId    = info.deviceId
                )
            }

            override fun streamingFeaturesReady(
                id: String,
                features: Set<PolarBleApi.PolarDeviceDataType>
            ) {
                if (features.contains(PolarBleApi.PolarDeviceDataType.PPI)) {
                    startPpiStreaming(id)
                }
                if (features.contains(PolarBleApi.PolarDeviceDataType.ACC)) {
                    startAccStreaming(id)
                }
            }

            override fun deviceDisconnected(info: PolarDeviceInfo) {
                reset()
            }

            override fun batteryLevelReceived(id: String, level: Int) {
                _deviceState.value = _deviceState.value.copy(batteryLevel = level)
            }
        })
    }

    /* ================== PPI STREAM ================== */

    @Suppress("DEPRECATION")
    private fun startPpiStreaming(deviceId: String) {
        ppiDisposable = api.startOhrPPIStreaming(deviceId)
            .subscribe({ data ->

                val now       = System.currentTimeMillis()
                val batchSize = data.samples.size
                val dt        = if (lastTimestamp == null) 1.0
                else ((now - lastTimestamp!!) / 1000.0).coerceIn(0.1, 2.0)
                lastTimestamp = now

                // Procesăm fiecare mostră cu timestamp interpolat
                data.samples.forEachIndexed { index, sample ->

                    // Filtrăm mostrele fără contact cu pielea
                    if (sample.skinContactSupported && !sample.skinContactStatus) return@forEachIndexed

                    val rr = sample.ppi.toDouble()

                    // 1. Filtru biologic dur — eliminăm valori imposibile fizic
                    if (rr < MIN_RR_MS || rr > MAX_RR_MS) return@forEachIndexed

                    // 2. Timestamp estimat pentru această bătaie
                    // Distribuim timpul batch-ului uniform între mostre
                    val sampleTime = now - ((batchSize - 1 - index) *
                            (dt * 1000.0 / batchSize)).toLong()

                    // 3. HRV buffer pentru RMSSD
                    rrBuffer.add(rr)
                    if (rrBuffer.size > maxRrSize) rrBuffer.removeAt(0)

                    // 4. MAD outlier detection — mai robust decât range fix
                    if (!isOutlierMAD(rr)) {
                        addRrToBuffer(sampleTime, rr)
                    }
                }

                // 5. Calcul HR real din fereastra temporală
                val currentHr = calculateHrFromWindow() ?: return@subscribe

                // 6. Rate limiting bazat pe timp real și fiziologie
                val displayHr = applyRateLimit(currentHr, dt)

                // 7. RMSSD calculat temporal (ultimele 30 secunde)
                val rmssd = calculateTemporalRMSSD()

                // 8. Actualizăm metricele
                updateMetrics(displayHr, rmssd, dt)

                // 9. AI Stress Engine
                stressDataStream.onNewDataReceived(
                    bvp = rrTimestampBuffer.lastOrNull()?.second ?: rr2bvpProxy(),
                    acc = latestAccMagnitude
                )

            }, { err ->
                Log.e("POLAR", "PPI stream error: ${err.message}")
            })
    }

    /* ================== MAD OUTLIER DETECTION ================== */

    /**
     * Median Absolute Deviation — robust pentru distribuții non-gaussiene.
     * Returnează true dacă RR-ul e outlier și trebuie ignorat.
     */
    private fun isOutlierMAD(rr: Double): Boolean {
        if (rrMadBuffer.size < 5) {
            rrMadBuffer.add(rr)
            return false
        }

        val sorted   = rrMadBuffer.sorted()
        val median   = sorted[sorted.size / 2]
        val mad      = rrMadBuffer.map { abs(it - median) }.sorted().let {
            it[it.size / 2]
        }.coerceAtLeast(10.0) // Min 10ms pentru a evita threshold 0

        // Threshold: 3 × MAD — standard în literatura de specialitate
        val isOutlier = abs(rr - median) > 3.0 * mad

        // Actualizăm buffer-ul (sliding window)
        rrMadBuffer.add(rr)
        if (rrMadBuffer.size > madWindowSize) rrMadBuffer.removeAt(0)

        return isOutlier
    }

    /* ================== BUFFER TEMPORAL ================== */

    private fun addRrToBuffer(timestamp: Long, rr: Double) {
        rrTimestampBuffer.add(timestamp to rr)

        // Eliminăm RR-urile mai vechi decât fereastra de calcul
        val cutoff = timestamp - hrCalculationWindowMs
        rrTimestampBuffer.removeAll { it.first < cutoff }
    }

    /* ================== CALCUL HR DIN FEREASTRA TEMPORALA ================== */

    /**
     * HR calculat ca medie a intervalelor RR din ultimele 4 secunde.
     * Mult mai stabil decât 60000/rr_individual.
     */
    private fun calculateHrFromWindow(): Double? {
        if (rrTimestampBuffer.size < 2) return null

        val validRrs = rrTimestampBuffer.map { it.second }
        val meanRr   = validRrs.average()

        return 60000.0 / meanRr
    }

    /* ================== RATE LIMITER FIZIOLOGIC ================== */

    /**
     * Limitează schimbarea HR la MAX_HR_CHANGE_BPM_PER_SEC.
     * Bazat pe timp real (dt), nu pe număr de sample-uri.
     * Reacție mai rapidă la schimbări mari (sprint) față de variații mici (zgomot).
     */
    private fun applyRateLimit(newHr: Double, dt: Double): Int {
        val maxChange = MAX_HR_CHANGE_BPM_PER_SEC * dt.coerceAtMost(1.0)

        val limitedHr = if (lastHr == null) {
            newHr // Primul sample — acceptăm direct fără limitare
        } else {
            val delta = (newHr - lastHr!!).coerceIn(-maxChange, maxChange)
            lastHr!! + delta
        }

        lastHr = limitedHr
        return limitedHr.toInt()
    }

    /* ================== RMSSD TEMPORAL ================== */

    /**
     * RMSSD calculat din ultimele 30 de secunde, nu din ultimele 60 RR.
     * Mai consistent temporal — nu depinde de frecvența cardiacă.
     */
    private fun calculateTemporalRMSSD(): Double {
        val cutoff    = System.currentTimeMillis() - 30_000L
        val recentRrs = rrTimestampBuffer
            .filter { it.first > cutoff }
            .map { it.second }

        if (recentRrs.size < 3) return 0.0

        val diffs = recentRrs.zipWithNext { a, b -> (b - a) * (b - a) }
        return sqrt(diffs.average())
    }

    /* ================== UPDATE METRICI ================== */

    private fun updateMetrics(hr: Int, rmssd: Double, dt: Double) {
        val zone            = calcZone(hr, userMaxHr)
        val timePerSampleMin = dt / 60.0

        // CNS Score bazat pe RMSSD
        val cnsRaw   = if (rmssd > 5.0 && rmssd < 150.0)
            (kotlin.math.ln(rmssd) * 20.0).coerceIn(0.0, 100.0)
        else 0.0
        val cnsScore = cnsRaw.toInt()

        // TRIMP și Calorii
        accumulatedTrimp    += zone * timePerSampleMin
        accumulatedCalories += hr * timePerSampleMin * 0.12

        // Salvare sample pentru graficul HR (la fiecare 2 secunde)
        val now = System.currentTimeMillis()
        if (now - lastSampleTimestamp >= 2000) {
            workoutHeartRateSamples.add(hr)
            lastSampleTimestamp = now
        }

        // Stress AI
        val currentStressScore = stressDataStream.currentStressScore
        val currentStressLevel = stressDataStream.currentStressLevel

        _athleteVitals.value = AthleteVitals(
            heartRate    = hr,
            rmssd        = rmssd,
            trainingZone = zone,
            cnsScore     = cnsScore,
            trimpScore   = accumulatedTrimp,
            calories     = accumulatedCalories.toInt(),
            stressScore  = currentStressScore,
            stressLevel  = currentStressLevel
        )
    }

    /* ================== PROXY BVP PENTRU AI ================== */

    /**
     * Returnează ultimul RR interval ca proxy pentru BVP.
     * Modelul WESAD a fost antrenat pe BVP optic — RR e cea mai bună
     * aproximare disponibilă cu Polar H10/Verity Sense.
     */
    private fun rr2bvpProxy(): Double {
        return rrTimestampBuffer.lastOrNull()?.second ?: 0.0
    }

    /* ================== ACC STREAM ================== */

    private fun startAccStreaming(deviceId: String) {
        accDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
            .flatMapPublisher { settings ->
                api.startAccStreaming(deviceId, settings.maxSettings())
            }
            .subscribe(
                { polarAccData ->
                    for (sample in polarAccData.samples) {
                        val x         = sample.x.toDouble()
                        val y         = sample.y.toDouble()
                        val z         = sample.z.toDouble()
                        latestAccMagnitude = sqrt(x * x + y * y + z * z)
                    }
                },
                { error -> Log.e("POLAR_ACC", "ACC stream error: $error") }
            )
    }

    /* ================== CONTROL ================== */

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

    fun stopScan() {
        scanDisposable?.dispose()
        scanDisposable = null
    }

    fun connectToDevice(id: String)       { api.connectToDevice(id) }
    fun disconnectFromDevice(id: String)  { api.disconnectFromDevice(id); reset() }
    fun getHrSamples(): List<Int>         = workoutHeartRateSamples.toList()

    fun prepareNewWorkout() {
        // Reset complet la începutul unui antrenament nou
        accumulatedTrimp    = 0.0
        accumulatedCalories = 0.0
        workoutHeartRateSamples.clear()
        lastSampleTimestamp = 0L

        // Reset HR calculation state
        lastHr = null
        lastTimestamp = null
        rrTimestampBuffer.clear()
        rrMadBuffer.clear()

        _athleteVitals.value = AthleteVitals()
    }

    private fun reset() {
        lastHr        = null
        lastTimestamp = null

        rrBuffer.clear()
        rrTimestampBuffer.clear()
        rrMadBuffer.clear()

        ppiDisposable?.dispose()
        accDisposable?.dispose()
        ppiDisposable = null
        accDisposable = null

        _deviceState.value      = DeviceState(false)
        _athleteVitals.value    = AthleteVitals()
        accumulatedTrimp        = 0.0
        accumulatedCalories     = 0.0
        lastCalculationTime     = 0L
        workoutHeartRateSamples.clear()
        lastSampleTimestamp     = 0L
    }

    /* ================== UTILS ================== */

    private fun calcZone(hr: Int, max: Int): Int {
        val p = hr.toDouble() / max * 100
        return when {
            p < 60 -> 1
            p < 70 -> 2
            p < 80 -> 3
            p < 90 -> 4
            else   -> 5
        }
    }
}