package com.application.polarapplication.ai.planning

import com.application.polarapplication.ai.daily.WorkoutType
import java.time.LocalDate

data class MicroCycle (
    val startDate: LocalDate,
    val endDate: LocalDate,
    val workouts: List<WorkoutType>
)


