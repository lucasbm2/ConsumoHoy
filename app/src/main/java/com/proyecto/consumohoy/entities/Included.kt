package com.proyecto.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Included(
    val type: String,
    val id: String,
    val attributes: IncludedAttributes
)