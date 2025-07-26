package com.ravi.samstudioapp.domain.model

data class DailyLog(
    val date: Long,
    val mood: String,
    val sleepHours: Double,
    val notes: String,
    val waterGlasses: Int,
    val exerciseMinutes: Int,
    val energyLevel: Int, // 1-10 scale
    val gratitude: String
) 