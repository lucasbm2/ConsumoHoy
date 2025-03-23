package com.example.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class CacheControl(
    val public: Boolean,
    val maxAge: Int
)
