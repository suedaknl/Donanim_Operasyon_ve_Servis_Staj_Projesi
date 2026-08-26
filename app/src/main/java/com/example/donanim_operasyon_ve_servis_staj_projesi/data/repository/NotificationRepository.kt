package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.datasource.FirestoreNotificationCenterDataSource
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote.FirestoreNotificationDataSource
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.NotificationDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestoreCenterDataSource: FirestoreNotificationCenterDataSource,
    private val firestoreTokenDataSource: FirestoreNotificationDataSource,
    private val notificationDao: NotificationDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    suspend fun saveToken(
        uid: String,
        role: String,
        personnelId: Int?,
        token: String
    ): Result<Unit> {
        return firestoreTokenDataSource.saveToken(uid, role, personnelId, token)
    }

    suspend fun clearToken(uid: String): Result<Unit> {
        return firestoreTokenDataSource.clearToken(uid)
    }

    fun startSync(recipientUid: String) {
        if (recipientUid.isBlank()) return

        stopSync()

        syncJob = repositoryScope.launch {
            try {
                firestoreCenterDataSource
                    .observeNotifications(recipientUid)
                    .collectLatest { remoteList ->

                        android.util.Log.d(
                            "NotificationSync",
                            "Firestore'dan gelen bildirim sayısı: ${remoteList.size} (UID: $recipientUid)"
                        )

                        remoteList.forEach { notification ->
                            notificationDao.upsert(notification)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e(
                    "NotificationSync",
                    "Sync hatası: ${e.message}"
                )
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun getNotifications(
        recipientUid: String
    ): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsForUser(recipientUid)
    }

    fun getUnreadCount(
        recipientUid: String
    ): Flow<Int> {
        return notificationDao.getUnreadCount(recipientUid)
    }

    suspend fun markAsRead(
        notificationId: String,
        recipientUid: String
    ) {
        try {
            firestoreCenterDataSource.markAsRead(
                notificationId,
                recipientUid
            )

            notificationDao.markAsRead(
                notificationId,
                recipientUid
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllAsRead(recipientUid: String) {
        try {
            firestoreCenterDataSource.markAllAsRead(recipientUid)
            notificationDao.markAllAsRead(recipientUid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteNotification(notificationId: String) {
        try {
            firestoreCenterDataSource.deleteNotification(notificationId)
            notificationDao.deleteById(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllNotifications(recipientUid: String) {
        try {
            firestoreCenterDataSource.clearAllForUser(recipientUid)
            notificationDao.deleteByRecipient(recipientUid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}