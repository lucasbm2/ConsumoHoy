package com.example.consumohoy.firebase

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.consumohoy.conexion.RetrofitClient
import com.example.consumohoy.entities.Included
import com.example.consumohoy.entities.Attributes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PrecioWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
            val now = ZonedDateTime.now(ZoneId.systemDefault())

            //USAR ESTIMACION PVPC
            val before20_30 = now.hour < 20 || (now.hour == 20 && now.minute < 30)

// Si es antes de las 20:30, estimamos el día siguiente
// Si es después, intentamos obtener PVPC real para ese día
            val fechaBase = LocalDate.now().plusDays(1)

// A partir de las 00:00 del día siguiente, ya usamos PVPC real si está disponible
            val forceRevisionPVPC = LocalDate.now() == fechaBase

            Log.i("PrecioWorker", "🕒 Hora actual: ${now.hour}:${now.minute}")
            Log.i("PrecioWorker", "📅 Fecha base seleccionada: $fechaBase")
            Log.i("PrecioWorker", "🔁 ¿Forzar revisión PVPC real? $forceRevisionPVPC")
            //USAR ESTIMACION PVPC


            Log.i("PrecioWorker", "Intentando obtener precios para la fecha: $fechaBase")
            val startDateTime = LocalDateTime.of(fechaBase, LocalTime.MIN)
            val endDateTime = LocalDateTime.of(fechaBase, LocalTime.MAX)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val startDate = startDateTime.atZone(ZoneId.systemDefault()).format(formatter)
            val endDate = endDateTime.atZone(ZoneId.systemDefault()).format(formatter)

            val response = RetrofitClient.apiService.obtenerPrecios(
                startDate = startDate,
                endDate = endDate,
                timeTrunc = "hour"
            )

            val included = response.included

            //////CALCULAR PROMEDIO PRECIO PVPC AUN SIN ACTUALIZAR
            val diferenciaMedia = 59.9f // Esta es la media que calculamos tú y yo

// Convertimos included a mutable para poder modificarlo si es necesario
            val includedMutable = included?.toMutableList() ?: mutableListOf()


            val spotItem = includedMutable.firstOrNull {
                it.attributes?.title?.contains("spot", ignoreCase = true) == true
            }
            val spotValores = spotItem?.attributes?.values

            var pvpcValores = includedMutable.firstOrNull {
                it.attributes?.title?.contains("pvpc", ignoreCase = true) == true
            }?.attributes?.values

// Si no hay PVPC real, creamos uno estimado y lo metemos en included
            if (pvpcValores.isNullOrEmpty() && spotValores != null && spotItem != null) {
                val valoresEstimados = spotValores.map {
                    it.copy(value = it.value.toFloat() + diferenciaMedia)
                }

                // Creamos un nuevo Included simulado tipo PVPC
                val pvpcEstimado = Included(
                    id = "9999",
                    type = "pvpc-estimado",
                    attributes = spotItem.attributes.copy(
                        title = "PVPC (estimado)",
                        values = valoresEstimados
                    )
                )


                includedMutable.add(pvpcEstimado)
                Log.i("PrecioWorker", "🟡 PVPC estimado añadido a included")

// Guardar en SharedPreferences
                val gson = com.google.gson.Gson()
                val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                val pvpcJson = gson.toJson(pvpcEstimado)
                prefs.edit().putString("pvpc_estimado_json", pvpcJson).apply()

                Log.i("PrecioWorker", "🟡 PVPC estimado guardado en SharedPreferences")


                // Actualizamos pvpcValores para usarlo más abajo
                pvpcValores = valoresEstimados
            }

            val listaEstimaciones = mutableMapOf<String, Float>()

            if (!pvpcValores.isNullOrEmpty() && forceRevisionPVPC) {
                // Si ya tenemos los datos reales del PVPC, los usamos
                pvpcValores.forEach { valor ->
                    listaEstimaciones[valor.datetime] = valor.value.toFloat()
                }
                Log.i("PrecioWorker", "🔵 Usando datos REALES de PVPC")
            } else {
                // Si NO tenemos PVPC, lo estimamos sumando al SPOT
                spotValores?.forEach { valor ->
                    val estimado = valor.value.toFloat() + diferenciaMedia
                    listaEstimaciones[valor.datetime] = estimado
                }
                Log.i("PrecioWorker", "🟡 Usando ESTIMACIÓN de PVPC con +$diferenciaMedia €/MWh")
            }

            Log.i("PrecioWorker", "🔍 SPOT horas: ${spotValores?.size ?: 0}")
            Log.i("PrecioWorker", "🔍 PVPC horas: ${pvpcValores?.size ?: 0}")


            //////CALCULAR PROMEDIO PRECIO PVPC AUN SIN ACTUALIZAR

            val spotValue = included?.firstOrNull {
                it.attributes?.title?.contains("spot", ignoreCase = true) == true
            }?.attributes?.values?.lastOrNull()?.value?.toFloat()

            //COMENTO PARA PONER LA NOTIFICACION MAS TARDE
//            val pvpcValue = included?.firstOrNull {
//                it.attributes?.title?.contains("pvpc", ignoreCase = true) == true
//            }?.attributes?.values?.lastOrNull()?.value?.toFloat()


            included?.forEach {
                Log.i("PrecioWorker", "ID encontrado en included: ${it.id}")
            }
            included?.forEach {
                Log.i("PrecioWorker", "ID: ${it.id}, title: ${it.attributes?.title}, type: ${it.type}")
            }

            val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)



            if (spotValue != null) {
                NotificationHelper.showNotification(context, "Precio diario actualizado (SPOT)", "Valor: $spotValue €/MWh")
                prefs.edit().putFloat("spot", spotValue).apply()

            }


            //COMENTO PARA PONER LA NOTIFICACION MAS TARDE
//            if (pvpcValue != null) {
//                NotificationHelper.showNotification(context, "Precio diario actualizado (PVPC)", "Valor: $pvpcValue €/MWh")
//                prefs.edit().putFloat("pvpc", pvpcValue).apply()
//            }


            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
