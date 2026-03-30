package com.application.polarapplication.ai.analysis

class StressManager {

    /**
     * Primește datele procesate și returnează:
     * 0 -> Calm / Non-Stress
     * 1 -> Stres Ridicat
     */
    fun predictStress(inputs: DoubleArray): Int {
        // AICI folosim numele fisierului tau Java (StressClassifier)
        // si metoda lui (score)
        val result = StressClassifier.score(inputs)

        // Rezultatul este un double[] de tipul [probabilitate_calm, probabilitate_stres]
        return if (result[1] > result[0]) 1 else 0
    }

    /**
     * Transformă datele brute în vectorul de features (14-15 intrări)
     */
    fun prepareInputs(
        bvpSamples: List<Double>,
        accSamples: List<Double>
    ): DoubleArray {
        val inputs = DoubleArray(15)
        if (bvpSamples.isEmpty()) return inputs

        // BVP features (RR intervals ca proxy)
        inputs[0] = bvpSamples.average()                    // mean_bvp
        inputs[1] = calculateStd(bvpSamples)                // std_bvp
        inputs[2] = bvpSamples.min()                        // min_bvp
        inputs[3] = bvpSamples.max()                        // max_bvp
        inputs[4] = inputs[3] - inputs[2]                   // range_bvp

        // Percentile 25, 50, 75
        val sorted = bvpSamples.sorted()
        val n = sorted.size
        inputs[5] = sorted[(n * 0.25).toInt().coerceIn(0, n-1)]  // p25
        inputs[6] = sorted[(n * 0.50).toInt().coerceIn(0, n-1)]  // p50 (median)
        inputs[7] = sorted[(n * 0.75).toInt().coerceIn(0, n-1)]  // p75

        // ACC features
        if (accSamples.isNotEmpty()) {
            inputs[8]  = accSamples.average()               // acc_mean
            inputs[9]  = calculateStd(accSamples)           // acc_std
            inputs[10] = accSamples.min()                   // acc_min
            inputs[11] = accSamples.max()                   // acc_max
            inputs[12] = inputs[11] - inputs[10]            // acc_range
        }

        // RMSSD din RR intervals (HRV feature)
        if (bvpSamples.size >= 2) {
            val diffs = bvpSamples.zipWithNext { a, b -> (b - a) * (b - a) }
            inputs[13] = Math.sqrt(diffs.average())         // rmssd proxy
        }

        // Sample count normalizat
        inputs[14] = (bvpSamples.size / 500.0).coerceIn(0.0, 1.0)

        return inputs
    }

    private fun calculateStd(data: List<Double>): Double {
        if (data.isEmpty()) return 0.0
        val mean = data.average()
        return Math.sqrt(data.map { Math.pow(it - mean, 2.0) }.average())
    }
}
