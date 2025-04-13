package com.example.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Value(
    val value: Float,
    val percentage: Double,
    val datetime: String
)
