package com.proyecto.consumohoy.database

import androidx.room.*

@Dao
interface ConsumptionDao {
    @Insert
    suspend fun insert(entry: ConsumptionEntry)

    @Query("SELECT * FROM consumption_entries")
    suspend fun getAll(): List<ConsumptionEntry>

    @Delete
    suspend fun delete(entry: ConsumptionEntry)
}
