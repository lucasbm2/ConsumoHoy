package com.proyecto.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Meta(
    val cacheControl: CacheControl
)
