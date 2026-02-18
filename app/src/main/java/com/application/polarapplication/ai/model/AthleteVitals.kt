package com.application.polarapplication.ai.model

data class AthleteVitals(
    val heartRate: Int = 0,
    val rmssd: Double = 0.0,
    val trainingZone: Int = 0,
    val cnsScore: Int = 0,
    val trimpScore: Double = 0.0,
    val calories: Int = 0

) {

    fun getBompaReadiness(): String {
        return when {
            rmssd > 50.0 -> "Excelent (Gata de Putere Maximă)"
            rmssd > 30.0 -> "Normal (Antrenament Standard)"
            rmssd > 0.0 -> "Obosit (Recuperare Necesară)"
            else -> "Se calculează..."
        }
    }

}