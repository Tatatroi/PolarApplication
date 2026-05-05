package com.application.polarapplication.athletic

import android.content.Context
import com.application.polarapplication.model.TrainingSessionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class AthleticScore(
    val strength:    Float = 0f,
    val speed:       Float = 0f,
    val endurance:   Float = 0f,
    val hrr:         Float = 0f,
    val loadBalance: Float = 50f
)

data class ScoreSnapshot(
    val timestamp:   Long  = System.currentTimeMillis(),
    val strength:    Float = 0f,
    val speed:       Float = 0f,
    val endurance:   Float = 0f,
    val hrr:         Float = 0f,
    val loadBalance: Float = 50f
)

data class ScoreChange(
    val axis:   String,
    val delta:  Float,
    val reason: String
)

class AthleticProfileManager(context: Context) {

    private val prefs = context.getSharedPreferences("athletic_profile", Context.MODE_PRIVATE)
    private val gson  = Gson()

    private val _scores = MutableStateFlow(loadScores())
    val scores: StateFlow<AthleticScore> = _scores

    private val _hasCompletedInitialTest = MutableStateFlow(
        prefs.getBoolean("initial_test_done", false)
    )
    val hasCompletedInitialTest: StateFlow<Boolean> = _hasCompletedInitialTest

    private val _lastScoreChange = MutableStateFlow<ScoreChange?>(null)
    val lastScoreChange: StateFlow<ScoreChange?> = _lastScoreChange

    private val _scoreHistory = MutableStateFlow(loadScoreHistory())
    val scoreHistory: StateFlow<List<ScoreSnapshot>> = _scoreHistory

    // ─────────────────────────────────────────────
    // PRAGURI MINIME DE VALIDITATE
    // Sub acestea, sesiunea nu contează deloc
    // ─────────────────────────────────────────────
    private val MIN_DURATION_SECONDS = 300L  // 5 minute
    private val MIN_TRIMP            = 5.0   // efort minim
    private val MIN_AVG_HR           = 80    // puls mediu minim (altfel era inactiv)

    // ─────────────────────────────────────────────
    // LOAD / SAVE
    // ─────────────────────────────────────────────

    private fun loadScores() = AthleticScore(
        strength    = prefs.getFloat("score_strength",    0f),
        speed       = prefs.getFloat("score_speed",       0f),
        endurance   = prefs.getFloat("score_endurance",   0f),
        hrr         = prefs.getFloat("score_hrr",         0f),
        loadBalance = prefs.getFloat("score_loadbalance", 50f)
    )

    private fun saveScores(s: AthleticScore) {
        prefs.edit()
            .putFloat("score_strength",    s.strength)
            .putFloat("score_speed",       s.speed)
            .putFloat("score_endurance",   s.endurance)
            .putFloat("score_hrr",         s.hrr)
            .putFloat("score_loadbalance", s.loadBalance)
            .putLong("last_update",        System.currentTimeMillis())
            .apply()
        _scores.value = s
    }

