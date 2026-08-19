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

            // Room Entity'sini Firestore formatına manuel mapliyoruz (gender eklendi)
            val personnelMap = hashMapOf(
                "id" to personnel.id,
                "fullName" to personnel.fullName,
                "username" to personnel.username,
                "phoneNumber" to personnel.phoneNumber,
                "role" to personnel.role,
                "isActive" to personnel.isActive,
                "email" to personnel.email,
                "firebaseUid" to personnel.firebaseUid,
                "gender" to personnel.gender // Cinsiyet bilgisi Firestore'a kaydediliyor
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
                    firebaseUid = document.getString("firebaseUid") ?: document.id,
                    gender = document.getString("gender") ?: "ERKEK" // Okunurken eklendi (Eski kayıtlarda yoksa güvenli varsayılan)
                )
                Result.success(personnel)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePersonnelLocation(uid: String, lat: Double, lon: Double): Result<Unit> {
        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val collection = db.collection("personnel")

            // Personel dokümanını UID ile bul
            val snapshot = collection.whereEqualTo("firebaseUid", uid).get().await()

            if (!snapshot.isEmpty) {
                val docId = snapshot.documents.first().id
                val updates = mapOf(
                    "currentLatitude" to lat,
                    "currentLongitude" to lon,
                    "lastLocationUpdate" to System.currentTimeMillis() // Mevcut timestamp standardımız
                )
                collection.document(docId).update(updates).await()
            } else {
                println("Location Update Hatası: Bu UID ile personel dokümanı bulunamadı.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- YENİ EKLENEN FONKSİYON: Admin Haritası İçin Canlı Personel Konumlarını Okuma ---
    suspend fun getPersonnelLocations(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = collection.get().await()
            val list = snapshot.documents.map { doc ->
                mapOf(
                    "id" to (doc.getLong("id")?.toInt() ?: 0),
                    "firebaseUid" to (doc.getString("firebaseUid") ?: ""),
                    "fullName" to (doc.getString("fullName") ?: ""),
                    "currentLatitude" to (doc.getDouble("currentLatitude") ?: 0.0),
                    "currentLongitude" to (doc.getDouble("currentLongitude") ?: 0.0),
                    "lastLocationUpdate" to (doc.getLong("lastLocationUpdate") ?: 0L)
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
                    firebaseUid = document.getString("firebaseUid") ?: document.id,
                    gender = document.getString("gender") ?: "ERKEK" // Toplu okumada da güvenli maplendi
                )
            }
            Result.success(personnelList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}