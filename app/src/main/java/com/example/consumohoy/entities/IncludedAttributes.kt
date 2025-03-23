package com.example.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class IncludedAttributes(
    val title: String,
    val lastUpdate: String,
    val description: String? = null,
    val values: List<Value> = emptyList(),
    val total: Double? = null,
    val totalPercentage: Double? = null
)