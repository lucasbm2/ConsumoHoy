package com.proyecto.consumohoy.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ConsumptionEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun consumptionDao(): ConsumptionDao
}
