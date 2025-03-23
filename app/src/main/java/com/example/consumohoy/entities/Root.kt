package com.example.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Root(
    val data: Data,
    val included: List<Included>? = null
)
