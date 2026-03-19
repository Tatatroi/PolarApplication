package com.application.polarapplication.ai.daily


object DailyWorkoutGenerator {

    fun generate(type: WorkoutType, fatigue: Int = 0): String {

        val strengthOptions = listOf(
            "Forță maximă: 5×5, pauză 3 min",
            "Forță explozivă: 6×30 sec intensitate mare",
            "Hipertrofie: 4×12, ritm controlat",
            "Forță de bază: 3×8, intensitate moderată"
        )

        val enduranceOptions = listOf(
            "Rezistență: 30 min la 65–75% HRmax",
            "Intervale: 10×1 min la 85% HRmax",
            "Tempo run: 20 min la 80% HRmax",
            "LISS: 45 min ritm ușor"
        )

        val speedOptions = listOf(
            "Sprinturi: 10×5 sec, pauză 40 sec",
            "Agilitate: schimbări de direcție 6×30 sec",
            "Reacție: sprint la semnal",
            "Interval viteză: 5×20 sec"
        )

        val recoveryOptions = listOf(
            "Recuperare activă: 20–30 min mers",
            "Mobilitate și stretching ușor",
            "Jog 15 min ritm foarte ușor"
        )

        val restOptions = listOf("Zi de odihnă totală.")

        val chosen = when(type) {
            WorkoutType.STRENGTH -> strengthOptions.random()
            WorkoutType.ENDURANCE -> enduranceOptions.random()
            WorkoutType.SPEED -> speedOptions.random()
            WorkoutType.RECOVERY -> recoveryOptions.random()
            WorkoutType.REST -> restOptions.first()
        }

        return chosen
    }
}
