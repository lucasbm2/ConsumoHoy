package com.example.consumohoy.entities

class Device(
    val id: Int = 0,
    val name: String,
    val consumption: Double,
    val durationMin: Int,
    val flexible: Boolean = true
)