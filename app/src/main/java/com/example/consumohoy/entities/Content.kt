package com.example.consumohoy.entities

import kotlinx.serialization.Serializable
import com.example.consumohoy.entities.Data
@Serializable
data class Content(
    val data: List<Data>
)
