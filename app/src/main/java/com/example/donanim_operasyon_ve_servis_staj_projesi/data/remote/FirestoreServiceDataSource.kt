package com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote

import android.net.Uri
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreServiceDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val collection = firestore.collection("services")

    // --- İŞ HAVUZU: REALTIME LISTENER (Esnek Filtreleme) ---
    fun observePoolJobs(): Flow<List<ServiceRecord>> = callbackFlow {
        val listener = collection
            .whereEqualTo("assignmentType", "POOL")
            .whereEqualTo("isArchived", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val records = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val assignedId = doc.getLong("assignedPersonnelId")
                        val assignedUid = doc.getString("assignedPersonnelUid")

                        // Atanmamış (assignedPersonnelId null/0 ve uid boş olanlar) havuz işleridir
                        if (assignedId == null && assignedUid.isNullOrBlank()) {
                            ServiceRecord(
                                id = 0,
                                companyName = doc.getString("companyName") ?: "",
                                deviceType = doc.getString("deviceType") ?: "",
                                deviceModel = doc.getString("deviceModel") ?: "",
                                serialNumber = doc.getString("serialNumber") ?: "",
                                location = doc.getString("location") ?: "",
                                priority = doc.getString("priority") ?: "Normal",
                                issueDescription = doc.getString("issueDescription") ?: "",
                                status = doc.getString("status") ?: "Bekliyor",
                                date = doc.getString("date") ?: "",
                                assignedPersonnelId = null,
                                assignedPersonnelName = null,
                                assignedPersonnelUid = null,
                                contactPerson = doc.getString("contactPerson"),
                                contactPhone = doc.getString("contactPhone"),
                                address = doc.getString("address"),
                                plannedDate = doc.getString("plannedDate"),
                                firestoreId = doc.id,
                                rejectionReason = doc.getString("rejectionReason"),
                                latitude = doc.getDouble("latitude"),
                                longitude = doc.getDouble("longitude"),
                                isArchived = doc.getBoolean("isArchived") ?: false,
                                archivedAt = doc.getLong("archivedAt"),
                                assignmentType = doc.getString("assignmentType") ?: "POOL"
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(records)
            }

        awaitClose { listener.remove() }
    }

    // --- İŞ HAVUZU: ATOMİK CLAIM TRANSACTION ---
    suspend fun claimPoolJob(
        firestoreId: String,
        personnelId: Int,
        personnelName: String,
        personnelUid: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = collection.document(firestoreId)
                val snapshot = transaction.get(docRef)

                val assignmentType = snapshot.getString("assignmentType")
                val assignedId = snapshot.getLong("assignedPersonnelId")

                if (assignmentType != "POOL" || assignedId != null) {
                    throw Exception("Bu iş başka bir personel tarafından az önce üstlenildi.")
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "assignmentType" to "DIRECT",
                        "assignedPersonnelId" to personnelId,
                        "assignedPersonnelName" to personnelName,
                        "assignedPersonnelUid" to personnelUid
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- STORAGE VE SUBCOLLECTION FONKSİYONLARI ---

    suspend fun uploadPhotoToFirebase(localUriString: String, firestoreId: String, photoType: String): Result<String> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş."))

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

    suspend fun uploadSignatureToFirebase(
        localUriString: String?,
        firestoreId: String,
        signatureData: String?
    ): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("Firestore ID boş.")
                )
            }

            val timestamp = System.currentTimeMillis()
            var downloadUrl: String? = null

            if (!localUriString.isNullOrBlank()) {
                try {
                    val cleanPath = localUriString
                        .replace("file://", "")
                        .replace("file:", "")

                    val uri = if (localUriString.startsWith("content://")) {
                        Uri.parse(localUriString)
                    } else {
                        Uri.fromFile(java.io.File(cleanPath))
                    }

                    val fileName = "signature_$timestamp.png"
                    val storageRef = storage.reference.child(
                        "services/$firestoreId/signatures/$fileName"
                    )

                    storageRef.putFile(uri).await()
                    downloadUrl = storageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val signatureMap = hashMapOf<String, Any>(
                "timestamp" to timestamp
            )

            if (!signatureData.isNullOrBlank()) {
                signatureMap["signatureData"] = signatureData
            }

            if (!downloadUrl.isNullOrBlank()) {
                signatureMap["downloadUrl"] = downloadUrl
            }

            if (signatureData.isNullOrBlank() && downloadUrl.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("Kaydedilecek imza verisi bulunamadı.")
                )
            }

            collection
                .document(firestoreId)
                .collection("signatures")
                .add(signatureMap)
                .await()

            Result.success(Unit)

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
                "assignedPersonnelId" to record.assignedPersonnelId,
                "assignedPersonnelUid" to record.assignedPersonnelUid,
                "assignedPersonnelName" to record.assignedPersonnelName,
                "firestoreId" to record.firestoreId,
                "rejectionReason" to record.rejectionReason,
                "latitude" to record.latitude,
                "longitude" to record.longitude,
                "isArchived" to record.isArchived,
                "archivedAt" to record.archivedAt,
                "assignmentType" to record.assignmentType
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
                    assignedPersonnelId = document.getLong("assignedPersonnelId")?.toInt(),
                    assignedPersonnelName = document.getString("assignedPersonnelName"),
                    contactPerson = document.getString("contactPerson"),
                    contactPhone = document.getString("contactPhone"),
                    address = document.getString("address"),
                    plannedDate = document.getString("plannedDate"),
                    firestoreId = document.id,
                    assignedPersonnelUid = document.getString("assignedPersonnelUid"),
                    rejectionReason = document.getString("rejectionReason"),
                    latitude = document.getDouble("latitude"),
                    longitude = document.getDouble("longitude"),
                    isArchived = document.getBoolean("isArchived") ?: false,
                    archivedAt = document.getLong("archivedAt"),
                    assignmentType = document.getString("assignmentType") ?: "DIRECT"
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
                "assignedPersonnelId" to record.assignedPersonnelId,
                "assignedPersonnelUid" to record.assignedPersonnelUid,
                "assignedPersonnelName" to record.assignedPersonnelName,
                "firestoreId" to firestoreId,
                "rejectionReason" to record.rejectionReason,
                "latitude" to record.latitude,
                "longitude" to record.longitude,
                "isArchived" to record.isArchived,
                "archivedAt" to record.archivedAt,
                "assignmentType" to record.assignmentType
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

    // --- İŞLEM GEÇMİŞİ (SERVICE HISTORY) FONKSİYONLARI ---

    suspend fun addServiceHistory(
        firestoreId: String,
        eventType: String,
        title: String,
        description: String?,
        status: String,
        performedByUid: String?,
        performedByName: String?,
        performedByRole: String?
    ): Result<Unit> {
        return try {
            if (firestoreId.isBlank()) return Result.failure(IllegalArgumentException("Firestore ID boş."))

            val historyData = hashMapOf(
                "eventType" to eventType,
                "title" to title,
                "description" to (description ?: ""),
                "status" to status,
                "performedByUid" to (performedByUid ?: ""),
                "performedByName" to (performedByName ?: "Bilinmeyen"),
                "performedByRole" to (performedByRole ?: "Sistem"),
                "timestamp" to System.currentTimeMillis()
            )

            collection.document(firestoreId).collection("history").add(historyData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServiceHistory(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = collection.document(firestoreId)
                .collection("history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
            val historyList = snapshot.documents.mapNotNull { it.data }
            Result.success(historyList)
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
                    assignedPersonnelId = document.getLong("assignedPersonnelId")?.toInt(),
                    assignedPersonnelName = document.getString("assignedPersonnelName"),
                    contactPerson = document.getString("contactPerson"),
                    contactPhone = document.getString("contactPhone"),
                    address = document.getString("address"),
                    plannedDate = document.getString("plannedDate"),
                    firestoreId = document.id,
                    assignedPersonnelUid = document.getString("assignedPersonnelUid"),
                    rejectionReason = document.getString("rejectionReason"),
                    latitude = document.getDouble("latitude"),
                    longitude = document.getDouble("longitude"),
                    isArchived = document.getBoolean("isArchived") ?: false,
                    archivedAt = document.getLong("archivedAt"),
                    assignmentType = document.getString("assignmentType") ?: "DIRECT"
                )
                servicesList.add(service)
            }
            Result.success(servicesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}