package com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreServiceDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = firestore.collection("services")

    // Admin tarafından yeni iş emri oluşturulduğunda Firestore'a kaydetmek / güncellemek için
    suspend fun saveServiceRecord(record: ServiceRecord): Result<String> {
        return try {
            val serviceMap = hashMapOf(
                "companyName" to record.companyName,
                "deviceType" to record.deviceType,
                "deviceModel" to record.deviceModel,
                "serialNumber" to record.serialNumber,
                "location" to record.location,
                "priority" to record.priority,
                "issueDescription" to record.issueDescription,
                "status" to record.status,
                "date" to record.date,
                "contactPerson" to record.contactPerson,
                "contactPhone" to record.contactPhone,
                "address" to record.address,
                "plannedDate" to record.plannedDate,
                "assignedPersonnelUid" to record.assignedPersonnelUid,
                "firestoreId" to record.firestoreId,
                "rejectionReason" to record.rejectionReason // rejectionReason eklendi
            )

            if (!record.firestoreId.isNullOrEmpty()) {
                // Eğer firestoreId varsa mevcut dokümanı güncelle (Duplicate engelleme)
                collection.document(record.firestoreId!!).set(serviceMap).await()
                Result.success(record.firestoreId!!)
            } else {
                // Yoksa yeni doküman oluştur ve üretilen ID'yi dön
                val docRef = collection.add(serviceMap).await()

                // Dokümanın kendisine de kendi ID'sini alan olarak yazıyoruz
                docRef.update("firestoreId", docRef.id).await()

                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ADMİN İÇİN: Firestore'daki tüm mevcut gerçek iş emirlerini okumak için
    suspend fun getAllServices(): Result<List<ServiceRecord>> {
        return try {
            val snapshot = collection.get().await()
            val servicesList = mutableListOf<ServiceRecord>()

            for (document in snapshot.documents) {
                val service = ServiceRecord(
                    id = 0, // Room auto-generate için 0; Repository'de mevcut local ID ile ezilecek veya Insert edilecek
                    companyName = document.getString("companyName") ?: "",
                    deviceType = document.getString("deviceType") ?: "",
                    deviceModel = document.getString("deviceModel") ?: "",
                    serialNumber = document.getString("serialNumber") ?: "",
                    location = document.getString("location") ?: "",
                    priority = document.getString("priority") ?: "Normal",
                    issueDescription = document.getString("issueDescription") ?: "",
                    status = document.getString("status") ?: "Bekliyor",
                    date = document.getString("date") ?: "",
                    assignedPersonnelId = null,
                    contactPerson = document.getString("contactPerson"),
                    contactPhone = document.getString("contactPhone"),
                    address = document.getString("address"),
                    plannedDate = document.getString("plannedDate"),
                    firestoreId = document.id, // En önemli eşleştirme anahtarı
                    assignedPersonnelUid = document.getString("assignedPersonnelUid"),
                    rejectionReason = document.getString("rejectionReason") // rejectionReason okunuyor
                )
                servicesList.add(service)
            }
            Result.success(servicesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. İş Emrini Düzenleme (Update)
    suspend fun updateService(record: ServiceRecord): Result<Unit> {
        return try {
            val firestoreId = record.firestoreId ?: return Result.failure(IllegalArgumentException("Firestore ID eksik"))

            val serviceMap = hashMapOf(
                "companyName" to record.companyName,
                "deviceType" to record.deviceType,
                "deviceModel" to record.deviceModel,
                "serialNumber" to record.serialNumber,
                "location" to record.location,
                "priority" to record.priority,
                "issueDescription" to record.issueDescription,
                "status" to record.status,
                "date" to record.date,
                "contactPerson" to record.contactPerson,
                "contactPhone" to record.contactPhone,
                "address" to record.address,
                "plannedDate" to record.plannedDate,
                "assignedPersonnelId" to record.assignedPersonnelId,
                "assignedPersonnelUid" to record.assignedPersonnelUid,
                "firestoreId" to firestoreId,
                "rejectionReason" to record.rejectionReason // rejectionReason eklendi
            )

            collection.document(firestoreId).set(serviceMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Sadece Durum Güncelleme (Status Update)
    suspend fun updateServiceStatus(firestoreId: String, newStatus: String): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş"))

            collection.document(firestoreId)
                .update("status", newStatus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2.1 Personel İş Reddetme Güncellemesi (Status + Rejection Reason)
    suspend fun rejectService(firestoreId: String, rejectionReason: String): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş"))

            val updates = mapOf(
                "status" to "İptal Edildi", // Mevcut ServiceStatus.IPTAL değeriyle birebir uyumlu
                "rejectionReason" to rejectionReason
            )

            collection.document(firestoreId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. İş Emrini Silme (Delete)
    suspend fun deleteService(firestoreId: String): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş"))

            collection.document(firestoreId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // PERSONEL İÇİN: Sadece giriş yapan personele atanmış iş emirlerini okumak için
    suspend fun getServicesForPersonnel(uid: String): Result<List<ServiceRecord>> {
        return try {
            val snapshot = collection.whereEqualTo("assignedPersonnelUid", uid).get().await()
            val servicesList = mutableListOf<ServiceRecord>()

            for (document in snapshot.documents) {
                val service = ServiceRecord(
                    id = 0,
                    companyName = document.getString("companyName") ?: "",
                    deviceType = document.getString("deviceType") ?: "",
                    deviceModel = document.getString("deviceModel") ?: "",
                    serialNumber = document.getString("serialNumber") ?: "",
                    location = document.getString("location") ?: "",
                    priority = document.getString("priority") ?: "Normal",
                    issueDescription = document.getString("issueDescription") ?: "",
                    status = document.getString("status") ?: "Bekliyor",
                    date = document.getString("date") ?: "",
                    assignedPersonnelId = null,
                    contactPerson = document.getString("contactPerson"),
                    contactPhone = document.getString("contactPhone"),
                    address = document.getString("address"),
                    plannedDate = document.getString("plannedDate"),
                    firestoreId = document.id,
                    assignedPersonnelUid = document.getString("assignedPersonnelUid"),
                    rejectionReason = document.getString("rejectionReason") // rejectionReason okunuyor
                )
                servicesList.add(service)
            }
            Result.success(servicesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}