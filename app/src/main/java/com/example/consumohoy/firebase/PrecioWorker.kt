package com.example.consumohoy.firebase

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.consumohoy.conexion.RetrofitClient
import com.example.consumohoy.entities.Included
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Clase para actualizar precios en segundo plano gracias a WorkManager y Coroutines
class PrecioWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    //Propiedades para acceder a SharedPreferences que es donde se guardan los precios
    val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)

    //Aqui se hace la llamada a la API para obtener los precios
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
// Si es antes de las 20:30, estimamos el día siguiente
// Si es después, intentamos obtener PVPC real para ese día
            val fechaBase = LocalDate.now().plusDays(1)


            Log.i("PrecioWorker", "Intentando obtener precios para la fecha: $fechaBase")
            val startDateTime = LocalDateTime.of(fechaBase, LocalTime.MIN)
            val endDateTime = LocalDateTime.of(fechaBase, LocalTime.MAX)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val startDate = startDateTime.atZone(ZoneId.systemDefault()).format(formatter)
            val endDate = endDateTime.atZone(ZoneId.systemDefault()).format(formatter)

            //Respuesta que devuelve la API con todos los datos del dia
            val response = RetrofitClient.apiService.obtenerPrecios(
                startDate = startDate,
                endDate = endDate,
                timeTrunc = "hour"
            )

            //Guardamos los datos en SharedPreferences para acceder a ellos en otras pantallas
            val included = response.included

            //////CALCULAR PROMEDIO PRECIO PVPC AUN SIN ACTUALIZAR
            val diferenciaMedia = 59.9f // Esta es la media que calculamos tú y yo

// Convertimos included a mutable para poder modificarlo si es necesario
            val includedMutable = included?.toMutableList() ?: mutableListOf()

            //SpotItem es el precio del mercado mayorista (SPOT)
            val spotItem = includedMutable.firstOrNull {
                it.attributes?.title?.contains("spot", ignoreCase = true) == true
            }

            // SpotValores es la lista horaria de precios del mercado mayorista (SPOT)
            val spotValores = spotItem?.attributes?.values

            //PVPCValores es la lista horaria de precios del PVPC
            var pvpcValores = includedMutable.firstOrNull {
                it.attributes?.title?.contains("pvpc", ignoreCase = true) == true
            }?.attributes?.values

            // Si no hay PVPC real, creamos uno estimado y lo metemos en included
            if (pvpcValores.isNullOrEmpty() && spotValores != null && spotItem != null) {
                val valoresEstimados = spotValores.map {
                    it.copy(value = it.value.toFloat() + diferenciaMedia)
                }

                // Creamos un nuevo Included simulado tipo PVPC (ahora pvpc estimado)
                //Para añadirlo a Included y guardarlo en SharedPreferences para acceder a él en otras pantallas
                val pvpcEstimado = Included(
                    id = "9999",
                    type = "pvpc-estimado",
                    attributes = spotItem.attributes.copy(
                        title = "PVPC (estimado)",
                        values = valoresEstimados
                    )
                )
                // Añadimos el nuevo Included a la lista
                includedMutable.add(pvpcEstimado)


                Log.i("PrecioWorker", "🟡 PVPC estimado añadido a included")

// Guardar en SharedPreferences
                val gson = com.google.gson.Gson()
                val pvpcJson = gson.toJson(pvpcEstimado)
                prefs.edit().putString("pvpc_estimado_json", pvpcJson).apply()

                Log.i("PrecioWorker", "🟡 PVPC estimado guardado en SharedPreferences")


                // Actualizamos pvpcValores para usarlo más abajo
                pvpcValores = valoresEstimados
            }

            //SpotValue es el precio del mercado mayorista (SPOT) en el momento
            val spotValue = included?.firstOrNull {
                it.attributes?.title?.contains("spot", ignoreCase = true) == true
            }?.attributes?.values?.lastOrNull()?.value?.toFloat()
            val rawSpotDatetime = spotValores?.lastOrNull()?.datetime

// Formatear la fecha SPOT al estilo dd-MM-yyyy
            val spotFechaFormateada = try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val outputFormat = java.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val fecha = inputFormat.parse(rawSpotDatetime?.replace("Z", "") ?: "")
                outputFormat.format(fecha ?: "")
            } catch (e: Exception) {
                "fecha desconocida"
            }

            //Si no hay precios de SPOT, no lanzamos notificacion
            if (spotValue != null) {
                NotificationHelper.showNotification(
                    context,
                    "Precio diario actualizado (SPOT)",
                    "Datos de: $spotFechaFormateada"
                )
                prefs.edit().putFloat("spot", spotValue).apply()
            }

            // PVPCValue es el último precio del PVPC (estimado o real)
            val pvpcValue = pvpcValores?.lastOrNull()?.value?.toFloat()
            val rawDatetime = pvpcValores?.lastOrNull()?.datetime

            val fechaFormateada = try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val outputFormat = java.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val fecha = inputFormat.parse(rawDatetime?.replace("Z", "") ?: "")
                outputFormat.format(fecha ?: "")
            } catch (e: Exception) {
                "fecha desconocida"
            }

            if (pvpcValue != null) {
                NotificationHelper.showNotification(
                    context,
                    "Precio diario actualizado (PVPC)",
                    "Datos de: $fechaFormateada"
                )
                prefs.edit().putFloat("pvpc", pvpcValue).apply()
            }


            //Result es el resultado de la llamada a la API
            Result.success()

            //Si hay error, intentarlo de nuevo
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
