package com.example.donanim_operasyon_ve_servis_staj_projesi.data.datasource

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.NotificationEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationCenterDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val notificationsCollection = firestore.collection("notifications")

    fun observeNotifications(recipientUid: String): Flow<List<NotificationEntity>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("recipientUid", recipientUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val timestamp = doc.get("createdAt") as? Timestamp
                        val createdAtMillis = timestamp?.toDate()?.time ?: doc.getLong("createdAt") ?: 0L

                        NotificationEntity(
                            id = doc.id,
                            recipientUid = doc.getString("recipientUid").orEmpty(),
                            role = doc.getString("role").orEmpty(),
                            type = doc.getString("type").orEmpty(),
                            title = doc.getString("title").orEmpty(),
                            body = doc.getString("body").orEmpty(),
                            targetId = doc.getString("targetId"),
                            createdAt = createdAtMillis,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAllAsRead(recipientUid: String) {
        val snapshot = notificationsCollection
            .whereEqualTo("recipientUid", recipientUid)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        val batch = firestore.batch()
        for (doc in snapshot.documents) {
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }

    suspend fun deleteNotification(notificationId: String) {
        notificationsCollection.document(notificationId).delete().await()
    }

    suspend fun clearAllForUser(recipientUid: String) {
        val snapshot = notificationsCollection
            .whereEqualTo("recipientUid", recipientUid)
            .get()
            .await()

        val batch = firestore.batch()
        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}