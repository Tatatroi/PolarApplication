package com.application.polarapplication.ai.planning

import com.application.polarapplication.ai.daily.WorkoutType

object MicroCycleGenerator {
    fun generate(phase: String): List<WorkoutType>{
        return when (phase) {
            "general" -> listOf(
                WorkoutType.STRENGTH,
                WorkoutType.ENDURANCE,
                WorkoutType.STRENGTH,
                WorkoutType.RECOVERY,
                WorkoutType.SPEED,
                WorkoutType.ENDURANCE,
                WorkoutType.REST
            )

            "specific" -> listOf(
                WorkoutType.STRENGTH,
                WorkoutType.SPEED,
                WorkoutType.ENDURANCE,
                WorkoutType.RECOVERY,
                WorkoutType.STRENGTH,
                WorkoutType.SPEED,
                WorkoutType.REST
            )

            "precomp" -> listOf(
                WorkoutType.STRENGTH,
                WorkoutType.ENDURANCE,
                WorkoutType.STRENGTH,
                WorkoutType.RECOVERY,
                WorkoutType.SPEED,
                WorkoutType.ENDURANCE,
                WorkoutType.REST
            )

            "comp" -> listOf(
                WorkoutType.SPEED,
                WorkoutType.SPEED,
                WorkoutType.RECOVERY,
                WorkoutType.SPEED,
                WorkoutType.RECOVERY,
                WorkoutType.REST,
                WorkoutType.REST
            )

            "recovery" -> List(7) { WorkoutType.RECOVERY }

            else -> List(7) { WorkoutType.REST }
        }
    }
}