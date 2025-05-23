package com.example.thebest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SensorData(
    val temperature: Double,
    val humidity: Double,
    val light: Int,
    val soil: Int
)