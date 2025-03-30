package com.example.consumohoy.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.consumohoy.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Enviar token al servidor si es necesario
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        crearCanalNotificacion()
        remoteMessage.notification?.let {
            mostrarNotificacion(it.title, it.body)
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Actualización de Precios"
            val descripcion = "Notifica actualizaciones del mercado eléctrico."
            val importancia = NotificationManager.IMPORTANCE_HIGH
            val canal = NotificationChannel("channel_precio", nombre, importancia).apply {
                description = descripcion
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(canal)
        }
    }

    private fun mostrarNotificacion(titulo: String?, cuerpo: String?) {
        val builder = NotificationCompat.Builder(this, "channel_precio")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo ?: "ConsumoHoy")
            .setContentText(cuerpo ?: "Nueva actualización disponible")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        @Suppress("MissingPermission")
        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
