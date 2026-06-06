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
        val inputs = stressManager.prepareInputs(bvpBuffer, accBuffer)
        val (prediction, score) = stressManager.predictStressWithScore(inputs)
        currentStressLevel = prediction
        currentStressScore = score // probabilitatea reală [0..1]
    }
}
