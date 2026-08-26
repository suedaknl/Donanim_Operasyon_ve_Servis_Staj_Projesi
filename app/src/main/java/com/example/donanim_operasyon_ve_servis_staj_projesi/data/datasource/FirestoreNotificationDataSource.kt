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
class FirestoreNotificationDataSource @Inject constructor(
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
                        val id = doc.id
                        val rUid = doc.getString("recipientUid") ?: return@mapNotNull null
                        val role = doc.getString("role") ?: ""
                        val type = doc.getString("type") ?: ""
                        val title = doc.getString("title") ?: ""
                        val body = doc.getString("body") ?: ""
                        val targetId = doc.getString("targetId")

                        val rawCreatedAt = doc.get("createdAt")
                        val createdAt: Long = when (rawCreatedAt) {
                            is Timestamp -> rawCreatedAt.toDate().time
                            is Number -> rawCreatedAt.toLong()
                            else -> System.currentTimeMillis()
                        }

                        val isRead = doc.getBoolean("isRead") ?: false

                        NotificationEntity(
                            id = id,
                            recipientUid = rUid,
                            role = role,
                            type = type,
                            title = title,
                            body = body,
                            targetId = targetId,
                            createdAt = createdAt,
                            isRead = isRead
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String, recipientUid: String) {
        val docRef = notificationsCollection.document(notificationId)
        val docSnap = docRef.get().await()
        if (docSnap.exists() && docSnap.getString("recipientUid") == recipientUid) {
            docRef.update("isRead", true).await()
        }
    }

    suspend fun markAllAsRead(recipientUid: String) {
        val snapshot = notificationsCollection
            .whereEqualTo("recipientUid", recipientUid)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        if (snapshot.isEmpty) return

        val batch = firestore.batch()
        for (doc in snapshot.documents) {
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }
}