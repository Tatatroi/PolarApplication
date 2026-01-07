package com.application.polarapplication.model

data class Workout(
    val title: String,
    val duration: String,
    val focus: String,
    val intensity: String = "Medium" // You can add more fields as you grow
)