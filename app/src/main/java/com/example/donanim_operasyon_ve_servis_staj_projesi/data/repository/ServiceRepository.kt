package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceClosingSignature
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PersonnelDao
import kotlinx.coroutines.flow.Flow
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote.FirestoreServiceDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServiceRepository(
    private val serviceDao: ServiceDao,
    private val personnelDao: PersonnelDao,
    private val firestoreDataSource: FirestoreServiceDataSource = FirestoreServiceDataSource()
) {

    suspend fun insertRecord(record: ServiceRecord) {
        val personnelUid = record.assignedPersonnelId?.let { personnelId ->
            try {
                val personnel = personnelDao.getPersonnelById(personnelId)
                personnel?.firebaseUid
            } catch (e: Exception) {
                null
            }
        }

        val recordWithPersonnel = record.copy(assignedPersonnelUid = personnelUid)
        serviceDao.insertRecord(recordWithPersonnel)

        withContext(Dispatchers.IO) {
            try {
                val allRecords = serviceDao.getAllRecords()
                val savedRecord = allRecords.find {
                    it.companyName == recordWithPersonnel.companyName &&
                            it.serialNumber == recordWithPersonnel.serialNumber &&
                            it.firestoreId == null
                } ?: recordWithPersonnel

                val result = firestoreDataSource.saveServiceRecord(savedRecord)

                result.onSuccess { firestoreId ->
                    val updatedRecord = savedRecord.copy(firestoreId = firestoreId)
                    serviceDao.updateService(updatedRecord)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        return serviceDao.getServiceById(id)
    }

    suspend fun clearAssignedPersonnel(personnelId: Int) {
        serviceDao.clearAssignedPersonnel(personnelId)
    }

    suspend fun getRecordsByPersonnelId(personnelId: Int): List<ServiceRecord> {
        return serviceDao.getRecordsByPersonnelId(personnelId)
    }

    suspend fun insertServiceNote(note: ServiceNote) {
        serviceDao.insertServiceNote(note)
    }

    suspend fun getClosingSignatureByServiceId(serviceId: Int): ServiceClosingSignature? {
        return serviceDao.getClosingSignature(serviceId)
    }

    fun getNotesForService(serviceRecordId: Int): Flow<List<ServiceNote>> {
        return serviceDao.getNotesForService(serviceRecordId)
    }

    suspend fun insertServicePhoto(photo: ServicePhoto) {
        serviceDao.insertServicePhoto(photo)
    }

    fun getPhotosForService(serviceRecordId: Int): Flow<List<ServicePhoto>> {
        return serviceDao.getPhotosForService(serviceRecordId)
    }

    suspend fun deleteServicePhoto(photo: ServicePhoto) {
        serviceDao.deleteServicePhoto(photo)
    }

    suspend fun completeServiceWork(
        serviceRecord: ServiceRecord,
        personnelId: Int,
        closingNoteText: String,
        signatureUri: String
    ): Result<Unit> {
        return try {
            val freshRecord = serviceDao.getServiceById(serviceRecord.id)
                ?: return Result.failure(Exception("Hata: İş emri veritabanında bulunamadı."))

            if (freshRecord.assignedPersonnelId != personnelId) {
                return Result.failure(Exception("Hata: Bu iş emrini kapatma yetkiniz yok."))
            }

            val currentStatus = freshRecord.status.trim()
            val isValidForClosing = currentStatus.equals(ServiceStatus.ISLEME_BASLANDI, ignoreCase = true) ||
                    currentStatus.equals(ServiceStatus.PARCA_BEKLENIYOR, ignoreCase = true)

            if (!isValidForClosing) {
                return Result.failure(Exception("Hata: İş emri kapatılabilmesi için 'İşleme Başlandı' durumunda olmalıdır."))
            }

            if (closingNoteText.isBlank() || signatureUri.isBlank()) {
                return Result.failure(Exception("Hata: Kapanış notu ve imza zorunludur."))
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
                signatureLocalUri = signatureUri,
                createdAt = System.currentTimeMillis()
            )

            val updatedRecord = freshRecord.copy(status = ServiceStatus.TAMAMLANDI)

            // 1. Yerel Room Transaction Kaydı
            serviceDao.completeServiceTransaction(updatedRecord, closingNote, signature)

            // 2. Firestore Senkronizasyonu
            withContext(Dispatchers.IO) {
                try {
                    val firestoreId = updatedRecord.firestoreId
                    if (!firestoreId.isNullOrEmpty()) {
                        firestoreDataSource.updateService(updatedRecord)
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

                    val matchedPersonnelId: Int? = remoteService.assignedPersonnelUid?.let { uid ->
                        allPersonnel.find { it.firebaseUid == uid }?.id
                    }

                    if (existingLocalService != null) {
                        val serviceToUpdate = remoteService.copy(
                            id = existingLocalService.id,
                            assignedPersonnelId = matchedPersonnelId ?: existingLocalService.assignedPersonnelId
                        )
                        serviceDao.updateService(serviceToUpdate)
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
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
                    } else {
                        serviceDao.insertRecord(serviceToSave)
                    }
                }
            }
        }
    }
}