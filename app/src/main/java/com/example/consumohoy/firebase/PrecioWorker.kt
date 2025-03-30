package com.example.consumohoy.firebase

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.consumohoy.conexion.RetrofitClient
import com.example.consumohoy.firebase.NotificationHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PrecioWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = java.time.ZonedDateTime.now()

            val fechaBase = if (now.hour > 20 || (now.hour == 20 && now.minute >= 30)) {
                LocalDate.now().plusDays(1)
            } else {
                Log.i("PrecioWorker", "Antes de las 20:30, se reintentará más tarde.")
                return Result.retry() // Esto provocará un nuevo intento
            }

            val startDateTime = LocalDateTime.of(fechaBase, LocalTime.MIN)
            val endDateTime = LocalDateTime.of(fechaBase, LocalTime.MAX)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val startDate = startDateTime.atZone(java.time.ZoneId.systemDefault()).format(formatter)
            val endDate = endDateTime.atZone(java.time.ZoneId.systemDefault()).format(formatter)

            val response = RetrofitClient.apiService.obtenerPrecios(
                startDate = startDate,
                endDate = endDate,
                timeTrunc = "hour"
            )

            val included = response.included

            val spotValue = included
                ?.firstOrNull { it.id.contains("spot") }
                ?.attributes?.values?.lastOrNull()
                ?.value?.toFloat()

            val pvpcValue = included
                ?.firstOrNull { it.id.contains("pvpc") }
                ?.attributes?.values?.lastOrNull()
                ?.value?.toFloat()

            val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
            val lastSpot = prefs.getFloat("spot", -1f)
            val lastPvpc = prefs.getFloat("pvpc", -1f)

            @Suppress("MissingPermission")
            if (spotValue != null && spotValue != lastSpot) {
                NotificationHelper.showNotification(context, "Precio SPOT actualizado", "Nuevo: $spotValue €/MWh")
                prefs.edit().putFloat("spot", spotValue).apply()
            }

            @Suppress("MissingPermission")
            if (pvpcValue != null && pvpcValue != lastPvpc) {
                NotificationHelper.showNotification(context, "Precio PVPC actualizado", "Nuevo: $pvpcValue €/MWh")
                prefs.edit().putFloat("pvpc", pvpcValue).apply()
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
