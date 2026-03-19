package com.application.polarapplication.ai.analysis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class StressDataStream(private val stressManager: StressManager) {
    private val bvpBuffer = mutableListOf<Double>()
    private val accBuffer = mutableListOf<Double>()
    var currentStressScore by mutableStateOf(0f)
    var currentStressLevel by mutableStateOf(0) // 0 sau 1

    fun onNewDataReceived(bvp: Double, acc: Double) {
        bvpBuffer.add(bvp)
        accBuffer.add(acc)

        if (bvpBuffer.size >= 500) {
            processAndPredict()
            bvpBuffer.removeAt(0)
            accBuffer.removeAt(0)
        }
    }

    private fun processAndPredict() {
        // 1. Pregătim inputul (Mean, Std, etc. în ordinea din Kaggle)
        val inputs = stressManager.prepareInputs(bvpBuffer, accBuffer)

        // 2. Rulăm modelul tău de 36.000 de linii
        val prediction = stressManager.predictStress(inputs)

        // 3. Actualizăm scorul pentru animația siluetei
        // (Dacă predicția e 1, forțăm un scor mare, dacă e 0, unul mic)
        currentStressLevel = prediction
        currentStressScore = if (prediction == 1) 0.85f else 0.2f
    }
}