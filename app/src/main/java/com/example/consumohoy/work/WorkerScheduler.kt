package com.example.consumohoy.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.consumohoy.firebase.PrecioPvpcWorker
import com.example.consumohoy.firebase.PrecioWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    // Lanza ambos Workers: uno para SPOT + estimado, otro solo para PVPC real
    fun scheduleWorkers(context: Context) {
        schedulePrecioWorker(context)
        schedulePvpcWorker(context)
    }

    // Programa PrecioWorker a las 20:30 todos los días para obtener SPOT y PVPC estimado
    private fun schedulePrecioWorker(context: Context) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(currentTime)) add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val dailyRequest = PeriodicWorkRequestBuilder<PrecioWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,  // para reintentar si falla el primer intento de coger datos
                15, TimeUnit.MINUTES   // mínimo permitido
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // solo si hay conexión
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "precio_worker_diario",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyRequest
        )

        Log.i("WorkerScheduler", "PrecioWorker programado para las 20:30")
    }

    // Programa PrecioPvpcWorker a la 01:00 para lanzar la notificación del PVPC real si ya está disponible
    private fun schedulePvpcWorker(context: Context) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(currentTime)) add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val pvpcRequest = PeriodicWorkRequestBuilder<PrecioPvpcWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // solo si hay conexión
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "precio_worker_pvpc",
            ExistingPeriodicWorkPolicy.REPLACE,
            pvpcRequest
        )

        Log.i("WorkerScheduler", "PrecioPvpcWorker programado para la 01:00")
    }
}