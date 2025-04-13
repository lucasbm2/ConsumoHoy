package com.example.consumohoy.firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.consumohoy.R

//Creo un objeto NotificationHelper para crear y mostrar notificaciones
object NotificationHelper {
    private const val CHANNEL_ID = "precios_channel"
    private const val CHANNEL_NAME = "Actualización de precios"
    private const val CHANNEL_DESC = "Notificaciones sobre el precio SPOT y PVPC"

    //Creo un canal de notificacion para Android Oreo y superiores para recibir notificaciones
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    //Mostrar notificacion con titulo y cuerpo definidos en funcion de los parametros que introduzca
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(context: Context, titulo: String, mensaje: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}