package com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("notification_users")

    suspend fun saveToken(
        uid: String,
        role: String,
        personnelId: Int?,
        token: String
    ): Result<Unit> {
        return try {
            if (uid.isBlank()) return Result.failure(IllegalArgumentException("UID boş olamaz"))

            val data = mapOf(
                "uid" to uid,
                "role" to role,
                "personnelId" to personnelId,
                "fcmToken" to token,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            collection.document(uid).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearToken(uid: String): Result<Unit> {
        return try {
            if (uid.isBlank()) return Result.failure(IllegalArgumentException("UID boş olamaz"))

            val updates = mapOf(
                "fcmToken" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            collection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}