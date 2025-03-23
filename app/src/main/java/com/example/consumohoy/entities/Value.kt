package com.example.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Value(
    val value: Double,
    val percentage: Double,
    val datetime: String
)
