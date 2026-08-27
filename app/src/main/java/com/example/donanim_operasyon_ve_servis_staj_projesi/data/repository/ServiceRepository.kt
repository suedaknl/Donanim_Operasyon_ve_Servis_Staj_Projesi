package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceClosingSignature
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PersonnelDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceFeedback
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceFeedbackDao
import kotlinx.coroutines.flow.Flow
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote.FirestoreServiceDataSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ServiceRepository @Inject constructor(
    private val serviceDao: ServiceDao,
    private val personnelDao: PersonnelDao,
    private val firestoreDataSource: FirestoreServiceDataSource,
    private val serviceFeedbackDao: ServiceFeedbackDao
) {

    suspend fun insertRecord(record: ServiceRecord) {
        val isPool = record.assignmentType == "POOL"

        val assignedPersonnel = if (isPool) null else record.assignedPersonnelId?.let { personnelId ->
            try {
                personnelDao.getPersonnelById(personnelId)
            } catch (e: Exception) {
                null
            }
        }

        // Çoğaltma veya yeni kayıt sırasında operasyonel alanların resetlenmesi
        val cleanRecord = record.copy(
            id = 0,
            firestoreId = null,
            status = ServiceStatus.BEKLIYOR,
            assignmentType = if (isPool) "POOL" else "DIRECT",
            assignedPersonnelId = if (isPool) null else record.assignedPersonnelId,
            assignedPersonnelUid = if (isPool) null else assignedPersonnel?.firebaseUid,
            assignedPersonnelName = if (isPool) null else assignedPersonnel?.fullName,
            isArchived = false,
            archivedAt = null,
            rejectionReason = null,
            poolAssignmentDeadline = if (isPool) record.poolAssignmentDeadline else null
        )

        serviceDao.insertRecord(cleanRecord)

        withContext(Dispatchers.IO) {
            try {
                val allRecords = serviceDao.getAllRecords()
                val savedRecord = allRecords.find {
                    it.companyName == cleanRecord.companyName &&
                            it.serialNumber == cleanRecord.serialNumber &&
                            it.firestoreId == null
                } ?: cleanRecord

                val result = firestoreDataSource.saveServiceRecord(savedRecord)

                result.onSuccess { firestoreId ->
                    val updatedRecord = savedRecord.copy(firestoreId = firestoreId)
                    serviceDao.updateService(updatedRecord)

                    recordHistory(
                        firestoreId = firestoreId,
                        eventType = if (isPool) "JOB_ADDED_TO_POOL" else "SERVICE_CREATED",
                        title = if (isPool) "İş Havuzuna Gönderildi" else "İş Emri Oluşturuldu",
                        description = "Firma: ${savedRecord.companyName} (${savedRecord.deviceType})",
                        status = savedRecord.status,
                        performedByRole = "Admin"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- MÜŞTERİ DEĞERLENDİRMESİNİ FIRESTORE'DAN ÇEKİP ROOM'A SENKRONİZE ETME ---
    suspend fun syncServiceFeedback(serviceId: Int, firestoreId: String) {
        withContext(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val targetIds = listOfNotNull(serviceId.toString(), firestoreId.takeIf { !it.isNullOrBlank() })

                for (targetId in targetIds) {
                    firestore.collection("services")
                        .document(targetId)
                        .collection("feedback")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot != null && !snapshot.isEmpty) {
                                val doc = snapshot.documents[0]
                                val rating = doc.getLong("rating")?.toInt() ?: 0
                                val quality = doc.getLong("quality")?.toInt() ?: rating
                                val staff = doc.getLong("staff")?.toInt() ?: rating
                                val speed = doc.getLong("speed")?.toInt() ?: rating
                                val comment = doc.getString("comment") ?: ""

                                // --- GÜVENLİ TIMESTAMP OKUMA (Çökmeyi Önler) ---
                                val timestamp = try {
                                    doc.getLong("timestamp") ?: doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }

                                kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                                    val feedback = ServiceFeedback(
                                        serviceId = serviceId,
                                        firestoreId = targetId,
                                        rating = rating,
                                        quality = quality,
                                        staff = staff,
                                        speed = speed,
                                        comment = comment,
                                        timestamp = timestamp
                                    )
                                    serviceFeedbackDao.insertFeedback(feedback)
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- İŞ HAVUZU: FIRESTORE SOURCE OF TRUTH (OBSERVE) ---
    fun observePoolJobs(): Flow<List<ServiceRecord>> {
        return firestoreDataSource.observePoolJobs()
    }

    // --- İŞ HAVUZU: GET POOL JOBS (Doğrudan Firestore Bağlantısı) ---
    fun getPoolJobs(): Flow<List<ServiceRecord>> {
        return firestoreDataSource.observePoolJobs()
    }

    // --- İŞ HAVUZU: FIRESTORE TRANSACTION TABANLI CLAIM ---
    suspend fun claimPoolJob(
        serviceId: Int,
        firestoreId: String,
        personnelId: Int,
        personnelName: String,
        personnelUid: String
    ): Result<Unit> {
        val result = firestoreDataSource.claimPoolJob(
            firestoreId = firestoreId,
            personnelId = personnelId,
            personnelName = personnelName,
            personnelUid = personnelUid
        )

        if (result.isSuccess) {
            withContext(Dispatchers.IO) {
                try {
                    val localRecord = serviceDao.getServiceByFirestoreId(firestoreId)
                        ?: serviceDao.getServiceById(serviceId)

                    if (localRecord != null) {
                        val updatedLocal = localRecord.copy(
                            assignmentType = "DIRECT",
                            assignedPersonnelId = personnelId,
                            assignedPersonnelName = personnelName,
                            assignedPersonnelUid = personnelUid,
                            status = ServiceStatus.BEKLIYOR
                        )
                        serviceDao.updateService(updatedLocal)
                    }

                    recordHistory(
                        firestoreId = firestoreId,
                        eventType = "POOL_JOB_CLAIMED",
                        title = "İş Havuzundan Üstlenildi",
                        description = "Üstlenen Personel: $personnelName",
                        status = ServiceStatus.BEKLIYOR,
                        performedByUid = personnelUid,
                        performedByName = personnelName,
                        performedByRole = "Personel"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return Result.success(Unit)
        } else {
            return Result.failure(result.exceptionOrNull() ?: Exception("Bu iş başka bir personel tarafından az önce üstlenildi."))
        }
    }

    // --- ESKİ OVERLOAD (Uyumluluk İçin) ---
    suspend fun claimPoolJob(
        serviceId: Int,
        personnelId: Int,
        personnelName: String,
        personnelUid: String
    ): Boolean {
        val record = serviceDao.getServiceById(serviceId)
        if (record?.firestoreId != null) {
            val res = claimPoolJob(serviceId, record.firestoreId!!, personnelId, personnelName, personnelUid)
            return res.isSuccess
        }
        return false
    }

    suspend fun deleteRecord(record: ServiceRecord) {
        serviceDao.deleteRecord(record)

        withContext(Dispatchers.IO) {
            try {
                val firestoreId = record.firestoreId
                if (!firestoreId.isNullOrEmpty()) {
                    firestoreDataSource.deleteService(firestoreId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateStatus(recordId: Int, newStatus: String) {
        serviceDao.updateStatus(recordId, newStatus)

        withContext(Dispatchers.IO) {
            try {
                val record = serviceDao.getServiceById(recordId)
                val firestoreId = record?.firestoreId

                if (!firestoreId.isNullOrEmpty()) {
                    firestoreDataSource.updateServiceStatus(firestoreId, newStatus)

                    val (eventType, title) = when (newStatus) {
                        ServiceStatus.YOLDA -> "SERVICE_ACCEPTED" to "İş Emri Kabul Edildi (Yolda)"
                        ServiceStatus.ISLEME_BASLANDI -> "SERVICE_STARTED" to "İşleme Başlandı"
                        ServiceStatus.PARCA_BEKLENIYOR -> "PART_WAITING" to "Parça Bekleniyor"
                        else -> "STATUS_CHANGED" to "Durum Güncellendi: $newStatus"
                    }
                    recordHistory(
                        firestoreId = firestoreId,
                        eventType = eventType,
                        title = title,
                        status = newStatus,
                        performedByRole = "Personel"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getAllRecords(): List<ServiceRecord> {
        return serviceDao.getAllRecords()
    }

    suspend fun updateService(service: ServiceRecord) {
        val personnelUid = service.assignedPersonnelId?.let { personnelId ->
            try {
                personnelDao.getPersonnelById(personnelId)?.firebaseUid
            } catch (e: Exception) {
                null
            }
        }

        val finalStatus = if (service.status == ServiceStatus.IPTAL && service.assignedPersonnelId != null) {
            ServiceStatus.BEKLIYOR
        } else {
            service.status
        }

        val serviceToUpdate = service.copy(
            assignedPersonnelUid = personnelUid,
            status = finalStatus,
            rejectionReason = if (finalStatus == ServiceStatus.BEKLIYOR) null else service.rejectionReason
        )

        serviceDao.updateService(serviceToUpdate)

        withContext(Dispatchers.IO) {
            try {
                if (!serviceToUpdate.firestoreId.isNullOrEmpty()) {
                    firestoreDataSource.updateService(serviceToUpdate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getServiceById(id: Int): ServiceRecord? {
        val record = serviceDao.getServiceById(id)
        record?.firestoreId?.let { firestoreId ->
            if (firestoreId.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    syncServiceFeedback(id, firestoreId)
                }
            }
        }
        return record
    }

    suspend fun clearAssignedPersonnel(personnelId: Int) {
        serviceDao.clearAssignedPersonnel(personnelId)
    }

    suspend fun getRecordsByPersonnelId(personnelId: Int): List<ServiceRecord> {
        return serviceDao.getRecordsByPersonnelId(personnelId)
    }

    suspend fun insertServiceNote(note: ServiceNote) {
        serviceDao.insertServiceNote(note)

        withContext(Dispatchers.IO) {
            try {
                val serviceRecord = serviceDao.getServiceById(note.serviceRecordId)
                val firestoreId = serviceRecord?.firestoreId

                if (!firestoreId.isNullOrBlank()) {
                    firestoreDataSource.uploadNoteToFirebase(note, firestoreId)

                    if (note.noteType != "CLOSING") {
                        recordHistory(
                            firestoreId = firestoreId,
                            eventType = "NOTE_ADDED",
                            title = "Servis Notu Eklendi",
                            description = note.note,
                            status = serviceRecord.status,
                            performedByRole = "Personel"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getRemoteNotesForService(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            firestoreDataSource.getServiceNotes(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRemotePhotosForService(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            firestoreDataSource.getServicePhotos(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRemoteSignaturesForService(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            firestoreDataSource.getServiceSignatures(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClosingSignatureByServiceId(serviceId: Int): ServiceClosingSignature? {
        return serviceDao.getClosingSignature(serviceId)
    }

    fun getNotesForService(serviceRecordId: Int): Flow<List<ServiceNote>> {
        return serviceDao.getNotesForService(serviceRecordId)
    }

    suspend fun insertServicePhoto(photo: ServicePhoto) {
        serviceDao.insertServicePhoto(photo)

        withContext(Dispatchers.IO) {
            try {
                val serviceRecord = serviceDao.getServiceById(photo.serviceRecordId)
                val firestoreId = serviceRecord?.firestoreId

                if (!firestoreId.isNullOrBlank()) {
                    val photoType = photo.photoType ?: photo.photoCategory ?: "Diger"
                    firestoreDataSource.uploadPhotoToFirebase(
                        localUriString = photo.localUri,
                        firestoreId = firestoreId,
                        photoType = photoType
                    )

                    recordHistory(
                        firestoreId = firestoreId,
                        eventType = "PHOTO_ADDED",
                        title = "Fotoğraf Eklendi ($photoType)",
                        status = serviceRecord.status,
                        performedByRole = "Personel"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getPhotosForService(serviceRecordId: Int): Flow<List<ServicePhoto>> {
        return serviceDao.getPhotosForService(serviceRecordId)
    }

    suspend fun deleteServicePhoto(photo: ServicePhoto) {
        serviceDao.deleteServicePhoto(photo)
    }

    suspend fun getRemoteHistoryForService(firestoreId: String): Result<List<Map<String, Any>>> {
        return try {
            firestoreDataSource.getServiceHistory(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recordHistory(
        firestoreId: String?,
        eventType: String,
        title: String,
        description: String? = null,
        status: String,
        performedByUid: String? = null,
        performedByName: String? = null,
        performedByRole: String? = null
    ) {
        if (!firestoreId.isNullOrBlank()) {
            try {
                firestoreDataSource.addServiceHistory(
                    firestoreId = firestoreId,
                    eventType = eventType,
                    title = title,
                    description = description,
                    status = status,
                    performedByUid = performedByUid,
                    performedByName = performedByName,
                    performedByRole = performedByRole
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun completeServiceWork(
        serviceRecord: ServiceRecord,
        personnelId: Int,
        closingNoteText: String,
        signatureData: String,
        signatureUri: String? = null
    ): Result<Unit> {
        return try {
            val freshRecord = serviceDao.getServiceById(serviceRecord.id)
                ?: return Result.failure(Exception("Hata: İş emri veritabanında bulunamadı."))

            if (freshRecord.assignedPersonnelId != personnelId) {
                return Result.failure(Exception("Hata: Bu iş emrini kapatma yetkiniz yok."))
            }

            val currentStatus = freshRecord.status.trim()

            val isValidForClosing =
                currentStatus.equals(ServiceStatus.ISLEME_BASLANDI, ignoreCase = true) ||
                        currentStatus.equals(ServiceStatus.PARCA_BEKLENIYOR, ignoreCase = true)

            if (!isValidForClosing) {
                return Result.failure(
                    Exception(
                        "Hata: İş emri kapatılabilmesi için 'İşleme Başlandı' veya " +
                                "'Parça Bekleniyor' durumunda olmalıdır."
                    )
                )
            }

            if (closingNoteText.isBlank() || signatureData.isBlank()) {
                return Result.failure(
                    Exception("Hata: Kapanış notu ve imza zorunludur.")
                )
            }

            val closingNote = ServiceNote(
                serviceRecordId = freshRecord.id,
                personnelId = personnelId,
                note = closingNoteText,
                noteType = "CLOSING",
                createdAt = System.currentTimeMillis()
            )

            val signature = ServiceClosingSignature(
                serviceRecordId = freshRecord.id,
                personnelId = personnelId,
                signatureData = signatureData,
                signatureLocalUri = signatureUri ?: "",
                createdAt = System.currentTimeMillis()
            )

            val updatedRecord = freshRecord.copy(
                status = ServiceStatus.TAMAMLANDI
            )

            serviceDao.completeServiceTransaction(
                updatedRecord,
                closingNote,
                signature
            )

            withContext(Dispatchers.IO) {
                try {
                    val firestoreId = updatedRecord.firestoreId

                    if (!firestoreId.isNullOrEmpty()) {
                        firestoreDataSource.completeServiceInFirestore(
                            firestoreId = firestoreId,
                            status = ServiceStatus.TAMAMLANDI
                        )

                        if (!signatureUri.isNullOrBlank()) {
                            firestoreDataSource.uploadSignatureToFirebase(
                                localUriString = signatureUri,
                                firestoreId = firestoreId,
                                signatureData = signatureData
                            )
                        }

                        firestoreDataSource.uploadNoteToFirebase(
                            note = closingNote,
                            firestoreId = firestoreId
                        )

                        recordHistory(
                            firestoreId = firestoreId,
                            eventType = "SIGNATURE_ADDED",
                            title = "Dijital İmza Alındı",
                            status = ServiceStatus.TAMAMLANDI,
                            performedByRole = "Personel"
                        )

                        recordHistory(
                            firestoreId = firestoreId,
                            eventType = "SERVICE_COMPLETED",
                            title = "İş Tamamlandı",
                            description = closingNoteText,
                            status = ServiceStatus.TAMAMLANDI,
                            performedByRole = "Personel"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncAllServices() {
        val result = firestoreDataSource.getAllServices()

        result.onSuccess { remoteServices ->
            val allPersonnel = try {
                personnelDao.getAllPersonnelList()
            } catch (e: Exception) {
                emptyList()
            }

            val allLocalServices = try {
                serviceDao.getAllRecords()
            } catch (e: Exception) {
                emptyList()
            }

            remoteServices.forEach { remoteService ->
                val firestoreId = remoteService.firestoreId
                if (!firestoreId.isNullOrEmpty()) {
                    val existingLocalService = serviceDao.getServiceByFirestoreId(firestoreId)

                    val matchedPersonnelId: Int? = if (remoteService.assignmentType == "POOL") null else remoteService.assignedPersonnelUid?.let { uid ->
                        allPersonnel.find { it.firebaseUid == uid }?.id
                    }

                    if (existingLocalService != null) {
                        val serviceToUpdate = remoteService.copy(
                            id = existingLocalService.id,
                            assignedPersonnelId = matchedPersonnelId ?: existingLocalService.assignedPersonnelId
                        )
                        serviceDao.updateService(serviceToUpdate)
                        // Senkronizasyon sırasında feedback verisini de çekelim
                        syncServiceFeedback(existingLocalService.id, firestoreId)
                    } else {
                        val ghostRecord = allLocalServices.find {
                            it.firestoreId == null &&
                                    it.companyName == remoteService.companyName &&
                                    it.serialNumber == remoteService.serialNumber
                        }

                        if (ghostRecord != null) {
                            val serviceToUpdate = remoteService.copy(
                                id = ghostRecord.id,
                                assignedPersonnelId = matchedPersonnelId
                            )
                            serviceDao.updateService(serviceToUpdate)
                            syncServiceFeedback(ghostRecord.id, firestoreId)
                        } else {
                            val serviceToInsert = remoteService.copy(
                                assignedPersonnelId = matchedPersonnelId
                            )
                            serviceDao.insertRecord(serviceToInsert)
                        }
                    }
                }
            }
        }
    }

    suspend fun rejectService(serviceId: Int, rejectionReason: String): Result<Unit> {
        return try {
            val localRecord = serviceDao.getServiceById(serviceId) ?: return Result.failure(Exception("Kayıt bulunamadı"))
            val updatedRecord = localRecord.copy(status = ServiceStatus.IPTAL, rejectionReason = rejectionReason)

            serviceDao.updateService(updatedRecord)

            if (!updatedRecord.firestoreId.isNullOrEmpty()) {
                val firestoreResult = firestoreDataSource.rejectService(updatedRecord.firestoreId!!, rejectionReason)

                if (firestoreResult.isFailure) {
                    return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Firebase güncellemesi başarısız"))
                }

                recordHistory(
                    firestoreId = updatedRecord.firestoreId,
                    eventType = "SERVICE_REJECTED",
                    title = "İş Emri Reddedildi / İptal Edildi",
                    description = rejectionReason,
                    status = ServiceStatus.IPTAL,
                    performedByRole = "Personel"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAndStartServiceWork(recordId: Int, personnelId: Int, distance: Float) {
        val record = serviceDao.getServiceById(recordId) ?: return

        if (record.status == ServiceStatus.YOLDA) {
            val firestoreId = record.firestoreId
            if (!firestoreId.isNullOrBlank()) {
                recordHistory(
                    firestoreId = firestoreId,
                    eventType = "LOCATION_VERIFIED",
                    title = "İş Konumu Doğrulandı",
                    description = "Personel iş noktasına ${distance.toInt()} m uzaklıkta.",
                    status = ServiceStatus.ISLEME_BASLANDI,
                    performedByRole = "Personel"
                )
            }
            updateStatus(recordId, ServiceStatus.ISLEME_BASLANDI)
        }
    }

    suspend fun syncServicesFromFirestore(
        personnelUid: String,
        localPersonnelId: Int
    ) {
        val result = firestoreDataSource.getServicesForPersonnel(personnelUid)

        result.onSuccess { remoteServices ->
            remoteServices.forEach { remoteService ->
                val firestoreId = remoteService.firestoreId
                if (!firestoreId.isNullOrEmpty()) {
                    val existingLocalService = serviceDao.getServiceByFirestoreId(firestoreId)

                    val serviceToSave = remoteService.copy(
                        assignedPersonnelId = localPersonnelId,
                        id = existingLocalService?.id ?: 0
                    )

                    if (existingLocalService != null) {
                        serviceDao.updateService(serviceToSave)
                        syncServiceFeedback(existingLocalService.id, firestoreId)
                    } else {
                        serviceDao.insertRecord(serviceToSave)
                    }
                }
            }
        }
    }
}