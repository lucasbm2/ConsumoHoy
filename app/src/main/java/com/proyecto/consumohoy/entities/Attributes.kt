package com.proyecto.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Attributes(
    val title: String,
    val lastUpdate: String,
    val description: String? = null
)