    private fun loadScoreHistory(): List<ScoreSnapshot> {
        val json = prefs.getString("score_history", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ScoreSnapshot>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun saveScoreHistory(history: List<ScoreSnapshot>) {
        prefs.edit().putString("score_history", gson.toJson(history)).apply()
        _scoreHistory.value = history
    }

    private fun addSnapshot(score: AthleticScore) {
        val snapshot = ScoreSnapshot(
            timestamp   = System.currentTimeMillis(),
            strength    = score.strength,
            speed       = score.speed,
            endurance   = score.endurance,
            hrr         = score.hrr,
            loadBalance = score.loadBalance
        )
        val history = (_scoreHistory.value + snapshot).takeLast(90)
        saveScoreHistory(history)
    }

    // ─────────────────────────────────────────────
    // TEST INITIAL
    // ─────────────────────────────────────────────

    fun saveInitialTestResults(
        strengthScore: Float, speedScore: Float,
        enduranceScore: Float, hrrScore: Float
    ) {
        val initial = AthleticScore(
            strength    = strengthScore.coerceIn(0f, 100f),
            speed       = speedScore.coerceIn(0f, 100f),
            endurance   = enduranceScore.coerceIn(0f, 100f),
            hrr         = hrrScore.coerceIn(0f, 100f),
            loadBalance = 50f
        )
        saveScores(initial)
        addSnapshot(initial)
        prefs.edit()
            .putBoolean("initial_test_done", true)
            .putLong("test_date", System.currentTimeMillis())
            .apply()
        _hasCompletedInitialTest.value = true
    }

    // ─────────────────────────────────────────────
    // UPDATE DUPĂ SESIUNE
    // Logica principală bazată pe date reale
    // ─────────────────────────────────────────────

    fun updateAfterSession(session: TrainingSessionEntity) {
        // 1. Verificăm dacă sesiunea e validă
        if (!isSessionValid(session)) {
            android.util.Log.d("ATHLETIC", "Sesiune invalidă — ignorată (dur=${session.durationSeconds}s, trimp=${session.finalTrimp}, avgHr=${session.avgHeartRate})")
            return
        }

        val current = _scores.value

        // 2. Calculăm punctele bazate pe indicatori reali
        val pts = calculatePoints(session)

        // 3. Calculăm delta HRR din recuperarea cardiacă
        val hrrDelta = calculateHrrDelta(session)

        android.util.Log.d("ATHLETIC", "Sesiune validă: tip=${session.type}, pts=$pts, hrrDelta=$hrrDelta, trimp=${session.finalTrimp}, dur=${session.durationSeconds}s")

        var updated = current
        var changeAxis = ""; var changeDelta = 0f; var changeReason = ""

        when (session.type.uppercase()) {
            "STRENGTH" -> {
                updated = updated.copy(strength = (current.strength + pts).coerceIn(0f, 100f))
                changeAxis = "Strength"; changeDelta = pts
                changeReason = buildReason(session, pts)
            }
            "SPEED" -> {
                updated = updated.copy(speed = (current.speed + pts).coerceIn(0f, 100f))
                changeAxis = "Speed"; changeDelta = pts
                changeReason = buildReason(session, pts)
            }
            "ENDURANCE" -> {
                updated = updated.copy(endurance = (current.endurance + pts).coerceIn(0f, 100f))
                changeAxis = "Endurance"; changeDelta = pts
                changeReason = buildReason(session, pts)
            }
            "RECOVERY", "REST" -> {
                // Recovery adaugă doar la HRR, nu la alte axe
                updated = updated.copy(hrr = (current.hrr + 0.3f).coerceIn(0f, 100f))
                changeAxis = "HRR"; changeDelta = 0.3f
                changeReason = "Recovery session"
            }
        }

        // HRR se actualizează pentru orice tip de sesiune (nu recovery)
        if (session.type.uppercase() !in listOf("RECOVERY", "REST")) {
            updated = updated.copy(hrr = (updated.hrr + hrrDelta).coerceIn(0f, 100f))
        }

        saveScores(updated)
        addSnapshot(updated)

        if (changeAxis.isNotEmpty()) {
            _lastScoreChange.value = ScoreChange(changeAxis, changeDelta, changeReason)
        }
    }

    // ─────────────────────────────────────────────
    // VALIDARE SESIUNE
    // ─────────────────────────────────────────────

    private fun isSessionValid(session: TrainingSessionEntity): Boolean {
        // Sesiunile vechi fără durationSeconds le acceptăm dacă au TRIMP și HR valid
        val durationOk = session.durationSeconds >= MIN_DURATION_SECONDS ||
                (session.durationSeconds == 0L && session.finalTrimp >= MIN_TRIMP)
        val trimpOk    = session.finalTrimp >= MIN_TRIMP
        val hrOk       = session.avgHeartRate >= MIN_AVG_HR

        return durationOk && trimpOk && hrOk
    }

    // ─────────────────────────────────────────────
    // CALCUL PUNCTE BAZAT PE INDICATORI REALI
    // ─────────────────────────────────────────────

    private fun calculatePoints(session: TrainingSessionEntity): Float {
        // Fiecare component contribuie la scorul final

        // 1. TRIMP — volumul de muncă (0-4 pts)
        // TRIMP mic = sesiune ușoară, TRIMP mare = sesiune grea
        val trimpPts = when {
            session.finalTrimp >= 150 -> 4.0f  // sesiune foarte grea (>2.5 ore intens)
            session.finalTrimp >= 100 -> 3.0f  // sesiune grea
            session.finalTrimp >= 60  -> 2.5f  // sesiune medie-grea
            session.finalTrimp >= 30  -> 2.0f  // sesiune medie
            session.finalTrimp >= 15  -> 1.5f  // sesiune ușoară-medie
            session.finalTrimp >= 5   -> 1.0f  // sesiune ușoară
            else                      -> 0f
        }

        // 2. Intensitate (zona de puls atinsă) — (0-2 pts)
        // MaxHR mare față de userMaxHr înseamnă că ai atins zone înalte
        val hrIntensityRatio = session.maxHeartRate.toFloat() / 200f  // aproximare userMaxHr
        val intensityPts = when {
            hrIntensityRatio >= 0.90f -> 2.0f  // Zone 5
            hrIntensityRatio >= 0.80f -> 1.5f  // Zone 4
            hrIntensityRatio >= 0.70f -> 1.0f  // Zone 3
            hrIntensityRatio >= 0.60f -> 0.5f  // Zone 2
            else                      -> 0.2f  // Zone 1
        }

        // 3. Durată bonus — sesiunile lungi adaugă un mic bonus (0-1 pt)
        val durationPts = when {
            session.durationSeconds >= 3600 -> 1.0f  // > 1 oră
            session.durationSeconds >= 1800 -> 0.7f  // > 30 min
            session.durationSeconds >= 900  -> 0.4f  // > 15 min
            session.durationSeconds >= 300  -> 0.2f  // > 5 min
            else                            -> 0f
        }

        // 4. CNS penalizare — dacă CNS la final e foarte mic, efortul a fost prea mare
        // și corpul nu a putut absorbi bine antrenamentul
        val cnsPenalty = when {
            session.cnsScoreAtEnd < 20 -> -0.5f  // over-trained
            session.cnsScoreAtEnd < 35 -> -0.2f  // obosit
            else                       -> 0f
        }

        val total = (trimpPts + intensityPts + durationPts + cnsPenalty).coerceAtLeast(0f)

        android.util.Log.d("ATHLETIC", "Puncte: trimp=$trimpPts, intensity=$intensityPts, duration=$durationPts, cnsPenalty=$cnsPenalty, total=$total")

        return total
    }

    // ─────────────────────────────────────────────
    // CALCUL HRR DELTA
    // Bazat pe recuperarea cardiacă reală
    // ─────────────────────────────────────────────

    private fun calculateHrrDelta(session: TrainingSessionEntity): Float {
        // Diferența dintre MaxHR și AvgHR indică capacitatea de recuperare
        // Un sportiv bine antrenat are MaxHR mare dar AvgHR moderat
        val hrDiff = session.maxHeartRate - session.avgHeartRate

        return when {
            hrDiff >= 40 -> 1.5f   // recuperare cardiacă excelentă
            hrDiff >= 30 -> 1.0f   // recuperare bună
            hrDiff >= 20 -> 0.5f   // recuperare medie
            hrDiff >= 10 -> 0.2f   // recuperare slabă
            else         -> 0f     // fără recuperare detectabilă
        }
    }

    private fun buildReason(session: TrainingSessionEntity, pts: Float): String {
        val durMin = session.durationSeconds / 60
        return "${session.type} · ${durMin}min · TRIMP=${session.finalTrimp.toInt()} · +${"%.1f".format(pts)}pts"
    }

    // ─────────────────────────────────────────────
    // LOAD BALANCE (ACWR)
    // ─────────────────────────────────────────────

    fun updateLoadBalance(allSessions: List<TrainingSessionEntity>) {
        val score   = acwrToScore(calculateAcwr(allSessions))
        val updated = _scores.value.copy(loadBalance = score)
        saveScores(updated)
    }

    // ─────────────────────────────────────────────
    // DEGRADARE ZILNICĂ
    // Dacă nu antrenezi, pierzi puncte
    // ─────────────────────────────────────────────

    fun applyDailyDecay(allSessions: List<TrainingSessionEntity>) {
        val today   = LocalDate.now()
        val current = _scores.value

        val updated = current.copy(
            strength  = applyDecayForType("STRENGTH",  current.strength,  allSessions, today),
            speed     = applyDecayForType("SPEED",     current.speed,     allSessions, today),
            endurance = applyDecayForType("ENDURANCE", current.endurance, allSessions, today),
            hrr       = applyHrrDecay(current.hrr, allSessions, today)
        )

        if (updated != current) {
            saveScores(updated)
            addSnapshot(updated)
        }
    }

    private fun applyDecayForType(
        type: String, current: Float,
        sessions: List<TrainingSessionEntity>, today: LocalDate
    ): Float {
        val last = sessions
            .filter { it.type.uppercase() == type }
            .maxByOrNull { it.date }

        val daysSinceLast = if (last == null) 999L else ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(last.date).atZone(ZoneId.systemDefault()).toLocalDate(), today
        )

        // Degradare progresivă — cu cât e mai mult de când ai antrenat, cu atât pierzi mai mult
        val decay = when {
            daysSinceLast <= 3  -> 0f    // 0-3 zile: fără degradare
            daysSinceLast <= 7  -> 0.5f  // 4-7 zile: degradare ușoară
            daysSinceLast <= 14 -> 1.5f  // 1-2 săptămâni: degradare medie
            daysSinceLast <= 21 -> 2.5f  // 2-3 săptămâni: degradare semnificativă
            else                -> 4.0f  // > 3 săptămâni: degradare mare
        }

        return (current - decay).coerceIn(0f, 100f)
    }

    private fun applyHrrDecay(
        current: Float,
        sessions: List<TrainingSessionEntity>,
        today: LocalDate
    ): Float {
        val last = sessions.maxByOrNull { it.date }
        val daysSinceLast = if (last == null) 999L else ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(last.date).atZone(ZoneId.systemDefault()).toLocalDate(), today
        )

        // HRR se degradează mai lent decât forța/viteza
        val decay = when {
            daysSinceLast <= 5  -> 0f
            daysSinceLast <= 14 -> 0.3f
            daysSinceLast <= 21 -> 0.8f
            else                -> 1.5f
        }

        return (current - decay).coerceIn(0f, 100f)
    }

