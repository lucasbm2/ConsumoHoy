package com.proyecto.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Content(
    val data: List<Data>
)
