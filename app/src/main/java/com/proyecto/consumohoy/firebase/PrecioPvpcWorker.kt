package com.proyecto.consumohoy.firebase

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proyecto.consumohoy.conexion.RetrofitClient
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

//Clase para actualizar precios de PVPC
class PrecioPvpcWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
            val fechaBase = LocalDate.now().plusDays(1)
            val startDateTime = LocalDateTime.of(fechaBase, LocalTime.MIN)
            val endDateTime = LocalDateTime.of(fechaBase, LocalTime.MAX)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val startDate = startDateTime.atZone(ZoneId.systemDefault()).format(formatter)
            val endDate = endDateTime.atZone(ZoneId.systemDefault()).format(formatter)

            val response = RetrofitClient.apiService.obtenerPrecios(startDate, endDate, "hour")
            val included = response.included

            val pvpcValores = included?.firstOrNull {
                it.attributes?.title?.contains("pvpc", ignoreCase = true) == true
            }?.attributes?.values

            val pvpcValue = pvpcValores?.lastOrNull()?.value?.toFloat()
            val rawDatetime = pvpcValores?.lastOrNull()?.datetime

            val fechaFormateada = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val fecha = inputFormat.parse(rawDatetime?.replace("Z", "") ?: "")
                outputFormat.format(fecha ?: "")
            } catch (e: Exception) {
                "fecha desconocida"
            }

            if (pvpcValue != null) {
                NotificationHelper.showNotification(
                    context,
                    "Precio PVPC actualizado",
                    "Datos de: $fechaFormateada"
                )
                prefs.edit().putFloat("pvpc", pvpcValue).apply()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
