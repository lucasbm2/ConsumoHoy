package com.proyecto.consumohoy.database

import androidx.room.Database
import androidx.room.RoomDatabase

// Clase para la base de datos
@Database(entities = [ConsumptionEntry::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun consumptionDao(): ConsumptionDao
}
