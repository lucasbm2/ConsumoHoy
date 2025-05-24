package com.proyecto.consumohoy.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consumption_entries")
data class ConsumptionEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val power: Float, //en W
    val priority: String,
    val usageMinutes: Int //por defecto
)