    // ─────────────────────────────────────────────
    // RESET
    // ─────────────────────────────────────────────

    fun resetProfile() {
        prefs.edit()
            .remove("score_strength").remove("score_speed")
            .remove("score_endurance").remove("score_hrr")
            .remove("score_loadbalance").remove("initial_test_done")
            .remove("test_date").remove("score_history")
            .apply()
        _scores.value                  = AthleticScore()
        _hasCompletedInitialTest.value = false
        _scoreHistory.value            = emptyList()
        _lastScoreChange.value         = null
    }

    // ─────────────────────────────────────────────
    // ACWR
    // ─────────────────────────────────────────────

    private fun calculateAcwr(sessions: List<TrainingSessionEntity>): Float {
        val now     = System.currentTimeMillis()
        val acute   = sessions
            .filter { now - it.date <= 7L * 86400000 }
            .sumOf { it.finalTrimp }
        val chronic = sessions
            .filter { now - it.date <= 28L * 86400000 }
            .sumOf { it.finalTrimp } / 4.0
        if (chronic < 1.0) return 1.0f
        return (acute / chronic).toFloat().coerceIn(0f, 3f)
    }

    private fun acwrToScore(acwr: Float) = when {
        acwr in 0.8f..1.3f -> 100f
        acwr in 0.7f..0.8f -> 75f
        acwr in 1.3f..1.5f -> 75f
        acwr in 0.5f..0.7f -> 45f
        acwr in 1.5f..1.8f -> 45f
        else               -> 20f
    }

