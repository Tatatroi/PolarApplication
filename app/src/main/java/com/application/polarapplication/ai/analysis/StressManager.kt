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
        // Asigură-te că mărimea DoubleArray(X) corespunde cu ce cere modelul tau
        val inputs = DoubleArray(15)

        // Exemplu de populare (trebuie să respecți ordinea din Kaggle!)
        if (bvpSamples.isNotEmpty()) {
            inputs[0] = bvpSamples.average()
            inputs[1] = calculateStd(bvpSamples)
        }

        // ... adaugă restul până la capăt ...

        return inputs
    }

    private fun calculateStd(data: List<Double>): Double {
        if (data.isEmpty()) return 0.0
        val mean = data.average()
        return Math.sqrt(data.map { Math.pow(it - mean, 2.0) }.average())
    }
}
