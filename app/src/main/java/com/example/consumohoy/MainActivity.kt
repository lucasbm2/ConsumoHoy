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
import androidx.work.WorkManager
import com.example.consumohoy.firebase.NotificationHelper
import com.example.consumohoy.firebase.PrecioWorker
import com.example.consumohoy.navigation.NavigationWrapper
import com.google.firebase.messaging.FirebaseMessaging
import androidx.work.OneTimeWorkRequestBuilder
import com.example.consumohoy.work.WorkerScheduler

class MainActivity : ComponentActivity() {

    //Se ejecuta al iniciar la app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ejecuta una vez al iniciar
        val workRequest = OneTimeWorkRequestBuilder<PrecioWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)

        // Crea canal de notificaciones
        NotificationHelper.createChannel(this)

        // Suscribirse al topic de Firebase
        FirebaseMessaging.getInstance().subscribeToTopic("precios")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "Suscrito con éxito al topic 'precios'")
                } else {
                    Log.e("MainActivity", "Error al suscribirse al topic 'precios'")
                }

                // Solicitar permiso si es Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }

                // 💡 Aquí usas tu WorkerScheduler personalizado
                WorkerScheduler.scheduleWorkers(this)

                setContent {
                    NavigationWrapper()
                }
            }


    }
}