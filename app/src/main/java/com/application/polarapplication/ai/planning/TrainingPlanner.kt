package com.application.polarapplication.ai.planning

import com.application.polarapplication.ai.model.TrainingPlan
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TrainingPlanner {

    private val phases = listOf(
        "general" to 0.40,
        "specific" to 0.30,
        "precomp" to 0.20,
        "comp" to 0.05,
        "recovery" to 0.05
    )

    fun generatePlan(
        competitionDate: LocalDate,
        startDate: LocalDate = LocalDate.now(),
        availableDays: Set<Int> = setOf(1, 2, 3, 4, 5),
        focus: String = "full"
    ): TrainingPlan {
        val totalDays = ChronoUnit.DAYS.between(startDate, competitionDate).toInt()
            .coerceAtLeast(7)

        var currentStart = startDate
        val mesoList = mutableListOf<MesoCycle>()

        for ((phase, percentage) in phases) {
            val days = (totalDays * percentage).toInt().coerceAtLeast(7)
            val phaseEnd = currentStart.plusDays(days.toLong())

            val weeks = (days / 7).coerceAtLeast(1)
            val microcycles = (0 until weeks).map { weekIndex ->
                MicroCycle(
                    startDate = currentStart.plusDays((weekIndex * 7).toLong()),
                    endDate = currentStart.plusDays((weekIndex * 7 + 6).toLong()),
                    workouts = MicroCycleGenerator.generate(phase, availableDays, focus) // <-- aici
                )
            }

            mesoList.add(MesoCycle(currentStart, phaseEnd, phase, microcycles))
            currentStart = phaseEnd
        }

        return TrainingPlan(startDate, competitionDate, mesoList)
    }
}
