package com.example.consumohoy.firebase

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.consumohoy.conexion.RetrofitClient
import com.example.consumohoy.firebase.NotificationHelper
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

            val fechaBase = if (now.hour > 20 || (now.hour == 20 && now.minute >= 30)) {
                LocalDate.now().plusDays(1)
            } else {
                Log.i("PrecioWorker", "Antes de las 20:30, se reintentará más tarde.")
                return Result.retry() // Esto provocará un nuevo intento
            }

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
