package com.example.consumohoy.entities

import kotlinx.serialization.Serializable

@Serializable
data class Root(
    val data: Data,
    var included: List<Included>? = null
)
