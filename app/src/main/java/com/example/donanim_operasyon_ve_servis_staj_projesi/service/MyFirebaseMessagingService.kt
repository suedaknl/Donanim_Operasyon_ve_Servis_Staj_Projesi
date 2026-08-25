package com.example.donanim_operasyon_ve_servis_staj_projesi.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.donanim_operasyon_ve_servis_staj_projesi.MainActivity
import com.example.donanim_operasyon_ve_servis_staj_projesi.R
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.NotificationRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "FCM_TOKEN generated (new/refreshed)")

        serviceScope.launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val sessionManager = SessionManager(applicationContext)
                val role = sessionManager.getUserRole() ?: "PERSONNEL"
                val personnelId = if (role == "PERSONNEL") sessionManager.getPersonnelId() else null

                if (role.isNotBlank()) {
                    notificationRepository.saveToken(
                        uid = currentUser.uid,
                        role = role,
                        personnelId = personnelId,
                        token = token
                    )
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data["type"] ?: "DEFAULT"
        Log.d("FCM_MESSAGE", "FCM_MESSAGE received type=$type")

        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Servis Bildirimi"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "Yeni bir bildiriminiz var."
        val targetId = remoteMessage.data["targetId"] ?: ""

        showLocalNotification(title, body, type, targetId)
    }

    private fun showLocalNotification(title: String, body: String, type: String, targetId: String) {
        val channelId = "service_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servis Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Servis operasyon ve bildirim kanalı"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            putExtra("notification_target_id", targetId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}