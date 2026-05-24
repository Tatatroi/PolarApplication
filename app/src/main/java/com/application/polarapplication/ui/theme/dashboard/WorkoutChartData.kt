package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────
// DATA STRUCTURES
// ─────────────────────────────────────────────

data class HrSample(
    val timestamp:  Long,  // System.currentTimeMillis()
    val heartRate:  Int,
    val zone:       Int
)

data class Peak(
    val timestamp:  Long,
    val bpm:        Int,
    val zone:       Int,
    val elapsedSec: Long   // secunde de la start antrenament
)

data class RecoveryEvent(
    val peak:            Peak,
    val recoveryTimeSec: Long,   // secunde până la HR stabil după peak
    val hrDrop:          Int,    // BPM scăzut
    val rating:          RecoveryRating
)

enum class RecoveryRating(val label: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF4ADE80)),
    GOOD     ("Good",      Color(0xFF60A5FA)),
    MODERATE ("Moderate",  Color(0xFFFBBF24)),
    POOR     ("Poor",      Color(0xFFF87171))
}

data class ZoneDistribution(
    val zone:       Int,
    val durationMs: Long,
    val percentage: Float
)

// ─────────────────────────────────────────────
// ZONE COLORS
// ─────────────────────────────────────────────

fun zoneColor(zone: Int): Color = when (zone) {
    1    -> Color(0xFF9CA3AF)
    2    -> Color(0xFF60A5FA)
    3    -> Color(0xFF4ADE80)
    4    -> Color(0xFFF97316)
    5    -> Color(0xFFEF4444)
    else -> Color(0xFF6B7280)
}

fun zoneName(zone: Int): String = when (zone) {
    1    -> "Z1"
    2    -> "Z2"
    3    -> "Z3"
    4    -> "Z4"
    5    -> "Z5"
    else -> "—"
}

fun zoneFullName(zone: Int): String = when (zone) {
    1    -> "Recovery"
    2    -> "Aerobic"
    3    -> "Tempo"
    4    -> "Threshold"
    5    -> "Anaerobic"
    else -> "—"
}

// ─────────────────────────────────────────────
// ALGORITHMS
// ─────────────────────────────────────────────

fun calcZone(hr: Int, maxHr: Int): Int {
    val pct = hr.toDouble() / maxHr * 100
    return when {
        pct < 60 -> 1
        pct < 70 -> 2
        pct < 80 -> 3
        pct < 90 -> 4
        else     -> 5
    }
}

suspend fun detectPeaks(
    samples:     List<HrSample>,
    minDeltaBpm: Int = 15,
    minTimeSec:  Int = 10,
    windowSize:  Int = 5
): List<Peak> = withContext(Dispatchers.Default) {
    if (samples.size < windowSize * 2) return@withContext emptyList()

    // 1. Moving average smoothing
    val smoothed = samples.mapIndexed { i, s ->
        val start = maxOf(0, i - windowSize / 2)
        val end   = minOf(samples.size, i + windowSize / 2 + 1)
        val avg   = samples.subList(start, end).map { it.heartRate }.average().toInt()
        s.copy(heartRate = avg)
    }

    val peaks = mutableListOf<Peak>()
    var lastPeakTime = 0L
    var lastPeakBpm  = 0

    for (i in windowSize until smoothed.size - windowSize) {
        val current = smoothed[i]
        val isLocalMax = (i - windowSize until i).all { smoothed[it].heartRate <= current.heartRate } &&
                (i + 1..i + windowSize).all { smoothed[it].heartRate <= current.heartRate }

        if (!isLocalMax) continue

        val timeSinceLast = (current.timestamp - lastPeakTime) / 1000
        val deltaBpm      = current.heartRate - lastPeakBpm

        if (timeSinceLast >= minTimeSec && (deltaBpm >= minDeltaBpm || peaks.isEmpty())) {
            val elapsedSec = if (samples.isNotEmpty())
                (current.timestamp - samples.first().timestamp) / 1000 else 0L

            peaks.add(Peak(
                timestamp  = current.timestamp,
                bpm        = samples[i].heartRate, // original, not smoothed
                zone       = current.zone,
                elapsedSec = elapsedSec
            ))
            lastPeakTime = current.timestamp
            lastPeakBpm  = current.heartRate
        }
    }
    peaks
}

fun calculateZoneDistribution(
    samples: List<HrSample>
): List<ZoneDistribution> {
    if (samples.isEmpty()) return emptyList()

    val zoneMs = mutableMapOf<Int, Long>()
    for (i in 1 until samples.size) {
        val dt   = samples[i].timestamp - samples[i - 1].timestamp
        val zone = samples[i - 1].zone
        zoneMs[zone] = (zoneMs[zone] ?: 0L) + dt
    }

    val totalMs = zoneMs.values.sum().coerceAtLeast(1L)
    return (1..5).map { zone ->
        val dur = zoneMs[zone] ?: 0L
        ZoneDistribution(
            zone       = zone,
            durationMs = dur,
            percentage = dur.toFloat() / totalMs
        )
    }
}

fun calculateRecoveryEvents(
    samples:    List<HrSample>,
    peaks:      List<Peak>,
    windowSec:  Int = 60
): List<RecoveryEvent> {
    if (peaks.isEmpty() || samples.isEmpty()) return emptyList()

    return peaks.mapNotNull { peak ->
        val peakIdx = samples.indexOfFirst { it.timestamp >= peak.timestamp }
        if (peakIdx < 0) return@mapNotNull null

        val windowSamples = samples.drop(peakIdx).take(windowSec * 2)
        if (windowSamples.size < 5) return@mapNotNull null

        val minHrAfter  = windowSamples.minOf { it.heartRate }
        val hrDrop      = peak.bpm - minHrAfter
        val recoveryIdx = windowSamples.indexOfFirst { it.heartRate == minHrAfter }
        val recoverySec = if (recoveryIdx >= 0)
            (windowSamples[recoveryIdx].timestamp - peak.timestamp) / 1000 else windowSec.toLong()

        val rating = when {
            hrDrop >= 30 -> RecoveryRating.EXCELLENT
            hrDrop >= 20 -> RecoveryRating.GOOD
            hrDrop >= 10 -> RecoveryRating.MODERATE
            else         -> RecoveryRating.POOR
        }

        RecoveryEvent(
            peak            = peak,
            recoveryTimeSec = recoverySec,
            hrDrop          = hrDrop,
            rating          = rating
        )
    }
}

fun formatElapsed(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}