    // ─────────────────────────────────────────────
    // HELPERS PUBLICI PENTRU TEST INITIAL
    // ─────────────────────────────────────────────

    fun calculateEnduranceScore(hrSamplesInZ2: Int, totalSamples: Int, avgHrDrift: Float): Float {
        if (totalSamples == 0) return 10f
        return ((hrSamplesInZ2.toFloat() / totalSamples * 100f) -
                (avgHrDrift / 10f).coerceIn(0f, 30f)).coerceIn(5f, 100f)
    }

    fun calculateStrengthScore(avgHr: Int, hrAfter90: Int, maxHr: Int): Float {
        if (avgHr == 0) return 10f
        return ((avgHr.toFloat() / maxHr * 50f) +
                (1f - hrAfter90.toFloat() / avgHr) * 50f).coerceIn(5f, 100f)
    }

    fun calculateSpeedScore(peakHr: Int, hrAfter60: Int, maxHr: Int): Float {
        if (peakHr == 0) return 10f
        return ((peakHr.toFloat() / maxHr * 60f) +
                (1f - hrAfter60.toFloat() / peakHr) * 40f).coerceIn(5f, 100f)
    }

    fun calculateHrrScore(peakHr: Int, hrAfter60: Int): Float {
        val drop = (peakHr - hrAfter60).toFloat().coerceAtLeast(0f)
        return when {
            drop >= 30f -> 100f; drop >= 25f -> 85f; drop >= 20f -> 70f
            drop >= 15f -> 50f;  drop >= 10f -> 35f; drop >= 5f  -> 20f
            else        -> 10f
        }
    }
}