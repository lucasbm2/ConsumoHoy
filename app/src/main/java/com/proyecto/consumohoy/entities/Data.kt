package com.proyecto.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Data(
    val type: String,
    val id: String,
    val attributes: Attributes
)
