package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import android.util.Log
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote.FirestoreNotificationDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val dataSource: FirestoreNotificationDataSource
) {
    suspend fun saveToken(uid: String, role: String, personnelId: Int?, token: String) {
        val result = dataSource.saveToken(uid, role, personnelId, token)
        if (result.isSuccess) {
            Log.d("FCM_TOKEN", "FCM_TOKEN saved role=$role, personnelId=$personnelId")
        } else {
            Log.e("FCM_TOKEN", "Failed to save FCM token: ${result.exceptionOrNull()?.message}")
        }
    }

    suspend fun clearToken(uid: String) {
        val result = dataSource.clearToken(uid)
        if (result.isSuccess) {
            Log.d("FCM_TOKEN", "FCM_TOKEN cleared for uid=$uid")
        } else {
            Log.e("FCM_TOKEN", "Failed to clear FCM token: ${result.exceptionOrNull()?.message}")
        }
    }
}