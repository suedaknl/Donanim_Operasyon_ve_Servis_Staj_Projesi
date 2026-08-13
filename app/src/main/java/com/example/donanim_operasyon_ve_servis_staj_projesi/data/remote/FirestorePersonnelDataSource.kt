package com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestorePersonnelDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = firestore.collection("personnel")

    suspend fun savePersonnel(personnel: Personnel): Result<Unit> {
        return try {
            val uid = personnel.firebaseUid ?: return Result.failure(IllegalArgumentException("Firebase UID eksik"))

            // Room Entity'sini Firestore formatına manuel mapliyoruz (Güvenlik için 'password' çıkarıldı)
            val personnelMap = hashMapOf(
                "id" to personnel.id,
                "fullName" to personnel.fullName,
                "username" to personnel.username,
                "phoneNumber" to personnel.phoneNumber,
                "role" to personnel.role,
                "isActive" to personnel.isActive,
                "email" to personnel.email,
                "firebaseUid" to personnel.firebaseUid
            )

            // UID'yi doküman ID'si olarak kullanıp kaydediyoruz
            collection.document(uid).set(personnelMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPersonnel(uid: String): Result<Personnel?> {
        return try {
            val document = collection.document(uid).get().await()

            if (document.exists()) {
                val personnel = Personnel(
                    id = document.getLong("id")?.toInt() ?: 0,
                    fullName = document.getString("fullName") ?: "",
                    username = document.getString("username") ?: "",
                    phoneNumber = document.getString("phoneNumber") ?: "",
                    role = document.getString("role") ?: "",
                    password = "", // Firestore'dan şifre gelmez
                    isActive = document.getBoolean("isActive") ?: false,
                    email = document.getString("email") ?: "",
                    firebaseUid = document.getString("firebaseUid") ?: document.id
                )
                Result.success(personnel)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // --- FAZ 3: EKSİK OLAN VE EKLENEN FONKSİYON ---
    suspend fun getAllPersonnel(): Result<List<Personnel>> {
        return try {
            val snapshot = collection.get().await()
            val personnelList = snapshot.documents.mapNotNull { document ->
                Personnel(
                    id = document.getLong("id")?.toInt() ?: 0,
                    fullName = document.getString("fullName") ?: "",
                    username = document.getString("username") ?: "",
                    phoneNumber = document.getString("phoneNumber") ?: "",
                    role = document.getString("role") ?: "",
                    password = "", // Firestore'dan şifre gelmez
                    isActive = document.getBoolean("isActive") ?: false,
                    email = document.getString("email") ?: "",
                    firebaseUid = document.getString("firebaseUid") ?: document.id
                )
            }
            Result.success(personnelList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}