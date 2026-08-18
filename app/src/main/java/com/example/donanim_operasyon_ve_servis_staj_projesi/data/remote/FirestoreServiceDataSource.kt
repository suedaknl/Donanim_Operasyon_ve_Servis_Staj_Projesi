package com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote

import android.net.Uri
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirestoreServiceDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val collection = firestore.collection("services")

    // --- STORAGE VE SUBCOLLECTION FONKSİYONLARI ---

    suspend fun uploadPhotoToFirebase(localUriString: String, firestoreId: String, photoType: String): Result<String> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş."))

            // DÜZELTME: Dosya yolundaki bozuk "file:/" tekrarları temizlendi, güvenli URI oluşturuldu
            val cleanPath = localUriString.replace("file://", "").replace("file:", "")
            val uri = if (localUriString.startsWith("content://")) {
                android.net.Uri.parse(localUriString)
            } else {
                android.net.Uri.fromFile(java.io.File(cleanPath))
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "${photoType}_$timestamp.jpg"

            val storageRef = storage.reference.child("services/$firestoreId/photos/$fileName")
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val photoData = hashMapOf(
                "photoType" to photoType,
                "downloadUrl" to downloadUrl,
                "timestamp" to timestamp
            )
            collection.document(firestoreId).collection("photos").add(photoData).await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadSignatureToFirebase(localUriString: String, firestoreId: String): Result<String> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş."))

            // DÜZELTME: İmza dosya yolu temizlenip güvenli URI'ye çevrildi
            val cleanPath = localUriString.replace("file://", "").replace("file:", "")
            val uri = if (localUriString.startsWith("content://")) {
                android.net.Uri.parse(localUriString)
            } else {
                android.net.Uri.fromFile(java.io.File(cleanPath))
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "signature_$timestamp.png"

            val storageRef = storage.reference.child("services/$firestoreId/signatures/$fileName")
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val signatureData = hashMapOf(
                "downloadUrl" to downloadUrl,
                "timestamp" to timestamp
            )
            collection.document(firestoreId).collection("signatures").add(signatureData).await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServicePhotos(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = collection.document(firestoreId).collection("photos").get().await()
            val photos = snapshot.documents.mapNotNull { it.data }
            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServiceSignatures(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = collection.document(firestoreId).collection("signatures").get().await()
            val signatures = snapshot.documents.mapNotNull { it.data }
            Result.success(signatures)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadNoteToFirebase(note: com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote, firestoreId: String): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş."))

            val noteData = hashMapOf(
                "note" to note.note,
                "noteType" to (note.noteType ?: "GENERAL"),
                "personnelId" to note.personnelId,
                "createdAt" to note.createdAt
            )

            collection.document(firestoreId).collection("notes").add(noteData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServiceNotes(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = collection.document(firestoreId).collection("notes").get().await()
            val notes = snapshot.documents.mapNotNull { it.data }
            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- MEVCUT FONKSİYONLAR ---

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
                "rejectionReason" to record.rejectionReason
            )

            if (!record.firestoreId.isNullOrEmpty()) {
                collection.document(record.firestoreId!!).set(serviceMap).await()
                Result.success(record.firestoreId!!)
            } else {
                val docRef = collection.add(serviceMap).await()
                docRef.update("firestoreId", docRef.id).await()
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllServices(): Result<List<ServiceRecord>> {
        return try {
            val snapshot = collection.get().await()
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
                    rejectionReason = document.getString("rejectionReason")
                )
                servicesList.add(service)
            }
            Result.success(servicesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
                "assignedPersonnelUid" to record.assignedPersonnelUid,
                "firestoreId" to firestoreId,
                "rejectionReason" to record.rejectionReason
            )

            collection.document(firestoreId).set(serviceMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    suspend fun completeServiceInFirestore(
        firestoreId: String,
        status: String
    ): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş"))

            val updates = mutableMapOf<String, Any>(
                "status" to status
            )

            collection.document(firestoreId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectService(firestoreId: String, rejectionReason: String): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş"))

            val updates = mapOf(
                "status" to "İptal",
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
                    rejectionReason = document.getString("rejectionReason")
                )
                servicesList.add(service)
            }
            Result.success(servicesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}