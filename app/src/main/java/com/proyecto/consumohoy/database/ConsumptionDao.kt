package com.proyecto.consumohoy.database

import androidx.room.*

// Interfaz para acceder a la base de datos
@Dao
interface ConsumptionDao {
    @Insert
    suspend fun insert(entry: ConsumptionEntry)

    @Update
    suspend fun update(entry: ConsumptionEntry)

    @Query("SELECT * FROM consumption_entries")
    suspend fun getAll(): List<ConsumptionEntry>

    @Delete
    suspend fun delete(entry: ConsumptionEntry)
}
