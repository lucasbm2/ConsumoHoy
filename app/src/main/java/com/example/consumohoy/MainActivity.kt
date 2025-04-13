package com.example.consumohoy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.consumohoy.firebase.NotificationHelper
import com.example.consumohoy.firebase.PrecioWorker
import com.example.consumohoy.navigation.NavigationWrapper
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val workRequest = OneTimeWorkRequestBuilder<PrecioWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)

        NotificationHelper.createChannel(this)

        //Hace que el movil se suscriba al topic "precios"
        //Para poder recibir notificaciones push
        FirebaseMessaging.getInstance().subscribeToTopic("precios")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "Suscrito con exito al topic 'precios'")
                } else {
                    Log.e("MainActivity", "Error al suscribirse al topic 'precios'")
                }

                // Permiso para notificaciones
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    //funcion checkselfpermission comprueba en el manifest si el permiso ya fue otorgado
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        //Si no fue otorgado, se solicita
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            1001
                        )
                    }
                }

                //Para recibir notificaciones push, que se ejecute cada vez que se reciba una notificacion
                val currentTime = java.util.Calendar.getInstance()
                val targetTime = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 20)
                    set(java.util.Calendar.MINUTE, 30)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    if (before(currentTime)) {
                        add(java.util.Calendar.DAY_OF_MONTH, 1)
                    }
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
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "precio_worker_diario",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    dailyRequest
                )

                setContent {
                    NavigationWrapper()
                }
            }
    }
}
