package com.example.consumohoy.conexion

import com.example.consumohoy.entities.Root
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    //Especificamos ruta del EndPoint y parámetros a enviar en la consulta
    @GET("mercados/precios-mercados-tiempo-real")
    suspend fun obtenerPrecios(
        @Query("start_date") startDate: String,  // Fecha de inicio
        @Query("end_date") endDate: String,      // Fecha de fin
        @Query("time_trunc") timeTrunc: String   // Intervalo de tiempo ("hour")
    ): Root  // Devuelve el objeto directamente
}
